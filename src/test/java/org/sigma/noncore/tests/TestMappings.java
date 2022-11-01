/*
 * Copyright (c) 2022.
 * This code is copyright Bernard Bou <1313ou@gmail.com>
 * This software is released under the GNU Public License 3 <http://www.gnu.org/copyleft/gpl.html>.
 */

package org.sigma.noncore.tests;

import org.sigma.core.BaseSumoProvider;
import org.sigma.core.Utils;

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
