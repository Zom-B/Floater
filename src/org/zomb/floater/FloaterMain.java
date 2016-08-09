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

import javax.imageio.ImageIO;
import java.io.*;

/**
 * @author Zom-B
 * @version 1.1
 * @since 1.0
 */
// Created 2014-03-28
@SuppressWarnings({"UseOfSystemOutOrSystemErr", "CallToSystemExit"})
public enum FloaterMain {
	;

	public static void main(String... args) throws IOException {
		if (args.length == 0)
			showCommandLineSyntax();

		File inFile = new File(args[0]);

		File outFile = null;
		if (args.length > 1) {
			outFile = new File(args[1]);
			if (inFile.equals(outFile)) {
				System.out.println("Both files cannot be te same.");
				System.exit(-2);
			}
		}

		FloaterInterpreter vm = new FloaterInterpreter();
		vm.setImage(ImageIO.read(inFile));

		simulate(vm);

		if (outFile != null) {
			try (OutputStream out = new BufferedOutputStream(new FileOutputStream(outFile))) {
				ImageIO.write(vm.img, "png", out);
			}
		}
	}

	private static void showCommandLineSyntax() {
		System.out.println("Floater (esoteric programming language) v1.1 by Zom-B");
		System.out.println("https://github.com/Zom-B/Floater");
		System.out.println("http://esolangs.org/wiki/Floater");
		System.out.println();
		System.out.println("Usage:");
		System.out.println("java -jar floater.jar <program>.png");
		System.out.println("  Discards changes to the program memory");
		System.out.println("java -jar floater.jar <program>.png <output>.png");
		System.out.println("  Saves changes to the program memory");
		System.out.println();
		System.out.println("If the program doesn't terminate, abort with CTRL+C.");
		System.out.println("No output image will be saved when aborted.");
		System.exit(-1);
	}

	private static void simulate(FloaterInterpreter vm) {
		while (vm.runState < 2) {
			vm.execute();
			vm.fetch();
		}
	}

}
