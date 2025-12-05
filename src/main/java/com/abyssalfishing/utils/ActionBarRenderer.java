package com.abyssalfishing.utils;

import com.abyssalfishing.AbyssalFishing;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public class ActionBarRenderer {
    private static final MinecraftClient client = MinecraftClient.getInstance();
    
    public static void render(DrawContext context) {
        if (client.player == null) return;
        if (AbyssalFishing.fishingManager == null) return;

        String msg = AbyssalFishing.fishingManager.getActiveHudMessage();
        if (msg == null || msg.isEmpty()) return;

        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        int textWidth = client.textRenderer.getWidth(Text.literal(msg));
        int padding = 8;
        int boxWidth = textWidth + padding * 2;
        int boxHeight = 14 + padding;

        int x = (screenWidth - boxWidth) / 2;
        int y = screenHeight - 100; // higher above hotbar

        // Background with gradient effect
        context.fill(x - 3, y - 3, x + boxWidth + 3, y + boxHeight + 3, 0xAA000000);
        context.fill(x - 2, y - 2, x + boxWidth + 2, y + boxHeight + 2, 0x88000000);
        
        // Border
        context.drawBorder(x - 2, y - 2, boxWidth + 4, boxHeight + 4, 0xFF00FF00);
        
        // Draw text with shadow
        context.drawText(client.textRenderer, Text.literal(msg), x + padding, y + (padding/2), 0xFFFFFF, true);
    }
}
