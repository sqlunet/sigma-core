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

import org.sigma.core.Tuple.Triple;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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
	private static final String LOG_SOURCE = "Clausifier";

	private static final Logger LOGGER = Logger.getLogger(Clausifier.class.getName());

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
	 * @param f a Formula
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
	public static Triple<List<Clause>, Map<String, String>, Formula> clausify(@NotNull final Formula f)
	{
		LOGGER.entering(LOG_SOURCE, f.toFlatString());

		// clausal form
		@NotNull Triple<Formula, Map<String, String>, Formula> cff = clausalForm(f);
		@Nullable Formula clausalForm = cff.first;
		assert clausalForm != null;

		// clauses
		@NotNull List<Formula> clausalFormulas = extractClauses(clausalForm.form);

		// result [1] clauses, [2] var renamings, [3] input formula
		@Nullable List<Clause> clauses = new ArrayList<>();
		if (!clausalFormulas.isEmpty())
		{
			// sort them into negative and positive
			clauses = new ArrayList<>();
			for (@NotNull final Formula f2 : clausalFormulas)
			{
				@NotNull Clause clause = new Clause();
				if (f2.listP())
				{
					for (@NotNull IterableFormula itF2 = new IterableFormula(f2.form); !itF2.empty(); itF2.pop())
					{
						// compute negativity
						boolean isNegLit = false;
						@NotNull String lit = itF2.car();
						if (Lisp.listP(lit) && Formula.NOT.equals(Lisp.car(lit)))
						{
							// lit = (not C)
							lit = Lisp.cadr(lit);
							// lit = C
							isNegLit = true;
						}
						if (Formula.LOGICAL_FALSE.equals(lit))
						{
							// False
							isNegLit = true;
						}

						// add to containers
						if (isNegLit)
						{
							clause.negativeLits.add(Formula.of(lit));
						}
						else
						{
							clause.positiveLits.add(Formula.of(lit));
						}
					}
				}
				else if (Formula.LOGICAL_FALSE.equals(f2.form))
				{
					clause.negativeLits.add(f2);
				}
				else
				{
					clause.positiveLits.add(f2);
				}
				clauses.add(clause);
			}
			// Collections.sort(negLits);
			// Collections.sort(posLits);
		}

		@NotNull Triple<List<Clause>, Map<String, String>, Formula> result = new Triple<>();
		result.first = clauses;
		result.second = cff.second;
		result.third = cff.third;
		LOGGER.exiting(LOG_SOURCE, result.toString());
		return result;
	}

	/**
	 * Clausal form
	 *
	 * @param f a Formula
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
	public static Triple<Formula, Map<String, String>, Formula> clausalForm(@NotNull final Formula f)
	{
		LOGGER.entering(LOG_SOURCE, f.toFlatString());
		@NotNull String form = f.form;

		// process
		form = equivalencesOut(form);
		form = implicationsOut(form);
		form = negationsIn(form);
		@NotNull Map<String, String> topLevelVars = new HashMap<>();
		@NotNull Map<String, String> scopedRenames = new HashMap<>();
		@NotNull Map<String, String> allRenames = new HashMap<>();
		form = Variables.renameVariables(form, topLevelVars, scopedRenames, allRenames);
		form = existentialsOut(form);
		form = universalsOut(form);
		form = disjunctionsIn(form);
		@NotNull Map<String, String> standardizedRenames = new HashMap<>();
		form = standardizeApart(form, standardizedRenames);
		allRenames.putAll(standardizedRenames);

		// result: [1] clausal form formula, [2] var renamings, [3] input formula
		@NotNull Triple<Formula, Map<String, String>, Formula> result = new Triple<>();
		result.first = Formula.of(form);
		result.second = allRenames;
		result.third = f;
		LOGGER.exiting(LOG_SOURCE, result.toString());
		return result;
	}

	/**
	 * Clausal form
	 *
	 * @param f a Formula
	 * @return The new clausal-form Formula,
	 */
	@Nullable
	public static Formula clausalForm1(@NotNull final Formula f)
	{
		return clausalForm(f).first;
	}

	// I F F

	/**
	 * This method converts every occurrence of '<=>' in the Formula
	 * to a conjunct with two occurrences of '=>'.
	 *
	 * @param f a Formula
	 * @return A Formula with no occurrences of '<=>'.
	 */
	@NotNull
	public static Formula equivalencesOut(@NotNull final Formula f)
	{
		return Formula.of(equivalencesOut(f.form));
	}

	/**
	 * This method converts every occurrence of '<=>' in the Formula
	 * to a conjunct with two occurrences of '=>'.
	 *
	 * @param form formula string
	 * @return A formula string with no occurrences of '<=>'.
	 */
	@NotNull
	static String equivalencesOut(@NotNull final String form)
	{
		if (Lisp.listP(form) && !(Lisp.empty(form)))
		{
			@NotNull String head = Lisp.car(form);
			if (!head.isEmpty() && Lisp.listP(head))
			{
				@NotNull String newHead = equivalencesOut(head);
				return Lisp.cons(equivalencesOut(Lisp.cdr(form)), newHead);
			}
			else if (Formula.IFF.equals(head))
			{
				@NotNull String newSecond = equivalencesOut(Lisp.cadr(form));
				@NotNull String newThird = equivalencesOut(Lisp.caddr(form));
				return Formula.LP + Formula.AND + Formula.SPACE + //
						Formula.LP + Formula.IF + Formula.SPACE + newSecond + Formula.SPACE + newThird + Formula.RP + Formula.SPACE + //
						Formula.LP + Formula.IF + Formula.SPACE + newThird + Formula.SPACE + newSecond + Formula.RP + Formula.RP;
			}
			else
			{
				return Lisp.cons(equivalencesOut(Lisp.cdr(form)), head);
			}
		}
		return form;
	}

	// I F

	/**
	 * This method converts every occurrence of "(=> LHS RHS)" in the
	 * Formula to a disjunct of the form "(or (not LHS) RHS)".
	 *
	 * @param f a Formula
	 * @return A Formula with no occurrences of '=>'.
	 */
	@NotNull
	public static Formula implicationsOut(@NotNull final Formula f)
	{
		return Formula.of(implicationsOut(f.form));
	}

	/**
	 * This method converts every occurrence of "(=> LHS RHS)" in the
	 * Formula to a disjunct of the form "(or (not LHS) RHS)".
	 *
	 * @return A Formula with no occurrences of '=>'.
	 */
	@NotNull
	static String implicationsOut(@NotNull final String form)
	{
		if (Lisp.listP(form) && !Lisp.empty(form))
		{
			@NotNull String head = Lisp.car(form);
			if (!head.isEmpty() && Lisp.listP(head))
			{
				@NotNull String newHead = implicationsOut(head);
				return Lisp.cons(implicationsOut(Lisp.cdr(form)), newHead);
			}
			else if (head.equals(Formula.IF))
			{
				@NotNull String newSecond = implicationsOut(Formula.of(Lisp.cadr(form))).form;
				@NotNull String newThird = implicationsOut(Formula.of(Lisp.caddr(form))).form;
				return Formula.LP + Formula.OR + Formula.SPACE + Formula.LP + Formula.NOT + Formula.SPACE + newSecond + Formula.RP + Formula.SPACE + newThird + Formula.RP;
			}
			else
			{
				return Lisp.cons(implicationsOut(Lisp.cdr(form)), head);
			}
		}
		return form;
	}

	// N E G A T I O N

	/**
	 * This method 'pushes in' all occurrences of 'not', so that each
	 * occurrence has the narrowest possible scope, and also removes
	 * from the Formula all occurrences of '(not (not ...))'.
	 *
	 * @param f a Formula
	 * @return A formula string with all occurrences of 'not' accorded
	 * narrowest scope, and no occurrences of '(not (not ...))'.
	 */
	@NotNull
	public static Formula negationsIn(@NotNull final Formula f)
	{
		return Formula.of(negationsIn(f.form));
	}

	/**
	 * This method 'pushes in' all occurrences of 'not', so that each
	 * occurrence has the narrowest possible scope, and also removes
	 * from the Formula all occurrences of '(not (not ...))'.
	 *
	 * @param form formula string
	 * @return A formula string with all occurrences of 'not' accorded
	 * narrowest scope, and no occurrences of '(not (not ...))'.
	 */
	@NotNull
	static String negationsIn(@NotNull final String form)
	{
		@NotNull String form0 = form;
		@Nullable String form1 = null;
		// Here we repeatedly apply negationsIn() until there are no more changes.
		while (!form0.equals(form1))
		{
			form1 = negationsInStep(form0);
			form0 = form1;
		}
		return form0;
	}

	/**
	 * This method is used in negationsIn().  It recursively 'pushes
	 * in' all occurrences of 'not', so that each occurrence has the
	 * narrowest possible scope, and also removes from the Formula all
	 * occurrences of '(not (not ...))'.
	 *
	 * @param form formula string
	 * @return A formula string with all occurrences of 'not' accorded
	 * narrowest scope, and no occurrences of '(not (not ...))'.
	 */
	@NotNull
	private static String negationsInStep(@NotNull final String form)
	{
		if (Lisp.listP(form))
		{
			if (Lisp.empty(form))
			{
				return form;
			}
			@NotNull String arg0 = Lisp.car(form);
			@NotNull String arg1 = Lisp.cadr(form);
			if (Formula.NOT.equals(arg0) && Lisp.listP(arg1))
			{
				// (not negated)
				// (not (head ...))
				@NotNull String negated = arg1;
				@NotNull String head = Lisp.car(negated);
				if (Formula.NOT.equals(head))
				{
					// (not (not negated2))
					@NotNull String negated2 = Lisp.cadr(negated);
					// (not (not A)) -> A
					return negated2;
				}
				if (Formula.isCommutative(head))
				{
					// (not (or|and cdr))
					// (not (or|and A B))
					@NotNull String newOp = Formula.AND.equals(head) ? Formula.OR : Formula.AND;
					// (not (or A B)) -> (and (not A) (not B))
					// (not (and A B)) -> (or (not A) (not B))
					return Lisp.cons(augmentElements(Lisp.cdr(negated), Formula.LP + Formula.NOT + Formula.SPACE, Formula.RP), newOp);
				}
				if (Formula.isQuantifier(head))
				{
					@NotNull String vars = Lisp.cadr(negated);
					@NotNull String arg2_of_arg1 = Lisp.caddr(negated);
					@NotNull String quant = (head.equals(Formula.UQUANT) ? Formula.EQUANT : Formula.UQUANT);
					arg2_of_arg1 = "(not " + arg2_of_arg1 + ")";
					@NotNull String newForm = "(" + quant + " " + vars + " " + negationsIn(arg2_of_arg1) + ")";
					return newForm;
				}
				@NotNull String newForm = ("(not " + negationsIn(negated) + ")");
				return newForm;
			}
			if (Formula.isQuantifier(arg0))
			{
				@NotNull String arg2 = Lisp.caddr(form);
				@NotNull String newArg2 = negationsIn(arg2);
				return "(" + arg0 + " " + arg1 + " " + newArg2 + ")";
			}
			if (Lisp.listP(arg0))
			{
				return Lisp.cons(negationsIn(Lisp.cdr(form)), negationsIn(arg0));
			}
			return Lisp.cons(negationsIn(Lisp.cdr(form)), arg0);
		}
		return form;
	}

	// N E S T E D   O P E R A T O R S

	/**
	 * This method returns a new Formula in which nested 'and', 'or',
	 * and 'not' operators have been unnested:
	 * (not (not <literal> ...)) -> <literal>
	 * (and (and <literal-sequence> ...)) -> (and <literal-sequence> ...)
	 * (or (or <literal-sequence> ...)) -> (or <literal-sequence> ...)
	 *
	 * @param f A Formula
	 * @return A new SUO-KIF Formula in which nested commutative
	 * operators and 'not' have been unnested.
	 */
	@NotNull
	public static Formula nestedOperatorsOut(@NotNull final Formula f)
	{
		return Formula.of(nestedOperatorsOut(f.form));
	}

	/**
	 * This method returns a new Formula in which nested 'and', 'or',
	 * and 'not' operators have been unnested:
	 * (not (not <literal> ...)) -> <literal>
	 * (and (and <literal-sequence> ...)) -> (and <literal-sequence> ...)
	 * (or (or <literal-sequence> ...)) -> (or <literal-sequence> ...)
	 *
	 * @param form formula string
	 * @return A new formula string in which nested commutative
	 * operators and 'not' have been unnested.
	 */
	@NotNull
	static String nestedOperatorsOut(@NotNull final String form)
	{
		@NotNull String form0 = form;
		@Nullable String form1 = null;
		// Here we repeatedly apply nestedOperatorsOutStep until there are no more changes.
		while (!form0.equals(form1))
		{
			form1 = nestedOperatorsOutStep(form0);
			form0 = form1;
		}
		return form0;
	}

	/**
	 * This method returns a new Formula in which nested 'and', 'or',
	 * and 'not' operators have been unnested:
	 * (not (not <literal> ...)) -> <literal>
	 * (and (and <literal-sequence> ...)) -> (and <literal-sequence> ...)
	 * (or (or <literal-sequence> ...)) -> (or <literal-sequence> ...)
	 *
	 * @param form formula string
	 * @return A new formula string in which nested commutative
	 * operators and 'not' have been unnested.
	 */
	private static String nestedOperatorsOutStep(@NotNull final String form)
	{
		if (Lisp.listP(form))
		{
			if (Lisp.empty(form))
			{
				return form;
			}
			@NotNull String head = Lisp.car(form);
			if (Formula.isCommutative(head) || Formula.NOT.equals(head))
			{
				@NotNull List<String> literals = new ArrayList<>();
				for (@Nullable IterableFormula itF = new IterableFormula(Lisp.cdr(form)); !itF.empty(); itF.pop())
				{
					@NotNull String lit = itF.car();
					if (Lisp.listP(lit))
					{
						if (Lisp.car(lit).equals(head))
						{
							if (head.equals(Formula.NOT))
							{
								@NotNull String newF = Lisp.cadr(lit);
								return nestedOperatorsOutStep(newF);
							}
							for (@Nullable IterableFormula it2F = new IterableFormula(Lisp.cdr(lit)); !it2F.empty(); it2F.pop())
							{
								literals.add(nestedOperatorsOutStep(it2F.car()));
							}
						}
						else
						{
							literals.add(nestedOperatorsOutStep(lit));
						}
					}
					else
					{
						literals.add(lit);
					}
				}

				@NotNull StringBuilder sb = new StringBuilder((Formula.LP + head));
				for (String literal : literals)
				{
					sb.append(Formula.SPACE).append(literal);
				}
				sb.append(Formula.RP);
				return sb.toString();
			}
			@NotNull String newArg0 = nestedOperatorsOutStep(head);
			return Lisp.cons(nestedOperatorsOutStep(Lisp.cdr(form)), newArg0);
		}
		return form;
	}

	// D I S J U N C T I O N S

	/**
	 * This method returns a new Formula in which all occurrences of
	 * 'or' have been accorded the least possible scope.
	 * (or P (and Q R)) -> (and (or P Q) (or P R))
	 *
	 * @param f a Formula
	 * @return A new SUO-KIF Formula in which occurrences of 'or' have
	 * been 'moved in' as far as possible.
	 */
	@NotNull
	public static Formula disjunctionsIn(@NotNull final Formula f)
	{
		return Formula.of(disjunctionsIn(f.form));
	}

	@NotNull
	static String disjunctionsIn(@NotNull final String form)
	{
		@NotNull String form0 = form;
		@Nullable String form1 = null;
		// Here we repeatedly apply disjunctionsInStep(nestedOperatorsOut(f)) until there are no more changes.
		while (!form0.equals(form1))
		{
			form1 = disjunctionsInStep(nestedOperatorsOut(form0));
			form0 = form1;
		}
		return form0;
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
	private static String disjunctionsInStep(@NotNull final String form)
	{
		if (Lisp.listP(form))
		{
			if (Lisp.empty(form))
			{
				return form;
			}
			@NotNull String head = Lisp.car(form);
			if (Formula.OR.equals(head))
			{
				@NotNull List<String> disjuncts = new ArrayList<>();
				@NotNull List<String> conjuncts = new ArrayList<>();
				for (@Nullable IterableFormula itF = new IterableFormula(Lisp.cdr(form)); !itF.empty(); itF.pop())
				{
					@NotNull String head2 = itF.car();
					if (Lisp.listP(head2) && Formula.AND.equals(Lisp.car(head2)) && conjuncts.isEmpty())
					{
						@Nullable String rest2 = disjunctionsInStep(Lisp.cdr(head2));
						for (@Nullable IterableFormula it2F = new IterableFormula(rest2); !it2F.empty(); it2F.pop())
						{
							conjuncts.add(it2F.car());
						}
					}
					else
					{
						disjuncts.add(head2);
					}
				}

				if (conjuncts.isEmpty())
				{
					return form;
				}

				@NotNull String result = Formula.EMPTY_LIST.form;
				for (@NotNull String conjunct : conjuncts)
				{
					@NotNull String result2 = disjunctionsIn(Lisp.cons(Lisp.cons(joinToList(disjuncts), conjunct), Formula.OR));
					result = Lisp.cons(result, result2);
				}
				result = Lisp.cons(result, Formula.AND);
				return result;
			}

			return Lisp.cons(disjunctionsInStep(Lisp.cdr(form)), disjunctionsInStep(head));
		}
		return form;
	}

	// E X I S T E N T I A L S

	/**
	 * This method returns a new Formula in which all existentially
	 * quantified variables have been replaced by Skolem terms.
	 *
	 * @param f a Formula
	 * @return A new SUO-KIF Formula without existentially quantified
	 * variables.
	 */
	@NotNull
	public static Formula existentialsOut(@NotNull final Formula f)
	{
		return Formula.of(existentialsOut(f.form));
	}

	static String existentialsOut(@NotNull final String form)
	{
		// Existentially quantified variable substitution pairs: var -> skolem term.
		@NotNull Map<String, String> evSubs = new HashMap<>();

		// Implicitly universally quantified variables.
		@NotNull Set<String> iUQVs = new TreeSet<>();

		// Explicitly quantified variables.
		@NotNull Set<String> scopedVars = new TreeSet<>();

		// Explicitly universally quantified variables.
		@NotNull Set<String> scopedUQVs = new TreeSet<>();

		// Collect the implicitly universally qualified variables from the Formula.
		collectUQVars(form, iUQVs, scopedVars);

		// Do the recursive term replacement, and return the results.
		return existentialsOut(form, evSubs, iUQVs, scopedUQVs);
	}

	private static String existentialsOut(@NotNull final String form, @NotNull final Map<String, String> evSubs, @NotNull final Set<String> iUQVs, @NotNull final Set<String> scopedUQVs)
	{
		if (Lisp.listP(form))
		{
			if (Lisp.empty(form))
			{
				return form;
			}
			@NotNull String arg0 = Lisp.car(form);
			if (arg0.equals(Formula.UQUANT))
			{
				// Copy the scoped variables set to protect variable scope as we descend below this quantifier.
				@NotNull SortedSet<String> newScopedUQVs = new TreeSet<>(scopedUQVs);
				@NotNull String varList = Lisp.cadr(form);

				for (@NotNull IterableFormula varListF = new IterableFormula(varList); !varListF.empty(); varListF.pop())
				{
					@NotNull String var = varListF.car();
					newScopedUQVs.add(var);
				}
				@NotNull String arg2 = Lisp.caddr(form);
				@NotNull String newForm = "(forall " + varList + " " + existentialsOut(arg2, evSubs, iUQVs, newScopedUQVs) + ")";
				return newForm;
			}
			if (arg0.equals(Formula.EQUANT))
			{
				// Collect the relevant universally quantified variables.
				@NotNull SortedSet<String> uQVs = new TreeSet<>(iUQVs);
				uQVs.addAll(scopedUQVs);
				// Collect the existentially quantified variables.
				@NotNull List<String> eQVs = new ArrayList<>();
				@NotNull String varList = Lisp.cadr(form);
				for (@NotNull IterableFormula varListF = new IterableFormula(varList); !varListF.empty(); varListF.pop())
				{
					@NotNull String var = varListF.car();
					eQVs.add(var);
				}

				// For each existentially quantified variable, create a corresponding skolem term, and store the pair in the evSubs map.
				for (String var : eQVs)
				{
					@NotNull String skTerm = Variables.newSkolemTerm(uQVs);
					evSubs.put(var, skTerm);
				}
				@NotNull String arg2 = Lisp.caddr(form);
				return existentialsOut(arg2, evSubs, iUQVs, scopedUQVs);
			}
			@NotNull String newArg0 = existentialsOut(arg0, evSubs, iUQVs, scopedUQVs);
			return Lisp.cons(existentialsOut(Lisp.cdr(form), evSubs, iUQVs, scopedUQVs), newArg0);
		}
		if (Formula.isVariable(form))
		{
			String newTerm = evSubs.get(form);
			if (newTerm != null && !newTerm.isEmpty())
			{
				return newTerm;
			}
			return form;
		}
		return form;
	}

	// U N I V E R S A L S

	/**
	 * This method returns a new Formula in which explicit universal
	 * quantifiers have been removed.
	 *
	 * @param f a Formula
	 * @return A new SUO-KIF Formula without explicit universal
	 * quantifiers.
	 */
	@NotNull
	public static Formula universalsOut(@NotNull final Formula f)
	{
		return Formula.of(universalsOut(f.form));
	}

	/**
	 * This method returns a new Formula in which explicit universal
	 * quantifiers have been removed.
	 *
	 * @param form a formula string
	 * @return A new SUO-KIF formula string without explicit universal
	 * quantifiers.
	 */
	static String universalsOut(@NotNull final String form)
	{
		if (Lisp.listP(form))
		{
			if (Lisp.empty(form))
			{
				return form;
			}
			@NotNull String head = Lisp.car(form);
			if (head.equals(Formula.UQUANT))
			{
				@NotNull String body = Lisp.caddr(form);
				return universalsOut(body);
			}
			return Lisp.cons(universalsOut(Lisp.cdr(form)), universalsOut(head));
		}
		return form;
	}

	/**
	 * This method collects all variables in Formula that appear to be
	 * only implicitly universally quantified and adds them to the
	 * SortedSet iuqvs.  Note the iuqvs must be passed in.
	 *
	 * @param form       A formula string
	 * @param iuqvs      A SortedSet for accumulating variables that appear
	 *                   to be implicitly universally quantified.
	 * @param scopedVars A SortedSet containing explicitly quantified
	 *                   variables.
	 */
	private static void collectUQVars(@NotNull final String form, @NotNull final Set<String> iuqvs, @NotNull final Set<String> scopedVars)
	{
		if (Lisp.listP(form) && !Lisp.empty(form))
		{
			@NotNull String head = Lisp.car(form);
			if (Formula.isQuantifier(head))
			{
				// Copy the scopedVars set to protect variable scope as we descend below this quantifier.
				@NotNull SortedSet<String> newScopedVars = new TreeSet<>(scopedVars);

				@NotNull String vars = Lisp.cadr(form);
				for (@Nullable IterableFormula itF = new IterableFormula(vars); !itF.empty(); itF.pop())
				{
					@NotNull String var = itF.car();
					newScopedVars.add(var);
				}
				@NotNull String body = Lisp.caddr(form);
				collectUQVars(body, iuqvs, newScopedVars);
			}
			else
			{
				collectUQVars(head, iuqvs, scopedVars);
				@NotNull String cdr = Lisp.cdr(form);
				if (!cdr.isEmpty())
				{
					collectUQVars(cdr, iuqvs, scopedVars);
				}
			}
		}
		else if (Formula.isVariable(form) && !(scopedVars.contains(form)))
		{
			iuqvs.add(form);
		}
	}

	// R E M O V E   O P E R A T O R S   /   S P L I T   I N T O   C L A U S E S

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
	static List<Formula> extractClauses(@NotNull final String form)
	{
		@NotNull List<Formula> result = new ArrayList<>();
		if (!form.isEmpty())
		{
			@NotNull List<Formula> clauses = new ArrayList<>();
			if (Lisp.listP(form))
			{
				@NotNull String head = Lisp.car(form);
				if (Formula.AND.equals(head))
				{
					for (@Nullable IterableFormula itF = new IterableFormula(Lisp.cdr(form)); !itF.empty(); itF.pop())
					{
						clauses.add(Formula.of(itF.car()));
					}
				}
			}
			if (clauses.isEmpty())
			{
				clauses.add(Formula.of(form));
			}

			for (@NotNull Formula clause : clauses)
			{
				@NotNull String clause2 = Formula.EMPTY_LIST.form;
				if (Lisp.listP(clause.form))
				{
					if (Formula.OR.equals(Lisp.car(clause.form)))
					{
						for (@NotNull IterableFormula itF = new IterableFormula(Lisp.cdr(clause.form)); !itF.empty(); itF.pop())
						{
							clause2 = Lisp.cons(clause2, itF.car());
						}
					}
				}
				if (Lisp.empty(clause2))
				{
					clause2 = Lisp.cons(clause2, clause.form);
				}
				result.add(Formula.of(clause2));
			}
		}
		return result;
	}

	// E L E M E N T S

	/**
	 * This method augments each element of the Formula by
	 * concatenating optional Strings before and after the element.
	 * Note that in most cases the input Formula will be simply a
	 * list, not a well-formed SUO-KIF Formula, and that the output
	 * will therefore not necessarily be a well-formed Formula.
	 *
	 * @param form   formula string
	 * @param before A String that, if present, is prepended to every
	 *               element of the Formula.
	 * @param after  A String that, if present, is postpended to every
	 *               element of the Formula.
	 * @return A Formula, or, more likely, simply a list, with the
	 * String values corresponding to before and after added to each
	 * element.
	 */
	@NotNull
	private static String augmentElements(@NotNull final String form, @SuppressWarnings("SameParameterValue") @NotNull final String before, @SuppressWarnings("SameParameterValue") @NotNull final String after)
	{
		if (Lisp.listP(form))
		{
			@NotNull StringBuilder sb = new StringBuilder();
			for (@Nullable IterableFormula itF = new IterableFormula(form); !itF.empty(); itF.pop())
			{
				@NotNull String element = itF.car();
				element = before + element + after;
				sb.append(Formula.SPACE).append(element);
			}
			sb = new StringBuilder(Formula.LP + sb.toString().trim() + Formula.RP);
			return sb.toString();
		}
		return form;
	}

	@NotNull
	private static String joinToList(@NotNull final Collection<String> elements)
	{
		return Formula.LP + String.join(Formula.SPACE, elements) + Formula.RP;
	}

	// S T A N D A R D I Z E  A P A R T

	/**
	 * This method returns a formula in which variables for separate
	 * clauses have been 'standardized apart'.
	 *
	 * @param form      formula string
	 * @param renameMap A Map for capturing one-to-one variable rename
	 *                  correspondences.  Keys are new variables.  Values are old
	 *                  variables.
	 * @return A formula string.
	 */
	@NotNull
	private static String standardizeApart(@NotNull final String form, @Nullable final Map<String, String> renameMap)
	{
		if (!form.isEmpty())
		{
			// First, break the formula into separate clauses, if necessary.
			@NotNull List<String> clauses = new ArrayList<>();
			if (Lisp.listP(form))
			{
				@NotNull String head = Lisp.car(form);
				if (Formula.AND.equals(head))
				{
					for (@NotNull IterableFormula itF = new IterableFormula(Lisp.cdr(form)); !itF.empty(); itF.pop())
					{
						clauses.add(itF.car());
					}
				}
			}
			if (clauses.isEmpty())
			{
				clauses.add(form);
			}

			// 'Standardize apart' by renaming the variables in each clause.
			@NotNull Map<String, String> renames = new HashMap<>();
			@NotNull Map<String, String> reverseRenames = Objects.requireNonNullElseGet(renameMap, HashMap::new);
			clauses = clauses.stream().map(c -> standardizeApart(c, renames, reverseRenames)).collect(Collectors.toList());

			// Construct the new formula to return.
			if (clauses.size() > 1)
			{
				return Formula.LP + Formula.AND + Formula.SPACE + String.join(Formula.SPACE, clauses) + Formula.RP;
			}
			else
			{
				return clauses.get(0);
			}
		}
		return form;
	}

	/**
	 * This is a helper method for standardizeApart(renameMap).  It
	 * assumes that the Formula will be a single clause.
	 *
	 * @param form           formula string
	 * @param renames        A Map of correspondences between old variables
	 *                       and new variables.
	 * @param reverseRenames A Map of correspondences between new
	 *                       variables and old variables.
	 * @return A formula string
	 */
	@NotNull
	private static String standardizeApart(@NotNull final String form, @NotNull final Map<String, String> renames, @NotNull final Map<String, String> reverseRenames)
	{
		if (Lisp.listP(form) && !Lisp.empty(form))
		{
			return Lisp.cons(standardizeApart(Lisp.cdr(form), renames, reverseRenames), standardizeApart(Lisp.car(form), renames, reverseRenames));
		}
		else if (Formula.isVariable(form))
		{
			@Nullable String renamedVar = renames.get(form);
			if (renamedVar == null || renamedVar.isEmpty())
			{
				renamedVar = Variables.newVar();
				renames.put(form, renamedVar);
				reverseRenames.put(renamedVar, form);
			}
			return renamedVar;
		}
		return form;
	}


	@NotNull
	public static String clausalFormToString(@NotNull final Triple<List<Clause>, Map<String, String>, Formula> cf)
	{
		return "formula= " + cf.third.form + '\n' + "clauses=" + cf.first + '\n' + "map= " + cf.second;
	}
}
