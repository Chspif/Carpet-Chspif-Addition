package chspif.mixins;

import chspif.AvoidCinnabarGoal;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSkeleton.class)
public class AbstractSkeletonMixin
{
    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void chspifAddAvoidCinnabarGoal(CallbackInfo ci)
    {
        AbstractSkeleton self = (AbstractSkeleton) (Object) this;
        self.getGoalSelector().addGoal(2, new AvoidCinnabarGoal(self, 6.0F, 1.0, 1.2));
    }
}
