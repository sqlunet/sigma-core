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

package org.sigma.core;

import org.sigma.core.kif.KIF;

import java.io.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toUnmodifiableList;

/**
 * Contains methods for reading, writing knowledge bases and their
 * configurations.
 */
public class BaseKB implements KBIface, KBQuery, Serializable
{
	private static final long serialVersionUID = 2L;

	private static final String LOG_SOURCE = "BaseKB";

	private static final boolean WARN_DUPLICATES = "yes".equalsIgnoreCase(KBSettings.getPref(KBSettings.KEY_ADD_HOLDS_PREFIX));

	private static final Logger LOGGER = Logger.getLogger(BaseKB.class.getName());

	/**
	 * The location of preprocessed KIF files
	 */
	@Nullable
	public final String kbDir;

	/**
	 * The name of the knowledge base.
	 */
	@Nullable
	public final String name;

	// constituents

	/**
	 * A List of Strings that are the full canonical pathnames of the files that comprise the KB.
	 */
	public final Collection<String> constituents = new ArrayList<>();

	// core data

	/**
	 * A Set of Strings, which are all the terms in the KB.
	 */
	public final Set<String> terms = new TreeSet<>();

	/**
	 * A Map of all the Formula objects in the KB.
	 * Each key is a String representation of a Formula.
	 * Each value is the Formula object corresponding to the key.
	 */
	public final Map<String, Formula> formulas = new LinkedHashMap<>();

	/**
	 * A Map of Lists of String formulae, containing all the formulae in the KB.
	 * Keys are the formula itself, a formula ID, and term indexes created in KIF.createKey().
	 * The actual formula can be retrieved by using the returned String as the key for the variable formulaMap
	 */
	public final Map<String, Collection<Formula>> formulaIndex = new HashMap<>();

	// format maps

	/**
	 * The natural language formatting strings for relations in the KB.
	 * It is a Map of language keys and Map values.
	 * The interior Map is term name keys and String values.
	 */
	@NotNull
	protected final Map<String, Map<String, String>> formatMap = new HashMap<>();

	/**
	 * The natural language strings for terms in the KB.
	 * It is a Map of language keys and Map values.
	 * The interior Map is term name keys and String values.
	 */
	@NotNull
	protected final Map<String, Map<String, String>> termFormatMap = new HashMap<>();

	// log

	/**
	 * Errors and warnings found during loading of the KB constituents.
	 */
	public final Set<String> errors = new LinkedHashSet<>();

	// C O N S T R U C T O R

	/**
	 * Constructor (for deserialization)
	 */
	protected BaseKB()
	{
		name = null;
		kbDir = null;
	}

	/**
	 * Constructor which takes the name of the KB and the location where KBs preprocessed for Vampire should be placed.
	 *
	 * @param name name
	 * @param dir  directory
	 */
	public BaseKB(@Nullable final String name, @Nullable final String dir)
	{
		this.name = name;
		kbDir = dir;
	}

	/**
	 * Constructor
	 *
	 * @param name name
	 */
	public BaseKB(@Nullable final String name)
	{
		this.name = name;
		kbDir = KBSettings.getPref(KBSettings.KEY_KBDIR);
	}

	// L O A D

	/**
	 * Add a new KB constituent by reading in the file, and then
	 * merging the formulas with the existing set of formulas.  All
	 * assertion caches are rebuilt, the current Vampire process is
	 * destroyed, and a new one is created.
	 *
	 * @param filename - the full path of the file being added.
	 * @return false if unrecoverable error.
	 */
	public boolean addConstituent(@NotNull final String filename)
	{
		return addConstituent(filename, null, null);
	}

	/**
	 * Add a new KB constituent by reading in the file, and then merging
	 * the formulas with the existing set of formulas.
	 *
	 * @param filePath     - The full path of the file being added
	 * @param postAdd      - Post adding constituent, passed the canonical path
	 * @param arityChecker - Arity checker function
	 * @return false if unrecoverable error.
	 */
	public boolean addConstituent(@NotNull final String filePath, @Nullable final Consumer<String> postAdd, @Nullable final Function<Formula, Boolean> arityChecker)
	{
		LOGGER.entering(LOG_SOURCE, "addConstituent", "Constituent = " + FileUtil.basename(filePath));

		// sanity check
		@NotNull String id = getUniqueName(filePath);

		// read
		try (InputStream is = new FileInputStream(filePath))
		{
			return addConstituent(is, id, postAdd, arityChecker);
		}
		catch (IOException ioe)
		{
			@NotNull String error = ioe.getMessage() + " in " + id;
			LOGGER.severe(error);
			errors.add(error);
			return false;
		}
	}

	/**
	 * Add a new KB constituent by reading in the input stream, and then merging
	 * the formulas with the existing set of formulas.
	 *
	 * @param is           - input stream
	 * @param id           - input stream
	 * @return false if unrecoverable error.
	 */
	public boolean addConstituent(@NotNull final InputStream is, @NotNull final String id)
	{
		return addConstituent(is, id, null, null);
	}

	/**
	 * Add a new KB constituent by reading in the input stream, and then merging
	 * the formulas with the existing set of formulas.
	 *
	 * @param is           - input stream
	 * @param postAdd      - Post adding constituent, passed the id
	 * @param arityChecker - Arity checker function
	 * @return false if unrecoverable error.
	 */
	public boolean addConstituent(@NotNull final InputStream is, @NotNull final String id, @Nullable final Consumer<String> postAdd, @Nullable final Function<Formula, Boolean> arityChecker)
	{
		LOGGER.entering(LOG_SOURCE, "addConstituent", "Constituent = " + id);

		// sanity check
		if (constituents.contains(id))
		{
			errors.add("Error: " + id + " already loaded.");
		}
		LOGGER.finer("Adding " + id + " to KB.");

		// read KIF file
		@NotNull KIF file = new KIF();
		try
		{
			file.read(is, id);
			errors.addAll(file.warnings);
		}
		catch (IOException ioe)
		{
			@NotNull String error = ioe.getMessage() + " in " + id;
			LOGGER.severe(error);
			errors.add(error);
			return false;
		}

		// formulas duplicate check
		LOGGER.finer("Parsed file " + id + " containing " + file.formulas.size() + " KIF expressions");
		int keyCount = 0;
		int formulaCount = 0;
		for (String form : file.formulas)
		{
			boolean duplicate = formulas.containsKey(form);
			if (duplicate)
			{
				Formula f = file.formulaIndex.get(form).get(0);
				Formula existingFormula = formulas.get(form);
				@NotNull String error = "Duplicate axiom in " + f.sourceFile + ":" + f.startLine;
				@NotNull String error2 = "also in " + existingFormula.sourceFile + ":" + existingFormula.startLine;
				errors.add(error + " " + f.form + " " + error2);
				if (WARN_DUPLICATES)
				{
					LOGGER.warning(error + " " + f.form + " " + error2);
				}
			}
		}

		// inherit formulas + index
		for (String key : file.formulaIndex.keySet())
		{
			// Iterate through the formulas in the file, adding them to the KB, at the appropriate key.
			// Note that this is a slow operation that needs to be improved
			for (@NotNull Formula f : file.formulaIndex.get(key))
			{
				boolean allow = true;
				if (arityChecker != null)
				{
					allow = arityChecker.apply(f);
					if (!allow)
					{
						errors.add("REJECTED formula at " + f.sourceFile + ':' + f.startLine + " because of incorrect arity: " + f.form);
						System.err.println("REJECTED formula at " + f.sourceFile + ':' + f.startLine + " because of incorrect arity: " + f.form);
					}
				}
				if (allow)
				{
					synchronized (this)
					{
						@NotNull Collection<Formula> indexed = formulaIndex.computeIfAbsent(key, k -> new ArrayList<>());
						if (!indexed.contains(f))
						{
							// accept formula
							indexed.add(f);
							++keyCount;
						}
						if (!formulas.containsKey(f.form))
						{
							// accept formula
							formulas.put(f.form, f);
							++formulaCount;
						}
					}
				}
			}

			// progress
			if (keyCount % 1000 == 1)
			{
				FileUtil.PROGRESS_OUT.print('+');
			}
		}

		// inherit terms
		synchronized (this)
		{
			terms.addAll(file.terms);
		}

		// add as constituent
		if (!constituents.contains(id))
		{
			constituents.add(id);
		}

		// Post adding constituent.
		if (postAdd != null)
		{
			postAdd.accept(id);
		}

		FileUtil.PROGRESS_OUT.println();
		LOGGER.info("Added " + id + " to KB: keys=" + keyCount + ", formulas=" + formulaCount);
		LOGGER.exiting(LOG_SOURCE, "addConstituent", "Constituent " + id + " successfully added to KB: " + name);
		return true;
	}

