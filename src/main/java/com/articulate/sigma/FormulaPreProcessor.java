package com.articulate.sigma;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Pre-processing for sending to the inference engine.
 */
public class FormulaPreProcessor
{
	private static final Logger logger = Logger.getLogger(FormulaPreProcessor.class.getName());

	private static final String LOG_SOURCE = "FormulaPP";

	/**
	 * For any given formula, stop generating new pred var
	 * instantiations and row var expansions if this threshold value
	 * has been exceeded.  The default value is 2000.
	 */
	private static final int AXIOM_EXPANSION_LIMIT = 2000;

	/**
	 * Pre-process a formula before sending it to the theorem
	 * prover. This includes ignoring meta-knowledge like
	 * documentation strings, translating mathematical operators,
	 * quoting higher-order formulas, expanding row variables and
	 * prepending the 'holds__' predicate.
	 *
	 * @param f0      formula to preprocess
	 * @param isQuery If true the Formula is a query and should be
	 *                existentially quantified, else the Formula is a
	 *                statement and should be universally quantified
	 * @param kb      The KB to be used for processing this Formula
	 * @return a List of Formula(s), which could be empty.
	 */
	@NotNull
	public static List<Formula> preProcess(@NotNull Formula f0, boolean isQuery, @NotNull KB kb)
	{
		if (logger.isLoggable(Level.FINER))
		{
			@NotNull String[] params = {"isQuery = " + isQuery, "kb = " + kb.name};
			logger.entering(LOG_SOURCE, "preProcess", params);
		}
		@NotNull List<Formula> results = new ArrayList<>();
		try
		{
			if (!f0.form.isEmpty())
			{
				@NotNull KBManager mgr = KBManager.getInstance();
				if (!f0.isBalancedList())
				{
					@NotNull String errStr = "Unbalanced parentheses or quotes";
					f0.errors.add(errStr);
					errStr += " in " + f0.form;
					logger.warning(errStr);
					// mgr.setError(mgr.getError() + " " + errStr);
					return results;
				}
				boolean ignoreStrings = false;
				boolean translateIneq = true;
				boolean translateMath = true;
				@NotNull Formula f = Formula.of(f0.form);
				if (StringUtil.containsNonAsciiChars(f.form))
				{
					f = Formula.of(StringUtil.replaceNonAsciiChars(f.form));
				}

				boolean addHoldsPrefix = mgr.getPref("holdsPrefix").equalsIgnoreCase("yes");
				@NotNull List<Formula> variableReplacements = replacePredVarsAndRowVars(f0, kb, addHoldsPrefix);
				f0.errors.addAll(f.getErrors());

				// Iterate over the formulae resulting from predicate variable instantiation and row variable expansion,
				// passing each to preProcessRecurse for further processing.
				@NotNull List<Formula> accumulator = addInstancesOfSetOrClass(kb, isQuery, variableReplacements, f0.sourceFile);
				if (!accumulator.isEmpty())
				{
					boolean addSortals = mgr.getPref("typePrefix").equalsIgnoreCase("yes");
					for (@NotNull Formula f1 : accumulator)
					{
						@NotNull Formula newF1 = Formula.of(f1.form);
						if (addSortals && !isQuery && newF1.form.matches(".*\\?\\w+.*"))  // isLogicalOperator(arg0) ||
						{
							newF1 = Formula.of(newF1.addTypeRestrictions(kb));
						}

						@NotNull String newForm = preProcessRecurse(newF1, "", ignoreStrings, translateIneq, translateMath);
						@NotNull Formula newF2 = Formula.of(newForm);
						f0.errors.addAll(newF2.getErrors());
						if (newF2.isOkForInference(isQuery))
						{
							newF2.sourceFile = f0.sourceFile;
							results.add(newF2);
						}
						else
						{
							@NotNull String errStr = "Rejected formula for inference";
							f0.errors.add(errStr);
							errStr += " in " + newForm;
							logger.warning(errStr);
							// mgr.setError(mgr.getError() + " " +  errStr);
						}
					}
				}
			}
		}
		catch (Exception ex)
		{
			logger.warning(ex.getMessage());
			ex.printStackTrace();
		}
		logger.exiting(LOG_SOURCE, "preProcess", results);
		return results;
	}

