package chspif;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AvoidCinnabarGoal extends Goal
{
    private static final int SCAN_INTERVAL = 1;
    private static final double SPRINT_DISTANCE_SQR = 49.0;

    private static final Set<Block> CINNABAR_BLOCKS = new HashSet<>();

    static
    {
        CINNABAR_BLOCKS.add(Blocks.CINNABAR);
        CINNABAR_BLOCKS.add(Blocks.CINNABAR_STAIRS);
        CINNABAR_BLOCKS.add(Blocks.CINNABAR_SLAB);
        CINNABAR_BLOCKS.add(Blocks.CINNABAR_WALL);
        CINNABAR_BLOCKS.add(Blocks.POLISHED_CINNABAR);
        CINNABAR_BLOCKS.add(Blocks.POLISHED_CINNABAR_STAIRS);
        CINNABAR_BLOCKS.add(Blocks.POLISHED_CINNABAR_SLAB);
        CINNABAR_BLOCKS.add(Blocks.POLISHED_CINNABAR_WALL);
        CINNABAR_BLOCKS.add(Blocks.CINNABAR_BRICKS);
        CINNABAR_BLOCKS.add(Blocks.CINNABAR_BRICK_STAIRS);
        CINNABAR_BLOCKS.add(Blocks.CINNABAR_BRICK_SLAB);
        CINNABAR_BLOCKS.add(Blocks.CINNABAR_BRICK_WALL);
        CINNABAR_BLOCKS.add(Blocks.CHISELED_CINNABAR);
    }

    private final PathfinderMob mob;
    private final float maxDist;
    private final double walkSpeedModifier;
    private final double sprintSpeedModifier;

    private int ticksUntilScan;
    private @Nullable Vec3 avoidPos;
    private @Nullable Path path;

    public AvoidCinnabarGoal(PathfinderMob mob, float maxDist, double walkSpeedModifier, double sprintSpeedModifier)
    {
        this.mob = mob;
        this.maxDist = maxDist;
        this.walkSpeedModifier = walkSpeedModifier;
        this.sprintSpeedModifier = sprintSpeedModifier;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse()
    {
        if (!ChspifSettings.undeadAvoidCinnabar)
        {
            return false;
        }
        if (++this.ticksUntilScan < SCAN_INTERVAL)
        {
            return false;
        }
        this.ticksUntilScan = 0;

        Vec3 threatPos = this.findNearestThreat();
        if (threatPos == null)
        {
            return false;
        }

        this.avoidPos = threatPos;
        Vec3 fleePos = DefaultRandomPos.getPosAway(this.mob, 16, 7, threatPos);
        if (fleePos == null)
        {
            return false;
        }
        if (threatPos.distanceToSqr(fleePos) < threatPos.distanceToSqr(this.mob.position()))
        {
            return false;
        }

        this.path = this.mob.getNavigation().createPath(fleePos.x, fleePos.y, fleePos.z, 0);
        return this.path != null;
    }

    @Override
    public boolean canContinueToUse()
    {
        return !this.mob.getNavigation().isDone();
    }

    @Override
    public void start()
    {
        this.mob.getNavigation().moveTo(this.path, this.walkSpeedModifier);
    }

    @Override
    public void stop()
    {
        this.avoidPos = null;
        this.ticksUntilScan = SCAN_INTERVAL;
    }

    @Override
    public void tick()
    {
        if (this.avoidPos != null && this.mob.distanceToSqr(this.avoidPos) < SPRINT_DISTANCE_SQR)
        {
            this.mob.getNavigation().setSpeedModifier(this.sprintSpeedModifier);
        }
        else
        {
            this.mob.getNavigation().setSpeedModifier(this.walkSpeedModifier);
        }
    }

    private @Nullable Vec3 findNearestThreat()
    {
        Vec3 blockCenter = this.findNearestCinnabarBlockCenter();
        Vec3 playerPos = this.findNearestCinnabarPlayer();

        if (blockCenter == null)
        {
            return playerPos;
        }
        if (playerPos == null)
        {
            return blockCenter;
        }
        return this.mob.distanceToSqr(blockCenter) <= this.mob.distanceToSqr(playerPos) ? blockCenter : playerPos;
    }

    private @Nullable Vec3 findNearestCinnabarBlockCenter()
    {
        BlockPos block = this.findNearestCinnabarBlock();
        return block != null ? Vec3.atCenterOf(block) : null;
    }

    private @Nullable Vec3 findNearestCinnabarPlayer()
    {
        List<Player> players = this.mob.level().getEntitiesOfClass(
                Player.class,
                this.mob.getBoundingBox().inflate(this.maxDist, 3.0, this.maxDist),
                player -> !player.isSpectator() && AvoidCinnabarGoal.holdsCinnabar(player));
        Player nearest = null;
        double nearestDistSqr = Double.MAX_VALUE;
        for (Player player : players)
        {
            double distSqr = this.mob.distanceToSqr(player);
            if (distSqr < nearestDistSqr)
            {
                nearestDistSqr = distSqr;
                nearest = player;
            }
        }
        return nearest != null ? nearest.position() : null;
    }

    private static boolean holdsCinnabar(Player player)
    {
        return isCinnabarItem(player.getMainHandItem()) || isCinnabarItem(player.getOffhandItem());
    }

    private static boolean isCinnabarItem(ItemStack stack)
    {
        return !stack.isEmpty() && CINNABAR_BLOCKS.contains(Block.byItem(stack.getItem()));
    }

    private @Nullable BlockPos findNearestCinnabarBlock()
    {
        AABB box = this.mob.getBoundingBox().inflate(this.maxDist, 3.0, this.maxDist);
        int minX = Mth.floor(box.minX);
        int minY = Mth.floor(box.minY);
        int minZ = Mth.floor(box.minZ);
        int maxX = Mth.floor(box.maxX);
        int maxY = Mth.floor(box.maxY);
        int maxZ = Mth.floor(box.maxZ);

        double mobX = this.mob.getX();
        double mobY = this.mob.getY();
        double mobZ = this.mob.getZ();
        BlockPos nearest = null;
        double nearestDistSqr = Double.MAX_VALUE;

        for (BlockPos pos : BlockPos.betweenClosed(minX, minY, minZ, maxX, maxY, maxZ))
        {
            if (CINNABAR_BLOCKS.contains(this.mob.level().getBlockState(pos).getBlock()))
            {
                double distSqr = pos.distToCenterSqr(mobX, mobY, mobZ);
                if (distSqr < nearestDistSqr)
                {
                    nearestDistSqr = distSqr;
                    nearest = pos.immutable();
                }
            }
        }
        return nearest;
    }
}