	private String getUniqueName(@NotNull final String filePath)
	{
		try
		{
			return new File(filePath).getCanonicalPath();
		}
		catch (IOException e)
		{
			return Integer.toString(filePath.hashCode());
		}
	}

	// T E R M S

	/**
	 * Returns a set of Strings, which are all the terms in the knowledge base.
	 *
	 * @return a set of all the terms in the KB.
	 */
	@Override
	@NotNull
	public Set<String> getTerms()
	{
		return terms;
	}

	/**
	 * Count the number of terms in the knowledge base in order to
	 * present statistics to the user.
	 *
	 * @return The integer number of terms in the knowledge base.
	 */
	public int getTermsCount()
	{
		return terms.size();
	}

	/**
	 * Takes a term and returns true if the term occurs in the KB.
	 *
	 * @param term A String.
	 * @return true or false.
	 */
	public boolean containsTerm(@NotNull final String term)
	{
		return terms.contains(term) || findTermsMatching(term).size() == 1;
	}

	/**
	 * Return List of all non-relation Terms in a List
	 *
	 * @param terms input list
	 * @return A List of non-relation Terms
	 */
	@NotNull
	public static Collection<String> filterNonRelnTerms(@NotNull final Collection<String> terms)
	{
		return terms.stream().filter(BaseKB::isNonReln).collect(toList());
	}

	/**
	 * Return List of all relnTerms in a List
	 *
	 * @param terms input list
	 * @return A List of relTerms
	 */
	@NotNull
	public static Collection<String> filterRelnTerms(@NotNull final Collection<String> terms)
	{
		return terms.stream().filter(BaseKB::isReln).collect(toList());
	}

	/**
	 * Takes a Regular Expression and returns a List
	 * containing every term in the KB that has a match with the RE.
	 *
	 * @param regexp A String
	 * @return A List of terms that have a match to term
	 */
	@NotNull
	public Collection<String> findTermsMatching(@NotNull final String regexp) throws PatternSyntaxException
	{
		@NotNull Pattern p = Pattern.compile(regexp);
		return terms.stream().filter(t -> p.matcher(t).matches()).collect(toList());
	}

	// T E R M S   T E S T S

	/**
	 * Test whether t is a non-relation.
	 *
	 * @param t A String.
	 * @return true if t is a SUO-KIF non-relation, else false.
	 */
	public static boolean isNonReln(@NotNull final String t)
	{
		if (!t.isEmpty())
		{
			return Character.isUpperCase(t.charAt(0));
		}
		return false;
	}

	/**
	 * Test whether t is a relation.
	 *
	 * @param t A String.
	 * @return true if t is a SUO-KIF relation, else false.
	 */
	public static boolean isReln(@NotNull final String t)
	{
		if (!t.isEmpty())
		{
			return Character.isLowerCase(t.charAt(0));
		}
		return false;
	}

	/**
	 * Test whether t is a variable.
	 *
	 * @param t A String.
	 * @return true if t is a SUO-KIF variable, else false.
	 */
	public static boolean isVariable(@NotNull final String t)
	{
		if (!t.isEmpty())
		{
			return t.startsWith(Formula.V_PREFIX) || t.startsWith(Formula.R_PREFIX);
		}
		return false;
	}

	/**
	 * Test whether t is a quantifier.
	 *
	 * @param t A String.
	 * @return true if t is a SUO-KIF logical quantifier, else
	 * false.
	 */
	public static boolean isQuantifier(@NotNull final String t)
	{
		return Formula.UQUANT.equals(t) || Formula.EQUANT.equals(t);
	}

	// F O R M U L A S

	/**
	 * An accessor providing a Set of un-preProcessed String
	 * representations of Formulae.
	 *
	 * @return A SortedSet of Strings.
	 */
	@NotNull
	public Set<String> getForms()
	{
		return formulas.keySet();
	}

	/**
	 * An accessor providing a Collection of Formulae.
	 *
	 * @return A Collection of Formulae.
	 */
	@NotNull
	public Collection<Formula> getFormulas()
	{
		return formulas.values();
	}

	/**
	 * Count the number of formulas.
	 *
	 * @return The long number of formulas in the knowledge base.
	 */
	public long getCountFormulas()
	{
		return formulas.size();
	}

	/**
	 * Count the number of rules in the knowledge base.
	 * Note that the set of rules is a subset of the set of formulas.
	 *
	 * @return The long number of rules in the knowledge base.
	 */
	public long getCountRules()
	{
		return getFormulaCount(Formula::isRule);
	}

	/**
	 * Count the number of rules in the knowledge base
	 *
	 * @param predicate filter predicate
	 * @return The long number of rules in the knowledge base.
	 */
	public long getFormulaCount(@NotNull final Predicate<Formula> predicate)
	{
		return formulas.values().stream().filter(predicate).count();
	}

	// Q U E R Y

	@Override
	public Collection<Formula> queryFormulas(final String arg1, final int pos1)
	{
		return Collections.unmodifiableCollection(askWithRestriction(pos1, arg1));
	}

	@Override
	public Collection<Formula> queryFormulas(final String arg1, final int pos1, final String arg2, final int pos2)
	{
		return Collections.unmodifiableCollection(askWithRestriction(pos1, arg1, pos2, arg2));
	}

	@Override
	public Collection<Formula> queryFormulas(final String arg1, final int pos1, final String arg2, final int pos2, final String arg3, final int pos3)
	{
		return Collections.unmodifiableCollection(askWithRestriction(pos1, arg1, pos2, arg2, pos3, arg3));
	}

	// A S K

	public enum AskKind
	{
		ARG("arg"), ANT("ant"), CONS("cons"), STMT("stmt");

		public final String query;

		AskKind(String query)
		{
			this.query = query;
		}

		@Override
		public String toString()
		{
			return query;
		}
	}

	/**
	 * Ask by building a key into formula index and retrieving value from it.
	 * Returns a List containing the Formulas that match the request.
	 *
	 * @param kind May be one of "ant", "cons", "stmt", or "arg"
	 * @param arg  The term that appears in the statements being
	 *             requested.
	 * @param pos  The argument position of the term being asked
	 *             for.  The first argument after the predicate
	 *             is "1". This parameter is ignored if the kind
	 *             is "ant", "cons" or "stmt".
	 * @return A List of Formula(s), which will be empty if no match found.
	 */
	@NotNull
	public Collection<Formula> ask(@NotNull final AskKind kind, final int pos, @Nullable final String arg)
	{
		// sanity check
		if (arg == null || arg.isEmpty())
		{
			@NotNull String errStr = "Error in BaseKB.ask(\"" + kind + "\", " + pos + ", \"" + arg + "\"): " + "search term is null, or an empty string";
			LOGGER.warning(errStr);
			throw new IllegalArgumentException(errStr);
		}
		if (arg.length() > 1 && arg.charAt(0) == '"' && arg.charAt(arg.length() - 1) == '"')
		{
			@NotNull String errStr = "Error in BaseKB.ask(): Strings are not indexed.  No results for " + arg;
			LOGGER.warning(errStr);
			throw new IllegalArgumentException(errStr);
		}

		// query formula index
		@NotNull String key = AskKind.ARG.equals(kind) ? //
				AskKind.ARG + "-" + pos + "-" + arg : //
				kind.toString() + "-" + arg;
		Collection<Formula> result = formulaIndex.get(key);
		return result != null ? result : new ArrayList<>();
	}

	// A S K   W I T H   R E S T R I C T I O N (S)

