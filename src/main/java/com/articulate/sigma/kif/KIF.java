/* This code is copyright Articulate Software (c) 2003.  Some portions
copyright Teknowledge (c) 2003 and reused under the terms of the GNU license.
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software
and Teknowledge in any writings, briefings, publications, presentations, or
other representations of any software which incorporates, builds on, or uses this
code.  Please cite the following article in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment,
in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico.  See also http://sigmakee.sourceforget.net
*/
package com.articulate.sigma.kif;

import com.articulate.sigma.Formula;
import com.articulate.sigma.KBManager;
import com.articulate.sigma.StringUtil;

import java.io.*;
import java.text.ParseException;
import java.util.*;
import java.util.logging.Logger;

/**
 * A class designed to read a file in SUO-KIF format into memory.
 * See &lt;http://suo.ieee.org/suo-kif.html&gt; for a language specification.
 * readFile() and writeFile() are the primary methods.
 *
 * @author Adam Pease
 */
public class KIF implements Serializable
{
	private static final long serialVersionUID = -7400641288078157956L;

	private static final Logger logger = Logger.getLogger(KIF.class.getName());

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
	public final SortedSet<String> terms = new TreeSet<>();

	/**
	 * A Map of Lists of Formulas.  @see KIF.createKey for key format.
	 */
	public final Map<String, List<Formula>> formulas = new HashMap<>();

	/**
	 * A "raw" Set of unique Strings which are the formulas from the file without any further processing, in the order which they appear in the file.
	 */
	public final Set<String> formulaSet = new LinkedHashSet<>();

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
	public static int count = 0;

	/**
	 * Lines for comments
	 */
	private int totalLinesForComments = 0;

	/**
	 * Warnings generated during parsing
	 */
	public final SortedSet<String> warningSet = new TreeSet<>();

	/**
	 * Constructor
	 */
	public KIF()
	{
	}

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

	/**
	 * This routine sets up the StreamTokenizer_s so that it parses SUO-KIF.
	 * = &lt; $gt; are treated as word characters, as are normal alphanumerics.
	 * ; is the line comment character and " is the quote character.
	 *
	 * @param st stream tokenizer
	 */
	public static void setupStreamTokenizer(StreamTokenizer_s st)
	{
		st.whitespaceChars(0, 32);
		st.ordinaryChars(33, 44);    // !"#$%&'()*+,
		st.wordChars(45, 46);        // -.
		st.ordinaryChar(47);                // /
		st.wordChars(48, 58);        // 0-9:
		st.ordinaryChar(59);                // ;
		st.wordChars(60, 64);        // <=>?@
		st.wordChars(65, 90);        // A-Z
		st.ordinaryChars(91, 94);    // [\]^
		st.wordChars(95, 95);        // _
		st.ordinaryChar(96);                // `
		st.wordChars(97, 122);        // a-z
		st.ordinaryChars(123, 255);    // {|}~
		st.quoteChar('"');
		st.commentChar(';');
		st.eolIsSignificant(true);
	}

