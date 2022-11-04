/*
 * Copyright (c) 2022.
 * This code is copyright Bernard Bou <1313ou@gmail.com>
 * This software is released under the GNU Public License 3 <http://www.gnu.org/copyleft/gpl.html>.
 */

package org.sigma.core.tests;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sigma.core.Formula;
import org.sigma.core.Lisp;
import org.sigma.core.RowVars;
import org.sigma.core.SumoProvider;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.sigma.core.Helpers.OUT;

@ExtendWith({SumoProvider.class})
public class TestRowVarsWithKb
{
	@BeforeAll
	public static void init()
	{
		SumoProvider.SUMO.buildRelationCaches();
		SumoProvider.SUMO.buildRelationValenceCache();
	}

	//  E X P A N D

	private static final Function<String, Integer> arityGetter = r -> {
		int valence = SumoProvider.SUMO.getValence(r);
		OUT.println(r + " has valence " + valence);
		return valence;
	};

	private static final Formula[] TO_EXPAND = { //
			Formula.of("(or (attribute @ROW) (property @ROW))"), //
			Formula.of("(or (attribute @ROW) (domain @ROW))"), //
			Formula.of("(or (attribute @ROW) (foobar @ROW))"), //

			Formula.of("(or (attribute a @ROW) (domain a @ROW))"), //
			Formula.of("(or (attribute @ROW b) (domain @ROW b))"), //

			Formula.of("(or (partition a @ROW) (subclass @ROW a))"), //

			Formula.of("(or (?REL1 @ROW) (?REL2 a @ROW))"), //
	};

	@Test
	public void expandRowVarsWithKb()
	{
		for (Formula f : TO_EXPAND)
		{
			List<Formula> rfs = RowVars.expandRowVars(f, arityGetter);
			OUT.println("formula=\n" + f.toFlatString());
			OUT.println("expanded=\n" + rfs.stream().map(Formula::toFlatString).collect(Collectors.joining("\n")));
			OUT.println();
		}
	}

	// C O M P U T E

	@Test
	public void computeRowVarsWithRelations()
	{
		final Map<String, String> varsToVars = Map.of("?MYROW3", "@MYROW1", "?MYROW4", "@MYROW2");
		OUT.println("varsToVars=" + varsToVars);
		OUT.println();

		var fs = List.of("(rel @ROW)", "(rel ?MYROW3)", "(a (rel @ROW) b)", "(=> (a (rel3 @ROW) b) (rel4 c @ROW) d))", "(=> (rel1 @MYROW1) (rel2 @MYROW2))", "(=> (rel1 ?MYROW3) (rel2 ?MYROW4))");
		for (String f : fs)
		{
			final Map<String, Set<String>> varsToRelns = new HashMap<>();
			RowVars.computeRowVarsWithRelations(f, varsToVars, varsToRelns);
			OUT.println("formula=" + f);
			OUT.println("varsToRelns=" + varsToRelns);
			OUT.println();
			assertFalse(varsToRelns.isEmpty());
		}
	}

	private static final String[] TEMPLATES = new String[]{ //
			"(and (%s @ROW) (%s @ROW))", "(=> (%s @ROW) (%s @ROW))", //
	};

	private void getRowVarExpansionRange(String[][] ps, int min, int max)
	{
		for (String[] p : ps)
		{
			for (String t : TEMPLATES)
			{
				String f = String.format(t, p[0], p[1]);
				OUT.println("formula=" + f);
				int[] count = RowVars.getRowVarExpansionRange(f, "@ROW", arityGetter);
				OUT.println("out=" + Arrays.toString(count));
				OUT.println();
				assertEquals(min, count[0]);
				assertEquals(max, count[1]);
			}
		}
	}

	@Test
	public void getRowVarExpansionRange2()
	{
		String[][] ps = new String[][]{ //
				{"brother", "sister"}, // 2 2
				{"sister", "brother"}, // 2 2
				{"brother", "domain"}, // 2 3
				{"domain", "brother"}, // 3 2
				{"brother", "rel"}, // 2 -1
				{"rel", "brother"}, // -1 2
				{"brother", "partition"}, // 2 0
				{"partition", "brother"}, // 0 2
		};
		getRowVarExpansionRange(ps, 1, 3);
	}

	@Test
	public void getRowVarExpansionRange3()
	{
		String[][] ps = new String[][]{ //
				{"domain", "documentation"}, // 3 3
				{"documentation", "domain"}, // 3 3

				{"rel", "domain"}, // -1 3
				{"domain", "rel"}, // 3 -1
				{"partition", "domain"}, //  0 3
				{"domain", "partition"}, // 3 0
		};
		getRowVarExpansionRange(ps, 1, 4);
	}

	@Test
	public void getRowVarExpansionRangeUnknown()
	{
		String[][] ps = new String[][]{ //
				{"rel", "partition"}, // -1 0
				{"partition", "rel"}, // 0 -1
				{"partition", "partition"}, // 0 0
				{"rel", "rel"}, // -1 -1
		};
		getRowVarExpansionRange(ps, 0, 8);
	}

	//A D J U S T

	private static final String args = "a1 b2 c3 d4 e5 f6 g7 h8 i9 j0 ";

	@Test
	public void adjustExpansionCountUnknown()
	{
		final int inCount = RowVars.MAX_EXPANSION;

		for (int i = 0; i < 10; i++)
		{
			String f = "(rel @ROW " + args.substring(0, i * 3) + ")";
			OUT.println("formula=" + f);
			int nargs = Lisp.elements(Lisp.cdr(f)).size();
			assertEquals(i + 1, nargs);

			String reln = Lisp.car(f);
			int arity = arityGetter.apply(reln);
			boolean hasVariableArityRelation = arity == 0;
			int count = RowVars.adjustExpansionCount(f, "@ROW", hasVariableArityRelation, inCount);
			OUT.println("in=" + inCount);
			OUT.println("out=" + count + (hasVariableArityRelation ? " --- variable-arity reln" : ""));
			OUT.println();
			// assertEquals(fs.get(f), count);
		}
	}

	@Test
	public void adjustExpansionCountTwo()
	{
		final int inCount = 2;

		for (int i = 0; i < 10; i++)
		{
			String f = "(brother @ROW " + args.substring(0, i * 3) + ")";
			OUT.println("formula=" + f);
			String reln = Lisp.car(f);
			int nargs = Lisp.elements(Lisp.cdr(f)).size();
			int arity = arityGetter.apply(reln);
			boolean hasVariableArityRelation = arity == 0;
			int count = RowVars.adjustExpansionCount(f, "@ROW", hasVariableArityRelation, inCount);
			OUT.println("in=" + inCount);
			OUT.println("out=" + count + (hasVariableArityRelation ? " --- variable-arity reln" : ""));
			OUT.println();
			// assertEquals(fs.get(f), count);
		}
	}

	@Test
	public void adjustExpansionCountVariable()
	{
		final int inCount = 0;

		for (int i = 1; i < 10; i++)
		{
			String f = "(partition @ROW " + args.substring(0, i * 3) + ")";
			OUT.println("formula=" + f);
			String reln = Lisp.car(f);
			int nargs = Lisp.elements(Lisp.cdr(f)).size();
			int arity = arityGetter.apply(reln);
			boolean hasVariableArityRelation = arity == 0;
			int count = RowVars.adjustExpansionCount(f, "@ROW", hasVariableArityRelation, inCount);
			OUT.println("in=" + inCount);
			OUT.println("out=" + count + (hasVariableArityRelation ? " --- variable-arity reln" : ""));
			OUT.println();
			// assertEquals(fs.get(f), count);
		}
	}
}
