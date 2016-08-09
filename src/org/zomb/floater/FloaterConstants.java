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

/**
 * @author Zom-B
 * @version 1.1
 * @since 1.0
 */
// Created 2014-03-17
public enum FloaterConstants {
	;

	// Mnemonics for all instructions.
	public static final String[][] INSTRUCTIONS = {
			{"NOP"},                                  // 0
			{"PUSH"},                                 // 1
			{"DUP", "SWAP"},                          // 2
			{"GET", "SET"},                           // 3
			{"PAUSE"},                                // 4
			{"NOT", "AND", "OR", "XOR"},              // 5
			{"EQ", "LT", "GT", "SIGN"},               // 6
			{"PRINT", "INPUT", "IOMODE"},             // 7
			{"SET PIXEL", "GET PIXEL", "GFXMODE"},    // 8
			{"RND", "PUSH IP", "PUSH SP"},            // 9
			{"ROUND", "FLOOR", "CEIL", "TRUNC"},      // A
			{"ADD", "SUB", "MUL", "DIV"},             // B
			{"SQRT", "POW", "EXP", "LOG", "LOGBASE"}, // C
			{"SIN", "COS", "TAN", "ATN", "ANGBASE"},  // D
			{"<reserved>"},                           // E
			{"FORWARD", "DEFLECT"}};                  // F

	// The table that translates opcode to color.
	public static final int[] DOS16 = {
			0x000000, 0x0000AA, 0x00AA00, 0x00AAAA,
			0xAA0000, 0xAA00AA, 0xAA5500, 0xAAAAAA,
			0x555555, 0x5555FF, 0x55FF55, 0x55FFFF,
			0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF};

	// The table that translates pre-processed color to opcode.
	public static final int[] REV_DOS16 = {
			0, 1, 1, 9, 2, 3, 1, 9, 2, 2, 3, 11, 10, 10, 11, 11,
			4, 5, 1, 9, 6, 8, 1, 9, 2, 2, 3, 11, 10, 10, 11, 11,
			4, 4, 5, 13, 6, 4, 5, 13, 6, 6, 7, 15, 14, 14, 15, 15,
			12, 12, 13, 13, 12, 12, 13, 13, 14, 14, 15, 15, 14, 14, 15, 15};

	static final int ADDRESS_START = 1;

	public static final String[] DIRECTIONS = {"Down", "Right", "Up", "Left"};
	public static final String[] RUNSTATES = {"Running", "Paused", "Halted"};

	private static final int STEPS_UNTIL_DUPLICATE = 6;

	public static int bigger(int x) {
		return x + (1 << Integer.bitCount(Integer.highestOneBit(x / STEPS_UNTIL_DUPLICATE) - 1));
	}

	public static int smaller(int x) {
		return x <= STEPS_UNTIL_DUPLICATE ?
				STEPS_UNTIL_DUPLICATE :
				x - (1 << Integer.bitCount(Integer.highestOneBit(x / (STEPS_UNTIL_DUPLICATE + 1)) - 1));
	}
}
