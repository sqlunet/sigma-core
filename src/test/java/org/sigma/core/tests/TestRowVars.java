/*
 * Copyright (c) 2022.
 * This code is copyright Bernard Bou <1313ou@gmail.com>
 * This software is released under the GNU Public License 3 <http://www.gnu.org/copyleft/gpl.html>.
 */

package org.sigma.core.tests;

import org.sigma.core.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.sigma.core.Utils.OUT;

public class TestRowVars
{
	private static final Function<String, Integer> arityGetter = r -> {
		int valence;
		switch (r)
		{
			case "foo1":
				valence = 1;
				break;

			case "foo2":
			case "instance":
			case "subclass":
				valence = 2;
				break;

			case "foo3":
			case "domain":
				valence = 3;
				break;

			case "foo4":
				valence = 4;
				break;

			case "foo0":
			case "partition":
			case "assert":
			case "assertat":
				valence = 0;
				break;

			case "foo_":
			default:
				valence = -1;
				break;
		}
		// OUT.println(r + " has valence " + valence);
		return valence;
	};

	//  E X P A N D

	private static final Formula[] TO_EXPAND = { //

			Formula.of("(=> (foo1 @ROW) (bar @ROW))"), //
			Formula.of("(=> (foo2 @ROW) (bar @ROW))"), //
			Formula.of("(=> (foo3 @ROW) (bar @ROW))"), //
			Formula.of("(=> (foo4 @ROW) (bar @ROW))"), //
			Formula.of("(=> (foo0 @ROW) (bar @ROW))"), //
			Formula.of("(=> (foo_ @ROW) (bar @ROW))"), //

			Formula.of("(=> (foo1 @ROW) (foo4 @ROW))"), //
			Formula.of("(=> (foo2 @ROW) (foo3 @ROW))"), //
			Formula.of("(=> (foo3 @ROW) (foo2 @ROW))"), //
			Formula.of("(=> (foo4 @ROW) (foo1 @ROW))"), //
			Formula.of("(=> (foo0 @ROW) (foo_ @ROW))"), //
			Formula.of("(=> (foo_ @ROW) (foo0 @ROW))"), //

			Formula.of("(=> (instance @ROW S) (bar @ROW))"), //
			Formula.of("(=> (subclass C @ROW) (bar @ROW))"), //
			Formula.of("(=> (domain R 2 @ROW) (bar @ROW))"), //

			Formula.of("(=> (instance @ROW) (bar @ROW))"), //
			Formula.of("(=> (subclass @ROW) (bar @ROW))"), //
			Formula.of("(=> (domain @ROW) (bar @ROW))"), //
			Formula.of("(=> (domain R @ROW) (bar @ROW))"), //

			Formula.of("(=> (instance @I @C) (hasinstance @C @I))"), //
			Formula.of("(=> (subclass @C1 @C2) (superclass @C2 @C1))"), //
			Formula.of("(=> (and (isclass @C) (domain @ROW @C)) (foo @ROW @C))"), //

			Formula.of("(=> (partition C @ROW) (foo @ROW))"), //
			Formula.of("(=> (partition C @ROW) (isclass @ROW))"), //

			Formula.of("(=> (assert @ROW) (assertat ?T @ROW))"), //

			//			Formula.of("(=> (and (subrelation ?REL1 ?REL2) (holds__ ?REL1 @ROW)) (holds__ ?REL2 @ROW))"), //
			//			Formula.of("(=> (attribute @ROW) (property @ROW))"), //
			//			Formula.of("(=> (and (instance attribute Predicate) (instance property Predicate) (attribute @ROW1)) (property @ROW))"), //
			//			Formula.of("(=> (and (instance piece Predicate) (instance part Predicate) (piece @ROW)) (part @ROW))"), //
	};

	@Test
	public void expandRowVarsWithValenceProvider()
	{
		for (Formula f : TO_EXPAND)
		{
			List<Formula> rfs = RowVars.expandRowVars(f, arityGetter);
			OUT.println("formula=\n" + f.toFlatString());
			OUT.println("expanded=\n" + rfs.stream().map(Formula::toFlatString).collect(Collectors.joining("\n")));
			OUT.println();
		}
	}

}