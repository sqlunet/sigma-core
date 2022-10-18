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

@ExtendWith({KBLoader.class})
public class TestDump
{
	@Test
	public void testDumpTerms()
	{
		Dump.dumpTerms(KBLoader.kb, Utils.OUT);
	}

	@Test
	public void testDumpFormulas()
	{
		Dump.dumpFormulas(KBLoader.kb, Utils.OUT);
	}

	@Test
	public void testDumpPredicates()
	{
		Dump.dumpPredicates(KBLoader.kb, Utils.OUT);
	}

	@Test
	public void testDumpSuperClassesOf()
	{
		Dump.dumpSuperClassesOf(KBLoader.kb, "Insect", Utils.OUT);
	}

	@Test
	public void testDumpSubClassesOf()
	{
		Dump.dumpSubClassesOf(KBLoader.kb, "Insect", Utils.OUT);
	}

	@Test
	public void testDumpClasses()
	{
		Dump.dumpClasses(KBLoader.kb, Utils.OUT);
	}

	@Test
	public void testDumpFunctions()
	{
		Dump.dumpFunctions(KBLoader.kb, Utils.OUT);
	}

	@Test
	public void testDumpTermTree()
	{
		Dump.dumpTermTree(KBLoader.kb, Utils.OUT);
	}

	@BeforeAll
	public static void init()
	{
		Processor.collectFiles(KBLoader.kb);
		Processor.collectTerms(KBLoader.kb);
		Processor.collectFormulas(KBLoader.kb);

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
		new KBLoader().load();
		init();
		TestDump d = new TestDump();
		d.testDumpTerms();
		d.testDumpFormulas();
		d.testDumpFormulas();
	}
}
