package com.articulate.sigma;

import com.articulate.sigma.noncore.FormulaPreProcessor;
import com.articulate.sigma.noncore.Instantiate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.text.Normalizer;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith({SumoProvider.class})
public class TestPreProcesss
{
	//@Disabled
	@Test
	public void testPreProcess()
	{
		Formula f = Formula.of("(r a b)");
		List<Formula> fs = FormulaPreProcessor.preProcess(f,false, SumoProvider.sumo);
		Utils.OUT.print(fs);
	}

	@Test
	public void testGatherPredVars()
	{
		Formula f0 = Formula.of("(=> (and (instance ?GUN Gun) (instance ?U UnitOfLength) (effectiveRange ?GUN (MeasureFn ?LM ?U)) (distance ?GUN ?O (MeasureFn ?LM1 ?U)) (instance ?O Organism) (not (exists (?O2) (between ?O ?O2 ?GUN))) (lessThanOrEqualTo ?LM1 ?LM))" + "(capability	(KappaFn ?KILLING (and (instance ?KILLING Killing) (patient ?KILLING ?O))) instrument ?GUN))");
		Formula f = Formula.of("(KappaFn ?KILLING (and " +
				"(instance ?KILLING Killing) " +
				"(patient ?KILLING ?O)))");
		Map<String, List<String>> m =  Instantiate.gatherPredVars(f, SumoProvider.sumo);
		Utils.OUT.print(m);
	}

	public static void main(String[] args)
	{
		new SumoProvider().load();
		TestPreProcesss p = new TestPreProcesss();
		p.testPreProcess();
	}
}
