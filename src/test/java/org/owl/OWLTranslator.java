/*
 * Copyright (c) 2022.
 * This code is copyright Bernard Bou <1313ou@gmail.com>
 * This software is released under the GNU Public License 3 <http://www.gnu.org/copyleft/gpl.html>.
 */

/*
 * @author Bernard Bou
 * @author Adam Pease
 * Created on 20 juin 2009
 * Filename : OWLTranslator.java
 */
package org.owl;

import org.sigma.core.*;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Read and write OWL format from Sigma data structures.
 */
public class OWLTranslator
{
	/**
	 * <code>kb</code> is cached kb to be expressed in OWL
	 */
	private final BaseKB kb;

	public OWLTranslator(final BaseKB kb)
	{
		this.kb = kb;
	}

	/**
	 * Write this term as class
	 *
	 * @param ps           printwriter
	 * @param term         term
	 * @param superClasses class's superclasses
	 */
	public void writeClass(@NotNull final PrintStream ps, @NotNull final String term, @Nullable final List<String> superClasses)
	{
		ps.println("<owl:Class rdf:ID=\"" + term + "\">");
		writeDoc(ps, term);

		if (superClasses != null)
		{
			for (@NotNull final String superClass : superClasses)
			{
				// assert Lisp.atom(superClass) : superClass; fails (FoodForFn Animal)
				if (Lisp.atom(superClass))
				{
					ps.println("  <rdfs:subClassOf rdf:resource=\"#" + superClass + "\"/>");
				}
			}
		}
		ps.println("</owl:Class>");
		ps.println();
	}

