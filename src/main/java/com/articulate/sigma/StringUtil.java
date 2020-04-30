/* This code is copyright Articulate Software (c) 2003.  Some
portions copyright Teknowledge (c) 2003 and reused under the terms of
the GNU license.  This software is released under the GNU Public
License <http://www.gnu.org/copyleft/gpl.html>.  Users of this code
also consent, by use of this code, to credit Articulate Software and
Teknowledge in any writings, briefings, publications, presentations,
or other representations of any software which incorporates, builds
on, or uses this code.  Please cite the following article in any
publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment, in
Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed
Systems, August 9, Acapulco, Mexico.
See also http://sigmakee.sourceforge.net
*/

package com.articulate.sigma;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.SimpleTimeZone;
// import org.apache.commons.codec.binary.Base64;

/**
 * A utility class that defines static methods for common string
 * manipulation operations.
 */
public class StringUtil
{

	private StringUtil()
	{
		// This class should not have any instances.
	}

	/**
	 * Returns the default line separator token for the current
	 * runtime platform.
	 *
	 * @return line separator
	 */
	public static String getLineSeparator()
	{
		return System.getProperty("line.separator");
	}

	/**
	 * A String token that separates a qualified KIF term name
	 * from the namespace abbreviation prefix that qualifies it.
	 */
	private static final String KIF_NAMESPACE_DELIMITER = ":";

	/**
	 * Returns the string used in SUO-KIF to separate a namespace
	 * prefix from the term it qualifies.
	 *
	 * @return Kif namespace delimiter
	 */
	public static String getKifNamespaceDelimiter()
	{
		return KIF_NAMESPACE_DELIMITER;
	}

	/**
	 * A String token that separates a qualified term name from
	 * the W3C namespace abbreviation prefix that qualifies it.
	 */
	private static final String W3C_NAMESPACE_DELIMITER = ":";

	/**
	 * Returns the string preferred by W3C to separate a namespace
	 * prefix from the term it qualifies.
	 *
	 * @return W3C namespace delimiter
	 */
	public static String getW3cNamespaceDelimiter()
	{
		return W3C_NAMESPACE_DELIMITER;
	}

	/**
	 * A "safe" alphanumeric ASCII string that can be substituted for
	 * the W3C or SUO-KIF string delimiting a namespace prefix from an
	 * unqualified term name.  The safe delimiter is used to produce
	 * input formulae or files that can be loaded by Vampire and other
	 * provers unable to handle term names containing non-alphanumeric
	 * characters.
	 */
	private static final String SAFE_NAMESPACE_DELIMITER = "0xx1";

	/**
	 * Returns a "safe" alphanumeric ASCII string that can be
	 * substituted for the W3C or SUO-KIF string delimiting a
	 * namespace prefix from an unqualified term name.  The safe
	 * delimiter is used to produce input formulae or files that can
	 * be loaded by Vampire and other provers unable to handle term
	 * names containing non-alphanumeric characters.
	 *
	 * @return safe namespace delimiter
	 */
	public static String getSafeNamespaceDelimiter()
	{
		return SAFE_NAMESPACE_DELIMITER;
	}

	/**
	 * Removes all balanced ASCII double-quote characters from each
	 * end of the String s, if any are present.
	 *
	 * @param s input string
	 * @return input without enclosing quotes
	 */
	public static String removeEnclosingQuotes(String s)
	{
		return removeEnclosingQuotes(s, Integer.MAX_VALUE);
	}

	/**
	 * Removes n layers of balanced ASCII double-quote characters from each end of the String s, if any are present.
	 *
	 * @param s input string
	 * @param n number of balanced ASCII double-quote characters
	 * @return input without enclosing quotes
	 */
	public static String removeEnclosingQuotes(String s, int n)
	{
		StringBuilder sb = new StringBuilder();
		if (!s.isEmpty())
		{
			sb.append(s);
			int lastI = (sb.length() - 1);
			for (int count = 0; ((count < n) && (lastI > 0) && (sb.charAt(0) == '"') && (sb.charAt(lastI) == '"')); count++)
			{
				sb.deleteCharAt(lastI);
				sb.deleteCharAt(0);
				lastI = (sb.length() - 1);
			}
		}
		return sb.toString();
	}

	/**
	 * @param str A String
	 * @return A String with space characters normalized to match the
	 * conventions for written English text.  All linefeeds and
	 * carriage returns are replaced with spaces.
	 */
	public static String normalizeSpaceChars(String str)
	{
		String result = str;
		if (!result.isEmpty())
		{
			// result = result.replaceAll("(?s)\\s", " ");
			result = result.replaceAll("\\s+", " ");
			result = result.replaceAll("\\(\\s+", "(");
			// result = result.replaceAll("\\.\\s+", ".  ");
			// result = result.replaceAll("\\?\\s+", "?  ");
			// result = result.replaceAll("\\:\\s+", ":  ");
			// result = result.replaceAll("\\!\\s+", "!  ");
		}
		return result;
	}

