package chspif;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
//#if MC>=260000
import net.minecraft.world.entity.EntityTypes;
//#endif
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Detects the vanilla four-crystal arrangement in the Overworld. */
public final class EnderDragonSummoner {
    private static final Set<BlockPos> ACTIVE_CENTERS = new HashSet<>();

    private EnderDragonSummoner() {
    }

    public static void tick(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            if (level.dimension() != Level.OVERWORLD) {
                continue;
            }
            ACTIVE_CENTERS.removeIf(center -> !hasNearbyCrystals(level, center));
            for (var player : level.players()) {
                List<EndCrystal> crystals = level.getEntitiesOfClass(EndCrystal.class,
                        new AABB(player.blockPosition()).inflate(128));
                for (EndCrystal crystal : crystals) {
                    tryPattern(level, crystal.blockPosition(), 3, 0);
                    tryPattern(level, crystal.blockPosition(), -3, 0);
                    tryPattern(level, crystal.blockPosition(), 0, 3);
                    tryPattern(level, crystal.blockPosition(), 0, -3);
                }
            }
        }
    }

    private static void tryPattern(ServerLevel level, BlockPos anchor, int centerOffsetX, int centerOffsetZ) {
        BlockPos center = anchor.offset(centerOffsetX, 0, centerOffsetZ);
        if (ACTIVE_CENTERS.contains(center) || !hasCrystal(level, anchor)) {
            return;
        }
        if (!hasCrystal(level, center.offset(3, 0, 0)) || !hasCrystal(level, center.offset(-3, 0, 0))
                || !hasCrystal(level, center.offset(0, 0, 3)) || !hasCrystal(level, center.offset(0, 0, -3))) {
            return;
        }
        //#if MC>=260000
        EnderDragon dragon = EntityTypes.ENDER_DRAGON.create(level, EntitySpawnReason.TRIGGERED);
        //#else
        //$$ EnderDragon dragon = EntityType.ENDER_DRAGON.create(level, EntitySpawnReason.TRIGGERED);
        //#endif
        if (dragon == null) {
            return;
        }
        ACTIVE_CENTERS.add(center);
        for (EndCrystal crystal : level.getEntitiesOfClass(EndCrystal.class, new AABB(center).inflate(4))) {
            crystal.discard();
        }
        dragon.setPos(center.getX() + 0.5, center.getY() + 8.0, center.getZ() + 0.5);
        dragon.setPersistenceRequired();
        if (dragon instanceof EnderDragonPetAccess pet) {
            pet.chspifSetHome(center);
            pet.chspifSetOverworldPet(true);
        }
        level.addFreshEntity(dragon);
    }

    private static boolean hasCrystal(ServerLevel level, BlockPos pos) {
        for (EndCrystal crystal : level.getEntitiesOfClass(EndCrystal.class, new AABB(pos))) {
            if (crystal.blockPosition().equals(pos)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasNearbyCrystals(ServerLevel level, BlockPos center) {
        return !level.getEntitiesOfClass(EndCrystal.class, new AABB(center).inflate(4)).isEmpty();
    }
}
