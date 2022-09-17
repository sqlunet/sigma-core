package com.articulate.sigma;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
		this.negativeLits = new ArrayList<>();
		this.positiveLits = new ArrayList<>();
	}

	public Clause(final List<Formula> negativeLits, final List<Formula> positiveLits)
	{
		this.negativeLits = negativeLits;
		this.positiveLits = positiveLits;
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		if (negativeLits != null && !negativeLits.isEmpty())
		{
			for (Formula lit : negativeLits)
			{
				sb.append("\n- ").append(lit.toFlatString());
			}
		}
		if (positiveLits != null && !positiveLits.isEmpty())
		{
			for (Formula lit : positiveLits)
			{
				sb.append("\n+ ").append(lit.toFlatString());
			}
		}
		return sb.toString();
	}

	public static String cfToString(final Tuple.Triple<List<Clause>, Map<String, String>, Formula> cf)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("formula= ").append(cf.third).append('\n');
		sb.append("clauses= ").append(cf.first).append('\n');
		sb.append("map= ").append(cf.second).append('\n');
		return sb.toString();
	}
}
