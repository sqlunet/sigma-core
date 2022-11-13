/*
 * Copyright (c) 2022.
 * This code is copyright Bernard Bou <1313ou@gmail.com>
 * This software is released under the GNU Public License 3 <http://www.gnu.org/copyleft/gpl.html>.
 */

package org.sqlunet.sumo;

import org.sigma.core.NotNull;
import org.sigma.core.Sumo;
import org.sigma.core.SumoProvider;
import org.sigma.core.Helpers;

import org.sqlunet.common.NotFoundException;
import org.sqlunet.sumo.objects.Formula;
import org.sqlunet.sumo.objects.SUFile;
import org.sqlunet.sumo.objects.Term;

import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.fail;

public class Main
{
	private static final PrintStream PS = System.out; // ps

	private static final PrintStream PS2 = System.out; // ps2

	private static Sumo SUMO; // ps

	public static void init()
	{
		SUMO = new SumoProvider().load();
		SUMO.buildRelationCaches();

		Processor.collectFiles(SUMO);
		Processor.collectTerms(SUMO);
		Processor.collectFormulas(SUMO);

		SUFile.COLLECTOR.open();
		Term.COLLECTOR.open();
		Formula.COLLECTOR.open();
	}

	public static void shutdown()
	{
		SUFile.COLLECTOR.close();
		Term.COLLECTOR.close();
		Formula.COLLECTOR.close();
	}

	public static void main(@NotNull String[] args) throws NotFoundException
	{
		if (args.length == 0)
		{
			System.err.println("<op>");
			return;
		}
		String arg0 = args[0];

		switch (arg0)
		{
			// @formatter:off
			case "k": init(); files(); break;
			case "T": init(); terms(); break;
			case "TA": init(); termsAndAttrs(); break;
			case "tA": init(); termAttrs(); break;
			case "F": init();  formulas(); break;
			case "FA": init(); formulasAndArgs(); break;
			case "fA": init(); formulaArgs(); break;

			default:
				@NotNull String errMsg = //
						"k\tfiles\n"+  //
								"T\tterms\n"+  //
								"TA\tterms and attributes\n"+  //
								"tA\tterms attributes\n"+  //
								"F\tformulas\n"+ //
								"FA\tformulas and arguments\n"+ //
								"fA\tformulas arguments\n"+ //
								"";
				System.err.println(errMsg);
				break;
			// @formatter:on
		}
		shutdown();

	}

	public static void files()
	{
		try //(
		//SetCollector<SUFile> ignored = SUFile.COLLECTOR.open()
		// )
		{
			Processor.insertFiles(PS, SUFile.COLLECTOR.keySet());
		}
		catch (Exception e)
		{
			fail(e.getMessage());
		}
	}

	public static void termsAndAttrs()
	{
		try //(
		// SetCollector<Term> ignored = Term.COLLECTOR.open()
		// )
		{
			Processor.insertTermsAndAttrs(PS, PS2, Term.COLLECTOR.keySet(), SUMO);
		}
		catch (Exception e)
		{
			fail(e.getMessage());
		}
	}

	public static void termAttrs()
	{
		try //(
		// SetCollector<Term> ignored = Term.COLLECTOR.open()
		// )
		{
			Processor.insertTermAttrs(PS, Term.COLLECTOR.keySet(), SUMO);
		}
		catch (Exception e)
		{
			fail(e.getMessage());
		}
	}

	public static void terms()
	{
		try //(
		// SetCollector<Term> ignored = Term.COLLECTOR.open()
		// )
		{
			Processor.insertTerms(PS, PS, Term.COLLECTOR.keySet());
		}
		catch (Exception e)
		{
			fail(e.getMessage());
		}
	}

	public static void formulasAndArgs()
	{
		try //(
		// SetCollector<Term> ignored = Term.COLLECTOR.open(); //
		// SetCollector<SUFile> ignored2 = SUFile.COLLECTOR.open(); //
		// SetCollector<Formula> ignored3 = Formula.COLLECTOR.open(); //
		//)
		{
			Processor.insertFormulasAndArgs(PS, PS2, Formula.COLLECTOR.keySet());
		}
		catch (Exception e)
		{
			fail(e.getMessage());
		}
	}

	public static void formulas()
	{
		try //(
		// SetCollector<Term> ignored = Term.COLLECTOR.open(); //
		// SetCollector<SUFile> ignored2 = SUFile.COLLECTOR.open(); //
		// SetCollector<Formula> ignored3 = Formula.COLLECTOR.open(); //
		//)
		{
			Processor.insertFormulas(PS, Formula.COLLECTOR.keySet());
		}
		catch (Exception e)
		{
			fail(e.getMessage());
		}
	}

	public static void formulaArgs()
	{
		try //(
		//SetCollector<Term> ignored = Term.COLLECTOR.open(); //
		//SetCollector<File> ignored2 = File.COLLECTOR.open(); //
		//SetCollector<Formula> ignored3 = Formula.COLLECTOR.open(); //
		//)
		{
			Processor.insertFormulaArgs(Helpers.OUT, Formula.COLLECTOR.keySet());
		}
		catch (Exception e)
		{
			fail(e.getMessage());
		}
	}
}
