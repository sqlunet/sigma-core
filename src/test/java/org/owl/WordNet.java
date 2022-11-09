/*
 * Copyright (c) 2022.
 * This code is copyright Bernard Bou <1313ou@gmail.com>
 * This software is released under the GNU Public License 3 <http://www.gnu.org/copyleft/gpl.html>.
 */

package org.owl;

import java.io.PrintStream;
import java.util.*;

class WordNet
{
	public Map<String, Collection<String>> verbFrames;

	public static String getPOSfromKey(final String sense)
	{
		return null;
	}

	public static String getWordFromKey(final String sense)
	{
		return null;
	}

	public static String posLettersToNumber(final String pos)
	{
		return null;
	}

	/**
	 * Write WordNet class definitions
	 */
	static void writeWordNetClassDefinitions(PrintStream ps)
	{
		List<String> WordNetClasses = List.of("Synset", "NounSynset", "VerbSynset", "AdjectiveSynset", "AdverbSynset");
		for (final String term : WordNetClasses)
		{
			ps.println("<owl:Class rdf:about=\"#" + term + "\">");
			ps.println("  <rdfs:label xml:lang=\"en\">" + term + "</rdfs:label>");
			if (!term.equals("Synset"))
			{
				ps.println("  <rdfs:subClassOf rdf:resource=\"#Synset\"/>");
				String POS = term.substring(0, term.indexOf("Synset"));
				ps.println("  <rdfs:comment xml:lang=\"en\">A group of " + POS + "s having the same meaning.</rdfs:comment>");
			}
			else
			{
				ps.println("  <rdfs:comment xml:lang=\"en\">A group of words having the same meaning.</rdfs:comment>");
			}
			ps.println("</owl:Class>");
		}
		ps.println("<owl:Class rdf:about=\"#WordSense\">");
		ps.println("  <rdfs:label xml:lang=\"en\">word sense</rdfs:label>");
		ps.println("  <rdfs:comment xml:lang=\"en\">A particular sense of a word.</rdfs:comment>");
		ps.println("</owl:Class>");
		ps.println("<owl:Class rdf:about=\"#Word\">");
		ps.println("  <rdfs:label xml:lang=\"en\">word</rdfs:label>");
		ps.println("  <rdfs:comment xml:lang=\"en\">A particular word.</rdfs:comment>");
		ps.println("</owl:Class>");
		ps.println("<owl:Class rdf:about=\"#VerbFrame\">");
		ps.println("  <rdfs:label xml:lang=\"en\">verb frame</rdfs:label>");
		ps.println("  <rdfs:comment xml:lang=\"en\">A string template showing allowed form of use of a verb.</rdfs:comment>");
		ps.println("</owl:Class>");
	}

	/**
	 * Write OWL format for SUMO-WordNet mappings.
	 *
	 * @param synset is a POS prefixed synset number
	 */
	static void writeWordNetSynset(PrintStream ps, String synset)
	{
		if (synset.startsWith("WN30-"))
		{
			synset = synset.substring(5);
		}
		Map<String, String> al = wn.synsetsToWords.get(synset);
		if (al != null)
		{
			ps.println("<owl:Thing rdf:about=\"#WN30-" + synset + "\">");
			String parent = "Noun";
			switch (synset.charAt(0))
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
			if (al.size() > 0)
			{
				ps.println("  <rdfs:label>" + al.get(0) + "</rdfs:label>");
			}
			for (int i = 0; i < al.size(); i++)
			{
				String word = al.get(i);
				String wordAsID = OWLTranslator2.stringToKIFid(word);
				ps.println("  <wnd:word rdf:resource=\"#WN30Word-" + wordAsID + "\"/>");
			}
			String doc = null;
			switch (synset.charAt(0))
			{
				case '1':
					doc = wn.nounDocumentationHash.get(synset.substring(1));
					break;
				case '2':
					doc = wn.verbDocumentationHash.get(synset.substring(1));
					break;
				case '3':
					doc = wn.adjectiveDocumentationHash.get(synset.substring(1));
					break;
				case '4':
					doc = wn.adverbDocumentationHash.get(synset.substring(1));
					break;
			}
			doc = OWLTranslator2.processStringForXMLOutput(doc);
			ps.println("  <rdfs:comment xml:lang=\"en\">" + doc + "</rdfs:comment>");
			var al2 = wn.relations.get(synset);
			if (al2 != null)
			{
				for (AVPair avp : al2)
				{
					String rel = OWLTranslator2.stringToKIFid(avp.attribute);
					ps.println("  <wnd:" + rel + " rdf:resource=\"#WN30-" + avp.value + "\"/>");
				}
			}
			ps.println("</owl:Thing>");
		}
	}

