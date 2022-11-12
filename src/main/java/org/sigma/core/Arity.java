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
import java.util.HashSet;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Arity related static methods
 */
public class Arity
{
	/**
	 * This constant indicates the maximum predicate arity supported
	 * by the current implementation of Sigma.
	 */
	public static final int MAX_PREDICATE_ARITY = 7;

	/**
	 * Operator arity
	 *
	 * @param op operator
	 * @return the integer arity of the given logical operator
	 */
	public static int operatorArity(@NotNull final String op)
	{
		// compute op index
		//                       0               1               2            3            4           5           6
		@NotNull String[] ops = {Formula.UQUANT, Formula.EQUANT, Formula.NOT, Formula.AND, Formula.OR, Formula.IF, Formula.IFF};
		int idx = 0;
		while (idx < ops.length && !op.equals(ops[idx]))
		{
			idx++;
		}

		if (idx <= 2)
		{
			// Formula.UQUANT, Formula.EQUANT, Formula.NOT
			return 1;
		}
		else
		{
			// Formula.AND, Formula.OR, Formula.IF, Formula.IFF
			if (idx < ops.length)
			{
				return 2;
			}
			else
			{
				return -1;
			}
		}
	}

	public static boolean hasCorrectArity(final String form, @NotNull final Function<String, Integer> arityGetter)
	{
		try
		{
			hasCorrectArityThrows(form, arityGetter);
			return true;
		}
		catch (ArityException ae)
		{
			return false;
		}
	}

	public static void hasCorrectArityThrows(final String form, @NotNull final Function<String, Integer> arityGetter) throws ArityException
	{
		String form2 = form;

		// remove quantifier and variable list
		form2 = form2.replaceAll(Formula.EQUANT + "\\s+(\\([^(]+?\\))", "");
		form2 = form2.replaceAll(Formula.UQUANT + "\\s+(\\([^(]+?\\))", "");

		// replace strings with dummy ?MATCH
		form2 = form2.replaceAll("\".*?\"", "?MATCH");

		// catch non empty lists
		@NotNull Pattern p = Pattern.compile("(\\([^(]+?\\))"); // +? is one or more times but as few as possible
		@NotNull Matcher m = p.matcher(form2);
		while (m.find())
		{
			// list
			String subform = m.group(1);
			if (subform.length() > 2) // else empty list ()
			{
				subform = subform.substring(1, subform.length() - 1); // strip parentheses
			}
			@NotNull String[] split = subform.split("\\s+");
			if (split.length > 1) // has arguments
			{
				// relation
				String reln = split[0];
				if (!reln.startsWith(Formula.V_PREFIX))
				{
					int arity;
					if (Formula.IF.equals(reln) || Formula.IFF.equals(reln))
					{
						arity = 2;
					}
					else
					{
						arity = arityGetter.apply(reln);
					}

					// disregard statements using the @ROW variable as it
					// will more often than not resolve to a wrong arity
					boolean hasRowVars = false;
					for (int i = 1; i < split.length; i++)
					{
						if (split[i].startsWith(Formula.R_PREFIX))
						{
							hasRowVars = true;
							break;
						}
					}
					if (!hasRowVars)
					{
						int foundArity = split.length - 1;
						if (arity >= 1 && foundArity != arity)
						{
							throw new ArityException(reln, arity, foundArity);
						}
					}
				}
			}

			// replace visited list with dummy
			form2 = form2.replace(Formula.LP + subform + Formula.RP, "?MATCH");
			m = p.matcher(form2);
		}
	}

	/**
	 * Test if this Formula contains any variable arity relations
	 *
	 * @param form a Formula
	 * @param kb   - The KB used to compute variable arity relations.
	 * @return Returns true if this Formula contains any variable
	 * arity relations, else returns false.
	 */
	public static boolean containsVariableArityRelation(@NotNull final String form, @NotNull final KB kb)
	{
		@NotNull Collection<String> variableArityRelns = new HashSet<>();
		variableArityRelns.addAll(KB.VA_RELNS);
		variableArityRelns.addAll(kb.getCachedRelationValues("instance", "VariableArityRelation", 2, 1));

		for (@NotNull String reln : variableArityRelns)
		{
			if (form.contains(reln))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Arity exception that records the found and expected arities for a relation
	 */
	public static class ArityException extends Exception
	{
		private static final long serialVersionUID = 5770027459770147573L;

		final String rel;

		final int expectedArity;

		final int foundArity;

		public ArityException(final String rel, final int expectedArity, final int foundArity)
		{
			this.rel = rel;
			this.expectedArity = expectedArity;
			this.foundArity = foundArity;
		}

		@NotNull
		@Override
		public String toString()
		{
			return "ArityException{" + "rel='" + rel + '\'' + ", expected=" + expectedArity + ", found=" + foundArity + '}';
		}
	}
}
