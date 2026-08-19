package chspif.mixins;

import chspif.ChspifSettings;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.Set;

@Mixin(Item.class)
public class ItemMixin
{
    private static final Set<Block> GLASS_LIKE = new HashSet<>();

    static
    {
        GLASS_LIKE.add(Blocks.GLOWSTONE);
        GLASS_LIKE.add(Blocks.REDSTONE_LAMP);
        GLASS_LIKE.add(Blocks.SEA_LANTERN);
        GLASS_LIKE.add(Blocks.PEARLESCENT_FROGLIGHT);
        GLASS_LIKE.add(Blocks.VERDANT_FROGLIGHT);
        GLASS_LIKE.add(Blocks.OCHRE_FROGLIGHT);
        GLASS_LIKE.add(Blocks.GLASS_PANE);
        GLASS_LIKE.addAll(Blocks.STAINED_GLASS_PANE.asList());
    }

    @Inject(method = "getDestroySpeed", at = @At("HEAD"), cancellable = true)
    private void chspifNetheriteHoeGlassSpeed(ItemStack itemStack, BlockState state, CallbackInfoReturnable<Float> cir)
    {
        if (ChspifSettings.netheriteHoeGlassSpeed
                && itemStack.getItem() == Items.NETHERITE_HOE
                && (state.is(BlockTags.IMPERMEABLE) || GLASS_LIKE.contains(state.getBlock())))
        {
            cir.setReturnValue(3.0f);
        }
    }
}
