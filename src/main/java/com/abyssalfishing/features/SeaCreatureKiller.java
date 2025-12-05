package com.abyssalfishing.features;

import com.abyssalfishing.AbyssalFishing;
import com.abyssalfishing.config.AbyssalConfig;
import com.abyssalfishing.utils.InventoryUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.WaterCreatureEntity;
import net.minecraft.entity.passive.SquidEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.stream.Collectors;

public class SeaCreatureKiller {
    private final MinecraftClient client;
    private final AbyssalConfig config;
    
    private boolean isKilling;
    private Entity targetEntity;
    private long lastAttackTime;
    private int attackCount;
    private float lastKnownHealth;
    private long lastHealthCheck;
    private boolean isMovingToTarget;
    private long lastMovementUpdate;
    private long lastRotationUpdate;
    
    // Position saving for return after killing
    private Vec3d savedPosition;
    private float savedYaw;
    private float savedPitch;
    private boolean hasSavedPosition;
    
    private static final double ATTACK_RANGE = 4.0;
    private static final double PATHFINDING_RANGE = 8.0; // Start pathfinding if further than this
    private static final double MIN_DISTANCE = 3.0; // Stop moving when this close
    private static final long ATTACK_COOLDOWN_BASE = 400; // Base attack cooldown (ms)
    private static final long ATTACK_COOLDOWN_VARIANCE = 150; // Random variance for natural timing
    private static final long HEALTH_CHECK_INTERVAL = 200; // Check health every 200ms
    private static final long MOVEMENT_UPDATE_INTERVAL = 50; // Update movement every 50ms
    private static final long ROTATION_UPDATE_INTERVAL = 20; // Update rotation every 20ms for smooth aiming
    
    // Humanization variables
    private float currentRotationSpeed = 0.2f; // Dynamic rotation speed (varies)
    private long lastRotationSpeedChange = 0;
    private static final long ROTATION_SPEED_CHANGE_INTERVAL = 1000; // Change rotation speed every 1s
    private long nextAttackTime = 0; // Calculated attack time with variance
    private boolean isSprinting = false; // Natural sprinting
    private long lastSprintToggle = 0;
    private static final long SPRINT_TOGGLE_INTERVAL = 2000; // Toggle sprint every 2s
    
    public SeaCreatureKiller() {
        this.client = MinecraftClient.getInstance();
        this.config = AbyssalFishing.config;
        this.isKilling = false;
        this.attackCount = 0;
    }
    
