package com.abyssalfishing.config;

import com.abyssalfishing.AbyssalFishing;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

public class AbyssalConfig {
    private final File configFile;
    private final Properties properties;
    
    // Core fishing settings
    public boolean autoStart = true;
    public boolean playSounds = true;
    public boolean showHUD = true;
    
    // Timing settings
    public long baseWaitTime = 5000;
    public long maxWaitTime = 20000;
    public long reactionTime = 150;
    public long baseCooldown = 2000;
    
    // Safety settings
    public boolean pauseOnMovement = true;
    public boolean pauseOnRotation = true;
    public int maxFailedAttempts = 5;
    public float humanizationFactor = 1.0f;
    public int emergencyHealthThreshold = 10;
    
    
    // Sea creature killing
    public boolean killSeaCreatures = true;
    public boolean killSquid = false;
    public boolean useMageWeapons = true;
    public boolean useMeleeWeapons = true;
    
    // AFK prevention
    public boolean preventAFK = true;
    
    // HUD message duration (ms)
    public long hudMessageDuration = 7000;
    
    // Debug overlay
    public boolean showDebugOverlay = false;
    public KeyBinding guiKey = new KeyBinding(
        "key.abyssalfishing.gui",
        InputUtil.Type.KEYSYM,
        GLFW.GLFW_KEY_INSERT,
        "category.abyssalfishing.main"
    );
    
    public AbyssalConfig() {
        this.configFile = new File("config/abyssalfishing.properties");
        this.properties = new Properties();
        load();
    }
    
    public void load() {
        try {
            if (configFile.exists()) {
                FileReader reader = new FileReader(configFile);
                properties.load(reader);
                reader.close();
                
                autoStart = getBoolean("autoStart", autoStart);
                playSounds = getBoolean("playSounds", playSounds);
                showHUD = getBoolean("showHUD", showHUD);
                
                baseWaitTime = getLong("baseWaitTime", baseWaitTime);
                maxWaitTime = getLong("maxWaitTime", maxWaitTime);
                reactionTime = getLong("reactionTime", reactionTime);
                baseCooldown = getLong("baseCooldown", baseCooldown);
                
                pauseOnMovement = getBoolean("pauseOnMovement", pauseOnMovement);
                pauseOnRotation = getBoolean("pauseOnRotation", pauseOnRotation);
                maxFailedAttempts = getInt("maxFailedAttempts", maxFailedAttempts);
                humanizationFactor = getFloat("humanizationFactor", humanizationFactor);
                emergencyHealthThreshold = getInt("emergencyHealthThreshold", emergencyHealthThreshold);
                
                
                killSeaCreatures = getBoolean("killSeaCreatures", killSeaCreatures);
                killSquid = getBoolean("killSquid", killSquid);
                useMageWeapons = getBoolean("useMageWeapons", useMageWeapons);
                useMeleeWeapons = getBoolean("useMeleeWeapons", useMeleeWeapons);
                
                preventAFK = getBoolean("preventAFK", preventAFK);
                hudMessageDuration = getLong("hudMessageDuration", hudMessageDuration);
                showDebugOverlay = getBoolean("showDebugOverlay", showDebugOverlay);
                
                AbyssalFishing.LOGGER.info("Configuration loaded successfully");
            }
        } catch (IOException e) {
            AbyssalFishing.LOGGER.error("Failed to load configuration", e);
        }
    }
    
    public void save() {
        try {
            configFile.getParentFile().mkdirs();
            
            properties.setProperty("autoStart", String.valueOf(autoStart));
            properties.setProperty("playSounds", String.valueOf(playSounds));
            properties.setProperty("showHUD", String.valueOf(showHUD));
            
            properties.setProperty("baseWaitTime", String.valueOf(baseWaitTime));
            properties.setProperty("maxWaitTime", String.valueOf(maxWaitTime));
            properties.setProperty("reactionTime", String.valueOf(reactionTime));
            properties.setProperty("baseCooldown", String.valueOf(baseCooldown));
            
            properties.setProperty("pauseOnMovement", String.valueOf(pauseOnMovement));
            properties.setProperty("pauseOnRotation", String.valueOf(pauseOnRotation));
            properties.setProperty("maxFailedAttempts", String.valueOf(maxFailedAttempts));
            properties.setProperty("humanizationFactor", String.valueOf(humanizationFactor));
            properties.setProperty("emergencyHealthThreshold", String.valueOf(emergencyHealthThreshold));
            
            
            properties.setProperty("killSeaCreatures", String.valueOf(killSeaCreatures));
            properties.setProperty("killSquid", String.valueOf(killSquid));
            properties.setProperty("useMageWeapons", String.valueOf(useMageWeapons));
            properties.setProperty("useMeleeWeapons", String.valueOf(useMeleeWeapons));
            
            properties.setProperty("preventAFK", String.valueOf(preventAFK));
            properties.setProperty("hudMessageDuration", String.valueOf(hudMessageDuration));
            properties.setProperty("showDebugOverlay", String.valueOf(showDebugOverlay));
            
            FileWriter writer = new FileWriter(configFile);
            properties.store(writer, "AbyssalFishing Configuration");
            writer.close();
            
            AbyssalFishing.LOGGER.info("Configuration saved successfully");
        } catch (IOException e) {
            AbyssalFishing.LOGGER.error("Failed to save configuration", e);
        }
    }
    
    private boolean getBoolean(String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        return value != null ? Boolean.parseBoolean(value) : defaultValue;
    }
    
    private int getInt(String key, int defaultValue) {
        String value = properties.getProperty(key);
        try {
            return value != null ? Integer.parseInt(value) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    private long getLong(String key, long defaultValue) {
        String value = properties.getProperty(key);
        try {
            return value != null ? Long.parseLong(value) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    private float getFloat(String key, float defaultValue) {
        String value = properties.getProperty(key);
        try {
            return value != null ? Float.parseFloat(value) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    public KeyBinding getGUIKey() {
        return guiKey;
    }
}