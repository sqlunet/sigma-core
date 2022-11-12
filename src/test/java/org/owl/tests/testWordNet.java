/*
 * Copyright (c) 2022.
 * This code is copyright Bernard Bou <1313ou@gmail.com>
 * This software is released under the GNU Public License 3 <http://www.gnu.org/copyleft/gpl.html>.
 */

package org.owl.tests;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.owl.WordNet;
import org.owl.WordNetOwl;
import org.sigma.core.Helpers;

import java.io.IOException;

public class testWordNet
{
	static private WordNet WN;

	@Test
	public void testWordnetRelationDefinitions()
	{
		WordNetOwl.writeWordNetRelationDefinitions(Helpers.OUT);
	}

	@Test
	public void testWordNetClassDefinitions()
	{
		WordNetOwl.writeWordNetClassDefinitions(Helpers.OUT);
	}

	@Test
	public void testWordNetVerbFrames()
	{
		WordNetOwl.writeVerbFrames(Helpers.OUT);
	}

	@Test
	public void testWordNetWords()
	{
		WordNetOwl.writeWords(WN, Helpers.OUT);
	}

	@Test
	public void testWordNetSynsets()
	{
		WordNetOwl.writeSynsets(WN, Helpers.OUT);
	}

	@Test
	public void testWordNetSenses()
	{
		WordNetOwl.writeSenses(WN, Helpers.OUT);
	}

	@Test
	public void testWordNetExceptions()
	{
		WordNetOwl.writeExceptions(WN, Helpers.OUT);
	}

	@Disabled
	@Test
	public void testWordNet()
	{
		WordNetOwl.writeWordNet(WN, Helpers.OUT);
	}

	@BeforeAll
	static void init() throws IOException
	{
		WN = new WordNet();
		WN.init();
	}
}
