package org.sqlunet.sumo;

import org.sqlunet.common.NotFoundException;
import org.sqlunet.sumo.joins.Formula_Arg;
import org.sqlunet.sumo.objects.*;

import java.io.IOException;
import java.io.PrintStream;
import java.text.ParseException;
import java.util.Collection;

public class Processor
{
	public static void collectFiles(final Kb kb)
	{
		for (final String filename : kb.getFilenames())
		{
			SUFile.make(filename);
		}
	}

	public static void insertFiles(final PrintStream ps, final Iterable<SUFile> files)
	{
		for (final SUFile file : files)
		{
			String row = file.dataRow();
			ps.println(row);
		}
	}

	public static void collectTerms(final Kb kb)
	{
		for (final String term : kb.terms)
		{
			Term.make(term);
		}
	}

	public static void insertTerms(final PrintStream ps, final PrintStream ps2, final Iterable<Term> terms)
	{
		for (final Term term : terms)
		{
			String row = term.dataRow();
			ps.println(row);
		}
	}

	public static void insertTermsAndAttrs(final PrintStream ps, final PrintStream ps2, final Iterable<Term> terms, final Kb kb)
	{
		for (final Term term : terms)
		{
			String row = term.dataRow();
			ps.println(row);

			int termid = term.resolve();
			try
			{
				final Collection<TermAttr> attributes = TermAttr.make(term, kb);
				for (final TermAttr attribute : attributes)
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

	public static void insertTermAttrs(final PrintStream ps, final Iterable<Term> terms, final Kb kb)
	{
		for (final Term term : terms)
		{
			int termid = term.resolve();
			try
			{
				final Collection<TermAttr> attributes = TermAttr.make(term, kb);
				for (final TermAttr attribute : attributes)
				{
					String row2 = String.format("%d,%s", termid, attribute.dataRow());
					String comment2 = term.comment();
					ps.printf("%s -- %s%n", row2, comment2);
				}
			}
			catch (NotFoundException ignored)
			{
			}
		}
	}

	public static void collectFormulas(final Kb kb)
	{
		for (final com.articulate.sigma.Formula formula : kb.formulaMap.values())
		{
			Formula.make(formula);
		}
	}

	public static void insertFormulas(final PrintStream ps, final Iterable<Formula> formulas)
	{
		for (final Formula formula : formulas)
		{
			// formula
			String row = formula.dataRow();
			ps.println(row);
		}
	}

	public static void insertFormulasAndArgs(final PrintStream ps, final PrintStream ps2, final Iterable<Formula> formulas) throws NotFoundException, ParseException, IOException
	{
		for (final Formula formula : formulas)
		{
			// formula
			String row = formula.dataRow();
			ps.println(row);

			// formula args
			for (final Formula_Arg formula_arg : Formula_Arg.make(formula))
			{
				String row2 = formula_arg.dataRow();
				Arg arg = formula_arg.getArg();
				Term term = formula_arg.getTerm();
				String commentArg2 = arg.comment();
				String commentTerm2 = term.comment();
				ps2.printf("\t%s -- %s, %s%n", row2, commentArg2, commentTerm2);
			}
		}
	}

	public static void insertFormulaArgs(final PrintStream ps, final Iterable<Formula> formulas) throws NotFoundException, ParseException, IOException
	{
		for (final Formula formula : formulas)
		{
			long formulaId = formula.resolve();

			// formula args
			for (final Formula_Arg formula_arg : Formula_Arg.make(formula))
			{
				String row2 = String.format("%s", formula_arg.dataRow());
				String comment2 = formula_arg.comment();
				ps.printf("%s -- %s%n", row2, comment2);
			}
		}
	}
}
