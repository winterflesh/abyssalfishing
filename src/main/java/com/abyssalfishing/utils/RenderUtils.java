package com.abyssalfishing.utils;

import com.abyssalfishing.AbyssalFishing;
import com.abyssalfishing.config.AbyssalConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public class RenderUtils {
    private static final MinecraftClient client = MinecraftClient.getInstance();
    private static final AbyssalConfig config = AbyssalFishing.config;
    
    // HUD positioning
    private static int hudX = 10;
    private static int hudY = 10;
    
    public static void renderFishingHUD(DrawContext context) {
        if (!config.showHUD) return;
        if (client.player == null) return;
        if (AbyssalFishing.fishingManager == null) return;
        
        // Main HUD Panel (dark style)
        int panelWidth = 220;
        int panelHeight = calculateHUDHeight();
        drawDarkPanel(context, hudX, hudY, panelWidth, panelHeight, "AbyssalFishing");
        
        // Content
        int contentX = hudX + 8;
        int contentY = hudY + 22;
        int lineHeight = 12;
        
        // Status
        String status = AbyssalFishing.fishingManager.isActive() ? "§aActive" : "§cInactive";
        drawText(context, "Status: " + status, contentX, contentY, 0xFFFFFF);
        
        // State
        String state = AbyssalFishing.fishingManager.getCurrentState().toString();
        drawText(context, "State: §e" + state, contentX, contentY + lineHeight, 0xFFFFFF);
        
        // Statistics
        int totalCatches = AbyssalFishing.fishingManager.getTotalCatches();
        int seaCreatures = AbyssalFishing.fishingManager.getSeaCreaturesCaught();
        int consecutive = AbyssalFishing.fishingManager.getConsecutiveCatches();
        
        drawText(context, "§7Total: §a" + totalCatches, contentX, contentY + lineHeight * 2, 0xFFFFFF);
        drawText(context, "§7Creatures: §d" + seaCreatures, contentX, contentY + lineHeight * 3, 0xFFFFFF);
        
        // Catch rate
        long sessionTime = AbyssalFishing.fishingManager.getSessionTime();
        if (sessionTime > 0) {
            double hours = sessionTime / 3600000.0;
            if (hours > 0) {
                double catchRate = totalCatches / hours;
                drawText(context, String.format("§7Rate: §e%.1f/h", catchRate), contentX, contentY + lineHeight * 4, 0xFFFFFF);
                
                if (totalCatches > 0) {
                    double creaturePercent = (seaCreatures * 100.0) / totalCatches;
                    drawText(context, String.format("§7SC: §d%.1f%%", creaturePercent), contentX + 110, contentY + lineHeight * 4, 0xFFFFFF);
                }
            }
        }
        
        // Streak
        if (consecutive > 0) {
            drawText(context, "§7Streak: §e" + consecutive, contentX, contentY + lineHeight * 5, 0xFFFFFF);
        }
        
        // Success rate
        double successRate = AbyssalFishing.fishingManager.getSuccessRate();
        if (successRate > 0) {
            int failed = AbyssalFishing.fishingManager.getFailedCatches();
            drawText(context, "§7Success: §e" + String.format("%.1f%%", successRate) + 
                (failed > 0 ? " §7(" + failed + " failed)" : ""), contentX, contentY + lineHeight * 6, 0xFFFFFF);
        }
    }
    
    private static int calculateHUDHeight() {
        if (AbyssalFishing.fishingManager != null && AbyssalFishing.fishingManager.isActive()) {
            int baseHeight = 100;
            if (AbyssalFishing.fishingManager.getConsecutiveCatches() > 0) {
                baseHeight += 12;
            }
            return baseHeight;
        }
        return 80;
    }
    
    private static void drawDarkPanel(DrawContext context, int x, int y, int width, int height, String title) {
        // Main background (dark gray)
        context.fill(x, y, x + width, y + height, 0xFF1A1A1A);
        context.drawBorder(x, y, width, height, 0xFF000000);
        
        // Title bar
        context.fill(x, y, x + width, y + 18, 0xFF2A2A2A);
        context.drawText(client.textRenderer, title, x + 6, y + 5, 0xFFFFFF, true);
        
        // Separator line
        context.fill(x + 4, y + 18, x + width - 4, y + 19, 0xFF000000);
    }
    
    private static void drawText(DrawContext context, String text, int x, int y, int color) {
        context.drawText(client.textRenderer, Text.literal(text), x, y, color, true);
    }
}
