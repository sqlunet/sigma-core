package com.articulate.sigma;

import com.articulate.sigma.noncore.Types;
import com.articulate.sigma.noncore.Types2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.*;

@ExtendWith({SumoProvider.class})
public class TestTypes
{
	private static final String[] RELN = { //
			"instance", //
			"subclass", //
			"subset", //
			"domain", //
			"disjoint", //

			"brother", //
			"sister", //
			"wife", //
			"MeasureFn", //
			"ListFn", //
			"PropertyFn", //
			"KappaFn", //
			"component", //
			"material", //
			"ingredient", //
			"capability", //
			"precondition", //
			"version", //
	};

	private static final Formula[] UFORMULAS = { //

			Formula.of("(wife ?A Charles)"), //
			Formula.of("(brother ?A Betty)"), //
			Formula.of("(and (wife ?A Charles) (brother ?B ?A))"), //
			Formula.of("(wife ?A ?B)"), //
			Formula.of("(instance ?A ?B)"), //

			Formula.of("(=> (foo ?A ?B) (bar ?B ?A))"),  //
			Formula.of("(=> (wife ?A ?B) (husband ?B ?A))"), //
			Formula.of("(=> (husband Bob ?A) (foobar ?A))"),  //
			Formula.of("(forall (?A ?B) (=> (wife ?A ?B) (husband ?B ?A)))"), //
			Formula.of("(=> (instance ?A ?B) (=> (subclass ?B ?C) (instance ?A ?C)))"), //

			Formula.of("(forall (?A ?B) (subclass ?A MyClass))"), //
			Formula.of("(forall (?A ?B) (material ?A ?B))"), //
			Formula.of("(forall (?A ?B) (ingredient ?A ?B))"), //
			Formula.of("(forall (?A ?B) (capability ?A ?B))"), //
			Formula.of("(forall (?A ?B) (precondition ?A ?B))"), //
			Formula.of("(forall (?A ?B) (version ?A ?B))"), //
	};

	private static final Formula[] EFORMULAS = { //

			Formula.of("(exist (?A ?B) (subclass ?A MyClass))"), //
			Formula.of("(exist (?A ?B) (material ?A ?B))"), //
			Formula.of("(exist (?A ?B) (ingredient ?A ?B))"), //
			Formula.of("(exist (?A ?B) (capability ?A ?B))"), //
			Formula.of("(exist (?A ?B) (precondition ?A ?B))"), //
			Formula.of("(exist (?A ?B) (version ?A ?B))"), //
	};

	@Test
	public void testAddTypeRestrictionsU()
	{
		for (var f : UFORMULAS)
		{
			Utils.OUT.println("formula=" + f.toFlatString());
			Formula f2 = Formula.of(Types.addTypeRestrictions(f, SumoProvider.sumo));
			Utils.OUT.println("restricted=" + f2.toFlatString());
			Utils.OUT.println("restricted=" + f2.toPrettyString());
			Utils.OUT.println();
		}
	}

	@Test
	public void testAddTypeRestrictionsE()
	{
		for (var f : EFORMULAS)
		{
			Utils.OUT.println("formula=" + f.toFlatString());
			Formula f2 = Formula.of(Types.addTypeRestrictions(f, SumoProvider.sumo));
			Utils.OUT.println("restricted=" + f2.toFlatString());
			Utils.OUT.println("restricted=" + f2.toPrettyString());
			Utils.OUT.println();
		}
	}

	@Test
	public void testComputeTypeRestrictionsU()
	{
		for (var f : UFORMULAS)
		{
			String var = "?A";
			String var2 = "?B";
			Utils.OUT.println("formula=" + f.toFlatString());

			@NotNull List<String> classes = new ArrayList<>();
			@NotNull List<String> superclasses = new ArrayList<>();
			Types2.computeTypeRestrictions(f, var, classes, superclasses, SumoProvider.sumo);
			if (!classes.isEmpty())
			{
				Utils.OUT.println(var + " must be instance of " + classes);
			}
			if (!superclasses.isEmpty())
			{
				Utils.OUT.println(var + " must be subclass of " + superclasses);
			}

			@NotNull List<String> classes2 = new ArrayList<>();
			@NotNull List<String> superclasses2 = new ArrayList<>();
			Types2.computeTypeRestrictions(f, var2, classes2, superclasses2, SumoProvider.sumo);
			if (!classes2.isEmpty())
			{
				Utils.OUT.println(var2 + " must be instance of " + classes2);
			}
			if (!superclasses2.isEmpty())
			{
				Utils.OUT.println(var2 + " must be subclass of " + superclasses2);
			}
			Utils.OUT.println();
		}
	}

