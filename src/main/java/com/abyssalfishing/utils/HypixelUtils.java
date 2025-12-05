package com.abyssalfishing.utils;

import com.abyssalfishing.AbyssalFishing;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.regex.Pattern;

public class HypixelUtils {
    private static boolean onHypixel = false;
    private static boolean receivedSuspiciousMessage = false;
    private static long lastSuspiciousMessageTime = 0;
    
    // Hypixel server patterns
    private static final Pattern HYPIXEL_PATTERN = Pattern.compile(".*hypixel\\.net.*", Pattern.CASE_INSENSITIVE);
    private static final Pattern SUSPICIOUS_MESSAGE_PATTERN = Pattern.compile(
        "(macro|bot|cheat|hack|automated|suspicious|watchdog)", 
        Pattern.CASE_INSENSITIVE
    );
    
    public static void initialize() {
        AbyssalFishing.LOGGER.info("HypixelUtils initialized");
    }
    
    public static boolean isOnHypixel() {
        // For testing, always return true to allow fishing anywhere
        // In production, uncomment the server check below:
        MinecraftClient client = MinecraftClient.getInstance();
        /*
        if (client.getCurrentServerEntry() != null) {
            String serverAddress = client.getCurrentServerEntry().address;
            return HYPIXEL_PATTERN.matcher(serverAddress).matches() ||
                   serverAddress.contains("hypixel") ||
                   serverAddress.contains("mini");
        }
        */
        return true;
    }
    
    public static boolean isInSkyBlock() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            // In 1.21.5, getServerBrand() was removed, use connection details instead
            if (isOnHypixel()) {
                return true;
            }
        }
        return false;
    }
    
    public static boolean isWaterBlock(World world, BlockPos pos) {
        if (world == null) return false;
        
        Block block = world.getBlockState(pos).getBlock();
        return block == Blocks.WATER || 
               block == Blocks.ICE || 
               block == Blocks.PACKED_ICE ||
               block == Blocks.FROSTED_ICE;
    }
    
    public static boolean isLavaBlock(World world, BlockPos pos) {
        if (world == null) return false;
        
        Block block = world.getBlockState(pos).getBlock();
        return block == Blocks.LAVA;
    }
    
    public static boolean isFishingSpot(BlockPos pos) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return false;
        
        // Check if position is suitable for fishing
        Block block = client.world.getBlockState(pos).getBlock();
        return block == Blocks.WATER;
    }
    
    public static void onChatMessage(String message) {
        // Check for suspicious messages from server
        if (SUSPICIOUS_MESSAGE_PATTERN.matcher(message).find()) {
            receivedSuspiciousMessage = true;
            lastSuspiciousMessageTime = System.currentTimeMillis();
            AbyssalFishing.LOGGER.warn("Received suspicious message: " + message);
        }
        
        // Check for Hypixel-specific messages
        if (message.contains("Hypixel") || message.contains("skyblock")) {
            onHypixel = true;
        }
    }
    
    public static boolean receivedSuspiciousMessage() {
        // Reset after 5 minutes
        if (System.currentTimeMillis() - lastSuspiciousMessageTime > 300000) {
            receivedSuspiciousMessage = false;
        }
        return receivedSuspiciousMessage;
    }
    
    public static boolean isInSafeArea() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return false;
        
        // Check if player is in a safe area (hub, island, etc.)
        String dimension = client.player.getWorld().getRegistryKey().getValue().toString();
        return dimension.contains("overworld") || dimension.contains("hub");
    }
    
    public static boolean canFishHere() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return false;
        
        // Check if player is in a fishing-friendly location
        if (!isOnHypixel()) return false;
        if (!isInSkyBlock()) return false;
        if (!isInSafeArea()) return false;
        
        return true;
    }
    
    public static String getCurrentLocation() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return "Unknown";
        
        String dimension = client.player.getWorld().getRegistryKey().getValue().toString();
        BlockPos pos = client.player.getBlockPos();
        
        return String.format("%s at %d, %d, %d", dimension, pos.getX(), pos.getY(), pos.getZ());
    }
    
    public static boolean isPlayerMoving() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return false;
        
        // Check if player is moving
        return client.player.getVelocity().lengthSquared() > 0.01;
    }
    
    public static boolean isPlayerRotating() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return false;
        
        // This would need to track previous rotation values
        // For now, return false
        return false;
    }
    
    public static int getPlayerHealth() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return 20;
        
        return (int) Math.ceil(client.player.getHealth());
    }
    
    public static boolean isPlayerInCombat() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return false;
        
        // Check if player was recently hurt
        return client.player.hurtTime > 0;
    }
    
    public static String getHypixelStatus() {
        return String.format("On Hypixel: %s, In SkyBlock: %s, Location: %s",
            isOnHypixel() ? "Yes" : "No",
            isInSkyBlock() ? "Yes" : "No",
            getCurrentLocation());
    }
}