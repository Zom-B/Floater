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
// Created 2005-11-29
// Renamed and simplified 2016-08-08
public class Instruction {
	public final int opcode;
	public final int param;

	public Instruction(int opcode, int param) {
		this.opcode = opcode;
		this.param = param;
	}
}
