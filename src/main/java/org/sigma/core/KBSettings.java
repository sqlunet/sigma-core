/*
 * Copyright (c) 2022.
 * This code is copyright Articulate Software (c) 2003.  Some portions copyright Teknowledge (c) 2003
 * and reused under the terms of the GNU license.
 * Significant portions of the code have been revised, trimmed, revamped,enhanced by
 * Bernard Bou <1313ou@gmail.com>
 * This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
 * Users of this code also consent, by use of this code, to credit Articulate Software and Teknowledge
 * in any writings, briefings, publications, presentations, or other representations of any software
 * which incorporates, builds on, or uses this code.
 */

package org.sigma.core;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Settings
 * Replaces KBManager
 */
public class KBSettings
{
	@NotNull
	public static final Properties PREFS = new Properties();

	static
	{
		String path = System.getProperty("config","sigma.properties");
		try (@NotNull InputStream is = Files.newInputStream(Path.of(path)))
		{
			PREFS.load(is);
		}
		catch (IOException | InvalidPathException e)
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
