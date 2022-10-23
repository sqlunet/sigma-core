package com.articulate.sigma;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith({BaseSumoProvider.class})
public class TestFormatMaps
{
	@Test
	public void testFormMaps()
	{
		Map<String, String> fm = BaseSumoProvider.sumo.getFormatMap("EnglishLanguage");
		assertNotNull(fm);
		String mapped = fm.get("entails");
		Utils.OUT.println(mapped);
		assertEquals("%1 %n{doesn't} &%entail%p{s} %2", mapped);
	}

	@Test
	public void testTermFormMaps()
	{
		Map<String, String> fm = BaseSumoProvider.sumo.getTermFormatMap("EnglishLanguage");
		assertNotNull(fm);
		String mapped = fm.get("Entity");
		Utils.OUT.println(mapped);
		assertEquals("entity", mapped);
	}

	@BeforeAll
	public static void init()
	{
	}

	@AfterAll
	public static void shutdown()
	{
	}

	public static void main(String[] args)
	{
		new BaseSumoProvider().load();
		init();
		TestFormatMaps t = new TestFormatMaps();
		t.testFormMaps();
	}
}
