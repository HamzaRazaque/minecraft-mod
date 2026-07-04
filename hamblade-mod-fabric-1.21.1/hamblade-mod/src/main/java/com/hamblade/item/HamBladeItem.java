package com.hamblade.item;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

import java.util.List;

public class HamBladeItem extends SwordItem {

    private static final String CHARGE_KEY = "HamBladeCharge";
    private static final int MAX_CHARGE = 3;
    private static final float ARMOR_PIERCE_DAMAGE = 6.0f;

    public HamBladeItem(ToolMaterial material, Item.Settings settings) {
        super(material, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!(attacker instanceof PlayerEntity player)) return super.postHit(stack, target, attacker);

        World world = attacker.getWorld();
        int charge = getCharge(stack);

        if (charge < MAX_CHARGE) {
            charge++;
            setCharge(stack, charge);
            player.sendMessage(
                Text.literal("⚡ HamBlade charged: " + getChargeBar(charge)).formatted(getChargeColor(charge)),
                true
            );
            world.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
                    SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS,
                    0.5f, 0.8f + (charge * 0.2f));
            spawnChargeParticles(world, target, charge);
        } else {
            charge = 0;
            setCharge(stack, charge);
            float newHealth = target.getHealth() - ARMOR_PIERCE_DAMAGE;
            target.setHealth(Math.max(0, newHealth));
            if (newHealth <= 0) target.kill();

            player.sendMessage(
                Text.literal("💥 FULL CHARGE! 3 Hearts of armor-piercing damage!").formatted(Formatting.RED),
                true
            );
            world.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
                    SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 0.6f, 1.5f);
            world.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
                    SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1.0f, 1.2f);
            spawnDischargeParticles(world, target);
        }

        return super.postHit(stack, target, attacker);
