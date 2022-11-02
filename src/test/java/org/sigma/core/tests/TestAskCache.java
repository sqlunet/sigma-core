/*
 * Copyright (c) 2022.
 * This code is copyright Bernard Bou <1313ou@gmail.com>
 * This software is released under the GNU Public License 3 <http://www.gnu.org/copyleft/gpl.html>.
 */

package org.sigma.core.tests;

import org.sigma.core.Dump;
import org.sigma.core.SumoProvider;
import org.sigma.core.Utils;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith({SumoProvider.class})
public class TestAskCache
{
	@Test
	public void testDumpPredicates()
	{
		Dump.dumpPredicates(SumoProvider.SUMO, Utils.OUT);
	}

	@Test
	public void testDumpSuperClassesOf()
	{
		Dump.dumpSuperClassesOfWithPredicateSubsumption(SumoProvider.SUMO, "Insect", Utils.OUT);
	}

	@Test
	public void testDumpSubClassesOf()
	{
		Dump.dumpSubClassesOfWithPredicateSubsumption(SumoProvider.SUMO, "Insect", Utils.OUT);
	}

	@Test
	public void testDumpFunctions()
	{
		Dump.dumpFunctions(SumoProvider.SUMO, Utils.OUT);
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
		new SumoProvider().load();
		init();
		TestAskCache d = new TestAskCache();
	}
}