package com.articulate.sigma;

import org.junit.BeforeClass;
import org.junit.Test;
import org.sqlunet.sumo.SUMOKb;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TestArity
{
	@BeforeClass
	public static void noLogging()
	{
		String loggingPath = "logging.properties";
		System.setProperty("java.util.logging.config.file", loggingPath);
	}

	private static String kbPath;
	private static SUMOKb kb;

	private static final String[] FILES = {"Merge.kif", "Mid-level-ontology.kif"};

	private static final String[] RELS = {"instance", "member", "depth", "ethnicityPercentInRegion", "sectorCompositionOfGDPInPeriod", "sharedBorderLength", "totalGDPInPeriod",};

	@BeforeClass
	public static void init()
	{
		kbPath = System.getProperty("sumopath");
		if (kbPath == null)
		{
			kbPath = System.getenv("SUMOHOME");
		}
		assertNotNull("Pass KB location as -Dsumopath=<somewhere> or SUMOHOME=<somewhere> in env", kbPath);
		kb = new SUMOKb(kbPath);
		System.out.printf("Kb building%n");
		boolean result = kb.make(FILES);
		assertTrue(result);

		System.out.println();
		for (String rel : RELS)
		{
			System.out.printf("'%s' valence %s%n", rel, kb.getValence(rel));
		}
	}

	private static String[] strFs = { //
//			"(beliefGroupPercentInRegion Religions-Christian 25)", //
//			"(beliefGroupPercentInRegion Religions-Muslim 1)", //
//			"(ethnicityPercentInRegion EthnicGroups-African 98)", //
//			"(ethnicityPercentInRegion EthnicGroups-Asian 1)", //
//			"(sectorCompositionOfGDPInPeriod AgriculturalSector 18.3 (YearFn 1996))", //
//			"(sectorCompositionOfGDPInPeriod IndustrialSector 35.3 (YearFn 1996))", //
//			"(sharedBorderLength Botswana (MeasureFn 813 Kilometer))", //
//			"(sharedBorderLength Mozambique (MeasureFn 1231 Kilometer))", //
//			"(totalGDPInPeriod (MeasureFn 18100 (GigaFn USDollar)) (YearFn 1996))", //
			"(depth Seas-ArabianSea 5203)", //
//			"(depth Seas-ArcticOcean 5220)", //
//			"(=> (and (instance ?EXPERIMENT Experimenting) (instance ?INTERVAL TimeInterval)) (equal (DivisionFn (monetaryValue (KappaFn ?AMOUNT (exists (?PURCHASE) (and (member ?PURCHASE (QualifyingPurchasesFn ?EXPERIMENT)) (during (WhenFn ?PURCHASE) ?INTERVAL) (transactionAmount ?PURCHASE ?AMOUNT))))) (CardinalityFn (KappaFn ?USER (exists (?PURCHASE) (and (member ?PURCHASE (QualifyingPurchasesFn ?EXPERIMENT)) (during (WhenFn ?PURCHASE) ?INTERVAL) (agent ?PURCHASE ?USER)))))) (GPIFn ?EXPERIMENT ?INTERVAL)))", //
//			"(=> (and (instance ?EXPERIMENT Experimenting) (instance ?INTERVAL TimeInterval)) (equal (DivisionFn (monetaryValue (KappaFn ?AMOUNT (exists (?PURCHASE) (and (member ?PURCHASE (QualifyingPurchasesFn ?EXPERIMENT)) (during (WhenFn ?PURCHASE) ?INTERVAL) (transactionAmount ?PURCHASE ?AMOUNT))))) (CardinalityFn (KappaFn ?USER (exists (?PURCHASE) (and (member ?PURCHASE (QualifyingPurchasesFn ?EXPERIMENT)) (during (WhenFn ?PURCHASE) ?INTERVAL) (agent ?PURCHASE ?USER)))))) (GPSFn ?EXPERIMENT)))", //
//			"(=> (and (instance ?VISITOR Human) (instance ?COLL Collection) (forall (?EXPERIMENTING ?EVENT) (=> (and (instance ?EXPERIMENTING Experimenting) (instance ?EVENT Process) (member ?EVENT (QualifyingEventsFn ?EXPERIMENT) (capability ?EVENT experiencer ?VISITOR))) (member ?EVENT ?COLL))) (=> (member ?PROC ?COLL) (and (instance ?PROC Process) (exists (?EXP) (and (instance ?EXP Experimenting) (member ?PROC (QualifyingEventsFn ?EXP) (capability ?PROC experiencer ?VISITOR))))))) (equal (QualifiedTreatmentsFn ?VISITOR) ?COLL))", //
//  		"(=> (and (instance ?V Vaccination) (experiencer ?V ?H)) (exists (?VAC) (and (instance ?VAC Vaccine) (holdsDuring (ImmediateFutureFn (WhenFn ?V) (contains ?H ?VAC))))))",
//			"(=> (and (instance ?SU SoftwareUpgrading) (patient ?SU ?C) (instance ?C Computer)) (exists (?P) (and (objectTransferred ?SU ?P) (instance ?P ComputerProgram) (holdsDuring (BeginFn (WhenFn ?SU) (softwareVersion ?P PreviousVersion))) (holdsDuring (EndFn (WhenFn ?SU) (softwareVersion ?P CurrentVersion))))))",
			"(totalGDPInPeriod (MeasureFn 18100 (GigaFn USDollar)) (YearFn 1996))",
	}; //


	private static String[] okStrFs = { //
			"(totalGDPInPeriod (MeasureFn 18100 (GigaFn USDollar)) (YearFn 1996))",
//			"(=> (and (instance ?SU SoftwareUpgrading) (patient ?SU ?C) (instance ?C Computer)) (exists (?P) (and (objectTransferred ?SU ?P) (instance ?P ComputerProgram) (holdsDuring (BeginFn (WhenFn ?SU)) (softwareVersion ?P PreviousVersion)) (holdsDuring (EndFn (WhenFn ?SU)) (softwareVersion ?P CurrentVersion)))))",
//			"(=> (and (instance ?V Vaccination) (experiencer ?V ?H)) (exists (?VAC) (and (instance ?VAC Vaccine) (holdsDuring (ImmediateFutureFn (WhenFn ?V)) (contains ?H ?VAC)))))"
//			"(depth Seas-YellowSea 200 m)", //
//			"(ethnicityPercentInRegion EthnicGroups-African 98)", //
//			"(ethnicityPercentInRegion EthnicGroups-Asian 1)", //
//			"(sectorCompositionOfGDPInPeriod AgriculturalSector 18.3 (YearFn 1996))", //
//			"(sectorCompositionOfGDPInPeriod IndustrialSector 35.3 (YearFn 1996))", //
//			"(sharedBorderLength Botswana (MeasureFn 813 Kilometer))",  //
//			"(sharedBorderLength Mozambique (MeasureFn 1231 Kilometer))", //
//			"(totalGDPInPeriod (MeasureFn 18100 (GigaFn USDollar)) (YearFn 1996))", //
	};

	@Test
	public void aritySuccessTest()
	{
		arityTest(okStrFs);
	}

	@Test(expected = AssertionError.class)
	public void arityFailTest()
	{
		arityTest(strFs);
	}

	public void arityTest(String[] formulas)
	{
		boolean success = true;
		for (String strF : formulas)
		{
			Formula f = new Formula().set(strF);
			//System.out.println(strF);
			//System.out.println(f);
			try
			{
				f.hasCorrectArityThrows(kb);
				System.out.println(f);
			}
			catch (Formula.ArityException ae)
			{
				success = false;
				System.err.println(ae + " in " + f);
			}
		}
		assertTrue(success);
	}
}
