package com.articulate.sigma;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith({SumoProvider.class})
public class TestArity
{
	private static final boolean silent = System.getProperties().containsKey("SILENT");

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
			}; //


	private static final String[] okStrFs = { //
			"(totalGDPInPeriod Zimbabwe (MeasureFn 18100 (GigaFn USDollar)) (YearFn 1996))",
			"(waterDepth Seas-YellowSea (MeasureFn 200 Meter))", //
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

	public void arityTest(String[] forms)
	{
		boolean success = true;
		for (String form : forms)
		{
			Formula f = Formula.of(form);
			try
			{
				f.hasCorrectArityThrows(SumoProvider.sumo::getValence);
				if (!silent)
				{
					Utils.OUT_INFO.println(f);
				}
			}
			catch (Arity.ArityException ae)
			{
				success = false;
				if (!silent)
				{
					Utils.OUT_WARN.println(ae + " in " + f);
				}
			}
		}
		assertTrue(success);
	}

	public static void main(String[] args)
	{
		new SumoProvider().load();
		Utils.getRelValences(RELS, SumoProvider.sumo);
		new TestArity().aritySuccessTest();
	}
}
