package chspif;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ChunkPos;

import java.util.Map;

public class ChspifExtension implements CarpetExtension {
    @Override
    public String version() {
        return "carpet-chspif";
    }

    @Override
    public void onGameStarted() {
        CarpetServer.settingsManager.parseSettingsClass(ChspifSettings.class);
    }

    @Override
    public Map<String, String> canHasTranslations(String lang) {
        return ChspifTranslations.getTranslations(lang);
    }

    @Override
    public void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher,
            CommandBuildContext commandBuildContext) {
        dispatcher.register(Commands.literal("send")
                .requires(ChspifSettings::canUseSend)
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    SharedMailBox.openFor(player);
                    return 1;
                }));

        dispatcher.register(Commands.literal("smallfix")
                .requires(ChspifSettings::canUseSmallFix)
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    AttributeInstance scale = player.getAttribute(Attributes.SCALE);
                    if (scale != null) {
                        if (scale.getBaseValue() == 1.0) {
                            scale.setBaseValue(0.15);
                        } else {
                            scale.setBaseValue(1.0);
                        }
                    }
                    return 1;
                }));

        dispatcher.register(Commands.literal("chunkloadinfo")
                .requires(ChspifSettings::canUseChunkLoadInfo)
                .then(Commands.argument("blockX", IntegerArgumentType.integer())
                        .then(Commands.argument("blockZ", IntegerArgumentType.integer())
                                .executes(context -> showChunkInfo(context.getSource(),
                                        IntegerArgumentType.getInteger(context, "blockX"),
                                        IntegerArgumentType.getInteger(context, "blockZ"))))));

        dispatcher.register(Commands.literal("chunkmsptinfo")
                .requires(ChspifSettings::canUseChunkMsptInfo)
                .then(Commands.literal("on")
                        .executes(context -> {
                            ChunkMsptRenderer.setEnabled(true);
                            context.getSource().sendSuccess(
                                    () -> Component.literal("区块 mspt 显示已开启"), false);
                            return 1;
                        }))
                .then(Commands.literal("off")
                        .executes(context -> {
                            ChunkMsptRenderer.setEnabled(false);
                            context.getSource().sendSuccess(
                                    () -> Component.literal("区块 mspt 显示已关闭"), false);
                            return 1;
                        }))
                .then(Commands.literal("status")
                        .executes(context -> {
                            EntityMsptSampler.startOneShot(context.getSource());
                            context.getSource().sendSuccess(
                                    () -> Component.literal("采样中"), false);
                            return 1;
                        }))
                .then(Commands.literal("clear")
                        .executes(context -> {
                            ChunkMsptRenderer.requestCleanup();
                            context.getSource().sendSuccess(
                                    () -> Component.literal("已清理"), false);
                            return 1;
                        }))
                .then(Commands.literal("calculate")
                        .then(Commands.argument("chunkX1", IntegerArgumentType.integer())
                                .then(Commands.argument("chunkZ1", IntegerArgumentType.integer())
                                        .then(Commands.argument("chunkX2", IntegerArgumentType.integer())
                                                .then(Commands.argument("chunkZ2", IntegerArgumentType.integer())
                                                        .executes(context -> {
                                                            ChunkPos a = new ChunkPos(
                                                                    IntegerArgumentType.getInteger(context, "chunkX1"),
                                                                    IntegerArgumentType.getInteger(context, "chunkZ1"));
                                                            ChunkPos b = new ChunkPos(
                                                                    IntegerArgumentType.getInteger(context, "chunkX2"),
                                                                    IntegerArgumentType.getInteger(context, "chunkZ2"));
                                                            EntityMsptSampler.startOneShotRange(context.getSource(), a,
                                                                    b);
                                                            context.getSource().sendSuccess(
                                                                    () -> Component.literal("采样中"), false);
                                                            return 1;
                                                        })))))));
    }

    private static int showChunkInfo(CommandSourceStack source, int blockX, int blockZ) {
        ServerLevel level = source.getLevel();
        int chunkX = SectionPos.blockToSectionCoord(blockX);
        int chunkZ = SectionPos.blockToSectionCoord(blockZ);
        ChunkPos pos = new ChunkPos(chunkX, chunkZ);
        String debug = level.getChunkSource().getChunkDebugData(pos);
        if ("null".equals(debug)) {
            source.sendSuccess(
                    () -> Component
                            .literal("坐标 (" + blockX + ", " + blockZ + ") 区块坐标 (" + chunkX + ", " + chunkZ + ") 未加载"),
                    false);
            return 1;
        }
        DistanceManager distanceManager = level.getChunkSource().chunkMap.getDistanceManager();
        int loadLevel = distanceManager.getChunkLevel(pos.pack(), false);
        int computeLevel = distanceManager.getChunkLevel(pos.pack(), true);
        source.sendSuccess(() -> Component.literal("坐标 (" + blockX + ", " + blockZ + ") 区块坐标 (" + chunkX + ", " + chunkZ
                + ") 加载等级 " + loadLevel + " 计算等级 " + computeLevel), false);
        return 1;
    }

    @Override
    public void onServerLoadedWorlds(MinecraftServer server) {
        ChunkMsptRenderer.requestCleanup();
    }

    @Override
    public void onTick(MinecraftServer server) {
        ChunkMsptRenderer.cleanupTick(server);
        if (EntityMsptSampler.isSampling()) {
            if (ChunkMsptRenderer.isEnabled()) {
                EntityMsptSampler.updateCenters(server);
            }
            EntityMsptSampler.tick();
        }
        if (ChunkMsptRenderer.isEnabled()) {
            ChunkMsptRenderer.tick(server);
        }
    }

    @Override
    public void onServerClosed(MinecraftServer server) {
        ChunkMsptRenderer.setEnabled(false);
        SharedMailBox.getInstance().onServerClosed();
    }
}
