package com.articulate.sigma.noncore;

import com.articulate.sigma.*;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Types2
{
	private static final String LOG_SOURCE = "Types";

	private static final Logger logger = Logger.getLogger(Types2.class.getName());

	// C O M P U T E

	/**
	 * Does much of the real work for addTypeRestrictions() by
	 * recursing through the Formula and collecting type constraint
	 * information for the variable var.
	 *
	 * @param classes      A List of classes (class name Strings) of which any
	 *                     binding for var must be an instance: (instance var classes)
	 * @param superclasses A List of classes (class name Strings) of which any
	 *                     binding for var must be a subclass: (subclass var superclasses)
	 * @param var          A SUO-KIF variable.
	 * @param kb           The KB used to determine predicate and variable arg
	 *                     types.
	 */
	public static void computeTypeRestrictions(@NotNull final Formula f0, @NotNull final List<String> classes, @NotNull final List<String> superclasses, @NotNull final String var, @NotNull final KB kb)
	{
		computeTypeRestrictions(f0.form, classes, superclasses, var, kb, f0.errors);
	}

	public static void computeTypeRestrictions(@NotNull final String form, @NotNull final List<String> classes, @NotNull final List<String> superclasses, @NotNull final String var, @NotNull final KB kb, @NotNull final List<String> errors)
	{
		logger.entering(LOG_SOURCE, "computeTypeRestrictions", new String[]{"classes = " + classes, "superclasses = " + superclasses, "var = " + var, "kb = " + kb.name});
		if (!Lisp.listP(form) || !form.contains(var))
		{
			return;
		}
		@NotNull String head = Lisp.car(form);
		if (Formula.isQuantifier(head))
		{
			@NotNull String body = Lisp.getArgument(form, 2);
			if (body.contains(var))
			{
				computeTypeRestrictions(body, classes, superclasses, var, kb, errors);
			}
		}
		else if (Formula.isLogicalOperator(head))
		{
			int len = Lisp.listLength(form);
			for (int i = 1; i < len; i++)
			{
				@NotNull String argI = Lisp.getArgument(form, i);
				if (argI.contains(var))
				{
					computeTypeRestrictions(argI, classes, superclasses, var, kb, errors);
				}
			}
		}
		else
		{
			int valence = kb.getValence(head);
			@NotNull List<String> types = getTypeList(head, kb, errors);
			int len = Lisp.listLength(form);
			for (int i = 1; i < len; i++)
			{
				int argIdx = i;
				if (valence == 0) // pred is a VariableArityRelation
				{
					argIdx = 1;
				}
				@NotNull String arg = Lisp.getArgument(form, i);
				if (arg.contains(var))
				{
					if (Lisp.listP(arg))
					{
						computeTypeRestrictions(arg, classes, superclasses, var, kb, errors);
					}
					else if (var.equals(arg))
					{
						@Nullable String type = null;
						if (argIdx < types.size())
						{
							type = types.get(argIdx);
						}
						if (type == null)
						{
							type = findType(argIdx, head, kb);
						}
						if (type != null && !type.isEmpty() && !type.startsWith("Entity"))
						{
							boolean isSubclass = false;
							while (type.endsWith("+"))
							{
								isSubclass = true;
								type = type.substring(0, type.length() - 1);
							}
							if (isSubclass)
							{
								if (!superclasses.contains(type))
								{
									superclasses.add(type);
								}
							}
							else if (!classes.contains(type))
							{
								classes.add(type);
							}
						}
					}
				}
			}
			// Special treatment for equal
			if (head.equals("equal"))
			{
				@NotNull String arg1 = Lisp.getArgument(form, 1);
				@NotNull String arg2 = Lisp.getArgument(form, 2);
				@Nullable String term = null;
				if (var.equals(arg1))
				{
					term = arg2;
				}
				else if (var.equals(arg2))
				{
					term = arg1;
				}
				if (term != null && !term.isEmpty())
				{
					if (Lisp.listP(term))
					{
						if (Formula.isFunctionalTerm(term))
						{
							@NotNull String fn = Lisp.car(term);
							@NotNull List<String> classes2 = getTypeList(fn, kb, errors);
							@Nullable String className2 = null;
							if (!classes2.isEmpty())
							{
								className2 = classes2.get(0);
							}
							if (className2 == null)
							{
								className2 = findType(0, fn, kb);
							}
							if (className2 != null && !className2.isEmpty() && !className2.startsWith("Entity"))
							{
								boolean isSubclass = false;
								while (className2.endsWith("+"))
								{
									isSubclass = true;
									className2 = className2.substring(0, className2.length() - 1);
								}
								if (isSubclass)
								{
									if (!superclasses.contains(className2))
									{
										superclasses.add(className2);
									}
								}
								else if (!classes.contains(className2))
								{
									classes.add(className2);
								}
							}
						}
					}
					else
					{
						@NotNull Set<String> instanceOfs = kb.getCachedRelationValues("instance", term, 1, 2);
						if (!instanceOfs.isEmpty())
						{
							for (@NotNull String io : instanceOfs)
							{
								if (!io.equals("Entity") && !classes.contains(io))
								{
									classes.add(io);
								}
							}
						}
					}
				}
			}

			// Special treatment for instance or subclass, only if var.equals(arg1) and arg2 is a functional term.
			else if (List.of("instance", "subclass").contains(head))
			{
				@NotNull String arg1 = Lisp.getArgument(form, 1);
				@NotNull String arg2 = Lisp.getArgument(form, 2);
				if (var.equals(arg1) && Lisp.listP(arg2))
				{
					if (Formula.isFunctionalTerm(arg2))
					{
						@NotNull String fn = Lisp.car(arg2);
						@NotNull List<String> classes2 = getTypeList(fn, kb, errors);
						@Nullable String className2 = null;
						if (!classes2.isEmpty())
						{
							className2 = classes2.get(0);
						}
						if (className2 == null)
						{
							className2 = findType(0, fn, kb);
						}
						if (className2 != null && !className2.isEmpty() && !className2.startsWith("Entity"))
						{
							while (className2.endsWith("+"))
							{
								className2 = className2.substring(0, className2.length() - 1);
							}
							if (head.equals("subclass"))
							{
								if (!superclasses.contains(className2))
								{
									superclasses.add(className2);
								}
							}
							else if (!classes2.contains(className2))
							{
								classes.add(className2);
							}
						}
					}
				}
			}
		}
		logger.exiting(LOG_SOURCE, "computeTypeRestrictions");
	}

	// C O M P U T E   V A R   T Y P E S

	/**
	 * A recursive utility method used to collect type information for
	 * the variables in this Formula.
	 *
	 * @param f0  A Formula
	 * @param map A Map used to store type information for the
	 *            variables in this Formula.
	 * @param kb  The KB used to compute the sortal constraints for
	 *            each variable.
	 */
	public static void computeVariableTypes(@NotNull final Formula f0, @NotNull final Map<String, List<List<String>>> map, @NotNull final KB kb)
	{
		computeVariableTypes(f0.form, map, kb, f0.errors);
	}

	/**
	 * A recursive utility method used to collect type information for
	 * the variables in this Formula.
	 *
	 * @param form   A formula string.
	 * @param map    A Map used to store type information for the
	 *               variables in this Formula.
	 * @param kb     The KB used to compute the sortal constraints for
	 *               each variable.
	 * @param errors error log
	 */
	public static void computeVariableTypes(@NotNull final String form, @NotNull final Map<String, List<List<String>>> map, @NotNull final KB kb, @NotNull final List<String> errors)
	{
		logger.entering(LOG_SOURCE, "computeVariableTypesR", new String[]{"map = " + map, "kb = " + kb.name});
		if (Lisp.listP(form) && !Lisp.empty(form))
		{
			int len = Lisp.listLength(form);
			@NotNull String arg0 = Lisp.car(form);
			if (Formula.isQuantifier(arg0) && len == 3)
			{
				computeVariableTypesQ(form, map, kb, errors);
			}
			else
			{
				for (int i = 0; i < len; i++)
				{
					@NotNull String argI = Lisp.getArgument(form, i);
					computeVariableTypes(argI, map, kb, errors);
				}
			}
		}
		logger.exiting(LOG_SOURCE, "computeVariableTypesR");
	}

	/**
	 * A recursive utility method used to collect type information for
	 * the variables in this Formula, which is assumed to have forall
	 * or exists as its arg0.
	 *
	 * @param f0  A Formula
	 * @param map A Map used to store type information for the
	 *            variables in this Formula.
	 * @param kb  The KB used to compute the sortal constraints for
	 *            each variable.
	 */
	private static void computeVariableTypesQ(@NotNull final Formula f0, @NotNull final Map<String, List<List<String>>> map, @NotNull final KB kb)
	{
		computeVariableTypesQ(f0.form, map, kb, f0.errors);
	}

	/**
	 * A recursive utility method used to collect type information for
	 * the variables in this Formula, which is assumed to have forall
	 * or exists as its arg0.
	 *
	 * @param form   A formula string
	 * @param map    A Map used to store type information for the
	 *               variables in this Formula.
	 * @param kb     The KB used to compute the sortal constraints for
	 *               each variable.
	 * @param errors error log
	 */
	private static void computeVariableTypesQ(@NotNull final String form, @NotNull final Map<String, List<List<String>>> map, @NotNull final KB kb, @NotNull final List<String> errors)
	{
		logger.entering(LOG_SOURCE, "computeVariableTypesQ", new String[]{"map = " + map, "kb = " + kb.name});
		@NotNull String body = Lisp.getArgument(form, 2);
		@NotNull String vars = Lisp.getArgument(form, 1);
		int nvars = Lisp.listLength(vars);
		for (int i = 0; i < nvars; i++)
		{
			@NotNull String var = Lisp.getArgument(vars, i);
			@NotNull List<List<String>> types = new ArrayList<>();
			@NotNull List<String> classes = new ArrayList<>();
			@NotNull List<String> subclasses = new ArrayList<>();
			computeTypeRestrictions(body, classes, subclasses, var, kb, errors);

			if (!subclasses.isEmpty())
			{
				winnowTypeList(subclasses, kb);
				if (!subclasses.isEmpty() && !classes.contains("SetOrClass"))
				{
					classes.add("SetOrClass");
				}
			}
			if (!classes.isEmpty())
			{
				winnowTypeList(classes, kb);
			}
			types.add(classes);
			types.add(subclasses);

			map.put(var, types);
		}
		computeVariableTypes(body, map, kb, errors);
		logger.exiting(LOG_SOURCE, "computeVariableTypesQ");
	}

	// T Y P E   L I S T S

	/**
	 * A + is appended to the type if the parameter must be a class
	 *
	 * @return the type for each argument to the given predicate, where
	 * List element 0 is the result, if a function, 1 is the
	 * first argument, 2 is the second etc.
	 */
	@NotNull
	private static List<String> getTypeList(@NotNull final String pred, @NotNull final KB kb, @NotNull final List<String> errors)
	{
		List<String> result;

		// build the sortalTypeCache key.
		@NotNull String key = "gtl" + pred + kb.name;
		@NotNull Map<String, List<String>> stc = kb.getSortalTypeCache();
		result = stc.get(key);
		if (result == null)
		{
			int valence = kb.getValence(pred);
			int len = Arity.MAX_PREDICATE_ARITY + 1;
			if (valence == 0)
			{
				len = 2;
			}
			else if (valence > 0)
			{
				len = valence + 1;
			}

			@NotNull Collection<Formula> al = kb.askWithRestriction(0, "domain", 1, pred);
			@NotNull Collection<Formula> al2 = kb.askWithRestriction(0, "domainSubclass", 1, pred);
			@NotNull Collection<Formula> al3 = kb.askWithRestriction(0, "range", 1, pred);
			@NotNull Collection<Formula> al4 = kb.askWithRestriction(0, "rangeSubclass", 1, pred);

			@NotNull String[] r = new String[len];
			addToTypeList(pred, al, r, false, errors);
			addToTypeList(pred, al2, r, true, errors);
			addToTypeList(pred, al3, r, false, errors);
			addToTypeList(pred, al4, r, true, errors);
			result = new ArrayList<>(Arrays.asList(r));

			stc.put(key, result);
		}
		return result;
	}

	/**
	 * A utility helper method for computing predicate data types.
	 */
	@NotNull
	@SuppressWarnings("UnusedReturnValue")
	private static String[] addToTypeList(@NotNull final String pred, @NotNull final Collection<Formula> al, @NotNull final String[] result, boolean classP, @NotNull final List<String> errors)
	{
		if (logger.isLoggable(Level.FINER))
		{
			@NotNull String[] params = {"pred = " + pred, "al = " + al, "result = " + Arrays.toString(result), "classP = " + classP};
			logger.entering(LOG_SOURCE, "addToTypeList", params);
		}
		// If the relations in al start with "range", argnum will be 0, and the arg position of the desired classnames will be 2.
		int argnum = 0;
		int clPos = 2;
		for (@NotNull Formula f : al)
		{
			// logger.finest("text: " + f.form);
			if (f.form.startsWith("(domain"))
			{
				argnum = Integer.parseInt(f.getArgument(2));
				clPos = 3;
			}
			@NotNull String cl = f.getArgument(clPos);
			if ((argnum < 0) || (argnum >= result.length))
			{
				@NotNull String errStr = "Possible arity confusion for " + pred;
				errors.add(errStr);
				logger.warning(errStr);
			}
			else if (result[argnum] == null || result[argnum].isEmpty())
			{
				if (classP)
				{
					cl += "+";
				}
				result[argnum] = cl;
			}
			else
			{
				if (!cl.equals(result[argnum]))
				{
					@NotNull String errStr = "Multiple types asserted for argument " + argnum + " of " + pred + ": " + cl + ", " + result[argnum];
					errors.add(errStr);
					logger.warning(errStr);
				}
			}
		}
		return result;
	}

	/**
	 * This method tries to remove all but the most specific relevant
	 * classes from a List of sortal classes.
	 *
	 * @param types A List of classes (class name Strings) that
	 *              constrain the value of a SUO-KIF variable.
	 * @param kb    The KB used to determine if any of the classes in the
	 *              List types are redundant.
	 */
	private static void winnowTypeList(@Nullable final List<String> types, @NotNull final KB kb)
	{
		if (logger.isLoggable(Level.FINER))
		{
			@NotNull String[] params = {"types = " + types, "kb = " + kb.name};
			logger.entering(LOG_SOURCE, "winnowTypeList", params);
		}
		if ((types != null) && (types.size() > 1))
		{
			@NotNull String[] valArr = types.toArray(new String[0]);
			for (int i = 0; i < valArr.length; i++)
			{
				boolean stop = false;
				for (int j = 0; j < valArr.length; j++)
				{
					if (i != j)
					{
						String clX = valArr[i];
						String clY = valArr[j];
						if (kb.isSubclass(clX, clY))
						{
							types.remove(clY);
							if (types.size() < 2)
							{
								stop = true;
								break;
							}
						}
					}
				}
				if (stop)
				{
					break;
				}
			}
		}
		logger.exiting(LOG_SOURCE, "winnowTypeList");
	}

	// F I N D

	/**
	 * Find the argument type restriction for a given predicate and
	 * argument number that is inherited from one of its
	 * super-relations.  A "+" is appended to the type if the
	 * parameter must be a class.  Argument number 0 is used for the
	 * return the type of a Function.
	 *
	 * @param argIdx argument index
	 * @param pred   predicate
	 * @param kb     knowledge base
	 * @return type restriction
	 */
	@Nullable
	public static String findType(int argIdx, @NotNull final String pred, @NotNull final KB kb)
	{
		logger.entering(LOG_SOURCE, "findType", new String[]{"numarg = " + argIdx, "pred = " + pred, "kb = " + kb.name});

		// build the sortalTypeCache key.
		@NotNull String key = "ft" + argIdx + pred + kb.name;

		// get type from sortal cache
		@NotNull Map<String, List<String>> typeCache = kb.getSortalTypeCache();
		List<String> results = typeCache.get(key);
		boolean isCached = results != null && !results.isEmpty();

		boolean cacheResult = !isCached;
		@Nullable String result = isCached ? results.get(0) : null;

		// compute value
		if (result == null)
		{
			@NotNull List<String> relns = new ArrayList<>();
			boolean found = false;
			@NotNull Set<String> accumulator = new HashSet<>();
			accumulator.add(pred);

			while (!found && !accumulator.isEmpty())
			{
				// accumulator -> relns
				relns.clear();
				relns.addAll(accumulator);
				accumulator.clear();

				for (@NotNull String reln : relns)
				{
					if (found)
					{
						break;
					}
					if (argIdx > 0)
					{//
						// (domain daughter 1 Organism)
						// (domain daughter 2 Organism)
						// (domain son 1 Organism)
						// (domain son 2 Organism)
						// (domain sibling 1 Organism)
						// (domain sibling 2 Organism)
						// (domain brother 1 Man)
						// (domain brother 2 Human)
						// (domain sister 1 Woman)
						// (domain sister 2 Human)
						// (domain acquaintance 1 Human)
						// (domain acquaintance 2 Human)
						// (domain mutualAcquaintance 1 Human)
						// (domain mutualAcquaintance 2 Human)
						// (domain spouse 1 Human)
						// (domain spouse 2 Human)
						// (domain husband 1 Man)
						// (domain husband 2 Woman)
						// (domain wife 1 Woman)
						// (domain wife 2 Man)

						@NotNull Collection<Formula> formulas = kb.askWithRestriction(0, "domain", 1, reln);
						for (@NotNull Formula f : formulas)
						{
							int argPos = Integer.parseInt(f.getArgument(2));
							if (argPos == argIdx)
							{
								result = f.getArgument(3);
								found = true;
								break;
							}
						}
						if (!found)
						{
							// (domainSubclass material 1 Substance)
							// (domainSubclass ingredient 1 Substance)
							// (domainSubclass ingredient 2 Substance)
							// (domainSubclass capability 1 Process)
							// (domainSubclass precondition 1 Process)
							// (domainSubclass precondition 2 Process)
							// (domainSubclass version 1 Artifact)
							// (domainSubclass version 2 Artifact)

							formulas = kb.askWithRestriction(0, "domainSubclass", 1, reln);
							for (@NotNull Formula f : formulas)
							{
								int argPos = Integer.parseInt(f.getArgument(2));
								if (argPos == argIdx)
								{
									result = f.getArgument(3) + "+";
									found = true;
									break;
								}
							}
						}
					}
					else if (argIdx == 0)
					{
						@NotNull Collection<Formula> formulas = kb.askWithRestriction(0, "range", 1, reln);
						if (!formulas.isEmpty())
						{
							Formula f = formulas.iterator().next();
							result = f.getArgument(2);
							found = true;
						}
						if (!found)
						{
							formulas = kb.askWithRestriction(0, "rangeSubclass", 1, reln);
							if (!formulas.isEmpty())
							{
								Formula f = formulas.iterator().next();
								result = f.getArgument(2) + "+";
								found = true;
							}
						}
					}
				}
				if (!found)
				{
					for (@NotNull String r : relns)
					{
						accumulator.addAll(kb.getTermsViaAskWithRestriction(1, r, 0, "subrelation", 2));
					}
				}
			}
			if (cacheResult && (result != null))
			{
				typeCache.put(key, Collections.singletonList(result));
			}
		}
		logger.exiting(LOG_SOURCE, "findType", result);
		return result;
	}
}
