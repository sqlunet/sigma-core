/* This code is copyright Articulate Software (c) 2003.
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code are also requested, to credit Articulate Software in any
writings, briefings, publications, presentations, or 
other representations of any software which incorporates,
builds on, or uses this code. Please cite the following
article in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment, 
in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico.  See also http://sigmakee.sourceforge.net
*/

package com.articulate.sigma;

import java.util.*;

/**
 * The code in the section below implements an algorithm for
 * translating SUO-KIF expressions to clausal form.  The
 * public methods are:
 * public Formula clausify()
 * public List clausifyWithRenameInfo()
 * public List toNegAndPosLitsWithRenameInfo()
 * The result is a single formula in conjunctive normal form
 * (CNF), which is actually a set of (possibly negated) clauses
 * surrounded by an "or".
 */
public class Clausifier
{
	private Formula formula;

	// This static variable holds the int value that is used to generate unique variable names.
	private static int VAR_INDEX = 0;

	// This static variable holds the int value that is used to generate unique Skolem terms.
	private static int SKOLEM_INDEX = 0;

	/**
	 * Constructor
	 *
	 * @param s formula string
	 */
	public Clausifier(String s)
	{
		formula = new Formula();
		formula.set(s);
	}

	/**
	 * Clausify
	 *
	 * @return an List that contains three items: The new
	 * clausal-form Formula, the original (input) SUO-KIF Formula, and
	 * a Map containing a graph of all the variable substitutions done
	 * during the conversion to clausal form.  This Map makes it
	 * possible to retrieve the correspondence between the variables
	 * in the clausal form and the variables in the original
	 * Formula. Some elements might be null if a clausal form
	 * cannot be generated.
	 */
	public Tuple.Triple<Formula, Formula, Map<String, String>> clausifyWithRenameInfo()
	{
		Formula old = new Formula();
		old.text = formula.text;
		Tuple.Triple<Formula, Formula, Map<String, String>> result = new Tuple.Triple<>();
		try
		{
			Map<String, String> topLevelVars = new HashMap<>();
			Map<String, String> scopedRenames = new HashMap<>();
			Map<String, String> allRenames = new HashMap<>();
			Map<String, String> standardizedRenames = new HashMap<>();
			formula = equivalencesOut();
			formula = implicationsOut();
			formula = negationsIn();
			formula = renameVariables(topLevelVars, scopedRenames, allRenames);
			formula = existentialsOut();
			formula = universalsOut();
			formula = disjunctionsIn();
			formula = standardizeApart(standardizedRenames);
			allRenames.putAll(standardizedRenames);

			result.first = formula;
			result.second = old;
			result.third = allRenames;
			// resetClausifyIndices();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		return result;
	}

	/**
	 * This method converts the SUO-KIF Formula to an List of
	 * clauses.  Each clause is an List containing an List
	 * of negative literals, and an List of positive literals.
	 * Either the neg lits list or the pos lits list could be empty.
	 * Each literal is a Formula object.
	 * The first object in the returned triplet is an List of
	 * clauses.
	 * The second object in the returned triplet is the original
	 * (input) Formula object (this).
	 * The third object in the returned List is a Map that
	 * contains a graph of all the variable substitutions done during
	 * the conversion of this Formula to clausal form.  This Map makes
	 * it possible to retrieve the correspondences between the
	 * variables in the clausal form and the variables in the original
	 * Formula.
	 *
	 * @return A three-element tuple,
	 * [
	 * // 1. clauses
	 * [
	 * // a clause
	 * [
	 * // negative literals
	 * [ Formula1, Formula2, ..., FormulaN ],
	 * // positive literals
	 * [ Formula1, Formula2, ..., FormulaN ]
	 * ],
	 * // another clause
	 * [
	 * // negative literals
	 * [ Formula1, Formula2, ..., FormulaN ],
	 * // positive literals
	 * [ Formula1, Formula2, ..., FormulaN ]
	 * ],
	 * ...,
	 * ],
	 * // 2. the original Formula,
	 * // 3. a Map of variable renamings,
	 * ]
	 */
	public Tuple.Triple<List<Clause>, Formula, Map<String, String>> toNegAndPosLitsWithRenameInfo()
	{
		Tuple.Triple<List<Clause>, Formula, Map<String, String>> result = new Tuple.Triple<>();
		try
		{
			Tuple.Triple<Formula, Formula, Map<String, String>> clausesWithRenameInfo = this.clausifyWithRenameInfo();

			Formula clausalForm = clausesWithRenameInfo.first;
			Clausifier clausifier = new Clausifier(clausalForm.text);
			List<Formula> clauses = clausifier.operatorsOut();
			if (!clauses.isEmpty())
			{
				List<Clause> newClauses = new ArrayList<>();
				for (Formula clause : clauses)
				{
					Clause literals = new Clause();
					if (clause.listP())
					{
						while (!clause.empty())
						{
							boolean isNegLit = false;
							String lit = clause.car();
							Formula litF = new Formula();
							litF.set(lit);
							if (litF.listP() && litF.car().equals(Formula.NOT))
							{
								litF.set(litF.cadr());
								isNegLit = true;
							}
							if (litF.text.equals(Formula.LOG_FALSE))
								isNegLit = true;
							if (isNegLit)
								literals.negativeLits.add(litF);
							else
								literals.positiveLits.add(litF);
							clause = clause.cdrAsFormula();
						}
					}
					else if (clause.text.equals(Formula.LOG_FALSE))
						literals.negativeLits.add(clause);
					else
						literals.positiveLits.add(clause);
					newClauses.add(literals);
				}
				// Collections.sort(negLits);
				// Collections.sort(posLits);
				result.first = newClauses;
			}
			result.second = clausesWithRenameInfo.second;
			result.third = clausesWithRenameInfo.third;
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		return result;
	}

	/**
	 * Convenience method
	 *
	 * @param f formula
	 * @return A three-element tuple,
	 * [
	 * // 1. clauses
	 * [
	 * // a clause
	 * [
	 * // negative literals
	 * [ Formula1, Formula2, ..., FormulaN ],
	 * // positive literals
	 * [ Formula1, Formula2, ..., FormulaN ]
	 * ],
	 * // another clause
	 * [
	 * // negative literals
	 * [ Formula1, Formula2, ..., FormulaN ],
	 * // positive literals
	 * [ Formula1, Formula2, ..., FormulaN ]
	 * ],
	 * ...,
	 * ],
	 * // 2. the original Formula,
	 * // 3. a Map of variable renamings,
	 * ]
	 */
	public static Tuple.Triple<List<Clause>, Formula, Map<String, String>> toNegAndPosLitsWithRenameInfo(Formula f)
	{
		Clausifier clausifier = new Clausifier(f.text);
		return clausifier.toNegAndPosLitsWithRenameInfo();
	}

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
	public static String normalizeVariables(String input)
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
	protected static String normalizeVariables(String input, @SuppressWarnings("SameParameterValue") boolean replaceSkolemTerms)
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
	protected static String normalizeVariables_1(String input, int[] idxs, Map<String, String> varMap, boolean replaceSkolemTerms)
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
					Formula f = new Formula();
					f.set(fList);

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
	 * This method converts every occurrence of '<=>' in the Formula
	 * to a conjunct with two occurrences of '=>'.
	 *
	 * @return A Formula with no occurrences of '<=>'.
	 */
	private Formula equivalencesOut()
	{
		Formula result = formula;
		if (formula.listP() && !(formula.empty()))
		{
			String head = formula.car();
			String newFStr;
			if (isNonEmpty(head) && Formula.listP(head))
			{
				Clausifier headF = new Clausifier(head);
				String newHead = headF.equivalencesOut().text;
				Clausifier clausifier = new Clausifier(formula.cdr());
				newFStr = clausifier.equivalencesOut().cons(newHead).text;
			}
			else if (head.equals(Formula.IFF))
			{
				String second = formula.cadr();
				Clausifier secondF = new Clausifier(second);
				String newSecond = secondF.equivalencesOut().text;
				String third = formula.caddr();
				Clausifier thirdF = new Clausifier(third);
				String newThird = thirdF.equivalencesOut().text;
				newFStr = ("(and (=> " + newSecond + " " + newThird + ") (=> " + newThird + " " + newSecond + "))");
			}
			else
			{
				Clausifier fourth = new Clausifier(formula.cdrAsFormula().text);
				newFStr = fourth.equivalencesOut().cons(head).text;
			}
			if (newFStr != null)
			{
				result = new Formula();
				result.set(newFStr);
			}
		}
		return result;
	}

	/**
	 * This method converts every occurrence of '(=> LHS RHS' in the
	 * Formula to a disjunct of the form '(or (not LHS) RHS)'.
	 *
	 * @return A Formula with no occurrences of '=>'.
	 */
	private Formula implicationsOut()
	{
		Formula result = formula;
		String newFStr;
		if (formula.listP() && !formula.empty())
		{
			String head = formula.car();
			if (isNonEmpty(head) && Formula.listP(head))
			{
				Clausifier headF = new Clausifier(head);
				String newHead = headF.implicationsOut().text;
				Clausifier clausifier = new Clausifier(formula.cdr());
				newFStr = clausifier.implicationsOut().cons(newHead).text;
			}
			else if (head.equals(Formula.IF))
			{
				String second = formula.cadr();
				Clausifier secondF = new Clausifier(second);
				String newSecond = secondF.implicationsOut().text;
				String third = formula.caddr();
				Clausifier thirdF = new Clausifier(third);
				String newThird = thirdF.implicationsOut().text;
				newFStr = "(or (not " + newSecond + ") " + newThird + ")";
			}
			else
			{
				Clausifier fourth = new Clausifier(formula.cdrAsFormula().text);
				newFStr = fourth.implicationsOut().cons(head).text;
			}
			if (newFStr != null)
			{
				result = new Formula();
				result.set(newFStr);
			}
		}
		return result;
	}

	/**
	 * This method 'pushes in' all occurrences of 'not', so that each
	 * occurrence has the narrowest possible scope, and also removes
	 * from the Formula all occurrences of '(not (not ...))'.
	 *
	 * @return A Formula with all occurrences of 'not' accorded
	 * narrowest scope, and no occurrences of '(not (not ...))'.
	 */
	private Formula negationsIn()
	{
		Formula f = formula;
		Formula result = negationsIn_1();
		// Here we repeatedly apply negationsIn_1() until there are no more changes.
		while (!f.text.equals(result.text))
		{
			f = result;
			result = negationsIn_1(f);
		}
		return result;
	}

	/**
	 * This method is used in negationsIn().  It recursively 'pushes
	 * in' all occurrences of 'not', so that each occurrence has the
	 * narrowest possible scope, and also removes from the Formula all
	 * occurrences of '(not (not ...))'.
	 *
	 * @return A Formula with all occurrences of 'not' accorded
	 * narrowest scope, and no occurrences of '(not (not ...))'.
	 */
	private Formula negationsIn_1()
	{
		try
		{
			if (formula.listP())
			{
				if (formula.empty())
				{
					return formula;
				}
				String arg0 = formula.car();
				String arg1 = formula.cadr();
				if (arg0.equals(Formula.NOT) && Formula.listP(arg1))
				{
					Formula arg1F = new Formula();
					arg1F.set(arg1);
					String arg0_of_arg1 = arg1F.car();
					if (arg0_of_arg1.equals(Formula.NOT))
					{
						String arg1_of_arg1 = arg1F.cadr();
						Formula arg1_of_arg1F = new Formula();
						arg1_of_arg1F.set(arg1_of_arg1);
						return arg1_of_arg1F;
					}
					if (Formula.isCommutative(arg0_of_arg1))
					{
						String newOp = (arg0_of_arg1.equals(Formula.AND) ? Formula.OR : Formula.AND);
						return listAll(arg1F.cdrAsFormula(), "(not ", ")").cons(newOp);
					}
					if (Formula.isQuantifier(arg0_of_arg1))
					{
						String vars = arg1F.cadr();
						String arg2_of_arg1 = arg1F.caddr();
						String quant = (arg0_of_arg1.equals(Formula.UQUANT) ? Formula.EQUANT : Formula.UQUANT);
						arg2_of_arg1 = ("(not " + arg2_of_arg1 + ")");
						Formula arg2_of_arg1F = new Formula();
						arg2_of_arg1F.set(arg2_of_arg1);
						String newFStr = ("(" + quant + " " + vars + " " + negationsIn_1(arg2_of_arg1F).text + ")");
						Formula newF = new Formula();
						newF.set(newFStr);
						return newF;
					}
					String newFStr = ("(not " + negationsIn_1(arg1F).text + ")");
					Formula newF = new Formula();
					newF.set(newFStr);
					return newF;
				}
				if (Formula.isQuantifier(arg0))
				{
					String arg2 = formula.caddr();
					Formula arg2F = new Formula();
					arg2F.set(arg2);
					String newArg2 = negationsIn_1(arg2F).text;
					String newFStr = ("(" + arg0 + " " + arg1 + " " + newArg2 + ")");
					Formula newF = new Formula();
					newF.set(newFStr);
					return newF;
				}
				if (Formula.listP(arg0))
				{
					Formula arg0F = new Formula();
					arg0F.set(arg0);
					return negationsIn_1(formula.cdrAsFormula()).cons(negationsIn_1(arg0F).text);
				}
				return negationsIn_1(formula.cdrAsFormula()).cons(arg0);
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		return formula;
	}

	/**
	 * convenience method
	 */
	private static Formula negationsIn_1(Formula f)
	{
		Clausifier temp = new Clausifier(f.text);
		return temp.negationsIn_1();
	}

	/**
	 * This method augments each element of the Formula by
	 * concatenating optional Strings before and after the element.
	 * Note that in most cases the input Formula will be simply a
	 * list, not a well-formed SUO-KIF Formula, and that the output
	 * will therefore not necessarily be a well-formed Formula.
	 *
	 * @param before A String that, if present, is prepended to every
	 *               element of the Formula.
	 * @param after  A String that, if present, is postpended to every
	 *               element of the Formula.
	 * @return A Formula, or, more likely, simply a list, with the
	 * String values corresponding to before and after added to each
	 * element.
	 */
	private Formula listAll(String before, String after)
	{
		Formula result = formula;
		if (formula.listP())
		{
			StringBuilder sb = new StringBuilder();
			Formula f = formula;
			while (!(f.empty()))
			{
				String element = f.car();
				if (isNonEmpty(before))
					element = (before + element);
				if (isNonEmpty(after))
					element += after;
				sb.append(Formula.SPACE).append(element);
				f = f.cdrAsFormula();
			}
			sb = new StringBuilder((Formula.LP + sb.toString().trim() + Formula.RP));
			if (isNonEmpty(sb.toString()))
			{
				result = new Formula();
				result.set(sb.toString());
			}
		}
		return result;
	}

	/**
	 * Convenience method
	 */
	private static Formula listAll(Formula f, @SuppressWarnings("SameParameterValue") String before, @SuppressWarnings("SameParameterValue") String after)
	{
		Clausifier clausifier = new Clausifier(f.text);
		return clausifier.listAll(before, after);
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
			VAR_INDEX = 0;
		else
			++VAR_INDEX;
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
			SKOLEM_INDEX = 0;
		else
			++SKOLEM_INDEX;
		return SKOLEM_INDEX;
	}

	/**
	 * This method returns a new SUO-KIF variable String, modifying
	 * any digit suffix to ensure that the variable will be unique.
	 *
	 * @param prefix An optional variable prefix string.
	 * @return A new SUO-KIF variable.
	 */
	private static String newVar(@SuppressWarnings("SameParameterValue") String prefix)
	{
		String base = Formula.VX;
		String varIdx = Integer.toString(incVarIndex());
		if (isNonEmpty(prefix))
		{
			List<String> woDigitSuffix = KB.getMatches(prefix, "var_with_digit_suffix");
			if (woDigitSuffix != null)
				base = woDigitSuffix.get(0);
			else if (prefix.startsWith(Formula.RVAR))
				base = Formula.RVAR;
			else if (prefix.startsWith(Formula.VX))
				base = Formula.VX;
			else
				base = prefix;
			if (!(base.startsWith(Formula.V_PREF) || base.startsWith(Formula.R_PREF)))
				base = (Formula.V_PREF + base);
		}
		return (base + varIdx);
	}

	/**
	 * This method returns a new SUO-KIF variable String, adding a
	 * digit suffix to ensure that the variable will be unique.
	 *
	 * @return A new SUO-KIF variable
	 */
	private static String newVar()
	{
		return newVar(null);
	}

	/**
	 * Convenience method to rename variabte
	 *
	 * @param f             formula
	 * @param topLevelVars  A Map that is used to track renames of implicitly universally quantified variables.
	 * @param scopedRenames A Map that is used to track renames of explicitly quantified variables.
	 * @param allRenames    A Map from all new vars in the Formula to their old counterparts.
	 * @return A new SUO-KIF Formula with all variables renamed.
	 */
	public static Formula renameVariables(Formula f, Map<String, String> topLevelVars, Map<String, String> scopedRenames, Map<String, String> allRenames)
	{
		Clausifier clausifier = new Clausifier(f.text);
		return clausifier.renameVariables(topLevelVars, scopedRenames, allRenames);
	}

	/**
	 * This method returns a new Formula in which all variables have
	 * been renamed to ensure uniqueness.
	 *
	 * @param topLevelVars  A Map that is used to track renames of implicitly universally quantified variables.
	 * @param scopedRenames A Map that is used to track renames of explicitly quantified variables.
	 * @param allRenames    A Map from all new vars in the Formula to their old counterparts.
	 * @return A new SUO-KIF Formula with all variables renamed.
	 */
	private Formula renameVariables(Map<String, String> topLevelVars, Map<String, String> scopedRenames, Map<String, String> allRenames)
	{
		try
		{
			if (formula.listP())
			{
				if (formula.empty())
				{
					return formula;
				}
				String arg0 = formula.car();
				if (Formula.isQuantifier(arg0))
				{
					// Copy the scopedRenames map to protect variable scope as we descend below this quantifier.
					Map<String, String> newScopedRenames = new HashMap<>(scopedRenames);

					String oldVars = formula.cadr();
					Formula oldVarsF = new Formula();
					oldVarsF.set(oldVars);
					StringBuilder newVars = new StringBuilder();
					while (!oldVarsF.empty())
					{
						String oldVar = oldVarsF.car();
						String newVar = newVar();
						newScopedRenames.put(oldVar, newVar);
						allRenames.put(newVar, oldVar);
						newVars.append(Formula.SPACE).append(newVar);
						oldVarsF = oldVarsF.cdrAsFormula();
					}
					newVars = new StringBuilder((Formula.LP + newVars.toString().trim() + Formula.RP));
					String arg2 = formula.caddr();
					Formula arg2F = new Formula();
					arg2F.set(arg2);
					String newArg2 = Clausifier.renameVariables(arg2F, topLevelVars, newScopedRenames, allRenames).text;
					String newFStr = (Formula.LP + arg0 + Formula.SPACE + newVars + Formula.SPACE + newArg2 + Formula.RP);
					Formula newF = new Formula();
					newF.set(newFStr);
					return newF;
				}
				Formula arg0F = new Formula();
				arg0F.set(arg0);
				String newArg0 = Clausifier.renameVariables(arg0F, topLevelVars, scopedRenames, allRenames).text;
				String newRest = Clausifier.renameVariables(formula.cdrAsFormula(), topLevelVars, scopedRenames, allRenames).text;
				Formula newRestF = new Formula();
				newRestF.set(newRest);
				String newFStr = newRestF.cons(newArg0).text;
				Formula newF = new Formula();
				newF.set(newFStr);
				return newF;
			}
			if (Formula.isVariable(formula.text))
			{
				String rnv = scopedRenames.get(formula.text);
				if (!isNonEmpty(rnv))
				{
					rnv = topLevelVars.get(formula.text);
					if (!isNonEmpty(rnv))
					{
						rnv = newVar();
						topLevelVars.put(formula.text, rnv);
						allRenames.put(rnv, formula.text);
					}
				}
				Formula newF = new Formula();
				newF.set(rnv);
				return newF;
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		return formula;
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
	private static String newSkolemTerm(SortedSet<String> vars)
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
			sb.append(idx);
		return sb.toString();
	}

	/**
	 * This method returns a new Formula in which all existentially
	 * quantified variables have been replaced by Skolem terms.
	 *
	 * @return A new SUO-KIF Formula without existentially quantified
	 * variables.
	 */
	private Formula existentialsOut()
	{
		// Existentially quantified variable substitution pairs: var -> skolem term.
		Map<String, String> evSubs = new HashMap<>();

		// Implicitly universally quantified variables.
		SortedSet<String> iUQVs = new TreeSet<>();

		// Explicitly quantified variables.
		SortedSet<String> scopedVars = new TreeSet<>();

		// Explicitly universally quantified variables.
		SortedSet<String> scopedUQVs = new TreeSet<>();

		// Collect the implicitly universally qualified variables from the Formula.
		collectIUQVars(iUQVs, scopedVars);

		// Do the recursive term replacement, and return the results.
		return existentialsOut(evSubs, iUQVs, scopedUQVs);
	}

	/**
	 * This method returns a new Formula in which all existentially
	 * quantified variables have been replaced by Skolem terms.
	 *
	 * @param evSubs     A Map of variable - skolem term substitution
	 *                   pairs.
	 * @param iUQVs      A SortedSet of implicitly universally quantified
	 *                   variables.
	 * @param scopedUQVs A SortedSet of explicitly universally
	 *                   quantified variables.
	 * @return A new SUO-KIF Formula without existentially quantified
	 * variables.
	 */
	private Formula existentialsOut(Map<String, String> evSubs, SortedSet<String> iUQVs, SortedSet<String> scopedUQVs)
	{
		try
		{
			if (formula.listP())
			{
				if (formula.empty())
				{
					return formula;
				}
				String arg0 = formula.car();
				if (arg0.equals(Formula.UQUANT))
				{
					// Copy the scoped variables set to protect variable scope as we descend below this quantifier.
					SortedSet<String> newScopedUQVs = new TreeSet<>(scopedUQVs);
					String varList = formula.cadr();
					Formula varListF = new Formula();
					varListF.set(varList);
					while (!(varListF.empty()))
					{
						String var = varListF.car();
						newScopedUQVs.add(var);
						varListF.set(varListF.cdr());
					}
					String arg2 = formula.caddr();
					Formula arg2F = new Formula();
					arg2F.set(arg2);
					String newStr = ("(forall " + varList + " " + existentialsOut(arg2F, evSubs, iUQVs, newScopedUQVs).text + ")");
					formula.set(newStr);
					return formula;
				}
				if (arg0.equals(Formula.EQUANT))
				{
					// Collect the relevant universally quantified variables.
					SortedSet<String> uQVs = new TreeSet<>(iUQVs);
					uQVs.addAll(scopedUQVs);
					// Collect the existentially quantified variables.
					List<String> eQVs = new ArrayList<>();
					String varList = formula.cadr();
					Formula varListF = new Formula();
					varListF.set(varList);
					while (!(varListF.empty()))
					{
						String var = varListF.car();
						eQVs.add(var);
						varListF.set(varListF.cdr());
					}
					// For each existentially quantified variable, create a corresponding skolem term, and store the pair in the evSubs map.
					for (String var : eQVs)
					{
						String skTerm = newSkolemTerm(uQVs);
						evSubs.put(var, skTerm);
					}
					String arg2 = formula.caddr();
					Formula arg2F = new Formula();
					arg2F.set(arg2);
					return existentialsOut(arg2F, evSubs, iUQVs, scopedUQVs);
				}
				Formula arg0F = new Formula();
				arg0F.set(arg0);
				String newArg0 = existentialsOut(arg0F, evSubs, iUQVs, scopedUQVs).text;
				return existentialsOut(formula.cdrAsFormula(), evSubs, iUQVs, scopedUQVs).cons(newArg0);
			}
			if (Formula.isVariable(formula.text))
			{
				String newTerm = evSubs.get(formula.text);
				if (isNonEmpty(newTerm))
					formula.set(newTerm);
				return formula;
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		return formula;
	}

	/**
	 * Convenience method
	 */
	private static Formula existentialsOut(Formula f, Map<String, String> evSubs, SortedSet<String> iUQVs, SortedSet<String> scopedUQVs)
	{
		Clausifier clausifier = new Clausifier(f.text);
		return clausifier.existentialsOut(evSubs, iUQVs, scopedUQVs);
	}

	/**
	 * This method collects all variables in Formula that appear to be
	 * only implicitly universally quantified and adds them to the
	 * SortedSet iuqvs.  Note the iuqvs must be passed in.
	 *
	 * @param iuqvs      A SortedSet for accumulating variables that appear
	 *                   to be implicitly universally quantified.
	 * @param scopedVars A SortedSet containing explicitly quantified
	 *                   variables.
	 */
	private void collectIUQVars(SortedSet<String> iuqvs, SortedSet<String> scopedVars)
	{
		try
		{
			if (formula.listP() && !formula.empty())
			{
				String arg0 = formula.car();
				if (Formula.isQuantifier(arg0))
				{
					// Copy the scopedVars set to protect variable  scope as we descend below this quantifier.
					SortedSet<String> newScopedVars = new TreeSet<>(scopedVars);

					String varList = formula.cadr();
					Formula varListF = new Formula();
					varListF.set(varList);
					while (!(varListF.empty()))
					{
						String var = varListF.car();
						newScopedVars.add(var);
						varListF = varListF.cdrAsFormula();
					}
					String arg2 = formula.caddr();
					Formula arg2F = new Formula();
					arg2F.set(arg2);
					collectIUQVars(arg2F, iuqvs, newScopedVars);
				}
				else
				{
					Formula arg0F = new Formula();
					arg0F.set(arg0);
					collectIUQVars(arg0F, iuqvs, scopedVars);
					collectIUQVars(formula.cdrAsFormula(), iuqvs, scopedVars);
				}
			}
			else if (Formula.isVariable(formula.text) && !(scopedVars.contains(formula.text)))
			{
				iuqvs.add(formula.text);
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}

	/**
	 * Convenience method
	 */
	private static void collectIUQVars(Formula f, SortedSet<String> iuqvs, SortedSet<String> scopedVars)
	{
		Clausifier temp = new Clausifier(f.text);
		temp.collectIUQVars(iuqvs, scopedVars);
	}

	/**
	 * This method returns a new Formula in which explicit universal
	 * quantifiers have been removed.
	 *
	 * @return A new SUO-KIF Formula without explicit universal
	 * quantifiers.
	 */
	private Formula universalsOut()
	{
		try
		{
			if (formula.listP())
			{
				if (formula.empty())
				{
					return formula;
				}
				String arg0 = formula.car();
				if (arg0.equals(Formula.UQUANT))
				{
					String arg2 = formula.caddr();
					formula.set(arg2);
					return universalsOut(formula);
				}
				Formula arg0F = new Formula();
				arg0F.set(arg0);
				String newArg0 = universalsOut(arg0F).text;
				return universalsOut(formula.cdrAsFormula()).cons(newArg0);
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		return formula;
	}

	/**
	 * Convenience method
	 */
	private static Formula universalsOut(Formula f)
	{
		Clausifier temp = new Clausifier(f.text);
		return temp.universalsOut();
	}

	/**
	 * This method returns a new Formula in which nested 'and', 'or',
	 * and 'not' operators have been unnested:
	 * (not (not <literal> ...)) -> <literal>
	 * (and (and <literal-sequence> ...)) -> (and <literal-sequence> ...)
	 * (or (or <literal-sequence> ...)) -> (or <literal-sequence> ...)
	 *
	 * @return A new SUO-KIF Formula in which nested commutative
	 * operators and 'not' have been unnested.
	 */
	private Formula nestedOperatorsOut()
	{
		Formula f = formula;
		Formula result = nestedOperatorsOut_1();

		// Here we repeatedly apply nestedOperatorsOut_1() until there are no more changes.
		while (!f.text.equals(result.text))
		{
			f = result;
			result = nestedOperatorsOut_1(f);
		}
		return result;
	}

	/**
	 * convenience method
	 */
	private static Formula nestedOperatorsOut(Formula f)
	{
		Clausifier temp = new Clausifier(f.text);
		return temp.nestedOperatorsOut();
	}

	/**
	 * @return A new SUO-KIF Formula in which nested commutative
	 * operators and 'not' have been unnested.
	 */
	private Formula nestedOperatorsOut_1()
	{
		try
		{
			if (formula.listP())
			{
				if (formula.empty())
				{
					return formula;
				}
				String arg0 = formula.car();
				if (Formula.isCommutative(arg0) || arg0.equals(Formula.NOT))
				{
					List<String> literals = new ArrayList<>();
					Formula restF = formula.cdrAsFormula();
					while (!(restF.empty()))
					{
						String lit = restF.car();
						Formula litF = new Formula();
						litF.set(lit);
						if (litF.listP())
						{
							String litFArg0 = litF.car();
							if (litFArg0.equals(arg0))
							{
								if (arg0.equals(Formula.NOT))
								{
									String newFStr = litF.cadr();
									Formula newF = new Formula();
									newF.set(newFStr);
									return nestedOperatorsOut_1(newF);
								}
								Formula rest2F = litF.cdrAsFormula();
								while (!(rest2F.empty()))
								{
									String rest2arg0 = rest2F.car();
									Formula rest2arg0F = new Formula();
									rest2arg0F.set(rest2arg0);
									literals.add(nestedOperatorsOut_1(rest2arg0F).text);
									rest2F = rest2F.cdrAsFormula();
								}
							}
							else
								literals.add(nestedOperatorsOut_1(litF).text);
						}
						else
							literals.add(lit);
						restF = restF.cdrAsFormula();
					}
					StringBuilder sb = new StringBuilder((Formula.LP + arg0));
					for (String literal : literals)
						sb.append(Formula.SPACE).append(literal);
					sb.append(Formula.RP);
					Formula newF = new Formula();
					newF.set(sb.toString());
					return newF;
				}
				Formula arg0F = new Formula();
				arg0F.set(arg0);
				String newArg0 = nestedOperatorsOut_1(arg0F).text;
				return nestedOperatorsOut_1(formula.cdrAsFormula()).cons(newArg0);
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		return formula;
	}

	/**
	 * convenience method
	 */
	private static Formula nestedOperatorsOut_1(Formula f)
	{
		Clausifier temp = new Clausifier(f.text);
		return temp.nestedOperatorsOut_1();
	}

	/**
	 * This method returns a new Formula in which all occurrences of
	 * 'or' have been accorded the least possible scope.
	 * (or P (and Q R)) -> (and (or P Q) (or P R))
	 *
	 * @return A new SUO-KIF Formula in which occurrences of 'or' have
	 * been 'moved in' as far as possible.
	 */
	private Formula disjunctionsIn()
	{
		Formula f = formula;
		Formula result = disjunctionsIn_1(nestedOperatorsOut());

		// Here we repeatedly apply disjunctionIn_1() until there are no more changes.
		while (!f.text.equals(result.text))
		{
			f = result;
			result = disjunctionsIn_1(nestedOperatorsOut(f));
		}
		return result;
	}

	/**
	 * @return A new SUO-KIF Formula in which occurrences of 'or' have
	 * been 'moved in' as far as possible.
	 */
	private Formula disjunctionsIn_1()
	{
		try
		{
			if (formula.listP())
			{
				if (formula.empty())
				{
					return formula;
				}
				String arg0 = formula.car();
				if (arg0.equals(Formula.OR))
				{
					List<String> disjuncts = new ArrayList<>();
					List<String> conjuncts = new ArrayList<>();
					Formula restF = formula.cdrAsFormula();
					while (!(restF.empty()))
					{
						String disjunct = restF.car();
						Formula disjunctF = new Formula();
						disjunctF.set(disjunct);
						if (disjunctF.listP() && disjunctF.car().equals(Formula.AND) && conjuncts.isEmpty())
						{
							Formula rest2F = disjunctionsIn_1(disjunctF.cdrAsFormula());
							while (!(rest2F.empty()))
							{
								conjuncts.add(rest2F.car());
								rest2F = rest2F.cdrAsFormula();
							}
						}
						else
							disjuncts.add(disjunct);
						restF = restF.cdrAsFormula();
					}

					if (conjuncts.isEmpty())
					{
						return formula;
					}

					Formula resultF = new Formula();
					resultF.set("()");
					StringBuilder disjunctsString = new StringBuilder();
					for (String disjunct : disjuncts)
						disjunctsString.append(Formula.SPACE).append(disjunct);
					disjunctsString = new StringBuilder((Formula.LP + disjunctsString.toString().trim() + Formula.RP));
					Formula disjunctsF = new Formula();
					disjunctsF.set(disjunctsString.toString());
					for (String conjunct : conjuncts)
					{
						String newDisjuncts = disjunctionsIn_1(disjunctsF.cons(conjunct).cons(Formula.OR)).text;
						resultF = resultF.cons(newDisjuncts);
					}
					resultF = resultF.cons(Formula.AND);
					return resultF;
				}
				Formula arg0F = new Formula();
				arg0F.set(arg0);
				String newArg0 = disjunctionsIn_1(arg0F).text;
				return disjunctionsIn_1(formula.cdrAsFormula()).cons(newArg0);
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		return formula;
	}

	/**
	 * convenience method
	 */
	private static Formula disjunctionsIn_1(Formula f)
	{
		Clausifier temp = new Clausifier(f.text);
		return temp.disjunctionsIn_1();
	}

	/**
	 * This method returns an List of clauses.  Each clause is a
	 * LISP list (really, a Formula) containing one or more Formulas.
	 * The LISP list is assumed to be a disjunction, but there is no
	 * 'or' at the head.
	 *
	 * @return An List of LISP lists, each of which contains one
	 * or more Formulas.
	 */
	private List<Formula> operatorsOut()
	{
		List<Formula> result = new ArrayList<>();
		try
		{
			List<Formula> clauses = new ArrayList<>();
			if (isNonEmpty(formula.text))
			{
				if (formula.listP())
				{
					String arg0 = formula.car();
					if (arg0.equals(Formula.AND))
					{
						Formula restF = formula.cdrAsFormula();
						while (!(restF.empty()))
						{
							String fStr = restF.car();
							Formula newF = new Formula();
							newF.set(fStr);
							clauses.add(newF);
							restF = restF.cdrAsFormula();
						}
					}
				}
				if (clauses.isEmpty())
					clauses.add(formula);
				for (Formula f : clauses)
				{
					Formula clauseF = new Formula();
					clauseF.set("()");
					if (f.listP())
					{
						if (f.car().equals(Formula.OR))
						{
							f = f.cdrAsFormula();
							while (!(f.empty()))
							{
								String lit = f.car();
								clauseF = clauseF.cons(lit);
								f = f.cdrAsFormula();
							}
						}
					}
					if (clauseF.empty())
						clauseF = clauseF.cons(f.text);
					result.add(clauseF);
				}
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		return result;
	}

	/**
	 * This method returns a Formula in which variables for separate
	 * clauses have been 'standardized apart'.
	 *
	 * @param renameMap A Map for capturing one-to-one variable rename
	 *                  correspondences.  Keys are new variables.  Values are old
	 *                  variables.
	 * @return A Formula.
	 */
	private Formula standardizeApart(Map<String, String> renameMap)
	{
		Formula result = formula;
		try
		{
			Map<String, String> reverseRenames;
			if (renameMap != null)
				reverseRenames = renameMap;
			else
				reverseRenames = new HashMap<>();

			// First, break the Formula into separate clauses, if necessary.
			List<Formula> clauses = new ArrayList<>();
			if (isNonEmpty(formula.text))
			{
				if (formula.listP())
				{
					String arg0 = formula.car();
					if (arg0.equals(Formula.AND))
					{
						Formula restF = formula.cdrAsFormula();
						while (!(restF.empty()))
						{
							String fStr = restF.car();
							Formula newF = new Formula();
							newF.set(fStr);
							clauses.add(newF);
							restF = restF.cdrAsFormula();
						}
					}
				}
				if (clauses.isEmpty())
					clauses.add(formula);
				// 'Standardize apart' by renaming the variables in each clause.
				int n = clauses.size();
				for (int i = 0; i < n; i++)
				{
					Map<String, String> renames = new HashMap<>();
					Formula oldClause = clauses.remove(0);
					clauses.add(standardizeApart_1(oldClause, renames, reverseRenames));
				}

				// Construct the new Formula to return.
				if (n > 1)
				{
					StringBuilder newFStr = new StringBuilder("(and");
					for (Formula f : clauses)
					{
						newFStr.append(Formula.SPACE).append(f.text);
					}
					newFStr.append(Formula.RP);
					Formula newF = new Formula();
					newF.set(newFStr.toString());
					result = newF;
				}
				else
					result = clauses.get(0);
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		return result;
	}

	/**
	 * This is a helper method for standardizeApart(renameMap).  It
	 * assumes that the Formula will be a single clause.
	 *
	 * @param renames        A Map of correspondences between old variables
	 *                       and new variables.
	 * @param reverseRenames A Map of correspondences between new
	 *                       variables and old variables.
	 * @return A Formula
	 */
	private Formula standardizeApart_1(Map<String, String> renames, Map<String, String> reverseRenames)
	{
		Formula result = formula;
		if (formula.listP() && !(formula.empty()))
		{
			String arg0 = formula.car();
			Formula arg0F = new Formula();
			arg0F.set(arg0);
			arg0F = standardizeApart_1(arg0F, renames, reverseRenames);
			result = standardizeApart_1(formula.cdrAsFormula(), renames, reverseRenames).cons(arg0F.text);
		}
		else if (Formula.isVariable(formula.text))
		{
			String rnv = renames.get(formula.text);
			if (!isNonEmpty(rnv))
			{
				rnv = newVar();
				renames.put(formula.text, rnv);
				reverseRenames.put(rnv, formula.text);
			}
			Formula rnvF = new Formula();
			rnvF.set(rnv);
			result = rnvF;
		}
		return result;
	}

	/**
	 * Convenience method
	 */
	private static Formula standardizeApart_1(Formula f, Map<String, String> renames, Map<String, String> reverseRenames)
	{
		Clausifier clausifier = new Clausifier(f.text);
		return clausifier.standardizeApart_1(renames, reverseRenames);
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
	public static String getOriginalVar(String var, Map<String, String> varMap)
	{
		String result = null;
		if (isNonEmpty(var) && (varMap != null))
		{
			result = var;
			String next = varMap.get(result);
			while (!((next == null) || next.equals(result)))
			{
				result = next;
				next = varMap.get(result);
			}
		}
		return result;
	}

	private static boolean isNonEmpty(String str)
	{
		return str != null && !str.isEmpty();
	}
}


