package com.articulate.sigma;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sqlunet.sumo.Sumo;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static com.articulate.sigma.Utils.OUT;
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
		getRelValences(RELS0, 0, SumoProvider.sumo, Utils.OUT);
	}

	@Test
	public void valencesTest1()
	{
		getRelValences(RELS1, 1, SumoProvider.sumo, Utils.OUT);
	}

	@Test
	public void valencesTest2()
	{
		getRelValences(RELS2, 2, SumoProvider.sumo, Utils.OUT);
	}

	@Test
	public void valencesTest3()
	{
		getRelValences(RELS3, 3, SumoProvider.sumo, Utils.OUT);
	}

	@Test
	public void valencesTestSamples()
	{
		getRelValences(RELS_SAMPLES, SumoProvider.sumo, Utils.OUT);
	}

	@Test
	public void valencesCache()
	{
		SumoProvider.sumo.relationValences.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e -> Utils.OUT.println(e.getKey() + " " + Arrays.toString(e.getValue())));
	}

	@Test
	public void valencesSpecialCache()
	{
		var relns = SumoProvider.sumo.collectRelations().stream().sorted().collect(Collectors.toCollection(TreeSet::new));
		for (String reln : relns)
		{
			@NotNull Set<String> classNames = SumoProvider.sumo.getCachedRelationValues("instance", reln, 1, 2);

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
		SumoProvider.sumo.buildRelationCaches();
		SumoProvider.sumo.cacheRelationValences();
	}

	public static void main(String[] args)
	{
		new SumoProvider().load();
		init();
		getRelValences(RELS, SumoProvider.sumo, Utils.OUT);
		TestValences t = new TestValences();
		t.valencesTest0();
		t.valencesTest1();
		t.valencesTest2();
		t.valencesTest3();
	}
}
