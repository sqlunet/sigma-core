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

package org.sigma.noncore;

import org.sigma.core.*;

import java.util.*;
import java.util.logging.Logger;

/**
 * Pre-processing for sending to the inference engine.
 */
public class FormulaPreProcessor
{
	private static final Logger LOGGER = Logger.getLogger(FormulaPreProcessor.class.getName());

	private static final String LOG_SOURCE = "FormulaPP";

	/**
	 * For any given formula, stop generating new pred var
	 * instantiations and row var expansions if this threshold value
	 * has been exceeded.  The default value is 2000.
	 */
	private static final int AXIOM_EXPANSION_LIMIT = 2000;

	public static final boolean ADD_HOLDS_PREFIX = "yes".equalsIgnoreCase(KBSettings.getPref("holdsPrefix"));
	private static final boolean ADD_SORTALS = "yes".equalsIgnoreCase(KBSettings.getPref("typePrefix"));

	/**
	 * Pre-process a formula before sending it to the theorem
	 * prover. This includes
	 * - ignoring meta-knowledge like documentation strings,
	 * - translating mathematical operators,
	 * - quoting higher-order formulas,
	 * - expanding row variables and
	 * - prepending the 'holds__' predicate.
	 *
	 * @param f0      formula to preprocess
	 * @param isQuery If true the Formula is a query and should be
	 *                existentially quantified, else the Formula is a
	 *                statement and should be universally quantified
	 * @param kb      The KB to be used for processing this Formula
	 * @return a List of Formula(s), which could be empty.
	 */
	@NotNull
	public static List<Formula> preProcess(@NotNull final Formula f0, final boolean isQuery, @NotNull final KB kb)
	{
		LOGGER.entering(LOG_SOURCE, "preProcess", new String[]{"isQuery = " + isQuery, "kb = " + kb.name});
		@NotNull List<Formula> results = new ArrayList<>();
		if (!f0.form.isEmpty())
		{
			// balanced list
			if (!f0.isBalancedList())
			{
				@NotNull String errStr = "Unbalanced parentheses or quotes";
				f0.errors.add(errStr);
				errStr += " in " + f0.form;
				LOGGER.warning(errStr);
				return results;
			}

			// non ascii
			@NotNull Formula f = Formula.of(f0.form);
			if (StringUtil.containsNonAsciiChars(f.form))
			{
				f = Formula.of(StringUtil.replaceNonAsciiChars(f.form));
			}

			// pred and row vars
			@NotNull List<Formula> variableReplacements = replacePredVarsAndRowVars(f0, kb, ADD_HOLDS_PREFIX);
			f0.errors.addAll(f.getErrors());

			// Iterate over the formulae resulting from predicate variable instantiation and row variable expansion,
			// passing each to preProcessRecurse for further processing.
			@NotNull List<Formula> accumulator = addInstancesOfSetOrClass(kb, isQuery, variableReplacements, f0.sourceFile);
			if (!accumulator.isEmpty())
			{
				for (@NotNull Formula f1 : accumulator)
				{
					@NotNull String form2 = f1.form;
					if (ADD_SORTALS && !isQuery && form2.matches(".*\\?\\w+.*"))  // isLogicalOperator(arg0) ||
					{
						form2 = Types.addTypeRestrictions(form2, kb);
					}

					final boolean ignoreStrings = false;
					final boolean translateIneq = true;
					final boolean translateMath = true;
					@NotNull String form3 = preProcessRecurse(form2, "", ignoreStrings, translateIneq, translateMath);
					@NotNull Formula f3 = Formula.of(form3);
					f0.errors.addAll(f3.getErrors());
					if (isOkForInference(f3, isQuery))
					{
						f3.sourceFile = f0.sourceFile;
						results.add(f3);
					}
					else
					{
						@NotNull String errStr = "Rejected formula for inference";
						f0.errors.add(errStr);
						errStr += " in " + form3;
						LOGGER.warning(errStr);
					}
				}
			}
		}
		LOGGER.exiting(LOG_SOURCE, "preProcess", results);
		return results;
	}

