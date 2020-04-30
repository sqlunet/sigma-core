package com.articulate.sigma;

import com.articulate.sigma.KB;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SUMOKb extends KB implements Serializable
{
	private static final long serialVersionUID = 3120000480284537868L;

	private static final String[] CORE_FILES = new String[] { "Merge.kif", "Mid-level-ontology.kif", "english_format.kif" };

	private String[] filenames;

	public SUMOKb(final String dirName)
	{
		super("SUMO", dirName);
	}

	public boolean make(final boolean full)
	{
		this.filenames = SUMOKb.getFiles(this.kbDir, full);
		final String[] filePaths = new String[this.filenames.length];
		for (int i = 0; i < filePaths.length; i++)
		{
			filePaths[i] = this.kbDir + File.separatorChar + this.filenames[i];
		}
		SUMOKb.makeKB(this, filePaths);
		return true;
	}

	private static void makeKB(final KB kb, final String[] filePaths)
	{
		for (final String filePath : filePaths)
		{
			System.out.println("\n" + filePath);
			kb.addConstituent(filePath);
		}
	}

	public boolean makeClausalForms()
	{
		long count = 0;
		for (List<Formula> fs : this.formulas.values())
		{
			for (Formula f : fs)
			{
				/* Tuple.Triplet<List<Clause>, Formula, Map<String, String>> cf = */
				f.getClausalForm();
				if ((count++ % 100L) == 0)
					System.out.println();
				System.out.print('!');
			}
		}
		return true;
	}

	protected static String[] getFiles(final String dirName, final boolean full)
	{
		if (full)
		{
			final List<String> list = new ArrayList<>(Arrays.asList(SUMOKb.CORE_FILES));
			for (final String filename : SUMOKb.getKifs(dirName))
			{
				if (list.contains(filename))
				{
					continue;
				}
				list.add(filename);
			}
			return list.toArray(new String[0]);
		}
		return SUMOKb.CORE_FILES;
	}

	private static String[] getKifs(final String dirName)
	{
		final File file = new File(dirName);
		if (file.exists() && file.isDirectory())
			return file.list((dir, name) -> name.endsWith(".kif"));
		return new String[] {};
	}

	public String[] getFilenames()
	{
		return this.filenames;
	}
}
