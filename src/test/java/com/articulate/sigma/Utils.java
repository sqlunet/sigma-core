package com.articulate.sigma;

import org.sqlunet.sumo.Sumo;

import java.io.OutputStream;
import java.io.PrintStream;

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

	public static PrintStream INFO_OUT = System.err;

	public static PrintStream OUT_WARN = System.out;

	public static PrintStream ERR = System.err;

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
}
