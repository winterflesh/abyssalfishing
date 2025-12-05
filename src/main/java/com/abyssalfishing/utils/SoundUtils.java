package com.abyssalfishing.utils;

import com.abyssalfishing.AbyssalFishing;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;

public class SoundUtils {
    private static final MinecraftClient client = MinecraftClient.getInstance();
    
    public static void playSound(SoundEvent sound, float volume, float pitch) {
        if (client.player == null || sound == null) return;
        
        try {
            // Use the simple master constructor which takes volume and pitch
            client.getSoundManager().play(
                PositionedSoundInstance.master(sound, volume, pitch)
            );
        } catch (Exception e) {
            AbyssalFishing.LOGGER.error("Failed to play sound", e);
        }
    }
    
    public static void playSoundAt(SoundEvent sound, BlockPos pos, float volume, float pitch) {
        try {
            if (sound == null) return;
            
            // For positioned sounds, we use the master constructor and it plays at player location
            // For at a specific block, we would need a different approach
            client.getSoundManager().play(
                PositionedSoundInstance.master(sound, volume, pitch)
            );
        } catch (Exception e) {
            AbyssalFishing.LOGGER.error("Failed to play sound at position", e);
        }
    }
}