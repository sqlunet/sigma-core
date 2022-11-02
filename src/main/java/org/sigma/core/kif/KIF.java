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

import org.sigma.core.*;

import java.io.*;
import java.text.ParseException;
import java.util.*;
import java.util.logging.Logger;

/**
 * A class designed to read a file in SUO-KIF format into memory.
 * See <a href="http://suo.ieee.org/suo-kif.html">suo-kif.html</a>; for a language specification.
 * readFile() and writeFile() are the primary methods.
 *
 * @author Adam Pease
 */
public class KIF implements Serializable
{
	private static final long serialVersionUID = -7400641288078157956L;

	private static final String LOG_SOURCE = "KIF";

	private static final Logger LOGGER = Logger.getLogger(KIF.class.getName());

	/**
	 * A numeric constant denoting normal parse mode, in which syntax constraints are enforced.
	 */
	public static final int NORMAL_PARSE_MODE = 1;

	/**
	 * A numeric constant denoting relaxed parse mode, in which fewer syntax constraints are enforced than in NORMAL_PARSE_MODE.
	 */
	public static final int RELAXED_PARSE_MODE = 2;

	/**
	 * Parse mode
	 */
	public final int parseMode = NORMAL_PARSE_MODE;

	/**
	 * The set of all terms in the knowledge base.  This is a set of Strings.
	 */
	public final Set<String> terms = new TreeSet<>();

	/**
	 * A Map of Lists of Formulas.  @see KIF.createKey for key format.
	 */
	public final Map<String, List<Formula>> formulaIndex = new HashMap<>();

	/**
	 * A "raw" Set of unique Strings which are the formulas from the file without any further processing, in the order which they appear in the file.
	 */
	public final Set<String> formulas = new LinkedHashSet<>();

	/**
	 * File name
	 */
	private String filename;

	/**
	 * File
	 */
	private File file;

	/**
	 * Count
	 */
	public int count = 0;

	/**
	 * Lines for comments
	 */
	private int totalLinesForComments = 0;

	/**
	 * Warnings generated during parsing
	 */
	public final Set<String> warnings = new TreeSet<>();

	// C O N S T R U C T

	/**
	 * Constructor
	 */
	public KIF()
	{
	}

	// A C C E S S

	/**
	 * Get file name
	 *
	 * @return file name
	 */
	public String getFilename()
	{
		return this.filename;
	}

	/**
	 * @return int Returns an integer value denoting the current parse
	 * mode.
	 */
	public int getParseMode()
	{
		return this.parseMode;
	}

	// P A R S E

