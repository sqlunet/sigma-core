/*
 * Copyright (c) 2022.
 * This code is copyright Bernard Bou <1313ou@gmail.com>
 * This software is released under the GNU Public License 3 <http://www.gnu.org/copyleft/gpl.html>.
 */

package org.sigma.core.tests;

import org.sigma.core.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith({SumoProvider.class})
public class TestArity
{
	private static final String[] RELS = { //
			"partition", //
			"instance", "range", "subclass", "subset", "subCollection", "son", "brother", //
			"domain", "domainSubclass", "documentation", "trusts", //

			"StartFn", "SineFn", //
			"AdditionFn", "UnionFn", "KappaFn", // //
			"SubstringFn", //

			"part", "piece", "depth",  //
			"ethnicityPercentInRegion", "sectorCompositionOfGDPInPeriod", "sharedBorderLength", "totalGDPInPeriod", //
	};

	private static final String[] RELS_MONDIAL = { //
			"ethnicityPercentInRegion", "sectorCompositionOfGDPInPeriod", "sharedBorderLength", "totalGDPInPeriod", //
	};

	private static final String[] FAIL_FORMULAS = { //
			"(beliefGroupPercentInRegion Religions-Christian 25)", //
			"(beliefGroupPercentInRegion Religions-Muslim 1)", //
			"(ethnicityPercentInRegion EthnicGroups-African 98)", //
			"(ethnicityPercentInRegion EthnicGroups-Asian 1)", //
			"(sectorCompositionOfGDPInPeriod AgriculturalSector 18.3 (YearFn 1996))", //
			"(sectorCompositionOfGDPInPeriod IndustrialSector 35.3 (YearFn 1996))", //
			"(sharedBorderLength Botswana (MeasureFn 813 Kilometer))", //
			"(sharedBorderLength Mozambique (MeasureFn 1231 Kilometer))", //
			"(totalGDPInPeriod (MeasureFn 18100 (GigaFn USDollar)) (YearFn 1996))", //
			"(depth Seas-ArabianSea 5203)", //
			"(depth Seas-ArcticOcean 5220)", //
			"(=> (and (instance ?EXPERIMENT Experimenting) (instance ?INTERVAL TimeInterval)) (equal (DivisionFn (monetaryValue (KappaFn ?AMOUNT (exists (?PURCHASE) (and (member ?PURCHASE (QualifyingPurchasesFn ?EXPERIMENT)) (during (WhenFn ?PURCHASE) ?INTERVAL) (transactionAmount ?PURCHASE ?AMOUNT))))) (CardinalityFn (KappaFn ?USER (exists (?PURCHASE) (and (member ?PURCHASE (QualifyingPurchasesFn ?EXPERIMENT)) (during (WhenFn ?PURCHASE) ?INTERVAL) (agent ?PURCHASE ?USER)))))) (GPIFn ?EXPERIMENT ?INTERVAL)))", //
			"(=> (and (instance ?EXPERIMENT Experimenting) (instance ?INTERVAL TimeInterval)) (equal (DivisionFn (monetaryValue (KappaFn ?AMOUNT (exists (?PURCHASE) (and (member ?PURCHASE (QualifyingPurchasesFn ?EXPERIMENT)) (during (WhenFn ?PURCHASE) ?INTERVAL) (transactionAmount ?PURCHASE ?AMOUNT))))) (CardinalityFn (KappaFn ?USER (exists (?PURCHASE) (and (member ?PURCHASE (QualifyingPurchasesFn ?EXPERIMENT)) (during (WhenFn ?PURCHASE) ?INTERVAL) (agent ?PURCHASE ?USER)))))) (GPSFn ?EXPERIMENT)))", //
			"(=> (and (instance ?VISITOR Human) (instance ?COLL Collection) (forall (?EXPERIMENTING ?EVENT) (=> (and (instance ?EXPERIMENTING Experimenting) (instance ?EVENT Process) (member ?EVENT (QualifyingEventsFn ?EXPERIMENT) (capability ?EVENT experiencer ?VISITOR))) (member ?EVENT ?COLL))) (=> (member ?PROC ?COLL) (and (instance ?PROC Process) (exists (?EXP) (and (instance ?EXP Experimenting) (member ?PROC (QualifyingEventsFn ?EXP) (capability ?PROC experiencer ?VISITOR))))))) (equal (QualifiedTreatmentsFn ?VISITOR) ?COLL))", //
			"(=> (and (instance ?V Vaccination) (experiencer ?V ?H)) (exists (?VAC) (and (instance ?VAC Vaccine) (holdsDuring (ImmediateFutureFn (WhenFn ?V) (contains ?H ?VAC))))))", "(=> (and (instance ?SU SoftwareUpgrading) (patient ?SU ?C) (instance ?C Computer)) (exists (?P) (and (objectTransferred ?SU ?P) (instance ?P ComputerProgram) (holdsDuring (BeginFn (WhenFn ?SU) (softwareVersion ?P PreviousVersion))) (holdsDuring (EndFn (WhenFn ?SU) (softwareVersion ?P CurrentVersion))))))", //
	};


