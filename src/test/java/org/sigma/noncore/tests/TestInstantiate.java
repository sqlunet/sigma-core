/*
 * Copyright (c) 2022.
 * This code is copyright Bernard Bou <1313ou@gmail.com>
 * This software is released under the GNU Public License 3 <http://www.gnu.org/copyleft/gpl.html>.
 */

package org.sigma.noncore.tests;

import org.sigma.core.Formula;
import org.sigma.core.SumoProvider;
import org.sigma.core.Utils;
import org.sigma.noncore.FormulaPreProcessor;
import org.sigma.noncore.Instantiate;
import org.sigma.noncore.RejectException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith({SumoProvider.class})
public class TestInstantiate
{
	@Test
	public void testInstantiate() throws RejectException
	{
		Formula[] fs = { //
				Formula.of("(REL ?A ?B)"), //
				Formula.of("(?REL a b)"), //
				Formula.of("(?REL ?A ?B)"), //
				Formula.of("(=> (and (instance ?REL SymmetricRelation) (?REL ?INST1 ?INST2)) (?REL ?INST2 ?INST1))"), //
		};
		for (var f : fs)
		{
			Utils.OUT.println("formula=" + f);
			var f2 = Instantiate.instantiateFormula(f, SumoProvider.SUMO.uniqueId);
			Utils.OUT.println(f2);
			Utils.OUT.println();
		}
	}

	@Test
	public void testGatherPredVars()
	{
		Formula[] fs = { //
				Formula.of("(=> (and (instance ?REL SymmetricRelation) (?REL ?INST1 ?INST2)) (?REL ?INST2 ?INST1))"), //
				Formula.of("(=> (and (subrelation ?REL1 ?REL2) (instance ?REL1 Predicate) (instance ?REL2 Predicate) (?REL1 @ROW)) (?REL2 @ROW))"), //
				Formula.of("(?REL a b)"), //
				Formula.of("(foo (?REL a b) bar)"), //
				Formula.of("(foo (ref ?A ?B) bar)"), //
		};
		for (var f : fs)
		{
			Utils.OUT.println("formula=" + f);
			Map<String, List<String>> m = Instantiate.gatherPredVars(f, SumoProvider.SUMO);
			Utils.OUT.println("gathered=" + m);
			Utils.OUT.println();
		}
	}

	@Test
	public void testReplacePredVars()
	{
		Formula[] fs = { //
				Formula.of("(=> (and (instance ?REL SymmetricRelation) (?REL ?INST1 ?INST2)) (?REL ?INST2 ?INST1))"), //
				Formula.of("(=> (and (subrelation ?REL1 ?REL2) (instance ?REL1 Predicate) (instance ?REL2 Predicate) (?REL1 @ROW)) (?REL2 @ROW))"), //
				Formula.of("(?REL a b)"), //
		};
		for (var f : fs)
		{
			Utils.OUT.println("formula=" + f);
			List<Formula> rfs = FormulaPreProcessor.replacePredVarsAndRowVars(f, SumoProvider.SUMO, FormulaPreProcessor.ADD_HOLDS_PREFIX);
			Utils.OUT.println("replaced=\n" + rfs.stream().map(Formula::toFlatString).collect(Collectors.joining("\n")));
			Utils.OUT.println();
		}
	}

	@Test
	public void testInstantiatePredVars() throws RejectException
	{
		Formula[] fs = { //
				Formula.of("(=> (and (instance ?REL SymmetricRelation) (?REL ?INST1 ?INST2)) (?REL ?INST2 ?INST1))"), //
				Formula.of("(=> (and (subrelation ?REL1 ?REL2) (instance ?REL1 Predicate) (instance ?REL2 Predicate) (?REL1 @ROW)) (?REL2 @ROW))"), //
				Formula.of("(?REL a b)"), //
		};
		for (var f : fs)
		{
			Utils.OUT.println("formula=" + f);
			List<Formula> rfs = Instantiate.instantiatePredVars(f, SumoProvider.SUMO);
			Utils.OUT.println("instantiated=\n" + rfs.stream().map(Formula::toFlatString).collect(Collectors.joining("\n")));
			Utils.OUT.println();
		}
	}

	public static void main(String[] args) throws RejectException
	{
		new SumoProvider().load();
		TestInstantiate i = new TestInstantiate();
		i.testInstantiate();
		i.testGatherPredVars();
		i.testReplacePredVars();
		i.testInstantiatePredVars();
	}
}