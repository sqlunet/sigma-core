/*
 * Copyright (c) 2022.
 * This code is copyright Bernard Bou <1313ou@gmail.com>
 * This software is released under the GNU Public License 3 <http://www.gnu.org/copyleft/gpl.html>.
 */

package org.owl;

import org.sigma.core.NotNull;
import org.sigma.core.Nullable;

import java.io.PrintStream;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static org.owl.WordNet.*;

public class WordNetOwl
{
	public static final String SYNSET_RESOURCE = "";

	public static final String WORD_RESOURCE = "Word";

	public static final String SENSE_RESOURCE = "WordSense";

	public static final String WNPREFIX = "WN30";

	public static final String WNSYNSET_RESOURCE = WNPREFIX + SYNSET_RESOURCE + '-';

	public static final String WNWORD_RESOURCE = WNPREFIX + WORD_RESOURCE + '-';

	public static final String WNSENSE_RESOURCE = WNPREFIX + SENSE_RESOURCE + '-';

	static WordNet wn;

	// S Y N S E T S

	/**
	 * Write WordNet synsets
	 */
	public static void writeSynsets(@NotNull WordNet wn, @NotNull PrintStream ps)
	{
		for (@NotNull final String synset9 : wn.wordsBySynset9.keySet())
		{
			writeSynset(wn, ps, synset9);
		}
	}

	/**
	 * Write OWL format for SUMO-WordNet mappings.
	 *
	 * @param synset is a POS prefixed synset number
	 */
	public static void writeSynset(@NotNull WordNet wn, @NotNull PrintStream ps, @NotNull String synset)
	{
		String synset9 = synset;
		if (synset.startsWith(WNPREFIX + '-'))
		{
			synset9 = synset9.substring(WNPREFIX.length() + 1);
		}
		List<String> members = wn.wordsBySynset9.get(synset9);
		if (members != null)
		{
			// members
			ps.println("<owl:Thing rdf:about=\"#" + WNSYNSET_RESOURCE + synset9 + "\">");
			@NotNull String parent = "Entity";
			switch (synset9.charAt(0))
			{
				case '1':
					parent = "NounSynset";
					break;
				case '2':
					parent = "VerbSynset";
					break;
				case '3':
					parent = "AdjectiveSynset";
					break;
				case '4':
					parent = "AdverbSynset";
					break;
			}
			ps.println("  <rdf:type rdf:resource=\"" + (parent.equals("Entity") ? "&owl;Thing" : "&wnd;" + parent) + "\"/>");
			if (members.size() > 0)
			{
				ps.println("  <rdfs:label>" + members.get(0) + "</rdfs:label>");
			}
			for (String word : members)
			{
				@Nullable String wordAsID = OWLTranslator2.stringToKIFid(word);
				ps.println("  <wnd:word rdf:resource=\"#" + WNWORD_RESOURCE + wordAsID + "\"/>");
			}

			// doc
			@Nullable String doc = null;
			switch (synset9.charAt(0))
			{
				case '1':
					doc = wn.nounDocumentation.get(synset9.substring(1));
					break;
				case '2':
					doc = wn.verbDocumentation.get(synset9.substring(1));
					break;
				case '3':
					doc = wn.adjectiveDocumentation.get(synset9.substring(1));
					break;
				case '4':
					doc = wn.adverbDocumentation.get(synset9.substring(1));
					break;
			}
			doc = OWLTranslator2.processStringForXMLOutput(doc);
			ps.println("  <rdfs:comment xml:lang=\"en\">" + doc + "</rdfs:comment>");

			// relations
			var relations = wn.relations.get(synset9);
			if (relations != null)
			{
				for (@NotNull Entry<String, String> relation : relations)
				{
					@Nullable String reln = OWLTranslator2.stringToKIFid(relation.attribute);
					ps.println("  <wnd:" + reln + " rdf:resource=\"#" + WNSYNSET_RESOURCE + relation.value + "\"/>");
				}
			}
			ps.println("</owl:Thing>");
		}
	}

