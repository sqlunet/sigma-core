package com.articulate.sigma;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL;

public class KBLoader implements BeforeAllCallback, ExtensionContext.Store.CloseableResource
{
	private static boolean started = false;

	@Override
	public void beforeAll(ExtensionContext context)
	{
		if (!started)
		{
			started = true;

			// The following line registers a callback hook when the root test context is shut down
			context.getRoot().getStore(GLOBAL).put("com.articulate.sigma.KBloader", this);

			// Your "before all tests" startup logic goes here
			Utils.loadKb(Utils.SAMPLE_FILES);
		}
	}

	@Override
	public void close()
	{
		// Your "after all tests" logic goes here
	}
}

// Then, any tests classes where you need this executed at least once, can be annotated with: @ExtendWith({KBLoader.class})
