/*
 * Copyright (c) 2022.
 * This code is copyright Bernard Bou <1313ou@gmail.com>
 * This software is released under the GNU Public License 3 <http://www.gnu.org/copyleft/gpl.html>.
 */

package org.sigma.core.tests;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.owl.OWLtranslator;
import org.sigma.core.BaseSumoProvider;
import org.sigma.core.Dump;
import org.sigma.core.Helpers;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

@ExtendWith({BaseSumoProvider.class})
public class TestOWL
{
	static OWLtranslator TRANSLATOR;

	@BeforeAll
	public static void init() throws IOException
	{
		TRANSLATOR = new OWLtranslator(BaseSumoProvider.SUMO);
	}

	@AfterAll
	public static void shutdown()
	{
	}

	@Test
	public void write()
	{
		TRANSLATOR.write(Helpers.OUT);
	}

	@Test
	public void writeInstances()
	{
		TRANSLATOR.writeInstances(Helpers.OUT);
	}

	@Test
	public void writeClasses()
	{
		TRANSLATOR.writeClasses(Helpers.OUT);
	}

	@Test
	public void writeRelations()
	{
		TRANSLATOR.writeRelations(Helpers.OUT);
	}

	public static void main(String[] args) throws IOException
	{
		new BaseSumoProvider().load();
		init();
		TestOWL t = new TestOWL();
		t.write();
		shutdown();
	}
}
