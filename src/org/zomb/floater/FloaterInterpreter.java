/*
 * This file is part of Floater.
 *
 * Floater is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Floater is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Floater.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.zomb.floater;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

/**
 * @author Zom-B
 * @version 1.0
 * @since 1.0
 */
// Created 2014-03-14
public class FloaterInterpreter {
	// Some random number source.
	private static final Random RND = new Random();

	// The program.
	BufferedImage img;

	// Instruction pointer.
	int ipx;
	int ipy;

	// Instruction state.
	int dir;
	int runState;

	// The memory, stack and accumulator.
	private double[] mem = new double[64];

	// Stack Pointer and memory size.
	int sp;

	// Configuration.
	private int ioMode;
	private int gfxMode;
	private double logFactor;
	private double angFactor;

	// Flood fill helper.
	int[][] flooded;

	// The fetched instruction:
	int opcode;
	int param;
	private boolean direction;

	public void setImage(BufferedImage img) {
		this.img = img;

		flooded = new int[img.getHeight()][img.getWidth()];

		reset();
	}

	public void reset() {
		// Default configuration:
		ioMode = 0;
		gfxMode = 0;
		logFactor = 1;
		angFactor = 1;

		// Starting conditions:
		ipx = 0;
		ipy = 0;
		dir = 0;
		runState = 0;
		sp = -1;

		// Try to find the start codel location.
		for (int x = 0; x < img.getWidth(); x++) {
			if (getRawPixel(x, 0) != 0) {
				ipx = x;
				break;
			}
		}

		fetch();
	}

	public void fetch() {
		int color = getRawPixel(ipx, ipy);
		opcode = decodeOpcode(color);
		param = decodeParam(color);
		direction = decodeDirection(color);
	}

