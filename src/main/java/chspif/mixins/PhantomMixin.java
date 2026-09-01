package chspif.mixins;

import chspif.ChspifSettings;
import chspif.PhantomFollowGoal;
import chspif.PhantomPetAccess;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(Phantom.class)
public abstract class PhantomMixin extends Mob implements PhantomPetAccess
{
    protected PhantomMixin(EntityType<? extends Mob> type, Level level)
    {
        super(type, level);
    }

    private static final float RIDE_SPEED = 0.6F;
    private static final int RIDE_SOUND_INTERVAL = 30;
    private static final int CALM_TICKS = 100;
    private static final int PET_PARTICLE_INTERVAL = 10;

    @Shadow
    private Vec3 moveTargetPoint;

    @Unique
    private UUID chspifOwnerUuid;
    @Unique
    private boolean chspifTamed;
    @Unique
    private boolean chspifFireImmune;
    @Unique
    private int chspifCalmTicks;

    @Override
    public boolean chspifPetMode()
    {
        return this.chspifTamed || this.isVehicle();
    }

    @Override
    public boolean chspifIsTamed()
    {
        return this.chspifTamed;
    }

    @Override
    public boolean chspifIsRidden()
    {
        return !this.getPassengers().isEmpty();
    }

    @Override
    public LivingEntity chspifGetOwner()
    {
        if (this.chspifOwnerUuid == null || this.level().isClientSide())
        {
            return null;
        }
        if (this.level() instanceof ServerLevel serverLevel)
        {
            return serverLevel.getPlayerByUUID(this.chspifOwnerUuid);
        }
        return null;
    }

    @Override
    public void chspifSetMoveTarget(Vec3 pos)
    {
        this.moveTargetPoint = pos;
    }

    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void chspifRegisterPetGoals(CallbackInfo ci)
    {
        this.goalSelector.addGoal(1, new PhantomFollowGoal(this));
    }

    @Inject(method = "canAttack", at = @At("HEAD"), cancellable = true)
    private void chspifSuppressAttack(ServerLevel level, LivingEntity target, TargetingConditions conditions, CallbackInfoReturnable<Boolean> cir)
    {
        if (this.chspifPetMode() || this.chspifCalmTicks > 0)
        {
            cir.setReturnValue(false);
        }
    }

    @Override
    public void chspifHandleInteract(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir)
    {
        if (!ChspifSettings.phantomPetTaming || this.level().isClientSide())
        {
            return;
        }
        ItemStack item = player.getItemInHand(hand);
        if (this.chspifTamed)
        {
            if (!player.getUUID().equals(this.chspifOwnerUuid))
            {
                return;
            }
            if (item.is(Items.ENCHANTED_GOLDEN_APPLE) && !this.chspifFireImmune)
            {
                item.consume(1, player);
                this.chspifFireImmune = true;
                this.chspifSpawnStateParticles();
                cir.setReturnValue(InteractionResult.SUCCESS);
                return;
            }
            if (item.is(Items.CHORUS_FRUIT) && this.getHealth() < this.getMaxHealth())
            {
                item.consume(1, player);
                this.heal(4.0F);
                cir.setReturnValue(InteractionResult.SUCCESS);
                return;
            }
            if (!item.isEmpty())
            {
                return;
            }
            this.ejectPassengers();
            if (player.startRiding(this, true, true))
            {
                this.navigation.stop();
                cir.setReturnValue(InteractionResult.SUCCESS);
            }
            else
            {
                cir.setReturnValue(InteractionResult.FAIL);
            }
        }
        else if (item.is(Items.CHORUS_FRUIT))
        {
            item.consume(1, player);
            this.chspifCalmTicks = CALM_TICKS;
            boolean success = this.random.nextInt(5) == 0;
            if (success)
            {
                this.chspifTamed = true;
                this.chspifOwnerUuid = player.getUUID();
                this.setTarget(null);
                this.navigation.stop();
                this.setPersistenceRequired();
            }
            else
            {
                this.setTarget(null);
            }
            this.chspifSpawnTamingParticles(success);
            cir.setReturnValue(InteractionResult.SUCCESS_SERVER);
        }
    }

