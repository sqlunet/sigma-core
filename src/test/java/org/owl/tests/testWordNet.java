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
import org.sigma.core.Helpers;

import java.io.IOException;

public class testWordNet
{
	static private WordNet WN;

	@Test
	public void testWordnetRelationDefinitions()
	{
		WordNet.writeOWLWordNetRelationDefinitions(Helpers.OUT);
	}

	@Test
	public void testWordNetClassDefinitions()
	{
		WordNet.writeOWLWordNetClassDefinitions(Helpers.OUT);
	}

	@Test
	public void testWordNetVerbFrames()
	{
		WordNet.writeOWLVerbFrames(Helpers.OUT);
	}


	@Test
	public void testWordNetSynsets()
	{
		WordNet.writeOWLWordNetSynsets(WN, Helpers.OUT);
	}

	@Test
	public void testWordNetSenseIndex()
	{
		WordNet.writeOWLSenseIndex(WN, Helpers.OUT);
	}

	@Test
	public void testWordNetExceptions()
	{
		WordNet.writeOWLWordNetExceptions(WN, Helpers.OUT);
	}

	@Test
	public void testWordNetWordsToSenses()
	{
		WordNet.writeOWLWordsToSenses(WN, Helpers.OUT);
	}

	@Disabled
	@Test
	public void testWordNet()
	{
		WordNet.writeOWLWordNet(WN, Helpers.OUT);
	}

	@BeforeAll
	static void init() throws IOException
	{
		WN = new WordNet();
		WN.initOnce();
	}
}
