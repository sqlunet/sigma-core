package com.articulate.sigma;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sqlunet.sumo.exception.NotFoundException;
import org.sqlunet.sumo.Processor;
import org.sqlunet.sumo.Kb;
import org.sqlunet.sumo.objects.SUFile;
import org.sqlunet.sumo.objects.Formula;
import org.sqlunet.sumo.objects.Term;

import java.io.IOException;
import java.text.ParseException;

import static org.junit.Assert.*;

public class TestProcessor
{
	@BeforeClass
	public static void noLogging()
	{
		String loggingPath = "logging.properties";
		System.setProperty("java.util.logging.config.file", loggingPath);
	}

	private static Kb kb;

	private static final String[] FILES = new String[]{"Merge.kif", "Mid-level-ontology.kif", "english_format.kif", "Communication.kif"};

	@BeforeClass
	public static void init()
	{
		String kbPath = System.getProperty("sumopath");
		if (kbPath == null)
		{
			kbPath = System.getenv("SUMOHOME");
		}
		assertNotNull("Pass KB location as -Dsumopath=<somewhere> or SUMOHOME=<somewhere> in env", kbPath);

		System.out.printf("Kb building%n");
		kb = new Kb(kbPath);
		boolean result = kb.make(FILES);
		assertTrue(result);
		System.out.printf("%nKb built%n");

		Processor.collectFiles(kb);
		Processor.collectTerms(kb);
		Processor.collectFormulas(kb);
		SUFile.COLLECTOR.open();
		Term.COLLECTOR.open();
		Formula.COLLECTOR.open();
	}

	@AfterClass
	public static void shutdown()
	{
		SUFile.COLLECTOR.close();
		Term.COLLECTOR.close();
		Formula.COLLECTOR.close();
	}

	@Test
	public void testProcessFiles()
	{
		System.out.println(">>>>>>>>>>");
		try // (SetCollector<SUMOFile> ignored = SUMOFile.COLLECTOR.open())
		{
			Processor.insertFiles(System.out, SUFile.COLLECTOR.keySet());
		}
		catch (Exception e)
		{
			fail(e.getMessage());
		}
		System.out.println("<<<<<<<<<<");
	}

	@Test
	public void testProcessTermsAndAttrs() throws NotFoundException
	{
		System.out.println(">>>>>>>>>>");
		try // (SetCollector<SUMOTerm> ignored = SUMOTerm.COLLECTOR.open())
		{
			Processor.insertTermsAndAttrs(System.out, System.out, Term.COLLECTOR.keySet(), kb);
		}
		catch (Exception e)
		{
			fail(e.getMessage());
		}
		System.out.println("<<<<<<<<<<");
	}

	@Test
	public void testProcessTermAttrs() throws NotFoundException
	{
		System.out.println(">>>>>>>>>>");
		try // (SetCollector<SUMOTerm> ignored = SUMOTerm.COLLECTOR.open())
		{
			Processor.insertTermAttrs(System.out, Term.COLLECTOR.keySet(), kb);
		}
		catch (Exception e)
		{
			fail(e.getMessage());
		}
		System.out.println("<<<<<<<<<<");
	}

	@Test
	public void testProcessTerms() throws NotFoundException
	{
		System.out.println(">>>>>>>>>>");
		try // (SetCollector<SUMOTerm> ignored = SUMOTerm.COLLECTOR.open())
		{
			Processor.insertTerms(System.out, System.out, Term.COLLECTOR.keySet());
		}
		catch (Exception e)
		{
			fail(e.getMessage());
		}
		System.out.println("<<<<<<<<<<");
	}

	@Test
	public void testProcessFormulasAndArgs() throws NotFoundException, ParseException, IOException
	{
		System.out.println(">>>>>>>>>>");
		try //(
		//SetCollector<SUMOTerm> ignored = SUMOTerm.COLLECTOR.open(); //
		//SetCollector<SUMOFile> ignored2 = SUMOFile.COLLECTOR.open(); //
		//SetCollector<SUMOFormula> ignored3 = SUMOFormula.COLLECTOR.open(); //
		//)
		{
			Processor.insertFormulasAndArgs(System.out, System.out, Formula.COLLECTOR.keySet());
		}
		catch (Exception e)
		{
			fail(e.getMessage());
		}
		System.out.println("<<<<<<<<<<");
	}

	@Test
	public void testProcessFormulas() throws NotFoundException, ParseException, IOException
	{
		System.out.println(">>>>>>>>>>");
		try //(
		//SetCollector<SUMOTerm> ignored = SUMOTerm.COLLECTOR.open(); //
		//SetCollector<SUMOFile> ignored2 = SUMOFile.COLLECTOR.open(); //
		//SetCollector<SUMOFormula> ignored3 = SUMOFormula.COLLECTOR.open(); //
		//)
		{
			Processor.insertFormulas(System.out, Formula.COLLECTOR.keySet());
		}
		catch (Exception e)
		{
			fail(e.getMessage());
		}
		System.out.println("<<<<<<<<<<");
	}

	@Test
	public void testProcessFormulasArgs() throws NotFoundException, ParseException, IOException
	{
		System.out.println(">>>>>>>>>>");
		try //(
		//SetCollector<SUMOTerm> ignored = SUMOTerm.COLLECTOR.open(); //
		//SetCollector<SUMOFile> ignored2 = SUMOFile.COLLECTOR.open(); //
		//SetCollector<SUMOFormula> ignored3 = SUMOFormula.COLLECTOR.open(); //
		//)
		{
			Processor.insertFormulaArgs(System.out, Formula.COLLECTOR.keySet());
		}
		catch (Exception e)
		{
			fail(e.getMessage());
		}
		System.out.println("<<<<<<<<<<");
	}
}
