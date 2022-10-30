package com.articulate.sigma;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.sqlunet.sumo.SUMO_Wn_Processor;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.fail;

public class TestMappings
{
	private static String kbPath;

	@BeforeAll
	public static void init()
	{
		Utils.turnOffLogging();
		kbPath = BaseSumoProvider.getPath();
	}

	@Test
	public void testMappings()
	{
		SUMO_Wn_Processor processor = new SUMO_Wn_Processor(kbPath);
		try
		{
			processor.run(Utils.OUT, Utils.OUT_WARN);
		}
		catch (IOException ioe)
		{
			fail(ioe.getMessage());
		}
	}

	public static void main(String[] args)
	{
		init();
		TestMappings t = new TestMappings();
		t.testMappings();
	}
}
