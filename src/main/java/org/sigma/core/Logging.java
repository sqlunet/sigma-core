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
import java.util.logging.LogManager;

/**
 * Log settings
 */
public class Logging
{
	public static void setLogging()
	{
		try (@Nullable InputStream is = Logging.class.getClassLoader().getResourceAsStream("logging.properties"))
		{
			//Properties props = new Properties();
			//props.load(is);
			//System.out.println(props.getProperty("java.util.logging.SimpleFormatter.format"));

			LogManager.getLogManager().readConfiguration(is);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
}
