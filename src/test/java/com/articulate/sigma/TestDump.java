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

@ExtendWith({SumoProvider.class})
public class TestDump
{
	@Test
	public void testDumpTerms()
	{
		Dump.dumpTerms(SumoProvider.sumo, Utils.OUT);
	}

	@Test
	public void testDumpFormulas()
	{
		Dump.dumpFormulas(SumoProvider.sumo, Utils.OUT);
	}

	@Test
	public void testDumpPredicates()
	{
		Dump.dumpPredicates(SumoProvider.sumo, Utils.OUT);
	}

	@Test
	public void testDumpClasses()
	{
		Dump.dumpClasses(SumoProvider.sumo, Utils.OUT);
	}

	@Test
	public void testDumpFunctions()
	{
		Dump.dumpFunctions(SumoProvider.sumo, Utils.OUT);
	}

	@Test
	public void testDumpRelations()
	{
		Dump.dumpRelations(SumoProvider.sumo, Utils.OUT);
	}

	@Test
	public void testDumpBinaryRelations()
	{
		Dump.dumpBinaryRelations(SumoProvider.sumo, Utils.OUT);
	}

	@Test
	public void testDumpTernaryRelations()
	{
		Dump.dumpTernaryRelations(SumoProvider.sumo, Utils.OUT);
	}

	@Test
	public void testDumpSuperClassesOfRelation()
	{
		Dump.dumpSuperClassesOf(SumoProvider.sumo, "Relation", Utils.OUT);
	}

	@Test
	public void testFullDumpSubClassesOfRelation()
	{
		dumpFullSubClassesOf("Relation");
	}

	@Test
	public void testFullDumpSubClassesOfPredicate()
	{
		dumpFullSubClassesOf("Predicate");
	}

	public void dumpFullSubClassesOf(@NotNull final String className)
	{
		Dump.dumpSubClassesOf(SumoProvider.sumo, className, Utils.OUT);
		Dump.dumpSubClassesOfWithPredicateSubsumption(SumoProvider.sumo, className, Utils.OUT);
	}

	@Test
	public void testDumpSubClassesOfRelation()
	{
		Dump.dumpSubClassesOf(SumoProvider.sumo, "Relation", Utils.OUT);
	}

	@Test
	public void testDumpTermClassTree()
	{
		Dump.dumpTermClassTree(SumoProvider.sumo, Utils.OUT);
	}

	@Test
	public void testDumpSuperClassesOfInsect()
	{
		Dump.dumpSuperClassesOf(SumoProvider.sumo, "Insect", Utils.OUT);
		Dump.dumpSuperClassesOfWithPredicateSubsumption(SumoProvider.sumo, "Insect", Utils.OUT);
	}

	@Test
	public void testDumpSubClassesOfInsect()
	{
		Dump.dumpSubClassesOf(SumoProvider.sumo, "Insect", Utils.OUT);
		Dump.dumpSubClassesOfWithPredicateSubsumption(SumoProvider.sumo, "Insect", Utils.OUT);
	}

	@BeforeAll
	public static void init()
	{
		Processor.collectFiles(SumoProvider.sumo);
		Processor.collectTerms(SumoProvider.sumo);
		Processor.collectFormulas(SumoProvider.sumo);

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
		new SumoProvider().load();
		init();
		TestDump d = new TestDump();
		d.testDumpTerms();
		d.testDumpFormulas();
		d.testDumpFormulas();
	}
}
