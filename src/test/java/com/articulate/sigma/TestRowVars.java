package com.articulate.sigma;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Set;

import static com.articulate.sigma.Utils.OUT;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith({SumoLoader.class})
public class TestRowVars
{
	private static final Formula f = Formula.of("(=> (and (subrelation ?REL1 ?REL2) (holds__ ?REL1 @ROW)) (holds__ ?REL2 @ROW))");

	@Test
	public void expandRowVars()
	{
		List<Formula> expanded = RowVars.expandRowVars(f, r -> "subrelation".equals(r) ? 5 : 8);
		OUT.println("Input: " + f);
		OUT.println("Enpansions: " + expanded);

		//assertEquals(Set.of("MultiplicationFn", "agent", "instance", "holdsDuring", "Obligation", "during", "Muslim", "0.025", "and", "Zakat", "patient", "attribute", "WhenFn", "greaterThan", "=>", "FullyFormed", "?C", "?H", "monetaryValue", "modalAttribute", "equal", "Year", "?T", "?W", "exists", "?Y", "?Z", "WealthFn"), terms);
	}
}