	/**
	 * Write OWL format for SUMO-WordNet mappings.
	 */
	public static void writeOWLSynsets(@NotNull PrintStream ps)
	{
		writeSynsets(wn, ps);
	}

	/**
	 * Write OWL format for SUMO-WordNet mappings.
	 *
	 * @param synset is a POS prefixed synset number
	 */
	public static void writeOWLSynset(@NotNull PrintStream ps, @NotNull String synset)
	{
		writeSynset(wn, ps, synset);
	}

	// S E N S E S

	/**
	 * Write WordNet senses
	 */
	public static void writeSenses(@NotNull WordNet wn, @NotNull PrintStream ps)
	{
		for (final String sense : wn.synsets8BySense.keySet())
		{
			String synset8 = wn.synsets8BySense.get(sense);
			@Nullable String kpos = getPOSFromSense(sense);
			@Nullable String kword = getWordFromSense(sense);
			POS pos = POS.parseCode(kpos);
			ps.println("<owl:Thing rdf:about=\"#" + WNSENSE_RESOURCE + sense + "\">");
			ps.println("  <rdf:type rdf:resource=\"#" + SENSE_RESOURCE + "\"/>");
			ps.println("  <rdfs:label xml:lang=\"en\">" + sense + "</rdfs:label>");
			ps.println("  <rdfs:comment xml:lang=\"en\">The WordNet word sense \"" + sense + "\".</rdfs:comment>");
			ps.println("  <wnd:synset rdf:resource=\"#" + WNSYNSET_RESOURCE + pos.toSynset9(synset8) + "\"/>");

			// frames
			if (pos == POS.VERB)
			{
				Collection<String> frames = wn.verbFrames.get(synset8 + '-' + kword);
				if (frames != null)
				{
					for (String frame : frames)
					{
						ps.println("  <wnd:verbFrame rdf:resource=\"#" + WNPREFIX + "VerbFrame-" + frame + "\"/>");
					}
				}
			}
			ps.println("</owl:Thing>");
		}
	}

	/**
	 * Write words' senses
	 */
	public static void writeWordsAndSenses(@NotNull WordNet wn, @NotNull PrintStream ps)
	{
		for (@NotNull final String word : wn.sensesByWord.keySet())
		{
			writeWordAndSenses(wn, ps, word);
		}
	}

	/**
	 * Write word's senses
	 */
	static void writeWordAndSenses(@NotNull WordNet wn, @NotNull PrintStream ps, @NotNull String word)
	{
		// word
		@Nullable String wordAsID = OWLTranslator2.stringToKIFid(word);
		ps.println("<owl:Thing rdf:about=\"#" + WNWORD_RESOURCE + wordAsID + "\">");
		ps.println("  <rdf:type rdf:resource=\"#" + WORD_RESOURCE + "\"/>");
		ps.println("  <rdfs:label xml:lang=\"en\">" + word + "</rdfs:label>");
		@NotNull String wordOrPhrase = "word";
		if (word.contains("_"))
		{
			wordOrPhrase = "phrase";
		}
		ps.println("  <rdfs:comment xml:lang=\"en\">The English " + wordOrPhrase + " \"" + word + "\".</rdfs:comment>");

		// senses
		List<String> senses = wn.sensesByWord.get(word);
		if (senses != null)
		{
			for (String sense : senses)
			{
				ps.println("  <wnd:senseKey rdf:resource=\"#" + WNSENSE_RESOURCE + sense + "\"/>");
			}
		}
		else
		{
			throw new IllegalArgumentException("No senses for word: " + word);
		}
		ps.println("</owl:Thing>");
	}

	/**
	 * Write WordNet sense index
	 */
	public static void writeOWLSenses(@NotNull PrintStream ps)
	{
		writeSenses(wn, ps);
	}

