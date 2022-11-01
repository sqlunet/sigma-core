/*
 * Copyright (c) 2022.
 * This code is copyright Articulate Software (c) 2003.  Some portions copyright Teknowledge (c) 2003
 * and reused under the terms of the GNU license.
 * Significant portions of the code have been revised, trimmed, revamped,enhanced by
 * Bernard Bou <1313ou@gmail.com>
 * This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
 * Users of this code also consent, by use of this code, to credit Articulate Software and Teknowledge
 * in any writings, briefings, publications, presentations, or other representations of any software
 * which incorporates, builds on, or uses this code.
 */

package org.sigma.core.kif;

import org.sigma.core.Nullable;

import java.io.Reader;

public class KifTokenizer extends StreamTokenizer_s
{
	/**
	 * Create a tokenizer that parses the given character stream.
	 *
	 * @param r a Reader object providing the input stream.
	 * @since JDK1.1
	 */
	public KifTokenizer(@Nullable final Reader r)
	{
		super(r);
		setup();
	}

	/**
	 * This routine sets up the StreamTokenizer_s so that it parses SUO-KIF.
	 * '=', '&lt;' '&gt;' are treated as word characters, as are normal alphanumerics.
	 * ';' is the line comment character and '"' is the quote character.
	 */
	public void setup()
	{
		whitespaceChars(0, 32);
		ordinaryChars(33, 44);           // !"#$%&'()*+,
		wordChars(45, 46);               // -.
		ordinaryChar(47);                         // /
		wordChars(48, 58);               // 0-9:
		ordinaryChar(59);                         // ;
		wordChars(60, 64);               // <=>?@
		wordChars(65, 90);               // A-Z
		ordinaryChars(91, 94);           // [\]^
		wordChars(95, 95);               // _
		ordinaryChar(96);                         // `
		wordChars(97, 122);              // a-z
		ordinaryChars(123, 255);         // {|}~
		quoteChar('"');
		commentChar(';');
		eolIsSignificant(true);
	}
}
