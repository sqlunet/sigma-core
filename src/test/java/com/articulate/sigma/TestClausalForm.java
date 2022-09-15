package com.articulate.sigma;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.sqlunet.sumo.Kb;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestClausalForm
{
	private static Kb kb;

	@Disabled
	@Test
	public void testClausalForm()
	{
		Utils.OUT_INFO.printf("%nKb making clausal form%n");
		boolean result = kb.makeClausalForms();
		assertTrue(result);
		Utils.OUT_INFO.printf("%nKb made clausal form%n");
	}

	@BeforeAll
	public static void init()
	{
		Utils.turnOffLogging();
		kb = Utils.loadKb(Utils.SAMPLE_FILES);
	}

	public static void main(String[] args)
	{
		init();
		TestClausalForm p = new TestClausalForm();
		p.testClausalForm();
	}
}
