package chspif;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class PhantomFollowGoal extends Goal
{
    private static final double FOLLOW_START_DIST_SQR = 36.0;
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
        return pet.chspifIsTamed() && !pet.chspifIsRidden();
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
        if (owner == null || owner.level() != phantom.level())
        {
            pet.chspifSetMoveTarget(phantom.position());
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
}
