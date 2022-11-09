/*
 * Copyright (c) 2022.
 * This code is copyright Bernard Bou <1313ou@gmail.com>
 * This software is released under the GNU Public License 3 <http://www.gnu.org/copyleft/gpl.html>.
 */

package org.sigma.core;

import org.sigma.core.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Settings
{
	static final String[] CORE_FILES = new String[]{"Merge.kif", "Mid-level-ontology.kif", "english_format.kif"};

	@NotNull
	static String[] getFiles(@NotNull final String dirName, final boolean full)
	{
		if (full)
		{
			@NotNull final List<String> list = new ArrayList<>(Arrays.asList(CORE_FILES));
			for (final String filename : getKifs(dirName))
			{
				if (list.contains(filename))
				{
					continue;
				}
				list.add(filename);
			}
			return list.toArray(new String[0]);
		}
		return CORE_FILES;
	}

	@Nullable
	private static String[] getKifs(@NotNull final String dirName)
	{
		@NotNull final File file = new File(dirName);
		if (file.exists() && file.isDirectory())
		{
			return file.list((dir, name) -> name.endsWith(".kif"));
		}
		return new String[]{};
	}
}
