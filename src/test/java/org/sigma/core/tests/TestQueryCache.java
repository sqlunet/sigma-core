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
		TestQueryCache d = new TestQueryCache();
	}
}
