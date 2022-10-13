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
		@NotNull String result = input;
		try
		{
			@NotNull int[] idxs = {1, 1};
			@NotNull Map<String, String> varMap = new HashMap<>();
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
	protected static String normalizeVariables_1(@NotNull String input, int[] idxs, @NotNull final Map<String, String> varMap, final boolean replaceSkolemTerms)
	{
		@NotNull String result = "";
		try
		{
			@NotNull String vBase = Formula.VVAR;
			@NotNull String rvBase = (Formula.RVAR + "VAR");
			@NotNull StringBuilder sb = new StringBuilder();
			@NotNull String input2 = input.trim();

			boolean isSkolem = Formula.isSkolemTerm(input2);
			if ((replaceSkolemTerms && isSkolem) || Formula.isVariable(input2))
			{
				String newVar = varMap.get(input2);
				if (newVar == null)
				{
					newVar = ((input2.startsWith(Formula.V_PREF) || isSkolem) ? (vBase + idxs[0]++) : (rvBase + idxs[1]++));
					varMap.put(input2, newVar);
				}
				sb.append(newVar);
			}
			else if (Lisp.listP(input2))
			{
				if (Lisp.empty(input2))
				{
					sb.append(input2);
				}
				else
				{
					@NotNull Formula f = Formula.of(input2);
					@NotNull List<String> tuple = f.elements();
					sb.append(Formula.LP);
					int i = 0;
					for (@NotNull String s : tuple)
					{
						if (i > 0)
						{
							sb.append(Formula.SPACE);
						}
						sb.append(normalizeVariables_1(s, idxs, varMap, replaceSkolemTerms));
						i++;
					}
					sb.append(Formula.RP);
				}
			}
			else
			{
				sb.append(input2);
			}
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
	private static String newVar(@SuppressWarnings("SameParameterValue") @Nullable String prefix)
	{
		String base = Formula.VX;
		@NotNull String varIdx = Integer.toString(incVarIndex());
		if (isNonEmpty(prefix))
		{
			@Nullable List<String> woDigitSuffix = KB.getMatches(prefix, "var_with_digit_suffix");
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
		@NotNull StringBuilder sb = new StringBuilder(Formula.SK_PREF);
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

	/**
	 * This method finds the original variable that corresponds to a new
	 * variable.  Note that the clausification algorithm has two variable
	 * renaming steps, and that after variables are standardized apart an
	 * original variable might correspond to multiple clause variables.
	 *
	 * @param var    A SUO-KIF variable (String)
	 * @param varMap A Map (graph) of successive new to old variable
	 *               correspondences.
	 * @return The original SUO-KIF variable corresponding to the input.
	 **/
	@Nullable
	public static String getOriginalVar(String var, @Nullable Map<String, String> varMap)
	{
		@Nullable String result = null;
		if (isNonEmpty(var) && (varMap != null))
		{
			result = var;
			for (String val = varMap.get(result); val != null && !val.equals(result); val = varMap.get(result))
			{
				result = val;
			}
		}
		return result;
	}

	/**
	 * This method returns a new Formula in which all variables have
	 * been renamed to ensure uniqueness.
	 *
	 * @param f             a Formula.
	 * @param topLevelVars  A Map that is used to track renames of implicitly universally quantified variables.
	 * @param scopedRenames A Map that is used to track renames of explicitly quantified variables.
	 * @param allRenames    A Map from all new vars in the Formula to their old counterparts.
	 * @return A new SUO-KIF Formula with all variables renamed.
	 */
	@NotNull
	public static Formula renameVariables(@NotNull final Formula f, @NotNull Map<String, String> topLevelVars, @NotNull Map<String, String> scopedRenames, @NotNull Map<String, String> allRenames)
	{
		try
		{
			if (f.listP())
			{
				if (f.empty())
				{
					return f;
				}
				@NotNull String arg0 = f.car();
				if (Formula.isQuantifier(arg0))
				{
					// Copy the scopedRenames map to protect variable scope as we descend below this quantifier.
					@NotNull Map<String, String> newScopedRenames = new HashMap<>(scopedRenames);

					@NotNull StringBuilder newVars = new StringBuilder();
					@Nullable Formula oldVarsF = Formula.of(f.cadr());
					for (@Nullable Formula itF = oldVarsF; itF != null && !itF.empty(); itF = itF.cdrAsFormula())
					{
						@NotNull String oldVar = itF.car();
						@NotNull String newVar = newVar();
						newScopedRenames.put(oldVar, newVar);
						allRenames.put(newVar, oldVar);
						newVars.append(Formula.SPACE).append(newVar);
					}
					newVars = new StringBuilder((Formula.LP + newVars.toString().trim() + Formula.RP));

					@NotNull Formula arg2F = Formula.of(f.caddr());
					@NotNull String newArg2 = renameVariables(arg2F, topLevelVars, newScopedRenames, allRenames).form;
					@NotNull String newForm = Formula.LP + arg0 + Formula.SPACE + newVars + Formula.SPACE + newArg2 + Formula.RP;
					return Formula.of(newForm);
				}
				@NotNull Formula arg0F = Formula.of(arg0);
				@NotNull String newArg0 = renameVariables(arg0F, topLevelVars, scopedRenames, allRenames).form;

				@NotNull String newRest = renameVariables(f.cdrOfListAsFormula(), topLevelVars, scopedRenames, allRenames).form;
				@NotNull Formula newRestF = Formula.of(newRest);

				@NotNull String newForm = newRestF.cons(newArg0).form;
				return Formula.of(newForm);
			}
			if (Formula.isVariable(f.form))
			{
				String rnv = scopedRenames.get(f.form);
				if (!isNonEmpty(rnv))
				{
					rnv = topLevelVars.get(f.form);
					if (!isNonEmpty(rnv))
					{
						rnv = newVar();
						topLevelVars.put(f.form, rnv);
						allRenames.put(rnv, f.form);
					}
				}
				return Formula.of(rnv);
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		return f;
	}

	/**
	 * Returns the number of SUO-KIF variables (only ? variables, not
	 * variables) in the input query literal.
	 *
	 * @param queryLiteral A List representing a Formula.
	 * @return An int.
	 */
	public static int getVarCount(@Nullable List<String> queryLiteral)
	{
		int result = 0;
		if (queryLiteral != null)
		{
			for (@NotNull String term : queryLiteral)
			{
				if (term.startsWith("?"))
				{
					result++;
				}
			}
		}
		return result;
	}
}
