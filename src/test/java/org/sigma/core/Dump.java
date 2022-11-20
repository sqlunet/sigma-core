/*
 * Copyright (c) 2022.
 * This code is copyright Bernard Bou <1313ou@gmail.com>
 * This software is released under the GNU Public License 3 <http://www.gnu.org/copyleft/gpl.html>.
 */

package org.sigma.core;

import java.io.PrintStream;
import java.util.Collection;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class Dump
{
	private static final String UP = "\uD83E\uDC45";

	private static final String DOWN = "\uD83E\uDC47";

	public static void dumpFormulas(@NotNull final BaseKB kb, @NotNull final PrintStream ps)
	{
		int i = 0;
		for (final Formula formula : kb.getFormulas())
		{
			i++;
			ps.println(i + " " + formula);
		}
	}

	public static void dumpTerms(@NotNull final BaseKB kb, @NotNull final PrintStream ps)
	{
		int i = 0;
		for (@NotNull final String term : kb.getTerms())
		{
			i++;
			ps.printf("[%d] %s doc=%s%n", i, term, getDoc(kb, term));
		}
	}

	public static void dumpClassTrees(@NotNull final BaseKB kb, @NotNull final PrintStream ps)
	{
		int i = 0;
		for (@NotNull final String term : kb.getTerms())
		{
			if (isClass(term, kb))
			{
				i++;
				ps.printf("[%d] %s doc=%s%n", i, term, getDoc(kb, term));
				dumpSuperClassesOf(kb, term, ps);
				dumpSubClassesOf(kb, term, ps);
				ps.println();
			}
		}
	}

	public static void dumpClassWithInstancesTrees(@NotNull final BaseKB kb, @NotNull final PrintStream ps)
	{
		int i = 0;
		for (@NotNull final String term : kb.getTerms())
		{
			if (isClass(term, kb))
			{
				i++;
				ps.printf("[%d] %s doc=%s%n", i, term, getDoc(kb, term));
				dumpInstancesOf(kb, term, ps);
				dumpSuperClassesOf(kb, term, ps);
				dumpSubClassesOf(kb, term, ps);
				ps.println();
			}
		}
	}

	// tree

	public static void dumpTreeOf(@NotNull final BaseKB kb, @NotNull final String term, @NotNull final PrintStream ps)
	{
		dumpInstancesOf(kb, term, ps);
		dumpClassesOf(kb, term, ps);
		dumpSubClassesOfRecurse(kb, term, 0, 2, ps);
		dumpSuperClassesOfRecurse(kb, term, 0, 2, ps);
	}

	// instances

	public static void dumpInstancesOf(@NotNull final BaseKB kb, @NotNull final String clazz, @NotNull final PrintStream ps)
	{
		dumpObjects(() -> kb.askInstancesOf(clazz).stream().sorted().collect(Collectors.toCollection(TreeSet::new)), ps, "[I]");
	}

	public static void dumpAllInstancesOf(@NotNull final KB kb, @NotNull final String clazz, @NotNull final PrintStream ps)
	{
		dumpObjects(() -> kb.getAllInstancesWithPredicateSubsumption(clazz).stream().sorted().collect(Collectors.toCollection(TreeSet::new)), ps, "[I]");
	}

	// classes

	public static boolean isClass(@NotNull final String term, @NotNull final BaseKB kb)
	{
		return !kb.askWithRestriction(0, "subclass", 1, term).isEmpty();
	}

	public static void dumpClasses(@NotNull final BaseKB kb, @NotNull final PrintStream ps)
	{
		dumpSubClassesOf(kb, "Entity", ps);
	}

	public static void dumpClassesOf(@NotNull final BaseKB kb, @NotNull final String clazz, @NotNull final PrintStream ps)
	{
		dumpObjects(() -> kb.getClassesOf(clazz).stream().sorted().collect(Collectors.toCollection(TreeSet::new)), ps, "[C]");
	}

	public static void dumpAllClassesOf(@NotNull final KB kb, @NotNull final String instance, @NotNull final PrintStream ps)
	{
		dumpObjects(() -> kb.getAllClassesOfWithPredicateSubsumption(instance).stream().sorted().collect(Collectors.toCollection(TreeSet::new)), ps, "[C]");
	}

	// subclasses

	public static void dumpSubClassesOfWithPredicateSubsumption(@NotNull final KB kb, @NotNull final String clazz, @NotNull final PrintStream ps)
	{
		dumpObjects(() -> kb.getAllSubClassesWithPredicateSubsumption(clazz).stream().sorted().collect(Collectors.toCollection(TreeSet::new)), ps);
	}

	public static void dumpSubClassesOf(@NotNull final BaseKB kb, @NotNull final String clazz, @NotNull final PrintStream ps)
	{
		ps.println(DOWN + " " + clazz);
		dumpSubClassesOfRecurse(kb, clazz, 1, Integer.MAX_VALUE, ps);
	}

	private static void dumpSubClassesOfRecurse(@NotNull final BaseKB kb, @NotNull final String clazz, final int level, final int maxlevel, @NotNull final PrintStream ps)
	{
		@NotNull final Collection<Formula> formulas = kb.askWithRestriction(0, "subclass", 2, clazz);
		if (!formulas.isEmpty())
		{
			int i = 0;
			for (@NotNull final Formula formula : formulas)
			{
				i++;
				@NotNull final String subclass = formula.getArgument(1);
				printClass(i, subclass, level, DOWN, ps);
				if (level < maxlevel)
				{
					dumpSubClassesOfRecurse(kb, subclass, level + 1, maxlevel, ps);
				}
			}
		}
	}

	// superclasses

	public static void dumpSuperClassesOfWithPredicateSubsumption(@NotNull final KB kb, @NotNull final String clazz, @NotNull final PrintStream ps)
	{
		dumpObjects(() -> kb.getAllSuperClassesWithPredicateSubsumption(clazz).stream().sorted().collect(Collectors.toCollection(TreeSet::new)), ps);
	}

	public static void dumpSuperClassesOf(@NotNull final BaseKB kb, @NotNull final String clazz, @NotNull final PrintStream ps)
	{
		ps.println(UP + " " + clazz);
		dumpSuperClassesOfRecurse(kb, clazz, 1, Integer.MAX_VALUE, ps);
	}

	private static void dumpSuperClassesOfRecurse(@NotNull final BaseKB kb, @NotNull final String term, final int level, final int maxlevel, @NotNull final PrintStream ps)
	{
		@NotNull final Collection<Formula> formulas = kb.askWithRestriction(0, "subclass", 1, term);
		if (!formulas.isEmpty())
		{
			int i = 0;
			for (@NotNull final Formula formula : formulas)
			{
				i++;
				@NotNull final String superclass = formula.getArgument(2);
				printClass(i, superclass, level, UP, ps);
				if (level < maxlevel)
				{
					dumpSuperClassesOfRecurse(kb, superclass, level + 1, maxlevel, ps);
				}
			}
		}
	}

	public static void printClass(final int index, final String clazz, final int level, final String bullet, @NotNull PrintStream ps)
	{
		ps.print("\t".repeat(level) + bullet + "[" + index + "] " + clazz);
		//ps.println(" doc=" + getDoc(kb, formulaString));
		ps.println();
	}

	public static void dumpPredicates(@NotNull final KB kb, @NotNull final PrintStream ps)
	{
		dumpObjects(() -> kb.collectPredicates().stream().sorted().collect(Collectors.toCollection(TreeSet::new)), ps);
	}

	public static void dumpFunctions(@NotNull final KB kb, @NotNull final PrintStream ps)
	{
		dumpObjects(() -> kb.collectFunctions().stream().sorted().collect(Collectors.toCollection(TreeSet::new)), ps);
	}

	public static void dumpClasses(@NotNull final KB kb, @NotNull final PrintStream ps)
	{
		dumpObjects(() -> kb.collectClasses().stream().sorted().collect(Collectors.toCollection(TreeSet::new)), ps);
	}

	public static void dumpRelations(@NotNull final KB kb, @NotNull final PrintStream ps)
	{
		dumpObjects(() -> kb.collectRelations().stream().sorted().collect(Collectors.toCollection(TreeSet::new)), ps);
	}

	public static void dumpUnaryFunctions(@NotNull final KB kb, @NotNull final PrintStream ps)
	{
		dumpObjects(() -> kb.collectInstancesOf("UnaryFunction").stream().sorted().collect(Collectors.toCollection(TreeSet::new)), ps);
	}

	public static void dumpBinaryFunctions(@NotNull final KB kb, @NotNull final PrintStream ps)
	{
		dumpObjects(() -> kb.collectInstancesOf("BinaryFunction").stream().sorted().collect(Collectors.toCollection(TreeSet::new)), ps);
	}

	public static void dumpBinaryRelations(@NotNull final KB kb, @NotNull final PrintStream ps)
	{
		dumpObjects(() -> kb.collectInstancesOf("BinaryRelation").stream().sorted().collect(Collectors.toCollection(TreeSet::new)), ps);
	}

	public static void dumpTernaryRelations(@NotNull final KB kb, @NotNull final PrintStream ps)
	{
		dumpObjects(() -> kb.collectInstancesOf("TernaryRelation").stream().sorted().collect(Collectors.toCollection(TreeSet::new)), ps);
	}

	private static <T extends Iterable<? extends String>> void dumpObjects(@NotNull final Supplier<T> supplier, @NotNull final PrintStream ps)
	{
		int i = 0;
		for (final String obj : supplier.get())
		{
			i++;
			ps.println(i + " " + obj);
		}
	}

	private static <T extends Iterable<? extends String>> void dumpObjects(@NotNull final Supplier<T> supplier, @NotNull final PrintStream ps, @NotNull final String bullet)
	{
		for (final String obj : supplier.get())
		{
			ps.println(bullet + " " + obj);
		}
	}

	@Nullable
	private static String getDoc(@NotNull final BaseKB kb, @NotNull final String term)
	{
		@NotNull final Collection<Formula> formulas = kb.askWithRestriction(0, "documentation", 1, term);
		if (!formulas.isEmpty())
		{
			final Formula formula = formulas.iterator().next();
			@NotNull String doc = formula.getArgument(3);
			doc = doc.replaceAll("\\n", "");
			return doc;
		}
		return null;
	}
}
