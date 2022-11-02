/*
 * Copyright (c) 2022.
 * This code is copyright Bernard Bou <1313ou@gmail.com>
 * This software is released under the GNU Public License 3 <http://www.gnu.org/copyleft/gpl.html>.
 */

package org.sigma.core;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.sqlunet.sumo.Sumo;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL;

public class SumoProvider implements BeforeAllCallback, ExtensionContext.Store.CloseableResource
{
	private static boolean started = false;

	public static Sumo SUMO;

	@Override
	public void beforeAll(ExtensionContext context)
	{
		if (!started)
		{
			// The following line registers a callback hook when the root test context is shut down
			context.getRoot().getStore(GLOBAL).put("com.articulate.sigma.SumoLoader", this);

			load();
		}
	}

	@Override
	public void close()
	{
		// Your "after all tests" logic goes here
	}

	public Sumo load()
	{
		started = true;

		Utils.turnOffLogging();

		// Your "before all tests" startup logic goes here
		SUMO = BaseSumoProvider.loadKb();
		SUMO.buildRelationCaches();
		SUMO.checkArity();
		assertNotNull(SumoProvider.SUMO);
		return SUMO;
	}
}