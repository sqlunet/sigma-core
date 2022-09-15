package com.articulate.sigma;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.sqlunet.sumo.Kb;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestClausalForm
{
	@BeforeAll
	public static void noLogging()
	{
		String loggingPath = "logging.properties";
		System.setProperty("java.util.logging.config.file", loggingPath);
	}

	private static Kb kb;

	@BeforeAll
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

	@Disabled
	@Test
	public void testClausalForm()
	{
		System.out.printf("%nKb making clausal form%n");
		boolean result = kb.makeClausalForms();
		assertTrue(result);
		System.out.printf("%nKb made clausal form%n");
	}
}
