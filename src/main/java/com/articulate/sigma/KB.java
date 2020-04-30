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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Contains methods for reading, writing knowledge bases and their
 * configurations.  Also contains the inference engine process for
 * the knowledge base.
 */
public class KB implements Serializable
{
	private static final long serialVersionUID = 1L;

	private static final Logger logger = Logger.getLogger(KB.class.getName());

	/**
	 * This list contains the names of SUMO Relations known to be
	 * instances of VariableArityRelation in at least some domain.  It
	 * is used only for TPTP generation, and should
	 * <strong>not</strong> be relied upon for any other purpose,
	 * since it is not automatically generated and might be out of
	 * date.
	 */
	public static final List<String> VA_RELNS = Arrays.asList( //
			"AssignmentFn", "GreatestCommonDivisorFn", "LatitudeFn", "LeastCommonMultipleFn", "ListFn", "LongitudeFn", "contraryAttribute",
			"disjointDecomposition", "exhaustiveAttribute", "exhaustiveDecomposition", "partition", "processList");

	/**
	 * A threshold limiting the number of values that will be added to a single relation cache table.
	 */
	private static final long MAX_CACHE_SIZE = 1000000;

	/**
	 * The String constant that is the suffix for files of cached assertions.
	 */
	public static final String _cacheFileSuffix = "_Cache.kif";

	/**
	 * The name of the knowledge base.
	 */
	public final String name;

	/**
	 * An List of Strings that are the full canonical pathnames of the files that comprise the KB.
	 */
	public final List<String> constituents = new ArrayList<>();

	/**
	 * The location of preprocessed KIF files
	 */
	public final String kbDir;

	/**
	 * Visibility
	 */
	private boolean isVisible = true;

	/**
	 * A synchronized SortedSet of Strings, which are all the terms in the KB.
	 */
	public final SortedSet<String> terms = Collections.synchronizedSortedSet(new TreeSet<>());

	/**
	 * A Map of all the Formula objects in the KB.  Each key is a String representation of a Formula.  Each value is the Formula
	 * object corresponding to the key.
	 */
	public final Map<String, Formula> formulaMap = new LinkedHashMap<>();

	/**
	 * A Map of Lists of String formulae, containing all the formulae in the KB.  Keys are the formula itself, a formula ID, and term
	 * indexes created in KIF.createKey().  The actual formula can be retrieved by using the returned String as the key for the variable formulaMap
	 */
	public final Map<String, List<Formula>> formulas = new HashMap<>();

	/**
	 * The natural language formatting strings for relations in the KB. It is a Map of language keys and Map values.
	 * The interior Map is term name keys and String values.
	 */
	protected Map<String, Map<String, String>> formatMap = new HashMap<>();

	/**
	 * The natural language strings for terms in the KB. It is a Map of language keys and Map values. The interior
	 * Map is term name keys and String values.
	 */
	protected Map<String, Map<String, String>> termFormatMap = new HashMap<>();

	/**
	 * A Map of Sets, which contain all the parent classes of a given class.
	 */
	public Map<String, Set<String>> parents = new HashMap<>();

	/**
	 * A Map of Sets, which contain all the child classes of a given class.
	 */
	public Map<String, Set<String>> children = new HashMap<>();

	/**
	 * A Map of Sets, which contain all the disjoint classes of a given class.
	 */
	public Map<String, Set<String>> disjoint = new HashMap<>();

	/**
	 * Relations with args
	 */
	private Map<String, boolean[]> relnsWithRelnArgs = null;

	/**
	 * Relation valences
	 */
	private final Map<String, int[]> relationValences = new HashMap<>();

	/**
	 * A List of the names of cached transitive relations.
	 */
	private final List<String> cachedTransitiveRelationNames = Arrays
			.asList("subclass", "subset", "subrelation", "subAttribute", "subOrganization", "subCollection", "subProcess", "geographicSubregion",
					"geopoliticalSubdivision");

	/**
	 * A List of the names of cached reflexive relations.
	 */
	private final List<String> cachedReflexiveRelationNames = Arrays
			.asList("subclass", "subset", "subrelation", "subAttribute", "subOrganization", "subCollection", "subProcess");

	/**
	 * A List of the names of cached relations.
	 */
	private final List<String> cachedRelationNames = Arrays.asList("instance", "disjoint");

	/**
	 * An List of RelationCache objects.
	 */
	private final List<RelationCache> relationCaches = new ArrayList<>();

	/**
	 * If true, assertions of the form (predicate x x) will be included in the relation cache tables.
	 */
	public final boolean cacheReflexiveAssertions = false;

	/**
	 * Errors and warnings found during loading of the KB constituents.
	 */
	public final SortedSet<String> errors = new TreeSet<>();

	/**
	 * A global counter used to ensure that constants created by instantiateFormula() are unique.
	 */
	private int genSym = 0;

	// C O N S T R U C T O R

	/**
	 * Constructor (for deserialization)
	 */
	protected KB()
	{
		name = null;
		kbDir = null;
	}

	/**
	 * Constructor which takes the name of the KB and the location where KBs preprocessed for Vampire should be placed.
	 *
	 * @param n   name
	 * @param dir directory
	 */
	public KB(String n, String dir)
	{
		name = n;
		kbDir = dir;
	}

	/**
	 * Constructor
	 *
	 * @param n          name
	 * @param dir        directory
	 * @param visibility visibility
	 */
	public KB(String n, String dir, boolean visibility)
	{
		this(n, dir);
		isVisible = visibility;
	}

	/**
	 * Constructor
	 *
	 * @param n name
	 */
	public KB(String n)
	{
		name = n;
		KBManager mgr = KBManager.getMgr();
		kbDir = mgr.getPref("kbDir");
	}

	/**
	 * Returns a synchronized SortedSet of Strings, which are all the terms in the KB.
	 *
	 * @return a synchronized sorted list of all the terms in the KB.
	 */
	public SortedSet<String> getTerms()
	{
		return this.terms;
	}

	/**
	 * Return List of all nonrelTerms in an List
	 *
	 * @param list input list
	 * @return An List of nonrelTerms
	 */
	public List<String> getAllNonRelTerms(List<String> list)
	{
		List<String> nonRelTerms = new ArrayList<>();
		for (String t : list)
		{
			if (Character.isUpperCase(t.charAt(0)))
				nonRelTerms.add(t);
		}
		return nonRelTerms;
	}

	/**
	 * Return List of all relTerms in an List
	 *
	 * @param list input list
	 * @return An List of relTerms
	 */
	public List<String> getAllRelTerms(List<String> list)
	{
		List<String> relTerms = new ArrayList<>();
		for (String t : list)
		{
			if (Character.isLowerCase(t.charAt(0)))
				relTerms.add(t);
		}
		return relTerms;
	}

	/**
	 * Takes a term (interpreted as a Regular Expression) and returns an List
	 * containing every term in the KB that has a match with the RE.
	 *
	 * @param term A String
	 * @return An List of terms that have a match to term
	 */
	public List<String> getREMatch(String term)
	{
		try
		{
			Pattern p = Pattern.compile(term);
			List<String> matchesList = new ArrayList<>();
			for (String t : getTerms())
			{
				Matcher m = p.matcher(t);
				if (m.matches())
					matchesList.add(t);
			}
			return matchesList;
		}
		catch (PatternSyntaxException ex)
		{
			List<String> err = new ArrayList<>();
			err.add("Invalid Input");
			return err;
		}
	}

	/**
	 * Visibility
	 *
	 * @return visibility
	 */
	public boolean isVisible()
	{
		return isVisible;
	}

	/**
	 * If this method returns true, then reflexive assertions will be
	 * included in the relation caches built when Sigma starts up.
	 *
	 * @return true or false
	 */
	public boolean getCacheReflexiveAssertions()
	{
		return cacheReflexiveAssertions;
	}

