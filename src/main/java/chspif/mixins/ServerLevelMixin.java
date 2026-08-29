package chspif.mixins;

import chspif.EntityMsptSampler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockEventData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.entity.EntityTickList;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.redstone.Orientation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Consumer;

@Mixin(ServerLevel.class)
public class ServerLevelMixin
{
    private static final ThreadLocal<Long> CHSPIF_MS_START_CHUNK = new ThreadLocal<>();
    private static final ThreadLocal<Long> CHSPIF_MS_START_EVENT = new ThreadLocal<>();
    private static final ThreadLocal<Long> CHSPIF_MS_START_BLOCK = new ThreadLocal<>();
    private static final ThreadLocal<Long> CHSPIF_MS_START_FLUID = new ThreadLocal<>();
    private static final ThreadLocal<Long> CHSPIF_MS_START_NEIGHBOR = new ThreadLocal<>();
    private static final ThreadLocal<Integer> CHSPIF_NEIGHBOR_DEPTH = ThreadLocal.withInitial(() -> 0);
    private static final ThreadLocal<Long> CHSPIF_MS_START_THUNDER = new ThreadLocal<>();

    @Inject(method = "tickChunk", at = @At("HEAD"))
    private void chspifChunkMsStart(LevelChunk chunk, int tickSpeed, CallbackInfo ci)
    {
        if (EntityMsptSampler.isSampling())
        {
            CHSPIF_MS_START_CHUNK.set(System.nanoTime());
        }
    }

    @Inject(method = "tickChunk", at = @At("RETURN"))
    private void chspifChunkMsEnd(LevelChunk chunk, int tickSpeed, CallbackInfo ci)
    {
        if (EntityMsptSampler.isSampling())
        {
            Long start = CHSPIF_MS_START_CHUNK.get();
            CHSPIF_MS_START_CHUNK.remove();
            if (start != null)
            {
                EntityMsptSampler.recordChunkRandom((ServerLevel) (Object) this, chunk.getPos(), (System.nanoTime() - start) / 1_000_000.0);
            }
        }
    }

    @Inject(method = "doBlockEvent", at = @At("HEAD"))
    private void chspifBlockEventMsStart(BlockEventData eventData, CallbackInfoReturnable<Boolean> cir)
    {
        if (EntityMsptSampler.isSampling())
        {
            CHSPIF_MS_START_EVENT.set(System.nanoTime());
        }
    }

    @Inject(method = "doBlockEvent", at = @At("RETURN"))
    private void chspifBlockEventMsEnd(BlockEventData eventData, CallbackInfoReturnable<Boolean> cir)
    {
        if (EntityMsptSampler.isSampling())
        {
            Long start = CHSPIF_MS_START_EVENT.get();
            CHSPIF_MS_START_EVENT.remove();
            if (start != null)
            {
                EntityMsptSampler.recordBlockEvent((ServerLevel) (Object) this, eventData.pos(), (System.nanoTime() - start) / 1_000_000.0);
            }
        }
    }

    @Inject(method = "tickBlock", at = @At("HEAD"))
    private void chspifBlockMsStart(BlockPos pos, Block block, CallbackInfo ci)
    {
        if (EntityMsptSampler.isSampling())
        {
            CHSPIF_MS_START_BLOCK.set(System.nanoTime());
        }
    }

    @Inject(method = "tickBlock", at = @At("RETURN"))
    private void chspifBlockMsEnd(BlockPos pos, Block block, CallbackInfo ci)
    {
        if (EntityMsptSampler.isSampling())
        {
            Long start = CHSPIF_MS_START_BLOCK.get();
            CHSPIF_MS_START_BLOCK.remove();
            if (start != null)
            {
                EntityMsptSampler.recordBlockTick((ServerLevel) (Object) this, pos, (System.nanoTime() - start) / 1_000_000.0);
            }
        }
    }

