/*
 * Copyright (c) 2022.
 * This code is copyright Bernard Bou <1313ou@gmail.com>
 * This software is released under the GNU Public License 3 <http://www.gnu.org/copyleft/gpl.html>.
 */

package org.owl;

import org.sigma.core.*;
import org.w3c.dom.*;

import java.io.*;
import java.util.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import static org.owl.WordNetOwl.*;
import static org.sigma.core.StringUtil.wordWrap;

/**
 * Read and write OWL format from Sigma data structures.
 */
public class OWLTranslator2
{
	static public final boolean WITH_WORDNET = false;

	static public final boolean WITH_YAGO = false;

	public static final String AXIOM_RESOURCE = "axiom-";

	/**
	 * Relations in SUMO that have a corresponding relation in
	 * OWL and therefore require special treatment.
	 */
	private static final List<String> SUMO_RESERVED_RELATIONS = List.of( //
			"disjoint",                     // owl:disjointWith
			"disjointDecomposition",        // owl:distinctMembers
			"documentation",                // rdfs:comment
			"domain",                       // rdfs:domain
			"instance",                     //
			"inverse",                      // owl:inverseOf
			"range",                        // rdfs:range
			"subclass",                     // rdfs:subClassOf
			"subrelation",                  //
			"synonymousExternalConcept");   // owl:sameAs or owl:equivalentClass or owl:equivalentProperty

	private static final List<String> OWL_RESERVED_RELATIONS = List.of( // c=class, i=instance, r=relation
			"rdf:about",          //
			"rdf:ID",                       //
			"rdf:nodeID",                   //
			"rdf:resource",                 //
			"rdfs:comment",                 // SUMO:documentation
			"rdfs:domain",                  // SUMO:domain 1
			"rdfs:range",                   // SUMO:domain 1 or SUMO:range
			"rdfs:subClassOf",              // SUMO:subclass
			"owl:allValuesFrom",            //
			"owl:backwardCompatibleWith",   //
			"owl:cardinality",              //
			"owl:complimentOf",             // c,c : not allowed in OWL-Lite
			"owl:differentFrom",            // i,i
			"owl:disjointWith",             // c,c : SUMO:disjoint, not allowed in OWL-Lite
			"owl:distinctMembers",          // SUMO:disjointDecomposition
			"owl:equivalentClass",          // c,c : SUMO:synonymousExternalConcept
			"owl:equivalentProperty",       // r,r : SUMO:synonymousExternalConcept
			"owl:hasValue",                 // not allowed in OWL-Lite
			"owl:imports",                  //
			"owl:incompatibleWith",         //
			"owl:intersectionOf",           //
			"owl:inverseOf",                // r,r : SUMO:inverse
			"owl:maxCardinality",           //
			"owl:minCardinality",           //
			"owl:oneOf",                    // not allowed in OWL-Lite
			"owl:onProperty",               //
			"owl:priorVersion",             //
			"owl:sameAs",                   // i,i : SUMO:synonymousExternalConcept (OWL instances)
			"owl:someValuesFrom",           //
			"owl:unionOf",                  // not allowed in OWL-Lite
			"owl:versionInfo");

	/**
	 * OWL DL requires a pairwise separation between classes,
	 * datatypes, datatype properties, object properties,
	 * annotation properties, ontology properties (i.e., the import
	 * and versioning stuff), individuals, data values and the
	 * built-in vocabulary.
	 */
	private static final List<String> OWL_RESERVED_CLASSES = List.of( //
			"rdf:List", //
			"rdf:Property", //
			"rdfs:Class", //
			"owl:AllDifferent", //
			"owl:AnnotationProperty", //
			"owl:Class", // same as rdfs:Class for OWL-Full
			"owl:DataRange", // not allowed in OWL-Lite
			"owl:DatatypeProperty", //
			"owl:DeprecatedClass", //
			"owl:DeprecatedProperty", //
			"owl:FunctionalProperty", //
			"owl:InverseFunctionalProperty",// 
			"owl:Nothing", //
			"owl:ObjectProperty", // same as rdf:Property for OWL-Full
			"owl:Ontology", //
			"owl:OntologyProperty", //
			"owl:Restriction", //
			"owl:SymmetricProperty", //
			"owl:Thing", // any instance - same as rdfs:Resource for OWL-Full
			"owl:TransitiveProperty");

	@NotNull
	private final BaseKB kb;

	@Nullable
	private final WordNet wn;

	@Nullable
	private final Yago yago;

	@Nullable
	private Map<String, Formula> axiomMap = null;

	/**
	 * A map of functional statements and the automatically
	 * generated term that is created for it.
	 */
	@Nullable
	private Map<String, String> functionTable = null;

	// C O N S T R U C T

	public OWLTranslator2(@NotNull final BaseKB kb)
	{
		this(kb, null, null);
	}

	public OWLTranslator2(@NotNull final BaseKB kb, @Nullable final WordNet wn, @Nullable final Yago yago)
	{
		this.kb = kb;
		this.wn = wn;
		this.yago = yago;
	}

	/**
	 * Create axiom map
	 */
	@NotNull
	private Map<String, Formula> createAxiomMap()
	{
		@NotNull Map<String, Formula> result = new TreeMap<>();
		for (@NotNull Formula f : kb.formulas.values())
		{
			if (f.isRule())
			{
				result.put(AXIOM_RESOURCE + f.createID(), f);
			}
		}
		return result;
	}

	@NotNull
	private Map<String, String> createFunctionTable()
	{
		@NotNull Map<String, String> result = new HashMap<>();
		@NotNull Set<String> terms = kb.getTerms();
		for (@NotNull String term : terms)
		{
			if (Character.isUpperCase(term.charAt(0)))
			{
				// (instance term ...)
				@NotNull Collection<Formula> instances = kb.askWithRestriction(0, "instance", 1, term); // Instance expressions for term.
				if (instances.size() > 0 && !kb.isChildOf(term, "BinaryRelation"))
				{
					createFunctionTable(term, result);
				}

				// (subclass term ...)
				@NotNull Collection<Formula> classes = kb.askWithRestriction(0, "subclass", 1, term); // Class expressions for term.
				if (classes.size() > 0)
				{
					createFunctionTable(term, result);
				}
			}
		}
		return result;
	}

