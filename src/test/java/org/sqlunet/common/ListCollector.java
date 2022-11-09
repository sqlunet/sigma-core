/*
 * Copyright (c) 2022.
 * This code is copyright Bernard Bou <1313ou@gmail.com>
 * This software is released under the GNU Public License 3 <http://www.gnu.org/copyleft/gpl.html>.
 */

package org.sqlunet.common;

import org.sigma.core.NotNull;

import java.io.Closeable;
import java.util.ArrayList;

public class ListCollector<T extends SetId> extends ArrayList<T> implements Closeable
{
	private final boolean isOpen = false;

	private int allocator = 0;

	public ListCollector()
	{
		super();
	}

	@NotNull
	public ListCollector<T> open()
	{
		return this;
	}

	@Override
	public boolean add(@NotNull T item)
	{
		item.setId(++allocator);
		return super.add(item);
	}

	@Override
	public void close()
	{
		clear();
	}

	@NotNull
	public String status()
	{
		return ":" + size();
	}
}
