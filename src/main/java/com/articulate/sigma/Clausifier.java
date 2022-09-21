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
		formula = Formula.of(form);
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
		@NotNull Tuple.Triple<List<Clause>, Map<String, String>, Formula> result = new Tuple.Triple<>();
		try
		{
			@NotNull Tuple.Triple<Formula, Map<String, String>, Formula> cff = clausalForm();
			@Nullable Formula clausalForm = cff.first;
			assert clausalForm != null;

			@NotNull List<Formula> clauses = new Clausifier(clausalForm.form).operatorsOut();
			if (!clauses.isEmpty())
			{
				@NotNull List<Clause> newClauses = new ArrayList<>();
				for (@NotNull final Formula clause : clauses)
				{
					@NotNull Clause literals = new Clause();
					if (clause.listP())
					{
						@Nullable Formula clause2 = clause;
						while (clause2 != null && !clause2.empty())
						{
							boolean isNegLit = false;
							@NotNull Formula litF = Formula.of(clause2.car());
							if (litF.listP() && litF.car().equals(Formula.NOT))
							{
								litF = Formula.of(litF.cadr());
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
							clause2 = clause2.cdrAsFormula();
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
				//noinspection CommentedOutCode
				{
					// Collections.sort(negLits);
					// Collections.sort(posLits);
				}
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
		@NotNull Formula oldF = Formula.of(formula.form);

		@NotNull Tuple.Triple<Formula, Map<String, String>, Formula> result = new Tuple.Triple<>();
		try
		{
			formula = equivalencesOut();
			formula = implicationsOut();
			formula = negationsIn();
			@NotNull Map<String, String> topLevelVars = new HashMap<>();
			@NotNull Map<String, String> scopedRenames = new HashMap<>();
			@NotNull Map<String, String> allRenames = new HashMap<>();
			formula = Variables.renameVariables(formula, topLevelVars, scopedRenames, allRenames);
			formula = existentialsOut();
			formula = universalsOut();
			formula = disjunctionsIn();

			@NotNull Map<String, String> standardizedRenames = new HashMap<>();
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
		@NotNull Formula result = formula;
		if (formula.listP() && !(formula.empty()))
		{
			@NotNull String head = formula.car();
			String newForm;
			if (Variables.isNonEmpty(head) && Formula.listP(head))
			{
				@NotNull String newHead = new Clausifier(head).equivalencesOut().form;
				newForm = new Clausifier(formula.cdr()).equivalencesOut().cons(newHead).form;
			}
			else if (head.equals(Formula.IFF))
			{
				@NotNull String newSecond = new Clausifier(formula.cadr()).equivalencesOut().form;
				@NotNull String newThird = new Clausifier(formula.caddr()).equivalencesOut().form;
				newForm = ("(and (=> " + newSecond + " " + newThird + ") (=> " + newThird + " " + newSecond + "))");
			}
			else
			{
				@NotNull Clausifier fourth = new Clausifier(formula.cdr());
				newForm = fourth.equivalencesOut().cons(head).form;
			}
			result = Formula.of(newForm);
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
		@NotNull Formula result = formula;
		String newForm;
		if (formula.listP() && !formula.empty())
		{
			@NotNull String head = formula.car();
			if (Variables.isNonEmpty(head) && Formula.listP(head))
			{
				@NotNull String newHead = new Clausifier(head).implicationsOut().form;
				newForm = new Clausifier(formula.cdr()).implicationsOut().cons(newHead).form;
			}
			else if (head.equals(Formula.IF))
			{
				@NotNull String newSecond = new Clausifier(formula.cadr()).implicationsOut().form;
				@NotNull String newThird = new Clausifier(formula.caddr()).implicationsOut().form;
				newForm = "(or (not " + newSecond + ") " + newThird + ")";
			}
			else
			{
				newForm = new Clausifier(formula.cdr()).implicationsOut().cons(head).form;
			}
			result = Formula.of(newForm);
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
		@NotNull Formula f = formula;
		@NotNull Formula result = negationsIn_1();
		// Here we repeatedly apply negationsIn_1() until there are no more changes.
		while (!f.form.equals(result.form))
		{
			f = result;
			result = negationsIn(f);
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
				@NotNull String arg0 = formula.car();
				@NotNull String arg1 = formula.cadr();
				if (arg0.equals(Formula.NOT) && Formula.listP(arg1))
				{
					@NotNull Formula arg1F = Formula.of(arg1);
					@NotNull String arg0_of_arg1 = arg1F.car();
					if (arg0_of_arg1.equals(Formula.NOT))
					{
						@NotNull String arg1_of_arg1 = arg1F.cadr();
						return Formula.of(arg1_of_arg1);
					}
					if (Formula.isCommutative(arg0_of_arg1))
					{
						@NotNull String newOp = (arg0_of_arg1.equals(Formula.AND) ? Formula.OR : Formula.AND);
						return listAll(arg1F.cdrOfListAsFormula(), "(not ", ")").cons(newOp);
					}
					if (Formula.isQuantifier(arg0_of_arg1))
					{
						@NotNull String vars = arg1F.cadr();
						@NotNull String arg2_of_arg1 = arg1F.caddr();
						@NotNull String quant = (arg0_of_arg1.equals(Formula.UQUANT) ? Formula.EQUANT : Formula.UQUANT);
						arg2_of_arg1 = "(not " + arg2_of_arg1 + ")";
						@NotNull Formula arg2_of_arg1F = Formula.of(arg2_of_arg1);
						@NotNull String newForm = "(" + quant + " " + vars + " " + negationsIn(arg2_of_arg1F).form + ")";
						return Formula.of(newForm);
					}
					@NotNull String newForm = ("(not " + negationsIn(arg1F).form + ")");
					return Formula.of(newForm);
				}
				if (Formula.isQuantifier(arg0))
				{
					@NotNull String arg2 = formula.caddr();
					@NotNull Formula arg2F = Formula.of(arg2);
					@NotNull String newArg2 = negationsIn(arg2F).form;
					return Formula.of("(" + arg0 + " " + arg1 + " " + newArg2 + ")");
				}
				if (Formula.listP(arg0))
				{
					@NotNull Formula arg0F = Formula.of(arg0);
					return negationsIn(formula.cdrOfListAsFormula()).cons(negationsIn(arg0F).form);
				}
				return negationsIn(formula.cdrOfListAsFormula()).cons(arg0);
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
	@NotNull
	private static Formula negationsIn(@NotNull Formula f)
	{
		return new Clausifier(f.form).negationsIn_1();
	}

	// L I S T

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
		if (formula.listP())
		{
			@NotNull StringBuilder sb = new StringBuilder();
			for (@Nullable Formula itF = formula; itF != null && !itF.empty(); itF = itF.cdrAsFormula())
			{
				@NotNull String element = itF.car();
				if (Variables.isNonEmpty(before))
				{
					element = (before + element);
				}
				if (Variables.isNonEmpty(after))
				{
					element += after;
				}
				sb.append(Formula.SPACE).append(element);

			}
			sb = new StringBuilder((Formula.LP + sb.toString().trim() + Formula.RP));
			if (Variables.isNonEmpty(sb.toString()))
			{
				return Formula.of(sb.toString());
			}
		}
		return formula;
	}

	/**
	 * Convenience method
	 */
	@NotNull
	private static Formula listAll(@NotNull Formula f, @SuppressWarnings("SameParameterValue") String before, @NotNull @SuppressWarnings("SameParameterValue") String after)
	{
		return new Clausifier(f.form).listAll(before, after);
	}

	// E X I S T E N T I A L S

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
		@NotNull Map<String, String> evSubs = new HashMap<>();

		// Implicitly universally quantified variables.
		@NotNull SortedSet<String> iUQVs = new TreeSet<>();

		// Explicitly quantified variables.
		@NotNull SortedSet<String> scopedVars = new TreeSet<>();

		// Explicitly universally quantified variables.
		@NotNull SortedSet<String> scopedUQVs = new TreeSet<>();

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
				@NotNull String arg0 = formula.car();
				if (arg0.equals(Formula.UQUANT))
				{
					// Copy the scoped variables set to protect variable scope as we descend below this quantifier.
					@NotNull SortedSet<String> newScopedUQVs = new TreeSet<>(scopedUQVs);
					@NotNull String varList = formula.cadr();
					@NotNull IterableFormula varListF = new IterableFormula(varList);
					while (!(varListF.empty()))
					{
						@NotNull String var = varListF.car();
						newScopedUQVs.add(var);
						varListF.pop();
					}
					@NotNull String arg2 = formula.caddr();
					@NotNull Formula arg2F = Formula.of(arg2);
					@NotNull String newForm = "(forall " + varList + " " + existentialsOut(arg2F, evSubs, iUQVs, newScopedUQVs).form + ")";
					formula = Formula.of(newForm);
					return formula;
				}
				if (arg0.equals(Formula.EQUANT))
				{
					// Collect the relevant universally quantified variables.
					@NotNull SortedSet<String> uQVs = new TreeSet<>(iUQVs);
					uQVs.addAll(scopedUQVs);
					// Collect the existentially quantified variables.
					@NotNull List<String> eQVs = new ArrayList<>();
					@NotNull String varList = formula.cadr();
					@NotNull IterableFormula varListF = new IterableFormula(varList);
					while (!(varListF.empty()))
					{
						@NotNull String var = varListF.car();
						eQVs.add(var);
						varListF.pop();
					}
					// For each existentially quantified variable, create a corresponding skolem term, and store the pair in the evSubs map.
					for (String var : eQVs)
					{
						@NotNull String skTerm = Variables.newSkolemTerm(uQVs);
						evSubs.put(var, skTerm);
					}
					@NotNull String arg2 = formula.caddr();
					@NotNull Formula arg2F = Formula.of(arg2);
					return existentialsOut(arg2F, evSubs, iUQVs, scopedUQVs);
				}
				@NotNull Formula arg0F = Formula.of(arg0);
				@NotNull String newArg0 = existentialsOut(arg0F, evSubs, iUQVs, scopedUQVs).form;
				return existentialsOut(formula.cdrOfListAsFormula(), evSubs, iUQVs, scopedUQVs).cons(newArg0);
			}
			if (Formula.isVariable(formula.form))
			{
				String newTerm = evSubs.get(formula.form);
				if (Variables.isNonEmpty(newTerm))
				{
					formula = Formula.of(newTerm);
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
		return new Clausifier(f.form).existentialsOut(evSubs, iUQVs, scopedUQVs);
	}

	// U N I V E R S A L S

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
				@NotNull String arg0 = formula.car();
				if (Formula.isQuantifier(arg0))
				{
					// Copy the scopedVars set to protect variable  scope as we descend below this quantifier.
					@NotNull SortedSet<String> newScopedVars = new TreeSet<>(scopedVars);

					@NotNull Formula varListF = Formula.of(formula.cadr());
					for (@Nullable Formula itF = varListF; itF != null && !itF.empty(); itF = itF.cdrAsFormula())
					{
						@NotNull String var = itF.car();
						newScopedVars.add(var);
					}
					@NotNull Formula arg2F = Formula.of(formula.caddr());
					collectIUQVars(arg2F, iuqvs, newScopedVars);
				}
				else
				{
					@NotNull Formula arg0F = Formula.of(arg0);
					collectIUQVars(arg0F, iuqvs, scopedVars);
					@Nullable Formula restF = formula.cdrAsFormula();
					if (restF != null)
					{
						collectIUQVars(restF, iuqvs, scopedVars);
					}
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
		@NotNull Clausifier temp = new Clausifier(f.form);
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
				@NotNull String arg0 = formula.car();
				if (arg0.equals(Formula.UQUANT))
				{
					@NotNull String arg2 = formula.caddr();
					formula = Formula.of(arg2);
					return universalsOut(formula);
				}
				@NotNull Formula arg0F = Formula.of(arg0);
				@NotNull String newArg0 = universalsOut(arg0F).form;
				return universalsOut(formula.cdrOfListAsFormula()).cons(newArg0);
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
		@NotNull Clausifier temp = new Clausifier(f.form);
		return temp.universalsOut();
	}

	// N E S T E D   O P E R A T O R S

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
		@NotNull Formula f = formula;
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
		@NotNull Clausifier temp = new Clausifier(f.form);
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
				@NotNull String arg0 = formula.car();
				if (Formula.isCommutative(arg0) || arg0.equals(Formula.NOT))
				{
					@NotNull List<String> literals = new ArrayList<>();
					for (@Nullable Formula itF = formula.cdrAsFormula(); itF != null && !itF.empty(); itF = itF.cdrAsFormula())
					{
						@NotNull String lit = itF.car();
						@NotNull Formula litF = Formula.of(lit);
						if (litF.listP())
						{
							if (litF.car().equals(arg0))
							{
								if (arg0.equals(Formula.NOT))
								{
									@NotNull Formula newF = Formula.of(litF.cadr());
									return nestedOperatorsOut_1(newF);
								}
								for (@Nullable Formula it2F = litF.cdrAsFormula(); it2F != null && !it2F.empty(); it2F = it2F.cdrAsFormula())
								{
									literals.add(nestedOperatorsOut_1(Formula.of(it2F.car())).form);
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

					}
					@NotNull StringBuilder sb = new StringBuilder((Formula.LP + arg0));
					for (String literal : literals)
					{
						sb.append(Formula.SPACE).append(literal);
					}
					sb.append(Formula.RP);
					return Formula.of(sb.toString());
				}
				@NotNull Formula arg0F = Formula.of(arg0);
				@NotNull String newArg0 = nestedOperatorsOut_1(arg0F).form;
				return nestedOperatorsOut_1(formula.cdrOfListAsFormula()).cons(newArg0);
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
	private static Formula nestedOperatorsOut_1(@NotNull Formula f)
	{
		return new Clausifier(f.form).nestedOperatorsOut_1();
	}

	// D I S J U N C T I O N S

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
		@NotNull Formula f = formula;
		@NotNull Formula result = disjunctionsIn(nestedOperatorsOut());

		// Here we repeatedly apply disjunctionIn_1() until there are no more changes.
		while (!f.form.equals(result.form))
		{
			f = result;
			result = disjunctionsIn(nestedOperatorsOut(f));
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
				@NotNull String arg0 = formula.car();
				if (arg0.equals(Formula.OR))
				{
					@NotNull List<String> disjuncts = new ArrayList<>();
					@NotNull List<String> conjuncts = new ArrayList<>();
					for (@Nullable Formula itF = formula.cdrAsFormula(); itF != null && !itF.empty(); itF = itF.cdrAsFormula())
					{
						@NotNull String disjunct = itF.car();
						@NotNull Formula disjunctF = Formula.of(disjunct);
						if (disjunctF.listP() && disjunctF.car().equals(Formula.AND) && conjuncts.isEmpty())
						{
							@Nullable Formula rest2F = disjunctionsIn(disjunctF.cdrOfListAsFormula());
							for (@Nullable Formula it2F = rest2F; it2F != null && !it2F.empty(); it2F = it2F.cdrAsFormula())
							{
								conjuncts.add(it2F.car());
							}
						}
						else
						{
							disjuncts.add(disjunct);
						}

					}

					if (conjuncts.isEmpty())
					{
						return formula;
					}

					@NotNull Formula resultF = Formula.EMPTY_LIST;
					@NotNull StringBuilder disjunctsString = new StringBuilder();
					for (String disjunct : disjuncts)
					{
						disjunctsString.append(Formula.SPACE).append(disjunct);
					}
					disjunctsString = new StringBuilder((Formula.LP + disjunctsString.toString().trim() + Formula.RP));
					@NotNull Formula disjunctsF = Formula.of(disjunctsString.toString());
					for (@NotNull String conjunct : conjuncts)
					{
						@NotNull String newDisjuncts = disjunctionsIn(disjunctsF.cons(conjunct).cons(Formula.OR)).form;
						resultF = resultF.cons(newDisjuncts);
					}
					resultF = resultF.cons(Formula.AND);
					return resultF;
				}
				@NotNull Formula arg0F = Formula.of(arg0);
				@NotNull String newArg0 = disjunctionsIn(arg0F).form;
				return disjunctionsIn(formula.cdrOfListAsFormula()).cons(newArg0);
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
	@NotNull
	private static Formula disjunctionsIn(@NotNull Formula f)
	{
		return new Clausifier(f.form).disjunctionsIn_1();
	}

	// O P E R A T O R S

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
		@NotNull List<Formula> result = new ArrayList<>();
		try
		{
			@NotNull List<Formula> clauses = new ArrayList<>();
			if (Variables.isNonEmpty(formula.form))
			{
				if (formula.listP())
				{
					@NotNull String arg0 = formula.car();
					if (arg0.equals(Formula.AND))
					{
						for (@Nullable Formula itF = formula.cdrAsFormula(); itF != null && !itF.empty(); itF = itF.cdrAsFormula())
						{
							@NotNull Formula newF = Formula.of(itF.car());
							clauses.add(newF);
						}
					}
				}
				if (clauses.isEmpty())
				{
					clauses.add(formula);
				}
				for (@NotNull Formula f : clauses)
				{
					@NotNull Formula clauseF = Formula.EMPTY_LIST;
					if (f.listP())
					{
						if (f.car().equals(Formula.OR))
						{
							for (@Nullable Formula itF = f; itF != null && !itF.empty(); itF = itF.cdrAsFormula())
							{
								clauseF = clauseF.cons(itF.car());
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

	// S T A N D A R D I Z E

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
			@NotNull Map<String, String> reverseRenames = Objects.requireNonNullElseGet(renameMap, HashMap::new);

			// First, break the Formula into separate clauses, if necessary.
			@NotNull List<Formula> clauses = new ArrayList<>();
			if (Variables.isNonEmpty(formula.form))
			{
				if (formula.listP())
				{
					@NotNull String arg0 = formula.car();
					if (arg0.equals(Formula.AND))
					{
						@Nullable Formula restF = formula.cdrAsFormula();
						while (restF != null && !restF.empty())
						{
							@NotNull String newForm = restF.car();
							@NotNull Formula newF = Formula.of(newForm);
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
					@NotNull Map<String, String> renames = new HashMap<>();
					Formula oldClause = clauses.remove(0);
					clauses.add(standardizeApart(oldClause, renames, reverseRenames));
				}

				// Construct the new Formula to return.
				if (n > 1)
				{
					@NotNull StringBuilder newForm = new StringBuilder("(and");
					for (@NotNull Formula f : clauses)
					{
						newForm.append(Formula.SPACE).append(f.form);
					}
					newForm.append(Formula.RP);
					result = Formula.of(newForm.toString());
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
	private Formula standardizeApart(@NotNull Map<String, String> renames, @NotNull Map<String, String> reverseRenames)
	{
		if (formula.listP() && !(formula.empty()))
		{
			@NotNull Formula arg0F = Formula.of(formula.car());
			arg0F = standardizeApart(arg0F, renames, reverseRenames);
			return standardizeApart(formula.cdrOfListAsFormula(), renames, reverseRenames).cons(arg0F.form);
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
			return Formula.of(rnv);
		}
		return formula;
	}

	/**
	 * Convenience method
	 */
	@NotNull
	private static Formula standardizeApart(@NotNull Formula f, @NotNull Map<String, String> renames, @NotNull Map<String, String> reverseRenames)
	{
		return new Clausifier(f.form).standardizeApart(renames, reverseRenames);
	}
}


