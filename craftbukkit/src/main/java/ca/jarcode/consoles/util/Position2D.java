package ca.jarcode.consoles.util;

import java.util.Objects;

public class Position2D {

	private final int x, y;

	public Position2D(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public int getY() {
		return y;
	}

	public int getX() {
		return x;
	}
	public Position2D copy() {
		return new Position2D(x, y);
	}
	@Override
	public boolean equals(Object another) {
		return  another instanceof Position2D
				&& ((Position2D) another).getX() == x && ((Position2D) another).getY() == y;
	}
	@Override
	public int hashCode() {
		return Objects.hash(x, y);
	}
}