	/**
	 * Pre-process a formula before sending it to the theorem prover.
	 * This includes
	 * - ignoring meta-knowledge like documentation strings,
	 * - translating mathematical operators,
	 * - quoting higher-order formulas,
	 * - expanding row variables and
	 * - prepending the 'holds__' predicate.
	 *
	 * @return a List of Formula(s)
	 */
	@NotNull
	private static String preProcessRecurse(@NotNull final String form, @NotNull final String previousPred, final boolean ignoreStrings, final boolean translateIneq, final boolean translateMath)
	{
		LOGGER.entering(LOG_SOURCE, "preProcessRecurse", new String[]{"f = " + form, "previousPred = " + previousPred, "ignoreStrings = " + ignoreStrings, "translateIneq = " + translateIneq, "translateMath = " + translateMath});
		@NotNull StringBuilder sb = new StringBuilder();
		if (Lisp.listP(form) && !Lisp.empty(form))
		{
			@NotNull String prefix = "";
			@NotNull String head = Lisp.car(form);
			if (Formula.isQuantifier(head))
			{
				// The list of quantified variables.
				sb.append(Formula.SPACE);
				sb.append(Lisp.cadr(form));
				// The formula following the list of variables.
				sb.append(Formula.SPACE);
				sb.append(preProcessRecurse(Lisp.caddr(form), "", ignoreStrings, translateIneq, translateMath));
			}
			else
			{
				@Nullable String cdr = Lisp.cdr(form);
				if (!cdr.isEmpty())
				{
					int argCount = 1;
					for (@NotNull IterableFormula itF = new IterableFormula(cdr); !itF.empty(); itF.pop())
					{
						argCount++;
						@NotNull String head2 = itF.car();
						if (Lisp.listP(head2))
						{
							@NotNull String head22 = preProcessRecurse(head2, head, ignoreStrings, translateIneq, translateMath);
							sb.append(Formula.SPACE);
							/*
							if (!Formula.isLogicalOperator(head) &&
									!Formula.isComparisonOperator(head) &&
									!Formula.isMathFunction(head) &&
									!Formula.isFunctionalTerm(head2))
							{
								sb.append(Formula.BACKTICK);
							}
							 */
							sb.append(head22);
						}
						else
						{
							sb.append(Formula.SPACE).append(head2);
						}
					}

					/*
					if (ADD_HOLDS_PREFIX)
					{
						if (!Formula.isLogicalOperator(head) && !Formula.isQuantifierList(head, previousPred))
						{
							prefix = "holds_";
						}
						if (Formula.isFunctionalTerm(form))
						{
							prefix = "apply_";
						}
						if (head.equals("holds"))
						{
							head = "";
							argCount--;
							prefix = prefix + argCount + "__ ";
						}
						else
						{
							if (!Formula.isLogicalOperator(head) && //
									!Formula.isQuantifierList(head, previousPred) && //
									!Formula.isMathFunction(head) && //
									!Formula.isComparisonOperator(head))
							{
								prefix = prefix + argCount + "__ ";
							}
							else
							{
								prefix = "";
							}
						}
					}
					 */
				}
			}
			// (prefix+head ...
			sb.insert(0, head);
			sb.insert(0, prefix);
			sb.insert(0, Formula.LP);
			sb.append(")");
		}
		LOGGER.exiting(LOG_SOURCE, "preProcessRecurse", sb.toString());
		return sb.toString();
	}

	/**
	 * Tries to successively instantiate predicate variables and then
	 * expand row variables in this Formula, looping until no new
	 * Formulae are generated.
	 *
	 * @param f0             A Formula
	 * @param kb             The KB to be used for processing this Formula
	 * @param addHoldsPrefix If true, predicate variables are not
	 *                       instantiated
	 * @return a List of Formula(s), which could be empty.
	 */
	@NotNull
	public static List<Formula> replacePredVarsAndRowVars(@NotNull final Formula f0, @NotNull final KB kb, final boolean addHoldsPrefix)
	{
		return replacePredVarsAndRowVars(f0.form, kb, f0.errors, addHoldsPrefix);
	}

	@NotNull
	static List<Formula> replacePredVarsAndRowVars(@NotNull final String form, @NotNull final KB kb, @NotNull final Collection<String> errors, boolean addHoldsPrefix)
	{
		LOGGER.entering(LOG_SOURCE, "replacePredVarsAndRowVars", new String[]{"kb = " + kb.name, "addHoldsPrefix = " + addHoldsPrefix});
		@NotNull Formula startF = Formula.of(form);
		int prevAccumulatorSize = 0;
		@NotNull Set<Formula> accumulator = new LinkedHashSet<>();
		accumulator.add(startF);
		while (accumulator.size() != prevAccumulatorSize)
		{
			prevAccumulatorSize = accumulator.size();

			// Pred var instantiations
			// Do this if we are not adding holds prefixes.
			if (!addHoldsPrefix)
			{
				@NotNull List<Formula> working = new ArrayList<>(accumulator);
				accumulator.clear();

				for (@NotNull Formula f : working)
				{
					try
					{
						@NotNull List<Formula> instantiations = Instantiate.instantiatePredVars(f, kb);
						errors.addAll(f.getErrors());

						if (instantiations.isEmpty())
						{
							// If the accumulator is empty -- no pred var instantiations were possible -- add
							// the original formula to the accumulator for possible row var expansion below.
							accumulator.add(f);
						}
						else
						{
							// It might not be possible to instantiate all pred vars until
							// after row vars have been expanded, so we loop until no new Formulae
							// are being generated.
							accumulator.addAll(instantiations);
						}
					}
					catch (RejectException r)
					{
						// If the formula can't be instantiated at all and so has been thrown "reject", don't add anything.
						@NotNull String errStr = "No predicate instantiations";
						errors.add(errStr);
						errStr += " for " + f.form;
						LOGGER.warning(errStr);
					}
				}
			}

			// Row var expansion. Iterate over the instantiated predicate formulas,
			// doing row var expansion on each.  If no predicate instantiations can be generated, the accumulator
			// will contain just the original input formula.
			if (!accumulator.isEmpty() && accumulator.size() < AXIOM_EXPANSION_LIMIT)
			{
				@NotNull List<Formula> working = new ArrayList<>(accumulator);
				accumulator.clear();
				for (@NotNull Formula f : working)
				{
					accumulator.addAll(RowVars.expandRowVars(f, kb::getValence));
					if (accumulator.size() > AXIOM_EXPANSION_LIMIT)
					{
						LOGGER.warning("Axiom expansion limit (" + AXIOM_EXPANSION_LIMIT + ") exceeded");
						break;
					}
				}
			}
		}
		@NotNull List<Formula> result = new ArrayList<>(accumulator);
		LOGGER.exiting(LOG_SOURCE, "replacePredVarsAndRowVars", result);
		return result;
	}

