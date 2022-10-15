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
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Contains methods for reading, writing knowledge bases and their
 * configurations.
 */
public class BaseKB implements KBIface, Serializable
{
	private static final long serialVersionUID = 1L;

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
	public final List<String> constituents = new ArrayList<>();

	// core data

	/**
	 * A synchronized SortedSet of Strings, which are all the terms in the KB.
	 */
	public final Set<String> terms = Collections.synchronizedSortedSet(new TreeSet<>());

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
		if (logger.isLoggable(Level.FINER))
		{
			@NotNull String[] params = {"filename = " + filename};
			logger.entering(LOG_SOURCE, "addConstituent", params);
		}
		try
		{
			@NotNull String filePath = new File(filename).getCanonicalPath();
			if (constituents.contains(filePath))
			{
				errors.add("Error: " + filePath + " already loaded.");
			}
			logger.finer("Adding " + filePath + " to KB.");

			// file
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
				@NotNull Collection<Formula> fs = formulaIndex.computeIfAbsent(key, k -> new ArrayList<>());
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
						if (!fs.contains(f))
						{
							// accept formula
							fs.add(f);
							formulas.put(f.form, f);
						}
						else
						{
							Formula existingFormula = formulas.get(f.form);
							String error = "WARNING: Duplicate axiom in " + f.sourceFile + " at line " + f.startLine + "\n" + //
									f.form + "\n" + //
									"WARNING: Existing formula appears in " + existingFormula.sourceFile + " at line " + existingFormula.startLine + "\n" + //
									"\n";
							System.err.println("WARNING: Duplicate detected.");
							errors.add(error);
						}
					}
				}

				// progress
				if ((count++ % 100) == 1)
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
		catch (Exception ex)
		{
			logger.severe(ex.getMessage() + "; \nStack Trace: " + Arrays.toString(ex.getStackTrace()));
		}
		logger.exiting(LOG_SOURCE, "addConstituent", "Constituent " + filename + "successfully added to KB: " + this.name);
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
		try
		{
			if (!formatMap.containsKey(lang))
			{
				// (format EnglishLanguage entails "%1 %n{doesn't} &%entail%p{s} %2")
				@NotNull Collection<Formula> formulas = askWithRestriction(0, "format", 1, lang);
				if (formulas.isEmpty())
				{
					logger.warning("No relation format file loaded for language " + lang);
				}
				else
				{
					Map<String, String> m = formatMap.computeIfAbsent(lang, k -> new HashMap<>());
					for (@NotNull Formula f : formulas)
					{
						@NotNull String key = f.getArgument(2);
						@NotNull String format = f.getArgument(3);
						format = StringUtil.removeEnclosingQuotes(format);
						m.put(key, format);
					}
				}
			}

			if (!termFormatMap.containsKey(lang))
			{
				//(termFormat EnglishLanguage Entity "entity")
				@NotNull Collection<Formula>formulas = askWithRestriction(0, "termFormat", 1, lang);
				if (formulas.isEmpty())
				{
					logger.warning("No term format file loaded for language: " + lang);
				}
				else
				{
					Map<String, String> m = termFormatMap.computeIfAbsent(lang, k -> new HashMap<>());
					for (@NotNull Formula f : formulas)
					{
						@NotNull String key = f.getArgument(2);
						@NotNull String format = f.getArgument(3);
						format = StringUtil.removeEnclosingQuotes(format);
						m.put(key, format);
					}
				}
			}
		}
		catch (Exception ex)
		{
			logger.warning(Arrays.toString(ex.getStackTrace()));
			ex.printStackTrace();
		}
	}

	/**
	 * Clears all loaded format and termFormat maps, for all languages.
	 */
	protected void clearFormatMaps()
	{
		for (Map<String, String> m : formatMap.values())
		{
			if (m != null)
			{
				m.clear();
			}
		}
		formatMap.clear();

		for (Map<String, String> m : termFormatMap.values())
		{
			if (m != null)
			{
				m.clear();
			}
		}
		termFormatMap.clear();
	}

	// T E R M S

	/**
	 * Returns a synchronized SortedSet of Strings, which are all the terms in the KB.
	 *
	 * @return a synchronized sorted list of all the terms in the KB.
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
		return getTerms().size();
	}

	/**
	 * Takes a term and returns true if the term occurs in the KB.
	 *
	 * @param term A String.
	 * @return true or false.
	 */
	public boolean containsTerm(@NotNull final String term)
	{
		return getTerms().contains(term) || findTermsMatching(term).size() == 1;
	}

	/**
	 * Takes a Regular Expression and returns a List
	 * containing every term in the KB that has a match with the RE.
	 *
	 * @param regexp A String
	 * @return A List of terms that have a match to term
	 */
	@NotNull
	public List<String> findTermsMatching(@NotNull final String regexp)
	{
		try
		{
			@NotNull List<String> result = new ArrayList<>();
			@NotNull Pattern p = Pattern.compile(regexp);
			for (@NotNull String t : getTerms())
			{
				@NotNull Matcher m = p.matcher(t);
				if (m.matches())
				{
					result.add(t);
				}
			}
			return result;
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
	public static List<String> filterNonRelnTerms(@NotNull final List<String> terms)
	{
		@NotNull List<String> result = new ArrayList<>();
		for (@NotNull String t : terms)
		{
			if (Character.isUpperCase(t.charAt(0)))
			{
				result.add(t);
			}
		}
		return result;
	}

	/**
	 * Return List of all relnTerms in a List
	 *
	 * @param terms input list
	 * @return A List of relTerms
	 */
	@NotNull
	public static List<String> filterRelnTerms(@NotNull final List<String> terms)
	{
		@NotNull List<String> result = new ArrayList<>();
		for (@NotNull String t : terms)
		{
			if (Character.isLowerCase(t.charAt(0)))
			{
				result.add(t);
			}
		}
		return result;
	}

	// F O R M U L A S

	/**
	 * An accessor providing a SortedSet of un-preProcessed String
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
	 * Count the number of formulas in the knowledge base in order to
	 * present statistics to the user.
	 *
	 * @return The integer number of formulas in the knowledge base.
	 */
	public int getCountFormulas()
	{
		return formulas.size();
	}

	/**
	 * Count the number of rules in the knowledge base in order to
	 * present statistics to the user. Note that the number of rules
	 * is a subset of the number of formulas.
	 *
	 * @return The integer number of rules in the knowledge base.
	 */
	public int getCountRules()
	{
		int count = 0;
		for (@NotNull Formula f : formulas.values())
		{
			if (f.isRule())
			{
				count++;
			}
		}
		return count;
	}

	// A S K

	/**
	 * Returns a List containing the Formulas that match the request.
	 *
	 * @param kind   May be one of "ant", "cons", "stmt", or "arg"
	 * @param term   The term that appears in the statements being
	 *               requested.
	 * @param argnum The argument position of the term being asked
	 *               for.  The first argument after the predicate
	 *               is "1". This parameter is ignored if the kind
	 *               is "ant", "cons" or "stmt".
	 * @return A List of Formula(s), which will be empty if no match found.
	 */
	@NotNull
	public Collection<Formula> ask(@NotNull final String kind, final int argnum, @Nullable final String term)
	{
		if (term == null || term.isEmpty())
		{
			@NotNull String errStr = "Error in KB.ask(\"" + kind + "\", " + argnum + ", \"" + term + "\"): " + "search term is null, or an empty string";
			logger.warning(errStr);
			throw new IllegalArgumentException(errStr);
		}
		if (term.length() > 1 && term.charAt(0) == '"' && term.charAt(term.length() - 1) == '"')
		{
			@NotNull String errStr = "Error in KB.ask(): Strings are not indexed.  No results for " + term;
			logger.warning(errStr);
			throw new IllegalArgumentException(errStr);
		}

		Collection<Formula> result;
		if ("arg".equals(kind))
		{
			result = formulaIndex.get(kind + "-" + argnum + "-" + term);
		}
		else
		{
			result = formulaIndex.get(kind + "-" + term);
		}
		if (result != null)
		{
			return result;
		}
		return new ArrayList<>();
	}

	/**
	 * Ask with restriction
	 *
	 * @param argnum1 number of args 1
	 * @param term1   term 1
	 * @param argnum2 number of args 2
	 * @param term2   term 2
	 * @return a List of Formulas in which the two terms
	 * provided appear in the indicated argument positions.  If there
	 * are no Formula(s) matching the given terms and respective
	 * argument positions, return an empty List.  Iterate
	 * through the smallest list of results.
	 */
	@NotNull
	public Collection<Formula> askWithRestriction(int argnum1, @NotNull String term1, int argnum2, @NotNull String term2)
	{
		@NotNull List<Formula> result = new ArrayList<>();
		try
		{
			if (!term1.isEmpty() && !term2.isEmpty())
			{
				@NotNull Collection<Formula> partial1 = ask("arg", argnum1, term1);
				@NotNull Collection<Formula> partial2 = ask("arg", argnum2, term2);
				@NotNull Collection<Formula> partial = partial1;
				int arg = argnum2;
				@NotNull String term = term2;
				if (partial1.size() > partial2.size())
				{
					partial = partial2;
					arg = argnum1;
					term = term1;
				}
				for (@NotNull Formula f : partial)
				{
					if (f.getArgument(arg).equals(term))
					{
						result.add(f);
					}
				}
			}
		}
		catch (Exception ex)
		{
			logger.warning(Arrays.toString(ex.getStackTrace()));
			ex.printStackTrace();
		}
		return result;
	}

	/**
	 * Returns a List of Formulas in which the two terms
	 * provided appear in the indicated argument positions.  If there
	 * are no Formula(s) matching the given terms and respective
	 * argument positions, return an empty List.
	 *
	 * @param argnum1 number of args 1
	 * @param term1   term 1
	 * @param argnum2 number of args 2
	 * @param term2   term 2
	 * @param argnum3 number of args 3
	 * @param term3   term 3
	 * @return List of formulae.
	 */
	@NotNull
	public Collection<Formula> askWithTwoRestrictions(int argnum1, @NotNull String term1, int argnum2, @NotNull String term2, int argnum3, @NotNull String term3)
	{
		@NotNull String[] args = new String[6];
		args[0] = "argnum1 = " + argnum1;
		args[1] = "term1 = " + term1;
		args[2] = "argnum2 = " + argnum2;
		args[3] = "term2 = " + term2;
		args[4] = "argnum3 = " + argnum3;
		args[5] = "term3 = " + term3;

		logger.entering(LOG_SOURCE, "askWithTwoRestrictions", args);
		@NotNull List<Formula> result = new ArrayList<>();
		if (!term1.isEmpty() && !term2.isEmpty() && !term3.isEmpty())
		{
			@NotNull Collection<Formula> partialA = new ArrayList<>();           // will get the smallest list
			@NotNull Collection<Formula> partial1 = ask("arg", argnum1, term1);
			@NotNull Collection<Formula> partial2 = ask("arg", argnum2, term2);
			@NotNull Collection<Formula> partial3 = ask("arg", argnum3, term3);
			int argB = -1;
			@NotNull String termB = "";
			int argC = -1;
			@NotNull String termC = "";
			if (partial1.size() > partial2.size() && partial1.size() > partial3.size())
			{
				argC = argnum1;
				termC = term1;
				if (partial2.size() > partial3.size())
				{
					argB = argnum2;
					termB = term2;
					partialA = partial3;
				}
				else
				{
					argB = argnum3;
					termB = term3;
					partialA = partial2;
				}
			}
			if (partial2.size() > partial1.size() && partial2.size() > partial3.size())
			{
				argC = argnum2;
				termC = term2;
				if (partial1.size() > partial3.size())
				{
					argB = argnum1;
					termB = term1;
					partialA = partial3;
				}
				else
				{
					argB = argnum3;
					termB = term3;
					partialA = partial1;
				}
			}
			if (partial3.size() > partial1.size() && partial3.size() > partial2.size())
			{
				argC = argnum3;
				termC = term3;
				if (partial1.size() > partial2.size())
				{
					argB = argnum1;
					termB = term1;
					partialA = partial2;
				}
				else
				{
					argB = argnum2;
					termB = term2;
					partialA = partial1;
				}
			}
			for (@NotNull Formula f : partialA)
			{
				if (f.getArgument(argB).equals(termB))
				{
					if (f.getArgument(argC).equals(termC))
					{
						result.add(f);
					}
				}
			}
		}
		logger.exiting(LOG_SOURCE, "askWithTwoRestrictions", result);
		return result;
	}

	/**
	 * Returns a List containing the Formulae retrieved,
	 * possibly via multiple asks that recursively use relation and
	 * all of its subrelations.  Note that the Formulas might be
	 * formed with different predicates, but all the predicates
	 * will be subrelations of relation and will be related to each
	 * other in a subsumption hierarchy.
	 *
	 * @param relation  The name of a predicate, which is assumed to be
	 *                  the 0th argument of one or more atomic
	 *                  formulae
	 * @param idxArgnum The argument position occupied by idxTerm in
	 *                  each ground Formula to be retrieved
	 * @param idxTerm   A constant that occupied idxArgnum position in
	 *                  each ground Formula to be retrieved
	 * @return a List of Formulas that satisfy the query, or an
	 * empty List if no Formulae are retrieved.
	 */
	@NotNull
	public Collection<Formula> askWithPredicateSubsumption(@NotNull String relation, int idxArgnum, @NotNull String idxTerm)
	{
		@NotNull List<Formula> result = new ArrayList<>();
		if (!relation.isEmpty() && !idxTerm.isEmpty() && (idxArgnum >= 0) /* && (idxArgnum < 7) */)
		{
			@NotNull Set<String> done = new HashSet<>();
			@NotNull Set<String> accumulator = new HashSet<>();
			@NotNull List<String> relns = new ArrayList<>();
			relns.add(relation);
			while (!relns.isEmpty())
			{
				for (@NotNull String reln : relns)
				{
					@NotNull Collection<Formula> formulae = this.askWithRestriction(0, reln, idxArgnum, idxTerm);
					result.addAll(formulae);
					formulae = this.askWithRestriction(0, "subrelation", 2, reln);
					for (@NotNull Formula f : formulae)
					{
						if (!done.contains(f.form))
						{
							@NotNull String arg = f.getArgument(1);
							if (!reln.equals(arg))
							{
								accumulator.add(arg);
								done.add(f.form);
							}
						}
					}
				}
				relns.clear();
				relns.addAll(accumulator);
				accumulator.clear();
			}
			// Remove duplicates; perhaps not necessary.
			@NotNull Set<Formula> ans2 = new HashSet<>(result);
			result.clear();
			result.addAll(ans2);
		}
		return result;
	}

	// F I N D

	/**
	 * Returns a List containing the terms (Strings) that
	 * correspond to targetArgnum in the ground atomic Formulae in
	 * which knownArg is in the argument position knownArgnum.  The
	 * List returned will contain no duplicate terms.
	 *
	 * @param knownArgnum  The argument position of knownArg
	 * @param knownArg     The term that appears in the argument
	 *                     knownArgnum of the ground atomic Formulae in
	 *                     the KB
	 * @param targetArgnum The argument position of the terms being sought
	 * @return A List of Strings, which will be empty if no
	 * match found.
	 */
	@NotNull
	public Collection<String> getTermsViaAsk(int knownArgnum, String knownArg, int targetArgnum)
	{
		@NotNull Collection<String> result = new ArrayList<>();
		@NotNull Collection<Formula> formulae = ask("arg", knownArgnum, knownArg);
		if (!formulae.isEmpty())
		{
			@NotNull SortedSet<String> ts = new TreeSet<>();
			for (@NotNull Formula f : formulae)
			{
				ts.add(f.getArgument(targetArgnum));
			}
			result.addAll(ts);
		}
		return result;
	}

	/**
	 * Returns a List containing the terms (Strings) that
	 * correspond to targetArgnum in the Formulas obtained from the
	 * method call askWithRestriction(argnum1, term1, argnum2, term2).
	 *
	 * @param argnum1        number of args 1
	 * @param term1          term 1
	 * @param argnum2        number of args 2
	 * @param term2          term 2
	 * @param targetArgnum   target     number of args
	 * @param predicatesUsed A Set to which will be added the
	 *                       predicates of the ground assertions
	 *                       actually used to gather the terms
	 *                       returned
	 * @return A List of terms, or an empty List if no
	 * terms can be retrieved.
	 */
	@NotNull
	public List<String> getTermsViaAskWithRestriction(int argnum1, @NotNull String term1, int argnum2, @NotNull String term2, int targetArgnum, @Nullable Set<String> predicatesUsed)
	{
		@NotNull List<String> result = new ArrayList<>();
		try
		{
			if (!term1.isEmpty() && !StringUtil.isQuotedString(term1) && !term2.isEmpty() && !StringUtil.isQuotedString(term2))
			{
				@NotNull Collection<Formula> formulae = askWithRestriction(argnum1, term1, argnum2, term2);
				for (@NotNull Formula f : formulae)
				{
					result.add(f.getArgument(targetArgnum));
				}

				// record predicates used
				if (predicatesUsed != null)
				{
					for (@NotNull Formula f : formulae)
					{
						predicatesUsed.add(f.car());
					}
				}
			}
		}
		catch (Exception ex)
		{
			logger.warning(Arrays.toString(ex.getStackTrace()));
			ex.printStackTrace();
		}
		return result;
	}

	/**
	 * Returns a List containing the terms (Strings) that
	 * correspond to targetArgnum in the Formulas obtained from the
	 * method call askWithRestriction(argnum1, term1, argnum2, term2).
	 *
	 * @param argnum1      number of args 1
	 * @param term1        term 1
	 * @param argnum2      number of args 2
	 * @param term2        term 2
	 * @param targetArgnum target     number of args
	 * @return A List of terms, or an empty List if no
	 * terms can be retrieved.
	 */
	@NotNull
	public Collection<String> getTermsViaAskWithRestriction(int argnum1, @NotNull String term1, int argnum2, @NotNull String term2, int targetArgnum)
	{
		return getTermsViaAskWithRestriction(argnum1, term1, argnum2, term2, targetArgnum, null);
	}

	/**
	 * Returns a List containing the SUO-KIF terms that match the request.
	 *
	 * @param argnum1      number of args 1
	 * @param term1        term 1
	 * @param argnum2      number of args 2
	 * @param term2        term 2
	 * @param argnum3      number of args 3
	 * @param term3        term 3
	 * @param targetArgnum number of target number of args
	 * @return A List of terms, or an empty List if no matches can be found.
	 */
	@NotNull
	public Collection<String> getTermsViaAskWithTwoRestrictions(int argnum1, @NotNull String term1, int argnum2, @NotNull String term2, int argnum3, @NotNull String term3, int targetArgnum)
	{
		@NotNull Collection<String> result = new ArrayList<>();
		@NotNull Collection<Formula> formulae = askWithTwoRestrictions(argnum1, term1, argnum2, term2, argnum3, term3);
		for (@NotNull Formula f : formulae)
		{
			result.add(f.getArgument(targetArgnum));
		}
		return result;
	}

	/**
	 * Returns the first term found that corresponds to targetArgnum
	 * in the Formulas obtained from the method call
	 * askWithRestriction(argnum1, term1, argnum2, term2).
	 *
	 * @param argnum1      number of args 1
	 * @param term1        term 1
	 * @param argnum2      number of args 2
	 * @param term2        term 2
	 * @param targetArgnum target     number of args
	 * @return A SUO-KIF term (String), or null is no answer can be retrieved.
	 */
	@Nullable
	public String getFirstTermViaAskWithRestriction(int argnum1, @NotNull String term1, int argnum2, @NotNull String term2, int targetArgnum)
	{
		@Nullable String result = null;
		try
		{
			@NotNull Collection<String> terms = getTermsViaAskWithRestriction(argnum1, term1, argnum2, term2, targetArgnum);
			if (!terms.isEmpty())
			{
				result = terms.iterator().next();
			}
		}
		catch (Exception ex)
		{
			logger.warning(Arrays.toString(ex.getStackTrace()));
			ex.printStackTrace();
		}
		return result;
	}

	/**
	 * Returns a List containing SUO-KIF constants, possibly
	 * retrieved via multiple asks that recursively use relation and
	 * all of its subrelations.
	 *
	 * @param relation       The name of a predicate, which is assumed to be
	 *                       the 0th argument of one or more atomic
	 *                       Formulae
	 * @param idxArgnum      The argument position occupied by term in the
	 *                       ground atomic Formulae that will be retrieved
	 *                       to gather the target (answer) terms
	 * @param idxTerm        A constant that occupies idxArgnum position in
	 *                       each of the ground atomic Formulae that will be
	 *                       retrieved to gather the target (answer) terms
	 * @param targetArgnum   The argument position of the answer terms
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
	public Collection<String> getTermsViaPredicateSubsumption(@NotNull String relation, int idxArgnum, @NotNull String idxTerm, int targetArgnum, boolean useInverses, Set<String> predicatesUsed)
	{
		@NotNull Collection<String> result = new ArrayList<>();
		if (!relation.isEmpty() && !idxTerm.isEmpty() && (idxArgnum >= 0) /* && (idxArgnum < 7) */)
		{
			@Nullable Collection<String> inverseSyns = null;
			@Nullable Collection<String> inverses = null;
			if (useInverses)
			{
				inverseSyns = getTermsViaAskWithRestriction(0, "subrelation", 2, "inverse", 1);
				inverseSyns.addAll(getTermsViaAskWithRestriction(0, "equal", 2, "inverse", 1));
				inverseSyns.addAll(getTermsViaAskWithRestriction(0, "equal", 1, "inverse", 2));
				inverseSyns.add("inverse");
				SetUtil.removeDuplicates(inverseSyns);
				inverses = new ArrayList<>();
			}
			@NotNull Set<String> reduced = new TreeSet<>();
			@NotNull List<String> accumulator = new ArrayList<>();
			@NotNull List<String> predicates = new ArrayList<>();
			predicates.add(relation);
			while (!predicates.isEmpty())
			{
				for (@NotNull String pred : predicates)
				{
					reduced.addAll(getTermsViaAskWithRestriction(0, pred, idxArgnum, idxTerm, targetArgnum, predicatesUsed));
					accumulator.addAll(getTermsViaAskWithRestriction(0, "subrelation", 2, pred, 1));
					accumulator.addAll(getTermsViaAskWithRestriction(0, "equal", 2, "subrelation", 1));
					accumulator.addAll(getTermsViaAskWithRestriction(0, "equal", 1, "subrelation", 2));
					accumulator.remove(pred);
					if (useInverses)
					{
						for (@NotNull String syn : inverseSyns)
						{
							inverses.addAll(getTermsViaAskWithRestriction(0, syn, 1, pred, 2));
							inverses.addAll(getTermsViaAskWithRestriction(0, syn, 2, pred, 1));
						}
					}
				}
				SetUtil.removeDuplicates(accumulator);
				predicates.clear();
				predicates.addAll(accumulator);
				accumulator.clear();
			}
			if (useInverses)
			{
				SetUtil.removeDuplicates(inverses);
				for (@NotNull String inv : inverses)
				{
					reduced.addAll(getTermsViaPredicateSubsumption(inv, targetArgnum, idxTerm, idxArgnum, false, predicatesUsed));
				}
			}
			result.addAll(reduced);
		}
		return result;
	}

	/**
	 * Returns a List containing SUO-KIF constants, possibly
	 * retrieved via multiple asks that recursively use relation and
	 * all of its subrelations.
	 *
	 * @param relation     The name of a predicate, which is assumed to be
	 *                     the 0th argument of one or more atomic
	 *                     Formulae
	 * @param idxArgnum    The argument position occupied by term in the
	 *                     ground atomic Formulae that will be retrieved
	 *                     to gather the target (answer) terms
	 * @param idxTerm      A constant that occupies idxArgnum position in
	 *                     each of the ground atomic Formulae that will be
	 *                     retrieved to gather the target (answer) terms
	 * @param targetArgnum The argument position of the answer terms
	 *                     in the Formulae to be retrieved
	 * @param useInverses  If true, the inverses of relation and its
	 *                     subrelations will be also be used to try to
	 *                     find answer terms
	 * @return a List of terms (SUO-KIF constants), or an
	 * empty List if no terms can be retrieved
	 */
	@NotNull
	public Collection<String> getTermsViaPredicateSubsumption(@NotNull String relation, int idxArgnum, @NotNull String idxTerm, int targetArgnum, boolean useInverses)
	{
		return getTermsViaPredicateSubsumption(relation, idxArgnum, idxTerm, targetArgnum, useInverses, null);
	}

	/**
	 * Returns the first SUO-KIF constant found via asks using
	 * relation and its subrelations.
	 *
	 * @param relation     The name of a predicate, which is assumed to be
	 *                     the 0th argument of one or more atomic
	 *                     Formulae.
	 * @param idxArgnum    The argument position occupied by term in the
	 *                     ground atomic Formulae that will be retrieved
	 *                     to gather the target (answer) terms.
	 * @param idxTerm      A constant that occupies idxArgnum position in
	 *                     each of the ground atomic Formulae that will be
	 *                     retrieved to gather the target (answer) terms.
	 * @param targetArgnum The argument position of the answer terms
	 *                     in the Formulae to be retrieved.
	 * @param useInverses  If true, the inverses of relation and its
	 *                     subrelations will be also be used to try to
	 *                     find answer terms.
	 * @return A SUO-KIF constants (String), or null if no term can be retrieved.
	 */
	@Nullable
	public String getFirstTermViaPredicateSubsumption(@NotNull String relation, int idxArgnum, @NotNull String idxTerm, int targetArgnum, boolean useInverses)
	{
		@Nullable String result = null;
		if (!relation.isEmpty() && !idxTerm.isEmpty() && (idxArgnum >= 0) /* && (idxArgnum < 7) */)
		{
			@NotNull Collection<String> terms = getTermsViaPredicateSubsumption(relation, idxArgnum, idxTerm, targetArgnum, useInverses);
			if (!terms.isEmpty())
			{
				result = terms.iterator().next();
			}
		}
		return result;
	}

	/**
	 * Returns a List containing the transitive closure of
	 * relation starting from idxTerm in position idxArgnum.  The
	 * result does not contain idxTerm.
	 *
	 * @param relation     The name of a predicate, which is assumed to be
	 *                     the 0th argument of one or more atomic
	 *                     Formulae
	 * @param idxArgnum    The argument position occupied by term in the
	 *                     ground atomic Formulae that will be retrieved
	 *                     to gather the target (answer) terms
	 * @param idxTerm      A constant that occupies idxArgnum position in
	 *                     the first "level" of ground atomic Formulae that
	 *                     will be retrieved to gather the target (answer)
	 *                     terms
	 * @param targetArgnum The argument position of the answer terms
	 *                     in the Formulae to be retrieved
	 * @param useInverses  If true, the inverses of relation and its
	 *                     subrelations will be also be used to try to
	 *                     find answer terms
	 * @return a List of terms (SUO-KIF constants), or an
	 * empty List if no terms can be retrieved
	 */
	@NotNull
	public Collection<String> getTransitiveClosureViaPredicateSubsumption(@NotNull String relation, int idxArgnum, @NotNull String idxTerm, int targetArgnum, boolean useInverses)
	{
		@NotNull Set<String> reduced = new TreeSet<>();
		@NotNull Set<String> accumulator = new TreeSet<>(getTermsViaPredicateSubsumption(relation, idxArgnum, idxTerm, targetArgnum, useInverses));
		@NotNull List<String> working = new ArrayList<>();
		while (!accumulator.isEmpty())
		{
			reduced.addAll(accumulator);
			working.clear();
			working.addAll(accumulator);
			accumulator.clear();
			for (@NotNull String term : working)
			{
				accumulator.addAll(getTermsViaPredicateSubsumption(relation, idxArgnum, term, targetArgnum, useInverses));
			}
		}
		return new ArrayList<>(reduced);
	}

	// I N S T A N C E

	/**
	 * Determine whether a particular term is an immediate instance,
	 * which has a statement of the form (instance term otherTerm).
	 * Note that this does not count for terms such as Attribute(s)
	 * and Relation(s), which may be defined as subAttribute(s) or
	 * subrelation(s) of another instance.  If the term is not an
	 * instance, return an empty List.  Otherwise, return an
	 * List of the Formula(s) in which the given term is
	 * defined as an instance.
	 *
	 * @param term A String.
	 * @return A List.
	 */
	@NotNull
	public Collection<Formula> instancesOf(@NotNull String term)
	{
		return askWithRestriction(1, term, 0, "instance");
	}

	/**
	 * Is instance
	 *
	 * @param term term
	 * @return whether term is instance.
	 */
	public boolean isInstance(@NotNull final String term)
	{
		@NotNull Collection<Formula> formulas = askWithRestriction(0, "instance", 1, term);
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
			@NotNull List<String> subresult = new ArrayList<>();
			@NotNull List<String> working = new ArrayList<>(classNames);
			while (!working.isEmpty())
			{
				for (int i = 0; i < working.size(); i++)
				{
					@NotNull Collection<Formula> lits = askWithRestriction(1, working.get(i), 0, "subclass");
					for (@NotNull Formula f : lits)
					{
						@NotNull String arg2 = f.getArgument(2);
						if (!working.contains(arg2))
						{
							subresult.add(arg2);
						}
					}
				}

				result.addAll(subresult);
				working.clear();
				working.addAll(subresult);
				subresult.clear();
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
			@NotNull List<String> subresult = new ArrayList<>();
			@NotNull List<String> working = new ArrayList<>(classNames);
			while (!working.isEmpty())
			{
				for (int i = 0; i < working.size(); i++)
				{
					@NotNull Collection<Formula> lits = askWithRestriction(2, working.get(i), 0, "subclass");
					for (@NotNull Formula f : lits)
					{
						@NotNull String arg1 = f.getArgument(1);
						if (!working.contains(arg1))
						{
							subresult.add(arg1);
						}
					}
				}

				result.addAll(subresult);
				working.clear();
				working.addAll(subresult);
				subresult.clear();
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

	// T E S T S

	/**
	 * A static utility method.
	 *
	 * @param obj Presumably, a String.
	 * @return true if obj is a SUO-KIF variable, else false.
	 */
	public static boolean isVariable(@NotNull String obj)
	{
		if (!obj.isEmpty())
		{
			return (obj.startsWith("?") || obj.startsWith("@"));
		}
		return false;
	}

	/**
	 * A static utility method.
	 *
	 * @param obj A String.
	 * @return true if obj is a SUO-KIF logical quantifier, else
	 * false.
	 */
	public static boolean isQuantifier(@NotNull String obj)
	{
		return (Formula.UQUANT.equals(obj) || Formula.EQUANT.equals(obj));
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
	 * results are added to it and it is returned.  If accumulator is
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
	 * @return alphabetically nearest terms to the given term, which is not in the KB.
	 */
	@NotNull
	private List<String> getNearestKTerms(@NotNull final String term, @SuppressWarnings("SameParameterValue") int k)
	{
		List<String> al;
		if (k == 0)
		{
			al = listWithBlanks(1);
		}
		else
		{
			al = listWithBlanks(2 * k);
		}

		@NotNull String[] t = getTerms().toArray(new String[0]);
		int i = 0;
		while (i < t.length - 1 && t[i].compareTo(term) < 0)
		{
			i++;
		}
		if (k == 0)
		{
			al.set(0, t[i]);
			return al;
		}
		int lower = i;
		while (i - lower < k && lower > 0)
		{
			lower--;
			al.set(k - (i - lower), t[lower]);
		}
		int upper = i - 1;

		logger.finer("Number of terms in this KB == " + t.length);

		while (upper - i < (k - 1) && upper < t.length - 1)
		{
			upper++;
			al.set(k + (upper - i), t[upper]);
		}
		return al;
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
		@NotNull Collection<List<String>> result = new ArrayList<>();
		if (formulas != null)
		{
			for (@NotNull Formula f : formulas)
			{
				result.add(f.elements());
			}
		}
		return result;
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
		@NotNull List<Formula> result = new ArrayList<>();
		if (forms != null)
		{
			for (@NotNull String form : forms)
			{
				@NotNull Formula f = Formula.of(form);
				result.add(f);
			}
		}
		return result;
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
		@NotNull StringBuilder sb = new StringBuilder();
		if (lits != null)
		{
			sb.append("(");
			for (int i = 0; i < lits.size(); i++)
			{
				if (i > 0)
				{
					sb.append(" ");
				}
				sb.append(lits.get(i));
			}
			sb.append(")");
		}
		return sb.toString();
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

	// H E L P E R S

	/**
	 * Create a List of the specific size, filled with empty strings.
	 *
	 * @return list of empty strings.
	 */
	@NotNull
	protected static List<String> listWithBlanks(int size)
	{
		@NotNull List<String> al = new ArrayList<>(size);
		for (int i = 0; i < size; i++)
		{
			al.add("");
		}
		return al;
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
			if (Character.isLowerCase(term.charAt(i)) || !Character.isLetter(term.charAt(i)))
			{
				result.append(term.charAt(i));
			}
			else
			{
				if (i + 1 < term.length() && Character.isUpperCase(term.charAt(i + 1)))
				{
					result.append(term.charAt(i));
				}
				else
				{
					if (i != 0)
					{
						result.append(" ");
					}
					result.append(Character.toLowerCase(term.charAt(i)));
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
		try
		{
			for (@NotNull Formula f : new TreeSet<>(formulas))
			{
				@NotNull String result = f.toProlog();
				if (!result.isEmpty())
				{
					pr.println(result);
				}
			}
		}
		catch (Exception ex)
		{
			logger.warning(Arrays.toString(ex.getStackTrace()));
			ex.printStackTrace();
		}
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
			writePrologFormulas(ask("arg", 0, "subAttribute"), pr);
			pr.println("\n% subrelation");
			writePrologFormulas(ask("arg", 0, "subrelation"), pr);
			pr.println("\n% disjoint");
			writePrologFormulas(ask("arg", 0, "disjoint"), pr);
			pr.println("\n% partition");
			writePrologFormulas(ask("arg", 0, "partition"), pr);
			pr.println("\n% instance");
			writePrologFormulas(ask("arg", 0, "instance"), pr);
			pr.println("\n% subclass");
			writePrologFormulas(ask("arg", 0, "subclass"), pr);
			pr.flush();
		}
		catch (Exception e)
		{
			logger.warning(e.getMessage());
			e.printStackTrace();
		}
	}
}