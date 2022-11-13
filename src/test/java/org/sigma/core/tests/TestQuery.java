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
import org.sigma.core.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@ExtendWith({BaseSumoProvider.class})
public class TestQuery
{
	@Test
	public void testAskSubrelationsOf()
	{
		@NotNull String[] ts = new String[]{"part", "brother", "sister", "sibling", "parent", "familyRelation", "relative", "subrelation", "instance", "inverse"};
		for (@NotNull String t : ts)
		{
			Helpers.OUT.println(t);
			@NotNull Collection<String> result = BaseSumoProvider.SUMO.queryRelation("subrelation", t, 2, 1);
			result.forEach(t2 -> Helpers.OUT.println("\t" + t2));
		}
	}

	@Test
	public void testAskSuperrelationsOf()
	{
		@NotNull String[] ts = new String[]{"part", "brother", "sister", "sibling", "parent", "familyRelation", "relative", "engineeringSubcomponent"};
		for (@NotNull String t : ts)
		{
			Helpers.OUT.println(t);
			@NotNull Collection<String> result = BaseSumoProvider.SUMO.queryRelation("subrelation", t, 1, 2);
			result.forEach(t2 -> Helpers.OUT.println("\t" + t2));
		}
	}

	@Test
	public void testSubrelations()
	{
		Helpers.OUT.println("sub relations (for subsumption):");
		@NotNull Collection<String> result = BaseSumoProvider.SUMO.getSubrelations();
		result.forEach(t2 -> Helpers.OUT.println("\t" + t2));
	}

	@Test
	public void testInverseRelations()
	{
		Helpers.OUT.println("inverse relations:");
		@NotNull Collection<String> result = BaseSumoProvider.SUMO.getInverseRelations();
		result.forEach(t2 -> Helpers.OUT.println("\t" + t2));
	}

	@Test
	public void testAskInverseRelationsOf()
	{
		@NotNull String[] ts = new String[]{"part", "smaller", "larger", "husband", "wife", "greaterThan"};
		for (@NotNull String t : ts)
		{
			Helpers.OUT.println(t);
			@NotNull Collection<String> result = BaseSumoProvider.SUMO.queryInverseRelationsOf(t);
			result.forEach(t2 -> Helpers.OUT.println("\t" + t2));
		}
	}

	@Test
	public void testGetTermsViaPredicateSubsumption()
	{
		@NotNull String[] rs = new String[]{"part"};
		@NotNull String[] as = new String[]{"Europe", "Car"};

		for (@NotNull String r : rs)
		{
			for (@NotNull String a : as)
			{
				@Nullable final Set<String> predicatesUsed2 = new HashSet<>();
				@NotNull Collection<String> result2 = BaseSumoProvider.SUMO.queryTermsWithSubsumption(r, a, 2, 1, true, predicatesUsed2);
				boolean empty2 = result2.isEmpty();
				if (!empty2)
				{
					Helpers.OUT.println("[1]\t" + r + "(" + a + ") -> " + result2 + " using " + predicatesUsed2);
					Helpers.OUT.println();
				}
			}
		}
	}

	@BeforeAll
	public static void init() throws IOException
	{
		try (@Nullable InputStream is = TestQuery.class.getResourceAsStream("/subsumption-tests.kif"))
		{
			assert is != null;
			BaseSumoProvider.SUMO.addConstituent(is, "subsumption-tests");
		}
	}

	@SuppressWarnings("EmptyMethod")
	@AfterAll
	public static void shutdown()
	{
		//
	}

	public static void main(String[] args) throws IOException
	{
		new BaseSumoProvider().load();
		init();
		@NotNull TestQuery q = new TestQuery();
		q.testAskSubrelationsOf();
		q.testAskSuperrelationsOf();
		q.testSubrelations();
		q.testInverseRelations();
		q.testAskInverseRelationsOf();
		q.testGetTermsViaPredicateSubsumption();
		shutdown();
	}
}
