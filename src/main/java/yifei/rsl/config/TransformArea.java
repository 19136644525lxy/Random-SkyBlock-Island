package yifei.rsl.config;

public class TransformArea {
	public int minX, minY, minZ;
	public int maxX, maxY, maxZ;
	public String name;
	
	public TransformArea(String name, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		this.name = name;
		this.minX = minX;
		this.minY = minY;
		this.minZ = minZ;
		this.maxX = maxX;
		this.maxY = maxY;
		this.maxZ = maxZ;
	}
	
	public TransformArea(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		this("default", minX, minY, minZ, maxX, maxY, maxZ);
	}
	
	public int getVolume() {
		return Math.abs((maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1));
	}
	
	public boolean contains(int x, int y, int z) {
		return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
	}
	
	@Override
	public String toString() {
		return name + ": (" + minX + "," + minY + "," + minZ + ") ~ (" + maxX + "," + maxY + "," + maxZ + ")";
	}
}