	/**
	 * Ask with restriction on a single argument (including arg 0)
	 *
	 * @param pos1 position of arg 1
	 * @param arg1 arg 1 (term)
	 * @return a List of Formulas in which the term
	 * provided appear in the indicated argument position.
	 * If there are no Formula(s) matching the given term and
	 * argument position, return an empty List.
	 */
	@NotNull
	public Collection<Formula> askWithRestriction(final int pos1, @NotNull final String arg1)
	{
		if (!arg1.isEmpty())
		{
			@NotNull Collection<Formula> result = ask(AskKind.ARG, pos1, arg1);
			return result; //.stream().distinct().collect(toList());
		}
		return Collections.emptyList();
	}

	/**
	 * Ask with restrictions on a pair of arguments (possibly arg 0).
	 *
	 * @param pos1 position of arg 1
	 * @param arg1 arg 1 (term)
	 * @param pos2 position of arg 2
	 * @param arg2 arg 2 (term)
	 * @return a List of Formulas in which the two terms
	 * provided appear in the indicated argument positions.
	 * If there are no Formula(s) matching the given terms and respective
	 * argument positions, return an empty List.
	 * Builds two lists if formulas and iterate through the smaller list.
	 */
	@NotNull
	public Collection<Formula> askWithRestriction(final int pos1, @NotNull final String arg1, final int pos2, @NotNull final String arg2)
	{
		if (!arg1.isEmpty() && !arg2.isEmpty())
		{
			@NotNull Collection<Formula> result1 = ask(AskKind.ARG, pos1, arg1);
			@NotNull Collection<Formula> result2 = ask(AskKind.ARG, pos2, arg2);
			boolean firstBigger = result1.size() > result2.size();

			// scan the smaller (source) for target
			@NotNull final Collection<Formula> source = firstBigger ? result2 : result1;
			final int targetPos = firstBigger ? pos1 : pos2;
			@NotNull final String targetArg = firstBigger ? arg1 : arg2;

			// intersection : filter source for targetPos at targetNum position
			return source.stream().filter(f -> f.getArgument(targetPos).equals(targetArg)).distinct().collect(toList());
		}
		return Collections.emptyList();
	}

	/**
	 * Ask with restrictions on a triple of arguments (possibly arg 0).
	 * Returns a List of Formulas in which the three terms
	 * provided appear in the indicated argument positions.  If there
	 * are no Formula(s) matching the given terms and respective
	 * argument positions, return an empty List.
	 * Builds three lists if formulas and iterate through the smallest list
	 * then second smallest.
	 *
	 * @param pos1 position of arg 1
	 * @param arg1 arg 1 (term)
	 * @param pos2 position of arg 2
	 * @param arg2 arg 2 (term)
	 * @param pos3 position of arg 3
	 * @param arg3 arg 3 (term)
	 * @return List of formulas.
	 */
	@NotNull
	public Collection<Formula> askWithRestriction(final int pos1, @NotNull final String arg1, final int pos2, @NotNull final String arg2, final int pos3, @NotNull final String arg3)
	{
		if (!arg1.isEmpty() && !arg2.isEmpty() && !arg3.isEmpty())
		{
			@NotNull Collection<Formula> result1 = ask(AskKind.ARG, pos1, arg1);
			int size1 = result1.size();
			@NotNull Collection<Formula> result2 = ask(AskKind.ARG, pos2, arg2);
			int size2 = result2.size();
			@NotNull Collection<Formula> result3 = ask(AskKind.ARG, pos3, arg3);
			int size3 = result3.size();

			// scan the smaller (source) for target
			@NotNull Collection<Formula> source = result3;
			int _targetPos1 = pos2;
			int _targetPos2 = pos1;
			@NotNull String _targetArg1 = arg2;
			@NotNull String _targetArg2 = arg1;
			if (size1 > size2 && size1 > size3)
			{
				// 1 biggest
				// _targetPos2 = pos1;
				_targetArg2 = arg1;
				if (size2 > size3)
				{
					// 2 second  biggest (1 2 3)
					// _targetPos1 = pos2;
					_targetArg1 = arg2;
					// source = result3;
				}
				else
				{
					// 3 second  biggest (1 3 2)
					_targetPos1 = pos3;
					_targetArg1 = arg3;
					source = result2;
				}
			}
			else if (size2 > size1 && size2 > size3)
			{
				// 2 biggest
				_targetPos2 = pos2;
				_targetArg2 = arg2;
				if (size1 > size3)
				{
					// 1 second biggest (2 1 3)
					_targetPos1 = pos1;
					_targetArg1 = arg1;
					// source = result3;
				}
				else
				{
					// 3 second biggest (2 3 1)
					_targetPos1 = pos3;
					_targetArg1 = arg3;
					source = result1;
				}
			}
			else if (size3 > size1 && size3 > size2)
			{
				// 3 biggest
				_targetPos2 = pos3;
				_targetArg2 = arg3;
				if (size1 > size2)
				{
					// 1 second biggest (3 1 2)
					_targetPos1 = pos1;
					_targetArg1 = arg1;
					source = result2;
				}
				else
				{
					// 2 second biggest (3 2 1)
					// _targetPos1 = pos2;
					_targetArg1 = arg2;
					source = result1;
				}
			}

			// intersection : filter source for targetArgPos1 at targetArgPos1 position and targetArgPos2 at targetArgPos2 position
			final int targetPos1 = _targetPos1;
			@NotNull final String targetArg1 = _targetArg1;
			final int targetPos2 = _targetPos2;
			@NotNull final String targetArg2 = _targetArg2;
			return source.stream().filter(f -> f.getArgument(targetPos1).equals(targetArg1) && f.getArgument(targetPos2).equals(targetArg2)).distinct().collect(toList());
		}
		return Collections.emptyList();
	}

	// A S K   W I T H   S U B S U M P T I O N

	/**
	 * Returns a List containing the Formulae retrieved,
	 * possibly via multiple asks that recursively use relation and
	 * all of its subrelations.
	 * Note that the Formulas might be formed with different predicates,
	 * but all the predicates will be subrelations of relation and
	 * will be related to each other in a subsumption hierarchy.
	 *
	 * @param reln0 The name of a predicate, which is assumed to be
	 *              the 0th argument of one or more atomic
	 *              formulae
	 * @param pos   The argument position occupied by idxTerm in
	 *              each ground Formula to be retrieved
	 * @param arg   A constant that occupies pos position in
	 *              each ground Formula to be retrieved
	 * @return a List of Formulas that satisfy the query, or an
	 * empty List if no Formulae are retrieved.
	 */
	@NotNull
	public Collection<Formula> askWithPredicateSubsumption(@NotNull final String reln0, final int pos, @NotNull final String arg)
	{
		return Queue.run(reln0, r -> queryFormulas(r, 0, arg, pos), this::querySubsumedRelationsOf);
	}

	// A S K   W I T H   L I T E R A L

	/**
	 * This method retrieves Formulas by asking the query expression
	 * query, and returns the results, if any, in a List.
	 * (predicate const|var+ )
	 *
	 * @param query The query, which is assumed to be a List
	 *              (atomic literal) consisting of a single predicate and its
	 *              arguments.  The arguments could be variables, constants, or a
	 *              mix of the two, but only the first constant encountered in a
	 *              left to right sweep over the literal will be used in the actual
	 *              query. The queried argument position is that of the constant.
	 * @return A List of Formula objects, or an empty List
	 * if no answers are retrieved.
	 */
	@NotNull
	public Collection<Formula> askWithLiteral(@Nullable final List<String> query)
	{
		if (query != null && !query.isEmpty())
		{
			String pred = query.get(0);

			// first constant
			@Nullable String arg = null;
			int pos = -1;
			int qLen = query.size();
			for (int i = 1; i < qLen; i++)
			{
				String argI = query.get(i);
				if (!argI.isEmpty() && !isVariable(argI))
				{
					arg = argI;
					pos = i;
					break;
				}
			}

			// ask
			return arg != null ? askWithRestriction(0, pred, pos, arg) : ask(AskKind.ARG, 0, pred);
		}
		return Collections.emptyList();
	}

	// S U B S U M P T I O N

