package org.sqlunet.sumo;

import com.articulate.sigma.SumoProvider;
import com.articulate.sigma.Utils;

import org.sqlunet.common.NotFoundException;
import org.sqlunet.sumo.objects.Formula;
import org.sqlunet.sumo.objects.SUFile;
import org.sqlunet.sumo.objects.Term;

import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.fail;

public class Main
{
	private static final PrintStream PS = System.out; // ps

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

	public static void main(String[] args) throws NotFoundException
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
			case "k": init(); files();; break;
			case "T": init(); terms(); break;
			case "TA": init(); termsAndAttrs();; break;
			case "tA": init(); termAttrs();; break;
			case "F": init();  formulas();; break;
			case "FA": init(); formulasAndArgs();; break;
			case "fA": init(); formulaArgs();; break;

			default:
				String errMsg = //
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

	public static void termsAndAttrs() throws NotFoundException
	{
		try //(
		// SetCollector<Term> ignored = Term.COLLECTOR.open()
		// )
		{
			Processor.insertTermsAndAttrs(PS, PS, Term.COLLECTOR.keySet(), SUMO);
		}
		catch (Exception e)
		{
			fail(e.getMessage());
		}
	}

	public static void termAttrs() throws NotFoundException
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

	public static void terms() throws NotFoundException
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

	public static void formulasAndArgs() throws NotFoundException
	{
		try //(
		// SetCollector<Term> ignored = Term.COLLECTOR.open(); //
		// SetCollector<SUFile> ignored2 = SUFile.COLLECTOR.open(); //
		// SetCollector<Formula> ignored3 = Formula.COLLECTOR.open(); //
		//)
		{
			Processor.insertFormulasAndArgs(PS, PS, Formula.COLLECTOR.keySet());
		}
		catch (Exception e)
		{
			fail(e.getMessage());
		}
	}

	public static void formulas() throws NotFoundException
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

	public static void formulaArgs() throws NotFoundException
	{
		try //(
		//SetCollector<Term> ignored = Term.COLLECTOR.open(); //
		//SetCollector<File> ignored2 = File.COLLECTOR.open(); //
		//SetCollector<Formula> ignored3 = Formula.COLLECTOR.open(); //
		//)
		{
			Processor.insertFormulaArgs(Utils.OUT, Formula.COLLECTOR.keySet());
		}
		catch (Exception e)
		{
			fail(e.getMessage());
		}
	}
}
