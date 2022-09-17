package com.articulate.sigma;

import java.io.File;

public class FileUtil
{
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