	/**
	 * This method has the side effect of setting the contents of formulaSet and formulas as it parses the file.
	 *
	 * @param reader reader
	 * @return a Set of warnings that may indicate syntax errors, but not fatal parse errors.It throws a
	 * ParseException with file line numbers if fatal errors are encountered during parsing.
	 */
	@NotNull
	@SuppressWarnings("UnusedReturnValue")
	protected Set<String> parse(@Nullable final Reader reader)
	{
		LOGGER.entering(LOG_SOURCE, "parse");
		int mode = getParseMode();
		LOGGER.finer("Parsing " + this.getFilename() + " with parseMode = " + ((mode == RELAXED_PARSE_MODE) ? "RELAXED_PARSE_MODE" : "NORMAL_PARSE_MODE"));

		if (reader == null)
		{
			@NotNull String errStr = "No Input Reader Specified";
			LOGGER.warning(errStr);
			warnings.add(errStr);
			return warnings;
		}

		int duplicateCount = 0;
		try
		{
			@NotNull String errStart = "Parsing error in " + filename;
			@NotNull StringBuilder expression = new StringBuilder();
			@NotNull StreamTokenizer_s tokenizer = new KifTokenizer(reader);

			int startLine = 0;
			int parenLevel = 0;
			boolean inRule = false;
			boolean inAntecedent = false;
			boolean inConsequent = false;
			int argumentNum = -1;

			@NotNull Set<String> keys = new HashSet<>();

			boolean isEOL = false;
			do
			{
				int lastVal = tokenizer.ttype;
				tokenizer.nextToken();

				// Check the situation when multiple KIF statements read as one
				// This relies on extra blank line to separate KIF statements

				// E O L

				if (tokenizer.ttype == StreamTokenizer.TT_EOL)
				{
					if (isEOL)
					{
						// Two line separators in a row, shows a new KIF statement is to start.  check if a new statement
						// has already been generated, otherwise report error
						if (!keys.isEmpty() || expression.length() > 0)
						{
							@NotNull String errStr = errStart + ": possible missed closing parenthesis near line " + startLine;
							LOGGER.warning(errStr);
							LOGGER.fine("st.sval=" + tokenizer.sval);
							int eLen = expression.length();
							if (eLen > 300)
							{
								LOGGER.fine("expression == ... " + expression.substring(eLen - 300));
							}
							else
							{
								LOGGER.fine("expression == " + expression);
							}
							throw new ParseException(errStr, startLine);
						}
					}
					else
					{
						// Found a first end of line character.
						isEOL = true;   // Turn on flag, to watch for a second consecutive one.
					}
					continue;
				}
				else if (isEOL)
				{
					isEOL = false;
				}

				// P A R E N

				if (tokenizer.ttype == '(')
				{
					// Turn off isEOL if a non-space token encountered
					// Open paren
					if (parenLevel == 0)
					{
						startLine = tokenizer.lineno() + totalLinesForComments;
					}
					parenLevel++;
					if (inRule && !inAntecedent && !inConsequent)
					{
						inAntecedent = true;
					}
					else
					{
						if (inRule && inAntecedent && (parenLevel == 2))
						{
							inAntecedent = false;
							inConsequent = true;
						}
					}
					if ((parenLevel != 0) && (lastVal != 40) && (expression.length() > 0))
					{
						expression.append(" ");   // add back whitespace that ST removes
					}
					expression.append("(");
				}
				else if (tokenizer.ttype == ')')
				{
					// )  - Close paren
					parenLevel--;
					expression.append(")");
					if (parenLevel == 0)
					{
						// The end of the statement...
						@NotNull String form = StringUtil.replaceDateTime(StringUtil.normalizeSpaceChars(expression.toString()));
						@NotNull Formula f = Formula.of(form);
						f.startLine = startLine;
						f.endLine = tokenizer.lineno() + totalLinesForComments;
						f.sourceFile = FileUtil.basename(filename);

						if (formulas.contains(f.form))
						{
							@NotNull String warning = ("Duplicate formula in " + filename + ":" + startLine + " " + expression);
							//lineStart + totalLinesForComments + expression;
							warnings.add(warning);
							duplicateCount++;
						}

						// Check argument validity ONLY if we are in NORMAL_PARSE_MODE.
						if (mode == NORMAL_PARSE_MODE)
						{
							@Nullable String errors = f.hasValidArgs((file != null ? file.getName() : null), file != null ? startLine : null);
							if (errors != null)
							{
								errors = f.hasValidQuantification();
							}
							if (errors != null)
							{
								@NotNull String errStr = errStart + ": Invalid number of arguments near line " + startLine;
								LOGGER.warning(errStr);
								LOGGER.fine("st.sval = " + tokenizer.sval);
								int eLen = expression.length();
								if (eLen > 300)
								{
									LOGGER.fine("expression == ... " + expression.substring(eLen - 300));
								}
								else
								{
									LOGGER.fine("expression == " + expression);
								}
								throw new ParseException(errStr, startLine);
							}
						}

						// Index formula
						keys.add(f.form); // Make the formula itself a key
						keys.add(f.createID());
						for (String key : keys)
						{
							@NotNull List<Formula> values = formulaIndex.computeIfAbsent(key, k -> new ArrayList<>());
							if (!values.contains(f))
							{
								values.add(f);
							}
						}
						formulas.add(f.form);
						count++;

						inConsequent = false;
						inRule = false;
						argumentNum = -1;
						expression = new StringBuilder();
						keys.clear();

						// progress
						if (count % 1000 == 1)
						{
							FileUtil.PROGRESS_OUT.print('.');
						}
					}
					else if (parenLevel < 0)
					{
						@NotNull String errStr = errStart + ": Extra closing parenthesis found near line " + startLine;
						LOGGER.warning(errStr);
						LOGGER.fine("st.sval = " + tokenizer.sval);
						int eLen = expression.length();
						if (eLen > 300)
						{
							LOGGER.fine("expression == ... " + expression.substring(eLen - 300));
						}
						else
						{
							LOGGER.fine("expression == " + expression);
						}
						throw new ParseException(errStr, startLine);
					}
				}

				// D O U B L E  Q U O T E

				else if (tokenizer.ttype == '"')
				{
					// " - It's a string
					if (tokenizer.sval != null)
					{
						tokenizer.sval = StringUtil.escapeQuoteChars(tokenizer.sval);
					}
					if (lastVal != 40) // Add back whitespace that ST removes
					{
						expression.append(" ");
					}
					expression.append("\"");

					@Nullable String comment = tokenizer.sval;
					if (comment != null)
					{
						totalLinesForComments += countChar(comment, (char) 0X0A);
						expression.append(comment);
					}

					expression.append("\"");
					if (parenLevel < 2)   // Don't care if parenLevel > 1
					{
						argumentNum = argumentNum + 1;
					}
				}

				// N U M B E R

				else if (tokenizer.ttype == StreamTokenizer.TT_NUMBER || (tokenizer.sval != null && Character.isDigit(tokenizer.sval.charAt(0))))
				{
					if (lastVal != 40) // add back whitespace that ST removes
					{
						expression.append(" ");
					}
					if (tokenizer.nval == 0)
					{
						expression.append(tokenizer.sval);
					}
					else
					{
						expression.append(tokenizer.nval);
					}
					if (parenLevel < 2) // Don't care if parenLevel > 1
					{
						argumentNum = argumentNum + 1;
					}
				}

				// W O R D

				else if (tokenizer.ttype == StreamTokenizer.TT_WORD)
				{
					// A token
					// parenLevel clause to prevent implications embedded in statements from being rules
					if ((Formula.IF.equals(tokenizer.sval) || Formula.IFF.equals(tokenizer.sval)) && parenLevel == 1)
					{
						inRule = true;
					}
					if (parenLevel < 2) // Don't care if parenLevel > 1
					{
						argumentNum = argumentNum + 1;
					}
					if (lastVal != 40) // Add back whitespace that ST removes
					{
						expression.append(" ");
					}
					expression.append(tokenizer.sval);
					if (expression.length() > 64000)
					{
						@NotNull String errStr = errStart + ": Sentence over 64000 characters new line " + startLine;
						LOGGER.warning(errStr);
						LOGGER.fine("tokenizer.sval = " + tokenizer.sval);
						int eLen = expression.length();
						if (eLen > 300)
						{
							LOGGER.fine("expression == ... " + expression.substring(eLen - 300));
						}
						else
						{
							LOGGER.fine("expression == " + expression);
						}
						throw new ParseException(errStr, startLine);
					}
					// Build the terms list and create special keys ONLY if we are in NORMAL_PARSE_MODE.
					if (mode == NORMAL_PARSE_MODE && tokenizer.sval.charAt(0) != Formula.V_PREFIX.charAt(0) && tokenizer.sval.charAt(0) != Formula.R_PREFIX.charAt(0))
					{
						// Variables are not terms
						terms.add(tokenizer.sval);

						// Collect all terms
						@NotNull String key = createKey(tokenizer.sval, inAntecedent, inConsequent, argumentNum, parenLevel);
						keys.add(key); // Collect all the keys until the end of the statement is reached.
					}
				}

				// B A C K Q U O T E

				else if (mode == RELAXED_PARSE_MODE && tokenizer.ttype == '`')
				{
					// allow '`' in relaxed parse mode.
					expression.append(" `");
				}

				// O T H E R

				else if (tokenizer.ttype != StreamTokenizer.TT_EOF)
				{
					@NotNull String errStr = errStart + ": Illegal character near line " + startLine;
					LOGGER.warning(errStr);
					LOGGER.fine("st.sval = " + tokenizer.sval);
					int eLen = expression.length();
					if (eLen > 300)
					{
						LOGGER.fine("expression == ... " + expression.substring(eLen - 300));
					}
					else
					{
						LOGGER.fine("expression == " + expression);
					}
					throw new ParseException(errStr, startLine);
				}
			}
			while (tokenizer.ttype != StreamTokenizer.TT_EOF);

			if (!keys.isEmpty() || expression.length() > 0)
			{
				@NotNull String errStr = errStart + ": Missed closing parenthesis near line " + startLine;
				LOGGER.warning(errStr);
				LOGGER.fine("st.sval == " + tokenizer.sval);
				int eLen = expression.length();
				if (eLen > 300)
				{
					LOGGER.fine("expression == ... " + expression.substring(eLen - 300));
				}
				else
				{
					LOGGER.fine("expression == " + expression);
				}
				throw new ParseException(errStr, startLine);
			}
		}
		catch (IOException | ParseException ex)
		{
			warnings.add("Error in KIF.parse(): " + ex.getMessage());
			LOGGER.severe("Error in KIF.parse(): " + ex.getMessage());
			LOGGER.severe("Error: " + Arrays.toString(ex.getStackTrace()));
			ex.printStackTrace();
		}
		if (duplicateCount > 0)
		{
			@NotNull String errStr = "Duplicates in KIF.parse(Reader): " + duplicateCount + " duplicate statement" + (duplicateCount > 1 ? "s " : " ") + " detected in " + (filename == null || filename.isEmpty() ? " the input file" : filename);
			LOGGER.warning(errStr);
		}
		if (!warnings.isEmpty())
		{
			for (@NotNull String w : warnings)
			{
				LOGGER.finer(w.matches("^(?i)Error.+") ? w : (" in KIF.parse(): " + w));
			}
		}
		FileUtil.PROGRESS_OUT.println();
		LOGGER.finer(String.format("count=%d formulas=%d index-k=%d index-v=%d index-distinctv=%d%n", count, formulas.size(), formulaIndex.size(), formulaIndex.values().stream().mapToInt(List::size).sum(), formulaIndex.values().stream().flatMap(Collection::stream).distinct().count()));
		LOGGER.exiting(LOG_SOURCE, "parse");
		return warnings;
	}

