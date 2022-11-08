/*
 * Copyright (c) 2022.
 * This code is copyright Bernard Bou <1313ou@gmail.com>
 * This software is released under the GNU Public License 3 <http://www.gnu.org/copyleft/gpl.html>.
 */

package org.sigma.core.tests;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sigma.core.*;

import java.io.PrintStream;
import java.util.Objects;

@ExtendWith({BaseSumoProvider.class})
public class TestConvert
{
	@Test
	public void convertToProlog()
	{
		convertFormulasToProlog(BaseSumoProvider.SUMO, Helpers.OUT);
	}

	public void convertFormulasToProlog(@NotNull final BaseKB kb, @NotNull final PrintStream ps)
	{
		kb.getFormulas().stream().map(Formula::toProlog).filter(Objects::nonNull).forEach(ps::println);
	}

	@BeforeAll
	public static void init()
	{
	}

	@AfterAll
	public static void shutdown()
	{
	}

	public static void main(String[] args)
	{
		new SumoProvider().load();
		init();
		TestConvert d = new TestConvert();
		d.convertToProlog();
		shutdown();
	}
}
