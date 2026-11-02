package com.shinobiextras.jutsu;

import com.shinobiextras.Bridge;
import javax.annotation.Nullable;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** O que cada jutsu extra faz (lado servidor). Retorna true se foi usado (cobra recarga e chakra). */
public final class JutsuActions {
    private JutsuActions() {}

    static boolean run(ExtraJutsu j, ServerPlayer p) {
        return switch (j) {
            case KIRIGAKURE -> kirigakure(p);
            case NIKUDAN_SENSHA -> charge(p, 1.6D, 14, 10.0F, 1.2D, 0, 60, 2);
            case GATSUUGA -> charge(p, 1.9D, 18, 5.0F, 0.3D, 3, 60, 1);
            case SHIKOTSUMYAKU -> shikotsumyaku(p);
            case KIKAICHU -> kikaichu(p);
            case BAKUHA -> bakuha(p);
            case SAMEHADA -> samehada(p);
            case CHIDORI_NAGASHI -> chidoriNagashi(p);
            case SHUNSHIN -> shunshin(p);
        };
    }

    // ------------------------------------------------------------------ utilidades

    /** Pode ser atingido? (nunca o proprio usuario, espectadores, criativo, armor stands). */
    static boolean canHarm(LivingEntity e, @Nullable Player caster) {
        if (e == caster || !e.isAlive() || e.isSpectator() || e instanceof ArmorStand) return false;
        return !(e instanceof Player pl && pl.isCreative());
    }

    static void msg(ServerPlayer p, String key) {
        p.displayClientMessage(Component.translatable("message.shinobiextras." + key), true);
    }

