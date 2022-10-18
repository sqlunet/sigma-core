package com.articulate.sigma;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sqlunet.sumo.Dump;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith({KBLoader.class})
public class TestAskCache
{
	@Test
	public void testDumpPredicates()
	{
		Dump.dumpPredicates(KBLoader.kb, Utils.OUT);
	}

	@Test
	public void testDumpSuperClassesOf()
	{
		Dump.dumpSuperClassesOf(KBLoader.kb, "Insect", Utils.OUT);
	}

	@Test
	public void testDumpSubClassesOf()
	{
		Dump.dumpSubClassesOf(KBLoader.kb, "Insect", Utils.OUT);
	}

	@Test
	public void testDumpFunctions()
	{
		Dump.dumpFunctions(KBLoader.kb, Utils.OUT);
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
		new KBLoader().load();
		init();
		TestAskCache d = new TestAskCache();
	}
}
