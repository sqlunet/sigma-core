package com.articulate.sigma;

import java.util.ArrayList;
import java.util.List;
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
	 * Pre-process a formula before sending it to the theorem prover. This includes
	 * ignoring meta-knowledge like documentation strings, translating
	 * mathematical operators, quoting higher-order formulas, expanding
	 * row variables and prepending the 'holds__' predicate.
	 *
	 * @return a List of Formula(s)
	 */
	@NotNull
	static private String preProcessRecurse(@NotNull Formula f, @NotNull String previousPred, boolean ignoreStrings, boolean translateIneq, boolean translateMath)
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
	static public List<Formula> preProcess(@NotNull Formula f0, boolean isQuery, @NotNull KB kb)
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
				@NotNull List<Formula> variableReplacements = f.replacePredVarsAndRowVars(kb, addHoldsPrefix);
				f0.errors.addAll(f.getErrors());

				// Iterate over the formulae resulting from predicate variable instantiation and row variable expansion,
				// passing each to preProcessRecurse for further processing.
				@NotNull List<Formula> accumulator = f0.addInstancesOfSetOrClass(kb, isQuery, variableReplacements);
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
}
