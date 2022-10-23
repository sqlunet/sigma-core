package com.articulate.sigma;

import org.junit.jupiter.api.Test;

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
			String format = Formula.of(form).format(" ", "\n");
			OUT.println("[" + format + "]");
		}
	}

	@Test
	public void formatLists()
	{
		for (String form : LISTS)
		{
			OUT.println("[" + form + "] -> ");
			String format = Formula.of(form).format(" ", "\n");
			OUT.println("[" + format + "]");
			OUT.println();
		}
	}

	@Test
	public void flatFormatLists()
	{
		for (String form : LISTS)
		{
			OUT.println("[" + form + "] -> ");
			String format = Formula.of(form).format("", " ");
			OUT.println("[" + format + "]");
			OUT.println();
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
	public void formatVariable()
	{
		assertEquals("(?X)", Formula.of("(?X)").form);
		assertEquals("?X", Formula.of("?X").form);
		assertEquals("X", Formula.of("X").form);
		assertEquals("(r ?X ?Y)", Formula.of("(r ?X ?Y)").form);
		assertEquals("(a (?X ?Y) b)", Formula.of("(a (?X ?Y) b)").form);
		assertEquals("(a (forall (?X ?Y) (c ?X ?Y)) b)", Formula.of("(a (forall (?X ?Y) (c ?X ?Y)) b)").form);
	}
}
