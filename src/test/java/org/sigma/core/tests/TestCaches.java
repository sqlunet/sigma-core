/*
 * Copyright (c) 2022.
 * This code is copyright Bernard Bou <1313ou@gmail.com>
 * This software is released under the GNU Public License 3 <http://www.gnu.org/copyleft/gpl.html>.
 */

package org.sigma.core.tests;

import org.sigma.core.KB;
import org.sigma.core.SumoProvider;
import org.sigma.core.Utils;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith({SumoProvider.class})
public class TestCaches
{
	@Test
	public void testCaches()
	{
		SumoProvider.SUMO.getRelationCaches().stream() //
				.filter(c -> c.size() > 0) //
				.sorted(Comparator.comparing(KB.RelationCache::getReln).thenComparing(KB.RelationCache::getKeyArgPos).thenComparing(KB.RelationCache::getValueArgPos)) //
				.forEach(c -> Utils.OUT.println(c + " size=" + c.size()));
		Utils.OUT.println();
	}

	@Test
	public void testCachedNames()
	{
		SumoProvider.SUMO.getCachedRelationNames().stream().sorted().forEach(Utils.OUT::println);
	}

	@Test
	public void testCachedTransitiveNames()
	{
		SumoProvider.SUMO.getCachedTransitiveRelationNames().stream().sorted().forEach(Utils.OUT::println);
	}

	@Test
	public void testCachedSymmetricNames()
	{
		SumoProvider.SUMO.getCachedSymmetricRelationNames().stream().sorted().forEach(Utils.OUT::println);
	}

	@Test
	public void testCachedReflexiveNames()
	{
		SumoProvider.SUMO.getCachedReflexiveRelationNames().stream().sorted().forEach(Utils.OUT::println);
	}

	@Test
	public void testCachesGet()
	{
		final Set<String> selected = Set.of("subrelation", "subclass", "instance", "disjoint", "inverse");
		SumoProvider.SUMO.getRelationCaches().stream() //
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
		new SumoProvider().load();
		init();
		TestCaches d = new TestCaches();
		d.testCaches();
		d.testCachesGet();
		d.testCachedNames();
		d.testCachedReflexiveNames();
		d.testCachedSymmetricNames();
		d.testCachedTransitiveNames();
	}
}
