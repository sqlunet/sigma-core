package com.articulate.sigma;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.sqlunet.sumo.Sumo;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL;

public class SumoLoader implements BeforeAllCallback, ExtensionContext.Store.CloseableResource
{
	private static boolean started = false;

	static Sumo sumo;

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

	public void load()
	{
		started = true;

		Utils.turnOffLogging();

		// Your "before all tests" startup logic goes here
		sumo = Utils.loadKb();
		sumo.buildRelationCaches();
		sumo.checkArity();
		assertNotNull(SumoLoader.sumo);
	}
}

// Then, any tests classes where you need this executed at least once, can be annotated with: @ExtendWith({KBLoader.class})
