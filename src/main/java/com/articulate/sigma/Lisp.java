package com.articulate.sigma;

public class Lisp
{
	/**
	 * Car
	 *
	 * @param form formula string
	 * @return the LISP 'car' as a String - the first
	 * element of the list.
	 * Currently (10/24/2007), this method returns the empty string
	 * ("") when invoked on an empty list.  Technically, this is
	 * wrong.  In most LISPS, the car of the empty list is the empty
	 * list (or nil).  But some parts of the Sigma code apparently
	 * expect this method to return the empty string when invoked on
	 * an empty list.
	 */
	@NotNull
	public static String car(@NotNull final String form)
	{
		// logger.entering(LOG_SOURCE, "car");
		if (listP(form))
		{
			if (empty(form))
			{
				// logger.exiting(LOG_SOURCE, "car", "\"\", was empty list");
				return "";
			}
			else
			{
				@NotNull StringBuilder sb = new StringBuilder();
				@NotNull String input = form.trim();
				int level = 0;
				char prev = '0';
				char quoteCharInForce = '0';
				boolean insideQuote = false;

				for (int i = 1, len = input.length(), end = len - 1; i < end; i++)
				{
					char ch = input.charAt(i);
					if (!insideQuote)
					{
						if (ch == '(')
						{
							sb.append(ch);
							level++;
						}
						else if (ch == ')')
						{
							sb.append(ch);
							level--;
							if (level <= 0)
							{
								break;
							}
						}
						else if (Character.isWhitespace(ch) && level <= 0)
						{
							if (sb.length() > 0)
							{
								break;
							}
						}
						else if (Formula.QUOTE_CHARS.contains(ch) && prev != '\\')
						{
							sb.append(ch);
							insideQuote = true;
							quoteCharInForce = ch;
						}
						else
						{
							sb.append(ch);
						}
					}
					else if (Formula.QUOTE_CHARS.contains(ch) && ch == quoteCharInForce && prev != '\\')
					{
						sb.append(ch);
						insideQuote = false;
						quoteCharInForce = '0';
						if (level <= 0)
						{
							break;
						}
					}
					else
					{
						sb.append(ch);
					}
					prev = ch;
				}
				@NotNull String result = sb.toString();
				// logger.exiting(LOG_SOURCE, "car", result);
				return result;
			}
		}
		// logger.exiting(LOG_SOURCE, "car", "\"\", was not a list");
		return "";
	}

	/**
	 * Cdr
	 *
	 * @param form formula string
	 * @return the LISP 'cdr' - the rest of a list minus its
	 * first element.
	 */
	@NotNull
	public static String cdr(@NotNull final String form)
	{
		// logger.entering(LOG_SOURCE, "cdr");
		if (listP(form))
		{
			if (empty(form))
			{
				// logger.exiting(LOG_SOURCE, "cdr", form + ", was empty list");
				return form;
			}
			else
			{
				@NotNull String input = form.trim();
				int level = 0;
				char prev = '0';
				char quoteCharInForce = '0';
				boolean insideQuote = false;
				int carCount = 0;

				int i = 1, len = input.length(), end = len - 1;
				for (; i < end; i++)
				{
					char ch = input.charAt(i);
					if (!insideQuote)
					{
						if (ch == '(')
						{
							carCount++;
							level++;
						}
						else if (ch == ')')
						{
							carCount++;
							level--;
							if (level <= 0)
							{
								break;
							}
						}
						else if (Character.isWhitespace(ch) && (level <= 0))
						{
							if (carCount > 0)
							{
								break;
							}
						}
						else if (Formula.QUOTE_CHARS.contains(ch) && (prev != '\\'))
						{
							carCount++;
							insideQuote = true;
							quoteCharInForce = ch;
						}
						else
						{
							carCount++;
						}
					}
					else if (Formula.QUOTE_CHARS.contains(ch) && (ch == quoteCharInForce) && (prev != '\\'))
					{
						carCount++;
						insideQuote = false;
						quoteCharInForce = '0';
						if (level <= 0)
						{
							break;
						}
					}
					else
					{
						carCount++;
					}
					prev = ch;

				}
				if (carCount > 0)
				{
					int j = i + 1;
					if (j < end)
					{
						@NotNull @SuppressWarnings("UnnecessaryLocalVariable") String result = "(" + input.substring(j, end).trim() + ")";
						// logger.exiting(LOG_SOURCE, "cdr", result);
						return result;
					}
					else
					{
						// logger.exiting(LOG_SOURCE, "cdr", "(), whole list consumed");
						return "()";
					}
				}
			}
		}
		// logger.exiting(LOG_SOURCE, "cdr", "\"\", was not a list");
		return "";
	}

	/**
	 * Atom
	 *
	 * @param form formula string
	 * @return whether the String is a LISP atom.
	 */
	public static boolean atom(@NotNull final String form)
	{
		if (!form.isEmpty())
		{
			@NotNull String form2 = form.trim();
			return StringUtil.isQuotedString(form2) || (!form2.contains(")") && !form2.matches(".*\\s.*"));
		}
		return false;
	}

	/**
	 * ListP
	 *
	 * @param form formula string
	 * @return whether the String is a list.
	 */
	public static boolean listP(@NotNull final String form)
	{
		if (!form.isEmpty())
		{
			@NotNull String form2 = form.trim();
			return form2.startsWith("(") && form2.endsWith(")");
		}
		return false;
	}

	/**
	 * Empty
	 *
	 * @param form formula string
	 * @return whether the String is an empty formula.  Not to be
	 * confused with a null string or empty string.  There must be
	 * parentheses with nothing or whitespace in the middle.
	 */
	public static boolean empty(@NotNull final String form)
	{
		return listP(form) && form.matches("\\(\\s*\\)");
	}

	/**
	 * Return the numbered argument of the given formula.  The first
	 * element of a formula (i.e. the predicate position) is number 0.
	 * Returns the empty string if there is no such argument position.
	 *
	 * @param form   form
	 * @param argNum argument number
	 * @return numbered argument.
	 */
	@NotNull
	public static String getArgument(@NotNull final String form, int argNum)
	{
		int i = 0;
		for (@NotNull IterableFormula f = new IterableFormula(form); !f.empty(); f.pop())
		{
			if (i == argNum)
			{
				return f.car();
			}
			i++;
		}
		return "";
	}
}
