package com.articulate.sigma;

import org.junit.jupiter.api.Test;

public class TestSumo
{
	@Test
	public void testLoad()
	{
		Utils.loadKb(Utils.getScope());
	}

	public static void main(String[] args)
	{
		new SumoProvider().load();
		new TestSumo().testLoad();
	}
}
