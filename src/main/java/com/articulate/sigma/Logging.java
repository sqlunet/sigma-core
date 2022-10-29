package com.articulate.sigma;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.LogManager;

public class Logging
{
	public static void setLogging()
	{
		try (InputStream is = Logging.class.getClassLoader().getResourceAsStream("logging.properties"))
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
