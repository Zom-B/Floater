package org.zomb.utilities;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Arrays;

/**
 * @deprecated Too specialized (part of PixelPanel)
 */
// Created 2014-03-01
@Deprecated
public class PixelImage {
	public final int width;
	public final int height;
	public final BufferedImage image;
	public final Graphics2D g;
	public final int[] pixels;

	public PixelImage(int width, int height) {
		// Clamp to minimum values.
		this.width = Math.max(width, 1);
		this.height = Math.max(height, 1);

		image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		image.setAccelerationPriority(0);
		pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
		g = image.createGraphics();
	}

	/**
	 * Clears the buffer with the specified paint or color.
	 *
	 * @param paint
	 */
	public synchronized void clear(Paint paint) {
		g.setPaint(paint);
		g.fillRect(0, 0, width, height);
	}

	/**
	 * Clears the buffer with the specified RGB triple.
	 *
	 * @param rgb
	 */
	public synchronized void clear(int rgb) {
		Arrays.fill(pixels, rgb);
	}

	/**
	 * Clears the buffer with the specified background image. The image will be tiled starting at the upper-left.
	 *
	 * @param bgtx
	 */
	public synchronized void clear(BufferedImage bgtx) {
		TexturePaint p = new TexturePaint(bgtx, new Rectangle2D.Float(0, 0, bgtx.getWidth(), bgtx.getHeight()));
		g.setPaint(p);
		g.fillRect(0, 0, width, height);
	}

	/**
	 * Fills a rectangular area between (x0, y0) inclusive and (x1, y1) exclusive.
	 *
	 * @param x0    the left side (inclusive)
	 * @param y0    the top side (inclusive)
	 * @param x1    the right side (exclusive)
	 * @param y1    the bottom side (exclusive)
	 * @param color
	 */
	public synchronized void fillRect(int x0, int y0, int x1, int y1, int color) {
		if (x0 < 0) {
			x0 = 0;
		}
		if (y0 < 0) {
			y0 = 0;
		}
		if (x1 > width) {
			x1 = width;
		}
		if (y1 > height) {
			y1 = height;
		}

		int p = x0 + y0 * width;
		for (int y = y0; y < y1; y++) {
			for (int x = x0; x < x1; x++) {
				pixels[p++] = color;
			}
			p += width + x0 - x1;
		}
	}

	public synchronized BufferedImage getImageCopy() {
		BufferedImage imageCopy = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

		imageCopy.setAccelerationPriority(0);
		int[] pixelsCopy = ((DataBufferInt) imageCopy.getRaster().getDataBuffer()).getData();
		System.arraycopy(pixels, 0, pixelsCopy, 0, pixelsCopy.length);

		return imageCopy;
	}
}
