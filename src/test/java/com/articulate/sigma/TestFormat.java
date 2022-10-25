package com.articulate.sigma;

import org.junit.jupiter.api.Test;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.stream.Stream;

import static com.articulate.sigma.Utils.OUT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TestFormat
{
	private static final boolean silent = System.getProperties().containsKey("SILENT");

	private static final String[] ATOMS = { //
			"a", "a b", "\"a b\"", "\"a b\"", "\"'a b'\"", "\"a ' b\"", "\"''\"", "'a b'", "''",}; //

	private static final String[] ILL_FORMED_FAIL_ATOMS = { //
			"a \" b",}; //

	private static final String[] SHOULD_FAIL_ATOMS = { //
			"a ' b",}; //

	private static final String[] FAILED_ATOMS = { //
			"",}; //

	private static final String[] LISTS = { //
			"()", "(a)", "(a b)", "(a b c)", "(a b c \"d e\")", "(a b c 'd e')", "(=> (a ?W) (b ?W))", //
	};

	private static final String[] FLISTS = { //
			"(=> (a ?W) (b ?W))", //
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
			check(form);
		}
	}

	@Test
	public void prettyFormatLists()
	{
		for (String form : FLISTS)
		{
			Formula f = Formula.of(form);
			OUT.println(f.form + " -> ");
			String format = f.toString();
			OUT.println("[\n" + format + "\n]");
			OUT.println();
			Formula f2 = Formula.of(format);
			assertEquals(f, f2);
		}
	}

	@Test
	public void flatFormatLists()
	{
		for (String form : LISTS)
		{
			check(form);
		}
	}

	@Test
	public void prologFormatLists()
	{
		for (String form : new String[]{"(foo a b)", "(foo c)"})
		{
			OUT.print(form + " -> ");
			Formula f = Formula.of(form);
			OUT.println(f.toProlog());
		}
	}

	@Test
	public void formatNestedLists()
	{
		String[] forms = new String[]{"(())", "((a b))", "(() a b c d)", "(a (b c) d)"};
		for (String form : forms)
		{
			check(form);
		}
	}

	@Test
	public void readFail()
	{
		for (String form : Stream.concat(Arrays.stream(FAILED_ATOMS), Arrays.stream(ILL_FORMED_FAIL_ATOMS)).toArray(String[]::new))
		{
			OUT.println("[" + form + "] -> ");
			assertThrows(IllegalArgumentException.class, () -> {
				Formula f = Formula.of(form);
				OUT.println("Not expected [" + f + "]");
			}, "IllegalArgumentException not thrown");
		}
	}

	@Test
	public void formatVariable()
	{
		String[] forms = new String[]{"(?X)", "?X", "X", "(r ?X ?Y)", "(a (?X ?Y) b)", "(a (forall (?X ?Y) (c ?X ?Y)) b)",};

		for (String form : forms)
		{
			check(form);
		}
	}

	private void check(String form)
	{
		// String formatted = Formula.toFlatString(form);
		//String formatted = Formula.format(form, "#", "!");
		String formatted = Formula.format(form, "", "");
		OUT.println(form + " -> " + formatted);
		// assertEquals(form, formatted);
	}
}