    @Inject(method = "tickFluid", at = @At("HEAD"))
    private void chspifFluidMsStart(BlockPos pos, Fluid fluid, CallbackInfo ci)
    {
        if (EntityMsptSampler.isSampling())
        {
            CHSPIF_MS_START_FLUID.set(System.nanoTime());
        }
    }

    @Inject(method = "tickFluid", at = @At("RETURN"))
    private void chspifFluidMsEnd(BlockPos pos, Fluid fluid, CallbackInfo ci)
    {
        if (EntityMsptSampler.isSampling())
        {
            Long start = CHSPIF_MS_START_FLUID.get();
            CHSPIF_MS_START_FLUID.remove();
            if (start != null)
            {
                EntityMsptSampler.recordFluidTick((ServerLevel) (Object) this, pos, (System.nanoTime() - start) / 1_000_000.0);
            }
        }
    }

    @Inject(method = "updateNeighborsAt(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;Lnet/minecraft/world/level/redstone/Orientation;)V",
            at = @At("HEAD"))
    private void chspifNeighborMsStart(BlockPos pos, Block sourceBlock, Orientation orientation, CallbackInfo ci)
    {
        if (EntityMsptSampler.isSampling())
        {
            if (CHSPIF_NEIGHBOR_DEPTH.get() == 0)
            {
                CHSPIF_MS_START_NEIGHBOR.set(System.nanoTime());
            }
            CHSPIF_NEIGHBOR_DEPTH.set(CHSPIF_NEIGHBOR_DEPTH.get() + 1);
        }
    }

    @Inject(method = "updateNeighborsAt(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;Lnet/minecraft/world/level/redstone/Orientation;)V",
            at = @At("RETURN"))
    private void chspifNeighborMsEnd(BlockPos pos, Block sourceBlock, Orientation orientation, CallbackInfo ci)
    {
        if (EntityMsptSampler.isSampling())
        {
            int depth = CHSPIF_NEIGHBOR_DEPTH.get() - 1;
            CHSPIF_NEIGHBOR_DEPTH.set(depth);
            if (depth == 0)
            {
                Long start = CHSPIF_MS_START_NEIGHBOR.get();
                CHSPIF_MS_START_NEIGHBOR.remove();
                if (start != null)
                {
                    EntityMsptSampler.recordNeighborUpdate((ServerLevel) (Object) this, pos, (System.nanoTime() - start) / 1_000_000.0);
                }
            }
        }
    }

    @Inject(method = "tickThunder", at = @At("HEAD"))
    private void chspifThunderMsStart(LevelChunk chunk, CallbackInfo ci)
    {
        if (EntityMsptSampler.isSampling())
        {
            CHSPIF_MS_START_THUNDER.set(System.nanoTime());
        }
    }

    @Inject(method = "tickThunder", at = @At("RETURN"))
    private void chspifThunderMsEnd(LevelChunk chunk, CallbackInfo ci)
    {
        if (EntityMsptSampler.isSampling())
        {
            Long start = CHSPIF_MS_START_THUNDER.get();
            CHSPIF_MS_START_THUNDER.remove();
            if (start != null)
            {
                EntityMsptSampler.recordThunder((ServerLevel) (Object) this, chunk.getPos(), (System.nanoTime() - start) / 1_000_000.0);
            }
        }
    }

    @Redirect(method = "tick(Ljava/util/function/BooleanSupplier;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/entity/EntityTickList;forEach(Ljava/util/function/Consumer;)V"))
    private void chspifEntityForEach(EntityTickList entityTickList, Consumer<Entity> original)
    {
        if (EntityMsptSampler.isSampling())
        {
            entityTickList.forEach(entity ->
            {
                long start = System.nanoTime();
                original.accept(entity);
                EntityMsptSampler.recordEntity(entity, (System.nanoTime() - start) / 1_000_000.0);
            });
        }
        else
        {
            entityTickList.forEach(original);
        }
    }
}
