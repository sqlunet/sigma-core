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
import org.sigma.core.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith({BaseSumoProvider.class})
public class TestQuery
{
	@Test
	public void testAskAllSubrelationsOf()
	{
		//Collection<Formula> result = BaseKBLoader.kb.askWithPredicateSubsumption();
		String[] ts = new String[]{"brother", "sister", "sibling", "parent", "familyRelation", "relative", "part"};
		for (String t : ts)
		{
			Utils.OUT.println(t);
			Collection<String> result = BaseSumoProvider.SUMO.query("subrelation", t, 2, 1);
			result.forEach(t2 -> Utils.OUT.println("\t" + t2));
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
			Collection<String> result = BaseSumoProvider.SUMO.query("subrelation", t, 1, 2);
			result.forEach(t2 -> Utils.OUT.println("\t" + t2));
		}
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
		TestQuery d = new TestQuery();
	}
}
