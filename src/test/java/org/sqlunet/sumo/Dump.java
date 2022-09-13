package org.sqlunet.sumo;

import java.util.List;

import com.articulate.sigma.Formula;
import com.articulate.sigma.KB;

public class Dump
{
	public static void dumpTerms(final KB kb)
	{
		int i = 0;
		for (final String term : kb.terms)
		{
			i++;
			System.out.print("term " + i + "=" + term);
			System.out.println(" doc=" + Dump.getDoc(kb, term));

			Dump.dumpParents(kb, term);
			Dump.dumpChildren(kb, term);
		}
	}

	public static void dumpParents(final KB kb, final String term)
	{
		final List<Formula> formulas = kb.askWithRestriction(0, "subclass", 1, term);
		if (formulas != null && !formulas.isEmpty())
		{
			int i = 0;
			for (final Formula formula : formulas)
			{
				i++;
				final String formulaString = formula.getArgument(2);
				System.out.print("\tparent" + i + "=" + formulaString);
				System.out.println(" doc=" + Dump.getDoc(kb, formulaString));
			}
		}
	}

	public static void dumpChildren(final KB kb, final String term)
	{
		final List<Formula> formulas = kb.askWithRestriction(0, "subclass", 2, term);
		if (formulas != null && !formulas.isEmpty())
		{
			int i = 0;
			for (final Formula formula : formulas)
			{
				i++;
				final String formulaString = formula.getArgument(1);
				System.out.print("\tchild" + i + "=" + formulaString);
				System.out.println(" doc=" + Dump.getDoc(kb, formulaString));
			}
		}
	}

	public static void dumpFormulas(final KB kb)
	{
		int i = 0;
		for (final Formula formula : kb.formulaMap.values())
		{
			i++;
			System.out.println(i + " " + formula);
		}
	}

	public static void dumpPredicates(final KB kb)
	{
		final List<String> predicates = kb.collectPredicates();
		int i = 0;
		for (final String predicate : predicates)
		{
			i++;
			System.out.println(i + " " + predicate);
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
