package com.articulate.sigma;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith({KBLoader.class})
public class TestClausalForm
{
	//@Disabled
	@Test
	public void testClausalForm()
	{
		Utils.OUT_INFO.printf("%nKb making clausal form%n");
		boolean result = KBLoader.kb.makeClausalForms();
		assertTrue(result);
		Utils.OUT_INFO.printf("%nKb made clausal form%n");
	}

	public static void main(String[] args)
	{
		new KBLoader().load();
		TestClausalForm p = new TestClausalForm();
		p.testClausalForm();
	}
}
