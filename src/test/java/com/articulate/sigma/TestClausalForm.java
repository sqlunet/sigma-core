package com.articulate.sigma;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith({SumoProvider.class})
public class TestClausalForm
{
	//@Disabled
	@Test
	public void testClausalForm()
	{
		Utils.OUT_INFO.printf("%nKb making clausal form%n");
		boolean result = SumoProvider.sumo.makeClausalForms();
		assertTrue(result);
		Utils.OUT_INFO.printf("%nKb made clausal form%n");

		for (Collection<Formula> fs : SumoProvider.sumo.formulaIndex.values())
		{
			for (Formula f : fs)
			{
				Tuple.Triple<List<Clause>, Map<String, String>, Formula> cf = f.getClausalForms();
				assert cf != null;
				assert cf.first != null;
				if(cf.first.isEmpty() && cf.second.isEmpty())
					continue;
				if(cf.first.get(0).negativeLits.isEmpty())
					continue;

				Utils.OUT.println(Clause.cfToString(cf));
			}
		}
	}

	public static void main(String[] args)
	{
		new SumoProvider().load();
		TestClausalForm p = new TestClausalForm();
		p.testClausalForm();
	}
}
