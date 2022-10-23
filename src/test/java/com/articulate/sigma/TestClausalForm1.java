package com.articulate.sigma;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.articulate.sigma.Utils.OUT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestClausalForm1
{
	private static final String[] FORMS = { //
			"(=> P Q)",//
			"(or P Q)",//
			"(and P Q)", //
			"()", //
			"(P)", //
			"(a b)", //
	};
	private static final String[] FORMS_WITH_VARS = { //
			"(=> (a ?X) (b ?X))", //
			"(or (a ?X) (b ?X))", //
			"(and (a ?X) (b ?X))", //
			"(forall (?X ?Y) (b ?X ?Y))", //
	};
	private static final String[] LONG_FORMS = { //
			"()", "(waterDepth ?S ?W)", "(=> (a ?W) (b ?W))", //
			"(=>" + //
					"  (instance ?AT AutomobileTransmission)" + //
					"  (hasPurpose ?AT" + //
					"    (exists (?C ?D ?A ?R1 ?N1 ?R2 ?R3 ?R4 ?N2 ?N3)" + //
					"      (and" + //
					"        (instance ?C Crankshaft)" + //
					"        (instance ?D Driveshaft)" + //
					"        (instance ?A Automobile)" + //
					"        (part ?D ?A)" + //
					"        (part ?AT ?A)" + //
					"        (part ?C ?A)" + //
					"        (connectedEngineeringComponents ?C ?AT)" + //
					"        (connectedEngineeringComponents ?D ?AT)" + //
					"        (instance ?R1 Rotating)" + //
					"        (instance ?R2 Rotating)" + //
					"        (instance ?R3 Rotating)" + //
					"        (instance ?R4 Rotating)" + //
					"        (patient ?R1 ?C)" + //
					"        (patient ?R2 ?C)" + //
					"        (patient ?R3 ?D)" + //
					"        (patient ?R4 ?D)" + //
					"        (causes ?R1 ?R3)" + //
					"        (causes ?R2 ?R4)" + //
					"        (not" + //
					"          (equal ?R1 ?R2))" + //
					"        (holdsDuring ?R1" + //
					"          (measure ?C (RotationFn ?N1 MinuteDuration)))" + //
					"        (holdsDuring ?R2" + //
					"          (measure ?C (RotationFn ?N1 MinuteDuration)))" + //
					"        (holdsDuring ?R3" + //
					"          (measure ?D (RotationFn ?N2 MinuteDuration)))" + //
					"        (holdsDuring ?R4" + //
					"          (measure ?D (RotationFn ?N3 MinuteDuration)))" + //
					"        (not" + //
					"          (equal ?N2 ?N3))))))", //
	};

	@Test
	public void testClausalSimpleForms()
	{
		for (String form : FORMS)
		{
			clausalForm(form, Clausifier::clausalForm1);
		}
	}

	@Test
	public void testClausalIndexedForms()
	{
		for (String form : FORMS_WITH_VARS)
		{
			clausalFormIgnoreIndexing(form, Clausifier::clausalForm1);
		}
	}

	@Test
	public void testClausalLongForms()
	{
		for (String form : LONG_FORMS)
		{
			clausalForm(form);
		}
	}

	@Test
	public void testNotNotClausalForms()
	{
		clausalForm("(not (not a))", Clausifier::negationsIn);
	}

	@Test
	public void testNotOrClausalForms()
	{
		clausalForm("(not (or a b))", Clausifier::negationsIn);
	}

	@Test
	public void testNotAndClausalForms()
	{
		clausalForm("(not (and a b))", Clausifier::negationsIn);
	}

	@Test
	public void testIfClausalForms()
	{
		clausalForm("(=> a b)", Clausifier::implicationsOut);
	}

	@Test
	public void testIffClausalForms()
	{
		clausalForm("(<=> a b)", Clausifier::equivalencesOut);
	}

	@Test
	public void testNestedOpClausalForms()
	{
		// -> <literal>
		// -> (and <literal-sequence> ...)
		// -> (or <literal-sequence> ...)

		clausalForm("(not (not a))", Clausifier::nestedOperatorsOut); // -> a
		clausalForm("(and (and a b))", Clausifier::nestedOperatorsOut); // -> (and a b)
		clausalForm("(or (or a b))", Clausifier::nestedOperatorsOut); // -> a
	}

	@Test
	public void testDisjunctClausalForms()
	{
		clausalForm("(or P (and Q R))", Clausifier::disjunctionsIn); //  -> (and (or P Q) (or P R))
	}

	@Test
	public void testUniversalClausalForms()
	{
		clausalFormRevertIndexing("(forall (?X1 ?X2) (equal ?X1 ?X2))", Clausifier::universalsOut); //  ->
		clausalFormRevertIndexing("(=>\n" + "   (instance ?REL AntisymmetricRelation)\n" + "   (forall (?INST1 ?INST2)\n" + "      (=>\n" + "         (and\n" + "            (?REL ?INST1 ?INST2)\n" + "            (?REL ?INST2 ?INST1))\n" + "         (equal ?INST1 ?INST2))))\n", Clausifier::universalsOut); //  ->
	}

	@Test
	public void testExistentialClausalForms()
	{
		clausalFormIgnoreIndexing("(exists (?OBJECT) (p ?OBJECT))", Clausifier::existentialsOut); //  ->
	}

	public void clausalForm(String form)
	{
		Formula f = Formula.of(form);
		Tuple.Triple<List<Clause>, Map<String, String>, Formula> cf = f.getClausalForms();
		OUT.println(Clause.cfToString(cf));
		OUT.println();
	}

	public void clausalForm(String form, Function<Formula, Formula> transform)
	{
		Formula f = Formula.of(form);
		Tuple.Triple<List<Clause>, Map<String, String>, Formula> cf = f.getClausalForms();
		Formula f2 = transform.apply(f);
		Tuple.Triple<List<Clause>, Map<String, String>, Formula> cf2 = f2.getClausalForms();

		assert cf != null;
		OUT.println(Clause.cfToString(cf));
		OUT.println("TRANSFORMED");
		assert cf2 != null;
		OUT.println(Clause.cfToString(cf2));

		List<Clause> clauses1 = cf.first;
		List<Clause> clauses2 = cf2.first;
		assertEquals(clauses1, clauses2);
		OUT.println();
	}

	public void clausalFormRevertIndexing(String form, Function<Formula, Formula> transform)
	{
		Formula f = Formula.of(form);
		Tuple.Triple<List<Clause>, Map<String, String>, Formula> cf = f.getClausalForms();
		assert cf != null;
		Map<String, String> inverseRenames = Variables.makeVarMapClosure(cf.second);

		Formula f2 = transform.apply(f);
		Tuple.Triple<List<Clause>, Map<String, String>, Formula> cf2 = f2.getClausalForms();
		assert cf2 != null;
		Map<String, String> inverseRenames2 = Variables.makeVarMapClosure(cf2.second);

		OUT.println(Clause.cfToString(cf));
		OUT.println("TRANSFORMED");
		OUT.println(Clause.cfToString(cf2));

		OUT.println("invm = " + inverseRenames);
		OUT.println("invm2 = " + inverseRenames2);
		List<Clause> clauses1 = cf.first;
		List<Clause> clauses2 = cf2.first;
		assert clauses1 != null;
		assert clauses2 != null;
		for (int i = 0; i < clauses1.size(); i++)
		{
			Clause clause1 = clauses1.get(i);
			Clause clause2 = clauses2.get(i);
			var flat1 = Stream.concat(clause1.negativeLits.stream(), clause1.positiveLits.stream()).sorted().map(f3 -> Variables.renameVariables(f3, inverseRenames)).map(Formula::toFlatString).collect(Collectors.toList());
			var flat2 = Stream.concat(clause2.negativeLits.stream(), clause2.positiveLits.stream()).sorted().map(f3 -> Variables.renameVariables(f3, inverseRenames2)).map(Formula::toFlatString).collect(Collectors.toList());
			OUT.println(flat1 + " ==\n" + flat2);
			assertEquals(flat1, flat2);
		}
		OUT.println();
	}

	public void clausalFormIgnoreIndexing(String form, Function<Formula, Formula> transform)
	{
		Formula f = Formula.of(form);
		Tuple.Triple<List<Clause>, Map<String, String>, Formula> cf = f.getClausalForms();

		Formula f2 = transform.apply(f);
		Tuple.Triple<List<Clause>, Map<String, String>, Formula> cf2 = f2.getClausalForms();

		assert cf != null;
		OUT.println(Clause.cfToString(cf));
		OUT.println("TRANSFORMED");
		assert cf2 != null;
		OUT.println(Clause.cfToString(cf2));

		List<Clause> clauses1 = cf.first;
		List<Clause> clauses2 = cf2.first;
		assert clauses1 != null;
		assert clauses2 != null;
		for (int i = 0; i < clauses1.size(); i++)
		{
			Clause clause1 = clauses1.get(i);
			Clause clause2 = clauses2.get(i);
			var flat1 = Stream.concat(clause1.negativeLits.stream(), clause1.positiveLits.stream()).sorted().map(f3 -> f3.form.replaceAll("[0-9]+","@")).collect(Collectors.toList());
			var flat2 = Stream.concat(clause2.negativeLits.stream(), clause2.positiveLits.stream()).sorted().map(f3 -> f3.form.replaceAll("[0-9]+","@")).collect(Collectors.toList());
			OUT.println(flat1 + " ==\n" + flat2);
			assertEquals(flat1, flat2);
		}
		OUT.println();
	}

	public static void main(String[] args)
	{
		TestClausalForm1 p = new TestClausalForm1();
		p.testClausalSimpleForms();
		p.testClausalLongForms();
		p.testNotNotClausalForms();
		p.testNotAndClausalForms();
		p.testIfClausalForms();
		p.testIffClausalForms();
	}
}
