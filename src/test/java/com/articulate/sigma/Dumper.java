/*
 * @author Bernard Bou
 * Created on 8 mai 2009
 * Filename : Main.java
 */
package com.articulate.sigma;

import java.io.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;

/**
 * KB Dumper class
 *
 * @author Bernard Bou 23 juin 2009
 */
public class Dumper
{
	/**
	 * Dump terms
	 *
	 * @param ps print stream
	 * @param kb kb
	 */
	static void dumpTerms(final PrintStream ps, final KB kb)
	{
		for (final String term : kb.terms)
		{
			ps.print(term);
			Dumper.dumpParents(ps, kb, term);
			Dumper.dumpChildren(ps, kb, term);
		}
	}

	/**
	 * Dump term's parents (superclasses)
	 *
	 * @param ps   print stream
	 * @param kb   kb
	 * @param term term
	 */
	@SuppressWarnings({ "cast" }) private static void dumpParents(final PrintStream ps, final KB kb, final String term)
	{
		final Collection<Formula> formulas = kb.askWithRestriction(0, "subclass", 1, term);
		if (formulas != null && !formulas.isEmpty())
		{
			for (final Formula f : formulas)
			{
				final String parent = f.getArgument(2);
				ps.print("\tparent" + "=" + parent);
			}
		}
	}

	/**
	 * Dump term's children (subclasses)
	 *
	 * @param ps   print stream
	 * @param kb   kb
	 * @param term term
	 */
	@SuppressWarnings({ "cast" }) private static void dumpChildren(final PrintStream ps, final KB kb, final String term)
	{
		final Collection<Formula> formulas = kb.askWithRestriction(0, "subclass", 2, term);
		if (formulas != null && !formulas.isEmpty())
		{
			for (final Formula f : formulas)
			{
				final String child = f.getArgument(1);
				ps.print("\tchild" + "=" + child);
			}
		}
	}

	/**
	 * Dump formulas
	 *
	 * @param ps print stream
	 * @param kb kb
	 */
	static public void dumpFormulas(final PrintStream ps, final KB kb)
	{
		for (final Formula f : kb.formulaMap.values())
		{
			ps.println(f.text);
		}
	}

	/**
	 * Dump predicates
	 *
	 * @param ps print stream
	 * @param kb kb
	 */
	static public void dumpPredicates(final PrintStream ps, final KB kb)
	{
		for (final String p : kb.collectPredicates())
		{
			ps.println(p);
		}
	}

	/**
	 * Dump terms with a tag (arfpARFP)
	 *
	 * @param ps print stream
	 * @param kb kb
	 */
	static public void dumpTaggedTerms(final PrintStream ps, final KB kb)
	{
		for (final String term : kb.terms)
		{
			ps.print(term + " ");

			if (kb.childOf(term, "Attribute"))
			{
				ps.print('A');
			}
			if (kb.childOf(term, "Relation"))
			{
				ps.print('R');
			}
			if (kb.childOf(term, "Predicate"))
			{
				ps.print('P');
			}
			if (kb.childOf(term, "Function"))
			{
				ps.print('F');
			}

			if (kb.isSubclass(term, "Attribute"))
			{
				ps.print('a');
			}
			if (kb.isSubclass(term, "Relation"))
			{
				ps.print('r');
			}
			if (kb.isSubclass(term, "Predicate"))
			{
				ps.print('p');
			}
			if (kb.isSubclass(term, "Function"))
			{
				ps.print('f');
			}

			ps.println();
		}
	}

	/**
	 * Dump formula keys
	 *
	 * @param ps print stream
	 * @param kb kb
	 */
	static public void dumpKeys(final PrintStream ps, final KB kb)
	{
		for (final String k : kb.formulas.keySet())
		{
			if (k.startsWith("(") || k.endsWith(".kif"))
			{
				continue;
			}
			ps.println(k + " => " + kb.formulas.get(k));
		}
	}

	/**
	 * Dump files with CVS info
	 *
	 * @param ps  print stream
	 * @param dir path to dir containing KIF files
	 * @throws IOException io exception
	 */
	static public void dumpFiles(final PrintStream ps, final String dir) throws IOException
	{
		final Map<String, CVSEntry> map = getCVS(dir);
		if (map != null)
			for (final Entry<String, CVSEntry> entry : map.entrySet())
			{
				final Date date = new Date(entry.getValue().date.getTime());
				ps.println(entry.getKey() + " " + entry.getValue().version + " " + DateFormat.getInstance().format(date));
			}
	}

	/**
	 * Encapsulates CVS info (version, date)
	 *
	 * @author Bernard Bou 23 juin 2009
	 */
	static class CVSEntry
	{
		String version;

		Date date;
	}

	/**
	 * Get CVS info (version, date)
	 *
	 * @param dirName dir name
	 * @return map filename->file CVS info
	 * @throws IOException io exception
	 */
	static public Map<String, CVSEntry> getCVS(final String dirName) throws IOException
	{
		final File file = new File(dirName + File.separatorChar + "CVS" + File.separatorChar + "Entries");
		if (file.exists())
		{
			final SimpleDateFormat format = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy", Locale.US);
			final Map<String, CVSEntry> map = new HashMap<>();
			try (BufferedReader reader = new BufferedReader(new FileReader(file)))
			{
				String line;
				while ((line = reader.readLine()) != null)
				{
					if (line.startsWith("D"))
					{
						continue;
					}

					// /Merge.kif/1.53/Thu Apr 30 00:55:03 2009//
					final String[] fields = line.split("/");
					final String filename = fields[1];
					final CVSEntry entry = new CVSEntry();
					entry.version = fields[2];
					try
					{
						entry.date = format.parse(fields[3]);
					}
					catch (final ParseException e)
					{
						entry.date = new Date();
					}
					map.put(filename, entry);
				}
				return map;
			}
		}
		return null;
	}
}
