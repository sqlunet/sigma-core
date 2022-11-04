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

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Basic interface that queries KB
 */
public interface KBQuery
{
	// F O R M U L A S

	/**
	 * Query for formulas with arg1 at pos1
	 *
	 * @param arg1 value of arg1
	 * @param pos1 position of arg1 in formula
	 * @return collection of formulas that satisfy all requirements
	 */
	@NotNull
	Collection<Formula> queryFormulas(@NotNull final String arg1, final int pos1);

	/**
	 * Query for formulas with arg1 at pos1 and arg2 at pos2
	 *
	 * @param arg1 value of arg1
	 * @param pos1 position of arg1 in formula
	 * @param arg2 value of arg2
	 * @param pos2 position of arg2 in formula
	 * @return collection of formulas that satisfy all requirements
	 */
	@NotNull
	Collection<Formula> queryFormulas(@NotNull final String arg1, final int pos1, @NotNull final String arg2, final int pos2);

	/**
	 * Query for formulas with arg1 at pos1 and arg2 at pos2 and arg3 at pos3
	 *
	 * @param arg1 value of arg1
	 * @param pos1 position of arg1 in formula
	 * @param arg2 value of arg2
	 * @param pos2 position of arg2 in formula
	 * @param arg3 value of arg3
	 * @param pos3 position of arg3 in formula
	 * @return collection of formulas that satisfy all requirements
	 */
	@NotNull
	Collection<Formula> queryFormulas(@NotNull final String arg1, final int pos1, @NotNull final String arg2, final int pos2, @NotNull final String arg3, final int pos3);

	// A R G S

	/**
	 * Collect arguments at targetArgPos in formulas with arg1 at pos1
	 *
	 * @param arg1         value of arg1
	 * @param pos1         position of arg1 in formula
	 * @param targetArgPos position of arguments to collect
	 * @return collection of targetArgPos at targetArgPos from formulas that satisfy all requirements
	 */
	@NotNull
	default Collection<String> queryArgs(@NotNull final String arg1, final int pos1, final int targetArgPos)
	{
		return queryFormulas(arg1, pos1).stream().map(Formula::elements).filter(e -> e.size() > targetArgPos).map(e -> e.get(targetArgPos)).collect(Collectors.toUnmodifiableSet());
	}

	/**
	 * Collect arguments at targetArgPos in formulas with arg1 at pos1 and arg2 at pos2
	 *
	 * @param arg1         value of arg1
	 * @param pos1         position of arg1 in formula
	 * @param arg2         value of arg2
	 * @param pos2         position of arg2 in formula
	 * @param targetArgPos position of arguments to collect
	 * @return collection of targetArgPos at targetArgPos from formulas that satisfy all requirements
	 */
	@NotNull
	default Collection<String> queryArgs(@NotNull final String arg1, final int pos1, @NotNull final String arg2, final int pos2, final int targetArgPos)
	{
		return queryFormulas(arg1, pos1, arg2, pos2).stream().map(Formula::elements).filter(e -> e.size() > targetArgPos).map(e -> e.get(targetArgPos)).collect(Collectors.toUnmodifiableSet());
	}

	/**
	 * Collect arguments at targetArgPos in formulas with arg1 at pos1 and arg2 at pos2 and arg3 at pos3
	 *
	 * @param arg1         value of arg1
	 * @param pos1         position of arg1 in formula
	 * @param arg2         value of arg2
	 * @param pos2         position of arg2 in formula
	 * @param arg3         value of arg3
	 * @param pos3         position of arg3 in formula
	 * @param targetArgPos position of arguments to collect
	 * @return collection of targetArgPos at targetArgPos from formulas that satisfy all requirements
	 */
	@NotNull
	default Collection<String> queryArgs(@NotNull final String arg1, final int pos1, final int pos2, @NotNull final String arg2, @NotNull final String arg3, final int pos3, final int targetArgPos)
	{
		return queryFormulas(arg1, pos1, arg2, pos2, arg3, pos3).stream().map(Formula::elements).filter(e -> e.size() > targetArgPos).map(e -> e.get(targetArgPos)).collect(Collectors.toUnmodifiableSet());
	}

	// R E L A T I O N S

	/**
	 * Collect arguments at targetArgPos in formulas with reln at position 0 and arg at pos
	 *
	 * @param reln         relation at position 0 in formula
	 * @param targetArgPos position of arguments to collect
	 * @return collection of targetArgPos at targetArgPos from formulas that satisfy all requirements
	 */
	@NotNull
	default Collection<String> query(@NotNull final String reln, final int targetArgPos)
	{
		return queryArgs(reln, 0, targetArgPos);
	}

	/**
	 * Collect arguments at targetArgPos in formulas with reln at position 0 and arg at pos
	 *
	 * @param reln         relation at position 0 in formula
	 * @param arg          value of arg
	 * @param pos          position of arg in formula
	 * @param targetArgPos position of arguments to collect
	 * @return collection of targetArgPos at targetArgPos from formulas that satisfy all requirements
	 */
	@NotNull
	default Collection<String> query(@NotNull final String reln, @NotNull final String arg, final int pos, final int targetArgPos)
	{
		return queryArgs(reln, 0, arg, pos, targetArgPos);
	}
}