	/**
	 * Write words' senses
	 */
	public static void writeOWLWordsAndSenses(@NotNull PrintStream ps)
	{
		writeWordsAndSenses(wn, ps);
	}

	/**
	 * Write word's senses
	 */
	static void writeOWLWordAndSenses(@NotNull PrintStream ps, @NotNull String word)
	{
		writeWordAndSenses(wn, ps, word);
	}

	// L I N K S

	/**
	 * Write WordNet links
	 */
	static void writeLink(@NotNull WordNet wn, @NotNull PrintStream ps, String term)
	{
		// Get list of synsets with part of speech prepended to the synset number.
		Collection<String> synsets9 = wn.SUMOTerms.get(term);
		if (synsets9 != null)
		{
			for (@NotNull String synset9 : synsets9)
			{
				// Get SUMO terms with the &% prefix and =, +, @ or [ suffix.
				@Nullable String termMapping = null;
				switch (synset9.charAt(0))
				{
					case '1':
						termMapping = wn.nounSUMOTerms.get(synset9.substring(1));
						break;
					case '2':
						termMapping = wn.verbSUMOTerms.get(synset9.substring(1));
						break;
					case '3':
						termMapping = wn.adjectiveSUMOTerms.get(synset9.substring(1));
						break;
					case '4':
						termMapping = wn.adverbSUMOTerms.get(synset9.substring(1));
						break;
				}
				@Nullable String rel = null;
				if (termMapping != null)
				{
					switch (termMapping.charAt(termMapping.length() - 1))
					{
						case '=':
							rel = "equivalenceRelation";
							break;
						case '+':
							rel = "subsumingRelation";
							break;
						case '@':
							rel = "instanceRelation";
							break;
						case ':':
							rel = "antiEquivalenceRelation";
							break;
						case '[':
							rel = "antiSubsumingRelation";
							break;
						case ']':
							rel = "antiInstanceRelation";
							break;
					}
				}
				ps.println("  <wnd:" + rel + " rdf:resource=\"&wnd;" + WNSYNSET_RESOURCE + synset9 + "\"/>");
			}
		}
	}

	/**
	 * Write WordNet links
	 */
	static void writeOWLLink(@NotNull PrintStream ps, String term)
	{
		writeLink(wn, ps, term);
	}

	// E X C E P T I O N S

	/**
	 * Write WordNet exceptions
	 */
	public static void writeExceptions(@NotNull WordNet wn, @NotNull PrintStream ps)
	{
		for (String plural : wn.nounExceptions.keySet())
		{
			String singular = wn.nounExceptions.get(plural);
			ps.println("<owl:Thing rdf:about=\"#" + plural + "\">");
			ps.println("  <wnd:singular>" + singular + "</wnd:singular>");
			ps.println("  <rdf:type rdf:resource=\"#" + WORD_RESOURCE + "\"/>");
			ps.println("  <rdfs:label xml:lang=\"en\">" + singular + "</rdfs:label>");
			ps.println("  <rdfs:comment xml:lang=\"en\">\"" + singular + "\", is the singular form" + " of the irregular plural \"" + plural + "\"</rdfs:comment>");
			ps.println("</owl:Thing>");
		}
		for (String past : wn.nounExceptions.keySet())
		{
			String infinitive = wn.verbExceptions.get(past);
			ps.println("<owl:Thing rdf:about=\"#" + past + "\">");
			ps.println("  <wnd:infinitive>" + infinitive + "</wnd:infinitive>");
			ps.println("  <rdf:type rdf:resource=\"#" + WORD_RESOURCE + "\"/>");
			ps.println("  <rdfs:label xml:lang=\"en\">" + past + "</rdfs:label>");
			ps.println("  <rdfs:comment xml:lang=\"en\">\"" + past + "\", is the irregular past tense form" + " of the infinitive \"" + infinitive + "\"</rdfs:comment>");
			ps.println("</owl:Thing>");
		}
	}

