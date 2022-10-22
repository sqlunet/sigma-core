package com.articulate.sigma;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.articulate.sigma.Utils.OUT;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestVarRename
{
	@Test
	public void replaceVars()
	{
		String[] forms = {"(forall (?X) (instance ?X Relation))", "(forall (?X) (instance ?X BiFunction))", "(forall (?X ?Y) (instance ?X ?Y))"};
		Map<String,String> topLevelVars = new HashMap<>(); //Map.of("?X", "?Y");
		Map<String,String> scopedRenames = new HashMap<>(); //Map.of("?X", "?Y");
		Map<String,String> allRenames = new HashMap<>(); //Map.of("?X", "?Y");
		for (String form : forms)
		{
			Formula f = Formula.of(form);
			Formula result = Variables.renameVariables(f, topLevelVars, scopedRenames, allRenames);
			OUT.println("Input: " + form + " formula=" + f);
			OUT.println("Result: " + result);
			OUT.println("topLevelVars: " + topLevelVars);
			OUT.println("scopedRenames: " + scopedRenames);
			OUT.println("allRenames: " + allRenames);
			OUT.println();
		}
	}
}
