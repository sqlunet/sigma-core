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
import java.util.Collection;

/**
 * Read and write OWL format from Sigma data structures.
 */
public class SimpleOWLTranslator
{
	/**
	 * <code>kb</code> is cached kb to be expressed in OWL
	 */
	private final BaseKB kb;

	public SimpleOWLTranslator(final BaseKB kb)
	{
		this.kb = kb;
	}

	public void writeTerm(@NotNull final PrintStream ps, @NotNull final String term)
	{
		// (subclass term someclass)
		@Nullable final Collection<String> superclasses = getRelated("subclass", term, 1, 2); // (subclass t x)
		// (instance term someclass)
		@Nullable final Collection<String> classes = getRelated("instance", term, 1, 2); // (instance t x)
		// (instance x term)
		@Nullable final Collection<String> instances = getRelated("instance", term, 2, 1);  // (instance x t)
		// (subclass x term)
		@Nullable final Collection<String> subclasses = getRelated("subclass", term, 2, 1); // (subclass x t)

		// flags
		final boolean isRelation = isRelation(term);
		final boolean isInstance = !classes.isEmpty();
		final boolean isClass = //
				!superclasses.isEmpty() || // has superclasses => is a class
						!subclasses.isEmpty() || // has subclasses => is a class
						!instances.isEmpty(); // has instances => is a class

		// relation
		if (isRelation)
		{
			writeRelation(ps, term, superclasses);
		}

		// classes, non-relational instances, functions
		// if (Character.isUpperCase(term.charAt(0))) // functions, non-relational instances or class start with uppercase
		{
			if (isRelation)
			{
				System.out.printf("[R] %s%s%s%n", isInstance ? "[I] " : "", isClass ? "[C] " : "", term);
			}

			// instance
			if (isInstance)
			{
				writeInstance(ps, term, classes, superclasses);
			}

			// class
			if (isClass)
			{
				if (kb.pathIsOrEndsWith(term, "Entity"))
				{
					writeClass(ps, term, superclasses);
				}
				// else
				// {
				//System.out.println("[C] " + term + " DISCARDED");
				// }
			}
		}
	}

	/**
	 * Write this term as class
	 *
	 * @param ps           print stream
	 * @param clazz        class term
	 * @param superclasses class's superclasses
	 */
	public void writeClass(@NotNull final PrintStream ps, @NotNull final String clazz, @Nullable final Collection<String> superclasses)
	{
		// System.out.println("[C] " + term);
		ps.println("<owl:Class rdf:about=\"#" + clazz + "\">");
		writeDoc(ps, clazz);
		if (superclasses != null)
		{
			ps.print(embedSuperClasses(superclasses));
		}
		ps.println("</owl:Class>");
		ps.println();
	}

	/**
	 * Write this term as instance
	 *
	 * @param ps           print stream
	 * @param clazz        class term
	 * @param classes      classes the term is instance of
	 * @param superclasses superclasses the term is subclass of (this instance is itself a class)
	 */
	public void writeInstance(@NotNull final PrintStream ps, @NotNull final String clazz, @Nullable final Collection<String> classes, @Nullable final Collection<String> superclasses)
	{
		// System.out.println("[I] " + term);

		// instance of these classes
		String embed = classes != null ? embedClasses(classes) : null;
		if (embed == null || embed.isEmpty())
		{
			return;
		}

		// instance of these classes
		ps.println("<owl:Thing rdf:about=\"#" + clazz + "\">");
		writeDoc(ps, clazz);

		// type: instance of these classes
		ps.print(embed);

		// superclass
		if (superclasses != null && !superclasses.isEmpty())
		{
			// is a class if has superclasses
			ps.println("  <rdf:type rdf:resource=\"http://www.w3.org/2002/07/owl#Class\"/>");

			// subclassOf
			ps.print(embedSuperClasses(superclasses));
		}
		ps.println("</owl:Thing>");
		ps.println();
	}

	/**
	 * Write this term as relation
	 *
	 * @param ps   print stream
	 * @param reln relation term
	 */
	public void writeRelation(@NotNull final PrintStream ps, @NotNull final String reln, @Nullable final Collection<String> superclasses)
	{
		// System.out.println("[R] " + term);
		ps.println("<owl:ObjectProperty rdf:about=\"#" + reln + "\">");

		// domain
		// (domain reln 1 ?)
		@Nullable final Collection<String> domains = getRelated("domain", reln, 1, "1", 2, 3);
		if (!domains.isEmpty())
		{
			for (@NotNull final String domain : domains)
			{
				assert Lisp.atom(domain);
				ps.println("  <rdfs:domain rdf:resource=\"#" + domain + "\" />");
			}
		}

		// range
		// (range reln ?)
		@Nullable final Collection<String> ranges = getRelated("range", reln, 1, 2);
		if (!ranges.isEmpty())
		{
			for (@NotNull final String range : ranges)
			{
				assert Lisp.atom(range);
				ps.println("  <rdfs:range rdf:resource=\"#" + range + "\" />");
			}
		}

		// super relations
		// (subrelation reln ?)
		@Nullable final Collection<String> superRelations = getRelated("subrelation", reln, 1, 2);
		if (!superRelations.isEmpty())
		{
			for (@NotNull final String superProperty : superRelations)
			{
				assert Lisp.atom(superProperty);
				ps.println("  <owl:subPropertyOf rdf:resource=\"#" + superProperty + "Property\" />");
			}
		}

		// superclasses
		if (superclasses != null && !superclasses.isEmpty())
		{
			// is a class if has superclasses
			ps.println("  <rdf:type rdf:resource=\"http://www.w3.org/2002/07/owl#Class\"/>");
			ps.print(embedSuperClasses(superclasses));
		}

		writeDoc(ps, reln);

		ps.println("</owl:ObjectProperty>");
		ps.println();
	}

