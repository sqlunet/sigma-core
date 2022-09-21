package com.articulate.sigma;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Map;

import static com.articulate.sigma.Utils.OUT;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestClausalForm1
{
	private static final String[] FORMS = { //
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

	//@Disabled
	@Test
	public void testClausalForm()
	{
		for (String form : FORMS)
		{
			Formula f = Formula.of(form);
			Tuple.Triple<List<Clause>, Map<String, String>, Formula> cf = f.getClausalForms();
			OUT.println(Clause.cfToString(cf));
		}
	}

	public static void main(String[] args)
	{
		TestClausalForm1 p = new TestClausalForm1();
		p.testClausalForm();
	}
}
