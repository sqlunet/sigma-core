/*
 * Copyright (c) 2022.
 * This code is copyright Bernard Bou <1313ou@gmail.com>
 * This software is released under the GNU Public License 3 <http://www.gnu.org/copyleft/gpl.html>.
 */

package org.sigma.core.tests;

import org.sigma.core.BaseKB;
import org.sigma.core.BaseSumoProvider;
import org.sigma.core.Dump;
import org.sigma.core.Formula;
import org.sigma.core.Helpers;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith({BaseSumoProvider.class})
public class TestAsk
{
	@Test
	public void askFormulas()
	{
		String term = "range";
		int pos = 0;
		Helpers.OUT.println(term + '@' + pos);
		Collection<Formula> result = BaseSumoProvider.SUMO.ask(BaseKB.AskKind.ARG, pos, term);
		result.forEach(f -> Helpers.OUT.println("\t" + f));
	}

	@Test
	public void askSubrelationFormulas()
	{
		Collection<Formula> result = BaseSumoProvider.SUMO.ask(BaseKB.AskKind.ARG, 0, "subrelation");
		result.forEach(Helpers.OUT::println);
	}

	@Test
	public void askPositions012()
	{
		String[] ts = new String[]{"part", "sister", "brother", "sibling", "parent", "familyRelation", "relative"};
		for (String t : ts)
		{
			askPositions012(t);
		}
		Helpers.OUT.println();
	}

	private void askPositions012(String term)
	{
		Helpers.OUT.println(term + ':');
		for (int i = 0; i < 3; i++)
		{
			Helpers.OUT.println("\t@" + i);
			Collection<Formula> result = BaseSumoProvider.SUMO.ask(BaseKB.AskKind.ARG, i, term);
			for (Formula f : result)
			{
				Helpers.OUT.println("\t\t" + f);
			}
		}
		Helpers.OUT.println();
	}

	@Test
	public void askCommutativityOfAskWithRestriction()
	{
		var result1 = BaseSumoProvider.SUMO.askWithRestriction(1, "inverse", 0, "instance");
		var result2 = BaseSumoProvider.SUMO.askWithRestriction(0, "instance", 1, "inverse");
		Helpers.OUT.println(result1);
		Helpers.OUT.println(result2);
		assertEquals(result1, result2);
	}

	@Test
	public void askSubrelationsOf()
	{
		final int targetPos = 1;
		final int pos = 2;
		final String[] ts = new String[]{"part", "brother", "sister", "sibling", "parent", "familyRelation", "relative",};
		for (String t : ts)
		{
			Helpers.OUT.println(t + ':');
			Collection<Formula> result = BaseSumoProvider.SUMO.askWithRestriction(0, "subrelation", pos, t);
			for (Formula f : result)
			{
				Helpers.OUT.println("\t" + f.getArgument(targetPos) + " - is arg[" + targetPos + "] in " + f);
			}
		}
	}

	@Test
	public void askSuperrelationsOf()
	{
		final int targetPos = 2;
		final int pos = 1;
		String[] ts = new String[]{"part", "brother", "sister", "sibling", "parent", "familyRelation", "relative", "engineeringSubcomponent"};
		for (String t : ts)
		{
			Helpers.OUT.println(t + ':');
			Collection<Formula> result = BaseSumoProvider.SUMO.askWithRestriction(0, "subrelation", pos, t);
			for (Formula f : result)
			{
				Helpers.OUT.println("\t" + f.getArgument(targetPos) + " - is arg[" + targetPos + "] in " + f);
			}
		}
	}

	@Test
	public void getDirectSuperClassesOf()
	{
		String[] cs = new String[]{"BinaryRelation", "Vertebrate"};
		for (String c : cs)
		{
			Collection<String> result = BaseSumoProvider.SUMO.getDirectSuperClassesOf(c);
			Helpers.OUT.println(c + ": " + result.size());
			result.stream().sorted().forEach(superc -> Helpers.OUT.println("\t" + superc));
		}
	}

	@Test
	public void getDirectSubClassesOf()
	{
		String[] cs = new String[]{"BinaryRelation", "Vertebrate"};
		for (String c : cs)
		{
			Collection<String> result = BaseSumoProvider.SUMO.getDirectSubClassesOf(c);
			Helpers.OUT.println(c + ": " + result.size());
			result.stream().sorted().forEach(subc -> Helpers.OUT.println("\t" + subc));
		}
	}

	@Test
	public void askWithPredicateSubsumption()
	{
		final int targetPos = 2;
		final int targetPos2 = 1;
		final String reln = "part";
		Helpers.OUT.println(reln);
		for (String t : new String[]{"Car", "Europe"})
		{
			Helpers.OUT.println("\t" + t + ':');
			Collection<Formula> result = BaseSumoProvider.SUMO.askWithPredicateSubsumption(reln, targetPos, t);
			for (Formula f : result)
			{
				Helpers.OUT.println("\t\t" + f.getArgument(targetPos2) + " - " + f.getArgument(0) + "<" + reln + " && " + f);
			}
		}
		Helpers.OUT.println();
	}

	@Test
	public void askTermsWithPredicateSubsumption()
	{
		for (String reln : new String[]{"part"})
		{
			for (String arg : new String[]{"Internet", "Car", "Europe"})
			{
				Helpers.OUT.println(reln + "(?@1 " + arg + "@2):");
				Collection<String> result = BaseSumoProvider.SUMO.queryTermsWithSubsumption(reln, arg, 2, 1, false);
				result.forEach(t -> Helpers.OUT.println("\t" + t));
			}
		}
		Helpers.OUT.println();
	}

