package com.articulate.sigma;

import org.junit.BeforeClass;
import org.junit.Test;
import org.sqlunet.sumo.Dump;
import org.sqlunet.sumo.Kb;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TestDump
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
	}

	@Test
	public void testDumpTerms()
	{
		System.out.println(">>>>>>>>>>");
		Dump.dumpTerms(kb);
		System.out.println("<<<<<<<<<<");
	}

	@Test
	public void testDumpFormulas()
	{
		System.out.println(">>>>>>>>>>");
		Dump.dumpFormulas(kb);
		System.out.println("<<<<<<<<<<");
	}

	@Test
	public void testDumpPredicates()
	{
		System.out.println(">>>>>>>>>>");
		Dump.dumpPredicates(kb);
		System.out.println("<<<<<<<<<<");
	}
}