	/**
	 * Write words' senses
	 */
	private static void writeWordsToSenses(PrintStream ps)
	{
		for (final String word : wn.wordsToSenseKeys.keySet())
		{
			writeOneWordToSenses(ps, word);
		}
	}

	/**
	 * Write word's senses
	 */
	static void writeOneWordToSenses(PrintStream ps, String word)
	{

		String wordAsID = OWLTranslator2.stringToKIFid(word);
		ps.println("<owl:Thing rdf:about=\"#WN30Word-" + wordAsID + "\">");
		ps.println("  <rdf:type rdf:resource=\"#Word\"/>");
		ps.println("  <rdfs:label xml:lang=\"en\">" + word + "</rdfs:label>");
		String wordOrPhrase = "word";
		if (word.contains("_"))
		{
			wordOrPhrase = "phrase";
		}
		ps.println("  <rdfs:comment xml:lang=\"en\">The English " + wordOrPhrase + " \"" + word + "\".</rdfs:comment>");
		List<String> senses = wn.wordsToSenseKeys.get(word);
		if (senses != null)
		{
			for (String sense : senses)
			{
				ps.println("  <wnd:senseKey rdf:resource=\"#WN30WordSense-" + sense + "\"/>");
			}
		}
		else
		{
			System.out.println("Error in OWLtranslator.writeOneWordToSenses(): no senses for word: " + word);
		}
		ps.println("</owl:Thing>");
	}

	/**
	 * Write WordNet definition relations
	 */
	static void writeWordNetRelationDefinitions(PrintStream ps)
	{
		List<String> WordNetRelations = List.of("antonym", "hypernym", "instance-hypernym", "hyponym", "instance-hyponym", "member-holonym", "substance-holonym", "part-holonym", "member-meronym", "substance-meronym", "part-meronym", "attribute", "derivationally-related", "domain-topic", "member-topic", "domain-region", "member-region", "domain-usage", "member-usage", "entailment", "cause", "also-see", "verb-group", "similar-to", "participle", "pertainym");
		for (final String rel : WordNetRelations)
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
		ps.println("  <rdfs:domain rdf:resource=\"#Word\" />");
		ps.println("  <rdfs:range rdf:resource=\"rdfs:Literal\" />");
		ps.println("  <rdfs:label xml:lang=\"en\">singular</rdfs:label>");
		ps.println("  <rdfs:comment xml:lang=\"en\">A relation between a WordNet synset and a word\n" + "which is a member of the synset.</rdfs:comment>");
		ps.println("</owl:ObjectProperty>");
		ps.println("<owl:ObjectProperty rdf:about=\"#infinitive\">");
		ps.println("  <rdfs:domain rdf:resource=\"#Word\" />");
		ps.println("  <rdfs:range rdf:resource=\"rdfs:Literal\" />");
		ps.println("  <rdfs:label xml:lang=\"en\">infinitive</rdfs:label>");
		ps.println("  <rdfs:comment xml:lang=\"en\">A relation between a word\n" + " in its past tense and infinitive form.</rdfs:comment>");
		ps.println("</owl:ObjectProperty>");
		ps.println("<owl:ObjectProperty rdf:about=\"#senseKey\">");
		ps.println("  <rdfs:domain rdf:resource=\"#Word\" />");
		ps.println("  <rdfs:range rdf:resource=\"#WordSense\" />");
		ps.println("  <rdfs:label xml:lang=\"en\">sense key</rdfs:label>");
		ps.println("  <rdfs:comment xml:lang=\"en\">A relation between a word\n" + "and a particular sense of the word.</rdfs:comment>");
		ps.println("</owl:ObjectProperty>");
		ps.println("<owl:ObjectProperty rdf:about=\"#synset\">");
		ps.println("  <rdfs:domain rdf:resource=\"#WordSense\" />");
		ps.println("  <rdfs:range rdf:resource=\"#Synset\" />");
		ps.println("  <rdfs:label xml:lang=\"en\">synset</rdfs:label>");
		ps.println("  <rdfs:comment xml:lang=\"en\">A relation between a sense of a particular word\n" + "and the synset in which it appears.</rdfs:comment>");
		ps.println("</owl:ObjectProperty>");
		ps.println("<owl:ObjectProperty rdf:about=\"#verbFrame\">");
		ps.println("  <rdfs:domain rdf:resource=\"#WordSense\" />");
		ps.println("  <rdfs:range rdf:resource=\"#VerbFrame\" />");
		ps.println("  <rdfs:label xml:lang=\"en\">verb frame</rdfs:label>");
		ps.println("  <rdfs:comment xml:lang=\"en\">A relation between a verb word sense and a template that\n" + "describes the use of the verb in a sentence.</rdfs:comment>");
		ps.println("</owl:ObjectProperty>");
	}

