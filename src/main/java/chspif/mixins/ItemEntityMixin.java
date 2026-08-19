package chspif.mixins;

import chspif.ChspifSettings;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public class ItemEntityMixin
{
    @Inject(method = "tick", at = @At("HEAD"))
    private void chspifItemGlowing(CallbackInfo ci)
    {
        ItemEntity self = (ItemEntity) (Object) this;
        if (self.hasGlowingTag() != ChspifSettings.glowingItems)
        {
            self.setGlowingTag(ChspifSettings.glowingItems);
        }
    }
}
