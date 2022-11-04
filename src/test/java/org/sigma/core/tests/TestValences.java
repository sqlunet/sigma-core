/*
 * Copyright (c) 2022.
 * This code is copyright Bernard Bou <1313ou@gmail.com>
 * This software is released under the GNU Public License 3 <http://www.gnu.org/copyleft/gpl.html>.
 */

package org.sigma.core.tests;

import org.sigma.core.NotNull;
import org.sigma.core.SumoProvider;
import org.sigma.core.Helpers;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sigma.core.Sumo;

import java.io.PrintStream;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith({SumoProvider.class})
public class TestValences
{
	private static final String[] RELS = { //
			"partition", //
			"instance", "range", "subclass", "subset", "subCollection", "son", "brother", //
			"domain", "domainSubclass", "documentation", "trusts", //

			"StartFn", "SineFn", //
			"AdditionFn", "UnionFn", "KappaFn", // //
			"SubstringFn", //

			"part", "piece", "depth",  //
			"ethnicityPercentInRegion", "sectorCompositionOfGDPInPeriod", "sharedBorderLength", "totalGDPInPeriod", //
	};

	private static final String[] RELS_SAMPLES = { //
			"instance", "subclass", "subset", "element", "partition", "range", "property", "attribute", "part", "piece", "holdsDuring", "holds", "parents", "PropertyFn", "ListFn", "MemberFn",};

	private static final String[] RELS0 = { //
			"partition", //
	};

	private static final String[] RELS1 = { //
			"StartFn", "SineFn", //
	};

	private static final String[] RELS2 = { //
			"instance", "range", "subclass", "subset", "subCollection", "son", "brother", //
			"part", "piece", //
			"holdsDuring", //
			"AdditionFn", "UnionFn", "KappaFn", // //
	};

	private static final String[] RELS3 = { //
			"domain", "domainSubclass", "documentation", //
			"depth", //
			"SubstringFn", //
	};

	private static void getRelValences(final String[] relns, final Sumo sumo, final PrintStream ps)
	{
		for (String reln : relns)
		{
			var valence = sumo.getValence(reln);
			ps.printf("'%s' valence %s%n", reln, valence);
		}
	}

	private static void getRelValences(final String[] relns, int expected, final Sumo sumo, final PrintStream ps)
	{
		for (String reln : relns)
		{
			var valence = sumo.getValence(reln);
			ps.printf("'%s' valence %s%n", reln, valence);
			assertEquals(expected, valence, String.format("'%s' valence %d (expected %d)", reln, valence, expected));
		}
	}

	@Test
	public void valencesTest0()
	{
		getRelValences(RELS0, 0, SumoProvider.SUMO, Helpers.OUT);
	}

	@Test
	public void valencesTest1()
	{
		getRelValences(RELS1, 1, SumoProvider.SUMO, Helpers.OUT);
	}

	@Test
	public void valencesTest2()
	{
		getRelValences(RELS2, 2, SumoProvider.SUMO, Helpers.OUT);
	}

	@Test
	public void valencesTest3()
	{
		getRelValences(RELS3, 3, SumoProvider.SUMO, Helpers.OUT);
	}

	@Test
	public void valencesTestSamples()
	{
		getRelValences(RELS_SAMPLES, SumoProvider.SUMO, Helpers.OUT);
	}

	@Test
	public void valencesCache()
	{
		SumoProvider.SUMO.relationValences.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e -> Helpers.OUT.println(e.getKey() + " " + Arrays.toString(e.getValue())));
	}

	@Test
	public void valencesCache0()
	{
		SumoProvider.SUMO.relationValences.entrySet().stream().filter(e->e.getValue()[0] == 0).sorted(Map.Entry.comparingByKey()).forEach(e -> Helpers.OUT.println(e.getKey() + " " + Arrays.toString(e.getValue())));
	}

	@Test
	public void valencesCache1()
	{
		SumoProvider.SUMO.relationValences.entrySet().stream().filter(e->e.getValue()[0] == 1).sorted(Map.Entry.comparingByKey()).forEach(e -> Helpers.OUT.println(e.getKey() + " " + Arrays.toString(e.getValue())));
	}

	@Test
	public void valencesCache2()
	{
		SumoProvider.SUMO.relationValences.entrySet().stream().filter(e->e.getValue()[0] == 2).sorted(Map.Entry.comparingByKey()).forEach(e -> Helpers.OUT.println(e.getKey() + " " + Arrays.toString(e.getValue())));
	}

	@Test
	public void valencesCache3()
	{
		SumoProvider.SUMO.relationValences.entrySet().stream().filter(e->e.getValue()[0] == 3).sorted(Map.Entry.comparingByKey()).forEach(e -> Helpers.OUT.println(e.getKey() + " " + Arrays.toString(e.getValue())));
	}

	@Test
	public void valencesSpecialCache()
	{
		var relns = SumoProvider.SUMO.collectRelations().stream().sorted().collect(Collectors.toCollection(TreeSet::new));
		for (String reln : relns)
		{
			@NotNull Collection<String> classNames = SumoProvider.SUMO.getCachedRelationValues("instance", reln, 1, 2);

			// The kluge below is to deal with the fact that a function, by definition, has a valence
			// one less than the corresponding predicate.
			// An instance of TernaryRelation that is also an instance of Function has a valence of 2, not 3.
			if (reln.endsWith("Fn"))
			{
				// OUT.printf("%s named as function {%s}%n", reln, classNames);
				var pred = reln.substring(0, reln.length() - 2);
				if (relns.contains(pred))
				{
					fail(String.format("%s instance of {%s}%n", reln, classNames));
				}
			}
			if (classNames.contains("Function"))
			{
				// OUT.printf("%s instance of Function {%s}%n", reln, classNames);
				if (classNames.contains("Relation"))
				{
					fail(String.format("%s instance of %s%n", reln, classNames));
				}
			}
		}
	}

	@BeforeAll
	public static void init()
	{
		SumoProvider.SUMO.buildRelationCaches();
		SumoProvider.SUMO.buildRelationValenceCache();
	}

	public static void main(String[] args)
	{
		new SumoProvider().load();
		init();
		getRelValences(RELS, SumoProvider.SUMO, Helpers.OUT);
		TestValences t = new TestValences();
		t.valencesTest0();
		t.valencesTest1();
		t.valencesTest2();
		t.valencesTest3();
	}
}
