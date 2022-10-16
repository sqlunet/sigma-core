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

import java.util.Collection;

@ExtendWith({KBLoader.class})
public class TestAsk
{
	@Test
	public void testDumpPredicates()
	{
		Dump.dumpPredicates(KBLoader.kb, Utils.OUT);
	}

	@Test
	public void testDumpSuperClassesOf()
	{
		Dump.dumpSuperClassesOf("Insect", KBLoader.kb, Utils.OUT);
	}

	@Test
	public void testDumpSubClassesOf()
	{
		Dump.dumpSubClassesOf("Insect", KBLoader.kb, Utils.OUT);
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

	@Test
	public void testAsk()
	{
		//Collection<Formula> result = KBLoader.kb.askWithPredicateSubsumption();
		Collection<com.articulate.sigma.Formula> result = KBLoader.kb.ask("arg", 0, "engineeringSubcomponent");
		for (com.articulate.sigma.Formula f : result)
		{
			Utils.OUT.println(f);
		}
	}

	@Test
	public void testAskAllSubrelations()
	{
		//Collection<Formula> result = KBLoader.kb.askWithPredicateSubsumption();
		Collection<com.articulate.sigma.Formula> result = KBLoader.kb.ask("arg", 0, "subrelation");
		for (com.articulate.sigma.Formula f : result)
		{
			Utils.OUT.println(f);
		}
	}

	@Test
	public void testAskAllSubrelationsOf()
	{
		//Collection<Formula> result = KBLoader.kb.askWithPredicateSubsumption();
		String[] ts = new String[]{"brother", "sister", "sibling", "parent", "familyRelation", "relative", "part"};
		for (String t : ts)
		{
			Utils.OUT.println(t);
			Collection<com.articulate.sigma.Formula> result = KBLoader.kb.askWithRestriction(0, "subrelation", 2, t);
			for (com.articulate.sigma.Formula f : result)
			{
				Utils.OUT.println("\t" + f.getArgument(1) + " in " + f);
			}
		}
	}

	@Test
	public void testAskAllSuperrelationsOf()
	{
		//Collection<Formula> result = KBLoader.kb.askWithPredicateSubsumption();
		String[] ts = new String[]{"brother", "sister", "sibling", "parent", "familyRelation", "relative", "engineeringSubcomponent"};
		for (String t : ts)
		{
			Utils.OUT.println(t);
			Collection<com.articulate.sigma.Formula> result = KBLoader.kb.askWithRestriction(0, "subrelation", 1, t);
			for (com.articulate.sigma.Formula f : result)
			{
				Utils.OUT.println("\t" + f.getArgument(2) + " in " + f);
			}
		}
	}

	@Test
	public void testAsk012()
	{
		String[] ts = new String[]{"sister", "brother", "sibling", "parent", "familyRelation", "relative"};
		for (String t : ts)
		{
			testAsk012(t);
		}
		Utils.OUT.println();
	}

	private void testAsk012(String term)
	{
		Utils.OUT.println(term);
		for (int i = 0; i < 3; i++)
		{
			Utils.OUT.println("\t" + i);
			Collection<com.articulate.sigma.Formula> result = KBLoader.kb.ask("arg", i, term);
			for (com.articulate.sigma.Formula f : result)
			{
				Utils.OUT.println("\t\t" + f);
			}
		}
		Utils.OUT.println();
	}

	@Test
	public void testAskWithPredicateSubsumption()
	{
		for (String t : new String[]{"Internet", "TelevisionSystem", "RadioSystem"})
		{
			Utils.OUT.println(t);
			Collection<com.articulate.sigma.Formula> result = KBLoader.kb.askWithPredicateSubsumption("part", 2, t);
			for (com.articulate.sigma.Formula f : result)
			{
				Utils.OUT.println("\t" + f);
			}
		}
		Utils.OUT.println();
	}

	@BeforeAll
	public static void init()
	{
	}

	@AfterAll
	public static void shutdown()
	{
	}

	public static void main(String[] args)
	{
		new KBLoader().load();
		init();
		TestAsk d = new TestAsk();
	}
}
