package com.articulate.sigma;

import com.articulate.sigma.noncore.FormulaPreProcessor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith({SumoProvider.class})
public class TestPreProcesss
{
	//@Disabled
	@Test
	public void testPreProcess()
	{
		Formula f = Formula.of("(r a b)");
		FormulaPreProcessor.preProcess(f,false, SumoProvider.sumo);
	}

	public static void main(String[] args)
	{
		new SumoProvider().load();
		TestPreProcesss p = new TestPreProcesss();
		p.testPreProcess();
	}
}
