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
				Formula.of("(=> (wife ?A ?B) (husband ?B ?A))"),  //
		};

		for (var f : fs)
		{
			Utils.OUT.println(f);
			List<Formula> rfs = FormulaPreProcessor.preProcess(f, true, SumoProvider.sumo);
			List<Formula> rfs2 = FormulaPreProcessor.preProcess(f, false, SumoProvider.sumo);
			Utils.OUT.println("preprocessed (query)=\n" + rfs.stream().map(Formula::toFlatString).collect(Collectors.joining("\n")));
			Utils.OUT.println("preprocessed=\n" + rfs2.stream().map(Formula::toFlatString).collect(Collectors.joining("\n")));
			Utils.OUT.println();
		}
	}

	//@Disabled
	@Test
	public void testPreProcess2()
	{
		Formula[] fs = { //
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

	public static void main(String[] args)
	{
		new SumoProvider().load();
		TestPreProcess p = new TestPreProcess();
		p.testPreProcess();
		p.testPreProcess2();
	}
}
