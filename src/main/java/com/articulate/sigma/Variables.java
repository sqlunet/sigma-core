package com.articulate.sigma;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

public class Variables
{
	// This static variable holds the int value that is used to generate unique variable names.
	private static int VAR_INDEX = 0;
	// This static variable holds the int value that is used to generate unique Skolem terms.
	private static int SKOLEM_INDEX = 0;

	/**
	 * Returns a String in which all variables and row variables have
	 * been normalized -- renamed, in depth-first order of occurrence,
	 * starting from index 1 -- to support comparison of Formulae for
	 * equality.
	 *
	 * @param input A String representing a SUO-KIF Formula, possibly
	 *              containing variables to be normalized
	 * @return A String, typically representing a SUO-KIF Formula or
	 * part of a Formula, in which the original variables have been
	 * replaced by normalized forms
	 */
	@NotNull
	public static String normalizeVariables(@NotNull String input)
	{
		return normalizeVariables(input, false);
	}

	/**
	 * Returns a String in which all variables and row variables have
	 * been normalized -- renamed, in depth-first order of occurrence,
	 * starting from index 1 -- to support comparison of Formulae for
	 * equality.
	 *
	 * @param input              A String representing a SUO-KIF Formula, possibly
	 *                           containing variables to be normalized
	 * @param replaceSkolemTerms If true, all Skolem terms in input
	 *                           are treated as variables and are replaced with normalized
	 *                           variable terms
	 * @return A String, typically representing a SUO-KIF Formula or
	 * part of a Formula, in which the original variables have been
	 * replaced by normalized forms
	 */
	@NotNull
	protected static String normalizeVariables(@NotNull String input, @SuppressWarnings("SameParameterValue") boolean replaceSkolemTerms)
	{
		String result = input;
		try
		{
			int[] idxs = { 1, 1 };
			Map<String, String> varMap = new HashMap<>();
			result = normalizeVariables_1(input, idxs, varMap, replaceSkolemTerms);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		return result;
	}

	/**
	 * An internal helper method for normalizeVariables(String input).
	 *
	 * @param input              A String possibly containing variables to be
	 *                           normalized
	 * @param idxs               A two-place int[] in which int[0] is the current
	 *                           variable index, and int[1] is the current row variable index
	 * @param varMap             A Map in which the keys are old variables and the
	 *                           values are new variables
	 * @param replaceSkolemTerms If true, all Skolem terms in input
	 *                           are treated as variables and are replaced with normalized
	 *                           variable terms
	 * @return A String, typically a representing a SUO-KIF Formula or part of a Formula.
	 */
	@NotNull
	protected static String normalizeVariables_1(@NotNull String input, int[] idxs, @NotNull Map<String, String> varMap, boolean replaceSkolemTerms)
	{
		String result = "";
		try
		{
			String vBase = Formula.VVAR;
			String rvBase = (Formula.RVAR + "VAR");
			StringBuilder sb = new StringBuilder();
			String fList = input.trim();
			boolean isSkolem = Formula.isSkolemTerm(fList);
			if ((replaceSkolemTerms && isSkolem) || Formula.isVariable(fList))
			{
				String newVar = varMap.get(fList);
				if (newVar == null)
				{
					newVar = ((fList.startsWith(Formula.V_PREF) || isSkolem) ? (vBase + idxs[0]++) : (rvBase + idxs[1]++));
					varMap.put(fList, newVar);
				}
				sb.append(newVar);
			}
			else if (Formula.listP(fList))
			{
				if (Formula.empty(fList))
					sb.append(fList);
				else
				{
					Formula f = new Formula(fList);
					List<String> tuple = f.literalToList();
					sb.append(Formula.LP);
					int i = 0;
					for (String s : tuple)
					{
						if (i > 0)
							sb.append(Formula.SPACE);
						sb.append(normalizeVariables_1(s, idxs, varMap, replaceSkolemTerms));
						i++;
					}
					sb.append(Formula.RP);
				}
			}
			else
				sb.append(fList);
			result = sb.toString();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		return result;
	}

	/**
	 * This method increments VAR_INDEX and then returns the new int
	 * value.  If VAR_INDEX is already at Integer.MAX_VALUE, then
	 * VAR_INDEX is reset to 0.
	 *
	 * @return An int value between 0 and Integer.MAX_VALUE inclusive.
	 */
	private static int incVarIndex()
	{
		int oldVal = VAR_INDEX;
		if (oldVal == Integer.MAX_VALUE)
		{
			VAR_INDEX = 0;
		}
		else
		{
			++VAR_INDEX;
		}
		return VAR_INDEX;
	}

	/**
	 * This method increments SKOLEM_INDEX and then returns the new int
	 * value.  If SKOLEM_INDEX is already at Integer.MAX_VALUE, then
	 * SKOLEM_INDEX is reset to 0.
	 *
	 * @return An int value between 0 and Integer.MAX_VALUE inclusive.
	 */
	private static int incSkolemIndex()
	{
		int oldVal = SKOLEM_INDEX;
		if (oldVal == Integer.MAX_VALUE)
		{
			SKOLEM_INDEX = 0;
		}
		else
		{
			++SKOLEM_INDEX;
		}
		return SKOLEM_INDEX;
	}

	/**
	 * This method returns a new SUO-KIF variable String, modifying
	 * any digit suffix to ensure that the variable will be unique.
	 *
	 * @param prefix An optional variable prefix string.
	 * @return A new SUO-KIF variable.
	 */
	@NotNull
	private static String newVar(@Nullable String prefix)
	{
		String base = Formula.VX;
		String varIdx = Integer.toString(incVarIndex());
		if (isNonEmpty(prefix))
		{
			List<String> woDigitSuffix = KB.getMatches(prefix, "var_with_digit_suffix");
			if (woDigitSuffix != null)
			{
				base = woDigitSuffix.get(0);
			}
			else if (prefix.startsWith(Formula.RVAR))
			{
				base = Formula.RVAR;
			}
			else if (prefix.startsWith(Formula.VX))
			{
				base = Formula.VX;
			}
			else
			{
				base = prefix;
			}
			if (!(base.startsWith(Formula.V_PREF) || base.startsWith(Formula.R_PREF)))
			{
				base = (Formula.V_PREF + base);
			}
		}
		return (base + varIdx);
	}

	/**
	 * This method returns a new SUO-KIF variable String, adding a
	 * digit suffix to ensure that the variable will be unique.
	 *
	 * @return A new SUO-KIF variable
	 */
	@NotNull
	static String newVar()
	{
		return newVar(null);
	}

	/**
	 * This method returns a new, unique skolem term with each
	 * invocation.
	 *
	 * @param vars A sorted SortedSet of the universally quantified
	 *             variables that potentially define the skolem term.  The set may
	 *             be empty.
	 * @return A String.  The string will be a skolem functional term
	 * (a list) if vars contains variables.  Otherwise, it will be an
	 * atomic constant.
	 */
	@NotNull
	static String newSkolemTerm(@Nullable SortedSet<String> vars)
	{
		StringBuilder sb = new StringBuilder(Formula.SK_PREF);
		int idx = incSkolemIndex();
		if ((vars != null) && !vars.isEmpty())
		{
			sb.append(Formula.FN_SUFF + Formula.SPACE).append(idx);
			for (String var : vars)
			{
				sb.append(Formula.SPACE).append(var);
			}
			sb = new StringBuilder((Formula.LP + sb + Formula.RP));
		}
		else
		{
			sb.append(idx);
		}
		return sb.toString();
	}

	static boolean isNonEmpty(@Nullable String str)
	{
		return str != null && !str.isEmpty();
	}
}
