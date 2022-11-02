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
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * Handle operations on an individual formula.  This includes formatting.
 */
public class RowVars
{
	private static final String LOG_SOURCE = "RowVars";

	private static final Logger LOGGER = Logger.getLogger(RowVars.class.getName());

	public static final int MAX_EXPANSION = 8;

	// E X P A N D

	/**
	 * Expand row variables, keeping the information about the original
	 * source formula.  Each variable is treated like a macro that
	 * expands to up to seven regular variables.  For example
	 * (=&gt;
	 * (and
	 * (subrelation ?REL1 ?REL2)
	 * (holds__ ?REL1 @ROW))
	 * (holds__ ?REL2 @ROW))
	 * would become
	 * (=&gt;
	 * (and
	 * (subrelation ?REL1 ?REL2)
	 * (holds__ ?REL1 ?ARG1))
	 * (holds__ ?REL2 ?ARG1))
	 * (=&gt;
	 * (and
	 * (subrelation ?REL1 ?REL2)
	 * (holds__ ?REL1 ?ARG1 ?ARG2))
	 * (holds__ ?REL2 ?ARG1 ?ARG2))
	 * etc.
	 *
	 * @param f0          A Formula.
	 * @param arityGetter A function that get hte arity of a relation.
	 * @return a List of Formulas, or an empty List.
	 */
	@NotNull
	public static List<Formula> expandRowVars(@NotNull final Formula f0, @NotNull final Function<String, Integer> arityGetter)
	{
		LOGGER.entering(LOG_SOURCE, "expandRowVars", f0);
		@NotNull List<Formula> result = new ArrayList<>();
		@Nullable Set<String> rowVars = f0.form.contains(Formula.R_PREFIX) ? f0.collectRowVariables() : null;

		// If this Formula contains no row vars to expand, we just add it to result and quit.
		if (rowVars == null || rowVars.isEmpty())
		{
			result.add(f0);
		}
		else
		{
			@NotNull Formula f = Formula.copy(f0);

			@NotNull Set<Formula> accumulator = new LinkedHashSet<>();
			accumulator.add(f);

			// Iterate through the row variables
			for (@NotNull String rowVar : rowVars)
			{
				@NotNull List<Formula> toVisit = new ArrayList<>(accumulator);
				accumulator.clear();

				for (@NotNull Formula f2 : toVisit)
				{
					@NotNull String form2 = f2.form;
					if (!form2.contains(Formula.R_PREFIX) || form2.indexOf(Formula.DOUBLE_QUOTE_CHAR) > -1)
					{
						f2.sourceFile = f0.sourceFile;
						result.add(f2);
					}
					else
					{
						// expansion range
						int[] range = getRowVarExpansionRange(f2, rowVar, arityGetter);

						// try to adjust expansion range upper boundary
						boolean hasVariableArityRelation = range[0] == 0;
						range[1] = adjustExpansionCount(f0, rowVar, hasVariableArityRelation, range[1]);

						// replace
						@NotNull StringBuilder varRepl = new StringBuilder();
						for (int j = 1; j < range[1]; j++)
						{
							if (varRepl.length() > 0)
							{
								varRepl.append(" ");
							}
							varRepl.append(Formula.V_PREFIX);
							varRepl.append(rowVar.substring(1));
							varRepl.append(j);

							if (hasVariableArityRelation)
							{
								@NotNull String form3 = form2.replaceAll(rowVar, varRepl.toString());
								@NotNull Formula f3 = Formula.of(form3);

								// Copy the source file information for each expanded formula.
								f3.sourceFile = f0.sourceFile;

								if (f3.form.contains(Formula.R_PREFIX) && f3.form.indexOf(Formula.DOUBLE_QUOTE_CHAR) == -1)
								{
									accumulator.add(f3);
								}
								else
								{
									result.add(f3);
								}
							}
						}
						if (!hasVariableArityRelation)
						{
							@NotNull String form3 = form2.replaceAll(rowVar, varRepl.toString());
							@NotNull Formula f3 = Formula.of(form3);

							// Copy the source file information for each expanded formula.
							f3.sourceFile = f0.sourceFile;

							if (f3.form.contains(Formula.R_PREFIX) && f3.form.indexOf(Formula.DOUBLE_QUOTE_CHAR) == -1)
							{
								accumulator.add(f3);
							}
							else
							{
								result.add(f3);
							}
						}
					}
				}
			}
		}
		LOGGER.exiting(LOG_SOURCE, "expandRowVars", result);
		return result;
	}

