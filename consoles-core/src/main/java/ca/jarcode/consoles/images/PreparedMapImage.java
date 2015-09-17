package ca.jarcode.consoles.images;

/*

Prepared/rendered image that is ready to be displayed on an image component.

 */
public class PreparedMapImage {

	protected byte[][] data;
	protected int w, h;

	PreparedMapImage(byte[][] data, int w, int h) {
		this.data = data;
		this.w = w;
		this.h = h;
	}

	public int getWidth() {
		return w;
	}
	public int getHeight() {
		return h;
	}
	public void center() {
		byte[][] old = data.clone();
		if (w % 128 == 0 && h % 128 == 0) return;
		int nw = w % 128 == 0 ? w : w + (128 - (w % 128));
		int nh = h % 128 == 0 ? h : h + (128 - (h % 128));
		int ow = (nw - w) / 2;
		int oh = (nh - h) / 2;
		data = new byte[nw][nh];
		for (int x = ow; x < w + ow; x++) {
			System.arraycopy(old[x - ow], 0, data[x], oh, h + oh - oh);
		}
		w = nw;
		h = nh;
	}
	public void replace(byte target, byte replace) {
		for (int x = 0; x < w; x++) {
			for (int y = 0; y < h; y++) {
				if (data[x][y] == target)
					data[x][y] = replace;
			}
		}
	}
	public void background(byte b) {
		replace((byte) 0, b);
	}
}
