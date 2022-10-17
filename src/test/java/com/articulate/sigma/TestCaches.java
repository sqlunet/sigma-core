package com.articulate.sigma;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sqlunet.sumo.Dump;

import java.util.Collection;

@ExtendWith({KBLoader.class})
public class TestCaches
{
	@Test
	public void testCaches()
	{
		int i = 0;
		for (KB.RelationCache rc : KBLoader.kb.getRelationCaches())
		{
			Utils.OUT.println(rc.getRelationName() + " keyarg=" + rc.getKeyArgument() + " valarg=" + rc.getValueArgument() + " closure=" + rc.isClosureComputed());
			for (String key : rc.keySet())
			{
				Utils.OUT.println("\t" + key);
				if (i < 3)
				{
					for (String val : rc.get(key))
					{
						Utils.OUT.println("\t\t" + val);
					}
				}
			}
			Utils.OUT.println();
			i++;
		}
		Utils.OUT.println();
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
		new KBLoader().load();
		init();
		TestCaches d = new TestCaches();
	}
}
