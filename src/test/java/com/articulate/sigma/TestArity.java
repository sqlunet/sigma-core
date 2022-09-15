package com.articulate.sigma;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.sqlunet.sumo.Kb;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestArity
{
	private static Kb kb;

	private static final String[] RELS = {"instance", "member", "depth", "ethnicityPercentInRegion", "sectorCompositionOfGDPInPeriod", "sharedBorderLength", "totalGDPInPeriod",};

	private static final String[] strFs = { //
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
			"(totalGDPInPeriod (MeasureFn 18100 (GigaFn USDollar)) (YearFn 1996))",}; //


	private static final String[] okStrFs = { //
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

	@Test
	public void arityFailTest()
	{
		assertThrows(AssertionError.class, () -> arityTest(strFs));
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
				Utils.OUT_INFO.println(f);
			}
			catch (Formula.ArityException ae)
			{
				success = false;
				Utils.OUT_ERR.println(ae + " in " + f);
			}
		}
		assertTrue(success);
	}

	@BeforeAll
	public static void init()
	{
		Utils.turnOffLogging();
		kb = Utils.loadKb(Utils.CORE_FILES);
		Utils.getRelValences(RELS, kb);
	}

	public static void main(String[] args)
	{
		init();
		new TestArity().aritySuccessTest();
	}
}