	private boolean isRelation(@NotNull final String term)
	{
		// boolean discard = term.indexOf('>') != -1 || term.indexOf('<') != -1;
		return /* !discard && */ "BinaryRelation".equals(term) || kb.isChildOf(term, "BinaryRelation");
	}

	private String embedClasses(@NotNull final Collection<String> classes)
	{
		StringBuilder sb = new StringBuilder();
		for (@NotNull final String clazz : classes)
		{
			if (Lisp.atom(clazz) && kb.pathIsOrEndsWith(clazz, "Entity"))
			{
				// type of instance is the class
				sb.append("  <rdf:type rdf:resource=\"#").append(clazz).append("\"/>").append('\n');

				// top class
				if (clazz.equals("Class"))
				{
					sb.append("  <rdf:type rdf:resource=\"http://www.w3.org/2002/07/owl#Class\"/>").append('\n');
				}
			}
		}
		return sb.toString();
	}

	private String embedSuperClasses(@NotNull final Collection<String> superclasses)
	{
		StringBuilder sb = new StringBuilder();
		for (@NotNull final String superclass : superclasses)
		{
			if (Lisp.atom(superclass) && kb.pathIsOrEndsWith(superclass, "Entity"))
			{
				// subclass statement
				sb.append("  <rdfs:subClassOf rdf:resource=\"#").append(superclass).append("\"/>").append('\n');
			}
		}
		return sb.toString();
	}

	/**
	 * Write this term's documentation
	 *
	 * @param ps   print stream
	 * @param term term
	 */
	public void writeDoc(@NotNull final PrintStream ps, @NotNull final String term)
	{
		@Nullable final Collection<String> docs = getRelated("documentation", term, 1, 3);
		if (docs.isEmpty())
		{
			return;
		}
		ps.println("  <rdfs:comment>" + SimpleOWLTranslator.processDoc(docs.iterator().next()) + "</rdfs:comment>");
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
	 * @param reln      relation operator in formula
	 * @param arg       arg
	 * @param argPos    arg position
	 * @param targetPos target position
	 * @return list of terms
	 */
	@NotNull
	private Collection<String> getRelated(@NotNull final String reln, @NotNull final String arg, final int argPos, final int targetPos)
	{
		return kb.askTerms(0, reln, argPos, arg, targetPos);
	}

	/**
	 * Get terms related to this term in formulas having given argument. Same as above except the formula must have extra argument at given position.
	 *
	 * @param reln      relation operator in formula
	 * @param arg1      argument1
	 * @param arg1Pos   argument1 position
	 * @param arg2      argument2
	 * @param arg2Pos   argument2 position
	 * @param targetPos target position
	 * @return list of terms
	 */
	@NotNull
	private Collection<String> getRelated(@SuppressWarnings("SameParameterValue") @NotNull final String reln, final String arg1, @SuppressWarnings("SameParameterValue") final int arg1Pos, @NotNull final String arg2, @SuppressWarnings("SameParameterValue") final int arg2Pos, @SuppressWarnings("SameParameterValue") final int targetPos)
	{
		return kb.askTerms(0, reln, arg1Pos, arg1, arg2Pos, arg2, targetPos);
	}

	/**
	 * Write OWL file
	 *
	 * @param ps print stream
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
			writeTerm(ps, term);
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
			@Nullable final Collection<String> instances = getRelated("instance", term, 2, 1); // (instance x t), t has instances
			@Nullable final Collection<String> superclasses = getRelated("subclass", term, 1, 2); // (subclass t x), t is a subclass
			@Nullable final Collection<String> subclasses = getRelated("subclass", term, 2, 1); // (subclass x t), t is a superclass

			// either has instance(s) or is a subclass or is a superclass
			final boolean isClass = !instances.isEmpty() || !superclasses.isEmpty() || !subclasses.isEmpty();

			if (isClass)
			{
				writeClass(ps, term, superclasses);
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
			@Nullable final Collection<String> classes = getRelated("instance", term, 1, 2); // (instance t x), t is an is an instance
			@Nullable final Collection<String> superclasses = getRelated("subclass", term, 1, 2); // (subclass t x)

			// is an instance
			final boolean isInstance = !classes.isEmpty();

			if (isInstance)
			{
				writeInstance(ps, term, classes, superclasses);
			}
		}
		printTrailer(ps);
	}

	public void writeRelations(@NotNull final PrintStream ps)
	{
		printHeader(ps);
		for (@NotNull final String term : kb.terms)
		{
			boolean isRelation = isRelation(term);
			if (isRelation)
			{
				@Nullable final Collection<String> superclasses = getRelated("subclass", term, 1, 2); // (subclass t x)
				writeRelation(ps, term, superclasses);
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
		ps.println("</rdf:RDF>");
	}

	public static void main(@NotNull final String[] args) throws IOException
	{
		@NotNull final BaseKB kb = new BaseSumoProvider().load();

		@NotNull final SimpleOWLTranslator ot = new SimpleOWLTranslator(kb);
		if (args.length == 0 || "-".equals(args[0]))
		{
			ot.write(System.out);
		}
		else
		{
			try (@NotNull PrintStream ps = new PrintStream(args[0].isEmpty() ? "sumo.owl" : args[0]))
			{
				ot.write(ps);
			}
		}
	}
}
