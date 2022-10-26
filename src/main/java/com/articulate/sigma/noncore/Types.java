package com.articulate.sigma.noncore;

import com.articulate.sigma.*;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Types
{
	private static final String LOG_SOURCE = "Types";

	private static final Logger logger = Logger.getLogger(Types.class.getName());

	// A D D

	/**
	 * Add clauses for every variable in the antecedent to restrict its
	 * type to the type restrictions defined on every relation in which
	 * it appears.  For example
	 * (=&gt;
	 * (foo ?A B)
	 * (bar B ?A))
	 * (domain foo 1 Z)
	 * would result in
	 * (=&gt;
	 * (instance ?A Z)
	 * (=&gt;
	 * (foo ?A B)
	 * (bar B ?A)))
	 *
	 * @param f  A Formula
	 * @param kb The Knowledge Base
	 * @return A string representing the Formula with type added
	 */
	@NotNull
	public static String addTypeRestrictions(@NotNull final Formula f, @NotNull final KB kb)
	{
		return addTypeRestrictions(f.form, kb);
	}

	/**
	 * Add clauses for every variable in the antecedent to restrict its
	 * type to the type restrictions defined on every relation in which
	 * it appears.  For example
	 * (=&gt;
	 * (foo ?A B)
	 * (bar B ?A))
	 * (domain foo 1 Z)
	 * would result in
	 * (=&gt;
	 * (instance ?A Z)
	 * (=&gt;
	 * (foo ?A B)
	 * (bar B ?A)))
	 *
	 * @param form A formula string
	 * @param kb   The Knowledge Base
	 * @return A string representing the Formula with type added
	 */
	@NotNull
	public static String addTypeRestrictions(@NotNull final String form, @NotNull final KB kb)
	{
		logger.entering(LOG_SOURCE, "addTypeRestrictions", kb.name);
		@NotNull String form2 = Formula.of(form).makeQuantifiersExplicit(false);
		@NotNull String result = insertTypeRestrictionsR(form2, new ArrayList<>(), kb);
		logger.exiting(LOG_SOURCE, "addTypeRestrictions", result);
		return result;
	}

	// I N S E R T

	/**
	 * When invoked on a formula, this method returns a String
	 * representation of the Formula with type constraints added for
	 * all explicitly quantified variables, if possible.  Otherwise, a
	 * String representation of the original Formula is returned.
	 *
	 * @param form  A formula form
	 * @param shelf A List, each element of which is a quaternary List
	 *              containing a SUO-KIF variable String, a token "U" or "E"
	 *              indicating how the variable is quantified, a List of instance
	 *              classes, and a List of subclass classes
	 * @param kb    The KB used to determine predicate and variable arg
	 *              types.
	 * @return A String representation of a Formula, with type
	 * restrictions added.
	 */
	@NotNull
	private static String insertTypeRestrictionsR(@NotNull final String form, @NotNull final List<Tuple.Quad<String, String, List<String>, List<String>>> shelf, @NotNull final KB kb)
	{
		logger.entering(LOG_SOURCE, "insertTypeRestrictionsR", new String[]{"shelf = " + shelf, "kb = " + kb.name});
		@NotNull String result = form;
		if (Lisp.listP(form) && !Lisp.empty(form) && form.matches(".*\\?\\w+.*"))
		{
			@NotNull StringBuilder sb = new StringBuilder();
			int len = Lisp.listLength(form);
			@NotNull String head = Lisp.car(form);
			if (Formula.isQuantifier(head) && len == 3)
			{
				if (Formula.UQUANT.equals(head))
				{
					sb.append(insertTypeRestrictionsU(form, shelf, kb));
				}
				else
				{
					sb.append(insertTypeRestrictionsE(form, shelf, kb));
				}
			}
			else
			{
				sb.append("(");
				for (int i = 0; i < len; i++)
				{
					@NotNull String argI = Lisp.getArgument(form, i);
					if (i > 0)
					{
						sb.append(" ");
						if (Formula.isVariable(argI))
						{
							@Nullable String type = findType(i, head, kb);
							if (type != null && !type.isEmpty() && !type.startsWith("Entity"))
							{
								boolean sc = false;
								while (type.endsWith("+"))
								{
									sc = true;
									type = type.substring(0, type.length() - 1);
								}
								if (sc)
								{
									Shelf.addScForVar(argI, type, shelf);
								}
								else
								{
									Shelf.addIoForVar(argI, type, shelf);
								}
							}
						}
					}
					sb.append(insertTypeRestrictionsR(argI, shelf, kb));
				}
				sb.append(")");
			}
			result = sb.toString();
		}
		logger.exiting(LOG_SOURCE, "insertTypeRestrictionsR", result);
		return result;
	}

	/**
	 * When invoked on a formula that begins with explicit universal
	 * quantification, this method returns a String representation of
	 * the Formula with type constraints added for the top level
	 * quantified variables, if possible.  Otherwise, a String
	 * representation of the original Formula is returned.
	 *
	 * @param form  A formula form
	 * @param shelf A List of quaternary Lists, each of which
	 *              contains type information about a variable
	 * @param kb    The KB used to determine predicate and variable arg
	 *              types.
	 * @return A String representation of a Formula, with type
	 * restrictions added.
	 */
	@NotNull
	private static String insertTypeRestrictionsU(@NotNull final String form, @NotNull final List<Tuple.Quad<String, String, List<String>, List<String>>> shelf, @NotNull final KB kb)
	{
		logger.entering(LOG_SOURCE, "insertTypeRestrictionsU", new String[]{"shelf = " + shelf, "kb = " + kb.name});
		String result;
		@NotNull String varList = Lisp.getArgument(form, 1);

		@NotNull List<Tuple.Quad<String, String, List<String>, List<String>>> newShelf = Shelf.makeNewShelf(shelf);
		int vLen = Lisp.listLength(varList);
		for (int i = 0; i < vLen; i++)
		{
			Shelf.addVarDataQuad(Lisp.getArgument(varList, i), "U", newShelf);
		}

		@NotNull String arg2 = insertTypeRestrictionsR(Lisp.getArgument(form, 2), newShelf, kb);

		@NotNull Set<String> constraints = new LinkedHashSet<>();
		for (@NotNull Tuple.Quad<String, String, List<String>, List<String>> quad : newShelf)
		{
			String var = quad.first;
			String token = quad.second;
			if (token.equals("U"))
			{
				List<String> ios = quad.third;
				List<String> scs = quad.fourth;
				if (!scs.isEmpty())
				{
					winnowTypeList(scs, kb);
					if (!scs.isEmpty())
					{
						if (!ios.contains("SetOrClass"))
						{
							ios.add("SetOrClass");
						}
						for (String sc : scs)
						{
							@NotNull String constraint = "(subclass " + var + " " + sc + ")";
							if (!arg2.contains(constraint))
							{
								constraints.add(constraint);
							}
						}
					}
				}
				if (!ios.isEmpty())
				{
					winnowTypeList(ios, kb);
					for (String io : ios)
					{
						@NotNull String constraint = "(instance " + var + " " + io + ")";
						if (!arg2.contains(constraint))
						{
							constraints.add(constraint);
						}
					}
				}
			}
		}

		@NotNull StringBuilder sb = new StringBuilder();
		sb.append("(forall ");
		sb.append(varList);
		if (constraints.isEmpty())
		{
			sb.append(" ");
			sb.append(arg2);
		}
		else
		{
			sb.append(" (=>");
			int cLen = constraints.size();
			if (cLen > 1)
			{
				sb.append(" (and");
			}
			for (String constraint : constraints)
			{
				sb.append(" ");
				sb.append(constraint);
			}
			if (cLen > 1)
			{
				sb.append(")");
			}
			sb.append(" ");
			sb.append(arg2);
			sb.append(")");
		}
		sb.append(")");
		result = sb.toString();
		logger.exiting(LOG_SOURCE, "insertTypeRestrictionsU", result);
		return result;
	}

	/**
	 * When invoked on a formula that begins with explicit existential
	 * quantification, this method returns a String representation of
	 * the Formula with type constraints added for the top level
	 * quantified variables, if possible.  Otherwise, a String
	 * representation of the original Formula is returned.
	 *
	 * @param form  A formula string
	 * @param shelf A List of quaternary Lists, each of which
	 *              contains type information about a variable
	 * @param kb    The KB used to determine predicate and variable arg
	 *              types.
	 * @return A String representation of a Formula, with type
	 * restrictions added.
	 */
	@NotNull
	private static String insertTypeRestrictionsE(@NotNull final String form, @NotNull final List<Tuple.Quad<String, String, List<String>, List<String>>> shelf, @NotNull final KB kb)
	{
		if (logger.isLoggable(Level.FINER))
		{
			@NotNull String[] params = {"shelf = " + shelf, "kb = " + kb.name};
			logger.entering(LOG_SOURCE, "insertTypeRestrictionsE", params);
		}
		String result;
		@NotNull String varList = Lisp.getArgument(form, 1);

		@NotNull List<Tuple.Quad<String, String, List<String>, List<String>>> newShelf = Shelf.makeNewShelf(shelf);
		int vLen = Lisp.listLength(varList);
		for (int i = 0; i < vLen; i++)
		{
			Shelf.addVarDataQuad(Lisp.getArgument(varList, i), "E", newShelf);
		}

		@NotNull String arg2 = insertTypeRestrictionsR(Lisp.getArgument(form, 2), newShelf, kb);
		@NotNull Set<String> constraints = new LinkedHashSet<>();
		@NotNull StringBuilder sb = new StringBuilder();
		for (@NotNull Tuple.Quad<String, String, List<String>, List<String>> quad : newShelf)
		{
			String var = quad.first;
			String token = quad.second;
			if (token.equals("E"))
			{
				List<String> ios = quad.third;
				List<String> scs = quad.fourth;
				if (!ios.isEmpty())
				{
					winnowTypeList(ios, kb);
					for (String io : ios)
					{
						sb.setLength(0);
						sb.append("(instance ").append(var).append(" ").append(io).append(")");
						@NotNull String constraint = sb.toString();
						if (!arg2.contains(constraint))
						{
							constraints.add(constraint);
						}
					}
				}
				if (!scs.isEmpty())
				{
					winnowTypeList(scs, kb);
					for (String sc : scs)
					{
						sb.setLength(0);
						sb.append("(subclass ").append(var).append(" ").append(sc).append(")");
						@NotNull String constraint = sb.toString();
						if (!arg2.contains(constraint))
						{
							constraints.add(constraint);
						}
					}
				}
			}
		}
		sb.setLength(0);
		sb.append("(exists ");
		sb.append(varList);
		if (constraints.isEmpty())
		{
			sb.append(" ");
			sb.append(arg2);
		}
		else
		{
			sb.append(" (and");
			for (String constraint : constraints)
			{
				sb.append(" ");
				sb.append(constraint);
			}
			if (Lisp.car(arg2).equals("and"))
			{
				int nextFLen = Lisp.listLength(arg2);
				for (int k = 1; k < nextFLen; k++)
				{
					sb.append(" ");
					sb.append(Lisp.getArgument(arg2, k));
				}
			}
			else
			{
				sb.append(" ");
				sb.append(arg2);
			}
			sb.append(")");
		}
		sb.append(")");
		result = sb.toString();
		logger.exiting(LOG_SOURCE, "insertTypeRestrictionsE", result);
		return result;
	}

	// C O M P U T E

	/**
	 * Does much of the real work for addTypeRestrictions() by
	 * recursing through the Formula and collecting type constraint
	 * information for the variable var.
	 *
	 * @param ios A List of classes (class name Strings) of which any
	 *            binding for var must be an instance.
	 * @param scs A List of classes (class name Strings) of which any
	 *            binding for var must be a subclass.
	 * @param var A SUO-KIF variable.
	 * @param kb  The KB used to determine predicate and variable arg
	 *            types.
	 */
	private static void computeTypeRestrictions(@NotNull final Formula f0, @NotNull final List<String> ios, @NotNull final List<String> scs, @NotNull final String var, @NotNull final KB kb)
	{
		if (logger.isLoggable(Level.FINER))
		{
			@NotNull String[] params = {"ios = " + ios, "scs = " + scs, "var = " + var, "kb = " + kb.name};
			logger.entering(LOG_SOURCE, "computeTypeRestrictions", params);
		}
		if (!f0.listP() || !f0.form.contains(var))
		{
			return;
		}
		@NotNull Formula f = Formula.of(f0.form);
		@NotNull String pred = f.car();
		if (Formula.isQuantifier(pred))
		{
			@NotNull String arg2 = f.getArgument(2);
			if (arg2.contains(var))
			{
				@NotNull Formula nextF = Formula.of(arg2);
				computeTypeRestrictions(nextF, ios, scs, var, kb);
			}
		}
		else if (Formula.isLogicalOperator(pred))
		{
			int len = f.listLength();
			for (int i = 1; i < len; i++)
			{
				@NotNull String argI = f.getArgument(i);
				if (argI.contains(var))
				{
					@NotNull Formula nextF = Formula.of(argI);
					computeTypeRestrictions(nextF, ios, scs, var, kb);
				}
			}
		}
		else
		{
			int valence = kb.getValence(pred);
			@NotNull List<String> types = getTypeList(pred, kb, f0.errors);
			int len = f.listLength();
			for (int i = 1; i < len; i++)
			{
				int argIdx = i;
				if (valence == 0) // pred is a VariableArityRelation
				{
					argIdx = 1;
				}
				@NotNull String arg = f.getArgument(i);
				if (arg.contains(var))
				{
					if (Lisp.listP(arg))
					{
						@NotNull Formula nextF = Formula.of(arg);
						computeTypeRestrictions(nextF, ios, scs, var, kb);
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
							type = findType(argIdx, pred, kb);
						}
						if (type != null && !type.isEmpty() && !type.startsWith("Entity"))
						{
							boolean sc = false;
							while (type.endsWith("+"))
							{
								sc = true;
								type = type.substring(0, type.length() - 1);
							}
							if (sc)
							{
								if (!scs.contains(type))
								{
									scs.add(type);
								}
							}
							else if (!ios.contains(type))
							{
								ios.add(type);
							}
						}
					}
				}
			}
			// Special treatment for equal
			if (pred.equals("equal"))
			{
				@NotNull String arg1 = f.getArgument(1);
				@NotNull String arg2 = f.getArgument(2);
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
						@NotNull Formula nextF = Formula.of(term);
						if (nextF.isFunctionalTerm())
						{
							@NotNull String fn = nextF.car();
							@NotNull List<String> classes = getTypeList(fn, kb, f0.errors);
							@Nullable String cl = null;
							if (!classes.isEmpty())
							{
								cl = classes.get(0);
							}
							if (cl == null)
							{
								cl = findType(0, fn, kb);
							}
							if (cl != null && !cl.isEmpty() && !cl.startsWith("Entity"))
							{
								boolean sc = false;
								while (cl.endsWith("+"))
								{
									sc = true;
									cl = cl.substring(0, cl.length() - 1);
								}
								if (sc)
								{
									if (!scs.contains(cl))
									{
										scs.add(cl);
									}
								}
								else if (!ios.contains(cl))
								{
									ios.add(cl);
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
								if (!io.equals("Entity") && !ios.contains(io))
								{
									ios.add(io);
								}
							}
						}
					}
				}
			}
			// Special treatment for instance or subclass, only if var.equals(arg1) and arg2 is a functional term.
			else if (List.of("instance", "subclass").contains(pred))
			{
				@NotNull String arg1 = f.getArgument(1);
				@NotNull String arg2 = f.getArgument(2);
				if (var.equals(arg1) && Lisp.listP(arg2))
				{
					@NotNull Formula nextF = Formula.of(arg2);
					if (nextF.isFunctionalTerm())
					{
						@NotNull String fn = nextF.car();
						@NotNull List<String> classes = getTypeList(fn, kb, f0.errors);
						@Nullable String cl = null;
						if (!classes.isEmpty())
						{
							cl = classes.get(0);
						}
						if (cl == null)
						{
							cl = findType(0, fn, kb);
						}
						if (cl != null && !cl.isEmpty() && !cl.startsWith("Entity"))
						{
							while (cl.endsWith("+"))
							{
								cl = cl.substring(0, cl.length() - 1);
							}
							if (pred.equals("subclass"))
							{
								if (!scs.contains(cl))
								{
									scs.add(cl);
								}
							}
							else if (!ios.contains(cl))
							{
								ios.add(cl);
							}
						}
					}
				}
			}
		}
		logger.exiting(LOG_SOURCE, "computeTypeRestrictions");
	}

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
	private static void computeVariableTypesR(@NotNull final Formula f0, @NotNull final Map<String, List<List<String>>> map, @NotNull final KB kb)
	{
		if (logger.isLoggable(Level.FINER))
		{
			@NotNull String[] params = {"map = " + map, "kb = " + kb.name};
			logger.entering(LOG_SOURCE, "computeVariableTypesR", params);
		}
		if (f0.listP() && !f0.empty())
		{
			int len = f0.listLength();
			@NotNull String arg0 = f0.car();
			if (Formula.isQuantifier(arg0) && (len == 3))
			{
				computeVariableTypesQ(f0, map, kb);
			}
			else
			{
				for (int i = 0; i < len; i++)
				{
					@NotNull Formula nextF = Formula.of(f0.getArgument(i));
					computeVariableTypesR(nextF, map, kb);
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
		if (logger.isLoggable(Level.FINER))
		{
			@NotNull String[] params = {"map = " + map, "kb = " + kb.name};
			logger.entering(LOG_SOURCE, "computeVariableTypesQ", params);
		}
		@NotNull Formula varListF = Formula.of(f0.getArgument(1));
		@NotNull Formula nextF = Formula.of(f0.getArgument(2));

		int vLen = varListF.listLength();
		for (int i = 0; i < vLen; i++)
		{
			@NotNull List<List<String>> types = new ArrayList<>();
			@NotNull List<String> ios = new ArrayList<>();
			@NotNull List<String> scs = new ArrayList<>();
			@NotNull String var = varListF.getArgument(i);
			computeTypeRestrictions(nextF, ios, scs, var, kb);
			if (!scs.isEmpty())
			{
				winnowTypeList(scs, kb);
				if (!scs.isEmpty() && !ios.contains("SetOrClass"))
				{
					ios.add("SetOrClass");
				}
			}
			if (!ios.isEmpty())
			{
				winnowTypeList(ios, kb);
			}
			types.add(ios);
			types.add(scs);
			map.put(var, types);
		}
		computeVariableTypesR(nextF, map, kb);
		logger.exiting(LOG_SOURCE, "computeVariableTypesQ");
	}

	/**
	 * A + is appended to the type if the parameter must be a class
	 *
	 * @return the type for each argument to the given predicate, where
	 * List element 0 is the result, if a function, 1 is the
	 * first argument, 2 is the second etc.
	 */
	@NotNull
	private static List<String> getTypeList(@NotNull final String pred, @NotNull final KB kb, final List<String> errors)
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
	private static String[] addToTypeList(@NotNull final String pred, @NotNull final Collection<Formula> al, @NotNull final String[] result, boolean classP, final List<String> errors)
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
			else if (result[argnum].isEmpty())
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
		logger.entering(LOG_SOURCE, "findType", new String[] {"numarg = " + argIdx, "pred = " + pred, "kb = " + kb.name});

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
