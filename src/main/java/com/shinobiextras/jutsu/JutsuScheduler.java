package com.shinobiextras.jutsu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

/** Efeitos que continuam depois do clique: nevoa, investidas, aura de raios e bombas de argila. */
public final class JutsuScheduler {
    private JutsuScheduler() {}

    public record Mist(ResourceKey<Level> dim, long end, double x, double y, double z) {}
    public record Aura(ResourceKey<Level> dim, long end) {}
    public record Bomb(ResourceKey<Level> dim, long end, double x, double y, double z, UUID owner) {}

    public static final class Charge {
        final ResourceKey<Level> dim;
        int ticks;
        int age;
        final double dx, dz, speed, kb;
        final float dmg;
        final int every;
        final Set<UUID> hit = new HashSet<>();

        public Charge(ResourceKey<Level> dim, int ticks, double dx, double dz, double speed, float dmg, double kb, int every) {
            this.dim = dim;
            this.ticks = ticks;
            this.dx = dx;
            this.dz = dz;
            this.speed = speed;
            this.dmg = dmg;
            this.kb = kb;
            this.every = every;
        }
    }

    static final Map<UUID, Mist> MIST = new HashMap<>();
    static final Map<UUID, Charge> CHARGES = new HashMap<>();
    static final Map<UUID, Aura> AURAS = new HashMap<>();
    static final List<Bomb> BOMBS = new ArrayList<>();

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        int tc = server.getTickCount();

