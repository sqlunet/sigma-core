package com.articulate.sigma.noncore;

import com.articulate.sigma.*;

import java.util.*;
import java.util.logging.Logger;

/**
 * Types
 */
public class Types
{
	private static final String LOG_SOURCE = "Types";

	private static final Logger LOGGER = Logger.getLogger(Types.class.getName());

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
		LOGGER.entering(LOG_SOURCE, "addTypeRestrictions", kb.name);
		@NotNull String form2 = Formula.of(form).makeQuantifiersExplicit(false);
		@NotNull String result = insertTypeRestrictions(form2, new Shelf(), kb);
		LOGGER.exiting(LOG_SOURCE, "addTypeRestrictions", result);
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
	private static String insertTypeRestrictions(@NotNull final String form, @NotNull final Shelf shelf, @NotNull final KB kb)
	{
		LOGGER.entering(LOG_SOURCE, "insertTypeRestrictions", new String[]{"shelf = " + shelf, "kb = " + kb.name});
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
							@Nullable String type = findType(head, i, kb);
							if (type != null && !type.isEmpty() && !type.startsWith("Entity"))
							{
								boolean isSuperclass = false;
								while (type.endsWith("+"))
								{
									isSuperclass = true;
									type = type.substring(0, type.length() - 1);
								}
								if (isSuperclass)
								{
									shelf.addSuperclassForVar(argI, type);
								}
								else
								{
									shelf.addClassForVar(argI, type);
								}
							}
						}
					}
					sb.append(insertTypeRestrictions(argI, shelf, kb));
				}
				sb.append(")");
			}
			result = sb.toString();
		}
		LOGGER.exiting(LOG_SOURCE, "insertTypeRestrictions", result);
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
	private static String insertTypeRestrictionsU(@NotNull final String form, @NotNull final Shelf shelf, @NotNull final KB kb)
	{
		LOGGER.entering(LOG_SOURCE, "insertTypeRestrictionsU", new String[]{"shelf = " + shelf, "kb = " + kb.name});

		// var list
		@NotNull String vars = Lisp.getArgument(form, 1);
		int nvars = Lisp.listLength(vars);

		// shelf with data for the vars in var list
		@NotNull Shelf newShelf = Shelf.makeNewShelf(shelf);
		for (int i = 0; i < nvars; i++)
		{
			newShelf.addVarData(Lisp.getArgument(vars, i), 'U');
		}

		// body
		@NotNull String body = Lisp.getArgument(form, 2);
		@NotNull String newBody = insertTypeRestrictions(body, newShelf, kb);

		// constraints
		@NotNull Set<String> constraints = makeUConstraints(newBody, newShelf, kb);

		// prepend constraints to body using and
		@NotNull String result = prependUConstraints(vars, newBody, constraints);
		LOGGER.exiting(LOG_SOURCE, "insertTypeRestrictionsU", result);
		return result;
	}

	@NotNull
	private static Set<String> makeUConstraints(@NotNull final String body, @NotNull final Shelf newShelf, @NotNull final KB kb)
	{
		@NotNull Set<String> constraints = new LinkedHashSet<>();
		for (@NotNull Shelf.Data varData : newShelf)
		{
			String var = varData.first;
			Character token = varData.second;
			if (token == 'U')
			{
				List<String> classes = varData.third;
				List<String> superclasses = varData.fourth;

				// superclasses var must be a subclass of
				if (!superclasses.isEmpty())
				{
					winnowTypeList(superclasses, kb);
					if (!superclasses.isEmpty())
					{
						if (!classes.contains("SetOrClass"))
						{
							classes.add("SetOrClass");
						}
						for (String superclass : superclasses)
						{
							// (subclass var superclass)
							@NotNull String constraint = Formula.LP + "subclass" + Formula.SPACE + var + Formula.SPACE + superclass + Formula.RP;
							if (!body.contains(constraint))
							{
								constraints.add(constraint);
							}
						}
					}
				}
				// classes var must be an instance of
				if (!classes.isEmpty())
				{
					winnowTypeList(classes, kb);
					for (String className : classes)
					{
						// (instance var class)
						@NotNull String constraint = Formula.LP + "instance" + Formula.SPACE + var + Formula.SPACE + className + Formula.RP;
						if (!body.contains(constraint))
						{
							constraints.add(constraint);
						}
					}
				}
			}
		}
		return constraints;
	}

	@NotNull
	private static String prependUConstraints(String vars, String body, @NotNull Set<String> constraints)
	{
		@NotNull StringBuilder sb = new StringBuilder();

		// (forall vars
		sb.append(Formula.LP) //
				.append(Formula.UQUANT) //
				.append(Formula.SPACE) //
				.append(vars);
		if (constraints.isEmpty())
		{
			// (forall vars body2
			sb.append(Formula.SPACE) //
					.append(body);
		}
		else
		{
			// (forall vars (=>
			sb.append(Formula.SPACE).append(Formula.LP).append(Formula.IF);
			int nconstraints = constraints.size();
			if (nconstraints > 1)
			{
				//  (forall vars (=> (and
				sb.append(Formula.SPACE).append(Formula.LP).append("and");
			}
			// (forall vars (=> constraint
			// (forall vars (=> (and constraint1 constraint2 ...
			for (String constraint : constraints)
			{
				sb.append(Formula.SPACE);
				sb.append(constraint);
			}
			if (nconstraints > 1)
			{
				// (forall vars (=> (and constraint1 constraint2 ...)
				sb.append(Formula.RP);
			}
			// (forall vars (=> constraint body2)
			// (forall vars (=> (and constraint1 constraint2 ...) body2)
			sb.append(Formula.SPACE);
			sb.append(body);
			sb.append(Formula.RP);
		}
		// (forall vars (=> constraint body2))
		// (forall vars (=> (and constraint1 constraint2 ...) body2))
		// (forall vars body2)
		sb.append(Formula.RP);
		return sb.toString();
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
	private static String insertTypeRestrictionsE(@NotNull final String form, @NotNull final Shelf shelf, @NotNull final KB kb)
	{
		LOGGER.entering(LOG_SOURCE, "insertTypeRestrictionsE", new String[]{"shelf = " + shelf, "kb = " + kb.name});

		// var list
		@NotNull String vars = Lisp.getArgument(form, 1);
		int nvars = Lisp.listLength(vars);

		// shelf with data for the vars in var list
		@NotNull Shelf newShelf = Shelf.makeNewShelf(shelf);
		for (int i = 0; i < nvars; i++)
		{
			newShelf.addVarData(Lisp.getArgument(vars, i), 'E');
		}

		// body
		@NotNull String body = Lisp.getArgument(form, 2);
		@NotNull String newBody = insertTypeRestrictions(body, newShelf, kb);

		// constraints
		@NotNull Set<String> constraints = makeEConstraints(newBody, newShelf, kb);

		// prepend constraints to body using and
		@NotNull String result = prependEConstraints(vars, newBody, constraints);
		LOGGER.exiting(LOG_SOURCE, "insertTypeRestrictionsE", result);
		return result;
	}

	@NotNull
	private static Set<String> makeEConstraints(@NotNull final String body, @NotNull final Shelf newShelf, @NotNull final KB kb)
	{
		@NotNull Set<String> constraints = new LinkedHashSet<>();
		for (@NotNull Shelf.Data varData : newShelf)
		{
			String var = varData.first;
			Character token = varData.second;
			if (token == 'E')
			{
				List<String> classes = varData.third;
				List<String> superclasses = varData.fourth;

				// superclasses var must be a subclass of
				if (!superclasses.isEmpty())
				{
					winnowTypeList(superclasses, kb);
					for (String superclass : superclasses)
					{
						// (subclass var superclass)
						@NotNull String constraint = Formula.LP + "subclass" + Formula.SPACE + var + Formula.SPACE + superclass + Formula.RP;
						if (!body.contains(constraint))
						{
							constraints.add(constraint);
						}
					}
				}
				// classes var must be an instance of
				if (!classes.isEmpty())
				{
					winnowTypeList(classes, kb);
					for (String className : classes)
					{
						// (instance var class)
						@NotNull String constraint = Formula.LP + "instance" + Formula.SPACE + var + Formula.SPACE + className + Formula.RP;
						if (!body.contains(constraint))
						{
							constraints.add(constraint);
						}
					}
				}
			}
		}
		return constraints;
	}

	@NotNull
	private static String prependEConstraints(String vars, @NotNull String body, @NotNull Set<String> constraints)
	{
		@NotNull StringBuilder sb = new StringBuilder();

		// (exist vars
		sb.append(Formula.LP) //
				.append(Formula.EQUANT) //
				.append(Formula.SPACE) //
				.append(vars);
		if (constraints.isEmpty())
		{
			// (exist vars body2
			sb.append(Formula.SPACE) //
					.append(body);
		}
		else
		{
			// (exist vars (and constraint1 constraint2 ...
			sb.append(Formula.SPACE) //
					.append(Formula.LP) //
					.append("and");
			for (String constraint : constraints)
			{
				sb.append(Formula.SPACE) //
						.append(constraint);
			}
			if (Lisp.car(body).equals("and"))
			{
				// body=(and conjunct1 conjunct2 ...)
				// (exist vars (and constraint1 constraint2 ... conjunct1 conjunct2 ...
				int nconjuncts = Lisp.listLength(body);
				for (int k = 1; k < nconjuncts; k++)
				{
					sb.append(Formula.SPACE) //
							.append(Lisp.getArgument(body, k));
				}
			}
			else
			{
				// (exist vars (and constraint1 constraint2 ... conjunct1 conjunct2 ...
				// (exist vars (and constraint1 constraint2 ... body2
				sb.append(Formula.SPACE) //
						.append(body);
			}
			// (exist vars (and constraint1 constraint2 ... conjunct1 conjunct2 ...)
			// (exist vars (and constraint1 constraint2 ... body2)
			sb.append(Formula.RP);
		}
		// (exist vars (and constraint1 constraint2 ... conjunct1 conjunct2 ...))
		// (exist vars (and constraint1 constraint2 ... body2))
		sb.append(Formula.RP);
		return sb.toString();
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
		LOGGER.entering(LOG_SOURCE, "winnowTypeList", new String[]{"types = " + types, "kb = " + kb.name});
		if (types != null && types.size() > 1)
		{
			@NotNull String[] typeArray = types.toArray(new String[0]);
			for (int i = 0; i < typeArray.length; i++)
			{
				boolean stop = false;
				for (int j = 0; j < typeArray.length; j++)
				{
					if (i != j)
					{
						String className = typeArray[i];
						String className2 = typeArray[j];
						if (kb.isSubclass(className, className2))
						{
							types.remove(className2);
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
		LOGGER.exiting(LOG_SOURCE, "winnowTypeList", types);
	}

	// F I N D

	/**
	 * Find the argument type restriction for a given predicate and
	 * argument number that is inherited from one of its
	 * super-relations.
	 * A "+" is appended to the type if the
	 * parameter must be a class.
	 * Argument number 0 is used for the return type of a Function.
	 *
	 * @param pred         predicate
	 * @param targetArgPos argument index
	 * @param kb           knowledge base
	 * @return type restriction
	 */
	@Nullable
	public static String findType(@NotNull final String pred, int targetArgPos, @NotNull final KB kb)
	{
		LOGGER.entering(LOG_SOURCE, "findType", new String[]{"pred = " + pred, "pos = " + targetArgPos, "kb = " + kb.name});

		// build the sortalTypeCache key.
		@NotNull String key = "ft" + targetArgPos + pred + kb.name;

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
			@NotNull Set<String> accumulator = new HashSet<>();
			accumulator.add(pred);

			boolean found = false;
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
					if (targetArgPos > 0)
					{
						// argument 1, 2 ...
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
							if (argPos == targetArgPos)
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
								if (argPos == targetArgPos)
								{
									result = f.getArgument(3) + "+";
									found = true;
									break;
								}
							}
						}
					}
					else if (targetArgPos == 0)
					{
						// argument number 0 is used for the return type of a Function.
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
			// cache result
			if (cacheResult && result != null)
			{
				typeCache.put(key, Collections.singletonList(result));
			}
		}
		LOGGER.exiting(LOG_SOURCE, "findType", result);
		return result;
	}
}
