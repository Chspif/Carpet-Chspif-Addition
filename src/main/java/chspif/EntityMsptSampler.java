package chspif;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EntityMsptSampler
{
    private static final double ALPHA = 1.0 / 40.0;
    private static final long CLEAN_INTERVAL = 200;
    private static final long STALE_LIMIT = 100;
    private static final int ONE_SHOT_TICKS = 40;
    private static final int RANGE_RADIUS = 1;

    private static final Map<ResourceKey<Level>, Map<ChunkPos, ChunkSample>> CHUNKS = new HashMap<>();
    private static final Map<ResourceKey<Level>, Map<ChunkPos, ChunkSample>> ONE_SHOT_CHUNKS = new HashMap<>();
    private static long tick = 0;
    private static boolean oneShot = false;
    private static int oneShotTicksLeft = 0;
    private static CommandSourceStack oneShotRequester = null;
    private static boolean rangeMode = false;
    private static ChunkPos rangeMin = null;
    private static ChunkPos rangeMax = null;
    private static Set<ChunkPos> centers = Set.of();

    public static boolean isSampling()
    {
        return ChunkMsptRenderer.isEnabled() || oneShot;
    }

    public static boolean isInRange(ChunkPos pos)
    {
        return centers.contains(pos);
    }

    public static void updateCenters(MinecraftServer server)
    {
        Set<ChunkPos> set = new HashSet<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers())
        {
            if (player.isRemoved())
            {
                continue;
            }
            ChunkPos center = player.chunkPosition();
            for (int dx = -RANGE_RADIUS; dx <= RANGE_RADIUS; dx++)
            {
                for (int dz = -RANGE_RADIUS; dz <= RANGE_RADIUS; dz++)
                {
                    set.add(new ChunkPos(center.x() + dx, center.z() + dz));
                }
            }
        }
        centers = set;
    }

    public static void startOneShot(CommandSourceStack requester)
    {
        ONE_SHOT_CHUNKS.clear();
        oneShot = true;
        oneShotTicksLeft = ONE_SHOT_TICKS;
        oneShotRequester = requester;
        rangeMode = false;
        rangeMin = null;
        rangeMax = null;
    }

    public static void startOneShotRange(CommandSourceStack requester, ChunkPos a, ChunkPos b)
    {
        ONE_SHOT_CHUNKS.clear();
        oneShot = true;
        oneShotTicksLeft = ONE_SHOT_TICKS;
        oneShotRequester = requester;
        rangeMode = true;
        rangeMin = new ChunkPos(Math.min(a.x(), b.x()), Math.min(a.z(), b.z()));
        rangeMax = new ChunkPos(Math.max(a.x(), b.x()), Math.max(a.z(), b.z()));
    }

    public static void stopSampling()
    {
        oneShot = false;
        oneShotRequester = null;
        rangeMode = false;
        rangeMin = null;
        rangeMax = null;
    }

    private static boolean inCalcRange(ChunkPos pos)
    {
        return pos.x() >= rangeMin.x() && pos.x() <= rangeMax.x()
                && pos.z() >= rangeMin.z() && pos.z() <= rangeMax.z();
    }

    public static void recordEntity(Entity entity, double ms)
    {
        if (entity instanceof Display)
        {
            return;
        }
        accumulate(entity.level().dimension(), entity.chunkPosition(), ms, Source.ENTITY, true);
    }

    public static void recordBlockEntity(Level level, BlockPos pos, double ms)
    {
        accumulate(level.dimension(), ChunkPos.containing(pos), ms, Source.BLOCK_ENTITY, false);
    }

    public static void recordBlockTick(Level level, BlockPos pos, double ms)
    {
        accumulate(level.dimension(), ChunkPos.containing(pos), ms, Source.BLOCK_TICK, false);
    }

    public static void recordFluidTick(Level level, BlockPos pos, double ms)
    {
        accumulate(level.dimension(), ChunkPos.containing(pos), ms, Source.FLUID_TICK, false);
    }

    public static void recordBlockEvent(Level level, BlockPos pos, double ms)
    {
        accumulate(level.dimension(), ChunkPos.containing(pos), ms, Source.BLOCK_EVENT, false);
    }

    public static void recordChunkRandom(Level level, ChunkPos pos, double ms)
    {
        accumulate(level.dimension(), pos, ms, Source.RANDOM_TICK, false);
    }

    public static void recordNeighborUpdate(Level level, BlockPos pos, double ms)
    {
        accumulate(level.dimension(), ChunkPos.containing(pos), ms, Source.NEIGHBOR_UPDATE, false);
    }

    public static void recordThunder(Level level, ChunkPos pos, double ms)
    {
        accumulate(level.dimension(), pos, ms, Source.THUNDER, false);
    }

    public static void recordSpawning(Level level, ChunkPos pos, double ms)
    {
        accumulate(level.dimension(), pos, ms, Source.SPAWNING, false);
    }

    private static void accumulate(ResourceKey<Level> dim, ChunkPos pos, double ms, Source source, boolean isEntity)
    {
        if (ChunkMsptRenderer.isEnabled() && centers.contains(pos))
        {
            ChunkSample sample = CHUNKS.computeIfAbsent(dim, k -> new HashMap<>())
                    .computeIfAbsent(pos, k -> new ChunkSample());
            addSample(sample, source, ms, isEntity);
            sample.lastTick = tick;
        }
        if (oneShot)
        {
            if (!rangeMode || inCalcRange(pos))
            {
                ChunkSample sample = ONE_SHOT_CHUNKS.computeIfAbsent(dim, k -> new HashMap<>())
                        .computeIfAbsent(pos, k -> new ChunkSample());
                addSample(sample, source, ms, isEntity);
            }
        }
    }

    private static void addSample(ChunkSample sample, Source source, double ms, boolean isEntity)
    {
        switch (source)
        {
            case ENTITY -> sample.tickEntity += ms;
            case BLOCK_ENTITY -> sample.tickBlockEntity += ms;
            case BLOCK_TICK -> sample.tickBlockTick += ms;
            case FLUID_TICK -> sample.tickFluidTick += ms;
            case RANDOM_TICK -> sample.tickRandomTick += ms;
            case BLOCK_EVENT -> sample.tickBlockEvent += ms;
            case NEIGHBOR_UPDATE -> sample.tickNeighborUpdate += ms;
            case THUNDER -> sample.tickThunder += ms;
            case SPAWNING -> sample.tickSpawning += ms;
        }
        if (isEntity)
        {
            sample.tickEntityCount++;
        }
    }

    public static void tick()
    {
        tick++;
        updateEma(CHUNKS);
        if (oneShot)
        {
            updateEma(ONE_SHOT_CHUNKS);
            if (--oneShotTicksLeft <= 0)
            {
                CommandSourceStack requester = oneShotRequester;
                boolean isRange = rangeMode;
                ChunkPos rMin = rangeMin;
                ChunkPos rMax = rangeMax;
                oneShot = false;
                oneShotRequester = null;
                rangeMode = false;
                rangeMin = null;
                rangeMax = null;
                if (isRange)
                {
                    ChunkMsptRenderer.printRangeTotal(requester, rMin, rMax);
                }
                else
                {
                    ChunkMsptRenderer.printTopChunks(requester, true);
                }
                ONE_SHOT_CHUNKS.clear();
            }
        }
        if (tick % CLEAN_INTERVAL == 0)
        {
            for (Map<ChunkPos, ChunkSample> dim : CHUNKS.values())
            {
                dim.values().removeIf(sample -> tick - sample.lastTick > STALE_LIMIT);
            }
        }
    }

    private static void updateEma(Map<ResourceKey<Level>, Map<ChunkPos, ChunkSample>> data)
    {
        for (Map<ChunkPos, ChunkSample> dim : data.values())
        {
            for (ChunkSample sample : dim.values())
            {
                sample.emaEntity = sample.emaEntity * (1.0 - ALPHA) + sample.tickEntity * ALPHA;
                sample.emaBlockEntity = sample.emaBlockEntity * (1.0 - ALPHA) + sample.tickBlockEntity * ALPHA;
                sample.emaBlockTick = sample.emaBlockTick * (1.0 - ALPHA) + sample.tickBlockTick * ALPHA;
                sample.emaFluidTick = sample.emaFluidTick * (1.0 - ALPHA) + sample.tickFluidTick * ALPHA;
                sample.emaRandomTick = sample.emaRandomTick * (1.0 - ALPHA) + sample.tickRandomTick * ALPHA;
                sample.emaBlockEvent = sample.emaBlockEvent * (1.0 - ALPHA) + sample.tickBlockEvent * ALPHA;
                sample.emaNeighborUpdate = sample.emaNeighborUpdate * (1.0 - ALPHA) + sample.tickNeighborUpdate * ALPHA;
                sample.emaThunder = sample.emaThunder * (1.0 - ALPHA) + sample.tickThunder * ALPHA;
                sample.emaSpawning = sample.emaSpawning * (1.0 - ALPHA) + sample.tickSpawning * ALPHA;
                sample.emaEntityCount = sample.emaEntityCount * (1.0 - ALPHA) + sample.tickEntityCount * ALPHA;
                sample.tickEntity = 0;
                sample.tickBlockEntity = 0;
                sample.tickBlockTick = 0;
                sample.tickFluidTick = 0;
                sample.tickRandomTick = 0;
                sample.tickBlockEvent = 0;
                sample.tickNeighborUpdate = 0;
                sample.tickThunder = 0;
                sample.tickSpawning = 0;
                sample.tickEntityCount = 0;
            }
        }
    }

    public static ChunkInfo getChunkInfo(ResourceKey<Level> dim, ChunkPos pos)
    {
        Map<ChunkPos, ChunkSample> map = CHUNKS.get(dim);
        if (map == null)
        {
            return null;
        }
        ChunkSample sample = map.get(pos);
        if (sample == null)
        {
            return null;
        }
        return new ChunkInfo(pos, sample);
    }

    public static List<ChunkInfo> getLiveTopChunks(ResourceKey<Level> dim, int n)
    {
        return getTopChunks(CHUNKS, dim, n);
    }

    public static List<ChunkInfo> getOneshotTopChunks(ResourceKey<Level> dim, int n)
    {
        return getTopChunks(ONE_SHOT_CHUNKS, dim, n);
    }

    private static List<ChunkInfo> getTopChunks(Map<ResourceKey<Level>, Map<ChunkPos, ChunkSample>> data,
            ResourceKey<Level> dim, int n)
    {
        Map<ChunkPos, ChunkSample> map = data.get(dim);
        if (map == null || map.isEmpty())
        {
            return List.of();
        }
        List<Map.Entry<ChunkPos, ChunkSample>> list = new ArrayList<>(map.entrySet());
        list.sort((a, b) -> Double.compare(b.getValue().totalMspt(), a.getValue().totalMspt()));
        List<ChunkInfo> result = new ArrayList<>();
        for (int i = 0; i < Math.min(n, list.size()); i++)
        {
            result.add(new ChunkInfo(list.get(i).getKey(), list.get(i).getValue()));
        }
        return result;
    }

    public static void reset()
    {
        CHUNKS.clear();
        tick = 0;
    }

    private enum Source
    {
        ENTITY, BLOCK_ENTITY, BLOCK_TICK, FLUID_TICK, RANDOM_TICK, BLOCK_EVENT, NEIGHBOR_UPDATE, THUNDER, SPAWNING
    }

    public static class ChunkInfo
    {
        public final ChunkPos pos;
        public final double total;
        public final double entity;
        public final double blockEntity;
        public final double blockTick;
        public final double fluidTick;
        public final double randomTick;
        public final double blockEvent;
        public final double neighborUpdate;
        public final double thunder;
        public final double spawning;
        public final int count;

        public ChunkInfo(ChunkPos pos, ChunkSample sample)
        {
            this.pos = pos;
            this.entity = sample.emaEntity;
            this.blockEntity = sample.emaBlockEntity;
            this.blockTick = sample.emaBlockTick;
            this.fluidTick = sample.emaFluidTick;
            this.randomTick = sample.emaRandomTick;
            this.blockEvent = sample.emaBlockEvent;
            this.neighborUpdate = sample.emaNeighborUpdate;
            this.thunder = sample.emaThunder;
            this.spawning = sample.emaSpawning;
            this.total = this.entity + this.blockEntity + this.blockTick + this.fluidTick + this.randomTick
                    + this.blockEvent + this.neighborUpdate + this.thunder + this.spawning;
            this.count = (int) Math.round(sample.emaEntityCount);
        }
    }

    private static class ChunkSample
    {
        double emaEntity;
        double emaBlockEntity;
        double emaBlockTick;
        double emaFluidTick;
        double emaRandomTick;
        double emaBlockEvent;
        double emaNeighborUpdate;
        double emaThunder;
        double emaSpawning;
        double emaEntityCount;
        double tickEntity;
        double tickBlockEntity;
        double tickBlockTick;
        double tickFluidTick;
        double tickRandomTick;
        double tickBlockEvent;
        double tickNeighborUpdate;
        double tickThunder;
        double tickSpawning;
        int tickEntityCount;
        long lastTick;

        double totalMspt()
        {
            return emaEntity + emaBlockEntity + emaBlockTick + emaFluidTick + emaRandomTick + emaBlockEvent
                    + emaNeighborUpdate + emaThunder + emaSpawning;
        }
    }
}
