package com.articulate.sigma;

import com.articulate.sigma.noncore.FormulaPreProcessor;
import com.articulate.sigma.noncore.Instantiate;
import com.articulate.sigma.noncore.RejectException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith({SumoProvider.class})
public class TestPreProcess
{
	//@Disabled
	@Test
	public void testPreProcess()
	{
		Formula[] fs = { //
				Formula.of("(r a b)"), //
				Formula.of("(=> (and (instance ?REL SymmetricRelation) (?REL ?INST1 ?INST2)) (?REL ?INST2 ?INST1))"), //
				Formula.of("(=> (and (subrelation ?REL1 ?REL2) (instance ?REL1 Predicate) (instance ?REL2 Predicate) (?REL1 @ROW)) (?REL2 @ROW))"), //
		};

		for (var f : fs)
		{
			Utils.OUT.println(f);
			List<Formula> rfs = FormulaPreProcessor.preProcess(f, false, SumoProvider.sumo);
			Utils.OUT.println("preprocessed=\n" + rfs.stream().map(Formula::toFlatString).collect(Collectors.joining("\n")));
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
		};
		for (var f : fs)
		{
			Utils.OUT.println("formula=" + f);
			Map<String, List<String>> m = Instantiate.gatherPredVars(f, SumoProvider.sumo);
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
			List<Formula> rfs = FormulaPreProcessor.replacePredVarsAndRowVars(f, SumoProvider.sumo, FormulaPreProcessor.ADD_HOLDS_PREFIX);
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
			List<Formula> rfs = Instantiate.instantiatePredVars(f, SumoProvider.sumo);
			Utils.OUT.println("instantiated=\n" + rfs.stream().map(Formula::toFlatString).collect(Collectors.joining("\n")));
			Utils.OUT.println();
		}
	}

	public static void main(String[] args)
	{
		new SumoProvider().load();
		TestPreProcess p = new TestPreProcess();
		p.testPreProcess();
	}
}