	private void createFunctionTable(String term, @NotNull Map<String, String> result)
	{
		// (reln term range)
		@NotNull Collection<Formula> statements = kb.ask(BaseKB.AskKind.ARG, 1, term);
		for (@NotNull Formula f : statements)
		{
			@NotNull String reln = f.getArgument(0);
			if (!reln.equals("instance") && !reln.equals("subclass") && !reln.equals("documentation") && !reln.equals("subrelation") && kb.isChildOf(reln, "BinaryRelation"))
			{
				// range
				@NotNull String range = f.getArgument(2);
				if (Lisp.listP(range))
				{
					result.put(range, instantiateFunction(range));
				}
			}
		}
	}

	// W R I T E

	// TERMS

	/**
	 * Write OWL format.
	 */
	public void writeSUMOTerm(@NotNull PrintStream ps, @NotNull String term)
	{
		if (kb.isChildOf(term, "BinaryRelation") && kb.askIsInstance(term))
		{
			writeRelationsOf(ps, term);
		}
		if (Character.isUpperCase(term.charAt(0)))
		{
			@NotNull Collection<Formula> instances = kb.askWithRestriction(0, "instance", 1, term);  // Instance expressions for term.
			@NotNull Collection<Formula> classes = kb.askWithRestriction(0, "subclass", 1, term);    // Class expressions for term.
			if (instances.size() > 0 && !kb.isChildOf(term, "BinaryRelation"))
			{
				writeInstancesOf(ps, term, instances);
			}
			boolean isInstance = false;
			if (classes.size() > 0)
			{
				if (instances.size() > 0)
				{
					isInstance = true;
				}
				writeClassesOf(ps, term, classes, isInstance);
			}
		}
	}

	/**
	 * Write OWL format for a SUMO or WordNet term.
	 */
	public void writeTerm(@NotNull PrintStream ps, @NotNull String term)
	{
		if (term.startsWith(WNPREFIX))
		{
			WordNetOwl.writeWordNetHeader(ps);
			if (term.startsWith(WNSYNSET_RESOURCE))
			{
				WordNetOwl.writeSynset(wn, ps, term);
			}
			else if (term.startsWith(WNWORD_RESOURCE))
			{
				@NotNull String word = term.substring(WNWORD_RESOURCE.length());
				WordNetOwl.writeWord(wn, ps, word);
			}
			else if (term.startsWith(WNSENSE_RESOURCE))
			{
				@NotNull String word = term.substring(WNSENSE_RESOURCE.length());
				WordNetOwl.writeWord(wn, ps, word);
			}
			WordNetOwl.writeWordNetTrailer(ps);
		}
		else
		{
			writeKBHeader(ps);
			if (term.startsWith(AXIOM_RESOURCE))
			{
				writeAxiom(ps, term);
			}
			else
			{
				writeSUMOTerm(ps, term);
			}
			writeKBTrailer(ps);
		}
	}

	// INSTANCES

	/**
	 * Write all instances in KB
	 *
	 * @param ps print stream
	 */
	public void writeInstances(@NotNull final PrintStream ps)
	{
		@NotNull Set<String> terms = kb.getTerms();
		for (@NotNull String term : terms)
		{
			writeInstancesOf(ps, term);
		}
	}

	/**
	 * Write instances of term
	 *
	 * @param ps   print stream
	 * @param term term
	 */
	public void writeInstancesOf(@NotNull PrintStream ps, @NotNull String term)
	{
		if (Character.isUpperCase(term.charAt(0)))
		{
			@NotNull Collection<Formula> instances = kb.askWithRestriction(0, "instance", 1, term);  // Instance expressions for term.
			if (instances.size() > 0 && !kb.isChildOf(term, "BinaryRelation"))
			{
				writeInstancesOf(ps, term, instances);
			}
		}
	}

	/**
	 * Write instances of term
	 *
	 * @param ps        print stream
	 * @param term      term
	 * @param instances its instances
	 */
	private void writeInstancesOf(@NotNull PrintStream ps, @NotNull String term, @NotNull Collection<Formula> instances)
	{
		ps.println("<owl:Thing rdf:about=\"#" + term + "\">");
		@Nullable String kbName = kb.name;
		ps.println("  <rdfs:isDefinedBy rdf:resource=\"http://www.ontologyportal.org/" + kbName + ".owl\"/>");
		for (@NotNull Formula f : instances)
		{
			@NotNull String parent = f.getArgument(2);
			if (Lisp.atom(parent))
			{
				ps.println("  <rdf:type rdf:resource=\"" + (parent.equals("Entity") ? "&owl;Thing" : "#" + parent) + "\"/>");
			}
		}
		writeDocumentation(ps, term);

		// (reln term range)
		@NotNull Collection<Formula> statements = kb.ask(BaseKB.AskKind.ARG, 1, term);
		for (@NotNull Formula f : statements)
		{
			@NotNull String reln = f.getArgument(0);
			if (!reln.equals("instance") && !reln.equals("subclass") && !reln.equals("documentation") && !reln.equals("subrelation") && kb.isChildOf(reln, "BinaryRelation"))
			{
				// non-standard binary relation
				// range
				String range = f.getArgument(2);
				if (range.isEmpty())
				{
					System.out.println("Error in writeInstancesOf(): missing range in statement: " + f);
					continue;
				}
				if (Lisp.listP(range))
				{
					range = instantiateFunction(range);
				}
				if (range.charAt(0) == '"' && range.charAt(range.length() - 1) == '"')
				{
					range = removeQuotes(range);
					if (range.startsWith("http://"))
					{
						ps.println("  <" + reln + " rdf:datatype=\"&xsd;anyURI\">" + range + "</" + reln + ">");
					}
					else
					{
						ps.println("  <" + reln + " rdf:datatype=\"&xsd;string\">" + range + "</" + reln + ">");
					}
				}
				else if ((range.charAt(0) == '-' && Character.isDigit(range.charAt(1)) || Character.isDigit(range.charAt(0))) && !range.contains("."))
				{
					ps.println("  <" + reln + " rdf:datatype=\"&xsd;integer\">" + range + "</" + reln + ">");
				}
				else
				{
					ps.println("  <" + reln + " rdf:resource=\"" + (range.equals("Entity") ? "&owl;Thing" : "#" + range) + "\" />");
				}
			}
		}

		writeSynonymous(ps, term, "instance");
		writeTermFormat(ps, term);
		writeAxiomLinks(ps, term);
		if (WITH_YAGO)
		{
			writeYAGOMapping(ps, term);
		}
		if (WITH_WORDNET)
		{
			writeWordNetLink(ps, term);
		}

		ps.println("</owl:Thing>");
		ps.println();
	}

	// CLASSES

	/**
	 * Write all classes in KB
	 *
	 * @param ps print stream
	 */
	public void writeClasses(@NotNull final PrintStream ps)
	{
		@NotNull Set<String> terms = kb.getTerms();
		for (@NotNull String term : terms)
		{
			writeClassesOf(ps, term);
		}
	}

