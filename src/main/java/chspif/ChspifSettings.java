package chspif;

import carpet.api.settings.Rule;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

import static carpet.api.settings.RuleCategory.*;

public class ChspifSettings
{
    public static final String CHSPIF = "chspif";

    @Rule(categories = {CHSPIF, FEATURE})
    public static boolean piglinIgnoreGoldTrim = false;

    @Rule(categories = {CHSPIF, FEATURE})
    public static boolean glowingItems = false;

    @Rule(categories = {CHSPIF, FEATURE})
    public static boolean glowingMinecarts = false;

    @Rule(categories = {CHSPIF, FEATURE})
    public static boolean netheriteHoeGlassSpeed = false;

    @Rule(categories = {CHSPIF, FEATURE})
    public static boolean undeadAvoidCinnabar = false;

    @Rule(categories = {CHSPIF, COMMAND},
            options = {"0", "1", "2", "3", "4", "ops", "true", "false"},
            strict = false)
    public static String commandSend = "false";

    @Rule(categories = {CHSPIF, COMMAND},
            options = {"0", "1", "2", "3", "4", "ops", "true", "false"},
            strict = false)
    public static String commandSmallFix = "false";

    @Rule(categories = {CHSPIF, COMMAND},
            options = {"0", "1", "2", "3", "4", "ops", "true", "false"},
            strict = false)
    public static String commandChunkLoadInfo = "false";

    public static boolean canUseSend(CommandSourceStack source)
    {
        return canUseCommand(commandSend, source);
    }

    public static boolean canUseSmallFix(CommandSourceStack source)
    {
        return canUseCommand(commandSmallFix, source);
    }

    public static boolean canUseChunkLoadInfo(CommandSourceStack source)
    {
        return canUseCommand(commandChunkLoadInfo, source);
    }

    @Rule(categories = {CHSPIF, COMMAND},
            options = {"0", "1", "2", "3", "4", "ops", "true", "false"},
            strict = false)
    public static String commandChunkMsptInfo = "false";

    public static boolean canUseChunkMsptInfo(CommandSourceStack source)
    {
        return canUseCommand(commandChunkMsptInfo, source);
    }

    private static boolean canUseCommand(String ruleValue, CommandSourceStack source)
    {
        if (ruleValue == null)
        {
            return false;
        }
        return switch (ruleValue)
        {
            case "true" -> true;
            case "false" -> false;
            case "ops", "2" -> Commands.LEVEL_GAMEMASTERS.check(source.permissions());
            case "0" -> Commands.LEVEL_ALL.check(source.permissions());
            case "1" -> Commands.LEVEL_MODERATORS.check(source.permissions());
            case "3" -> Commands.LEVEL_ADMINS.check(source.permissions());
            case "4" -> Commands.LEVEL_OWNERS.check(source.permissions());
            default -> false;
        };
    }
}
