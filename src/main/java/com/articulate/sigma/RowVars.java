/* This code is copyright Articulate Software (c) 2003.  Some
portions copyright Teknowledge (c) 2003 and reused under the terms of
the GNU license.  This software is released under the GNU Public
License &lt;http://www.gnu.org/copyleft/gpl.html&gt;.  Users of this code
also consent, by use of this code, to credit Articulate Software and
Teknowledge in any writings, briefings, publications, presentations,
or other representations of any software which incorporates, builds
on, or uses this code.  Please cite the following article in any
publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment, in
Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed
Systems, August 9, Acapulco, Mexico. See also http://sigmakee.sourceforge.net
*/

package com.articulate.sigma;

import java.util.*;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handle operations on an individual formula.  This includes formatting.
 */
public class RowVars
{
	private static final String LOG_SOURCE = "RowVars";

	private static final Logger logger = Logger.getLogger(RowVars.class.getName());

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
		logger.entering(LOG_SOURCE, "expandRowVars", f0);
		@NotNull List<Formula> result = new ArrayList<>();
		@Nullable Set<String> rowVars = f0.form.contains(Formula.R_PREFIX) ? f0.collectRowVariables() : null;

		// If this Formula contains no row vars to expand, we just add it to resultList and quit.
		if ((rowVars == null) || rowVars.isEmpty())
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
				@NotNull List<Formula> todo = new ArrayList<>(accumulator);
				accumulator.clear();

