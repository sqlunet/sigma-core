package org.sqlunet.sumo;

import com.articulate.sigma.Formula;

import org.sqlunet.sumo.objects.*;

import java.io.File;
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
			SUMOFile.make(filename);
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
		}
	}

	public static void insertTermsAndAttrs(final PrintStream ps, final PrintStream ps2, final Iterable<SUMOTerm> terms, final SUMOKb kb) throws NotFoundException
	{
		for (final SUMOTerm sUMOTerm : terms)
		{
			String row = sUMOTerm.dataRow();
			ps.println(row);

			int termid = sUMOTerm.resolve();
			try
			{
				final Collection<SUMOTermAttr> attributes = SUMOTermAttr.make(sUMOTerm, kb);
				for (final SUMOTermAttr attribute : attributes)
				{
					String row2 = String.format("%d,%s", termid, attribute.dataRow());
					ps2.println('\t' + row2);
				}
			}
			catch (NotFoundException ignored)
			{
			}
		}
	}

	public static void insertTermAttrs(final PrintStream ps, final Iterable<SUMOTerm> terms, final SUMOKb kb) throws NotFoundException
	{
		for (final SUMOTerm sUMOTerm : terms)
		{
			int termid = sUMOTerm.resolve();
			try
			{
				final Collection<SUMOTermAttr> attributes = SUMOTermAttr.make(sUMOTerm, kb);
				for (final SUMOTermAttr attribute : attributes)
				{
					String row2 = String.format("%d,%s", termid, attribute.dataRow());
					String comment2 = sUMOTerm.comment();
					ps.printf("%s -- %s%n", row2, comment2);
				}
			}
			catch (NotFoundException ignored)
			{
			}
		}
	}

	public static void collectFormulas(final SUMOKb kb)
	{
		for (final Formula formula : kb.formulaMap.values())
		{
			SUMOFormula.make(formula);
		}
	}

	public static void insertFormulas(final PrintStream ps, final Iterable<SUMOFormula> formulas) throws NotFoundException, ParseException, IOException
	{
		for (final SUMOFormula sUMOFormula : formulas)
		{
			// formula
			String row = sUMOFormula.dataRow();
			ps.println(row);
		}
	}

	public static void insertFormulasAndArgs(final PrintStream ps, final PrintStream ps2, final Iterable<SUMOFormula> formulas) throws NotFoundException, ParseException, IOException
	{
		for (final SUMOFormula sUMOFormula : formulas)
		{
			// formula
			String row = sUMOFormula.dataRow();
			ps.println(row);

			// formula args
			for (final SUMOFormula_Arg formula_arg : SUMOFormula_Arg.make(sUMOFormula))
			{
				String row2 = formula_arg.dataRow();
				SUMOArg arg = formula_arg.getArg();
				SUMOTerm term = formula_arg.getTerm();
				String commentArg2 = arg.comment();
				String commentTerm2 = term.comment();
				ps2.printf("\t%s -- %s, %s%n", row2, commentArg2, commentTerm2);
			}
		}
	}

	public static void insertFormulaArgs(final PrintStream ps, final Iterable<SUMOFormula> formulas) throws NotFoundException, ParseException, IOException
	{
		for (final SUMOFormula sUMOFormula : formulas)
		{
			long formulaId = sUMOFormula.resolve();

			// formula args
			for (final SUMOFormula_Arg formula_arg : SUMOFormula_Arg.make(sUMOFormula))
			{
				String row2 = String.format("%s", formula_arg.dataRow());
				String comment2 = formula_arg.comment();
				ps.printf("%s -- %s%n", row2, comment2);
			}
		}
	}
}
