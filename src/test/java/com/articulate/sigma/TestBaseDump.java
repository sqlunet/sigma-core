package com.articulate.sigma;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sqlunet.sumo.Dump;
import org.sqlunet.sumo.Processor;
import org.sqlunet.sumo.objects.Formula;
import org.sqlunet.sumo.objects.SUFile;
import org.sqlunet.sumo.objects.Term;

@ExtendWith({BaseSumoProvider.class})
public class TestBaseDump
{
	@Test
	public void testDumpTerms()
	{
		Dump.dumpTerms(BaseSumoProvider.sumo, Utils.OUT);
	}

	@Test
	public void testDumpFormulas()
	{
		Dump.dumpFormulas(BaseSumoProvider.sumo, Utils.OUT);
	}

	@BeforeAll
	public static void init()
	{
		Processor.collectFiles(BaseSumoProvider.sumo);
		Processor.collectTerms(BaseSumoProvider.sumo);
		Processor.collectFormulas(BaseSumoProvider.sumo);

		SUFile.COLLECTOR.open();
		Term.COLLECTOR.open();
		Formula.COLLECTOR.open();
	}

	@AfterAll
	public static void shutdown()
	{
		SUFile.COLLECTOR.close();
		Term.COLLECTOR.close();
		Formula.COLLECTOR.close();
	}

	public static void main(String[] args)
	{
		new BaseSumoProvider().load();
		init();
		TestBaseDump d = new TestBaseDump();
		d.testDumpTerms();
		d.testDumpFormulas();
		d.testDumpFormulas();
	}
}
