package chspif.mixins;

import chspif.EntityMsptSampler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(NaturalSpawner.class)
public class NaturalSpawnerMixin
{
    private static final ThreadLocal<Long> CHSPIF_MS_START_SPAWN = new ThreadLocal<>();

    @Inject(method = "spawnForChunk", at = @At("HEAD"))
    private static void chspifSpawnMsStart(ServerLevel level, LevelChunk chunk,
            NaturalSpawner.SpawnState state, List<MobCategory> spawningCategories, CallbackInfo ci)
    {
        if (EntityMsptSampler.isSampling())
        {
            CHSPIF_MS_START_SPAWN.set(System.nanoTime());
        }
    }

    @Inject(method = "spawnForChunk", at = @At("RETURN"))
    private static void chspifSpawnMsEnd(ServerLevel level, LevelChunk chunk,
            NaturalSpawner.SpawnState state, List<MobCategory> spawningCategories, CallbackInfo ci)
    {
        if (EntityMsptSampler.isSampling())
        {
            Long start = CHSPIF_MS_START_SPAWN.get();
            CHSPIF_MS_START_SPAWN.remove();
            if (start != null)
            {
                EntityMsptSampler.recordSpawning(level, chunk.getPos(), (System.nanoTime() - start) / 1_000_000.0);
            }
        }
    }
}
