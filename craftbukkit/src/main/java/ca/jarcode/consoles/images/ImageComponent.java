package ca.jarcode.consoles.images;

import ca.jarcode.consoles.internal.ConsoleComponent;
import ca.jarcode.consoles.internal.ConsoleRenderer;
import ca.jarcode.consoles.api.CanvasGraphics;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

/*

Component that can render a large variety of image formats. JPEG images
have an odd bug sometimes, where everything is sampled as a pink-ish color.

 */
public class ImageComponent extends ConsoleComponent {

	public static final int[] MAPPINGS = new int[144];

	private static int gray(int v) {
		return color(v, v, v);
	}
	private static void r(int c, int v) {
		MAPPINGS[v] = c;
	}
	private static int color(int r, int g, int b) {
		return new Color(r, g, b, 0).getRGB();
	}
	// sample the pixel at x, y and return the best map pixel id for the color
	private static byte sample(int x, int y, BufferedImage image) {
		int color = image.getRGB(x, y);
		int r = (color >> 16) & 0xFF;
		int g = (color >> 8) & 0xFF;
		int b = color & 0xFF;
		// if 100% transparent pixel, return 0 (transparent)
		if ((color >> 24) == 0x00) {
			return 0;
		}
		byte best = 0;
		double bd = Double.MAX_VALUE;
		for (int k = 0; k < MAPPINGS.length; k++) {
			int c = MAPPINGS[k];
			if (c != 0) {
				int kr = (c >> 16) & 0xFF;
				int kg = (c >> 8) & 0xFF;
				int kb = c & 0xFF;
				double kd = getDistance(r, g, b, kr, kg, kb);
				if (bd > kd) {
					bd = kd;
					// handle overflow
					best = (byte) (k < 128 ? k : -129 + (k - 127));
				}
				// exact color match!
				if (bd == 0)
					break;
			}
		}
		return best;
	}
	// extracted & modified from spigot source
	private static double getDistance(int r1, int g1, int b1, int r2, int g2, int b2) {
		double mean = (r1 + r2) / 2.0;
		double r = r1 - r2;
		double g = g1 - g2;
		int b = b1 - b2;
		double weightR = 2 + mean / 256.0;
		double weightG = 4.0;
		double weightB = 2 + (255 - mean) / 256.0;
		return weightR * r * r + weightG * g * g + weightB * b * b;
	}
	// can be used to render the image off-thread
	public static PreparedMapImage render(BufferedImage image) {
		byte[][] buffer = new byte[image.getWidth()][image.getHeight()];
		for (int x = 0; x < image.getWidth(); x++) {
			for (int y = 0; y < image.getHeight(); y++) {
				buffer[x][y] = sample(x, y, image);
			}
		}
		return new PreparedMapImage(buffer, image.getWidth(), image.getHeight());
	}
	public static PreparedMapImage render(URL url) throws IOException {
		BufferedImage image = ImageIO.read(url);
		return render(image);
	}
	public static ImageComponent createFromURL(ConsoleRenderer renderer, URL link) throws IOException {
		BufferedImage image = ImageIO.read(link);
		return new ImageComponent(renderer, render(image));
	}

	private byte[][] buffer;

	public ImageComponent(ConsoleRenderer renderer, PreparedMapImage image) {
		super(image.w, image.h, renderer);
		buffer = image.data;
	}
	@Override
	public void paint(CanvasGraphics g, String context) {
		for (int t = 0; t < getWidth(); t++) {
			for (int j = 0; j < getHeight(); j++) {
				byte b = buffer[t][j];
				if (b != 0)
					g.draw(t, j, b);
			}
		}
	}

	// RGB->Map colors
	// this took forever
	// pls mojang
	// why u do dis

