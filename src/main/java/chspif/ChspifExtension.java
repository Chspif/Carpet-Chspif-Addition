package chspif;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import net.minecraft.server.MinecraftServer;

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
}