	/**
	 * This method has the side effect of setting the contents of formulaSet and formulas as it parses the file.
	 *
	 * @param reader reader
	 * @return a Set of warnings that may indicate syntax errors, but not fatal parse errors.It throws a
	 * ParseException with file line numbers if fatal errors are encountered during parsing.
	 */
	@SuppressWarnings("UnusedReturnValue") protected Set<String> parse(Reader reader)
	{
		logger.entering("KIF", "parse");
		int mode = this.getParseMode();
		logger.info("Parsing " + this.getFilename() + " with parseMode = " + ((mode == RELAXED_PARSE_MODE) ? "RELAXED_PARSE_MODE" : "NORMAL_PARSE_MODE"));

		if (reader == null)
		{
			String errStr = "No Input Reader Specified";
			logger.warning("No input string reader specified.");
			warningSet.add(errStr);
			System.err.println(errStr);
			logger.warning("Exiting KIF.parse without doing anything.");
			return warningSet;
		}
		StringBuilder expression = new StringBuilder();
		String errStart = "Parsing error in " + filename;
		int duplicateCount = 0;
		try
		{
			count++;
			StreamTokenizer_s st = new StreamTokenizer_s(reader);
			KIF.setupStreamTokenizer(st);
			int parenLevel = 0;
			boolean inRule = false;
			int argumentNum = -1;
			boolean inAntecedent = false;
			boolean inConsequent = false;
			Formula f = new Formula();
			Set<String> keySet = new HashSet<>();
			boolean isEOL = false;
			do
			{
				int lastVal = st.ttype;
				st.nextToken();
				// Check the situation when multiple KIF statements read as one
				// This relies on extra blank line to separate KIF statements
				if (st.ttype == StreamTokenizer.TT_EOL)
				{
					if (isEOL)
					{
						// Two line separators in a row, shows a new KIF statement is to start.  check if a new statement
						// has already been generated, otherwise report error
						if (!keySet.isEmpty() || (expression.length() > 0))
						{
							String errStr = errStart + ": possible missed closing parenthesis near line " + f.startLine;
							logger.warning(errStr);
							logger.fine("st.sval=" + st.sval);
							int eLen = expression.length();
							if (eLen > 300)
								logger.fine("expression == ... " + expression.substring(eLen - 300));
							else
								logger.fine("expression == " + expression.toString());
							throw new ParseException(errStr, f.startLine);
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
					isEOL = false;
				if (st.ttype == 40)
				{
					// Turn off isEOL if a non-space token encountered
					// Open paren
					if (parenLevel == 0)
					{
						f = new Formula();
						f.startLine = st.lineno() + totalLinesForComments;
						f.sourceFile = filename;
					}
					parenLevel++;
					if (inRule && !inAntecedent && !inConsequent)
						inAntecedent = true;
					else
					{
						if (inRule && inAntecedent && (parenLevel == 2))
						{
							inAntecedent = false;
							inConsequent = true;
						}
					}
					if ((parenLevel != 0) && (lastVal != 40) && (expression.length() > 0))
						expression.append(" ");   // add back whitespace that ST removes
					expression.append("(");
				}
				else if (st.ttype == 41)
				{
					// )  - Close paren
					parenLevel--;
					expression.append(")");
					if (parenLevel == 0)
					{
						// The end of the statement...
						String fStr = StringUtil.normalizeSpaceChars(expression.toString());
						f.text = StringUtil.replaceDateTime(fStr).intern();
						if (formulaSet.contains(f.text))
						{
							String warning = ("WARNING: Duplicate formula at line " + f.startLine + " of " + f.sourceFile + ": " + expression);
							//lineStart + totalLinesForComments + expression;
							warningSet.add(warning);
							System.err.println(warning);
							duplicateCount++;
						}
						// Check argument validity ONLY if we are in NORMAL_PARSE_MODE.
						if (mode == NORMAL_PARSE_MODE)
						{
							String validArgs = f.validArgs((file != null ? file.getName() : null), (file != null ? f.startLine : null));
							if (validArgs == null || validArgs.isEmpty())
								validArgs = f.badQuantification();
							if (!validArgs.isEmpty())
							{
								String errStr = errStart + ": Invalid number of arguments near line " + f.startLine;
								logger.warning(errStr);
								logger.fine("st.sval = " + st.sval);
								int eLen = expression.length();
								if (eLen > 300)
									logger.fine("expression == ... " + expression.substring(eLen - 300));
								else
									logger.fine("expression == " + expression.toString());
								throw new ParseException(errStr, f.startLine);
							}
						}
						// Make the formula itself a key
						keySet.add(f.text);
						keySet.add(f.createID());
						f.endLine = st.lineno() + totalLinesForComments;
						for (String fKey : keySet)
						{
							// Add the expression but ...
							if (formulas.containsKey(fKey))
							{
								if (!formulaSet.contains(f.text))
								{
									// don't add keys if formula is already present
									List<Formula> list = formulas.get(fKey);
									if (!list.contains(f))
										list.add(f);
								}
							}
							else
							{
								List<Formula> list = new ArrayList<>();
								list.add(f);
								formulas.put(fKey, list);
							}
						}
						formulaSet.add(f.text);
						inConsequent = false;
						inRule = false;
						argumentNum = -1;
						expression = new StringBuilder();
						keySet.clear();
					}
					else if (parenLevel < 0)
					{
						String errStr = errStart + ": Extra closing parenthesis found near line " + f.startLine;
						logger.warning(errStr);
						logger.fine("st.sval = " + st.sval);
						int eLen = expression.length();
						if (eLen > 300)
							logger.fine("expression == ... " + expression.substring(eLen - 300));
						else
							logger.fine("expression == " + expression.toString());
						throw new ParseException(errStr, f.startLine);
					}
				}
				else if (st.ttype == 34)
				{
					// " - It's a string
					st.sval = StringUtil.escapeQuoteChars(st.sval);
					if (lastVal != 40) // Add back whitespace that ST removes
						expression.append(" ");
					expression.append("\"");
					String com = st.sval;
					totalLinesForComments += countChar(com, (char) 0X0A);
					expression.append(com);
					expression.append("\"");
					if (parenLevel < 2)   // Don't care if parenLevel > 1
						argumentNum = argumentNum + 1;
				}
				else if ((st.ttype == StreamTokenizer.TT_NUMBER) ||           // number
						(st.sval != null && (Character.isDigit(st.sval.charAt(0)))))
				{
					if (lastVal != 40) // add back whitespace that ST removes
						expression.append(" ");
					if (st.nval == 0)
						expression.append(st.sval);
					else
						expression.append(st.nval);
					if (parenLevel < 2) // Don't care if parenLevel > 1
						argumentNum = argumentNum + 1; // RAP - added on 11/27/04
				}
				else if (st.ttype == StreamTokenizer.TT_WORD)
				{
					// A token
					if (("=>".equals(st.sval) || "<=>".equals(st.sval)) && parenLevel == 1)
						// RAP - added parenLevel clause on 11/27/04 to
						// Prevent implications embedded in statements from being rules
						inRule = true;
					if (parenLevel < 2) // Don't care if parenLevel > 1
						argumentNum = argumentNum + 1;
					if (lastVal != 40) // Add back whitespace that ST removes
						expression.append(" ");
					expression.append(st.sval);
					if (expression.length() > 64000)
					{
						String errStr = errStart + ": Sentence over 64000 characters new line " + f.startLine;
						logger.warning(errStr);
						logger.fine("st.sval = " + st.sval);
						int eLen = expression.length();
						if (eLen > 300)
							logger.fine("expression == ... " + expression.substring(eLen - 300));
						else
							logger.fine("expression == " + expression.toString());
						throw new ParseException(errStr, f.startLine);
					}
					// Build the terms list and create special keys ONLY if we are in NORMAL_PARSE_MODE.
					if ((mode == NORMAL_PARSE_MODE) && (st.sval.charAt(0) != '?') && (st.sval.charAt(0) != '@'))
					{
						// Variables are not terms
						terms.add(st.sval); // Collect all terms
						String key = createKey(st.sval, inAntecedent, inConsequent, argumentNum, parenLevel);
						keySet.add(key); // Collect all the keys until the end of the statement is reached.
					}
				}
				else if ((mode == RELAXED_PARSE_MODE) && (st.ttype == 96))
				{
					// AB: 5/2007 - allow '`' in relaxed parse mode.
					expression.append(" `");
				}
				else if (st.ttype != StreamTokenizer.TT_EOF)
				{
					String errStr = errStart + ": Illegal character near line " + f.startLine;
					logger.warning(errStr);
					logger.fine("st.sval = " + st.sval);
					int eLen = expression.length();
					if (eLen > 300)
						logger.fine("expression == ... " + expression.substring(eLen - 300));
					else
						logger.fine("expression == " + expression.toString());
					throw new ParseException(errStr, f.startLine);
				}
			}
			while (st.ttype != StreamTokenizer.TT_EOF);

			if (!keySet.isEmpty() || expression.length() > 0)
			{
				String errStr = errStart + ": Missed closing parenthesis near line " + f.startLine;
				logger.warning(errStr);
				logger.fine("st.sval == " + st.sval);
				int eLen = expression.length();
				if (eLen > 300)
					logger.fine("expression == ... " + expression.substring(eLen - 300));
				else
					logger.fine("expression == " + expression.toString());
				throw new ParseException(errStr, f.startLine);
			}
		}
		catch (Exception ex)
		{
			warningSet.add("Error in KIF.parse(): " + ex.getMessage());
			logger.severe("Error in KIF.parse(): " + ex.getMessage());
			logger.severe("Error: " + Arrays.toString(ex.getStackTrace()));
			ex.printStackTrace();
		}
		if (duplicateCount > 0)
		{
			String warning = "WARNING in KIF.parse(Reader): " + duplicateCount + " duplicate statement" + (duplicateCount > 1 ? "s " : " ") + //
					"detected in " + (filename == null || filename.isEmpty() ? " the input file" : filename);
			logger.warning(warning);
		}
		if (!warningSet.isEmpty())
		{
			for (String w : warningSet)
			{
				logger.finer(w.matches("^(?i)Error.+") ? w : ("WARNING in KIF.parse(): " + w));
			}
		}
		logger.exiting("KIF", "parse");
		return warningSet;
	}

	/**
	 * This routine creates a key that relates a token in a
	 * logical statement to the entire statement.  It prepends
	 * to the token a string indicating its position in the
	 * statement.  The key is of the form type-[num]-term, where [num]
	 * is only present when the type is "arg", meaning a statement in which
	 * the term is nested only within one pair of parentheses.  The other
	 * possible types are "ant" for rule antecedent, "cons" for rule consequent,
	 * and "stmt" for cases where the term is nested inside multiple levels of
	 * parentheses.  An example key would be arg-0-instance for a appearance of
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
	private String createKey(String sVal, boolean inAntecedent, boolean inConsequent, int argumentNum, int parenLevel)
	{
		if (sVal == null)
		{
			sVal = "null";
		}
		String key = "";
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
	private int countChar(String str, @SuppressWarnings("SameParameterValue") char c)
	{
		int len = 0;
		char[] cArray = str.toCharArray();
		for (char value : cArray)
		{
			if (value == c)
				len++;
		}
		return len;
	}

	/**
	 * Read a KIF file.
	 *
	 * @param fileName - the full pathname of the file.
	 * @throws Exception exception
	 */
	public void readFile(String fileName) throws Exception
	{
		logger.entering("KIF", "readFile", fileName);

		Exception exThr = null;
		this.file = new File(fileName);
		this.filename = file.getCanonicalPath();
		try (FileReader fr = new FileReader(this.file))
		{
			parse(fr);
		}
		catch (Exception ex)
		{
			exThr = ex;
			String er = ex.getMessage();
			logger.severe("ERROR in KIF.readFile(\"" + fileName + "\"):" + "  " + er);
			KBManager.getMgr().setError(KBManager.getMgr().getError() + "\n<br/>" + er + " in file " + fileName + "\n<br/>");
		}
		logger.exiting("KIF", "readFile");
		if (exThr != null)
			throw exThr;
	}
}