	// spigot has its own copy of these, but they are outdated (these are for 1.8.1)
	static {
		// grass
		r(color(88, 124, 39), 4);
		r(color(108, 151, 47), 5);
		r(color(125, 176, 55), 6);
		r(color(66, 93, 29), 7);

		// sand & gravel
		r(color(172, 162, 114), 8);
		r(color(210, 199, 138), 9);
		r(color(244, 230, 161), 10);
		r(color(128, 122, 85), 11);

		// something else
		r(gray(138), 12);
		r(gray(169), 13);
		r(gray(197), 14);
		r(gray(104), 15);

		// lava & TNT
		r(color(178, 0, 0), 16);
		r(color(217, 0, 0), 17);
		r(color(252, 0, 0), 18);
		r(color(133, 0, 0), 19);

		// ice & packed ice
		r(color(111, 111, 178), 20);
		r(color(136, 136, 217), 21);
		r(color(158, 158, 252), 22);
		r(color(83, 83, 133), 23);

		// metal
		r(gray(116), 24);
		r(gray(142), 25);
		r(gray(165), 26);
		r(gray(87), 27);

		// plants
		r(color(0, 86, 0), 28);
		r(color(0, 105, 0), 29);
		r(color(0, 123, 0), 30);
		r(color(0, 64, 0), 31);

		// snow
		r(gray(178), 32);
		r(gray(217), 33);
		r(gray(252), 34);
		r(gray(133), 35);

		// clay
		r(color(114, 117, 127), 36);
		r(color(139, 142, 156), 37);
		r(color(162, 166, 182), 38);
		r(color(85, 87, 96), 39);

		// dirt
		r(color(105, 75, 53), 40);
		r(color(128, 93, 65), 41);
		r(color(149, 108, 76), 42);
		r(color(78, 56, 39), 43);

		// stone, cobble & ore
		r(gray(78), 44);
		r(gray(95), 45);
		r(gray(111), 46);
		r(gray(58), 47);

		// water
		r(color(44, 44, 178), 48);
		r(color(54, 54, 217), 49);
		r(color(63, 63, 252), 50);
		r(color(33, 33, 133), 51);

		// logs
		r(color(99, 83, 49), 52);
		r(color(122, 101, 61), 53);
		r(color(141, 118, 71), 54);
		r(color(74, 62, 38), 55);

		// wool, carpet & stained clay
		r(color(178, 175, 170), 56);
		r(color(217, 214, 208), 57);
		r(color(252, 249, 242), 58);
		r(color(133, 131, 127), 59);

		// orange stuff
		r(color(150, 88, 36), 60);
		r(color(184, 108, 43), 61);
		r(color(213, 125, 50), 62);
		r(color(113, 66, 27), 63);

		// purple stuff
		r(color(124, 52, 150), 64);
		r(color(151, 64, 184), 65);
		r(color(176, 75, 213), 66);
		r(color(93, 39, 113), 67);

		// light blue stuff
		r(color(71, 107, 150), 68);
		r(color(87, 130, 184), 69);
		r(color(101, 151, 213), 70);
		r(color(53, 80, 113), 71);

		// yellow stuff
		r(color(159, 159, 36), 72);
		r(color(195, 195, 43), 73);
		r(color(226, 226, 50), 74);
		r(color(120, 120, 27), 75);

		// lime stuff
		r(color(88, 142, 17), 76);
		r(color(108, 174, 121), 77);
		r(color(125, 202, 25), 78);
		r(color(66, 107, 13), 79);

		// pink stuff
		r(color(168, 88, 115), 80);
		r(color(206, 108, 140), 81);
		r(color(239, 125, 163), 82);
		r(color(126, 66, 86), 83);

		// grey stuff
		r(gray(52), 84);
		r(gray(64), 85);
		r(gray(75), 86);
		r(gray(39), 87);

		// light grey stuff
		r(gray(107), 88);
		r(gray(130), 89);
		r(gray(151), 90);
		r(gray(80), 91);

		// cyan stuff
		r(color(52, 88, 107), 92);
		r(color(64, 108, 130), 93);
		r(color(75, 125, 151), 94);
		r(color(39, 66, 80), 95);

		// purple stuff
		r(color(88, 43, 124), 96);
		r(color(108, 53, 151), 97);
		r(color(125, 62, 176), 98);
		r(color(66, 33, 193), 99);

		// blue stuff
		r(color(36, 52, 124), 100);
		r(color(43, 64, 151), 101);
		r(color(50, 75, 176), 102);
		r(color(27, 39, 93), 103);

		// brown stuff
		r(color(71, 52, 36), 104);
		r(color(87, 64, 43), 105);
		r(color(101, 75, 50), 106);
		r(color(53, 39, 27), 107);

		// green stuff
		r(color(71, 88, 36), 108);
		r(color(87, 108, 43), 109);
		r(color(101, 125, 50), 110);
		r(color(53, 66, 27), 111);

		// red stuff
		r(color(107, 36, 36), 112);
		r(color(130, 43, 43), 113);
		r(color(151, 50, 50), 114);
		r(color(80, 27, 27), 115);

		// black stuff
		r(gray(17), 116);
		r(gray(21), 117);
		r(gray(25), 118);
		r(gray(13), 119);

		// gold stuff
		r(color(174, 166, 53), 120);
		r(color(212, 203, 65), 121);
		r(color(247, 235, 76), 122);
		r(color(130, 125, 39), 123);

		// diamond/blueish stuff
		r(color(63, 152, 148), 124);
		r(color(78, 186, 181), 125);
		r(color(91, 216, 210), 126);
		r(color(47, 114, 111), 127);

		// that one blue ore block that I can't remember
		r(color(51, 89, 178), 128);
		r(color(62, 109, 217), 129);
		r(color(73, 129, 252), 130);
		r(color(39, 6, 133), 131);

		// emerald
		r(color(0, 151, 39), 132);
		r(color(0, 185, 49), 133);
		r(color(0, 214, 57), 134);
		r(color(0, 113, 30), 135);

		// random stuff (brown)
		r(color(90, 59, 34), 136);
		r(color(110, 73, 41), 137);
		r(color(127, 85, 48), 138);
		r(color(67, 44, 25), 139);

		// random stuff (red)
		r(color(78, 1, 0), 140);
		r(color(95, 1, 0), 141);
		r(color(111, 2, 0), 142);
		r(color(58, 1, 0), 143);
	}
}
