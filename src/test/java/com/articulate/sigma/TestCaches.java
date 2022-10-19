package com.articulate.sigma;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith({SumoLoader.class})
public class TestCaches
{
	@Test
	public void testCaches()
	{
		SumoLoader.sumo.getRelationCaches().stream() //
				.filter(c -> c.size() > 0) //
				.sorted(Comparator.comparing(KB.RelationCache::getReln).thenComparing(KB.RelationCache::getKeyArgPos).thenComparing(KB.RelationCache::getValueArgPos)) //
				.forEach(c -> Utils.OUT.println(c + " size=" + c.size()));
		Utils.OUT.println();
	}

	@Test
	public void testCachedNames()
	{
		SumoLoader.sumo.getCachedRelationNames().stream().sorted().forEach(Utils.OUT::println);
	}

	@Test
	public void testCachedTransitiveNames()
	{
		SumoLoader.sumo.getCachedTransitiveRelationNames().stream().sorted().forEach(Utils.OUT::println);
	}

	@Test
	public void testCachedSymmetricNames()
	{
		SumoLoader.sumo.getCachedSymmetricRelationNames().stream().sorted().forEach(Utils.OUT::println);
	}

	@Test
	public void testCachedReflexiveNames()
	{
		SumoLoader.sumo.getCachedReflexiveRelationNames().stream().sorted().forEach(Utils.OUT::println);
	}

	@Test
	public void testCachesGet()
	{
		final Set<String> selected = Set.of("subrelation", "subclass", "instance", "disjoint", "inverse");
		SumoLoader.sumo.getRelationCaches().stream() //
				.filter(c -> selected.contains(c.getReln())).filter(c -> c.size() > 0) //
				.sorted(Comparator.comparing(KB.RelationCache::getReln).thenComparing(KB.RelationCache::getKeyArgPos).thenComparing(KB.RelationCache::getValueArgPos)) //
				.forEach(c -> {
					Utils.OUT.println(c);
					c.keySet().stream().sorted().limit(5).forEach(key -> {
						var vals = c.get(key);
						Utils.OUT.println("\t" + key + " -> " + vals);
					});
					Utils.OUT.println("\t...");
				});
		Utils.OUT.println("...");
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
		new SumoLoader().load();
		init();
		TestCaches d = new TestCaches();
	}
}