	/**
	 * Write classes of term
	 *
	 * @param ps   print stream
	 * @param term term
	 */
	public void writeClassesOf(@NotNull PrintStream ps, @NotNull String term)
	{
		if (Character.isUpperCase(term.charAt(0)))
		{
			@NotNull Collection<Formula> instances = kb.askWithRestriction(0, "instance", 1, term);  // Instance expressions for term.
			@NotNull Collection<Formula> classes = kb.askWithRestriction(0, "subclass", 1, term);    // Class expressions for term.
			boolean isInstance = false;
			if (classes.size() > 0)
			{
				if (instances.size() > 0)
				{
					isInstance = true;
				}
				writeClassesOf(ps, term, classes, isInstance);
			}
		}
	}

	/**
	 * Write classes of term
	 *
	 * @param ps         print stream
	 * @param term       term
	 * @param classes    its classes
	 * @param isInstance whether term is instance
	 */
	private void writeClassesOf(@NotNull PrintStream ps, @NotNull String term, @NotNull Collection<Formula> classes, boolean isInstance)
	{
		if (isInstance)
		{
			ps.println("<owl:Class rdf:about=\"#" + term + "\">");
		}
		else
		{
			ps.println("<owl:Class rdf:about=\"#" + term + "\">");
		}

		@Nullable String kbName = kb.name;
		ps.println("  <rdfs:isDefinedBy rdf:resource=\"http://www.ontologyportal.org/" + kbName + ".owl\"/>");

		for (@NotNull Formula f : classes)
		{
			@NotNull String parent = f.getArgument(2);
			if (Lisp.atom(parent))
			{
				ps.println("  <rdfs:subClassOf rdf:resource=\"" + (parent.equals("Entity") ? "&owl;Thing" : "#" + parent) + "\"/>");
			}
		}
		writeDocumentation(ps, term);

		// (reln term range)
		@NotNull Collection<Formula> statements = kb.ask(BaseKB.AskKind.ARG, 1, term);
		for (@NotNull Formula f : statements)
		{
			@NotNull String rel = f.getArgument(0);
			if (!rel.equals("instance") && !rel.equals("subclass") && !rel.equals("documentation") && !rel.equals("subrelation") && kb.isChildOf(rel, "BinaryRelation"))
			{
				// non-standard binary relation
				// range
				String range = f.getArgument(2);
				if (range.isEmpty())
				{
					throw new IllegalStateException("Missing range in statement: " + f);
				}
				if (Lisp.listP(range))
				{
					range = instantiateFunction(range);
				}

				if (rel.equals("disjoint"))
				{
					ps.println("  <owl:disjointWith rdf:resource=\"" + (range.equals("Entity") ? "&owl;Thing" : "#" + range) + "\" />");
				}
				else if (rel.equals("synonymousExternalConcept"))
				{
					// since argument order is reversed between OWL and SUMO, this must be handled below
				}
				else if (range.charAt(0) == '"' && range.charAt(range.length() - 1) == '"')
				{
					range = removeQuotes(range);
					if (range.startsWith("http://"))
					{
						ps.println("  <" + rel + " rdf:datatype=\"&xsd;anyURI\">" + range + "</" + rel + ">");
					}
					else
					{
						ps.println("  <" + rel + " rdf:datatype=\"&xsd;string\">" + range + "</" + rel + ">");
					}
				}
				else if (((range.charAt(0) == '-' && Character.isDigit(range.charAt(1))) || (Character.isDigit(range.charAt(0)))) //
						&& !range.contains("."))
				{
					ps.println("  <" + rel + " rdf:datatype=\"&xsd;integer\">" + range + "</" + rel + ">");
				}
				else
				{
					ps.println("  <" + rel + " rdf:resource=\"" + (range.equals("Entity") ? "&owl;Thing" : "#" + range) + "\" />");
				}
			}
		}
		@NotNull Collection<Formula> syns = kb.askWithRestriction(0, "synonymousExternalConcept", 2, term);

		for (@NotNull Formula form : syns)
		{
			@Nullable String st = form.getArgument(1);
			st = stringToKIFid(st);
			@NotNull String lang = form.getArgument(3);
			ps.println("  <owl:equivalentClass rdf:resource=\"" + (lang.equals("Entity") ? "&owl;Thing" : "#" + lang) + ":" + st + "\" />");
		}

		writeSynonymous(ps, term, "class");
		writeTermFormat(ps, term);
		writeAxiomLinks(ps, term);
		if (WITH_YAGO)
		{
			yago.writeMapping(ps, term);
		}
		if (WITH_WORDNET)
		{
			WordNetOwl.writeLink(wn, ps, term);
		}

		ps.println("</owl:Class>");
		ps.println();
	}

	// RELATIONS

	/**
	 * Write all relations
	 *
	 * @param ps print stream
	 */
	public void writeRelations(@NotNull final PrintStream ps)
	{
		@NotNull Set<String> terms = kb.getTerms();
		for (@NotNull String term : terms)
		{
			writeRelationsOf(ps, term);
		}
	}

