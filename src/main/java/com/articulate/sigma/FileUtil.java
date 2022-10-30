package com.articulate.sigma;

import java.io.File;
import java.io.PrintStream;

public class FileUtil
{
	public static final PrintStream PROGRESS_OUT = System.err;

	@NotNull
	public static String basename(@NotNull final String fileName)
	{
		if (fileName.lastIndexOf(File.separator) > -1)
		{
			return fileName.substring(fileName.lastIndexOf(File.separator) + 1);
		}
		return fileName;
	}
}