        if (!CHARGES.isEmpty()) tickCharges(server);
        if (tc % 5 == 0 && !AURAS.isEmpty()) tickAuras(server);
        if (tc % 10 == 0 && !MIST.isEmpty()) tickMist(server);
        if (!BOMBS.isEmpty()) tickBombs(server);
    }

    // ------------------------------------------------------------ investidas
    private static void tickCharges(MinecraftServer server) {
        Iterator<Map.Entry<UUID, Charge>> it = CHARGES.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Charge> en = it.next();
            Charge c = en.getValue();
            ServerPlayer p = server.getPlayerList().getPlayer(en.getKey());
            if (p == null || !p.isAlive() || p.level().dimension() != c.dim || c.ticks-- <= 0) {
                it.remove();
                continue;
            }
            c.age++;
            p.setDeltaMovement(c.dx * c.speed, Math.max(p.getDeltaMovement().y, -0.2D), c.dz * c.speed);
            p.hurtMarked = true;
            p.fallDistance = 0.0F;

            ServerLevel lv = (ServerLevel) p.level();
            lv.sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 0.5D, p.getZ(), 3, 0.3D, 0.3D, 0.3D, 0.01D);
            AABB box = p.getBoundingBox().inflate(1.0D, 0.5D, 1.0D);
            for (LivingEntity t : lv.getEntitiesOfClass(LivingEntity.class, box, x -> JutsuActions.canHarm(x, p))) {
                boolean strike;
                if (c.every == 0) strike = c.hit.add(t.getUUID());
                else strike = c.age % c.every == 0;
                if (!strike) continue;
                t.invulnerableTime = 0;
                t.hurt(lv.damageSources().playerAttack(p), c.dmg);
                t.setDeltaMovement(t.getDeltaMovement().add(c.dx * c.kb, 0.3D, c.dz * c.kb));
                t.hurtMarked = true;
            }
        }
    }

    // ------------------------------------------------------ aura Chidori Nagashi
    private static void tickAuras(MinecraftServer server) {
        Iterator<Map.Entry<UUID, Aura>> it = AURAS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Aura> en = it.next();
            Aura a = en.getValue();
            ServerPlayer p = server.getPlayerList().getPlayer(en.getKey());
            if (p == null || !p.isAlive() || p.level().dimension() != a.dim() || p.level().getGameTime() >= a.end()) {
                it.remove();
                continue;
            }
            ServerLevel lv = (ServerLevel) p.level();
            lv.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.getX(), p.getY() + 1.0D, p.getZ(), 15, 0.5D, 0.9D, 0.5D, 0.2D);
            for (LivingEntity t : lv.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(3.5D), x -> JutsuActions.canHarm(x, p))) {
                t.invulnerableTime = 0;
                t.hurt(lv.damageSources().playerAttack(p), 3.0F);
                Vec3 push = t.position().subtract(p.position()).multiply(1.0D, 0.0D, 1.0D);
                if (push.lengthSqr() > 1.0E-4D) {
                    push = push.normalize().scale(0.35D);
                    t.setDeltaMovement(t.getDeltaMovement().add(push.x, 0.1D, push.z));
                    t.hurtMarked = true;
                }
            }
        }
    }

    // ------------------------------------------------------------ nevoa
    private static void tickMist(MinecraftServer server) {
        Iterator<Map.Entry<UUID, Mist>> it = MIST.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Mist> en = it.next();
            Mist m = en.getValue();
            ServerLevel lv = server.getLevel(m.dim());
            if (lv == null || lv.getGameTime() >= m.end()) {
                it.remove();
                continue;
            }
            ServerPlayer owner = server.getPlayerList().getPlayer(en.getKey());
            lv.sendParticles(ParticleTypes.CLOUD, m.x(), m.y() + 1.0D, m.z(), 60, 6.0D, 1.5D, 6.0D, 0.01D);
            AABB box = new AABB(m.x() - 10.0D, m.y() - 4.0D, m.z() - 10.0D, m.x() + 10.0D, m.y() + 6.0D, m.z() + 10.0D);
            for (LivingEntity t : lv.getEntitiesOfClass(LivingEntity.class, box, x -> JutsuActions.canHarm(x, owner))) {
                t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, false, false));
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, false));
                if (owner != null && t instanceof Mob mob && mob.getTarget() == owner && mob.distanceToSqr(owner) > 16.0D) {
                    mob.setTarget(null);
                }
            }
        }
    }

    // ------------------------------------------------------------ bombas de argila
    private static void tickBombs(MinecraftServer server) {
        Iterator<Bomb> it = BOMBS.iterator();
        while (it.hasNext()) {
            Bomb b = it.next();
            ServerLevel lv = server.getLevel(b.dim());
            if (lv == null) {
                it.remove();
                continue;
            }
            if (lv.getGameTime() < b.end()) {
                lv.sendParticles(ParticleTypes.FIREWORK, b.x(), b.y() + 0.5D, b.z(), 2, 0.2D, 0.2D, 0.2D, 0.02D);
                continue;
            }
            it.remove();
            ServerPlayer owner = server.getPlayerList().getPlayer(b.owner());
            Vec3 c = new Vec3(b.x(), b.y(), b.z());
            // sem dano em blocos de proposito: so fere seres vivos
            for (LivingEntity t : lv.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(6.0D), x -> JutsuActions.canHarm(x, owner))) {
                double dist = t.position().distanceTo(c);
                if (dist > 6.0D) continue;
                float dmg = (float) (16.0D * (1.0D - dist / 6.5D));
                t.hurt(lv.damageSources().explosion(owner, owner), Math.max(dmg, 2.0F));
                Vec3 push = t.position().subtract(c).multiply(1.0D, 0.0D, 1.0D);
                if (push.lengthSqr() > 1.0E-4D) {
                    push = push.normalize().scale(0.9D);
                    t.setDeltaMovement(t.getDeltaMovement().add(push.x, 0.5D, push.z));
                    t.hurtMarked = true;
                }
            }
            lv.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y + 0.5D, c.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            lv.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y + 0.5D, c.z, 10, 2.0D, 1.0D, 2.0D, 0.0D);
            lv.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 3.0F, 1.0F);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent e) {
        MIST.clear();
        CHARGES.clear();
        AURAS.clear();
        BOMBS.clear();
    }
}