    public void update() {
        if (client.player == null) return;
        
        // If returning to position (after killing), handle that first
        if (hasSavedPosition && savedPosition != null && !isKilling) {
            returnToSavedPosition();
            return;
        }
        
        if (!isKilling || targetEntity == null) {
            return;
        }
        
        // Check if target is still valid
        if (!targetEntity.isAlive() || targetEntity.isRemoved()) {
            AbyssalFishing.LOGGER.info("Sea creature killed or removed");
            stopKilling();
            return;
        }
        
        double distance = client.player.distanceTo(targetEntity);
        
        // Check if target is too far away
        if (distance > ATTACK_RANGE * 3) {
            AbyssalFishing.LOGGER.warn("Sea creature too far away: " + String.format("%.2f", distance));
            stopKilling();
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        
        // Update rotation speed occasionally for natural variation
        if (currentTime - lastRotationSpeedChange > ROTATION_SPEED_CHANGE_INTERVAL) {
            // Vary rotation speed between 0.15 and 0.3 for natural feel
            currentRotationSpeed = 0.15f + (float)(Math.random() * 0.15f);
            lastRotationSpeedChange = currentTime;
        }
        
        // AUTO-ROTATE: Always rotate towards target (smooth and natural)
        if (currentTime - lastRotationUpdate > ROTATION_UPDATE_INTERVAL) {
            rotateTowardsTarget();
            lastRotationUpdate = currentTime;
        }
        
        // Natural sprinting (toggle occasionally)
        if (currentTime - lastSprintToggle > SPRINT_TOGGLE_INTERVAL) {
            if (distance > ATTACK_RANGE && Math.random() > 0.3) { // 70% chance to sprint when far
                isSprinting = !isSprinting;
                if (client.player != null) {
                    client.player.setSprinting(isSprinting);
                }
            }
            lastSprintToggle = currentTime;
        }
        
        // Pathfinding: Move towards target if too far (with natural pauses)
        if (distance > ATTACK_RANGE && distance <= PATHFINDING_RANGE) {
            // Occasionally pause movement for natural feel (5% chance)
            if (Math.random() > 0.05) {
                if (currentTime - lastMovementUpdate > MOVEMENT_UPDATE_INTERVAL) {
                    moveTowardsTarget();
                    lastMovementUpdate = currentTime;
                }
            }
        } else if (distance <= MIN_DISTANCE) {
            // Stop moving if too close
            stopMovement();
            if (client.player != null) {
                client.player.setSprinting(false);
            }
        }
        
        // Track health to detect if creature is dying
        if (currentTime - lastHealthCheck > HEALTH_CHECK_INTERVAL) {
            try {
                float currentHealth = getEntityHealth(targetEntity);
                if (lastKnownHealth > 0 && currentHealth >= lastKnownHealth && attackCount > 5) {
                    // Health not decreasing after multiple attacks - might need different strategy
                    AbyssalFishing.LOGGER.debug("Health not decreasing, trying different attack");
                }
                lastKnownHealth = currentHealth;
                lastHealthCheck = currentTime;
            } catch (Exception e) {
                // Health check failed, continue anyway
            }
        }
        
        // Attack the creature (with natural timing variance)
        if (distance <= ATTACK_RANGE || isMovingToTarget) {
            if (nextAttackTime == 0 || currentTime >= nextAttackTime) {
                attackCreature();
                // Calculate next attack time with variance
                long variance = (long)(Math.random() * ATTACK_COOLDOWN_VARIANCE * 2) - ATTACK_COOLDOWN_VARIANCE;
                nextAttackTime = currentTime + ATTACK_COOLDOWN_BASE + variance;
            }
        }
    }
    
    /**
     * Rotates player to look at target (smooth and natural)
     * Called frequently for smooth rotation with natural variation
     */
    private void rotateTowardsTarget() {
        if (targetEntity == null || client.player == null) return;
        
        Vec3d playerPos = client.player.getPos();
        Vec3d targetPos = targetEntity.getPos();
        
        // Calculate direction to target
        double dx = targetPos.x - playerPos.x;
        double dy = targetPos.y - playerPos.y;
        double dz = targetPos.z - playerPos.z;
        
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        
        // Calculate yaw and pitch to look at target
        float targetYaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0f;
        float targetPitch = (float) (-Math.atan2(dy, horizontalDistance) * 180.0 / Math.PI);
        
        // Add small random offset for natural aiming (not perfect every time)
        double yawOffset = (Math.random() - 0.5) * 2.0; // ±1 degree
        double pitchOffset = (Math.random() - 0.5) * 1.5; // ±0.75 degree
        targetYaw += (float)yawOffset;
        targetPitch += (float)pitchOffset;
        
        // Smooth rotation
        float currentYaw = client.player.getYaw();
        float currentPitch = client.player.getPitch();
        
        // Normalize angles
        while (targetYaw - currentYaw > 180) targetYaw -= 360;
        while (targetYaw - currentYaw < -180) targetYaw += 360;
        
        // Smooth interpolation with dynamic speed (more natural)
        float newYaw = currentYaw + (targetYaw - currentYaw) * currentRotationSpeed;
        float newPitch = currentPitch + (targetPitch - currentPitch) * currentRotationSpeed;
        
        // Clamp pitch
        newPitch = Math.max(-90, Math.min(90, newPitch));
        
        client.player.setYaw(newYaw);
        client.player.setPitch(newPitch);
    }
    
    private void moveTowardsTarget() {
        if (targetEntity == null || client.player == null) return;
        
        Vec3d playerPos = client.player.getPos();
        Vec3d targetPos = targetEntity.getPos();
        
        // Calculate direction to target
        double dx = targetPos.x - playerPos.x;
        double dy = targetPos.y - playerPos.y;
        double dz = targetPos.z - playerPos.z;
        
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        
        // Pathfinding: Apply velocity to move towards target (natural movement)
        if (distance > MIN_DISTANCE) {
            // Calculate movement direction (horizontal only, don't move vertically)
            Vec3d direction = new Vec3d(dx, 0, dz).normalize();
            
            // Variable speed for natural movement (slightly faster when sprinting)
            double baseSpeed = isSprinting ? 0.18 : 0.15;
            // Add small random variation
            double speedVariation = (Math.random() - 0.5) * 0.03;
            double speed = baseSpeed + speedVariation;
            
            Vec3d velocity = direction.multiply(speed);
            
            // Only apply if player is on ground or in water (to avoid falling)
            if (client.player.isOnGround() || client.player.isTouchingWater()) {
                Vec3d currentVelocity = client.player.getVelocity();
                // Add horizontal movement while preserving vertical velocity
                // Use smaller multiplier for smoother, more natural movement
                double movementMultiplier = 0.08 + (Math.random() * 0.04); // 0.08-0.12 for variation
                client.player.setVelocity(
                    currentVelocity.x + velocity.x * movementMultiplier,
                    currentVelocity.y, // Keep vertical velocity
                    currentVelocity.z + velocity.z * movementMultiplier
                );
            }
            
            isMovingToTarget = true;
        } else {
            stopMovement();
        }
    }
    
    private void stopMovement() {
        isMovingToTarget = false;
    }
    
    public boolean shouldKill() {
        if (!config.killSeaCreatures || client.player == null || client.world == null) {
            return false;
        }
        
        // Find nearby sea creatures - convert Iterable to List manually
        List<Entity> nearbyEntities = new java.util.ArrayList<>();
        for (Entity entity : client.world.getEntities()) {
            if (isSeaCreature(entity) && entity.isAlive() && !entity.isRemoved() && 
                client.player.distanceTo(entity) <= ATTACK_RANGE) {
                nearbyEntities.add(entity);
            }
        }
        
        if (!nearbyEntities.isEmpty()) {
            targetEntity = nearbyEntities.get(0); // Target closest creature
            return true;
        }
        
        return false;
    }
    
    public void startKilling(Entity entity) {
        if (!config.killSeaCreatures) return;
        if (entity == null || !entity.isAlive()) return;
        
        // SAVE POSITION before starting to kill (for return after killing)
        if (client.player != null && !hasSavedPosition) {
            savedPosition = client.player.getPos();
            savedYaw = client.player.getYaw();
            savedPitch = client.player.getPitch();
            hasSavedPosition = true;
            AbyssalFishing.LOGGER.debug("Saved fishing position: " + String.format("%.2f, %.2f, %.2f", savedPosition.x, savedPosition.y, savedPosition.z));
        }
        
        this.targetEntity = entity;
        this.isKilling = true;
        this.attackCount = 0;
        this.lastAttackTime = 0;
        this.nextAttackTime = 0; // Reset attack timing
        this.lastKnownHealth = getEntityHealth(entity);
        this.lastHealthCheck = System.currentTimeMillis();
        
        // Initialize rotation speed
        this.currentRotationSpeed = 0.15f + (float)(Math.random() * 0.15f);
        this.lastRotationSpeedChange = System.currentTimeMillis();
        
        // Initialize sprint state
        this.isSprinting = false;
        this.lastSprintToggle = System.currentTimeMillis();
        
        AbyssalFishing.LOGGER.info("Started killing sea creature: " + stripColorCodes(entity.getName().getString()) + " (HP: " + String.format("%.1f", lastKnownHealth) + ")");
    }
    
    public void stopKilling() {
        this.isKilling = false;
        this.targetEntity = null;
        this.attackCount = 0;
        this.lastKnownHealth = 0;
        this.isMovingToTarget = false;
        this.nextAttackTime = 0;
        
        // Stop sprinting
        if (client.player != null) {
            client.player.setSprinting(false);
        }
        
        // RETURN TO SAVED POSITION after killing
        if (hasSavedPosition && savedPosition != null && client.player != null) {
            returnToSavedPosition();
        }
        
        AbyssalFishing.LOGGER.debug("Stopped killing sea creature");
    }
    
    private long lastReturnUpdate = 0;
    private static final long RETURN_UPDATE_INTERVAL = 50; // Update return movement every 50ms
    
    /**
     * Returns player to saved fishing position after killing sea creature
     * Called repeatedly until player returns to position (natural movement)
     */
    private void returnToSavedPosition() {
        if (client.player == null || savedPosition == null) return;
        
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastReturnUpdate < RETURN_UPDATE_INTERVAL) {
            return;
        }
        lastReturnUpdate = currentTime;
        
        Vec3d currentPos = client.player.getPos();
        double distance = currentPos.distanceTo(savedPosition);
        
        // Check if close enough to saved position
        if (distance < 1.5) {
            // Close enough, reset and continue fishing
            hasSavedPosition = false;
            savedPosition = null;
            if (client.player != null) {
                client.player.setSprinting(false);
            }
            AbyssalFishing.LOGGER.info("Returned to fishing position");
            return;
        }
        
        // Only return if moved significantly (more than 2 blocks)
        if (distance > 2.0) {
            // Calculate direction to saved position
            double dx = savedPosition.x - currentPos.x;
            double dz = savedPosition.z - currentPos.z;
            double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
            
            // Rotate towards saved position (smooth and natural)
            float targetYaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0f;
            float currentYaw = client.player.getYaw();
            
            // Normalize angles
            while (targetYaw - currentYaw > 180) targetYaw -= 360;
            while (targetYaw - currentYaw < -180) targetYaw += 360;
            
            // Smooth rotation with variable speed
            float rotationSpeed = 0.2f + (float)(Math.random() * 0.1f); // 0.2-0.3
            float newYaw = currentYaw + (targetYaw - currentYaw) * rotationSpeed;
            client.player.setYaw(newYaw);
            
            // Smooth pitch return
            float pitchDiff = savedPitch - client.player.getPitch();
            float newPitch = client.player.getPitch() + pitchDiff * 0.2f;
            client.player.setPitch(newPitch);
            
            // Move towards saved position using velocity (natural movement)
            if (client.player.isOnGround() || client.player.isTouchingWater()) {
                Vec3d direction = new Vec3d(dx, 0, dz).normalize();
                
                // Variable speed for natural feel
                double speed = 0.15 + (Math.random() * 0.05); // 0.15-0.2
                Vec3d velocity = direction.multiply(speed);
                
                Vec3d currentVelocity = client.player.getVelocity();
                double movementMultiplier = 0.08 + (Math.random() * 0.04); // 0.08-0.12
                client.player.setVelocity(
                    currentVelocity.x + velocity.x * movementMultiplier,
                    currentVelocity.y,
                    currentVelocity.z + velocity.z * movementMultiplier
                );
            }
        } else {
            // Already close, just reset
            hasSavedPosition = false;
            savedPosition = null;
            if (client.player != null) {
                client.player.setSprinting(false);
            }
        }
    }
    
