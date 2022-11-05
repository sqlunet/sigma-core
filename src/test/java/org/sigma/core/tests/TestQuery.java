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
		String[] ts = new String[]{"brother", "sister", "sibling", "parent", "familyRelation", "relative", "part"};
		for (String t : ts)
		{
			Helpers.OUT.println(t);
			Collection<String> result = BaseSumoProvider.SUMO.query("subrelation", t, 2, 1);
			result.forEach(t2 -> Helpers.OUT.println("\t" + t2));
		}
	}

	@Test
	public void testAskAllSuperrelationsOf()
	{
		String[] ts = new String[]{"brother", "sister", "sibling", "parent", "familyRelation", "relative", "engineeringSubcomponent"};
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
		String[] ts = new String[]{"smaller", "larger", "husband", "wife", "greaterThan"};
		for (String t : ts)
		{
			Helpers.OUT.println(t);
			Collection<String> result = BaseSumoProvider.SUMO.queryInverseRelationsOf(t);
			result.forEach(t2 -> Helpers.OUT.println("\t" + t2));
		}
	}

	@Test
	public void testCompare()
	{
		String[] ts = new String[]{"disjoint", "smaller", "larger", "husband", "wife", "greaterThan"};
		String[] as = new String[]{"wife", "greaterThan"};
		for (String t : ts)
		{
			for (String a : as)
			{
				@Nullable final Set<String> predicatesUsed = new HashSet<>();
				@Nullable final Set<String> predicatesUsed2 = new HashSet<>();
				Helpers.OUT.println(t);
				Collection<String> result = BaseSumoProvider.SUMO.getTermsViaPredicateSubsumption(t, 1, a, 2, true, predicatesUsed);
				Collection<String> result2 = BaseSumoProvider.SUMO.getTermsViaPredicateSubsumption2(t, 1, a, 2, true, predicatesUsed2);
				Helpers.OUT.println("\tresult:");
				result.forEach(t2 -> Helpers.OUT.println("\t\t" + t2 + " " + predicatesUsed));
				Helpers.OUT.println("\tresult2:");
				result2.forEach(t2 -> Helpers.OUT.println("\t\t" + t2 + " " + predicatesUsed2));
			}
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
