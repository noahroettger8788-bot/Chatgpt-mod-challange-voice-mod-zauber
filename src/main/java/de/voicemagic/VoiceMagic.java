package de.voicemagic;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.SilverfishEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class VoiceMagic implements ModInitializer {
    public static final String MOD_ID = "voicemagic";
    public static final Identifier CAST_PACKET = new Identifier(MOD_ID, "cast");
    private static final List<RestoreTask> RESTORES = new ArrayList<>();

    @Override
    public void onInitialize() {
        ServerPlayNetworking.registerGlobalReceiver(CAST_PACKET, (server, player, handler, buf, responseSender) -> {
            String spell = buf.readString(64);
            server.execute(() -> Effects.apply(player, spell));
        });

        ServerTickEvents.END_WORLD_TICK.register(world -> {
            Iterator<RestoreTask> it = RESTORES.iterator();
            while (it.hasNext()) {
                RestoreTask task = it.next();
                if (task.world == world && world.getTime() >= task.restoreAt) {
                    if (world.getBlockState(task.pos).isAir()) {
                        world.setBlockState(task.pos, task.state);
                    }
                    it.remove();
                }
            }
        });
    }

    private record RestoreTask(ServerWorld world, BlockPos pos, BlockState state, long restoreAt) {}

    public static class Effects {
        public static void apply(ServerPlayerEntity p, String id) {
            ServerWorld w = p.getServerWorld();
            BlockPos feet = p.getBlockPos();
            switch (id) {
                case "digga" -> p.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 1200, 0));
                case "creeper" -> {
                    CreeperEntity creeper = new CreeperEntity(net.minecraft.entity.EntityType.CREEPER, w);
                    Vec3d behind = p.getPos().subtract(Vec3d.fromPolar(0, p.getYaw()).normalize().multiply(3));
                    creeper.refreshPositionAndAngles(behind.x, behind.y, behind.z, p.getYaw(), 0);
                    creeper.setCharged(true);
                    w.spawnEntity(creeper);
                }
                case "hilfe" -> {
                    for (int i = 0; i < 3; i++) {
                        ZombieEntity zombie = new ZombieEntity(net.minecraft.entity.EntityType.ZOMBIE, w);
                        zombie.refreshPositionAndAngles(p.getX() + (i - 1) * 1.5, p.getY(), p.getZ() + 2, p.getYaw(), 0);
                        w.spawnEntity(zombie);
                    }
                }
                case "curse" -> temporarilyReplace(w, feet.down(), Blocks.AIR.getDefaultState(), 40);
                case "launch" -> p.teleport(w, p.getX(), p.getY() + 15, p.getZ(), p.getYaw(), p.getPitch());
                case "fire" -> {
                    p.setOnFireFor(6);
                    temporarilyReplace(w, feet.down(), Blocks.MAGMA_BLOCK.getDefaultState(), 40);
                }
                case "dark" -> {
                    p.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 200, 0));
                    p.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 200, 0));
                }
                case "hunger" -> {
                    p.getHungerManager().setFoodLevel(6);
                    p.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 200, 0));
                }
                case "bro" -> p.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 400, 0));
                case "speed" -> {
                    p.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 200, 2));
                    p.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, 200, 1));
                }
                case "heal" -> {
                    if (p.experienceLevel >= 3) {
                        p.addExperienceLevels(-3);
                        p.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 100, 2));
                    } else {
                        p.sendMessage(Text.literal("Voice Magic: Du brauchst 3 Level."), true);
                    }
                }
                case "water" -> {
                    BlockPos waterPos = feet.down();
                    if (w.getBlockState(waterPos).isAir() || w.getBlockState(waterPos).isReplaceable()) {
                        w.setBlockState(waterPos, Blocks.WATER.getDefaultState());
                    }
                }
                case "light" -> {
                    p.damage(w.getDamageSources().magic(), 2.0F);
                    BlockPos torchPos = feet;
                    if (w.getBlockState(torchPos).isAir()) w.setBlockState(torchPos, Blocks.TORCH.getDefaultState());
                }
                case "invisible" -> {
                    p.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, 300, 0));
                    for (int slot = 0; slot < p.getInventory().armor.size(); slot++) {
                        ItemStack armor = p.getInventory().armor.get(slot);
                        if (!armor.isEmpty()) {
                            p.dropItem(armor.copy(), true, false);
                            p.getInventory().armor.set(slot, ItemStack.EMPTY);
                        }
                    }
                }
                default -> { return; }
            }
            w.playSound(null, p.getBlockPos(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.7F, 1.2F);
        }

        private static void temporarilyReplace(ServerWorld world, BlockPos pos, BlockState replacement, int ticks) {
            BlockState old = world.getBlockState(pos);
            world.setBlockState(pos, replacement);
            RESTORES.add(new RestoreTask(world, pos.toImmutable(), old, world.getTime() + ticks));
        }
    }
}
