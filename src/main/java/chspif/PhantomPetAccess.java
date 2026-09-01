package chspif;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public interface PhantomPetAccess
{
    boolean chspifPetMode();

    boolean chspifIsTamed();

    boolean chspifIsRidden();

    LivingEntity chspifGetOwner();

    void chspifSetMoveTarget(Vec3 pos);

    void chspifHandleInteract(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir);

    void chspifHandleFireImmunity(CallbackInfoReturnable<Boolean> cir);
}
