package chspif.mixins;

import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Display.class)
public interface DisplayAccessor
{
    @Invoker("setBillboardConstraints")
    void chspifSetBillboard(Display.BillboardConstraints constraints);
}
