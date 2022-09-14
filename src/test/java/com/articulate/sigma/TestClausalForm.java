package com.articulate.sigma;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.sqlunet.sumo.Kb;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TestClausalForm
{
	@BeforeClass
	public static void noLogging()
	{
		String loggingPath = "logging.properties";
		System.setProperty("java.util.logging.config.file", loggingPath);
	}

	private static Kb kb;

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
		boolean result = kb.make(true);
		assertTrue(result);
		System.out.printf("%nKb built%n");
	}

	@Ignore
	@Test
	public void testClausalForm()
	{
		System.out.printf("%nKb making clausal form%n");
		boolean result = kb.makeClausalForms();
		assertTrue(result);
		System.out.printf("%nKb made clausal form%n");
	}
}
