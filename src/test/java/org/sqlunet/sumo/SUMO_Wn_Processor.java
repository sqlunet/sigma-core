package org.sqlunet.sumo;

import org.sqlunet.sumo.collector.SetCollector;
import org.sqlunet.sumo.exception.AlreadyFoundException;
import org.sqlunet.sumo.objects.Term;
import org.sqlunet.sumo.joins.Term_Sense;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class SUMO_Wn_Processor
{
	private static final String[] POSES = {"noun", "verb", "adj", "adv"};

	private static final String SUMO_TEMPLATE = "WordNetMappings/WordNetMappings30-%s.txt";

	private final String home;

	public SUMO_Wn_Processor(final String home)
	{
		this.home = home;
	}

	public void run(final PrintStream ps) throws IOException
	{
		for (final String pos : POSES)
		{
			collect(pos);
		}
		try (SetCollector<Term> ignored = Term.COLLECTOR.open())
		{
			for (final Term_Sense map : Term_Sense.SET)
			{
				String row = map.dataRow();
				String comment = map.comment();
				//ps.printf("%s -- %s%n", row, comment);
			}
		}
	}

	public void collect(final String posName) throws IOException
	{
		final String filename = this.home + File.separator + String.format(SUMO_TEMPLATE, posName);

		// pos
		final char pos = posName.charAt(0);

		// iterate on synsets
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(Paths.get(filename)))))
		{
			int lineno = 0;
			String line;
			while ((line = reader.readLine()) != null)
			{
				lineno++;
				line = line.trim();
				if (line.isEmpty() || line.charAt(0) == ' ' || line.charAt(0) == ';' || !line.contains("&%"))
				{
					continue;
				}

				// read
				try
				{
					final String term = Term.parse(line);
					/* final SUMOTerm_Sense mapping = */
					Term_Sense.parse(term, line, pos); // side effect: term mapping collected into set
				}
				catch (IllegalArgumentException iae)
				{
					System.err.println(pos + " " + "line " + lineno + ": ILLEGAL [" + iae.getMessage() + "] : " + line);
				}
				catch (AlreadyFoundException afe)
				{
					System.err.println(pos + " " + "line " + lineno + ": DUPLICATE [" + afe.getMessage() + "] : " + line);
					// System.err.println( "DUPLICATE [" + afe.getMessage() + "] : at line " + lineno + " : " + line);
				}
			}
		}
	}
}
