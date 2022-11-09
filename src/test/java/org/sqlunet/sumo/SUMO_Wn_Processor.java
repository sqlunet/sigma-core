/*
 * Copyright (c) 2022.
 * This code is copyright Bernard Bou <1313ou@gmail.com>
 * This software is released under the GNU Public License 3 <http://www.gnu.org/copyleft/gpl.html>.
 */

package org.sqlunet.sumo;

import org.sigma.core.NotNull;
import org.sqlunet.common.SetCollector;
import org.sqlunet.common.AlreadyFoundException;
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

	public void run(@NotNull final PrintStream ps, @NotNull final PrintStream pse) throws IOException
	{
		for (@NotNull final String pos : POSES)
		{
			collect(pos, pse);
		}
		try (SetCollector<Term> ignored = Term.COLLECTOR.open())
		{
			for (@NotNull final Term_Sense map : Term_Sense.SET)
			{
				String row = map.dataRow();
				String comment = map.comment();
				ps.printf("%s -- %s%n", row, comment);
			}
		}
	}

	public void collect(@NotNull final String posName, @NotNull final PrintStream pse) throws IOException
	{
		@NotNull final String filename = this.home + File.separator + String.format(SUMO_TEMPLATE, posName);

		// pos
		final char pos = posName.charAt(0);

		// iterate on synsets
		try (@NotNull BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(Paths.get(filename)))))
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
					@NotNull final String term = Term.parse(line);
					/* final SUMOTerm_Sense mapping = */
					Term_Sense.parse(term, line, pos); // side effect: term mapping collected into set
				}
				catch (IllegalArgumentException iae)
				{
					pse.println("line " + lineno + '-' + pos + " " + ": ILLEGAL [" + iae.getMessage() + "] : " + line);
				}
				catch (AlreadyFoundException afe)
				{
					pse.println("line " + lineno + '-' + pos + " " + ": DUPLICATE [" + afe.getMessage() + "] : " + line);
				}
			}
		}
	}
}
