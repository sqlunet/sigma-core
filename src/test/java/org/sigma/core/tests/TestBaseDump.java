/*
 * Copyright (c) 2022.
 * This code is copyright Bernard Bou <1313ou@gmail.com>
 * This software is released under the GNU Public License 3 <http://www.gnu.org/copyleft/gpl.html>.
 */

package org.sigma.core.tests;

import org.sigma.core.BaseSumoProvider;
import org.sigma.core.Dump;
import org.sigma.core.Utils;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sqlunet.sumo.Processor;
import org.sqlunet.sumo.objects.Formula;
import org.sqlunet.sumo.objects.SUFile;
import org.sqlunet.sumo.objects.Term;

@ExtendWith({BaseSumoProvider.class})
public class TestBaseDump
{
	@Test
	public void testDumpTerms()
	{
		Dump.dumpTerms(BaseSumoProvider.SUMO, Utils.OUT);
	}

	@Test
	public void testDumpFormulas()
	{
		Dump.dumpFormulas(BaseSumoProvider.SUMO, Utils.OUT);
	}

	@BeforeAll
	public static void init()
	{
		Processor.collectFiles(BaseSumoProvider.SUMO);
		Processor.collectTerms(BaseSumoProvider.SUMO);
		Processor.collectFormulas(BaseSumoProvider.SUMO);

		SUFile.COLLECTOR.open();
		Term.COLLECTOR.open();
		Formula.COLLECTOR.open();
	}

	@AfterAll
	public static void shutdown()
	{
		SUFile.COLLECTOR.close();
		Term.COLLECTOR.close();
		Formula.COLLECTOR.close();
	}

	public static void main(String[] args)
	{
		new BaseSumoProvider().load();
		init();
		TestBaseDump d = new TestBaseDump();
		d.testDumpTerms();
		d.testDumpFormulas();
		d.testDumpFormulas();
	}
}
