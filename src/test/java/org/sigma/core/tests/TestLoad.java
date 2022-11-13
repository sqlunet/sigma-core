/*
 * Copyright (c) 2022.
 * This code is copyright Bernard Bou <1313ou@gmail.com>
 * This software is released under the GNU Public License 3 <http://www.gnu.org/copyleft/gpl.html>.
 */

package org.sigma.core.tests;

import org.junit.jupiter.api.BeforeAll;
import org.sigma.core.BaseSumoProvider;

import org.junit.jupiter.api.Test;
import org.sigma.core.Logging;

import java.io.IOException;

public class TestLoad
{
	@Test
	public void testLoad() throws IOException
	{
		BaseSumoProvider.loadKb();
	}

	@BeforeAll
	public static void init()
	{
		Logging.setLogging();
	}

	public static void main(String[] args) throws IOException
	{
		init();
		new TestLoad().testLoad();
	}
}
