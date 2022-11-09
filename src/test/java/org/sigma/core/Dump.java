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

	// instances

	public static void dumpInstancesOf(@NotNull final KB kb, @NotNull final String className, @NotNull final PrintStream ps)
	{
		dumpObjects(() -> kb.askInstancesOf(className).stream().sorted().collect(Collectors.toCollection(TreeSet::new)), ps);
	}

	public static void dumpAllInstancesOf(@NotNull final KB kb, @NotNull final String className, @NotNull final PrintStream ps)
	{
		dumpObjects(() -> kb.getAllInstancesWithPredicateSubsumption(className).stream().sorted().collect(Collectors.toCollection(TreeSet::new)), ps);
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

	public static void dumpClassesOf(@NotNull final KB kb, @NotNull final String className, @NotNull final PrintStream ps)
	{
		dumpObjects(() -> kb.getClassesOf(className).stream().sorted().collect(Collectors.toCollection(TreeSet::new)), ps);
	}

	public static void dumpAllClassesOf(@NotNull final KB kb, @NotNull final String instance, @NotNull final PrintStream ps)
	{
		dumpObjects(() -> kb.getAllClassesOfWithPredicateSubsumption(instance).stream().sorted().collect(Collectors.toCollection(TreeSet::new)), ps);
	}

	// subclasses

	public static void dumpSubClassesOfWithPredicateSubsumption(@NotNull final KB kb, @NotNull final String className, @NotNull final PrintStream ps)
	{
		dumpObjects(() -> kb.getAllSubClassesWithPredicateSubsumption(className).stream().sorted().collect(Collectors.toCollection(TreeSet::new)), ps);
	}

	public static void dumpSubClassesOf(@NotNull final BaseKB kb, @NotNull final String className, @NotNull final PrintStream ps)
	{
		ps.println(DOWN + " " + className);
		dumpSubClassesOfRecurse(kb, className, 1, ps);
	}

	private static void dumpSubClassesOfRecurse(@NotNull final BaseKB kb, @NotNull final String className, final int level, @NotNull final PrintStream ps)
	{
		@NotNull final Collection<Formula> formulas = kb.askWithRestriction(0, "subclass", 2, className);
		if (!formulas.isEmpty())
		{
			int i = 0;
			for (@NotNull final Formula formula : formulas)
			{
				i++;
				@NotNull final String subClassName = formula.getArgument(1);
				printClass(i, subClassName, level, DOWN, ps);
				dumpSubClassesOfRecurse(kb, subClassName, level + 1, ps);
			}
		}
	}

	// superclasses

	public static void dumpSuperClassesOfWithPredicateSubsumption(@NotNull final KB kb, @NotNull final String className, @NotNull final PrintStream ps)
	{
		dumpObjects(() -> kb.getAllSuperClassesWithPredicateSubsumption(className).stream().sorted().collect(Collectors.toCollection(TreeSet::new)), ps);
	}

	public static void dumpSuperClassesOf(@NotNull final BaseKB kb, @NotNull final String className, @NotNull final PrintStream ps)
	{
		ps.println(UP + " " + className);
		dumpSuperClassesOfRecurse(kb, className, 1, ps);
	}

	private static void dumpSuperClassesOfRecurse(@NotNull final BaseKB kb, @NotNull final String term, final int level, @NotNull final PrintStream ps)
	{
		@NotNull final Collection<Formula> formulas = kb.askWithRestriction(0, "subclass", 1, term);
		if (!formulas.isEmpty())
		{
			int i = 0;
			for (@NotNull final Formula formula : formulas)
			{
				i++;
				@NotNull final String superclassName = formula.getArgument(2);
				printClass(i, superclassName, level, UP, ps);
				dumpSuperClassesOfRecurse(kb, superclassName, level + 1, ps);
			}
		}
	}

	public static void printClass(final int index, final String className, final int level, final String bullet, @NotNull PrintStream ps)
	{
		ps.print("\t".repeat(level) + bullet + "[" + index + "] " + className);
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
