package com.articulate.sigma;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.articulate.sigma.Utils.OUT;
import static org.junit.jupiter.api.Assertions.assertTrue;

//@ExtendWith({KBLoader.class})
public class TestUnify
{
	//@Disabled
	@Test
	public void testUnify()
	{
		Formula f1 = Formula.of("(Man ?X)");
		Formula f2 = Formula.of("(Man Lincoln)");

		testUnify(f1, f2);
	}

	public void testUnify(@NotNull final Formula f1, @NotNull final Formula f2)
	{
		Map<String, String> u = f1.unify(f2);
		OUT.println("f1 u f2=" + u);

		Map<String, String> u2 = f2.unify(f1);
		OUT.println("f2 u f1=" + u2);
	}

	public static void main(String[] args)
	{
		new SumoProvider().load();
		TestUnify p = new TestUnify();
		p.testUnify();
	}
}
