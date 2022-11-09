/*
 * Copyright (c) 2022.
 * This code is copyright Bernard Bou <1313ou@gmail.com>
 * This software is released under the GNU Public License 3 <http://www.gnu.org/copyleft/gpl.html>.
 */

package org.sigma.core;

import java.io.PrintStream;
import java.util.Arrays;

public class Main
{
	private static final PrintStream PS = System.out; // ps

	private static Sumo SUMO; // ps

	public static void dumpTerms()
	{
		Dump.dumpTerms(SUMO, PS);
	}

	public static void dumpFormulas()
	{
		Dump.dumpFormulas(SUMO, PS);
	}


	public static void dumpClassTrees()
	{
		Dump.dumpClassTrees(SUMO, PS);
	}


	public static void dumpRelations()
	{
		Dump.dumpRelations(SUMO, PS);
	}

	public static void dumpPredicates()
	{
		Dump.dumpPredicates(SUMO, PS);
	}

	public static void dumpFunctions()
	{
		Dump.dumpFunctions(SUMO, PS);
	}

	public static void dumpUnaryFunctions()
	{
		Dump.dumpUnaryFunctions(SUMO, PS);
	}

	public static void dumpBinaryFunctions()
	{
		Dump.dumpBinaryFunctions(SUMO, PS);
	}

	public static void dumpBinaryRelations()
	{
		Dump.dumpBinaryRelations(SUMO, PS);
	}

	public static void dumpTernaryRelations()
	{
		Dump.dumpTernaryRelations(SUMO, PS);
	}

	public static void dumpClasses()
	{
		Dump.dumpClasses(SUMO, PS);
	}

	public static void dumpTerms(@NotNull String... terms)
	{
		for (var t : terms)
		{
			PS.println("=".repeat(80));
			dumpSubClassesOf(t);
			PS.println("-".repeat(80));
			dumpSuperClassesOf(t);
			PS.println("-".repeat(80));
			dumpInstancesOf(t);
			PS.println("-".repeat(80));
			dumpClassesOf(t);
			PS.println("=".repeat(80));
		}
	}

	public static void dumpSuperClassesOf(@NotNull final String... classes)
	{
		Arrays.stream(classes).forEach(Main::dumpSuperClassesOf1);
	}

	public static void dumpAllSuperClassesOf(@NotNull final String... classes)
	{
		Arrays.stream(classes).forEach(Main::dumpAllSuperClassesOf1);
	}

	public static void dumpSubClassesOf(@NotNull final String... classes)
	{
		Arrays.stream(classes).forEach(Main::dumpSubClassesOf1);
	}

	public static void dumpAllSubClassesOf(@NotNull final String... classes)
	{
		Arrays.stream(classes).forEach(Main::dumpAllSubClassesOf1);
	}

	public static void dumpInstancesOf(@NotNull final String... classes)
	{
		Arrays.stream(classes).forEach(Main::dumpInstancesOf1);
	}

	public static void dumpAllInstancesOf(@NotNull final String... classes)
	{
		Arrays.stream(classes).forEach(Main::dumpAllInstancesOf1);
	}

	public static void dumpClassesOf(@NotNull final String... classes)
	{
		Arrays.stream(classes).forEach(Main::dumpClassesOf1);
	}

	public static void dumpAllClassesOf(@NotNull final String... classes)
	{
		Arrays.stream(classes).forEach(Main::dumpAllClassesOf1);
	}

	// 1

	private static void dumpSuperClassesOf1(@NotNull final String className)
	{
		PS.println("SUPERCLASSES OF " + className);
		Dump.dumpSuperClassesOf(SUMO, className, PS);
		PS.println();
	}

	private static void dumpAllSuperClassesOf1(@NotNull final String className)
	{
		PS.println("ALL SUPERCLASSES OF " + className);
		Dump.dumpSuperClassesOfWithPredicateSubsumption(SUMO, className, PS);
		PS.println();
	}

	private static void dumpSubClassesOf1(@NotNull final String className)
	{
		PS.println("SUBCLASSES OF " + className);
		Dump.dumpSubClassesOf(SUMO, className, PS);
		PS.println();
	}

	private static void dumpAllSubClassesOf1(@NotNull final String className)
	{
		PS.println("ALL SUBCLASSES OF " + className);
		Dump.dumpSubClassesOfWithPredicateSubsumption(SUMO, className, PS);
		PS.println();
	}

	private static void dumpInstancesOf1(@NotNull final String className)
	{
		PS.println("INSTANCES OF " + className);
		Dump.dumpInstancesOf(SUMO, className, PS);
		PS.println();
	}

