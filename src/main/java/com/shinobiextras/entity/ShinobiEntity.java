package com.shinobiextras.entity;

import com.shinobiextras.ModRegistry;
import com.shinobiextras.jutsu.ExtraJutsu;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Shinobis que o Narutoto nao tem: Zabuza, Kisame, Deidara, Kimimaro (hostis, largam o pergaminho
 * do jutsu deles) e Rock Lee (pacifico, so reage se for atacado).
 */
public class ShinobiEntity extends PathfinderMob {

    public enum Kind {
        //          id          vida    dano   veloc.  armadura ovo1      ovo2      hostil  pergaminho que larga
        ZABUZA("zabuza",       200.0F, 14.0F, 0.34D, 6.0D,  0xB8C0C8, 0x3A4A5A, true,  ExtraJutsu.KIRIGAKURE),
        KISAME("kisame",       260.0F, 16.0F, 0.32D, 8.0D,  0x3B6EA5, 0x1A1A2A, true,  ExtraJutsu.SAMEHADA),
        DEIDARA("deidara",     180.0F,  8.0F, 0.36D, 4.0D,  0xF2D14B, 0x1A1A1A, true,  ExtraJutsu.BAKUHA),
        KIMIMARO("kimimaro",   240.0F, 14.0F, 0.38D, 5.0D,  0xEDEDED, 0x6B8F71, true,  ExtraJutsu.SHIKOTSUMYAKU),
        ROCK_LEE("rock_lee",   140.0F, 10.0F, 0.46D, 2.0D,  0x2E8B3A, 0x1A1A1A, false, null);

        public final String id;
        public final float health;
        public final float damage;
        public final double speed;
        public final double armor;
        public final int eggPrimary;
        public final int eggSecondary;
        public final boolean hostile;
        @Nullable
        public final ExtraJutsu scroll;

        Kind(String id, float health, float damage, double speed, double armor, int eggPrimary, int eggSecondary,
             boolean hostile, @Nullable ExtraJutsu scroll) {
            this.id = id;
            this.health = health;
            this.damage = damage;
            this.speed = speed;
            this.armor = armor;
            this.eggPrimary = eggPrimary;
            this.eggSecondary = eggSecondary;
            this.hostile = hostile;
            this.scroll = scroll;
        }
    }

    private final Kind kind;

    private int cdA;
    private int cdB;
    private int cdC;
    // Deidara: bomba de argila armada
    private int bombTimer;
    private double bombX, bombY, bombZ;

    public ShinobiEntity(EntityType<? extends ShinobiEntity> type, Level level, Kind kind) {
        super(type, level);
        this.kind = kind;
        this.xpReward = 120;
    }