    @Override
    public void chspifHandleFireImmunity(CallbackInfoReturnable<Boolean> cir)
    {
        if (this.chspifFireImmune)
        {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void chspifRideTravel(Vec3 input, CallbackInfo ci)
    {
        if (!this.chspifTamed)
        {
            return;
        }
        Player rider = null;
        for (Entity passenger : this.getPassengers())
        {
            if (passenger instanceof Player player && player.getUUID().equals(this.chspifOwnerUuid))
            {
                rider = player;
                break;
            }
        }
        if (rider == null)
        {
            return;
        }
        this.setYRot(rider.getYRot());
        this.setXRot(-rider.getXRot());
        this.yBodyRot = rider.getYRot();
        this.setYHeadRot(rider.getYRot());
        Vec3 look = rider.getLookAngle();
        double pitch = rider.getXRot() * (Math.PI / 180.0);
        float up = (float) -Math.sin(pitch);
        boolean parked = this.onGround() && up <= 0.05F;
        if (parked)
        {
            this.setDeltaMovement(Vec3.ZERO);
        }
        else
        {
            this.setDeltaMovement(new Vec3(look.x * RIDE_SPEED, look.y * RIDE_SPEED, look.z * RIDE_SPEED));
            this.move(MoverType.SELF, this.getDeltaMovement());
        }
        if (!parked && this.tickCount % RIDE_SOUND_INTERVAL == 0)
        {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 0.6F, 1.0F);
        }
        ci.cancel();
    }

    @Inject(method = "getAmbientSound", at = @At("HEAD"), cancellable = true)
    private void chspifSilenceAmbient(CallbackInfoReturnable<SoundEvent> cir)
    {
        if (this.chspifTamed)
        {
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "getHurtSound", at = @At("HEAD"), cancellable = true)
    private void chspifSilenceHurt(DamageSource source, CallbackInfoReturnable<SoundEvent> cir)
    {
        if (this.chspifTamed)
        {
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void chspifTick(CallbackInfo ci)
    {
        if (this.level().isClientSide())
        {
            return;
        }
        if (this.chspifCalmTicks > 0)
        {
            this.chspifCalmTicks--;
        }
        if (this.chspifTamed && this.tickCount % PET_PARTICLE_INTERVAL == 0)
        {
            this.chspifSpawnStateParticles();
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("HEAD"))
    private void chspifSave(ValueOutput output, CallbackInfo ci)
    {
        if (this.chspifOwnerUuid != null)
        {
            output.putString("ChspifOwner", this.chspifOwnerUuid.toString());
        }
        output.putBoolean("ChspifTamed", this.chspifTamed);
        output.putBoolean("ChspifFireImmune", this.chspifFireImmune);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("HEAD"))
    private void chspifLoad(ValueInput input, CallbackInfo ci)
    {
        this.chspifOwnerUuid = input.getString("ChspifOwner").map(UUID::fromString).orElse(null);
        this.chspifTamed = input.getBooleanOr("ChspifTamed", false);
        this.chspifFireImmune = input.getBooleanOr("ChspifFireImmune", false);
        if (this.chspifTamed)
        {
            this.setPersistenceRequired();
        }
    }

    @Unique
    private void chspifSpawnTamingParticles(boolean success)
    {
        ParticleOptions particle = success ? ParticleTypes.HEART : ParticleTypes.SMOKE;
        if (this.level() instanceof ServerLevel serverLevel)
        {
            serverLevel.sendParticles(particle, this.getX(), this.getY() + 0.5, this.getZ(), 7, 0.5, 0.5, 0.5, 0.02);
        }
    }

    @Unique
    private void chspifSpawnStateParticles()
    {
        ParticleOptions particle = this.chspifFireImmune ? ParticleTypes.DRIPPING_OBSIDIAN_TEAR : ParticleTypes.END_ROD;
        if (this.level() instanceof ServerLevel serverLevel)
        {
            serverLevel.sendParticles(particle, this.getX(), this.getY() + 0.5, this.getZ(), 2, 0.4, 0.4, 0.4, 0.02);
        }
    }
}
