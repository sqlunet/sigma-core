/*
 * Copyright (c) 2022.
 * This code is copyright Bernard Bou <1313ou@gmail.com>
 * This software is released under the GNU Public License 3 <http://www.gnu.org/copyleft/gpl.html>.
 */

package org.sqlunet.sumo;

import org.sigma.core.*;

import org.sqlunet.common.NotFoundException;
import org.sqlunet.sumo.joins.Formula_Arg;
import org.sqlunet.sumo.objects.*;
import org.sqlunet.sumo.objects.Formula;

import java.io.IOException;
import java.io.PrintStream;
import java.text.ParseException;
import java.util.Collection;

public class Processor
{
	public static void collectFiles(@NotNull final FileGetter kb)
	{
		for (@NotNull final String filename : kb.getFilenames())
		{
			SUFile.make(filename);
		}
	}

	public static void insertFiles(@NotNull final PrintStream ps, @NotNull final Iterable<SUFile> files)
	{
		for (@NotNull final SUFile file : files)
		{
			String row = file.dataRow();
			ps.printf("(%s),%n", row);
		}
	}

	public static void collectTerms(@NotNull final KBIface kb)
	{
		for (final String term : kb.getTerms())
		{
			Term.make(term);
		}
	}

	public static void insertTerms(@NotNull final PrintStream ps, final PrintStream ps2, @NotNull final Iterable<Term> terms)
	{
		for (@NotNull final Term term : terms)
		{
			String row = term.dataRow();
			ps.printf("(%s),%n", row);
		}
	}

	public static void insertTermsAndAttrs(@NotNull final PrintStream ps, @NotNull final PrintStream ps2, @NotNull final Iterable<Term> terms, @NotNull final Sumo kb)
	{
		for (@NotNull final Term term : terms)
		{
			// term
			String row = term.dataRow();
			ps.printf("(%s),%n", row);

			// atttributes
			int termid = term.resolve();
			try
			{
				@NotNull final Collection<TermAttr> attributes = TermAttr.make(term, kb);
				for (@NotNull final TermAttr attribute : attributes)
				{
					String row2 = String.format("%d,%s", termid, attribute.dataRow());
					@Nullable String comment2 = term.comment();
					ps2.printf("\t(%s), -- %s%n", row2, comment2);
				}
			}
			catch (NotFoundException ignored)
			{
			}
		}
	}

	public static void insertTermAttrs(@NotNull final PrintStream ps, @NotNull final Iterable<Term> terms, @NotNull final Sumo kb)
	{
		for (@NotNull final Term term : terms)
		{
			int termid = term.resolve();
			try
			{
				@NotNull final Collection<TermAttr> attributes = TermAttr.make(term, kb);
				for (@NotNull final TermAttr attribute : attributes)
				{
					String row2 = String.format("%d,%s", termid, attribute.dataRow());
					@Nullable String comment2 = term.comment();
					ps.printf("\t(%s), -- %s%n", row2, comment2);
				}
			}
			catch (NotFoundException ignored)
			{
			}
		}
	}

	public static void collectFormulas(@NotNull final KBIface kb)
	{
		for (@NotNull final org.sigma.core.Formula formula : kb.getFormulas())
		{
			Formula.make(formula);
		}
	}

	public static void insertFormulas(@NotNull final PrintStream ps, @NotNull final Iterable<Formula> formulas)
	{
		for (@NotNull final Formula formula : formulas)
		{
			// formula
			String row = formula.dataRow();
			ps.printf("(%s),%n", row);
		}
	}

	public static void insertFormulasAndArgs(@NotNull final PrintStream ps, @NotNull final PrintStream ps2, @NotNull final Iterable<Formula> formulas) throws NotFoundException, ParseException, IOException
	{
		for (@NotNull final Formula formula : formulas)
		{
			// formula
			String row = formula.dataRow();
			ps.printf("(%s),%n", row);

			// formula args
			for (@NotNull final Formula_Arg formula_arg : Formula_Arg.make(formula))
			{
				String row2 = formula_arg.dataRow();
				Arg arg = formula_arg.getArg();
				Term term = formula_arg.getTerm();
				@Nullable String commentArg2 = arg.comment();
				@Nullable String commentTerm2 = term.comment();
				ps2.printf("\t(%s), -- %s, %s%n", row2, commentArg2, commentTerm2);
			}
		}
	}

	public static void insertFormulaArgs(@NotNull final PrintStream ps, @NotNull final Iterable<Formula> formulas) throws NotFoundException, ParseException, IOException
	{
		for (@NotNull final Formula formula : formulas)
		{
			// args
			for (@NotNull final Formula_Arg formula_arg : Formula_Arg.make(formula))
			{
				String row2 = String.format("%s", formula_arg.dataRow());
				@Nullable String comment2 = formula_arg.comment();
				ps.printf("(%s), -- %s%n", row2, comment2);
			}
		}
	}
}