	@SuppressWarnings({"SwitchStatementWithoutDefaultBranch", "NestedSwitchStatement", "SwitchStatementDensity",
			"TooBroadScope"})
	public void execute() {
		// Halted?
		if (dir == -1) {
			return;
		}

		boolean move = true;

		double pop1;
		double pop2;
		double pop3;

		switch (opcode) {
			case 0x1: // PUSH
				push(param);
				break;
			case 0x2:
				switch (param) {
					case 1: // DUP
						pop1 = pop();
						push(pop1);
						push(pop1);
						break;
					case 2: // SWAP
						pop1 = pop();
						pop2 = pop();
						push(pop1);
						push(pop2);
						break;
				}
				break;
			case 0x3:
				switch (param) {
					case 1: // GET
						pop1 = pop() - FloaterConstants.ADDRESS_START;
						push(get(pop1));
						break;
					case 2: // SET
						pop1 = pop() - FloaterConstants.ADDRESS_START;
						pop2 = pop();
						set(pop1, pop2);
						break;
				}
				break;
			case 0x4:
				runState = 1;
				break;
			case 0x5:
				switch (param) {
					case 1: // NOT
						pop1 = pop();
						push(~toLong(pop1));
						break;
					case 2: // AND
						pop1 = pop();
						pop2 = pop();
						push(toLong(pop1) & toLong(pop2));
						break;
					case 3: // OR
						pop1 = pop();
						pop2 = pop();
						push(toLong(pop1) | toLong(pop2));
						break;
					case 4: // XOR
						pop1 = pop();
						pop2 = pop();
						push(toLong(pop1) ^ toLong(pop2));
						break;
				}
				break;
			case 0x6:
				switch (param) {
					case 1: // EQ
						pop1 = pop();
						push(fromBool(pop1 == 0));
						break;
					case 2: // LT
						pop1 = pop();
						push(fromBool(pop1 < 0));
						break;
					case 3: // GT
						pop1 = pop();
						push(fromBool(pop1 > 0));
						break;
					case 4: // SIGN
						pop1 = pop();
						push(Math.signum(pop1));
						break;
				}
				break;
			case 0x7:
				switch (param) {
					case 1: // PRINT
						print(pop());
						break;
					case 2: // INPUT
						push(input());
						break;
					case 3: // IO MODE
						pop1 = pop();
						pop();
						ioMode = toInt(pop1);
						break;
				}
				break;
			case 0x8:
				switch (param) {
					case 1: // SET PIXEL
						pop1 = pop() - FloaterConstants.ADDRESS_START; // Y
						pop2 = pop() - FloaterConstants.ADDRESS_START; // X
						pop3 = pop();
						setPixel(pop2, pop1, pop3);
						break;
					case 2: // GET PIXEL
						pop1 = pop() - FloaterConstants.ADDRESS_START; // Y
						pop2 = pop() - FloaterConstants.ADDRESS_START; // X
						push(getPixel(pop2, pop1));
						break;
					case 3: // GFX MODE
						pop1 = pop();
						gfxMode = toInt(pop1);
						break;
				}
				break;
			case 0x9:
				switch (param) {
					case 1: // RND
						push(RND.nextDouble());
						break;
					case 2: // PUSH IP
						push(ipx + FloaterConstants.ADDRESS_START);
						push(ipy + FloaterConstants.ADDRESS_START);
						break;
					case 3: // PUSH SP
						push(sp + FloaterConstants.ADDRESS_START);
						break;
				}
				break;
			case 0xA:
				switch (param) {
					case 1: // ROUND
						pop1 = pop();
						push(Math.floor(pop1 + 0.5));
						break;
					case 2: // FLOOR
						pop1 = pop();
						push(Math.floor(pop1));
						break;
					case 3: // CEIL
						pop1 = pop();
						push(Math.ceil(pop1));
						break;
					case 4: // TRUNC
						pop1 = pop();
						push(Math.signum(pop1) * Math.floor(Math.abs(pop1)));
						break;
				}
				break;
			case 0xB:
				switch (param) {
					case 1: // ADD
						pop1 = pop();
						pop2 = pop();
						push(pop2 + pop1);
						break;
					case 2: // SUB
						pop1 = pop();
						pop2 = pop();
						push(pop2 - pop1);
						break;
					case 3: // MUL
						pop1 = pop();
						pop2 = pop();
						push(pop2 * pop1);
						break;
					case 4: // DIV
						pop1 = pop();
						pop2 = pop();
						push(pop2 / pop1);
						break;
				}
				break;
			case 0xC:
				switch (param) {
					case 1: // SQRT
						pop1 = pop();
						push(Math.sqrt(pop1));
						break;
					case 2: // POW
						pop1 = pop();
						pop2 = pop();
						push(Math.pow(pop2, pop1));
						break;
					case 3: // EXP
						pop1 = pop();
						push(Math.exp(pop1 * logFactor));
						break;
					case 4: // LOG
						pop1 = pop();
						push(Math.log(pop1) / logFactor);
						break;
					case 5: // LOG BASE
						pop1 = pop();
						logFactor = Math.log(pop1);
						break;
				}
				break;
			case 0xD:
				switch (param) {
					case 1: // SIN]]
						pop1 = pop();
						push(Math.sin(pop1 * angFactor));
						break;
					case 2: // COS
						pop1 = pop();
						push(Math.cos(pop1 * angFactor));
						break;
					case 3: // TAN
						pop1 = pop();
						push(Math.tan(pop1 * angFactor));
						break;
					case 4: // ATN
						pop1 = pop();
						push(Math.atan(pop1) / angFactor);
						break;
					case 5: // ANG BASE
						pop1 = pop();
						angFactor = 6.2831853071795864769252867665590058 / pop1;
						break;
				}
				break;
			case 0xE:
				break;
			case 0x0F:
				switch (param) {
					case 1: // FORWARD
						break;
					case 2: // ROTATE
						if (direction)
							dir = dir + 1 & 3;
						else
							dir = dir + 3 & 3;
						break;
				}
				break;
		}

		if (move) {
			// Advance instruction pointer.
			ipx += 1 - Math.abs(1 - dir);
			ipy += Math.abs(2 - dir) - 1;
		}

		// Exit check.
		if (ipx < 0 || ipy < 0 || ipx >= img.getWidth() || ipy >= img.getHeight()) {
			runState = 2;
		}
	}

	public int getRawPixel(int x, int y) {
		// Safely get.
		if (x < 0 || y < 0 || x >= img.getWidth() || y >= img.getHeight()) {
			return 0;
		}

		return img.getRGB(x, y) & 0xFFFFFF;
	}

	public static int decodeOpcode(int color) {
		// Pre-process the color.
		// The following formulas convert component values in the following way:
		// 0 -> 0 -> 0
		// 85 -> 2 -> 1
		// 128 -> 4 -> 1
		// 170 -> 5 -> 2
		// 192 -> 6 -> 2
		// 255 -> 7 -> 3
		int red = ((color >> 16) / 32 * 4 + 3) / 10;
		int grn = ((color >> 8 & 0xFF) / 32 * 4 + 3) / 10;
		int blu = ((color & 0xFF) / 32 * 4 + 3) / 10;

		// Reverse palette lookup.
		return FloaterConstants.REV_DOS16[(red << 2 | grn) << 2 | blu];
	}

