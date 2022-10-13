package com.articulate.sigma;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class KBSettings
{
	@NotNull
	public static Properties prefs = new Properties();
	static
	{
		String path = System.getProperty("settings");
		try (InputStream is = Files.newInputStream(Path.of(path)))
		{
			prefs.load(is);
		}
		catch (IOException e)
		{
			// ignore;
		}
	}

	/**
	 * Get the preference corresponding to the given key.
	 *
	 * @param key key
	 * @return value
	 */
	@NotNull
	public static String getPref(@NotNull final String key)
	{
		return prefs.getProperty(key, "");
	}
}
