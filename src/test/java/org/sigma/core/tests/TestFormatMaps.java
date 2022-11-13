/*
 * Copyright (c) 2022.
 * This code is copyright Bernard Bou <1313ou@gmail.com>
 * This software is released under the GNU Public License 3 <http://www.gnu.org/copyleft/gpl.html>.
 */

package org.sigma.core.tests;

import org.sigma.core.BaseSumoProvider;
import org.sigma.core.Helpers;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sigma.core.NotNull;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith({BaseSumoProvider.class})
public class TestFormatMaps
{
	@Test
	public void testFormMaps()
	{
		Map<String, String> fm = BaseSumoProvider.SUMO.getFormatMap("EnglishLanguage");
		assertNotNull(fm);
		String mapped = fm.get("entails");
		Helpers.OUT.println(mapped);
		assertEquals("%1 %n{doesn't} &%entail%p{s} %2", mapped);
	}

	@Test
	public void testTermFormMaps()
	{
		Map<String, String> fm = BaseSumoProvider.SUMO.getTermFormatMap("EnglishLanguage");
		assertNotNull(fm);
		String mapped = fm.get("Entity");
		Helpers.OUT.println(mapped);
		assertEquals("entity", mapped);
	}

	@SuppressWarnings("EmptyMethod")
	@BeforeAll
	public static void init()
	{
	}

	@SuppressWarnings("EmptyMethod")
	@AfterAll
	public static void shutdown()
	{
	}

	public static void main(String[] args) throws IOException
	{
		new BaseSumoProvider().load();
		init();
		@NotNull TestFormatMaps t = new TestFormatMaps();
		t.testFormMaps();
	}
}
