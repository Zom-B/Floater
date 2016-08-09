package org.zomb.utilities;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * @deprecated Unsafe, confusing, and no real advantage to using images manually.
 */
// Created 2009-04-04
// Updated 2013-12-10 Split up from {@link PixelWindow}.
// Updated 2014-01-07 Split up to {@link WaveFormChart}
@Deprecated
@SuppressWarnings("PublicField")
public abstract class PixelPanel extends JComponent {
	private PixelImage image = null;    // null means uninitialized.
	private Graphics panelG = null;
	public Graphics2D g = null;
	public int[] pixels = null;

	public PixelPanel() {
		setFocusable(true); // Disabled by default. Allows keyboard listeners.
	}

	/**
	 * This method should be called by the user when the contents of the buffers should be shown on the containing
	 * panel.
	 */
	public synchronized final void repaintNow() {
		// Bugfix: panels flicker when contained in a tab that's "not visible" (isVisible returns true)
		if (panelG != null) {
			// panelG.drawImage(image.image, 0, 0, null);
			//
			// Toolkit.getDefaultToolkit().sync();
			repaint();
		}
	}

	/**
	 * Clears the buffer with the specified paint or color.
	 *
	 * @param color
	 */
	public synchronized void clear(Paint color) {
		image.clear(color);
	}

	/**
	 * Clears the buffer with the specified RGB triple.
	 *
	 * @param rgb
	 */
	public synchronized void clear(int rgb) {
		image.clear(rgb);
	}

	/**
	 * Clears the buffer with the specified background image. The image will be tiled starting at the upper-left.
	 *
	 * @param bgtx
	 */
	public synchronized void clear(BufferedImage bgtx) {
		image.clear(bgtx);
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
	public void fillRect(int x0, int y0, int x1, int y1, int color) {
		image.fillRect(x0, y0, x1, y1, color);
	}

	public BufferedImage getImage() {
		return image.image;
	}

	public BufferedImage getImageCopy() {
		return image.getImageCopy();
	}

	public PixelImage getPixelImage() {
		return image;
	}

	/**
	 * This method is called after the component is shown for the first time and all buffers and fields are valid (they
	 * are initialized as {@code null}).
	 */
	public abstract void initialized();

	/**
	 * This method is called every time the window is resized. Additionally this is also called one time after this
	 * component is first shown, because the Java Swing API doesn't consider that a 'resize'. Subclasses should use
	 * this
	 * to start (re)drawing the contents, as all the buffers and fields are renewed.
	 */
	public abstract void resized();

	/**
	 * Initializes this class as if it were only a {@link PixelImage}. This allows subclasses to inherit the abstract
	 * methods of {@link PixelPanel} without needing to write a separate wrapper class.
	 *
	 * @param g a non-null {@link Graphics} that the image will be drawn to instead the panel.
	 */
	public void initNonDisplayed(Graphics g) {
		panelG = g;

		initializeImage();
	}

	@Override
	public void paintComponent(Graphics g) {
		if (panelG == null) {
			panelG = getGraphics();

			// Is it displayable?
			if (panelG == null) {
				return;
			}

			initializeImage();

			// Notify subclasses.
			initialized();
			resized();
		} else if (getWidth() != image.width || getHeight() != image.height) {
			panelG.dispose();
			panelG = getGraphics();

			initializeImage();

			// Notify subclasses.
			resized();
		}

		// Not the same as repaintNow() !!
		// Uses different Graphics instance to prevent flickering.
		// Took me 3 hours to find even with my original PixelWindow.
		g.drawImage(image.image, 0, 0, this);
	}

	private Graphics initializeImage() {
		image = new PixelImage(getWidth(), getHeight());
		g = image.g;
		pixels = image.pixels;
		return g;
	}
}