    public boolean isReturningToPosition() {
        return hasSavedPosition && savedPosition != null;
    }
    
    private void attackCreature() {
        if (client.player == null || targetEntity == null) return;
        
        // Small random delay before attack (human reaction time simulation)
        if (Math.random() > 0.7) { // 30% chance for small delay
            try {
                Thread.sleep((long)(Math.random() * 30)); // 0-30ms delay
            } catch (InterruptedException e) {
                // Ignore
            }
        }
        
        ItemStack mainHand = client.player.getMainHandStack();
        
        // Determine attack method
        if (isMageWeapon(mainHand)) {
            useMageAbility();
        } else if (isWeapon(mainHand)) {
            useMeleeAttack();
        } else {
            // Fallback to basic attack
            useBasicAttack();
        }
        
        lastAttackTime = System.currentTimeMillis();
        attackCount++;
        
        if (config.playSounds) {
            // Play attack sound
        }
    }
    
    private boolean isWeapon(ItemStack stack) {
        if (stack.isEmpty()) return false;
        
        String itemName = stack.getItem().toString().toLowerCase();
        return itemName.contains("sword") ||
               itemName.contains("axe") ||
               itemName.contains("pickaxe") ||
               itemName.contains("shovel") ||
               itemName.contains("hoe") ||
               itemName.contains("weapon") ||
               itemName.contains("staff") ||
               itemName.contains("mace") ||
               itemName.contains("club") ||
               itemName.contains("hammer");
    }
    
