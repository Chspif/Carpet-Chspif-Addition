package chspif.mixins;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Display.TextDisplay.class)
public interface TextDisplayAccessor
{
    @Invoker("setText")
    void chspifSetText(Component text);

    @Invoker("setLineWidth")
    void chspifSetLineWidth(int width);

    @Invoker("setBackgroundColor")
    void chspifSetBackgroundColor(int color);

    @Invoker("setTextOpacity")
    void chspifSetTextOpacity(byte opacity);
}
