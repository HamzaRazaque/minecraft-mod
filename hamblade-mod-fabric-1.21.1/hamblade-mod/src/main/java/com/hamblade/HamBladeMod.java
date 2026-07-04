package com.hamblade;

import com.hamblade.item.HamBladeItem;
import com.hamblade.item.ModItems;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HamBladeMod implements ModInitializer {
    public static final String MOD_ID = "hamblade";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("HamBlade Mod Initializing...");
        ModItems.registerItems();
        LOGGER.info("HamBlade Mod Loaded! The blade awaits.");
    }
}
