package chspif.mixins;

import chspif.EnderDragonPetAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(EnderDragon.class)
public abstract class EnderDragonMixin extends Mob implements EnderDragonPetAccess {
    @Unique private boolean chspifOverworldPet;
    @Unique private boolean chspifTamed;
    @Unique private UUID chspifOwner;
    @Unique private BlockPos chspifHome;

    protected EnderDragonMixin(EntityType<? extends Mob> type, Level level) {
        super(type, level);
    }

    @Override public void chspifSetHome(BlockPos home) { this.chspifHome = home.immutable(); }
    @Override public void chspifSetOverworldPet(boolean value) { this.chspifOverworldPet = value; }

    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void chspifPreventPetDeath(ServerLevel level, DamageSource source, float amount,
            CallbackInfoReturnable<Boolean> cir) {
        if (!chspifOverworldPet || chspifTamed || getHealth() > amount) return;
        Entity attacker = source.getEntity();
        if (attacker instanceof Player player) {
            chspifTamed = true;
            chspifOwner = player.getUUID();
            setHealth(Math.max(1.0F, getMaxHealth() * 0.25F));
            setTarget(null);
            setPersistenceRequired();
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void chspifRide(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (!chspifTamed || level().isClientSide() || !player.getUUID().equals(chspifOwner)) return;
        if (player.getItemInHand(hand).isEmpty() && player.startRiding(this, true, true)) {
            cir.setReturnValue(InteractionResult.SUCCESS_SERVER);
        }
    }

    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void chspifTravel(Vec3 input, CallbackInfo ci) {
        if (!chspifTamed) return;
        Player rider = getPassengers().stream().filter(Player.class::isInstance).map(Player.class::cast).findFirst().orElse(null);
        if (rider == null) return;
        setYRot(rider.getYRot());
        setXRot(rider.getXRot());
        Vec3 look = rider.getLookAngle().scale(0.8);
        setDeltaMovement(look);
        move(MoverType.SELF, getDeltaMovement());
        ci.cancel();
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void chspifPetTick(CallbackInfo ci) {
        if (chspifTamed) {
            setPersistenceRequired();
            if (getPassengers().isEmpty() && tickCount % 40 == 0) {
            }
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("HEAD"))
    private void chspifSave(ValueOutput output, CallbackInfo ci) {
        output.putBoolean("ChspifOverworldPet", chspifOverworldPet);
        output.putBoolean("ChspifTamed", chspifTamed);
        if (chspifOwner != null) output.putString("ChspifOwner", chspifOwner.toString());
        if (chspifHome != null) output.putLong("ChspifHome", chspifHome.asLong());
    }

    @Inject(method = "readAdditionalSaveData", at = @At("HEAD"))
    private void chspifLoad(ValueInput input, CallbackInfo ci) {
        chspifOverworldPet = input.getBooleanOr("ChspifOverworldPet", false);
        chspifTamed = input.getBooleanOr("ChspifTamed", false);
        chspifOwner = input.getString("ChspifOwner").map(UUID::fromString).orElse(null);
        chspifHome = input.getLong("ChspifHome").map(BlockPos::of).orElse(null);
    }
}
