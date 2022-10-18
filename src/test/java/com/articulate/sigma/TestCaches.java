package com.articulate.sigma;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sqlunet.sumo.Dump;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith({KBLoader.class})
public class TestCaches
{
	@Test
	public void testCaches()
	{
		KBLoader.kb.getRelationCaches().stream() //
				.filter(c -> c.size() > 0) //
				.sorted(Comparator.comparing(KB.RelationCache::getRelationName).thenComparing(KB.RelationCache::getKeyArgument).thenComparing(KB.RelationCache::getValueArgument)) //
				.forEach(rc -> Utils.OUT.println("(" + rc.getRelationName() + " k@" + rc.getKeyArgument() + " v@" + rc.getValueArgument() + ") closure=" + rc.isClosureComputed() + " size=" + rc.size()));
		Utils.OUT.println();
	}

	@Test
	public void testCachedNames()
	{
		KBLoader.kb.getCachedRelationNames().stream().sorted().forEach(Utils.OUT::println);
	}

	@Test
	public void testCachedTransitiveNames()
	{
		KBLoader.kb.getCachedTransitiveRelationNames().stream().sorted().forEach(Utils.OUT::println);
	}

	@Test
	public void testCachedSymmetricNames()
	{
		KBLoader.kb.getCachedSymmetricRelationNames().stream().sorted().forEach(Utils.OUT::println);
	}

	@Test
	public void testCachedReflexiveNames()
	{
		KBLoader.kb.getCachedReflexiveRelationNames().stream().sorted().forEach(Utils.OUT::println);
	}

	@Test
	public void testCachesGet()
	{
		final Set<String> selected = Set.of("subrelation", "subclass", "instance", "disjoint", "inverse");
		KBLoader.kb.getRelationCaches().stream() //
				.filter(c -> selected.contains(c.getRelationName())).filter(c -> c.size() > 0) //
				.sorted(Comparator.comparing(KB.RelationCache::getRelationName).thenComparing(KB.RelationCache::getKeyArgument).thenComparing(KB.RelationCache::getValueArgument)) //
				.forEach(rc -> {
					Utils.OUT.println("(" + rc.getRelationName() + " k@" + rc.getKeyArgument() + " v@" + rc.getValueArgument() + ") closure=" + rc.isClosureComputed());
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
		new KBLoader().load();
		init();
		TestCaches d = new TestCaches();
	}
}