    /** Entidade na mira, com auxilio de mira (cone estreito) se nao acertou direto. */
    @Nullable
    static LivingEntity livingInSight(Player p, double range) {
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getViewVector(1.0F);
        Vec3 end = eye.add(look.scale(range));
        AABB box = p.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0D);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(p.level(), p, eye, end, box,
                e -> e instanceof LivingEntity le && canHarm(le, p) && e.isPickable());
        if (hit != null && hit.getEntity() instanceof LivingEntity target) {
            HitResult block = p.pick(range, 1.0F, false);
            if (block.getType() == HitResult.Type.BLOCK
                    && block.getLocation().distanceToSqr(eye) < hit.getLocation().distanceToSqr(eye)) {
                return null;
            }
            return target;
        }
        LivingEntity best = null;
        double bestScore = 0.97D;
        for (LivingEntity e : p.level().getEntitiesOfClass(LivingEntity.class, box.inflate(1.0D), x -> canHarm(x, p) && x.isPickable())) {
            Vec3 to = e.getEyePosition().subtract(eye);
            double dist = to.length();
            if (dist < 0.5D || dist > range) continue;
            double dot = to.normalize().dot(look);
            if (dot > bestScore && p.hasLineOfSight(e)) {
                bestScore = dot;
                best = e;
            }
        }
        return best;
    }

    /** Ponto de impacto: alvo na mira, ou o bloco para onde o jogador olha. null se nada. */
    @Nullable
    private static Vec3 aimPoint(ServerPlayer p, double range) {
        LivingEntity t = livingInSight(p, range);
        if (t != null) return t.position();
        HitResult hr = p.pick(range, 1.0F, false);
        if (hr instanceof BlockHitResult bhr && hr.getType() == HitResult.Type.BLOCK) {
            return Vec3.atCenterOf(bhr.getBlockPos().above()).subtract(0.0D, 0.5D, 0.0D);
        }
        return null;
    }

    // ------------------------------------------------------------------ jutsus

    private static boolean kirigakure(ServerPlayer p) {
        ServerLevel lv = (ServerLevel) p.level();
        JutsuScheduler.MIST.put(p.getUUID(), new JutsuScheduler.Mist(lv.dimension(), lv.getGameTime() + 300,
                p.getX(), p.getY(), p.getZ()));
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 300, 0));
        lv.sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 1.0D, p.getZ(), 120, 6.0D, 1.5D, 6.0D, 0.02D);
        lv.playSound(null, p.blockPosition(), SoundEvents.ILLUSIONER_PREPARE_BLINDNESS, SoundSource.PLAYERS, 1.0F, 0.8F);
        return true;
    }

    /** Nikudan Sensha / Gatsuuga: investida que fere quem estiver no caminho. */
    private static boolean charge(ServerPlayer p, double speed, int ticks, float dmg, double kb, int every,
                                  int resistTicks, int resistAmp) {
        ServerLevel lv = (ServerLevel) p.level();
        Vec3 look = p.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
        if (look.lengthSqr() < 1.0E-4D) look = new Vec3(0.0D, 0.0D, 1.0D);
        look = look.normalize();
        JutsuScheduler.CHARGES.put(p.getUUID(), new JutsuScheduler.Charge(lv.dimension(), ticks, look.x, look.z,
                speed, dmg, kb, every));
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, resistTicks, resistAmp));
        lv.playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0F, 0.6F);
        return true;
    }

    private static boolean shikotsumyaku(ServerPlayer p) {
        Vec3 c = aimPoint(p, 32.0D);
        if (c == null) {
            msg(p, "no_target");
            return false;
        }
        ServerLevel lv = (ServerLevel) p.level();
        for (LivingEntity e : lv.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(3.5D, 2.5D, 3.5D), x -> canHarm(x, p))) {
            e.hurt(lv.damageSources().playerAttack(p), 11.0F);
            e.setDeltaMovement(e.getDeltaMovement().add(0.0D, 0.9D, 0.0D));
            e.hurtMarked = true;
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
        }
        BlockParticleOption bone = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BONE_BLOCK.defaultBlockState());
        lv.sendParticles(bone, c.x, c.y + 0.5D, c.z, 80, 2.0D, 1.0D, 2.0D, 0.15D);
        lv.sendParticles(ParticleTypes.CRIT, c.x, c.y + 1.0D, c.z, 30, 2.0D, 1.0D, 2.0D, 0.2D);
        lv.playSound(null, p.blockPosition(), SoundEvents.BONE_BLOCK_BREAK, SoundSource.PLAYERS, 1.5F, 0.6F);
        return true;
    }

    private static boolean kikaichu(ServerPlayer p) {
        LivingEntity t = livingInSight(p, 24.0D);
        if (t == null) {
            msg(p, "no_target");
            return false;
        }
        ServerLevel lv = (ServerLevel) p.level();
        t.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 1));
        t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 0));
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 0));
        if (t instanceof ServerPlayer other) Bridge.addChakra(other, -300.0D);
        lv.sendParticles(ParticleTypes.SMOKE, t.getX(), t.getY() + 1.0D, t.getZ(), 60, 0.4D, 0.7D, 0.4D, 0.05D);
        lv.playSound(null, t.blockPosition(), SoundEvents.BEE_LOOP_AGGRESSIVE, SoundSource.PLAYERS, 1.5F, 0.8F);
        return true;
    }

    private static boolean bakuha(ServerPlayer p) {
        Vec3 c = aimPoint(p, 40.0D);
        if (c == null) {
            msg(p, "no_target");
            return false;
        }
        ServerLevel lv = (ServerLevel) p.level();
        JutsuScheduler.BOMBS.add(new JutsuScheduler.Bomb(lv.dimension(), lv.getGameTime() + 40, c.x, c.y, c.z, p.getUUID()));
        lv.sendParticles(ParticleTypes.FIREWORK, c.x, c.y + 0.5D, c.z, 30, 0.4D, 0.4D, 0.4D, 0.05D);
        lv.playSound(null, p.blockPosition(), SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    private static boolean samehada(ServerPlayer p) {
        LivingEntity t = livingInSight(p, 6.0D);
        if (t == null) {
            msg(p, "no_target_close");
            return false;
        }
        ServerLevel lv = (ServerLevel) p.level();
        t.hurt(lv.damageSources().playerAttack(p), 8.0F);
        p.heal(6.0F);
        if (t instanceof ServerPlayer other) {
            if (Bridge.addChakra(other, -500.0D)) Bridge.addChakra(p, 500.0D);
        }
        lv.sendParticles(ParticleTypes.SOUL, t.getX(), t.getY() + 1.0D, t.getZ(), 30, 0.3D, 0.6D, 0.3D, 0.1D);
        lv.sendParticles(ParticleTypes.SOUL, p.getX(), p.getY() + 1.0D, p.getZ(), 15, 0.3D, 0.6D, 0.3D, 0.05D);
        lv.playSound(null, t.blockPosition(), SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1.5F, 0.7F);
        return true;
    }

    private static boolean chidoriNagashi(ServerPlayer p) {
        ServerLevel lv = (ServerLevel) p.level();
        JutsuScheduler.AURAS.put(p.getUUID(), new JutsuScheduler.Aura(lv.dimension(), lv.getGameTime() + 160));
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 160, 0));
        lv.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.getX(), p.getY() + 1.0D, p.getZ(), 50, 0.6D, 1.0D, 0.6D, 0.3D);
        lv.playSound(null, p.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.5F, 1.6F);
        return true;
    }

    private static boolean shunshin(ServerPlayer p) {
        ServerLevel lv = (ServerLevel) p.level();
        Vec3 look = p.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
        if (look.lengthSqr() < 1.0E-4D) {
            msg(p, "no_space");
            return false;
        }
        look = look.normalize();
        double moved = 0.0D;
        for (double d = 0.5D; d <= 12.0D; d += 0.5D) {
            AABB box = p.getBoundingBox().move(look.x * d, 0.0D, look.z * d);
            if (!lv.noCollision(p, box)) break;
            moved = d;
        }
        if (moved < 1.5D) {
            msg(p, "no_space");
            return false;
        }
        lv.sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 1.0D, p.getZ(), 25, 0.3D, 0.6D, 0.3D, 0.03D);
        p.teleportTo(p.getX() + look.x * moved, p.getY(), p.getZ() + look.z * moved);
        p.fallDistance = 0.0F;
        lv.sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 1.0D, p.getZ(), 25, 0.3D, 0.6D, 0.3D, 0.03D);
        lv.playSound(null, p.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 1.7F);
        return true;
    }
}
