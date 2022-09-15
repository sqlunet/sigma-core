package com.articulate.sigma;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.sqlunet.sumo.Dump;
import org.sqlunet.sumo.Kb;
import org.sqlunet.sumo.Processor;
import org.sqlunet.sumo.objects.SUFile;
import org.sqlunet.sumo.objects.Term;

public class TestDump
{
	private static Kb kb;

	@Test
	public void testDumpTerms()
	{
		Dump.dumpTerms(kb);
	}

	@Test
	public void testDumpFormulas()
	{
		Dump.dumpFormulas(kb);
	}

	@Test
	public void testDumpPredicates()
	{
		Dump.dumpPredicates(kb);
	}

	@BeforeAll
	public static void init()
	{
		Utils.turnOffLogging();
		kb = Utils.loadKb(Utils.SAMPLE_FILES);

		Processor.collectFiles(kb);
		Processor.collectTerms(kb);
		Processor.collectFormulas(kb);
		SUFile.COLLECTOR.open();
		Term.COLLECTOR.open();
		org.sqlunet.sumo.objects.Formula.COLLECTOR.open();
	}

	public static void main(String[] args)
	{
		init();
		TestDump d = new TestDump();
		d.testDumpTerms();
		d.testDumpFormulas();
		d.testDumpFormulas();
	}
}
