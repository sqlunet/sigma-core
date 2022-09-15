package com.articulate.sigma;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestKb
{
	@Test
	public void testLoad()
	{
		Utils.loadKb(Utils.ALL_FILES);
	}

	@BeforeAll
	public static void init()
	{
		Utils.turnOffLogging();
	}

	public static void main(String[] args)
	{
		init();
		new TestKb().testLoad();
	}
}
