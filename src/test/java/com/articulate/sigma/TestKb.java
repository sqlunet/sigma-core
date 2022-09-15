package com.articulate.sigma;

import org.junit.jupiter.api.Test;

public class TestKb
{
	@Test
	public void testLoad()
	{
		Utils.loadKb(Utils.ALL_FILES);
	}

	public static void main(String[] args)
	{
		new KBLoader().load();
		new TestKb().testLoad();
	}
}
