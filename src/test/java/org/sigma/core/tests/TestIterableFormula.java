/*
 * Copyright (c) 2022.
 * This code is copyright Bernard Bou <1313ou@gmail.com>
 * This software is released under the GNU Public License 3 <http://www.gnu.org/copyleft/gpl.html>.
 */

package org.sigma.core.tests;

import org.junit.jupiter.api.Test;
import org.sigma.core.IterableFormula;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.sigma.core.Helpers.OUT;

public class TestIterableFormula
{
	@Test
	public void testEmptyString()
	{
		var f = "";
		OUT.printf("[%s]%n", f);
		IterableFormula itF = new IterableFormula(f);
		OUT.printf("\tempty=%b car=%s cdr=%s %n", itF.empty(), itF.car(), itF.cdr());
		assertFalse(itF.empty());
		assertEquals("", itF.car());
		assertEquals("", itF.cdr());
		assertThrows(IllegalStateException.class, itF::pop, "IllegalStateException not thrown");
		OUT.println();
	}

	@Test
	public void testAtoms()
	{
		var fs = List.of("a", "\"a\"");
		for (var f : fs)
		{
			OUT.printf("[%s]%n", f);
			IterableFormula itF = new IterableFormula(f);
			OUT.printf("\tempty=%b car=%s cdr=%s %n", itF.empty(), itF.car(), itF.cdr());
			assertFalse(itF.empty());
			assertEquals("", itF.car());
			assertEquals("", itF.cdr());
			assertThrows(IllegalStateException.class, itF::pop, "IllegalStateException not thrown");
			OUT.println();
		}
	}

	@Test
	public void testLists()
	{
		var fs = List.of("(a)", "(a b c)", "(a)", "((a) (b) c)", "()");
		for (var f : fs)
		{
			OUT.printf("%s%n", f);
			int i = 0;
			for (IterableFormula itF = new IterableFormula(f); !itF.empty(); itF.pop())
			{
				OUT.printf("\t[%d] car=%s cdr=%s %n", ++i, itF.car(), itF.cdr());
			}
			OUT.println();
		}
	}

	@Test
	public void testCar()
	{
		var fs = List.of("(a)", "(a b c)", "(a)", "((a) (b) c)", "()");
		for (var f : fs)
		{
			OUT.printf("%s%n", f);
			int i = 0;
			for (IterableFormula itF = new IterableFormula(f); !itF.empty(); itF.pop())
			{
				OUT.printf("\t[%d] car=%s%n", ++i, itF.car());
			}
			OUT.println();
		}
	}

	@Test
	public void testCdr()
	{
		var fs = List.of("(a)", "(a b c)", "(a)", "((a) (b) c)", "()");
		for (var f : fs)
		{
			OUT.printf("%s%n", f);
			int i = 0;
			for (IterableFormula itF = new IterableFormula(f); !itF.empty(); itF.pop())
			{
				OUT.printf("\t[%d] cdr=%s%n", ++i, itF.cdr());
			}
			OUT.println();
		}
	}

	public static void main(String[] args)
	{
		TestIterableFormula t = new TestIterableFormula();
		t.testLists();
		t.testAtoms();
	}
}
