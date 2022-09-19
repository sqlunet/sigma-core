package com.articulate.sigma;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;

import static com.articulate.sigma.Utils.OUT;
import static org.junit.jupiter.api.Assertions.*;

public class TestLisp
{
	private static final boolean silent = System.getProperties().containsKey("SILENT");

	private static final String[] ATOMS = { //
			"a", "a b", "\"a b\"", "\"a b\"", "\"'a b'\"", "\"a ' b\"", "\"''\"", "'a b'", "''",}; //

	private static final String[] ILL_FORMED_FAIL_ATOMS = { //
			"a \" b", "a ' b",}; //

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

	private static final Collection<String> ALL = new ArrayList<>();

	static
	{
		Collections.addAll(ALL, ATOMS);
		Collections.addAll(ALL, LISTS);
	}

	@Test
	public void formatAtomsTest()
	{
		for (String form : ATOMS)
		{
			OUT.print("[" + form + "] -> ");
			String format = new Formula(form).format("", " ", "\r\n");
			OUT.println("[" + format + "]");
		}
	}

	@Test
	public void formatListsTest()
	{
		for (String form : LISTS)
		{
			OUT.print("[" + form + "] -> ");
			String format = new Formula(form).format("", " ", "\r\n");
			OUT.println("[" + format + "]");
		}
	}

	@Test
	public void formatFailTest()
	{
		for (String form : FAILED_ATOMS)
		{
			OUT.println("[" + form + "] -> ");
			assertThrows(IllegalArgumentException.class, () -> {
				String format = new Formula(form).format("", "", "\n");
				OUT.println("[" + format + "]");
			});
		}
	}

	@Test
	public void cdrTestOnLists()
	{
		cdrTest(LISTS, f -> assertTrue(f.listP()));
	}

	@Test
	public void carTestOnLists()
	{
		carTest(LISTS, f -> assertTrue(f.listP()));
	}

	@Test
	public void cdrTestOnAtoms()
	{
		cdrTest(ATOMS, f -> assertFalse(f.listP()));
	}

	public void cdrTest(final String[] fs, final Consumer<Formula> test)
	{
		for (String list : fs)
		{
			Formula f = new Formula(list);
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

	public void carTest(final String[] fs, final Consumer<Formula> test)
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
				Formula carF = new Formula(car);

				OUT.println("\tcar" + i + "=" + car);
				OUT.println("\tcarF" + i + "=" + carF);
				i += 1;
			}
		}
	}

	public static void main(String[] args)
	{
		new TestLisp().cdrTestOnLists();
	}
}