	/**
	 * Write WordNet sense index
	 */
	private static void writeSenseIndex(PrintStream ps)
	{
		for (final String sense : wn.senseIndex.keySet())
		{
			String synset = wn.senseIndex.get(sense);
			ps.println("<owl:Thing rdf:about=\"#WN30WordSense-" + sense + "\">");
			ps.println("  <rdf:type rdf:resource=\"#WordSense\"/>");
			ps.println("  <rdfs:label xml:lang=\"en\">" + sense + "</rdfs:label>");
			ps.println("  <rdfs:comment xml:lang=\"en\">The WordNet word sense \"" + sense + "\".</rdfs:comment>");
			String pos = getPOSfromKey(sense);
			String word = getWordFromKey(sense);
			String posNum = posLettersToNumber(pos);
			ps.println("  <wnd:synset rdf:resource=\"#WN30-" + posNum + synset + "\"/>");
			if (posNum.equals("2"))
			{
				Collection<String> frames = wn.verbFrames.get(synset + "-" + word);
				if (frames != null)
				{
					for (String frame : frames)
					{
						ps.println("  <wnd:verbFrame rdf:resource=\"#WN30VerbFrame-" + frame + "\"/>");
					}
				}
			}
			ps.println("</owl:Thing>");
		}
	}

	/**
	 * Write WordNet links
	 */
	private static void writeWordNetLink(PrintStream ps, String term)
	{
		wn.initOnce();
		// get list of synsets with part of speech prepended to the synset number.
		Collection<String> al = wn.SUMOHash.get(term);
		if (al != null)
		{
			for (String synset : al)
			{
				String termMapping = null;
				// GetSUMO terms with the &% prefix and =, +, @ or [ suffix.
				switch (synset.charAt(0))
				{
					case '1':
						termMapping = wn.nounSUMOHash.get(synset.substring(1));
						break;
					case '2':
						termMapping = wn.verbSUMOHash.get(synset.substring(1));
						break;
					case '3':
						termMapping = wn.adjectiveSUMOHash.get(synset.substring(1));
						break;
					case '4':
						termMapping = wn.adverbSUMOHash.get(synset.substring(1));
						break;
				}
				String rel = null;
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
				ps.println("  <wnd:" + rel + " rdf:resource=\"&wnd;WN30-" + synset + "\"/>");
			}
		}
	}

	/**
	 * Write verb frames
	 */
	static void writeVerbFrames(PrintStream ps)
	{
		List<String> verbFrames = List.of("Something ----s", "Somebody ----s", "It is ----ing", "Something is ----ing PP", "Something ----s something Adjective/Noun", "Something ----s Adjective/Noun", "Somebody ----s Adjective", "Somebody ----s something", "Somebody ----s somebody", "Something ----s somebody", "Something ----s something", "Something ----s to somebody", "Somebody ----s on something", "Somebody ----s somebody something", "Somebody ----s something to somebody", "Somebody ----s something from somebody", "Somebody ----s somebody with something", "Somebody ----s somebody of something", "Somebody ----s something on somebody", "Somebody ----s somebody PP", "Somebody ----s something PP", "Somebody ----s PP", "Somebody's (body part) ----s", "Somebody ----s somebody to INFINITIVE", "Somebody ----s somebody INFINITIVE", "Somebody ----s that CLAUSE", "Somebody ----s to somebody", "Somebody ----s to INFINITIVE", "Somebody ----s whether INFINITIVE", "Somebody ----s somebody into V-ing something", "Somebody ----s something with something", "Somebody ----s INFINITIVE", "Somebody ----s VERB-ing", "It ----s that CLAUSE", "Something ----s INFINITIVE");
		for (int i = 0; i < verbFrames.size(); i++)
		{
			String frame = verbFrames.get(i);
			String numString = String.valueOf(i);
			if (numString.length() == 1)
			{
				numString = "0" + numString;
			}
			ps.println("<owl:Thing rdf:about=\"#WN30VerbFrame-" + numString + "\">");
			ps.println("  <rdfs:comment xml:lang=\"en\">" + frame + "</rdfs:comment>");
			ps.println("  <rdfs:label xml:lang=\"en\">" + frame + "</rdfs:label>");
			ps.println("  <rdf:type rdf:resource=\"#VerbFrame\"/>");
			ps.println("</owl:Thing>");
		}
	}

