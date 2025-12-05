package com.abyssalfishing.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.entry.RegistryEntry;
import com.abyssalfishing.AbyssalFishing;

import java.util.ArrayList;
import java.util.List;

public class InventoryUtils {
    
    public static boolean hasBaitInInventory(MinecraftClient client) {
        if (client.player == null) return false;
        
        PlayerInventory inventory = client.player.getInventory();
        
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (isBaitItem(stack)) {
                return true;
            }
        }
        return false;
    }
    
    public static boolean isBaitItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        
        String itemName = stack.getName().getString().toLowerCase();
        
        return itemName.contains("bait") ||
               itemName.contains("worm") ||
               stack.getItem().toString().toLowerCase().contains("carrot") ||
               stack.getItem().toString().toLowerCase().contains("fish") ||
               stack.getItem().toString().toLowerCase().contains("pufferfish") ||
               stack.getItem().toString().toLowerCase().contains("salmon") ||
               stack.getItem().toString().toLowerCase().contains("prismarine") ||
               stack.getItem().toString().toLowerCase().contains("glowstone") ||
               stack.getItem().toString().toLowerCase().contains("ink_sac") ||
               stack.getItem().toString().toLowerCase().contains("pumpkin_seeds") ||
               stack.getItem().toString().toLowerCase().contains("wheat_seeds");
    }
    
    public static boolean isRodDamaged(ItemStack rod, int threshold) {
        if (rod.isEmpty() || !rod.isDamageable()) {
            return false;
        }
        
        int currentDurability = rod.getMaxDamage() - rod.getDamage();
        int maxDurability = rod.getMaxDamage();
        double durabilityPercent = (double) currentDurability / maxDurability * 100;
        
        return durabilityPercent < threshold;
    }
    
    public static List<String> getEnchantments(ItemStack stack) {
        List<String> enchantments = new ArrayList<>();
        
        if (stack.isEmpty()) return enchantments;
        
        // In 1.21.5, enchantments are in ItemEnchantmentsComponent
        ItemEnchantmentsComponent enchantComponent = stack.get(DataComponentTypes.ENCHANTMENTS);
        if (enchantComponent == null) return enchantments;
        
        enchantComponent.getEnchantmentEntries().forEach(entry -> {
            RegistryEntry<Enchantment> enchRef = entry.getKey();
            int level = entry.getValue();
            // Use toString() to get enchantment name
            String enchantName = enchRef.toString() + " " + intToRoman(level);
            enchantments.add(enchantName);
        });
        
        return enchantments;
    }
    
    public static String intToRoman(int num) {
        String[] roman = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return num > 0 && num <= roman.length ? roman[num - 1] : String.valueOf(num);
    }
    
    public static int getEmptySlots(PlayerInventory inventory) {
        int emptySlots = 0;
        for (int i = 0; i < inventory.size(); i++) {
            if (inventory.getStack(i).isEmpty()) {
                emptySlots++;
            }
        }
        return emptySlots;
    }
    
    public static boolean hasSpaceInInventory(PlayerInventory inventory) {
        return getEmptySlots(inventory) > 0;
    }
    
    public static int countItems(PlayerInventory inventory, String itemName) {
        int count = 0;
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (!stack.isEmpty() && stack.getName().getString().toLowerCase().contains(itemName.toLowerCase())) {
                count += stack.getCount();
            }
        }
        return count;
    }
    
    public static ItemStack findBestFishingRod(PlayerInventory inventory) {
        ItemStack bestRod = ItemStack.EMPTY;
        int bestScore = 0;
        
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (!stack.isEmpty() && stack.getItem().toString().toLowerCase().contains("rod")) {
                int score = calculateRodScore(stack);
                if (score > bestScore) {
                    bestScore = score;
                    bestRod = stack;
                }
            }
        }
        
        return bestRod;
    }
    
    private static int calculateRodScore(ItemStack rod) {
        int score = 0;
        
        // Base score for being a fishing rod
        if (rod.getItem().toString().toLowerCase().contains("fishing")) {
            score += 100;
        }
        
        // Add score for enchantments
        ItemEnchantmentsComponent enchantComponent = rod.get(DataComponentTypes.ENCHANTMENTS);
        if (enchantComponent != null) {
            for (var entry : enchantComponent.getEnchantmentEntries()) {
                String enchantName = entry.getKey().toString().toLowerCase();
                int level = entry.getValue();
                
                if (enchantName.contains("lure")) {
                    score += level * 20;
                } else if (enchantName.contains("luck_of_the_sea") || enchantName.contains("luck of the sea")) {
                    score += level * 15;
                } else if (enchantName.contains("angler")) {
                    score += level * 10;
                } else if (enchantName.contains("caster")) {
                    score += level * 5;
                }
            }
        }
        
        // Add score for durability
        if (rod.isDamageable()) {
            int durabilityPercent = (rod.getMaxDamage() - rod.getDamage()) * 100 / rod.getMaxDamage();
            score += durabilityPercent / 10;
        }
        
        return score;
    }
    
    public static boolean isHotbarSlotEmpty(PlayerInventory inventory, int slot) {
        if (slot < 0 || slot >= 9) return false;
        return inventory.getStack(slot).isEmpty();
    }
    
    public static boolean moveItemToHotbar(MinecraftClient client, int sourceSlot, int hotbarSlot) {
        if (client.player == null || client.interactionManager == null) {
            return false;
        }
        
        try {
            // Pick up item - use null for inventory transaction
            client.interactionManager.clickSlot(
                client.player.currentScreenHandler.syncId,
                sourceSlot,
                0,
                net.minecraft.screen.slot.SlotActionType.PICKUP,
                client.player
            );
            
            // Place in hotbar
            client.interactionManager.clickSlot(
                client.player.currentScreenHandler.syncId,
                hotbarSlot,
                0,
                net.minecraft.screen.slot.SlotActionType.PICKUP,
                client.player
            );
            
            return true;
        } catch (Exception e) {
            AbyssalFishing.LOGGER.error("Failed to move item to hotbar", e);
            return false;
        }
    }
}