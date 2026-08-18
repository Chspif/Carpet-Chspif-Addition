package chspif;

import carpet.CarpetServer;
import net.fabricmc.api.ModInitializer;

public class CarpetChspifMod implements ModInitializer
{
    @Override
    public void onInitialize()
    {
        CarpetServer.manageExtension(new ChspifExtension());
    }
}
