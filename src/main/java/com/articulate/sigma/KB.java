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

import com.articulate.sigma.noncore.Types;

import java.io.Serializable;
import java.util.*;
import java.util.logging.Logger;

/**
 * Contains methods for reading, writing knowledge bases and their
 * configurations.
 */
public class KB extends BaseKB implements KBIface, Serializable
{
	private static final long serialVersionUID = 1L;

	private static final String LOG_SOURCE = "KB";

	private static final Logger logger = Logger.getLogger(KB.class.getName());


	/**
	 * Instances of RelationCache hold the cached extensions and, when
	 * possible, the computed closures, of selected relations.
	 * Canonical examples are the caches for subclass and instance.
	 */
	static class RelationCache extends HashMap<String, Set<String>>
	{
		private static final long serialVersionUID = 4096365216833534082L;

		private final String relationName;

		private final int keyArgument;

		private final int valueArgument;

		private boolean closureComputed;

		public RelationCache(String predName, int keyArg, int valueArg)
		{
			relationName = predName;
			keyArgument = keyArg;
			valueArgument = valueArg;
		}

		public String getRelationName()
		{
			return relationName;
		}

		public int getKeyArgument()
		{
			return keyArgument;
		}

		public int getValueArgument()
		{
			return valueArgument;
		}

		public void setIsClosureComputed()
		{
			closureComputed = true;
		}

		public boolean isClosureComputed()
		{
			return closureComputed;
		}
	}

	/**
	 * Perform arity check when adding constituent
	 */
	private static final boolean PERFORM_ARITY = true;

	/**
	 * A List of the names of cached relations.
	 */
	private static final List<String> RELNS = List.of("instance", "disjoint");

	/**
	 * A List of the names of cached transitive relations.
	 */
	private static final List<String> TRANSITIVE_RELNS = List.of("subclass", "subset", "subrelation", "subAttribute", "subOrganization", "subCollection", "subProcess", "geographicSubregion", "geopoliticalSubdivision");

	/**
	 * A List of the names of cached reflexive relations.
	 */
	private static final List<String> REFLEXIVE_RELNS = List.of("subclass", "subset", "subrelation", "subAttribute", "subOrganization", "subCollection", "subProcess");

	/**
	 * This list contains the names of SUMO Relations known to be
	 * instances of VariableArityRelation in at least some domain.  It
	 * is used only for TPTP generation, and should
	 * <strong>not</strong> be relied upon for any other purpose,
	 * since it is not automatically generated and might be out of
	 * date.
	 */
	public static final List<String> VA_RELNS = Arrays.asList( //
			"AssignmentFn", "GreatestCommonDivisorFn", "LatitudeFn", "LeastCommonMultipleFn", "ListFn", "LongitudeFn", "contraryAttribute", "disjointDecomposition", "exhaustiveAttribute", "exhaustiveDecomposition", "partition", "processList");

	/**
	 * A threshold limiting the number of values that will be added to a single relation cache table.
	 */
	private static final long MAX_CACHE_SIZE = 1000000;

	/**
	 * The String constant that is the suffix for files of cached assertions.
	 */
	public static final String _cacheFileSuffix = "_Cache.kif";

	/**
	 * A Map of Sets, which contain all the parent classes of a given class.
	 */
	@Nullable
	public Map<String, Set<String>> parents = new HashMap<>();

	/**
	 * A Map of Sets, which contain all the child classes of a given class.
	 */
	@Nullable
	public Map<String, Set<String>> children = new HashMap<>();

	/**
	 * A Map of Sets, which contain all the disjoint classes of a given class.
	 */
	@Nullable
	public Map<String, Set<String>> disjoint = new HashMap<>();

	/**
	 * Relations with args
	 */
	@Nullable
	private Map<String, boolean[]> relnsWithRelnArgs = null;

	/**
	 * A List of RelationCache objects.
	 */
	private final List<RelationCache> relationCaches = new ArrayList<>();

	/**
	 * Relation valences
	 */
	private final Map<String, int[]> relationValences = new HashMap<>();

	/**
	 * If true, assertions of the form (predicate x x) will be included in the relation cache tables.
	 */
	public final boolean cacheReflexiveAssertions = false;

	// C O N S T R U C T O R

	/**
	 * Constructor (for deserialization)
	 */
	protected KB()
	{
		super(null, null);
	}

	/**
	 * Constructor which takes the name of the KB and the location where KBs preprocessed for Vampire should be placed.
	 *
	 * @param name name
	 * @param dir  directory
	 */
	public KB(@Nullable String name, @Nullable String dir)
	{
		super(name, dir);
	}

