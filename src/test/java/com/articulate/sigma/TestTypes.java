package com.articulate.sigma;

import com.articulate.sigma.noncore.FormulaPreProcessor;
import com.articulate.sigma.noncore.Types;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

@ExtendWith({SumoProvider.class})
public class TestTypes
{
	//@Disabled
	@Test
	public void testTypes()
	{
		Formula[] fs = { //
				Formula.of("(=> (foo ?A B) (bar B ?A))"),  //
				// Formula.of("(=> (foo ?A B) (bar B ?A)) (domain foo 1 Z)"),  //
				//Formula.of("(=> (instance ?A Z) (=> (foo ?A B) (bar B ?A)))"), //
		};

		for (var f : fs)
		{
			Utils.OUT.println("formula=" + f.toFlatString());
			Formula f2 = Formula.of(Types.addTypeRestrictions(f, SumoProvider.sumo));
			Utils.OUT.println("restricted=" + f2.toFlatString());
			Utils.OUT.println();
		}
	}

	public static void main(String[] args)
	{
		new SumoProvider().load();
		TestTypes p = new TestTypes();
		p.testTypes();
	}
}
