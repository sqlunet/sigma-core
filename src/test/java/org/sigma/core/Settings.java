/*
 * Copyright (c) 2022.
 * This code is copyright Bernard Bou <1313ou@gmail.com>
 * This software is released under the GNU Public License 3 <http://www.gnu.org/copyleft/gpl.html>.
 */

package org.sigma.core;

import org.sigma.core.NotNull;

import java.io.File;
import java.util.*;

public class Settings
{
	static final Set<String> CORE_FILES = Set.of("Mid-level-ontology.kif", "Merge.kif", "english_format.kif");

	@NotNull
	static Set<String> getFiles(@NotNull final String dirName, @SuppressWarnings("SameParameterValue") final boolean full)
	{
		if (full)
		{
			@NotNull final Set<String> list = new LinkedHashSet<>();
			list.addAll(CORE_FILES);
			list.addAll(getKifs(dirName));
			return list;
		}
		return CORE_FILES;
	}

	@NotNull
	private static Collection<String> getKifs(@NotNull final String dirName)
	{
		@NotNull final File file = new File(dirName);
		if (file.exists() && file.isDirectory())
		{
			var files = file.list((dir, name) -> name.endsWith(".kif"));
			if (files != null)
				return List.of(files);
		}
		return Collections.emptyList();
	}
}
