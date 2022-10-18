/* This code is copyright Articulate Software (c) 2003.  Some
portions copyright Teknowledge (c) 2003 and reused under the terms of the GNU
license.  This software is released under the GNU Public License
<http://www.gnu.org/copyleft/gpl.html>.  Users of this code also consent,
by use of this code, to credit Articulate Software and Teknowledge in any
writings, briefings, publications, presentations, or other representations
of any software which incorporates, builds on, or uses this code.  Please
cite the following article in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment, in Working
Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico. see also
http://sigmakee.sourceforge.net
*/

package com.articulate.sigma;

import com.articulate.sigma.kif.KIF;

import java.io.File;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.text.ParseException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static java.util.stream.Collectors.toList;

/**
 * Contains methods for reading, writing knowledge bases and their
 * configurations.
 */
public class BaseKB implements KBIface, Serializable
{
	private static final long serialVersionUID = 2L;

	private static final String LOG_SOURCE = "BaseKB";

	private static final Logger logger = Logger.getLogger(BaseKB.class.getName());

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
	public final Set<String> errors = new TreeSet<>();

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
		kbDir = KBSettings.getPref("kbDir");
	}

	// L O A D

	/**
	 * Add a new KB constituent by reading in the file, and then
	 * merging the formulas with the existing set of formulas.  All
	 * assertion caches are rebuilt, the current Vampire process is
	 * destroyed, and a new one is created.
	 *
	 * @param filename - the full path of the file being added.
	 */
	public void addConstituent(@NotNull String filename)
	{
		addConstituent(filename, null, null);
	}

	/**
	 * Add a new KB constituent by reading in the file, and then merging
	 * the formulas with the existing set of formulas.
	 *
	 * @param filename     - The full path of the file being added
	 * @param postAdd      - Post adding constituent, passed the canonical path
	 * @param arityChecker - Arity checker function
	 */
	public void addConstituent(@NotNull String filename, @Nullable final Consumer<String> postAdd, @Nullable final Function<Formula, Boolean> arityChecker)
	{
		logger.entering(LOG_SOURCE, "addConstituent", "Constituent = " + filename);
		try
		{
			@NotNull String filePath = new File(filename).getCanonicalPath();
			if (constituents.contains(filePath))
			{
				errors.add("Error: " + filePath + " already loaded.");
			}
			logger.finer("Adding " + filePath + " to KB.");

			// read KIF file
			@NotNull KIF file = new KIF();
			try
			{
				file.readFile(filePath);
				errors.addAll(file.warnings);
			}
			catch (Exception ex)
			{
				@NotNull StringBuilder error = new StringBuilder();
				error.append(ex.getMessage());
				if (ex instanceof ParseException)
				{
					error.append(" at line ").append(((ParseException) ex).getErrorOffset());
				}
				error.append(" in file ").append(filePath);
				logger.severe(error.toString());
				errors.add(error.toString());
			}

			// inherit formulas
			logger.finer("Parsed file " + filePath + " containing " + file.formulas.keySet().size() + " KIF expressions");
			int count = 0;
			for (String key : file.formulas.keySet())
			{
				// Iterate through the formulas in the file, adding them to the KB, at the appropriate key.
				// Note that this is a slow operation that needs to be improved
				for (@NotNull Formula f : file.formulas.get(key))
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
						boolean ok = false;
						synchronized (this)
						{
							@NotNull Collection<Formula> indexed = formulaIndex.computeIfAbsent(key, k -> new ArrayList<>());
							if (!indexed.contains(f))
							{
								// accept formula
								indexed.add(f);
								formulas.put(f.form, f);
								ok = true;
							}
						}
						if (!ok)
						{
							Formula existingFormula = formulas.get(f.form);
							String error = //
									"WARNING: Duplicate axiom in " + f.sourceFile + " at line " + f.startLine + "\n" + f.form + "\n" + //
											"WARNING: Existing formula appears in " + existingFormula.sourceFile + " at line " + existingFormula.startLine + "\n" + "\n";
							errors.add(error);
							System.err.println("WARNING: Duplicate detected.");
						}
					}
				}

				// progress
				if (count++ % 100 == 1)
				{
					System.out.print(".");
				}
			}

			// inherit terms
			synchronized (this)
			{
				terms.addAll(file.terms);
			}

			// add as constituent
			if (!constituents.contains(filePath))
			{
				constituents.add(filePath);
			}
			logger.info("Added " + filePath + " to KB");

			// Clear the formatMap and termFormatMap for this KB.
			// clearFormatMaps();

			// Post adding constituent.
			if (postAdd != null)
			{
				postAdd.accept(filePath);
			}
		}
		catch (Exception e)
		{
			logger.severe(e.getMessage() + "; \nStack Trace: " + Arrays.toString(e.getStackTrace()));
		}
		logger.exiting(LOG_SOURCE, "addConstituent", "Constituent " + filename + " successfully added to KB: " + this.name);
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
	public int getCountTerms()
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
	 * Takes a Regular Expression and returns a List
	 * containing every term in the KB that has a match with the RE.
	 *
	 * @param regexp A String
	 * @return A List of terms that have a match to term
	 */
	@NotNull
	public Collection<String> findTermsMatching(@NotNull final String regexp)
	{
		try
		{
			@NotNull Pattern p = Pattern.compile(regexp);
			return terms.stream().filter(t -> p.matcher(t).matches()).collect(toList());
		}
		catch (PatternSyntaxException ex)
		{
			logger.warning(ex.getMessage());
			throw ex;
		}

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
		return terms.stream().filter(BaseKB::isReln).collect(toList());
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

	// T E S T S

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
			return t.startsWith("?") || t.startsWith("@");
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
	 * @return The long number of rules in the knowledge base.
	 */
	public long getFormulaCount(@NotNull final Predicate<Formula> predicate)
	{
		return formulas.values().stream().filter(predicate).count();
	}

	// A S K

	public static final String ASK_ARG = "arg";
	public static final String ASK_ANT = "ant";
	public static final String ASK_CONS = "cons";
	public static final String ASK_STMT = "stmt";

	/**
	 * Returns a List containing the Formulas that match the request.
	 * The formula index is used.
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
	public Collection<Formula> ask(@NotNull final String kind, final int pos, @Nullable final String arg)
	{
		// sanity check
		if (arg == null || arg.isEmpty())
		{
			@NotNull String errStr = "Error in BaseKB.ask(\"" + kind + "\", " + pos + ", \"" + arg + "\"): " + "search term is null, or an empty string";
			logger.warning(errStr);
			throw new IllegalArgumentException(errStr);
		}
		if (arg.length() > 1 && arg.charAt(0) == '"' && arg.charAt(arg.length() - 1) == '"')
		{
			@NotNull String errStr = "Error in BaseKB.ask(): Strings are not indexed.  No results for " + arg;
			logger.warning(errStr);
			throw new IllegalArgumentException(errStr);
		}

		// query formula index
		String key = ASK_ARG.equals(kind) ? //
				ASK_ARG + "-" + pos + "-" + arg : //
				kind + "-" + arg;
		Collection<Formula> result = formulaIndex.get(key);
		return result != null ? result : new ArrayList<>();
	}

	/**
	 * Ask with restriction
	 *
	 * @param pos1 position of arg 1
	 * @param arg1 arg 1 (term)
	 * @param pos2 position of arg 2
	 * @param arg2 arg 2 (term)
	 * @return a List of Formulas in which the two terms
	 * provided appear in the indicated argument positions.
	 * If there are no Formula(s) matching the given terms and respective
	 * argument positions, return an empty List.
	 * Iterate through the smallest list of results.
	 */
	@NotNull
	public Collection<Formula> askWithRestriction(final int pos1, @NotNull final String arg1, final int pos2, @NotNull final String arg2)
	{
		if (!arg1.isEmpty() && !arg2.isEmpty())
		{
			@NotNull Collection<Formula> result1 = ask(ASK_ARG, pos1, arg1);
			@NotNull Collection<Formula> result2 = ask(ASK_ARG, pos2, arg2);
			boolean firstBigger = result1.size() > result2.size();

			// scan the smaller (source) for target
			@NotNull final Collection<Formula> source = firstBigger ? result2 : result1;
			final int targetPos = firstBigger ? pos1 : pos2;
			@NotNull final String targetArg = firstBigger ? arg1 : arg2;

			// intersection : filter source for targetPos at targetNum position
			return source.stream().filter(f -> f.getArgument(targetPos).equals(targetArg)).distinct().collect(toList());
		}
		return new ArrayList<>();
	}

	/**
	 * Returns a List of Formulas in which the two terms
	 * provided appear in the indicated argument positions.  If there
	 * are no Formula(s) matching the given terms and respective
	 * argument positions, return an empty List.
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
	public Collection<Formula> askWithTwoRestrictions(final int pos1, @NotNull final String arg1, final int pos2, @NotNull final String arg2, final int pos3, @NotNull final String arg3)
	{
		if (!arg1.isEmpty() && !arg2.isEmpty() && !arg3.isEmpty())
		{
			@NotNull Collection<Formula> result1 = ask(ASK_ARG, pos1, arg1);
			int size1 = result1.size();
			@NotNull Collection<Formula> result2 = ask(ASK_ARG, pos2, arg2);
			int size2 = result2.size();
			@NotNull Collection<Formula> result3 = ask(ASK_ARG, pos3, arg3);
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
				// _targetPos2 = argpos1;
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
			final String targetArg1 = _targetArg1;
			final int targetPos2 = _targetPos2;
			final String targetArg2 = _targetArg2;
			return source.stream().filter(f -> f.getArgument(targetPos1).equals(targetArg1) && f.getArgument(targetPos2).equals(targetArg2)).distinct().collect(toList());
		}
		return new ArrayList<>();
	}

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
		@NotNull Collection<Formula> result = new HashSet<>();
		if (!reln0.isEmpty() && !arg.isEmpty() && pos >= 0 /* && (pos < 7) */)
		{
			@NotNull Set<String> visitedForms = new HashSet<>();
			@NotNull Set<String> subrelns = new HashSet<>();
			@NotNull List<String> relnToVisit = new ArrayList<>();
			relnToVisit.add(reln0);
			while (!relnToVisit.isEmpty())
			{
				for (@NotNull String reln : relnToVisit)
				{
					// collect
					// (reln ... arg ...)
					@NotNull Collection<Formula> subresult = askWithRestriction(0, reln, pos, arg);
					result.addAll(subresult);

					// compute subrelations to reln
					// (subrelation ? reln)
					for (@NotNull Formula f : askWithRestriction(0, "subrelation", 2, reln))
					{
						if (!visitedForms.contains(f.form))
						{
							@NotNull String subreln = f.getArgument(1);
							if (!reln.equals(subreln))
							{
								subrelns.add(subreln);
								visitedForms.add(f.form);
							}
						}
					}
				}
				relnToVisit.clear();
				relnToVisit.addAll(subrelns);
				subrelns.clear();
			}
		}
		return result;
	}

	/**
	 * This method retrieves Formulas by asking the query expression
	 * query, and returns the results, if any, in a List.
	 *
	 * @param query The query, which is assumed to be a List
	 *              (atomic literal) consisting of a single predicate and its
	 *              arguments.  The arguments could be variables, constants, or a
	 *              mix of the two, but only the first constant encountered in a
	 *              left to right sweep over the literal will be used in the actual
	 *              query.
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
				String argi = query.get(i);
				if (!argi.isEmpty() && !isVariable(argi))
				{
					arg = argi;
					pos = i;
					break;
				}
			}

			// ask
			return arg != null ? askWithRestriction(pos, arg, 0, pred) : ask(ASK_ARG, 0, pred);
		}
		return new ArrayList<>();
	}

	// F I N D

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
	public Collection<String> getTermsViaAsk(final int pos, final String arg, final int targetPos)
	{
		@NotNull Collection<Formula> formulas = ask(ASK_ARG, pos, arg);
		return formulas.stream().map(f -> f.getArgument(targetPos)).distinct().collect(toList());
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
	public List<String> getTermsViaAskWithRestriction(final int pos1, @NotNull final String arg1, final int pos2, @NotNull final String arg2, final int targetPos, @Nullable final Set<String> predicatesUsed)
	{
		if (!arg1.isEmpty() && !StringUtil.isQuotedString(arg1) && !arg2.isEmpty() && !StringUtil.isQuotedString(arg2))
		{
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
					.collect(toList());
		}
		return new ArrayList<>();
	}

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
	public Collection<String> getTermsViaAskWithRestriction(final int pos1, @NotNull final String arg1, final int pos2, @NotNull final String arg2, final int targetPos)
	{
		return getTermsViaAskWithRestriction(pos1, arg1, pos2, arg2, targetPos, null);
	}

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
	public Collection<String> getTermsViaAskWithTwoRestrictions(final int pos1, @NotNull final String arg1, final int pos2, @NotNull final String arg2, final int pos3, @NotNull final String arg3, final int targetPos)
	{
		@NotNull Collection<Formula> formulas = askWithTwoRestrictions(pos1, arg1, pos2, arg2, pos3, arg3);
		return formulas.stream().map(f -> f.getArgument(targetPos)).distinct().collect(toList());
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
	public String getFirstTermViaAskWithRestriction(final int pos1, @NotNull final String arg1, final int pos2, @NotNull final String arg2, final int targetPos)
	{
		@NotNull Collection<String> terms = getTermsViaAskWithRestriction(pos1, arg1, pos2, arg2, targetPos);
		if (!terms.isEmpty())
		{
			return terms.iterator().next();
		}
		return null;
	}

	/**
	 * Returns a List containing SUO-KIF constants, possibly
	 * retrieved via multiple asks that recursively use relation and
	 * all of its subrelations.
	 *
	 * @param reln           The name of a predicate, which is assumed to be
	 *                       the 0th argument of one or more atomic
	 *                       Formulae
	 * @param pos            The argument position occupied by arg in the
	 *                       ground atomic Formulae that will be retrieved
	 *                       to gather the target (answer) terms
	 * @param arg            A constant that occupies pos position in
	 *                       each of the ground atomic Formulae that will be
	 *                       retrieved to gather the target (answer) terms
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
	public Collection<String> getTermsViaPredicateSubsumption(@NotNull final String reln, final int pos, @NotNull String arg, final int targetPos, boolean useInverses, @Nullable final Set<String> predicatesUsed)
	{
		@NotNull Set<String> result = new HashSet<>();
		if (!reln.isEmpty() && !arg.isEmpty() && pos >= 0 /* && pos < 7 */)
		{
			@Nullable Set<String> inverseRelns = null;
			@Nullable Collection<String> inverses = null;
			if (useInverses)
			{
				inverseRelns = new HashSet<>();
				inverseRelns.addAll(getTermsViaAskWithRestriction(0, "subrelation", 2, "inverse", 1)); // (subrelation ? inverse)
				inverseRelns.addAll(getTermsViaAskWithRestriction(0, "equal", 2, "inverse", 1)); // (equal ? inverse)
				inverseRelns.addAll(getTermsViaAskWithRestriction(0, "equal", 1, "inverse", 2)); // (equal inverse ?)
				inverseRelns.add("inverse");
				inverses = new HashSet<>();
			}
			@NotNull Set<String> subrelations = new HashSet<>();
			@NotNull List<String> predicatesToVisit = new ArrayList<>();
			predicatesToVisit.add(reln);
			while (!predicatesToVisit.isEmpty())
			{
				for (@NotNull String predicate : predicatesToVisit)
				{
					// subresult
					result.addAll(getTermsViaAskWithRestriction(0, predicate, pos, arg, targetPos, predicatesUsed));

					// subrelations
					subrelations.addAll(getTermsViaAskWithRestriction(0, "subrelation", 2, predicate, 1));
					subrelations.addAll(getTermsViaAskWithRestriction(0, "equal", 2, "subrelation", 1));
					subrelations.addAll(getTermsViaAskWithRestriction(0, "equal", 1, "subrelation", 2));
					subrelations.remove(predicate);

					if (useInverses)
					{
						for (@NotNull String inverseReln : inverseRelns)
						{
							inverses.addAll(getTermsViaAskWithRestriction(0, inverseReln, 1, predicate, 2));
							inverses.addAll(getTermsViaAskWithRestriction(0, inverseReln, 2, predicate, 1));
						}
					}
				}

				predicatesToVisit.clear();
				predicatesToVisit.addAll(subrelations);
				subrelations.clear();
			}
			if (useInverses)
			{
				for (@NotNull String inverse : inverses)
				{
					result.addAll(getTermsViaPredicateSubsumption(inverse, targetPos, arg, pos, false, predicatesUsed));
				}
			}
		}
		return result;
	}

	/**
	 * Returns a List containing SUO-KIF constants, possibly
	 * retrieved via multiple asks that recursively use relation and
	 * all of its subrelations.
	 *
	 * @param reln        The name of a predicate, which is assumed to be
	 *                    the 0th argument of one or more atomic
	 *                    Formulae
	 * @param pos         The argument position occupied by term in the
	 *                    ground atomic Formulae that will be retrieved
	 *                    to gather the target (answer) terms
	 * @param arg         A constant that occupies pos position in
	 *                    each of the ground atomic Formulae that will be
	 *                    retrieved to gather the target (answer) terms
	 * @param targetPos   The argument position of the answer terms
	 *                    in the Formulae to be retrieved
	 * @param useInverses If true, the inverses of relation and its
	 *                    subrelations will be also be used to try to
	 *                    find answer terms
	 * @return a List of terms (SUO-KIF constants), or an
	 * empty List if no terms can be retrieved
	 */
	@NotNull
	public Collection<String> getTermsViaPredicateSubsumption(@NotNull final String reln, final int pos, @NotNull String arg, final int targetPos, final boolean useInverses)
	{
		return getTermsViaPredicateSubsumption(reln, pos, arg, targetPos, useInverses, null);
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
	public String getFirstTermViaPredicateSubsumption(@NotNull final String reln, final int pos, @NotNull final String arg, final int targetPos, final boolean useInverses)
	{
		@Nullable String result = null;
		if (!reln.isEmpty() && !arg.isEmpty() && pos >= 0 /* && pos < 7 */)
		{
			@NotNull Collection<String> terms = getTermsViaPredicateSubsumption(reln, pos, arg, targetPos, useInverses);
			if (!terms.isEmpty())
			{
				result = terms.iterator().next();
			}
		}
		return result;
	}

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
	public Collection<String> getTransitiveClosureViaPredicateSubsumption(@NotNull final String reln, final int pos, @NotNull final String arg, final int targetPos, boolean useInverses)
	{
		@NotNull Set<String> result = new TreeSet<>();
		// collect all ?x such that (reln ... arg@pos ... ?x@targetPos ...)
		// arg and ?x are related through reln
		@NotNull Collection<String> termsToVisit = getTermsViaPredicateSubsumption(reln, pos, arg, targetPos, useInverses);
		while (!termsToVisit.isEmpty())
		{
			result.addAll(termsToVisit);

			// transitively
			@NotNull List<String> working = new ArrayList<>(termsToVisit);
			termsToVisit.clear();
			for (@NotNull String arg2 : working)
			{
				// collect all ?y such that (reln ... ?x@pos ... ?y@targetPos ...)
				termsToVisit.addAll(getTermsViaPredicateSubsumption(reln, pos, arg2, targetPos, useInverses));
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
	public Collection<Formula> instanceFormulasOf(@NotNull final String inst)
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
	public boolean isInstance(@NotNull final String term)
	{
		// (instance term ?CLASS)
		@NotNull Collection<Formula> formulas = instanceFormulasOf(term);
		return formulas.size() > 0;
	}

	// S U P E R C L A S S E S   /   S U B C L A S S E S

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
	public Set<String> getAllSuperClasses(@Nullable final Set<String> classNames)
	{
		@NotNull Set<String> result = new HashSet<>();
		if (classNames != null && !classNames.isEmpty())
		{
			@NotNull List<String> superclasses = new ArrayList<>();
			@NotNull List<String> classesToVisit = new ArrayList<>(classNames);
			while (!classesToVisit.isEmpty())
			{
				for (String className : classesToVisit)
				{
					// collect super classes
					// (subclass class ?)
					for (@NotNull Formula f : askWithRestriction(0, "subclass", 1, className))
					{
						@NotNull String superclass = f.getArgument(2);
						if (!classesToVisit.contains(superclass))
						{
							superclasses.add(superclass);
						}
					}
				}

				result.addAll(superclasses);
				classesToVisit.clear();
				classesToVisit.addAll(superclasses);
				superclasses.clear();
			}
		}
		return result;
	}

	/**
	 * This method retrieves the upward transitive closure of this Class
	 * names. The input Class is not included in the result set.
	 *
	 * @param className A String containing a SUO-KIF class name
	 * @return A Set of SUO-KIF class names, which could be empty.
	 */
	@NotNull
	public Set<String> getAllSuperClasses(@NotNull final String className)
	{
		return getAllSuperClasses(Set.of(className));
	}

	/**
	 * This method retrieves the downward transitive closure of all Class
	 * names contained in the input set.  The members of the input set are
	 * not included in the result set.
	 *
	 * @param classNames A Set object containing SUO-KIF class names (Strings).
	 * @return A Set of SUO-KIF class names, which could be empty.
	 */
	@NotNull
	private Set<String> getAllSubClasses(@Nullable final Set<String> classNames)
	{
		@NotNull Set<String> result = new HashSet<>();
		if (classNames != null && !classNames.isEmpty())
		{
			@NotNull List<String> subclasses = new ArrayList<>();
			@NotNull List<String> classesToVisit = new ArrayList<>(classNames);
			while (!classesToVisit.isEmpty())
			{
				for (String className : classesToVisit)
				{
					// collect sub classes
					// (subclass ? class)
					for (@NotNull Formula f : askWithRestriction(0, "subclass", 2, className))
					{
						@NotNull String subclass = f.getArgument(1);
						if (!classesToVisit.contains(subclass))
						{
							subclasses.add(subclass);
						}
					}
				}

				result.addAll(subclasses);
				classesToVisit.clear();
				classesToVisit.addAll(subclasses);
				subclasses.clear();
			}
		}
		return result;
	}

	/**
	 * This method retrieves the downward transitive closure of this Class
	 * names. The input Class is not included in the result set.
	 *
	 * @param className A String containing a SUO-KIF class name
	 * @return A Set of SUO-KIF class names, which could be empty.
	 */
	@NotNull
	public Set<String> getAllSubClasses(@NotNull final String className)
	{
		return getAllSubClasses(Set.of(className));
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
		List<String> result = k == 0 ? listWithBlanks(1) : listWithBlanks(2 * k);

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
		String term2 = Character.toUpperCase(term.charAt(0)) + term.substring(1);
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
		String term2 = Character.toLowerCase(term.charAt(0)) + term.substring(1);
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
		return new ArrayList<>();
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
		return new ArrayList<>();
	}

	/**
	 * Converts a literal (List object) to a String.
	 *
	 * @param lits A List representing a SUO-KIF formula.
	 * @return A String representing a SUO-KIF formula.
	 */
	@NotNull
	public static String literalListToString(@Nullable List<String> lits)
	{
		if (lits != null)
		{
			return Formula.LP + String.join(" ", lits) + Formula.RP;
		}
		return "";
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
		@NotNull String form = literalListToString(lits);
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
		logger.entering(LOG_SOURCE, "getTermFormatMap", "lang = " + lang0);
		String lang = lang0;
		if (lang == null || lang.isEmpty())
		{
			lang = "EnglishLanguage";
		}
		if (termFormatMap.isEmpty())
		{
			loadFormatMaps(lang);
		}
		logger.exiting(LOG_SOURCE, "getTermFormatMap", formatMap.get(lang));
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
		logger.entering(LOG_SOURCE, "getFormatMap", "lang = " + lang0);
		String lang = lang0;
		if (lang == null || lang.isEmpty())
		{
			lang = "EnglishLanguage";
		}
		if (formatMap.isEmpty())
		{
			loadFormatMaps(lang);
		}
		logger.exiting(LOG_SOURCE, "getFormatMap", formatMap.get(lang));
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
				logger.warning("No relation format file loaded for language " + lang);
				return;
			}

			Map<String, String> m = formatMap.computeIfAbsent(lang, k -> new HashMap<>());
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
				logger.warning("No term format file loaded for language: " + lang);
				return;
			}
			Map<String, String> m = termFormatMap.computeIfAbsent(lang, k -> new HashMap<>());
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
	 * @return list of empty strings.
	 */
	@NotNull
	protected static List<String> listWithBlanks(int size)
	{
		String[] array = new String[size];
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
	 * @param term term
	 * @return pretty-printed term
	 */
	@NotNull
	public static String prettyPrint(@NotNull String term)
	{
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
	private void writePrologFormulas(@NotNull Collection<Formula> formulas, @NotNull PrintWriter pr)
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
			logger.finer("Writing Prolog");

			pr.println("% Copyright (c) 2006-2009 Articulate Software Incorporated");
			pr.println("% This software released under the GNU Public License <https://www.gnu.org/copyleft/gpl.html>.");
			pr.println("% This is a very lossy translation to prolog of the KIF ontologies available at www.ontologyportal.org\n");

			pr.println("% subAttribute");
			writePrologFormulas(ask(ASK_ARG, 0, "subAttribute"), pr);
			pr.println("\n% subrelation");
			writePrologFormulas(ask(ASK_ARG, 0, "subrelation"), pr);
			pr.println("\n% disjoint");
			writePrologFormulas(ask(ASK_ARG, 0, "disjoint"), pr);
			pr.println("\n% partition");
			writePrologFormulas(ask(ASK_ARG, 0, "partition"), pr);
			pr.println("\n% instance");
			writePrologFormulas(ask(ASK_ARG, 0, "instance"), pr);
			pr.println("\n% subclass");
			writePrologFormulas(ask(ASK_ARG, 0, "subclass"), pr);
			pr.flush();
		}
		catch (Exception e)
		{
			logger.warning(e.getMessage());
			e.printStackTrace();
		}
	}
}
