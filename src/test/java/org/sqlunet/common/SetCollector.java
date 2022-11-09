/*
 * Copyright (c) 2022.
 * This code is copyright Bernard Bou <1313ou@gmail.com>
 * This software is released under the GNU Public License 3 <http://www.gnu.org/copyleft/gpl.html>.
 */

package org.sqlunet.common;

import org.sigma.core.NotNull;

import java.io.Closeable;
import java.util.Comparator;
import java.util.HashMap;
import java.util.TreeMap;

public class SetCollector<T> extends TreeMap<T, Integer> implements Closeable
{
	private boolean isOpen = false;

	public SetCollector(Comparator<T> comparator)
	{
		super(comparator);
	}

	@NotNull
	public SetCollector<T> open()
	{
		int i = 1;
		for (T k : keySet())
		{
			put(k, i++);
		}
		isOpen = true;
		//System.err.println("[OPEN]:" + size());
		return this;
	}

	/**
	 * Add item key to map
	 *
	 * @param item item key
	 * @return false if already there
	 */
	public boolean add(T item)
	{
		// avoid changing value to null
		// putIfAbsent(item, null) uses get and throw not-open exception
		if (containsKey(item))
		{
			return false;
		}
		return put(item, null) == null; // null if there was no mapping
	}

	@Override
	public Integer get(final Object key)
	{
		if (!isOpen)
		{
			throw new IllegalStateException(this + " not open");
		}
		return super.get(key);
	}

	@Override
	public void close()
	{
		isOpen = false;
		clear();
		// System.err.println("[CLOSE]:" + size());
	}

	@NotNull
	public String status()
	{
		return "#" + size();
	}

	@NotNull
	public HashMap<T, Integer> toHashMap()
	{
		return new HashMap<>(this);
	}
}
