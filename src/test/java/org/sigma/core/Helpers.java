/*
 * Copyright (c) 2022.
 * This code is copyright Bernard Bou <1313ou@gmail.com>
 * This software is released under the GNU Public License 3 <http://www.gnu.org/copyleft/gpl.html>.
 */

package org.sigma.core;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class Helpers
{
	public static final PrintStream NULL_OUT = new PrintStream(new OutputStream()
	{
		public void write(int b)
		{
			//DO NOTHING
		}
	});
	@Nullable
	public static final String[] ALL_FILES = null;
	public static final String[] CORE_FILES = {"Merge.kif", "Mid-level-ontology.kif", "english_format.kif"};
	public static final String[] TINY_FILES = {"tinySUMO.kif"};

	public static PrintStream OUT = System.out;

	public static PrintStream INFO_OUT = System.err;

	public static PrintStream OUT_WARN = System.out;

	public static PrintStream OUT_ERR = System.err;

	public static void turnOffLogging()
	{
		Logging.setLogging();

		/*
		final String pathKey = "java.util.logging.config.file";
		final String pathValue = "logging.properties";
		//pathValue = Utils.class.getClassLoader().getResource("logging.properties").getFile();
		System.setProperty(pathKey, pathValue);

		final String classKey = "java.util.logging.config.class";
		final String classValue = System.getProperty(classKey);
		if (classValue != null && !classValue.isEmpty())
		{
			System.err.println(classKey + " = " + classValue);
		}
		*/

		boolean silent = System.getProperties().containsKey("SILENT");
		if (silent)
		{
			OUT = NULL_OUT;
			OUT_WARN = NULL_OUT;
			INFO_OUT = NULL_OUT;
		}
	}

	@NotNull
	public static String getPath()
	{
		String kbPath = System.getProperty("sumopath");
		if (kbPath == null)
		{
			kbPath = System.getenv("SUMOHOME");
		}
		assertNotNull(kbPath, "Pass KB location as -Dsumopath=<somewhere> or SUMOHOME=<somewhere> in env");
		return kbPath;
	}

	@Nullable
	public static String[] getScope()
	{
		String scope = System.getProperties().getProperty("scope", "all");
		switch (scope)
		{
			case "all":
				return ALL_FILES;
			case "core":
				return CORE_FILES;
			case "tiny":
				return TINY_FILES;
			default:
				return Stream.concat(Arrays.stream(CORE_FILES), Arrays.stream(scope.split("\\s"))).toArray(String[]::new);
		}
	}
}
