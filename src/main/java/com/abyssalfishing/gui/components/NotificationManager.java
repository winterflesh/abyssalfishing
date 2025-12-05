package com.abyssalfishing.gui.components;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.MinecraftClient;
import java.util.ArrayList;
import java.util.List;

public class NotificationManager {
    private static final NotificationManager INSTANCE = new NotificationManager();
    private final List<Notification> notifications = new ArrayList<>();
    private static final int MAX_NOTIFICATIONS = 5;
    private static final int NOTIFICATION_WIDTH = 200;
    private static final int NOTIFICATION_HEIGHT = 30;
    private static final int NOTIFICATION_SPACING = 5;
    
    public static NotificationManager getInstance() {
        return INSTANCE;
    }
    
    public void addNotification(String message, NotificationType type) {
        notifications.add(new Notification(message, type, System.currentTimeMillis()));
        if (notifications.size() > MAX_NOTIFICATIONS) {
            notifications.remove(0);
        }
    }
    
    public void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        
        int startX = screenWidth - NOTIFICATION_WIDTH - 10;
        int startY = screenHeight - 100;
        
        long currentTime = System.currentTimeMillis();
        notifications.removeIf(n -> currentTime - n.timestamp > 3000); // Remove after 3 seconds
        
        for (int i = 0; i < notifications.size(); i++) {
            Notification notif = notifications.get(i);
            int y = startY - (i * (NOTIFICATION_HEIGHT + NOTIFICATION_SPACING));
            notif.render(context, startX, y, NOTIFICATION_WIDTH, NOTIFICATION_HEIGHT);
        }
    }
    
    private static class Notification {
        final String message;
        final NotificationType type;
        final long timestamp;
        
        Notification(String message, NotificationType type, long timestamp) {
            this.message = message;
            this.type = type;
            this.timestamp = timestamp;
        }
        
        void render(DrawContext context, int x, int y, int width, int height) {
            // Main background (matching HUD style)
            context.fill(x, y, x + width, y + height, 0xFF1A1A1A);
            
            // Title bar with type-specific accent color (matching HUD style)
            int accentColor = type == NotificationType.SUCCESS ? 0xFF2A4A2A :  // Subtle green
                             type == NotificationType.ERROR ? 0xFF4A2A2A :    // Subtle red
                             0xFF2A2A4A;  // Subtle blue for INFO
            context.fill(x, y, x + width, y + 3, accentColor);
            
            // Border (matching HUD style - subtle)
            context.drawBorder(x, y, width, height, 0xFF000000);
            
            // Text with type-specific color (matching Minecraft color codes)
            MinecraftClient client = MinecraftClient.getInstance();
            String displayText = message.length() > 30 ? message.substring(0, 27) + "..." : message;
            int textX = x + 6;
            int textY = y + (height - 8) / 2;
            
            // Use Minecraft color codes for text
            String colorCode = type == NotificationType.SUCCESS ? "§a" :  // Green
                              type == NotificationType.ERROR ? "§c" :  // Red
                              "§e";  // Yellow for INFO
            context.drawText(client.textRenderer, net.minecraft.text.Text.literal(colorCode + displayText), textX, textY, 0xFFFFFF, true);
        }
    }
    
    public enum NotificationType {
        SUCCESS, ERROR, INFO
    }
}