	/**
	 * All Subrelations in KB
	 * Currently returns singleton {'subrelation'}
	 *
	 * @return 'subrelation' and other subrelations of 'subrelation'
	 */
	public Set<String> getSubrelations()
	{
		// get all subrelations of 'subrelation' or equal to 'subrelation'
		@NotNull Set<String> result = new HashSet<>();
		result.add("subrelation");
		result.addAll(query("subrelation", "subrelation", 2, 1)); // (subrelation ? subrelation)
		result.addAll(query("equal", "subrelation", 2, 1)); // (equal ? subrelation)
		result.addAll(query("equal", "subrelation", 1, 2)); // (equal subrelation ?)
		return result;
	}

	/**
	 * Subsumed relations of a relation
	 * Subrelations are those sr asserted with
	 * - a (subrelation sr r) statement or
	 * - a (subsubrelation sr r) statement where
	 * subsubrelation is a subrelation of 'subrelation', asserted by
	 * a (subrelation subsubrelation subrelation) statement,
	 * currently none.
	 *
	 * @param reln A relation
	 * @return subsumed relations of reln
	 */
	public Set<String> querySubsumedRelationsOf(@NotNull final String reln)
	{
		// get all subrelations of 'subrelation'
		// (subrelation ?X subrelation)
		@NotNull Collection<String> subrelns = getSubrelations();
		return querySubsumedRelationsOf(reln, subrelns);
	}

	/**
	 * Subsumed relations of a relation
	 * Subrelations are those sr asserted with
	 * - a (subrelation sr r) statement or
	 * - a (subsubrelation sr r) statement where
	 * subsubrelation is a subrelation of 'subrelation', asserted by
	 * a (subrelation subsubrelation subrelation) statement,
	 * currently none.
	 *
	 * @param reln     A relation
	 * @param subrelns Relations that qualify as subrelation (includes 'subrelation').
	 * @return subsumed relations of reln
	 */
	public Set<String> querySubsumedRelationsOf(@NotNull final String reln, @NotNull final Collection<String> subrelns)
	{
		// get all subrelations of reln.
		@NotNull Set<String> result = new HashSet<>();
		//result.add(reln); // do not include self ib result
		result.addAll(query("equal", reln, 2, 1));
		result.addAll(query("equal", reln, 1, 2));
		for (@NotNull String subreln : subrelns)
		{
			// (subreln ?X reln ?X subreln)
			// (subrelation|subrelationofsubrelation ?X instance|subclass)
			// (subrelation immediateInstance instance) -> immediateInstance
			// (subrelation element instance) -> element
			// (subrelation immediateSubclass subclass) -> immediateSubclass
			// (subrelation subset subclass) -> subset
			result.addAll(query(subreln, reln, 2, 1));
			result.addAll(query("equal", subreln, 2, 1));
			result.addAll(query("equal", subreln, 1, 2));
		}
		return result;
	}

	/**
	 * Direct subsumed relations of a relation ('instance', 'subclass').
	 * Subrelations are those asserted with a (subrelation ?X r)
	 * statement.
	 * This does not consider subrelations of 'subrelation' as
	 * possibly asserting a subrelation relationship.
	 * See querySubsumedRelationsOf()
	 *
	 * @param reln A relation (usually 'instance', 'subclass')
	 * @return subsumed relations of reln
	 */
	public Set<String> queryDirectSubsumedRelationsOf(@NotNull final String reln)
	{
		@NotNull Set<String> result = new HashSet<>();
		for (@NotNull Formula f : askWithRestriction(0, "subrelation", 2, reln))
		{
			// get subrelation
			@NotNull String subreln = f.getArgument(1);
			if (!reln.equals(subreln))
			{
				result.add(subreln);
			}
		}
		return result;
	}

	// I N V E R S E

	/**
	 * All Inverse relations in the KB.
	 * Currently returns singleton {'inverse'}
	 *
	 * @return 'inverse', subrelations of 'inverse', relations equal to 'inverse'
	 */
	public Set<String> getInverseRelations()
	{
		// get all subrelations of 'subrelation' or equal to 'subrelation'
		@NotNull Set<String> result = new HashSet<>();
		result.add("inverse");
		result.addAll(query("equal", "inverse", 2, 1)); // (equal ? inverse)
		result.addAll(query("equal", "inverse", 1, 2)); // (equal inverse ?)
		result.addAll(query("subrelation", "inverse", 2, 1)); // (subrelation ? inverse)
		return result;
	}

	/**
	 * Inverse relations of a relation
	 * Inverse relations are those ir asserted with
	 * - a (inverse ir r) statement or
	 * - a (inversereln ir r) statement
	 * where inversereln is
	 * - a subrelation of 'inverse' asserted by a (subrelation inversereln inverse) statement, or
	 * - a relation equal to 'inverse' asserted by (equal inversereln inverse) or (equal inverse inversereln)
	 *
	 * @param reln A relation
	 * @return inverse relations of reln
	 */
	public Set<String> queryInverseRelationsOf(@NotNull final String reln)
	{
		@NotNull Collection<String> inverseRelns = getInverseRelations();
		return queryInverseRelationsOf(reln, inverseRelns);
	}

	/**
	 * Inverse relations of a relation
	 * Inverse relations are those ir asserted with
	 * - a (inverse ir r) statement or
	 * - a (inversereln ir r) statement
	 * where inversereln is
	 * - a subrelation of 'inverse' asserted by a (subrelation inversereln inverse) statement, or
	 * - a relation equal to 'inverse' asserted by (equal inversereln inverse) or (equal inverse inversereln)
	 *
	 * @param reln         A relation
	 * @param inverseRelns Relations that qualify as inverse (including 'inverse')
	 * @return inverse relations of reln
	 */
	public Set<String> queryInverseRelationsOf(@NotNull final String reln, @NotNull final Collection<String> inverseRelns)
	{
		// get all inverses of reln
		// (inversereln ?X reln) or
		// (inversereln reln ?X)
		@NotNull Set<String> result = new HashSet<>();
		for (@NotNull String inverseReln : inverseRelns)
		{
			result.addAll(query(inverseReln, reln, 1, 2));
			result.addAll(query(inverseReln, reln, 2, 1));
		}
		return result;
	}

	/**
	 * Direct inverse relations of a relation
	 * Subrelations are those sr asserted with
	 * a (inverse ir r) statement.
	 * This does not consider other 'inverse' relations.
	 * See queryInverseRelationsOf()
	 *
	 * @param reln A relation
	 * @return inverse relations of reln
	 */
	public Set<String> queryDirectInverseRelationsOf(@NotNull final String reln)
	{
		// get all inverses of reln'
		// (inverse ?X reln) or
		// (inverse reln ?X)
		@NotNull Set<String> result = new HashSet<>();
		result.addAll(query("inverse", reln, 1, 2));
		result.addAll(query("inverse", reln, 2, 1));
		return result;
	}

	// F I N D

	protected boolean checkParams(@NotNull final String... args)
	{
		for (var arg : args)
		{
			if (arg.isEmpty() || StringUtil.isQuotedString(arg))
			{
				return false;
			}
		}
		return true;
	}

	// A S K   T E R M S   1

	/**
	 * Returns a List containing the terms (Strings) that
	 * at targetPos in the ground atomic Formulae in
	 * which arg is in the argument position pos.  The
	 * List returned will contain no duplicate terms.
	 *
	 * @param pos       The argument position of arg
	 * @param arg       The term that appears in the argument
	 *                  pos of the ground atomic Formulae in
	 *                  the KB
	 * @param targetPos The argument position of the terms being sought
	 * @return A List of Strings, which will be empty if no
	 * match found.
	 */
	@NotNull
	public Collection<String> askTerms(final int pos, final String arg, final int targetPos)
	{
		if (!checkParams(arg))
		{
			return Collections.emptyList();
		}
		@NotNull Collection<Formula> formulas = ask(AskKind.ARG, pos, arg);
		return formulas.stream().map(f -> f.getArgument(targetPos)).distinct().collect(toUnmodifiableList());
	}

	// A S K   T E R M S   2