	/**
	 * Write  relations of term
	 *
	 * @param ps   print stream
	 * @param term term
	 */
	public void writeRelationsOf(@NotNull PrintStream ps, @NotNull String term)
	{
		if (kb.isChildOf(term, "BinaryRelation") && kb.askIsInstance(term))
		{
			@NotNull String propType = "ObjectProperty";
			if (kb.isChildOf(term, "SymmetricRelation"))
			{
				propType = "SymmetricProperty";
			}
			else if (kb.isChildOf(term, "TransitiveRelation"))
			{
				propType = "TransitiveProperty";
			}
			else if (kb.isChildOf(term, "Function"))
			{
				propType = "FunctionalProperty";
			}
			ps.println("<owl:" + propType + " rdf:about=\"#" + term + "\">");

			@NotNull Collection<Formula> argTypes = kb.askWithRestriction(0, "domain", 1, term);  // domain expressions for term.
			for (@NotNull Formula f : argTypes)
			{
				@NotNull String arg = f.getArgument(2);
				@NotNull String argType = f.getArgument(3);
				if (arg.equals("1") && Lisp.atom(argType))
				{
					ps.println("  <rdfs:domain rdf:resource=\"" + (argType.equals("Entity") ? "&owl;Thing" : "#" + argType) + "\" />");
				}
				if (arg.equals("2") && Lisp.atom(argType))
				{
					ps.println("  <rdfs:range rdf:resource=\"" + (argType.equals("Entity") ? "&owl;Thing" : "#" + argType) + "\" />");
				}
			}

			@NotNull Collection<Formula> ranges = kb.askWithRestriction(0, "range", 1, term);  // domain expressions for term.
			if (ranges.size() > 0)
			{
				Formula f = ranges.iterator().next();
				@NotNull String argType = f.getArgument(2);
				if (Lisp.atom(argType))
				{
					ps.println("  <rdfs:range rdf:resource=\"" + (argType.equals("Entity") ? "&owl;Thing" : "#" + argType) + "\" />");
				}
			}

			@NotNull Collection<Formula> inverses = kb.askWithRestriction(0, "inverse", 1, term);  // inverse expressions for term.
			if (inverses.size() > 0)
			{
				Formula f = inverses.iterator().next();
				@NotNull String arg = f.getArgument(2);
				if (Lisp.atom(arg))
				{
					ps.println("  <owl:inverseOf rdf:resource=\"" + (arg.equals("Entity") ? "&owl;Thing" : "#" + arg) + "\" />");
				}
			}

			@NotNull Collection<Formula> subs = kb.askWithRestriction(0, "subrelation", 1, term);  // subrelation expressions for term.
			for (@NotNull Formula f : subs)
			{
				@NotNull String superProp = f.getArgument(2);
				ps.println("  <owl:subPropertyOf rdf:resource=\"" + (superProp.equals("Entity") ? "&owl;Thing" : "#" + superProp) + "\" />");
			}

			writeDocumentation(ps, term);
			writeSynonymous(ps, term, "relation");
			writeTermFormat(ps, term);
			writeAxiomLinks(ps, term);
			if (WITH_YAGO)
			{
				yago.writeMapping(ps, term);
			}
			if (WITH_WORDNET)
			{
				writeWordNetLink(ps, term);
			}

			ps.println("</owl:" + propType + ">");
			ps.println();
		}
	}

	// FUNCTIONS

	/**
	 * State definitional information for automatically defined
	 * terms that replace function statements.
	 *
	 * @param ps print stream
	 */
	public void writeFunctionalTerms(@NotNull PrintStream ps)
	{
		assert functionTable != null;
		for (@NotNull final String functionTerm : functionTable.keySet())
		{
			String term = functionTable.get(functionTerm);
			@NotNull Formula f = Formula.of(functionTerm);
			@NotNull String func = f.getArgument(0);
			@NotNull Collection<Formula> ranges = kb.askWithRestriction(0, "range", 1, func);
			if (ranges.size() > 0)
			{
				Formula f2 = ranges.iterator().next();
				@NotNull String range = f2.getArgument(2);
				ps.println("<owl:Thing rdf:about=\"#" + term + "\">");
				ps.println("  <rdf:type rdf:resource=\"" + (range.equals("Entity") ? "&owl;Thing" : "#" + range) + "\"/>");
				ps.println("  <rdfs:comment>A term generated automatically in the " + "translation from SUO-KIF to OWL to replace the functional " + "term " + functionTerm + " that cannot be directly " + "expressed in OWL. </rdfs:comment>");
				ps.println("</owl:Thing>");
				ps.println();
			}
			else
			{
				@NotNull Collection<Formula> subranges = kb.askWithRestriction(0, "rangeSubclass", 1, functionTerm);
				if (subranges.size() > 0)
				{
					Formula f2 = subranges.iterator().next();
					@NotNull String range = f2.getArgument(2);
					ps.println("<owl:Class rdf:about=\"#" + term + "\">");
					ps.println("  <rdfs:subClassOf rdf:resource=\"" + (range.equals("Entity") ? "&owl;Thing" : "#" + range) + "\"/>");
					ps.println("  <rdfs:comment>A term generated automatically in the " + "translation from SUO-KIF to OWL to replace the functional " + "term " + functionTerm + " that connect be directly " + "expressed in OWL. </rdfs:comment>");
					ps.println("</owl:Class>");
					ps.println();
				}
				else
				{
					return;
				}
			}
		}
	}

	// FORMAT

	/**
	 * Write term format
	 */
	private void writeTermFormat(@NotNull PrintStream ps, @NotNull String term)
	{
		@NotNull Collection<Formula> al = kb.askWithRestriction(0, "termFormat", 2, term);
		for (@NotNull Formula form : al)
		{
			@NotNull String lang = form.getArgument(1);
			if (lang.equals("EnglishLanguage"))
			{
				lang = "en";
			}
			String st = form.getArgument(3);
			st = removeQuotes(st);
			ps.println("  <rdfs:label xml:lang=\"" + lang + "\">" + st + "</rdfs:label>");
		}
	}

	// AXIOMS

	/**
	 * Write Axioms
	 */
	public void writeAxioms(@NotNull PrintStream ps)
	{
		for (@NotNull Formula f : kb.formulas.values())
		{
			if (f.isRule())
			{
				String form = f.toFlatString();
				form = form.replaceAll("<=>", "iff");
				form = form.replaceAll("=>", "implies");
				form = processDoc(form);
				ps.println("<owl:Thing rdf:about=\"#" + AXIOM_RESOURCE + f.createID() + "\">");
				ps.println("  <rdfs:comment xml:lang=\"en\">A SUO-KIF axiom that may not be directly expressible in OWL. " + "See www.ontologyportal.org for the original SUO-KIF source.\n " + form + "</rdfs:comment>");
				ps.println("</owl:Thing>");
				ps.println();
			}
		}
	}

	/**
	 * Write one axiom
	 */
	private void writeAxiom(@NotNull PrintStream ps, String id)
	{
		assert axiomMap != null;
		Formula f = axiomMap.get(id);
		if (f != null && f.isRule())
		{
			String form = f.toFlatString();
			form = form.replaceAll("<=>", "iff");
			form = form.replaceAll("=>", "implies");
			form = processDoc(form);
			ps.println("<owl:Thing rdf:about=\"#" + id + "\">");
			ps.println("  <rdfs:comment xml:lang=\"en\">A SUO-KIF axiom that may not be directly expressible in OWL. " + "See www.ontologyportal.org for the original SUO-KIF source.\n " + form + "</rdfs:comment>");
			ps.println("</owl:Thing>");
		}
		else
		{
			System.out.println("Error in OWLtranslator.writeOneAxiom(): null or non-axiom for ID: " + id);
		}
	}

