package com.abyssalfishing.utils;

import com.abyssalfishing.AbyssalFishing;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.projectile.FishingBobberEntity;

public class DebugOverlay {
    private static final MinecraftClient client = MinecraftClient.getInstance();
    private static final int LINE_HEIGHT = 10;
    private static final int PADDING = 4;
    private static final int TEXT_COLOR = 0x00FF00; // Green text

    public static void render(DrawContext context) {
        if (client.player == null) return;
        if (AbyssalFishing.fishingManager == null) return;

        int x = 5;
        int y = 5;

        // Title
        drawString(context, "§6=== FISHING DEBUG ===", x, y);
        y += LINE_HEIGHT;

        // Fishing state
        drawString(context, "§eState: §r" + AbyssalFishing.fishingManager.getCurrentState(), x, y);
        y += LINE_HEIGHT;

        drawString(context, "§eActive: §r" + (AbyssalFishing.fishingManager.isActive() ? "§aYES" : "§cNO"), x, y);
        y += LINE_HEIGHT;

        // Bobber info
        FishingBobberEntity bobber = AbyssalFishing.fishingManager.getFishingBobber();
        if (bobber != null) {
            double velocity = bobber.getVelocity().lengthSquared();
            drawString(context, "§eBobber Velocity: §r" + String.format("%.6f", velocity), x, y);
            y += LINE_HEIGHT;

            double currentY = bobber.getY();
            drawString(context, "§eBobber Y: §r" + String.format("%.2f", currentY), x, y);
            y += LINE_HEIGHT;

            drawString(context, "§eY Velocity: §r" + String.format("%.4f", bobber.getVelocity().y), x, y);
            y += LINE_HEIGHT;

            // Settlement status
            String settlementStatus = getSettlementStatus();
            drawString(context, "§eSettled: §r" + settlementStatus, x, y);
            y += LINE_HEIGHT;

            // Detection threshold
            drawString(context, "§eThresholds: §rVel=0.001 | Y=0.15", x, y);
            y += LINE_HEIGHT;
        } else {
            drawString(context, "§eBobber: §cNOT FOUND", x, y);
            y += LINE_HEIGHT;
        }

        // Statistics
        y += LINE_HEIGHT;
        drawString(context, "§6=== STATS ===", x, y);
        y += LINE_HEIGHT;

        drawString(context, "§eCatches: §r" + AbyssalFishing.fishingManager.getTotalCatches(), x, y);
        y += LINE_HEIGHT;

        drawString(context, "§eConsecutive: §r" + AbyssalFishing.fishingManager.getConsecutiveCatches(), x, y);
        y += LINE_HEIGHT;
        
        drawString(context, "§eFailed: §r" + AbyssalFishing.fishingManager.getFailedCatches(), x, y);
        y += LINE_HEIGHT;
        
        double successRate = AbyssalFishing.fishingManager.getSuccessRate();
        drawString(context, "§eSuccess Rate: §r" + String.format("%.1f%%", successRate), x, y);
        y += LINE_HEIGHT;

        long sessionTime = AbyssalFishing.fishingManager.getSessionTime() / 1000;
        drawString(context, "§eSession Time: §r" + sessionTime + "s", x, y);
        y += LINE_HEIGHT;

        // Sea Creature Killer info
        if (AbyssalFishing.fishingManager.getCurrentState() == com.abyssalfishing.core.FishingManager.FishingState.KILLING_CREATURE) {
            y += LINE_HEIGHT;
            drawString(context, "§6=== SEA CREATURE ===", x, y);
            y += LINE_HEIGHT;
            
            com.abyssalfishing.features.SeaCreatureKiller killer = 
                AbyssalFishing.fishingManager.getSeaCreatureKiller();
            if (killer != null && killer.isKilling()) {
                float health = killer.getTargetHealth();
                float maxHealth = killer.getTargetMaxHealth();
                double distance = killer.getTargetDistance();
                drawString(context, "§eHP: §r" + String.format("%.1f/%.1f", health, maxHealth), x, y);
                y += LINE_HEIGHT;
                drawString(context, "§eDistance: §r" + String.format("%.2f", distance), x, y);
                y += LINE_HEIGHT;
                drawString(context, "§eAttacks: §r" + killer.getAttackCount(), x, y);
            }
        }

        // Control hints
        y += LINE_HEIGHT;
        drawString(context, "§7Press L to toggle debug", x, y);
    }

    private static String getSettlementStatus() {
        // Use actual settlement state from FishingManager
        FishingBobberEntity bobber = AbyssalFishing.fishingManager.getFishingBobber();
        if (bobber == null) return "§cN/A";

        boolean isSettled = AbyssalFishing.fishingManager.isBobberSettled();
        double velocity = bobber.getVelocity().lengthSquared();

        if (isSettled) {
            return "§aREADY (vel=" + String.format("%.6f", velocity) + ")";
        } else {
            return "§cSETTLING (vel=" + String.format("%.6f", velocity) + ")";
        }
    }

    private static void drawString(DrawContext context, String text, int x, int y) {
        context.drawText(client.textRenderer, text, x, y, TEXT_COLOR, false);
    }
}
