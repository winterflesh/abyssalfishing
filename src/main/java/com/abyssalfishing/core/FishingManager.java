package com.abyssalfishing.core;

import com.abyssalfishing.AbyssalFishing;
import com.abyssalfishing.config.AbyssalConfig;
import com.abyssalfishing.utils.HypixelUtils;
import com.abyssalfishing.utils.SoundUtils;
import com.abyssalfishing.utils.PlayerUtils;
import com.abyssalfishing.features.SeaCreatureKiller;
import com.abyssalfishing.features.MouseMovement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class FishingManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("AbyssalFishing");
    private final MinecraftClient client;
    private final AbyssalConfig config;
    private final Random random;
    private final SeaCreatureKiller seaCreatureKiller;
    private final MouseMovement mouseMovement;
    
    // Fishing state
    private boolean active;
    private FishingState currentState;
    private FishingBobberEntity fishingBobber;
    private long lastCastTime;
    private long lastCatchTime;
    private long lastReelTime;
    private int consecutiveCatches;
    private int failedAttempts;
    
    // Anti-detection
    private long nextActionDelay;
    private float humanizationFactor;
    
    // Humanization: Random breaks and variations
    private long lastRandomBreak = 0;
    private static final long RANDOM_BREAK_INTERVAL_MIN = 300000; // 5 minutes
    private static final long RANDOM_BREAK_INTERVAL_MAX = 900000; // 15 minutes
    private long nextRandomBreak = 0;
    private boolean inRandomBreak = false;
    private long randomBreakEndTime = 0;
    private static final long RANDOM_BREAK_DURATION_MIN = 2000; // 2 seconds
    private static final long RANDOM_BREAK_DURATION_MAX = 8000; // 8 seconds
    
    // Bobber settling detection
    private boolean bobberSettled;
    private long bobberSettleStartTime;
    private long bobberSettledTime = 0; // Time when bobber finished settling
    // settle threshold (lengthSquared) and duration (ms)
    private static final double BOBBER_SETTLE_VELOCITY_THRESHOLD = 0.005; // relaxed threshold
    private static final long BOBBER_SETTLE_DURATION_MS = 500; // longer settle time
    private static final long DETECTION_DELAY_AFTER_SETTLE_MS = 1000; // Wait 1 second after settling before detecting
    
    // Bobber position tracking for detection
    private double lastBobberY = 0;
    private double settledBobberY = 0; // Y position after bobber settles
    private boolean settledYRecorded = false; // Whether we've recorded the settled Y position
    private long lastPositionCheckTime = 0;

    // Transient HUD message (action-bar style rendered above hotbar)
    private String hudMessage = null;
    private long hudMessageExpiry = 0L;
    // Bite detection thresholds
    private static final double BITE_VELOCITY_THRESHOLD = 0.001; // velocity squared threshold for bite
    private static final double BITE_Y_DISPLACEMENT = 0.15; // bobber moves down 0.15 blocks when pulled    // Statistics
    private int totalCatches;
    private int seaCreaturesCaught;
    private int failedCatches;
    private long sessionStartTime;
    private long lastSuccessfulCatch;
    private int maxConsecutiveCatches;

    public enum FishingState {
        IDLE,
        CASTING,
        WAITING,
        REELING,
        COOLDOWN,
        KILLING_CREATURE
    }

    public void setHUDMessage(String msg, long durationMs) {
        if (msg == null || durationMs <= 0) return;
        this.hudMessage = msg;
        this.hudMessageExpiry = System.currentTimeMillis() + durationMs;
        AbyssalFishing.LOGGER.debug("HUD message set: " + msg + " for " + durationMs + "ms");
    }

    public String getActiveHudMessage() {
        if (hudMessage == null) return null;
        if (System.currentTimeMillis() > hudMessageExpiry) {
            hudMessage = null;
            hudMessageExpiry = 0L;
            return null;
        }
        return hudMessage;
    }

    public FishingManager() {
        this.client = MinecraftClient.getInstance();
        this.config = AbyssalFishing.config;
        this.random = ThreadLocalRandom.current();
        this.seaCreatureKiller = new SeaCreatureKiller();
        this.mouseMovement = new MouseMovement();
        this.active = false;
        this.currentState = FishingState.IDLE;
        this.sessionStartTime = System.currentTimeMillis();
        this.lastCastTime = System.currentTimeMillis();
        this.lastCatchTime = System.currentTimeMillis();
        this.nextActionDelay = System.currentTimeMillis();
        this.humanizationFactor = 1.0f;
        
        // Schedule first random break
        scheduleNextRandomBreak();
    }
    
    private void scheduleNextRandomBreak() {
        long delay = RANDOM_BREAK_INTERVAL_MIN + 
            (long)(random.nextDouble() * (RANDOM_BREAK_INTERVAL_MAX - RANDOM_BREAK_INTERVAL_MIN));
        nextRandomBreak = System.currentTimeMillis() + delay;
    }
    
    // Cache for performance optimization
    private long lastBobberUpdate = 0;
    private long lastEmergencyCheck = 0;
    private long lastAFKUpdate = 0;
    private static final long BOBBER_UPDATE_INTERVAL = 50; // Update bobber every 50ms instead of every tick
    private static final long EMERGENCY_CHECK_INTERVAL = 500; // Check emergency conditions every 500ms
    private static final long AFK_UPDATE_INTERVAL = 100; // Update AFK prevention every 100ms
    
    public void update() {
        if (!active) return;
        if (client.player == null || client.world == null) return;
        
        long currentTime = System.currentTimeMillis();
        
        // Handle random breaks (human-like behavior)
        if (inRandomBreak) {
            if (currentTime >= randomBreakEndTime) {
                inRandomBreak = false;
                scheduleNextRandomBreak();
                LOGGER.debug("Random break ended, resuming fishing");
            } else {
                return; // Still in break, don't do anything
            }
        } else if (currentTime >= nextRandomBreak) {
            // Start random break
            inRandomBreak = true;
            long breakDuration = RANDOM_BREAK_DURATION_MIN + 
                (long)(random.nextDouble() * (RANDOM_BREAK_DURATION_MAX - RANDOM_BREAK_DURATION_MIN));
            randomBreakEndTime = currentTime + breakDuration;
            LOGGER.debug("Taking random break for " + breakDuration + "ms");
            return;
        }
        
        // Prevent AFK detection (only every 100ms for performance)
        if (config.preventAFK && currentTime - lastAFKUpdate > AFK_UPDATE_INTERVAL) {
            mouseMovement.update();
            lastAFKUpdate = currentTime;
        }
        
        // Update fishing bobber reference (cached, not every tick)
        if (currentTime - lastBobberUpdate > BOBBER_UPDATE_INTERVAL) {
            updateFishingBobber();
            lastBobberUpdate = currentTime;
        }

        // Main fishing logic
        switch (currentState) {
            case IDLE:
                handleIdle();
                break;
                
            case CASTING:
                handleCasting();
                break;
                
            case WAITING:
                handleWaiting();
                break;
                
            case REELING:
                handleReeling();
                break;
                
            case COOLDOWN:
                handleCooldown();
                break;
                
            case KILLING_CREATURE:
                handleKillingCreature();
                break;
        }
        
        // Check for emergency conditions (only every 500ms for performance)
        if (currentTime - lastEmergencyCheck > EMERGENCY_CHECK_INTERVAL) {
            checkEmergencyConditions();
            lastEmergencyCheck = currentTime;
        }
    }
    
    private void handleIdle() {
        // Wait a moment before starting to cast
        if (System.currentTimeMillis() - lastCastTime > 500) {
            transitionToState(FishingState.CASTING);
        }
    }
    
    // Auto-retry tracking
    private long bobberSpawnTimeout = 0;
    private int bobberRetryCount = 0;
    private static final long BOBBER_SPAWN_TIMEOUT_MS = 2000; // Wait 2 seconds for bobber to appear
    private static final int MAX_BOBBER_RETRIES = 3;
    
    private void handleCasting() {
        if (client.player == null) return;
        
        // Check if player has fishing rod
        if (!hasFishingRod()) {
            LOGGER.warn("No fishing rod in hand! Stopping fishing.");
            stopFishing("No fishing rod");
            return;
        }
        
        // Check if player is over water before casting
        if (!isPlayerOverWater()) {
            LOGGER.debug("Player not over water, waiting...");
            // Wait a bit and try again
            nextActionDelay = System.currentTimeMillis() + 500;
            return;
        }
        
        // Apply humanized delay
        if (System.currentTimeMillis() < nextActionDelay) {
            return;
        }
        
        // Cast the fishing rod
        if (castFishingRod()) {
            lastCastTime = System.currentTimeMillis();
            bobberSpawnTimeout = System.currentTimeMillis() + BOBBER_SPAWN_TIMEOUT_MS; // Set timeout

            // Reset bobber settling state - a new bobber will appear and initially move
            bobberSettled = false;
            bobberSettleStartTime = 0L;
            bobberSettledTime = 0;
            settledYRecorded = false;
            settledBobberY = 0;
            lastBobberY = 0;
            lastReelTime = 0; // Reset reel time
            reelStartTime = 0; // Reset reel start time
            cachedSeaCreature = null; // Clear sea creature cache on new cast

            transitionToState(FishingState.WAITING);

            // Randomized next action delay (more human-like)
            long waitTime = getRandomizedDelay(config.baseWaitTime, 0.25f);
            // Occasionally wait longer (human gets distracted)
            if (random.nextDouble() < 0.08) {
                waitTime = (long)(waitTime * (1.2 + random.nextDouble() * 0.3)); // 20-50% longer
            }
            nextActionDelay = System.currentTimeMillis() + (long)(waitTime * humanizationFactor);
        } else {
            // Failed to cast - increment failed attempts
            failedAttempts++;
            if (failedAttempts > 3) {
                LOGGER.warn("Multiple casting failures, pausing briefly");
                nextActionDelay = System.currentTimeMillis() + 2000; // 2 second pause
                failedAttempts = 0;
            }
        }
    }
    
    private void handleWaiting() {
        // Always wait minimum time before checking for fish
        long timeSinceCast = System.currentTimeMillis() - lastCastTime;
        if (timeSinceCast < 500) {
            // Don't check for fish in first 500ms
            return;
        }
        
        // AUTO-RETRY: Check if bobber appeared after cast
        if (fishingBobber == null && bobberSpawnTimeout > 0) {
            if (System.currentTimeMillis() > bobberSpawnTimeout) {
                // Bobber didn't appear - retry cast
                if (bobberRetryCount < MAX_BOBBER_RETRIES) {
                    bobberRetryCount++;
                    LOGGER.warn("Bobber didn't appear after cast, retrying (" + bobberRetryCount + "/" + MAX_BOBBER_RETRIES + ")");
                    bobberSpawnTimeout = 0; // Reset timeout
                    transitionToState(FishingState.CASTING);
                    nextActionDelay = System.currentTimeMillis() + 500; // Short delay before retry
                    return;
                } else {
                    LOGGER.error("Bobber failed to appear after " + MAX_BOBBER_RETRIES + " retries");
                    bobberRetryCount = 0;
                    bobberSpawnTimeout = 0;
                    failedCatches++;
                    transitionToState(FishingState.REELING); // Try to reel anyway
                    return;
                }
            }
        } else if (fishingBobber != null) {
            // Bobber appeared, reset retry counter
            bobberRetryCount = 0;
            bobberSpawnTimeout = 0;
        }
        
        // Ensure we don't check for bites until the bobber has settled in the water.
        // Update settling state based on current bobber velocity.
        updateBobberSettling();

        if (!bobberSettled) {
            // Still settling; skip bite checks
            return;
        }
        
        // Wait 1 second after settling before starting detection
        if (bobberSettledTime > 0) {
            long timeSinceSettled = System.currentTimeMillis() - bobberSettledTime;
            if (timeSinceSettled < DETECTION_DELAY_AFTER_SETTLE_MS) {
                // Still waiting for detection delay after settling
                return;
            }
        } else {
            // bobberSettledTime not set yet, skip detection
            return;
        }

        // Check if bobber was pulled underwater (fish bite)
        boolean fishBite = checkForFishBite();
        
        // Timeout - if waited too long, reel anyway
        if (timeSinceCast > config.maxWaitTime) {
            LOGGER.debug("Waited " + timeSinceCast + "ms, timeout - reeling");
            failedCatches++; // Count timeout as failed catch
            transitionToState(FishingState.REELING);
            return;
        }
        
        // Fish detected - transition directly to reeling (no delay state)
        if (fishBite) {
            // Human-like reaction: sometimes miss the bite (5% chance)
            if (random.nextDouble() < 0.05) {
                LOGGER.debug("Missed fish bite (human-like error)");
                return; // Don't react this time
            }
            
            // Skip FISH_DETECTED state and go straight to reeling for faster response
            // Add humanization delay with more variance
            long baseReaction = Math.max(50, config.reactionTime / 2);
            long reactionDelay = getRandomizedDelay(baseReaction, 0.3f); // More variance
            nextActionDelay = System.currentTimeMillis() + reactionDelay;
            transitionToState(FishingState.REELING);
        }
    }
    
    private boolean checkForFishBite() {
        if (fishingBobber == null) {
            return false;
        }
        
        // Don't require isTouchingWater() - it's unreliable
        // Instead, just check if bobber is still in the world
        if (fishingBobber.getWorld() == null) {
            return false;
        }
        
        // IMPORTANT: Check if bobber is actually in water before detecting
        if (!isBobberInWater(fishingBobber)) {
            // Bobber not in water yet, don't detect
            return false;
        }

        double velocity = fishingBobber.getVelocity().lengthSquared();
        double currentY = fishingBobber.getY();
        
        // Check hooked entity detection (sea creatures) FIRST - this is most reliable
        Entity hookedEntity = getHookedEntity(fishingBobber);
        if (hookedEntity != null && hookedEntity.isAlive()) {
            LOGGER.debug("Sea creature detected! Entity: " + hookedEntity.getName().getString() + " (velocity: " + String.format("%.6f", velocity) + ")");
            // Store the entity for later killing after reeling
            // For now, just reel it in - we'll check for the entity after reeling
            updatePositionTracking();
            return true;
        }

        // Y-position based detection (bobber pulled down) - PRIMARY METHOD
        // This is more reliable than velocity according to todo.md
        if (bobberSettled && settledYRecorded && settledBobberY > 0) {
            double yDrop = settledBobberY - currentY;
            if (yDrop >= BITE_Y_DISPLACEMENT) {
                LOGGER.debug("Fish bite detected by Y displacement: " + String.format("%.2f", yDrop) + " blocks down (from settled Y=" + String.format("%.2f", settledBobberY) + ")");
                updatePositionTracking();
                return true;
            }
        }

        // Velocity-based detection as fallback (less reliable)
        if (velocity > BITE_VELOCITY_THRESHOLD) {
            LOGGER.debug("Fish bite detected by velocity: " + String.format("%.6f", velocity));
            updatePositionTracking();
            return true;
        }

        // Update position tracking for next frame
        updatePositionTracking();
        
        return false;
    }
    
    private Entity getHookedEntity(FishingBobberEntity bobber) {
        // Try multiple methods to get hooked entity for compatibility
        try {
            // Method 1: Direct method call (if available in mappings)
            java.lang.reflect.Method method = bobber.getClass().getMethod("getHookedEntity");
            Object result = method.invoke(bobber);
            if (result instanceof Entity) {
                return (Entity) result;
            }
        } catch (NoSuchMethodException e) {
            // Try alternative method names
            try {
                java.lang.reflect.Method method = bobber.getClass().getMethod("method_3716"); // Yarn mapping name
                Object result = method.invoke(bobber);
                if (result instanceof Entity) {
                    return (Entity) result;
                }
            } catch (Exception ignored) {
            }
        } catch (Exception e) {
            LOGGER.debug("Error getting hooked entity via reflection: " + e.getMessage());
        }
        
        // Method 2: Check nearby entities that might be hooked
        if (client.world != null && client.player != null) {
            // Check if any entity is very close to the bobber (within 0.5 blocks)
            for (Entity entity : client.world.getEntities()) {
                if (entity != null && entity.isAlive() && !entity.isRemoved()) {
                    double distance = bobber.distanceTo(entity);
                    if (distance < 0.5 && entity != client.player) {
                        // This might be a hooked entity
                        return entity;
                    }
                }
            }
        }
        
        return null;
    }

    private void updatePositionTracking() {
        if (fishingBobber != null) {
            lastBobberY = fishingBobber.getY();
            lastPositionCheckTime = System.currentTimeMillis();
        }
    }

    private void updateBobberSettling() {
        if (fishingBobber == null) {
            // No bobber yet; not settled
            bobberSettled = false;
            bobberSettleStartTime = 0L;
            return;
        }

        double vel = fishingBobber.getVelocity().lengthSquared();

        // Once settled, stay settled - don't reset on velocity spikes (those are fish bites!)
        if (bobberSettled) {
            return;
        }

        if (vel <= BOBBER_SETTLE_VELOCITY_THRESHOLD) {
            if (bobberSettleStartTime == 0L) {
                bobberSettleStartTime = System.currentTimeMillis();
            } else {
                long since = System.currentTimeMillis() - bobberSettleStartTime;
                if (since >= BOBBER_SETTLE_DURATION_MS) {
                    if (!bobberSettled) {
                        // Record the settled Y position when bobber first settles
                        settledBobberY = fishingBobber.getY();
                        settledYRecorded = true;
                        lastBobberY = settledBobberY;
                        bobberSettledTime = System.currentTimeMillis(); // Record when settling finished
                        LOGGER.debug("Bobber settled at Y=" + String.format("%.2f", settledBobberY) + " after " + since + "ms (vel=" + String.format("%.6f", vel) + "). Detection will start in " + DETECTION_DELAY_AFTER_SETTLE_MS + "ms");
                    }
                    bobberSettled = true;
                }
            }
        } else {
            // Bobber still moving; reset settle timer
            bobberSettleStartTime = 0L;
        }
    }
    
    // Reeling timeout tracking
    private long reelStartTime = 0;
    private static final long MAX_REEL_TIME_MS = 3000; // Max 3 seconds of reeling
    
    private void handleReeling() {
        // Track when we started reeling
        if (reelStartTime == 0) {
            reelStartTime = System.currentTimeMillis();
            // Click ONCE when entering reeling state
            reelIn();
            lastReelTime = System.currentTimeMillis();
        }
        
        // Safety timeout - if reeling too long, something went wrong
        long reelDuration = System.currentTimeMillis() - reelStartTime;
        if (reelDuration > MAX_REEL_TIME_MS) {
            LOGGER.warn("Reeling timeout - bobber stuck or no fish. Resetting.");
            reelStartTime = 0;
            lastReelTime = 0;
            updateFishingBobber();
            // Force remove bobber reference
            if (fishingBobber != null) {
                fishingBobber = null;
            }
            failedCatches++;
            transitionToState(FishingState.COOLDOWN);
            return;
        }
        
        // Wait for bobber to be removed (reeled in) - just wait, don't spam clicks
        updateFishingBobber();
        if (fishingBobber != null) {
            // Bobber still exists, wait for it to disappear (already clicked once)
            return;
        }
        
        // Reset reel tracking
        reelStartTime = 0;
        
        // Bobber is gone, reel was successful
        long catchTime = System.currentTimeMillis();
        totalCatches++;
        lastCatchTime = catchTime;
        lastSuccessfulCatch = catchTime;
        consecutiveCatches++;
        if (consecutiveCatches > maxConsecutiveCatches) {
            maxConsecutiveCatches = consecutiveCatches;
        }
        failedAttempts = 0;
        lastReelTime = 0; // Reset for next time
        
        // After reeling, check if we caught a sea creature that needs to be killed
        if (config.killSeaCreatures) {
            // Wait a brief moment for entity to spawn after reel (check immediately first time)
            // Then check for nearby sea creatures that were just caught
            Entity nearbyCreature = findNearbySeaCreature();
            if (nearbyCreature != null && seaCreatureKiller.shouldKill()) {
                seaCreatureKiller.startKilling(nearbyCreature);
                transitionToState(FishingState.KILLING_CREATURE);
                return;
            }
            // If no creature found immediately, wait a bit and check again
            if (System.currentTimeMillis() - catchTime < 300) {
                return; // Wait a bit more for entity to spawn
            }
            // Final check
            nearbyCreature = findNearbySeaCreature();
            if (nearbyCreature != null && seaCreatureKiller.shouldKill()) {
                seaCreatureKiller.startKilling(nearbyCreature);
                transitionToState(FishingState.KILLING_CREATURE);
                return;
            }
        }
        
        // Cooldown before next cast (with human-like variation)
        // Sometimes take longer breaks (10% chance)
        long baseCooldown = config.baseCooldown;
        if (random.nextDouble() < 0.1) {
            // Take a longer break occasionally
            baseCooldown = (long)(baseCooldown * 1.5);
        }
        long cooldown = getRandomizedDelay(baseCooldown, 0.25f); // More variance
        nextActionDelay = System.currentTimeMillis() + cooldown;
        
        transitionToState(FishingState.COOLDOWN);
    }
    
    // Cache for sea creature search
    private Entity cachedSeaCreature = null;
    private long lastSeaCreatureSearch = 0;
    private static final long SEA_CREATURE_SEARCH_INTERVAL = 200; // Search every 200ms
    
    private Entity findNearbySeaCreature() {
        if (client.player == null || client.world == null) return null;
        
        // Use cached result if recent
        long currentTime = System.currentTimeMillis();
        if (cachedSeaCreature != null && currentTime - lastSeaCreatureSearch < SEA_CREATURE_SEARCH_INTERVAL) {
            // Validate cached entity is still valid
            if (cachedSeaCreature.isAlive() && !cachedSeaCreature.isRemoved() && 
                client.player.distanceTo(cachedSeaCreature) <= 5.0) {
                return cachedSeaCreature;
            } else {
                cachedSeaCreature = null; // Invalid, clear cache
            }
        }
        
        // Use Box query for better performance instead of iterating all entities
        net.minecraft.util.math.Box searchBox = new net.minecraft.util.math.Box(
            client.player.getX() - 5, client.player.getY() - 2, client.player.getZ() - 5,
            client.player.getX() + 5, client.player.getY() + 2, client.player.getZ() + 5
        );
        
        List<Entity> nearbyEntities = client.world.getEntitiesByClass(
            Entity.class, searchBox, entity -> 
                entity != null && entity.isAlive() && !entity.isRemoved() && 
                entity != client.player && isSeaCreatureType(entity)
        );
        
        if (!nearbyEntities.isEmpty()) {
            cachedSeaCreature = nearbyEntities.get(0);
            lastSeaCreatureSearch = currentTime;
            return cachedSeaCreature;
        }
        
        cachedSeaCreature = null;
        lastSeaCreatureSearch = currentTime;
        return null;
    }
    
    private boolean isSeaCreatureType(Entity entity) {
        if (entity == null || !entity.isAlive() || entity.isRemoved()) {
            return false;
        }
        
        // CRITICAL: Never attack player pets - this causes bans!
        if (isPlayerPet(entity)) {
            return false;
        }
        
        // First check: Is it a water creature type?
        if (entity instanceof net.minecraft.entity.mob.WaterCreatureEntity) {
            // But exclude regular squid if disabled
            if (entity instanceof net.minecraft.entity.passive.SquidEntity && !config.killSquid) {
                return false;
            }
            return true;
        }
        
        // Second check: Parse entity name (remove color codes for Hypixel compatibility)
        String rawName = entity.getName().getString();
        String cleanName = stripColorCodes(rawName).toLowerCase();
        
        // Hypixel-specific sea creature patterns
        // Common sea creatures on Hypixel SkyBlock
        return cleanName.contains("guardian") ||
               cleanName.contains("squid") ||
               cleanName.contains("hydra") ||
               cleanName.contains("shark") ||
               cleanName.contains("sea walker") ||
               cleanName.contains("sea guardian") ||
               cleanName.contains("sea witch") ||
               cleanName.contains("sea archer") ||
               cleanName.contains("sea leech") ||
               cleanName.contains("frozen steve") ||
               cleanName.contains("night squid") ||
               cleanName.contains("water hydra") ||
               cleanName.contains("deep sea") ||
               cleanName.contains("frost walker") ||
               cleanName.contains("yeti") ||
               cleanName.contains("reindrake") ||
               cleanName.contains("emperor") ||
               cleanName.contains("lord jawbus") ||
               cleanName.contains("great white") ||
               cleanName.contains("nurse shark") ||
               cleanName.contains("blue shark") ||
               cleanName.contains("tiger shark") ||
               cleanName.contains("revenant horror") ||
               cleanName.contains("terror") ||
               cleanName.contains("phantom fisher") ||
               cleanName.contains("grim reaper") ||
               cleanName.contains("sea") ||
               cleanName.contains("water") ||
               cleanName.contains("deep") ||
               cleanName.contains("abyss") ||
               cleanName.contains("fisher") ||
               cleanName.contains("meg") ||
               // Generic patterns (less specific, check last)
               (cleanName.contains("fish") && !cleanName.contains("fishing"));
    }
    
    /**
     * Checks if entity is a player pet (to avoid attacking pets and getting banned)
     * Uses multiple detection methods for maximum safety
     */
    private boolean isPlayerPet(Entity entity) {
        if (entity == null || client.player == null) return false;
        
        // Method 1: Check entity name for pet indicators
        String rawName = entity.getName().getString();
        String cleanName = stripColorCodes(rawName).toLowerCase();
        
        // Hypixel pet naming patterns
        if (cleanName.contains("pet") || 
            cleanName.contains("[lvl") ||
            cleanName.contains("level") ||
            cleanName.matches(".*\\[lvl\\s*\\d+\\].*")) {
            return true;
        }
        
        // Method 2: Check if entity is tamed (oswojone)
        if (entity instanceof net.minecraft.entity.passive.TameableEntity tameable) {
            if (tameable.isTamed()) {
                // If tamed, assume it's a pet (safer to skip all tamed entities)
                return true;
            }
        }
        
        // Method 3: Check NBT data for pet tags (Hypixel-specific)
        try {
            net.minecraft.nbt.NbtCompound nbt = entity.writeNbt(new net.minecraft.nbt.NbtCompound());
            if (nbt != null) {
                // Check for pet-related tags
                if (nbt.contains("Pet") || 
                    nbt.contains("pet") ||
                    nbt.contains("OwnerUUID") ||
                    nbt.contains("ownerUUID") ||
                    nbt.contains("OwnerName") ||
                    nbt.contains("ownerName")) {
                    return true;
                }
                
                // If OwnerUUID tag exists, assume it's a pet
                // (UUID reading method not available in this version, but tag presence is enough)
                if (nbt.contains("OwnerUUID") || nbt.contains("ownerUUID")) {
                    return true;
                }
            }
        } catch (Exception e) {
            // NBT check failed, continue with other checks
        }
        
        // Method 4: Check if entity is following player (pet behavior)
        // Skip this check as getTarget() method is not available in this version
        // Rely on other detection methods instead
        
        // Method 5: Check for common pet entity types that shouldn't be attacked
        if (entity instanceof net.minecraft.entity.passive.WolfEntity ||
            entity instanceof net.minecraft.entity.passive.CatEntity ||
            entity instanceof net.minecraft.entity.passive.ParrotEntity ||
            entity instanceof net.minecraft.entity.passive.HorseEntity ||
            entity instanceof net.minecraft.entity.passive.LlamaEntity ||
            entity instanceof net.minecraft.entity.passive.FoxEntity) {
            // These are common pet types - be extra careful
            double distance = client.player.distanceTo(entity);
            if (distance < 15.0) {
                return true; // Safe to assume it's a pet if close
            }
        }
        
        return false;
    }
    
    /**
     * Checks if player is positioned over water (for better casting)
     * Prevents casting when not over water
     */
    private boolean isPlayerOverWater() {
        if (client.player == null || client.world == null) return false;
        
        BlockPos playerPos = client.player.getBlockPos();
        
        // Check blocks below player (up to 3 blocks down)
        for (int y = 0; y <= 3; y++) {
            BlockPos checkPos = playerPos.down(y);
            if (HypixelUtils.isWaterBlock(client.world, checkPos)) {
                return true;
            }
        }
        
        // Also check blocks around player (in case player is at edge of water)
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos checkPos = playerPos.add(x, -1, z);
                if (HypixelUtils.isWaterBlock(client.world, checkPos)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Checks if bobber is actually in water
     * This prevents false detections when bobber is in air or on land
     */
    private boolean isBobberInWater(FishingBobberEntity bobber) {
        if (bobber == null || client.world == null) return false;
        
        // Check if bobber is touching water
        if (bobber.isTouchingWater()) {
            return true;
        }
        
        // Additional check: verify bobber position is in water block
        BlockPos bobberPos = bobber.getBlockPos();
        BlockPos posBelow = bobberPos.down();
        
        // Check if bobber is in water block or block below is water
        if (HypixelUtils.isWaterBlock(client.world, bobberPos) || 
            HypixelUtils.isWaterBlock(client.world, posBelow)) {
            return true;
        }
        
        // Check surrounding blocks for water
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos checkPos = bobberPos.add(x, 0, z);
                if (HypixelUtils.isWaterBlock(client.world, checkPos)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Removes Minecraft color codes (§) and formatting from text
     * This is important for Hypixel where entity names have color formatting
     */
    private String stripColorCodes(String text) {
        if (text == null) return "";
        // Remove all § codes (color codes and formatting)
        return text.replaceAll("§[0-9a-fk-or]", "");
    }
    
    private void handleCooldown() {
        if (System.currentTimeMillis() >= nextActionDelay) {
            transitionToState(FishingState.CASTING);
        }
    }
    
    private void handleKillingCreature() {
        // Always update sea creature killer (handles returning to position too)
        seaCreatureKiller.update();
        
        if (seaCreatureKiller.isKilling()) {
            // Still killing, continue
            return;
        }
        
        // Check if returning to position
        if (seaCreatureKiller.isReturningToPosition()) {
            // Wait for return to complete
            return;
        }
        
        // Killing finished and returned to position, continue fishing
        seaCreaturesCaught++;
        transitionToState(FishingState.COOLDOWN);
    }
    
    private boolean castFishingRod() {
        if (client.player == null || client.interactionManager == null) return false;
        
        // Use fishing rod cast
        client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
        
        if (config.playSounds) {
            // Play casting sound via SoundUtils
            SoundUtils.playSound(createSoundEvent("entity.bobber.throw"), 0.5f, 1.0f);
        }
        
        return true;
    }
    
    private boolean reelIn() {
        if (client.player == null || client.interactionManager == null) return false;
        
        client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
        
        if (config.playSounds) {
            SoundUtils.playSound(createSoundEvent("entity.fishing_player.reel_in"), 0.8f, 1.0f);
        }
        
        return true;
    }
    
    private boolean hasFishingRod() {
        if (client.player == null) return false;
        
        ItemStack mainHand = client.player.getMainHandStack();
        return mainHand.getItem() instanceof FishingRodItem;
    }
    
    private boolean isVisualFishDetected() {
        if (fishingBobber == null) {
            return false;
        }
        
        // Check if bobber is in water (touching water block)
        if (!fishingBobber.isTouchingWater()) {
            return false;
        }
        
        // Check if fish has taken the bobber (indicated by velocity or particle effects)
        // In Minecraft, when a fish bites, the bobber moves and gets pulled
        // We check if the bobber has moved downward (indicating fish took it)
        double bobberMotionY = fishingBobber.getVelocity().y;
        
        // Fish bite causes downward motion or splashing
        // Detect if bobber is moving (not stationary in water)
        return Math.abs(fishingBobber.getVelocity().lengthSquared()) > 0.01;
    }
    
    private boolean shouldStartFishing() {
        if (!config.autoStart) return false;
        return isNearWater();
    }
    
    private boolean isNearWater() {
        if (client.player == null) return false;
        
        BlockPos playerPos = client.player.getBlockPos();
        World world = client.world;
        
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                for (int y = -2; y <= 2; y++) {
                    BlockPos checkPos = playerPos.add(x, y, z);
                    if (HypixelUtils.isWaterBlock(world, checkPos)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    private void updateFishingBobber() {
        if (client.player != null) {
            fishingBobber = client.player.fishHook;
        }
    }
    
    private void checkEmergencyConditions() {
        if (client.player == null) return;
        
        if (client.player.getHealth() < config.emergencyHealthThreshold) {
            stopFishing("Low health");
            return;
        }
        
        if (PlayerUtils.hasPlayerMoved() && config.pauseOnMovement) {
            // Player moved - pause fishing
            return;
        }
        
        if (PlayerUtils.hasPlayerRotated() && config.pauseOnRotation) {
            // Player rotated - pause fishing
            return;
        }
    }
    
    private void recalibrateTiming() {
        humanizationFactor = Math.max(0.8f, humanizationFactor - 0.1f);
        failedAttempts = 0;
    }
    
    private long getRandomizedDelay(long baseDelay, float variance) {
        // Use Gaussian distribution for more natural timing (centered around baseDelay)
        double gaussian = random.nextGaussian();
        // Clamp to reasonable range (±variance)
        gaussian = Math.max(-variance, Math.min(variance, gaussian * (variance / 2.0)));
        float randomFactor = 1.0f + (float)gaussian;
        return (long) (baseDelay * randomFactor);
    }
    
    private void transitionToState(FishingState newState) {
        currentState = newState;
        AbyssalFishing.LOGGER.debug("Fishing state transition: " + currentState);
    }
    
    public void toggleFishing() {
        if (active) {
            stopFishing("User disabled fishing");
        } else {
            startFishing();
        }
    }
    
    public void startFishing() {
        // Check prerequisites
        if (client.player == null) {
            LOGGER.warn("Cannot start fishing: player is null");
            return;
        }
        
        if (!hasFishingRod()) {
            setHUDMessage("§c[AbyssalFishing] §fNo fishing rod in hand!", config.hudMessageDuration);
            LOGGER.warn("Cannot start fishing: no fishing rod");
            return;
        }
        
        active = true;
        currentState = FishingState.IDLE;
        sessionStartTime = System.currentTimeMillis();
        failedAttempts = 0;
        failedCatches = 0;
        maxConsecutiveCatches = 0;
        nextActionDelay = System.currentTimeMillis();
        AbyssalFishing.LOGGER.info("Fishing started");
        setHUDMessage("§6[AbyssalFishing] §fFishing started", config.hudMessageDuration);
    }
    
    public void stopFishing(String reason) {
        active = false;
        currentState = FishingState.IDLE;
        fishingBobber = null;
        AbyssalFishing.LOGGER.info("Fishing stopped: " + reason);
        setHUDMessage("§6[AbyssalFishing] §fFishing stopped: " + reason, config.hudMessageDuration);
    }
    
    
    
    // Getters
    public boolean isActive() { return active; }
    public FishingState getCurrentState() { return currentState; }
    public int getTotalCatches() { return totalCatches; }
    public int getSeaCreaturesCaught() { return seaCreaturesCaught; }
    public int getFailedCatches() { return failedCatches; }
    public int getMaxConsecutiveCatches() { return maxConsecutiveCatches; }
    public long getSessionTime() { return System.currentTimeMillis() - sessionStartTime; }
    public int getConsecutiveCatches() { return consecutiveCatches; }
    public FishingBobberEntity getFishingBobber() { return fishingBobber; }
    public boolean isBobberSettled() { return bobberSettled; }
    public double getBobberVelocity() { return fishingBobber != null ? fishingBobber.getVelocity().lengthSquared() : 0.0; }
    public double getSuccessRate() {
        int total = totalCatches + failedCatches;
        return total > 0 ? (double) totalCatches / total * 100.0 : 0.0;
    }
    public long getTimeSinceLastCatch() {
        return lastSuccessfulCatch > 0 ? System.currentTimeMillis() - lastSuccessfulCatch : 0;
    }
    
    public SeaCreatureKiller getSeaCreatureKiller() {
        return seaCreatureKiller;
    }
    
    private SoundEvent createSoundEvent(String soundId) {
        // Placeholder - return null for now, SoundUtils will handle the actual sound
        return null;
    }
}