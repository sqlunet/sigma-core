package com.articulate.sigma;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.function.Function;

import static com.articulate.sigma.Utils.OUT;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith({SumoProvider.class})
public class TestRowVars
{
	private static final Formula[] fs = { //
			Formula.of("(=> (and (subrelation ?REL1 ?REL2) (holds__ ?REL1 @ROW)) (holds__ ?REL2 @ROW))"), //
			Formula.of("(=> (attribute @ROW) (property @ROW))"), //
			Formula.of("(=> (and (instance attribute Predicate) (instance property Predicate) (attribute @ROW1)) (property @ROW))"), //
			Formula.of("(=> (and (instance piece Predicate) (instance part Predicate) (piece @ROW)) (part @ROW))"), //
	};

	@Test
	public void expandRowVars()
	{
		final Function<String, Integer> arityGetter = r -> {
			System.err.println(r);
			switch (r)
			{
				case "property":
				case "attribute":
				case "part":
				case "piece":
					return 2;
				case "holds__":
					return 1;
				default:
					return 1;
			}
		};
		for (Formula f : fs)
		{
			List<Formula> expanded = RowVars.expandRowVars(f, arityGetter);
			OUT.println("formula=" + f);
			OUT.println("expanded=" + expanded);
			OUT.println();
		}
	}

	@Test
	public void expandRowVarsWithKb()
	{
		SumoProvider.sumo.buildRelationCaches();
		SumoProvider.sumo.cacheRelationValences();
		final Function<String, Integer> arityGetter = r -> {
			int v = SumoProvider.sumo.getValence(r);
			System.err.println(r + " " + v);
			return v;
		};
		for (Formula f : fs)
		{
			List<Formula> expanded = RowVars.expandRowVars(f, arityGetter);
			OUT.println("formula=" + f);
			OUT.println("expanded=" + expanded);
			OUT.println();
		}
	}
}
