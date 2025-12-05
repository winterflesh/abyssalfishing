package com.abyssalfishing.gui;

import com.abyssalfishing.AbyssalFishing;
import com.abyssalfishing.config.AbyssalConfig;
import com.abyssalfishing.gui.components.ToggleSwitch;
import com.abyssalfishing.gui.components.DropdownWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import java.util.Arrays;
import java.util.List;

public class AbyssalFishingGUI extends Screen {
    private final Screen parent;
    private final AbyssalConfig config;
    private int currentTab = 0; // 0 = Fishing, 1 = Safety, 2 = Sea Creatures, 3 = Statistics
    
    // Sidebar
    private static final int SIDEBAR_WIDTH = 50;
    private int selectedSidebarIndex = 0;
    
    // Toggle switches
    private ToggleSwitch autoStartToggle;
    private ToggleSwitch playSoundsToggle;
    private ToggleSwitch showHUDToggle;
    private ToggleSwitch showDebugToggle;
    private ToggleSwitch preventAFKToggle;
    private ToggleSwitch killSeaCreaturesToggle;
    private ToggleSwitch pauseOnMovementToggle;
    private ToggleSwitch pauseOnRotationToggle;
    private ToggleSwitch killSquidToggle;
    private ToggleSwitch useMageWeaponsToggle;
    private ToggleSwitch useMeleeWeaponsToggle;
    
    // Text fields
    private TextFieldWidget baseWaitField;
    private TextFieldWidget maxWaitField;
    private TextFieldWidget reactionField;
    private TextFieldWidget cooldownField;
    private TextFieldWidget hudDurationField;
    private TextFieldWidget emergencyHealthField;
    private TextFieldWidget detectionDelayField;
    
    // Dropdowns
    private DropdownWidget weaponTypeDropdown;
    
    public AbyssalFishingGUI(Screen parent) {
        super(Text.literal("AbyssalFishing"));
        this.parent = parent;
        this.config = AbyssalFishing.config;
    }

    @Override
    protected void init() {
        super.init();
        clearChildren();
        initCurrentTab();
    }
    
    private void initCurrentTab() {
        switch (currentTab) {
            case 0: initFishingTab(); break;
            case 1: initSafetyTab(); break;
            case 2: initSeaCreaturesTab(); break;
            case 3: initStatisticsTab(); break;
        }
        
        // Save/Discard buttons (always visible)
        int btnY = height - 30;
        addDrawableChild(ButtonWidget.builder(Text.literal("Save & Close"), btn -> {
            saveAllFields();
            config.save();
            com.abyssalfishing.gui.components.NotificationManager.getInstance()
                .addNotification("Config saved!", com.abyssalfishing.gui.components.NotificationManager.NotificationType.SUCCESS);
            MinecraftClient.getInstance().setScreen(parent);
        }).dimensions(width - 220, btnY, 100, 20).build());
        
        addDrawableChild(ButtonWidget.builder(Text.literal("Discard"), btn -> {
            MinecraftClient.getInstance().setScreen(parent);
        }).dimensions(width - 110, btnY, 100, 20).build());
    }
    
