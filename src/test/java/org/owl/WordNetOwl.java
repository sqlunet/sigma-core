/*
 * Copyright (c) 2022.
 * This code is copyright Bernard Bou <1313ou@gmail.com>
 * This software is released under the GNU Public License 3 <http://www.gnu.org/copyleft/gpl.html>.
 */

package org.owl;

import org.sigma.core.NotNull;
import org.sigma.core.Nullable;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static org.owl.WordNet.*;

public class WordNetOwl
{
	static WordNet wn;

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
	 * Write WordNet definition relations
	 */
	public static void writeOWLWordNetRelationDefinitions(@NotNull PrintStream ps)
	{
		WordNet.writeOWLWordNetRelationDefinitions(ps);
	}

	/**
	 * Write verb frames
	 */
	public static void writeOWLVerbFrames(@NotNull PrintStream ps)
	{
		WordNet.writeOWLVerbFrames(ps);
	}


	/**
	 * Write OWL format for SUMO-WordNet mappings.
	 */
	public static void writeOWLWordNetSynsets(@NotNull PrintStream ps)
	{
		WordNet.writeOWLWordNetSynsets(wn, ps);
	}

	/**
	 * Write OWL format for SUMO-WordNet mappings.
	 *
	 * @param synset is a POS prefixed synset number
	 */
	public static void writeOWLWordNetSynset(@NotNull PrintStream ps, @NotNull String synset)
	{
		WordNet.writeOWLWordNetSynset(wn, ps, synset);
	}

	/**
	 * Write words' senses
	 */
	public static void writeOWLWordsToSenses(@NotNull PrintStream ps)
	{
		WordNet.writeOWLWordsToSenses(wn, ps);
	}

	/**
	 * Write word's senses
	 */
	static void writeOWLOneWordToSenses(@NotNull PrintStream ps, @NotNull String word)
	{
		WordNet.writeOWLOneWordToSenses(wn, ps, word);
	}

	/**
	 * Write WordNet sense index
	 */
	public static void writeOWLSenseIndex(@NotNull PrintStream ps)
	{
		WordNet.writeOWLSenseIndex(wn, ps);
	}

	/**
	 * Write WordNet links
	 */
	private static void writeOWLWordNetLink(@NotNull PrintStream ps, String term) throws IOException
	{
		WordNet.writeOWLWordNetLink(wn, ps, term);
	}

	/**
	 * Write WordNet exceptions
	 */
	public static void writeOWLWordNetExceptions(@NotNull PrintStream ps)
	{
		WordNet.writeOWLWordNetExceptions(wn, ps);
	}

	/**
	 * Write WordNet header
	 */
	static void writeOWLWordNetHeader(@NotNull PrintStream ps)
	{
		WordNet.writeOWLWordNetHeader(ps);
	}

	/**
	 * Write WordNet trailer
	 */
	static void writeOWLWordNetTrailer(@NotNull PrintStream ps)
	{
		WordNet.writeOWLWordNetTrailer(ps);
	}

	/**
	 * Write OWL format for SUMO-WordNet mappings.
	 */
	public static void writeOWLWordNet(@NotNull PrintStream ps)
	{
		writeOWLWordNetHeader(ps);
		writeOWLWordNetSynsets(ps);
		writeOWLWordNetRelationDefinitions(ps);
		writeOWLWordNetClassDefinitions(ps);
		writeOWLWordNetExceptions(ps);
		writeOWLVerbFrames(ps);
		writeOWLWordsToSenses(ps);
		writeOWLSenseIndex(ps);
		writeOWLWordNetTrailer(ps);
	}
}
