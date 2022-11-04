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
import org.sigma.core.BaseKB;
import org.sigma.core.BaseSumoProvider;
import org.sigma.core.SumoProvider;
import org.sigma.core.Utils;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith({SumoProvider.class})
public class TestQueryCache
{
	@Test
	public void testQueryAllSubrelationsOf()
	{
		String[] ts = new String[]{"brother", "sister", "sibling", "parent", "familyRelation", "relative", "part"};
		for (String t : ts)
		{
			Utils.OUT.println(t);
			Collection<String> result = SumoProvider.SUMO.query("subrelation", t, 2, 1);
			result.forEach(t2 -> Utils.OUT.println("\t" + t2));
		}
	}

	@Test
	public void testAskAllSubrelationsOf()
	{
		String[] ts = new String[]{"brother", "sister", "sibling", "parent", "familyRelation", "relative", "part"};
		for (String t : ts)
		{
			Utils.OUT.println(t);
			Collection<String> result = SumoProvider.SUMO.ask("subrelation", t, 2, 1);
			result.forEach(t2 -> Utils.OUT.println("\t" + t2));
		}
	}

	@Test
	public void testCompareQueryAskAllSubrelationsOf()
	{
		String[] ts = new String[]{"brother", "sister", "sibling", "parent", "familyRelation", "relative", "part"};
		for (String t : ts)
		{
			Utils.OUT.println(t);
			Collection<String> askResult = SumoProvider.SUMO.ask("subrelation", t, 2, 1);
			Collection<String> queryResult = SumoProvider.SUMO.query("subrelation", t, 2, 1);
			assertTrue(queryResult.containsAll(askResult));
			//assertEquals(askResult, queryResultresult2);
		}
	}

	@Test
	public void testQueryPredicateSubsumption()
	{
		for (String reln : new String[]{"part"})
		{
			Utils.OUT.println(reln);
			Collection<String> result = SumoProvider.SUMO.querySubsumedRelationsOf(reln);
			for (String t : result)
			{
				Utils.OUT.println("\t" + t);
			}
		}
		Utils.OUT.println();
	}

	@Test
	public void testAskPredicateSubsumption()
	{
		for (String reln : new String[]{"part"})
		{
			Utils.OUT.println(reln);
			Collection<String> result = SumoProvider.SUMO.querySubsumedRelationsOf(reln);
			for (String t : result)
			{
				Utils.OUT.println("\t" + t);
			}
		}
		Utils.OUT.println();
	}

	@Test
	public void testComparePredicateSubsumptions()
	{
		for (String reln : new String[]{"part"})
		{
			Utils.OUT.println(reln);
			Collection<String> queyResult = SumoProvider.SUMO.querySubsumedRelationsOf(reln);
			for (String t : queyResult)
			{
				Utils.OUT.println("\t" + t);
			}
			Utils.OUT.println(reln);
			Collection<String> askResult = SumoProvider.SUMO.askSubsumedRelationsOf(reln);
			for (String t : queyResult)
			{
				Utils.OUT.println("\t" + t);
			}
			assertEquals(askResult, queyResult);
		}
		Utils.OUT.println();
	}

	@BeforeAll
	public static void init()
	{
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
