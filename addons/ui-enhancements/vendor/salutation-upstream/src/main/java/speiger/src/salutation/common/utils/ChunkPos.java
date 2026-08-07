package speiger.src.salutation.common.utils;

public class ChunkPos {
	public final int x;
	public final int z;

	public ChunkPos(long longIn) {
		this.x = (int)longIn;
		this.z = (int)(longIn >> 32);
	}

	public ChunkPos(int xPos, int zPos) {
		x = xPos;
		z = zPos;
	}

	public ChunkPos offset(int xOff, int zOff) {
		return new ChunkPos(x + xOff, z + zOff);
	}

	public static long add(long pos, int x, int z) {
		return asLong(x + getX(pos), z + getZ(pos));
	}

	public static long asLong(int x, int z) {
		return x & 4294967295L | (z & 4294967295L) << 32;
	}

	public static int getX(long pos) {
		return (int)pos;
	}

	public static int getZ(long pos) {
		return (int)(pos >> 32);
	}

	@Override
	public int hashCode() {
		int i = 1664525 * x + 1013904223;
		int j = 1664525 * (z ^ -559038737) + 1013904223;
		return i ^ j;
	}

	@Override
	public boolean equals(Object arg) {
		if(arg instanceof ChunkPos) {
			ChunkPos pos = (ChunkPos)arg;
			return pos.x == x && pos.z == z;
		}
		return false;
	}

	public long asLong() {
		return asLong(x, z);
	}

	@Override
	public String toString() {
		return "X: "+x+" Z: "+z;
	}

	public ChunkPos toChunkFile() {
		return new ChunkPos(x >> 5, z >> 5);
	}

	public ChunkPos toChunkPos() {
		return new ChunkPos(x >> 4, z >> 4);
	}

	public int getDistance(int xPos, int zPos) {
		int xDis = x - xPos;
		int zDis = z - zPos;
		return (xDis * xDis) + (zDis * zDis);
	}

	public double getSqDistance(int xPos, int zPos) {
		return Math.sqrt(getDistance(xPos, zPos));
	}

	public int getSquDistane(ChunkPos pos) {
		return (int)getSqDistance(pos.x, pos.z);
	}
}
