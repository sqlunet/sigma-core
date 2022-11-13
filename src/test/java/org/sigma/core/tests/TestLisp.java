/*
 * Copyright (c) 2022.
 * This code is copyright Bernard Bou <1313ou@gmail.com>
 * This software is released under the GNU Public License 3 <http://www.gnu.org/copyleft/gpl.html>.
 */

package org.sigma.core.tests;

import org.sigma.core.Formula;
import org.sigma.core.IterableFormula;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.sigma.core.NotNull;
import org.sigma.core.Nullable;

import java.util.List;
import java.util.function.Consumer;

import static org.sigma.core.Helpers.OUT;
import static org.junit.jupiter.api.Assertions.*;

public class TestLisp
{
	private static final String[] ATOMS = { //
			"a", "a b", "\"a b\"", "\"a b\"", "\"'a b'\"", "\"a ' b\"", "\"''\"", "'a b'", "''",}; //

	private static final String[] ILL_FORMED_FAIL_ATOMS = { //
			"a \" b",}; //

	private static final String[] FAILED_ATOMS = { //
			"",}; //

	private static final String[] LISTS = { //
			"()", "(a)", "(a b)", "(a b c)", "(a b c \"d e\")", "(a b c 'd e')", "(=> (a ?W) (b ?W))", //
			"(=>" + //
					"  (instance ?AT AutomobileTransmission)" + //
					"  (hasPurpose ?AT" + //
					"    (exists (?C ?D ?A ?R1 ?N1 ?R2 ?R3 ?R4 ?N2 ?N3)" + //
					"      (and" + //
					"        (instance ?C Crankshaft)" + //
					"        (instance ?D Driveshaft)" + //
					"        (instance ?A Automobile)" + //
					"        (part ?D ?A)" + //
					"        (part ?AT ?A)" + //
					"        (part ?C ?A)" + //
					"        (connectedEngineeringComponents ?C ?AT)" + //
					"        (connectedEngineeringComponents ?D ?AT)" + //
					"        (instance ?R1 Rotating)" + //
					"        (instance ?R2 Rotating)" + //
					"        (instance ?R3 Rotating)" + //
					"        (instance ?R4 Rotating)" + //
					"        (patient ?R1 ?C)" + //
					"        (patient ?R2 ?C)" + //
					"        (patient ?R3 ?D)" + //
					"        (patient ?R4 ?D)" + //
					"        (causes ?R1 ?R3)" + //
					"        (causes ?R2 ?R4)" + //
					"        (not" + //
					"          (equal ?R1 ?R2))" + //
					"        (holdsDuring ?R1" + //
					"          (measure ?C (RotationFn ?N1 MinuteDuration)))" + //
					"        (holdsDuring ?R2" + //
					"          (measure ?C (RotationFn ?N1 MinuteDuration)))" + //
					"        (holdsDuring ?R3" + //
					"          (measure ?D (RotationFn ?N2 MinuteDuration)))" + //
					"        (holdsDuring ?R4" + //
					"          (measure ?D (RotationFn ?N3 MinuteDuration)))" + //
					"        (not" + //
					"          (equal ?N2 ?N3))))))", //
	};

	@Test
	public void cdrOnLists()
	{
		cdr(LISTS, f -> Assertions.assertTrue(f.listP()));
	}

	@Test
	public void carOnLists()
	{
		car(LISTS, f -> Assertions.assertTrue(f.listP()));
	}

	@Test
	public void cdrOnAtoms()
	{
		cdr(ATOMS, f -> Assertions.assertFalse(f.listP()));
	}

	public void cdr(@NotNull final String[] fs, @NotNull final Consumer<Formula> test)
	{
		for (@NotNull String list : fs)
		{
			@NotNull Formula f = Formula.of(list);
			test.accept(f);

			@NotNull String car = f.car();
			@NotNull String cdr = f.cdr();
			@Nullable Formula cdrF = f.cdrAsFormula();

			OUT.println("formula=\"" + f + "\"");
			OUT.println("\tcar=" + car);
			OUT.println("\tcdr=" + cdr);
			OUT.println("\tcdrF=" + cdrF);

			int i = 1;
			for (@Nullable Formula itF = f.cdrAsFormula(); itF != null && itF.listP(); itF = itF.cdrAsFormula())
			{
				OUT.println("\tcdrF" + i + "=" + itF);
				if (itF.empty())
				{
					break;
				}
				i += 1;
			}
		}
	}