	/**
	 * Write WordNet exceptions
	 */
	public static void writeOWLExceptions(@NotNull PrintStream ps)
	{
		writeExceptions(wn, ps);
	}

	// D E F I N I T I O N S

	/**
	 * Write WordNet class definitions
	 */
	public static void writeOWLWordNetClassDefinitions(@NotNull PrintStream ps)
	{
		@NotNull List<String> WordNetClasses = List.of("Synset", "NounSynset", "VerbSynset", "AdjectiveSynset", "AdverbSynset");
		for (@NotNull final String term : WordNetClasses)
		{
			ps.println("<owl:Class rdf:about=\"#" + term + "\">");
			ps.println("  <rdfs:label xml:lang=\"en\">" + term + "</rdfs:label>");
			if (!term.equals("Synset"))
			{
				ps.println("  <rdfs:subClassOf rdf:resource=\"#Synset\"/>");
				@NotNull String POS = term.substring(0, term.indexOf("Synset"));
				ps.println("  <rdfs:comment xml:lang=\"en\">A group of " + POS + "s having the same meaning.</rdfs:comment>");
			}
			else
			{
				ps.println("  <rdfs:comment xml:lang=\"en\">A group of words having the same meaning.</rdfs:comment>");
			}
			ps.println("</owl:Class>");
		}
		ps.println("<owl:Class rdf:about=\"#" + SENSE_RESOURCE + "\">");
		ps.println("  <rdfs:label xml:lang=\"en\">word sense</rdfs:label>");
		ps.println("  <rdfs:comment xml:lang=\"en\">A particular sense of a word.</rdfs:comment>");
		ps.println("</owl:Class>");
		ps.println("<owl:Class rdf:about=\"#" + WORD_RESOURCE + "\">");
		ps.println("  <rdfs:label xml:lang=\"en\">word</rdfs:label>");
		ps.println("  <rdfs:comment xml:lang=\"en\">A particular word.</rdfs:comment>");
		ps.println("</owl:Class>");
		ps.println("<owl:Class rdf:about=\"#VerbFrame\">");
		ps.println("  <rdfs:label xml:lang=\"en\">verb frame</rdfs:label>");
		ps.println("  <rdfs:comment xml:lang=\"en\">A string template showing allowed form of use of a verb.</rdfs:comment>");
		ps.println("</owl:Class>");
	}

