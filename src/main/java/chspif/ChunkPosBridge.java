package chspif;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

public class ChunkPosBridge {
	//#if MC>=260000
	public static int x(ChunkPos pos) { return pos.x(); }
	public static int z(ChunkPos pos) { return pos.z(); }
	public static long key(ChunkPos pos) { return pos.pack(); }
	public static ChunkPos of(BlockPos pos) { return ChunkPos.containing(pos); }
	//#else
	//$$ public static int x(ChunkPos pos) { return pos.x; }
	//$$ public static int z(ChunkPos pos) { return pos.z; }
	//$$ public static long key(ChunkPos pos) { return ChunkPos.asLong(pos.x, pos.z); }
	//$$ public static ChunkPos of(BlockPos pos) { return new ChunkPos(pos); }
	//#endif
}
