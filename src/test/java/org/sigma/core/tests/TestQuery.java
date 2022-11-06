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
	public void testAskSubrelationsOf()
	{
		String[] ts = new String[]{"part", "brother", "sister", "sibling", "parent", "familyRelation", "relative", "subrelation", "instance", "inverse"};
		for (String t : ts)
		{
			Helpers.OUT.println(t);
			Collection<String> result = BaseSumoProvider.SUMO.query("subrelation", t, 2, 1);
			result.forEach(t2 -> Helpers.OUT.println("\t" + t2));
		}
	}

	@Test
	public void testAskSuperrelationsOf()
	{
		String[] ts = new String[]{"part", "brother", "sister", "sibling", "parent", "familyRelation", "relative", "engineeringSubcomponent"};
		for (String t : ts)
		{
			Helpers.OUT.println(t);
			Collection<String> result = BaseSumoProvider.SUMO.query("subrelation", t, 1, 2);
			result.forEach(t2 -> Helpers.OUT.println("\t" + t2));
		}
	}

	@Test
	public void testSubrelations()
	{
		Helpers.OUT.println("sub relations (for subsumption):");
		Collection<String> result = BaseSumoProvider.SUMO.getSubrelations();
		result.forEach(t2 -> Helpers.OUT.println("\t" + t2));
	}

	@Test
	public void testInverseRelations()
	{
		Helpers.OUT.println("inverse relations:");
		Collection<String> result = BaseSumoProvider.SUMO.getInverseRelations();
		result.forEach(t2 -> Helpers.OUT.println("\t" + t2));
	}

	@Test
	public void testAskInverseRelationsOf()
	{
		String[] ts = new String[]{"part", "smaller", "larger", "husband", "wife", "greaterThan"};
		for (String t : ts)
		{
			Helpers.OUT.println(t);
			Collection<String> result = BaseSumoProvider.SUMO.queryInverseRelationsOf(t);
			result.forEach(t2 -> Helpers.OUT.println("\t" + t2));
		}
	}

	@Test
	public void testGetTermsViaPredicateSubsumption0()
	{
		String[] rs = new String[]{"part"};
		String[] as = new String[]{"Europe", "car"};

		for (String r : rs)
		{
			for (String a : as)
			{
				@Nullable final Set<String> predicatesUsed = new HashSet<>();
				Collection<String> result = BaseSumoProvider.SUMO.getTermsViaPredicateSubsumption0(r, 2, a, 1, true, predicatesUsed);
				Helpers.OUT.println("[0]\t" + r + "(" + a + ") -> " + result + " using " + predicatesUsed);
				Helpers.OUT.println();
			}
		}
	}

	@Test
	public void testGetTermsViaPredicateSubsumption()
	{
		String[] rs = new String[]{"part"};
		String[] as = new String[]{"Europe", "car"};

		for (String r : rs)
		{
			for (String a : as)
			{
				@Nullable final Set<String> predicatesUsed2 = new HashSet<>();
				Collection<String> result2 = BaseSumoProvider.SUMO.getTermsViaPredicateSubsumption(r, 2, a, 1, true, predicatesUsed2);
				boolean empty2 = result2.isEmpty();
				if (!empty2)
				{
					Helpers.OUT.println("[1]\t" + r + "(" + a + ") -> " + result2 + " using " + predicatesUsed2);
					Helpers.OUT.println();
				}
			}
		}
	}

	@Test
	public void testCompareGetTermsViaPredicateSubsumption1vs2()
	{
		String[] rs = new String[]{"part"};
		String[] as = new String[]{"Europe", "car"};

		for (String r : rs)
		{
			for (String a : as)
			{
				@Nullable final Set<String> predicatesUsed = new HashSet<>();
				@Nullable final Set<String> predicatesUsed2 = new HashSet<>();
				Collection<String> result = BaseSumoProvider.SUMO.getTermsViaPredicateSubsumption(r, 2, a, 1, true, predicatesUsed);
				Collection<String> result2 = BaseSumoProvider.SUMO.getTermsViaPredicateSubsumption0(r, 2, a, 1, true, predicatesUsed2);
				Helpers.OUT.println("[1]\t" + r + "(" + a + ") -> " + result + " using " + predicatesUsed);
				Helpers.OUT.println("[0]\t" + r + "(" + a + ") -> " + result2 + " using " + predicatesUsed2);
				Helpers.OUT.println();
				assertEquals(result, result2);
			}
		}
	}

	@BeforeAll
	public static void init()
	{
		BaseSumoProvider.SUMO.addConstituent("tests.kif");
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