	private int decodeParam(int color) {
		// Prepare.
		for (int y = 0; y < img.getHeight(); y++)
			Arrays.fill(flooded[y], 0);

		// No param.
		if (color == 0) return 0;

		// Prevent previous codel area from contributing to the param.
		if (getRawPixel(ipx - (1 - Math.abs(1 - dir)), ipy - (Math.abs(2 - dir) - 1)) == color) {
			// go back;
			int xb = ipx - (1 - Math.abs(1 - dir));
			int yb = ipy - (Math.abs(2 - dir) - 1);
			flooded[yb][xb] = -1;

			{ // go right-hand-side
				int xs = xb;
				int ys = yb;
				int x = ipx;
				int y = ipy;
				while (true) {
					xs -= Math.abs(2 - dir) - 1;
					ys += 1 - Math.abs(1 - dir);
					x -= Math.abs(2 - dir) - 1;
					y += 1 - Math.abs(1 - dir);
					if (getRawPixel(xs, ys) != color || getRawPixel(x, y) != color) break;

					flooded[ys][xs] = -1;
				}
			}

			{ // go left-hand-side
				int xs = xb;
				int ys = yb;
				int x = ipx;
				int y = ipy;
				while (true) {
					xs += Math.abs(2 - dir) - 1;
					ys -= 1 - Math.abs(1 - dir);
					x += Math.abs(2 - dir) - 1;
					y -= 1 - Math.abs(1 - dir);
					if (getRawPixel(xs, ys) != color || getRawPixel(x, y) != color) break;

					flooded[ys][xs] = -1;
				}
			}
		}

		// Prevent next codel are from contributing to the param.
		if (getRawPixel(ipx + 1 - Math.abs(1 - dir), ipy + Math.abs(2 - dir) - 1) == color) {
			// go back;
			int xb = ipx + 1 - Math.abs(1 - dir);
			int yb = ipy + Math.abs(2 - dir) - 1;
			flooded[yb][xb] = -1;

			{ // go right-hand-side
				int xs = xb;
				int ys = yb;
				int x = ipx;
				int y = ipy;
				while (true) {
					xs -= Math.abs(2 - dir) - 1;
					ys += 1 - Math.abs(1 - dir);
					x -= Math.abs(2 - dir) - 1;
					y += 1 - Math.abs(1 - dir);
					if (getRawPixel(xs, ys) != color || getRawPixel(x, y) != color) break;

					flooded[ys][xs] = -1;
				}
			}

			{ // go left-hand-side
				int xs = xb;
				int ys = yb;
				int x = ipx;
				int y = ipy;
				while (true) {
					xs += Math.abs(2 - dir) - 1;
					ys -= 1 - Math.abs(1 - dir);
					x += Math.abs(2 - dir) - 1;
					y -= 1 - Math.abs(1 - dir);
					if (getRawPixel(xs, ys) != color || getRawPixel(x, y) != color) break;

					flooded[ys][xs] = -1;
				}
			}
		}

		// Flood fill while counting the area.
		int area = floodFill(color, 0, ipx, ipy);
		return area;
	}

	private int floodFill(int opcode, int area, int x, int y) {
		// Check if the coordinate is unavailable.
		if (flooded[y][x] != 0 || getRawPixel(x, y) != opcode) {
			return area;
		}

		// Take this coordinate.
		area++;
		flooded[y][x] = area;

		// Handle the four wind directions.
		if (y > 0)
			area = floodFill(opcode, area, x, y - 1);
		if (x > 0)
			area = floodFill(opcode, area, x - 1, y);
		if (x < img.getWidth() - 1)
			area = floodFill(opcode, area, x + 1, y);
		if (y < img.getHeight() - 1)
			area = floodFill(opcode, area, x, y + 1);

		return area;
	}

	private boolean decodeDirection(int color) {
		return getRawPixel(ipx - Math.abs(2 - dir) + 1, ipy + 1 - Math.abs(1 - dir)) == color;
	}

	public void push(double param) {
		sp++;

		// Expand memory if necessary.
		if (sp == mem.length)
			mem = Arrays.copyOf(mem, mem.length * 2);

		mem[sp] = param;
	}

	public double pop() {
		// Bounds check.
		if (sp < 0)
			return 0;

		double result = mem[sp];
		sp--;
		return result;
	}

	public void set(double addressDouble, double value) {
		int address = (int) Math.round(addressDouble);

		// Relative-to-stack addressing.
		if (address < 0)
			address = sp + address + 1 + FloaterConstants.ADDRESS_START;

		// Bounds check.
		if (address < 0 || address > sp) return;

		mem[address] = value;
	}

	public double get(double addressDouble) {
		int address = (int) Math.round(addressDouble);

		// Relative-to-stack addressing.
		if (address < 0)
			address = sp + address + FloaterConstants.ADDRESS_START;

		// Bounds check.
		if (address < 0 || address > sp)
			return 0;

		return mem[address];
	}

