/*
 * Copyright (c) 2022.
 * This code is copyright Articulate Software (c) 2003.  Some portions copyright Teknowledge (c) 2003
 * and reused under the terms of the GNU license.
 * Significant portions of the code have been revised, trimmed, revamped,enhanced by
 * Bernard Bou <1313ou@gmail.com>
 * This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
 * Users of this code also consent, by use of this code, to credit Articulate Software and Teknowledge
 * in any writings, briefings, publications, presentations, or other representations of any software
 * which incorporates, builds on, or uses this code.
 */

package org.sigma.core;

import java.util.*;
import java.util.function.Function;

public class Queue
{
	/**
	 * Process
	 *
	 * @param k0         initial key
	 * @param func       function to get value from key
	 * @param subKeyFunc function to get subkeys from key
	 * @param <K>        key
	 * @param <V>        value type
	 * @return collection of V
	 */
	@NotNull
	static public <K, V> Collection<V> run(@NotNull K k0, @NotNull final Function<K, Collection<V>> func, @NotNull final Function<K, Collection<K>> subKeyFunc)
	{
		// collects results
		@NotNull Collection<V> result = new HashSet<>();

		// history
		@NotNull Set<K> visited = new HashSet<>();

		// initial queue feed
		@NotNull Set<K> queue = new HashSet<>();
		queue.add(k0);

		// process queue until empty
		while (!queue.isEmpty())
		{
			// collects subKeys to r
			@NotNull Collection<K> subKeys = new HashSet<>();

			// process queue
			for (@NotNull K k : queue)
			{
				// collect this iteration's results
				@NotNull Collection<V> values = func.apply(k);
				result.addAll(values);

				// compute subKeys to key k
				subKeys = subKeyFunc.apply(k);
			}
			// mark keys in the queue as visited
			visited.addAll(queue);

			// clear queue
			queue.clear();

			// feed queue with subkeys, recurse
			queue.addAll(subKeys);
			queue.removeAll(visited);
		}
		return result;
	}
}