				for (@NotNull Formula f2 : todo)
				{
					@NotNull String form2 = f2.form;
					if (!form2.contains(Formula.R_PREFIX) || (form2.contains("\"")))
					{
						f2.sourceFile = f0.sourceFile;
						result.add(f2);
					}
					else
					{
						int[] range = getRowVarExpansionRange(f2, rowVar, arityGetter);

						boolean hasVariableArityRelation = range[0] == 0;
						range[1] = adjustExpansionCount(f0, rowVar, hasVariableArityRelation, range[1]);

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

								if (f3.form.contains(Formula.R_PREFIX) && (!f3.form.contains("\"")))
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

							if (f3.form.contains(Formula.R_PREFIX) && (f3.form.indexOf('"') == -1))
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
		logger.exiting(LOG_SOURCE, "expandRowVars", result);
		return result;
	}

	/**
	 * This method attempts to revise the number of row var expansions
	 * to be done, based on the occurrence of forms such as (<pred>
	 * Note that variables such as ?ITEM throw off the
	 * default expected expansion count, and so must be dealt with to
	 * prevent unnecessary expansions.
	 *
	 * @param f0            A Formula.
	 * @param variableArity Indicates whether the overall expansion
	 *                      count for the Formula is governed by a variable arity relation,
	 *                      or not.
	 * @param count         The default expected expansion count, possibly to
	 *                      be revised.
	 * @param var           The row variable to be expanded.
	 * @return An int value, the revised expansion count.  In most
	 * cases, the count will not change.
	 */
	private static int adjustExpansionCount(@NotNull final Formula f0, @NotNull final String var, boolean variableArity, int count)
	{
		if (logger.isLoggable(Level.FINER))
		{
			@NotNull String[] params = {"variableArity = " + variableArity, "count = " + count, "var = " + var};
			logger.entering(LOG_SOURCE, "adjustExpansionCount", params);
		}
		int revisedCount = count;
		if (!var.isEmpty())
		{
			@NotNull String rowVar = var;
			if (!var.startsWith("@"))
			{
				rowVar = ("@" + var);
			}
			@NotNull List<Formula> accumulator = new ArrayList<>();
			if (f0.listP() && !f0.empty())
			{
				accumulator.add(f0);
			}
			while (!accumulator.isEmpty())
			{
				@NotNull List<Formula> fs = new ArrayList<>(accumulator);
				accumulator.clear();
				for (@NotNull final Formula f : fs)
				{
					@NotNull List<String> literal = f.elements();
					int len = literal.size();
					if (literal.contains(rowVar) && !Formula.isVariable(f.car()))
					{
						if (!variableArity && (len > 2))
						{
							revisedCount = (count - (len - 2));
						}
						else if (variableArity)
						{
							revisedCount = (10 - len);
						}
					}
					if (revisedCount < 2)
					{
						revisedCount = 2;
					}
					@Nullable Formula f2 = f;
					while (f2 != null && !f2.empty())
					{
						@NotNull Formula argF = Formula.of(f2.car());
						if (argF.listP() && !argF.empty())
						{
							accumulator.add(argF);
						}
						f2 = f2.cdrAsFormula();
					}
				}
			}
		}
		logger.exiting(LOG_SOURCE, "adjustExpansionCount", revisedCount);
		return revisedCount;
	}

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
	 * [1,8].  If the Formula does not contain
	 */
	private static int[] getRowVarExpansionRange(@NotNull final Formula f0, final String rowVar, @NotNull final Function<String, Integer> arityGetter)
	{
		if (logger.isLoggable(Level.FINER))
		{
			@NotNull String[] params = {"f0 = " + f0, "rowVar = " + rowVar};
			logger.entering(LOG_SOURCE, "getRowVarExpansionRange", params);
		}
		@NotNull int[] result = new int[]{1, 8};
		if (!rowVar.isEmpty())
		{
			@NotNull String var = rowVar;
			if (!var.startsWith(Formula.R_PREFIX))
			{
				var = Formula.R_PREFIX + var;
			}
			@NotNull Map<String, int[]> minMaxMap = getRowVarsMinMax(f0, arityGetter);
			int[] range = minMaxMap.get(var);
			if (range != null)
			{
				result = range;
			}
		}
		logger.exiting(LOG_SOURCE, "getRowVarExpansionRange", result);
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
	private static Map<String, int[]> getRowVarsMinMax(@NotNull final Formula f0, @NotNull final Function<String, Integer> arityGetter)
	{
		logger.entering(LOG_SOURCE, "getRowVarsMinMax", f0);
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
		@NotNull Map<String, SortedSet<String>> rowVarRelns = new HashMap<>();
		for (@Nullable Clause clause : clauses)
		{
			if (clause != null)
			{
				// First we get the neg lits.  It may be that we should use *only* the neg lits for this
				// task, but we will start by combining the neg lits and pos lits into one list of literals
				// and see how that works.
				List<Formula> literals = clause.negativeLits;
				List<Formula> posLits = clause.positiveLits;
				literals.addAll(posLits);
				for (@NotNull Formula litF : literals)
				{
					computeRowVarsWithRelations(litF, rowVarRelns, varMap);
				}
			}
			// logger.finest("rowVarRelns == " + rowVarRelns);
			if (!rowVarRelns.isEmpty())
			{
				for (String rowVar : rowVarRelns.keySet())
				{
					@Nullable String origRowVar = Variables.getOriginalVar(rowVar, varMap);
					@NotNull int[] minMax = result.computeIfAbsent(origRowVar, k -> new int[]{0, 8});
					SortedSet<String> val = rowVarRelns.get(rowVar);
					for (@NotNull String reln : val)
					{
						int arity = arityGetter.apply(reln);
						if (arity >= 1)
						{
							minMax[0] = 1;
							int arityPlusOne = (arity + 1);
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
		logger.exiting(LOG_SOURCE, "getRowVarsMinMax", result);
		return result;
	}

	/**
	 * Finds all the relations in this Formula that are applied to row
	 * variables, and for which a specific arity might be computed.
	 * Note that results are accumulated in varsToRelns, and the
	 * variable correspondences (if any) in varsToVars are used to
	 * compute the results.
	 *
	 * @param f0          A Formula.
	 * @param varsToRelns A Map for accumulating row var data for one
	 *                    Formula literal.  The keys are row variables (Strings) and the
	 *                    values are SortedSets containing relations (Strings) that might
	 *                    help to constrain the row var during row var expansion.
	 * @param varsToVars  A Map of variable correspondences, the leaves
	 *                    of which might include row variables
	 */
	private static void computeRowVarsWithRelations(@NotNull final Formula f0, @NotNull final Map<String, SortedSet<String>> varsToRelns, @Nullable final Map<String, String> varsToVars)
	{
		@NotNull Formula f = f0;
		if (f.listP() && !f.empty())
		{
			@NotNull String relation = f.car();
			if (!Formula.isVariable(relation) && !relation.equals(Formula.SKFN))
			{
				@Nullable Formula newF = f.cdrAsFormula();
				while (newF != null && newF.listP() && !newF.empty())
				{
					@NotNull String term = newF.car();
					@Nullable String rowVar = term;
					if (Formula.isVariable(rowVar))
					{
						if (rowVar.startsWith(Formula.V_PREFIX) && (varsToVars != null))
						{
							rowVar = Variables.getOriginalVar(term, varsToVars);
						}
					}
					if (rowVar != null && rowVar.startsWith(Formula.R_PREFIX))
					{
						SortedSet<String> relns = varsToRelns.get(term);
						if (relns == null)
						{
							relns = new TreeSet<>();
							varsToRelns.put(term, relns);
							varsToRelns.put(rowVar, relns);
						}
						relns.add(relation);
					}
					else
					{
						@NotNull Formula termF = Formula.of(term);
						computeRowVarsWithRelations(termF, varsToRelns, varsToVars);
					}

					newF = newF.cdrAsFormula();
				}
			}
		}
	}
}
