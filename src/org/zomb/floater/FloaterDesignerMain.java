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

import org.zomb.utilities.CustomFileFilter;
import org.zomb.utilities.MutableTableModel;
import org.zomb.utilities.PixelPanel;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.MouseInputListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Zom-B
 * @version 1.1
 * @since 1.0
 */
// Created 2014-03-17
// Changed 2016-08-08 Bugfix 1.1 VK_DOWN caused wrap depending on image width
public class FloaterDesignerMain extends JFrame implements WindowListener, MouseInputListener, ActionListener,
		DropTargetListener {
	private static final String TITLE = "Floater designer";
	private static final Stroke DEFAULT_STROKE = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);

	public static void main(String... args) {
		SwingUtilities.invokeLater(() -> {
			try {
				UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
			} catch (ReflectiveOperationException | UnsupportedLookAndFeelException ignored) {
			}

			new FloaterDesignerMain();
		});
	}

	// Reusable shapes for drawing in the EDT.
	private static final Ellipse2D.Float ELLIPSE = new Ellipse2D.Float();
	private static final Line2D.Float LINE = new Line2D.Float();
	private static final Rectangle2D.Float RECTANGLE = new Rectangle2D.Float();

	private final MutableTableModel stackModel = new MutableTableModel(new String[]{"Addr", "Value"});
	private final MutableTableModel instructionModel = new MutableTableModel(new String[]{"Mnemonic", "Instruction"});

	private final JButton newButton = new JButton("New");
	private final JButton loadButton = new JButton("Load");
	private final JButton revertButton = new JButton("Revert");
	private final JButton saveButton = new JButton("Save");
	private final JButton saveAsButton = new JButton("Save As...");
	private final JButton narrowerButton = new JButton("◄");
	private final JButton widerButton = new JButton("►");
	private final JButton flatterButton = new JButton("▲");
	private final JButton higherButton = new JButton("▼");
	private final JButton leftButton = new JButton("←");
	private final JButton rightButton = new JButton("→");
	private final JButton upButton = new JButton("↑");
	private final JButton downButton = new JButton("↓");
	private final JTextArea statusArea = new JTextArea(3, 3);
	private final JTable stackTable = new JTable(stackModel);
	private final JTable instructionTable = new JTable(instructionModel);
	private final PixelPanel imagePanel = new PixelPanel() {
		@Override
		public void initialized() {
			FloaterDesignerMain.this.initialized();
		}

		@Override
		public void resized() {
			newZoom();

			imagePanel.g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			imagePanel.g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			imagePanel.g.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
			imagePanel.g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
					RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
			imagePanel.g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints
					.VALUE_FRACTIONALMETRICS_ON);
			imagePanel.g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
					RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
			imagePanel.g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
					RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
			imagePanel.g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints
					.VALUE_COLOR_RENDER_QUALITY);
			imagePanel.g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
			imagePanel.g.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
			imagePanel.g.translate(0.5, 0.5);

			FloaterDesignerMain.this.update(true, false);
		}
	};
	private final JButton resetButton = new JButton("Reset");
	private final JButton stepButton = new JButton("Step");
	private final JToggleButton runButton = new JToggleButton("Run");
	private final JToggleButton run2Button = new JToggleButton("Run quick");

	private final Timer timer = new Timer(0, this);
	private boolean fast;
	private final FloaterInterpreter vm = new FloaterInterpreter();
	private BufferedImage doubleBuffer;
	private int mouseX;
	private int mouseY;
	private int mouseButton;
	private int selectedInstruction;
	private File openedFile;
	private boolean modified;
	private BufferedImage imgBackup;
	private float zoom;

	public FloaterDesignerMain() {
		super(TITLE);
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		addWindowListener(this);

		{
			getContentPane().setLayout(new BorderLayout());

			{
				JPanel p2 = new JPanel(new BorderLayout());

				p2.add(statusArea, BorderLayout.PAGE_START);
				{
					JScrollPane sp = new JScrollPane();
					sp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
					sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
					sp.getViewport().setView(stackTable);
					sp.setPreferredSize(new Dimension(223, 200));
					p2.add(sp, BorderLayout.CENTER);
				}

				getContentPane().add(p2, BorderLayout.LINE_START);
			}
			{
				JPanel p = new JPanel(new BorderLayout());

				{
					JToolBar t = new JToolBar();

					t.add(newButton);
					t.add(loadButton);
					t.add(revertButton);
					t.add(saveButton);
					t.add(saveAsButton);
					t.addSeparator();
					t.add(new JLabel("Size:"));
					t.add(narrowerButton);
					t.add(higherButton);
					t.add(flatterButton);
					t.add(widerButton);
					t.addSeparator();
					t.add(new JLabel("Move:"));
					t.add(leftButton);
					t.add(downButton);
					t.add(upButton);
					t.add(rightButton);
					t.addSeparator();

					p.add(t, BorderLayout.PAGE_START);
				}
				p.add(imagePanel, BorderLayout.CENTER);
				{
					JPanel p2 = new JPanel(new FlowLayout(FlowLayout.CENTER));

					p2.add(resetButton);
					p2.add(stepButton);
					p2.add(runButton);
					p2.add(run2Button);

					p.add(p2, BorderLayout.PAGE_END);
				}

				getContentPane().add(p, BorderLayout.CENTER);
			}
			{
				JScrollPane sp = new JScrollPane();
				sp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
				sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
				sp.getViewport().setView(instructionTable);
				sp.setPreferredSize(new Dimension(196, 200));
				getContentPane().add(sp, BorderLayout.LINE_END);
			}
		}

		stackTable.setCellSelectionEnabled(false);
		stackTable.setRowSelectionAllowed(false);
		stackTable.setColumnSelectionAllowed(false);
		stackTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		stackTable.getColumnModel().getColumn(0).setPreferredWidth(51);
		stackTable.getColumnModel().getColumn(1).setPreferredWidth(151);
		instructionTable.setCellSelectionEnabled(false);
		instructionTable.setRowSelectionAllowed(true);
		instructionTable.setColumnSelectionAllowed(false);
		instructionTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		instructionTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		instructionTable.setForeground(Color.LIGHT_GRAY);
		instructionTable.setBackground(Color.BLACK);
		instructionTable.getTableHeader().setReorderingAllowed(false);
		instructionTable.getColumnModel().setColumnMargin(0);
		instructionTable.getColumnModel().getColumn(0).setPreferredWidth(80);
		instructionTable.getColumnModel().getColumn(1).setPreferredWidth(95);
		instructionTable.getColumnModel().getColumn(1)
				.setCellRenderer(new InstructionTableCellRenderer());
		instructionTable.setRowSorter(new TableRowSorter<>(instructionModel));

		prepareInstructionTable();
		instructionTable.setRowSelectionInterval(1, 1);
		selectedInstruction = ((Instruction) instructionTable.getValueAt(instructionTable.getSelectedRow(), 1)).opcode;

		setSize(1024, 768);
		setLocationRelativeTo(null);
		setVisible(true);
	}

	private void initialized() {
		newImage();

		newButton.addActionListener(this);
		loadButton.addActionListener(this);
		revertButton.addActionListener(this);
		saveButton.addActionListener(this);
		saveAsButton.addActionListener(this);
		narrowerButton.addActionListener(this);
		widerButton.addActionListener(this);
		flatterButton.addActionListener(this);
		higherButton.addActionListener(this);
		leftButton.addActionListener(this);
		rightButton.addActionListener(this);
		upButton.addActionListener(this);
		downButton.addActionListener(this);
		resetButton.addActionListener(this);
		stepButton.addActionListener(this);
		runButton.addActionListener(this);
		run2Button.addActionListener(this);

		instructionTable.addMouseListener(this);

		imagePanel.addMouseListener(this);
		imagePanel.addMouseMotionListener(this);
		imagePanel.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
		imagePanel.setDropTarget(new DropTarget(imagePanel, this));

		imagePanel.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "VK_LEFT");
		imagePanel.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "VK_RIGHT");
		imagePanel.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "VK_UP");
		imagePanel.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "VK_DOWN");
		imagePanel.getActionMap().put("VK_LEFT", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (vm.dir != 1) {
					vm.dir = 3;
				}
				vm.ipx = vm.ipx > 0 ? vm.ipx - 1 : 0;
				vm.fetch();
				update(false, false);
			}
		});
		imagePanel.getActionMap().put("VK_RIGHT", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (vm.dir != 3) {
					vm.dir = 1;
				}
				vm.ipx = vm.ipx < vm.img.getWidth() - 1 ? vm.ipx + 1 : vm.img.getWidth() - 1;
				vm.fetch();
				update(false, false);
			}
		});
		imagePanel.getActionMap().put("VK_UP", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (vm.dir != 0) {
					vm.dir = 2;
				}
				vm.ipy = vm.ipy > 0 ? vm.ipy - 1 : 0;
				vm.fetch();
				update(false, false);
			}
		});
		imagePanel.getActionMap().put("VK_DOWN", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (vm.dir != 2) {
					vm.dir = 0;
				}
				vm.ipy = vm.ipy < vm.img.getHeight() - 1 ? vm.ipy + 1 : vm.img.getHeight() - 1;
				vm.fetch();
				update(false, false);
			}
		});
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();

		if (o == timer) {
			vm.execute();
			if (vm.runState > 0) {
				stop();
			}
			if (vm.runState < 2) {
				vm.fetch();
			}
			if (!fast) {
				update(true, true);
			}
		} else if (o == newButton) {
			newImage();
		} else if (o == loadButton) {
			JFileChooser chooser = new JFileChooser(".");
			chooser.setMultiSelectionEnabled(false);
			chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			chooser.setAcceptAllFileFilterUsed(false);
			FileFilter selectedFilter = new CustomFileFilter("PNG files", "png");
			chooser.addChoosableFileFilter(selectedFilter);
			chooser.setAcceptAllFileFilterUsed(true);
			chooser.setFileFilter(selectedFilter);
			while (true) {
				int returnVal = chooser.showOpenDialog(this);
				if (returnVal != JFileChooser.APPROVE_OPTION) {
					return;
				}

				openedFile = chooser.getSelectedFile();
				if (openedFile.exists()) {
					try {
						loadImage(openedFile);
						return;
					} catch (IOException ex) {
						JOptionPane.showMessageDialog(this, ex.getMessage(), getTitle(), JOptionPane.ERROR_MESSAGE);
						Logger.getGlobal().log(Level.WARNING, null, ex);
					}
				}
			}
		} else if (o == revertButton) {
			try {
				if (openedFile == null) {
					revertImage();
				} else {
					loadImage(openedFile);
				}
			} catch (IOException ex) {
				JOptionPane.showMessageDialog(this, ex.getMessage(), getTitle(), JOptionPane.ERROR_MESSAGE);
				Logger.getGlobal().log(Level.WARNING, null, ex);
			}
		} else if (o == saveButton || o == saveAsButton) {
			saveImage(o == saveAsButton);
		} else if (o == narrowerButton) {
			resizeImg(0, 0, FloaterConstants.smaller(vm.img.getWidth()), vm.img.getHeight
					());
			update(true, true);
		} else if (o == widerButton) {
			resizeImg(0, 0, FloaterConstants.bigger(vm.img.getWidth()), vm.img.getHeight());
			update(true, true);
		} else if (o == flatterButton) {
			resizeImg(0, 0, vm.img.getWidth(), FloaterConstants.smaller(vm.img.getHeight
					()));
			update(true, true);
		} else if (o == higherButton) {
			resizeImg(0, 0, vm.img.getWidth(), FloaterConstants.bigger(vm.img.getHeight()));
			update(true, true);
		} else if (o == leftButton) {
			resizeImg(-1, 0, vm.img.getWidth(), vm.img.getHeight());
			update(true, true);
		} else if (o == rightButton) {
			resizeImg(1, 0, vm.img.getWidth(), vm.img.getHeight());
			update(true, true);
		} else if (o == upButton) {
			resizeImg(0, -1, vm.img.getWidth(), vm.img.getHeight());
			update(true, true);
		} else if (o == downButton) {
			resizeImg(0, 1, vm.img.getWidth(), vm.img.getHeight());
			update(true, true);
		} else if (o == resetButton) {
			if (timer.isRunning()) {
				stop();
			}
			vm.reset();
			update(false, true);
		} else if (o == stepButton) {
			if (timer.isRunning()) {
				stop();
			} else {
				if (vm.runState > 1) {
					vm.reset();
				} else {
					vm.execute();
					vm.fetch();
				}
				update(true, true);
			}
		} else if (o == runButton) {
			if (runButton.isSelected()) {
				if (vm.runState > 1) {
					vm.reset();
				}
				vm.runState = 0;
				fast = false;
				timer.start();
			} else {
				stop();
			}
		} else if (o == run2Button) {
			if (run2Button.isSelected()) {
				if (vm.runState > 1) {
					vm.reset();
				}
				vm.runState = 0;
				fast = true;
				timer.start();
			} else {
				stop();
			}
		}
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}

	@Override
	public void mousePressed(MouseEvent e) {
		mouseButton = e.getButton();
		if (e.getSource() != instructionTable) {
			if (e.getSource() == imagePanel) {
				if (mouseButton == MouseEvent.BUTTON2) {
					mouseX = (int) (e.getX() / zoom);
					mouseY = (int) (e.getY() / zoom);

					if (mouseX < 0 || mouseY < 0 || mouseX >= vm.img.getWidth() || mouseY >= vm.img.getHeight()) {
						mouseX = -1; // Flags absence of cursor.
						return;
					}

					vm.ipx = mouseX;
					vm.ipy = mouseY;
					vm.runState = 0;
					vm.fetch();

					update(false, false);
					return;
				}

				mouseDragged(e);
			}
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (e.getSource() == instructionTable) {
			int selectedRow = instructionTable.getSelectedRow();
			selectedInstruction = ((Instruction) instructionTable.getValueAt(selectedRow, 1)).opcode;
			instructionTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			instructionTable.setRowSelectionInterval(selectedRow, selectedRow);
		}
	}

	@Override
	public void mouseClicked(MouseEvent e) {
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		mouseX = (int) (e.getX() / zoom);
		mouseY = (int) (e.getY() / zoom);

		if (mouseX < 0 || mouseY < 0 || mouseX >= vm.img.getWidth() || mouseY >= vm.img.getHeight()) {
			mouseX = -1; // Flags absence of cursor.
		}

		update(false, false);
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		int x = (int) (e.getX() / zoom);
		int y = (int) (e.getY() / zoom);

		if (mouseButton == MouseEvent.BUTTON2) {
			if (x == mouseX && y == mouseY)
				return;

			vm.dir = (int) Math.round(Math.atan2(x - mouseX, y - mouseY) / 1.57 + 4) % 4;
			vm.fetch();

			update(false, false);
		} else {
			mouseX = x;
			mouseY = y;

			if (x < 0 || y < 0 || x >= vm.img.getWidth() || y >= vm.img.getHeight()) {
				mouseX = -1; // Flags absence of cursor.
			} else {
				if (mouseButton == MouseEvent.BUTTON1) {
					int color = FloaterInterpreter.decodeOpcode(vm.getRawPixel(mouseX, mouseY));
					if ((e.getModifiersEx() & (InputEvent.SHIFT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK)) != 0) {
						selectedInstruction = color;

						instructionTable.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
						int start = -1;
						for (int i = 0; i < instructionModel.getRowCount(); i++) {
							Instruction instruction = (Instruction) instructionModel.getValueAt(i, 1);
							if (instruction.opcode == selectedInstruction && start == -1) {
								start = i;
							} else if (start != -1 && instruction.opcode != selectedInstruction) {
								instructionTable.setRowSelectionInterval(start, i - 1);
								break;
							}
						}
					} else {
						modified |= selectedInstruction != color;
						vm.setPixel(mouseX, mouseY, selectedInstruction);
						vm.fetch();
					}
				} else if (mouseButton == MouseEvent.BUTTON3) {
					modified |= vm.getPixel(mouseX, mouseY) != 0;
					vm.setPixel(mouseX, mouseY, 0);
					vm.fetch();
				}
			}

			update(true, false);
		}
	}

	@Override
	public void windowActivated(WindowEvent e) {
	}

	@Override
	public void windowClosed(WindowEvent e) {
	}

	@Override
	public void windowClosing(WindowEvent e) {
		if (modified) {
			int result = JOptionPane
					.showConfirmDialog(this, "Save image?", getTitle(), JOptionPane.YES_NO_CANCEL_OPTION);

			if (result == JOptionPane.CANCEL_OPTION) {
				return;
			} else if (result == JOptionPane.YES_OPTION) {
				if (!saveImage(false)) {
					return;
				}
			}
		}

		dispose();
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
	}

	@Override
	public void windowIconified(WindowEvent e) {
	}

	@Override
	public void windowOpened(WindowEvent e) {
	}

	@Override
	public void dragEnter(DropTargetDragEvent dtde) {
	}

	@Override
	public void dragExit(DropTargetEvent dte) {
	}

	@Override
	public void dragOver(DropTargetDragEvent dtde) {
	}

	@Override
	@SuppressWarnings("unchecked")
	public void drop(DropTargetDropEvent dtde) {
		try {
			Transferable transferable = dtde.getTransferable();

			if (!transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
				return;
			}

			dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);

			List<File> files = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);

			loadImage(files.get(0));
		} catch (UnsupportedFlavorException | IOException ex) {
			Logger.getGlobal().log(Level.WARNING, null, ex);
		} finally {
			dtde.getDropTargetContext().dropComplete(true);
		}
	}

	@Override
	public void dropActionChanged(DropTargetDragEvent dtde) {
	}

	private void prepareInstructionTable() {
		for (int color = 0; color < FloaterConstants.INSTRUCTIONS.length; color++) {
			String[] instructionsForColor = FloaterConstants.INSTRUCTIONS[color];
			for (int count = 0; count < instructionsForColor.length; count++) {
				instructionModel.add(new Object[]{instructionsForColor[count], new Instruction(color, count)});
			}
		}
	}

	private void newZoom() {
		int width = vm.img.getWidth();
		int height = vm.img.getHeight();
		zoom = Math.min((imagePanel.getWidth() - 1.0f) / width, (imagePanel.getHeight() - 1.0f) / height);
	}

	private void newImage() {
		imgBackup = new BufferedImage(24, 24, BufferedImage.TYPE_INT_RGB);
		vm.setImage(imgBackup);

		// Create copy by resizing to the same size.
		resizeImg(0, 0, imgBackup.getWidth(), imgBackup.getHeight());

		modified = false;
		openedFile = null;
		update(true, true);
	}

	private void loadImage(File file) throws IOException {
		imgBackup = ImageIO.read(file);
		vm.setImage(imgBackup);

		// Create copy by resizing to the same size.
		resizeImg(0, 0, imgBackup.getWidth(), imgBackup.getHeight());

		modified = false;
		update(true, true);
	}

	private void revertImage() throws IOException {
		if (openedFile == null) {
			vm.setImage(imgBackup);

			// Create copy by resizing to the same size.
			resizeImg(0, 0, imgBackup.getWidth(), imgBackup.getHeight());

			update(true, true);
		} else {
			loadImage(openedFile);
		}
	}

	private boolean saveImage(boolean saveAs) {
		File file = saveAs ? null : this.openedFile;
		JFileChooser chooser = new JFileChooser(".");
		chooser.setMultiSelectionEnabled(false);
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		chooser.setAcceptAllFileFilterUsed(false);
		CustomFileFilter selectedFilter = new CustomFileFilter("PNG files", "png");
		chooser.addChoosableFileFilter(selectedFilter);
		chooser.setAcceptAllFileFilterUsed(true);
		chooser.setFileFilter(selectedFilter);
		if (file != null) {
			chooser.setSelectedFile(file);
		}
		while (true) {
			if (file == null) {
				int returnVal = chooser.showSaveDialog(this);
				if (returnVal != JFileChooser.APPROVE_OPTION) {
					return false;
				}
				file = chooser.getSelectedFile();
			}

			if (!file.exists()) {
				if (file.getName().lastIndexOf('.') <= 0) {
					file = new File(file.getPath() + ".png");
				}
			}

			try (OutputStream out = new BufferedOutputStream(new FileOutputStream(file))) {
				ImageIO.write(vm.img, "png", out);

				this.openedFile = file;
				modified = false;
				return true;
			} catch (IOException ex) {
				JOptionPane.showMessageDialog(this, ex.getMessage(), getTitle(), JOptionPane.ERROR_MESSAGE);
				Logger.getGlobal().log(Level.WARNING, null, ex);
				file = null;
			}
		}
	}

	private void resizeImg(int x, int y, int w, int h) {
		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		g.drawImage(vm.img, x, y, null);
		g.dispose();

		vm.setImage(img);
		newZoom();
	}

	private void stop() {
		timer.stop();
		runButton.setSelected(false);
		run2Button.setSelected(false);
		if (fast) {
			fast = false;
			update(true, true);
		}
	}

	private void update(boolean redrawImage, boolean updateStack) {
		assert SwingUtilities.isEventDispatchThread();

		drawProgram(redrawImage || fast);

		if (vm.runState < 2) {
			drawInstructionPointer();

			if (zoom >= 2)
				drawParamFlood();
		}

		if (zoom >= 4 && mouseX >= 0)
			drawCursor();

		updateStatus();

		if (updateStack)
			updateStackTable();

		updateTitle();
	}

	private void drawProgram(boolean redrawImage) {
		if (redrawImage) {
			int width = vm.img.getWidth();
			int height = vm.img.getHeight();

			newZoom();
			imagePanel.clear(0);

			// Draw image.
			drawImage();

			// Draw grid.
			imagePanel.g.setPaint(new Color(0x404040));
			if (zoom >= 3) {
				for (int x = 1; x < width; x++) {
					LINE.setLine(x * zoom, -1, x * zoom, height * zoom);
					imagePanel.g.draw(LINE);
				}
				for (int y = 1; y < height; y++) {
					LINE.setLine(-1, y * zoom, width * zoom, y * zoom);
					imagePanel.g.draw(LINE);
				}
			}
			RECTANGLE.setFrame(-1, -1, width * zoom + 1, height * zoom + 1);
			imagePanel.g.draw(RECTANGLE);

			// Make doubleBuffer.
			doubleBuffer = imagePanel.getImageCopy();
		} else {
			// Draw doubleBuffer.
			imagePanel.g.translate(-0.5, -0.5);
			imagePanel.g.drawImage(doubleBuffer, 0, 0, null);
			imagePanel.g.translate(0.5, 0.5);
		}
	}

	private void drawInstructionPointer() {
		Color contrastColor = vm.opcode < 7 || vm.opcode == 8 ? Color.WHITE : Color.BLACK;

		imagePanel.g.setPaint(contrastColor);

		// Ring
		if (zoom >= 3) {
			float x0 = (vm.ipx + 0.2f) * zoom;
			float y0 = (vm.ipy + 0.2f) * zoom;
			float x1 = (vm.ipx + 0.8f) * zoom;
			float y1 = (vm.ipy + 0.8f) * zoom;
			ELLIPSE.setFrameFromDiagonal(x0, y0, x1, y1);
			imagePanel.g.draw(ELLIPSE);
		}

		// Stick
		if (zoom >= 4) {
			float x = (vm.ipx + 0.5f) * zoom;
			float y = (vm.ipy + 0.5f) * zoom;
			float dx = (1 - Math.abs(1 - vm.dir)) * zoom * 0.5f;
			float dy = (Math.abs(2 - vm.dir) - 1) * zoom * 0.5f;
			LINE.setLine(x, y, x + dx, y + dy);
			imagePanel.g.draw(LINE);
		}
	}

	private void drawParamFlood() {
		int width = vm.img.getWidth();
		int height = vm.img.getHeight();

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				if (x == vm.ipx && y == vm.ipy) {
					// Don't draw where the IP is at.
				} else if (vm.flooded[y][x] > 0) {
					drawParamDot(x, y);
				} else if (zoom > 5 && vm.flooded[y][x] < 0) {
					drawParamCross(x, y);
				}
			}
		}
	}

	private void drawImage() {
		int width = vm.img.getWidth();
		int height = vm.img.getHeight();

		// Images start at the top-left corner of a pixel instead of the center.
		imagePanel.g.translate(-0.5, -0.5);
		imagePanel.g.drawImage(vm.img, 0, 0, (int) (width * zoom) + 1, (int) (height * zoom) + 1, 0, 0, width, height,
				null);
		imagePanel.g.translate(0.5, 0.5);
	}

	private void drawCursor() {
		float wallThickness = zoom / 10;

		imagePanel.g.setPaint(new Color(FloaterConstants.DOS16[selectedInstruction]));

		Stroke stroke = new BasicStroke(wallThickness, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER);
		imagePanel.g.setStroke(stroke);

		RECTANGLE.setFrame(mouseX * zoom + wallThickness / 2,
				mouseY * zoom + wallThickness / 2,
				zoom - wallThickness, zoom - wallThickness);
		imagePanel.g.draw(RECTANGLE);
		imagePanel.g.setStroke(DEFAULT_STROKE);
	}

	private void drawParamDot(int x, int y) {
		float x0 = (x + 0.275f) * zoom;
		float y0 = (y + 0.275f) * zoom;
		float x1 = (x + 0.725f) * zoom;
		float y1 = (y + 0.725f) * zoom;

		ELLIPSE.setFrameFromDiagonal(x0, y0, x1, y1);
		imagePanel.g.fill(ELLIPSE);
	}

	private void drawParamCross(int x, int y) {
		float x0 = (x + 0.25f) * zoom;
		float y0 = (y + 0.25f) * zoom;
		float x1 = (x + 0.75f) * zoom;
		float y1 = (y + 0.75f) * zoom;

		LINE.setLine(x0, y0, x1, y1);
		imagePanel.g.draw(LINE);

		LINE.setLine(x0, y1, x1, y0);
		imagePanel.g.draw(LINE);
	}

	private void updateStatus() {
		StringBuilder status = new StringBuilder(64);
		status.append("IP: (").append(vm.ipx + FloaterConstants.ADDRESS_START).append(", ")
				.append(vm.ipy + FloaterConstants.ADDRESS_START).append(") ");
		status.append(FloaterConstants.DIRECTIONS[vm.dir]);
		status.append("\nInstruction: ");

		if (vm.runState > 1) {
			status.append('<').append(FloaterConstants.RUNSTATES[vm.runState].toLowerCase()).append('>');
		} else {
			int index = vm.opcode < 2 ? 0 : vm.param - 1;
			if (index >= FloaterConstants.INSTRUCTIONS[vm.opcode].length) {
				status.append("Error!");
			} else {
				status.append(FloaterConstants.INSTRUCTIONS[vm.opcode][index]);
				if (vm.opcode == 1) {
					status.append(' ');
					status.append(Integer.toString(vm.param));
					if (vm.param >= 32 && vm.param <= 127) {
						status.append(" \"");
						status.append((char) vm.param);
						status.append('"');
					}
				}
			}
		}

		status.append("\nX: ").append(mouseX + FloaterConstants.ADDRESS_START)
				.append(", Y: ").append(mouseY + FloaterConstants.ADDRESS_START);

		statusArea.setText(status.toString());
	}

	private void updateStackTable() {
		stackModel.clear();

		for (int i = 0; i <= vm.sp; i++) {
			StringBuilder address = new StringBuilder(8);
			address.append(Integer.toString(i + FloaterConstants.ADDRESS_START));
			if (i < vm.sp - 1)
				address.append(" / ").append(Integer.toString(i - vm.sp + 1));

			double value = vm.get(i);
			long longValue = Math.round(value);

			StringBuilder text = new StringBuilder(20);
			if (longValue >= 32 && longValue <= 127)
				text.append('"').append((char) longValue).append("\" ");

			if (longValue == value)
				text.append(Long.toString(longValue));
			else
				text.append(Double.toString(value));

			stackModel.add(new Object[]{address, text.toString()});
		}
	}

	private void updateTitle() {
		StringBuilder title = new StringBuilder(64);
		title.append(TITLE);
		title.append(" [");
		title.append(openedFile == null ? "new program" : openedFile.getName());
		if (modified) {
			title.append(" *");
		}
		title.append(']');
		setTitle(title.toString());

		imagePanel.repaintNow();
		imagePanel.requestFocus();
	}
}
