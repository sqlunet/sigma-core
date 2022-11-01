/*
 * Copyright (c) 2022.
 * This code is copyright Bernard Bou <1313ou@gmail.com>
 * This software is released under the GNU Public License 3 <http://www.gnu.org/copyleft/gpl.html>.
 */

package org.sigma.core.tests;

import org.sigma.core.Formula;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class TestEquals
{
	@Test
	public void formatFail()
	{
		var f1 = Formula.of("a");
		var f2 = f1; //Formula.of("a");
		var l1 = List.of(f1);
		var l2 = List.of(f2);
		var la1 = Arrays.asList(f1);
		var la2 = Arrays.asList(f2);
		List<Formula> lf1 = Arrays.asList(f1);
		List<Formula> lf2 = Arrays.asList(f2);

		boolean eqf = f1.equals(f2);
		boolean eqlf = lf1.equals(lf2);
		boolean eqal = la1.equals(la2);
		boolean eqa = l1.equals(l2);
		boolean eqa2 = Objects.equals(l1, l2);
	}
}
