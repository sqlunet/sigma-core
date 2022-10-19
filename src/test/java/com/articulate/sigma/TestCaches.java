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
				.sorted(Comparator.comparing(KB.RelationCache::getRelationName).thenComparing(KB.RelationCache::getKeyArgPos).thenComparing(KB.RelationCache::getValueArgPos)) //
				.forEach(rc -> Utils.OUT.println("(" + rc.getRelationName() + " k@" + rc.getKeyArgPos() + " v@" + rc.getValueArgPos() + ") closure=" + rc.isClosureComputed() + " size=" + rc.size()));
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
				.filter(c -> selected.contains(c.getRelationName())).filter(c -> c.size() > 0) //
				.sorted(Comparator.comparing(KB.RelationCache::getRelationName).thenComparing(KB.RelationCache::getKeyArgPos).thenComparing(KB.RelationCache::getValueArgPos)) //
				.forEach(rc -> {
					Utils.OUT.println("(" + rc.getRelationName() + " k@" + rc.getKeyArgPos() + " v@" + rc.getValueArgPos() + ") closure=" + rc.isClosureComputed());
					rc.keySet().stream().sorted().limit(5).forEach(key -> {
						var vals = rc.get(key);
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
