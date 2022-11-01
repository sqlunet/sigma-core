/*
 * Copyright (c) 2022.
 * This code is copyright Bernard Bou <1313ou@gmail.com>
 * This software is released under the GNU Public License 3 <http://www.gnu.org/copyleft/gpl.html>.
 */

package org.sigma.core;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.sqlunet.sumo.BaseSumo;
import org.sqlunet.sumo.Sumo;

import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL;

public class BaseSumoProvider implements BeforeAllCallback, ExtensionContext.Store.CloseableResource
{
	public static final String[] ALL_FILES = null;
	public static final String[] CORE_FILES = {"Merge.kif", "Mid-level-ontology.kif", "english_format.kif"};
	public static final String[] TINY_FILES = {"tinySUMO.kif"};
	private static boolean started = false;

	public static BaseSumo SUMO;

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

	public static Sumo loadKb()
	{
		return loadKb(getScope());
	}

	public static Sumo loadKb(final String[] files)
	{
		String kbPath = getPath();
		Sumo kb = new Sumo(kbPath);
		Utils.INFO_OUT.printf("KB building%n");
		boolean result = kb.make(files);
		assertTrue(result);
		Utils.INFO_OUT.printf("KB built%n");
		return kb;
	}

	public static BaseSumo loadBaseKb()
	{
		return loadBaseKb(getScope());
	}

	public static BaseSumo loadBaseKb(final String[] files)
	{
		String kbPath = getPath();
		BaseSumo kb = new BaseSumo(kbPath);
		Utils.INFO_OUT.printf("Kb building%n");
		boolean result = kb.make(files);
		assertTrue(result);
		Utils.INFO_OUT.printf("Kb built%n");
		return kb;
	}

	@Override
	public void beforeAll(ExtensionContext context)
	{
		if (!started)
		{
			// The following line registers a callback hook when the root test context is shut down
			context.getRoot().getStore(GLOBAL).put("com.articulate.sigma.BaseSumoLoader", this);

			load();
		}
	}

	@Override
	public void close()
	{
		// Your "after all tests" logic goes here
	}

	public void load()
	{
		started = true;

		Utils.turnOffLogging();

		// Your "before all tests" startup logic goes here
		SUMO = loadBaseKb();
		assertNotNull(BaseSumoProvider.SUMO);
	}
}