	/**
	 * @param str A String
	 * @return A String with all double quote characters properly
	 * escaped with a left slash character.
	 */
	public static String escapeQuoteChars(String str)
	{
		String result = str;
		if (!str.isEmpty())
		{
			StringBuilder sb = new StringBuilder();
			char prevCh = 'x';
			char ch;
			for (int i = 0; i < str.length(); i++)
			{
				ch = str.charAt(i);
				if ((ch == '"') && (prevCh != '\\'))
				{
					sb.append('\\');
				}
				sb.append(ch);
				prevCh = ch;
			}
			result = sb.toString();
		}
		return result;
	}

	/**
	 * @param str A String
	 * @return A String with all left slash characters removed
	 */
	public static String removeQuoteEscapes(String str)
	{
		String result = str;
		if (!str.isEmpty())
		{
			StringBuilder sb = new StringBuilder();
			char prevCh = 'x';
			char ch;
			int strLen = str.length();
			for (int i = 0; i < strLen; i++)
			{
				ch = str.charAt(i);
				if ((ch == '"') && (prevCh == '\\'))
				{
					sb.deleteCharAt(sb.length() - 1);
				}
				sb.append(ch);
				prevCh = ch;
			}
			result = sb.toString();
		}
		return result;
	}

	/**
	 * @param str A String
	 * @return A String with all sequences of two double quote
	 * characters have been replaced by a left slash character
	 * followed by a double quote character.
	 */
	public static String replaceRepeatedDoubleQuotes(String str)
	{
		String result = str;
		if (!str.isEmpty())
		{
			StringBuilder sb = new StringBuilder();
			char prevCh = 'x';
			char ch;
			for (int i = 0; i < str.length(); i++)
			{
				ch = str.charAt(i);
				if ((ch == '"') && (prevCh == '"'))
				{
					sb.setCharAt(sb.length() - 1, '\\');
				}
				sb.append(ch);
				prevCh = ch;
			}
			result = sb.toString();
		}
		return result;
	}

	/**
	 * Test whether string contains non-ascii characters
	 *
	 * @param str A String
	 * @return true if str contains any non-ASCII characters, else
	 * false.
	 */
	public static boolean containsNonAsciiChars(String str)
	{
		return !str.isEmpty() && str.matches(".*[^\\p{ASCII}].*");
	}

	/**
	 * Replace non-ascii characters
	 *
	 * @param str A String
	 * @return A String with all non-ASCII characters replaced by "x".
	 */
	public static String replaceNonAsciiChars(String str)
	{
		String result = str;
		if (!result.isEmpty())
		{
			result = result.replaceAll("[^\\p{ASCII}]", "x");
		}
		return result;
	}