	public void car(@NotNull final String[] fs, @NotNull final Consumer<IterableFormula> test)
	{
		for (@NotNull String list : fs)
		{
			@NotNull IterableFormula f = new IterableFormula(list);
			test.accept(f);

			OUT.println("formula=\"" + f + "\"");

			int i = 1;
			for (; ; f.pop())
			{
				@NotNull String car = f.car();
				if ("".equals(car))
				{
					break;
				}
				@NotNull Formula carF = Formula.of(car);

				OUT.println("\tcar" + i + "=" + car);
				OUT.println("\tcarF" + i + "=" + carF);
				i += 1;
			}
		}
	}

	@Test
	public void appendNonEmptyList()
	{
		@NotNull Formula f = Formula.of("(a b c d)");
		@NotNull Formula f2 = Formula.of("(e f g)");
		OUT.println("formula=" + f);
		OUT.println("formula2=" + f2);
		@NotNull Formula result = f.append(f2);
		OUT.println("append=" + result);
		assertEquals("(a b c d e f g)", result.form);
	}

	@Test
	public void appendEmptyList()
	{
		@NotNull Formula f = Formula.of("(a b c d)");
		@NotNull Formula f2 = Formula.EMPTY_LIST;
		OUT.println("formula=" + f);
		OUT.println("formula2=" + f2);
		@NotNull Formula result = f.append(f2);
		OUT.println("append=" + result);
		assertEquals("(a b c d)", result.form);
	}

	@Test
	public void appendEmptyListEmptyList()
	{
		@NotNull Formula f = Formula.EMPTY_LIST;
		@NotNull Formula f2 = Formula.EMPTY_LIST;
		OUT.println("formula=" + f);
		OUT.println("formula2=" + f2);
		@NotNull Formula result = f.append(f2);
		OUT.println("append=" + result);
		assertEquals("()", result.form);
	}

	@Test
	public void appendNonList()
	{
		@NotNull Formula f = Formula.of("(a b c d)");
		@NotNull Formula f2 = Formula.of("f");
		OUT.println("formula=" + f);
		OUT.println("formula2=" + f2);
		@NotNull Formula result = f.append(f2);
		OUT.println("append=" + result);
		assertEquals("(a b c d f)", result.form);
	}

	@Test
	public void consEmptyListAtom()
	{
		@NotNull Formula f = Formula.EMPTY_LIST;
		@NotNull Formula f2 = Formula.of("a");
		OUT.println("formula=" + f);
		OUT.println("formula2=" + f2);
		@NotNull Formula result = f.cons(f2);
		OUT.println("cons=" + result);
		assertEquals("(a)", result.form);
	}

	@Test
	public void consEmptyListList()
	{
		@NotNull Formula f = Formula.EMPTY_LIST;
		@NotNull Formula f2 = Formula.of("(a b)");
		OUT.println("formula=" + f);
		OUT.println("formula2=" + f2);
		@NotNull Formula result = f.cons(f2);
		OUT.println("cons=" + result);
		assertEquals("((a b))", result.form);
	}

	@Test
	public void consEmptyListEmptyList()
	{
		@NotNull Formula f = Formula.of(Formula.EMPTY_LIST.form);
		@NotNull Formula f2 = Formula.of(Formula.EMPTY_LIST.form);
		OUT.println("formula=" + f);
		OUT.println("formula2=" + f2);
		@NotNull Formula result = f.cons(f2);
		OUT.println("cons=" + result);
		assertEquals("(())", result.form);
	}

	@Test
	public void consListAtom()
	{
		@NotNull Formula f = Formula.of("(a b c d)");
		@NotNull Formula f2 = Formula.of("e");
		OUT.println("formula=" + f);
		OUT.println("formula2=" + f2);
		@NotNull Formula result = f.cons(f2);
		OUT.println("cons=" + result);
		assertEquals("(e a b c d)", result.form);
	}

