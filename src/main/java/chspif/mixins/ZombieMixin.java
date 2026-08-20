package chspif.mixins;

import chspif.AvoidCinnabarGoal;
import net.minecraft.world.entity.monster.zombie.Zombie;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Zombie.class)
public class ZombieMixin
{
    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void chspifAddAvoidCinnabarGoal(CallbackInfo ci)
    {
        Zombie self = (Zombie) (Object) this;
        self.getGoalSelector().addGoal(2, new AvoidCinnabarGoal(self, 6.0F, 1.0, 1.2));
    }
}
