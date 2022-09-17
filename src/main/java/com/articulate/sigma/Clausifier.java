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
	@NotNull
	private Formula formula;

	/**
	 * Constructor
	 *
	 * @param form formula string
	 */
	public Clausifier(@NotNull final String form)
	{
		formula = new Formula(form);
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
	 * // 2. a Map of variable renamings,
	 * // 3. the original Formula,
	 * ]
	 */
	@NotNull
	public static Tuple.Triple<List<Clause>, Map<String, String>, Formula> clausify(@NotNull final Formula f)
	{
		return new Clausifier(f.form).clausify();
	}

	/**
	 * This method converts the SUO-KIF Formula to a List of
	 * clauses.  Each clause is a List containing a List
	 * of negative literals, and a List of positive literals.
	 * Either the neg lits list or the pos lits list could be empty.
	 * Each literal is a Formula object.
	 * The first object in the returned triplet is a List of
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
	 * // 2. a Map of variable renamings,
	 * // 3. the original Formula,
	 * ]
	 */
	@NotNull
	private Tuple.Triple<List<Clause>, Map<String, String>, Formula> clausify()
	{
		Tuple.Triple<List<Clause>, Map<String, String>, Formula> result = new Tuple.Triple<>();
		try
		{
			Tuple.Triple<Formula, Map<String, String>, Formula> cff = clausalForm();
			Formula clausalForm = cff.first;
			assert clausalForm != null;

			List<Formula> clauses = new Clausifier(clausalForm.form).operatorsOut();
			if (!clauses.isEmpty())
			{
				List<Clause> newClauses = new ArrayList<>();
				for (Formula clause : clauses)
				{
					Clause literals = new Clause();
					if (clause.listP())
					{
						while (clause != null && !clause.empty())
						{
							boolean isNegLit = false;
							String lit = clause.car();
							Formula litF = new Formula(lit);
							if (litF.listP() && litF.car().equals(Formula.NOT))
							{
								litF.set(litF.cadr());
								isNegLit = true;
							}
							if (litF.form.equals(Formula.LOGICAL_FALSE))
							{
								isNegLit = true;
							}
							if (isNegLit)
							{
								literals.negativeLits.add(litF);
							}
							else
							{
								literals.positiveLits.add(litF);
							}
							clause = clause.cdrAsFormula();
						}
					}
					else if (clause.form.equals(Formula.LOGICAL_FALSE))
					{
						literals.negativeLits.add(clause);
					}
					else
					{
						literals.positiveLits.add(clause);
					}
					newClauses.add(literals);
				}
				// Collections.sort(negLits);
				// Collections.sort(posLits);
				result.first = newClauses;
			}
			result.second = cff.second;
			result.third = cff.third;
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		return result;
	}

	/**
	 * Clausal form
	 *
	 * @return a List that contains three items:
	 * 1- The new clausal-form Formula,
	 * 2- A Map containing a graph of all the variable substitutions done
	 * during the conversion to clausal form.
	 * 3- and the original (input) SUO-KIF Formula, and
	 * <p>
	 * This Map makes it possible to retrieve the correspondence between the variables
	 * in the clausal form and the variables in the original
	 * Formula. Some elements might be null if a clausal form
	 * cannot be generated.
	 */
	@NotNull
	public Tuple.Triple<Formula, Map<String, String>, Formula> clausalForm()
	{
		Formula oldF = new Formula(formula.form);

		Tuple.Triple<Formula, Map<String, String>, Formula> result = new Tuple.Triple<>();
		try
		{
			formula = equivalencesOut();
			formula = implicationsOut();
			formula = negationsIn();
			Map<String, String> topLevelVars = new HashMap<>();
			Map<String, String> scopedRenames = new HashMap<>();
			Map<String, String> allRenames = new HashMap<>();
			formula = renameVariables(topLevelVars, scopedRenames, allRenames);
			formula = existentialsOut();
			formula = universalsOut();
			formula = disjunctionsIn();

			Map<String, String> standardizedRenames = new HashMap<>();
			formula = standardizeApart(standardizedRenames);
			allRenames.putAll(standardizedRenames);

			result.first = formula;
			result.second = allRenames;
			result.third = oldF;
			// resetClausifyIndices();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		return result;
	}

	// I M P L I C A T I O N

	/**
	 * This method converts every occurrence of '<=>' in the Formula
	 * to a conjunct with two occurrences of '=>'.
	 *
	 * @return A Formula with no occurrences of '<=>'.
	 */
	@NotNull
	private Formula equivalencesOut()
	{
		Formula result = formula;
		if (formula.listP() && !(formula.empty()))
		{
			String head = formula.car();
			String newForm;
			if (Variables.isNonEmpty(head) && Formula.listP(head))
			{
				Clausifier headF = new Clausifier(head);
				String newHead = headF.equivalencesOut().form;
				Clausifier clausifier = new Clausifier(formula.cdr());
				newForm = clausifier.equivalencesOut().cons(newHead).form;
			}
			else if (head.equals(Formula.IFF))
			{
				String second = formula.cadr();
				Clausifier secondF = new Clausifier(second);
				String newSecond = secondF.equivalencesOut().form;
				String third = formula.caddr();
				Clausifier thirdF = new Clausifier(third);
				String newThird = thirdF.equivalencesOut().form;
				newForm = ("(and (=> " + newSecond + " " + newThird + ") (=> " + newThird + " " + newSecond + "))");
			}
			else
			{
				Clausifier fourth = new Clausifier(formula.cdrAsFormula().form);
				newForm = fourth.equivalencesOut().cons(head).form;
			}
			result = new Formula(newForm);
		}
		return result;
	}

	/**
	 * This method converts every occurrence of "(=> LHS RHS)" in the
	 * Formula to a disjunct of the form "(or (not LHS) RHS)".
	 *
	 * @return A Formula with no occurrences of '=>'.
	 */
	@NotNull
	private Formula implicationsOut()
	{
		Formula result = formula;
		String newForm;
		if (formula.listP() && !formula.empty())
		{
			String head = formula.car();
			if (Variables.isNonEmpty(head) && Formula.listP(head))
			{
				Clausifier headF = new Clausifier(head);
				String newHead = headF.implicationsOut().form;
				Clausifier clausifier = new Clausifier(formula.cdr());
				newForm = clausifier.implicationsOut().cons(newHead).form;
			}
			else if (head.equals(Formula.IF))
			{
				String second = formula.cadr();
				Clausifier secondF = new Clausifier(second);
				String newSecond = secondF.implicationsOut().form;
				String third = formula.caddr();
				Clausifier thirdF = new Clausifier(third);
				String newThird = thirdF.implicationsOut().form;
				newForm = "(or (not " + newSecond + ") " + newThird + ")";
			}
			else
			{
				Clausifier fourth = new Clausifier(formula.cdrAsFormula().form);
				newForm = fourth.implicationsOut().cons(head).form;
			}
			result = new Formula(newForm);
		}
		return result;
	}

	// N E G A T I O N

	/**
	 * This method 'pushes in' all occurrences of 'not', so that each
	 * occurrence has the narrowest possible scope, and also removes
	 * from the Formula all occurrences of '(not (not ...))'.
	 *
	 * @return A Formula with all occurrences of 'not' accorded
	 * narrowest scope, and no occurrences of '(not (not ...))'.
	 */
	@NotNull
	private Formula negationsIn()
	{
		Formula f = formula;
		Formula result = negationsIn_1();
		// Here we repeatedly apply negationsIn_1() until there are no more changes.
		while (!f.form.equals(result.form))
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
	@NotNull
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
					Formula arg1F = new Formula(arg1);
					String arg0_of_arg1 = arg1F.car();
					if (arg0_of_arg1.equals(Formula.NOT))
					{
						String arg1_of_arg1 = arg1F.cadr();
						return new Formula(arg1_of_arg1);
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
						arg2_of_arg1 = "(not " + arg2_of_arg1 + ")";
						Formula arg2_of_arg1F = new Formula(arg2_of_arg1);
						String newForm = "(" + quant + " " + vars + " " + negationsIn_1(arg2_of_arg1F).form + ")";
						return new Formula(newForm);
					}
					String newForm = ("(not " + negationsIn_1(arg1F).form + ")");
					return new Formula(newForm);
				}
				if (Formula.isQuantifier(arg0))
				{
					String arg2 = formula.caddr();
					Formula arg2F = new Formula(arg2);
					String newArg2 = negationsIn_1(arg2F).form;
					return new Formula("(" + arg0 + " " + arg1 + " " + newArg2 + ")");
				}
				if (Formula.listP(arg0))
				{
					Formula arg0F = new Formula(arg0);
					return negationsIn_1(formula.cdrAsFormula()).cons(negationsIn_1(arg0F).form);
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
	 * Convenience method
	 */
	private static Formula negationsIn_1(@NotNull Formula f)
	{
		return new Clausifier(f.form).negationsIn_1();
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
	@NotNull
	private Formula listAll(String before, @NotNull String after)
	{
		Formula resultF = formula;
		if (formula.listP())
		{
			StringBuilder sb = new StringBuilder();
			Formula f = formula;
			while (!(f.empty()))
			{
				String element = f.car();
				if (Variables.isNonEmpty(before))
				{
					element = (before + element);
				}
				if (Variables.isNonEmpty(after))
				{
					element += after;
				}
				sb.append(Formula.SPACE).append(element);
				f = f.cdrAsFormula();
			}
			sb = new StringBuilder((Formula.LP + sb.toString().trim() + Formula.RP));
			if (Variables.isNonEmpty(sb.toString()))
			{
				resultF = new Formula(sb.toString());
			}
		}
		return resultF;
	}

	/**
	 * Convenience method
	 */
	@NotNull
	private static Formula listAll(@NotNull Formula f, @SuppressWarnings("SameParameterValue") String before, @NotNull @SuppressWarnings("SameParameterValue") String after)
	{
		return new Clausifier(f.form).listAll(before, after);
	}

	/**
	 * Convenience method to rename variable
	 *
	 * @param f             formula
	 * @param topLevelVars  A Map that is used to track renames of implicitly universally quantified variables.
	 * @param scopedRenames A Map that is used to track renames of explicitly quantified variables.
	 * @param allRenames    A Map from all new vars in the Formula to their old counterparts.
	 * @return A new SUO-KIF Formula with all variables renamed.
	 */
	public static Formula renameVariables(@NotNull Formula f, @NotNull Map<String, String> topLevelVars, @NotNull Map<String, String> scopedRenames, @NotNull Map<String, String> allRenames)
	{
		return new Clausifier(f.form).renameVariables(topLevelVars, scopedRenames, allRenames);
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
	@NotNull
	private Formula renameVariables(@NotNull Map<String, String> topLevelVars, @NotNull Map<String, String> scopedRenames, @NotNull Map<String, String> allRenames)
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
					Formula oldVarsF = new Formula(oldVars);
					StringBuilder newVars = new StringBuilder();
					while (!oldVarsF.empty())
					{
						String oldVar = oldVarsF.car();
						String newVar = Variables.newVar();
						newScopedRenames.put(oldVar, newVar);
						allRenames.put(newVar, oldVar);
						newVars.append(Formula.SPACE).append(newVar);
						oldVarsF = oldVarsF.cdrAsFormula();
					}
					newVars = new StringBuilder((Formula.LP + newVars.toString().trim() + Formula.RP));
					String arg2 = formula.caddr();
					Formula arg2F = new Formula(arg2);
					String newArg2 = Clausifier.renameVariables(arg2F, topLevelVars, newScopedRenames, allRenames).form;
					String newForm = Formula.LP + arg0 + Formula.SPACE + newVars + Formula.SPACE + newArg2 + Formula.RP;
					return new Formula(newForm);
				}
				Formula arg0F = new Formula(arg0);
				String newArg0 = Clausifier.renameVariables(arg0F, topLevelVars, scopedRenames, allRenames).form;
				String newRest = Clausifier.renameVariables(formula.cdrAsFormula(), topLevelVars, scopedRenames, allRenames).form;
				Formula newRestF = new Formula(newRest);
				String newForm = newRestF.cons(newArg0).form;
				return new Formula(newForm);
			}
			if (Formula.isVariable(formula.form))
			{
				String rnv = scopedRenames.get(formula.form);
				if (!Variables.isNonEmpty(rnv))
				{
					rnv = topLevelVars.get(formula.form);
					if (!Variables.isNonEmpty(rnv))
					{
						rnv = Variables.newVar();
						topLevelVars.put(formula.form, rnv);
						allRenames.put(rnv, formula.form);
					}
				}
				return new Formula(rnv);
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		return formula;
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
	private Formula existentialsOut(@NotNull Map<String, String> evSubs, @NotNull SortedSet<String> iUQVs, @NotNull SortedSet<String> scopedUQVs)
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
					MutableFormula varListF = new MutableFormula(varList);
					while (!(varListF.empty()))
					{
						String var = varListF.car();
						newScopedUQVs.add(var);
						varListF.pop();
					}
					String arg2 = formula.caddr();
					Formula arg2F = new Formula(arg2);
					String newForm = "(forall " + varList + " " + existentialsOut(arg2F, evSubs, iUQVs, newScopedUQVs).form + ")";
					formula.set(newForm);
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
					MutableFormula varListF = new MutableFormula(varList);
					while (!(varListF.empty()))
					{
						String var = varListF.car();
						eQVs.add(var);
						varListF.pop();
					}
					// For each existentially quantified variable, create a corresponding skolem term, and store the pair in the evSubs map.
					for (String var : eQVs)
					{
						String skTerm = Variables.newSkolemTerm(uQVs);
						evSubs.put(var, skTerm);
					}
					String arg2 = formula.caddr();
					Formula arg2F = new Formula(arg2);
					return existentialsOut(arg2F, evSubs, iUQVs, scopedUQVs);
				}
				Formula arg0F = new Formula(arg0);
				String newArg0 = existentialsOut(arg0F, evSubs, iUQVs, scopedUQVs).form;
				return existentialsOut(formula.cdrAsFormula(), evSubs, iUQVs, scopedUQVs).cons(newArg0);
			}
			if (Formula.isVariable(formula.form))
			{
				String newTerm = evSubs.get(formula.form);
				if (Variables.isNonEmpty(newTerm))
				{
					formula.set(newTerm);
				}
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
	private static Formula existentialsOut(@NotNull Formula f, @NotNull Map<String, String> evSubs, @NotNull SortedSet<String> iUQVs, @NotNull SortedSet<String> scopedUQVs)
	{
		Clausifier clausifier = new Clausifier(f.form);
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
	private void collectIUQVars(@NotNull SortedSet<String> iuqvs, @NotNull SortedSet<String> scopedVars)
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
					Formula varListF = new Formula(varList);
					while (!(varListF.empty()))
					{
						String var = varListF.car();
						newScopedVars.add(var);
						varListF = varListF.cdrAsFormula();
					}
					String arg2 = formula.caddr();
					Formula arg2F = new Formula(arg2);
					collectIUQVars(arg2F, iuqvs, newScopedVars);
				}
				else
				{
					Formula arg0F = new Formula(arg0);
					collectIUQVars(arg0F, iuqvs, scopedVars);
					collectIUQVars(formula.cdrAsFormula(), iuqvs, scopedVars);
				}
			}
			else if (Formula.isVariable(formula.form) && !(scopedVars.contains(formula.form)))
			{
				iuqvs.add(formula.form);
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
	private static void collectIUQVars(@NotNull Formula f, @NotNull SortedSet<String> iuqvs, @NotNull SortedSet<String> scopedVars)
	{
		Clausifier temp = new Clausifier(f.form);
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
				Formula arg0F = new Formula(arg0);
				String newArg0 = universalsOut(arg0F).form;
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
	private static Formula universalsOut(@NotNull Formula f)
	{
		Clausifier temp = new Clausifier(f.form);
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
	@NotNull
	private Formula nestedOperatorsOut()
	{
		Formula f = formula;
		Formula result = nestedOperatorsOut_1();

		// Here we repeatedly apply nestedOperatorsOut_1() until there are no more changes.
		while (!f.form.equals(result.form))
		{
			f = result;
			result = nestedOperatorsOut_1(f);
		}
		return result;
	}

	/**
	 * convenience method
	 */
	@NotNull
	private static Formula nestedOperatorsOut(@NotNull Formula f)
	{
		Clausifier temp = new Clausifier(f.form);
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
						Formula litF = new Formula(lit);
						if (litF.listP())
						{
							String litFArg0 = litF.car();
							if (litFArg0.equals(arg0))
							{
								if (arg0.equals(Formula.NOT))
								{
									String newForm = litF.cadr();
									Formula newF = new Formula(newForm);
									return nestedOperatorsOut_1(newF);
								}
								Formula rest2F = litF.cdrAsFormula();
								while (!(rest2F.empty()))
								{
									String rest2arg0 = rest2F.car();
									Formula rest2arg0F = new Formula(rest2arg0);
									literals.add(nestedOperatorsOut_1(rest2arg0F).form);
									rest2F = rest2F.cdrAsFormula();
								}
							}
							else
							{
								literals.add(nestedOperatorsOut_1(litF).form);
							}
						}
						else
						{
							literals.add(lit);
						}
						restF = restF.cdrAsFormula();
					}
					StringBuilder sb = new StringBuilder((Formula.LP + arg0));
					for (String literal : literals)
					{
						sb.append(Formula.SPACE).append(literal);
					}
					sb.append(Formula.RP);
					return new Formula(sb.toString());
				}
				Formula arg0F = new Formula(arg0);
				String newArg0 = nestedOperatorsOut_1(arg0F).form;
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
	private static Formula nestedOperatorsOut_1(@NotNull Formula f)
	{
		Clausifier temp = new Clausifier(f.form);
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
	@NotNull
	private Formula disjunctionsIn()
	{
		Formula f = formula;
		Formula result = disjunctionsIn_1(nestedOperatorsOut());

		// Here we repeatedly apply disjunctionIn_1() until there are no more changes.
		while (!f.form.equals(result.form))
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
	@NotNull
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
						Formula disjunctF = new Formula(disjunct);
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
						{
							disjuncts.add(disjunct);
						}
						restF = restF.cdrAsFormula();
					}

					if (conjuncts.isEmpty())
					{
						return formula;
					}

					Formula resultF = new Formula("()");
					StringBuilder disjunctsString = new StringBuilder();
					for (String disjunct : disjuncts)
					{
						disjunctsString.append(Formula.SPACE).append(disjunct);
					}
					disjunctsString = new StringBuilder((Formula.LP + disjunctsString.toString().trim() + Formula.RP));
					Formula disjunctsF = new Formula(disjunctsString.toString());
					for (String conjunct : conjuncts)
					{
						String newDisjuncts = disjunctionsIn_1(disjunctsF.cons(conjunct).cons(Formula.OR)).form;
						resultF = resultF.cons(newDisjuncts);
					}
					resultF = resultF.cons(Formula.AND);
					return resultF;
				}
				Formula arg0F = new Formula(arg0);
				String newArg0 = disjunctionsIn_1(arg0F).form;
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
	private static Formula disjunctionsIn_1(@NotNull Formula f)
	{
		Clausifier temp = new Clausifier(f.form);
		return temp.disjunctionsIn_1();
	}

	/**
	 * This method returns a List of clauses.  Each clause is a
	 * LISP list (really, a Formula) containing one or more Formulas.
	 * The LISP list is assumed to be a disjunction, but there is no
	 * 'or' at the head.
	 *
	 * @return A List of LISP lists, each of which contains one
	 * or more Formulas.
	 */
	@NotNull
	private List<Formula> operatorsOut()
	{
		List<Formula> result = new ArrayList<>();
		try
		{
			List<Formula> clauses = new ArrayList<>();
			if (Variables.isNonEmpty(formula.form))
			{
				if (formula.listP())
				{
					String arg0 = formula.car();
					if (arg0.equals(Formula.AND))
					{
						Formula restF = formula.cdrAsFormula();
						while (!(restF.empty()))
						{
							String newForm = restF.car();
							Formula newF = new Formula(newForm);
							clauses.add(newF);
							restF = restF.cdrAsFormula();
						}
					}
				}
				if (clauses.isEmpty())
				{
					clauses.add(formula);
				}
				for (Formula f : clauses)
				{
					Formula clauseF = new Formula("()");
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
					{
						clauseF = clauseF.cons(f.form);
					}
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
	@NotNull
	private Formula standardizeApart(@Nullable Map<String, String> renameMap)
	{
		Formula result = formula;
		try
		{
			Map<String, String> reverseRenames;
			if (renameMap != null)
			{
				reverseRenames = renameMap;
			}
			else
			{
				reverseRenames = new HashMap<>();
			}

			// First, break the Formula into separate clauses, if necessary.
			List<Formula> clauses = new ArrayList<>();
			if (Variables.isNonEmpty(formula.form))
			{
				if (formula.listP())
				{
					String arg0 = formula.car();
					if (arg0.equals(Formula.AND))
					{
						Formula restF = formula.cdrAsFormula();
						while (restF != null && !restF.empty())
						{
							String newForm = restF.car();
							Formula newF = new Formula(newForm);
							clauses.add(newF);
							restF = restF.cdrAsFormula();
						}
					}
				}
				if (clauses.isEmpty())
				{
					clauses.add(formula);
				}
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
					StringBuilder newForm = new StringBuilder("(and");
					for (Formula f : clauses)
					{
						newForm.append(Formula.SPACE).append(f.form);
					}
					newForm.append(Formula.RP);
					result = new Formula(newForm.toString());
				}
				else
				{
					result = clauses.get(0);
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
	 * This is a helper method for standardizeApart(renameMap).  It
	 * assumes that the Formula will be a single clause.
	 *
	 * @param renames        A Map of correspondences between old variables
	 *                       and new variables.
	 * @param reverseRenames A Map of correspondences between new
	 *                       variables and old variables.
	 * @return A Formula
	 */
	@NotNull
	private Formula standardizeApart_1(@NotNull Map<String, String> renames, @NotNull Map<String, String> reverseRenames)
	{
		Formula result = formula;
		if (formula.listP() && !(formula.empty()))
		{
			String arg0 = formula.car();
			Formula arg0F = new Formula(arg0);
			arg0F = standardizeApart_1(arg0F, renames, reverseRenames);
			result = standardizeApart_1(formula.cdrAsFormula(), renames, reverseRenames).cons(arg0F.form);
		}
		else if (Formula.isVariable(formula.form))
		{
			String rnv = renames.get(formula.form);
			if (!Variables.isNonEmpty(rnv))
			{
				rnv = Variables.newVar();
				renames.put(formula.form, rnv);
				reverseRenames.put(rnv, formula.form);
			}
			result = new Formula(rnv);
		}
		return result;
	}

	/**
	 * Convenience method
	 */
	@NotNull
	private static Formula standardizeApart_1(@NotNull Formula f, @NotNull Map<String, String> renames, @NotNull Map<String, String> reverseRenames)
	{
		Clausifier clausifier = new Clausifier(f.form);
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
	@Nullable
	public static String getOriginalVar(String var, @Nullable Map<String, String> varMap)
	{
		String result = null;
		if (Variables.isNonEmpty(var) && (varMap != null))
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

}