	/**
	 * Write Axiom links
	 */
	private void writeAxiomLinks(@NotNull PrintStream ps, String term)
	{
		@NotNull Collection<Formula> fs = kb.ask(BaseKB.AskKind.ANT, 0, term);
		for (@NotNull Formula f : fs)
		{
			@NotNull String fid = f.createID();
			ps.println("  <kbd:axiom rdf:resource=\"#" + AXIOM_RESOURCE + fid + "\"/>");
		}
		fs = kb.ask(BaseKB.AskKind.CONS, 0, term);
		for (@NotNull Formula f : fs)
		{
			@NotNull String fid = f.createID();
			ps.println("  <kbd:axiom rdf:resource=\"#" + AXIOM_RESOURCE + fid + "\"/>");
		}
	}

	// SYNONYMOUS

	/**
	 * Write Synonymous
	 */
	private void writeSynonymous(@NotNull PrintStream ps, @NotNull String term, @NotNull String termType)
	{
		@NotNull Collection<Formula> syn = kb.askWithRestriction(0, "synonymousExternalConcept", 2, term);
		for (@NotNull Formula form : syn)
		{
			@Nullable String st = form.getArgument(1);
			st = stringToKIFid(st);
			@NotNull String lang = form.getArgument(3);
			switch (termType)
			{
				case "relation":
					ps.println("  <owl:equivalentProperty rdf:resource=\"" + (lang.equals("Entity") ? "&owl;Thing" : "#" + lang) + ":" + st + "\" />");
					break;
				case "instance":
					ps.println("  <owl:sameAs rdf:resource=\"" + (lang.equals("Entity") ? "&owl;Thing" : "#" + lang) + ":" + st + "\" />");
					break;
				case "class":
					ps.println("  <owl:equivalentClass rdf:resource=\"" + (lang.equals("Entity") ? "&owl;Thing" : "#" + lang) + ":" + st + "\" />");
					break;
			}
		}
	}

	// DOCUMENTATION

	/**
	 * Write Documentation
	 */
	private void writeDocumentation(@NotNull PrintStream ps, @NotNull String term)
	{
		@NotNull Collection<Formula> doc = kb.askWithRestriction(0, "documentation", 1, term);    // Class expressions for term.
		for (@NotNull Formula form : doc)
		{
			@NotNull String lang = form.getArgument(2);
			@NotNull String documentation = form.getArgument(3);
			@NotNull String langString = "";
			if (lang.equals("EnglishLanguage"))
			{
				langString = " xml:lang=\"en\"";
			}
			if (documentation.isEmpty())
			{
				ps.println("  <rdfs:comment" + langString + ">" + wordWrap(processDoc(documentation)) + "</rdfs:comment>");
			}
		}
	}

	// MAIN