	/**
	 * Adds statements of the form (instance &lt;Entity&gt; &lt;SetOrClass&gt;) if
	 * they are not already in the KB.
	 *
	 * @param kb                   The KB to be used for processing the input Formulae
	 *                             in variableReplacements
	 * @param isQuery              If true, this method just returns the initial
	 *                             input List, variableReplacements, with no additions
	 * @param variableReplacements A List of Formulae in which
	 *                             predicate variables and row variables have already been
	 *                             replaced, and to which (instance &lt;Entity&gt; &lt;SetOrClass&gt;)
	 *                             Formulae might be added
	 * @param sourceFile           inherited source file
	 * @return a List of Formula(s), which could be larger than
	 * the input List, variableReplacements, or could be empty.
	 */
	@NotNull
	public static List<Formula> addInstancesOfSetOrClass(@NotNull final KB kb, final boolean isQuery, @Nullable final List<Formula> variableReplacements, @NotNull final String sourceFile)
	{
		@NotNull List<Formula> result = new ArrayList<>();
		if ((variableReplacements != null) && !variableReplacements.isEmpty())
		{
			if (isQuery)
			{
				result.addAll(variableReplacements);
			}
			else
			{
				@NotNull Set<Formula> formulas = new LinkedHashSet<>();
				for (@NotNull Formula f : variableReplacements)
				{
					formulas.add(f);

					// Make sure every SetOrClass is stated to be such.
					if (f.listP() && !f.empty())
					{
						@NotNull String arg0 = f.car();
						int start = -1;
						if ("subclass".equals(arg0))
						{
							start = 0;
						}
						else if ("instance".equals(arg0))
						{
							start = 1;
						}
						if (start > -1)
						{
							@NotNull final List<String> args = List.of(f.getArgument(1), f.getArgument(2));
							int argsLen = args.size();
							for (int i = start; i < argsLen; i++)
							{
								String arg = args.get(i);
								if (!Formula.isVariable(arg) && !"SetOrClass".equals(arg) && Lisp.atom(arg))
								{
									// (instance arg SetOrClass)
									@NotNull String instanceForm = Formula.LP + "instance " + arg + Formula.SPACE + "SetOrClass" + Formula.RP;
									@NotNull Formula instanceF = Formula.of(instanceForm);
									instanceF.sourceFile = sourceFile;
									if (!kb.formulas.containsKey(instanceForm))
									{
										@NotNull Map<String, List<String>> cache = kb.getSortalTypeCache();
										if (cache.get(instanceForm) == null)
										{
											cache.put(instanceForm, Collections.singletonList(instanceForm));
											formulas.add(instanceF);
										}
									}
								}
							}
						}
					}
				}
				result.addAll(formulas);
			}
		}
		return result;
	}

	/**
	 * Returns true if this Formula appears not to have any of the
	 * characteristics that would cause it to be rejected during
	 * translation to TPTP form, or cause problems during inference.
	 * Otherwise, returns false.
	 *
	 * @param query true if this Formula represents a query, else
	 *              false.
	 * @return boolean
	 */
	static boolean isOkForInference(@NotNull Formula f0, boolean query)
	{
		// kb isn't used yet, because the checks below are purely
		// syntactic.  But it probably will be used in the future.
		// (<relation> ?X ...) - no free variables in an
		// atomic formula that doesn't contain a string
		// unless the formula is a query.
		// The formula does not contain a string.
		// The formula contains a free variable.
		// ... add more patterns here, as needed.
		return !(// (equal ?X ?Y ?Z ...) - equal is strictly binary.
				// No longer necessary?  NS: 2009-06-12
				// text.matches(".*\\(\\s*equal\\s+\\?*\\w+\\s+\\?*\\w+\\s+\\?*\\w+.*")

				// The formula contains non-ASCII characters.
				StringUtil.containsNonAsciiChars(f0.form) || ( //
						!query && !Formula.isLogicalOperator(f0.car()) &&  //
								f0.form.indexOf(Formula.DOUBLE_QUOTE_CHAR) == -1 &&  //
								f0.form.matches(".*\\?\\w+.*")));
	}
}