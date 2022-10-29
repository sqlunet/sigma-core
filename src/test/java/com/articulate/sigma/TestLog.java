package com.articulate.sigma;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
		Utils.turnOffLogging();
	}

	@AfterAll
	public static void shutdown()
	{
	}

	public static void main(String[] args)
	{
		init();
		TestLog l = new TestLog();
		l.testInfo();
		l.testWarn();
		l.testSevere();
		l.testFine();
	}
}
