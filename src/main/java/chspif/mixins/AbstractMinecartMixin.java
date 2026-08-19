package chspif.mixins;

import chspif.ChspifSettings;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractMinecart.class)
public class AbstractMinecartMixin
{
    @Inject(method = "tick", at = @At("HEAD"))
    private void chspifMinecartGlowing(CallbackInfo ci)
    {
        AbstractMinecart self = (AbstractMinecart) (Object) this;
        if (self.hasGlowingTag() != ChspifSettings.glowingMinecarts)
        {
            self.setGlowingTag(ChspifSettings.glowingMinecarts);
        }
    }
}
