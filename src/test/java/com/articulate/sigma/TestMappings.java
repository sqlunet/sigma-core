package com.articulate.sigma;

import org.junit.BeforeClass;
import org.junit.Test;
import org.sqlunet.sumo.SUMO_Wn_Processor;

import java.io.IOException;

import static org.junit.Assert.*;

public class TestMappings
{
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
	public void testMappings()
	{
		SUMO_Wn_Processor processor = new SUMO_Wn_Processor(kbPath);
		try
		{
			processor.run(System.out);
		}
		catch (IOException ioe)
		{
			fail(ioe.getMessage());
		}
	}
}
