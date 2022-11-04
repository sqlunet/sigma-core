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

public class Sumo extends KB implements FileGetter, Serializable
{
	private static final long serialVersionUID = 3120000480284537868L;

	protected static PrintStream INFO_OUT = System.err;

	private static final boolean silent = System.getProperties().containsKey("SILENT");

	private String[] filenames;

	public Sumo(final String dirName)
	{
		super("SUMO", dirName);
	}

	public boolean make(final String[] files)
	{
		if (files == null)
		{
			return make(Settings.getFiles(this.kbDir, true));
		}
		filenames = files;
		final String[] filePaths = Arrays.stream(files).map(f -> kbDir + File.separatorChar + f).toArray(String[]::new);
		makeKB(this, filePaths);
		return true;
	}

	private static void makeKB(final KB kb, final String[] filePaths)
	{
		for (final String filePath : filePaths)
		{
			INFO_OUT.println(FileUtil.basename(filePath));
			kb.addConstituent(filePath);
		}
		kb.buildRelationCaches();
	}

	private static void makeKBAndCache(final KB kb, final String[] filePaths)
	{
		for (final String filePath : filePaths)
		{
			INFO_OUT.println("\n" + filePath);
			kb.addConstituentAndBuildCaches(filePath);
		}
	}

	public boolean makeClausalForms()
	{
		long count = 0;
		for (Collection<Formula> fs : formulaIndex.values())
		{
			for (Formula f : fs)
			{
				/* Tuple.Triple<List<Clause>, Formula, Map<String, String>> cf = */
				f.getClausalForms();
				if (!silent)
				{
					if ((count++ % 1000L) == 0)
					{
						INFO_OUT.println();
					}
					INFO_OUT.print('!');
				}
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
