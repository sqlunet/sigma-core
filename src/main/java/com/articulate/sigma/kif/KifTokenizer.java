package com.articulate.sigma.kif;

import com.articulate.sigma.Nullable;

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
	 * = &lt; $gt; are treated as word characters, as are normal alphanumerics.
	 * ; is the line comment character and " is the quote character.
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
