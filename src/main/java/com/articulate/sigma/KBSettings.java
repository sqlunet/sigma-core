package com.articulate.sigma;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class KBSettings
{
	@NotNull
	public static final Properties PREFS = new Properties();

	static
	{
		String path = System.getProperty("sumosettings");

		try (InputStream is = Files.newInputStream(Path.of(path)))
		{
			PREFS.load(is);
		}
		catch (Exception e)
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
		return PREFS.getProperty(key, "");
	}

	/**
	 * Get the preference corresponding to the given key.
	 *
	 * @param key          key
	 * @param defaultValue default value
	 * @return value
	 */
	@NotNull
	public static String getPref(@NotNull final String key, @NotNull final String defaultValue)
	{
		return PREFS.getProperty(key, defaultValue);
	}
}
