package com.abyssalfishing.features;

import com.abyssalfishing.AbyssalFishing;
import com.abyssalfishing.config.AbyssalConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;
import org.lwjgl.glfw.GLFW;

import java.util.Random;

public class MouseMovement {
    private final MinecraftClient client;
    private final AbyssalConfig config;
    private final Random random;
    
    private long lastMovementTime;
    private float targetYaw;
    private float targetPitch;
    private float currentYaw;
    private float currentPitch;
    private boolean isMoving;
    private int movementPhase;
    
    private static final float MOVEMENT_SPEED = 0.5f;
    private static final float MAX_YAW_CHANGE = 10.0f;
    private static final float MAX_PITCH_CHANGE = 5.0f;
    
    public MouseMovement() {
        this.client = MinecraftClient.getInstance();
        this.config = AbyssalFishing.config;
        this.random = new Random();
        this.lastMovementTime = 0;
        this.isMoving = false;
        this.movementPhase = 0;
    }
    
    public void update() {
        if (!config.preventAFK || client.player == null) return;
        
        long currentTime = System.currentTimeMillis();
        
        // Initialize current rotation if not set
        if (lastMovementTime == 0) {
            currentYaw = client.player.getYaw();
            currentPitch = client.player.getPitch();
            lastMovementTime = currentTime;
        }
        
        // Check if we should start a new movement
        if (!isMoving && currentTime - lastMovementTime > getNextMovementDelay()) {
            startNewMovement();
        }
        
        // Handle ongoing movement
        if (isMoving) {
            updateMovement();
        }
    }
    
    private void startNewMovement() {
        if (client.player == null) return;
        
        // Generate random target rotation
        float baseYaw = client.player.getYaw();
        float basePitch = client.player.getPitch();
        
        // Small random changes to avoid detection
        targetYaw = baseYaw + (random.nextFloat() - 0.5f) * MAX_YAW_CHANGE;
        targetPitch = Math.max(-90, Math.min(90, 
            basePitch + (random.nextFloat() - 0.5f) * MAX_PITCH_CHANGE));
        
        currentYaw = baseYaw;
        currentPitch = basePitch;
        isMoving = true;
        movementPhase = 0;
        
        AbyssalFishing.LOGGER.debug("Starting mouse movement - Target: Yaw " + targetYaw + ", Pitch " + targetPitch);
    }
    
    private void updateMovement() {
        if (client.player == null) return;
        
        // Smooth movement towards target
        float yawDiff = targetYaw - currentYaw;
        float pitchDiff = targetPitch - currentPitch;
        
        // Check if we've reached the target
        if (Math.abs(yawDiff) < 0.1f && Math.abs(pitchDiff) < 0.1f) {
            finishMovement();
            return;
        }
        
        // Move towards target
        if (Math.abs(yawDiff) > 0.1f) {
            currentYaw += Math.signum(yawDiff) * Math.min(Math.abs(yawDiff), MOVEMENT_SPEED);
        }
        
        if (Math.abs(pitchDiff) > 0.1f) {
            currentPitch += Math.signum(pitchDiff) * Math.min(Math.abs(pitchDiff), MOVEMENT_SPEED);
        }
        
        // Apply the rotation
        client.player.setYaw(currentYaw);
        client.player.setPitch(currentPitch);
        
        movementPhase++;
    }
    
    private void finishMovement() {
        isMoving = false;
        lastMovementTime = System.currentTimeMillis();
        
        // Ensure we reach the exact target
        if (client.player != null) {
            client.player.setYaw(targetYaw);
            client.player.setPitch(targetPitch);
        }
        
        AbyssalFishing.LOGGER.debug("Finished mouse movement after " + movementPhase + " phases");
    }
    
    private long getNextMovementDelay() {
        // More natural: Random delay between 20 seconds and 3 minutes
        // Shorter delays are more common (human-like)
        long baseDelay = 20000;
        long maxDelay = 180000;
        // Use exponential distribution for more natural timing
        double randomFactor = Math.pow(random.nextDouble(), 2.0); // Bias towards shorter delays
        return baseDelay + (long) (randomFactor * (maxDelay - baseDelay));
    }
    
    public boolean isMoving() {
        return isMoving;
    }
    
    public long getTimeSinceLastMovement() {
        return System.currentTimeMillis() - lastMovementTime;
    }
    
    public void forceMovement() {
        // Force an immediate movement
        lastMovementTime = 0;
        isMoving = false;
    }
}