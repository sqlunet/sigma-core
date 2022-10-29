package com.articulate.sigma;

import com.articulate.sigma.noncore.FormulaPreProcessor;
import com.articulate.sigma.noncore.Instantiate;
import com.articulate.sigma.noncore.RejectException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ExtendWith({SumoProvider.class})
public class TestInstantiate
{
	@Test
	public void testInstantiate() throws RejectException
	{
		Formula[] fs = { //
				Formula.of("(=> (and (instance ?REL SymmetricRelation) (?REL ?INST1 ?INST2)) (?REL ?INST2 ?INST1))"), //
				Formula.of("(?REL a b)"), //
		};
		for (var f : fs)
		{
			Utils.OUT.println("formula=" + f);
			@NotNull final List<Formula> assertions = new ArrayList<>();
			Instantiate.instantiateFormula(f, SumoProvider.sumo.uniqueId, assertions);
			Utils.OUT.println(assertions);
			Utils.OUT.println();
		}
	}

	public static void main(String[] args) throws RejectException
	{
		new SumoProvider().load();
		TestInstantiate i = new TestInstantiate();
		i.testInstantiate();
	}
}