	/**
	 * Write OWL file header.
	 */
	private void writeKBHeader(@NotNull PrintStream ps)
	{
		ps.println("<!DOCTYPE rdf:RDF [");
		ps.println("   <!ENTITY wnd \"http://www.ontologyportal.org/WNDefs.owl#\">");
		ps.println("   <!ENTITY kbd \"http://www.ontologyportal.org/KBDefs.owl#\">");
		ps.println("   <!ENTITY xsd \"http://www.w3.org/2001/XMLSchema#\">");
		ps.println("   <!ENTITY rdf \"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">");
		ps.println("   <!ENTITY rdfs \"http://www.w3.org/2000/01/rdf-schema#\">");
		ps.println("   <!ENTITY owl \"http://www.w3.org/2002/07/owl#\">");
		ps.println("]>");
		ps.println("<rdf:RDF");
		ps.println("xmlns=\"http://www.ontologyportal.org/SUMO.owl#\"");
		ps.println("xml:base=\"http://www.ontologyportal.org/SUMO.owl\"");
		ps.println("xmlns:wnd=\"http://www.ontologyportal.org/WNDefs.owl#\"");
		ps.println("xmlns:kbd=\"http://www.ontologyportal.org/KBDefs.owl#\"");
		ps.println("xmlns:xsd=\"http://www.w3.org/2001/XMLSchema#\"");
		ps.println("xmlns:rdf =\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"");
		ps.println("xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\"");
		ps.println("xmlns:owl =\"http://www.w3.org/2002/07/owl#\">");
		ps.println("<owl:Ontology rdf:about=\"http://www.ontologyportal.org/SUMO.owl\">");
		ps.println("<rdfs:comment xml:lang=\"en\">A provisional and necessarily lossy translation to OWL.  Please see");
		ps.println("www.ontologyportal.org for the original KIF, which is the authoritative");
		ps.println("source.  This software is released under the GNU Public License");
		ps.println("www.gnu.org.</rdfs:comment>");
		@NotNull Date d = new Date();
		ps.println("<rdfs:comment xml:lang=\"en\">Produced on date: " + d + "</rdfs:comment>");
		ps.println("</owl:Ontology>");
	}

	/**
	 * Write OWL file header.
	 */
	private void writeKBTrailer(@NotNull PrintStream ps)
	{
		ps.println("</rdf:RDF>");
	}

	/**
	 * Write OWL format.
	 */
	public void write() throws IOException
	{
		@Nullable String path = kb.name;
		if (path == null)
		{
			path = "KB";
		}
		if (!path.endsWith(".owl"))
		{
			path += ".owl";
		}
		try (@NotNull PrintStream ps = new PrintStream(path))
		{
			write(ps);
		}
	}

	/**
	 * Write OWL format.
	 */
	public void write(@NotNull PrintStream ps)
	{
		writeKBHeader(ps);
		@NotNull Set<String> terms = kb.getTerms();
		for (@NotNull String term : terms)
		{
			writeSUMOTerm(ps, term);
		}
		writeFunctionalTerms(ps);
		writeAxioms(ps);
		writeKBTrailer(ps);
	}

	/**
	 * Write OWL format.
	 */
	private void writeSUMOOWLDefs(@NotNull PrintStream ps)
	{
		ps.println("<owl:ObjectProperty rdf:about=\"#axiom\">");
		ps.println("  <rdfs:domain rdf:resource=\"&owl;Thing\" />");
		ps.println("  <rdfs:range rdf:resource=\"rdfs:string\"/>");
		ps.println("  <rdfs:label xml:lang=\"en\">axiom</rdfs:label>");
		ps.println("  <rdfs:comment xml:lang=\"en\">A relation between a term\n" + "and a SUO-KIF axiom that defines (in part) the meaning of the term.</rdfs:comment>");
		ps.println("</owl:ObjectProperty>");
	}

	/**
	 * Write OWL format.
	 */
	public void writeDefs() throws IOException
	{
		try (@NotNull PrintStream ps = new PrintStream("KBDefs.owl"))
		{
			@NotNull Date d = new Date();
			ps.println("<!DOCTYPE rdf:RDF [");
			ps.println("   <!ENTITY wnd \"http://www.ontologyportal.org/WNDefs.owl#\">");
			ps.println("   <!ENTITY kbd \"http://www.ontologyportal.org/KBDefs.owl#\">");
			ps.println("   <!ENTITY xsd \"http://www.w3.org/2001/XMLSchema#\">");
			ps.println("   <!ENTITY rdf \"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">");
			ps.println("   <!ENTITY rdfs \"http://www.w3.org/2000/01/rdf-schema#\">");
			ps.println("   <!ENTITY owl \"http://www.w3.org/2002/07/owl#\">");
			ps.println("]>");
			ps.println("<rdf:RDF");
			ps.println("xmlns=\"http://www.ontologyportal.org/KBDefs.owl#\"");
			ps.println("xml:base=\"http://www.ontologyportal.org/KBDefs.owl\"");
			ps.println("xmlns:wnd=\"http://www.ontologyportal.org/WNDefs.owl#\"");
			ps.println("xmlns:kbd=\"http://www.ontologyportal.org/KBDefs.owl#\"");
			ps.println("xmlns:xsd=\"http://www.w3.org/2001/XMLSchema#\"");
			ps.println("xmlns:rdf =\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"");
			ps.println("xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\"");
			ps.println("xmlns:owl =\"http://www.w3.org/2002/07/owl#\">");
			ps.println("<owl:Ontology rdf:about=\"http://www.ontologyportal.org/KBDefs.owl\">");
			ps.println("<rdfs:comment xml:lang=\"en\">A provisional and necessarily lossy translation to OWL.  Please see");
			ps.println("www.ontologyportal.org for the original KIF, which is the authoritative");
			ps.println("source.  This software is released under the GNU Public License");
			ps.println("www.gnu.org.</rdfs:comment>");
			ps.println("<rdfs:comment xml:lang=\"en\">Produced on date: " + d + "</rdfs:comment>");
			ps.println("</owl:Ontology>");
			writeSUMOOWLDefs(ps);
			ps.println("</rdf:RDF>");
		}

		if (WITH_WORDNET)
		{
			try (@NotNull PrintStream ps = new PrintStream("WNDefs.owl"))
			{
				@NotNull Date d = new Date();
				ps.println("<!DOCTYPE rdf:RDF [");
				ps.println("   <!ENTITY wnd \"http://www.ontologyportal.org/WNDefs.owl#\">");
				ps.println("   <!ENTITY kbd \"http://www.ontologyportal.org/KBDefs.owl#\">");
				ps.println("   <!ENTITY xsd \"http://www.w3.org/2001/XMLSchema#\">");
				ps.println("   <!ENTITY rdf \"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">");
				ps.println("   <!ENTITY rdfs \"http://www.w3.org/2000/01/rdf-schema#\">");
				ps.println("   <!ENTITY owl \"http://www.w3.org/2002/07/owl#\">");
				ps.println("]>");
				ps.println("<rdf:RDF");
				ps.println("xmlns=\"http://www.ontologyportal.org/WNDefs.owl#\"");
				ps.println("xml:base=\"http://www.ontologyportal.org/WNDefs.owl\"");
				ps.println("xmlns:wnd=\"http://www.ontologyportal.org/WNDefs.owl#\"");
				ps.println("xmlns:kbd=\"http://www.ontologyportal.org/KBDefs.owl#\"");
				ps.println("xmlns:xsd=\"http://www.w3.org/2001/XMLSchema#\"");
				ps.println("xmlns:rdf =\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"");
				ps.println("xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\"");
				ps.println("xmlns:owl =\"http://www.w3.org/2002/07/owl#\">");
				ps.println("<owl:Ontology rdf:about=\"http://www.ontologyportal.org/WNDefs.owl\">");
				ps.println("<rdfs:comment xml:lang=\"en\">An expression of the Princeton WordNet " + "( http://wordnet.princeton.edu ) " + "in OWL.  Use is subject to the Princeton WordNet license at " + "http://wordnet.princeton.edu/wordnet/license/</rdfs:comment>");
				ps.println("<rdfs:comment xml:lang=\"en\">Produced on date: " + d + "</rdfs:comment>");
				ps.println("</owl:Ontology>");
				WordNetOwl.writeWordNetRelationDefinitions(ps);
				WordNetOwl.writeVerbFrames(ps);
				WordNetOwl.writeWordNetClassDefinitions(ps);
				WordNetOwl.writeExceptions(wn, ps);
				ps.println("</rdf:RDF>");
			}
		}
	}

	// Y A G O

	/**
	 * Write YAGO mapping
	 */
	private void writeYAGOMapping(@NotNull PrintStream ps, String term)
	{
		yago.writeMapping(ps, term);
	}

	// W O R D N E T

	private void writeWordNetLink(@NotNull final PrintStream ps, final String term)
	{
		WordNetOwl.writeLink(wn, ps, term);
	}

	// X M L   H E L P E R S

	@NotNull
	static List<Element> getChildElements(@NotNull Element e)
	{
		@NotNull List<Element> result = new ArrayList<>();
		@NotNull NodeList nodes = e.getChildNodes();
		for (int i = 0; i < nodes.getLength(); i++)
		{
			Node node = nodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE)
			{
				result.add((Element) node);
			}
		}
		return result;
	}

	// H E L P E R S

	/**
	 * Process String for XML uutput
	 */
	static String processStringForXMLOutput(@Nullable String s)
	{
		if (s == null)
		{
			return null;
		}
		s = s.replaceAll("<", "&lt;");
		s = s.replaceAll(">", "&gt;");
		s = s.replaceAll("&", "&amp;");
		return s;
	}

	/**
	 * Process String for KIF uutput
	 */
	private static String processStringForKIFOutput(@Nullable String s)
	{
		if (s == null)
		{
			return null;
		}
		return s.replaceAll("\"", "&quot;");
	}

	/**
	 * Remove special characters in documentation.
	 */
	private static String processDoc(String doc)
	{
		String result = doc;
		result = result.replaceAll("&%", "");
		result = result.replaceAll("&", "&#38;");
		result = result.replaceAll(">", "&gt;");
		result = result.replaceAll("<", "&lt;");
		result = removeQuotes(result);
		return result;
	}

	/**
	 * Remove quotes around a string
	 */
	private static String removeQuotes(@Nullable String s)
	{
		if (s == null)
		{
			return null;
		}
		s = s.trim();
		if (s.length() < 1)
		{
			return s;
		}
		if (s.length() > 1 && s.charAt(0) == '"' && s.charAt(s.length() - 1) == '"')
		{
			s = s.substring(1, s.length() - 1);
		}
		return s;
	}

	/**
	 * Turn a function statement into an identifier.
	 */
	@NotNull
	private static String instantiateFunction(@NotNull final String s)
	{
		String result = removeQuotes(s);
		result = result.substring(1, s.length() - 1);  // remove outer parens
		return stringToKIFid(result);
	}

	/**
	 * Convert an arbitrary string to a legal KIF identifier by substituting dashes for illegal characters.
	 *
	 * @param s string
	 * @return legal KIF identifier
	 */
	@Nullable
	static String stringToKIFid(@Nullable final String s)
	{
		if (s == null)
		{
			return null;
		}
		return s.replaceAll("[\\s]+", "-");
	}

	// R E A D

	/**
	 * Read OWL format.
	 */
	public static void read(String filename) throws IOException
	{
		try (@NotNull PrintStream ps = new PrintStream(filename + ".kif"))
		{
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = dbf.newDocumentBuilder();
			Document doc = builder.newDocument();
			Element se = doc.getDocumentElement();
			System.out.println("INFO in OWLtranslator.read(): input filename: " + filename);
			System.out.println("INFO in OWLtranslator.read(): output filename: " + filename + ".kif");
			decode(ps, se, "", "", "");
		}
		catch (IOException e)
		{
			throw new IOException("Error writing file " + filename + "\n" + e.getMessage());
		}
		catch (ParserConfigurationException e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * Read OWL format and write out KIF.
	 */
	private static void decode(@NotNull PrintStream ps, @NotNull Element se, String parentTerm, String parentTag, String indent)
	{
		@Nullable String tag = se.getTagName();
		@Nullable String value = null;
		@Nullable String existential = null;
		@Nullable String parens = null;

		if (tag.equals("owl:Class") || //
				tag.equals("owl:ObjectProperty") || //
				tag.equals("owl:DatatypeProperty") || //
				tag.equals("owl:FunctionalProperty") || //
				tag.equals("owl:InverseFunctionalProperty") || //
				tag.equals("owl:TransitiveProperty") || //
				tag.equals("owl:SymmetricProperty") || //
				tag.equals("rdf:Description"))
		{
			parentTerm = se.getAttribute("rdf:ID");
			if (!parentTerm.isEmpty())
			{
				if (parentTerm.contains("#"))
				{
					parentTerm = parentTerm.substring(parentTerm.indexOf("#") + 1);
				}
			}
			else
			{
				parentTerm = se.getAttribute("rdf:about");
				if (!parentTerm.isEmpty())
				{
					if (parentTerm.contains("#"))
					{
						parentTerm = parentTerm.substring(parentTerm.indexOf("#") + 1);
					}
				}
				else
				{
					// ps.println(";; nodeID? ");
					parentTerm = se.getAttribute("rdf:nodeID");
					if (!parentTerm.isEmpty())
					{
						parentTerm = "?nodeID-" + parentTerm;
						existential = parentTerm;
					}
				}
			}
			parentTerm = stringToKIFid(parentTerm);
			// ps.println(";; parentTerm" + parentTerm);
			if ((tag.equals("owl:ObjectProperty") || tag.equals("owl:DatatypeProperty") || tag.equals("owl:InverseFunctionalProperty")) && !parentTerm.isEmpty())
			{
				ps.println(indent + "(instance " + parentTerm + " BinaryRelation)");
			}
			if (tag.equals("owl:TransitiveProperty") && !parentTerm.isEmpty())
			{
				ps.println(indent + "(instance " + parentTerm + " TransitiveRelation)");
			}
			if (tag.equals("owl:FunctionalProperty") && !parentTerm.isEmpty())
			{
				ps.println(indent + "(instance " + parentTerm + " SingleValuedRelation)");
			}
			if (tag.equals("owl:SymmetricProperty") && !parentTerm.isEmpty())
			{
				ps.println(indent + "(instance " + parentTerm + " SymmetricRelation)");
			}
		}
		else if (tag.equals("rdfs:domain"))
		{
			value = se.getAttribute("rdf:resource");
			if (!value.isEmpty())
			{
				if (value.contains("#"))
				{
					value = value.substring(value.indexOf("#") + 1);
				}
				value = stringToKIFid(value);
				if (!value.isEmpty() && parentTerm != null)
				{
					ps.println(indent + "(domain " + parentTerm + " 1 " + value + ")");
				}
			}
		}
		else if (tag.equals("rdfs:range"))
		{
			value = se.getAttribute("rdf:resource");
			if (!value.isEmpty())
			{
				if (value.contains("#"))
				{
					value = value.substring(value.indexOf("#") + 1);
				}
				value = stringToKIFid(value);
				if (!value.isEmpty() && parentTerm != null)
				{
					ps.println(indent + "(domain " + parentTerm + " 2 " + value + ")");
				}
			}
		}
		else if (tag.equals("rdfs:comment"))
		{
			@Nullable String text = se.getTextContent();
			text = processStringForKIFOutput(text);
			if (parentTerm != null && text != null)
			{
				ps.println(wordWrap(indent + "(documentation " + parentTerm + " EnglishLanguage \"" + text + "\")", 70));
			}
		}
		else if (tag.equals("rdfs:label"))
		{
			@Nullable String text = se.getTextContent();
			text = processStringForKIFOutput(text);
			if (parentTerm != null && text != null)
			{
				ps.println(wordWrap(indent + "(termFormat EnglishLanguage " + parentTerm + " \"" + text + "\")", 70));
			}
		}
		else if (tag.equals("owl:inverseOf"))
		{
			@NotNull List<Element> children = getChildElements(se);
			if (children.size() > 0)
			{
				Element child = children.get(0);
				if (child.getTagName().equals("owl:ObjectProperty") || child.getTagName().equals("owl:InverseFunctionalProperty"))
				{
					value = child.getAttribute("rdf:ID");
					if (value.isEmpty())
					{
						value = child.getAttribute("rdf:about");
					}
					if (value.isEmpty())
					{
						value = child.getAttribute("rdf:resource");
					}
					if (value.contains("#"))
					{
						value = value.substring(value.indexOf("#") + 1);
					}
				}
			}
			value = stringToKIFid(value);
			if (value != null && parentTerm != null)
			{
				ps.println(indent + "(inverse " + parentTerm + " " + value + ")");
			}
		}
		else if (tag.equals("rdfs:subClassOf"))
		{
			value = getParentReference(se);
			value = stringToKIFid(value);
			if (value != null)
			{
				ps.println(indent + "(subclass " + parentTerm + " " + value + ")");
			}
			else
			{
				ps.println(";; missing or unparsed subclass statment for " + parentTerm);
			}
		}
		else if (tag.equals("owl:Restriction"))
		{
		}
		else if (tag.equals("owl:onProperty"))
		{
		}
		else if (tag.equals("owl:unionOf"))
		{
			return;
		}
		else if (tag.equals("owl:complimentOf"))
		{
			return;
		}
		else if (tag.equals("owl:intersectionOf"))
		{
			return;
		}
		else if (tag.equals("owl:cardinality"))
		{
		}
		else if (tag.equals("owl:FunctionalProperty"))
		{
			value = se.getAttribute("rdf:about");
			if (value != null)
			{
				if (value.contains("#"))
				{
					value = value.substring(value.indexOf("#") + 1);
				}
				value = stringToKIFid(value);
				ps.println(indent + "(instance " + value + " SingleValuedRelation)");
			}
		}
		else if (tag.equals("owl:minCardinality"))
		{
		}
		else if (tag.equals("owl:maxCardinality"))
		{
		}
		else if (tag.equals("rdf:type"))
		{
			value = getParentReference(se);
			value = stringToKIFid(value);
			if (value != null)
			{
				ps.println(indent + "(instance " + parentTerm + " " + value + ")");
			}
			else
			{
				ps.println(";; missing or unparsed subclass statment for " + parentTerm);
			}
		}
		else
		{
			value = se.getAttribute("rdf:resource");
			if (!value.isEmpty())
			{
				if (value.contains("#"))
				{
					value = value.substring(value.indexOf("#") + 1);
				}
				value = stringToKIFid(value);
				tag = stringToKIFid(tag);
				if (!value.isEmpty() && parentTerm != null)
				{
					ps.println(indent + "(" + tag + " " + parentTerm + " " + value + ")");
				}
			}
			else
			{
				@Nullable String text = se.getTextContent();
				@NotNull String datatype = se.getAttribute("rdf:datatype");
				text = processStringForKIFOutput(text);
				if (!datatype.endsWith("integer") && !datatype.endsWith("decimal"))
				{
					text = "\"" + text + "\"";
				}
				tag = stringToKIFid(tag);
				if (!text.isEmpty() && !text.equals("\"\""))
				{
					if (parentTerm != null && !tag.isEmpty())
					{
						ps.println(indent + "(" + tag + " " + parentTerm + " " + text + ")");
					}
				}
				else
				{
					@NotNull List<Element> children = getChildElements(se);
					if (children.size() > 0)
					{
						Element child = children.get(0);
						if (child.getTagName().equals("owl:Class"))
						{
							value = child.getAttribute("rdf:ID");
							if (!value.isEmpty())
							{
								value = child.getAttribute("rdf:about");
							}
							if (value.contains("#"))
							{
								value = value.substring(value.indexOf("#") + 1);
							}
							if (!value.isEmpty() && parentTerm != null)
							{
								ps.println(indent + "(" + tag + " " + parentTerm + " " + value + ")");
							}
						}
					}
				}
			}
		}

		if (existential != null)
		{
			ps.println("(exists (" + existential + ") ");
			if (getChildElements(se).size() > 1)
			{
				ps.println("  (and ");
				indent = indent + "    ";
				parens = "))";
			}
			else
			{
				indent = indent + "  ";
				parens = ")";
			}
		}

		NamedNodeMap s = se.getAttributes();
		for (int i = 0; i < s.getLength(); i++)
		{
			Node n = s.item(i);
			@NotNull String att = n.getNodeName();
			String val = se.getNodeValue();
		}

		@NotNull List<Element> al = getChildElements(se);
		for (@NotNull Element child : al)
		{
			decode(ps, child, parentTerm, tag, indent);
		}
		if (existential != null)
		{
			ps.println(parens);
		}
	}

	/**
	 * Get parent reference
	 */
	@Nullable
	private static String getParentReference(@NotNull Element se)
	{
		@Nullable String value = null;
		@NotNull List<Element> children = getChildElements(se);
		if (children.size() > 0)
		{
			Element child = children.get(0);
			if (child.getTagName().equals("owl:Class"))
			{
				value = child.getAttribute("rdf:ID");
				if (value.isEmpty())
				{
					value = child.getAttribute("rdf:about");
				}
				if (value.contains("#"))
				{
					value = value.substring(value.indexOf("#") + 1);
				}
			}
		}
		else
		{
			value = se.getAttribute("rdf:resource");
			if (!value.isEmpty())
			{
				if (value.contains("#"))
				{
					value = value.substring(value.indexOf("#") + 1);
				}
			}
		}
		return stringToKIFid(value);
	}

	/**
	 * Init once
	 */
	public void init() throws IOException
	{
		axiomMap = createAxiomMap();
		functionTable = createFunctionTable();
		if (wn != null)
		{
			wn.init();
		}
		if (yago != null)
		{
			yago.init();
		}
	}

	/**
	 * Show help
	 */
	private static void showHelp()
	{
		System.out.println("OWL translator class");
		System.out.println("  options:");
		System.out.println("  -h - show this help screen");
		System.out.println("  -t <fname> - read OWL file and write translation to fname.kif");
		System.out.println("  -s - translate and write OWL version of kb to .owl");
		System.out.println("  -y - translate and write OWL version of kb including YAGO mappings to stdout");
	}

	/**
	 * Main
	 */
	public static void main(@Nullable String[] args) throws IOException
	{
		if (args != null && args.length > 0 && "-h".equals(args[0]))
		{
			showHelp();
			return;
		}

		@NotNull BaseKB kb = new BaseSumoProvider().load();

		if (args != null && args.length > 0)
		{
			boolean read = false;
			boolean withWordNet;
			boolean withYago;

			for (@NotNull String arg : args)
			{
				switch (arg)
				{
					case "-r":
						read = true;
						break;
					case "-w":
						withWordNet = true;
						break;
					case "-y":
						withYago = true;
						break;
				}
			}

			// read
			if (read)
			{
				// read OWL file and write translation to fname.kif"
				OWLTranslator2.read(args[1]);
				return;
			}

			// construct
			@Nullable WordNet wn = WITH_WORDNET && withWordNet ? new WordNet() : null;
			@Nullable Yago yago = WITH_YAGO && withYago ? new Yago() : null;

			@NotNull OWLTranslator2 ot = new OWLTranslator2(kb, wn, yago);
			ot.init();

			// write version of kb
			ot.writeDefs();
			ot.write();
		}
		else
		{
			showHelp();
		}
	}
}
