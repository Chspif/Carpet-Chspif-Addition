package chspif.mixins;

import carpet.logging.LoggerRegistry;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LoggerRegistry.class)
public class LoggerRegistryMixin
{
    private static boolean __chunkmspt;
}
