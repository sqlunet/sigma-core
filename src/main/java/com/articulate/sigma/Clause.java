package com.articulate.sigma;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Clause.
 * Each clause is a pair containing a List of negative literals, and a List of positive literals.
 * Either the neg lits list or the pos lits list could be empty. Each literal is a Formula object.
 */
public class Clause implements Serializable
{
	private static final long serialVersionUID = -7497457829015464476L;

	public final List<Formula> negativeLits;

	public final List<Formula> positiveLits;

	public Clause()
	{
		negativeLits = new ArrayList<>();
		positiveLits = new ArrayList<>();
	}

	public Clause(final List<Formula> negativeLits, final List<Formula> positiveLits)
	{
		this.negativeLits = negativeLits;
		this.positiveLits = positiveLits;
	}

	@NotNull
	@Override
	public String toString()
	{
		Stream<String> negatives = negativeLits == null ? Stream.empty() : negativeLits.stream().map(f -> '-' + f.form);
		Stream<String> positives = positiveLits == null ? Stream.empty() : positiveLits.stream().map(f -> '+' + f.form);
		return Stream.concat(negatives, positives).collect(Collectors.joining("\n"));
	}

	@NotNull
	public static String cfToString(@NotNull final Tuple.Triple<List<Clause>, Map<String, String>, Formula> cf)
	{
		return "formula= " + cf.third + '\n' + "clauses=" + cf.first + '\n' + "map= " + cf.second + '\n';
	}
}
