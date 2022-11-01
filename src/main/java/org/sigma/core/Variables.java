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

import java.util.*;
import java.util.AbstractMap.SimpleEntry;
import java.util.stream.Collectors;

public class Variables
{
	// This static variable holds the int value that is used to generate unique variable names.
	private static int VAR_INDEX = 0;
	// This static variable holds the int value that is used to generate unique Skolem terms.
	private static int SKOLEM_INDEX = 0;

	// N O R M A L I Z E

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
	public static String normalizeVariables(@NotNull final String input)
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
	public static String normalizeVariables(@NotNull final String input, @SuppressWarnings("SameParameterValue") final boolean replaceSkolemTerms)
	{
		@NotNull int[] idxs = {1, 1};
		@NotNull Map<String, String> varMap = new HashMap<>();
		return normalizeVariablesRecurse(input, idxs, varMap, replaceSkolemTerms);
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
	public static String normalizeVariablesRecurse(@NotNull final String input, final int[] idxs, @NotNull final Map<String, String> varMap, final boolean replaceSkolemTerms)
	{
		@NotNull String result;

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
				newVar = ((input2.startsWith(Formula.V_PREFIX) || isSkolem) ? (vBase + idxs[0]++) : (rvBase + idxs[1]++));
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
				@NotNull List<String> tuple = Formula.elements(input2);
				sb.append(Formula.LP);
				int i = 0;
				for (@NotNull String s : tuple)
				{
					if (i > 0)
					{
						sb.append(Formula.SPACE);
					}
					sb.append(normalizeVariablesRecurse(s, idxs, varMap, replaceSkolemTerms));
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
		return result;
	}

	// R E N A M E

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
	public static Formula renameVariables(@NotNull final Formula f, @NotNull final Map<String, String> topLevelVars, @NotNull final Map<String, String> scopedRenames, @Nullable final Map<String, String> allRenames)
	{
		return Formula.of(renameVariables(f.form, topLevelVars, scopedRenames, allRenames));
	}

	/**
	 * This method returns a new Formula in which all variables have
	 * been renamed to ensure uniqueness.
	 *
	 * @param form          a formula string.
	 * @param topLevelVars  A Map that is used to track renames of implicitly universally quantified variables.
	 * @param scopedRenames A Map that is used to track renames of explicitly quantified variables.
	 * @param allRenames    A Map from all new vars in the Formula to their old counterparts.
	 * @return A new SUO-KIF Formula with all variables renamed.
	 */
	@NotNull
	public static String renameVariables(@NotNull final String form, @NotNull final Map<String, String> topLevelVars, @NotNull final Map<String, String> scopedRenames, @Nullable final Map<String, String> allRenames)
	{
		if (Lisp.listP(form))
		{
			if (Lisp.empty(form))
			{
				return form;
			}
			@NotNull String head = Lisp.car(form);
			if (Formula.isQuantifier(head))
			{
				// Copy the scopedRenames map to protect variable scope as we descend below this quantifier.
				@NotNull Map<String, String> newScopedRenames = new HashMap<>(scopedRenames);

				@NotNull StringBuilder newVars = new StringBuilder();
				for (@Nullable IterableFormula itF = new IterableFormula(Lisp.cadr(form)); !itF.empty(); itF.pop())
				{
					@NotNull String oldVar = itF.car();
					@NotNull String newVar = newVar();
					newScopedRenames.put(oldVar, newVar);
					if (allRenames != null)
					{
						allRenames.put(newVar, oldVar);
					}
					newVars.append(Formula.SPACE).append(newVar);
				}
				newVars = new StringBuilder((Formula.LP + newVars.toString().trim() + Formula.RP));
				return Formula.LP + head + Formula.SPACE + newVars + Formula.SPACE + renameVariables(Lisp.caddr(form), topLevelVars, newScopedRenames, allRenames) + Formula.RP;
			}
			return Lisp.cons(renameVariables(Lisp.cdr(form), topLevelVars, scopedRenames, allRenames), renameVariables(head, topLevelVars, scopedRenames, allRenames));
		}
		if (Formula.isVariable(form))
		{
			// scoped
			String renamedVar = scopedRenames.get(form);
			if (renamedVar == null || renamedVar.isEmpty())
			{
				// top
				renamedVar = topLevelVars.get(form);
				if (renamedVar == null || renamedVar.isEmpty())
				{
					renamedVar = newVar();
					topLevelVars.put(form, renamedVar);
					if (allRenames != null)
					{
						allRenames.put(renamedVar, form);
					}
				}
			}
			return renamedVar;
		}
		return form;
	}

	/**
	 * This method returns a new Formula in which all variables have
	 * been renamed to ensure uniqueness.
	 *
	 * @param f             a Formula.
	 * @param topLevelVars  A Map that is used to track renames of implicitly universally quantified variables.
	 * @return A new SUO-KIF Formula with all variables renamed.
	 */
	@NotNull
	public static Formula renameVariables(@NotNull final Formula f, @NotNull final Map<String, String> topLevelVars)
	{
		return Formula.of(renameVariables(f.form, topLevelVars));
	}

	/**
	 * This method returns a new Formula in which all variables have
	 * been renamed to ensure uniqueness.
	 *
	 * @param form          a formula string.
	 * @param topLevelVars  A Map that is used to track renames of implicitly universally quantified variables.
	 * @return A new SUO-KIF Formula with all variables renamed.
	 */
	@NotNull
	public static String renameVariables(@NotNull final String form, @NotNull final Map<String, String> topLevelVars)
	{
		@NotNull final Map<String, String> scopedRenames = new HashMap<>();
		return renameVariables(form, topLevelVars, scopedRenames, null);
	}

	// N E W   V A R

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
	 * This method returns a new SUO-KIF variable String, modifying
	 * any digit suffix to ensure that the variable will be unique.
	 *
	 * @param prefix An optional variable prefix string.
	 * @return A new SUO-KIF variable.
	 */
	@NotNull
	private static String newVar(@SuppressWarnings("SameParameterValue") @Nullable final String prefix)
	{
		String base = Formula.VX;
		@NotNull String varIdx = Integer.toString(incVarIndex());
		if (prefix != null && !prefix.isEmpty())
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
			if (!(base.startsWith(Formula.V_PREFIX) || base.startsWith(Formula.R_PREFIX)))
			{
				base = (Formula.V_PREFIX + base);
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

	// N E W   S K O L E M   T E R M

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
	static String newSkolemTerm(@Nullable final Set<String> vars)
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

	// V A R   M A P S

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
	@NotNull
	public static String getOriginalVar(@NotNull final String var, @Nullable final Map<String, String> varMap)
	{
		@NotNull String result = var;
		if (!var.isEmpty() && varMap != null)
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
	 * This method maps variables to the original variables.
	 * @param varMap A Map (graph) of successive new to old variable
	 *               correspondences.
	 * @return The map of vars to original SUO-KIF variable corresponding to the input.
	 **/
	@NotNull
	public static Map<String,String> makeVarMapClosure(@NotNull final Map<String, String> varMap)
	{
		return varMap.keySet().stream().map(k->new SimpleEntry<>(k, getOriginalVar(k, varMap))).collect(Collectors.toMap(SimpleEntry::getKey,SimpleEntry::getValue));
	}

	// V A R   C O U N T

	/**
	 * Returns the number of SUO-KIF variables (only ? variables, not
	 * variables) in the input query literal.
	 *
	 * @param form A List representing a Formula.
	 * @return An int.
	 */
	public static int getVarCount(@Nullable final List<String> form)
	{
		int result = 0;
		if (form != null)
		{
			for (@NotNull String term : form)
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
