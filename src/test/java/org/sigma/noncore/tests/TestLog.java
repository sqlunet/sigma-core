/*
 * Copyright (c) 2022.
 * This code is copyright Bernard Bou <1313ou@gmail.com>
 * This software is released under the GNU Public License 3 <http://www.gnu.org/copyleft/gpl.html>.
 */

package org.sigma.noncore.tests;

import org.sigma.core.Helpers;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.sigma.core.NotNull;

import java.util.logging.Logger;

@Disabled
public class TestLog
{
	private static final String LOG_SOURCE = "TestLog";

	private static final Logger LOGGER = Logger.getLogger(TestLog.class.getName());

	@Test
	public void testInfo()
	{
		LOGGER.info("info");
	}

	@Test
	public void testWarn()
	{
		LOGGER.warning("warn");
	}

	@Test
	public void testSevere()
	{
		LOGGER.severe("severe");
	}

	@Test
	public void testFine()
	{
		LOGGER.fine("fine");
	}

	@BeforeAll
	public static void init()
	{
		Helpers.turnOffLogging();
	}

	@AfterAll
	public static void shutdown()
	{
	}

	public static void main(String[] args)
	{
		init();
		@NotNull TestLog l = new TestLog();
		l.testInfo();
		l.testWarn();
		l.testSevere();
		l.testFine();
	}
}