    private void useMageAbility() {
        if (client.player == null || client.interactionManager == null) return;
        
        // Right-click to activate mage ability
        client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
        
        AbyssalFishing.LOGGER.debug("Used mage ability on sea creature");
    }
    
    private void useMeleeAttack() {
        if (client.player == null || client.interactionManager == null || targetEntity == null) return;
        
        // Attack the entity
        client.interactionManager.attackEntity(client.player, targetEntity);
        
        AbyssalFishing.LOGGER.debug("Used melee attack on sea creature");
    }
    
    private void useBasicAttack() {
        // Fallback attack method
        useMeleeAttack();
    }
    
    private boolean isSeaCreature(Entity entity) {
        if (entity == null || !entity.isAlive() || entity.isRemoved()) {
            return false;
        }
        
        // CRITICAL: Never attack player pets - this causes bans!
        if (isPlayerPet(entity)) {
            return false;
        }
        
        // Check if entity is a sea creature (but not squid if disabled)
        if (entity instanceof SquidEntity && !config.killSquid) {
            return false;
        }
        
        // Check if it's a water creature or has specific naming patterns
        if (entity instanceof WaterCreatureEntity) {
            return true;
        }
        
        // Parse entity name (remove color codes for Hypixel compatibility)
        String rawName = entity.getName().getString();
        String cleanName = stripColorCodes(rawName).toLowerCase();
        
        // Hypixel-specific sea creature patterns
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
            AbyssalFishing.LOGGER.debug("Detected pet by name: " + rawName);
            return true;
        }
        
