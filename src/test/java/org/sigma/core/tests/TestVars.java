/*
 * Copyright (c) 2022.
 * This code is copyright Bernard Bou <1313ou@gmail.com>
 * This software is released under the GNU Public License 3 <http://www.gnu.org/copyleft/gpl.html>.
 */

package org.sigma.core.tests;

import org.sigma.core.Formula;
import org.sigma.core.NotNull;
import org.sigma.core.Tuple;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.sigma.core.Helpers.OUT;
import static org.junit.jupiter.api.Assertions.*;

public class TestVars
{
	private static final Formula f = Formula.of("(=>   (and     (attribute ?H Muslim)     (equal       (WealthFn ?H) ?W)) (modalAttribute   (exists (?Z ?T)     (and       (instance ?Z Zakat)       (instance ?Y Year)       (during ?Y         (WhenFn ?H))       (holdsDuring ?Y         (attribute ?H FullyFormed))       (agent ?Z ?H)       (patient ?Z ?T)       (monetaryValue ?T ?C)       (greaterThan ?C         (MultiplicationFn ?W 0.025)))) Obligation)) ");

	@Test
	public void collectTerms()
	{
		@NotNull Set<String> terms = f.collectTerms();
		OUT.println("Input: " + f);
		OUT.println("Terms: " + terms);

		assertEquals(Set.of("MultiplicationFn", "agent", "instance", "holdsDuring", "Obligation", "during", "Muslim", "0.025", "and", "Zakat", "patient", "attribute", "WhenFn", "greaterThan", "=>", "FullyFormed", "?C", "?H", "monetaryValue", "modalAttribute", "equal", "Year", "?T", "?W", "exists", "?Y", "?Z", "WealthFn"), terms);
	}

	@Test
	public void collectRelationConstants()
	{
		@NotNull Set<String> terms = f.collectRelationConstants();
		OUT.println("Input: " + f);
		OUT.println("Relation constants: " + terms);

		//assertEquals(Set.of("MultiplicationFn", "agent", "instance", "holdsDuring", "Obligation", "during", "Muslim", "0.025", "and", "Zakat", "patient", "attribute", "WhenFn", "greaterThan", "=>", "FullyFormed", "?C", "?H", "monetaryValue", "modalAttribute", "equal", "Year", "?T", "?W", "exists", "?Y", "?Z", "WealthFn"), terms);
	}

	@Test
	public void collectVariables()
	{
		@NotNull Set<String> allVars = f.collectAllVariables();
		@NotNull Set<String> qVars = f.collectQuantifiedVariables();
		Set<String> uqVars = f.collectUnquantifiedVariables();
		@NotNull Tuple.Pair<Set<String>, Set<String>> vars = f.collectVariables();

		OUT.println("Input: " + f);
		OUT.println("Variables: " + allVars);
		OUT.println("All variables: " + allVars);
		OUT.println("Quantified variables: " + qVars);
		OUT.println("Unquantified variables: " + uqVars);

		assertEquals(Set.of("?C", "?T", "?W", "?H", "?Y", "?Z"), allVars);
		assertEquals(Set.of("?T", "?Z"), qVars);
		assertEquals(Set.of("?C", "?W", "?H", "?Y"), uqVars);
		assertEquals(Set.of("?T", "?Z"), vars.first);
		assertEquals(Set.of("?C", "?W", "?H", "?Y"), vars.second);
	}

	@Test
	public void collectVariablesOrdered()
	{
		@NotNull List<String> vars = f.collectAllVariablesOrdered();

		OUT.println("Input: " + f);
		OUT.println("Variables: " + vars);

		assertEquals(List.of("?H", "?H", "?W", "?Z", "?T", "?Z", "?Y", "?Y", "?H", "?Y", "?H", "?Z", "?H", "?Z", "?T", "?T", "?C", "?C", "?W"), vars);
	}

	@Test
	public void collectRowVariables()
	{
		@NotNull Formula f = Formula.of("(@A (@B E) (F @C))");
		@NotNull Set<String> vars = f.collectRowVariables();

		OUT.println("Input: " + f);
		OUT.println("Variables: " + vars);

		assertEquals(Set.of("@A", "@B", "@C"), vars);
	}

	@Test
	public void replaceVars()
	{
		@NotNull String[] forms = {"?REL", "(?REL)", "(?REL b)", "(a ?REL)", "(instance ?REL Transitive)", "(<=> (instance ?REL TransitiveRelation) (forall (?INST1 ?INST2 ?INST3) (=> (and (?REL ?INST1 ?INST2) (?REL ?INST2 ?INST3)) (?REL ?INST1 ?INST3))))"};
		for (@NotNull String form : forms)
		{
			@NotNull Formula f = Formula.of(form);
			@NotNull Formula result = f.replaceVariable("?REL", "part");
			OUT.println("Input: " + form + " formula=" + f);
			OUT.println("Result: " + result);
			OUT.println();
			assertEquals(f.form.replaceAll("\\?REL", "part"), result.form);
		}
	}

	@Test
	public void substituteVars()
	{
		@NotNull String[] forms = {"(<=> (instance ?REL TransitiveRelation) (forall (?INST1 ?INST2 ?INST3) (=> (and (?REL ?INST1 ?INST2) (?REL ?INST2 ?INST3)) (?REL ?INST1 ?INST3))))", //
				"(<=> (instance part TransitiveRelation) (forall (?REL2 ?REL2 ?REL2) (=> (and (part ?REL2 ?REL2) (part ?REL2 ?REL2)) (part ?REL2 ?REL2))))"};
		@NotNull Map<String, String> map = Map.of("?REL", "part", "?REL2", "part", "?INST1", "inst1", "?INST2", "inst2", "?INST3", "inst3");
		for (@NotNull String form : forms)
		{
			@NotNull Formula f = Formula.of(form);
			@NotNull Formula result = f.substituteVariables(map);
			OUT.println("Input: " + form + " formula=" + f);
			OUT.println("Result: " + result);
			OUT.println();
			assertEquals(f.form.replaceAll("\\?REL[0-9]?", "part").replaceAll("\\?INST", "inst"), result.form);
		}
	}

	@Test
	public void substituteVarsIterative()
	{
		@NotNull String[] forms = {"(<=> (instance ?REL TransitiveRelation) (forall (?INST1 ?INST2 ?INST3) (=> (and (?REL ?INST1 ?INST2) (?REL ?INST2 ?INST3)) (?REL ?INST1 ?INST3))))"};
		@NotNull Map<String, String> map = Map.of("?REL", "part", "?INST1", "?REL2", "?INST2", "?REL2", "?INST3", "?REL2", "?REL2", "?REL");
		for (@NotNull String form : forms)
		{
			@NotNull String result = Formula.substituteVariablesIterative(form, map);
			OUT.println("Input: " + form + " formula=" + f);
			OUT.println("Result: " + result);
			OUT.println();
			assertEquals(form.replaceAll("\\?[A-Z0-9]+", "part"), result);
		}
	}

	@Test
	public void quantifierExplicit()
	{
		@NotNull Formula f = Formula.of("(A B C ?X (D ?X) ?Y (E ?Y))");
		@NotNull String eResult = f.makeQuantifiersExplicit(true);
		@NotNull String uResult = f.makeQuantifiersExplicit(false);
		OUT.println("Input: " + f);
		OUT.println("eResult: " + eResult);
		OUT.println("uResult: " + uResult);
		OUT.println();
		assertEquals("(exists (?X ?Y) " + f.form + ")", eResult);
		assertEquals("(forall (?X ?Y) " + f.form + ")", uResult);
	}
}
