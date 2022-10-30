package com.articulate.sigma;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sqlunet.sumo.Processor;
import org.sqlunet.common.NotFoundException;
import org.sqlunet.sumo.objects.Formula;
import org.sqlunet.sumo.objects.SUFile;
import org.sqlunet.sumo.objects.Term;

import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith({SumoProvider.class})
public class TestProcessor
{
	@Test
	public void testProcessFiles()
	{
		try // (SetCollector<File> ignored = File.COLLECTOR.open())
		{
			Processor.insertFiles(Utils.OUT, SUFile.COLLECTOR.keySet());
		}
		catch (Exception e)
		{
			fail(e.getMessage());
		}
	}

	@Test
	public void testProcessTermsAndAttrs() throws NotFoundException
	{
		try // (SetCollector<Term> ignored = Term.COLLECTOR.open())
		{
			Processor.insertTermsAndAttrs(Utils.OUT, Utils.OUT, Term.COLLECTOR.keySet(), SumoProvider.sumo);
		}
		catch (Exception e)
		{
			fail(e.getMessage());
		}
	}

	@Test
	public void testProcessTermAttrs() throws NotFoundException
	{
		try // (SetCollector<Term> ignored = Term.COLLECTOR.open())
		{
			Processor.insertTermAttrs(Utils.OUT, Term.COLLECTOR.keySet(), SumoProvider.sumo);
		}
		catch (Exception e)
		{
			fail(e.getMessage());
		}
	}

	@Test
	public void testProcessTerms() throws NotFoundException
	{
		try // (SetCollector<Term> ignored = Term.COLLECTOR.open())
		{
			Processor.insertTerms(Utils.OUT, Utils.OUT, Term.COLLECTOR.keySet());
		}
		catch (Exception e)
		{
			fail(e.getMessage());
		}
	}

	@Test
	public void testProcessFormulasAndArgs() throws NotFoundException
	{
		try //(
		//SetCollector<Term> ignored = Term.COLLECTOR.open(); //
		//SetCollector<File> ignored2 = File.COLLECTOR.open(); //
		//SetCollector<Formula> ignored3 = Formula.COLLECTOR.open(); //
		//)
		{
			Processor.insertFormulasAndArgs(Utils.OUT, Utils.OUT, Formula.COLLECTOR.keySet());
		}
		catch (Exception e)
		{
			fail(e.getMessage());
		}
	}

	@Test
	public void testProcessFormulas() throws NotFoundException
	{
		try //(
		//SetCollector<Term> ignored = Term.COLLECTOR.open(); //
		//SetCollector<File> ignored2 = File.COLLECTOR.open(); //
		//SetCollector<Formula> ignored3 = Formula.COLLECTOR.open(); //
		//)
		{
			Processor.insertFormulas(Utils.OUT, Formula.COLLECTOR.keySet());
		}
		catch (Exception e)
		{
			fail(e.getMessage());
		}
	}

	@Test
	public void testProcessFormulasArgs() throws NotFoundException
	{
		try //(
		//SetCollector<Term> ignored = Term.COLLECTOR.open(); //
		//SetCollector<File> ignored2 = File.COLLECTOR.open(); //
		//SetCollector<Formula> ignored3 = Formula.COLLECTOR.open(); //
		//)
		{
			Processor.insertFormulaArgs(Utils.OUT, Formula.COLLECTOR.keySet());
		}
		catch (Exception e)
		{
			fail(e.getMessage());
		}
	}

	@BeforeAll
	public static void init()
	{
		Processor.collectFiles(SumoProvider.sumo);
		Processor.collectTerms(SumoProvider.sumo);
		Processor.collectFormulas(SumoProvider.sumo);

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

	public static void main(String[] args) throws NotFoundException
	{
		new SumoProvider().load();
		init();
		TestProcessor p = new TestProcessor();
		p.testProcessFiles();
		p.testProcessTermsAndAttrs();
		p.testProcessFormulasAndArgs();
		shutdown();
	}
}