	/**
	 * Read a KIF file.
	 *
	 * @param fileName - the full pathname of the file.
	 * @throws Exception exception
	 */
	public void readFile(@NotNull String fileName) throws Exception
	{
		LOGGER.entering(LOG_SOURCE, "readFile", fileName);

		this.file = new File(fileName);
		this.filename = file.getCanonicalPath();
		try (@NotNull FileReader fr = new FileReader(this.file))
		{
			parse(fr);
		}
		catch (Exception ex)
		{
			String errStr = ex.getMessage();
			LOGGER.severe("ERROR in KIF.readFile(\"" + fileName + "\"):" + "  " + errStr);
			throw ex;
		}
		finally
		{
			LOGGER.exiting(LOG_SOURCE, "readFile");
		}
	}

	// H E L P E R S

	/**
	 * This routine creates a key that relates a token in a
	 * logical statement to the entire statement.  It prepends
	 * to the token a string indicating its position in the
	 * statement.  The key is of the form type-[num]-term, where [num]
	 * is only present when the type is "arg", meaning a statement in which
	 * the term is nested only within one pair of parentheses.  The other
	 * possible types are "ant" for rule antecedent, "cons" for rule consequent,
	 * and "stmt" for cases where the term is nested inside multiple levels of
	 * parentheses.  An example key would be arg-0-instance for an appearance of
	 * the term "instance" in a statement in the predicate position.
	 *
	 * @param sVal         - the token such as "instance", "Human" etc.
	 * @param inAntecedent - whether the term appears in the antecedent of a rule.
	 * @param inConsequent - whether the term appears in the consequent of a rule.
	 * @param argumentNum  - the argument position in which the term appears.  The
	 *                     predicate position is argument 0.  The first argument is 1 etc.
	 * @param parenLevel   - if the paren level is > 1 then the term appears nested
	 *                     in a statement and the argument number is ignored.
	 */
	@NotNull
	private String createKey(@Nullable String sVal, boolean inAntecedent, boolean inConsequent, int argumentNum, int parenLevel)
	{
		if (sVal == null)
		{
			sVal = "null";
		}
		@NotNull String key = "";
		if (inAntecedent)
		{
			key = key.concat("ant-");
			key = key.concat(sVal);
		}
		if (inConsequent)
		{
			key = key.concat("cons-");
			key = key.concat(sVal);
		}
		if (!inAntecedent && !inConsequent && (parenLevel == 1))
		{
			key = key.concat("arg-");
			key = key.concat(String.valueOf(argumentNum));
			key = key.concat("-");
			key = key.concat(sVal);
		}
		if (!inAntecedent && !inConsequent && (parenLevel > 1))
		{
			key = key.concat("stmt-");
			key = key.concat(sVal);
		}
		return (key);
	}

	/**
	 * Count the number of appearances of a certain character in a string.
	 *
	 * @param str - the string to be tested.
	 * @param c   - the character to be counted.
	 */
	private int countChar(@NotNull String str, @SuppressWarnings("SameParameterValue") char c)
	{
		int len = 0;
		@NotNull char[] cArray = str.toCharArray();
		for (char value : cArray)
		{
			if (value == c)
			{
				len++;
			}
		}
		return len;
	}
}