	@Test
	public void consListEmptyList()
	{
		@NotNull Formula f = Formula.of("(a b c d)");
		@NotNull Formula f2 = Formula.EMPTY_LIST;
		OUT.println("formula=" + f);
		OUT.println("formula2=" + f2);
		@NotNull Formula result = f.cons(f2);
		OUT.println("cons=" + result);
		assertEquals("(() a b c d)", result.form);
	}

	@Test
	public void elementsEmptyList()
	{
		@NotNull Formula f = Formula.EMPTY_LIST;
		OUT.println("formula=" + f);
		@NotNull List<String> result = f.elements();
		OUT.println("elements=" + result);
		assertTrue(result.isEmpty());
	}

	@Test
	public void elementsList()
	{
		@NotNull Formula f = Formula.of("(a b () c (e f) d)");
		OUT.println("formula=" + f);
		@NotNull List<String> result = f.elements();
		OUT.println("elements=" + result);
		assertEquals("a", result.get(0));
		assertEquals("b", result.get(1));
		assertEquals("()", result.get(2));
		assertEquals("c", result.get(3));
		assertEquals("(e f)", result.get(4));
		assertEquals("d", result.get(5));
	}

	@Test
	public void elementsAtom()
	{
		@NotNull Formula f = Formula.of("a");
		OUT.println("formula=" + f);
		@NotNull List<String> result = f.elements();
		OUT.println("elements=" + result);
		assertTrue(result.isEmpty());
	}

	@Test
	public void elementsAtoms()
	{
		@NotNull Formula f = Formula.of("a b");
		OUT.println("formula=" + f);
		@NotNull List<String> result = f.elements();
		OUT.println("elements=" + result);
		assertTrue(result.isEmpty());
	}

	@Test
	public void listLength()
	{
		@NotNull List<Formula> fs = List.of( //
				Formula.of("(a b c (d e f))"), //
				Formula.of("(=> (foo ?A B) (bar B ?A))"), //
				Formula.of("(forall (?A) (=> (foo ?A B) (bar B ?A)))"), //
				Formula.of("(forall (?A) (=> (foo ?A B) (bar B ?A)) (domain foo 1 Z))") //
		);
		@NotNull List<Integer> is = List.of( //
				4, 3, 3, 4);
		for (@NotNull Formula f : fs)
		{
			OUT.println("formula=" + f);
			int result = f.listLength();
			OUT.println("listlen=" + result);
			OUT.println();
			int i = fs.indexOf(f);
			assertEquals(is.get(i), result);
		}
	}

	@Test
	public void listLengthQueer()
	{
		@NotNull List<Formula> fs = List.of( //
				Formula.of("(a b c) (d e f)"));
		for (@NotNull Formula f : fs)
		{
			OUT.println("formula=" + f);
			int result = f.listLength();
			OUT.println("listlen=" + result);
			OUT.println();
			//assertEquals(4, result);
		}
	}

	@Test
	public void listLengthEmpty()
	{
		@NotNull Formula f = Formula.of("()");
		OUT.println("formula=" + f);
		int result = f.listLength();
		OUT.println("listlen=" + result);
		assertEquals(0, result);
	}

	@Test
	public void listLengthNonList()
	{
		@NotNull Formula f = Formula.of("a");
		OUT.println("formula=" + f);
		int result = f.listLength();
		OUT.println("listlen=" + result);
		assertEquals(-1, result);
	}

	public static void main(String[] args)
	{
		@NotNull TestLisp t = new TestLisp();
		t.carOnLists();
		t.cdrOnLists();
		t.cdrOnAtoms();
		t.appendNonEmptyList();
		t.appendEmptyList();
		t.appendEmptyListEmptyList();
		t.appendNonList();
		t.consEmptyListAtom();
		t.consEmptyListList();
		t.consEmptyListEmptyList();
		t.consListEmptyList();
		t.consListAtom();
		t.elementsAtom();
		t.elementsAtoms();
		t.elementsList();
		t.elementsEmptyList();
	}
}
