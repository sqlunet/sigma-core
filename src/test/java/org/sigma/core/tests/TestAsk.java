/*
 * Copyright (c) 2022.
 * This code is copyright Bernard Bou <1313ou@gmail.com>
 * This software is released under the GNU Public License 3 <http://www.gnu.org/copyleft/gpl.html>.
 */

package org.sigma.core.tests;

import org.sigma.core.BaseKB;
import org.sigma.core.BaseSumoProvider;
import org.sigma.core.Dump;
import org.sigma.core.Formula;
import org.sigma.core.Utils;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith({BaseSumoProvider.class})
public class TestAsk
{
	@Test
	public void testAsk()
	{
		String term = "range";
		int pos = 0;
		Utils.OUT.println(term + ':');
		Collection<Formula> result = BaseSumoProvider.SUMO.ask(BaseKB.ASK_ARG, pos, term);
		for (Formula f : result)
		{
			Utils.OUT.println("\t" + f);
		}
	}

	@Test
	public void testAskAllSubrelations()
	{
		//Collection<Formula> result = BaseKBLoader.kb.askWithPredicateSubsumption();
		Collection<Formula> result = BaseSumoProvider.SUMO.ask(BaseKB.ASK_ARG, 0, "subrelation");
		for (Formula f : result)
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
			Utils.OUT.println(t + ':');
			Collection<Formula> result = BaseSumoProvider.SUMO.askWithRestriction(0, "subrelation", 2, t);
			for (Formula f : result)
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
			Utils.OUT.println(t + ':');
			Collection<Formula> result = BaseSumoProvider.SUMO.askWithRestriction(0, "subrelation", 1, t);
			for (Formula f : result)
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
		Utils.OUT.println(term + ':');
		for (int i = 0; i < 3; i++)
		{
			Utils.OUT.println("\t@" + i);
			Collection<Formula> result = BaseSumoProvider.SUMO.ask(BaseKB.ASK_ARG, i, term);
			for (Formula f : result)
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
			Utils.OUT.println("\t" + t + ':');
			Collection<Formula> result = BaseSumoProvider.SUMO.askWithPredicateSubsumption(reln, 2, t);
			for (Formula f : result)
			{
				Utils.OUT.println("\t\t" + f.getArgument(0) + "<" + reln + " and " + f);
			}
		}
		Utils.OUT.println();
	}

	@Test
	public void testAskTermsWithPredicateSubsumption()
	{
		for (String reln : new String[]{"part"})
		{
			Utils.OUT.println(reln + ':');
			final Set<String> predicatesUsed = new HashSet<>();
			Collection<String> result = BaseSumoProvider.SUMO.getTermsViaPredicateSubsumption(reln, 2, "Internet", 1, false, predicatesUsed);
			for (String t : result)
			{
				Utils.OUT.println("\t" + t + " " + predicatesUsed);
			}
		}
		Utils.OUT.println();
	}

	@Test
	public void testAskCommutativityOfAskWithRestriction()
	{
		var result1 = BaseSumoProvider.SUMO.askWithRestriction(1, "inverse", 0, "instance");
		var result2 = BaseSumoProvider.SUMO.askWithRestriction(0, "instance", 1, "inverse");
		Utils.OUT.println(result1);
		Utils.OUT.println(result2);
		assertEquals(result1, result2);
	}

	@Test
	public void testTransitiveClosureViaPredicateSubsumption()
	{
		for (String reln : new String[]{"part"})
		{
			Utils.OUT.println("\t" + reln);
			Collection<String> result = BaseSumoProvider.SUMO.getTransitiveClosureViaPredicateSubsumption(reln, 2, "Internet", 1, false);
			for (String t : result)
			{
				Utils.OUT.println("\t\t" + t);
			}
		}
		Utils.OUT.println();
	}

	@Test
	public void testPredicateSubsumption()
	{
		for (String reln : new String[]{"part"})
		{
			Utils.OUT.println(reln);
			Collection<String> result = BaseSumoProvider.SUMO.querySubsumedRelationsOf(reln);
			for (String t : result)
			{
				Utils.OUT.println("\t" + t);
			}
		}
		Utils.OUT.println();
	}

	@BeforeAll
	public static void init()
	{
		Dump.dumpClasses(BaseSumoProvider.SUMO, Utils.OUT);
		Dump.dumpClassTrees(BaseSumoProvider.SUMO, Utils.OUT);
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
