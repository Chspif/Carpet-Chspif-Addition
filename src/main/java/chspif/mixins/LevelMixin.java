package chspif.mixins;

import chspif.EntityMsptSampler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(Level.class)
public class LevelMixin
{
    private static final ThreadLocal<Long> CHSPIF_MS_START_ENTITY = new ThreadLocal<>();

    @Inject(method = "guardEntityTick", at = @At("HEAD"))
    private void chspifEntityMsStart(Consumer<Entity> tick, Entity entity, CallbackInfo ci)
    {
        if (EntityMsptSampler.isSampling())
        {
            CHSPIF_MS_START_ENTITY.set(System.nanoTime());
        }
    }

    @Inject(method = "guardEntityTick", at = @At("RETURN"))
    private void chspifEntityMsEnd(Consumer<Entity> tick, Entity entity, CallbackInfo ci)
    {
        if (EntityMsptSampler.isSampling())
        {
            Long start = CHSPIF_MS_START_ENTITY.get();
            CHSPIF_MS_START_ENTITY.remove();
            if (start != null)
            {
                EntityMsptSampler.recordEntity(entity, (System.nanoTime() - start) / 1_000_000.0);
            }
        }
    }

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
