package com.articulate.sigma;

import org.sqlunet.sumo.BaseSumo;
import org.sqlunet.sumo.Sumo;

import java.io.OutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Utils
{
	public static final PrintStream NULL_OUT = new PrintStream(new OutputStream()
	{
		public void write(int b)
		{
			//DO NOTHING
		}
	});

	public static PrintStream OUT = System.out;

	public static final PrintStream OUT_INFO = System.out;

	public static PrintStream OUT_WARN = System.out;

	public static PrintStream OUT_ERR = System.err;

	public static void turnOffLogging()
	{
		final String pathKey = "java.util.logging.config.file";
		final String pathValue = "logging.properties";
		System.setProperty(pathKey, pathValue);

		final String classKey = "java.util.logging.config.class";
		final String classValue = System.getProperty(classKey);
		if (classValue != null && !classValue.isEmpty())
		{
			System.err.println(classKey + " = " + classValue);
		}

		boolean silent = System.getProperties().containsKey("SILENT");
		if (silent)
		{
			OUT = NULL_OUT;
			OUT_WARN = NULL_OUT;
		}
	}

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

	public static final String[] ALL_FILES = null;

	public static final String[] CORE_FILES = {"Merge.kif", "Mid-level-ontology.kif", "english_format.kif"};

	public static final String[] TINY_FILES = {"tinySUMO.kif"};
	public static final String[] SAMPLE_FILES = {"Merge.kif", "Mid-level-ontology.kif", "english_format.kif", "Communications.kif"};

	public static String[] getScope()
	{
		String scope = System.getProperties().getProperty("SCOPE", "all");
		switch (scope)
		{
			case "all":
				return ALL_FILES;
			case "core":
				return CORE_FILES;
			case "tiny":
				return TINY_FILES;
			case "samples":
				return SAMPLE_FILES;
			default:
				return scope.split("\\s");
		}
	}

	public static Sumo loadKb()
	{
		return loadKb(getScope());
	}

	public static Sumo loadKb(final String[] files)
	{
		String kbPath = Utils.getPath();
		Sumo kb = new Sumo(kbPath);
		System.out.printf("Kb building%n");
		boolean result = kb.make(files);
		assertTrue(result);
		System.out.printf("%nKb built%n");
		return kb;
	}

	public static BaseSumo loadBaseKb()
	{
		return loadBaseKb(getScope());
	}

	public static BaseSumo loadBaseKb(final String[] files)
	{
		String kbPath = Utils.getPath();
		BaseSumo kb = new BaseSumo(kbPath);
		System.out.printf("Kb building%n");
		boolean result = kb.make(files);
		assertTrue(result);
		System.out.printf("%nKb built%n");
		return kb;
	}

	public static void getRelValences(final String[] relns, final Sumo sumo)
	{
		System.out.println();
		for (String reln : relns)
		{
			var valence= sumo.getValence(reln);
			System.out.printf("'%s' valence %s%n", reln, valence);
		}
	}
}