	/**
	 * Returns a List containing the terms (Strings) that
	 * correspond to targetPos in the Formulas obtained from the
	 * method call askWithRestriction(pos1, arg1, pos2, arg2).
	 *
	 * @param pos1      number of args 1
	 * @param arg1      term 1
	 * @param pos2      number of args 2
	 * @param arg2      term 2
	 * @param targetPos target     number of args
	 * @return A List of terms, or an empty List if no
	 * terms can be retrieved.
	 */
	@NotNull
	public Collection<String> askTerms(final int pos1, @NotNull final String arg1, final int pos2, @NotNull final String arg2, final int targetPos)
	{
		if (!checkParams(arg1, arg2))
		{
			return Collections.emptyList();
		}
		@NotNull Collection<Formula> formulas = askWithRestriction(pos1, arg1, pos2, arg2);
		return formulas.stream() //
				.map(f -> f.getArgument(targetPos)) //
				.collect(toUnmodifiableList());
	}

	/**
	 * Returns a List containing the terms (Strings) that
	 * correspond to targetPos in the Formulas obtained from the
	 * method call askWithRestriction(pos1, arg1, pos2, arg2).
	 *
	 * @param pos1           position of args 1
	 * @param arg1           term 1
	 * @param pos2           position of args 2
	 * @param arg2           term 2
	 * @param targetPos      target     position of args
	 * @param predicatesUsed A Set to which will be added the
	 *                       predicates of the ground assertions
	 *                       actually used to gather the terms
	 *                       returned
	 * @return A List of terms, or an empty List if no
	 * terms can be retrieved.
	 */
	@NotNull
	public Collection<String> askTerms(final int pos1, @NotNull final String arg1, final int pos2, @NotNull final String arg2, final int targetPos, @Nullable final Set<String> predicatesUsed)
	{
		if (!checkParams(arg1, arg2))
		{
			return Collections.emptyList();
		}
		@NotNull Collection<Formula> formulas = askWithRestriction(pos1, arg1, pos2, arg2);
		return formulas.stream() //
				.peek(f -> {
					if (predicatesUsed != null)
					{
						// record predicates used
						predicatesUsed.add(f.car());
					}
				})//
				.map(f -> f.getArgument(targetPos)) //
				.collect(toUnmodifiableList());
	}

	/**
	 * Returns the first term found that corresponds to targetPos
	 * in the Formulas obtained from the method call
	 * askWithRestriction(pos1, arg1, pos2, arg2).
	 *
	 * @param pos1      number of args 1
	 * @param arg1      term 1
	 * @param pos2      number of args 2
	 * @param arg2      term 2
	 * @param targetPos target     number of args
	 * @return A SUO-KIF term (String), or null is no answer can be retrieved.
	 */
	@Nullable
	public String askFirstTerm(final int pos1, @NotNull final String arg1, final int pos2, @NotNull final String arg2, final int targetPos)
	{
		@NotNull Collection<String> terms = askTerms(pos1, arg1, pos2, arg2, targetPos);
		if (!terms.isEmpty())
		{
			return terms.iterator().next();
		}
		return null;
	}

	// A S K   T E R M S  3

	/**
	 * Returns a List containing the SUO-KIF terms that match the request.
	 *
	 * @param pos1      number of args 1
	 * @param arg1      term 1
	 * @param pos2      number of args 2
	 * @param arg2      term 2
	 * @param pos3      number of args 3
	 * @param arg3      term 3
	 * @param targetPos number of target number of args
	 * @return A List of terms, or an empty List if no matches can be found.
	 */
	@NotNull
	public Collection<String> askTerms(final int pos1, @NotNull final String arg1, final int pos2, @NotNull final String arg2, final int pos3, @NotNull final String arg3, final int targetPos)
	{
		if (!checkParams(arg1, arg2, arg3))
		{
			return Collections.emptyList();
		}
		@NotNull Collection<Formula> formulas = askWithRestriction(pos1, arg1, pos2, arg2, pos3, arg3);
		return formulas.stream().map(f -> f.getArgument(targetPos)).distinct().collect(toUnmodifiableList());
	}

	/**
	 * Returns a List containing SUO-KIF constants, possibly
	 * retrieved via multiple asks that recursively use relation and
	 * all of its subrelations.
	 *
	 * @param reln0          The name of a predicate, which is assumed to be
	 *                       the 0th argument of one or more atomic
	 *                       Formulae
	 * @param arg            A constant that occupies pos position in
	 *                       each of the ground atomic Formulae that will be
	 *                       retrieved to gather the target (answer) terms
	 * @param pos            The argument position occupied by arg in the
	 *                       ground atomic Formulae that will be retrieved
	 *                       to gather the target (answer) terms
	 * @param targetPos      The argument position of the answer terms
	 *                       in the Formulae to be retrieved
	 * @param useInverses    If true, the inverses of relation and its
	 *                       subrelations will be also be used to try to
	 *                       find answer terms
	 * @param predicatesUsed A Set to which will be added the
	 *                       predicates of the ground assertions
	 *                       actually used to gather the terms
	 *                       returned
	 * @return a List of terms (SUO-KIF constants), or an
	 * empty List if no terms can be retrieved
	 */
	@NotNull
	public Collection<String> askTerms(@NotNull final String reln0, @NotNull final String arg, final int pos, final int targetPos, final boolean useInverses, @Nullable final Set<String> predicatesUsed)
	{
		if (!checkParams(reln0, arg) || pos < 0 /* || pos >= Arity.MAX_PREDICATE_ARITY */)
		{
			return Collections.emptyList();
		}
		return Queue.run(reln0, //
				r -> askTerms(0, r, pos, arg, targetPos, predicatesUsed), //
				this::querySubsumedRelationsOf, //
				!useInverses ? null : r -> askTerms(0, r, targetPos, arg, pos, predicatesUsed), //
				!useInverses ? null : this::queryInverseRelationsOf, //
				predicatesUsed);
	}

	// Q U E R Y   T E R M S

	/**
	 * Returns a List containing SUO-KIF constants, possibly
	 * retrieved via multiple asks that recursively use relation and
	 * all of its subrelations.
	 *
	 * @param reln        The name of a predicate, which is assumed to be
	 *                    the 0th argument of one or more atomic
	 *                    Formulae
	 * @param arg         A constant that occupies pos position in
	 *                    each of the ground atomic Formulae that will be
	 *                    retrieved to gather the target (answer) terms
	 * @param pos         The argument position occupied by term in the
	 *                    ground atomic Formulae that will be retrieved
	 *                    to gather the target (answer) terms
	 * @param targetPos   The argument position of the answer terms
	 *                    in the Formulae to be retrieved
	 * @param useInverses If true, the inverses of relation and its
	 *                    subrelations will be also be used to try to
	 *                    find answer terms
	 * @return a List of terms (SUO-KIF constants), or an
	 * empty List if no terms can be retrieved
	 */
	@NotNull
	public Collection<String> squeryTerms(@NotNull final String reln, @NotNull final String arg, final int pos, final int targetPos, final boolean useInverses)
	{
		return squeryTerms(reln, arg, pos, targetPos, useInverses, null);
	}

	/**
	 * Returns a List containing SUO-KIF constants, possibly
	 * retrieved via multiple asks that recursively use relation and
	 * all of its subrelations.
	 *
	 * @param reln0          The name of a predicate, which is assumed to be
	 *                       the 0th argument of one or more atomic
	 *                       Formulae
	 * @param arg            A constant that occupies pos position in
	 *                       each of the ground atomic Formulae that will be
	 *                       retrieved to gather the target (answer) terms
	 * @param pos            The argument position occupied by arg in the
	 *                       ground atomic Formulae that will be retrieved
	 *                       to gather the target (answer) terms
	 * @param targetPos      The argument position of the answer terms
	 *                       in the Formulae to be retrieved
	 * @param useInverses    If true, the inverses of relation and its
	 *                       subrelations will be also be used to try to
	 *                       find answer terms
	 * @param predicatesUsed A Set to which will be added the
	 *                       predicates of the ground assertions
	 *                       actually used to gather the terms
	 *                       returned
	 * @return a List of terms (SUO-KIF constants), or an
	 * empty List if no terms can be retrieved
	 */
	@NotNull
	public Collection<String> squeryTerms(@NotNull final String reln0, @NotNull final String arg, final int pos, final int targetPos, final boolean useInverses, @Nullable final Set<String> predicatesUsed)
	{
		if (!checkParams(reln0, arg) || pos < 0 /* || pos >= Arity.MAX_PREDICATE_ARITY */)
		{
			return Collections.emptyList();
		}
		return Queue.run(reln0, //
				r -> query(r, arg, pos, targetPos), //
				this::querySubsumedRelationsOf, //
				!useInverses ? null : r -> query(r, arg, targetPos, pos), //
				!useInverses ? null : this::queryInverseRelationsOf, //
				predicatesUsed);
	}

