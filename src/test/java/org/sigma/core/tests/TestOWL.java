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
import org.owl.OWLTranslator;
import org.sigma.core.BaseSumoProvider;
import org.sigma.core.Helpers;
import org.sigma.core.NotNull;

import java.io.IOException;

@ExtendWith({BaseSumoProvider.class})
public class TestOWL
{
	static OWLTranslator TRANSLATOR;

	@BeforeAll
	public static void init() throws IOException
	{
		TRANSLATOR = new OWLTranslator(BaseSumoProvider.SUMO);
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
		@NotNull TestOWL t = new TestOWL();
		t.write();
		shutdown();
	}
}
