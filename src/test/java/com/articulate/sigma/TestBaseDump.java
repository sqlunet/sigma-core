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

@ExtendWith({BaseKBLoader.class})
public class TestBaseDump
{
	@Test
	public void testDumpTerms()
	{
		Dump.dumpTerms(BaseKBLoader.kb, Utils.OUT);
	}

	@Test
	public void testDumpFormulas()
	{
		Dump.dumpFormulas(BaseKBLoader.kb, Utils.OUT);
	}

	@BeforeAll
	public static void init()
	{
		Processor.collectFiles(BaseKBLoader.kb);
		Processor.collectTerms(BaseKBLoader.kb);
		Processor.collectFormulas(BaseKBLoader.kb);

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
		new BaseKBLoader().load();
		init();
		TestBaseDump d = new TestBaseDump();
		d.testDumpTerms();
		d.testDumpFormulas();
		d.testDumpFormulas();
	}
}