	/**
	 * Returns a date/time string corresponding to pattern.  The date/time returned is the date/time of the method call.
	 * The locale is UTC (Greenwich).
	 *
	 * @param pattern Examples: yyyy, yyyy-MM-dd.
	 * @return a date/time string corresponding to pattern
	 */
	public static String getDateTime(String pattern)
	{
		String dateTime = "";
		try
		{
			if (!pattern.isEmpty())
			{
				SimpleDateFormat sdf = new SimpleDateFormat(pattern);
				sdf.setTimeZone(new SimpleTimeZone(0, "Greenwich"));
				dateTime = sdf.format(new Date());
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		return dateTime;
	}

	/**
	 * If the input String contains the sequence {date}pattern{date}, replaces the first occurrence of this sequence with a UTC
	 * date/time string formatted according to pattern.  If the input String does not contain the sequence, it is returned unaltered.
	 *
	 * @param input The input String into which a formatted date/time
	 *              will be inserted
	 * @return String with the first occurrence of sequence replaced with a UTC date/time string
	 */
	public static String replaceDateTime(String input)
	{
		String output = input;
		try
		{
			String token = "{date}";
			if (!output.isEmpty() && output.contains(token))
			{
				int tLen = token.length();
				StringBuilder sb = new StringBuilder(output);
				int p1f = sb.indexOf(token);
				while (p1f > -1)
				{
					int p1b = (p1f + tLen);
					if (p1b < sb.length())
					{
						int p2f = sb.indexOf(token, p1b);
						if (p2f > -1)
						{
							String pattern = sb.substring(p1b, p2f);
							int p2b = (p2f + tLen);
							sb.replace(p1f, p2b, getDateTime(pattern));
							p1f = sb.indexOf(token);
						}
					}
				}
				output = sb.toString();
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		return output;
	}

	/**
	 * Returns true if input appears to be a URI string, else returns false.
	 *
	 * @param input A String
	 * @return whether input appears to be a URI string.
	 */
	@SuppressWarnings("BooleanMethodIsAlwaysInverted") public static boolean isUri(String input)
	{
		return !input.isEmpty() && (input.matches("^.?http://.+") || input.matches("^.?file://.+"));
	}

	/**
	 * Returns true if input appears to be a quoted String, else returns false.
	 *
	 * @param input A String
	 * @return whether input appears to be a quoted String
	 */
	public static boolean isQuotedString(String input)
	{
		boolean result = false;
		if (!input.isEmpty())
		{
			int iLen = input.length();
			if (iLen > 2)
			{
				char fc = input.charAt(0);
				char lc = input.charAt(iLen - 1);
				result = (((fc == '"') && (lc == '"')) || (((fc == '\'') || (fc == '`')) && (lc == '\'')));
			}
		}
		return result;
	}

	/**
	 * Returns true if every char in input is a digit char, else returns false.
	 *
	 * @param input A String
	 * @return whether every char in input is a digit char
	 */
	public static boolean isDigitString(String input)
	{
		return !input.isEmpty() && !input.matches(".*\\D+.*");
	}

	/**
	 * Replaces the namespace delimiter in term with a string that is safe for inference, and for all common file systems.
	 *
	 * @param term term
	 * @return term with namespace delimiter that is safe for inference
	 */
	public static String toSafeNamespaceDelimiter(String term)
	{
		String result = term;
		if (!term.isEmpty() && !isUri(term))
		{
			String safe = getSafeNamespaceDelimiter();
			String kif = getKifNamespaceDelimiter();
			String w3c = getW3cNamespaceDelimiter();
			result = term.replaceFirst(kif, safe);
			if (!kif.equals(w3c))
			{
				result = result.replaceFirst(w3c, safe);
			}
		}
		return result;
	}

	/**
	 * Replaces the namespace delimiter in term with a string that is
	 * safe for inference and for all common file systems, but only if
	 * kbHref is an empty string or == null.  If kbHref is not empty,
	 * term is probably being prepared for display in the Sigma
	 * Browser and does not have to be converted to a "safe" form.
	 *
	 * @param kbHref KB href
	 * @param term   term
	 * @return term with namespace delimiter that is safe for inference
	 */
	public static String toSafeNamespaceDelimiter(String kbHref, String term)
	{
		String result = term;
		if (kbHref == null || kbHref.isEmpty())
			result = toSafeNamespaceDelimiter(term);
		return result;
	}

	/**
	 * W3C to Kif
	 *
	 * @param term W3C term
	 * @return Kif term
	 */
	public static String w3cToKif(String term)
	{
		String result = term;
		if (!term.isEmpty() && !isUri(term))
		{
			result = term.replaceFirst(getW3cNamespaceDelimiter(), getKifNamespaceDelimiter());
		}
		return result;
	}

	/**
	 * Kif to W3C term
	 *
	 * @param term Kif term
	 * @return W3C term
	 */
	public static String kifToW3c(String term)
	{
		String result = term;
		if (!term.isEmpty() && !isUri(term))
		{
			result = term.replaceFirst(getKifNamespaceDelimiter(), getW3cNamespaceDelimiter());
		}
		return result;
	}

	/**
	 * Unquote
	 *
	 * @param input input
	 * @return unquoted input
	 */
	public static String unquote(String input)
	{
		String ans = input;
		ans = removeEnclosingQuotes(ans);
		return replaceRepeatedDoubleQuotes(ans);
	}

	/**
	 * Test whether term is local term reference
	 *
	 * @param term term
	 * @return whether term is local term reference
	 */
	public static boolean isLocalTermReference(String term)
	{
		boolean result = false;
		if (!term.isEmpty())
		{
			List<String> blankNodeTokens = Arrays.asList("#", "~", getLocalReferenceBaseName());
			for (String bnt : blankNodeTokens)
			{
				result = (term.startsWith(bnt) && !term.matches(".*\\s+.*"));
				if (result)
					break;
			}
		}
		return result;
	}

	/**
	 * The base String used to create the names of local composite members.
	 */
	private static final String LOCAL_REF_BASE_NAME = "LocalRef";

	/**
	 * Get local reference base name
	 *
	 * @return local reference base name
	 */
	public static String getLocalReferenceBaseName()
	{
		return LOCAL_REF_BASE_NAME;
	}

	/**
	 * Wrap input
	 *
	 * @param input  input
	 * @param length line length
	 * @return wrapped input
	 */
	public static String wordWrap(String input, int length)
	{
		String result = input;
		try
		{
			if (!input.isEmpty() && (length > 0) && (input.length() > length))
			{
				StringBuilder sb = new StringBuilder(input);
				String ls = System.getProperty("line.separator");
				int lsLen = ls.length();
				int j = length;
				int i = 0;
				while (sb.length() > j)
				{
					while ((j > i) && !Character.isWhitespace(sb.charAt(j)))
						j--;
					if (j > i)
					{
						sb.deleteCharAt(j);
						sb.insert(j, ls);
						i = (j + lsLen);
						j = (i + length);
					}
					else
					{
						j += length;
						while ((j < sb.length()) && !Character.isWhitespace(sb.charAt(j)))
							j++;
						if (j < sb.length())
						{
							sb.deleteCharAt(j);
							sb.insert(j, ls);
							i = (j + lsLen);
							j = (i + length);
						}
					}
				}
				result = sb.toString();
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		return result;
	}

	/**
	 * Convenience method to wrap input with default line length of 70
	 *
	 * @param input input
	 * @return wrapped input
	 */
	public static String wordWrap(String input)
	{
		return wordWrap(input, 70);
	}
}
