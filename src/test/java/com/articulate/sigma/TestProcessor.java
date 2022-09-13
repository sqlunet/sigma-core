package com.articulate.sigma;

import org.junit.BeforeClass;
import org.junit.Test;
import org.sqlunet.sumo.NotFoundException;
import org.sqlunet.sumo.Processor;
import org.sqlunet.sumo.SUMOKb;
import org.sqlunet.sumo.SetCollector;
import org.sqlunet.sumo.objects.SUMOFile;
import org.sqlunet.sumo.objects.SUMOFormula;
import org.sqlunet.sumo.objects.SUMOTerm;

import java.io.IOException;
import java.text.ParseException;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TestProcessor
{
	@BeforeClass
	public static void noLogging()
	{
		String loggingPath = "logging.properties";
		System.setProperty("java.util.logging.config.file", loggingPath);
	}

	private static SUMOKb kb;

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
		kb = new SUMOKb(kbPath);
		boolean result = kb.make(FILES);
		assertTrue(result);
		System.out.printf("%nKb built%n");

		Processor.collectFiles(kb);
		Processor.collectTerms(kb);
		Processor.collectFormulas(kb);
	}

	@Test
	public void testProcessTerms() throws NotFoundException
	{
		System.out.println(">>>>>>>>>>");
		try (SetCollector<SUMOTerm> ignored = SUMOTerm.COLLECTOR.open())
		{
			Processor.insertTerms(System.out, System.out, SUMOTerm.COLLECTOR.toHashMap().keySet());
		}
		System.out.println("<<<<<<<<<<");
	}

	@Test
	public void testProcessFormulas() throws NotFoundException, ParseException, IOException
	{
		System.out.println(">>>>>>>>>>");
		try ( //
		      //SetCollector<SUMOTerm> ignored = SUMOTerm.COLLECTOR.open(); //
		      //SetCollector<SUMOFile> ignored2 = SUMOFile.COLLECTOR.open(); //
		      SetCollector<SUMOFormula> ignored3 = SUMOFormula.COLLECTOR.open(); //
		)
		{
			Processor.insertFormulas(System.out, System.out, SUMOFormula.COLLECTOR.keySet());
			System.out.println("<<<<<<<<<<");
		}
	}

	@Test
	public void testProcessFiles()
	{
		System.out.println(">>>>>>>>>>");
		try (SetCollector<SUMOFile> ignored = SUMOFile.COLLECTOR.open())
		{
			Processor.insertFiles(System.out, SUMOFile.COLLECTOR.keySet());
		}
		System.out.println("<<<<<<<<<<");
	}
}
