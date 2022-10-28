package com.articulate.sigma;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.articulate.sigma.Utils.OUT;

@ExtendWith({SumoProvider.class})
public class TestRowVars
{
	private static final Formula[] fs = { //
			Formula.of("(=> (and (subrelation ?REL1 ?REL2) (holds__ ?REL1 @ROW)) (holds__ ?REL2 @ROW))"), //
			Formula.of("(=> (attribute @ROW) (property @ROW))"), //
			Formula.of("(=> (and (instance attribute Predicate) (instance property Predicate) (attribute @ROW1)) (property @ROW))"), //
			Formula.of("(=> (and (instance piece Predicate) (instance part Predicate) (piece @ROW)) (part @ROW))"), //
	};

	@BeforeAll
	public static void init()
	{
		SumoProvider.sumo.buildRelationCaches();
		SumoProvider.sumo.cacheRelationValences();
	}

	@Test
	public void expandRowVarsWithValenceProvider()
	{
		final Function<String, Integer> arityGetter = r -> {
			int valence;
			switch (r)
			{
				case "property":
				case "attribute":
				case "part":
				case "piece":
					valence = 2;
					break;
				case "holds__":
					valence = 0;
					break;
				default:
					valence = -1;
					break;
			}
			OUT.println(r + " has valence " + valence);
			return valence;
		};
		for (Formula f : fs)
		{
			List<Formula> rfs = RowVars.expandRowVars(f, arityGetter);
			OUT.println("formula=\n" + f.toFlatString());
			OUT.println("expanded=\n" + rfs.stream().map(Formula::toFlatString).collect(Collectors.joining("\n")));
			OUT.println();
		}
	}

	@Test
	public void expandRowVarsWithKb()
	{
		final Function<String, Integer> arityGetter = r -> {
			int valence = SumoProvider.sumo.getValence(r);
			OUT.println(r + " has valence " + valence);
			return valence;
		};
		for (Formula f : fs)
		{
			List<Formula> rfs = RowVars.expandRowVars(f, arityGetter);
			OUT.println("formula=\n" + f.toFlatString());
			OUT.println("expanded=\n" + rfs.stream().map(Formula::toFlatString).collect(Collectors.joining("\n")));
			OUT.println();
		}
	}
}
