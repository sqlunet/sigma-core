/*
 * Copyright (c) 2022.
 * This code is copyright Bernard Bou <1313ou@gmail.com>
 * This software is released under the GNU Public License 3 <http://www.gnu.org/copyleft/gpl.html>.
 */

package org.sigma.core.tests;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sigma.core.*;

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
		Utils.INFO_OUT.printf("%nKb making clausal form%n");
		boolean result = SumoProvider.SUMO.makeClausalForms();
		assertTrue(result);
		Utils.INFO_OUT.printf("%nKb made clausal form%n");

		for (Collection<Formula> fs : SumoProvider.SUMO.formulaIndex.values())
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
