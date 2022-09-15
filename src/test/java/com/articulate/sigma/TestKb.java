package com.articulate.sigma;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.sqlunet.sumo.Kb;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestKb
{
	@BeforeAll
	public static void init()
	{
		turnOffLogging();
		getPath();
	}

	private static void turnOffLogging()
	{
		String loggingPath = "logging.properties";
		System.setProperty("java.util.logging.config.file", loggingPath);
	}

	private static void getPath()
	{
		kbPath = System.getProperty("sumopath");
		if (kbPath == null)
		{
			kbPath = System.getenv("SUMOHOME");
		}
		assertNotNull(kbPath, "Pass KB location as -Dsumopath=<somewhere> or SUMOHOME=<somewhere> in env");
	}

	private static String kbPath;

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
