/*
 * Copyright (c) 2022.
 * This code is copyright Bernard Bou <1313ou@gmail.com>
 * This software is released under the GNU Public License 3 <http://www.gnu.org/copyleft/gpl.html>.
 */

package org.sigma.core.tests;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sigma.core.BaseSumoProvider;
import org.sigma.core.Helpers;
import org.sigma.core.SumoProvider;

import java.util.Collection;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith({SumoProvider.class})
public class TestQueryCache
{
	@Test
	public void testQueryAllSubrelationsOf()
	{
		String[] ts = new String[]{"part", "brother", "sister", "sibling", "parent", "familyRelation", "relative", };
		for (String t : ts)
		{
			Helpers.OUT.println(t);
			Collection<String> result = SumoProvider.SUMO.query("subrelation", t, 2, 1);
			result.forEach(t2 -> Helpers.OUT.println("\t" + t2));
		}
	}

	@Test
	public void testAskAllSubrelationsOf()
	{
		String[] ts = new String[]{"part", "brother", "sister", "sibling", "parent", "familyRelation", "relative", };
		for (String t : ts)
		{
			Helpers.OUT.println(t);
			Collection<String> result = SumoProvider.SUMO.ask("subrelation", t, 2, 1);
			result.forEach(t2 -> Helpers.OUT.println("\t" + t2));
		}
	}

	@Test
	public void testCompareQueryAskAllSubrelationsOf()
	{
		String[] ts = new String[]{"part", "brother", "sister", "sibling", "parent", "familyRelation", "relative",};
		for (String t : ts)
		{
			Helpers.OUT.println(t);
			Collection<String> askResult = SumoProvider.SUMO.ask("subrelation", t, 2, 1);
			Collection<String> queryResult = SumoProvider.SUMO.query("subrelation", t, 2, 1);
			assertTrue(queryResult.containsAll(askResult));
			if (!queryResult.equals(askResult))
			{
				queryResult = new HashSet<>(queryResult);
				queryResult.removeAll(askResult);
				queryResult.stream().sorted().forEach(r -> Helpers.OUT.println("\t+ " + r));
			}
		}
	}

	@Test
	public void testQueryPredicateSubsumption()
	{
		for (String reln : new String[]{"part"})
		{
			Helpers.OUT.println(reln);
			Collection<String> result = SumoProvider.SUMO.querySubsumedRelationsOf(reln);
			result.stream().sorted().forEach(t -> Helpers.OUT.println("\t" + t));
		}
		Helpers.OUT.println();
	}

	@Test
	public void testAskPredicateSubsumption()
	{
		for (String reln : new String[]{"part"})
		{
			Helpers.OUT.println(reln);
			Collection<String> result = SumoProvider.SUMO.askSubsumedRelationsOf(reln);
			result.stream().sorted().forEach(t -> Helpers.OUT.println("\t" + t));
		}
		Helpers.OUT.println();
	}

	@Test
	public void testComparePredicateSubsumptions()
	{
		for (String reln : new String[]{"part"})
		{
			Helpers.OUT.println(reln);
			Collection<String> queryResult = SumoProvider.SUMO.querySubsumedRelationsOf(reln);
			queryResult.stream().sorted().forEach(t -> Helpers.OUT.println("\t" + t));

			Helpers.OUT.println(reln);
			Collection<String> askResult = SumoProvider.SUMO.askSubsumedRelationsOf(reln);
			askResult.stream().sorted().forEach(t -> Helpers.OUT.println("\t" + t));

			Helpers.OUT.println("diff");
			if (!queryResult.containsAll(askResult))
			{
				askResult.removeAll(queryResult);
				askResult.stream().sorted().forEach(r -> Helpers.OUT.println("\t- " + r));
				assertTrue(askResult.isEmpty());
			}
			if (!queryResult.equals(askResult))
			{
				queryResult.removeAll(askResult);
				queryResult.stream().sorted().forEach(r -> Helpers.OUT.println("\t+ " + r));
			}
		}
		Helpers.OUT.println();
	}

	@BeforeAll
	public static void init()
	{
		SumoProvider.SUMO.addConstituent("tests.kif");
		SumoProvider.SUMO.buildRelationCaches();
	}

	@AfterAll
	public static void shutdown()
	{
	}

	public static void main(String[] args)
	{
		new BaseSumoProvider().load();
		init();
		TestQueryCache q = new TestQueryCache();
		q.testAskAllSubrelationsOf();
		q.testQueryAllSubrelationsOf();
		q.testCompareQueryAskAllSubrelationsOf();
	}
}