	/**
	 * Returns the first SUO-KIF constant found via asks using
	 * relation and its subrelations.
	 *
	 * @param reln        The name of a predicate, which is assumed to be
	 *                    the 0th argument of one or more atomic
	 *                    Formulae.
	 * @param pos         The argument position occupied by term in the
	 *                    ground atomic Formulae that will be retrieved
	 *                    to gather the target (answer) terms.
	 * @param arg         A constant that occupies pos position in
	 *                    each of the ground atomic Formulae that will be
	 *                    retrieved to gather the target (answer) terms.
	 * @param targetPos   The argument position of the answer terms
	 *                    in the Formulae to be retrieved.
	 * @param useInverses If true, the inverses of relation and its
	 *                    subrelations will be also be used to try to
	 *                    find answer terms.
	 * @return A SUO-KIF constants (String), or null if no term can be retrieved.
	 */
	@Nullable
	public String squeryFirstTerm(@NotNull final String reln, final int pos, @NotNull final String arg, final int targetPos, final boolean useInverses)
	{
		if (!checkParams(reln, arg) || pos < 0 /* || pos >= Arity.MAX_PREDICATE_ARITY */)
		{
			return null;
		}
		@NotNull Collection<String> terms = squeryTerms(reln, arg, pos, targetPos, useInverses);
		if (!terms.isEmpty())
		{
			return terms.iterator().next();
		}
		return null;
	}

	// C L O S U R E

	/**
	 * Returns a List containing the transitive closure of
	 * relation starting from arg in position pos.  The
	 * result does not contain arg.
	 *
	 * @param reln        The name of a predicate, which is assumed to be
	 *                    the 0th argument of one or more atomic
	 *                    Formulae
	 * @param pos         The argument position occupied by term in the
	 *                    ground atomic Formulae that will be retrieved
	 *                    to gather the target (answer) terms
	 * @param arg         A constant that occupies pos position in
	 *                    the first "level" of ground atomic Formulae that
	 *                    will be retrieved to gather the target (answer)
	 *                    terms
	 * @param targetPos   The argument position of the answer terms
	 *                    in the Formulae to be retrieved
	 * @param useInverses If true, the inverses of relation and its
	 *                    subrelations will be also be used to try to
	 *                    find answer terms
	 * @return a List of terms (SUO-KIF constants), or an
	 * empty List if no terms can be retrieved
	 */
	@NotNull
	public Collection<String> getTransitiveClosure(@NotNull final String reln, final int pos, @NotNull final String arg, final int targetPos, boolean useInverses)
	{
		if (!checkParams(reln, arg) || pos < 0 /* || pos >= Arity.MAX_PREDICATE_ARITY */)
		{
			return Collections.emptySet();
		}
		@NotNull Set<String> result = new HashSet<>();

		// collect all ?x such that (reln ... arg@pos ... ?x@targetPos ...)
		// arg and ?x are related through reln
		@NotNull Collection<String> queue = squeryTerms(reln, arg, pos, targetPos, useInverses);
		while (!queue.isEmpty())
		{
			// add this level
			result.addAll(queue);

			// compute next level
			@NotNull List<String> args2 = new ArrayList<>(queue);
			queue.clear();
			for (@NotNull String arg2 : args2)
			{
				// collect all ?y such that (reln ... ?x@pos ... ?y@targetPos ...)
				queue.addAll(squeryTerms(reln, arg2, pos, targetPos, useInverses));
			}
		}
		return result;
	}

	// I N S T A N C E

	/**
	 * Determine whether a particular inst is an immediate instance,
	 * which has a statement of the form (instance inst otherTerm).
	 * Note that this does not count for terms such as Attribute(s)
	 * and Relation(s), which may be defined as subAttribute(s) or
	 * subrelation(s) of another instance.  If the inst is not an
	 * instance, return an empty List.  Otherwise, return a
	 * List of the Formula(s) in which the given inst is
	 * defined as an instance.
	 *
	 * @param inst A String.
	 * @return A List.
	 */
	@NotNull
	public Collection<Formula> askInstanceFormulasOf(@NotNull final String inst)
	{
		// (instance inst ?CLASS)
		return askWithRestriction(0, "instance", 1, inst);
	}

	/**
	 * Is instance
	 *
	 * @param term term
	 * @return whether term is instance.
	 */
	public boolean askIsInstance(@NotNull final String term)
	{
		// (instance term ?CLASS)
		@NotNull Collection<Formula> formulas = askInstanceFormulasOf(term);
		return formulas.size() > 0;
	}

	@NotNull
	public Set<String> askInstancesOf(@NotNull final String className)
	{
		// (instance ?INST class)
		return askWithRestriction(0, "instance", 2, className) //
				.stream() //
				.map(f -> f.getArgument(1)) //
				.collect(Collectors.toSet());
	}

	@NotNull
	public Set<String> getClassesOf(@NotNull final String inst)
	{
		// (instance ?INST class)
		return askWithRestriction(0, "instance", 1, inst) //
				.stream() //
				.map(f -> f.getArgument(2)) //
				.collect(Collectors.toSet());
	}

	// S U P E R C L A S S E S   /   S U B C L A S S E S

	/**
	 * Retrieves the direct subclasses of this Class name.
	 * The input Class is not included in the result set.
	 *
	 * @param className A String containing a SUO-KIF class name
	 * @return A Set of SUO-KIF class names, which could be empty.
	 */
	@NotNull
	public Collection<String> getDirectSubClassesOf(@NotNull final String className)
	{
		return askWithRestriction(0, "subclass", 2, className) //
				.stream() //
				.map(f -> f.getArgument(1)) //
				.collect(Collectors.toSet());
	}

	/**
	 * Retrieves the direct super classes of this Class name.
	 * The input Class is not included in the result set.
	 *
	 * @param className A String containing a SUO-KIF class name
	 * @return A Set of SUO-KIF class names, which could be empty.
	 */
	@NotNull
	public Collection<String> getDirectSuperClassesOf(@NotNull final String className)
	{
		return askWithRestriction(0, "subclass", 1, className) //
				.stream() //
				.map(f -> f.getArgument(2)) //
				.collect(Collectors.toSet());
	}

	/**
	 * Retrieves the downward transitive closure of this Class name.
	 * The input class name is not included in the result set.
	 *
	 * @param className A SUO-KIF class name (String).
	 * @return A Set of SUO-KIF class names, which could be empty.
	 */
	@NotNull
	public Collection<String> getAllSubClassesOf(@Nullable final String className)
	{
		return getTransitiveClosure("subclass", 2, className, 1, true);
	}

	/**
	 * Retrieves the downward transitive closure of all Class names
	 * contained in the input set.  The members of the input set are
	 * not included in the result set.
	 *
	 * @param classNames A Set object containing SUO-KIF class names (Strings).
	 * @return A Set of SUO-KIF class names, which could be empty.
	 */
	@NotNull
	public Set<String> getAllSubClassesOf(@Nullable final Set<String> classNames)
	{
		if (classNames != null && !classNames.isEmpty())
		{
			return classNames.stream().flatMap(c -> getAllSubClassesOf(c).stream()).collect(Collectors.toSet());
		}
		return Collections.emptySet();
	}

	/**
	 * Retrieves the upward transitive closure of this Class name.
	 * The input Class is not included in the result set.
	 *
	 * @param className A String containing a SUO-KIF class name
	 * @return A Set of SUO-KIF class names, which could be empty.
	 */
	@NotNull
	public Collection<String> getAllSuperClassesOf(@NotNull final String className)
	{
		return getTransitiveClosure("subclass", 1, className, 2, true);
	}

	/**
	 * This method retrieves the upward transitive closure of all Class
	 * names contained in the input set.  The members of the input set are
	 * not included in the result set.
	 *
	 * @param classNames A Set object containing SUO-KIF class names
	 *                   (Strings).
	 * @return A Set of SUO-KIF class names, which could be empty.
	 */
	@NotNull
	public Collection<String> getAllSuperClassesOf(@Nullable final Set<String> classNames)
	{
		if (classNames != null && !classNames.isEmpty())
		{
			return classNames.stream().flatMap(c -> getAllSuperClassesOf(c).stream()).collect(Collectors.toSet());
		}
		return Collections.emptySet();
	}