	// E X P A N S I O N   R A N G E

	/**
	 * Returns a two-place int[] indicating the low and high points of
	 * the expansion range (number of row var instances) for the input
	 * row var.
	 *
	 * @param f0          A Formula.
	 * @param rowVar      The row var (String) to be expanded.
	 * @param arityGetter A function that get hte arity of a relation.
	 * @return A two-place int[] object.  The int[] indicates a
	 * numeric range.  int[0] holds the start (the lowest number) in the
	 * range, and int[1] holds the highest number.  The default is
	 * [1,8].
	 */
	private static int[] getRowVarExpansionRange(@NotNull final Formula f0, @NotNull final String rowVar, @NotNull final Function<String, Integer> arityGetter)
	{
		return getRowVarExpansionRange(f0.form, rowVar, arityGetter);
	}

	/**
	 * Returns a two-place int[] indicating the low and high points of
	 * the expansion range (number of row var instances) for the input
	 * row var.
	 *
	 * @param form        A formula string.
	 * @param rowVar      The row var (String) to be expanded.
	 * @param arityGetter A function that get hte arity of a relation.
	 * @return A two-place int[] object.  The int[] indicates a
	 * numeric range.  int[0] holds the start (the lowest number) in the
	 * range, and int[1] holds the highest number.  The default is
	 * [1,8].
	 */
	public static int[] getRowVarExpansionRange(@NotNull final String form, @NotNull final String rowVar, @NotNull final Function<String, Integer> arityGetter)
	{
		LOGGER.entering(LOG_SOURCE, "getRowVarExpansionRange", new String[]{"form = " + form, "rowVar = " + rowVar});
		@NotNull int[] result = new int[]{1, MAX_EXPANSION};
		if (!rowVar.isEmpty())
		{
			// check var prefix
			@NotNull String var = rowVar;
			if (!var.startsWith(Formula.R_PREFIX))
			{
				var = Formula.R_PREFIX + var;
			}

			// get rowvars minmaxes
			@NotNull Map<String, int[]> var2minMax = getRowVarsExpansionRange(form, arityGetter);

			// get selected
			int[] range = var2minMax.get(var);
			if (range != null)
			{
				result = range;
			}
		}
		LOGGER.exiting(LOG_SOURCE, "getRowVarExpansionRange", result);
		return result;
	}

	/**
	 * Applied to a SUO-KIF Formula with row variables, this method
	 * returns a Map containing an int[] of length 2 for each row var
	 * that indicates the minimum and maximum number of row var
	 * expansions to perform.
	 *
	 * @param f0          A Formula.
	 * @param arityGetter A function that get hte arity of a relation.
	 * @return A Map in which the keys are distinct row variables and
	 * the values are two-place int[] objects.  The int[] indicates a
	 * numeric range.  int[0] is the start (the lowest number) in the
	 * range, and int[1] is the end.  If the Formula contains no row
	 * vars, the Map is empty.
	 */
	@NotNull
	public static Map<String, int[]> getRowVarsExpansionRange(@NotNull final Formula f0, @NotNull final Function<String, Integer> arityGetter)
	{
		LOGGER.entering(LOG_SOURCE, "getRowVarsMinMax", f0);
		@NotNull Map<String, int[]> result = new HashMap<>();
		@Nullable Tuple.Triple<List<Clause>, Map<String, String>, Formula> clauseData = f0.getClausalForms();
		if (clauseData == null)
		{
			return result;
		}

		@Nullable List<Clause> clauses = clauseData.first;
		if (clauses == null || clauses.isEmpty())
		{
			return result;
		}

		Map<String, String> varMap = clauseData.second;
		@NotNull Map<String, Set<String>> rowVarRelns = new HashMap<>();
		for (@Nullable Clause clause : clauses)
		{
			// collect the relations the rowvar is argument of in clause
			if (clause != null)
			{
				// First we get the neg lits.  It may be that we should use *only* the neg lits for this
				// task, but we will start by combining the neg lits and pos lits into one list of literals
				// and see how that works.
				List<Formula> litFs = clause.negativeLits;
				List<Formula> posLits = clause.positiveLits;
				litFs.addAll(posLits);
				for (@NotNull Formula litF : litFs)
				{
					computeRowVarsWithRelations(litF, varMap, rowVarRelns);
				}
			}

			// range
			if (!rowVarRelns.isEmpty())
			{
				for (String rowVar : rowVarRelns.keySet())
				{
					@NotNull String origRowVar = Variables.getOriginalVar(rowVar, varMap);
					@NotNull int[] minMax = result.computeIfAbsent(origRowVar, k -> new int[]{0, MAX_EXPANSION});
					Set<String> val = rowVarRelns.get(rowVar);
					for (@NotNull String reln : val)
					{
						int arity = arityGetter.apply(reln);
						if (arity >= 1)
						{
							// min
							minMax[0] = 1;
							// max = min(arity+1, max)
							int arityPlusOne = arity + 1;
							if (arityPlusOne < minMax[1])
							{
								minMax[1] = arityPlusOne;
							}
						}
						//else
						//{
						// It's a VariableArityRelation or we
						// can't find an arity, so do nothing.
						//}
					}
				}
			}
		}
		LOGGER.exiting(LOG_SOURCE, "getRowVarsMinMax", result);
		return result;
	}

