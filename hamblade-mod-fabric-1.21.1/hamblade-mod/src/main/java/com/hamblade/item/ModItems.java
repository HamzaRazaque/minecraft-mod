package com.hamblade.item;

import com.hamblade.HamBladeMod;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ToolMaterials;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModItems {

    public static final HamBladeItem HAM_BLADE = new HamBladeItem(
            ToolMaterials.NETHERITE,
            new Item.Settings()
                    .maxCount(1)
                    .rarity(net.minecraft.util.Rarity.EPIC)
    );

    public static void registerItems() {
        Registry.register(
                Registries.ITEM,
                Identifier.of(HamBladeMod.MOD_ID, "ham_blade"),
                HAM_BLADE
        );

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> {
            entries.add(HAM_BLADE);
        });

        HamBladeMod.LOGGER.info("Registered HamBlade items.");
    }
}
