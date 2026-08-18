package chspif.mixins;

import chspif.ChspifSettings;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.equipment.trim.TrimMaterials;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PiglinAi.class)
public class PiglinAiMixin
{
    @Inject(method = "isWearingSafeArmor", at = @At("HEAD"), cancellable = true)
    private static void chspifGoldTrimCountsAsSafe(LivingEntity entity, CallbackInfoReturnable<Boolean> cir)
    {
        if (ChspifSettings.piglinIgnoreGoldTrim && hasGoldTrim(entity))
        {
            cir.setReturnValue(true);
        }
    }

    private static boolean hasGoldTrim(LivingEntity entity)
    {
        for (EquipmentSlot slot : EquipmentSlotGroup.ARMOR)
        {
            ItemStack stack = entity.getItemBySlot(slot);
            ArmorTrim trim = stack.get(DataComponents.TRIM);
            if (trim != null && trim.material().is(TrimMaterials.GOLD))
            {
                return true;
            }
        }
        return false;
    }
}