    private void initFishingTab() {
        int panelX = SIDEBAR_WIDTH + 20;
        int panelY = 20;
        int panelWidth = 400;
        int yOffset = panelY + 30;
        int gap = 50; // Increased gap for better spacing
        
        // Core Fishing Panel - toggles with descriptions below
        autoStartToggle = addDrawableChild(new ToggleSwitch(
            panelX + 10, yOffset, 40, 16,
            config.autoStart, () -> config.autoStart = autoStartToggle.isEnabled()
        ));
        
        playSoundsToggle = addDrawableChild(new ToggleSwitch(
            panelX + 10, yOffset + gap, 40, 16,
            config.playSounds, () -> config.playSounds = playSoundsToggle.isEnabled()
        ));
        
        showHUDToggle = addDrawableChild(new ToggleSwitch(
            panelX + 10, yOffset + gap * 2, 40, 16,
            config.showHUD, () -> config.showHUD = showHUDToggle.isEnabled()
        ));
        
        showDebugToggle = addDrawableChild(new ToggleSwitch(
            panelX + 10, yOffset + gap * 3, 40, 16,
            config.showDebugOverlay, () -> config.showDebugOverlay = showDebugToggle.isEnabled()
        ));
        
        // Timing Panel - text fields with descriptions below
        int timingY = yOffset + gap * 4 + 30;
        baseWaitField = new TextFieldWidget(textRenderer, panelX + 10, timingY, 150, 20, Text.literal("Base Wait (ms)"));
        baseWaitField.setText(String.valueOf(config.baseWaitTime));
        baseWaitField.setMaxLength(6);
        addDrawableChild(baseWaitField);
        
        maxWaitField = new TextFieldWidget(textRenderer, panelX + 10, timingY + gap, 150, 20, Text.literal("Max Wait (ms)"));
        maxWaitField.setText(String.valueOf(config.maxWaitTime));
        maxWaitField.setMaxLength(6);
        addDrawableChild(maxWaitField);
        
        reactionField = new TextFieldWidget(textRenderer, panelX + 10, timingY + gap * 2, 150, 20, Text.literal("Reaction Time (ms)"));
        reactionField.setText(String.valueOf(config.reactionTime));
        reactionField.setMaxLength(5);
        addDrawableChild(reactionField);
        
        cooldownField = new TextFieldWidget(textRenderer, panelX + 10, timingY + gap * 3, 150, 20, Text.literal("Cooldown (ms)"));
        cooldownField.setText(String.valueOf(config.baseCooldown));
        cooldownField.setMaxLength(6);
        addDrawableChild(cooldownField);
        
        detectionDelayField = new TextFieldWidget(textRenderer, panelX + 10, timingY + gap * 4, 150, 20, Text.literal("Detection Delay (ms)"));
        detectionDelayField.setText("1000");
        detectionDelayField.setMaxLength(5);
        addDrawableChild(detectionDelayField);
    }
    
    private void initSafetyTab() {
        int panelX = SIDEBAR_WIDTH + 20;
        int panelY = 20;
        int yOffset = panelY + 30;
        int gap = 50; // Increased gap for better spacing
        
        preventAFKToggle = addDrawableChild(new ToggleSwitch(
            panelX + 10, yOffset, 40, 16,
            config.preventAFK, () -> config.preventAFK = preventAFKToggle.isEnabled()
        ));
        
        pauseOnMovementToggle = addDrawableChild(new ToggleSwitch(
            panelX + 10, yOffset + gap, 40, 16,
            config.pauseOnMovement, () -> config.pauseOnMovement = pauseOnMovementToggle.isEnabled()
        ));
        
        pauseOnRotationToggle = addDrawableChild(new ToggleSwitch(
            panelX + 10, yOffset + gap * 2, 40, 16,
            config.pauseOnRotation, () -> config.pauseOnRotation = pauseOnRotationToggle.isEnabled()
        ));
        
        hudDurationField = new TextFieldWidget(textRenderer, panelX + 10, yOffset + gap * 3, 150, 20, Text.literal("HUD Duration (ms)"));
        hudDurationField.setText(String.valueOf(config.hudMessageDuration));
        hudDurationField.setMaxLength(5);
        addDrawableChild(hudDurationField);
        
        emergencyHealthField = new TextFieldWidget(textRenderer, panelX + 10, yOffset + gap * 4, 150, 20, Text.literal("Min Health"));
        emergencyHealthField.setText(String.valueOf(config.emergencyHealthThreshold));
        emergencyHealthField.setMaxLength(2);
        addDrawableChild(emergencyHealthField);
    }
    
    private void initSeaCreaturesTab() {
        int panelX = SIDEBAR_WIDTH + 20;
        int panelY = 20;
        int yOffset = panelY + 30;
        int gap = 50; // Increased gap for better spacing
        
        killSeaCreaturesToggle = addDrawableChild(new ToggleSwitch(
            panelX + 10, yOffset, 40, 16,
            config.killSeaCreatures, () -> config.killSeaCreatures = killSeaCreaturesToggle.isEnabled()
        ));
        
        killSquidToggle = addDrawableChild(new ToggleSwitch(
            panelX + 10, yOffset + gap, 40, 16,
            config.killSquid, () -> config.killSquid = killSquidToggle.isEnabled()
        ));
        
        useMageWeaponsToggle = addDrawableChild(new ToggleSwitch(
            panelX + 10, yOffset + gap * 2, 40, 16,
            config.useMageWeapons, () -> config.useMageWeapons = useMageWeaponsToggle.isEnabled()
        ));
        
        useMeleeWeaponsToggle = addDrawableChild(new ToggleSwitch(
            panelX + 10, yOffset + gap * 3, 40, 16,
            config.useMeleeWeapons, () -> config.useMeleeWeapons = useMeleeWeaponsToggle.isEnabled()
        ));
        
        List<String> weaponOptions = Arrays.asList("Mage Only", "Melee Only", "Both");
        int selectedIndex = config.useMageWeapons && config.useMeleeWeapons ? 2 :
                           config.useMageWeapons ? 0 : 1;
        weaponTypeDropdown = addDrawableChild(new DropdownWidget(
            panelX + 10, yOffset + gap * 4, 200, 20,
            weaponOptions, selectedIndex,
            idx -> {
                config.useMageWeapons = (idx == 0 || idx == 2);
                config.useMeleeWeapons = (idx == 1 || idx == 2);
            }
        ));
    }
    
