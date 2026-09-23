package chspif;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;

import java.util.List;

public class ChunkMsptRenderer
{
    public static void printTopChunks(ServerPlayer player)
    {
        if (player == null)
        {
            return;
        }
        ServerLevel level = (ServerLevel) player.level();
        List<EntityMsptSampler.ChunkInfo> top = EntityMsptSampler.getOneshotTopChunks(level.dimension(), 10);
        if (top.isEmpty())
        {
            player.sendSystemMessage(Component.literal("暂无采样数据"));
            return;
        }
        long avgNanos = level.getServer().getAverageTickTimeNanos();
        double avgMs = avgNanos / 1_000_000.0;
        DistanceManager distanceManager = level.getChunkSource().chunkMap.getDistanceManager();
        for (int i = 0; i < top.size(); i++)
        {
            EntityMsptSampler.ChunkInfo info = top.get(i);
            int rank = i + 1;
            double pct = avgMs <= 0 ? 0 : info.total / avgMs * 100.0;
            int loadLevel = distanceManager.getChunkLevel(ChunkPosBridge.key(info.pos), false);
            int computeLevel = distanceManager.getChunkLevel(ChunkPosBridge.key(info.pos), true);
            player.sendSystemMessage(Component.literal(
                    "第" + rank + "名 区块 (" + ChunkPosBridge.x(info.pos) + ", " + ChunkPosBridge.z(info.pos) + ") 总")
                    .append(Component.literal(String.format("%.2f", info.total)).withStyle(ChatFormatting.GREEN))
                    .append(Component.literal("ms 实体")
                            .append(Component.literal(String.valueOf(info.count)).withStyle(ChatFormatting.GREEN)))
                    .append(Component.literal(" 占tick"))
                    .append(Component.literal(String.format("%.1f", pct)).withStyle(ChatFormatting.GREEN))
                    .append(Component.literal("% 加载等级"))
                    .append(Component.literal(String.valueOf(loadLevel)).withStyle(ChatFormatting.GREEN))
                    .append(Component.literal(" 计算等级"))
                    .append(Component.literal(String.valueOf(computeLevel)).withStyle(ChatFormatting.GREEN)));
        }
    }

    public static void printRangeTotal(ServerPlayer player, ChunkPos min, ChunkPos max)
    {
        if (player == null || min == null || max == null)
        {
            return;
        }
        ServerLevel level = (ServerLevel) player.level();
        List<EntityMsptSampler.ChunkInfo> all = EntityMsptSampler.getOneshotTopChunks(level.dimension(),
                Integer.MAX_VALUE);
        double total = 0;
        double eu = 0;
        double tt = 0;
        double ct = 0;
        double bu = 0;
        double ms = 0;
        double be = 0;
        double te = 0;
        long entitySum = 0;
        int count = 0;
        for (EntityMsptSampler.ChunkInfo info : all)
        {
            if (ChunkPosBridge.x(info.pos) >= ChunkPosBridge.x(min) && ChunkPosBridge.x(info.pos) <= ChunkPosBridge.x(max)
                    && ChunkPosBridge.z(info.pos) >= ChunkPosBridge.z(min) && ChunkPosBridge.z(info.pos) <= ChunkPosBridge.z(max))
            {
                total += info.total;
                eu += info.entity;
                tt += info.blockTick + info.fluidTick;
                ct += info.randomTick + info.thunder;
                bu += info.neighborUpdate;
                ms += info.spawning;
                be += info.blockEvent;
                te += info.blockEntity;
                entitySum += info.count;
                count++;
            }
        }
        long avgNanos = level.getServer().getAverageTickTimeNanos();
        double avgMs = avgNanos / 1_000_000.0;
        double pct = avgMs <= 0 ? 0 : total / avgMs * 100.0;
        player.sendSystemMessage(Component.literal(
                "范围 X(" + ChunkPosBridge.x(min) + "~" + ChunkPosBridge.x(max) + ") Z(" + ChunkPosBridge.z(min) + "~" + ChunkPosBridge.z(max) + ") 总mspt ")
                .append(Component.literal(String.format("%.2f", total)).withStyle(ChatFormatting.GREEN))
                .append(Component.literal("ms 区块")
                        .append(Component.literal(String.valueOf(count)).withStyle(ChatFormatting.GREEN)))
                .append(Component.literal(" 实体")
                        .append(Component.literal(String.valueOf(entitySum)).withStyle(ChatFormatting.GREEN)))
                .append(Component.literal(" 占tick"))
                .append(Component.literal(String.format("%.1f", pct)).withStyle(ChatFormatting.GREEN))
                .append(Component.literal("%")));
        player.sendSystemMessage(triple("EU", eu, "TT", tt, "CT", ct));
        player.sendSystemMessage(quad("BU", bu, "MS", ms, "BE", be, "TE", te));
    }

    public static Component[] hudForPlayer(Player player)
    {
        ChunkPos pos = player.chunkPosition();
        EntityMsptSampler.ChunkInfo info = EntityMsptSampler.getChunkInfo(player.level().dimension(), pos);
        if (info == null)
        {
            return new Component[]{Component.literal("区块 (" + ChunkPosBridge.x(pos) + "," + ChunkPosBridge.z(pos) + ") 暂无数据")};
        }
        Component first = Component.literal("区块 (" + ChunkPosBridge.x(pos) + "," + ChunkPosBridge.z(pos) + ")").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(" 总").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(String.format(" %.2fms", info.total)).withStyle(ChatFormatting.GREEN))
                .append(Component.literal(" 实体").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(" " + info.count).withStyle(ChatFormatting.GREEN));
        return new Component[]{
                first,
                triple("EU", info.entity, "TT", info.blockTick + info.fluidTick,
                        "CT", info.randomTick + info.thunder),
                quad("BU", info.neighborUpdate, "MS", info.spawning,
                        "BE", info.blockEvent, "TE", info.blockEntity)
        };
    }

    private static MutableComponent quad(String l1, double v1, String l2, double v2, String l3, double v3, String l4,
            double v4)
    {
        return labeled(l1, v1).append(Component.literal("  ").withStyle(ChatFormatting.GRAY))
                .append(labeled(l2, v2)).append(Component.literal("  ").withStyle(ChatFormatting.GRAY))
                .append(labeled(l3, v3)).append(Component.literal("  ").withStyle(ChatFormatting.GRAY))
                .append(labeled(l4, v4));
    }

    private static MutableComponent triple(String l1, double v1, String l2, double v2, String l3, double v3)
    {
        return labeled(l1, v1).append(Component.literal("  ").withStyle(ChatFormatting.GRAY))
                .append(labeled(l2, v2)).append(Component.literal("  ").withStyle(ChatFormatting.GRAY))
                .append(labeled(l3, v3));
    }

    private static MutableComponent labeled(String label, double value)
    {
        return Component.literal(label + ": ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.format("%.2f", value)).withStyle(ChatFormatting.GREEN));
    }
}