	private static void dumpAllInstancesOf1(@NotNull final String className)
	{
		PS.println("ALL INSTANCES OF " + className);
		Dump.dumpAllInstancesOf(SUMO, className, PS);
		PS.println();
	}

	private static void dumpClassesOf1(@NotNull final String inst)
	{
		PS.println("CLASSES " + inst + " IS INSTANCE OF");
		Dump.dumpClassesOf(SUMO, inst, PS);
		PS.println();
	}

	private static void dumpAllClassesOf1(@NotNull final String inst)
	{
		PS.println("CLASSES " + inst + " IS INSTANCE OF");
		Dump.dumpAllClassesOf(SUMO, inst, PS);
		PS.println();
	}

	public static void init()
	{
		SUMO = new SumoProvider().load();
		SUMO.buildRelationCaches();
		//		Processor.collectFiles(SUMO);
		//		Processor.collectTerms(SUMO);
		//		Processor.collectFormulas(SUMO);
		//
		//		SUFile.COLLECTOR.open();
		//		Term.COLLECTOR.open();
		//		Formula.COLLECTOR.open();
	}

	public static void shutdown()
	{
		//		SUFile.COLLECTOR.close();
		//		Term.COLLECTOR.close();
		//		Formula.COLLECTOR.close();
	}

	public static void main(@NotNull String[] args)
	{
		if (args.length == 0)
		{
			System.err.println("<op> args");
			return;
		}
		String arg0 = args[0];
		@NotNull String[] args2 = Arrays.stream(args).skip(1).toArray(String[]::new);

		switch (arg0)
		{
			// @formatter:off
			case "T": init(); dumpTerms(); break;
			case "F": init(); dumpFormulas(); break;
			case "Z": init(); dumpClasses(); break;

			case "ct": init(); dumpClassTrees(); break;

			case "r": init(); dumpRelations(); break;
			case "p": init(); dumpPredicates(); break;
			case "f": init(); dumpFunctions(); break;
			case "f1": init(); dumpUnaryFunctions(); break;
			case "f2": init(); dumpBinaryFunctions(); break;
			case "r2": init(); dumpBinaryRelations(); break;
			case "r3": init(); dumpTernaryRelations(); break;

			case "A": init(); dumpTerms("Attribute"); break;

			case "Cr": init(); dumpSuperClassesOf("Relation"); break;
			case "cr":init(); dumpSubClassesOf("Relation"); break;
			case "c*r": init(); dumpAllSubClassesOf("Relation"); break;

			case "C": init(); dumpSuperClassesOf(args2); break;
			case "C*": init(); dumpAllSuperClassesOf(args2); break;
			case "c": init(); dumpSubClassesOf(args2); break;
			case "c*": init(); dumpAllSubClassesOf(args2); break;

			case "i": init(); dumpInstancesOf(args2); break;
			case "i*": init(); dumpAllInstancesOf(args2); break;
			case "z": init(); dumpClassesOf(args2); break;
			case "z*": init(); dumpAllClassesOf(args2); break;
			default:
				@NotNull String errMsg = //
						"T\tdumpTerms\n"+
						"F\tdumpFormulas\n"+
						"Z\tdumpClasses\n"+

						"ct\tdumpClassTrees\n"+

						"r\tdumpRelations\n"+
						"p\tdumpPredicates\n"+
						"f\tdumpFunctions\n"+
						"f1\tdumpUnaryFunctions\n"+
						"f2\tdumpBinaryFunctions\n"+
						"r2\tdumpBinaryRelations\n"+
						"r3\tdumpTernaryRelations\n"+

						"A\tdumpAttribute\n"+

						"Cr\tdumpSuperClassesOf(Relation)\n"+
						"cr\tdumpSubClassesOf(Relation)\n"+
						"c*r\tdumpAllSubClassesOf(Relation)\n"+

						"C\tdumpSuperClassesOf(args)\n"+
						"C*\tdumpAllSuperClassesOf(arg2)\n"+
						"c\tdumpSubClassesOf(arg2)\n"+
						"c*\tdumpAllSubClassesOf(arg2)\n"+

						"i\tdumpInstancesOf(arg2)\n"+
						"i+\tdumpAllInstancesOf(arg2)\n"+
						"z\tdumpClassesOf(arg2)\n"+
						"z*\tdumpAllClassesOf(arg2)\n"+
						"";
				System.err.println(errMsg);
				break;
			// @formatter:on
		}
		shutdown();
	}
}