	// P A T T E R N S

	/**
	 * A Map for holding compiled regular expression patterns.
	 * The map is initialized by calling compilePatterns().
	 */
	@Nullable
	private static Map<String, Tuple.Pair<Pattern, Integer>> REGEX_PATTERNS = null;

	/**
	 * This method returns a compiled regular expression Pattern
	 * object indexed by key.
	 *
	 * @param key A String that is the retrieval key for a compiled
	 *            regular expression Pattern.
	 * @return A compiled regular expression Pattern instance.
	 */
	@Nullable
	public static Pattern getCompiledPattern(@NotNull final String key)
	{
		if (!key.isEmpty() && REGEX_PATTERNS != null)
		{
			Tuple.Pair<Pattern, Integer> val = REGEX_PATTERNS.get(key);
			if (val != null)
			{
				return val.first;
			}
		}
		return null;
	}

	/**
	 * This method returns the int value that identifies the regular
	 * expression binding group to be returned when there is a match.
	 *
	 * @param key A String that is the retrieval key for the binding
	 *            group index associated with a compiled regular expression
	 *            Pattern.
	 * @return An int that indexes a binding group.
	 */
	public static int getPatternGroupIndex(@NotNull String key)
	{
		if (!key.isEmpty() && (REGEX_PATTERNS != null))
		{
			Tuple.Pair<Pattern, Integer> val = REGEX_PATTERNS.get(key);
			if (val != null)
			{
				return val.second;
			}
		}
		return -1;
	}

	/**
	 * This method compiles and stores regular expression Pattern
	 * objects and binding group indexes as two cell List
	 * objects.  Each List is indexed by a String retrieval key.
	 */
	private static void compilePatterns()
	{
		if (REGEX_PATTERNS == null)
		{
			REGEX_PATTERNS = new HashMap<>();
			@NotNull String[][] data = { //
					{"row_var", "\\@ROW\\d*", "0"}, //
					{"open_lit", "\\(\\w+\\s+\\?\\w+[a-zA-Z_0-9-?\\s]+\\)", "0"},  //
					{"pred_var_1", "\\(holds\\s+(\\?\\w+)\\W", "1"},  //
					{"pred_var_2", "\\((\\?\\w+)\\W", "1"}, //
					{"var_with_digit_suffix", "(\\D+)\\d*", "1"}};
			for (String[] entry : data)
			{
				String patternKey = entry[0];
				@NotNull Pattern p = Pattern.compile(entry[1]);
				int groupN = Integer.parseInt(entry[2]);
				@NotNull Tuple.Pair<Pattern, Integer> val = new Tuple.Pair<>();
				val.first = p;
				val.second = groupN;
				REGEX_PATTERNS.put(patternKey, val);
			}
		}
	}

	/**
	 * This method finds regular expression matches in an input string
	 * using a compiled Pattern and binding group index retrieved with
	 * patternKey.  If the List accumulator is provided, match
	 * results are added to it, and it is returned.  If accumulator is
	 * not provided (is null), then a new ArrayList is created and
	 * returned if matches are found.
	 *
	 * @param input      The input String in which matches are sought.
	 * @param patternKey A String used as the retrieval key for a
	 *                   regular expression Pattern object, and an int index identifying
	 *                   a binding group.
	 * @param result2    An optional List to which matches are
	 *                   added.  Note that if accumulator is provided, it will be the
	 *                   return value even if no new matches are found in the input
	 *                   String.
	 * @return A List, or null if no matches are found and an
	 * accumulator is not provided.
	 */
	@Nullable
	public static List<String> getMatches(@NotNull String input, @NotNull String patternKey, @Nullable List<String> result2)
	{
		@Nullable List<String> result = null;
		if (result2 != null)
		{
			result = result2;
		}
		if (REGEX_PATTERNS == null)
		{
			BaseKB.compilePatterns();
		}
		if (!input.isEmpty() && !patternKey.isEmpty())
		{
			@Nullable Pattern p = BaseKB.getCompiledPattern(patternKey);
			if (p != null)
			{
				@NotNull Matcher m = p.matcher(input);
				int gIdx = BaseKB.getPatternGroupIndex(patternKey);
				if (gIdx >= 0)
				{
					while (m.find())
					{
						String group = m.group(gIdx);
						if (!group.isEmpty())
						{
							if (result == null)
							{
								result = new ArrayList<>();
							}
							if (!result.contains(group))
							{
								result.add(group);
							}
						}
					}
				}
			}
		}
		return result;
	}

	/**
	 * This method finds regular expression matches in an input string
	 * using a compiled Pattern and binding group index retrieved with
	 * patternKey, and returns the results, if any, in a List.
	 *
	 * @param input      The input String in which matches are sought.
	 * @param patternKey A String used as the retrieval key for a
	 *                   regular expression Pattern object, and an int index identifying
	 *                   a binding group.
	 * @return A List, or null if no matches are found.
	 */
	@Nullable
	public static List<String> getMatches(@NotNull String input, @NotNull String patternKey)
	{
		return BaseKB.getMatches(input, patternKey, null);
	}

	// N E A R E S T

	/**
	 * Get the alphabetically nearest terms to the given term, which
	 * is not in the KB.  Elements 0-(k-1) should be alphabetically
	 * lesser and k-(2*k-1) alphabetically greater.  If the term is
	 * at the beginning or end of the alphabet, fill in blank items
	 * with the empty string: "".
	 *
	 * @param term target term
	 * @param k    expected range after and before term
	 * @return alphabetically the nearest terms to the given term, which is not in the KB.
	 */
	@NotNull
	private List<String> getNearestKTerms(@NotNull final String term, @SuppressWarnings("SameParameterValue") int k)
	{
		@NotNull List<String> result = k == 0 ? listWithBlanks(1) : listWithBlanks(2 * k);

		// terms is a sorted set
		@NotNull final String[] t = terms.toArray(new String[0]);
		final int n = t.length;

		// i = position of term or first nearest after term
		int i = 0;
		while (i < n - 1 && t[i].compareTo(term) < 0)
		{
			i++;
		}
		// if one value expected
		if (k == 0)
		{
			result.set(0, t[i]);
			return result;
		}
		// k values expected before
		int lower = i;
		while (i - lower < k && lower > 0)
		{
			lower--;
			result.set(k - (i - lower), t[lower]);
		}
		// k values expected after
		int upper = i - 1;
		while (upper - i < k - 1 && upper < n - 1)
		{
			upper++;
			result.set(k + upper - i, t[upper]);
		}
		return result;
	}

	/**
	 * Get the alphabetically nearest terms to the given term, which
	 * is not in the KB.  Elements 0-14 should be alphabetically lesser and
	 * 15-29 alphabetically greater.  If the term is at the beginning or end
	 * of the alphabet, fill in blank items with the empty string: "".
	 */
	@NotNull
	private List<String> getNearestTerms(@NotNull final String term)
	{
		return getNearestKTerms(term, 15);
	}

	/**
	 * Get the neighbors of this initial uppercase term (class or function).
	 *
	 * @param term term
	 * @return nearest relations
	 */
	@NotNull
	public List<String> getNearestRelations(@NotNull final String term)
	{
		@NotNull String term2 = Character.toUpperCase(term.charAt(0)) + term.substring(1);
		return getNearestTerms(term2);
	}

	/**
	 * Get the neighbors of this initial lowercase term (relation).
	 *
	 * @param term term
	 * @return nearest non relations
	 */
	@NotNull
	public List<String> getNearestNonRelations(@NotNull final String term)
	{
		@NotNull String term2 = Character.toLowerCase(term.charAt(0)) + term.substring(1);
		return getNearestTerms(term2);
	}

	// C O N V E R T

	/**
	 * Converts all Formula objects in the input List to List tuples of elements.
	 *
	 * @param formulas A list of Formulas.
	 * @return A List of formula tuples (Lists), or an empty List.
	 */
	@NotNull
	public static Collection<List<String>> formulasToLists(@Nullable Collection<Formula> formulas)
	{
		if (formulas != null)
		{
			return formulas.stream().map(Formula::elements).collect(toList());
		}
		return Collections.emptyList();
	}

