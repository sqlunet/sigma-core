package org.sqlunet.sumo;

import com.articulate.sigma.Formula;

import org.sqlunet.sumo.objects.*;

import java.io.IOException;
import java.io.PrintStream;
import java.text.ParseException;
import java.util.Collection;
import java.util.Date;

public class Processor
{
	public static void collectFiles(final SUMOKb kb)
	{
		for (final String filename : kb.getFilenames())
		{
			final String version = null;
			final Date date = null;
			SUMOFile.make(filename, version, date);
		}
	}

	public static void insertFiles(final PrintStream ps, final Iterable<SUMOFile> files)
	{
		for (final SUMOFile sUMOFile : files)
		{
			String row = sUMOFile.dataRow();
			ps.println(row);
		}
	}


	public static void collectTerms(final SUMOKb kb)
	{
		for (final String term : kb.terms)
		{
			SUMOTerm.make(term);
		}
	}

	public static void insertTerms(final PrintStream ps, final PrintStream ps2, final Iterable<SUMOTerm> terms) throws NotFoundException
	{
		for (final SUMOTerm sUMOTerm : terms)
		{
			String row = sUMOTerm.dataRow();
			ps.println(row);

			//			try
			//			{
			//				final Collection<SUMOTermAttr> attributes = SUMOTermAttr.make(kb, term);
			//				for (final SUMOTermAttr attribute : attributes)
			//				{
			//					String row2 = attribute.dataRow();
			//					ps2.println('\t' + row2);
			//				}
			//			}
			//			catch (NotFoundException nfe)
			//			{
			//			}
		}
	}

	public static void collectFormulas(final SUMOKb kb)
	{
		for (final Formula formula : kb.formulaMap.values())
		{
			SUMOFormula.make(formula);
		}
	}

	public static void insertFormulas(final PrintStream ps, final PrintStream ps2, final Iterable<SUMOFormula> formulas) throws NotFoundException, ParseException, IOException
	{
		for (final SUMOFormula sUMOFormula : formulas)
		{
			// formula
			String row = sUMOFormula.dataRow();
			ps.println(row);

			// formula args
			for (final SUMOParseMap arg : SUMOParseMap.make(sUMOFormula))
			{
				String row2 = String.format("\t%s,%s", sUMOFormula.resolve(), arg.dataRow());
				ps2.println(row2);
			}
		}
	}
}
