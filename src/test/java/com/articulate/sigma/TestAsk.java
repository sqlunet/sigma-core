package com.articulate.sigma;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sqlunet.sumo.Dump;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith({BaseSumoProvider.class})
public class TestAsk
{
	@Test
	public void testDumpSubClassesOf()
	{
		Dump.dumpSubClassesOf(BaseSumoProvider.sumo, "Insect", Utils.OUT);
	}

	@Test
	public void testDumpClasses()
	{
		Dump.dumpClasses(BaseSumoProvider.sumo, Utils.OUT);
	}

	@Test
	public void testDumpTermTree()
	{
		Dump.dumpClassTrees(BaseSumoProvider.sumo, Utils.OUT);
	}

	@Test
	public void testAsk()
	{
		String term = "engineeringSubcomponent";
		int pos = 0;
		Utils.OUT.println(term + " @ " + pos);
		Collection<com.articulate.sigma.Formula> result = BaseSumoProvider.sumo.ask(BaseKB.ASK_ARG, pos, term);
		for (com.articulate.sigma.Formula f : result)
		{
			Utils.OUT.println("\t" + f);
		}
	}

	@Test
	public void testAskAllSubrelations()
	{
		//Collection<Formula> result = BaseKBLoader.kb.askWithPredicateSubsumption();
		Collection<com.articulate.sigma.Formula> result = BaseSumoProvider.sumo.ask(BaseKB.ASK_ARG, 0, "subrelation");
		for (com.articulate.sigma.Formula f : result)
		{
			Utils.OUT.println(f);
		}
	}

	@Test
	public void testAskAllSubrelationsOf()
	{
		//Collection<Formula> result = BaseKBLoader.kb.askWithPredicateSubsumption();
		String[] ts = new String[]{"brother", "sister", "sibling", "parent", "familyRelation", "relative", "part"};
		for (String t : ts)
		{
			Utils.OUT.println(t);
			Collection<com.articulate.sigma.Formula> result = BaseSumoProvider.sumo.askWithRestriction(0, "subrelation", 2, t);
			for (com.articulate.sigma.Formula f : result)
			{
				Utils.OUT.println("\t" + f.getArgument(1) + " in " + f);
			}
		}
	}

	@Test
	public void testAskAllSuperrelationsOf()
	{
		//Collection<Formula> result = BaseKBLoader.kb.askWithPredicateSubsumption();
		String[] ts = new String[]{"brother", "sister", "sibling", "parent", "familyRelation", "relative", "engineeringSubcomponent"};
		for (String t : ts)
		{
			Utils.OUT.println(t);
			Collection<com.articulate.sigma.Formula> result = BaseSumoProvider.sumo.askWithRestriction(0, "subrelation", 1, t);
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
			Utils.OUT.println("\t@" + i);
			Collection<com.articulate.sigma.Formula> result = BaseSumoProvider.sumo.ask(BaseKB.ASK_ARG, i, term);
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
		final String reln = "part";
		Utils.OUT.println(reln);
		for (String t : new String[]{"Internet", "TelevisionSystem", "RadioSystem"})
		{
			Utils.OUT.println("\t" + t);
			Collection<com.articulate.sigma.Formula> result = BaseSumoProvider.sumo.askWithPredicateSubsumption(reln, 2, t);
			for (com.articulate.sigma.Formula f : result)
			{
				Utils.OUT.println("\t\t" + f.getArgument(0) + "<" + reln + " " + f);
			}
		}
		Utils.OUT.println();
	}

	@Test
	public void testAskTermsWithPredicateSubsumption()
	{
		for (String reln : new String[]{"part"})
		{
			Utils.OUT.println("\t" + reln);
			final Set<String> predicatesUsed = new HashSet<>();
			Collection<String> result = BaseSumoProvider.sumo.getTermsViaPredicateSubsumption(reln, 2, "Internet", 1, false, predicatesUsed);
			for (String t : result)
			{
				Utils.OUT.println("\t\t" + t + " " + predicatesUsed);
			}
		}
		Utils.OUT.println();
	}

	@Test
	public void testTransitiveClosureViaPredicateSubsumption()
	{
		for (String reln : new String[]{"part"})
		{
			Utils.OUT.println("\t" + reln);
			Collection<String> result = BaseSumoProvider.sumo.getTransitiveClosureViaPredicateSubsumption(reln, 2, "Internet", 1, false);
			for (String t : result)
			{
				Utils.OUT.println("\t\t" + t);
			}
		}
		Utils.OUT.println();
	}

	@Test
	public void testAskSymmetryOfAskWithRestriction()
	{
		String term0 = "instance";
		String term1 = "inverse";
		var result1 = BaseSumoProvider.sumo.askWithRestriction(1, term1, 0, term0);
		Utils.OUT.println(result1);
		Utils.OUT.println();
		var result2 = BaseSumoProvider.sumo.askWithRestriction(0, term0, 1, term1);
		Utils.OUT.println(result2);

		assertEquals(result1, result2);
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
		new BaseSumoProvider().load();
		init();
		TestAsk d = new TestAsk();
	}
}
