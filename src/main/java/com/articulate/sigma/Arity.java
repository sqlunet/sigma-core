package com.articulate.sigma;

import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
		@NotNull String[] ops = {Formula.UQUANT, Formula.EQUANT, Formula.NOT, Formula.AND, Formula.OR, Formula.IF, Formula.IFF};

		int idx = 0;
		while (idx < ops.length && !op.equals(ops[idx]))
		{
			idx++;
		}

		if (idx <= 2)
		{
			return 1;
		}
		else
		{
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
		}
		catch (ArityException ae)
		{
			return false;
		}
		return true;
	}

	public static void hasCorrectArityThrows(final String form0, @NotNull final Function<String, Integer> arityGetter) throws ArityException
	{
		String form = form0;
		form = form.replaceAll(Formula.EQUANT + "\\s+(\\([^(]+?\\))", "");
		form = form.replaceAll(Formula.UQUANT + "\\s+(\\([^(]+?\\))", "");
		form = form.replaceAll("\".*?\"", "?MATCH");

		@NotNull Pattern p = Pattern.compile("(\\([^(]+?\\))");
		@NotNull Matcher m = p.matcher(form);
		while (m.find())
		{
			String subform = m.group(1);
			if (subform.length() > 2)
			{
				subform = subform.substring(1, subform.length() - 1);
			}
			@NotNull String[] split = subform.split(" ");
			if (split.length > 1)
			{
				String reln = split[0];
				if (!reln.startsWith(Formula.V_PREF))
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

					boolean startsWith = false;
					// disregard statements using the @ROW variable as it
					// will more often than not resolve to a wrong arity
					for (int i = 1; i < split.length; i++)
					{
						if (split[i].startsWith(Formula.R_PREF))
						{
							startsWith = true;
							break;
						}
					}
					if (!startsWith)
					{
						int foundArity = split.length - 1;
						if (arity >= 1 && foundArity != arity)
						{
							throw new ArityException(reln, arity, foundArity);
						}
					}
				}
			}
			form = form.replace(Formula.LP + subform + Formula.RP, "?MATCH");
			m = p.matcher(form);
		}
	}

	/**
	 * Test if this Formula contains any variable arity relations
	 *
	 * @param f0 a Formula
	 * @param kb - The KB used to compute variable arity relations.
	 * @return Returns true if this Formula contains any variable
	 * arity relations, else returns false.
	 */
	public static boolean containsVariableArityRelation(@NotNull final Formula f0, @NotNull final KB kb)
	{
		@NotNull Set<String> relns = kb.getCachedRelationValues("instance", "VariableArityRelation", 2, 1);
		relns.addAll(KB.VA_RELNS);

		boolean result = false;
		for (@NotNull String reln : relns)
		{
			result = f0.form.contains(reln);
			if (result)
			{
				break;
			}
		}
		return result;
	}

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
