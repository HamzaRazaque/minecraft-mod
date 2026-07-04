package com.hamblade.item;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

public class HamBladeItem extends SwordItem {

    private static final String CHARGE_KEY = "HamBladeCharge";
    private static final String COOLDOWN_KEY = "HamBladeCooldown";
    private static final int MAX_CHARGE = 3;
    private static final float ARMOR_PIERCE_DAMAGE = 6.0f;
    private static final float POWER_DAMAGE = 14.0f; // 7 hearts = 14 half hearts
    private static final int COOLDOWN_TICKS = 400; // 20 seconds
    private static final int STRENGTH_TICKS = 600; // 30 seconds

    public HamBladeItem(ToolMaterial material, Item.Settings settings) {
        super(material, settings);
    }

    // Shift + right click to activate power
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);

        if (player.isSneaking()) {
            long currentTime = world.getTime();
            long lastUsed = getLastUsedTime(stack);
            long elapsed = currentTime - lastUsed;

            if (lastUsed != 0 && elapsed < COOLDOWN_TICKS) {
                long remaining = (COOLDOWN_TICKS - elapsed) / 20;
                player.sendMessage(
                    Text.literal("⏳ HamBlade power on cooldown! " + remaining + "s remaining").formatted(Formatting.RED),
                    true
                );
                return TypedActionResult.fail(stack);
            }

            // Activate power!
            setLastUsedTime(stack, currentTime);

            // Deal 7 hearts true damage to all nearby enemies
            if (!world.isClient) {
                world.getOtherEntities(player, player.getBoundingBox().expand(5.0), e -> e instanceof LivingEntity)
                    .forEach(e -> {
                        LivingEntity living = (LivingEntity) e;
                        float newHealth = living.getHealth() - POWER_DAMAGE;
                        living.setHealth(Math.max(0, newHealth));
                        if (newHealth <= 0) living.kill();
                    });

                // Give Strength II for 30 seconds
                player.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.STRENGTH, STRENGTH_TICKS, 1, false, true, true
                ));

                // Particles and sound
                if (world instanceof ServerWorld serverWorld) {
                    serverWorld.spawnParticles(ParticleTypes.FLASH,
                            player.getX(), player.getY() + 1.0, player.getZ(),
                            1, 0, 0, 0, 0);
                    serverWorld.spawnParticles(ParticleTypes.ELECTRIC_SPARK,
                            player.getX(), player.getY() + 1.0, player.getZ(),
                            80, 1.5, 1.0, 1.5, 0.3);
                    serverWorld.spawnParticles(ParticleTypes.END_ROD,
                            player.getX(), player.getY() + 1.0, player.getZ(),
                            60, 1.5, 1.0, 1.5, 0.2);
                }

                world.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 1.0f, 1.0f);
                world.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ENTITY_WITHER_SHOOT, SoundCategory.PLAYERS, 1.0f, 1.5f);
            }

            player.sendMessage(
                Text.literal("⚡ HAMBLADE POWER! 7 Hearts damage + Strength II for 30s!").formatted(Formatting.GOLD),
                true
            );

            return TypedActionResult.success(stack);
        }

        return super.use(world, player, hand);
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
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);
        if (selected && !world.isClient && world instanceof ServerWorld serverWorld) {
            if (world.getTime() % 5 == 0) {
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

    private int getCharge(ItemStack stack) {
        NbtCompound nbt = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
        return nbt.getInt(CHARGE_KEY);
    }

    private void setCharge(ItemStack stack, int charge) {
        NbtCompound nbt = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
        nbt.putInt(CHARGE_KEY, charge);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }

    private long getLastUsedTime(ItemStack stack) {
        NbtCompound nbt = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
        return nbt.getLong(COOLDOWN_KEY);
    }

    private void setLastUsedTime(ItemStack stack, long time) {
        NbtCompound nbt = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
        nbt.putLong(COOLDOWN_KEY, time);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }

    private String getChargeBar(int charge) {
        return "[" + "█".repeat(charge) + "░".repeat(MAX_CHARGE - charge) + "]";
    }

    private Formatting getChargeColor(int charge) {
        return switch (charge) {
            case 1 -> Formatting.YELLOW;
            case 2 -> Formatting.GOLD;
            case 3 -> Formatting.RED;
            default -> Formatting.WHITE;
        };
    }

    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, RegistryWrapper.WrapperLookup registryLookup) {
        int charge = getCharge(stack);
        tooltip.add(Text.literal("Charge: " + getChargeBar(charge)).formatted(getChargeColor(charge)));
        tooltip.add(Text.literal("Hit 3 times to unleash armor-piercing damage!").formatted(Formatting.GRAY));
        tooltip.add(Text.literal("Full charge: 3 ❤ true damage").formatted(Formatting.RED));
        tooltip.add(Text.literal("Shift + Right Click: 7 ❤ damage + Strength II (30s)").formatted(Formatting.GOLD));
        tooltip.add(Text.literal("Power cooldown: 20 seconds").formatted(Formatting.GRAY));
    }
}