	/**
	 * Write WordNet definition relations
	 */
	public static void writeOWLWordNetRelationDefinitions(@NotNull PrintStream ps)
	{
		@NotNull List<String> WordNetRelations = List.of("antonym", "hypernym", "instance-hypernym", "hyponym", "instance-hyponym", "member-holonym", "substance-holonym", "part-holonym", "member-meronym", "substance-meronym", "part-meronym", "attribute", "derivationally-related", "domain-topic", "member-topic", "domain-region", "member-region", "domain-usage", "member-usage", "entailment", "cause", "also-see", "verb-group", "similar-to", "participle", "pertainym");
		for (@NotNull final String rel : WordNetRelations)
		{
			String tag;
			if (rel.equals("antonym") || rel.equals("similar-to") || rel.equals("verb-group") || rel.equals("derivationally-related"))
			{
				tag = "owl:SymmetricProperty";
			}
			else
			{
				tag = "owl:ObjectProperty";
			}
			ps.println("<" + tag + " rdf:about=\"#" + rel + "\">");
			ps.println("  <rdfs:label xml:lang=\"en\">" + rel + "</rdfs:label>");
			ps.println("  <rdfs:domain rdf:resource=\"#Synset\" />");
			ps.println("  <rdfs:range rdf:resource=\"#Synset\" />");
			ps.println("</" + tag + ">");
		}
		ps.println("<owl:ObjectProperty rdf:about=\"#word\">");
		ps.println("  <rdfs:domain rdf:resource=\"#Synset\" />");
		ps.println("  <rdfs:range rdf:resource=\"rdfs:Literal\" />");
		ps.println("  <rdfs:label xml:lang=\"en\">word</rdfs:label>");
		ps.println("  <rdfs:comment xml:lang=\"en\">A relation between a WordNet synset and a word\n" + "which is a member of the synset.</rdfs:comment>");
		ps.println("</owl:ObjectProperty>");
		ps.println("<owl:ObjectProperty rdf:about=\"#singular\">");
		ps.println("  <rdfs:domain rdf:resource=\"#" + WORD_RESOURCE + "\" />");
		ps.println("  <rdfs:range rdf:resource=\"rdfs:Literal\" />");
		ps.println("  <rdfs:label xml:lang=\"en\">singular</rdfs:label>");
		ps.println("  <rdfs:comment xml:lang=\"en\">A relation between a WordNet synset and a word\n" + "which is a member of the synset.</rdfs:comment>");
		ps.println("</owl:ObjectProperty>");
		ps.println("<owl:ObjectProperty rdf:about=\"#infinitive\">");
		ps.println("  <rdfs:domain rdf:resource=\"#" + WORD_RESOURCE + "\" />");
		ps.println("  <rdfs:range rdf:resource=\"rdfs:Literal\" />");
		ps.println("  <rdfs:label xml:lang=\"en\">infinitive</rdfs:label>");
		ps.println("  <rdfs:comment xml:lang=\"en\">A relation between a word\n" + " in its past tense and infinitive form.</rdfs:comment>");
		ps.println("</owl:ObjectProperty>");
		ps.println("<owl:ObjectProperty rdf:about=\"#senseKey\">");
		ps.println("  <rdfs:domain rdf:resource=\"#" + WORD_RESOURCE + "\" />");
		ps.println("  <rdfs:range rdf:resource=\"#" + SENSE_RESOURCE + "\" />");
		ps.println("  <rdfs:label xml:lang=\"en\">sense key</rdfs:label>");
		ps.println("  <rdfs:comment xml:lang=\"en\">A relation between a word\n" + "and a particular sense of the word.</rdfs:comment>");
		ps.println("</owl:ObjectProperty>");
		ps.println("<owl:ObjectProperty rdf:about=\"#synset\">");
		ps.println("  <rdfs:domain rdf:resource=\"#" + SENSE_RESOURCE + "\" />");
		ps.println("  <rdfs:range rdf:resource=\"#Synset\" />");
		ps.println("  <rdfs:label xml:lang=\"en\">synset</rdfs:label>");
		ps.println("  <rdfs:comment xml:lang=\"en\">A relation between a sense of a particular word\n" + "and the synset in which it appears.</rdfs:comment>");
		ps.println("</owl:ObjectProperty>");
		ps.println("<owl:ObjectProperty rdf:about=\"#verbFrame\">");
		ps.println("  <rdfs:domain rdf:resource=\"#" + SENSE_RESOURCE + "\" />");
		ps.println("  <rdfs:range rdf:resource=\"#VerbFrame\" />");
		ps.println("  <rdfs:label xml:lang=\"en\">verb frame</rdfs:label>");
		ps.println("  <rdfs:comment xml:lang=\"en\">A relation between a verb word sense and a template that\n" + "describes the use of the verb in a sentence.</rdfs:comment>");
		ps.println("</owl:ObjectProperty>");
	}

