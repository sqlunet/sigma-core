package com.articulate.sigma;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sqlunet.sumo.Dump;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith({SumoLoader.class})
public class TestAskCache
{
	@Test
	public void testDumpPredicates()
	{
		Dump.dumpPredicates(SumoLoader.sumo, Utils.OUT);
	}

	@Test
	public void testDumpSuperClassesOf()
	{
		Dump.dumpSuperClassesOf(SumoLoader.sumo, "Insect", Utils.OUT);
	}

	@Test
	public void testDumpSubClassesOf()
	{
		Dump.dumpSubClassesOf(SumoLoader.sumo, "Insect", Utils.OUT);
	}

	@Test
	public void testDumpFunctions()
	{
		Dump.dumpFunctions(SumoLoader.sumo, Utils.OUT);
	}

	@BeforeAll
	public static void init()
	{
	}

	@AfterAll
	public static void shutdown()
	{
	}

	public static void main(String[] args)
	{
		new SumoLoader().load();
		init();
		TestAskCache d = new TestAskCache();
	}
}
