package com.articulate.sigma;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Clause.
 * Each clause is an pair containing an List of negative literals, and an List of positive literals.
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

	public Clause(List<Formula> negativeLits, List<Formula> positiveLits)
	{
		this.negativeLits = negativeLits;
		this.positiveLits = positiveLits;
	}
}
