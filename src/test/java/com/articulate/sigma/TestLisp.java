package com.articulate.sigma;

import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static com.articulate.sigma.Utils.OUT;
import static org.junit.jupiter.api.Assertions.*;

public class TestLisp
{
	private static final boolean silent = System.getProperties().containsKey("SILENT");

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
	public void formatAtoms()
	{
		for (String form : ATOMS)
		{
			OUT.print("[" + form + "] -> ");
			String format = Formula.of(form).format(" ", "\r\n");
			OUT.println("[" + format + "]");
		}
	}

	@Test
	public void formatLists()
	{
		for (String form : LISTS)
		{
			OUT.print("[" + form + "] -> ");
			String format = Formula.of(form).format(" ", "\r\n");
			OUT.println("[" + format + "]");
		}
	}

	@Test
	public void formatFail()
	{
		for (String form : FAILED_ATOMS)
		{
			OUT.println("[" + form + "] -> ");
			assertThrows(IllegalArgumentException.class, () -> {
				String format = Formula.of(form).format("", "\n");
				OUT.println("[" + format + "]");
			});
		}
		for (String form : ILL_FORMED_FAIL_ATOMS)
		{
			OUT.println("[" + form + "] -> ");
			assertThrows(IllegalArgumentException.class, () -> {
				String format = Formula.of(form).format("", "\n");
				OUT.println("[" + format + "]");
			});
		}
	}

	@Test
	public void cdrOnLists()
	{
		cdr(LISTS, f -> assertTrue(f.listP()));
	}

	@Test
	public void carOnLists()
	{
		car(LISTS, f -> assertTrue(f.listP()));
	}

	@Test
	public void cdrOnAtoms()
	{
		cdr(ATOMS, f -> assertFalse(f.listP()));
	}

	public void cdr(final String[] fs, final Consumer<Formula> test)
	{
		for (String list : fs)
		{
			Formula f = Formula.of(list);
			test.accept(f);

			String car = f.car();
			String cdr = f.cdr();
			Formula cdrF = f.cdrAsFormula();

			OUT.println("formula=\"" + f + "\"");
			OUT.println("\tcar=" + car);
			OUT.println("\tcdr=" + cdr);
			OUT.println("\tcdrF=" + cdrF);

			int i = 1;
			for (Formula itF = f.cdrAsFormula(); itF != null && itF.listP(); itF = itF.cdrAsFormula())
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

	public void car(final String[] fs, final Consumer<IterableFormula> test)
	{
		for (String list : fs)
		{
			IterableFormula f = new IterableFormula(list);
			test.accept(f);

			OUT.println("formula=\"" + f + "\"");

			int i = 1;
			for (; ; f.pop())
			{
				String car = f.car();
				if ("".equals(car))
				{
					break;
				}
				Formula carF = Formula.of(car);

				OUT.println("\tcar" + i + "=" + car);
				OUT.println("\tcarF" + i + "=" + carF);
				i += 1;
			}
		}
	}

	@Test
	public void appendNonEmptyList()
	{
		Formula f = Formula.of("(a b c d)");
		Formula f2 = Formula.of("(e g f)");
		OUT.println("formula=" + f);
		OUT.println("formula2=" + f2);
		OUT.println("append=" + f.append(f2));
	}

	@Test
	public void appendEmptyList()
	{
		Formula f = Formula.of("(a b c d)");
		Formula f2 = Formula.EMPTY_LIST;
		OUT.println("formula=" + f);
		OUT.println("formula2=" + f2);
		OUT.println("append=" + f.append(f2));
	}

	@Test
	public void appendEmptyListEmptyList()
	{
		Formula f = Formula.EMPTY_LIST;
		Formula f2 = Formula.EMPTY_LIST;
		OUT.println("formula=" + f);
		OUT.println("formula2=" + f2);
		OUT.println("append=" + f.append(f2));
	}

	@Test
	public void appendNonList()
	{
		Formula f = Formula.of("(a b c d)");
		Formula f2 = Formula.of("f");
		OUT.println("formula=" + f);
		OUT.println("formula2=" + f2);
		OUT.println("append=" + f.append(f2));
	}

	@Test
	public void consEmptyListAtom()
	{
		Formula f = Formula.EMPTY_LIST;
		Formula f2 = Formula.of("a");
		OUT.println("formula=" + f);
		OUT.println("formula2=" + f2);
		OUT.println("cons=" + f.cons(f2));
	}

	@Test
	public void consEmptyListList()
	{
		Formula f = Formula.EMPTY_LIST;
		Formula f2 = Formula.of("(a b)");
		OUT.println("formula=" + f);
		OUT.println("formula2=" + f2);
		OUT.println("cons=" + f.cons(f2));
	}

	@Test
	public void consEmptyListEmptyList()
	{
		Formula f = Formula.EMPTY_LIST;
		Formula f2 = Formula.EMPTY_LIST;
		OUT.println("formula=" + f);
		OUT.println("formula2=" + f2);
		OUT.println("cons=" + f.cons(f2));
	}

	@Test
	public void consListAtom()
	{
		Formula f = Formula.of("(a b c d)");
		Formula f2 = Formula.of("e");
		OUT.println("formula=" + f);
		OUT.println("formula2=" + f2);
		OUT.println("cons=" + f.cons(f2));
	}

	@Test
	public void consListEmptyList()
	{
		Formula f = Formula.of("(a b c d)");
		Formula f2 = Formula.EMPTY_LIST;
		OUT.println("formula=" + f);
		OUT.println("formula2=" + f2);
		OUT.println("cons=" + f.cons(f2));
	}

	@Test
	public void elementsEmptyList()
	{
		Formula f = Formula.EMPTY_LIST;
		OUT.println("formula=" + f);
		OUT.println("elements=" + f.elements());
	}

	@Test
	public void elementsList()
	{
		Formula f = Formula.of("(a b () c (e f) d)");
		OUT.println("formula=" + f);
		OUT.println("elements=" + f.elements());
	}

	@Test
	public void elementsAtom()
	{
		Formula f = Formula.of("a");
		OUT.println("formula=" + f);
		OUT.println("elements=" + f.elements());
	}

	public static void main(String[] args)
	{
		TestLisp t = new TestLisp();
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
	}
}