	/**
	 * Applied to a SUO-KIF formula with row variables, this method
	 * returns a Map containing an int[] of length 2 for each row var
	 * that indicates the minimum and maximum number of row var
	 * expansions to perform.
	 *
	 * @param form        A formula string.
	 * @param arityGetter A function that get hte arity of a relation.
	 * @return A Map in which the keys are distinct row variables and
	 * the values are two-place int[] objects.  The int[] indicates a
	 * numeric range.  int[0] is the start (the lowest number) in the
	 * range, and int[1] is the end.  If the Formula contains no row
	 * vars, the Map is empty.
	 */
	@NotNull
	private static Map<String, int[]> getRowVarsExpansionRange(@NotNull final String form, @NotNull final Function<String, Integer> arityGetter)
	{
		return getRowVarsExpansionRange(Formula.of(form), arityGetter);
	}

	// A D J U S T

	/**
	 * This method attempts to revise the number of row var expansions
	 * to be done, based on the occurrence of forms such as (<pred>
	 * Note that variables such as ?ITEM throw off the
	 * default expected expansion count, and so must be dealt with to
	 * prevent unnecessary expansions.
	 *
	 * @param f0                              A Formula.
	 * @param governedByVariableArityRelation Indicates whether the overall expansion
	 *                                        count for the Formula is governed by a variable arity relation,
	 *                                        or not.
	 * @param count                           The default expected expansion count, possibly to
	 *                                        be revised.
	 * @param var                             The row variable to be expanded.
	 * @return An int value, the revised expansion count.  In most
	 * cases, the count will not change.
	 */
	private static int adjustExpansionCount(@NotNull final Formula f0, @NotNull final String var, boolean governedByVariableArityRelation, int count)
	{
		return adjustExpansionCount(f0.form, var, governedByVariableArityRelation, count);
	}

	/**
	 * This method attempts to revise the number of row var expansions
	 * to be done, based on the occurrence of forms such as (<pred> @ROW ...)
	 * Note that variables such as ?ITEM throw off the
	 * default expected expansion count, and so must be dealt with to
	 * prevent unnecessary expansions.
	 *
	 * @param form                            A formula string
	 * @param governedByVariableArityRelation Indicates whether the overall expansion
	 *                                        count for the formula is governed by a variable-arity relation,
	 *                                        or not.
	 * @param count                           The default expected expansion count, possibly to
	 *                                        be revised.
	 * @param var                             The row variable to be expanded.
	 * @return An int value, the revised expansion count.  In most
	 * cases, the count will not change.
	 */
	public static int adjustExpansionCount(@NotNull final String form, @NotNull final String var, boolean governedByVariableArityRelation, int count)
	{
		LOGGER.entering(LOG_SOURCE, "adjustExpansionCount", new String[]{"variableArity = " + governedByVariableArityRelation, "count = " + count, "var = " + var});
		int revisedCount = count;
		if (!var.isEmpty())
		{
			// row var
			@NotNull String rowVar = var;
			if (!var.startsWith(Formula.R_PREFIX))
			{
				rowVar = Formula.R_PREFIX + var;
			}

			@NotNull List<String> accumulator = new ArrayList<>();
			if (Lisp.listP(form) && !Lisp.empty(form))
			{
				accumulator.add(form);
			}
			while (!accumulator.isEmpty())
			{
				@NotNull List<String> forms2 = new ArrayList<>(accumulator);
				accumulator.clear();
				for (@NotNull final String form2 : forms2)
				{
					@NotNull List<String> elements2 = Lisp.elements(form2);
					int nelements = elements2.size();
					if (elements2.contains(rowVar) && !Formula.isVariable(Lisp.car(form2)))
					{
						if (!governedByVariableArityRelation && nelements > 2)
						{
							revisedCount = count - (nelements - 2);
						}
						else if (governedByVariableArityRelation)
						{
							revisedCount = 10 - nelements;
						}
					}
					if (revisedCount < 2)
					{
						revisedCount = 2;
					}

					// feed accumlator
					for (@NotNull IterableFormula itF = new IterableFormula(form2); !itF.empty(); itF.pop())
					{
						@NotNull String arg = itF.car();
						if (Lisp.listP(arg) && !Lisp.empty(arg))
						{
							accumulator.add(arg);
						}
					}
				}
			}
		}
		LOGGER.exiting(LOG_SOURCE, "adjustExpansionCount", revisedCount);
		return revisedCount;
	}

