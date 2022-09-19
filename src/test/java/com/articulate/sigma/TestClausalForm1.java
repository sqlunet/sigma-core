package com.articulate.sigma;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith({KBLoader.class})
public class TestClausalForm1
{
	//@Disabled
	@Test
	public void testClausalForm()
	{
		Formula f = Formula.of("(waterDepth ?S ?W)");
		Tuple.Triple<List<Clause>, Map<String, String>, Formula> cf = f.getClausalForms();
		System.out.println(Clause.cfToString(cf));
	}

	public static void main(String[] args)
	{
		new KBLoader().load();
		TestClausalForm1 p = new TestClausalForm1();
		p.testClausalForm();
	}
}
