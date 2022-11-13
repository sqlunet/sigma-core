/*
 * Copyright (c) 2022.
 * This code is copyright Bernard Bou <1313ou@gmail.com>
 * This software is released under the GNU Public License 3 <http://www.gnu.org/copyleft/gpl.html>.
 */

package org.sigma.core;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.IOException;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL;

public class BaseSumoProvider implements BeforeAllCallback, ExtensionContext.Store.CloseableResource
{
	private static boolean started = false;

	public static BaseSumo SUMO;

	@NotNull
	public static Sumo loadKb() throws IOException
	{
		return loadKb(Helpers.getScope());
	}

	@NotNull
	public static Sumo loadKb(final Collection<String> files)
	{
		@NotNull String kbPath = Helpers.getPath();
		@NotNull Sumo kb = new Sumo(kbPath);
		Helpers.INFO_OUT.printf("KB building%n");
		boolean result = kb.make(files);
		assertTrue(result);
		Helpers.INFO_OUT.printf("KB built%n");
		return kb;
	}

	@NotNull
	public static BaseSumo loadBaseKb() throws IOException
	{
		return loadBaseKb(Helpers.getScope());
	}

	@NotNull
	public static BaseSumo loadBaseKb(final Collection<String> files)
	{
		@NotNull String kbPath = Helpers.getPath();
		@NotNull BaseSumo kb = new BaseSumo(kbPath);
		Helpers.INFO_OUT.printf("Kb building%n");
		boolean result = kb.make(files);
		assertTrue(result);
		Helpers.INFO_OUT.printf("Kb built%n");
		return kb;
	}

	@Override
	public void beforeAll(@NotNull ExtensionContext context) throws IOException
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

	@NotNull
	public BaseSumo load() throws IOException
	{
		started = true;

		Helpers.turnOffLogging();

		SUMO = loadBaseKb();
		assertNotNull(BaseSumoProvider.SUMO);
		return SUMO;
	}
}
