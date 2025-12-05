package com.abyssalfishing.gui.components;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.text.Text;

public class ToggleSwitch extends ClickableWidget {
    private boolean enabled;
    private final Runnable onToggle;
    
    public ToggleSwitch(int x, int y, int width, int height, boolean initialValue, Runnable onToggle) {
        super(x, y, width, height, Text.empty());
        this.enabled = initialValue;
        this.onToggle = onToggle;
    }
    
    @Override
    public void onClick(double mouseX, double mouseY) {
        this.enabled = !this.enabled;
        if (onToggle != null) {
            onToggle.run();
        }
    }
    
    protected boolean clicked(double mouseX, double mouseY) {
        return this.active && this.visible && mouseX >= (double)this.getX() && mouseY >= (double)this.getY() && 
               mouseX < (double)(this.getX() + this.width) && mouseY < (double)(this.getY() + this.height);
    }
    
    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        // Background
        int bgColor = enabled ? 0xFF4CAF50 : 0xFF757575;
        context.fill(getX(), getY(), getX() + width, getY() + height, 0xFF2A2A2A);
        context.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, bgColor);
        
        // Toggle circle
        int circleSize = height - 4;
        int circleX = enabled ? getX() + width - circleSize - 2 : getX() + 2;
        int circleY = getY() + 2;
        
        context.fill(circleX, circleY, circleX + circleSize, circleY + circleSize, 0xFFFFFFFF);
        
        // Border
        context.drawBorder(getX(), getY(), width, height, 0xFF000000);
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        builder.put(NarrationPart.TITLE, this.getMessage());
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}

