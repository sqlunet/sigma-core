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
				List<String> classes = quad.third;
				List<String> subclasses = quad.fourth;
				if (!subclasses.isEmpty())
				{
					winnowTypeList(subclasses, kb);
					if (!subclasses.isEmpty())
					{
						if (!classes.contains("SetOrClass"))
						{
							classes.add("SetOrClass");
						}
						for (String sc : subclasses)
						{
							@NotNull String constraint = "(subclass " + var + " " + sc + ")";
							if (!arg2.contains(constraint))
							{
								constraints.add(constraint);
							}
						}
					}
				}
				if (!classes.isEmpty())
				{
					winnowTypeList(classes, kb);
					for (String io : classes)
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
				List<String> classes = quad.third;
				List<String> subclasses = quad.fourth;
				if (!classes.isEmpty())
				{
					winnowTypeList(classes, kb);
					for (String io : classes)
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
				if (!subclasses.isEmpty())
				{
					winnowTypeList(subclasses, kb);
					for (String subclass : subclasses)
					{
						sb.setLength(0);
						sb.append("(subclass ").append(var).append(" ").append(subclass).append(")");
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

	// T Y P E   L I S T S

	/**
	 * This method tries to remove all but the most specific relevant
	 * classes from a List of sortal classes.
	 *
	 * @param types A List of classes (class name Strings) that
	 *              constrain the value of a SUO-KIF variable.
	 * @param kb    The KB used to determine if any of the classes in the
	 *              List types are redundant.
	 */
	static void winnowTypeList(@Nullable final List<String> types, @NotNull final KB kb)
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
	static String findType(int argIdx, @NotNull final String pred, @NotNull final KB kb)
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
