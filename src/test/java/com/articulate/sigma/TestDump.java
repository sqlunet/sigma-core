package com.articulate.sigma;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sqlunet.sumo.Processor;
import org.sqlunet.sumo.objects.Formula;
import org.sqlunet.sumo.objects.SUFile;
import org.sqlunet.sumo.objects.Term;

import java.util.stream.Stream;

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
	public void testDumpUnaryRelations()
	{
		Dump.dumpUnaryFunctions(SumoProvider.sumo, Utils.OUT);
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
	public void testDumpClassesOfPropertyAndAttribute()
	{
		dumpAllClassesOf("property");
		dumpAllClassesOf("attribute");
	}

	@Test
	public void testDumpInstancesOfAttribute()
	{
		dumpSubClassesOf("Attribute");
		Utils.OUT.println("-".repeat(80));
		dumpSuperClassesOf("Attribute");
		Utils.OUT.println("-".repeat(80));
		dumpAllInstancesOf("Attribute");
	}

	@Test
	public void testDumpTermClassTree()
	{
		Dump.dumpClassTrees(SumoProvider.sumo, Utils.OUT);
	}

	// Relation

	@Test
	public void testDumpSuperClassesOfRelation()
	{
		dumpSuperClassesOf("Relation");
	}

	@Test
	public void testDumpAllSuperClassesOfRelation()
	{
		dumpAllSuperClassesOf("Relation");
	}

	@Test
	public void testDumpSubClassesOfRelation()
	{
		dumpSubClassesOf("Relation");
	}

	@Test
	public void testDumpAllSubClassesOfRelation()
	{
		dumpAllSubClassesOf("Relation");
	}

	@Test
	public void testDumpClassesOfRelation()
	{
		dumpClassesOf("Relation");
	}

	@Test
	public void testDumpAllClassesOfRelation()
	{
		dumpAllClassesOf("Relation");
	}

	@Test
	public void testDumpInstancesOfRelation()
	{
		dumpInstancesOf("Relation");
	}

	@Test
	public void testDumpAllInstancesOfRelation()
	{
		dumpAllInstancesOf("Relation");
	}

	// Predicate

	@Test
	public void testDumpSuperClassesOfPredicate()
	{
		dumpSuperClassesOf("Predicate");
	}

	@Test
	public void testDumpAllSuperClassesOfPredicate()
	{
		dumpAllSuperClassesOf("Predicate");
	}

	@Test
	public void testDumpSubClassesOfPredicate()
	{
		dumpSubClassesOf("Predicate");
	}

	@Test
	public void testDumpAllSubClassesOfPredicate()
	{
		dumpAllSubClassesOf("Predicate");
	}

	@Test
	public void testDumpClassesOfPredicate()
	{
		dumpClassesOf("Predicate");
	}

	@Test
	public void testDumpAllClassesOfPredicate()
	{
		dumpAllClassesOf("Predicate");
	}

	@Test
	public void testDumpInstancesOfPredicate()
	{
		dumpInstancesOf("Predicate");
	}

	@Test
	public void testDumpAllInstancesOfPredicate()
	{
		dumpAllInstancesOf("Predicate");
	}


	// samples

	@Test
	public void testDumpSuperClassesOfSamples()
	{
		Stream.of("Insect", "BinaryFunction", "TernaryRelation").forEach(TestDump::dumpSuperClassesOf);
	}

	@Test
	public void testDumpSubClassesOfSamples()
	{
		Stream.of("Insect", "BinaryFunction", "TernaryRelation").forEach(TestDump::dumpSubClassesOf);
	}

	// helpers

	private static void dumpClassesOf(@NotNull final String inst)
	{
		Utils.OUT.println(inst);
		Dump.dumpClassesOf(SumoProvider.sumo, inst, Utils.OUT);
		Utils.OUT.println();
	}

	private static void dumpAllClassesOf(@NotNull final String inst)
	{
		Utils.OUT.println(inst);
		Dump.dumpAllClassesOf(SumoProvider.sumo, inst, Utils.OUT);
		Utils.OUT.println();
	}

	private static void dumpInstancesOf(@NotNull final String className)
	{
		Utils.OUT.println(className);
		Dump.dumpInstancesOf(SumoProvider.sumo, className, Utils.OUT);
		Utils.OUT.println();
	}

	private static void dumpAllInstancesOf(@NotNull final String className)
	{
		Utils.OUT.println(className);
		Dump.dumpAllInstancesOf(SumoProvider.sumo, className, Utils.OUT);
		Utils.OUT.println();
	}

	private static void dumpSuperClassesOf(@NotNull final String className)
	{
		Utils.OUT.println(className);
		Dump.dumpSuperClassesOf(SumoProvider.sumo, className, Utils.OUT);
		Utils.OUT.println();
	}

	private static void dumpAllSuperClassesOf(@NotNull final String className)
	{
		Utils.OUT.println(className);
		Dump.dumpSuperClassesOfWithPredicateSubsumption(SumoProvider.sumo, className, Utils.OUT);
		Utils.OUT.println();
	}

	private static void dumpSubClassesOf(@NotNull final String className)
	{
		Utils.OUT.println(className);
		Dump.dumpSubClassesOf(SumoProvider.sumo, className, Utils.OUT);
		Utils.OUT.println();
	}

	private static void dumpAllSubClassesOf(@NotNull final String className)
	{
		Utils.OUT.println(className);
		Dump.dumpSubClassesOfWithPredicateSubsumption(SumoProvider.sumo, className, Utils.OUT);
		Utils.OUT.println();
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
		d.testDumpClasses();
		d.testDumpRelations();
	}
}
