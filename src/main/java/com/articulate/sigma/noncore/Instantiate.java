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

	private static final Logger LOGGER = Logger.getLogger(Instantiate.class.getName());

	/**
	 * Replace variables in a formula with "gensym" constants.
	 *
	 * @param f          formula
	 * @param uniqueId   unique ID supplier
	 * @param assertions assertions formulas to collect result
	 * @return instantiated formula
	 */
	public static Formula instantiateFormula(@NotNull final Formula f, @NotNull final Supplier<Integer> uniqueId, @NotNull final List<Formula> assertions)
	{
		Formula f2 = instantiateFormula(f, uniqueId);
		assertions.add(f2);
		return f2;
	}

	/**
	 * Replace variables in a formula with "gensym" constants.
	 *
	 * @param f        formula
	 * @param uniqueId unique ID supplier
	 * @return instantiated formula
	 */
	public static Formula instantiateFormula(@NotNull final Formula f, @NotNull final Supplier<Integer> uniqueId)
	{
		@NotNull Set<String> vars = f.collectAllVariables();

		@NotNull Map<String, String> varMap = new TreeMap<>();
		for (String var : vars)
		{
			varMap.put(var, "gensym" + uniqueId.get());
		}

		return f.substituteVariables(varMap);
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
		return instantiatePredVars(f0.form, kb);
	}

	/**
	 * Returns a List of the Formulas that result from replacing
	 * all arg0 predicate variables in the input Formula with
	 * predicate names.
	 *
	 * @param form A Formula.
	 * @param kb   A KB that is used for processing the Formula.
	 * @return A List of Formulas, or an empty List if no instantiations can be generated.
	 * @throws RejectException reject exception
	 */
	@NotNull
	public static List<Formula> instantiatePredVars(@NotNull final String form, @NotNull final KB kb) throws RejectException
	{
		@NotNull List<Formula> result = new ArrayList<>();
		try
		{
			if (Lisp.listP(form))
			{
				@NotNull String arg0 = Lisp.getArgument(form, 0);

				// First we do some checks to see if it is worth processing the formula.
				// ... ( ?PREDVAR ...
				if (Formula.isLogicalOperator(arg0) && form.matches(".*\\(\\s*\\?\\w+.*"))
				{
					// Get all pred vars, and then compute query lits for the pred vars, indexed by var.
					@NotNull Map<String, List<String>> varsWithTypes = gatherPredVars(form, kb);
					if (!varsWithTypes.containsKey("arg0"))
					{
						// The formula has no predicate variables in arg0 position, so just return it.
						result.add(Formula.of(form));
					}
					else
					{
						@NotNull List<Tuple.Pair<String, List<List<String>>>> indexedQueryLits = prepareIndexedQueryLiterals(form, kb, varsWithTypes);
						@NotNull List<Tuple.Triple<List<List<String>>, List<String>, Collection<List<String>>>> substForms = new ArrayList<>();

						// First, gather all substitutions.
						for (Tuple.Pair<String, List<List<String>>> varQueryTuples : indexedQueryLits)
						{
							Tuple.Triple<List<List<String>>, List<String>, Collection<List<String>>> substTuples = computeSubstitutionTuples(kb, varQueryTuples);
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
									int sfLast = sfSize - 1;
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
							@NotNull String form2 = form;
							for (@NotNull Tuple.Triple<List<List<String>>, List<String>, Collection<List<String>>> substTuples : substForms)
							{
								@Nullable List<List<String>> litsToRemove = substTuples.first;
								if (litsToRemove != null)
								{
									for (List<String> lit : litsToRemove)
									{
										form2 = tryRemoveMatchingLits(form2, lit);
									}
								}
							}

							// Now generate pred var instantiations from the possibly simplified formula.
							@NotNull List<String> templates = new ArrayList<>();
							templates.add(form2);

							// Iterate over all var plus query lits forms, getting a list of substitution literals.
							@NotNull Set<String> accumulator = new HashSet<>();
							for (@Nullable Tuple.Triple<List<List<String>>, List<String>, Collection<List<String>>> substTuples : substForms)
							{
								if (substTuples != null)
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
												LOGGER.warning("Rejected formula because of incorrect arity: " + template);
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
			LOGGER.warning("Rejected formula because " + r.getMessage());
			throw r;
		}
		return result;
	}

	/**
	 * This method returns a triple of query answer literals.
	 * [1] The first element is a List of query literals that might be
	 * used to simplify the Formula to be instantiated.
	 * [2] The second element is the query literal (List) that will be used as a
	 * template for doing the variable substitutions.
	 * [3] All subsequent elements are ground literals (Lists).
	 *
	 * @param kb        A KB to query for answers.
	 * @param queryLits A List of query literals.  The first item in
	 *                  the list will be a SUO-KIF variable (String), which indexes the
	 *                  list.  Each subsequent item is a query literal (List).
	 * @return A triple of literals, or null if no query answers can be found.
	 */
	private static Tuple.Triple<List<List<String>>, List<String>, Collection<List<String>>> computeSubstitutionTuples(@Nullable final KB kb, @Nullable final Tuple.Pair<String, List<List<String>>> queryLits)
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
			@NotNull List<List<String>> iLits = new ArrayList<>();
			@NotNull List<List<String>> qLits = new ArrayList<>(sortedQLits);
			sortedQLits.clear();

			for (@NotNull List<String> ql : qLits)
			{
				if (ql.get(0).equals("instance"))
				{
					iLits.add(ql);
				}
				else
				{
					sortedQLits.add(ql);
				}
			}
			sortedQLits.addAll(iLits);

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
					{
						// if (accumulator.size() < answers.size()) {
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
	 * @param f0         A Formula.
	 * @param kb         The KB to use for computing variable type signatures.
	 * @param varTypeMap A Map from variables to their types, as
	 *                   explained in the javadoc entry for gatherPredVars(kb)
	 * @return A List, or null if the input formula contains no
	 * predicate variables.
	 */
	@NotNull
	private static List<Tuple.Pair<String, List<List<String>>>> prepareIndexedQueryLiterals(@NotNull final Formula f0, @NotNull final KB kb, @Nullable final Map<String, List<String>> varTypeMap)
	{
		return prepareIndexedQueryLiterals(f0.form, kb, varTypeMap);
	}

	/**
	 * This method returns a List in which each element is
	 * a pair.  The first item of each pair is a variable.
	 * The second item in each pair is a list of query literals
	 * (Lists).
	 *
	 * @param form       A formula string.
	 * @param kb         The KB to use for computing variable type signatures.
	 * @param varTypeMap A Map from variables to their types, as
	 *                   explained in the javadoc entry for gatherPredVars(kb)
	 * @return A List, or null if the input formula contains no
	 * predicate variables.
	 */
	@NotNull
	private static List<Tuple.Pair<String, List<List<String>>>> prepareIndexedQueryLiterals(@NotNull final String form, @NotNull final KB kb, @Nullable final Map<String, List<String>> varTypeMap)
	{
		if (LOGGER.isLoggable(Level.FINER))
		{
			@NotNull String[] params = {"kb = " + kb.name, "varTypeMap = " + varTypeMap};
			LOGGER.entering(LOG_SOURCE, "prepareIndexedQueryLiterals", params);
		}
		@NotNull List<Tuple.Pair<String, List<List<String>>>> result = new ArrayList<>();
		@NotNull Map<String, List<String>> varsWithTypes = varTypeMap != null ? varTypeMap : gatherPredVars(form, kb);

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
						@Nullable Tuple.Pair<String, List<List<String>>> indexedQueryLits = gatherPredVarQueryLits(Formula.of(form), kb, varWithTypes);
						if (indexedQueryLits != null)
						{
							result.add(indexedQueryLits);
						}
					}
				}
			}
			// Else if the formula doesn't contain any arg0 pred vars, do nothing.
		}
		LOGGER.exiting(LOG_SOURCE, "prepareIndexedQueryLiterals", result);
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
	public static Map<String, List<String>> gatherPredVars(@NotNull final Formula f0, @NotNull final KB kb)
	{
		return gatherPredVars(f0.form, kb);
	}

	/**
	 * This method collects and returns all predicate variables that
	 * occur in the Formula.
	 *
	 * @param form a formula string
	 * @param kb   The KB to be used for computations involving
	 *             assertions.
	 * @return a Map in which the keys are predicate variables,
	 * and the values are Lists containing one or more class
	 * names that indicate the type constraints that apply to the
	 * variable.  If no predicate variables can be gathered from the
	 * Formula, the Map will be empty.  The first element in each
	 * List is the variable itself.  Subsequent elements are the
	 * types of the variable.  If no types for the variable can be
	 * determined, the List will contain just the variable.
	 */
	@NotNull
	public static Map<String, List<String>> gatherPredVars(@NotNull final String form, @NotNull final KB kb)
	{
		LOGGER.entering(LOG_SOURCE, "gatherPredVars", kb.name);
		@NotNull Map<String, List<String>> result = new HashMap<>();
		if (!form.isEmpty())
		{
			@NotNull List<String> working = new ArrayList<>();
			@NotNull List<String> accumulator = new ArrayList<>();
			if (Lisp.listP(form) && !Lisp.empty(form))
			{
				accumulator.add(form);
			}
			while (!accumulator.isEmpty())
			{
				working.clear();
				working.addAll(accumulator);
				accumulator.clear();

				for (@NotNull String form2 : working)
				{
					int len = Lisp.listLength(form2);
					@NotNull String arg0 = Lisp.getArgument(form2, 0);
					if (Formula.isQuantifier(arg0) || "holdsDuring".equals(arg0) || "KappaFn".equals(arg0))
					{
						if (len > 2)
						{
							@NotNull String arg2 = Lisp.getArgument(form2, 2);
							if (Lisp.listP(form2) && !Lisp.empty(form2))
							{
								accumulator.add(arg2);
							}
						}
						else
						{
							LOGGER.warning("Malformed?: " + form2);
						}
					}
					else if ("holds".equals(arg0))
					{
						accumulator.add(Lisp.cdr(form2));
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
							@NotNull String argJ = Lisp.getArgument(form2, j);
							if (signature != null && signature.length > j && signature[j] && Formula.isVariable(argJ))
							{
								List<String> vals = result.get(argJ);
								if (vals == null)
								{
									vals = new ArrayList<>();
									result.put(argJ, vals);
									vals.add(argJ);
								}
								@Nullable String argType = kb.getArgType(arg0, j);
								if (argType != null && !vals.contains(argType))
								{
									vals.add(argType);
								}
							}
							else
							{
								if (Lisp.listP(argJ) && !Lisp.empty(argJ))
								{
									accumulator.add(argJ);
								}
							}
						}
					}
				}
			}
		}
		LOGGER.exiting(LOG_SOURCE, "gatherPredVars", result);
		return result;
	}

	/**
	 * This method collects and returns literals likely to be of use
	 * as templates for retrieving predicates to be substituted for
	 * var.
	 *
	 * @param varsWithTypes A List containing a variable followed,
	 *                      optionally, by class names indicating the type of the variable.
	 * @return A pair of literals (Lists) with var as first.
	 * The element of the pair is the variable (String).
	 * The second element is a List corresponding to SUO-KIF
	 * formulas, which will be used as query templates.
	 */
	@Nullable
	private static Tuple.Pair<String, List<List<String>>> gatherPredVarQueryLits(@NotNull final Formula f0, @NotNull final KB kb, @NotNull final List<String> varsWithTypes)
	{
		@NotNull Tuple.Pair<String, List<List<String>>> result = new Tuple.Pair<>();
		result.second = new ArrayList<>();

		String var = varsWithTypes.get(0);
		@NotNull Set<String> added = new HashSet<>();
		@Nullable Map<String, String> varMap = f0.getVarMap();

		// Get the clauses for this Formula.
		@Nullable List<Clause> clauses = f0.getClauses();
		if (clauses != null)
		{
			for (@NotNull Clause clause : clauses)
			{
				if (!clause.negativeLits.isEmpty())
				{
					// Try the negLits first.
					@NotNull List<Formula> lits = clause.negativeLits;

					// Then try the posLits only if there still are no results.
					// lits = new ArrayList<>(clause.negativeLits);
					// lits.addAll(clause.positiveLits);

					// Try the negLits first.  Then try the posLits only if there still are no results.
					for (@NotNull Formula f : lits)
					{
						if (f.form.matches(".*SkFn\\s+\\d+.*") || f.form.matches(".*Sk\\d+.*"))
						{
							continue;
						}
						@NotNull String arg0 = f.getArgument(0);
						if (!arg0.isEmpty())
						{
							// If arg0 corresponds to var, then var has to be of type Predicate, not of types Function or List.
							if (Formula.isVariable(arg0))
							{
								@Nullable String origVar = Variables.getOriginalVar(arg0, varMap);
								if (origVar != null && origVar.equals(var) && !varsWithTypes.contains("Predicate"))
								{
									varsWithTypes.add("Predicate");
								}
							}
							else
							{
								@NotNull List<String> queryLit = new ArrayList<>();
								queryLit.add(arg0);
								boolean foundVar = false;
								int len = f.listLength();
								for (int i = 1; i < len; i++)
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
								if (queryLit.size() != len)
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
									if (!("instance".equals(arg0) && "Relation".equals(term)))
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

		// If we have previously collected type info for the variable, convert that info query lits now.
		int nvarsWithTypes = varsWithTypes.size();
		if (nvarsWithTypes > 1)
		{
			for (int j = 1; j < nvarsWithTypes; j++)
			{
				String argType = varsWithTypes.get(j);
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
		if (result.second.isEmpty())
		{
			return null;
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
	 * This method tries to remove literals from the Formula that
	 * match litArr.  It is intended for use in simplification of this
	 * Formula during predicate variable instantiation, and so only
	 * attempts removals that are likely to be safe in that context.
	 *
	 * @param form A formula string
	 * @param lits A List object representing a SUO-KIF atomic
	 *             formula.
	 * @return A new formula with at least some occurrences of litF
	 * removed, or the original formula if no removals are possible.
	 */
	@NotNull
	private static String tryRemoveMatchingLits(@NotNull final String form, @Nullable final List<String> lits)
	{
		@NotNull String lit = StringUtil.makeForm(lits);
		if (!lit.isEmpty())
		{
			return tryRemoveMatchingLits(form, lit);
		}
		return form;
	}

	/**
	 * This method tries to remove literals from the formula that
	 * match lit.  It is intended for use in simplification of this
	 * formula during predicate variable instantiation, and so only
	 * attempts removals that are likely to be safe in that context.
	 *
	 * @param form A formula string
	 * @param lit  A SUO-KIF literal.
	 * @return A new formula string with at least some occurrences of lit
	 * removed, or the original formula if no removals are possible.
	 */
	@NotNull
	private static String tryRemoveMatchingLits(@NotNull final String form, @NotNull final String lit)
	{
		LOGGER.entering(LOG_SOURCE, "maybeRemoveMatchingLits", lit);
		@NotNull String result = form;
		if (Lisp.listP(form) && !Lisp.empty(form))
		{
			@NotNull StringBuilder sb = new StringBuilder();
			@NotNull String arg0 = Lisp.car(form);
			if (List.of(Formula.IF, Formula.IFF).contains(arg0))
			{
				@NotNull String arg1 = Lisp.getArgument(form, 1);
				@NotNull String arg2 = Lisp.getArgument(form, 2);
				if (arg1.equals(lit))
				{
					sb.append(tryRemoveMatchingLits(arg2, lit));
				}
				else if (arg2.equals(lit))
				{
					sb.append(tryRemoveMatchingLits(arg1, lit));
				}
				else
				{
					sb.append(Formula.LP) //
							.append(arg0) //
							.append(Formula.SPACE) //
							.append(tryRemoveMatchingLits(arg1, lit)) //
							.append(Formula.SPACE) //
							.append(tryRemoveMatchingLits(arg2, lit)) //
							.append(Formula.RP);
				}
			}
			else if (Formula.isQuantifier(arg0) || "holdsDuring".equals(arg0) || "KappaFn".equals(arg0))
			{
				@NotNull String arg1 = Lisp.cadr(form);
				@NotNull String arg2 = Lisp.caddr(form);
				sb.append(Formula.LP) //
						.append(arg0) //
						.append(Formula.SPACE) //
						.append(arg1) //
						.append(Formula.SPACE) //
						.append(tryRemoveMatchingLits(arg2, lit)) //
						.append(Formula.RP);
			}
			else if (Formula.isCommutative(arg0))
			{
				@NotNull List<String> elements = Lisp.elements(form);
				elements.remove(lit);

				@NotNull StringBuilder args = new StringBuilder();
				int len = elements.size();
				for (int i = 1; i < len; i++)
				{
					@NotNull String argI = elements.get(i);
					args.append(" ").append(tryRemoveMatchingLits(argI, lit));
				}
				if (len > 2)
				{
					args = new StringBuilder(Formula.LP + arg0 + args + Formula.RP);
				}
				else
				{
					args = new StringBuilder(args.toString().trim());
				}
				sb.append(args);
			}
			else
			{
				sb.append(form);
			}
			result = sb.toString();
		}
		LOGGER.exiting(LOG_SOURCE, "maybeRemoveMatchingLits", result);
		return result;
	}
}
