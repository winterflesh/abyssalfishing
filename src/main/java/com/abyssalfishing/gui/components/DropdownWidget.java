package com.abyssalfishing.gui.components;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.text.Text;
import java.util.List;
import java.util.ArrayList;
import java.util.function.Consumer;

public class DropdownWidget extends ClickableWidget {
    private final List<String> options;
    private int selectedIndex;
    private boolean expanded = false;
    private final Consumer<Integer> onSelectionChange;
    
    public DropdownWidget(int x, int y, int width, int height, List<String> options, int initialIndex, Consumer<Integer> onSelectionChange) {
        super(x, y, width, height, Text.empty());
        this.options = new ArrayList<>(options);
        this.selectedIndex = Math.max(0, Math.min(initialIndex, options.size() - 1));
        this.onSelectionChange = onSelectionChange;
    }
    
    @Override
    public void onClick(double mouseX, double mouseY) {
        if (expanded) {
            // Check if clicking on an option (expanded dropdown)
            int optionHeight = 14;
            int startY = getY() + height;
            int totalOptionsHeight = options.size() * optionHeight;
            
            // Check if click is within dropdown options area
            if (mouseX >= getX() && mouseX <= getX() + width && 
                mouseY >= startY && mouseY < startY + totalOptionsHeight) {
                
                int clickedIndex = (int) ((mouseY - startY) / optionHeight);
                if (clickedIndex >= 0 && clickedIndex < options.size()) {
                    selectedIndex = clickedIndex;
                    expanded = false;
                    if (onSelectionChange != null) {
                        onSelectionChange.accept(selectedIndex);
                    }
                    return;
                }
            }
            
            // If clicking on main button while expanded, close it
            if (mouseX >= getX() && mouseX <= getX() + width && 
                mouseY >= getY() && mouseY <= getY() + height) {
                expanded = false;
                return;
            }
            
            // Clicked outside - close dropdown
            expanded = false;
        } else {
            // Expand dropdown
            expanded = true;
        }
    }
    
    @Override
    protected boolean clicked(double mouseX, double mouseY) {
        if (!this.active || !this.visible) {
            return false;
        }
        
        if (expanded) {
            // Check if clicking on dropdown button or any option
            int optionHeight = 14;
            int startY = getY() + height;
            int dropdownHeight = options.size() * optionHeight;
            
            // Main dropdown button
            boolean clickedButton = mouseX >= (double)this.getX() && mouseY >= (double)this.getY() && 
                                    mouseX < (double)(this.getX() + this.width) && 
                                    mouseY < (double)(this.getY() + this.height);
            
            // Options area
            boolean clickedOption = mouseX >= (double)this.getX() && 
                                   mouseY >= (double)startY &&
                                   mouseX < (double)(this.getX() + this.width) &&
                                   mouseY < (double)(startY + dropdownHeight);
            
            return clickedButton || clickedOption;
        }
        // Not expanded - only check main button
        return mouseX >= (double)this.getX() && mouseY >= (double)this.getY() && 
               mouseX < (double)(this.getX() + this.width) && 
               mouseY < (double)(this.getY() + this.height);
    }
    
    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        // Main dropdown box
        context.fill(getX(), getY(), getX() + width, getY() + height, 0xFF2A2A2A);
        context.drawBorder(getX(), getY(), width, height, 0xFF000000);
        
        // Selected text
        String selected = options.get(selectedIndex);
        int textX = getX() + 4;
        int textY = getY() + (height - 8) / 2;
        context.drawText(MinecraftClient.getInstance().textRenderer, selected, textX, textY, 0xFFFFFF, false);
        
        // Arrow
        int arrowX = getX() + width - 12;
        int arrowY = getY() + (height - 4) / 2;
        if (expanded) {
            // Up arrow
            context.fill(arrowX, arrowY, arrowX + 4, arrowY + 1, 0xFFFFFFFF);
            context.fill(arrowX + 1, arrowY + 1, arrowX + 3, arrowY + 2, 0xFFFFFFFF);
            context.fill(arrowX + 2, arrowY + 2, arrowX + 2, arrowY + 2, 0xFFFFFFFF);
        } else {
            // Down arrow
            context.fill(arrowX + 2, arrowY, arrowX + 2, arrowY, 0xFFFFFFFF);
            context.fill(arrowX + 1, arrowY + 1, arrowX + 3, arrowY + 2, 0xFFFFFFFF);
            context.fill(arrowX, arrowY + 2, arrowX + 4, arrowY + 3, 0xFFFFFFFF);
        }
        
        // Expanded options - render on top layer
        if (expanded) {
            int optionHeight = 14;
            int startY = getY() + height;
            int dropdownHeight = options.size() * optionHeight;
            
            // Background with higher z-index effect (darker)
            context.fill(getX(), startY, getX() + width, startY + dropdownHeight, 0xFF1A1A1A);
            context.drawBorder(getX(), startY, width, dropdownHeight, 0xFF000000);
            
            // Options
            for (int i = 0; i < options.size(); i++) {
                int optionY = startY + i * optionHeight;
                boolean hovered = mouseX >= getX() && mouseX <= getX() + width &&
                                 mouseY >= optionY && mouseY < optionY + optionHeight;
                
                // Highlight hovered or selected option
                if (hovered) {
                    context.fill(getX() + 1, optionY, getX() + width - 1, optionY + optionHeight, 0xFF4A4A4A);
                } else if (i == selectedIndex) {
                    context.fill(getX() + 1, optionY, getX() + width - 1, optionY + optionHeight, 0xFF3A3A3A);
                }
                
                // Draw text with shadow for better visibility
                int textColor = i == selectedIndex ? 0xFF4CAF50 : 0xFFFFFF;
                context.drawText(MinecraftClient.getInstance().textRenderer, Text.literal(options.get(i)), 
                    getX() + 4, optionY + 2, textColor, true);
            }
        }
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        builder.put(NarrationPart.TITLE, this.getMessage());
    }
    
    public int getSelectedIndex() {
        return selectedIndex;
    }
    
    public String getSelectedOption() {
        return options.get(selectedIndex);
    }
    
    public boolean isExpanded() {
        return expanded;
    }
    
    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }
    
    public List<String> getOptions() {
        return new ArrayList<>(options);
    }
    
    public int getHeight() {
        return height;
    }
    
    public int getWidth() {
        return width;
    }
    
}

