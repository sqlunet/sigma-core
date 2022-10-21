package com.articulate.sigma.noncore;

import com.articulate.sigma.*;

import java.util.*;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Instantiate
{
	private static final String LOG_SOURCE = "Instantiate";

	private static final Logger logger = Logger.getLogger(Instantiate.class.getName());

	/**
	 * This method returns a triple of query answer literals.
	 * The first element is a List of query literals that might be
	 * used to simplify the Formula to be instantiated.
	 * The second element is the query literal (List) that will be used as a
	 * template for doing the variable substitutions.
	 * All subsequent elements are ground literals (Lists).
	 *
	 * @param kb        A KB to query for answers.
	 * @param queryLits A List of query literals.  The first item in
	 *                  the list will be a SUO-KIF variable (String), which indexes the
	 *                  list.  Each subsequent item is a query literal (List).
	 * @return A triple of literals, or null if no query answers can be found.
	 */
	private static Tuple.Triple<List<List<String>>, List<String>, Collection<List<String>>> computeSubstitutionTuples(@NotNull final Formula f0, @Nullable final KB kb, @Nullable final Tuple.Pair<String, List<List<String>>> queryLits)
	{
		if (kb != null && queryLits != null)
		{
			// Variable
			String idxVar = queryLits.first;

			// Sort the query lits by number of variables.
			@NotNull List<List<String>> sortedQLits = new ArrayList<>(queryLits.second);
			sortedQLits.remove(0);
			if (sortedQLits.size() > 1)
			{
				@NotNull Comparator<List<String>> comp = (o1, o2) -> {
					@NotNull Integer c1 = Variables.getVarCount(o1);
					@NotNull Integer c2 = Variables.getVarCount(o2);
					return c1.compareTo(c2);
				};
				sortedQLits.sort(Collections.reverseOrder(comp));
			}

			// Put instance literals last.
			@NotNull List<List<String>> ioLits = new ArrayList<>();
			@NotNull List<List<String>> qLits = new ArrayList<>(sortedQLits);
			sortedQLits.clear();

			for (@NotNull List<String> ql : qLits)
			{
				if (ql.get(0).equals("instance"))
				{
					ioLits.add(ql);
				}
				else
				{
					sortedQLits.add(ql);
				}
			}
			sortedQLits.addAll(ioLits);

			// Literals that will be used to try to simplify the formula before pred var instantiation.
			@NotNull List<List<String>> simplificationLits = new ArrayList<>();

			// The literal that will serve as the pattern for extracting var replacement terms from answer/ literals.
			@Nullable List<String> keyLit = null;

			// The list of answer literals retrieved using the query lits, possibly built up via a sequence of multiple queries.
			@Nullable Collection<List<String>> answers = null;

			@NotNull Set<String> working = new HashSet<>();

			boolean satisfiable = true;
			boolean tryNextQueryLiteral = true;

			// The first query lit for which we get an answer is the key lit.
			for (int i = 0; i < sortedQLits.size() && tryNextQueryLiteral; i++)
			{
				List<String> ql = sortedQLits.get(i);
				@NotNull Collection<Formula> accumulator = kb.askWithLiteral(ql);
				satisfiable = !accumulator.isEmpty();
				tryNextQueryLiteral = (satisfiable || (Variables.getVarCount(ql) > 1));
				// !((String)(ql.get(0))).equals("instance")
				if (satisfiable)
				{
					simplificationLits.add(ql);
					if (keyLit == null)
					{
						keyLit = ql;
						answers = KB.formulasToLists(accumulator);
					}
					else
					{  // if (accumulator.size() < answers.size()) {
						@NotNull Collection<List<String>> accumulator2 = KB.formulasToLists(accumulator);

						// Winnow the answers list.
						working.clear();
						int varPos = ql.indexOf(idxVar);
						for (@NotNull List<String> ql2 : accumulator2)
						{
							String term = ql2.get(varPos);
							// if (!term.endsWith("Fn")) {
							working.add(term);
							// }
						}

						accumulator2.clear();
						accumulator2.addAll(answers);
						answers.clear();
						varPos = keyLit.indexOf(idxVar);
						for (@NotNull List<String> ql2 : accumulator2)
						{
							String term = ql2.get(varPos);
							if (working.contains(term))
							{
								answers.add(ql2);
							}
						}
					}
				}
			}
			if (satisfiable && (keyLit != null))
			{
				@NotNull Tuple.Triple<List<List<String>>, List<String>, Collection<List<String>>> result = new Tuple.Triple<>();
				result.first = simplificationLits;
				result.second = keyLit;
				result.third = answers;
				return result;
			}
		}
		return null;
	}

	/**
	 * This method returns a List in which each element is
	 * a pair.  The first item of each pair is a variable.
	 * The second item in each pair is a list of query literals
	 * (Lists).
	 *
	 * @param kb         The KB to use for computing variable type signatures.
	 * @param varTypeMap A Map from variables to their types, as
	 *                   explained in the javadoc entry for gatherPredVars(kb)
	 * @return A List, or null if the input formula contains no
	 * predicate variables.
	 */
	@NotNull
	private static List<Tuple.Pair<String, List<List<String>>>> prepareIndexedQueryLiterals(@NotNull final Formula f0, @NotNull final KB kb, @Nullable final Map<String, List<String>> varTypeMap)
	{
		if (Instantiate.logger.isLoggable(Level.FINER))
		{
			@NotNull String[] params = {"kb = " + kb.name, "varTypeMap = " + varTypeMap};
			Instantiate.logger.entering(Instantiate.LOG_SOURCE, "prepareIndexedQueryLiterals", params);
		}
		@NotNull List<Tuple.Pair<String, List<List<String>>>> result = new ArrayList<>();
		@NotNull Map<String, List<String>> varsWithTypes = varTypeMap != null ? varTypeMap : gatherPredVars(f0, kb);
		// logger.finest("varsWithTypes = " + varsWithTypes);

		if (!varsWithTypes.isEmpty())
		{
			List<String> yOrN = varsWithTypes.get("arg0");
			if (yOrN.size() == 1 && "yes".equalsIgnoreCase(yOrN.get(0)))
			{
				// Try to simplify the formula.
				for (@NotNull String var : varsWithTypes.keySet())
				{
					if (Formula.isVariable(var))
					{
						List<String> varWithTypes = varsWithTypes.get(var);
						@Nullable Tuple.Pair<String, List<List<String>>> indexedQueryLits = gatherPredVarQueryLits(f0, kb, varWithTypes);
						if (indexedQueryLits != null)
						{
							result.add(indexedQueryLits);
						}
					}
				}
			}
			// Else if the formula doesn't contain any arg0 pred vars, do nothing.
		}
		Instantiate.logger.exiting(Instantiate.LOG_SOURCE, "prepareIndexedQueryLiterals", result);
		return result;
	}

	/**
	 * This method tries to remove literals from the Formula that
	 * match litArr.  It is intended for use in simplification of this
	 * Formula during predicate variable instantiation, and so only
	 * attempts removals that are likely to be safe in that context.
	 *
	 * @param lits A List object representing a SUO-KIF atomic
	 *             formula.
	 * @return A new Formula with at least some occurrences of litF
	 * removed, or the original Formula if no removals are possible.
	 */
	@NotNull
	private static Formula maybeRemoveMatchingLits(@NotNull final Formula f0, List<String> lits)
	{
		@Nullable Formula f = KB.literalListToFormula(lits);
		if (f != null)
		{
			return maybeRemoveMatchingLits(f0, f);
		}
		else
		{
			return f0;
		}
	}

	/**
	 * This method tries to remove literals from the Formula that
	 * match litF.  It is intended for use in simplification of this
	 * Formula during predicate variable instantiation, and so only
	 * attempts removals that are likely to be safe in that context.
	 *
	 * @param litF A SUO-KIF literal (atomic Formula).
	 * @return A new Formula with at least some occurrences of litF
	 * removed, or the original Formula if no removals are possible.
	 */
	@NotNull
	private static Formula maybeRemoveMatchingLits(@NotNull final Formula f0, @NotNull final Formula litF)
	{
		Instantiate.logger.entering(Instantiate.LOG_SOURCE, "maybeRemoveMatchingLits", litF);
		@Nullable Formula result = null;
		@NotNull Formula f = f0;
		if (f.listP() && !f.empty())
		{
			@NotNull StringBuilder litBuf = new StringBuilder();
			@NotNull String arg0 = f.car();
			if (Arrays.asList(Formula.IF, Formula.IFF).contains(arg0))
			{
				@NotNull String arg1 = f.getArgument(1);
				@NotNull String arg2 = f.getArgument(2);
				if (arg1.equals(litF.form))
				{
					@NotNull Formula arg2F = Formula.of(arg2);
					litBuf.append(maybeRemoveMatchingLits(arg2F, litF).form);
				}
				else if (arg2.equals(litF.form))
				{
					@NotNull Formula arg1F = Formula.of(arg1);
					litBuf.append(maybeRemoveMatchingLits(arg1F, litF).form);
				}
				else
				{
					@NotNull Formula arg1F = Formula.of(arg1);
					@NotNull Formula arg2F = Formula.of(arg2);
					litBuf.append("(") //
							.append(arg0) //
							.append(" ") //
							.append(maybeRemoveMatchingLits(arg1F, litF).form) //
							.append(" ") //
							.append(maybeRemoveMatchingLits(arg2F, litF).form) //
							.append(")");
				}
			}
			else if (Formula.isQuantifier(arg0) || arg0.equals("holdsDuring") || arg0.equals("KappaFn"))
			{
				@NotNull Formula arg2F = Formula.of(f.caddr());
				litBuf.append("(").append(arg0).append(" ").append(f.cadr()).append(" ").append(maybeRemoveMatchingLits(arg2F, litF).form).append(")");
			}
			else if (Formula.isCommutative(arg0))
			{
				@NotNull List<String> lits = f.elements();
				lits.remove(litF.form);
				@NotNull StringBuilder args = new StringBuilder();
				int len = lits.size();
				for (int i = 1; i < len; i++)
				{
					@NotNull Formula argF = Formula.of(lits.get(i));
					args.append(" ").append(maybeRemoveMatchingLits(argF, litF).form);
				}
				if (len > 2)
				{
					args = new StringBuilder(("(" + arg0 + args + ")"));
				}
				else
				{
					args = new StringBuilder(args.toString().trim());
				}
				litBuf.append(args);
			}
			else
			{
				litBuf.append(f.form);
			}
			result = Formula.of(litBuf.toString());
		}
		if (result == null)
		{
			result = f0;
		}
		Instantiate.logger.exiting(Instantiate.LOG_SOURCE, "maybeRemoveMatchingLits", result);
		return result;
	}

	/**
	 * This method collects and returns all predicate variables that
	 * occur in the Formula.
	 *
	 * @param f0 a Formula
	 * @param kb The KB to be used for computations involving
	 *           assertions.
	 * @return a Map in which the keys are predicate variables,
	 * and the values are Lists containing one or more class
	 * names that indicate the type constraints tha apply to the
	 * variable.  If no predicate variables can be gathered from the
	 * Formula, the Map will be empty.  The first element in each
	 * List is the variable itself.  Subsequent elements are the
	 * types of the variable.  If no types for the variable can be
	 * determined, the List will contain just the variable.
	 */
	@NotNull
	protected static Map<String, List<String>> gatherPredVars(@NotNull final Formula f0, @NotNull final KB kb)
	{
		Instantiate.logger.entering(Instantiate.LOG_SOURCE, "gatherPredVars", kb.name);
		@NotNull Map<String, List<String>> result = new HashMap<>();
		if (!f0.form.isEmpty())
		{
			@NotNull List<Formula> working = new ArrayList<>();
			@NotNull List<Formula> accumulator = new ArrayList<>();
			if (f0.listP() && !f0.empty())
			{
				accumulator.add(f0);
			}
			while (!accumulator.isEmpty())
			{
				working.clear();
				working.addAll(accumulator);
				accumulator.clear();

				for (@NotNull Formula f : working)
				{
					int len = f.listLength();
					@NotNull String arg0 = f.getArgument(0);
					if (Formula.isQuantifier(arg0) || arg0.equals("holdsDuring") || arg0.equals("KappaFn"))
					{
						if (len > 2)
						{
							@NotNull String arg2 = f.getArgument(2);
							@NotNull Formula newF = Formula.of(arg2);
							if (f.listP() && !f.empty())
							{
								accumulator.add(newF);
							}
						}
						else
						{
							Instantiate.logger.warning("Malformed?: " + f.form);
						}
					}
					else if (arg0.equals("holds"))
					{
						accumulator.add(f.cdrAsFormula());
					}
					else if (Formula.isVariable(arg0))
					{
						List<String> vals = result.get(arg0);
						if (vals == null)
						{
							vals = new ArrayList<>();
							result.put(arg0, vals);
							vals.add(arg0);
						}
						// Record the fact that we found at least one variable in the arg0 position.
						result.put("arg0", Collections.singletonList("yes"));
					}
					else
					{
						boolean[] signature = kb.getRelnArgSignature(arg0);
						for (int j = 1; j < len; j++)
						{
							@NotNull String argN = f.getArgument(j);
							if ((signature != null) && (signature.length > j) && signature[j] && Formula.isVariable(argN))
							{
								List<String> vals = result.get(argN);
								if (vals == null)
								{
									vals = new ArrayList<>();
									result.put(argN, vals);
									vals.add(argN);
								}
								@Nullable String argType = kb.getArgType(arg0, j);
								if (!((argType == null) || vals.contains(argType)))
								{
									vals.add(argType);
								}
							}
							else
							{
								@NotNull Formula argF = Formula.of(argN);
								if (argF.listP() && !argF.empty())
								{
									accumulator.add(argF);
								}
							}
						}
					}
				}
			}
		}
		Instantiate.logger.exiting(Instantiate.LOG_SOURCE, "gatherPredVars", result);
		return result;
	}

	/**
	 * This method collects and returns literals likely to be of use
	 * as templates for retrieving predicates to be substituted for
	 * var.
	 *
	 * @param varWithTypes A List containing a variable followed,
	 *                     optionally, by class names indicating the type of the variable.
	 * @return A pair of literals (Lists) with var as first.
	 * The element of the pair is the variable (String).
	 * The second element is a List corresponding to SUO-KIF
	 * formulas, which will be used as query templates.
	 */
	@Nullable
	private static Tuple.Pair<String, List<List<String>>> gatherPredVarQueryLits(@NotNull final Formula f0, @NotNull final KB kb, @NotNull final List<String> varWithTypes)
	{
		@NotNull Tuple.Pair<String, List<List<String>>> result = new Tuple.Pair<>();
		String var = varWithTypes.get(0);
		@NotNull Set<String> added = new HashSet<>();
		@Nullable Map<String, String> varMap = f0.getVarMap();

		// Get the clauses for this Formula.
		@Nullable List<Clause> clauses = f0.getClauses();
		if (clauses != null)
		{
			for (@NotNull Clause clause : clauses)
			{
				List<Formula> negLits = clause.negativeLits;
				// List<Formula> posLits = clause.positiveLits;
				if (!negLits.isEmpty())
				{
					int cim = 1;
					for (int ci = 0; ci < cim; ci++)
					{
						// Try the negLits first.  Then try the posLits only if there still are no results.
						@NotNull @SuppressWarnings("ConstantConditions") List<Formula> lit = ci == 0 ? clause.negativeLits : clause.positiveLits;
						for (@NotNull Formula f : lit)
						{
							if (f.form.matches(".*SkFn\\s+\\d+.*") || f.form.matches(".*Sk\\d+.*"))
							{
								continue;
							}
							int fLen = f.listLength();
							@NotNull String arg0 = f.getArgument(0);
							if (!arg0.isEmpty())
							{
								// If arg0 corresponds to var, then var has to be of type Predicate, not of types Function or List.
								if (Formula.isVariable(arg0))
								{
									@Nullable String origVar = Variables.getOriginalVar(arg0, varMap);
									if (origVar != null && origVar.equals(var) && !varWithTypes.contains("Predicate"))
									{
										varWithTypes.add("Predicate");
									}
								}
								else
								{
									@NotNull List<String> queryLit = new ArrayList<>();
									queryLit.add(arg0);
									boolean foundVar = false;
									for (int i = 1; i < fLen; i++)
									{
										@Nullable String arg = f.getArgument(i);
										if (!Lisp.listP(arg))
										{
											if (Formula.isVariable(arg))
											{
												arg = Variables.getOriginalVar(arg, varMap);
												if (arg != null && arg.equals(var))
												{
													foundVar = true;
												}
											}
											queryLit.add(arg);
										}
									}
									if (queryLit.size() != fLen)
									{
										continue;
									}
									// If the literal does not start with a variable or with "holds" and does not
									// contain Skolem terms, but does contain the variable in which we're interested,
									// it is probably suitable as a query template, or might serve as a starting
									// place.  Use it, or a literal obtained with it.
									if (isPossibleRelnArgQueryPred(f0, kb, arg0) && foundVar)
									{
										// || arg0.equals("disjoint"))
										String term = "";
										if (queryLit.size() > 2)
										{
											term = queryLit.get(2);
										}
										if (!(arg0.equals("instance") && term.equals("Relation")))
										{
											@NotNull String queryLitStr = queryLit.toString();
											if (!added.contains(queryLitStr))
											{
												result.second.add(queryLit);
												added.add(queryLitStr);
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
		// If we have previously collected type info for the variable, convert that info query lits now.
		int vtLen = varWithTypes.size();
		if (vtLen > 1)
		{
			for (int j = 1; j < vtLen; j++)
			{
				String argType = varWithTypes.get(j);
				if (!argType.equals("Relation"))
				{
					@NotNull List<String> queryLit = new ArrayList<>();
					queryLit.add("instance");
					queryLit.add(var);
					queryLit.add(argType);
					@NotNull String qlString = queryLit.toString();
					if (!added.contains(qlString))
					{
						result.second.add(queryLit);
						added.add(qlString);
					}
				}
			}
		}
		// Add the variable to the pair
		result.first = var;
		// return null if it does not contain any query literals.
		if (!result.second.isEmpty())
		{
			return null;
		}
		return result;
	}

	/**
	 * Returns a List of the Formulae that result from replacing
	 * all arg0 predicate variables in the input Formula with
	 * predicate names.
	 *
	 * @param f0 A Formula.
	 * @param kb A KB that is used for processing the Formula.
	 * @return A List of Formulas, or an empty List if no instantiations can be generated.
	 * @throws RejectException reject exception
	 */
	@NotNull
	public static List<Formula> instantiatePredVars(@NotNull final Formula f0, @NotNull final KB kb) throws RejectException
	{
		@NotNull List<Formula> result = new ArrayList<>();
		try
		{
			if (f0.listP())
			{
				@NotNull String arg0 = f0.getArgument(0);
				// First we do some checks to see if it is worth processing the formula.
				if (Formula.isLogicalOperator(arg0) && f0.form.matches(".*\\(\\s*\\?\\w+.*"))
				{
					// Get all pred vars, and then compute query lits for the pred vars, indexed by var.
					@NotNull Map<String, List<String>> varsWithTypes = gatherPredVars(f0, kb);
					if (!varsWithTypes.containsKey("arg0"))
					{
						// The formula has no predicate variables in arg0 position, so just return it.
						result.add(f0);
					}
					else
					{
						@NotNull List<Tuple.Pair<String, List<List<String>>>> indexedQueryLits = prepareIndexedQueryLiterals(f0, kb, varsWithTypes);
						@NotNull List<Tuple.Triple<List<List<String>>, List<String>, Collection<List<String>>>> substForms = new ArrayList<>();

						// First, gather all substitutions.
						for (Tuple.Pair<String, List<List<String>>> varQueryTuples : indexedQueryLits)
						{
							Tuple.Triple<List<List<String>>, List<String>, Collection<List<String>>> substTuples = computeSubstitutionTuples(f0, kb, varQueryTuples);
							if (substTuples != null)
							{
								if (substForms.isEmpty())
								{
									substForms.add(substTuples);
								}
								else
								{
									int stSize = substTuples.third.size();

									int sfSize = substForms.size();
									int sfLast = (sfSize - 1);
									for (int i = 0; i < sfSize; i++)
									{
										int iSize = substForms.get(i).third.size();
										if (stSize < iSize)
										{
											substForms.add(i, substTuples);
											break;
										}
										if (i == sfLast)
										{
											substForms.add(substTuples);
										}
									}
								}
							}
						}

						if (!substForms.isEmpty())
						{
							// Try to simplify the Formula.
							@NotNull Formula f = f0;
							for (@NotNull Tuple.Triple<List<List<String>>, List<String>, Collection<List<String>>> substTuples : substForms)
							{
								@Nullable List<List<String>> litsToRemove = substTuples.first;
								if (litsToRemove != null)
								{
									for (List<String> lit : litsToRemove)
									{
										f = maybeRemoveMatchingLits(f, lit);
									}
								}
							}

							// Now generate pred var instantiations from the possibly simplified formula.
							@NotNull List<String> templates = new ArrayList<>();
							templates.add(f.form);

							// Iterate over all var plus query lits forms, getting a list of substitution literals.
							@NotNull Set<String> accumulator = new HashSet<>();
							for (@Nullable Tuple.Triple<List<List<String>>, List<String>, Collection<List<String>>> substTuples : substForms)
							{
								if ((substTuples != null))
								{
									// Iterate over all ground lits ...
									// Do not use litsToRemove, which we have already used above.
									// List<List<String>> litsToRemove = substTuples.first;

									// Remove and hold the tuple that indicates the variable substitution pattern.
									List<String> varTuple = substTuples.second;

									for (@NotNull List<String> groundLit : substTuples.third)
									{
										// Iterate over all formula templates, substituting terms from each ground lit for vars in the template.
										for (@NotNull String template : templates)
										{
											@NotNull Formula templateF = Formula.of(template);
											Set<String> quantVars = templateF.collectVariables().first;
											for (int i = 0; i < varTuple.size(); i++)
											{
												String var = varTuple.get(i);
												if (Formula.isVariable(var))
												{
													String term = groundLit.get(i);
													// Don't replace variables that are explicitly quantified.
													if (!quantVars.contains(var))
													{
														@NotNull List<Pattern> patterns = new ArrayList<>();
														@NotNull List<String> patternStrings = Arrays.asList("(\\W*\\()(\\s*holds\\s+\\" + var + ")(\\W+)",
																// "(\\W*\\()(\\s*\\" + var + ")(\\W+)",
																"(\\W*)(\\" + var + ")(\\W+)");
														for (@NotNull String patternString : patternStrings)
														{
															patterns.add(Pattern.compile(patternString));
														}
														for (@NotNull Pattern pattern : patterns)
														{
															@NotNull Matcher m = pattern.matcher(template);
															template = m.replaceAll("$1" + term + "$3");
														}
													}
												}
											}
											if (Arity.hasCorrectArity(template, kb::getValence))
											{
												accumulator.add(template);
											}
											else
											{
												Instantiate.logger.warning("Rejected formula because of incorrect arity: " + template);
												break;
											}
										}
									}
									templates.clear();
									templates.addAll(accumulator);
									accumulator.clear();
								}
							}
							result.addAll(KB.formsToFormulas(templates));
						}
						if (result.isEmpty())
						{
							throw new RejectException();
						}
					}
				}
			}
		}
		catch (RejectException r)
		{
			Instantiate.logger.warning("Rejected formula because " + r.getMessage());
			throw r;
		}
		return result;
	}

	/**
	 * Return true if the input predicate can take relation names as
	 * arguments, else returns false.
	 */
	private static boolean isPossibleRelnArgQueryPred(@NotNull final Formula f0, @NotNull final KB kb, @NotNull final String predicate)
	{
		return !predicate.isEmpty() && (kb.getRelnArgSignature(predicate) != null || predicate.equals("instance"));
	}

	/**
	 * Replace variables in a formula with "gensym" constants.
	 *
	 * @param f          formula
	 * @param uniqueId   unique ID supplier
	 * @param assertions assertions formulae
	 */
	public static void instantiateFormula(@NotNull final Formula f, @NotNull final Supplier<Integer> uniqueId, @NotNull final List<Formula> assertions)
	{
		logger.finer("pre = " + f);

		@NotNull Set<String> vars = f.collectAllVariables();
		Instantiate.logger.fine("vars = " + vars);

		@NotNull Map<String, String> m = new TreeMap<>();
		for (String var : vars)
		{
			m.put(var, "gensym" + uniqueId.get());
		}
		Instantiate.logger.fine("map = " + m);

		Formula f2 = f.substituteVariables(m);
		assertions.add(f2);
	}
}
