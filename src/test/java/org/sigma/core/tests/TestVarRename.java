/*
 * Copyright (c) 2022.
 * This code is copyright Bernard Bou <1313ou@gmail.com>
 * This software is released under the GNU Public License 3 <http://www.gnu.org/copyleft/gpl.html>.
 */

package org.sigma.core.tests;

import org.sigma.core.Formula;
import org.sigma.core.NotNull;
import org.sigma.core.Variables;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.sigma.core.Helpers.OUT;

public class TestVarRename
{
	@Test
	public void replaceVars()
	{
		@NotNull String[] forms = {"(forall (?X) (instance ?X Relation))", "(forall (?X) (instance ?X BiFunction))", "(forall (?X ?Y) (instance ?X ?Y))"};
		@NotNull Map<String,String> topLevelVars = new HashMap<>(); //Map.of("?X", "?Y");
		@NotNull Map<String,String> scopedRenames = new HashMap<>(); //Map.of("?X", "?Y");
		@NotNull Map<String,String> allRenames = new HashMap<>(); //Map.of("?X", "?Y");
		for (@NotNull String form : forms)
		{
			@NotNull Formula f = Formula.of(form);
			@NotNull Formula result = Variables.renameVariables(f, topLevelVars, scopedRenames, allRenames);
			OUT.println("Input: " + form + " formula=" + f);
			OUT.println("Result: " + result);
			OUT.println("topLevelVars: " + topLevelVars);
			OUT.println("scopedRenames: " + scopedRenames);
			OUT.println("allRenames: " + allRenames);
			OUT.println();
		}
	}
}
