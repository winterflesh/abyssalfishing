package com.abyssalfishing.utils;

import com.abyssalfishing.AbyssalFishing;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;

public class PlayerUtils {
    private static Vec3d lastPosition = null;
    private static float lastYaw = 0;
    private static float lastPitch = 0;
    
    public static boolean isPlayerMoving() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return false;
        
        Vec3d velocity = client.player.getVelocity();
        return velocity.lengthSquared() > 0.001;
    }
    
    public static boolean hasPlayerMoved() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return false;
        
        Vec3d currentPos = client.player.getPos();
        
        if (lastPosition == null) {
            lastPosition = currentPos;
            return false;
        }
        
        boolean moved = currentPos.distanceTo(lastPosition) > 0.1;
        lastPosition = currentPos;
        
        return moved;
    }
    
    public static boolean hasPlayerRotated() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return false;
        
        float currentYaw = client.player.getYaw();
        float currentPitch = client.player.getPitch();
        
        boolean rotated = Math.abs(currentYaw - lastYaw) > 1.0 || 
                         Math.abs(currentPitch - lastPitch) > 1.0;
        
        lastYaw = currentYaw;
        lastPitch = currentPitch;
        
        return rotated;
    }
    
    public static boolean isPlayerInWater() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return false;
        
        return client.player.isTouchingWater();
    }
    
    public static boolean isPlayerSubmerged() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return false;
        
        return client.player.isSubmergedInWater();
    }
    
    public static float getPlayerHealth() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return 20.0f;
        
        return client.player.getHealth();
    }
    
    public static float getPlayerHunger() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return 20.0f;
        
        return client.player.getHungerManager().getFoodLevel();
    }
    
    public static boolean isPlayerHungry() {
        return getPlayerHunger() < 10;
    }
    
    public static boolean isPlayerInDanger() {
        float health = getPlayerHealth();
        return health < 10.0f || isPlayerInCombat();
    }
    
    public static boolean isPlayerInCombat() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return false;
        
        return client.player.hurtTime > 0;
    }
    
    public static boolean canPlayerFish() {
        if (!HypixelUtils.isOnHypixel()) {
            AbyssalFishing.LOGGER.warn("Not on Hypixel");
            return false;
        }
        
        if (isPlayerInDanger()) {
            AbyssalFishing.LOGGER.warn("Player in danger");
            return false;
        }
        
        if (isPlayerHungry()) {
            AbyssalFishing.LOGGER.warn("Player is hungry");
            return false;
        }
        
        return true;
    }
    
    public static void resetPositionTracking() {
        lastPosition = null;
        lastYaw = 0;
        lastPitch = 0;
    }
}