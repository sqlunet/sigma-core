package org.sqlunet.sumo;

import com.articulate.sigma.*;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.sqlunet.sumo.BaseSumo.INFO_OUT;

public class Sumo extends KB implements FileGetter, Serializable
{
	private static final long serialVersionUID = 3120000480284537868L;

	private static final String[] CORE_FILES = new String[]{"Merge.kif", "Mid-level-ontology.kif", "english_format.kif"};

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
			return make(getFiles(this.kbDir, true));
		}
		this.filenames = files;
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

	@NotNull
	protected static String[] getFiles(final String dirName, final boolean full)
	{
		if (full)
		{
			final List<String> list = new ArrayList<>(Arrays.asList(CORE_FILES));
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

	private static String[] getKifs(final String dirName)
	{
		final File file = new File(dirName);
		if (file.exists() && file.isDirectory())
		{
			return file.list((dir, name) -> name.endsWith(".kif"));
		}
		return new String[]{};
	}

	@Override
	public String[] getFilenames()
	{
		return filenames;
	}
}