	/**
	 * Write this term as instance
	 *
	 * @param ps           printwriter
	 * @param term         term
	 * @param classes      classes the term is instance of
	 * @param superClasses superclasses the term is subclass of (this instance is itself a class)
	 */
	public void writeInstance(@NotNull final PrintStream ps, @NotNull final String term, @Nullable final List<String> classes, @Nullable final List<String> superClasses)
	{
		ps.println("<owl:Thing rdf:ID=\"" + term + "\">");
		writeDoc(ps, term);

		// instance of these classes
		if (classes != null)
		{
			for (@NotNull final String thisClass : classes)
			{
				assert Lisp.atom(thisClass);
				ps.println("  <rdf:type rdf:resource=\"#" + thisClass + "\"/>"); //$NON-NLS-1$ //$NON-NLS-2$
				if (thisClass.equals("Class"))
				{
					ps.println("  <rdf:type rdf:resource=\"http://www.w3.org/2002/07/owl#Class\"/>");
				}
			}
		}

		// subclass of these classes
		if (superClasses != null)
		{
			ps.println("  <rdf:type rdf:resource=\"http://www.w3.org/2002/07/owl#Class\"/>");
			for (@NotNull final String thisSuperClass : superClasses)
			{
				assert Lisp.atom(thisSuperClass);
				ps.println("  <rdfs:subClassOf rdf:resource=\"#" + thisSuperClass + "\"/>"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		ps.println("</owl:Thing>");
		ps.println();
	}

	/**
	 * Write this term as relation
	 *
	 * @param ps   printwriter
	 * @param term term
	 */
	public void writeRelation(@NotNull final PrintStream ps, @NotNull final String term)
	{
		ps.println("<owl:ObjectProperty rdf:ID=\"" + term + "\">"); //$NON-NLS-1$//$NON-NLS-2$
		writeDoc(ps, term);

		// domain
		@Nullable final List<String> theseDomains = getRelated("domain", "1", term, 1, 2, 3);
		if (theseDomains != null)
		{
			for (@NotNull final String thisDomain : theseDomains)
			{
				assert Lisp.atom(thisDomain);
				ps.println("  <rdfs:domain rdf:resource=\"#" + thisDomain + "\" />");
			}
		}

		// range
		@Nullable final List<String> theseRanges = getRelated("domain", "2", term, 1, 2, 3);
		if (theseRanges != null)
		{
			for (@NotNull final String thisRange : theseRanges)
			{
				assert Lisp.atom(thisRange);
				ps.println("  <rdfs:range rdf:resource=\"#" + thisRange + "\" />");
			}
		}

		// superproperties
		@Nullable final List<String> theseSuperProperties = getRelated("subrelation", term, 1, 2);
		if (theseSuperProperties != null)
		{
			for (@NotNull final String thisSuperProperty : theseSuperProperties)
			{
				assert Lisp.atom(thisSuperProperty);
				ps.println("  <owl:subPropertyOf rdf:resource=\"#" + thisSuperProperty + "\" />");
			}
		}
		ps.println("</owl:ObjectProperty>");
		ps.println();
	}

	/**
	 * Write this term's documentation
	 *
	 * @param ps   printwriter
	 * @param term term
	 */
	public void writeDoc(@NotNull final PrintStream ps, @NotNull final String term)
	{
		@Nullable final List<String> docs = getRelated("documentation", term, 1, 3);
		if (docs == null || docs.isEmpty())
		{
			return;
		}
		ps.println("  <rdfs:comment>" + OWLTranslator.processDoc(docs.get(0)) + "</rdfs:comment>");
	}

	/**
	 * Process doc string
	 *
	 * @param doc doc string
	 * @return processed doc string
	 */
	@NotNull
	public static String processDoc(final String doc)
	{
		String result = doc;
		result = result.replaceAll("&%", "");
		result = result.replaceAll("&", "&#38;");
		result = result.replaceAll(">", "&gt;");
		result = result.replaceAll("<", "&lt;");
		return result;
	}

	/**
	 * Get terms related to this term in formulas
	 *
	 * @param relationOp relation operator in formula
	 * @param term       term
	 * @param termPos    term's position
	 * @param targetPos  target position
	 * @return list of terms
	 */
	@Nullable
	private List<String> getRelated(@NotNull final String relationOp, @NotNull final String term, final int termPos, final int targetPos)
	{
		@NotNull final Collection<Formula> theseFormulas = this.kb.askWithRestriction(0, relationOp, termPos, term);
		if (theseFormulas == null || theseFormulas.isEmpty())
		{
			return null;
		}
		@NotNull final List<String> theseTerms = new ArrayList<String>();
		for (@NotNull final Formula thisFormula : theseFormulas)
		{
			theseTerms.add(thisFormula.getArgument(targetPos));
		}
		return theseTerms;
	}

	/**
	 * Get terms related to this term in formulas having given argument. Same as above except the formula must have extra argument at given position.
	 *
	 * @param relationOp relation operator in formula
	 * @param arg        required argument
	 * @param term       term
	 * @param termPos    term's position
	 * @param argPos     argument position
	 * @param targetPos  target position
	 * @return list of terms
	 */
	@Nullable
	private List<String> getRelated(@NotNull final String relationOp, final String arg, @NotNull final String term, final int termPos, final int argPos, final int targetPos)
	{
		@NotNull final Collection<Formula> theseFormulas = this.kb.askWithRestriction(0, relationOp, termPos, term);
		if (theseFormulas == null || theseFormulas.isEmpty())
		{
			return null;
		}
		@NotNull final List<String> theseTerms = new ArrayList<String>();
		for (@NotNull final Formula thisFormula : theseFormulas)
		{
			if (thisFormula.getArgument(argPos).equals(arg))
			{
				theseTerms.add(thisFormula.getArgument(targetPos));
			}
		}
		return theseTerms;
	}

	/**
	 * Write OWL file
	 *
	 * @param ps print stream
	 * @throws IOException io exception
	 */
	public void write(@NotNull final PrintStream ps)
	{
		printHeader(ps);
		for (@NotNull final String term : kb.terms)
		{
			if (term.indexOf('>') != -1 || term.indexOf('<') != -1 || term.contains("-1"))
			{
				continue;
			}

			// attributes
			@Nullable final List<String> classes = getRelated("instance", term, 1, 2); // (instance t x)
			@Nullable final List<String> superclasses = getRelated("subclass", term, 1, 2); // (subclass t x)
			@Nullable final List<String> subclasses = getRelated("subclass", term, 2, 1); // (subclass x t)
			final boolean isBinaryRelation = kb.isChildOf(term, "BinaryRelation");
			final boolean isClass = superclasses != null && !superclasses.isEmpty() || subclasses != null && !subclasses.isEmpty();
			final boolean isInstance = classes != null && !classes.isEmpty();

			if (isBinaryRelation)
			{
				writeRelation(ps, term);
			}
			else if (isInstance)
			{
				writeInstance(ps, term, classes, superclasses);
			}
			else if (isClass)
			{
				writeClass(ps, term, superclasses);
			}
		}
		printTrailer(ps);
	}

	public void writeClasses(@NotNull final PrintStream ps)
	{
		printHeader(ps);
		for (@NotNull final String term : kb.terms)
		{
			if (term.indexOf('>') != -1 || term.indexOf('<') != -1 || term.contains("-1"))
			{
				continue;
			}

			// attributes
			@Nullable final List<String> classes = getRelated("instance", term, 1, 2); // (instance t x)
			@Nullable final List<String> superclasses = getRelated("subclass", term, 1, 2); // (subclass t x)
			@Nullable final List<String> subclasses = getRelated("subclass", term, 2, 1); // (subclass x t)
			final boolean isClass = superclasses != null && !superclasses.isEmpty() || subclasses != null && !subclasses.isEmpty();

			if (isClass)
			{
				writeClass(ps, term, superclasses);
			}
		}
		printTrailer(ps);
	}

	public void writeRelations(@NotNull final PrintStream ps)
	{
		printHeader(ps);
		for (@NotNull final String term : kb.terms)
		{
			if (term.indexOf('>') != -1 || term.indexOf('<') != -1 || term.contains("-1"))
			{
				continue;
			}

			// attributes
			final boolean isBinaryRelation = kb.isChildOf(term, "BinaryRelation");
			if (isBinaryRelation)
			{
				writeRelation(ps, term);
			}
		}
		printTrailer(ps);
	}

	public void writeInstances(@NotNull final PrintStream ps)
	{
		printHeader(ps);
		for (@NotNull final String term : kb.terms)
		{
			if (term.indexOf('>') != -1 || term.indexOf('<') != -1 || term.contains("-1"))
			{
				continue;
			}

			// attributes
			@Nullable final List<String> classes = getRelated("instance", term, 1, 2); // (instance t x)
			@Nullable final List<String> superclasses = getRelated("subclass", term, 1, 2); // (subclass t x)
			final boolean isInstance = classes != null && !classes.isEmpty();

			if (isInstance)
			{
				writeInstance(ps, term, classes, superclasses);
			}
		}
		printTrailer(ps);
	}

	private void printHeader(@NotNull final PrintStream ps)
	{
		ps.println("<rdf:RDF");
		ps.println("xmlns:rdf =\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"");
		ps.println("xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\"");
		ps.println("xmlns:owl =\"http://www.w3.org/2002/07/owl#\">");

		ps.println("<owl:Ontology rdf:about=\"\">");
		ps.println("<rdfs:comment xml:lang=\"en\">A provisional and necessarily lossy translation to OWL.  Please see");
		ps.println("www.ontologyportal.org for the original KIF, which is the authoritative");
		ps.println("source.  This software is released under the GNU Public License");
		ps.println("www.gnu.org.</rdfs:comment>");
		ps.println("<rdfs:comment xml:lang=\"en\">BB");
		ps.println("www.gnu.org.</rdfs:comment>");
		ps.println("</owl:Ontology>");
		ps.println();
	}

	private void printTrailer(@NotNull final PrintStream ps)
	{
		ps.println();
		ps.println("</rdf:RDF>");
	}

	public static void main(@NotNull final String[] args) throws IOException
	{
		final KB kb = new SumoProvider().load();
		@NotNull final OWLTranslator ot = new OWLTranslator(kb);
		if (args.length == 0 || "-".equals(args[0]))
		{
			ot.write(System.out);
		}
		else
		{
			try (@NotNull PrintStream ps = new PrintStream("sumo.owl"))
			{
				ot.write(ps);
			}
		}
	}
}