	/**
	 * Pre-process a formula before sending it to the theorem prover. This includes
	 * ignoring meta-knowledge like documentation strings, translating
	 * mathematical operators, quoting higher-order formulas, expanding
	 * row variables and prepending the 'holds__' predicate.
	 *
	 * @return a List of Formula(s)
	 */
	@NotNull
	private static String preProcessRecurse(@NotNull Formula f, @NotNull String previousPred, boolean ignoreStrings, boolean translateIneq, boolean translateMath)
	{
		if (logger.isLoggable(Level.FINER))
		{
			@NotNull String[] params = {"f = " + f, "previousPred = " + previousPred, "ignoreStrings = " + ignoreStrings, "translateIneq = " + translateIneq, "translateMath = " + translateMath};
			logger.entering(LOG_SOURCE, "preProcessRecurse", params);
		}
		@NotNull StringBuilder sb = new StringBuilder();
		try
		{
			if (f.listP() && !f.empty())
			{
				@NotNull String prefix = "";
				@NotNull String pred = f.car();
				if (Formula.isQuantifier(pred))
				{
					// The list of quantified variables.
					sb.append(" ");
					sb.append(f.cadr());
					// The formula following the list of variables.
					@NotNull String next = f.caddr();
					@NotNull Formula nextF = Formula.of(next);
					sb.append(" ");
					sb.append(preProcessRecurse(nextF, "", ignoreStrings, translateIneq, translateMath));
				}
				else
				{
					@Nullable Formula restF = f.cdrAsFormula();
					if (restF != null)
					{
						int argCount = 1;
						for (@NotNull IterableFormula restF2 = new IterableFormula(restF.form); !restF2.empty(); restF2.pop())
						{
							argCount++;
							@NotNull String arg = restF2.car();

							@NotNull Formula argF = Formula.of(arg);
							if (argF.listP())
							{
								@NotNull String res = preProcessRecurse(argF, pred, ignoreStrings, translateIneq, translateMath);
								sb.append(" ");
								if (!Formula.isLogicalOperator(pred) && !Formula.isComparisonOperator(pred) && !Formula.isMathFunction(pred) && !argF.isFunctionalTerm())
								{
									sb.append("`");
								}
								sb.append(res);
							}
							else
							{
								sb.append(" ").append(arg);
							}
						}

						if (KBManager.getInstance().getPref("holdsPrefix").equals("yes"))
						{
							if (!Formula.isLogicalOperator(pred) && !Formula.isQuantifierList(pred, previousPred))
							{
								prefix = "holds_";
							}
							if (f.isFunctionalTerm())
							{
								prefix = "apply_";
							}
							if (pred.equals("holds"))
							{
								pred = "";
								argCount--;
								prefix = prefix + argCount + "__ ";
							}
							else
							{
								if (!Formula.isLogicalOperator(pred) && //
										!Formula.isQuantifierList(pred, previousPred) && //
										!Formula.isMathFunction(pred) && //
										!Formula.isComparisonOperator(pred))
								{
									prefix = prefix + argCount + "__ ";
								}
								else
								{
									prefix = "";
								}
							}
						}
					}
				}
				sb.insert(0, pred);
				sb.insert(0, prefix);
				sb.insert(0, "(");
				sb.append(")");
			}
		}
		catch (Exception ex)
		{
			logger.warning(ex.getMessage());
			ex.printStackTrace();
		}
		logger.exiting(LOG_SOURCE, "preProcessRecurse", sb.toString());
		return sb.toString();
	}