	/**
	 * Converts all Strings in the input List to Formula objects.
	 *
	 * @param forms A collection of forms.
	 * @return A List of Formulas, or an empty List.
	 */
	@NotNull
	public static Collection<Formula> formsToFormulas(@Nullable final Collection<String> forms)
	{
		if (forms != null)
		{
			return forms.stream().map(Formula::of).collect(toList());
		}
		return Collections.emptyList();
	}

	/**
	 * Converts a literal (List object) to a Formula.
	 *
	 * @param lits literal
	 * @return A SUO-KIF Formula object, or null if no Formula can be
	 * created.
	 */
	@Nullable
	public static Formula literalListToFormula(@Nullable final List<String> lits)
	{
		@NotNull String form = StringUtil.makeForm(lits);
		if (!form.isEmpty())
		{
			return Formula.of(form);
		}
		return null;
	}

	// F O R M A T   M A P S

	/**
	 * This method creates a dictionary (Map) of SUO-KIF term symbols
	 * -- the keys -- and a natural language string for each key that
	 * is the preferred name for the term -- the values -- in the
	 * context denoted by lang.  If the Map has already been built and
	 * the language hasn't changed, just return the existing map.
	 * This is a case of "lazy evaluation".
	 *
	 * @param lang0 language
	 * @return An instance of Map where the keys are terms and the
	 * values are format strings.
	 */
	public Map<String, String> getTermFormatMap(@Nullable String lang0)
	{
		LOGGER.entering(LOG_SOURCE, "getTermFormatMap", "lang = " + lang0);
		@Nullable String lang = lang0;
		if (lang == null || lang.isEmpty())
		{
			lang = "EnglishLanguage";
		}
		if (termFormatMap.isEmpty())
		{
			loadFormatMaps(lang);
		}
		LOGGER.exiting(LOG_SOURCE, "getTermFormatMap", formatMap.get(lang));
		return termFormatMap.get(lang);
	}

	/**
	 * This method creates an association list (Map) of the natural
	 * language format string and the relation name for which that
	 * format string applies.  If the map has already been built and
	 * the language hasn't changed, just return the existing map.
	 * This is a case of "lazy evaluation".
	 *
	 * @param lang0 language
	 * @return An instance of Map where the keys are relation names
	 * and the values are format strings.
	 */
	public Map<String, String> getFormatMap(@Nullable final String lang0)
	{
		LOGGER.entering(LOG_SOURCE, "getFormatMap", "lang = " + lang0);
		@Nullable String lang = lang0;
		if (lang == null || lang.isEmpty())
		{
			lang = "EnglishLanguage";
		}
		if (formatMap.isEmpty())
		{
			loadFormatMaps(lang);
		}
		LOGGER.exiting(LOG_SOURCE, "getFormatMap", formatMap.get(lang));
		return formatMap.get(lang);
	}

	/**
	 * Populates the format maps for language lang.
	 *
	 * @param lang language
	 */
	protected void loadFormatMaps(@NotNull final String lang)
	{
		if (!formatMap.containsKey(lang))
		{
			// (format EnglishLanguage entails "%1 %n{doesn't} &%entail%p{s} %2")
			@NotNull Collection<Formula> formulas = askWithRestriction(0, "format", 1, lang);
			if (formulas.isEmpty())
			{
				LOGGER.warning("No relation format file loaded for language " + lang);
				return;
			}

			@NotNull Map<String, String> m = formatMap.computeIfAbsent(lang, k -> new HashMap<>());
			formulas.forEach(f -> {
				@NotNull String key = f.getArgument(2);
				@NotNull String format = f.getArgument(3);
				format = StringUtil.removeEnclosingQuotes(format);
				m.put(key, format);
			});
		}

		if (!termFormatMap.containsKey(lang))
		{
			//(termFormat EnglishLanguage Entity "entity")
			@NotNull Collection<Formula> formulas = askWithRestriction(0, "termFormat", 1, lang);
			if (formulas.isEmpty())
			{
				LOGGER.warning("No term format file loaded for language: " + lang);
				return;
			}
			@NotNull Map<String, String> m = termFormatMap.computeIfAbsent(lang, k -> new HashMap<>());
			formulas.forEach(f -> {
				@NotNull String key = f.getArgument(2);
				@NotNull String format = f.getArgument(3);
				format = StringUtil.removeEnclosingQuotes(format);
				m.put(key, format);
			});
		}
	}

	/**
	 * Clears all loaded format and termFormat maps, for all languages.
	 */
	protected void clearFormatMaps()
	{
		formatMap.values().forEach(m -> {
			if (m != null)
			{
				m.clear();
			}
		});
		formatMap.clear();

		termFormatMap.values().forEach(m -> {
			if (m != null)
			{
				m.clear();
			}
		});
		termFormatMap.clear();
	}

	// H E L P E R S

	/**
	 * Create a List of the specific size, filled with empty strings.
	 *
	 * @param size size of list
	 * @return list of empty strings.
	 */
	@NotNull
	protected static List<String> listWithBlanks(int size)
	{
		@NotNull String[] array = new String[size];
		Arrays.fill(array, "");
		return Arrays.asList(array);
	}

	// I N S T A N T I A T E

	/**
	 * A global counter used to ensure that constants created by instantiateFormula() are unique.
	 */
	private int genSym = 0;

	public final Supplier<Integer> uniqueId = () -> genSym++;

	// P R I N T

	/**
	 * Pretty print
	 *
	 * @param term0 term
	 * @return pretty-printed term
	 */
	@NotNull
	public static String prettyPrint(@NotNull final String term0)
	{
		String term = term0;
		if (term.endsWith("Fn"))
		{
			term = term.substring(0, term.length() - 2);
		}

		@NotNull StringBuilder result = new StringBuilder();
		for (int i = 0; i < term.length(); i++)
		{
			char c = term.charAt(i);
			if (Character.isLowerCase(c) || !Character.isLetter(c))
			{
				result.append(c);
			}
			else
			{
				if (i + 1 < term.length() && Character.isUpperCase(term.charAt(i + 1)))
				{
					result.append(c);
				}
				else
				{
					if (i != 0)
					{
						result.append(" ");
					}
					result.append(Character.toLowerCase(c));
				}
			}
		}
		return result.toString();
	}

	// P R O L O G

	/**
	 * Write Prolog formula
	 */
	private void writePrologFormulas(@NotNull final Collection<Formula> formulas, @NotNull final PrintWriter pr)
	{
		formulas.stream().sorted().map(Formula::toProlog).filter(p -> !p.isEmpty()).forEach(pr::println);
	}

	/**
	 * Write Prolog
	 *
	 * @param ps - print stream
	 */
	public void writeProlog(@NotNull final PrintStream ps)
	{
		try (@NotNull PrintWriter pr = new PrintWriter(ps))
		{
			LOGGER.finer("Writing Prolog");

			pr.println("% Copyright (c) 2006-2009 Articulate Software Incorporated");
			pr.println("% This software released under the GNU Public License <https://www.gnu.org/copyleft/gpl.html>.");
			pr.println("% This is a very lossy translation to prolog of the KIF ontologies available at www.ontologyportal.org\n");

			pr.println("% subAttribute");
			writePrologFormulas(ask(AskKind.ARG, 0, "subAttribute"), pr);
			pr.println("\n% subrelation");
			writePrologFormulas(ask(AskKind.ARG, 0, "subrelation"), pr);
			pr.println("\n% disjoint");
			writePrologFormulas(ask(AskKind.ARG, 0, "disjoint"), pr);
			pr.println("\n% partition");
			writePrologFormulas(ask(AskKind.ARG, 0, "partition"), pr);
			pr.println("\n% instance");
			writePrologFormulas(ask(AskKind.ARG, 0, "instance"), pr);
			pr.println("\n% subclass");
			writePrologFormulas(ask(AskKind.ARG, 0, "subclass"), pr);
			pr.flush();
		}
		catch (Exception e)
		{
			LOGGER.warning(e.getMessage());
			e.printStackTrace();
		}
	}
}