	@SuppressWarnings("SwitchStatementWithoutDefaultBranch")
	public void setPixel(double xd, double yd, double colorDouble) {
		int x = (int) Math.round(xd);
		int y = (int) Math.round(yd);

		// Bounds check.
		if (x < 0 || y < 0) return;

		// Enlarge when necessary.
		int w = img.getWidth();
		int h = img.getHeight();
		if (x >= w || y >= h) {
			while (x >= w) {
				w = FloaterConstants.bigger(w);
			}
			while (y >= h) {
				h = FloaterConstants.bigger(h);
			}

			BufferedImage newImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
			Graphics2D g = newImg.createGraphics();
			g.drawImage(this.img, 0, 0, null);
			g.dispose();

			this.img = newImg;
			flooded = new int[h][w];
		}

		int color;
		int red;
		int grn;
		int blu;

		switch (gfxMode) {
			case 0: // DOS16
				color = Math.floorMod((int) Math.floor(colorDouble), 16);
				img.setRGB(x, y, FloaterConstants.DOS16[color]);
				break;
			case 1: // Float GRAYSCALE
				color = (int) Math.round(Math.max(0, Math.min(1, colorDouble)) * 255);
				img.setRGB(x, y, color * 0x10101);
				break;
			case 2: // 6BPP
				color = Math.floorMod((int) Math.floor(colorDouble), 64);
				red = color >> 4;
				grn = color >> 2 & 3;
				blu = color & 3;
				img.setRGB(x, y, ((red << 8 | grn) << 8 | blu) << 6);
				break;
			case 3: // 12BPP
				color = Math.floorMod((int) Math.floor(colorDouble), 4096);
				red = color >> 8;
				grn = color >> 4 & 15;
				blu = color & 15;
				img.setRGB(x, y, ((red << 8 | grn) << 8 | blu) << 4);
				break;
			case 4: // 18BPP
				color = Math.floorMod((int) Math.floor(colorDouble), 262144);
				red = color >> 12;
				grn = color >> 6 & 63;
				blu = color & 63;
				img.setRGB(x, y, ((red << 8 | grn) << 8 | blu) << 2);
				break;
			case 5: // 24BPP
				color = Math.floorMod((int) Math.floor(colorDouble), 16777216);
				img.setRGB(x, y, color);
				break;
		}
	}

	@SuppressWarnings("SwitchStatementWithoutDefaultBranch")
	public double getPixel(double xd, double yd) {

		int x = (int) Math.round(xd);
		int y = (int) Math.round(yd);

		// Bounds check.
		if (x < 0 || y < 0 || x >= img.getWidth() || y >= img.getHeight()) {
			return 0;
		}

		int rgb = img.getRGB(x, y) & 0xFFFFFF;
		int red;
		int grn;
		int blu;

		switch (gfxMode) {
			case 0: // DOS16
				return decodeOpcode(rgb);
			case 1: // 8BPP GRAYSCALE
				red = rgb >> 16;
				grn = rgb >> 14 & 3;
				blu = rgb >> 6 & 3;
				return 0.299 * red + 0.587 * grn + 0.114 * blu;
			case 2: // 6BPP
				red = rgb >> 22;
				grn = rgb >> 14 & 3;
				blu = rgb >> 6 & 3;
				return (red << 2 | grn) << 2 | blu;
			case 3: // 12BPP
				red = rgb >> 20;
				grn = rgb >> 12 & 3;
				blu = rgb >> 4 & 3;
				return (red << 4 | grn) << 4 | blu;
			case 4: // 18BPP
				red = rgb >> 18;
				grn = rgb >> 10 & 3;
				blu = rgb >> 2 & 3;
				return (red << 6 | grn) << 6 | blu;
			case 5: // 24BPP
				return rgb;
		}
		return 0;
	}

	@SuppressWarnings("SwitchStatementWithoutDefaultBranch")
	private double input() {
		try {
			switch (ioMode) {
				case 0: // Character
					return System.in.read();
				case 2: // Float
					break;
				case 1: // Integer
					break;
			}
		} catch (IOException ignored) {
		}

		return 0;
	}

	@SuppressWarnings({"SwitchStatementWithoutDefaultBranch", "UseOfSystemOutOrSystemErr"})
	private void print(double value) {
		switch (ioMode) {
			case 0: // Character
				System.out.write((int) Math.round(value));
				System.out.flush();
				break;
			case 2: // Float
				System.out.print(Double.toString(value));
				break;
			case 1: // Integer
				System.out.print(toLong(value));
				break;
		}
	}

	private static double modulo(double numerator, double denominator) {
		return numerator - (int) Math.floor(numerator / denominator) * denominator;
	}

	private static int toInt(double value) {
		return (int) Math.round(value);
	}

	private static long toLong(double value) {
		return Math.round(value);
	}

	private static double fromBool(boolean value) {
		return value ? -1 : 0;
	}
}