	/**
	 * Tries to successively instantiate predicate variables and then
	 * expand row variables in this Formula, looping until no new
	 * Formulae are generated.
	 *
	 * @param kb             The KB to be used for processing this Formula
	 * @param addHoldsPrefix If true, predicate variables are not
	 *                       instantiated
	 * @return a List of Formula(s), which could be empty.
	 */
	@NotNull
	static List<Formula> replacePredVarsAndRowVars(@NotNull final Formula f0, @NotNull final KB kb, boolean addHoldsPrefix)
	{
		if (logger.isLoggable(Level.FINER))
		{
			@NotNull String[] params = {"kb = " + kb.name, "addHoldsPrefix = " + addHoldsPrefix};
			logger.entering(LOG_SOURCE, "replacePredVarsAndRowVars", params);
		}
		@NotNull Formula startF = Formula.copy(f0);
		int prevAccumulatorSize = 0;
		@NotNull Set<Formula> accumulator = new LinkedHashSet<>();
		accumulator.add(startF);
		while (accumulator.size() != prevAccumulatorSize)
		{
			prevAccumulatorSize = accumulator.size();

			// Do pred var instantiations if we are not adding holds prefixes.
			if (!addHoldsPrefix)
			{
				@NotNull List<Formula> working = new ArrayList<>(accumulator);
				accumulator.clear();

				for (@NotNull Formula f : working)
				{
					try
					{
						@NotNull List<Formula> instantiations = f.instantiatePredVars(kb);
						f0.errors.addAll(f.getErrors());

						// logger.finest("instantiations == " + instantiations);
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
					catch (Formula.RejectException r)
					{
						// If the formula can't be instantiated at all and so has been thrown "reject", don't add anything.
						@NotNull String errStr = "No predicate instantiations";
						f0.errors.add(errStr);
						errStr += " for " + f.form;
						logger.warning(errStr);
					}
				}
			}
			// Row var expansion. Iterate over the instantiated predicate formulas,
			// doing row var expansion on each.  If no predicate instantiations can be generated, the accumulator
			// will contain just the original input formula.
			if (!accumulator.isEmpty() && (accumulator.size() < AXIOM_EXPANSION_LIMIT))
			{
				@NotNull List<Formula> working = new ArrayList<>(accumulator);
				accumulator.clear();
				for (@NotNull Formula f : working)
				{
					accumulator.addAll(RowVars.expandRowVars(f, r -> kb.getValence(r)));
					if (accumulator.size() > AXIOM_EXPANSION_LIMIT)
					{
						logger.warning("Axiom expansion limit (" + AXIOM_EXPANSION_LIMIT + ") exceeded");
						break;
					}
				}
			}
		}
		@NotNull List<Formula> result = new ArrayList<>(accumulator);
		logger.exiting(LOG_SOURCE, "replacePredVarsAndRowVars", result);
		return result;
	}

	/**
	 * Adds statements of the form (instance &lt;Entity> &lt;SetOrClass>) if
	 * they are not already in the KB.
	 *
	 * @param kb                   The KB to be used for processing the input Formulae
	 *                             in variableReplacements
	 * @param isQuery              If true, this method just returns the initial
	 *                             input List, variableReplacements, with no additions
	 * @param variableReplacements A List of Formulae in which
	 *                             predicate variables and row variables have already been
	 *                             replaced, and to which (instance <Entity> <SetOrClass>)
	 *                             Formulae might be added
	 * @param sourceFile           inherited source file
	 * @return a List of Formula(s), which could be larger than
	 * the input List, variableReplacements, or could be empty.
	 */
	@NotNull
	public static List<Formula> addInstancesOfSetOrClass(@NotNull final KB kb, boolean isQuery, @Nullable List<Formula> variableReplacements, @Nullable String sourceFile)
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
				@NotNull Set<Formula> formulae = new LinkedHashSet<>();
				for (@NotNull Formula f : variableReplacements)
				{
					formulae.add(f);

					// Make sure every SetOrClass is stated to be such.
					if (f.listP() && !f.empty())
					{
						@NotNull String arg0 = f.car();
						int start = -1;
						if (arg0.equals("subclass"))
						{
							start = 0;
						}
						else if (arg0.equals("instance"))
						{
							start = 1;
						}
						if (start > -1)
						{
							@NotNull List<String> args = Arrays.asList(f.getArgument(1), f.getArgument(2));
							int argsLen = args.size();
							for (int i = start; i < argsLen; i++)
							{
								String arg = args.get(i);
								if (!Formula.isVariable(arg) && !arg.equals("SetOrClass") && Lisp.atom(arg))
								{
									@NotNull StringBuilder sb = new StringBuilder();
									sb.setLength(0);
									sb.append("(instance ");
									sb.append(arg);
									sb.append(" SetOrClass)");
									@NotNull String ioStr = sb.toString();
									@NotNull Formula ioF = Formula.of(ioStr);
									ioF.sourceFile = sourceFile;
									if (!kb.formulaMap.containsKey(ioStr))
									{
										@NotNull Map<String, List<String>> stc = kb.getSortalTypeCache();
										if (stc.get(ioStr) == null)
										{
											stc.put(ioStr, Collections.singletonList(ioStr));
											formulae.add(ioF);
										}
									}
								}
							}
						}
					}
				}
				result.addAll(formulae);
			}
		}
		return result;
	}
}