	/**
	 * Write WordNet exceptions
	 */
	private static void writeWordNetExceptions(PrintStream ps)
	{
		for (String plural : wn.exceptionNounHash.keySet())
		{
			String singular = wn.exceptionNounHash.get(plural);
			ps.println("<owl:Thing rdf:about=\"#" + plural + "\">");
			ps.println("  <wnd:singular>" + singular + "</wnd:singular>");
			ps.println("  <rdf:type rdf:resource=\"#Word\"/>");
			ps.println("  <rdfs:label xml:lang=\"en\">" + singular + "</rdfs:label>");
			ps.println("  <rdfs:comment xml:lang=\"en\">\"" + singular + "\", is the singular form" + " of the irregular plural \"" + plural + "\"</rdfs:comment>");
			ps.println("</owl:Thing>");
		}
		for (String past : wn.exceptionNounHash.keySet())
		{
			String infinitive = wn.exceptionVerbHash.get(past);
			ps.println("<owl:Thing rdf:about=\"#" + past + "\">");
			ps.println("  <wnd:infinitive>" + infinitive + "</wnd:infinitive>");
			ps.println("  <rdf:type rdf:resource=\"#Word\"/>");
			ps.println("  <rdfs:label xml:lang=\"en\">" + past + "</rdfs:label>");
			ps.println("  <rdfs:comment xml:lang=\"en\">\"" + past + "\", is the irregular past tense form" + " of the infinitive \"" + infinitive + "\"</rdfs:comment>");
			ps.println("</owl:Thing>");
		}
	}

	/**
	 * Write WordNet header
	 */
	static void writeWordNetHeader(PrintStream ps)
	{
		Date d = new Date();
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
	private static void writeWordNetTrailer(PrintStream ps)
	{
		ps.println("</rdf:RDF>");
	}

	/**
	 * Write OWL format for SUMO-WordNet mappings.
	 */
	public static void writeWordNet(PrintStream ps)
	{
		writeWordNetHeader(ps);
		writeWordNetRelationDefinitions(ps);
		writeWordNetClassDefinitions(ps);
		// Get POS-prefixed synsets.
		for (final String synset : wn.synsetsToWords.keySet())
		{
			writeWordNetSynset(ps, synset);
		}
		writeWordNetExceptions(ps);
		writeVerbFrames(ps);
		writeWordsToSenses(ps);
		writeSenseIndex(ps);
		writeWordNetTrailer(ps);
	}

	public void initOnce()
	{
	}

	static class AVPair
	{
		String attribute;
		String value;
	}

	static WordNet wn;

	public Map<String, Collection<String>> SUMOHash;

	public Map<String, String> nounSUMOHash;

	public Map<String, String> verbSUMOHash;

	public Map<String, String> adjectiveSUMOHash;

	public Map<String, String> adverbSUMOHash;

	public Map<String, String> nounDocumentationHash;

	public Map<String, String> verbDocumentationHash;

	public Map<String, String> adjectiveDocumentationHash;

	public Map<String, String> adverbDocumentationHash;


	public Map<String, List<AVPair>> relations;

	public Map<String, Map<String, String>> synsetsToWords;

	public HashMap<String, List<String>> wordsToSenseKeys;

	public HashMap<String, String> senseIndex;

	public HashMap<String, String> exceptionNounHash;

	public HashMap<String, String> exceptionVerbHash;
}
