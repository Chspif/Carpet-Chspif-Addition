package chspif;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.Map;

public class ChspifExtension implements CarpetExtension
{
    @Override
    public String version()
    {
        return "carpet-chspif";
    }

    @Override
    public void onGameStarted()
    {
        CarpetServer.settingsManager.parseSettingsClass(ChspifSettings.class);
    }

    @Override
    public Map<String, String> canHasTranslations(String lang)
    {
        return ChspifTranslations.getTranslations(lang);
    }

    @Override
    public void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandBuildContext)
    {
        dispatcher.register(Commands.literal("send")
                .requires(ChspifSettings::canUseSend)
                .executes(context ->
                {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    SharedMailBox.openFor(player);
                    return 1;
                }));

        dispatcher.register(Commands.literal("smallfix")
                .requires(ChspifSettings::canUseSmallFix)
                .executes(context ->
                {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    AttributeInstance scale = player.getAttribute(Attributes.SCALE);
                    if (scale != null)
                    {
                        if (scale.getBaseValue() == 1.0)
                        {
                            scale.setBaseValue(0.15);
                        }
                        else
                        {
                            scale.setBaseValue(1.0);
                        }
                    }
                    return 1;
                }));
    }

    @Override
    public void onServerClosed(MinecraftServer server)
    {
        SharedMailBox.getInstance().onServerClosed();
    }
}
