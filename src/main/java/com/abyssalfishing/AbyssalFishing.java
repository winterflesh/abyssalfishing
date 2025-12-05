package com.abyssalfishing;

import com.abyssalfishing.core.FishingManager;
import com.abyssalfishing.config.AbyssalConfig;
import com.abyssalfishing.gui.AbyssalFishingGUI;
import com.abyssalfishing.utils.RenderUtils;
import com.abyssalfishing.utils.HypixelUtils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbyssalFishing implements ClientModInitializer {
    public static final String MOD_ID = "abyssalfishing";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    // Core components
    public static FishingManager fishingManager;
    public static AbyssalConfig config;
    
    // Key bindings
    public static KeyBinding toggleFishingKey;
    public static KeyBinding openConfigKey;
    public static KeyBinding toggleDebugKey;
    
    // Track previous key states to detect presses
    private static boolean prevToggleState = false;
    private static boolean prevConfigState = false;
    private static boolean prevDebugState = false;
    
    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing AbyssalFishing - The Ultimate Hypixel SkyBlock Fishing Mod");
        
        // Initialize configuration
        config = new AbyssalConfig();
        config.load();
        LOGGER.info("Config loaded");
        
        // Initialize core systems
        fishingManager = new FishingManager();
        
        // Initialize key bindings
        initializeKeyBindings();
        LOGGER.info("Key bindings registered");
        
        // Register event handlers
        registerEvents();
        LOGGER.info("Events registered");
        
        // Initialize Hypixel detection
        HypixelUtils.initialize();
        
        LOGGER.info("AbyssalFishing initialized successfully!");
    }
    
    private void initializeKeyBindings() {
        toggleFishingKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.abyssalfishing.toggle",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_F,
            "category.abyssalfishing.main"
        ));
        
        openConfigKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.abyssalfishing.config",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_O,
            "category.abyssalfishing.main"
        ));
        
        toggleDebugKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.abyssalfishing.debug",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_L,
            "category.abyssalfishing.main"
        ));
    }
    
    private void registerEvents() {
        // Main tick event
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            
            // Handle key inputs (always available)
            if (toggleFishingKey.wasPressed()) {
                LOGGER.info("F key pressed - toggling fishing");
                fishingManager.toggleFishing();
                // HUD message is handled by FishingManager.startFishing/stopFishing
            }
            
            if (openConfigKey.wasPressed()) {
                LOGGER.info("O key pressed - opening config");
                client.setScreen(new AbyssalFishingGUI(client.currentScreen));
                fishingManager.setHUDMessage("§6[AbyssalFishing] §fOpened config", config.hudMessageDuration);
            }
            
            if (toggleDebugKey.wasPressed()) {
                config.showDebugOverlay = !config.showDebugOverlay;
                config.save();
                String status = config.showDebugOverlay ? "§aON" : "§cOFF";
                fishingManager.setHUDMessage("§6[AbyssalFishing] §fDebug overlay " + status, config.hudMessageDuration);
                LOGGER.info("Debug overlay toggled: " + config.showDebugOverlay);
            }
            
            // Update fishing systems (works everywhere now)
            if (client.world != null) {
                fishingManager.update();
            }
        });
        
        // HUD rendering
        HudRenderCallback.EVENT.register((drawContext, delta) -> {
            if (config.showHUD) {
                RenderUtils.renderFishingHUD(drawContext);
            }
            // Always render action-bar style HUD messages (above hotbar)
            com.abyssalfishing.utils.ActionBarRenderer.render(drawContext);
            // Render notifications
            com.abyssalfishing.gui.components.NotificationManager.getInstance().render(drawContext);
            // Render debug overlay if enabled
            if (config.showDebugOverlay) {
                com.abyssalfishing.utils.DebugOverlay.render(drawContext);
            }
        });
    }
}      