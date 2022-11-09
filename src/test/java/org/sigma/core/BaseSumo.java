/*
 * Copyright (c) 2022.
 * This code is copyright Bernard Bou <1313ou@gmail.com>
 * This software is released under the GNU Public License 3 <http://www.gnu.org/copyleft/gpl.html>.
 */

package org.sigma.core;

import java.io.File;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;

public class BaseSumo extends BaseKB implements FileGetter, Serializable
{
	private static final long serialVersionUID = 3120000480284537868L;

	private static final boolean SILENT = System.getProperties().containsKey("SILENT");

	private static final PrintStream PROGRESS_OUT = SILENT ? Helpers.NULL_OUT : System.err;

	private String[] filenames;

	public BaseSumo(final String dirName)
	{
		super("SUMO", dirName);
	}

	public boolean make(@Nullable final String[] files)
	{
		if (files == null)
		{
			return make(Settings.getFiles(this.kbDir, true));
		}
		filenames = files;
		@NotNull final String[] filePaths = Arrays.stream(files).map(f -> kbDir + File.separatorChar + f).toArray(String[]::new);
		makeKB(this, filePaths);
		return true;
	}

	private static void makeKB(@NotNull final BaseKB kb, @NotNull final String[] filePaths)
	{
		for (@NotNull final String filePath : filePaths)
		{
			PROGRESS_OUT.println(FileUtil.basename(filePath));
			kb.addConstituent(filePath);
		}
	}

	public boolean makeClausalForms()
	{
		long count = 0;
		for (@NotNull Collection<Formula> fs : formulaIndex.values())
		{
			for (@NotNull Formula f : fs)
			{
				/* Tuple.Triple<List<Clause>, Formula, Map<String, String>> cf = */
				f.getClausalForms();
				if ((count++ % 1000L) == 0)
				{
					PROGRESS_OUT.println();
				}
				PROGRESS_OUT.print('!');
			}
		}
		return true;
	}

	@Override
	public String[] getFilenames()
	{
		return filenames;
	}
}
