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

package org.sigma.core;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.SimpleTimeZone;

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

	// S P A C E S

	/**
	 * Normalizes space characters
	 *
	 * @param str A String
	 * @return A String with space characters normalized to match the
	 * conventions for written English text.  All linefeeds and
	 * carriage returns are replaced with spaces.
	 */
	@NotNull
	public static String normalizeSpaceChars(@NotNull final String str)
	{
		String result = str;
		if (!result.isEmpty())
		{
			result = result.replaceAll("\\s+", " ");
			result = result.replaceAll("\\(\\s+", "(");
		}
		return result;
	}

	// N O N - A S C I I

	/**
	 * Test whether string contains non-ascii characters
	 *
	 * @param str A String
	 * @return true if str contains any non-ASCII characters, else
	 * false.
	 */
	public static boolean containsNonAsciiChars(@NotNull final String str)
	{
		return !str.isEmpty() && str.matches(".*[^\\p{ASCII}].*");
	}

	/**
	 * Replace non-ascii characters
	 *
	 * @param str A String
	 * @return A String with all non-ASCII characters replaced by "x".
	 */
	@NotNull
	public static String replaceNonAsciiChars(@NotNull final String str)
	{
		String result = str;
		if (!result.isEmpty())
		{
			result = result.replaceAll("[^\\p{ASCII}]", "x");
		}
		return result;
	}

	// Q U O T E

	/**
	 * Removes all balanced ASCII double-quote characters from each
	 * end of the String s, if any are present.
	 *
	 * @param s input string
	 * @return input without enclosing quotes
	 */
	@NotNull
	public static String removeEnclosingQuotes(@NotNull String s)
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
	@NotNull
	public static String removeEnclosingQuotes(@NotNull String s, int n)
	{
		@NotNull StringBuilder sb = new StringBuilder();
		if (!s.isEmpty())
		{
			sb.append(s);
			int lastI = sb.length() - 1;
			for (int count = 0; count < n && lastI > 0 && sb.charAt(0) == '"' && sb.charAt(lastI) == '"'; count++)
			{
				sb.deleteCharAt(lastI);
				sb.deleteCharAt(0);
				lastI = sb.length() - 1;
			}
		}
		return sb.toString();
	}

	/**
	 * Escapes quote characters
	 *
	 * @param str A String
	 * @return A String with all double quote characters properly
	 * escaped with a left slash character.
	 */
	@NotNull
	public static String escapeQuoteChars(@NotNull final String str)
	{
		@NotNull String result = str;
		if (!str.isEmpty())
		{
			@NotNull StringBuilder sb = new StringBuilder();
			char prevCh = '\0';
			char ch;
			int n = str.length();
			for (int i = 0; i < n; i++)
			{
				ch = str.charAt(i);
				if (ch == '"' && prevCh != '\\')
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
	 * Removes quote escapes
	 *
	 * @param str A String
	 * @return A String with all left slash characters removed
	 */
	@NotNull
	public static String removeQuoteEscapes(@NotNull final String str)
	{
		@NotNull String result = str;
		if (!str.isEmpty())
		{
			@NotNull StringBuilder sb = new StringBuilder();
			char prevCh = '\0';
			char ch;
			int n = str.length();
			for (int i = 0; i < n; i++)
			{
				ch = str.charAt(i);
				if (ch == '"' && prevCh == '\\')
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
	@NotNull
	public static String replaceRepeatedDoubleQuotes(@NotNull final String str)
	{
		@NotNull String result = str;
		if (!str.isEmpty())
		{
			@NotNull StringBuilder sb = new StringBuilder();
			char prevCh = '\0';
			char ch;
			int n = str.length();
			for (int i = 0; i < n; i++)
			{
				ch = str.charAt(i);
				if (ch == '"' && prevCh == '"')
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
	 * Returns true if input appears to be a quoted String, else returns false.
	 * "xxx",'xxx',`xxx'
	 *
	 * @param str A String
	 * @return whether input appears to be a quoted String
	 */
	public static boolean isQuotedString(@NotNull final String str)
	{
		if (!str.isEmpty())
		{
			int n = str.length();
			if (n > 2)
			{
				char c1 = str.charAt(0);
				char lc = str.charAt(n - 1);
				return (c1 == '"' && lc == '"') || ((c1 == '\'' || c1 == '`') && lc == '\'');
			}
		}
		return false;
	}

	/**
	 * Unquote
	 *
	 * @param str input
	 * @return unquoted input
	 */
	@NotNull
	public static String unquote(String str)
	{
		return replaceRepeatedDoubleQuotes(removeEnclosingQuotes(str));
	}

	// D A T E

	/**
	 * Returns a date/time string corresponding to pattern.  The date/time returned is the date/time of the method call.
	 * The locale is UTC (Greenwich).
	 *
	 * @param pattern Examples: yyyy, yyyy-MM-dd.
	 * @return a date/time string corresponding to pattern
	 */
	@NotNull
	public static String getDateTime(@NotNull final String pattern)
	{
		if (!pattern.isEmpty())
		{
			@NotNull SimpleDateFormat format = new SimpleDateFormat(pattern);
			format.setTimeZone(new SimpleTimeZone(0, "Greenwich"));
			return format.format(new Date());
		}
		return "";
	}

	/**
	 * If the input String contains the sequence {date}pattern{date}, replaces the first occurrence of this sequence with a UTC
	 * date/time string formatted according to pattern.  If the input String does not contain the sequence, it is returned unaltered.
	 *
	 * @param str The input String into which a formatted date/time
	 *            will be inserted
	 * @return String with all occurrences of sequence replaced with a UTC date/time string
	 */
	@NotNull
	public static String replaceDateTime(@NotNull final String str)
	{
		@NotNull String token = "{date}";
		if (!str.isEmpty() && str.contains(token))
		{
			int tLen = token.length();
			@NotNull StringBuilder sb = new StringBuilder(str);
			int p1f = sb.indexOf(token);
			while (p1f > -1)
			{
				// {date}xxyyyzz{date}
				// ^     ^      ^     ^
				// p1f   p1b    p2f   p2b
				int p1b = p1f + tLen;
				if (p1b < sb.length())
				{
					int p2f = sb.indexOf(token, p1b);
					if (p2f > -1)
					{
						String pattern = sb.substring(p1b, p2f);
						int p2b = p2f + tLen;
						sb.replace(p1f, p2b, getDateTime(pattern));
						p1f = sb.indexOf(token);
					}
				}
			}
			return sb.toString();
		}
		return str;
	}

	// D I G I T

	/**
	 * Returns true if every char in input is a digit char, else returns false.
	 *
	 * @param input A String
	 * @return whether every char in input is a digit char
	 */
	public static boolean isDigitString(@NotNull final String input)
	{
		return !input.isEmpty() && !input.matches(".*\\D+.*");
	}

	// U R I

	/**
	 * Returns true if input appears to be a URI string, else returns false.
	 *
	 * @param str A String
	 * @return whether input appears to be a URI string.
	 */
	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	public static boolean isUri(@NotNull final String str)
	{
		return !str.isEmpty() && (str.matches("^.?http://.+") || str.matches("^.?file://.+"));
	}

	// N A M E S P A C E

	// kif

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
	@NotNull
	public static String getKifNamespaceDelimiter()
	{
		return KIF_NAMESPACE_DELIMITER;
	}

	/**
	 * W3C to Kif
	 *
	 * @param term W3C term
	 * @return Kif term
	 */
	@NotNull
	public static String w3cToKif(@NotNull final String term)
	{
		if (!term.isEmpty() && !isUri(term))
		{
			return term.replaceFirst(getW3cNamespaceDelimiter(), getKifNamespaceDelimiter());
		}
		return term;
	}

	// w3c

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
	@NotNull
	public static String getW3cNamespaceDelimiter()
	{
		return W3C_NAMESPACE_DELIMITER;
	}

	/**
	 * Kif to W3C term
	 *
	 * @param term Kif term
	 * @return W3C term
	 */
	@NotNull
	public static String kifToW3c(@NotNull final String term)
	{
		@NotNull String result = term;
		if (!term.isEmpty() && !isUri(term))
		{
			result = term.replaceFirst(getKifNamespaceDelimiter(), getW3cNamespaceDelimiter());
		}
		return result;
	}

	// safe

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
	@NotNull
	public static String getSafeNamespaceDelimiter()
	{
		return SAFE_NAMESPACE_DELIMITER;
	}

	/**
	 * Replaces the namespace delimiter in term with a string that is safe for inference, and for all common file systems.
	 *
	 * @param term term
	 * @return term with namespace delimiter that is safe for inference
	 */
	@NotNull
	public static String toSafeNamespaceDelimiter(@NotNull final String term)
	{
		if (!term.isEmpty() && !isUri(term))
		{
			@NotNull String kif = getKifNamespaceDelimiter();
			@NotNull String safe = getSafeNamespaceDelimiter();
			@NotNull String w3c = getW3cNamespaceDelimiter();
			@NotNull String result = term.replaceFirst(kif, safe);
			if (!kif.equals(w3c))
			{
				result = result.replaceFirst(w3c, safe);
			}
			return result;
		}
		return term;
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
	@NotNull
	public static String toSafeNamespaceDelimiter(@Nullable final String kbHref, @NotNull final String term)
	{
		if (kbHref == null || kbHref.isEmpty())
		{
			return toSafeNamespaceDelimiter(term);
		}
		return term;
	}

	// local

	/**
	 * Test whether term is local term reference
	 *
	 * @param term term
	 * @return whether term is local term reference
	 */
	public static boolean isLocalTermReference(@NotNull final String term)
	{
		boolean result = false;
		if (!term.isEmpty())
		{
			@NotNull List<String> blankNodeTokens = Arrays.asList("#", "~", getLocalReferenceBaseName());
			for (@NotNull String bnt : blankNodeTokens)
			{
				result = (term.startsWith(bnt) && !term.matches(".*\\s+.*"));
				if (result)
				{
					break;
				}
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
	@NotNull
	public static String getLocalReferenceBaseName()
	{
		return LOCAL_REF_BASE_NAME;
	}

	// L I N E   W R A P

	/**
	 * Wrap input
	 *
	 * @param str    input
	 * @param length line length
	 * @return wrapped input
	 */
	@NotNull
	public static String wordWrap(@NotNull final String str, int length)
	{
		if (length > 0 && str.length() > length)
		{
			String ls = System.getProperty("line.separator");
			int lsLen = ls.length();
			@NotNull StringBuilder sb = new StringBuilder(str);
			int j = length;
			int i = 0;
			while (sb.length() > j)
			{
				while (j > i && !Character.isWhitespace(sb.charAt(j)))
				{
					j--;
				}
				if (j > i)
				{
					sb.deleteCharAt(j);
					sb.insert(j, ls);
					i = j + lsLen;
					j = i + length;
				}
				else
				{
					j += length;
					while (j < sb.length() && !Character.isWhitespace(sb.charAt(j)))
					{
						j++;
					}
					if (j < sb.length())
					{
						sb.deleteCharAt(j);
						sb.insert(j, ls);
						i = j + lsLen;
						j = i + length;
					}
				}
			}
			return sb.toString();
		}
		return str;
	}

	/**
	 * Convenience method to wrap input with default line length of 75
	 *
	 * @param str input
	 * @return wrapped input
	 */
	@NotNull
	public static String wordWrap(@NotNull final String str)
	{
		return wordWrap(str, 75);
	}

	// L I S T

	/**
	 * Assemble elements to list formula string.
	 *
	 * @param lits A List representing lits.
	 * @return A list formula string or empty string
	 */
	@NotNull
	public static String makeForm(@Nullable final List<String> lits)
	{
		if (lits != null)
		{
			return Formula.LP + String.join(Formula.SPACE, lits) + Formula.RP;
		}
		return "";
	}
}