	/**
	 * Write verb frames
	 */
	public static void writeOWLVerbFrames(@NotNull PrintStream ps)
	{
		@NotNull List<String> verbFrames = List.of("Something ----s", "Somebody ----s", "It is ----ing", "Something is ----ing PP", "Something ----s something Adjective/Noun", "Something ----s Adjective/Noun", "Somebody ----s Adjective", "Somebody ----s something", "Somebody ----s somebody", "Something ----s somebody", "Something ----s something", "Something ----s to somebody", "Somebody ----s on something", "Somebody ----s somebody something", "Somebody ----s something to somebody", "Somebody ----s something from somebody", "Somebody ----s somebody with something", "Somebody ----s somebody of something", "Somebody ----s something on somebody", "Somebody ----s somebody PP", "Somebody ----s something PP", "Somebody ----s PP", "Somebody's (body part) ----s", "Somebody ----s somebody to INFINITIVE", "Somebody ----s somebody INFINITIVE", "Somebody ----s that CLAUSE", "Somebody ----s to somebody", "Somebody ----s to INFINITIVE", "Somebody ----s whether INFINITIVE", "Somebody ----s somebody into V-ing something", "Somebody ----s something with something", "Somebody ----s INFINITIVE", "Somebody ----s VERB-ing", "It ----s that CLAUSE", "Something ----s INFINITIVE");
		for (int i = 0; i < verbFrames.size(); i++)
		{
			String frame = verbFrames.get(i);
			@NotNull String numString = String.valueOf(i);
			if (numString.length() == 1)
			{
				numString = "0" + numString;
			}
			ps.println("<owl:Thing rdf:about=\"#" + WNPREFIX + "VerbFrame-" + numString + "\">");
			ps.println("  <rdfs:comment xml:lang=\"en\">" + frame + "</rdfs:comment>");
			ps.println("  <rdfs:label xml:lang=\"en\">" + frame + "</rdfs:label>");
			ps.println("  <rdf:type rdf:resource=\"#VerbFrame\"/>");
			ps.println("</owl:Thing>");
		}
	}

	// M A I N

	/**
	 * Write WordNet header
	 */
	static void writeOWLWordNetHeader(@NotNull PrintStream ps)
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
		ps.println("xmlns=\"http://www.ontologyportal.org/WordNet.owl#\"");
		ps.println("xml:base=\"http://www.ontologyportal.org/WordNet.owl\"");
		ps.println("xmlns:wnd =\"http://www.ontologyportal.org/WNDefs.owl#\"");
		ps.println("xmlns:kbd =\"http://www.ontologyportal.org/KBDefs.owl#\"");
		ps.println("xmlns:xsd=\"http://www.w3.org/2001/XMLSchema#\"");
		ps.println("xmlns:rdf =\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"");
		ps.println("xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\"");
		ps.println("xmlns:owl =\"http://www.w3.org/2002/07/owl#\">");
		ps.println("<owl:Ontology rdf:about=\"http://www.ontologyportal.org/WordNet.owl\">");
		ps.println("<rdfs:comment xml:lang=\"en\">An expression of the Princeton WordNet " + "( http://wordnet.princeton.edu ) " + "in OWL.  Use is subject to the Princeton WordNet license at " + "http://wordnet.princeton.edu/wordnet/license/</rdfs:comment>");
		ps.println("<rdfs:comment xml:lang=\"en\">Produced on date: " + d + "</rdfs:comment>");
		ps.println("</owl:Ontology>");
	}

	/**
	 * Write WordNet trailer
	 */
	static void writeOWLWordNetTrailer(@NotNull PrintStream ps)
	{
		ps.println("</rdf:RDF>");
	}

	/**
	 * Write OWL format for SUMO-WordNet mappings.
	 */
	public static void writeWordNet(@NotNull WordNet wn, @NotNull PrintStream ps)
	{
		writeOWLWordNetHeader(ps);

		writeSynsets(wn, ps);
		writeWordsAndSenses(wn, ps);
		writeSenses(wn, ps);
		writeExceptions(wn, ps);

		writeOWLWordNetClassDefinitions(ps);
		writeOWLWordNetRelationDefinitions(ps);
		writeOWLVerbFrames(ps);

		writeOWLWordNetTrailer(ps);
	}

	/**
	 * Write OWL format for SUMO-WordNet mappings.
	 */
	public static void writeOWLWordNet(@NotNull PrintStream ps)
	{
		writeWordNet(wn, ps);
	}
}
