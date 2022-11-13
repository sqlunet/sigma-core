/*
 * Copyright (c) 2022.
 * This code is copyright Bernard Bou <1313ou@gmail.com>
 * This software is released under the GNU Public License 3 <http://www.gnu.org/copyleft/gpl.html>.
 */

package org.sigma.core.tests;

import org.sigma.core.NotNull;
import org.sigma.core.SumoProvider;
import org.sigma.core.Helpers;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sqlunet.sumo.Processor;
import org.sqlunet.sumo.objects.Formula;
import org.sqlunet.sumo.objects.SUFile;
import org.sqlunet.sumo.objects.Term;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith({SumoProvider.class})
public class TestProcessor
{
	@Test
	public void testProcessFiles()
	{
		try // (SetCollector<File> ignored = File.COLLECTOR.open())
		{
			Processor.insertFiles(Helpers.OUT, SUFile.COLLECTOR.keySet());
		}
		catch (Exception e)
		{
			fail(e.getMessage());
		}
	}

	@Test
	public void testProcessTermsAndAttrs()
	{
		try // (SetCollector<Term> ignored = Term.COLLECTOR.open())
		{
			Processor.insertTermsAndAttrs(Helpers.OUT, Helpers.OUT, Term.COLLECTOR.keySet(), SumoProvider.SUMO);
		}
		catch (Exception e)
		{
			fail(e.getMessage());
		}
	}

	@Test
	public void testProcessTermAttrs()
	{
		try // (SetCollector<Term> ignored = Term.COLLECTOR.open())
		{
			Processor.insertTermAttrs(Helpers.OUT, Term.COLLECTOR.keySet(), SumoProvider.SUMO);
		}
		catch (Exception e)
		{
			fail(e.getMessage());
		}
	}

	@Test
	public void testProcessTerms()
	{
		try // (SetCollector<Term> ignored = Term.COLLECTOR.open())
		{
			Processor.insertTerms(Helpers.OUT, Term.COLLECTOR.keySet());
		}
		catch (Exception e)
		{
			fail(e.getMessage());
		}
	}

	@Test
	public void testProcessFormulasAndArgs()
	{
		try //(
		//SetCollector<Term> ignored = Term.COLLECTOR.open(); //
		//SetCollector<File> ignored2 = File.COLLECTOR.open(); //
		//SetCollector<Formula> ignored3 = Formula.COLLECTOR.open(); //
		//)
		{
			Processor.insertFormulasAndArgs(Helpers.OUT, Helpers.OUT, Formula.COLLECTOR.keySet());
		}
		catch (Exception e)
		{
			fail(e.getMessage());
		}
	}

	@Test
	public void testProcessFormulas()
	{
		try //(
		//SetCollector<Term> ignored = Term.COLLECTOR.open(); //
		//SetCollector<File> ignored2 = File.COLLECTOR.open(); //
		//SetCollector<Formula> ignored3 = Formula.COLLECTOR.open(); //
		//)
		{
			Processor.insertFormulas(Helpers.OUT, Formula.COLLECTOR.keySet());
		}
		catch (Exception e)
		{
			fail(e.getMessage());
		}
	}

	@Test
	public void testProcessFormulasArgs()
	{
		try //(
		//SetCollector<Term> ignored = Term.COLLECTOR.open(); //
		//SetCollector<File> ignored2 = File.COLLECTOR.open(); //
		//SetCollector<Formula> ignored3 = Formula.COLLECTOR.open(); //
		//)
		{
			Processor.insertFormulaArgs(Helpers.OUT, Formula.COLLECTOR.keySet());
		}
		catch (Exception e)
		{
			fail(e.getMessage());
		}
	}

	@BeforeAll
	public static void init()
	{
		Processor.collectFiles(SumoProvider.SUMO);
		Processor.collectTerms(SumoProvider.SUMO);
		Processor.collectFormulas(SumoProvider.SUMO);

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

	public static void main(String[] args) throws IOException
	{
		new SumoProvider().load();
		init();
		@NotNull TestProcessor p = new TestProcessor();
		p.testProcessFiles();
		p.testProcessTermsAndAttrs();
		p.testProcessFormulasAndArgs();
		shutdown();
	}
}