	// C O M P U T E   R O W V A R ' S   G O V E R N I N G   R E L A T I O N S

	/**
	 * Finds all the relations in this Formula that are applied to row
	 * variables, and for which a specific arity might be computed.
	 * Note that results are accumulated in varsToRelns, and the
	 * variable correspondences (if any) in varsToVars are used to
	 * compute the results.
	 *
	 * @param f0          A Formula.
	 * @param varsToVars  A Map of variable correspondences, the leaves
	 *                    of which might include row variables
	 * @param varsToRelns A Map for accumulating row var data for one
	 *                    Formula literal.  The keys are row variables (Strings) and the
	 *                    values are SortedSets containing relations (Strings) that might
	 *                    help to constrain the row var during row var expansion.
	 */
	private static void computeRowVarsWithRelations(@NotNull final Formula f0, @Nullable final Map<String, String> varsToVars, @NotNull final Map<String, Set<String>> varsToRelns)
	{
		computeRowVarsWithRelations(f0.form, varsToVars, varsToRelns);
	}

	/**
	 * Finds all the relations in this formula that are applied to row
	 * variables, and for which a specific arity might be computed.
	 * Note that results are accumulated in varsToRelns, and the
	 * variable correspondences (if any) in varsToVars are used to
	 * compute the results.
	 *
	 * @param form        A formula string.
	 * @param varsToVars  A Map of variable correspondences, the leaves
	 *                    of which might include row variables
	 * @param varsToRelns A Map for accumulating row var data for one
	 *                    formula literal.  The keys are row variables (Strings) and the
	 *                    values are SortedSets containing relations (Strings) that might
	 *                    help to constrain the row var during row var expansion.
	 */
	public static void computeRowVarsWithRelations(@NotNull final String form, @Nullable final Map<String, String> varsToVars, @NotNull final Map<String, Set<String>> varsToRelns)
	{
		if (Lisp.listP(form) && !Lisp.empty(form))
		{
			@NotNull String reln = Lisp.car(form);
			if (!Formula.isVariable(reln) && !Formula.SKFN.equals(reln))
			{
				for (@NotNull IterableFormula itF = new IterableFormula(Lisp.cdr(form)); !itF.empty() && itF.listP(); itF.pop())
				{
					@NotNull final String arg = itF.car();

					// valued arg
					@NotNull String varg = arg;
					if (!varg.isEmpty() && Formula.isVariable(varg))
					{
						if (varg.startsWith(Formula.V_PREFIX) && varsToVars != null)
						{
							varg = Variables.getOriginalVar(arg, varsToVars);
						}
					}

					// handle arg
					if (varg.startsWith(Formula.R_PREFIX))
					{
						// handle rowvar
						Set<String> relns = varsToRelns.computeIfAbsent(arg, k -> new TreeSet<>());
						relns.add(reln);
						// varsToRelns.put(arg, relns);
						varsToRelns.put(varg, relns);
					}
					else if (!varg.isEmpty())
					{
						// recurse
						computeRowVarsWithRelations(arg, varsToVars, varsToRelns);
					}
				}
			}
		}
	}
}
