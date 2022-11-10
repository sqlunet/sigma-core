/*
 * Copyright (c) 2022.
 * This code is copyright Bernard Bou <1313ou@gmail.com>
 * This software is released under the GNU Public License 3 <http://www.gnu.org/copyleft/gpl.html>.
 */

package org.owl;

import org.sigma.core.NotNull;
import org.sigma.core.Nullable;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class WordNet
{
	static class Entry<K, V>
	{
		final K attribute;
		final V value;

		public Entry(final K attribute, final V value)
		{
			this.attribute = attribute;
			this.value = value;
		}
	}

	static WordNet wn;


	/**
	 * A HashMap where the keys are of the form word%POS:lex_filenum:lex_id (numeric POS)
	 * and values are 8 digit WordNet synset byte offsets. Note that all words are
	 * from index.sense, which reduces all words to lower case
	 */
	public HashMap<String, String> senseKeys = new HashMap<>();


	public HashMap<String, ArrayList<String>> wordsToSenseKeys = new HashMap<>();


	/**
	 * Keys are SUMO terms,
	 * Values are Collection of POS-prefixed 9-digit synset ids
	 * meaning that the part of speech code is prepended to the synset number.
	 */
	public Map<String, Collection<String>> SUMOHash = new HashMap<>();

	/**
	 * Keys are noun synset ids,
	 * Values are SUMO terms with the &% prefix and =, +, @ or [ suffix.
	 */
	public Map<String, String> nounSUMOHash = new HashMap<>();

	/**
	 * Keys are verb synset ids,
	 * Values are SUMO terms with the &% prefix and =, +, @ or [ suffix.
	 */
	public Map<String, String> verbSUMOHash = new HashMap<>();

	/**
	 * Keys are adjective synset ids,
	 * Values are SUMO terms with the &% prefix and =, +, @ or [ suffix.
	 */
	public Map<String, String> adjectiveSUMOHash = new HashMap<>();

	/**
	 * Keys are adverb synset ids,
	 * Values are SUMO terms with the &% prefix and =, +, @ or [ suffix.
	 */
	public Map<String, String> adverbSUMOHash = new HashMap<>();

	/**
	 * Keys are POS-prefixed synset ids.
	 * Values are Lists of words.
	 * Note that the order of words in the file is preserved.
	 */
	public Map<String, List<String>> synsetsToWords = new HashMap<>();

	/**
	 * Keys are words as keys
	 * Values are Lists of word senses which are Strings of the form
	 * word_POS_num (alpha POS like "VB") signifying the word, part of speech and number of
	 * the sense in WordNet.
	 * Note that all words are from index.sense, which reduces all words to lower case
	 */
	public Map<String, List<String>> wordToSenseKeys = new HashMap<>();

	/**
	 * Keys are of the form word%POS:lex_filenum:lex_id (numeric POS)
	 * Values are 8 digit WordNet synset byte offsets.
	 * Note that all words are from index.sense, which reduces all words to lower case
	 */
	public Map<String, String> senseKeyToSynset = new HashMap<>();

	/**
	 * Keys are of the form word_POS_sensenum (alpha POS like "VB")
	 * Values are 8 digit WordNet synset byte offsets.
	 * Note that all words are from index.sense, which reduces all words to lower case
	 */
	public HashMap<String, String> senseIndex = new HashMap<>();

	/**
	 * Keys are 9 digit POS prefixed WordNet synset byte offsets
	 * Values are of the form word_POS_sensenum (alpha POS like "VB").
	 * Note that all words are from index.sense, which reduces
	 * all words to lower case
	 */
	public Map<String, String> senseInvIndex = new HashMap<>();

	public Map<String, List<Entry<String, String>>> relations = new HashMap<>();

	/**
	 * Irregular plural forms.
	 * Key is the plural,
	 * Value is the singular.
	 */
	public Map<String, String> nounExceptions = new HashMap<>();

	/**
	 * Irregular plural forms.
	 * Key is the singular.
	 * Value is the plural,
	 */
	public Map<String, String> nounInvExceptions = new HashMap<>();

	/**
	 * Irregular past tenses
	 * Key is the past tense,
	 * Value is infinitive (without "to")
	 */
	public Map<String, String> verbExceptions = new HashMap<>();

	/**
	 * Irregular past tenses
	 * Key is infinitive (without "to")
	 * Value is the past tense,
	 */
	public Map<String, String> verbInvExceptions = new HashMap<>();

	public Map<String, String> nounDocumentation = new HashMap<>();

	public Map<String, String> verbDocumentation = new HashMap<>();

	public Map<String, String> adjectiveDocumentation = new HashMap<>();

	public Map<String, String> adverbDocumentation = new HashMap<>();

	/**
	 * Keys are 8 digit WordNet synset byte offsets or synsets appended
	 * with a dash and a specific word such as "12345678-foo".
	 * Values are Colelctions of verb frame numbers as strings.
	 */
	public Map<String, Collection<String>> verbFrames = new HashMap<>();

	/**
	 * Key is a 9-digit POS-prefixed sense
	 * Value is the number of times that sense occurs in the Brown corpus.
	 */
	public Map<String, Integer> senseFrequencies = new HashMap<>();

	/**
	 * Key is a word
	 * Value is a set of pairs with
	 * -the attribute is a 9-digit POS-prefixed sense which is the value of the pair,
	 * -the number of times that sense occurs in the Brown corpus, which is the key of the pair
	 */
	public Map<String, Set<Entry<String, String>>> wordFrequencies = new HashMap<>();

	/**
	 * Key is a word sense of the form word_POS_num signifying
	 * the word, part of speech and number of the sense in WordNet.
	 * Value is a Map of words and the number of times that word cooccurs in sentences
	 * with the word sense given in the key.
	 */
	public Map<String, Map<String, Integer>> wordCoFrequencies = new HashMap<>();

	/**
	 * Keys are uppercase words
	 * Values are their possibly mixed case original versions
	 */
	public Map<String, String> caseMap = new HashMap<>();

	/**
	 * List of English "stop words" such as "a", "at", "them", which have no or little
	 * inherent meaning when taken alone.
	 */
	public List<String> stopwords = new ArrayList<>();

	// W R I T E

	/**
	 * Write WordNet class definitions
	 */
	static void writeWordNetClassDefinitions(@NotNull PrintStream ps)
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
	static void writeWordNetSynset(@NotNull PrintStream ps, @NotNull String synset)
	{
		if (synset.startsWith("WN30-"))
		{
			synset = synset.substring(5);
		}
		List<String> members = wn.synsetsToWords.get(synset);
		if (members != null)
		{
			ps.println("<owl:Thing rdf:about=\"#WN30-" + synset + "\">");
			@NotNull String parent = "Noun";
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
			if (members.size() > 0)
			{
				ps.println("  <rdfs:label>" + members.get(0) + "</rdfs:label>");
			}
			for (String word : members)
			{
				@Nullable String wordAsID = OWLTranslator2.stringToKIFid(word);
				ps.println("  <wnd:word rdf:resource=\"#WN30Word-" + wordAsID + "\"/>");
			}
			@Nullable String doc = null;
			switch (synset.charAt(0))
			{
				case '1':
					doc = wn.nounDocumentation.get(synset.substring(1));
					break;
				case '2':
					doc = wn.verbDocumentation.get(synset.substring(1));
					break;
				case '3':
					doc = wn.adjectiveDocumentation.get(synset.substring(1));
					break;
				case '4':
					doc = wn.adverbDocumentation.get(synset.substring(1));
					break;
			}
			doc = OWLTranslator2.processStringForXMLOutput(doc);
			ps.println("  <rdfs:comment xml:lang=\"en\">" + doc + "</rdfs:comment>");
			var al2 = wn.relations.get(synset);
			if (al2 != null)
			{
				for (@NotNull Entry<String, String> avp : al2)
				{
					@Nullable String rel = OWLTranslator2.stringToKIFid(avp.attribute);
					ps.println("  <wnd:" + rel + " rdf:resource=\"#WN30-" + avp.value + "\"/>");
				}
			}
			ps.println("</owl:Thing>");
		}
	}

	/**
	 * Write words' senses
	 */
	private static void writeWordsToSenses(@NotNull PrintStream ps)
	{
		for (@NotNull final String word : wn.wordToSenseKeys.keySet())
		{
			writeOneWordToSenses(ps, word);
		}
	}

	/**
	 * Write word's senses
	 */
	static void writeOneWordToSenses(@NotNull PrintStream ps, @NotNull String word)
	{
		@Nullable String wordAsID = OWLTranslator2.stringToKIFid(word);
		ps.println("<owl:Thing rdf:about=\"#WN30Word-" + wordAsID + "\">");
		ps.println("  <rdf:type rdf:resource=\"#Word\"/>");
		ps.println("  <rdfs:label xml:lang=\"en\">" + word + "</rdfs:label>");
		@NotNull String wordOrPhrase = "word";
		if (word.contains("_"))
		{
			wordOrPhrase = "phrase";
		}
		ps.println("  <rdfs:comment xml:lang=\"en\">The English " + wordOrPhrase + " \"" + word + "\".</rdfs:comment>");
		List<String> senses = wn.wordToSenseKeys.get(word);
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
	static void writeWordNetRelationDefinitions(@NotNull PrintStream ps)
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
	private static void writeSenseIndex(@NotNull PrintStream ps)
	{
		for (final String sense : wn.senseIndex.keySet())
		{
			String synset = wn.senseIndex.get(sense);
			ps.println("<owl:Thing rdf:about=\"#WN30WordSense-" + sense + "\">");
			ps.println("  <rdf:type rdf:resource=\"#WordSense\"/>");
			ps.println("  <rdfs:label xml:lang=\"en\">" + sense + "</rdfs:label>");
			ps.println("  <rdfs:comment xml:lang=\"en\">The WordNet word sense \"" + sense + "\".</rdfs:comment>");
			@Nullable String pos = getPOSfromKey(sense);
			@Nullable String word = getWordFromKey(sense);
			int posNum = posLettersToNumber(pos);
			ps.println("  <wnd:synset rdf:resource=\"#WN30-" + posNum + synset + "\"/>");
			if (posNum == 2)
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
	private static void writeWordNetLink(@NotNull PrintStream ps, String term) throws IOException
	{
		wn.initOnce();
		// get list of synsets with part of speech prepended to the synset number.
		Collection<String> al = wn.SUMOHash.get(term);
		if (al != null)
		{
			for (@NotNull String synset : al)
			{
				@Nullable String termMapping = null;
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
				ps.println("  <wnd:" + rel + " rdf:resource=\"&wnd;WN30-" + synset + "\"/>");
			}
		}
	}

	/**
	 * Write verb frames
	 */
	static void writeVerbFrames(@NotNull PrintStream ps)
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
	private static void writeWordNetExceptions(@NotNull PrintStream ps)
	{
		for (String plural : wn.nounExceptions.keySet())
		{
			String singular = wn.nounExceptions.get(plural);
			ps.println("<owl:Thing rdf:about=\"#" + plural + "\">");
			ps.println("  <wnd:singular>" + singular + "</wnd:singular>");
			ps.println("  <rdf:type rdf:resource=\"#Word\"/>");
			ps.println("  <rdfs:label xml:lang=\"en\">" + singular + "</rdfs:label>");
			ps.println("  <rdfs:comment xml:lang=\"en\">\"" + singular + "\", is the singular form" + " of the irregular plural \"" + plural + "\"</rdfs:comment>");
			ps.println("</owl:Thing>");
		}
		for (String past : wn.nounExceptions.keySet())
		{
			String infinitive = wn.verbExceptions.get(past);
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
	static void writeWordNetHeader(@NotNull PrintStream ps)
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
	static void writeWordNetTrailer(@NotNull PrintStream ps)
	{
		ps.println("</rdf:RDF>");
	}

	/**
	 * Write OWL format for SUMO-WordNet mappings.
	 */
	public static void writeWordNet(@NotNull PrintStream ps)
	{
		writeWordNetHeader(ps);
		writeWordNetRelationDefinitions(ps);
		writeWordNetClassDefinitions(ps);
		// Get POS-prefixed synsets.
		for (@NotNull final String synset : wn.synsetsToWords.keySet())
		{
			writeWordNetSynset(ps, synset);
		}
		writeWordNetExceptions(ps);
		writeVerbFrames(ps);
		writeWordsToSenses(ps);
		writeSenseIndex(ps);
		writeWordNetTrailer(ps);
	}

	public void initOnce() throws IOException
	{
		load();
	}

	/**
	 * Read the WordNet files only on initialization of the class.
	 */
	private void load() throws IOException
	{
		makeFileMap();
		readNouns();
		readVerbs();
		readAdjectives();
		readAdverbs();
		createIgnoreCaseMap();
		//origMaxNounSynsetID = wn.maxNounSynsetID;
		//origMaxVerbSynsetID = wn.maxVerbSynsetID;
		readWordCoFrequencies();
		readStopWords();
		readSenseIndex(null);
		readSenseCount();
	}

	private void readNouns() throws IOException
	{
		// data
		Pattern pattern6 = Pattern.compile("^([0-9]{8})([\\S\\s]+)\\|\\s([\\S\\s]+?)\\s(\\(?&%\\S+[\\S\\s]+)$"); // "^([0-9]{8})([\\S\\s]+)\\|\\s([\\S\\s]+?)\\s(\\(?\\&\\%\\S+[\\S\\s]+)$"
		Pattern pattern7 = Pattern.compile("^([0-9]{8})([\\S\\s]+)\\|\\s([\\S\\s]+)$"); // "^([0-9]{8})([\\S\\s]+)\\|\\s([\\S\\s]+)$") -- no SUMO mapping
		File nounFile = getWnFile("noun_mappings");
		try (LineNumberReader lr = new LineNumberReader(new BufferedReader(new FileReader(nounFile))))
		{
			String line;
			while ((line = lr.readLine()) != null)
			{
				// progress
				if (lr.getLineNumber() % 1000 == 1)
				{
					System.out.print('.');
				}

				// process
				line = line.trim();
				Matcher m = pattern6.matcher(line);
				boolean anyAreNull = false;
				if (m.matches())
				{
					// 1-synset, 2-pointers, 3-docu, 4-SUMO term
					for (int i = 1; i <= 4; i++)
					{
						anyAreNull = (m.group(i) == null);
						if (anyAreNull)
						{
							break;
						}
					}
					if (!anyAreNull)
					{
						// 1-synset, 2-pointers, 3-docu, 4-SUMO term
						String synset = m.group(1);
						String pointers = m.group(2);
						String docu = m.group(3);
						String term = m.group(4);
						addSUMOMapping(term, "1" + synset);
						setMaxNounSynsetID(synset);
						nounDocumentation.put(synset, docu); // 1-synset, 2-pointers, 3-docu, 4-SUMO term
						processPointers("1" + synset, pointers);
					}
				}
				else
				{
					m = pattern7.matcher(line);
					if (m.matches())
					{
						// 1-synset, 2-pointers, 3-docu, no SUMO term
						String synset = m.group(1);
						String pointers = m.group(2);
						String docu = m.group(3);
						nounDocumentation.put(synset, docu);
						setMaxNounSynsetID(synset);
						processPointers("1" + synset, pointers);
					}
					else
					{
						if (line.length() > 0 && line.charAt(0) != ';')
						{
							throw new IllegalArgumentException("No match in for line " + line);
						}
					}
				}
			}
		}

		// except
		// synset_offset  lex_filenum  ss_type  w_cnt  word  lex_id  [word  lex_id...]  p_cnt  [ptr...]  [frames...]  |   gloss
		Pattern pattern8 = Pattern.compile("(\\S+)\\s+(\\S+)"); // "(\\S+)\\s+(\\S+)"
		Pattern pattern9 = Pattern.compile("(\\S+)\\s+(\\S+)\\s+(\\S+)"); // "(\\S+)\\s+(\\S+)\\s+(\\S+)"
		File nounFileExcep = getWnFile("noun_exceptions");
		try (LineNumberReader lr = new LineNumberReader(new BufferedReader(new FileReader(nounFileExcep))))
		{

			String line;
			while ((line = lr.readLine()) != null)
			{
				Matcher m = pattern8.matcher(line);
				if (m.matches())
				{
					String plural = m.group(1);
					String singular = m.group(2);
					nounExceptions.put(plural, singular);
					nounInvExceptions.put(singular, plural);
				}
				else
				{
					m = pattern9.matcher(line);
					if (m.matches())
					{
						String plural = m.group(1);
						String singular = m.group(2);
						String singular2 = m.group(3);
						nounExceptions.put(plural, singular);
						nounInvExceptions.put(singular, plural);
						nounInvExceptions.put(singular2, plural);
					}
					else if (!line.isEmpty() && line.charAt(0) != ';')
					{
						throw new IllegalArgumentException("No match in for line " + line);
					}
				}
			}
		}
	}

	private void readVerbs() throws IOException
	{
		Pattern pattern10 = Pattern.compile("^([0-9]{8})([^|]+)\\|\\s([\\S\\s]+?)\\s(\\(?\\&\\%\\S+[\\S\\s]+)$"); // "^([0-9]{8})([^\\|]+)\\|\\s([\\S\\s]+?)\\s(\\(?\\&\\%\\S+[\\S\\s]+)$"
		Pattern pattern11 = Pattern.compile("^([0-9]{8})([^|]+)\\|\\s([\\S\\s]+)$"); // "^([0-9]{8})([^\\|]+)\\|\\s([\\S\\s]+)$" -- no SUMO mapping
		File verbFile = getWnFile("verb_mappings");
		try (LineNumberReader lr = new LineNumberReader(new BufferedReader(new FileReader(verbFile))))
		{
			String line;
			while ((line = lr.readLine()) != null)
			{
				// progress
				if (lr.getLineNumber() % 1000 == 1)
				{
					System.out.print('.');
				}

				// process
				line = line.trim();

				// match with SUMO mapping
				Matcher m = pattern10.matcher(line);
				if (m.matches())
				{
					// 1-synset, 2-pointers, 3-docu, 4-SUMO term
					String synset = m.group(1);
					String pointers = m.group(2);
					String docu = m.group(3);
					String term = m.group(4);
					verbDocumentation.put(synset, docu);
					setMaxVerbSynsetID(synset);
					addSUMOMapping(term, "2" + synset);
					processPointers("2" + synset, pointers);
				}
				else
				{
					// match without SUMO mapping
					m = pattern11.matcher(line);
					if (m.matches())
					{
						// 1-synset, 2-pointers, 3-docu, no SUMO term
						String synset = m.group(1);
						String pointers = m.group(2);
						String docu = m.group(3);
						verbDocumentation.put(synset, docu);
						setMaxVerbSynsetID(synset);
						processPointers("2" + synset, pointers);
					}
					else
					{
						if (line.length() > 0 && line.charAt(0) != ';')
						{
							throw new IllegalArgumentException("No match for line " + line);
						}
					}
				}
			}
		}

		Pattern pattern12 = Pattern.compile("(\\S+)\\s+(\\S+).*"); // "(\\S+)\\s+(\\S+).*"
		File verbFileExcep = getWnFile("verb_exceptions");
		try (LineNumberReader lr = new LineNumberReader(new BufferedReader(new FileReader(verbFileExcep))))
		{
			String line;
			while ((line = lr.readLine()) != null)
			{
				// 12: p = Pattern.compile();
				Matcher m = pattern12.matcher(line);  // TODO: Note we ignore more then one base form
				if (m.matches())
				{
					// 1-past, 2-infinitive
					String past = m.group(1);
					String infinitive = m.group(2);
					verbExceptions.put(past, infinitive);
					verbInvExceptions.put(infinitive, past);
				}
				else if (line.length() > 0 && line.charAt(0) != ';')
				{
					throw new IllegalArgumentException("No match for line " + line);
				}
			}
		}
	}

	private void readAdjectives() throws IOException
	{
		Pattern pattern13 = Pattern.compile("^([0-9]{8})([\\S\\s]+)\\|\\s([\\S\\s]+?)\\s(\\(?\\&\\%\\S+[\\S\\s]+)$"); // "^([0-9]{8})([\\S\\s]+)\\|\\s([\\S\\s]+?)\\s(\\(?\\&\\%\\S+[\\S\\s]+)$"
		Pattern pattern14 = Pattern.compile("^([0-9]{8})([\\S\\s]+)\\|\\s([\\S\\s]+)$"); // "^([0-9]{8})([\\S\\s]+)\\|\\s([\\S\\s]+)$" -- no SUMO mapping
		File verbFile = getWnFile("adj_mappings");
		try (LineNumberReader lr = new LineNumberReader(new BufferedReader(new FileReader(verbFile))))
		{

			String line;
			while ((line = lr.readLine()) != null)
			{
				// progress
				if (lr.getLineNumber() % 1000 == 1)
				{
					System.out.print('.');
				}

				// process
				line = line.trim();

				// match with SUMO mapping
				Matcher m = pattern13.matcher(line);
				if (m.matches())
				{
					// 1-synset, 2-pointers, 3-docu, 4-SUMO term
					String synset = m.group(1);
					String pointers = m.group(2);
					String docu = m.group(3);
					String term = m.group(4);
					adjectiveDocumentation.put(synset, docu);
					addSUMOMapping(term, "3" + synset);
					processPointers("3" + synset, pointers);
				}
				else
				{
					// match without SUMO mapping
					m = pattern14.matcher(line);
					if (m.matches())
					{
						// 1-synset, 2-pointers, 3-docu, no SUMO term
						String synset = m.group(1);
						String pointers = m.group(2);
						String docu = m.group(3);
						adjectiveDocumentation.put(synset, docu);
						processPointers("3" + synset, pointers);
					}
					else
					{
						if (line.length() > 0 && line.charAt(0) != ';')
						{
							throw new IllegalArgumentException("No match for line " + line);
						}
					}
				}
			}
		}
	}

	private void readAdverbs() throws IOException
	{
		Pattern pattern15 = Pattern.compile("^([0-9]{8})([\\S\\s]+)\\|\\s([\\S\\s]+)\\s(\\(?&%\\S+[\\S\\s]+)$"); // "^([0-9]{8})([\\S\\s]+)\\|\\s([\\S\\s]+)\\s(\\(?\\&\\%\\S+[\\S\\s]+)$"
		Pattern pattern16 = Pattern.compile("^([0-9]{8})([\\S\\s]+)\\|\\s([\\S\\s]+)$"); // "^([0-9]{8})([\\S\\s]+)\\|\\s([\\S\\s]+)$"
		File adverbFile = getWnFile("adv_mappings");
		try (LineNumberReader lr = new LineNumberReader(new BufferedReader(new FileReader(adverbFile))))
		{
			String line;
			while ((line = lr.readLine()) != null)
			{
				// progress
				if (lr.getLineNumber() % 1000 == 1)
				{
					System.out.print('.');
				}

				// process
				line = line.trim();

				// match with SUMO mapping
				Matcher m = pattern15.matcher(line);
				if (m.matches())
				{
					// 1-synset, 2-pointers, 3-docu, 4-SUMO term
					String synset = m.group(1);
					String pointers = m.group(2);
					String docu = m.group(3);
					String term = m.group(4);
					adverbDocumentation.put(synset, docu);
					addSUMOMapping(term, "4" + synset);
					processPointers("4" + synset, pointers);
				}
				else
				{
					// match without SUMO mapping
					m = pattern16.matcher(line);
					if (m.matches())
					{
						// 1-synset, 2-pointers, 3-docu, 4-SUMO term
						String synset = m.group(1);
						String pointers = m.group(2);
						String docu = m.group(3);
						adverbDocumentation.put(synset, docu);
						processPointers("4" + synset, pointers);
					}
					else
					{
						if (line.length() > 0 && line.charAt(0) != ';')
						{
							throw new IllegalArgumentException("No match for line " + line);
						}
					}
				}
			}
		}
	}

	private void readSenseIndex(final String filePath) throws IOException
	{
		Pattern pattern18 = Pattern.compile("([^%]+)%([^:]*):([^:]*):([^:]*)?:([^:]*)?:([^ ]*)? ([^ ]+)? ([^ ]+).*"); // "([^%]+)%([^:]*):([^:]*):([^:]*):([^:]*):([^ ]*) ([^ ]+) ([^ ]+) .*"
		File senseIndexFile = getWnFile("sense_indexes");
		try (LineNumberReader lr = new LineNumberReader(new BufferedReader(new FileReader(senseIndexFile))))
		{
			String line;
			while ((line = lr.readLine()) != null)
			{
				// progress
				if (lr.getLineNumber() % 1000 == 1)
				{
					System.out.print('.');
				}

				// process
				line = line.trim();

				Matcher m = pattern18.matcher(line);
				if (m.matches())
				{
					String word = m.group(1);
					String pos = m.group(2);  // WN's ss_type
					String lexFilenum = m.group(3);
					String lexID = m.group(4);
					String headword = m.group(5);
					String headID = m.group(6);
					String synset = m.group(7);
					String sensenum = m.group(8);

					// sensekey
					String posString = posNumberToLetters(pos); // alpha POS - NN,VB etc
					String key = word + "_" + posString + "_" + sensenum;
					String sensekey = word + "%" + pos + ":" + lexFilenum + ":" + lexID;
					senseKeyToSynset.put(sensekey, synset);

					List<String> al = wordToSenseKeys.computeIfAbsent(word, k -> new ArrayList<String>());
					al.add(key);

					senseIndex.put(key, synset);
					senseInvIndex.put(pos + synset, key);
				}
			}
		}
	}

	private void readSenseCount() throws IOException
	{
		Pattern pattern26 = Pattern.compile("([^ ]+) ([^%]+)%([^:]*):[^:]*:[^:]*:[^:]*:[^ ]* ([^ ]+)");
		File senseIndexFile = getWnFile("cntlist");
		try (LineNumberReader lr = new LineNumberReader(new BufferedReader(new FileReader(senseIndexFile))))
		{
			String line;
			while ((line = lr.readLine()) != null)
			{
				// progress
				if (lr.getLineNumber() % 1000 == 1)
				{
					System.out.print('.');
				}

				// process
				line = line.trim();
				Matcher m = pattern26.matcher(line);
				if (m.matches())
				{
					String count = m.group(1);
					String word = m.group(2);
					String pos = m.group(3);
					String sensenum = m.group(4);

					caseMap.put(word.toUpperCase(), word);
					String pos2 = posNumberToLetters(pos);

					// word_POS_sensenum
					String key = word + "_" + pos2 + "_" + sensenum;

					// resolvable
					String synset8 = senseIndex.get(key);
					if (synset8 != null)
					{
						String synset = getSenseFromKey(key);
						Entry<String, String> avp = new Entry<>(key, synset);
						addToWordFreq(word, avp);
						int freq = Integer.parseInt(count);
						senseFrequencies.put(synset, freq);
					}
				}
			}
		}
	}

	private void readStopWords() throws IOException
	{
		File stopWordsFile = getWnFile("stopwords");
		try (LineNumberReader lr = new LineNumberReader(new BufferedReader(new FileReader(stopWordsFile))))
		{
			String line;
			while ((line = lr.readLine()) != null)
			{
				// progress
				if (lr.getLineNumber() % 1000 == 1)
				{
					System.out.print('.');
				}

				// process
				line = line.trim();
				stopwords.add(line);
			}
		}
	}

	private void readWordCoFrequencies() throws IOException
	{
		Pattern pattern17 = Pattern.compile("^Word: ([^ ]+) Values: (.*)"); // "^Word: ([^ ]+) Values: (.*)"
		File wordFrequenciesFile = getWnFile("word_frequencies");
		try (LineNumberReader lr = new LineNumberReader(new BufferedReader(new FileReader(wordFrequenciesFile))))
		{
			String line;
			while ((line = lr.readLine()) != null)
			{
				// progress
				if (lr.getLineNumber() % 1000 == 1)
				{
					System.out.print('.');
				}

				// process
				line = line.trim();
				Matcher m = pattern17.matcher(line);
				if (m.matches())
				{
					String key = m.group(1);
					String values = m.group(2);

					String[] words = values.split(" ");
					HashMap<String, Integer> frequencies = new HashMap<String, Integer>();
					for (int i = 0; i < words.length - 3; i++)
					{
						if (words[i].equals("SUMOterm:"))
						{
							i = words.length;
						}
						else
						{
							if (words[i].indexOf("_") == -1)
							{
								//System.out.println("INFO in WordNet.readWordFrequencies().  word: " + words[i]);
								//System.out.println("INFO in WordNet.readWordFrequencies().  line: " + line);
							}
							else
							{
								String word = words[i].substring(0, words[i].indexOf("_"));
								String freq = words[i].substring(words[i].lastIndexOf("_") + 1, words[i].length());
								frequencies.put(word.intern(), Integer.decode(freq));
							}
						}
					}
					wordCoFrequencies.put(key.intern(), frequencies);
				}
			}
		}
	}

	private void createIgnoreCaseMap()
	{
	}

	private void setMaxNounSynsetID(final String synset)
	{
		// if (isValidSynset8(synset))
		// 	maxNounSynsetID = synset;
	}

	private void setMaxVerbSynsetID(final String synset)
	{
		// if (isValidSynset8(synset))
		// 	maxVerbSynsetID = synset;
	}

	private void addSUMOMapping(final String term0, final String synset)
	{
		final String term = term0.trim();
		switch (synset.charAt(0))
		{
			case '1':
				nounSUMOHash.put(synset.substring(1), term);
				break;
			case '2':
				verbSUMOHash.put(synset.substring(1), term);
				break;
			case '3':
				adjectiveSUMOHash.put(synset.substring(1), term);
				break;
			case '4':
				adverbSUMOHash.put(synset.substring(1), term);
				break;
		}
		addSUMOHash(term, synset);
	}

	private void processPointers(final String synset, final String pointers0)
	{
		Pattern pattern0 = Pattern.compile("^\\s*\\d\\d\\s\\S\\s\\d\\S\\s"); // "^\\s*\\d\\d\\s\\S\\s\\d\\S\\s",
		Pattern pattern1 = Pattern.compile("^([a-zA-Z0-9'._\\-]\\S*)\\s([0-9a-f])\\s"); // "^([a-zA-Z0-9'._\\-]\\S*)\\s([0-9a-f])\\s"
		Pattern pattern2 = Pattern.compile("^...\\s"); // "^...\\s"
		Pattern pattern3 = Pattern.compile("^(\\S\\S?)\\s([0-9]{8})\\s(.)\\s([0-9a-f]{4})\\s?"); // "^(\\S\\S?)\\s([0-9]{8})\\s(.)\\s([0-9a-f]{4})\\s?"
		Pattern pattern4 = Pattern.compile("^..\\s"); // "^..\\s"
		Pattern pattern5 = Pattern.compile("^\\+\\s(\\d\\d)\\s(\\d\\d)\\s?"); // "^\\+\\s(\\d\\d)\\s(\\d\\d)\\s?"

		String pointers = pointers0;

		// process and remove prefix, should be left with: word lex_id [word  lex_id...]  p_cnt  [ptr...]  [frames...]
		Matcher m = pattern0.matcher(pointers);
		pointers = m.replaceFirst("");

		// word lex_id
		// process and remove words, should be left with: p_cnt  [ptr...]  [frames...]
		m = pattern1.matcher(pointers);
		while (m.lookingAt())
		{
			String word = m.group(1);
			// String count = m.group(2);

			// remove adj position
			if (word.length() > 3 && (word.substring(word.length() - 3, word.length()).equals("(a)") || word.substring(word.length() - 3, word.length()).equals("(p)")))
			{
				word = word.substring(0, word.length() - 3);
			}
			if (word.length() > 4 && word.substring(word.length() - 4, word.length()).equals("(ip)"))
			{
				word = word.substring(0, word.length() - 4);
			}
			addToSynsetsToWords(word, synset.substring(1), synset.substring(0, 1));

			// eat this word + lex_di
			pointers = m.replaceFirst("");

			// next
			m = pattern1.matcher(pointers);
		}

		// process and remove p_cnt, should be left with: [ptr...]  [frames...]
		m = pattern2.matcher(pointers);
		pointers = m.replaceFirst("");

		// process [ptr...]  [frames...]
		// where ptr is pointer_symbol synset_offset pos source/target
		m = pattern3.matcher(pointers);
		while (m.lookingAt())
		{
			// "^(\\S\\S?)\\s([0-9]{8})\\s(.)\\s([0-9a-f]{4})\\s?"
			//     symbol     offset       pos    src/target
			String ptr = m.group(1);
			String targetSynset = m.group(2);
			String targetPOS = m.group(3);
			// String sourceTarget = m.group(4);

			targetPOS = (Character.valueOf(posLetterToNumber(targetPOS.charAt(0)))).toString();

			// eat this ptr
			pointers = m.replaceFirst("");

			// next
			m = pattern3.matcher(pointers);

			// store
			ptr = convertWordNetPointer(ptr);
			Entry<String, String> av = new Entry<>(ptr, targetPOS + targetSynset);
			List<Entry<String, String>> synsetRelations = relations.computeIfAbsent(synset, k -> new ArrayList<>());
			synsetRelations.add(av);
		}

		// verb frames
		if (pointers.length() > 0 && !pointers.equals(" "))
		{
			// Only for verbs may we have the following leftover
			// f_cnt + f_num w_num [ +  f_num  w_num...]
			if (synset.charAt(0) == '2') // verb prefix
			{
				// eat f_cnt
				m = pattern4.matcher(pointers);
				pointers = m.replaceFirst("");

				// process f_num w_num
				m = pattern5.matcher(pointers);
				while (m.lookingAt())
				{
					String frameNum = m.group(1);
					String wordNum = m.group(2);
					String key;
					if (wordNum.equals("00"))
					{
						key = synset.substring(1);
					}
					else
					{
						int num = Integer.parseInt(wordNum);
						List<String> members = synsetsToWords.get(synset);
						if (members == null)
						{
							throw new IllegalArgumentException(synset + " has no words for pointers: \"" + pointers + "\"");
						}
						String word = members.get(num - 1);
						key = synset.substring(1) + "-" + word;
					}

					// store
					Collection<String> frames = verbFrames.computeIfAbsent(key, k -> new ArrayList<>());
					frames.add(frameNum);

					// eat
					pointers = m.replaceFirst("");

					// next
					m = pattern5.matcher(pointers);
				}
			}
			else
			{

				System.out.println("Error in processPointers(): " + synset.charAt(0) + " leftover pointers: \"" + pointers + "\"");
				throw new IllegalArgumentException("Leftover pointers: \"" + pointers + "\"");
			}
		}
	}

	private void addSUMOHash(final String term0, final String synset)
	{
		String term = term0.substring(2, term0.length() - 1);
		Collection<String> synsets = SUMOHash.computeIfAbsent(term, k -> new ArrayList<>());
		synsets.add(synset);
	}

	/**
	 * Add a synset and its corresponding word to the synsetsToWords
	 * map.  Prefix the synset with its part of speech before adding.
	 */
	private void addToSynsetsToWords(final String word, final String synset, final String POS)
	{
		// if (word.indexOf('_') > 0)
		//	multiWords.addMultiWord(word);

		List<String> al = synsetsToWords.computeIfAbsent(POS + synset, k->new ArrayList<>());
		al.add(word);

		// switch (POS.charAt(0)) {
		// 	case '1':
		// 		addToMap(nounSynsetHash,word,synset);
		// 		break;
		// 	case '2':
		// 		addToMap(verbSynsetHash,word,synset);
		// 		break;
		// 	case '3':
		// 		addToMap(adjectiveSynsetHash,word,synset);
		// 		break;
		// 	case '4':
		// 		addToMap(adverbSynsetHash,word,synset);
		// 		break;
		// }
		// addToMap(ignoreCaseSynsetHash,word.toUpperCase(),synset);
	}

	private void addToWordFreq(final String word, final Entry<String, String> avp) throws NumberFormatException
	{
		if (!isValidSynset9(avp.attribute))
		{
			return;
		}

		Set<Entry<String, String>> pq = wordFrequencies.computeIfAbsent(word, k -> new HashSet<>());
		pq.add(avp);
	}

	/**
	 * Extract the POS from a word_POS_num sense key.  Should be an
	 * alpha key, such as "VB".
	 */
	@NotNull
	private static String getPOSfromKey(final String senseKey)
	{
		int lastUS = senseKey.lastIndexOf("_");
		if (lastUS < 0)
		{
			throw new IllegalArgumentException("Missing POS: " + senseKey);
		}
		return senseKey.substring(lastUS - 2, lastUS);
	}

	/**
	 * Extract the word from a word_POS_num sense key.
	 */
	@NotNull
	private static String getWordFromKey(final String senseKey)
	{
		int lastUS = senseKey.lastIndexOf("_");
		if (lastUS < 0)
		{
			throw new IllegalArgumentException("Missing word: " + senseKey);
		}
		return senseKey.substring(0, lastUS - 3);
	}

	/**
	 * Pos to int
	 *
	 * @param pos pos
	 * @return int
	 */
	private static int posLettersToNumber(final String pos)
	{
		switch (pos.toUpperCase())
		{
			case "NN":
				return 1;
			case "VB":
				return 2;
			case "JJ":
				return 3;
			case "RB":
				return 4;
			case "AS":
				return 5;
		}
		throw new IllegalArgumentException(pos);
	}

	private static char posLetterToNumber(char pos)
	{
		switch (pos)
		{
			case 'n':
				return '1';
			case 'v':
				return '2';
			case 'a':
				return '3';
			case 'r':
				return '4';
			case 's':
				return '5';
		}
		throw new IllegalArgumentException(Character.toString(pos));
	}

	public static String posNumberToLetters(String pos)
	{
		switch (pos)
		{
			case "1":
				return "NN";
			case "2":
				return "VB";
			case "3":
				return "JJ";
			case "4":
				return "RB";
			case "5":
				return "AS";
		}
		throw new IllegalArgumentException(pos);
	}

	/**
	 * Extract the synset corresponding to a word_POS_num sense key.
	 */
	private String getSenseFromKey(final String senseKey)
	{
		String pos = getPOSfromKey(senseKey);
		int n = posLettersToNumber(pos);
		return n + senseIndex.get(senseKey);
	}

	/**
	 * Check whether a synset format is valid
	 */
	private static boolean isValidSynset8(final String synset)
	{
		try
		{
			Integer.parseInt(synset);
			return synset.length() == 8;
		}
		catch (NumberFormatException nfr)
		{
			return false;
		}
	}

	/**
	 * Check whether a synset format is valid
	 */
	private static boolean isValidSynset9(final String synset)
	{
		try
		{
			Integer.parseInt(synset);
			return synset.length() == 9;
		}
		catch (NumberFormatException nfr)
		{
			return false;
		}
	}

	/**
	 * Check whether a sense key format is valid
	 */
	private static boolean isValidKey(final String senseKey)
	{
		return senseKey.matches(".*_(NN|VB|JJ|RB|AS)_[\\d]+");
	}

	private static String convertWordNetPointer(final String ptr)
	{
		switch (ptr)
		{
			case "!":
				return "antonym";
			case "@":
				return "hypernym";
			case "@i":
				return "instance hypernym";
			case "~":
				return "hyponym";
			case "~i":
				return "instance hyponym";
			case "#m":
				return "member holonym";
			case "#s":
				return "substance holonym";
			case "#p":
				return "part holonym";
			case "%m":
				return "member meronym";
			case "%s":
				return "substance meronym";
			case "%p":
				return "part meronym";
			case "=":
				return "attribute";
			case "+":
				return "derivationally related";
			case ";c":
				return "domain topic";
			case "-c":
				return "member topic";
			case ";r":
				return "domain region";
			case "-r":
				return "member region";
			case ";u":
				return "domain usage";
			case "-u":
				return "member usage";
			case "*":
				return "entailment";
			case ">":
				return "cause";
			case "^":
				return "also see";
			case "$":
				return "verb group";
			case "&":
				return "similar to";
			case "<":
				return "participle";
			case "\\":
				return "pertainym";
		}
		throw new IllegalArgumentException(ptr);
	}

	Map<String, String> wnFilenames = makeFileMap();

	private static Map<String, String> makeFileMap()
	{
		Map<String, String> map = new HashMap<>();
		map.put("noun_mappings", "data/WordNetMappings/WordNetMappings30-noun.txt");
		map.put("verb_mappings", "data/WordNetMappings/WordNetMappings30-verb.txt");
		map.put("adj_mappings", "data/WordNetMappings/WordNetMappings30-adj.txt");
		map.put("adv_mappings", "data/WordNetMappings/WordNetMappings30-adv.txt");
		map.put("word_frequencies", "data/WordNetMappings/wordFrequencies.txt");
		map.put("stopwords", "data/WordNetMappings/stopwords.txt");

		map.put("noun_exceptions", "wordnet/noun.exc");
		map.put("verb_exceptions", "wordnet/verb.exc");
		map.put("adj_exceptions", "wordnet/adj.exc");
		map.put("adv_exceptions", "wordnet/adv.exc");
		map.put("sense_indexes", "wordnet/index.sense");
		map.put("cntlist", "wordnet/cntlist");
		return map;
	}

	private File getWnFile(final String fileKey)
	{
		return new File(wnFilenames.get(fileKey));
	}

	/**
	 * Main
	 */
	public static void main(@Nullable String[] args) throws IOException
	{
		WordNet wn = new WordNet();
		wn.initOnce();
	}
}