        // Method 2: Check if entity is tamed (oswojone)
        if (entity instanceof net.minecraft.entity.passive.TameableEntity tameable) {
            if (tameable.isTamed()) {
                // If tamed, assume it's a pet (safer to skip all tamed entities)
                AbyssalFishing.LOGGER.debug("Detected tamed entity (skipping): " + rawName);
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
     * Removes Minecraft color codes (§) and formatting from text
     * This is important for Hypixel where entity names have color formatting
     */
    private String stripColorCodes(String text) {
        if (text == null) return "";
        // Remove all § codes (color codes and formatting)
        return text.replaceAll("§[0-9a-fk-or]", "");
    }
    
    private float getEntityHealth(Entity entity) {
        if (entity instanceof LivingEntity livingEntity) {
            return livingEntity.getHealth();
        }
        return 0;
    }

    private float getEntityMaxHealth(Entity entity) {
        if (entity instanceof LivingEntity livingEntity) {
            return livingEntity.getMaxHealth();
        }
        return 0;
    }

    public float getTargetHealth() {
        return targetEntity != null && targetEntity.isAlive() ? getEntityHealth(targetEntity) : 0;
    }
    
    public float getTargetMaxHealth() {
        return targetEntity != null && targetEntity.isAlive() ? getEntityMaxHealth(targetEntity) : 0;
    }
    
    public double getTargetDistance() {
        if (targetEntity == null || client.player == null) return 0;
        return client.player.distanceTo(targetEntity);
    }
    
    private boolean isMageWeapon(ItemStack stack) {
        if (stack.isEmpty()) return false;
        
        String itemName = stack.getName().getString().toLowerCase();
        return itemName.contains("mage") ||
               itemName.contains("staff") ||
               itemName.contains("wand") ||
               itemName.contains("sceptre") ||
               itemName.contains("crystal") ||
               itemName.contains("orb");
    }
    
    public boolean isKilling() {
        return isKilling;
    }
    
    public Entity getTargetEntity() {
        return targetEntity;
    }
    
    public int getAttackCount() {
        return attackCount;
    }
}