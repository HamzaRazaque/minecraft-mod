package com.hamblade.item;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.Item;
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
import net.minecraft.client.item.TooltipContext;

public class HamBladeItem extends SwordItem {

    // NBT key for charge level (0-3)
    private static final String CHARGE_KEY = "HamBladeCharge";
    private static final int MAX_CHARGE = 3;
    // Armor-piercing damage in half-hearts (6 half-hearts = 3 hearts)
    private static final float ARMOR_PIERCE_DAMAGE = 6.0f;

    public HamBladeItem(ToolMaterial material, Item.Settings settings) {
        super(material, settings);
    }

    /**
     * Called when this sword hits an entity.
     * Charges up on each hit. On max charge, deals armor-piercing 3-heart damage.
     */
    @Override
    public boolean postHit(net.minecraft.item.ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!(attacker instanceof PlayerEntity player)) return super.postHit(stack, target, attacker);

        World world = attacker.getWorld();
        int charge = getCharge(stack);

        if (charge < MAX_CHARGE) {
            // Increase charge
            charge++;
            setCharge(stack, charge);
            player.sendMessage(
                Text.literal("⚡ HamBlade charged: " + getChargeBar(charge)).formatted(getChargeColor(charge)),
                true // action bar
            );

            // Play charge sound
            world.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
                    SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS,
                    0.5f, 0.8f + (charge * 0.2f));

            // Spawn lightsaber particles
            spawnChargeParticles(world, target, charge);

        } else {
            // FULLY CHARGED — deal armor-piercing damage
            charge = 0;
            setCharge(stack, charge);

            // Deal true damage (bypasses armor) via direct health reduction
            float currentHealth = target.getHealth();
            float newHealth = currentHealth - ARMOR_PIERCE_DAMAGE;

            target.setHealth(Math.max(0, newHealth));

            // Kill if health depleted
            if (newHealth <= 0) {
                target.kill();
            }

            player.sendMessage(
                Text.literal("💥 FULL CHARGE! 3 Hearts of armor-piercing damage!").formatted(Formatting.RED),
                true
            );

            // Epic discharge sound + particles
            world.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
                    SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 0.6f, 1.5f);

            world.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
                    SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1.0f, 1.2f);

            spawnDischargeParticles(world, target);
        }

        return super.postHit(stack, target, attacker);
    }

    /**
     * Tick the item to show ambient lightsaber particles when held.
     */
    @Override
    public void inventoryTick(net.minecraft.item.ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);

        if (selected && !world.isClient && world instanceof ServerWorld serverWorld) {
            if (world.getTime() % 5 == 0) {
                // Ambient glow particles around the player holding the blade
                serverWorld.spawnParticles(ParticleTypes.END_ROD,
                        entity.getX(), entity.getY() + 1.0, entity.getZ(),
                        2, 0.3, 0.3, 0.3, 0.02);
            }
        }
    }

    private void spawnChargeParticles(World world, LivingEntity target, int charge) {
        if (world instanceof ServerWorld serverWorld) {
            int count = charge * 5;
            serverWorld.spawnParticles(ParticleTypes.ELECTRIC_SPARK,
                    target.getX(), target.getY() + 1.0, target.getZ(),
                    count, 0.3, 0.3, 0.3, 0.1);
            serverWorld.spawnParticles(ParticleTypes.END_ROD,
                    target.getX(), target.getY() + 1.0, target.getZ(),
                    count, 0.2, 0.5, 0.2, 0.05);
        }
    }

    private void spawnDischargeParticles(World world, LivingEntity target) {
        if (world instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(ParticleTypes.FLASH,
                    target.getX(), target.getY() + 1.0, target.getZ(),
                    1, 0, 0, 0, 0);
            serverWorld.spawnParticles(ParticleTypes.ELECTRIC_SPARK,
                    target.getX(), target.getY() + 1.0, target.getZ(),
                    50, 0.5, 0.8, 0.5, 0.2);
            serverWorld.spawnParticles(ParticleTypes.END_ROD,
                    target.getX(), target.getY() + 1.5, target.getZ(),
                    30, 0.4, 0.6, 0.4, 0.15);
        }
    }

    private int getCharge(net.minecraft.item.ItemStack stack) {
        NbtCompound nbt = stack.getOrCreateNbt();
        return nbt.getInt(CHARGE_KEY);
    }

    private void setCharge(net.minecraft.item.ItemStack stack, int charge) {
        NbtCompound nbt = stack.getOrCreateNbt();
        nbt.putInt(CHARGE_KEY, charge);
    }

    private String getChargeBar(int charge) {
        String filled = "█".repeat(charge);
        String empty = "░".repeat(MAX_CHARGE - charge);
        return "[" + filled + empty + "]";
    }

    private Formatting getChargeColor(int charge) {
        return switch (charge) {
            case 1 -> Formatting.YELLOW;
            case 2 -> Formatting.GOLD;
            case 3 -> Formatting.RED;
            default -> Formatting.WHITE;
        };
    }

    @Override
    public void appendTooltip(net.minecraft.item.ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        int charge = getCharge(stack);
        tooltip.add(Text.literal("Charge: " + getChargeBar(charge)).formatted(getChargeColor(charge)));
        tooltip.add(Text.literal("Hit 3 times to unleash armor-piercing damage!").formatted(Formatting.GRAY));
        tooltip.add(Text.literal("Full charge: 3 ❤ true damage").formatted(Formatting.RED));
    }
}