	/**
	 * Get cached relation names
	 *
	 * @return An List of relation names (Strings).
	 */
	private List<String> getCachedRelationNames()
	{
		List<String> relationNames = new ArrayList<>();
		try
		{
			Set<String> reduced = new LinkedHashSet<>(cachedRelationNames);
			reduced.addAll(getCachedTransitiveRelationNames());
			reduced.addAll(getCachedSymmetricRelationNames());
			relationNames.addAll(reduced);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		return relationNames;
	}

	/**
	 * Returns a list of the names of cached transitive relations.
	 *
	 * @return An List of relation names (Strings).
	 */
	private List<String> getCachedTransitiveRelationNames()
	{
		List<String> result = new ArrayList<>(cachedTransitiveRelationNames);
		Set<String> trSet = getAllInstancesWithPredicateSubsumption("TransitiveRelation");
		for (String name : trSet)
		{
			if (!result.contains(name))
				result.add(name);
		}
		return result;
	}

	/**
	 * Returns a list of the names of cached symmetric relations.
	 *
	 * @return An List of relation names (Strings).
	 */
	private List<String> getCachedSymmetricRelationNames()
	{
		Set<String> symmSet = getAllInstancesWithPredicateSubsumption("SymmetricRelation");
		// symmSet.addAll(getTermsViaPredicateSubsumption("subrelation",2,"inverse",1,true));
		symmSet.add("inverse");
		return new ArrayList<>(symmSet);
	}

	/**
	 * Get cached reflexive relation names
	 *
	 * @return An List of relation names (Strings).
	 */
	private List<String> getCachedReflexiveRelationNames()
	{
		List<String> result = new ArrayList<>();
		List<String> reflexives = new ArrayList<>(cachedReflexiveRelationNames);
		for (String name : getAllInstancesWithPredicateSubsumption("ReflexiveRelation"))
		{
			if (!reflexives.contains(name))
				reflexives.add(name);
		}
		List<String> cached = getCachedRelationNames();
		for (String reflexive : reflexives)
		{
			if (cached.contains(reflexive))
				result.add(reflexive);
		}
		return result;
	}

	/**
	 * get relation caches
	 *
	 * @return An List of RelationCache objects.
	 */
	protected List<RelationCache> getRelationCaches()
	{
		return this.relationCaches;
	}

	/**
	 * This Map is used to cache sortal predicate argument type data
	 * whenever Formula.findType() or Formula.getTypeList() will be
	 * called hundreds of times inside KB.preProcess(), or to
	 * accomplish another expensive computation tasks.  The Map is
	 * cleared after each use in KB.preProcess(), but may retain its
	 * contents when used in other contexts.
	 */
	private Map<String, List<String>> sortalTypeCache = null;

	/**
	 * Returns the Map is used to cache sortal predicate argument type
	 * data whenever Formula.findType() or Formula.getTypeList() will
	 * be called hundreds of times inside KB.preProcess(), or to
	 * accomplish another expensive computation tasks.  The Map is
	 * cleared after each use in KB.preProcess(), but may retain its
	 * contents when used in other contexts.
	 *
	 * @return the Map is used to cache sortal predicate argument type data.
	 */
	public Map<String, List<String>> getSortalTypeCache()
	{
		if (sortalTypeCache == null)
			sortalTypeCache = new HashMap<>();
		return sortalTypeCache;
	}

	/**
	 * Initializes all RelationCaches.  Creates the RelationCache
	 * objects if they do not yet exist, and clears all existing
	 * RelationCache objects if clearExistingCaches is true.
	 *
	 * @param clearExistingCaches If true, all existing RelationCache
	 *                            maps are cleared and the List of RelationCaches is cleared, else
	 *                            all existing RelationCache objects and their contents are
	 *                            reused
	 */
	protected void initRelationCaches(boolean clearExistingCaches)
	{
		logger.entering("KB", "initRelationCaches", "clearExistingCaches = " + clearExistingCaches);
		if (clearExistingCaches)
		{
			// Clear all cache maps.
			for (RelationCache rc : getRelationCaches())
			{
				rc.clear();
			}
			getRelationCaches().clear();  // Discard all cache maps.
		}
		List<String> symmetric = getCachedSymmetricRelationNames();
		for (String reln : getCachedRelationNames())
		{
			getRelationCache(reln, 1, 2);
			// We put each symmetric relation -- disjoint and a few others -- into just one RelationCache table apiece.
			// All transitive binary relations are cached in two RelationCaches, one that looks "upward" from
			// the keys, and another that looks "downward" from the keys.
			if (!symmetric.contains(reln))
				getRelationCache(reln, 2, 1);
		}
		// We still set these legacy variables.  Eventually, they should be removed.
		parents = getRelationCache("subclass", 1, 2);
		children = getRelationCache("subclass", 2, 1);
		disjoint = getRelationCache("disjoint", 1, 2);
		logger.exiting("KB", "initRelationCaches");
	}

	/**
	 * Returns the RelationCache object identified by the input
	 * arguments: relation name, key argument position, and value
	 * argument position.
	 *
	 * @param relName  The name of the cached relation.
	 * @param keyArg   An int value that indicates the argument position
	 *                 of the cache keys.
	 * @param valueArg An int value that indicates the argument
	 *                 position of the cache values.
	 * @return a RelationCache object, or null if there is no cache corresponding to the input arguments.
	 */
	private RelationCache getRelationCache(String relName, int keyArg, int valueArg)
	{
		RelationCache result = null;
		try
		{
			if (!relName.isEmpty())
			{
				RelationCache cache;
				for (RelationCache relationCache : getRelationCaches())
				{
					cache = relationCache;
					if (cache.getRelationName().equals(relName) && (cache.getKeyArgument() == keyArg) && (cache.getValueArgument() == valueArg))
					{
						result = cache;
						break;
					}
				}
				if (result == null)
				{
					cache = new RelationCache(relName, keyArg, valueArg);
					getRelationCaches().add(cache);
					result = cache;
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
	 * Adds one value to the cache, indexed under keyTerm.
	 *
	 * @param cache     The RelationCache object to be updated.
	 * @param keyTerm   The String that is the key for this entry.
	 * @param valueTerm The String that is the value for this entry.
	 * @return The int value 1 if a new entry is added, else 0.
	 */
	private int addRelationCacheEntry(RelationCache cache, String keyTerm, String valueTerm)
	{
		int count = 0;
		if ((cache != null) && !keyTerm.isEmpty() && !valueTerm.isEmpty())
		{
			Set<String> valueSet = cache.computeIfAbsent(keyTerm, k -> new HashSet<>());
			if (valueSet.add(valueTerm))
				count++;
		}
		return count;
	}

	/**
	 * Returns the Set indexed by term in the RelationCache
	 * identified by relation, keyArg, and valueArg.
	 *
	 * @param relation A String, the name of a relation
	 * @param term     A String (key) that indexes a Set
	 * @param keyArg   An int value that, with relation and valueArg,
	 *                 identifies a RelationCache
	 * @param valueArg An int value that, with relation and keyArg,
	 *                 identifies a RelationCache
	 * @return A Set, which could be empty
	 */
	public Set<String> getCachedRelationValues(String relation, String term, int keyArg, int valueArg)
	{
		Set<String> result = new HashSet<>();
		RelationCache cache = getRelationCache(relation, keyArg, valueArg);
		if (cache != null)
		{
			Set<String> values = cache.get(term);
			if (values != null)
				result.addAll(values);
		}
		return result;
	}

	/**
	 * Check arity
	 */
	public void checkArity()
	{
		List<String> toRemove = new ArrayList<>();
		if (formulaMap.size() > 0)
		{
			for (String s : formulaMap.keySet())
			{
				Formula f = formulaMap.get(s);
				if (!f.hasCorrectArity(this))
				{
					errors.add("Formula in " + f.sourceFile + " rejected due to arity error: <br/>" + f.text);
					toRemove.add(f.text);
				}
			}
		}
		for (String s : toRemove)
			formulaMap.remove(s);
	}

	/**
	 * This method computes the transitive closure for the relation
	 * identified by relationName.  The results are stored in the
	 * RelationCache object for the relation and "direction" (looking
	 * from the arg1 keys toward arg2 parents, or looking from the
	 * arg2 keys toward arg1 children).
	 *
	 * @param relationName The name of a relation
	 */
	private void computeTransitiveCacheClosure(String relationName)
	{
		logger.entering("KB", "computerTransitiveCacheClosure", "relationName = " + relationName);
		long count = 0L;
		try
		{
			if (getCachedTransitiveRelationNames().contains(relationName))
			{
				RelationCache c1 = getRelationCache(relationName, 1, 2);
				RelationCache c2 = getRelationCache(relationName, 2, 1);
				if (c1 != null && c2 != null)
				{
					RelationCache inst1 = null;
					RelationCache inst2 = null;
					boolean isSubrelationCache = relationName.equals("subrelation");
					if (isSubrelationCache)
					{
						inst1 = getRelationCache("instance", 1, 2);
						inst2 = getRelationCache("instance", 2, 1);
					}
					Set<String> c1Keys = c1.keySet();

					boolean changed = true;
					while (changed)
					{
						changed = false;
						for (String keyTerm : c1Keys)
						{
							if (keyTerm == null || keyTerm.isEmpty())
								logger.warning("Error in KB.computeTransitiveCacheClosure(" + relationName + ") \n   keyTerm == " + ((keyTerm == null) ?
										null :
										"\"" + keyTerm + "\""));
							else
							{
								Set<String> valSet = c1.get(keyTerm);
								String[] valArr = valSet.toArray(new String[0]);
								for (String valTerm : valArr)
								{
									Set<String> valSet2 = c1.get(valTerm);
									if (valSet2 != null)
									{
										for (String s : valSet2)
										{
											if (count >= MAX_CACHE_SIZE)
												break;
											if (valSet.add(s))
											{
												changed = true;
												count++;
											}
										}
									}
									if (count < MAX_CACHE_SIZE)
									{
										valSet2 = c2.computeIfAbsent(valTerm, k -> new HashSet<>());
										if (valSet2.add(keyTerm))
										{
											changed = true;
											count++;
										}
									}
								}
								// Here we try to ensure that instances of Relation have at least some entry in the
								// "instance" caches, since this information is sometimes considered
								// redundant and so could be left out of .kif files.
								if (isSubrelationCache)
								{
									String valTerm = "Relation";
									if (keyTerm.endsWith("Fn"))
										valTerm = "Function";
									else
									{
										String nsDelim = StringUtil.getKifNamespaceDelimiter();
										int ndIdx = keyTerm.indexOf(nsDelim);
										String stripped = keyTerm;
										if (ndIdx > -1)
											stripped = keyTerm.substring(nsDelim.length() + ndIdx);
										if (Character.isLowerCase(stripped.charAt(0)) && !keyTerm.contains("("))
											valTerm = "Predicate";
									}
									addRelationCacheEntry(inst1, keyTerm, valTerm);
									addRelationCacheEntry(inst2, valTerm, keyTerm);
								}
							}
						}
						if (changed)
						{
							c1.setIsClosureComputed();
							c2.setIsClosureComputed();
						}
					}
				}
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		if (count > 0)
			logger.fine(count + " " + relationName + " entries computed");
		logger.exiting("KB", "computeTransitiveCacheClosure");
	}

	/**
	 * This method computes the closure for the cache of the instance
	 * relation, in both directions.
	 */
	private void computeInstanceCacheClosure()
	{
		logger.entering("KB", "computeInstanceCacheClosure");
		long count = 0L;
		try
		{
			RelationCache ic1 = getRelationCache("instance", 1, 2);
			RelationCache ic2 = getRelationCache("instance", 2, 1);
			RelationCache sc1 = getRelationCache("subclass", 1, 2);

			Set<String> ic1KeySet = ic1.keySet();
			for (String ic1KeyTerm : ic1KeySet)
			{
				Set<String> ic1ValSet = ic1.get(ic1KeyTerm);

				String[] ic1ValArr = ic1ValSet.toArray(new String[0]);
				for (String ic1ValTerm : ic1ValArr)
				{
					if (ic1ValTerm != null)
					{
						Set<String> sc1ValSet = sc1.get(ic1ValTerm);
						if (sc1ValSet != null)
						{
							for (String s : sc1ValSet)
							{
								if (count >= MAX_CACHE_SIZE)
									break;
								if (ic1ValSet.add(s))
								{
									count++;
								}
							}
						}
					}
				}
				if (count < MAX_CACHE_SIZE)
				{
					for (String ic1ValTerm : ic1ValSet)
					{
						Set<String> ic2ValSet = ic2.computeIfAbsent(ic1ValTerm, k -> new HashSet<>());
						if (ic2ValSet.add(ic1KeyTerm))
						{
							count++;
						}
					}
				}
			}

			ic1.setIsClosureComputed();
			ic2.setIsClosureComputed();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		if (count > 0)
			logger.info(count + " instance entries");

		logger.exiting("KB", "computeInstanceCacheClosure");
	}

	/**
	 * This method computes the closure for the caches of symmetric
	 * relations.  As currently implemented, it really applies to only
	 * disjoint.
	 */
	private void computeSymmetricCacheClosure(String relationName)
	{
		logger.entering("KB", "computeSymmetricCacheClosure", "relationName = " + relationName);
		long count = 0L;
		try
		{
			RelationCache dc1 = getRelationCache(relationName, 1, 2);
			RelationCache sc2 = (relationName.equals("disjoint") ? getRelationCache("subclass", 2, 1) : null);
			if (sc2 != null)
			{
				// int passes = 0; 	// One pass is sufficient.
				boolean changed = true;
				while (changed)
				{
					changed = false;

					Set<String> dc1KeySet = dc1.keySet();
					String[] dc1KeyArr = dc1KeySet.toArray(new String[0]);
					for (int i = 0; (i < dc1KeyArr.length) && (count < MAX_CACHE_SIZE); i++)
					{
						String dc1KeyTerm = dc1KeyArr[i];
						Set<String> dc1ValSet = dc1.get(dc1KeyTerm);
						String[] dc1ValArr = dc1ValSet.toArray(new String[0]);
						for (String dc1ValTerm : dc1ValArr)
						{
							Set<String> sc2ValSet = sc2.get(dc1ValTerm);
							if (sc2ValSet != null)
							{
								if (dc1ValSet.addAll(sc2ValSet))
								{
									changed = true;
								}
							}
						}
						Set<String> sc2ValSet = sc2.get(dc1KeyTerm);
						if (sc2ValSet != null)
						{
							for (String sc2ValTerm : sc2ValSet)
							{
								Set<String> dc1ValSet2 = dc1.computeIfAbsent(sc2ValTerm, k -> new HashSet<>());
								if (dc1ValSet2.addAll(dc1ValSet))
								{
									changed = true;
								}
							}
						}
						count = 0;
						for (Set<String> dc1ValSet3 : dc1.values())
						{
							count += dc1ValSet3.size();
						}
					}
					if (changed)
						dc1.setIsClosureComputed();
				}
			}
			// printDisjointness();
		}
		catch (Exception ex)
		{
			logger.warning(Arrays.toString(ex.getStackTrace()));
			ex.printStackTrace();
		}
		if (count > 0L)
			logger.info(count + " " + relationName + " entries computed");
		logger.exiting("KB", "computeSymmetricCacheClosure");
	}

	/**
	 * This method builds a cache of all Relations in the current KB
	 * for which at least one argument must be filled by a relation
	 * name (or a variable denoting a relation name).  This method
	 * should be called only after the subclass cache has been built.
	 */
	private void cacheRelnsWithRelnArgs()
	{
		logger.entering("KB", "cacheRelnsWithRelnArgs");
		try
		{
			if (relnsWithRelnArgs == null)
				relnsWithRelnArgs = new HashMap<>();
			relnsWithRelnArgs.clear();

			Set<String> relnClasses = getCachedRelationValues("subclass", "Relation", 2, 1);
			if (relnClasses != null)
				relnClasses.add("Relation");
			if (relnClasses != null)
			{
				for (String relnClass : relnClasses)
				{
					List<Formula> formulas = askWithRestriction(3, relnClass, 0, "domain");
					if (formulas != null)
					{
						for (Formula f : formulas)
						{
							String reln = f.getArgument(1);
							int valence = getValence(reln);
							if (valence < 1)
								valence = Formula.MAX_PREDICATE_ARITY;
							boolean[] signature = relnsWithRelnArgs.get(reln);
							if (signature == null)
							{
								signature = new boolean[valence + 1];
								Arrays.fill(signature, false);
								relnsWithRelnArgs.put(reln, signature);
							}
							int argPos = Integer.parseInt(f.getArgument(2));
							try
							{
								signature[argPos] = true;
							}
							catch (Exception e1)
							{
								logger.warning("Error in KB.cacheRelnsWithRelnArgs(): reln == " + reln + ", argPos == " + argPos + ", signature == " + Arrays
										.toString(signature));
								throw e1;
							}
						}
					}
				}
				// This is a kluge.  "format" (and "termFormat", which is not directly relevant here) should be defined as
				// predicates (meta-predicates) in Merge.kif, or in some language-independent paraphrase scaffolding .kif file.
				boolean[] signature = relnsWithRelnArgs.get("format");
				if (signature == null)
				{
					signature = new boolean[4];
					// signature = { false, false, true, false };
					for (int i = 0; i < signature.length; i++)
						signature[i] = (i == 2);
					relnsWithRelnArgs.put("format", signature);
				}
			}
		}
		catch (Exception ex)
		{
			logger.warning(Arrays.toString(ex.getStackTrace()));
			ex.printStackTrace();
		}
		int rwraSize = relnsWithRelnArgs.size();
		if (rwraSize > 0)
		{
			logger.info(rwraSize + " relation argument entries computed");
		}
		logger.exiting("KB", "cacheRelnsWithRelnArgs");
	}

	/**
	 * Returns a boolean[] if the input relation has at least one argument that must be filled by a relation name.
	 *
	 * @param relation relation
	 * @return whether the input relation has at least one argument that must be filled by a relation name.
	 */
	protected boolean[] getRelnArgSignature(String relation)
	{
		if (relnsWithRelnArgs != null)
		{
			return relnsWithRelnArgs.get(relation);
		}
		return null;
	}

	/**
	 * Cache relation valences
	 */
	private void cacheRelationValences()
	{
		logger.entering("KB", "cacheRelationValences");
		try
		{
			Set<String> relations = getCachedRelationValues("instance", "Relation", 2, 1);
			if (relations != null)
			{
				List<String> namePrefixes = Arrays.asList("VariableArity", "Unary", "Binary", "Ternary", "Quaternary", "Quintary");
				int npLen = namePrefixes.size();
				RelationCache ic1 = getRelationCache("instance", 1, 2);
				RelationCache ic2 = getRelationCache("instance", 2, 1);

				for (String reln : relations)
				{
					// Here we evaluate getValence() to build the relationValences cache, and use its return
					// value to fill in any info that might be missing from the "instance" cache.
					int valence = getValence(reln);
					if ((valence > -1) && (valence < npLen))
					{
						StringBuilder sb = new StringBuilder();
						if (reln.endsWith("Fn"))
						{
							if ((valence > 0) && (valence < 5))
							{
								sb.append(namePrefixes.get(valence));
								sb.append("Function");
							}
						}
						else
						{
							sb.append(namePrefixes.get(valence));
							sb.append("Relation");
						}
						String className = sb.toString();
						if (!className.isEmpty())
						{
							addRelationCacheEntry(ic1, reln, className);
							addRelationCacheEntry(ic2, className, reln);
						}
					}
				}
			}
		}
		catch (Exception ex)
		{
			logger.warning(Arrays.toString(ex.getStackTrace()));
			ex.printStackTrace();
		}
		logger.info("RelationValences == " + relationValences.size() + " entries");
		logger.exiting("KB", "cacheRelationValences");
	}

	/**
	 * Returns the type (SUO-KIF SetOrClass name) for any argument in
	 * argPos position of an assertion formed with the SUO-KIF
	 * Relation reln.  If no argument type value is directly stated
	 * for reln, this method tries to find a value inherited from one
	 * of reln's super-relations.
	 *
	 * @param reln   A String denoting a SUO-KIF Relation
	 * @param argPos An int denoting an argument position, where 0 is
	 *               the position of reln itself
	 * @return A String denoting a SUO-KIF SetOrClass, or null if no
	 * value can be obtained
	 */
	public String getArgType(String reln, int argPos)
	{
		String className = null;
		try
		{
			String argType = Formula.findType(argPos, reln, this);
			if (!argType.isEmpty())
			{
				if (argType.endsWith("+"))
					argType = "SetOrClass";
				className = argType;
			}
		}
		catch (Exception ex)
		{
			logger.warning(Arrays.toString(ex.getStackTrace()));
			ex.printStackTrace();
		}
		return className;
	}

	/**
	 * Returns the type (SUO-KIF SetOrClass name) for any argument in
	 * argPos position of an assertion formed with the SUO-KIF
	 * Relation reln.  If no argument type value is directly stated
	 * for reln, this method tries to find a value inherited from one
	 * of reln's super-relations.
	 *
	 * @param reln   A String denoting a SUO-KIF Relation
	 * @param argPos An int denoting an argument position, where 0 is
	 *               the position of reln itself
	 * @return A String denoting a SUO-KIF SetOrClass, or null if no
	 * value can be obtained.  A '+' is appended to the class name
	 * if the argument is a subclass of the class, rather than an instance
	 */
	public String getArgTypeClass(String reln, int argPos)
	{
		String className = null;
		try
		{
			String argType = Formula.findType(argPos, reln, this);
			if (!argType.isEmpty())
				className = argType;
		}
		catch (Exception ex)
		{
			logger.warning(Arrays.toString(ex.getStackTrace()));
			ex.printStackTrace();
		}
		return className;
	}

	/**
	 * Returns true if relnName is the name of a relation that is
	 * known to be, or computed to be, a variable arity relation.
	 *
	 * @param relnName A String that names a SUMO Relation (Predicate
	 *                 or Function).
	 * @return boolean
	 */
	public boolean isVariableArityRelation(String relnName)
	{
		return VA_RELNS.contains(relnName) || (getValence(relnName) == 0) || isInstanceOf(relnName, "VariableArityRelation");
	}

	/**
	 * List relations with relation arguments
	 *
	 * @return list of relations
	 */
	protected List<String> listRelnsWithRelnArgs()
	{
		if (relnsWithRelnArgs != null)
			return new ArrayList<>(relnsWithRelnArgs.keySet());
		return null;
	}

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
	 * @return An List.
	 */
	public List<Formula> instancesOf(String term)
	{
		return askWithRestriction(1, term, 0, "instance");
	}

	/**
	 * Returns true if i is an instance of c, else returns false.
	 *
	 * @param i A String denoting an instance.
	 * @param c A String denoting a Class.
	 * @return whether i is an instance of c.
	 */
	public boolean isInstanceOf(String i, String c)
	{
		boolean result = false;
		try
		{
			result = getCachedRelationValues("instance", i, 1, 2).contains(c);
			// was: getAllInstancesWithPredicateSubsumption(c);
		}
		catch (Exception ex)
		{
			logger.warning(Arrays.toString(ex.getStackTrace()));
			ex.printStackTrace();
		}
		return result;
	}

	/**
	 * Returns true if i is c, is an instance of c, or is subclass of c, else returns false.
	 *
	 * @param i A String denoting an instance.
	 * @param c A String denoting a Class.
	 * @return whether i is c, is an instance of c, or is subclass of c.
	 */
	public boolean isChildOf(String i, String c)
	{
		return i.equals(c) || isInstanceOf(i, c) || isSubclass(i, c);
	}

	/**
	 * Is instance
	 *
	 * @param term term
	 * @return whether term is instance.
	 */
	public boolean isInstance(String term)
	{
		List<Formula> al = askWithRestriction(0, "instance", 1, term);
		return (al != null && al.size() > 0);
	}

	/**
	 * Determine whether a particular class or instance "child" is a child of the given "parent".
	 *
	 * @param child  A String, the name of a term.
	 * @param parent A String, the name of a term.
	 * @return true if child and parent constitute an actual or
	 * implied relation in the current KB, else false.
	 */
	public boolean childOf(String child, String parent)
	{
		boolean result = child.equals(parent);
		if (!result)
		{
			List<String> preds = Arrays.asList("instance", "subclass", "subrelation");
			for (String pred : preds)
			{
				Set<String> parents = getCachedRelationValues(pred, child, 1, 2);
				result = parents.contains(parent);
				if (result)
					break;
			}
		}
		return result;
	}

	/**
	 * Test if the subclass cache supports the conclusion that c1 is a subclass of c2, else returns false.
	 *
	 * @param c1 A String, the name of a SetOrClass.
	 * @param c2 A String, the name of a SetOrClass.
	 * @return whether the subclass cache supports the conclusion that c1 is a subclass of c2.
	 */
	public boolean isSubclass(String c1, String c2)
	{
		boolean result = false;
		if (!c1.isEmpty() && !c2.isEmpty())
		{
			result = getCachedRelationValues("subclass", c1, 1, 2).contains(c2);
			// was: getAllSubClassesWithPredicateSubsumption(c2);
		}
		return result;
	}

	/**
	 * Builds all of the relation caches for the current KB.  If
	 * RelationCache Map objects already exist, they are cleared and
	 * discarded.  New RelationCache Maps are created, and all caches
	 * are rebuilt.
	 *
	 * @param clearExistingCaches If true, all existing caches are
	 *                            cleared and discarded and completely new caches are created,
	 *                            else if false, any existing caches are used and augmented
	 */
	public void buildRelationCaches(boolean clearExistingCaches)
	{
		logger.entering("KB", "buildRelationCaches", "clearExistingCaches = " + clearExistingCaches);
		long totalCacheEntries = 0L;
		int i;
		for (i = 1; true; i++)
		{
			initRelationCaches(clearExistingCaches);
			clearExistingCaches = false;

			cacheGroundAssertionsAndPredSubsumptionEntailments();
			for (String relationName : getCachedTransitiveRelationNames())
			{
				computeTransitiveCacheClosure(relationName);
			}
			computeInstanceCacheClosure();

			// "disjoint"
			for (String relationName : getCachedSymmetricRelationNames())
			{
				if (Objects.equals("disjoint", relationName))
					computeSymmetricCacheClosure(relationName);
			}
			cacheRelnsWithRelnArgs();
			cacheRelationValences();

			long entriesAfterThisIteration = 0L;
			for (RelationCache relationCache : getRelationCaches())
			{
				if (!relationCache.isEmpty())
				{
					for (Set<String> values : relationCache.values())
					{
						entriesAfterThisIteration += values.size();
					}
				}
			}
			if (entriesAfterThisIteration > totalCacheEntries)
				totalCacheEntries = entriesAfterThisIteration;
			else
				break;
			if (i > 4)
				break;
		}
		logger.info("Caching cycles == " + i + "\n Cache entries == " + totalCacheEntries);
		logger.exiting("KB", "buildRelationCaches");
	}

	/**
	 * Builds all of the relation caches for the current KB.  If
	 * RelationCache Map objects already exist, they are cleared and
	 * discarded.  New RelationCache Maps are created, and all caches
	 * are rebuilt.
	 */
	public void buildRelationCaches()
	{
		buildRelationCaches(true);
	}

	/**
	 * Populates all caches with ground assertions, from which
	 * closures can be computed.
	 */
	private void cacheGroundAssertionsAndPredSubsumptionEntailments()
	{
		logger.entering("KB", "cacheGroundAssertionsAndPredSubsumptionEntailments");
		List<String> symmetric = getCachedSymmetricRelationNames();
		List<String> reflexive = getCachedReflexiveRelationNames();

		int total = 0;
		for (String relation : getCachedRelationNames())
		{
			int count = 0;

			Set<String> relationSet = new HashSet<>(getTermsViaPredicateSubsumption("subrelation", 2, relation, 1, true));
			relationSet.add(relation);

			Set<Formula> formulae = new HashSet<>();
			for (String value : relationSet)
			{
				List<Formula> forms = ask("arg", 0, value);
				if (forms != null)
					formulae.addAll(forms);
			}
			if (!formulae.isEmpty())
			{
				RelationCache c1 = getRelationCache(relation, 1, 2);
				RelationCache c2 = getRelationCache(relation, 2, 1);
				for (Formula f : formulae)
				{
					if ((f.text.indexOf("(", 2) == -1) && !f.sourceFile.endsWith(_cacheFileSuffix))
					{
						String arg1 = f.getArgument(1).intern();
						String arg2 = f.getArgument(2).intern();

						if (!arg1.isEmpty() && !arg2.isEmpty())
						{
							count += addRelationCacheEntry(c1, arg1, arg2);
							count += addRelationCacheEntry(c2, arg2, arg1);

							// symmetric
							if (symmetric.contains(relation))
							{
								count += addRelationCacheEntry(c1, arg2, arg1);
								count += addRelationCacheEntry(c2, arg1, arg2);
							}

							// reflexive
							if (getCacheReflexiveAssertions() && reflexive.contains(relation))
							{
								count += addRelationCacheEntry(c1, arg1, arg1);
								count += addRelationCacheEntry(c1, arg2, arg2);
								count += addRelationCacheEntry(c2, arg1, arg1);
								count += addRelationCacheEntry(c2, arg2, arg2);
							}
						}
					}
				}
			}
			// More ways of collecting implied disjointness assertions.
			if (relation.equals("disjoint"))
			{
				formulae.clear();
				List<Formula> partitions = ask("arg", 0, "partition");
				List<Formula> decompositions = ask("arg", 0, "disjointDecomposition");
				if (partitions != null)
					formulae.addAll(partitions);
				if (decompositions != null)
					formulae.addAll(decompositions);
				RelationCache c1 = getRelationCache(relation, 1, 2);
				for (Formula f : formulae)
				{
					if ((f.text.indexOf("(", 2) == -1) && !f.sourceFile.endsWith(_cacheFileSuffix))
					{
						List<String> args = f.argumentsToList(2);
						for (int i = 0; i < args.size(); i++)
						{
							for (int j = 0; j < args.size(); j++)
							{
								if (i != j)
								{
									String arg1 = args.get(i).intern();
									String arg2 = args.get(j).intern();
									if (!arg1.isEmpty() && !arg2.isEmpty())
									{
										count += addRelationCacheEntry(c1, arg1, arg2);
										count += addRelationCacheEntry(c1, arg2, arg1);
									}
								}
							}
						}
					}
				}
			}
			if (count > 0)
			{
				logger.finer(relation + ": " + count + " entries added for " + relationSet);
				total += count;
			}
		}
		logger.info("Total: " + total + " new cache entries computed");
		logger.exiting("KB", "cacheGroundAssertionsAndPredSubsumptionEntailments");
	}

	/**
	 * Converts all Formula objects in the input List to List
	 * tuples.
	 *
	 * @param formulaList A list of Formulas.
	 * @return An List of formula tuples (Lists), or an empty List.
	 */
	public static List<List<String>> formulasToLists(List<Formula> formulaList)
	{
		List<List<String>> result = new ArrayList<>();
		if (formulaList != null)
		{
			for (Formula f : formulaList)
			{
				result.add(f.literalToList());
			}
		}
		return result;
	}

	/**
	 * Converts all Strings in the input List to Formula objects.
	 *
	 * @param strings A list of Strings.
	 * @return An List of Formulas, or an empty List.
	 */
	public static List<Formula> stringsToFormulas(List<String> strings)
	{
		List<Formula> result = new ArrayList<>();
		if (strings != null)
		{
			for (String string : strings)
			{
				Formula f = new Formula();
				f.set(string);
				result.add(f);
			}
		}
		return result;
	}

	/**
	 * Converts a literal (List object) to a String.
	 *
	 * @param lit A List representing a SUO-KIF formula.
	 * @return A String representing a SUO-KIF formula.
	 */
	public static String literalListToString(List<String> lit)
	{
		StringBuilder sb = new StringBuilder();
		if (lit != null)
		{
			sb.append("(");
			for (int i = 0; i < lit.size(); i++)
			{
				if (i > 0)
				{
					sb.append(" ");
				}
				sb.append(lit.get(i));
			}
			sb.append(")");
		}
		return sb.toString();
	}

	/**
	 * Converts a literal (List object) to a Formula.
	 *
	 * @param lit literal
	 * @return A SUO-KIF Formula object, or null if no Formula can be
	 * created.
	 */
	public static Formula literalListToFormula(List<String> lit)
	{
		Formula f = null;
		String formulaStr = literalListToString(lit);
		if (!formulaStr.isEmpty())
		{
			f = new Formula();
			f.set(formulaStr);
		}
		return f;
	}

	/**
	 * Returns an List containing the terms (Strings) that
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
	 * @return An List of terms, or an empty List if no
	 * terms can be retrieved.
	 */
	public List<String> getTermsViaAskWithRestriction(int argnum1, String term1, int argnum2, String term2, int targetArgnum, Set<String> predicatesUsed)
	{
		List<String> result = new ArrayList<>();
		try
		{
			if (!term1.isEmpty() && !StringUtil.isQuotedString(term1) && !term2.isEmpty() && !StringUtil.isQuotedString(term2))
			{
				List<Formula> formulae = askWithRestriction(argnum1, term1, argnum2, term2);
				for (Formula f : formulae)
				{
					result.add(f.getArgument(targetArgnum));
				}
				if (predicatesUsed != null)
				{
					for (Formula f : formulae)
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
	 * Returns an List containing the terms (Strings) that
	 * correspond to targetArgnum in the Formulas obtained from the
	 * method call askWithRestriction(argnum1, term1, argnum2, term2).
	 *
	 * @param argnum1      number of args 1
	 * @param term1        term 1
	 * @param argnum2      number of args 2
	 * @param term2        term 2
	 * @param targetArgnum target     number of args
	 * @return An List of terms, or an empty List if no
	 * terms can be retrieved.
	 */
	public List<String> getTermsViaAskWithRestriction(int argnum1, String term1, int argnum2, String term2, int targetArgnum)
	{
		return getTermsViaAskWithRestriction(argnum1, term1, argnum2, term2, targetArgnum, null);
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
	public String getFirstTermViaAskWithRestriction(int argnum1, String term1, int argnum2, String term2, int targetArgnum)
	{
		String result = null;
		try
		{
			List<String> terms = getTermsViaAskWithRestriction(argnum1, term1, argnum2, term2, targetArgnum);
			if (!terms.isEmpty())
				result = terms.get(0);
		}
		catch (Exception ex)
		{
			logger.warning(Arrays.toString(ex.getStackTrace()));
			ex.printStackTrace();
		}
		return result;
	}

	/**
	 * Ask with restriction
	 *
	 * @param argnum1 number of args 1
	 * @param term1   term 1
	 * @param argnum2 number of args 2
	 * @param term2   term 2
	 * @return an List of Formulas in which the two terms
	 * provided appear in the indicated argument positions.  If there
	 * are no Formula(s) matching the given terms and respective
	 * argument positions, return an empty List.  Iterate
	 * through the smallest list of results.
	 */
	public List<Formula> askWithRestriction(int argnum1, String term1, int argnum2, String term2)
	{
		List<Formula> result = new ArrayList<>();
		try
		{
			if (!term1.isEmpty() && !term2.isEmpty())
			{
				List<Formula> partial1 = ask("arg", argnum1, term1);
				List<Formula> partial2 = ask("arg", argnum2, term2);
				List<Formula> partial = partial1;
				int arg = argnum2;
				String term = term2;
				if (partial1.size() > partial2.size())
				{
					partial = partial2;
					arg = argnum1;
					term = term1;
				}
				for (Formula f : partial)
				{
					if (f.getArgument(arg).equals(term))
						result.add(f);
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
	 * Returns an List of Formulas in which the two terms
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
	public List<Formula> askWithTwoRestrictions(int argnum1, String term1, int argnum2, String term2, int argnum3, String term3)
	{
		String[] args = new String[6];
		args[0] = "argnum1 = " + argnum1;
		args[1] = "term1 = " + term1;
		args[0] = "argnum2 = " + argnum2;
		args[1] = "term2 = " + term2;
		args[0] = "argnum3 = " + argnum3;
		args[1] = "term3 = " + term3;

		logger.entering("KB", "askWithTwoRestrictions", args);
		List<Formula> result = new ArrayList<>();
		if (!term1.isEmpty() && !term2.isEmpty() && !term3.isEmpty())
		{
			List<Formula> partialA = new ArrayList<>();           // will get the smallest list
			List<Formula> partial1 = ask("arg", argnum1, term1);
			List<Formula> partial2 = ask("arg", argnum2, term2);
			List<Formula> partial3 = ask("arg", argnum3, term3);
			int argB = -1;
			String termB = "";
			int argC = -1;
			String termC = "";
			if (partial1 == null || partial2 == null || partial3 == null)
				return result;
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
			for (Formula f : partialA)
			{
				if (f.getArgument(argB).equals(termB))
				{
					if (f.getArgument(argC).equals(termC))
						result.add(f);
				}
			}
		}
		logger.exiting("KB", "askWithTwoRestrictions", result);
		return result;
	}

	/**
	 * Returns an List containing the SUO-KIF terms that match the request.
	 *
	 * @param argnum1      number of args 1
	 * @param term1        term 1
	 * @param argnum2      number of args 2
	 * @param term2        term 2
	 * @param argnum3      number of args 3
	 * @param term3        term 3
	 * @param targetArgnum number of target number of args
	 * @return An List of terms, or an empty List if no matches can be found.
	 */
	public List<String> getTermsViaAWTR(int argnum1, String term1, int argnum2, String term2, int argnum3, String term3, int targetArgnum)
	{
		List<String> result = new ArrayList<>();
		List<Formula> formulae = askWithTwoRestrictions(argnum1, term1, argnum2, term2, argnum3, term3);
		for (Formula f : formulae)
		{
			result.add(f.getArgument(targetArgnum));
		}
		return result;
	}

	/**
	 * Returns an List containing the terms (Strings) that
	 * correspond to targetArgnum in the ground atomic Formulae in
	 * which knownArg is in the argument position knownArgnum.  The
	 * List returned will contain no duplicate terms.
	 *
	 * @param knownArgnum  The argument position of knownArg
	 * @param knownArg     The term that appears in the argument
	 *                     knownArgnum of the ground atomic Formulae in
	 *                     the KB
	 * @param targetArgnum The argument position of the terms being sought
	 * @return An List of Strings, which will be empty if no
	 * match found.
	 */
	public List<String> getTermsViaAsk(int knownArgnum, String knownArg, int targetArgnum)
	{
		List<String> result = new ArrayList<>();
		List<Formula> formulae = ask("arg", knownArgnum, knownArg);
		if (!formulae.isEmpty())
		{
			SortedSet<String> ts = new TreeSet<>();
			for (Formula f : formulae)
			{
				ts.add(f.getArgument(targetArgnum));
			}
			result.addAll(ts);
		}
		return result;
	}

	/**
	 * Returns an List containing the Formulas that match the request.
	 *
	 * @param kind   May be one of "ant", "cons", "stmt", or "arg"
	 * @param term   The term that appears in the statements being
	 *               requested.
	 * @param argnum The argument position of the term being asked
	 *               for.  The first argument after the predicate
	 *               is "1". This parameter is ignored if the kind
	 *               is "ant", "cons" or "stmt".
	 * @return An List of Formula(s), which will be empty if no match found.
	 */
	public List<Formula> ask(String kind, int argnum, String term)
	{
		List<Formula> result = new ArrayList<>();
		if (term == null || term.isEmpty())
		{
			String msg = "Error in KB.ask(\"" + kind + "\", " + argnum + ", \"" + term + "\"): " + "search term is null, or an empty string";
			logger.warning(msg);
			throw new IllegalArgumentException(msg);
		}
		if (term.length() > 1 && term.charAt(0) == '"' && term.charAt(term.length() - 1) == '"')
		{
			String msg = "Error in KB.ask(): Strings are not indexed.  No results for " + term;
			logger.warning(msg);
			throw new IllegalArgumentException(msg);
		}
		List<Formula> formulas;
		if (kind.equals("arg"))
			formulas = this.formulas.get(kind + "-" + argnum + "-" + term);
		else
			formulas = this.formulas.get(kind + "-" + term);
		if (formulas != null)
			result.addAll(formulas);
		return result;
	}

	/**
	 * Returns an List containing the Formulae retrieved,
	 * possibly via multiple asks that recursively use relation and
	 * all of its subrelations.  Note that the Formulas might be
	 * formed with different predicates, but all of the predicates
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
	 * @return an List of Formulas that satisfy the query, or an
	 * empty List if no Formulae are retrieved.
	 */
	public List<Formula> askWithPredicateSubsumption(String relation, int idxArgnum, String idxTerm)
	{
		List<Formula> result = new ArrayList<>();
		if (!relation.isEmpty() && !idxTerm.isEmpty() && (idxArgnum >= 0) /* && (idxArgnum < 7) */)
		{
			Set<String> done = new HashSet<>();
			Set<String> accumulator = new HashSet<>();
			List<String> relns = new ArrayList<>();
			relns.add(relation);
			while (!relns.isEmpty())
			{
				for (String reln : relns)
				{
					List<Formula> formulae = this.askWithRestriction(0, reln, idxArgnum, idxTerm);
					result.addAll(formulae);
					formulae = this.askWithRestriction(0, "subrelation", 2, reln);
					for (Formula f : formulae)
					{
						if (!done.contains(f.text))
						{
							String arg = f.getArgument(1);
							if (!reln.equals(arg))
							{
								accumulator.add(arg);
								done.add(f.text);
							}
						}
					}
				}
				relns.clear();
				relns.addAll(accumulator);
				accumulator.clear();
			}
			// Remove duplicates; perhaps not necessary.
			Set<Formula> ans2 = new HashSet<>(result);
			result.clear();
			result.addAll(ans2);
		}
		return result;
	}

	/**
	 * Returns an List containing SUO-KIF constants, possibly
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
	 * @return an List of terms (SUO-KIF constants), or an
	 * empty List if no terms can be retrieved
	 */
	public List<String> getTermsViaPredicateSubsumption(String relation, int idxArgnum, String idxTerm, int targetArgnum, boolean useInverses,
			Set<String> predicatesUsed)
	{
		List<String> result = new ArrayList<>();
		if (!relation.isEmpty() && !idxTerm.isEmpty() && (idxArgnum >= 0) /* && (idxArgnum < 7) */)
		{
			List<String> inverseSyns = null;
			List<String> inverses = null;
			if (useInverses)
			{
				inverseSyns = getTermsViaAskWithRestriction(0, "subrelation", 2, "inverse", 1);
				inverseSyns.addAll(getTermsViaAskWithRestriction(0, "equal", 2, "inverse", 1));
				inverseSyns.addAll(getTermsViaAskWithRestriction(0, "equal", 1, "inverse", 2));
				inverseSyns.add("inverse");
				SetUtil.removeDuplicates(inverseSyns);
				inverses = new ArrayList<>();
			}
			SortedSet<String> reduced = new TreeSet<>();
			List<String> accumulator = new ArrayList<>();
			List<String> predicates = new ArrayList<>();
			predicates.add(relation);
			while (!predicates.isEmpty())
			{
				for (String pred : predicates)
				{
					reduced.addAll(getTermsViaAskWithRestriction(0, pred, idxArgnum, idxTerm, targetArgnum, predicatesUsed));
					accumulator.addAll(getTermsViaAskWithRestriction(0, "subrelation", 2, pred, 1));
					accumulator.addAll(getTermsViaAskWithRestriction(0, "equal", 2, "subrelation", 1));
					accumulator.addAll(getTermsViaAskWithRestriction(0, "equal", 1, "subrelation", 2));
					accumulator.remove(pred);
					if (useInverses)
					{
						for (String syn : inverseSyns)
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
				for (String inv : inverses)
				{
					reduced.addAll(getTermsViaPredicateSubsumption(inv, targetArgnum, idxTerm, idxArgnum, false, predicatesUsed));
				}
			}
			result.addAll(reduced);
		}
		return result;
	}

	/**
	 * Returns an List containing SUO-KIF constants, possibly
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
	 * @return an List of terms (SUO-KIF constants), or an
	 * empty List if no terms can be retrieved
	 */
	public List<String> getTermsViaPredicateSubsumption(String relation, int idxArgnum, String idxTerm, int targetArgnum, boolean useInverses)
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
	public String getFirstTermViaPredicateSubsumption(String relation, int idxArgnum, String idxTerm, int targetArgnum, boolean useInverses)
	{
		String result = null;
		if (!relation.isEmpty() && !idxTerm.isEmpty() && (idxArgnum >= 0) /* && (idxArgnum < 7) */)
		{
			List<String> terms = getTermsViaPredicateSubsumption(relation, idxArgnum, idxTerm, targetArgnum, useInverses);
			if (!terms.isEmpty())
			{
				result = terms.get(0);
			}
		}
		return result;
	}

	/**
	 * Returns an List containing the transitive closure of
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
	 * @return an List of terms (SUO-KIF constants), or an
	 * empty List if no terms can be retrieved
	 */
	public List<String> getTransitiveClosureViaPredicateSubsumption(String relation, int idxArgnum, String idxTerm, int targetArgnum, boolean useInverses)
	{
		Set<String> reduced = new TreeSet<>();
		Set<String> accumulator = new TreeSet<>(getTermsViaPredicateSubsumption(relation, idxArgnum, idxTerm, targetArgnum, useInverses));
		List<String> working = new ArrayList<>();
		while (!accumulator.isEmpty())
		{
			reduced.addAll(accumulator);
			working.clear();
			working.addAll(accumulator);
			accumulator.clear();
			for (String term : working)
				accumulator.addAll(getTermsViaPredicateSubsumption(relation, idxArgnum, term, targetArgnum, useInverses));
		}
		return new ArrayList<>(reduced);
	}

	/**
	 * Takes a term and returns true if the term occurs in the KB.
	 *
	 * @param term A String.
	 * @return true or false.
	 */
	public boolean containsTerm(String term)
	{
		if (getTerms().contains(term.intern()))
		{
			return true;
		}
		else
			return getREMatch(term.intern()).size() == 1;
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
	 * Count the number of relations in the knowledge base in order to
	 * present statistics to the user.
	 *
	 * @return The integer number of relations in the knowledge base.
	 */
	public int getCountRelations()
	{
		return this.getAllInstances("Relation").size();
	}

	/**
	 * Count the number of formulas in the knowledge base in order to
	 * present statistics to the user.
	 *
	 * @return The integer number of formulas in the knowledge base.
	 */
	public int getCountAxioms()
	{
		return formulaMap.size();
	}

	/**
	 * An accessor providing a SortedSet of un-preProcessed String
	 * representations of Formulae.
	 *
	 * @return A SortedSet of Strings.
	 */
	public SortedSet<String> getFormulas()
	{
		return new TreeSet<>(formulaMap.keySet());
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
		for (Formula f : formulaMap.values())
		{
			if (f.isRule())
			{
				count++;
			}
		}
		return count;
	}

	/**
	 * Create an List of the specific size, filled with empty strings.
	 *
	 * @return list of empty strings.
	 */
	private List<String> listWithBlanks(int size)
	{
		List<String> al = new ArrayList<>(size);
		for (int i = 0; i < size; i++)
			al.add("");
		return al;
	}

	/**
	 * Get the alphabetically nearest terms to the given term, which
	 * is not in the KB.  Elements 0-(k-1) should be alphabetically
	 * lesser and k-(2*k-1) alphabetically greater.  If the term is
	 * at the beginning or end of the alphabet, fill in blank items
	 * with the empty string: "".
	 *
	 * @return alphabetically nearest terms to the given term, which is not in the KB.
	 */
	private List<String> getNearestKTerms(String term, @SuppressWarnings("SameParameterValue") int k)
	{
		List<String> al;
		if (k == 0)
			al = listWithBlanks(1);
		else
			al = listWithBlanks(2 * k);

		String[] t = getTerms().toArray(new String[0]);
		int i = 0;
		while (i < t.length - 1 && t[i].compareTo(term) < 0)
			i++;
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
	private List<String> getNearestTerms(String term)
	{
		return getNearestKTerms(term, 15);
	}

	/**
	 * Get the neighbors of this initial uppercase term (class or function).
	 *
	 * @param term term
	 * @return nearest relations
	 */
	public List<String> getNearestRelations(String term)
	{
		term = Character.toUpperCase(term.charAt(0)) + term.substring(1);
		return getNearestTerms(term);
	}

	/**
	 * Get the neighbors of this initial lowercase term (relation).
	 *
	 * @param term term
	 * @return nearest non relations
	 */
	public List<String> getNearestNonRelations(String term)
	{
		term = Character.toLowerCase(term.charAt(0)) + term.substring(1);
		return getNearestTerms(term);
	}

	/**
	 * This List is used to limit the number of warning messages
	 * logged by loadFormatMaps(lang).  If an attempt to load format
	 * or termFormat values for lang is unsuccessful, the list is
	 * checked for the presence of lang.  If lang is not in the list,
	 * a warning message is logged and lang is added to the list.  The
	 * list is cleared whenever a constituent file is added or removed
	 * for KB, since the latter might affect the availability of
	 * format or termFormat values.
	 */
	protected final List<String> loadFormatMapsAttempted = new ArrayList<>();

	/**
	 * Populates the format maps for language lang.
	 *
	 * @param lang language
	 */
	protected void loadFormatMaps(String lang)
	{
		try
		{
			if (formatMap == null)
				formatMap = new HashMap<>();
			if (termFormatMap == null)
				termFormatMap = new HashMap<>();
			formatMap.computeIfAbsent(lang, k -> new HashMap<>());
			termFormatMap.computeIfAbsent(lang, k -> new HashMap<>());

			if (!loadFormatMapsAttempted.contains(lang))
			{
				List<Formula> col = askWithRestriction(0, "format", 1, lang);
				if ((col == null) || col.isEmpty())
				{
					logger.warning("No relation format file loaded for language " + lang);
				}
				else
				{
					Map<String, String> langFormatMap = formatMap.get(lang);
					for (Formula f : col)
					{
						String key = f.getArgument(2);
						String format = f.getArgument(3);
						format = StringUtil.removeEnclosingQuotes(format);
						langFormatMap.put(key, format);
					}
				}
				col = askWithRestriction(0, "termFormat", 1, lang);
				if ((col == null) || col.isEmpty())
				{
					logger.warning("No term format file loaded for language: " + lang);
				}
				else
				{
					Map<String, String> langTermFormatMap = termFormatMap.get(lang);
					for (Formula f : col)
					{
						String key = f.getArgument(2);
						String format = f.getArgument(3);
						format = StringUtil.removeEnclosingQuotes(format);
						langTermFormatMap.put(key, format);
					}
				}
				loadFormatMapsAttempted.add(lang);
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
		try
		{
			Map<String, String> m;
			if (formatMap != null)
			{
				for (Map<String, String> stringStringMap : formatMap.values())
				{
					m = stringStringMap;
					if (m != null)
						m.clear();
				}
				formatMap.clear();
			}
			if (termFormatMap != null)
			{
				for (Map<String, String> stringStringMap : termFormatMap.values())
				{
					m = stringStringMap;
					if (m != null)
						m.clear();
				}
				termFormatMap.clear();
			}
			loadFormatMapsAttempted.clear();
		}
		catch (Exception ex)
		{
			logger.warning(Arrays.toString(ex.getStackTrace()));
			ex.printStackTrace();
		}
	}

	/**
	 * This method creates a dictionary (Map) of SUO-KIF term symbols
	 * -- the keys -- and a natural language string for each key that
	 * is the preferred name for the term -- the values -- in the
	 * context denoted by lang.  If the Map has already been built and
	 * the language hasn't changed, just return the existing map.
	 * This is a case of "lazy evaluation".
	 *
	 * @param lang language
	 * @return An instance of Map where the keys are terms and the
	 * values are format strings.
	 */
	public Map<String, String> getTermFormatMap(String lang)
	{
		if (lang == null || lang.isEmpty())
		{
			lang = "EnglishLanguage";
		}
		if ((termFormatMap == null) || termFormatMap.isEmpty())
		{
			loadFormatMaps(lang);
		}
		Map<String, String> langTermFormatMap = termFormatMap.get(lang);
		if ((langTermFormatMap == null) || langTermFormatMap.isEmpty())
		{
			loadFormatMaps(lang);
		}
		return termFormatMap.get(lang);
	}

	/**
	 * This method creates an association list (Map) of the natural
	 * language format string and the relation name for which that
	 * format string applies.  If the map has already been built and
	 * the language hasn't changed, just return the existing map.
	 * This is a case of "lazy evaluation".
	 *
	 * @param lang language
	 * @return An instance of Map where the keys are relation names
	 * and the values are format strings.
	 */
	public Map<String, String> getFormatMap(String lang)
	{
		logger.entering("KB", "getFormatMap", "lang = " + lang);
		if (lang == null || lang.isEmpty())
		{
			lang = "EnglishLanguage";
		}
		if ((formatMap == null) || formatMap.isEmpty())
		{
			loadFormatMaps(lang);
		}
		Map<String, String> langFormatMap = formatMap.get(lang);
		if ((langFormatMap == null) || langFormatMap.isEmpty())
		{
			loadFormatMaps(lang);
		}
		logger.exiting("KB", "getFormatMap", formatMap.get(lang));
		return formatMap.get(lang);
	}

	/**
	 * Add a new KB constituent by reading in the file, and then
	 * merging the formulas with the existing set of formulas.  All
	 * assertion caches are rebuilt, the current Vampire process is
	 * destroyed, and a new one is created.
	 *
	 * @param filename - the full path of the file being added.
	 */
	public void addConstituent(String filename)
	{
		addConstituent(filename, true, true);
	}

	/**
	 * Add a new KB constituent by reading in the file, and then merging
	 * the formulas with the existing set of formulas.
	 *
	 * @param filename     - The full path of the file being added
	 * @param buildCachesP - If true, forces the assertion caches to be rebuilt
	 * @param performArity - If true, perform arity check
	 */
	public void addConstituent(String filename, boolean buildCachesP, boolean performArity)
	{
		if (logger.isLoggable(Level.FINER))
		{
			String[] params = { "filename = " + filename, "buildCachesP = " + buildCachesP, "performArity = " + performArity };
			logger.entering("KB", "addConstituent", params);
		}
		try
		{
			// Should be called prior to param passing
			//if (filename.endsWith(".owl") || filename.endsWith(".OWL") || filename.endsWith(".rdf") || filename.endsWith(".RDF"))
			//{
			//	OWLTranslator.read(filename);
			//	filename = filename + ".kif";
			//}
			File constituent = new File(filename);
			String canonicalPath = constituent.getCanonicalPath();
			KIF file = new KIF();

			if (constituents.contains(canonicalPath))
				errors.add("Error: " + canonicalPath + " already loaded.");
			logger.info("Adding " + canonicalPath + " to KB.");
			try
			{
				file.readFile(canonicalPath);
				errors.addAll(file.warningSet);
			}
			catch (Exception ex1)
			{
				StringBuilder error = new StringBuilder();
				error.append(ex1.getMessage());
				if (ex1 instanceof ParseException)
					error.append(" at line ").append(((ParseException) ex1).getErrorOffset());
				error.append(" in file ").append(canonicalPath);
				logger.severe(error.toString());
				errors.add(error.toString());
			}

			logger.info("Parsed file " + canonicalPath + " containing " + file.formulas.keySet().size() + " KIF expressions");
			int count = 0;
			for (String key : file.formulas.keySet())
			{
				// Iterate through the formulas in the file, adding them to the KB, at the appropriate key.
				// Note that this is a slow operation that needs to be improved
				List<Formula> list = formulas.computeIfAbsent(key, k -> new ArrayList<>());
				List<Formula> newList = file.formulas.get(key);
				for (Formula f : newList)
				{
					boolean correctArity = true;
					if (performArity)
					{
						if (!f.hasCorrectArity(this))
						{
							errors.add("The following formula rejected for incorrect arity: " + f.text);
							System.err.println("ERROR Formula rejected for incorrect arity: " + f.text);
							correctArity = false;
						}
					}
					if (correctArity)
					{
						String internedFormula = f.text.intern();
						if (!list.contains(f))
						{
							list.add(f);
							formulaMap.put(internedFormula, f);
						}
						else
						{
							StringBuilder error = new StringBuilder();
							error.append("WARNING: Duplicate axiom in ");
							error.append(f.sourceFile).append(" at line ").append(f.startLine).append("<br />");
							error.append(f.text).append("<p>");
							Formula existingFormula = formulaMap.get(internedFormula);
							error.append("WARNING: Existing formula appears in ");
							error.append(existingFormula.sourceFile).append(" at line ").append(existingFormula.startLine).append("<br />");
							error.append("<p>");
							System.err.println("WARNING: Duplicate detected.");
							errors.add(error.toString());
						}
					}
				}
				if ((count++ % 100) == 1)
				{
					System.out.print(".");
				}
			}

			synchronized (this.getTerms())
			{
				this.getTerms().addAll(file.terms);
			}
			if (!constituents.contains(canonicalPath))
				constituents.add(canonicalPath);
			logger.info("File " + canonicalPath + " loaded");

			// Clear the formatMap and termFormatMap for this KB.
			clearFormatMaps();
			if (buildCachesP && !canonicalPath.endsWith(_cacheFileSuffix))
				buildRelationCaches();
		}
		catch (Exception ex)
		{
			logger.severe(ex.getMessage() + "; \nStack Trace: " + Arrays.toString(ex.getStackTrace()));
		}

		logger.exiting("KB", "addConstituent", "Constituent " + filename + "successfully added to KB: " + this.name);

	}

	/**
	 * A Map for holding compiled regular expression patterns.
	 * The map is initialized by calling compilePatterns().
	 */
	private static Map<String, List<Object>> REGEX_PATTERNS = null;

	/**
	 * This method returns a compiled regular expression Pattern
	 * object indexed by key.
	 *
	 * @param key A String that is the retrieval key for a compiled
	 *            regular expression Pattern.
	 * @return A compiled regular expression Pattern instance.
	 */
	public static Pattern getCompiledPattern(String key)
	{
		if (!key.isEmpty() && (REGEX_PATTERNS != null))
		{
			List<Object> al = REGEX_PATTERNS.get(key);
			if (al != null)
				return (Pattern) al.get(0);
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
	public static int getPatternGroupIndex(String key)
	{
		if (!key.isEmpty() && (REGEX_PATTERNS != null))
		{
			List<Object> al = REGEX_PATTERNS.get(key);
			if (al != null)
			{
				return (Integer) al.get(1);
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
			String[][] patternArray = { { "row_var", "\\@ROW\\d*", "0" },
					// { "open_lit", "\\(\\w+\\s+\\?\\w+\\s+.\\w+\\s*\\)", "0" },
					{ "open_lit", "\\(\\w+\\s+\\?\\w+[a-zA-Z_0-9-?\\s]+\\)", "0" }, { "pred_var_1", "\\(holds\\s+(\\?\\w+)\\W", "1" },
					{ "pred_var_2", "\\((\\?\\w+)\\W", "1" }, { "var_with_digit_suffix", "(\\D+)\\d*", "1" } };
			for (String[] strings : patternArray)
			{
				String pName = strings[0];
				Pattern p = Pattern.compile(strings[1]);
				Integer groupN = Integer.parseInt(strings[2]);
				List<Object> pVal = new ArrayList<>();
				pVal.add(p);
				pVal.add(groupN);
				REGEX_PATTERNS.put(pName, pVal);
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
	 * @param input       The input String in which matches are sought.
	 * @param patternKey  A String used as the retrieval key for a
	 *                    regular expression Pattern object, and an int index identifying
	 *                    a binding group.
	 * @param accumulator An optional List to which matches are
	 *                    added.  Note that if accumulator is provided, it will be the
	 *                    return value even if no new matches are found in the input
	 *                    String.
	 * @return An List, or null if no matches are found and an
	 * accumulator is not provided.
	 */
	public static List<String> getMatches(String input, String patternKey, List<String> accumulator)
	{
		List<String> result = null;
		if (accumulator != null)
			result = accumulator;
		if (REGEX_PATTERNS == null)
			KB.compilePatterns();
		if (!input.isEmpty() && !patternKey.isEmpty())
		{
			Pattern p = KB.getCompiledPattern(patternKey);
			if (p != null)
			{
				Matcher m = p.matcher(input);
				int gIdx = KB.getPatternGroupIndex(patternKey);
				if (gIdx >= 0)
				{
					while (m.find())
					{
						String rv = m.group(gIdx);
						if (!rv.isEmpty())
						{
							if (result == null)
							{
								result = new ArrayList<>();
							}
							if (!(result.contains(rv)))
							{
								result.add(rv);
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
	 * patternKey, and returns the results, if any, in an List.
	 *
	 * @param input      The input String in which matches are sought.
	 * @param patternKey A String used as the retrieval key for a
	 *                   regular expression Pattern object, and an int index identifying
	 *                   a binding group.
	 * @return An List, or null if no matches are found.
	 */
	public static List<String> getMatches(String input, String patternKey)
	{
		return KB.getMatches(input, patternKey, null);
	}

	/**
	 * This method retrieves Formulas by asking the query expression
	 * queryLit, and returns the results, if any, in an List.
	 *
	 * @param queryLit The query, which is assumed to be a List
	 *                 (atomic literal) consisting of a single predicate and its
	 *                 arguments.  The arguments could be variables, constants, or a
	 *                 mix of the two, but only the first constant encountered in a
	 *                 left to right sweep over the literal will be used in the actual
	 *                 query.
	 * @return An List of Formula objects, or an empty List
	 * if no answers are retrieved.
	 */
	public List<Formula> askWithLiteral(List<String> queryLit)
	{
		List<Formula> result = new ArrayList<>();
		if ((queryLit != null) && !(queryLit.isEmpty()))
		{
			String pred = queryLit.get(0);
			if (pred.equals("instance") && isVariable(queryLit.get(1)) && !(isVariable(queryLit.get(2))))
			{
				String className = queryLit.get(2);
				Set<String> ai = getAllInstances(className);
				for (String inst : ai)
				{
					String fStr = "(instance " + inst + " " + className + ")";
					Formula f = new Formula();
					f.set(fStr);
					result.add(f);
				}
			}
			else if (pred.equals("valence") && isVariable(queryLit.get(1)) && isVariable(queryLit.get(2)))
			{
				Set<String> ai = getAllInstances("Relation");
				for (String inst : ai)
				{
					int valence = getValence(inst);
					if (valence > 0)
					{
						String fStr = "(valence " + inst + " " + valence + ")";
						Formula f = new Formula();
						f.set(fStr);
						result.add(f);
					}
				}
			}
			else
			{
				String constant = null;
				int cIdx = -1;
				int qlLen = queryLit.size();
				for (int i = 1; i < qlLen; i++)
				{
					String term = queryLit.get(i);
					if (!term.isEmpty() && !isVariable(term))
					{
						constant = term;
						cIdx = i;
						break;
					}
				}
				if (constant != null)
					result = askWithRestriction(cIdx, constant, 0, pred);
				else
					result = ask("arg", 0, pred);
			}
		}
		return result;
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
	public Set<String> getAllSuperClasses(Set<String> classNames)
	{
		Set<String> result = new HashSet<>();
		if ((classNames != null) && !(classNames.isEmpty()))
		{
			List<String> accumulator = new ArrayList<>();
			List<String> working = new ArrayList<>(classNames);
			while (!(working.isEmpty()))
			{
				for (int i = 0; i < working.size(); i++)
				{
					List<Formula> nextLits = askWithRestriction(1, working.get(i), 0, "subclass");
					if (nextLits != null)
					{
						for (Formula f : nextLits)
						{
							String arg2 = f.getArgument(2);
							if (!working.contains(arg2))
							{
								accumulator.add(arg2);
							}
						}
					}
				}
				result.addAll(accumulator);
				working.clear();
				working.addAll(accumulator);
				accumulator.clear();
			}
		}
		return result;
	}

	/**
	 * This method retrieves all subclasses of className, using both
	 * class and predicate (subrelation) subsumption.
	 *
	 * @param className The name of a Class.
	 * @return A Set of terms (string constants), which could be
	 * empty.
	 */
	public Set<String> getAllSubClassesWithPredicateSubsumption(String className)
	{
		Set<String> result = new TreeSet<>();
		if (!className.isEmpty())
		{
			// Get all subrelations of subrelation.
			Set<String> metarelations = getCachedRelationValues("subrelation", "subrelation", 2, 1);
			metarelations.add("subrelation");

			Set<String> relations = new HashSet<>();

			// Get all subrelations of subclass.
			for (String pred : metarelations)
			{
				relations.addAll(getCachedRelationValues(pred, "subclass", 2, 1));
			}
			relations.add("subclass");

			// Get all subclasses of className.
			for (String pred : relations)
			{
				result.addAll(getCachedRelationValues(pred, className, 2, 1));
			}
		}
		return result;
	}

	/**
	 * This method retrieves all superclasses of className, using both
	 * class and predicate (subrelation) subsumption.
	 *
	 * @param className The name of a Class.
	 * @return A Set of terms (string constants), which could be
	 * empty.
	 */
	public Set<String> getAllSuperClassesWithPredicateSubsumption(String className)
	{
		Set<String> result = new TreeSet<>();
		if (!className.isEmpty())
		{
			Set<String> relations = new HashSet<>();

			// Get all subrelations of subrelation.
			Set<String> metarelations = getCachedRelationValues("subrelation", "subrelation", 2, 1);
			metarelations.add("subrelation");

			// Get all subrelations of subclass.
			for (String pred : metarelations)
			{
				relations.addAll(getCachedRelationValues(pred, "subclass", 2, 1));
			}
			relations.add("subclass");

			// Get all superclasses of className.
			for (String pred : relations)
			{
				result.addAll(getCachedRelationValues(pred, className, 1, 2));
			}
		}
		return result;
	}

	/**
	 * This method retrieves all instances of className, using both
	 * predicate (subrelation) and class subsumption.
	 *
	 * @param className The name of a Class
	 * @return A Set of terms (string constants), which could be
	 * empty
	 */
	public Set<String> getAllInstancesWithPredicateSubsumption(String className)
	{
		return getAllInstancesWithPredicateSubsumption(className, true);
	}

	/**
	 * This method retrieves all instances of className, using
	 * predicate (subrelation) subsumption if gatherSubclasses is
	 * false, and using both predicate and subclass subsumption if
	 * gatherSubclasses is true.
	 *
	 * @param className        The name of a Class
	 * @param gatherSubclasses If true, all subclasses of className
	 *                         are gathered and their local instances are added to the set of
	 *                         returned terms
	 * @return A Set of terms (string constants), which could be
	 * empty
	 */
	public Set<String> getAllInstancesWithPredicateSubsumption(String className, boolean gatherSubclasses)
	{
		Set<String> result = new TreeSet<>();
		if (!className.isEmpty())
		{
			// Get all subrelations of subrelation.
			Set<String> metarelations = getCachedRelationValues("subrelation", "subrelation", 2, 1);
			metarelations.add("subrelation");

			Set<String> relations = new HashSet<>();

			// Get all subrelations of instance.
			for (String metarelation : metarelations)
			{
				relations.addAll(getCachedRelationValues(metarelation, "instance", 2, 1));
			}
			relations.add("instance");

			// Get all "local" or "immediate" instances of className, using instance and all gathered subrelations of instance.
			for (String relation : relations)
			{
				result.addAll(getCachedRelationValues(relation, className, 2, 1));
			}

			if (gatherSubclasses)
			{
				Set<String> subclasses = getAllSubClassesWithPredicateSubsumption(className);
				// subclasses.add(className);
				for (String subclass : subclasses)
				{
					for (String relation : relations)
					{
						result.addAll(getTermsViaAskWithRestriction(0, relation, 2, subclass, 1));
					}
				}
			}
		}
		return result;
	}

	/**
	 * This method retrieves all classes of which term is an instance,
	 * using both class and predicate (subrelation) subsumption.
	 *
	 * @param term The name of a SUO-KIF term.
	 * @return A Set of terms (class names), which could be
	 * empty.
	 */
	public Set<String> getAllInstanceOfsWithPredicateSubsumption(String term)
	{
		Set<String> result = new TreeSet<>();
		if (!term.isEmpty())
		{
			// Get all subrelations of subrelation.
			Set<String> metarelations = getCachedRelationValues("subrelation", "subrelation", 2, 1);
			metarelations.add("subrelation");
			Set<String> relations = new HashSet<>();

			// Get all subrelations of instance.
			for (String pred : metarelations)
			{
				relations.addAll(getCachedRelationValues(pred, "instance", 2, 1));
			}
			relations.add("instance");

			// Get all classes of which term is an instance.
			Set<String> classes = new HashSet<>();
			for (String pred : relations)
			{
				classes.addAll(getCachedRelationValues(pred, term, 1, 2));
			}
			result.addAll(classes);

			// Get all superclasses of classes.
			for (String cl : classes)
			{
				result.addAll(getAllSuperClassesWithPredicateSubsumption(cl));
			}
		}
		return result;
	}

	/**
	 * This method retrieves the downward transitive closure of all Class
	 * names contained in the input set.  The members of the input set are
	 * not included in the result set.
	 *
	 * @param classNames A Set object containing SUO-KIF class names (Strings).
	 * @return A Set of SUO-KIF class names, which could be empty.
	 */
	private Set<String> getAllSubClasses(Set<String> classNames)
	{
		Set<String> result = new HashSet<>();
		if (classNames != null && !classNames.isEmpty())
		{
			List<String> accumulator = new ArrayList<>();
			List<String> working = new ArrayList<>(classNames);
			while (!(working.isEmpty()))
			{
				for (int i = 0; i < working.size(); i++)
				{
					List<Formula> nextLits = askWithRestriction(2, working.get(i), 0, "subclass");
					if (nextLits != null)
					{
						for (Formula f : nextLits)
						{
							String arg1 = f.getArgument(1);
							if (!working.contains(arg1))
								accumulator.add(arg1);
						}
					}
				}
				result.addAll(accumulator);
				working.clear();
				working.addAll(accumulator);
				accumulator.clear();
			}
		}
		return result;
	}

	/**
	 * This method retrieves the downward transitive closure of all Class
	 * names contained in the input set.  The members of the input set are
	 * not included in the result set.
	 *
	 * @param className A String containing a SUO-KIF class name
	 * @return A Set of SUO-KIF class names, which could be empty.
	 */
	public Set<String> getAllSubClasses(String className)
	{
		Set<String> hs = new HashSet<>();
		hs.add(className);
		return getAllSubClasses(hs);
	}

	/**
	 * This method retrieves all instances of the classes named in the
	 * input set.
	 *
	 * @param classNames A Set object containing SUO-KIF class names
	 *                   (Strings).
	 * @return A SortedSet, possibly empty, containing SUO-KIF constant names.
	 */
	protected SortedSet<String> getAllInstances(Set<String> classNames)
	{
		SortedSet<String> result = new TreeSet<>();
		if ((classNames != null) && !classNames.isEmpty())
		{
			for (String className : classNames)
			{
				result.addAll(getCachedRelationValues("instance", className, 2, 1));
			}
		}
		return result;
	}

	/**
	 * This method retrieves all instances of the class named in the
	 * input String.
	 *
	 * @param className The name of a SUO-KIF Class.
	 * @return A SortedSet, possibly empty, containing SUO-KIF constant names.
	 */
	public SortedSet<String> getAllInstances(String className)
	{
		if (!className.isEmpty())
		{
			SortedSet<String> input = new TreeSet<>();
			input.add(className);
			return getAllInstances(input);
		}
		return new TreeSet<>();
	}

	/**
	 * This method tries to find or compute a valence for the input
	 * relation.
	 *
	 * @param relnName A String, the name of a SUO-KIF Relation.
	 * @return An int value. -1 means that no valence value could be
	 * found.  0 means that the relation is a VariableArityRelation.
	 * 1-5 are the standard SUO-KIF valence values.
	 */
	public int getValence(String relnName)
	{
		int result = -1;
		try
		{
			if (!relnName.isEmpty())
			{
				// First, see if the valence has already been cached.
				int[] rv = relationValences.get(relnName);
				if (rv != null)
				{
					result = rv[0];
					return result;
				}

				// Grab all of the superrelations too, since we have already computed them.
				Set<String> relnSet = getCachedRelationValues("subrelation", relnName, 1, 2);
				relnSet.add(relnName);
				for (String relation : relnSet)
				{
					if (result >= 0)
						break;
					// First, check to see if the KB actually contains an explicit valence value.  This is unlikely.
					List<Formula> literals = askWithRestriction(1, relation, 0, "valence");
					if (literals != null && !literals.isEmpty())
					{
						Formula f = literals.get(0);
						String digit = f.getArgument(2);
						if (!digit.isEmpty())
						{
							result = Integer.parseInt(digit);
							if (result >= 0)
							{
								break;
							}
						}
					}
					// See which valence-determining class the relation belongs to.
					Set<String> classNames = getCachedRelationValues("instance", relation, 1, 2);
					if (classNames != null)
					{
						String[][] tops = { { "VariableArityRelation", "0" }, { "UnaryFunction", "1" }, { "BinaryRelation", "2" }, { "TernaryRelation", "3" },
								{ "QuaternaryRelation", "4" }, { "QuintaryRelation", "5" }, };
						for (int i = 0; i < tops.length; i++)
						{
							if (classNames.contains(tops[i][0]))
							{
								result = Integer.parseInt(tops[i][1]);
								// The kluge below is to deal with the fact that a function, by definition, has a valence
								// one less than the corresponding predicate.  An instance of TernaryRelation that is also an instance
								// of Function has a valence of 2, not 3.
								if (i > 1 && (relation.endsWith("Fn") || classNames.contains("Function")) && !(tops[i][0]).endsWith("Function"))
								{
									--result;
								}
								break;
							}
						}
					}
				}
				// Cache the answer, if there is one.
				if (result >= 0)
				{
					int[] rv2 = new int[1];
					rv2[0] = result;
					relationValences.put(relnName, rv2);
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
	 * Collect predicates
	 *
	 * @return an List containing all predicates in this KB.
	 */
	public List<String> collectPredicates()
	{
		return new ArrayList<>(getCachedRelationValues("instance", "Predicate", 2, 1));
	}

	/**
	 * A static utility method.
	 *
	 * @param obj Presumably, a String.
	 * @return true if obj is a SUO-KIF variable, else false.
	 */
	public static boolean isVariable(String obj)
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
	public static boolean isQuantifier(String obj)
	{
		return (!obj.isEmpty() && (obj.equals("forall") || obj.equals("exists")));
	}

	/**
	 * Write Prolog formula
	 */
	private void writePrologFormulas(List<Formula> forms, PrintWriter pr)
	{
		try
		{
			SortedSet<Formula> ts = new TreeSet<>(forms);
			for (Formula f : ts)
			{
				String result = f.toProlog();
				if (result != null && !result.isEmpty())
					pr.println(result);
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
	public void writeProlog(PrintStream ps)
	{
		try (PrintWriter pr = new PrintWriter(ps))
		{
			logger.info("Writing ");

			pr.println("% Copyright (c) 2006-2009 Articulate Software Incorporated");
			pr.println("% This software released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.");
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

	/**
	 * Instances of RelationCache hold the cached extensions and, when
	 * possible, the computed closures, of selected relations.
	 * Canonical examples are the caches for subclass and instance.
	 */
	static class RelationCache extends HashMap<String, Set<String>>
	{
		private static final long serialVersionUID = 4096365216833534082L;

		private final String relationName;

		public String getRelationName()
		{
			return relationName;
		}

		private final int keyArgument;

		public int getKeyArgument()
		{
			return keyArgument;
		}

		private final int valueArgument;

		public int getValueArgument()
		{
			return valueArgument;
		}

		boolean closureComputed;

		public void setIsClosureComputed()
		{
			closureComputed = true;
		}

		public boolean getIsClosureComputed()
		{
			return closureComputed;
		}

		public RelationCache(String predName, int keyArg, int valueArg)
		{
			relationName = predName;
			keyArgument = keyArg;
			valueArgument = valueArg;
		}
	}

	/**
	 * Pretty print
	 *
	 * @param term term
	 * @return pretty-printed term
	 */
	public String prettyPrint(String term)
	{
		if (term.endsWith("Fn"))
			term = term.substring(0, term.length() - 2);

		StringBuilder result = new StringBuilder();
		for (int i = 0; i < term.length(); i++)
		{
			if (Character.isLowerCase(term.charAt(i)) || !Character.isLetter(term.charAt(i)))
				result.append(term.charAt(i));
			else
			{
				if (i + 1 < term.length() && Character.isUpperCase(term.charAt(i + 1)))
					result.append(term.charAt(i));
				else
				{
					if (i != 0)
						result.append(" ");
					result.append(Character.toLowerCase(term.charAt(i)));
				}
			}
		}
		return result.toString();
	}

	/**
	 * Replace variables in a formula with "gensym" constants.
	 *
	 * @param pre        formula
	 * @param assertions assertions formulae
	 */
	public void instantiateFormula(Formula pre, List<Formula> assertions)
	{
		logger.info("pre = " + pre);
		Tuple.Pair<List<String>, List<String>> al = pre.collectVariables();
		List<String> vars = new ArrayList<>();
		vars.addAll(al.first);
		vars.addAll(al.second);
		logger.fine("vars = " + vars);
		SortedMap<String, String> m = new TreeMap<>();
		for (String var : vars)
			m.put(var, "gensym" + genSym++);
		logger.fine("m = " + m);
		pre = pre.substituteVariables(m);
		assertions.add(pre);
	}
}
