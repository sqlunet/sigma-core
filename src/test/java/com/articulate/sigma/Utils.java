package com.articulate.sigma;

import org.sqlunet.sumo.Kb;

import java.io.OutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Utils
{
	public static PrintStream NULL_OUT = new PrintStream(new OutputStream()
	{
		public void write(int b)
		{
			//DO NOTHING
		}
	});

	public static PrintStream OUT = System.out;

	public static PrintStream OUT_INFO = System.out;

	public static PrintStream OUT_WARN = System.out;

	public static PrintStream OUT_ERR = System.err;

	public static void turnOffLogging()
	{
		String loggingPath = "logging.properties";
		System.setProperty("java.util.logging.config.file", loggingPath);
		System.out.println("java.util.logging.config.class = " + System.getProperty("java.util.logging.config.class"));
		System.out.println("java.util.logging.config.file = " + System.getProperty("java.util.logging.config.file"));
		boolean silent = System.getProperties().containsKey("SILENT");
		if (silent)
		{
			OUT = NULL_OUT;
		}
	}

	public static final String[] ALL_FILES = null;

	public static final String[] CORE_FILES = {"Merge.kif", "Mid-level-ontology.kif"};

	public static final String[] SAMPLE_FILES = {"Merge.kif", "Mid-level-ontology.kif", "Communication.kif"};

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

	public static Kb loadKb(final String[] files)
	{
		String kbPath = Utils.getPath();
		Kb kb = new Kb(kbPath);
		System.out.printf("Kb building%n");
		boolean result = kb.make(files);
		assertTrue(result);
		System.out.printf("%nKb built%n");
		return kb;
	}

	public static void getRelValences(final String[] rels, final Kb kb)
	{
		System.out.println();
		for (String rel : rels)
		{
			System.out.printf("'%s' valence %s%n", rel, kb.getValence(rel));
		}
	}
}
