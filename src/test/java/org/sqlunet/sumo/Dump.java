package org.sqlunet.sumo;

import com.articulate.sigma.Formula;
import com.articulate.sigma.KBIface;
import com.articulate.sigma.KB;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class Dump
{
	public static void dumpTerms(final KBIface kb, final PrintStream ps)
	{
		int i = 0;
		for (final String term : kb.getTerms())
		{
			i++;
			ps.println("term " + i + "=" + term);
		}
	}

	public static void dumpTermTree(final KB kb, final PrintStream ps)
	{
		int i = 0;
		for (final String term : kb.getTerms())
		{
			i++;
			ps.print("term " + i + "=" + term);
			//ps.print(" doc=" + Dump.getDoc(kb, term));
			ps.println();

			Dump.dumpParents(kb, term, ps);
			Dump.dumpChildren(kb, term, ps);
		}
	}

	public static void dumpParents(final KB kb, final String term, final PrintStream ps)
	{
		final List<Formula> formulas = kb.askWithRestriction(0, "subclass", 1, term);
		if (formulas != null && !formulas.isEmpty())
		{
			int i = 0;
			for (final Formula formula : formulas)
			{
				i++;
				final String formulaString = formula.getArgument(2);
				ps.print("\tparent" + i + "=" + formulaString);
				//ps.println(" doc=" + Dump.getDoc(kb, formulaString));
				ps.println();
			}
		}
	}

	public static void dumpChildren(final KB kb, final String term, final PrintStream ps)
	{
		final List<Formula> formulas = kb.askWithRestriction(0, "subclass", 2, term);
		if (formulas != null && !formulas.isEmpty())
		{
			int i = 0;
			for (final Formula formula : formulas)
			{
				i++;
				final String formulaString = formula.getArgument(1);
				ps.print("\tchild" + i + "=" + formulaString);
				//ps.println(" doc=" + Dump.getDoc(kb, formulaString));
				ps.println();
			}
		}
	}

	public static void dumpFormulas(final KBIface kb, final PrintStream ps)
	{
		int i = 0;
		for (final Formula formula : kb.getFormulas())
		{
			i++;
			ps.println(i + " " + formula);
		}
	}
	public static void dumpClasses(final KB kb, final PrintStream ps)
	{
		dumpSubClassesOf("Entity", kb, ps);
	}

	public static void dumpSubClassesOf(final String className, final KB kb, final PrintStream ps)
	{
		dumpObjects(() -> new ArrayList<>(kb.getAllSubClassesWithPredicateSubsumption(className)), ps);
	}

	public static void dumpSuperClassesOf(final String className, final KB kb, final PrintStream ps)
	{
		dumpObjects(() -> new ArrayList<>(kb.getAllSuperClassesWithPredicateSubsumption(className)), ps);
	}

	public static void dumpPredicates(final KB kb, final PrintStream ps)
	{
		dumpObjects(kb::collectPredicates, ps);
	}

	public static void dumpFunctions(final KB kb, final PrintStream ps)
	{
		dumpObjects(kb::collectFunctions, ps);
	}

	private static <T extends Iterable<? extends String>> void dumpObjects(final Supplier<T> supplier, final PrintStream ps)
	{
		int i = 0;
		for (final String obj : supplier.get())
		{
			i++;
			ps.println(i + " " + obj);
		}
	}

	private static String getDoc(final KB kb, final String term)
	{
		final List<Formula> formulas = kb.askWithRestriction(0, "documentation", 1, term);
		if (formulas != null && !formulas.isEmpty())
		{
			final Formula formula = formulas.get(0);
			String doc = formula.getArgument(2); // Note this will become 3 if we add language to documentation
			// doc = kb.formatDocumentation("http://", doc);
			doc = doc.replaceAll("\\n", "");
			return doc;
		}
		return null;
	}
}
