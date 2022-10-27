package com.articulate.sigma;

import com.articulate.sigma.noncore.FormulaPreProcessor;
import com.articulate.sigma.noncore.Types;
import com.articulate.sigma.noncore.Types2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.*;

@ExtendWith({SumoProvider.class})
public class TestTypes
{
	@Test
	public void testAddTypeRestrictions()
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
	public void testComputeTypeRestrictions()
	{
		Formula[] fs = { //
				Formula.of("(forall (?A ?B) (subclass ?A MyClass))"), //
				Formula.of("(forall (?A) (=> (wife ?A B) (husband B ?A)))"), //
				Formula.of("(and (wife ?A Betty) (sister ?A Alice))"), //
		};

		for (var f : fs)
		{
			String var = "?A";
			Utils.OUT.println("formula=" + f.toFlatString());
			@NotNull List<String> classes = new ArrayList<>();
			@NotNull List<String> superclasses = new ArrayList<>();
			Types2.computeTypeRestrictions(f, classes, superclasses, var, SumoProvider.sumo);
			Utils.OUT.println(var + " must be instance of " + classes);
			Utils.OUT.println(var + " must be subclass of " + superclasses);
			Utils.OUT.println();
		}
	}

	@Test
	public void testComputeVariableTypes()
	{
		Formula[] fs = { //
				Formula.of("(forall (?A) (=> (wife ?A B) (husband B ?A)))"),};

		for (var f : fs)
		{
			Utils.OUT.println("formula=" + f.toFlatString());
			Map<String, List<List<String>>> map = new HashMap<>();
			Types2.computeVariableTypes(f, map, SumoProvider.sumo);
			Utils.OUT.println(map);
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
		p.testAddTypeRestrictions();
	}
}
