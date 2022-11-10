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
		WordNetOwl.writeOWLWordNetRelationDefinitions(Helpers.OUT);
	}

	@Test
	public void testWordNetClassDefinitions()
	{
		WordNetOwl.writeOWLWordNetClassDefinitions(Helpers.OUT);
	}

	@Test
	public void testWordNetVerbFrames()
	{
		WordNetOwl.writeOWLVerbFrames(Helpers.OUT);
	}

	@Test
	public void testWordNetSynsets()
	{
		WordNetOwl.writeOWLWordNetSynsets(WN, Helpers.OUT);
	}

	@Test
	public void testWordNetSenseIndex()
	{
		WordNetOwl.writeOWLSenseIndex(WN, Helpers.OUT);
	}

	@Test
	public void testWordNetExceptions()
	{
		WordNetOwl.writeOWLWordNetExceptions(WN, Helpers.OUT);
	}

	@Test
	public void testWordNetWordsToSenses()
	{
		WordNetOwl.writeOWLWordsToSenses(WN, Helpers.OUT);
	}

	@Disabled
	@Test
	public void testWordNet()
	{
		WordNetOwl.writeOWLWordNet(WN, Helpers.OUT);
	}

	@BeforeAll
	static void init() throws IOException
	{
		WN = new WordNet();
		WN.initOnce();
	}
}
