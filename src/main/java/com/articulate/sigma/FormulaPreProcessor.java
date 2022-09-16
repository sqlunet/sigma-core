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

	private static final String LOG_SOURCE = "Formula";

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
			String[] params = {"f = " + f, "previousPred = " + previousPred, "ignoreStrings = " + ignoreStrings, "translateIneq = " + translateIneq, "translateMath = " + translateMath};
			logger.entering(LOG_SOURCE, "preProcessRecurse", params);
		}
		StringBuilder sb = new StringBuilder();
		try
		{
			if (f.listP() && !f.empty())
			{
				String prefix = "";
				String pred = f.car();
				// Formula predF = new Formula();
				// predF.read(pred);
				if (Formula.isQuantifier(pred))
				{
					// The list of quantified variables.
					sb.append(" ");
					sb.append(f.cadr());
					// The formula following the list of variables.
					String next = f.caddr();
					Formula nextF = new Formula();
					nextF.set(next);
					sb.append(" ");
					sb.append(preProcessRecurse(nextF, "", ignoreStrings, translateIneq, translateMath));
				}
				else
				{
					Formula restF = f.cdrAsFormula();
					if (restF != null)
					{
						int argCount = 1;
						while (!restF.empty())
						{
							argCount++;
							String arg = restF.car();

							Formula argF = new Formula();
							argF.set(arg);
							if (argF.listP())
							{
								String res = preProcessRecurse(argF, pred, ignoreStrings, translateIneq, translateMath);
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
							restF.text = restF.cdr();
						}
						if (KBManager.getMgr().getPref("holdsPrefix").equals("yes"))
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
			String[] params = {"isQuery = " + isQuery, "kb = " + kb.name};
			logger.entering(LOG_SOURCE, "preProcess", params);
		}
		List<Formula> results = new ArrayList<>();
		try
		{
			if (!f0.text.isEmpty())
			{
				KBManager mgr = KBManager.getMgr();
				if (!f0.isBalancedList())
				{
					String errStr = "Unbalanced parentheses or quotes";
					f0.errors.add(errStr);
					errStr += " in " + f0.text;
					logger.warning(errStr);
					// mgr.setError(mgr.getError() + " " + errStr);
					return results;
				}
				boolean ignoreStrings = false;
				boolean translateIneq = true;
				boolean translateMath = true;
				Formula f = new Formula();
				f.set(f0.text);
				if (StringUtil.containsNonAsciiChars(f.text))
				{
					f.text = StringUtil.replaceNonAsciiChars(f.text);
				}

				boolean addHoldsPrefix = mgr.getPref("holdsPrefix").equalsIgnoreCase("yes");
				List<Formula> variableReplacements = f.replacePredVarsAndRowVars(kb, addHoldsPrefix);
				f0.errors.addAll(f.getErrors());

				List<Formula> accumulator = f0.addInstancesOfSetOrClass(kb, isQuery, variableReplacements);
				// Iterate over the formulae resulting from predicate variable instantiation and row variable expansion,
				// passing each to preProcessRecurse for further processing.
				if (!accumulator.isEmpty())
				{
					boolean addSortals = mgr.getPref("typePrefix").equalsIgnoreCase("yes");
					for (Formula newF : accumulator)
					{
						if (addSortals && !isQuery && newF.text.matches(".*\\?\\w+.*"))  // isLogicalOperator(arg0) ||
						{
							newF.set(newF.addTypeRestrictions(kb));
						}

						String newFStr = preProcessRecurse(newF, "", ignoreStrings, translateIneq, translateMath);
						newF.set(newFStr);
						f0.errors.addAll(newF.getErrors());
						if (newF.isOkForInference(isQuery))
						{
							newF.sourceFile = f0.sourceFile;
							results.add(newF);
						}
						else
						{
							String errStr = "Rejected formula for inference";
							f0.errors.add(errStr);
							errStr += " in " + newFStr;
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
