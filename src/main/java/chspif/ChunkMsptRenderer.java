package chspif;

import chspif.mixins.ChunkMapAccessor;
import chspif.mixins.DisplayAccessor;
import chspif.mixins.TextDisplayAccessor;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChunkMsptRenderer {
    private static final int UPDATE_INTERVAL = 20;
    private static boolean enabled = false;
    private static int tickCounter = 0;
    private static final Map<ResourceKey<Level>, Map<ChunkPos, Display.TextDisplay>> displays = new HashMap<>();

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        if (enabled != value) {
            enabled = value;
            if (!value) {
                EntityMsptSampler.reset();
            }
        }
        if (!value) {
            for (Map<ChunkPos, Display.TextDisplay> dim : displays.values()) {
                for (Display.TextDisplay display : dim.values()) {
                    display.discard();
                }
            }
            displays.clear();
            requestCleanup();
        }
        EntityMsptSampler.stopSampling();
    }

    private static boolean pendingCleanup = false;

    public static void requestCleanup() {
        pendingCleanup = true;
    }

    public static void cleanupTick(MinecraftServer server) {
        if (!pendingCleanup) {
            return;
        }
        pendingCleanup = false;
        for (ServerLevel level : server.getAllLevels()) {
            List<Display.TextDisplay> leftovers = new ArrayList<>();
            level.getEntities(EntityTypeTest.forClass(Display.TextDisplay.class), e -> true, leftovers,
                    Integer.MAX_VALUE);
            for (Display.TextDisplay display : leftovers) {
                Component name = display.getCustomName();
                if (name != null && "chspif_mspt".equals(name.getString())) {
                    display.discard();
                }
            }
        }
    }

    public static void printTopChunks(CommandSourceStack source, boolean fromOneShot) {
        if (source == null) {
            return;
        }
        ServerLevel level = source.getLevel();
        List<EntityMsptSampler.ChunkInfo> top = fromOneShot
                ? EntityMsptSampler.getOneshotTopChunks(level.dimension(), 10)
                : EntityMsptSampler.getLiveTopChunks(level.dimension(), 10);
        if (top.isEmpty()) {
            source.sendSuccess(() -> Component.literal("暂无采样数据"), false);
            return;
        }
        long avgNanos = level.getServer().getAverageTickTimeNanos();
        double avgMs = avgNanos / 1_000_000.0;
        for (int i = 0; i < top.size(); i++) {
            EntityMsptSampler.ChunkInfo info = top.get(i);
            int rank = i + 1;
            double pct = avgMs <= 0 ? 0 : info.total / avgMs * 100.0;
            source.sendSuccess(() -> Component.literal(
                    "第" + rank + "名 区块 (" + info.pos.x() + ", " + info.pos.z() + ") 总"
                            + String.format("%.2fms 实体%d 占tick%.1f%%", info.total, info.count, pct)),
                    false);
        }
    }

    public static void printRangeTotal(CommandSourceStack source, ChunkPos min, ChunkPos max) {
        if (source == null || min == null || max == null) {
            return;
        }
        ServerLevel level = source.getLevel();
        List<EntityMsptSampler.ChunkInfo> all = EntityMsptSampler.getOneshotTopChunks(level.dimension(),
                Integer.MAX_VALUE);
        double total = 0;
        long entitySum = 0;
        int count = 0;
        for (EntityMsptSampler.ChunkInfo info : all) {
            if (info.pos.x() >= min.x() && info.pos.x() <= max.x()
                    && info.pos.z() >= min.z() && info.pos.z() <= max.z()) {
                total += info.total;
                entitySum += info.count;
                count++;
            }
        }
        long avgNanos = level.getServer().getAverageTickTimeNanos();
        double avgMs = avgNanos / 1_000_000.0;
        double pct = avgMs <= 0 ? 0 : total / avgMs * 100.0;
        double finalTotal = total;
        int finalCount = count;
        long finalEntitySum = entitySum;
        source.sendSuccess(() -> Component.literal(
                "范围 (" + min.x() + "," + min.z() + ")~(" + max.x() + "," + max.z() + ") 总mspt "
                        + String.format("%.2fms 区块%d 实体%d 占tick%.1f%%", finalTotal, finalCount, finalEntitySum, pct)),
                false);
    }

    public static void tick(MinecraftServer server) {
        if (!enabled) {
            return;
        }
        if (++tickCounter % UPDATE_INTERVAL != 0) {
            return;
        }
        for (ServerLevel level : server.getAllLevels()) {
            renderLevel(level);
        }
    }

    private static void renderLevel(ServerLevel level) {
        ResourceKey<Level> dim = level.dimension();
        Long2ObjectLinkedOpenHashMap<ChunkHolder> visible = ((ChunkMapAccessor) (Object) level
                .getChunkSource().chunkMap).chspifGetVisibleChunkMap();
        Map<ChunkPos, Display.TextDisplay> dimDisplays = displays.computeIfAbsent(dim, k -> new HashMap<>());

        LongIterator it = visible.keySet().iterator();
        while (it.hasNext()) {
            long key = it.nextLong();
            ChunkPos pos = ChunkPos.unpack(key);
            if (!EntityMsptSampler.isInRange(pos)) {
                Display.TextDisplay old = dimDisplays.remove(pos);
                if (old != null) {
                    old.discard();
                }
                continue;
            }
            ChunkHolder holder = visible.get(key);
            if (holder == null || holder.getTickingChunk() == null) {
                Display.TextDisplay old = dimDisplays.remove(pos);
                if (old != null) {
                    old.discard();
                }
                continue;
            }
            Display.TextDisplay display = dimDisplays.get(pos);
            if (display == null || display.isRemoved()) {
                display = spawn(level, pos);
                dimDisplays.put(pos, display);
            }
            updatePosition(display, level, pos);
            updateText(display, pos);
        }

        dimDisplays.entrySet().removeIf(entry -> {
            if (!visible.containsKey(entry.getKey().pack()) || !EntityMsptSampler.isInRange(entry.getKey())) {
                entry.getValue().discard();
                return true;
            }
            return false;
        });
    }

    private static Display.TextDisplay spawn(ServerLevel level, ChunkPos pos) {
        Display.TextDisplay display = new Display.TextDisplay(EntityTypes.TEXT_DISPLAY, level);
        display.setPos(pos.getMiddleBlockX() + 0.5, targetY(level, pos), pos.getMiddleBlockZ() + 0.5);
        display.setCustomName(Component.literal("chspif_mspt"));
        ((DisplayAccessor) display).chspifSetBillboard(Display.BillboardConstraints.CENTER);
        TextDisplayAccessor accessor = (TextDisplayAccessor) display;
        accessor.chspifSetBackgroundColor(0);
        accessor.chspifSetTextOpacity((byte) -1);
        accessor.chspifSetLineWidth(400);
        level.addFreshEntity(display);
        return display;
    }

    private static double targetY(ServerLevel level, ChunkPos pos) {
        ServerPlayer nearest = null;
        double bestSq = Double.MAX_VALUE;
        for (ServerPlayer player : level.players()) {
            if (player.isRemoved()) {
                continue;
            }
            double dx = player.getX() - (pos.getMiddleBlockX() + 0.5);
            double dz = player.getZ() - (pos.getMiddleBlockZ() + 0.5);
            double sq = dx * dx + dz * dz;
            if (sq < bestSq) {
                bestSq = sq;
                nearest = player;
            }
        }
        return nearest == null ? 320 : nearest.getEyeY() - 1.5;
    }

    private static void updatePosition(Display.TextDisplay display, ServerLevel level, ChunkPos pos) {
        display.setPos(pos.getMiddleBlockX() + 0.5, targetY(level, pos), pos.getMiddleBlockZ() + 0.5);
    }

    private static void updateText(Display.TextDisplay display, ChunkPos pos) {
        EntityMsptSampler.ChunkInfo info = EntityMsptSampler.getChunkInfo(display.level().dimension(), pos);
        if (info == null) {
            return;
        }
        String text = String.format(
                "实体数%d 总%.2fms\n实体%.2f 方块实体%.2f\n方块计划刻%.2f 流体计划刻%.2f\n随机刻%.2f 方块事件%.2f 邻接更新%.2f\n雷击%.2f 生物生成%.2f",
                info.count, info.total, info.entity, info.blockEntity, info.blockTick, info.fluidTick,
                info.randomTick, info.blockEvent, info.neighborUpdate, info.thunder, info.spawning);
        ((TextDisplayAccessor) display).chspifSetText(Component.literal(text));
    }
}
