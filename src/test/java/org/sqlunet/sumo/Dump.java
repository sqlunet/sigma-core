package org.sqlunet.sumo;

import com.articulate.sigma.BaseKB;
import com.articulate.sigma.Formula;
import com.articulate.sigma.KBIface;
import com.articulate.sigma.KB;

import java.io.PrintStream;
import java.util.Collection;
import java.util.function.Supplier;

public class Dump
{
	public static void dumpTerms(final BaseKB kb, final PrintStream ps)
	{
		int i = 0;
		for (final String term : kb.getTerms())
		{
			i++;
			ps.println("term " + i + "=" + term);
		}
	}

	public static void dumpTermTree(final BaseKB kb, final PrintStream ps)
	{
		int i = 0;
		for (final String term : kb.getTerms())
		{
			i++;
			ps.print("[" + i + "] " + term);
			//ps.print(" doc=" + Dump.getDoc(kb, term));
			ps.println();

			Dump.dumpSuperClassOf(kb, term, ps);
			Dump.dumpSubClassesOf(kb, term, ps);
		}
	}

	public static void dumpSuperClassOf(final BaseKB kb, final String term, final PrintStream ps)
	{
		final Collection<Formula> formulas = kb.askWithRestriction(0, "subclass", 1, term);
		if (!formulas.isEmpty())
		{
			int i = 0;
			for (final Formula formula : formulas)
			{
				i++;
				final String formulaString = formula.getArgument(2);
				ps.print("\t\uD83E\uDC45[" + i + "] " + formulaString);
				//ps.println(" doc=" + Dump.getDoc(kb, formulaString));
				ps.println();
			}
		}
	}

	public static void dumpSubClassesOf(final BaseKB kb, final String term, final PrintStream ps)
	{
		ps.println(term);
		final Collection<Formula> formulas = kb.askWithRestriction(0, "subclass", 2, term);
		if (!formulas.isEmpty())
		{
			int i = 0;
			for (final Formula formula : formulas)
			{
				i++;
				final String formulaString = formula.getArgument(1);
				ps.print("\t\uD83E\uDC47[" + i + "] " + formulaString);
				//ps.println(" doc=" + Dump.getDoc(kb, formulaString));
				ps.println();
			}
		}
	}

	public static void dumpFormulas(final BaseKB kb, final PrintStream ps)
	{
		int i = 0;
		for (final Formula formula : kb.getFormulas())
		{
			i++;
			ps.println(i + " " + formula);
		}
	}

	public static void dumpClasses(final BaseKB kb, final PrintStream ps)
	{
		ps.println("Entity (root class)");
		dumpSubClassesOf(kb, "Entity", ps);
	}

	public static void dumpSubClassesOf(final KB kb, final String className, final PrintStream ps)
	{
		dumpObjects(() -> kb.getAllSubClassesWithPredicateSubsumption(className), ps);
	}

	public static void dumpSuperClassesOf(final KB kb, final String className, final PrintStream ps)
	{
		dumpObjects(() -> kb.getAllSuperClassesWithPredicateSubsumption(className), ps);
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

	private static String getDoc(final BaseKB kb, final String term)
	{
		final Collection<Formula> formulas = kb.askWithRestriction(0, "documentation", 1, term);
		if (!formulas.isEmpty())
		{
			final Formula formula = formulas.iterator().next();
			String doc = formula.getArgument(2); // Note this will become 3 if we add language to documentation
			// doc = kb.formatDocumentation("http://", doc);
			doc = doc.replaceAll("\\n", "");
			return doc;
		}
		return null;
	}
}
