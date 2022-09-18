package com.articulate.sigma;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

public class TestLisp
{
	private static final boolean silent = System.getProperties().containsKey("SILENT");

	private static final String[] ATOMS = { //
			"'a b'","a", "a b", "\"a b\"","a \" b", "'a b'","a ' b", "",}; //

	private static final String[] LISTS = { //
			"()", "(a)", "(a b)", "(a b c)", "(a b c \"d e\")", "(=> (a ?W) (b ?W))",};

	private static final Collection<String> ALL = new ArrayList<>();

	static
	{
		Collections.addAll(ALL, ATOMS);
		Collections.addAll(ALL, LISTS);
	}

	@Test
	public void formatTest()
	{
		for (String form : ALL)
		{
			System.out.println("[" + form + "] -> [" + new Formula(form).format("", "", "\n") + "]");
		}
	}

	@Test
	public void cdrTestOnLists()
	{
		cdrTest(LISTS, f -> assertTrue(f.listP()));
	}

	@Test
	public void cdrTestOnAtoms()
	{
		cdrTest(ATOMS, f -> assertFalse(f.listP()));
	}

	public void cdrTest(String[] fs, Consumer<Formula> test)
	{
		for (String list : fs)
		{
			Formula f = new Formula(list);
			test.accept(f);

			String car = f.car();
			String cdr = f.cdr();
			Formula carF = new Formula(car);
			Formula cdrF = f.cdrAsFormula();

			System.out.println("formula=\"" + f + "\"");
			System.out.println("\tcar=" + car);
			System.out.println("\tcdr=" + cdr);
			System.out.println("\tcarF=" + carF);
			System.out.println("\tcdrF=" + cdrF);

			int i = 1;
			for (Formula itF = f.cdrAsFormula(); itF != null && itF.listP(); itF = itF.cdrAsFormula())
			{
				System.out.println("\tcdrF" + i + "=" + itF);
				if (itF.empty())
				{
					break;
				}
				i += 1;
			}
		}
	}

	public static void main(String[] args)
	{
		new TestLisp().cdrTestOnLists();
	}
}