    public static AttributeSupplier.Builder createAttributes(Kind kind) {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, kind.health)
                .add(Attributes.MOVEMENT_SPEED, kind.speed)
                .add(Attributes.ATTACK_DAMAGE, kind.damage)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5D)
                .add(Attributes.ARMOR, kind.armor);
    }

    public static boolean checkSpawn(EntityType<ShinobiEntity> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource random) {
        return Mob.checkMobSpawnRules(type, level, reason, pos, random) && level.getRawBrightness(pos, 0) > 8;
    }

    public Kind getKind() {
        return kind;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2D, true));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        // "kind" ainda e null aqui (construtor do Mob chama registerGoals): a condicao e avaliada so durante o jogo
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                e -> this.kind != null && this.kind.hostile));
    }

    private boolean canHarm(LivingEntity e) {
        if (e == this || e instanceof ShinobiEntity || !e.isAlive() || e.isSpectator() || e instanceof ArmorStand) return false;
        return !(e instanceof Player pl && pl.isCreative());
    }

    // ------------------------------------------------------------- habilidades

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (cdA > 0) cdA--;
        if (cdB > 0) cdB--;
        if (cdC > 0) cdC--;

        if (kind == Kind.DEIDARA && bombTimer > 0 && level() instanceof ServerLevel server) {
            if (--bombTimer == 0) detonate(server);
            else server.sendParticles(ParticleTypes.FIREWORK, bombX, bombY + 0.5D, bombZ, 2, 0.2D, 0.2D, 0.2D, 0.02D);
        }

        if (kind == Kind.ROCK_LEE) leeTick();

        LivingEntity target = getTarget();
        if (target == null || !target.isAlive() || !canHarm(target)) return;
        switch (kind) {
            case ZABUZA -> zabuzaTick(target);
            case KISAME -> kisameTick(target);
            case DEIDARA -> deidaraTick(target);
            case KIMIMARO -> kimimaroTick(target);
            default -> { }
        }
    }

    /** Zabuza: Kirigakure (cega quem esta perto) e Kubikiri (golpe pesado). */
    private void zabuzaTick(LivingEntity target) {
        ServerLevel server = (ServerLevel) level();
        double d2 = distanceToSqr(target);

        if (cdA <= 0 && d2 < 400.0D) {
            cdA = 400;
            for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(10.0D), this::canHarm)) {
                e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0));
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 0));
            }
            server.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 1.0D, getZ(), 150, 5.0D, 1.5D, 5.0D, 0.02D);
            playSound(SoundEvents.ILLUSIONER_PREPARE_BLINDNESS, 1.2F, 0.7F);
        }
        if (cdB <= 0 && d2 < 16.0D) {
            cdB = 100;
            target.hurt(damageSources().mobAttack(this), 12.0F);
            Vec3 push = target.position().subtract(position()).multiply(1.0D, 0.0D, 1.0D).normalize();
            target.setDeltaMovement(target.getDeltaMovement().add(push.x * 1.2D, 0.4D, push.z * 1.2D));
            target.hurtMarked = true;
            server.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + 1.0D, target.getZ(), 3, 0.3D, 0.3D, 0.3D, 0.0D);
            playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.2F, 0.6F);
        }
    }

    /** Kisame: Suiton (onda de agua) e Samehada (drena vida do alvo). */
    private void kisameTick(LivingEntity target) {
        ServerLevel server = (ServerLevel) level();
        double d2 = distanceToSqr(target);

        if (cdA <= 0 && d2 < 64.0D) {
            cdA = 140;
            for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(5.0D), this::canHarm)) {
                Vec3 push = e.position().subtract(position()).multiply(1.0D, 0.0D, 1.0D).normalize();
                e.hurt(damageSources().mobAttack(this), 7.0F);
                e.setDeltaMovement(e.getDeltaMovement().add(push.x * 1.4D, 0.5D, push.z * 1.4D));
                e.hurtMarked = true;
            }
            server.sendParticles(ParticleTypes.SPLASH, getX(), getY() + 1.0D, getZ(), 100, 2.5D, 0.7D, 2.5D, 0.2D);
            playSound(SoundEvents.PLAYER_SPLASH_HIGH_SPEED, 1.4F, 0.7F);
        }
        if (cdB <= 0 && d2 < 12.0D) {
            cdB = 90;
            target.hurt(damageSources().mobAttack(this), 8.0F);
            heal(6.0F);
            server.sendParticles(ParticleTypes.SOUL, target.getX(), target.getY() + 1.0D, target.getZ(), 25, 0.3D, 0.6D, 0.3D, 0.08D);
            playSound(SoundEvents.SOUL_ESCAPE, 1.2F, 0.7F);
        }
    }

    /** Deidara: planta uma bomba de argila onde o alvo esta; explode depois de 1,5 s (sem destruir blocos). */
    private void deidaraTick(LivingEntity target) {
        ServerLevel server = (ServerLevel) level();
        double d2 = distanceToSqr(target);
        if (cdA <= 0 && bombTimer <= 0 && d2 >= 9.0D && d2 <= 900.0D && hasLineOfSight(target)) {
            cdA = 120;
            bombTimer = 30;
            bombX = target.getX();
            bombY = target.getY();
            bombZ = target.getZ();
            server.sendParticles(ParticleTypes.FIREWORK, bombX, bombY + 0.5D, bombZ, 30, 0.4D, 0.4D, 0.4D, 0.05D);
            playSound(SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.2F, 1.0F);
        }
    }

    private void detonate(ServerLevel server) {
        Vec3 c = new Vec3(bombX, bombY, bombZ);
        for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(5.0D), this::canHarm)) {
            double dist = e.position().distanceTo(c);
            if (dist > 5.0D) continue;
            float dmg = (float) (14.0D * (1.0D - dist / 5.5D));
            e.hurt(damageSources().explosion(this, this), Math.max(dmg, 2.0F));
            Vec3 push = e.position().subtract(c).multiply(1.0D, 0.0D, 1.0D);
            if (push.lengthSqr() > 1.0E-4D) {
                push = push.normalize().scale(0.8D);
                e.setDeltaMovement(e.getDeltaMovement().add(push.x, 0.5D, push.z));
                e.hurtMarked = true;
            }
        }
        server.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y + 0.5D, c.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        server.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y + 0.5D, c.z, 10, 2.0D, 1.0D, 2.0D, 0.0D);
        server.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE, getSoundSource(), 3.0F, 1.0F);
    }

    /** Kimimaro: espinhos de osso saem do chao embaixo do alvo. */
    private void kimimaroTick(LivingEntity target) {
        ServerLevel server = (ServerLevel) level();
        double d2 = distanceToSqr(target);
        if (cdA <= 0 && d2 < 144.0D && hasLineOfSight(target)) {
            cdA = 100;
            Vec3 c = target.position();
            for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(3.0D, 2.0D, 3.0D), this::canHarm)) {
                e.hurt(damageSources().mobAttack(this), 10.0F);
                e.setDeltaMovement(e.getDeltaMovement().add(0.0D, 0.8D, 0.0D));
                e.hurtMarked = true;
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
            }
            BlockParticleOption bone = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BONE_BLOCK.defaultBlockState());
            server.sendParticles(bone, c.x, c.y + 0.5D, c.z, 70, 1.8D, 1.0D, 1.8D, 0.15D);
            playSound(SoundEvents.BONE_BLOCK_BREAK, 1.4F, 0.6F);
        }
    }

    /** Rock Lee: com pouca vida abre os portoes (forca e velocidade por 10 s). */
    private void leeTick() {
        if (cdC <= 0 && getHealth() < getMaxHealth() * 0.5F && level() instanceof ServerLevel server) {
            cdC = 800;
            addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 2));
            addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 2));
            server.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + 1.0D, getZ(), 40, 0.5D, 1.0D, 0.5D, 0.1D);
            playSound(SoundEvents.BEACON_POWER_SELECT, 1.2F, 1.4F);
        }
    }

    // ------------------------------------------------------------- vida / morte

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        if (kind.scroll != null) {
            this.spawnAtLocation(new ItemStack(ModRegistry.SCROLLS.get(kind.scroll).get()));
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!level().isClientSide && source.getEntity() instanceof Player) {
            ExperienceOrb.award((ServerLevel) level(), this.position(), this.xpReward);
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND && this.isAlive()) {
            if (!level().isClientSide) {
                player.displayClientMessage(
                        Component.translatable("entity.shinobiextras." + kind.id).append(": ")
                                .append(Component.translatable("dialog.shinobiextras." + kind.id)),
                        false);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PLAYER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PLAYER_DEATH;
    }
}
