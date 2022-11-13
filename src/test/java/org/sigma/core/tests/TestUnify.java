/*
 * Copyright (c) 2022.
 * This code is copyright Bernard Bou <1313ou@gmail.com>
 * This software is released under the GNU Public License 3 <http://www.gnu.org/copyleft/gpl.html>.
 */

package org.sigma.core.tests;

import org.sigma.core.Formula;
import org.sigma.core.NotNull;
import org.sigma.core.SumoProvider;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.sigma.core.Helpers.OUT;

//@ExtendWith({KBLoader.class})
public class TestUnify
{
	//@Disabled
	@Test
	public void testUnify()
	{
		@NotNull Formula f1 = Formula.of("(Man ?X)");
		@NotNull Formula f2 = Formula.of("(Man Lincoln)");

		testUnify(f1, f2);
	}

	public void testUnify(@NotNull final Formula f1, @NotNull final Formula f2)
	{
		Map<String, String> u = f1.unify(f2);
		OUT.println("f1 u f2=" + u);

		Map<String, String> u2 = f2.unify(f1);
		OUT.println("f2 u f1=" + u2);
	}

	public static void main(String[] args) throws IOException
	{
		new SumoProvider().load();
		@NotNull TestUnify p = new TestUnify();
		p.testUnify();
	}
}
