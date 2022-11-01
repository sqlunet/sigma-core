/*
 * Copyright (c) 2022.
 * This code is copyright Bernard Bou <1313ou@gmail.com>
 * This software is released under the GNU Public License 3 <http://www.gnu.org/copyleft/gpl.html>.
 */

package org.sigma.core.tests;

import org.sigma.core.BaseSumoProvider;
import org.sigma.core.SumoProvider;

import org.junit.jupiter.api.Test;

public class TestSumo
{
	@Test
	public void testLoad()
	{
		BaseSumoProvider.loadKb(BaseSumoProvider.getScope());
	}

	public static void main(String[] args)
	{
		new SumoProvider().load();
		new TestSumo().testLoad();
	}
}
