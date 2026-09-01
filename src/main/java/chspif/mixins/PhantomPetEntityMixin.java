package chspif.mixins;

import chspif.PhantomPetAccess;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class PhantomPetEntityMixin
{
    @Inject(method = "fireImmune", at = @At("RETURN"), cancellable = true)
    private void chspifFireImmunity(CallbackInfoReturnable<Boolean> cir)
    {
        if ((Object) this instanceof PhantomPetAccess pet)
        {
            pet.chspifHandleFireImmunity(cir);
        }
    }
}
