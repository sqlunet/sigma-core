/*
 * Copyright (c) 2022.
 * This code is copyright Bernard Bou <1313ou@gmail.com>
 * This software is released under the GNU Public License 3 <http://www.gnu.org/copyleft/gpl.html>.
 */

package org.sigma.noncore.tests;

import org.sigma.core.*;
import org.sigma.noncore.Types;
import org.sigma.noncore.Types2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
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
		for (@NotNull var f : UFORMULAS)
		{
			Helpers.OUT.println("formula=" + f.toFlatString());
			@NotNull Formula f2 = Formula.of(Types.addTypeRestrictions(f, SumoProvider.SUMO));
			Helpers.OUT.println("restricted=" + f2.toFlatString());
			Helpers.OUT.println("restricted=" + f2.toPrettyString());
			Helpers.OUT.println();
		}
	}

	@Test
	public void testAddTypeRestrictionsE()
	{
		for (@NotNull var f : EFORMULAS)
		{
			Helpers.OUT.println("formula=" + f.toFlatString());
			@NotNull Formula f2 = Formula.of(Types.addTypeRestrictions(f, SumoProvider.SUMO));
			Helpers.OUT.println("restricted=" + f2.toFlatString());
			Helpers.OUT.println("restricted=" + f2.toPrettyString());
			Helpers.OUT.println();
		}
	}

	@Test
	public void testComputeTypeRestrictionsU()
	{
		for (@NotNull var f : UFORMULAS)
		{
			@NotNull String var = "?A";
			@NotNull String var2 = "?B";
			Helpers.OUT.println("formula=" + f.toFlatString());

			@NotNull List<String> classes = new ArrayList<>();
			@NotNull List<String> superclasses = new ArrayList<>();
			Types2.computeTypeRestrictions(f, var, classes, superclasses, SumoProvider.SUMO);
			if (!classes.isEmpty())
			{
				Helpers.OUT.println(var + " must be instance of " + classes);
			}
			if (!superclasses.isEmpty())
			{
				Helpers.OUT.println(var + " must be subclass of " + superclasses);
			}

			@NotNull List<String> classes2 = new ArrayList<>();
			@NotNull List<String> superclasses2 = new ArrayList<>();
			Types2.computeTypeRestrictions(f, var2, classes2, superclasses2, SumoProvider.SUMO);
			if (!classes2.isEmpty())
			{
				Helpers.OUT.println(var2 + " must be instance of " + classes2);
			}
			if (!superclasses2.isEmpty())
			{
				Helpers.OUT.println(var2 + " must be subclass of " + superclasses2);
			}
			Helpers.OUT.println();
		}
	}

	@Test
	public void testComputeTypeRestrictionsE()
	{
		for (@NotNull var f : EFORMULAS)
		{
			@NotNull String var = "?A";
			@NotNull String var2 = "?B";
			Helpers.OUT.println("formula=" + f.toFlatString());

			@NotNull List<String> classes = new ArrayList<>();
			@NotNull List<String> superclasses = new ArrayList<>();
			Types2.computeTypeRestrictions(f, var, classes, superclasses, SumoProvider.SUMO);
			if (!classes.isEmpty())
			{
				Helpers.OUT.println(var + " must be instance of " + classes);
			}
			if (!superclasses.isEmpty())
			{
				Helpers.OUT.println(var + " must be subclass of " + superclasses);
			}

			@NotNull List<String> classes2 = new ArrayList<>();
			@NotNull List<String> superclasses2 = new ArrayList<>();
			Types2.computeTypeRestrictions(f, var2, classes2, superclasses2, SumoProvider.SUMO);
			if (!classes2.isEmpty())
			{
				Helpers.OUT.println(var2 + " must be instance of " + classes2);
			}
			if (!superclasses2.isEmpty())
			{
				Helpers.OUT.println(var2 + " must be subclass of " + superclasses2);
			}
			Helpers.OUT.println();
		}
	}

	@Test
	public void testComputeVariableTypes()
	{
		@NotNull Formula[] fs = { //
				Formula.of("(=> (wife ?A B) (husband B ?A))"), //
				Formula.of("(forall (?A) (=> (wife ?A B) (foobar B ?A)))"), //
				Formula.of("(forall (?A) (=> (wife ?A B) (husband B ?A)))"), //
				Formula.of("(forall (?A ?B) (=> (wife ?A ?B) (husband ?B ?A)))"), //
				Formula.of("(forall (?A ?B) (=> (husband ?A ?B) (wife ?B ?A)))"), //
				Formula.of("(forall (?A ?B ?C) (=> (and (sister ?A ?B) (husband ?C ?B)) (inlaw ?A ?C)"), //
				Formula.of("(forall (?A ?B) (=> (and (ingredient ?A ?B) (material ?B ?A)) foobar)"), //
		};

		for (@NotNull var f : fs)
		{
			Helpers.OUT.println("formula=" + f.toFlatString());
			@NotNull Map<String, List<List<String>>> map = new HashMap<>();
			Types2.computeVariableTypes(f, map, SumoProvider.SUMO);
			Helpers.OUT.println(map);
			Helpers.OUT.println();
		}
	}

	@Test
	public void testFindArgTypes()
	{
		for (@NotNull var reln : RELN)
		{
			@NotNull var t = new StringBuilder("(" + reln);
			@Nullable String ta;
			for (int i = 1; (ta = SumoProvider.SUMO.getArgType(reln, i)) != null; i++)
			{
				t.append(", ").append(ta);
			}
			@NotNull var tc = new StringBuilder("(" + reln);
			@Nullable String tac3;
			for (int i = 1; (tac3 = SumoProvider.SUMO.getArgTypeClass(reln, i)) != null; i++)
			{
				tc.append(", ").append(tac3);
			}
			t.append(')');
			tc.append(')');
			Helpers.OUT.println("domain      " + t);
			if (!t.toString().equals(tc.toString()))
			{
				Helpers.OUT.println("domainClass " + tc);
			}
			Helpers.OUT.println();
		}
	}

	@Test
	public void testFindTypes()
	{
		for (@NotNull var reln : RELN)
		{
			@Nullable String t1 = Types.findType(reln, 1, SumoProvider.SUMO);
			@Nullable String t2 = Types.findType(reln, 2, SumoProvider.SUMO);
			@Nullable String t3 = Types.findType(reln, 3, SumoProvider.SUMO);

			if (t3 == null)
			{
				Helpers.OUT.println("reln=" + reln + " type1=" + t1 + " type2=" + t2);
			}
			else
			{
				Helpers.OUT.println("reln=" + reln + " type1=" + t1 + " type2=" + t2 + " type3=" + t3);
			}
			Helpers.OUT.println();
		}
	}

	public static void main(String[] args) throws IOException
	{
		new SumoProvider().load();
		@NotNull TestTypes p = new TestTypes();
		p.testFindTypes();
		p.testFindArgTypes();
		p.testAddTypeRestrictionsU();
		p.testComputeTypeRestrictionsU();
		p.testAddTypeRestrictionsE();
		p.testComputeTypeRestrictionsE();
		p.testComputeVariableTypes();
	}
}
