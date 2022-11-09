/*
 * Copyright (c) 2022.
 * This code is copyright Bernard Bou <1313ou@gmail.com>
 * This software is released under the GNU Public License 3 <http://www.gnu.org/copyleft/gpl.html>.
 */

package org.sigma.noncore.tests;

import org.sigma.core.Formula;

import org.junit.jupiter.api.Test;
import org.sigma.core.NotNull;
import org.sigma.core.Nullable;

import java.util.List;

import static org.sigma.core.Helpers.OUT;
import static org.junit.jupiter.api.Assertions.*;

public class TestOther
{

	@Test
	public void isSimpleClause()
	{
		@NotNull Formula f = Formula.of("(not (instance ?X Human))");
		boolean isSimpleClause = f.isSimpleClause();
		OUT.println("Simple clause? : " + f + "\n" + isSimpleClause + "\n");
		assertFalse(isSimpleClause);

		f = Formula.of("(instance ?X Human)");
		isSimpleClause = f.isSimpleClause();
		OUT.println("Simple clause? : " + f + "\n" + isSimpleClause + "\n");
		assertTrue(isSimpleClause);

		f = Formula.of("(=> (attribute ?Agent Investor) (exists (?Investing) (agent ?Investing ?Agent)))");
		isSimpleClause = f.isSimpleClause();
		OUT.println("Simple clause? : " + f + "\n" + isSimpleClause + "\n");
		assertFalse(isSimpleClause);

		f = Formula.of("(member (SkFn 1 ?X3) ?X3)");
		isSimpleClause = f.isSimpleClause();
		OUT.println("Simple clause? : " + f + "\n" + isSimpleClause + "\n");
		assertTrue(isSimpleClause);

		f = Formula.of("(member ?VAR1 Org1-1)");
		isSimpleClause = f.isSimpleClause();
		OUT.println("Simple clause? : " + f + "\n" + isSimpleClause + "\n");
		assertTrue(isSimpleClause);

		f = Formula.of("(capability (KappaFn ?HEAR (and (instance ?HEAR Hearing) (agent ?HEAR ?HUMAN) (destination ?HEAR ?HUMAN) (origin ?HEAR ?OBJ))) agent ?HUMAN)");
		isSimpleClause = f.isSimpleClause();
		OUT.println("Simple clause? : " + f + "\n" + isSimpleClause + "\n");
		assertFalse(isSimpleClause);
	}

	@Test
	public void validArgs()
	{
		@NotNull Formula f = Formula.of("(=> (instance ?C Crankshaft) (instance ?C AutomobileTransmission))");
		@Nullable String error = f.hasValidArgs();
		OUT.println("Input: " + f);
		OUT.println("Valid: " + (error == null));
		assertNull(error);

		f = Formula.of("(=> (instance ?C Crankshaft) (or (instance ?C AutomobileTransmission)))");
		error = f.hasValidArgs();
		OUT.println("Input: " + f);
		OUT.println("Valid: " + error);
		assertNotNull(error);
	}

	@Test
	public void validArgsBig()
	{
		@NotNull Formula f = Formula.of("(=> (instance ?AT AutomobileTransmission) (hasPurpose ?AT (exists (?C ?D ?A ?R1 ?N1 ?R2 ?R3 ?R4 ?N2 ?N3) (and (instance ?C Crankshaft) (instance ?D Driveshaft) (instance ?A Automobile) (part ?D ?A) (part ?AT ?A) (part ?C ?A) (connectedEngineeringComponents ?C ?AT) (connectedEngineeringComponents ?D ?AT) (instance ?R1 Rotating) (instance ?R2 Rotating) (instance ?R3 Rotating) (instance ?R4 Rotating) (patient ?R1 ?C) (patient ?R2 ?C) (patient ?R3 ?D) (patient ?R4 ?D) (causes ?R1 ?R3) (causes ?R2 ?R4) (not (equal ?R1 ?R2)) (holdsDuring ?R1 (measure ?C (RotationFn ?N1 MinuteDuration))) (holdsDuring ?R2 (measure ?C (RotationFn ?N1 MinuteDuration))) (holdsDuring ?R3 (measure ?D (RotationFn ?N2 MinuteDuration))) (holdsDuring ?R4 (measure ?D (RotationFn ?N3 MinuteDuration))) (not (equal ?N2 ?N3))))))");
		@Nullable String error = f.hasValidArgs();
		OUT.println("Input: " + f);
		OUT.println("Valid: " + (error == null));
		assertNull(error);
	}

	@Test
	public void argList()
	{
		@NotNull Formula f = Formula.of("(termFormat EnglishLanguage experimentalControlProcess \"experimental control (process)\")");
		@Nullable List<String> args = f.simpleArgumentsToList(0);
		OUT.println("Input: " + f);
		OUT.println(args);
		OUT.println();
		assertEquals(List.of("termFormat", "EnglishLanguage", "experimentalControlProcess", "\"experimental control (process)\""), args);

		f = Formula.of("(termFormat EnglishLanguage experimentalControlProcess \"experimental control process\")");
		args = f.simpleArgumentsToList(0);
		OUT.println("Input: " + f);
		OUT.println(args);
		assertEquals(List.of("termFormat", "EnglishLanguage", "experimentalControlProcess", "\"experimental control process\""), args);
	}

	@Test
	public void argListComplex()
	{
		@NotNull Formula f = Formula.of("(during ?Y (WhenFn ?H))");
		@Nullable List<String> args = f.simpleArgumentsToList(1);
		OUT.println("Input: " + f);
		OUT.println(args);
		OUT.println();
		assertEquals(null, args);
	}

	@Test
	public void logicallyEquivalent()
	{
		@NotNull Formula f = Formula.of("(and A B C)");
		@NotNull Formula f2 = Formula.of("(and C B A)");
		boolean equiv = f.logicallyEquals(f2);
		OUT.println("Input: " + f);
		OUT.println("Input2: " + f2);
		OUT.println(equiv);
		OUT.println();
		assertTrue(equiv);
	}

	@Test

	public void logicallyEquivalent2()
	{
		@NotNull Formula f = Formula.of("(and A B (OR C D))");
		@NotNull Formula f2 = Formula.of("(and (OR C D) B A)");
		boolean equiv = f.logicallyEquals(f2);
		OUT.println("Input: " + f);
		OUT.println("Input2: " + f2);
		OUT.println(equiv);
		OUT.println();
		assertTrue(equiv);
	}
}
