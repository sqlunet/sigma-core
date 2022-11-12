/*
 * Copyright (c) 2022.
 * This code is copyright Articulate Software (c) 2003.  Some portions copyright Teknowledge (c) 2003
 * and reused under the terms of the GNU license.
 * Significant portions of the code have been revised, trimmed, revamped,enhanced by
 * Bernard Bou <1313ou@gmail.com>
 * This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
 * Users of this code also consent, by use of this code, to credit Articulate Software and Teknowledge
 * in any writings, briefings, publications, presentations, or other representations of any software
 * which incorporates, builds on, or uses this code.
 */

package org.sigma.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Clause.
 * Each clause is a pair containing a List of negative literals, and a List of positive literals.
 * Either the neg lits list or the pos lits list could be empty. Each literal is a Formula object.
 */
public class Clause implements Serializable
{
	private static final long serialVersionUID = -7497457829015464477L;

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

	@Override
	public boolean equals(@Nullable final Object o)
	{
		if (this == o)
		{
			return true;
		}
		if (o == null || getClass() != o.getClass())
		{
			return false;
		}
		@NotNull Clause that = (Clause) o;
		return Objects.equals(negativeLits, that.negativeLits) && Objects.equals(positiveLits, that.positiveLits);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(negativeLits, positiveLits);
	}

	@NotNull
	@Override
	public String toString()
	{
		@NotNull Stream<String> negatives = negativeLits == null ? Stream.empty() : negativeLits.stream().map(f -> '-' + f.form);
		@NotNull Stream<String> positives = positiveLits == null ? Stream.empty() : positiveLits.stream().map(f -> '+' + f.form);
		return Stream.concat(negatives, positives).collect(Collectors.joining(", "));
	}
}
