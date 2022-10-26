package com.articulate.sigma;

import com.articulate.sigma.noncore.FormulaPreProcessor;
import com.articulate.sigma.noncore.Types;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

@ExtendWith({SumoProvider.class})
public class TestTypes
{
	@Test
	public void testTypes()
	{
		Formula[] fs = { //
				Formula.of("(=> (foo ?A B) (bar B ?A))"),  //
				Formula.of("(=> (instance Z ?A) (=> (subclass ?A ?B) (instance Z ?B)))"), //
				Formula.of("(=> (wife ?A B) (husband B ?A))"),  //
				Formula.of("(=> (wife ?A ?B) (husband ?B ?A))"),  //
		};

		for (var f : fs)
		{
			Utils.OUT.println("formula=" + f.toFlatString());
			Formula f2 = Formula.of(Types.addTypeRestrictions(f, SumoProvider.sumo));
			Utils.OUT.println("restricted=" + f2.toFlatString());
			Utils.OUT.println();
		}
	}

	@Test
	public void testFindTypes()
	{
		String[] relns = { //
				"brother", //
				"sister", //
				"wife", //
				"instance", //
				"superclass", //
				"subclass", //
				"component", //
				"MeasureFn", //
				"ListFn", //
				"PropertyFn", //
				"KappaFn", //
				"material", //
				"ingredient", //
				"capability", //
				"precondition", //
				"version", //
		};

		for (var reln : relns)
		{
			String t1 = SumoProvider.sumo.getArgType(reln, 1);
			String t2 = SumoProvider.sumo.getArgType(reln, 2);
			String tc1 = SumoProvider.sumo.getArgTypeClass(reln, 1);
			String tc2 = SumoProvider.sumo.getArgTypeClass(reln, 2);

			Utils.OUT.println("reln=" + reln + " domain1=" + t1 + " domain2=" + t2);
			Utils.OUT.println("reln=" + reln + " domainclass1=" + tc1 + " domainclass2=" + tc2);
			Utils.OUT.println();
		}
	}

	public static void main(String[] args)
	{
		new SumoProvider().load();
		TestTypes p = new TestTypes();
		p.testFindTypes();
		p.testTypes();
	}
}
