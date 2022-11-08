/*
 * Copyright (c) 2022.
 * This code is copyright Bernard Bou <1313ou@gmail.com>
 * This software is released under the GNU Public License 3 <http://www.gnu.org/copyleft/gpl.html>.
 */

package org.sigma.core;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL;

public class BaseSumoProvider implements BeforeAllCallback, ExtensionContext.Store.CloseableResource
{
	private static boolean started = false;

	public static BaseSumo SUMO;

	public static Sumo loadKb()
	{
		return loadKb(Helpers.getScope());
	}

	public static Sumo loadKb(final String[] files)
	{
		String kbPath = Helpers.getPath();
		Sumo kb = new Sumo(kbPath);
		Helpers.INFO_OUT.printf("KB building%n");
		boolean result = kb.make(files);
		assertTrue(result);
		Helpers.INFO_OUT.printf("KB built%n");
		return kb;
	}

	public static BaseSumo loadBaseKb()
	{
		return loadBaseKb(Helpers.getScope());
	}

	public static BaseSumo loadBaseKb(final String[] files)
	{
		String kbPath = Helpers.getPath();
		BaseSumo kb = new BaseSumo(kbPath);
		Helpers.INFO_OUT.printf("Kb building%n");
		boolean result = kb.make(files);
		assertTrue(result);
		Helpers.INFO_OUT.printf("Kb built%n");
		return kb;
	}

	@Override
	public void beforeAll(ExtensionContext context)
	{
		if (!started)
		{
			//System.err.println("BASE PROVIDER");

			// register a callback hook when the root test context is shut down
			context.getRoot().getStore(GLOBAL).put("org.sigma.BaseSumoLoader", this);

			load();
		}
	}

	@Override
	public void close()
	{
		// Your "after all tests" logic goes here
	}

	public BaseSumo load()
	{
		started = true;

		Helpers.turnOffLogging();

		SUMO = loadBaseKb();
		assertNotNull(BaseSumoProvider.SUMO);
		return SUMO;
	}
}
