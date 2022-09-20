package com.articulate.sigma;

import org.junit.jupiter.api.Test;

import static com.articulate.sigma.Utils.OUT;

public class TestOther
{
	@Test
	public void collectVariables()
	{
		Formula f = Formula.of("(=>   (and     (attribute ?H Muslim)     (equal       (WealthFn ?H) ?W)) (modalAttribute   (exists (?Z ?T)     (and       (instance ?Z Zakat)       (instance ?Y Year)       (during ?Y         (WhenFn ?H))       (holdsDuring ?Y         (attribute ?H FullyFormed))       (agent ?Z ?H)       (patient ?Z ?T)       (monetaryValue ?T ?C)       (greaterThan ?C         (MultiplicationFn ?W 0.025)))) Obligation)) ");
		OUT.println("Input: " + f);
		OUT.println("All variables: " + f.collectAllVariables());
		OUT.println("Quantified variables: " + f.collectQuantifiedVariables());
		OUT.println("Unquantified variables: " + f.collectUnquantifiedVariables());
		OUT.println("Terms: " + f.collectTerms());
	}

	@Test
	public void isSimpleClause()
	{
		Formula f = Formula.of("(not (instance ?X Human))");
		OUT.println("Simple clause? : " + f + "\n" + f.isSimpleClause() + "\n");
		f = Formula.of("(instance ?X Human)");
		OUT.println("Simple clause? : " + f + "\n" + f.isSimpleClause() + "\n");
		f = Formula.of("(=> (attribute ?Agent Investor) (exists (?Investing) (agent ?Investing ?Agent)))");
		OUT.println("Simple clause? : " + f + "\n" + f.isSimpleClause() + "\n");
		f = Formula.of("(member (SkFn 1 ?X3) ?X3)");
		OUT.println("Simple clause? : " + f + "\n" + f.isSimpleClause() + "\n");
		f = Formula.of("(member ?VAR1 Org1-1)");
		OUT.println("Simple clause? : " + f + "\n" + f.isSimpleClause() + "\n");
		f = Formula.of("(capability (KappaFn ?HEAR (and (instance ?HEAR Hearing) (agent ?HEAR ?HUMAN) (destination ?HEAR ?HUMAN) (origin ?HEAR ?OBJ))) agent ?HUMAN)");
		OUT.println("Simple clause? : " + f + "\n" + f.isSimpleClause() + "\n");
	}

	@Test
	public void replaceVar()
	{
		String[] forms = {"?REL", "(?REL)", "(?REL b)", "(a ?REL)", "(instance ?REL Transitive)", "(<=> (instance ?REL TransitiveRelation) (forall (?INST1 ?INST2 ?INST3) (=> (and (?REL ?INST1 ?INST2) (?REL ?INST2 ?INST3)) (?REL ?INST1 ?INST3))))"};
		for (String form : forms)
		{
			Formula f = Formula.of(form);
			Formula f2 = f.replaceVar("?REL", "part");
			OUT.println("Input: " + form + " formula=" + f);
			OUT.println("Result: " + f2);
			OUT.println();
		}
	}

	@Test
	public void validArgs()
	{
		Formula f = Formula.of("(=> (instance ?AT AutomobileTransmission) (hasPurpose ?AT (exists (?C ?D ?A ?R1 ?N1 ?R2 ?R3 ?R4 ?N2 ?N3) (and (instance ?C Crankshaft) (instance ?D Driveshaft) (instance ?A Automobile) (part ?D ?A) (part ?AT ?A) (part ?C ?A) (connectedEngineeringComponents ?C ?AT) (connectedEngineeringComponents ?D ?AT) (instance ?R1 Rotating) (instance ?R2 Rotating) (instance ?R3 Rotating) (instance ?R4 Rotating) (patient ?R1 ?C) (patient ?R2 ?C) (patient ?R3 ?D) (patient ?R4 ?D) (causes ?R1 ?R3) (causes ?R2 ?R4) (not (equal ?R1 ?R2)) (holdsDuring ?R1 (measure ?C (RotationFn ?N1 MinuteDuration))) (holdsDuring ?R2 (measure ?C (RotationFn ?N1 MinuteDuration))) (holdsDuring ?R3 (measure ?D (RotationFn ?N2 MinuteDuration))) (holdsDuring ?R4 (measure ?D (RotationFn ?N3 MinuteDuration))) (not (equal ?N2 ?N3))))))");
		OUT.println("Input: " + f);
		OUT.println("Valid: " + "".equals(f.validArgs()));
	}

	@Test
	public void validArgsBig()
	{
		Formula f = Formula.of("(=> (instance ?AT AutomobileTransmission) (hasPurpose ?AT (exists (?C ?D ?A ?R1 ?N1 ?R2 ?R3 ?R4 ?N2 ?N3) (and (instance ?C Crankshaft) (instance ?D Driveshaft) (instance ?A Automobile) (part ?D ?A) (part ?AT ?A) (part ?C ?A) (connectedEngineeringComponents ?C ?AT) (connectedEngineeringComponents ?D ?AT) (instance ?R1 Rotating) (instance ?R2 Rotating) (instance ?R3 Rotating) (instance ?R4 Rotating) (patient ?R1 ?C) (patient ?R2 ?C) (patient ?R3 ?D) (patient ?R4 ?D) (causes ?R1 ?R3) (causes ?R2 ?R4) (not (equal ?R1 ?R2)) (holdsDuring ?R1 (measure ?C (RotationFn ?N1 MinuteDuration))) (holdsDuring ?R2 (measure ?C (RotationFn ?N1 MinuteDuration))) (holdsDuring ?R3 (measure ?D (RotationFn ?N2 MinuteDuration))) (holdsDuring ?R4 (measure ?D (RotationFn ?N3 MinuteDuration))) (not (equal ?N2 ?N3))))))");
		OUT.println("Input: " + f);
		OUT.println("Valid: " + "".equals(f.validArgs()));
	}

	@Test
	public void argList()
	{
		Formula f = Formula.of("(termFormat EnglishLanguage experimentalControlProcess \"experimental control (process)\")");
		Formula f2 = Formula.of("(termFormat EnglishLanguage experimentalControlProcess \"experimental control process\")");
		OUT.println("Input: " + f);
		OUT.println(f.simpleArgumentsToList(0));
		OUT.println("Input: " + f2);
		OUT.println(f2.simpleArgumentsToList(0));
	}

	@Test
	public void argListComplex()
	{
		Formula f = Formula.of("(during ?Y (WhenFn ?H))");
		OUT.println("Input: " + f);
		OUT.println(f.simpleArgumentsToList(1));
	}
}
