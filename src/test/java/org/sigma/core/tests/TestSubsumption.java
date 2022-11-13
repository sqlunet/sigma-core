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
import org.sigma.core.NotNull;
import org.sigma.core.SumoProvider;

import java.io.IOException;
import java.util.Collection;

@ExtendWith({SumoProvider.class})
public class TestSubsumption
{
	@Test
	public void testQueryAllSubrelationsOf()
	{
		@NotNull String[] ts = new String[]{"subrelation", "inverse"};
		for (@NotNull String t : ts)
		{
			Helpers.OUT.println(t);
			@NotNull Collection<String> result = SumoProvider.SUMO.queryRelation("subrelation", t, 2, 1);
			result.forEach(t2 -> Helpers.OUT.println("\t" + t2));
			Helpers.OUT.println();
		}
	}

	@Test
	public void testAskAllSubrelationsOf()
	{
		@NotNull String[] ts = new String[]{"subrelation", "inverse"};
		for (@NotNull String t : ts)
		{
			Helpers.OUT.println(t);
			@NotNull Collection<String> result = SumoProvider.SUMO.askRelation("subrelation", t, 2, 1);
			result.forEach(t2 -> Helpers.OUT.println("\t" + t2));
			Helpers.OUT.println();
		}
	}

	@Test
	public void testInverseRelationsOf()
	{
		SumoProvider.SUMO.getInverseRelations().forEach(t2 -> Helpers.OUT.println(t2));
	}

	@Test
	public void testSubRelationsOf()
	{
		SumoProvider.SUMO.getSubrelations().forEach(t2 -> Helpers.OUT.println(t2));
	}

	@BeforeAll
	public static void init()
	{
		SumoProvider.SUMO.buildRelationCaches();
	}

	@SuppressWarnings("EmptyMethod")
	@AfterAll
	public static void shutdown()
	{
	}

	public static void main(String[] args) throws IOException
	{
		new BaseSumoProvider().load();
		init();
		@NotNull TestSubsumption q = new TestSubsumption();
		q.testAskAllSubrelationsOf();
		q.testQueryAllSubrelationsOf();
	}
}
