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
import org.owl.OWLTranslator2;
import org.sigma.core.BaseSumoProvider;
import org.sigma.core.Helpers;
import org.sigma.core.NotNull;
import org.sigma.core.Nullable;

import java.io.IOException;
import java.io.InputStream;

@ExtendWith({BaseSumoProvider.class})
public class TestOWL2
{
	static OWLTranslator2 TRANSLATOR;

	@BeforeAll
	public static void init() throws IOException
	{
		try (@Nullable InputStream is = OWLTranslator2.class.getResourceAsStream("/functionalterms-tests.kif"))
		{
			BaseSumoProvider.SUMO.addConstituent(is, "subsumption-tests");
		}

		TRANSLATOR = new OWLTranslator2(BaseSumoProvider.SUMO);
		TRANSLATOR.initOnce();
	}

	@AfterAll
	public static void shutdown()
	{
	}

	@Test
	public void write() throws IOException
	{
		TRANSLATOR.write(Helpers.OUT);
	}

	@Test
	public void writeInstances() throws IOException
	{
		TRANSLATOR.writeInstances(Helpers.OUT);
	}

	@Test
	public void writeClasses() throws IOException
	{
		TRANSLATOR.writeClasses(Helpers.OUT);
	}

	@Test
	public void writeRelations() throws IOException
	{
		TRANSLATOR.writeRelations(Helpers.OUT);
	}

	@Test
	public void writeFunctions()
	{
		TRANSLATOR.writeFunctionalTerms(Helpers.OUT);
	}

	public static void main(String[] args) throws IOException
	{
		new BaseSumoProvider().load();
		init();
		@NotNull TestOWL2 t = new TestOWL2();
		t.write();
		shutdown();
	}
}
