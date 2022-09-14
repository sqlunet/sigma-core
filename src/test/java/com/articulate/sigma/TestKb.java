package com.articulate.sigma;

import org.junit.BeforeClass;
import org.junit.Test;
import org.sqlunet.sumo.Kb;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TestKb
{
	@BeforeClass
	public static void noLogging()
	{
		String loggingPath = "logging.properties";
		System.setProperty("java.util.logging.config.file", loggingPath);
	}

	private static String kbPath;

	@BeforeClass
	public static void init()
	{
		kbPath = System.getProperty("sumopath");
		if (kbPath == null)
		{
			kbPath = System.getenv("SUMOHOME");
		}
		assertNotNull("Pass KB location as -Dsumopath=<somewhere> or SUMOHOME=<somewhere> in env", kbPath);
	}

	@Test
	public void testLoad()
	{
		final Kb kb = new Kb(kbPath);
		System.out.printf("Kb building%n");
		boolean result = kb.make(true);
		assertTrue(result);
		System.out.printf("%nKb built%n");
	}
}
