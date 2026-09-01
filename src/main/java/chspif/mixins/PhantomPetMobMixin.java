package chspif.mixins;

import chspif.PhantomPetAccess;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public abstract class PhantomPetMobMixin
{
    @Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
    private void chspifMobInteract(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir)
    {
        if ((Object) this instanceof PhantomPetAccess pet)
        {
            pet.chspifHandleInteract(player, hand, cir);
        }
    }
}