	private static final String[] OK_FORMULAS = { //
			"(totalGDPInPeriod Zimbabwe (MeasureFn 18100 (GigaFn USDollar)) (YearFn 1996))", "(waterDepth Seas-YellowSea (MeasureFn 200 Meter))", //
			"(=> (and (instance ?SU SoftwareUpgrading) (patient ?SU ?C) (instance ?C Computer)) (exists (?P) (and (objectTransferred ?SU ?P) (instance ?P ComputerProgram) (holdsDuring (BeginFn (WhenFn ?SU)) (softwareVersion ?P PreviousVersion)) (holdsDuring (EndFn (WhenFn ?SU)) (softwareVersion ?P CurrentVersion)))))", "(=> (and (instance ?V Vaccination) (experiencer ?V ?H)) (exists (?VAC) (and (instance ?VAC Vaccine) (holdsDuring (ImmediateFutureFn (WhenFn ?V)) (contains ?H ?VAC)))))", "(depth Seas-YellowSea 200 m)", //
			"(ethnicityPercentInRegion Zimbawe EthnicGroups-African 98)", //
			"(ethnicityPercentInRegio Zimbawen EthnicGroups-Asian 1)", //
			"(sectorCompositionOfGDPInPeriod Zimbawe AgriculturalSector 18.3 (YearFn 1996))", //
			"(sectorCompositionOfGDPInPeriod Zimbawe IndustrialSector 35.3 (YearFn 1996))", //
			"(sharedBorderLength Zimbawe Botswana (MeasureFn 813 Kilometer))",  //
			"(sharedBorderLength Zimbawe Mozambique (MeasureFn 1231 Kilometer))", //
			"(totalGDPInPeriod Zimbawe (MeasureFn 18100 (GigaFn USDollar)) (YearFn 1996))", //
	};

	private static void getRelValences(@NotNull final String[] relns, @NotNull final Sumo sumo, @NotNull final PrintStream ps)
	{
		for (@NotNull String reln : relns)
		{
			var valence = sumo.getValence(reln);
			ps.printf("'%s' valence %s%n", reln, valence);
		}
	}

	@Test
	public void valencesSpecficTest()
	{
		getRelValences(RELS_MONDIAL, SumoProvider.SUMO, Helpers.OUT);
	}

	@Test
	public void aritySuccessTest()
	{
		arityTest(OK_FORMULAS);
	}

	@Test
	public void arityFailTest()
	{
		assertThrows(AssertionError.class, () -> arityTest(FAIL_FORMULAS));
	}

	public void arityTest(@NotNull String[] forms)
	{
		boolean success = true;
		for (@NotNull String form : forms)
		{
			@NotNull Formula f = Formula.of(form);
			try
			{
				f.hasCorrectArityThrows(SumoProvider.SUMO::getValence);
				Helpers.INFO_OUT.println(f);
			}
			catch (Arity.ArityException ae)
			{
				success = false;
				Helpers.OUT_WARN.println(ae + " in " + f);
			}
		}
		assertTrue(success);
	}

	public static void main(String[] args) throws IOException
	{
		new SumoProvider().load();
		getRelValences(RELS, SumoProvider.SUMO, Helpers.OUT);
		@NotNull TestArity t = new TestArity();
		t.aritySuccessTest();
	}
}