	/**
	 * Constructor
	 *
	 * @param name name
	 */
	public KB(@Nullable String name)
	{
		super(name);
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
	@Override
	public void addConstituent(@NotNull String filename)
	{
		addConstituent(filename, //
				// build cache
				file -> {
					if (!file.endsWith(_cacheFileSuffix))
					{
						buildRelationCaches();
					}
				}, //
				// arity checker
				PERFORM_ARITY ? f -> {
					try
					{
						f.hasCorrectArityThrows(this::getValence);
						return true;
					}
					catch (Arity.ArityException ae)
					{
						errors.add("REJECTED formula at " + f.sourceFile + ':' + f.startLine + " because of incorrect arity: " + f.form + " " + ae);
						System.err.println("REJECTED formula at " + f.sourceFile + ':' + f.startLine + " because of incorrect arity: " + f.form + " " + ae);
						return false;
					}
				} : null);
	}

	// A R I T Y / V A L E N C E

	/**
	 * Check arity
	 */
	public void checkArity()
	{
		@NotNull List<String> toRemove = new ArrayList<>();
		if (formulas.size() > 0)
		{
			for (String s : formulas.keySet())
			{
				Formula f = formulas.get(s);
				if (!f.hasCorrectArity(this::getValence))
				{
					errors.add("Formula in " + f.sourceFile + " rejected due to arity error: " + f.form);
					toRemove.add(f.form);
				}
			}
		}
		for (String s : toRemove)
		{
			formulas.remove(s);
		}
	}

	/**
	 * This method tries to find or compute a valence for the input
	 * relation.
	 *
	 * @param reln0 A String, the name of a SUO-KIF Relation.
	 * @return An int value. -1 means that no valence value could be
	 * found.  0 means that the relation is a VariableArityRelation.
	 * 1-5 are the standard SUO-KIF valence values.
	 */
	public int getValence(@NotNull String reln0)
	{
		int result = -1;
		try
		{
			if (!reln0.isEmpty())
			{
				// First, see if the valence has already been cached.
				int[] rv = relationValences.get(reln0);
				if (rv != null)
				{
					result = rv[0];
					return result;
				}

				// Grab all the superrelations too, since we have already computed them.
				@NotNull Set<String> relns = getCachedRelationValues("subrelation", reln0, 1, 2);
				relns.add(reln0);
				for (@NotNull String reln : relns)
				{
					if (result >= 0)
					{
						break;
					}
					// First, check to see if the KB actually contains an explicit valence value.  This is unlikely.
					@NotNull List<Formula> literals = askWithRestriction(1, reln, 0, "valence");
					if (!literals.isEmpty())
					{
						Formula f = literals.get(0);
						@NotNull String digit = f.getArgument(2);
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
					@NotNull Set<String> classNames = getCachedRelationValues("instance", reln, 1, 2);
					@NotNull String[][] tops = {{"VariableArityRelation", "0"}, {"UnaryFunction", "1"}, {"BinaryRelation", "2"}, {"TernaryRelation", "3"}, {"QuaternaryRelation", "4"}, {"QuintaryRelation", "5"},};
					for (int i = 0; i < tops.length; i++)
					{
						if (classNames.contains(tops[i][0]))
						{
							result = Integer.parseInt(tops[i][1]);
							// The kluge below is to deal with the fact that a function, by definition, has a valence
							// one less than the corresponding predicate.  An instance of TernaryRelation that is also an instance
							// of Function has a valence of 2, not 3.
							if (i > 1 && (reln.endsWith("Fn") || classNames.contains("Function")) && !(tops[i][0]).endsWith("Function"))
							{
								--result;
							}
							break;
						}
					}
				}
				// Cache the answer, if there is one.
				if (result >= 0)
				{
					@NotNull int[] rv2 = new int[1];
					rv2[0] = result;
					relationValences.put(reln0, rv2);
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
	 * Returns true if relnName is the name of a relation that is
	 * known to be, or computed to be, a variable arity relation.
	 *
	 * @param reln A String that names a SUMO Relation (Predicate
	 *             or Function).
	 * @return boolean
	 */
	public boolean isVariableArityRelation(@NotNull final String reln)
	{
		return VA_RELNS.contains(reln) || (getValence(reln) == 0) || isInstanceOf(reln, "VariableArityRelation");
	}

	/**
	 * Cache relation valences
	 */
	private void cacheRelationValences()
	{
		logger.entering(LOG_SOURCE, "cacheRelationValences");
		try
		{
			@NotNull Set<String> relations = getCachedRelationValues("instance", "Relation", 2, 1);
			@NotNull List<String> namePrefixes = Arrays.asList("VariableArity", "Unary", "Binary", "Ternary", "Quaternary", "Quintary");
			int npLen = namePrefixes.size();
			@Nullable RelationCache ic1 = getRelationCache("instance", 1, 2);
			@Nullable RelationCache ic2 = getRelationCache("instance", 2, 1);

			for (@NotNull String reln : relations)
			{
				// Here we evaluate getValence() to build the relationValences cache, and use its return
				// value to fill in any info that might be missing from the "instance" cache.
				int valence = getValence(reln);
				if ((valence > -1) && (valence < npLen))
				{
					@NotNull StringBuilder sb = new StringBuilder();
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
					@NotNull String className = sb.toString();
					if (!className.isEmpty())
					{
						addRelationCacheEntry(ic1, reln, className);
						addRelationCacheEntry(ic2, className, reln);
					}
				}
			}
		}
		catch (Exception ex)
		{
			logger.warning(Arrays.toString(ex.getStackTrace()));
			ex.printStackTrace();
		}
		logger.finer("RelationValences == " + relationValences.size() + " entries");
		logger.exiting(LOG_SOURCE, "cacheRelationValences");
	}

	// C A C H E D

	/**
	 * Get cached relation names
	 *
	 * @return A List of relation names (Strings).
	 */
	@NotNull
	private List<String> getCachedRelationNames()
	{
		@NotNull List<String> result = new ArrayList<>();
		try
		{
			@NotNull Set<String> relns = new LinkedHashSet<>(RELNS);
			relns.addAll(getCachedTransitiveRelationNames());
			relns.addAll(getCachedSymmetricRelationNames());
			result.addAll(relns);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		return result;
	}

	/**
	 * Returns the Set indexed by term in the RelationCache
	 * identified by relation, keyArg, and valueArg.
	 *
	 * @param reln     A String, the name of a relation
	 * @param term     A String (key) that indexes a Set
	 * @param keyArg   An int value that, with relation and valueArg,
	 *                 identifies a RelationCache
	 * @param valueArg An int value that, with relation and keyArg,
	 *                 identifies a RelationCache
	 * @return A Set, which could be empty
	 */
	@NotNull
	public Set<String> getCachedRelationValues(@NotNull final String reln, @NotNull final String term, int keyArg, int valueArg)
	{
		@NotNull Set<String> result = new HashSet<>();
		@Nullable RelationCache cache = getRelationCache(reln, keyArg, valueArg);
		if (cache != null)
		{
			@Nullable Set<String> values = cache.get(term);
			if (values != null)
			{
				result.addAll(values);
			}
		}
		return result;
	}

	/**
	 * Returns a list of the names of cached transitive relations.
	 *
	 * @return A List of relation names (Strings).
	 */
	@NotNull
	private List<String> getCachedTransitiveRelationNames()
	{
		@NotNull List<String> result = new ArrayList<>(TRANSITIVE_RELNS);
		@NotNull Set<String> trSet = getAllInstancesWithPredicateSubsumption("TransitiveRelation");
		for (String name : trSet)
		{
			if (!result.contains(name))
			{
				result.add(name);
			}
		}
		return result;
	}

	/**
	 * Returns a list of the names of cached symmetric relations.
	 *
	 * @return A List of relation names (Strings).
	 */
	@NotNull
	private List<String> getCachedSymmetricRelationNames()
	{
		@NotNull Set<String> symmSet = getAllInstancesWithPredicateSubsumption("SymmetricRelation");
		// symmSet.addAll(getTermsViaPredicateSubsumption("subrelation",2,"inverse",1,true));
		symmSet.add("inverse");
		return new ArrayList<>(symmSet);
	}

	/**
	 * Get cached reflexive relation names
	 *
	 * @return A List of relation names (Strings).
	 */
	@NotNull
	private List<String> getCachedReflexiveRelationNames()
	{
		@NotNull List<String> result = new ArrayList<>();
		@NotNull List<String> reflexives = new ArrayList<>(REFLEXIVE_RELNS);
		for (String name : getAllInstancesWithPredicateSubsumption("ReflexiveRelation"))
		{
			if (!reflexives.contains(name))
			{
				reflexives.add(name);
			}
		}
		@NotNull List<String> cached = getCachedRelationNames();
		for (String reflexive : reflexives)
		{
			if (cached.contains(reflexive))
			{
				result.add(reflexive);
			}
		}
		return result;
	}

	// C A C H E

	/**
	 * This Map is used to cache sortal predicate argument type data
	 * whenever Formula.findType() or Formula.getTypeList() will be
	 * called hundreds of times inside KB.preProcess(), or to
	 * accomplish another expensive computation tasks.  The Map is
	 * cleared after each use in KB.preProcess(), but may retain its
	 * contents when used in other contexts.
	 */
	@Nullable
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
	@NotNull
	public Map<String, List<String>> getSortalTypeCache()
	{
		if (sortalTypeCache == null)
		{
			sortalTypeCache = new HashMap<>();
		}
		return sortalTypeCache;
	}

	/**
	 * Builds all the relation caches for the current KB.  If
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
		logger.entering(LOG_SOURCE, "buildRelationCaches", "clearExistingCaches = " + clearExistingCaches);
		long totalCacheEntries = 0L;
		int i;
		for (i = 1; true; i++)
		{
			initRelationCaches(clearExistingCaches);
			clearExistingCaches = false;

			cacheGroundAssertionsAndPredSubsumptionEntailments();
			for (@NotNull String relationName : getCachedTransitiveRelationNames())
			{
				computeTransitiveCacheClosure(relationName);
			}
			computeInstanceCacheClosure();

			// "disjoint"
			for (@NotNull String relationName : getCachedSymmetricRelationNames())
			{
				if (Objects.equals("disjoint", relationName))
				{
					computeSymmetricCacheClosure(relationName);
				}
			}
			cacheRelnsWithRelnArgs();
			cacheRelationValences();

			long entriesAfterThisIteration = 0L;
			for (@NotNull RelationCache relationCache : relationCaches)
			{
				if (!relationCache.isEmpty())
				{
					for (@NotNull Set<String> values : relationCache.values())
					{
						entriesAfterThisIteration += values.size();
					}
				}
			}
			if (entriesAfterThisIteration > totalCacheEntries)
			{
				totalCacheEntries = entriesAfterThisIteration;
			}
			else
			{
				break;
			}
			if (i > 4)
			{
				break;
			}
		}
		logger.finest("Caching cycles == " + i + " Cache entries == " + totalCacheEntries);
		logger.exiting(LOG_SOURCE, "buildRelationCaches");
	}

	/**
	 * Builds all the relation caches for the current KB.  If
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
		logger.entering(LOG_SOURCE, "cacheGroundAssertionsAndPredSubsumptionEntailments");
		@NotNull List<String> symmetric = getCachedSymmetricRelationNames();
		@NotNull List<String> reflexive = getCachedReflexiveRelationNames();

		int total = 0;
		for (@NotNull String relation : getCachedRelationNames())
		{
			int count = 0;

			@NotNull Set<String> relationSet = new HashSet<>(getTermsViaPredicateSubsumption("subrelation", 2, relation, 1, true));
			relationSet.add(relation);

			@NotNull Set<Formula> formulae = new HashSet<>();
			for (String value : relationSet)
			{
				@NotNull List<Formula> forms = ask("arg", 0, value);
				formulae.addAll(forms);
			}
			if (!formulae.isEmpty())
			{
				@Nullable RelationCache c1 = getRelationCache(relation, 1, 2);
				@Nullable RelationCache c2 = getRelationCache(relation, 2, 1);
				for (@NotNull Formula f : formulae)
				{
					if ((f.form.indexOf("(", 2) == -1) && !f.sourceFile.endsWith(_cacheFileSuffix))
					{
						@NotNull String arg1 = f.getArgument(1);
						@NotNull String arg2 = f.getArgument(2);

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
				@NotNull List<Formula> partitions = ask("arg", 0, "partition");
				@NotNull List<Formula> decompositions = ask("arg", 0, "disjointDecomposition");
				formulae.addAll(partitions);
				formulae.addAll(decompositions);
				@Nullable RelationCache c1 = getRelationCache(relation, 1, 2);
				for (@NotNull Formula f : formulae)
				{
					if ((f.form.indexOf("(", 2) == -1) && !f.sourceFile.endsWith(_cacheFileSuffix))
					{
						@Nullable List<String> args = f.simpleArgumentsToList(2);
						if (args != null)
						{
							for (int i = 0; i < args.size(); i++)
							{
								for (int j = 0; j < args.size(); j++)
								{
									if (i != j)
									{
										@NotNull String arg1 = args.get(i);
										@NotNull String arg2 = args.get(j);
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
			}
			if (count > 0)
			{
				logger.finer(relation + ": " + count + " entries added for " + relationSet);
				total += count;
			}
		}
		logger.finer(total + " new cache entries computed");
		logger.exiting(LOG_SOURCE, "cacheGroundAssertionsAndPredSubsumptionEntailments");
	}

	/**
	 * Get relation caches
	 *
	 * @return A List of RelationCache objects.
	 */
	@NotNull
	protected List<RelationCache> getRelationCaches()
	{
		return this.relationCaches;
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
		logger.entering(LOG_SOURCE, "initRelationCaches", "clearExistingCaches = " + clearExistingCaches);
		if (clearExistingCaches)
		{
			// Clear all cache maps.
			for (@NotNull RelationCache rc : relationCaches)
			{
				rc.clear();
			}
			relationCaches.clear();  // Discard all cache maps.
		}
		@NotNull List<String> symmetric = getCachedSymmetricRelationNames();
		for (@NotNull String reln : getCachedRelationNames())
		{
			getRelationCache(reln, 1, 2);
			// We put each symmetric relation -- disjoint and a few others -- into just one RelationCache table apiece.
			// All transitive binary relations are cached in two RelationCaches, one that looks "upward" from
			// the keys, and another that looks "downward" from the keys.
			if (!symmetric.contains(reln))
			{
				getRelationCache(reln, 2, 1);
			}
		}
		// We still set these legacy variables.  Eventually, they should be removed.
		parents = getRelationCache("subclass", 1, 2);
		children = getRelationCache("subclass", 2, 1);
		disjoint = getRelationCache("disjoint", 1, 2);
		logger.exiting(LOG_SOURCE, "initRelationCaches");
	}

	/**
	 * Returns the RelationCache object identified by the input
	 * arguments: relation name, key argument position, and value
	 * argument position.
	 *
	 * @param reln     The name of the cached relation.
	 * @param keyArg   An int value that indicates the argument position
	 *                 of the cache keys.
	 * @param valueArg An int value that indicates the argument
	 *                 position of the cache values.
	 * @return a RelationCache object, or null if there is no cache corresponding to the input arguments.
	 */
	@Nullable
	private RelationCache getRelationCache(@NotNull String reln, int keyArg, int valueArg)
	{
		try
		{
			if (!reln.isEmpty())
			{
				for (@NotNull RelationCache relationCache : relationCaches)
				{
					if (relationCache.getRelationName().equals(reln) && (relationCache.getKeyArgument() == keyArg) && (relationCache.getValueArgument() == valueArg))
					{
						return relationCache;
					}
				}
				@NotNull RelationCache cache = new RelationCache(reln, keyArg, valueArg);
				relationCaches.add(cache);
				return cache;
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		return null;
	}

	/**
	 * Adds one value to the cache, indexed under keyTerm.
	 *
	 * @param cache     The RelationCache object to be updated.
	 * @param keyTerm   The String that is the key for this entry.
	 * @param valueTerm The String that is the value for this entry.
	 * @return The int value 1 if a new entry is added, else 0.
	 */
	private int addRelationCacheEntry(@Nullable RelationCache cache, @NotNull String keyTerm, @NotNull String valueTerm)
	{
		int count = 0;
		if ((cache != null) && !keyTerm.isEmpty() && !valueTerm.isEmpty())
		{
			@NotNull Set<String> valueSet = cache.computeIfAbsent(keyTerm, k -> new HashSet<>());
			if (valueSet.add(valueTerm))
			{
				count++;
			}
		}
		return count;
	}

	// closure

	/**
	 * This method computes the closure for the cache of the instance
	 * relation, in both directions.
	 */
	private void computeInstanceCacheClosure()
	{
		logger.entering(LOG_SOURCE, "computeInstanceCacheClosure");
		long count = 0L;
		try
		{
			@Nullable RelationCache ic1 = getRelationCache("instance", 1, 2);
			@Nullable RelationCache ic2 = getRelationCache("instance", 2, 1);
			@Nullable RelationCache sc1 = getRelationCache("subclass", 1, 2);
			if (ic1 != null && ic2 != null && sc1 != null)
			{
				@NotNull Set<String> ic1KeySet = ic1.keySet();
				for (String ic1KeyTerm : ic1KeySet)
				{
					Set<String> ic1ValSet = ic1.get(ic1KeyTerm);

					@NotNull String[] ic1ValArr = ic1ValSet.toArray(new String[0]);
					for (@Nullable String ic1ValTerm : ic1ValArr)
					{
						if (ic1ValTerm != null)
						{
							Set<String> sc1ValSet = sc1.get(ic1ValTerm);
							if (sc1ValSet != null)
							{
								for (String s : sc1ValSet)
								{
									if (count >= MAX_CACHE_SIZE)
									{
										break;
									}
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
							@NotNull Set<String> ic2ValSet = ic2.computeIfAbsent(ic1ValTerm, k -> new HashSet<>());
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
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		if (count > 0)
		{
			logger.finer(count + " instance entries");
		}
		logger.exiting(LOG_SOURCE, "computeInstanceCacheClosure");
	}

	/**
	 * This method computes the transitive closure for the relation
	 * identified by relationName.  The results are stored in the
	 * RelationCache object for the relation and "direction" (looking
	 * from the arg1 keys toward arg2 parents, or looking from the
	 * arg2 keys toward arg1 children).
	 *
	 * @param reln The name of a relation
	 */
	private void computeTransitiveCacheClosure(@NotNull final String reln)
	{
		logger.entering(LOG_SOURCE, "computerTransitiveCacheClosure", "relationName = " + reln);
		long count = 0L;
		try
		{
			if (getCachedTransitiveRelationNames().contains(reln))
			{
				@Nullable RelationCache c1 = getRelationCache(reln, 1, 2);
				@Nullable RelationCache c2 = getRelationCache(reln, 2, 1);
				if (c1 != null && c2 != null)
				{
					@Nullable RelationCache inst1 = null;
					@Nullable RelationCache inst2 = null;
					boolean isSubrelationCache = reln.equals("subrelation");
					if (isSubrelationCache)
					{
						inst1 = getRelationCache("instance", 1, 2);
						inst2 = getRelationCache("instance", 2, 1);
					}
					@NotNull Set<String> c1Keys = c1.keySet();

					boolean changed = true;
					while (changed)
					{
						changed = false;
						for (@Nullable String keyTerm : c1Keys)
						{
							if (keyTerm == null || keyTerm.isEmpty())
							{
								logger.warning("Error in KB.computeTransitiveCacheClosure(" + reln + ") \n   keyTerm == " + ((keyTerm == null) ? null : "\"" + keyTerm + "\""));
							}
							else
							{
								Set<String> valSet = c1.get(keyTerm);
								@NotNull String[] valArr = valSet.toArray(new String[0]);
								for (String valTerm : valArr)
								{
									Set<String> valSet2 = c1.get(valTerm);
									if (valSet2 != null)
									{
										for (String s : valSet2)
										{
											if (count >= MAX_CACHE_SIZE)
											{
												break;
											}
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
									@NotNull String valTerm = "Relation";
									if (keyTerm.endsWith("Fn"))
									{
										valTerm = "Function";
									}
									else
									{
										@NotNull String nsDelim = StringUtil.getKifNamespaceDelimiter();
										int ndIdx = keyTerm.indexOf(nsDelim);
										@NotNull String stripped = keyTerm;
										if (ndIdx > -1)
										{
											stripped = keyTerm.substring(nsDelim.length() + ndIdx);
										}
										if (Character.isLowerCase(stripped.charAt(0)) && !keyTerm.contains("("))
										{
											valTerm = "Predicate";
										}
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
		{
			logger.fine(count + " " + reln + " entries computed");
		}
		logger.exiting(LOG_SOURCE, "computeTransitiveCacheClosure");
	}

	/**
	 * This method computes the closure for the caches of symmetric
	 * relations.  As currently implemented, it really applies to only
	 * disjoint.
	 */
	private void computeSymmetricCacheClosure(@NotNull final String reln)
	{
		logger.entering(LOG_SOURCE, "computeSymmetricCacheClosure", "relationName = " + reln);
		long count = 0L;
		try
		{
			@Nullable RelationCache dc1 = getRelationCache(reln, 1, 2);
			@Nullable RelationCache sc2 = (reln.equals("disjoint") ? getRelationCache("subclass", 2, 1) : null);
			if (sc2 != null && dc1 != null)
			{
				// int passes = 0; 	// One pass is sufficient.
				boolean changed = true;
				while (changed)
				{
					changed = false;

					@NotNull Set<String> dc1KeySet = dc1.keySet();
					@NotNull String[] dc1KeyArr = dc1KeySet.toArray(new String[0]);
					for (int i = 0; (i < dc1KeyArr.length) && (count < MAX_CACHE_SIZE); i++)
					{
						String dc1KeyTerm = dc1KeyArr[i];
						Set<String> dc1ValSet = dc1.get(dc1KeyTerm);
						@NotNull String[] dc1ValArr = dc1ValSet.toArray(new String[0]);
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
								@NotNull Set<String> dc1ValSet2 = dc1.computeIfAbsent(sc2ValTerm, k -> new HashSet<>());
								if (dc1ValSet2.addAll(dc1ValSet))
								{
									changed = true;
								}
							}
						}
						count = 0;
						for (@NotNull Set<String> dc1ValSet3 : dc1.values())
						{
							count += dc1ValSet3.size();
						}
					}
					if (changed)
					{
						dc1.setIsClosureComputed();
					}
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
		{
			logger.finer(count + " " + reln + " entries computed");
		}
		logger.exiting(LOG_SOURCE, "computeSymmetricCacheClosure");
	}

	// relation arg type

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
	@Nullable
	public String getArgType(@NotNull final String reln, int argPos)
	{
		@Nullable String className = null;
		try
		{
			@Nullable String argType = Types.findType(argPos, reln, this);
			if (argType != null && !argType.isEmpty())
			{
				if (argType.endsWith("+"))
				{
					argType = "SetOrClass";
				}
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
	@Nullable
	public String getArgTypeClass(@NotNull final String reln, int argPos)
	{
		@Nullable String className = null;
		try
		{
			@Nullable String argType = Types.findType(argPos, reln, this);
			if (argType != null && !argType.isEmpty())
			{
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

	// relation args

	/**
	 * List relations with relation arguments
	 *
	 * @return list of relations
	 */
	@Nullable
	protected List<String> listRelnsWithRelnArgs()
	{
		if (relnsWithRelnArgs != null)
		{
			return new ArrayList<>(relnsWithRelnArgs.keySet());
		}
		return null;
	}

	/**
	 * This method builds a cache of all Relations in the current KB
	 * for which at least one argument must be filled by a relation
	 * name (or a variable denoting a relation name).  This method
	 * should be called only after the subclass cache has been built.
	 */
	private void cacheRelnsWithRelnArgs()
	{
		logger.entering(LOG_SOURCE, "cacheRelnsWithRelnArgs");
		try
		{
			if (relnsWithRelnArgs == null)
			{
				relnsWithRelnArgs = new HashMap<>();
			}
			relnsWithRelnArgs.clear();

			@NotNull Set<String> relnClasses = getCachedRelationValues("subclass", "Relation", 2, 1);
			relnClasses.add("Relation");
			for (@NotNull String relnClass : relnClasses)
			{
				@NotNull List<Formula> formulas = askWithRestriction(3, relnClass, 0, "domain");
				for (@NotNull Formula f : formulas)
				{
					@NotNull String reln = f.getArgument(1);
					int valence = getValence(reln);
					if (valence < 1)
					{
						valence = Arity.MAX_PREDICATE_ARITY;
					}
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
						logger.warning("Error in KB.cacheRelnsWithRelnArgs(): reln == " + reln + ", argPos == " + argPos + ", signature == " + Arrays.toString(signature));
						throw e1;
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
				{
					signature[i] = (i == 2);
				}
				relnsWithRelnArgs.put("format", signature);
			}
		}
		catch (Exception ex)
		{
			logger.warning(Arrays.toString(ex.getStackTrace()));
			ex.printStackTrace();
		}
		int count = relnsWithRelnArgs.size();
		if (count > 0)
		{
			logger.finer(count + " relation argument entries computed");
		}
		logger.exiting(LOG_SOURCE, "cacheRelnsWithRelnArgs");
	}

	/**
	 * Returns a boolean[] if the input relation has at least one argument that must be filled by a relation name.
	 *
	 * @param reln relation
	 * @return whether the input relation has at least one argument that must be filled by a relation name.
	 */
	public boolean[] getRelnArgSignature(@NotNull final String reln)
	{
		if (relnsWithRelnArgs != null)
		{
			return relnsWithRelnArgs.get(reln);
		}
		return null;
	}

	// config

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

	// F I N D

	// child

	/**
	 * Returns true if inst is className, is an instance of className, or is subclass of className, else returns false.
	 *
	 * @param inst      A String denoting an instance.
	 * @param className A String denoting a Class.
	 * @return whether inst is className, is an instance of className, or is subclass of className.
	 */
	public boolean isChildOf(@NotNull final String inst, @NotNull final String className)
	{
		return inst.equals(className) || isInstanceOf(inst, className) || isSubclass(inst, className);
	}

	/**
	 * Determine whether a particular class or instance "child" is a child of the given "parent".
	 *
	 * @param child  A String, the name of a term.
	 * @param parent A String, the name of a term.
	 * @return true if child and parent constitute an actual or
	 * implied relation in the current KB, else false.
	 */
	public boolean childOf(@NotNull final String child, final String parent)
	{
		boolean result = child.equals(parent);
		if (!result)
		{
			@NotNull List<String> preds = Arrays.asList("instance", "subclass", "subrelation");
			for (@NotNull String pred : preds)
			{
				@NotNull Set<String> parents = getCachedRelationValues(pred, child, 1, 2);
				result = parents.contains(parent);
				if (result)
				{
					break;
				}
			}
		}
		return result;
	}

	// subclass

	/**
	 * Test if the subclass cache supports the conclusion that className1 is a subclass of className2, else returns false.
	 *
	 * @param className1 A String, the name of a SetOrClass.
	 * @param className2 A String, the name of a SetOrClass.
	 * @return whether the subclass cache supports the conclusion that className1 is a subclass of className2.
	 */
	public boolean isSubclass(@NotNull final String className1, @NotNull final String className2)
	{
		boolean result = false;
		if (!className1.isEmpty() && !className2.isEmpty())
		{
			result = getCachedRelationValues("subclass", className1, 1, 2).contains(className2);
			// was: getAllSubClassesWithPredicateSubsumption(className2);
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
	@NotNull
	private Set<String> getAllSubClasses(@Nullable final Set<String> classNames)
	{
		@NotNull Set<String> result = new HashSet<>();
		if (classNames != null && !classNames.isEmpty())
		{
			@NotNull List<String> accumulator = new ArrayList<>();
			@NotNull List<String> working = new ArrayList<>(classNames);
			while (!(working.isEmpty()))
			{
				for (int i = 0; i < working.size(); i++)
				{
					@NotNull List<Formula> nextLits = askWithRestriction(2, working.get(i), 0, "subclass");
					for (@NotNull Formula f : nextLits)
					{
						@NotNull String arg1 = f.getArgument(1);
						if (!working.contains(arg1))
						{
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
	 * This method retrieves all subclasses of className, using both
	 * class and predicate (subrelation) subsumption.
	 *
	 * @param className The name of a Class.
	 * @return A Set of terms (string constants), which could be
	 * empty.
	 */
	@NotNull
	public Set<String> getAllSubClassesWithPredicateSubsumption(@NotNull final String className)
	{
		@NotNull Set<String> result = new TreeSet<>();
		if (!className.isEmpty())
		{
			// Get all subrelations of subrelation.
			@NotNull Set<String> metarelations = getCachedRelationValues("subrelation", "subrelation", 2, 1);
			metarelations.add("subrelation");

			@NotNull Set<String> relations = new HashSet<>();

			// Get all subrelations of subclass.
			for (@NotNull String pred : metarelations)
			{
				relations.addAll(getCachedRelationValues(pred, "subclass", 2, 1));
			}
			relations.add("subclass");

			// Get all subclasses of className.
			for (@NotNull String pred : relations)
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
	@NotNull
	public Set<String> getAllSuperClassesWithPredicateSubsumption(@NotNull final String className)
	{
		@NotNull Set<String> result = new TreeSet<>();
		if (!className.isEmpty())
		{
			@NotNull Set<String> relations = new HashSet<>();

			// Get all subrelations of subrelation.
			@NotNull Set<String> metarelations = getCachedRelationValues("subrelation", "subrelation", 2, 1);
			metarelations.add("subrelation");

			// Get all subrelations of subclass.
			for (@NotNull String pred : metarelations)
			{
				relations.addAll(getCachedRelationValues(pred, "subclass", 2, 1));
			}
			relations.add("subclass");

			// Get all superclasses of className.
			for (@NotNull String pred : relations)
			{
				result.addAll(getCachedRelationValues(pred, className, 1, 2));
			}
		}
		return result;
	}

	// instance of

	/**
	 * Returns true if i is an instance of c, else returns false.
	 *
	 * @param inst      A String denoting an instance.
	 * @param className A String denoting a Class.
	 * @return whether int is an instance of className.
	 */
	public boolean isInstanceOf(@NotNull final String inst, @NotNull final String className)
	{
		boolean result = false;
		try
		{
			result = getCachedRelationValues("instance", inst, 1, 2).contains(className);
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
	 * This method retrieves all classes of which inst is an instance,
	 * using both class and predicate (subrelation) subsumption.
	 *
	 * @param inst The name of a SUO-KIF term.
	 * @return A Set of terms (class names), which could be
	 * empty.
	 */
	@NotNull
	public Set<String> getAllInstanceOfsWithPredicateSubsumption(@NotNull final String inst)
	{
		@NotNull Set<String> result = new TreeSet<>();
		if (!inst.isEmpty())
		{
			// Get all subrelations of subrelation.
			@NotNull Set<String> metarelations = getCachedRelationValues("subrelation", "subrelation", 2, 1);
			metarelations.add("subrelation");
			@NotNull Set<String> relations = new HashSet<>();

			// Get all subrelations of instance.
			for (@NotNull String pred : metarelations)
			{
				relations.addAll(getCachedRelationValues(pred, "instance", 2, 1));
			}
			relations.add("instance");

			// Get all classes of which inst is an instance.
			@NotNull Set<String> classes = new HashSet<>();
			for (@NotNull String pred : relations)
			{
				classes.addAll(getCachedRelationValues(pred, inst, 1, 2));
			}
			result.addAll(classes);

			// Get all superclasses of classes.
			for (@NotNull String cl : classes)
			{
				result.addAll(getAllSuperClassesWithPredicateSubsumption(cl));
			}
		}
		return result;
	}

	// instance

	/**
	 * This method retrieves all instances of the classes named in the
	 * input set.
	 *
	 * @param classNames A Set object containing SUO-KIF class names
	 *                   (Strings).
	 * @return A SortedSet, possibly empty, containing SUO-KIF constant names.
	 */
	@NotNull
	protected Set<String> getAllInstances(@Nullable final Set<String> classNames)
	{
		@NotNull Set<String> result = new TreeSet<>();
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
	@NotNull
	public Set<String> getAllInstances(@NotNull final String className)
	{
		if (!className.isEmpty())
		{
			@NotNull SortedSet<String> input = new TreeSet<>();
			input.add(className);
			return getAllInstances(input);
		}
		return new TreeSet<>();
	}

	/**
	 * This method retrieves all instances of className, using both
	 * predicate (subrelation) and class subsumption.
	 *
	 * @param className The name of a Class
	 * @return A Set of terms (string constants), which could be
	 * empty
	 */
	@NotNull
	public Set<String> getAllInstancesWithPredicateSubsumption(@NotNull final String className)
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
	@NotNull
	public Set<String> getAllInstancesWithPredicateSubsumption(@NotNull final String className, boolean gatherSubclasses)
	{
		@NotNull Set<String> result = new TreeSet<>();
		if (!className.isEmpty())
		{
			// Get all subrelations of subrelation.
			@NotNull Set<String> metarelations = getCachedRelationValues("subrelation", "subrelation", 2, 1);
			metarelations.add("subrelation");

			@NotNull Set<String> relations = new HashSet<>();

			// Get all subrelations of instance.
			for (@NotNull String metarelation : metarelations)
			{
				relations.addAll(getCachedRelationValues(metarelation, "instance", 2, 1));
			}
			relations.add("instance");

			// Get all "local" or "immediate" instances of className, using instance and all gathered subrelations of instance.
			for (@NotNull String relation : relations)
			{
				result.addAll(getCachedRelationValues(relation, className, 2, 1));
			}

			if (gatherSubclasses)
			{
				@NotNull Set<String> subclasses = getAllSubClassesWithPredicateSubsumption(className);
				// subclasses.add(className);
				for (@NotNull String subclass : subclasses)
				{
					for (@NotNull String relation : relations)
					{
						result.addAll(getTermsViaAskWithRestriction(0, relation, 2, subclass, 1));
					}
				}
			}
		}
		return result;
	}

	// predicates, classes, functions

	/**
	 * Collect predicates
	 *
	 * @return a List containing all predicates in this KB.
	 */
	@NotNull
	public Collection<String> collectPredicates()
	{
		return getCachedRelationValues("instance", "Predicate", 2, 1);
	}

	@NotNull
	public Collection<String> collectClasses()
	{
		return getCachedRelationValues("subclass", "Class", 2, 1);
	}

	/**
	 * Collect functions
	 *
	 * @return a List containing all functions in this KB.
	 */
	@NotNull
	public Collection<String> collectFunctions()
	{
		return getCachedRelationValues("instance", "Function", 2, 1);
	}

	// A S K

	/**
	 * This method retrieves Formulas by asking the query expression
	 * queryLit, and returns the results, if any, in a List.
	 *
	 * @param queryLit The query, which is assumed to be a List
	 *                 (atomic literal) consisting of a single predicate and its
	 *                 arguments.  The arguments could be variables, constants, or a
	 *                 mix of the two, but only the first constant encountered in a
	 *                 left to right sweep over the literal will be used in the actual
	 *                 query.
	 * @return A List of Formula objects, or an empty List
	 * if no answers are retrieved.
	 */
	@NotNull
	public List<Formula> askWithLiteral(@Nullable final List<String> queryLit)
	{
		@NotNull List<Formula> result = new ArrayList<>();
		if (queryLit != null && !queryLit.isEmpty())
		{
			String pred = queryLit.get(0);
			if (pred.equals("instance") && isVariable(queryLit.get(1)) && !(isVariable(queryLit.get(2))))
			{
				String className = queryLit.get(2);
				@NotNull Set<String> insts = getAllInstances(className);
				for (String inst : insts)
				{
					@NotNull String form = "(instance " + inst + " " + className + ")";
					@NotNull Formula f = Formula.of(form);
					result.add(f);
				}
			}
			else if (pred.equals("valence") && isVariable(queryLit.get(1)) && isVariable(queryLit.get(2)))
			{
				@NotNull Set<String> ai = getAllInstances("Relation");
				for (@NotNull String inst : ai)
				{
					int valence = getValence(inst);
					if (valence > 0)
					{
						@NotNull String form = "(valence " + inst + " " + valence + ")";
						@NotNull Formula f = Formula.of(form);
						result.add(f);
					}
				}
			}
			else
			{
				@Nullable String constant = null;
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
				{
					result = askWithRestriction(cIdx, constant, 0, pred);
				}
				else
				{
					result = ask("arg", 0, pred);
				}
			}
		}
		return result;
	}
}
