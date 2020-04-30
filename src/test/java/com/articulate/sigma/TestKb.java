package com.articulate.sigma;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class TestKb
{
	@Before public void noLogging()
	{
		String loggingPath = "logging.properties";
		System.setProperty("java.util.logging.config.file", loggingPath);
	}

	@Test public void loadTest()
	{
		//String kbPath = System.getProperty("sumopath");
		//assertNotNull("Pass KB location as -Dsumopath=<somewhere>" , kbPath);

		String kbPath = System.getenv("SUMOHOME");
		final SUMOKb kb = new SUMOKb(kbPath);

		System.out.printf("Kb building%n");
		boolean result = kb.make(true);
		assertTrue(result);
		System.out.printf("%nKb built%n");

		System.out.println("Done");
	}

	@Ignore
	@Test public void makeClausalForm()
	{
		//String kbPath = System.getProperty("sumopath");
		//assertNotNull("Pass KB location as -Dsumopath=<somewhere>" , kbPath);

		String kbPath = System.getenv("SUMOHOME");
		final SUMOKb kb = new SUMOKb(kbPath);

		System.out.printf("Kb building%n");
		boolean result = kb.make(true);
		assertTrue(result);
		System.out.printf("%nKb built%n");

		System.out.printf("%nKb making clausal form%n");
		result = kb.makeClausalForms();
		assertTrue(result);
		System.out.printf("%nKb made clausal form%n");

		System.out.println("Done");
	}
}
