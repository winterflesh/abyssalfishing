package com.abyssalfishing.config;

import com.abyssalfishing.AbyssalFishing;
import com.abyssalfishing.gui.AbyssalFishingGUI;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class ModMenuIntegration implements ModMenuApi {
    
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new AbyssalFishingGUI(parent);
    }
}