package chspif;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import carpet.logging.HUDController;
import carpet.logging.Logger;
import carpet.logging.LoggerRegistry;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.SectionPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChspifExtension implements CarpetExtension {
    private static final String[] COLOR_NAMES = {
            "black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple",
            "gold", "gray", "dark_gray", "blue", "green", "aqua", "red", "light_purple",
            "yellow", "white"
    };

    @Override
    public String version() {
        return "carpet-chspif-addition";
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
    public void registerLoggers() {
        try {
            Field field = LoggerRegistry.class.getDeclaredField("__chunkmspt");
            LoggerRegistry.registerLogger("chunkmspt",
                    new ChunkMsptLogger(field, "chunkmspt", "true", new String[] { "status" }, false));
        } catch (NoSuchFieldException ignored) {
        }
        HUDController.register(server -> driveChunkMsptLogger());
    }

    private static void driveChunkMsptLogger() {
        Logger chunkMsptLog = LoggerRegistry.getLogger("chunkmspt");
        if (chunkMsptLog != null && chunkMsptLog.hasOnlineSubscribers()) {
            List<String> restore = new ArrayList<>();
            chunkMsptLog.log((option, player) -> {
                if ("status".equals(option)) {
                    EntityMsptSampler.startOneShot((ServerPlayer) player);
                    restore.add(player.getScoreboardName());
                    return null;
                }
                return ChunkMsptRenderer.hudForPlayer(player);
            });
            for (String name : restore) {
                if (ChunkMsptLogger.takePreviousSubscription(name)) {
                    LoggerRegistry.subscribePlayer(name, "chunkmspt", "true");
                } else {
                    LoggerRegistry.unsubscribePlayer(name, "chunkmspt");
                }
            }
        }
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
                .then(Commands.argument("chunkX1", IntegerArgumentType.integer())
                        .then(Commands.argument("chunkZ1", IntegerArgumentType.integer())
                                .then(Commands.argument("chunkX2", IntegerArgumentType.integer())
                                        .then(Commands.argument("chunkZ2", IntegerArgumentType.integer())
                                                .executes(context -> {
                                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                                    ChunkPos a = new ChunkPos(
                                                            IntegerArgumentType.getInteger(context, "chunkX1"),
                                                            IntegerArgumentType.getInteger(context, "chunkZ1"));
                                                    ChunkPos b = new ChunkPos(
                                                            IntegerArgumentType.getInteger(context, "chunkX2"),
                                                            IntegerArgumentType.getInteger(context, "chunkZ2"));
                                                    EntityMsptSampler.startOneShotRange(player, a, b);
                                                    context.getSource().sendSuccess(
                                                            () -> Component.literal("采样中"),
                                                            false);
                                                    return 1;
                                                }))))));

        dispatcher.register(Commands.literal("customrename")
                .requires(ChspifSettings::canUseCustomRename)
                .then(Commands.argument("color", StringArgumentType.word())
                        .suggests((context, builder) ->
                        {
                            for (String color : COLOR_NAMES)
                            {
                                builder.suggest(color);
                            }
                            return builder.buildFuture();
                        })
                        .then(Commands.argument("bold", IntegerArgumentType.integer(0, 1))
                                .then(Commands.argument("italic", IntegerArgumentType.integer(0, 1))
                                        .then(Commands.argument("underlined", IntegerArgumentType.integer(0, 1))
                                                .then(Commands.argument("strikethrough", IntegerArgumentType.integer(0, 1))
                                                        .then(Commands.argument("obfuscated", IntegerArgumentType.integer(0, 1))
                                                                .executes(context -> applyCustomRename(context)))))))));
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
        int loadLevel = distanceManager.getChunkLevel(ChunkPosBridge.key(pos), false);
        int computeLevel = distanceManager.getChunkLevel(ChunkPosBridge.key(pos), true);
        source.sendSuccess(() -> Component.literal("坐标 (" + blockX + ", " + blockZ + ") 区块坐标 (" + chunkX + ", " + chunkZ
                + ") 加载等级 " + loadLevel + " 计算等级 " + computeLevel), false);
        return 1;
    }

    private static int applyCustomRename(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ItemStack item = player.getMainHandItem();
        if (item.isEmpty())
        {
            context.getSource().sendFailure(Component.literal("主手没有物品"));
            return 0;
        }
        String colorName = StringArgumentType.getString(context, "color").toLowerCase(Locale.ROOT);
        TextColor color = TextColor.parseColor(colorName).result().orElse(null);
        if (color == null)
        {
            context.getSource().sendFailure(Component.literal("未知颜色: " + colorName));
            return 0;
        }
        Style style = Style.EMPTY
                .withColor(color)
                .withBold(IntegerArgumentType.getInteger(context, "bold") == 1)
                .withItalic(IntegerArgumentType.getInteger(context, "italic") == 1)
                .withUnderlined(IntegerArgumentType.getInteger(context, "underlined") == 1)
                .withStrikethrough(IntegerArgumentType.getInteger(context, "strikethrough") == 1)
                .withObfuscated(IntegerArgumentType.getInteger(context, "obfuscated") == 1);
        item.set(DataComponents.CUSTOM_NAME, item.getHoverName().copy().withStyle(style));
        context.getSource().sendSuccess(() -> Component.literal("已修改物品名字"), false);
        return 1;
    }

    @Override
    public void onTick(MinecraftServer server) {
        if (ChspifSettings.overworldEnderDragonPet) {
            EnderDragonSummoner.tick(server);
        }
        if (EntityMsptSampler.isSampling()) {
            if (EntityMsptSampler.isLiveSampling()) {
                EntityMsptSampler.updateCenters(server);
            }
            EntityMsptSampler.tick();
        }
    }

    @Override
    public void onServerClosed(MinecraftServer server) {
        SharedMailBox.getInstance().onServerClosed();
    }
}