	@Test
	public void testComputeTypeRestrictionsE()
	{
		for (var f : EFORMULAS)
		{
			String var = "?A";
			String var2 = "?B";
			Utils.OUT.println("formula=" + f.toFlatString());

			@NotNull List<String> classes = new ArrayList<>();
			@NotNull List<String> superclasses = new ArrayList<>();
			Types2.computeTypeRestrictions(f, var, classes, superclasses, SumoProvider.sumo);
			if (!classes.isEmpty())
			{
				Utils.OUT.println(var + " must be instance of " + classes);
			}
			if (!superclasses.isEmpty())
			{
				Utils.OUT.println(var + " must be subclass of " + superclasses);
			}

			@NotNull List<String> classes2 = new ArrayList<>();
			@NotNull List<String> superclasses2 = new ArrayList<>();
			Types2.computeTypeRestrictions(f, var2, classes2, superclasses2, SumoProvider.sumo);
			if (!classes2.isEmpty())
			{
				Utils.OUT.println(var2 + " must be instance of " + classes2);
			}
			if (!superclasses2.isEmpty())
			{
				Utils.OUT.println(var2 + " must be subclass of " + superclasses2);
			}
			Utils.OUT.println();
		}
	}

	@Test
	public void testComputeVariableTypes()
	{
		Formula[] fs = { //
				Formula.of("(=> (wife ?A B) (husband B ?A))"), //
				Formula.of("(forall (?A) (=> (wife ?A B) (foobar B ?A)))"), //
				Formula.of("(forall (?A) (=> (wife ?A B) (husband B ?A)))"), //
				Formula.of("(forall (?A ?B) (=> (wife ?A ?B) (husband ?B ?A)))"), //
				Formula.of("(forall (?A ?B) (=> (husband ?A ?B) (wife ?B ?A)))"), //
				Formula.of("(forall (?A ?B ?C) (=> (and (sister ?A ?B) (husband ?C ?B)) (inlaw ?A ?C)"), //
				Formula.of("(forall (?A ?B) (=> (and (ingredient ?A ?B) (material ?B ?A)) foobar)"), //
		};

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
	public void testFindArgTypes()
	{
		for (var reln : RELN)
		{
			var t = new StringBuilder("(" + reln);
			String ta;
			for (int i = 1; (ta = SumoProvider.sumo.getArgType(reln, i)) != null; i++)
			{
				t.append(", ").append(ta);
			}
			var tc = new StringBuilder("(" + reln);
			String tac3;
			for (int i = 1; (tac3 = SumoProvider.sumo.getArgTypeClass(reln, i)) != null; i++)
			{
				tc.append(", ").append(tac3);
			}
			t.append(')');
			tc.append(')');
			Utils.OUT.println("domain      " + t);
			if (!t.toString().equals(tc.toString()))
			{
				Utils.OUT.println("domainClass " + tc);
			}
			Utils.OUT.println();
		}
	}

	@Test
	public void testFindTypes()
	{
		for (var reln : RELN)
		{
			String t1 = Types.findType(reln, 1, SumoProvider.sumo);
			String t2 = Types.findType(reln, 2, SumoProvider.sumo);
			String t3 = Types.findType(reln, 3, SumoProvider.sumo);

			if (t3 == null)
			{
				Utils.OUT.println("reln=" + reln + " type1=" + t1 + " type2=" + t2);
			}
			else
			{
				Utils.OUT.println("reln=" + reln + " type1=" + t1 + " type2=" + t2 + " type3=" + t3);
			}
			Utils.OUT.println();
		}
	}

	public static void main(String[] args)
	{
		new SumoProvider().load();
		TestTypes p = new TestTypes();
		p.testFindTypes();
		p.testFindArgTypes();
		p.testAddTypeRestrictionsU();
		p.testComputeTypeRestrictionsU();
		p.testAddTypeRestrictionsE();
		p.testComputeTypeRestrictionsE();
		p.testComputeVariableTypes();
	}
}
