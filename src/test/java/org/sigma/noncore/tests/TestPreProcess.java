/*
 * Copyright (c) 2022.
 * This code is copyright Bernard Bou <1313ou@gmail.com>
 * This software is released under the GNU Public License 3 <http://www.gnu.org/copyleft/gpl.html>.
 */

package org.sigma.noncore.tests;

import org.sigma.core.Formula;
import org.sigma.core.NotNull;
import org.sigma.core.SumoProvider;
import org.sigma.core.Helpers;
import org.sigma.noncore.FormulaPreProcessor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.stream.Collectors;

@ExtendWith({SumoProvider.class})
public class TestPreProcess
{
	//@Disabled
	@Test
	public void testPreProcess()
	{
		@NotNull Formula[] fs = { //
				Formula.of("(r a b)"), //
				Formula.of("(=> (wife ?A ?B) (husband ?B ?A))"),  //
		};

		for (@NotNull var f : fs)
		{
			Helpers.OUT.println(f);
			@NotNull List<Formula> rfs = FormulaPreProcessor.preProcess(f, true, SumoProvider.SUMO);
			@NotNull List<Formula> rfs2 = FormulaPreProcessor.preProcess(f, false, SumoProvider.SUMO);
			Helpers.OUT.println("preprocessed (query)=\n" + rfs.stream().map(Formula::toFlatString).collect(Collectors.joining("\n")));
			Helpers.OUT.println("preprocessed=\n" + rfs2.stream().map(Formula::toFlatString).collect(Collectors.joining("\n")));
			Helpers.OUT.println();
		}
	}

	//@Disabled
	@Test
	public void testPreProcess2()
	{
		@NotNull Formula[] fs = { //
				Formula.of("(=> (and (instance ?REL SymmetricRelation) (?REL ?INST1 ?INST2)) (?REL ?INST2 ?INST1))"), //
				Formula.of("(=> (and (subrelation ?REL1 ?REL2) (instance ?REL1 Predicate) (instance ?REL2 Predicate) (?REL1 @ROW)) (?REL2 @ROW))"), //
		};

		for (@NotNull var f : fs)
		{
			Helpers.OUT.println(f);
			@NotNull List<Formula> rfs = FormulaPreProcessor.preProcess(f, false, SumoProvider.SUMO);
			Helpers.OUT.println("preprocessed=\n" + rfs.stream().map(Formula::toFlatString).collect(Collectors.joining("\n")));
			Helpers.OUT.println();
		}
	}

	public static void main(String[] args)
	{
		new SumoProvider().load();
		@NotNull TestPreProcess p = new TestPreProcess();
		p.testPreProcess();
		p.testPreProcess2();
	}
}