	@Test
	public void transitiveClosureOf()
	{
		final int pos = 2;
		final int targetPos = 1;
		for (String reln : new String[]{"part"})
		{
			for (String arg : new String[]{"Car", "Europe"})
			{
				Collection<String> result = BaseSumoProvider.SUMO.getTransitiveClosure(reln, 2, arg, 1, true);
				Helpers.OUT.println("closure of " + reln + "(" + arg + "@" + pos + " ?@" + targetPos + ") " + result.size());
				result.stream().sorted().forEach(t -> Helpers.OUT.println("\t" + t));
			}
		}
		Helpers.OUT.println();
	}

	@Test
	public void transitiveSubclassClosureOf()
	{
		final int pos = 2;
		final int targetPos = 1;
		for (String reln : new String[]{"subclass"})
		{
			for (String arg : new String[]{"BinaryRelation", "Vertebrate"})
			{
				Collection<String> result = BaseSumoProvider.SUMO.getTransitiveClosure(reln, pos, arg, targetPos, true);
				Helpers.OUT.println("closure of " + reln + "(" + arg + "@" + pos + " ?@" + targetPos + ") " + result.size());
				result.stream().sorted().forEach(t -> Helpers.OUT.println("\t" + t));
			}
		}
		Helpers.OUT.println();
	}

	@Test
	public void transitiveSuperclassClosureOf()
	{
		final int pos = 1;
		final int targetPos = 2;
		for (String reln : new String[]{"subclass"})
		{
			for (String arg : new String[]{"BinaryRelation", "Vertebrate"})
			{
				Collection<String> result = BaseSumoProvider.SUMO.getTransitiveClosure(reln, pos, arg, targetPos, true);

				Helpers.OUT.println("closure of " + reln + "(" + arg + "@" + pos + " ?@" + targetPos + ") " + result.size());
				result.stream().sorted().forEach(t -> Helpers.OUT.println("\t" + t));
			}
		}
		Helpers.OUT.println();
	}

	@Test
	public void getAllSuperClassesOf()
	{
		String[] cs = new String[]{"BinaryRelation", "Vertebrate"};
		for (String c : cs)
		{
			Collection<String> result = BaseSumoProvider.SUMO.getAllSuperClassesOf(c);
			Helpers.OUT.println(c + ": " + result.size());
			result.stream().sorted().forEach(superc -> Helpers.OUT.println("\t" + superc));
		}
	}

	@Test
	public void getAllSubClassesOf()
	{
		String[] cs = new String[]{"BinaryRelation", "Vertebrate"};
		for (String c : cs)
		{
			Collection<String> result = BaseSumoProvider.SUMO.getAllSubClassesOf(c);
			Helpers.OUT.println(c + ": " + result.size());
			result.stream().sorted().forEach(subc -> Helpers.OUT.println("\t" + subc));
		}
	}

	@Test
	public void subsumedRelationsOf()
	{
		for (String reln : new String[]{"part", "subclass", "instance"})
		{
			Collection<String> result = BaseSumoProvider.SUMO.querySubsumedRelationsOf(reln);
			Helpers.OUT.println(reln + ": " + result.size());
			result.forEach(r -> Helpers.OUT.println("\t" + r));
		}
	}

	@Test
	public void isChildOf()
	{
		var parentsChildren = new String[][][]{ //
				{{"BinaryRelation"}, {"UnaryFunction", "SymmetricRelation", "SineFn", "suffers", "physicalEnd", }}, //
				{{"BinaryFunction"}, {"MaxFn", "KappaFn", "AssociativeFunction",}}, //
		};

		for (var parentChildren : parentsChildren)
		{
			var parent = parentChildren[0][0];
			Helpers.OUT.println(parent);
			for (var child : parentChildren[1])
			{
				final boolean result = BaseSumoProvider.SUMO.isChildOf(child, parent);
				Helpers.OUT.println("\t" + child + " is child of " + parent + ": " + result);
			}
		}
	}

	@BeforeAll
	public static void init() throws IOException
	{
		try (InputStream is = TestAsk.class.getResourceAsStream("/subsumption-tests.kif"))
		{
			BaseSumoProvider.SUMO.addConstituent(is, "subsumption-tests");
		}
		Dump.dumpClasses(BaseSumoProvider.SUMO, Helpers.OUT);
		Dump.dumpClassTrees(BaseSumoProvider.SUMO, Helpers.OUT);
	}

	@AfterAll
	public static void shutdown()
	{
	}

	public static void main(String[] args) throws IOException
	{
		new BaseSumoProvider().load();
		init();
		TestAsk a = new TestAsk();
		a.askFormulas();
		a.askSubrelationFormulas();
		a.askPositions012();
		a.askCommutativityOfAskWithRestriction();
		a.askSubrelationsOf();
		a.askSuperrelationsOf();
		a.getDirectSuperClassesOf();
		a.getDirectSubClassesOf();
		a.askWithPredicateSubsumption();
		a.askTermsWithPredicateSubsumption();
		a.transitiveClosureOf();
		a.transitiveSubclassClosureOf();
		a.transitiveSuperclassClosureOf();
		a.getAllSuperClassesOf();
		a.getAllSubClassesOf();
		a.subsumedRelationsOf();
		shutdown();
	}
}
