package jarcode.consoles.util;

import org.bukkit.Location;

/*

Represents a 3D region

 */
public class Region {
	// private boolean printed = false;
	private final int x;
	private final int y;
	private final int z;
	private final int width; // x
	private final int depth; // z
	private final int height; // y

	public Region(Location pos1, Location pos2) {
		this(new LocalPosition(pos1), new LocalPosition(pos2));
	}
	public Region(LocalPosition pos1, LocalPosition pos2) {

		int temp;

		int x1 = pos1.x;
		int y1 = pos1.y;
		int z1 = pos1.z;
		int x2 = pos2.x;
		int y2 = pos2.y;
		int z2 = pos2.z;
		if (y1 > y2) {
			temp = y2;
			y2 = y1;
			y1 = temp;
		}
		if (z1 > z2) {
			temp = z2;
			z2 = z1;
			z1 = temp;
		}
		if (x1 > x2) {
			temp = x2;
			x2 = x1;
			x1 = temp;
		}
		x = x1;
		y = y1;
		z = z1;
		width = Math.abs(x2 - x1) + 1;
		height = Math.abs(y2 - y1) + 1;
		depth = Math.abs(z2 - z1) + 1;
	}

	public boolean inside(Location point) {
		if ((point.getBlockX() <= ((width + x) - 1)) && (point.getBlockX() >= x)) {
			if ((point.getBlockY() <= ((height + y) - 1)) && (point.getBlockY() >= y)) {
				if ((point.getBlockZ() <= ((depth + z) - 1)) && (point.getBlockZ() >= z)) {
					return true;
				}
			}
		}
		return false;
	}

	public Region grow(int gx, int gy, int gz) {
		return new Region(new LocalPosition(x - gz, y - gy, z - gz),
				new LocalPosition(x + width - 1 + gx, y + height - 1 + gy, z + depth - 1 + gz));
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public int getDepth() {
		return depth;
	}

	public LocalPosition[] getVCorners() {
		LocalPosition[] corners = new LocalPosition[4];
		corners[0] = new LocalPosition(x, y, z);
		corners[1] = new LocalPosition(x + (width - 1), y, z);
		corners[2] = new LocalPosition(x, y + (height - 1), z);
		corners[3] = new LocalPosition(x + (width - 1), y + (height - 1), z);
		return corners;
	}
	public LocalPosition[] getHCorners() {
		LocalPosition[] corners = new LocalPosition[4];
		corners[0] = new LocalPosition(x, y, z);
		corners[1] = new LocalPosition(x + (width - 1), y, z);
		corners[2] = new LocalPosition(x, y, z + (depth - 1));
		corners[3] = new LocalPosition(x + (width - 1), y, z + (depth - 1));
		return corners;
	}

	public LocalPosition getOrigin() {
		return new LocalPosition(x, y, z);
	}

	@Override
	public Region clone() {
		return new Region(getOrigin(), getOrigin().add(width - 1, height - 1, depth - 1));
	}

	public boolean overlap(Region r) {
		Range yr1 = new Range(y, (height + y) - 1);
		Range xr1 = new Range(x, (width + x) - 1);
		Range zr1 = new Range(z, (depth + z) - 1);

		Range yr2 = new Range(r.getOrigin().y, (r.getHeight() + r.getOrigin().y) - 1);
		Range xr2 = new Range(r.getOrigin().x, (r.getWidth() + r.getOrigin().x) - 1);
		Range zr2 = new Range(r.getOrigin().z, (r.getDepth() + r.getOrigin().z) - 1);

		return (inRange(yr1, yr2) && inRange(xr1, xr2) && inRange(zr1, zr2));
	}

	public boolean overlap(Region r, int border) {
		Region borderRegion = new Region(getOrigin().add(-border, -border, -border), getOrigin().add((width + border) - 1, (height + border) - 1, (depth + border) - 1));
		return borderRegion.overlap(r);
	}

	public boolean overlapIgnoreY(Region r, int border) {
		Region borderRegion = new Region(getOrigin().add(-border, -border, -border), getOrigin().add((width + border) - 1, (height + border) - 1, (depth + border) - 1));
		return borderRegion.overlapIgnoreY(r);
	}

	public boolean overlapIgnoreY(Region r) {
		Range xr1 = new Range(x, (width + x) - 1);
		Range zr1 = new Range(z, (depth + z) - 1);

		Range xr2 = new Range(r.getOrigin().x, (r.getWidth() + r.getOrigin().x) - 1);
		Range zr2 = new Range(r.getOrigin().z, (r.getDepth() + r.getOrigin().z) - 1);

		return (inRange(xr1, xr2) && inRange(zr1, zr2));
	}

	public boolean insideIgnoreY(Location point) {
		if ((point.getBlockX() <= ((width + x) - 1)) && (point.getBlockX() >= x)) {
			if ((point.getBlockZ() <= ((depth + z) - 1)) && (point.getBlockZ() >= z)) {
				return true;
			}
		}
		return false;
	}

	private boolean inRange(Range r1, Range r2) {
		if ((r1.v1 >= r2.v1) && (r1.v1 <= r2.v2)) {
			return true;
		} else if ((r1.v2 >= r2.v1) && (r1.v2 <= r2.v2)) {
			return true;
		} else if ((r2.v1 >= r1.v1) && (r2.v1 <= r1.v2)) {
			return true;
		} else {
			return ((r2.v2 >= r1.v1) && (r2.v2 <= r1.v2));
		}
	}

	private class Range {
		int v1;
		int v2;

		public Range(int v1, int v2) {
			this.v1 = v1;
			this.v2 = v2;
		}
	}

}
