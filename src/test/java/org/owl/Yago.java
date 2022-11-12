/*
 * Copyright (c) 2022.
 * This code is copyright Bernard Bou <1313ou@gmail.com>
 * This software is released under the GNU Public License 3 <http://www.gnu.org/copyleft/gpl.html>.
 */

package org.owl;

import org.sigma.core.NotNull;
import org.sigma.core.Nullable;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class Yago
{
	private static final String YAGO_SUMO_DIR = "data/WordNetMappings";

	private static final Map<String, String> FILES = makeFileMap();

	/**
	 * Keys are SUMO term name Strings, values are YAGO/DBPedia
	 * term name Strings.
	 */
	@Nullable
	private Map<String, String> SUMOYAGOMap = null;

	private static Map<String, String> makeFileMap()
	{
		Map<String, String> map = new HashMap<>();

		map.put("yago-sumo-mappings", YAGO_SUMO_DIR + "/yago-sumo-mappings.txt");
		return map;
	}

	/**
	 * Write YAGO mapping
	 */
	public void writeMapping(@NotNull PrintStream ps, String term)
	{
		assert SUMOYAGOMap != null;
		String YAGO = SUMOYAGOMap.get(term);
		ps.println("  <owl:sameAs rdf:resource=\"http://dbpedia.org/resource/" + YAGO + "\" />");
		ps.println("  <owl:sameAs rdf:resource=\"http://yago-knowledge.org/resource/" + YAGO + "\" />");
		ps.println("  <rdfs:seeAlso rdf:resource=\"https://en.wikipedia.org/wiki/" + YAGO + "\" />");
	}

	private static File getYagoFile(@SuppressWarnings("SameParameterValue") final String fileKey)
	{
		return new File(FILES.get(fileKey));
	}

	/**
	 * Read a mapping file from YAGO to SUMO terms and store in SUMOYAGOMap
	 */
	@NotNull
	public static Map<String, String> readYAGOSUMOMappings() throws IOException
	{
		@NotNull Map<String, String> result = new HashMap<>();

		@NotNull File f = getYagoFile("yago-sumo-mappings");
		try (@NotNull FileReader r = new FileReader(f); @NotNull LineNumberReader lr = new LineNumberReader(r))
		{
			String YAGO;
			String SUMO;
			String line;
			while ((line = lr.readLine()) != null)
			{
				line = line.trim();
				if (!line.isEmpty() && line.charAt(0) != '#')
				{
					YAGO = line.substring(0, line.indexOf(" "));
					SUMO = line.substring(line.indexOf(" ") + 1);
					result.put(SUMO, YAGO);
				}
			}
		}
		return result;
	}

	public void init() throws IOException
	{
		SUMOYAGOMap = readYAGOSUMOMappings();
	}
}
