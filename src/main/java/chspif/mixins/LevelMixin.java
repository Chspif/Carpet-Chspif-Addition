package chspif.mixins;

import chspif.EntityMsptSampler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Level.class)
public class LevelMixin
{
    @Redirect(method = "tickBlockEntities",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/TickingBlockEntity;tick()V"))
    private void chspifTickBlockEntity(TickingBlockEntity ticker)
    {
        if (EntityMsptSampler.isSampling())
        {
            BlockPos pos = ticker.getPos();
            long start = System.nanoTime();
            ticker.tick();
            if (pos != null)
            {
                EntityMsptSampler.recordBlockEntity((Level) (Object) this, pos, (System.nanoTime() - start) / 1_000_000.0);
            }
        }
        else
        {
            ticker.tick();
        }
    }
}
