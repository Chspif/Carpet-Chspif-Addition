package chspif;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.Set;

public class PhantomFollowGoal extends Goal
{
    private static final double FOLLOW_START_DIST_SQR = 36.0;
    private static final double TELEPORT_DIST_SQR = 48.0 * 48.0;
    private static final double FOLLOW_HEIGHT = 3.0;

    private final Mob phantom;
    private final PhantomPetAccess pet;

    public PhantomFollowGoal(Mob phantom)
    {
        this.phantom = phantom;
        this.pet = (PhantomPetAccess) phantom;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse()
    {
        return pet.chspifIsTamed() && !pet.chspifIsRidden() && !phantom.isPassenger();
    }

    @Override
    public boolean canContinueToUse()
    {
        return this.canUse();
    }

    @Override
    public void start()
    {
        phantom.getNavigation().stop();
    }

    @Override
    public void stop()
    {
        phantom.getNavigation().stop();
    }

    @Override
    public void tick()
    {
        LivingEntity owner = pet.chspifGetOwner();
        if (owner == null)
        {
            return;
        }
        if (owner.level() != phantom.level())
        {
            pet.chspifSetMoveTarget(phantom.position());
            return;
        }
        if (phantom.distanceToSqr(owner) > TELEPORT_DIST_SQR)
        {
            chspifTeleportNearOwner(owner);
            return;
        }
        if (phantom.distanceToSqr(owner) > FOLLOW_START_DIST_SQR)
        {
            pet.chspifSetMoveTarget(owner.position().add(0.0, FOLLOW_HEIGHT, 0.0));
        }
        else
        {
            pet.chspifSetMoveTarget(phantom.position());
        }
    }

    private void chspifTeleportNearOwner(LivingEntity owner)
    {
        if (!(owner.level() instanceof ServerLevel targetLevel))
        {
            return;
        }
        phantom.teleportTo(targetLevel, owner.getX(), owner.getY() + 2.0, owner.getZ(), Set.of(), owner.getYRot(), owner.getXRot(), false);
    }
}