    private void initStatisticsTab() {
        // Statistics tab doesn't need widgets
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Solid background - NO blur effect
        context.fill(0, 0, width, height, 0xFF101010);
        
        // Draw sidebar
        drawSidebar(context, mouseX, mouseY);
        
        // Draw main content area
        int contentX = SIDEBAR_WIDTH;
        context.fill(contentX, 0, width, height, 0xFF1A1A1A);
        
        // Draw labels and descriptions based on current tab (widgets are rendered by super.render)
        switch (currentTab) {
            case 0: renderFishingTab(context, mouseX, mouseY); break;
            case 1: renderSafetyTab(context, mouseX, mouseY); break;
            case 2: renderSeaCreaturesTab(context, mouseX, mouseY); break;
            case 3: renderStatisticsTab(context); break;
        }

        // Render widgets AFTER labels (so they appear on top)
        super.render(context, mouseX, mouseY, delta);
    }
    
    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // Override to prevent blur - we draw our own background in render()
    }
    
    private void drawSidebar(DrawContext context, int mouseX, int mouseY) {
        // Sidebar background
        context.fill(0, 0, SIDEBAR_WIDTH, height, 0xFF2A2A2A);
        context.drawBorder(0, 0, SIDEBAR_WIDTH, height, 0xFF000000);
        
        String[] icons = {"⚙", "🛡", "🐟", "📊"};
        String[] tooltips = {"Fishing", "Safety", "Sea Creatures", "Statistics"};
        
        int iconSize = 30;
        int startY = 20;
        int gap = 10;
        
        for (int i = 0; i < icons.length; i++) {
            int y = startY + i * (iconSize + gap);
            boolean selected = currentTab == i;
            boolean hovered = mouseX >= 5 && mouseX <= SIDEBAR_WIDTH - 5 &&
                             mouseY >= y && mouseY <= y + iconSize;
            
            // Background
            if (selected) {
                context.fill(5, y, SIDEBAR_WIDTH - 5, y + iconSize, 0xFF3A3A3A);
            } else if (hovered) {
                context.fill(5, y, SIDEBAR_WIDTH - 5, y + iconSize, 0xFF2A2A2A);
            }
            
            // Icon
            int iconX = (SIDEBAR_WIDTH - textRenderer.getWidth(icons[i])) / 2;
            int iconY = y + (iconSize - 8) / 2;
            context.drawText(textRenderer, icons[i], iconX, iconY, 
                selected ? 0xFFFFFFFF : 0xFFAAAAAA, false);
            
            // Click detection
            if (hovered && mouseX >= 5 && mouseX <= SIDEBAR_WIDTH - 5) {
                if (mouseY >= y && mouseY <= y + iconSize) {
                    // Tooltip
                    int tooltipX = SIDEBAR_WIDTH + 5;
                    int tooltipY = mouseY;
                    int tooltipWidth = textRenderer.getWidth(tooltips[i]) + 8;
                    context.fill(tooltipX, tooltipY - 10, tooltipX + tooltipWidth, tooltipY + 2, 0xE0000000);
                    context.drawText(textRenderer, tooltips[i], tooltipX + 4, tooltipY - 8, 0xFFFFFF, false);
                }
            }
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // First, let widgets (including dropdowns) handle clicks
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        
        // Close expanded dropdowns when clicking outside
        if (button == 0) {
            for (net.minecraft.client.gui.Element child : children()) {
                if (child instanceof com.abyssalfishing.gui.components.DropdownWidget dropdown) {
                    if (dropdown.isExpanded()) {
                        // Check if click is outside dropdown
                        int startY = dropdown.getY() + dropdown.getHeight();
                        int dropdownHeight = dropdown.getOptions().size() * 12;
                        if (mouseX < dropdown.getX() || mouseX > dropdown.getX() + dropdown.getWidth() ||
                            mouseY < dropdown.getY() || mouseY > startY + dropdownHeight) {
                            dropdown.setExpanded(false);
                        }
                    }
                }
            }
        }
        
        // Sidebar click detection (only if not clicking on widgets)
        if (mouseX >= 0 && mouseX <= SIDEBAR_WIDTH && button == 0) {
            int iconSize = 30;
            int startY = 20;
            int gap = 10;
            
            for (int i = 0; i < 4; i++) {
                int y = startY + i * (iconSize + gap);
                if (mouseY >= y && mouseY <= y + iconSize) {
                    currentTab = i;
                    clearChildren();
                    initCurrentTab();
                    return true;
                }
            }
        }
        
        return false;
    }
    
    private void renderFishingTab(DrawContext context, int mouseX, int mouseY) {
        int panelX = SIDEBAR_WIDTH + 20;
        int panelY = 20;
        int panelWidth = 450;
        int yOffset = panelY + 30;
        int gap = 50;
        
        // Core Fishing Section
        context.drawText(textRenderer, "Auto Start", panelX + 60, yOffset + 2, 0xFFFFFF, false);
        context.drawText(textRenderer, "§7Automatically start fishing when enabled", panelX + 60, yOffset + 18, 0xAAAAAA, false);
        
        context.drawText(textRenderer, "Play Sounds", panelX + 60, yOffset + gap + 2, 0xFFFFFF, false);
        context.drawText(textRenderer, "§7Play fishing sounds", panelX + 60, yOffset + gap + 18, 0xAAAAAA, false);
        
        context.drawText(textRenderer, "Show HUD", panelX + 60, yOffset + gap * 2 + 2, 0xFFFFFF, false);
        context.drawText(textRenderer, "§7Display fishing statistics HUD", panelX + 60, yOffset + gap * 2 + 18, 0xAAAAAA, false);
        
        context.drawText(textRenderer, "Debug Overlay", panelX + 60, yOffset + gap * 3 + 2, 0xFFFFFF, false);
        context.drawText(textRenderer, "§7Show debug information", panelX + 60, yOffset + gap * 3 + 18, 0xAAAAAA, false);
        
        // Timing Settings Section
        int timingY = yOffset + gap * 4 + 30;
        context.drawText(textRenderer, "§6=== Timing Settings ===", panelX + 10, timingY - 5, 0xFFFFFF, false);
        
        context.drawText(textRenderer, "Base Wait (ms)", panelX + 10, timingY + 25, 0xFFFFFF, false);
        context.drawText(textRenderer, "§7Minimum wait time before detecting fish", panelX + 10, timingY + 40, 0xAAAAAA, false);
        
        context.drawText(textRenderer, "Max Wait (ms)", panelX + 10, timingY + gap + 25, 0xFFFFFF, false);
        context.drawText(textRenderer, "§7Maximum wait time before timeout", panelX + 10, timingY + gap + 40, 0xAAAAAA, false);
        
        context.drawText(textRenderer, "Reaction Time (ms)", panelX + 10, timingY + gap * 2 + 25, 0xFFFFFF, false);
        context.drawText(textRenderer, "§7Delay before reeling after detection", panelX + 10, timingY + gap * 2 + 40, 0xAAAAAA, false);
        
        context.drawText(textRenderer, "Cooldown (ms)", panelX + 10, timingY + gap * 3 + 25, 0xFFFFFF, false);
        context.drawText(textRenderer, "§7Wait time between casts", panelX + 10, timingY + gap * 3 + 40, 0xAAAAAA, false);
        
        context.drawText(textRenderer, "Detection Delay (ms)", panelX + 10, timingY + gap * 4 + 25, 0xFFFFFF, false);
        context.drawText(textRenderer, "§7Delay after bobber settles", panelX + 10, timingY + gap * 4 + 40, 0xAAAAAA, false);
    }
    
    private void renderSafetyTab(DrawContext context, int mouseX, int mouseY) {
        int panelX = SIDEBAR_WIDTH + 20;
        int panelY = 20;
        int yOffset = panelY + 30;
        int gap = 50;
        
        context.drawText(textRenderer, "Prevent AFK", panelX + 60, yOffset + 2, 0xFFFFFF, false);
        context.drawText(textRenderer, "§7Random mouse movements to prevent AFK", panelX + 60, yOffset + 18, 0xAAAAAA, false);
        
        context.drawText(textRenderer, "Pause on Move", panelX + 60, yOffset + gap + 2, 0xFFFFFF, false);
        context.drawText(textRenderer, "§7Stop fishing if player moves", panelX + 60, yOffset + gap + 18, 0xAAAAAA, false);
        
        context.drawText(textRenderer, "Pause on Rotate", panelX + 60, yOffset + gap * 2 + 2, 0xFFFFFF, false);
        context.drawText(textRenderer, "§7Stop fishing if player rotates", panelX + 60, yOffset + gap * 2 + 18, 0xAAAAAA, false);
        
        context.drawText(textRenderer, "HUD Duration (ms)", panelX + 10, yOffset + gap * 3 + 25, 0xFFFFFF, false);
        context.drawText(textRenderer, "§7How long HUD messages show", panelX + 10, yOffset + gap * 3 + 40, 0xAAAAAA, false);
        
        context.drawText(textRenderer, "Min Health", panelX + 10, yOffset + gap * 4 + 25, 0xFFFFFF, false);
        context.drawText(textRenderer, "§7Stop fishing below this health", panelX + 10, yOffset + gap * 4 + 40, 0xAAAAAA, false);
    }
    
    private void renderSeaCreaturesTab(DrawContext context, int mouseX, int mouseY) {
        int panelX = SIDEBAR_WIDTH + 20;
        int panelY = 20;
        int yOffset = panelY + 30;
        int gap = 50;
        
        context.drawText(textRenderer, "Kill Sea Creatures", panelX + 60, yOffset + 2, 0xFFFFFF, false);
        context.drawText(textRenderer, "§7Auto-kill caught sea creatures", panelX + 60, yOffset + 18, 0xAAAAAA, false);
        
        context.drawText(textRenderer, "Kill Squid", panelX + 60, yOffset + gap + 2, 0xFFFFFF, false);
        context.drawText(textRenderer, "§7Also kill regular squid", panelX + 60, yOffset + gap + 18, 0xAAAAAA, false);
        
        context.drawText(textRenderer, "Use Mage Weapons", panelX + 60, yOffset + gap * 2 + 2, 0xFFFFFF, false);
        context.drawText(textRenderer, "§7Use magic weapons for killing", panelX + 60, yOffset + gap * 2 + 18, 0xAAAAAA, false);
        
        context.drawText(textRenderer, "Use Melee Weapons", panelX + 60, yOffset + gap * 3 + 2, 0xFFFFFF, false);
        context.drawText(textRenderer, "§7Use melee weapons for killing", panelX + 60, yOffset + gap * 3 + 18, 0xAAAAAA, false);
        
        context.drawText(textRenderer, "Weapon Type", panelX + 10, yOffset + gap * 4 + 25, 0xFFFFFF, false);
        context.drawText(textRenderer, "§7Choose weapon preference (Mage/Melee/Both)", panelX + 10, yOffset + gap * 4 + 40, 0xAAAAAA, false);
    }
    
    private void renderStatisticsTab(DrawContext context) {
        if (AbyssalFishing.fishingManager == null) {
            context.drawText(textRenderer, "Fishing Manager not initialized", 
                width / 2 - 100, height / 2, 0xFFFFFF, false);
            return;
        }
        
        int panelX = SIDEBAR_WIDTH + 20;
        int panelY = 20;
        int panelWidth = width - SIDEBAR_WIDTH - 40;
        int panelHeight = height - 60;
        
        // Statistics panel background
        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xFF2A2A2A);
        context.drawBorder(panelX, panelY, panelWidth, panelHeight, 0xFF000000);
        context.drawText(textRenderer, "Statistics", panelX + 10, panelY + 8, 0xFFFFFF, false);
        context.fill(panelX + 10, panelY + 18, panelX + panelWidth - 10, panelY + 19, 0xFF000000);
        
        int x = panelX + 20;
        int y = panelY + 30;
        int lineHeight = 15;
        
        int totalCatches = AbyssalFishing.fishingManager.getTotalCatches();
        int seaCreatures = AbyssalFishing.fishingManager.getSeaCreaturesCaught();
        int failedCatches = AbyssalFishing.fishingManager.getFailedCatches();
        int maxConsecutive = AbyssalFishing.fishingManager.getMaxConsecutiveCatches();
        long sessionTime = AbyssalFishing.fishingManager.getSessionTime();
        int consecutive = AbyssalFishing.fishingManager.getConsecutiveCatches();
        double successRate = AbyssalFishing.fishingManager.getSuccessRate();
        
        context.drawText(textRenderer, "Total Catches: " + totalCatches, x, y, 0xFFFFFF, false);
        y += lineHeight;
        context.drawText(textRenderer, "Sea Creatures: " + seaCreatures, x, y, 0xFFFFFF, false);
        y += lineHeight;
        context.drawText(textRenderer, "Failed Catches: " + failedCatches, x, y, 0xFFFFFF, false);
        y += lineHeight;
        context.drawText(textRenderer, "Success Rate: " + String.format("%.1f%%", successRate), x, y, 0xFFFFFF, false);
        y += lineHeight;
        context.drawText(textRenderer, "Current Streak: " + consecutive, x, y, 0xFFFFFF, false);
        y += lineHeight;
        context.drawText(textRenderer, "Best Streak: " + maxConsecutive, x, y, 0xFFFFFF, false);
        y += lineHeight;
        context.drawText(textRenderer, "Session Time: " + formatTime(sessionTime), x, y, 0xFFFFFF, false);
    }
    
    private void drawPanel(DrawContext context, int x, int y, int width, int height, String title) {
        // Panel background
        context.fill(x, y, x + width, y + height, 0xFF2A2A2A);
        context.drawBorder(x, y, width, height, 0xFF000000);
        
        // Title
        context.drawText(textRenderer, title, x + 10, y + 8, 0xFFFFFF, false);
        context.fill(x + 10, y + 18, x + width - 10, y + 19, 0xFF000000);
    }
    
    private void saveAllFields() {
        // Save text fields only if they exist (not null)
        if (baseWaitField != null) {
            try {
                String text = baseWaitField.getText();
                if (text != null && !text.isEmpty()) {
                    config.baseWaitTime = Long.parseLong(text);
                }
            } catch (NumberFormatException e) {
                AbyssalFishing.LOGGER.warn("Invalid baseWaitTime: " + baseWaitField.getText());
            }
        }
        if (maxWaitField != null) {
            try {
                String text = maxWaitField.getText();
                if (text != null && !text.isEmpty()) {
                    config.maxWaitTime = Long.parseLong(text);
                }
            } catch (NumberFormatException e) {
                AbyssalFishing.LOGGER.warn("Invalid maxWaitTime: " + maxWaitField.getText());
            }
        }
        if (reactionField != null) {
            try {
                String text = reactionField.getText();
                if (text != null && !text.isEmpty()) {
                    config.reactionTime = Long.parseLong(text);
                }
            } catch (NumberFormatException e) {
                AbyssalFishing.LOGGER.warn("Invalid reactionTime: " + reactionField.getText());
            }
        }
        if (cooldownField != null) {
            try {
                String text = cooldownField.getText();
                if (text != null && !text.isEmpty()) {
                    config.baseCooldown = Long.parseLong(text);
                }
            } catch (NumberFormatException e) {
                AbyssalFishing.LOGGER.warn("Invalid baseCooldown: " + cooldownField.getText());
            }
        }
        if (hudDurationField != null) {
            try {
                String text = hudDurationField.getText();
                if (text != null && !text.isEmpty()) {
                    config.hudMessageDuration = Long.parseLong(text);
                }
            } catch (NumberFormatException e) {
                AbyssalFishing.LOGGER.warn("Invalid hudMessageDuration: " + hudDurationField.getText());
            }
        }
        if (emergencyHealthField != null) {
            try {
                String text = emergencyHealthField.getText();
                if (text != null && !text.isEmpty()) {
                    config.emergencyHealthThreshold = Integer.parseInt(text);
                }
            } catch (NumberFormatException e) {
                AbyssalFishing.LOGGER.warn("Invalid emergencyHealthThreshold: " + emergencyHealthField.getText());
            }
        }
        if (detectionDelayField != null) {
            try {
                String text = detectionDelayField.getText();
                if (text != null && !text.isEmpty()) {
                    // Store detection delay if needed (currently not in config, but safe to parse)
                }
            } catch (NumberFormatException e) {
                AbyssalFishing.LOGGER.warn("Invalid detectionDelay: " + detectionDelayField.getText());
            }
        }
        
        // Save toggle states (they are always updated when toggled, so no need to save here)
    }
    
    private String formatTime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes % 60, seconds % 60);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds % 60);
        } else {
            return String.format("%ds", seconds);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
