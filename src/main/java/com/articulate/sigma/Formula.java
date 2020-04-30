/* This code is copyright Articulate Software (c) 2003.  Some
portions copyright Teknowledge (c) 2003 and reused under the terms of
the GNU license.  This software is released under the GNU Public
License &lt;http://www.gnu.org/copyleft/gpl.html&gt;.  Users of this code
also consent, by use of this code, to credit Articulate Software and
Teknowledge in any writings, briefings, publications, presentations,
or other representations of any software which incorporates, builds
on, or uses this code.  Please cite the following article in any
publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment, in
Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed
Systems, August 9, Acapulco, Mexico. See also http://sigmakee.sourceforge.net
*/

package com.articulate.sigma;

import java.io.File;
import java.io.Serializable;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handle operations on an individual formula.  This includes formatting.
 */
public class Formula implements Comparable<Formula>, Serializable
{
	private static final long serialVersionUID = 1L;

	private static final Logger logger = Logger.getLogger(Formula.class.getName());

	protected static final String AND = "and";
	protected static final String OR = "or";
	protected static final String NOT = "not";
	protected static final String IF = "=>";
	protected static final String IFF = "<=>";
	protected static final String UQUANT = "forall";
	protected static final String EQUANT = "exists";
	protected static final String EQUAL = "equal";
	protected static final String GT = "greaterThan";
	protected static final String GTET = "greaterThanOrEqualTo";
	protected static final String LT = "lessThan";
	protected static final String LTET = "lessThanOrEqualTo";

	protected static final String PLUSFN = "AdditionFn";
	protected static final String MINUSFN = "SubtractionFn";
	protected static final String TIMESFN = "MultiplicationFn";
	protected static final String DIVIDEFN = "DivisionFn";
	protected static final String SKFN = "SkFn";
	protected static final String SK_PREF = "Sk";
	protected static final String FN_SUFF = "Fn";
	protected static final String V_PREF = "?";
	protected static final String R_PREF = "@";
	protected static final String VX = "?X";
	protected static final String VVAR = "?VAR";
	protected static final String RVAR = "@ROW";

	protected static final String LP = "(";
	protected static final String RP = ")";
	protected static final String SPACE = " ";

	protected static final String LOG_FALSE = "False";

	/**
	 * The SUO-KIF logical operators.
	 */
	public static final List<String> LOGICAL_OPERATORS = Arrays.asList(UQUANT, EQUANT, AND, OR, NOT, IF, IFF);

	/**
	 * SUO-KIF mathematical comparison predicates.
	 */
	private static final List<String> COMPARISON_OPERATORS = Arrays.asList(EQUAL, GT, GTET, LT, LTET);

	/**
	 * The SUO-KIF mathematical functions are implemented in Vampire.
	 */
	private static final List<String> MATH_FUNCTIONS = Arrays.asList(PLUSFN, MINUSFN, TIMESFN, DIVIDEFN);

	/**
	 * For any given formula, stop generating new pred var
	 * instantiations and row var expansions if this threshold value
	 * has been exceeded.  The default value is 2000.
	 */
	private static final int AXIOM_EXPANSION_LIMIT = 2000;

	/**
	 * This constant indicates the maximum predicate arity supported
	 * by the current implementation of Sigma.
	 */
	protected static final int MAX_PREDICATE_ARITY = 7;

	// M E M B E R S

	/**
	 * The formula.
	 */
	public String text;

	/**
	 * A list of clausal (resolution) forms generated from this Formula.
	 */
	private Tuple.Triple<List<Clause>, Formula, Map<String, String>> clausalForm = null;

	/**
	 * The source file in which the formula appears.
	 */
	public String sourceFile;

	/**
	 * The line in the file on which the formula starts.
	 */
	public int startLine;

	/**
	 * The line in the file on which the formula ends.
	 */
	public int endLine;

	/**
	 * Error log
	 */
	public final List<String> errors = new ArrayList<>();

	// C O N S T R U C T O R

	/**
	 * Constructor
	 */
	public Formula()
	{
	}

	/**
	 * Returns a List of the clauses that together constitute the
	 * resolution form of this Formula.  The list could be empty if
	 * the clausal form has not yet been computed.
	 *
	 * @return Tuple
	 */
	public Tuple.Triple<List<Clause>, Formula, Map<String, String>> getClausalForm()
	{
		logger.entering("Formula", "getClausalForm");
		if (clausalForm == null)
		{
			if (isNonEmpty(this.text))
				this.clausalForm = Clausifier.toNegAndPosLitsWithRenameInfo(this);
		}
		logger.exiting("Formula", "getClausalForm", clausalForm);
		return this.clausalForm;
	}

	/**
	 * Returns a List of Clause objects.  Each such Clause contains, in
	 * turn, a pair of List objects.  Each List object in a pair
	 * contains Formula objects.  The Formula objects contained in the
	 * first List object (first) of a pair represent negative literals
	 * (antecedent conjuncts).  The Formula objects contained in the
	 * second List object (second) of a pair represent positive literals
	 * (consequent conjuncts).  Taken together, all of the clauses
	 * constitute the resolution form of this Formula.
	 *
	 * @return A List of Clauses.
	 */
	public List<Clause> getClauses()
	{
		Tuple.Triple<List<Clause>, Formula, Map<String, String>> clausesWithVarMap = getClausalForm();
		if (clausesWithVarMap == null)
			return null;
		return clausesWithVarMap.first;
	}

	/**
	 * Returns a map of the variable renames that occurred during the
	 * translation of this Formula into the clausal (resolution) form
	 * accessible via this.getClauses().
	 *
	 * @return A Map of String (SUO-KIF variable) key-value pairs.
	 */
	public Map<String, String> getVarMap()
	{
		Tuple.Triple<List<Clause>, Formula, Map<String, String>> clausesWithVarMap = getClausalForm();
		if (clausesWithVarMap == null)
			return null;
		return clausesWithVarMap.third;
	}

	// A C C E S S

	/**
	 * Read a String into the variable 'text'.
	 *
	 * @param s formula string
	 */
	public void set(String s)
	{
		this.text = s;
	}

	/**
	 * Get source file
	 *
	 * @return source file
	 */
	public String getSourceFile()
	{
		return this.sourceFile;
	}

	/**
	 * Set source filename
	 *
	 * @param filename source filename
	 */
	public void setSourceFile(String filename)
	{
		this.sourceFile = filename;
	}

	/**
	 * Get error log
	 *
	 * @return errors
	 */
	public List<String> getErrors()
	{
		return this.errors;
	}

	/**
	 * Create ID
	 *
	 * @return a unique ID by appending the hashCode() of the formula String to the file name in which it appears
	 */
	public String createID()
	{
		String fileName = this.sourceFile;
		if (isNonEmpty(fileName) && fileName.lastIndexOf(File.separator) > -1)
			fileName = fileName.substring(fileName.lastIndexOf(File.separator) + 1);
		int hc = this.text.hashCode();
		String result;
		if (hc < 0)
			result = "N" + Integer.toString(hc).substring(1) + fileName;
		else
			result = hc + fileName;
		// logger.finest("ID Created: " + result + "; For the formula: " + text);
		return result;
	}

	/**
	 * Copy the Formula.  This is in effect a deep copy.
	 *
	 * @return deep copy of this formula
	 */
	public Formula copy()
	{
		Formula result = new Formula();
		if (sourceFile != null)
			result.sourceFile = sourceFile.intern();
		result.startLine = this.startLine;
		result.endLine = this.endLine;
		if (text != null)
			result.text = this.text.intern();
		return result;
	}

	// I D E N T I T Y

	/**
	 * Test if the contents of the formula are equal to the argument. Normalize all variables.
	 *
	 * @param other other formula to compare to.
	 * @return whether the contents of the formula are equal to the argument.
	 */
	public boolean equals(Formula other)
	{
		String s = Clausifier.normalizeVariables(this.text).trim();
		String s2 = Clausifier.normalizeVariables(other.text).trim();
		return s.equals(s2);
	}

	/**
	 * Test if the contents of the formula are equal to the String argument.
	 * Normalize all variables.
	 *
	 * @param s2 other formula string to compare to.
	 * @return whether the contents of the formula are equal to the argument.
	 */
	public boolean equals(String s2)
	{
		String s = this.text;
		s = Clausifier.normalizeVariables(s);
		s2 = Clausifier.normalizeVariables(s2).intern();

		Formula f = new Formula();
		f.text = s;
		s = f.toString().trim().intern();

		Formula f2 = new Formula();
		f2.set(s2);
		s2 = f2.toString().trim().intern();
		return s.equals(s2);
	}

	/**
	 * If equals is overridden, hashCode must use the same "significant" fields.
	 */
	public int hashCode()
	{
		String s = Clausifier.normalizeVariables(this.text).trim();
		return s.hashCode();
	}

	// O R D E R I N G

	/**
	 * Implement the Comparable interface by defining the compareTo
	 * method.  Formulas are equal if their formula strings are equal.
	 *
	 * @return compare code
	 */
	public int compareTo(Formula other) throws ClassCastException
	{
		return text.compareTo(other.text);
	}

	// L I S P - L I K E

	/**
	 * Car
	 *
	 * @return the LISP 'car' of the formula as a String - the first
	 * element of the list. Note that this operation has no side
	 * effect on the Formula.
	 * Currently (10/24/2007) this method returns the empty string
	 * ("") when invoked on an empty list.  Technically, this is
	 * wrong.  In most LISPS, the car of the empty list is the empty
	 * list (or nil).  But some parts of the Sigma code apparently
	 * expect this method to return the empty string when invoked on
	 * an empty list.
	 */
	public String car()
	{
		// logger.entering("Formula", "car");
		String result = null;
		if (this.listP())
		{
			if (this.empty())
			{
				// NS: Clean this up someday.
				result = "";  // this.text;
			}
			else
			{
				StringBuilder sb = new StringBuilder();
				List<Character> quoteChars = Arrays.asList('"', '\'');
				int level = 0;
				char prev = '0';
				boolean insideQuote = false;
				char quoteCharInForce = '0';

				String input = this.text.trim();
				int len = input.length();
				int end = len - 1;
				for (int i = 1; i < end; i++)
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
						else if (Character.isWhitespace(ch) && (level <= 0))
						{
							if (sb.length() > 0)
							{
								break;
							}
						}
						else if (quoteChars.contains(ch) && (prev != '\\'))
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
					else if (quoteChars.contains(ch) && (ch == quoteCharInForce) && (prev != '\\'))
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
				result = sb.toString();
			}
		}
		// logger.exiting("Formula", "car", result);
		return result;
	}

	/**
	 * Cdr
	 *
	 * @return the LISP 'cdr' of the formula - the rest of a list minus its
	 * first element.
	 * Note that this operation has no side effect on the Formula.
	 */
	public String cdr()
	{
		// logger.entering("Formula", "cdr");
		String result = null;
		if (this.listP())
		{
			if (this.empty())
			{
				result = this.text;
			}
			else
			{
				List<Character> quoteChars = Arrays.asList('"', '\'');
				int level = 0;
				char prev = '0';
				boolean insideQuote = false;
				char quoteCharInForce = '0';
				int carCount = 0;

				String input = text.trim();
				int len = input.length();
				int end = len - 1;
				int i = 1;
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
						else if (quoteChars.contains(ch) && (prev != '\\'))
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
					else if (quoteChars.contains(ch) && (ch == quoteCharInForce) && (prev != '\\'))
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
						result = "(" + input.substring(j, end).trim() + ")";
					}
					else
					{
						result = "()";
					}
				}
			}
		}
		// logger.exiting("Formula", "cdr", result);
		return result;
	}

	/**
	 * Cons
	 *
	 * @param obj The String object that will become the 'car' (or
	 *            head) of the resulting Formula (list).
	 * @return a new Formula, or the original Formula if the cons fails.
	 * A new Formula which is the result of 'consing' a String
	 * into this Formula, similar to the LISP procedure of the same
	 * name.  This procedure is a little bit of a kludge, since this
	 * Formula is treated simply as a LISP object (presumably, a LISP
	 * list), and could be degenerate or malformed as a Formula.
	 * Note that this operation has no side effect on the original Formula.
	 */
	public Formula cons(String obj)
	{
		// logger.entering("Formula", "cons", obj);
		Formula result = this;
		String fStr = this.text;
		if (isNonEmpty(obj) && isNonEmpty(fStr))
		{
			String newFStr;
			if (this.listP())
			{
				if (this.empty())
					newFStr = ("(" + obj + ")");
				else
					newFStr = ("(" + obj + " " + fStr.substring(1, (fStr.length() - 1)) + ")");
			}
			else
				// This should never happen during clausification, but we include it to make this procedure behave (almost) like its LISP namesake.
				newFStr = ("(" + obj + " . " + fStr + ")");

			result = new Formula();
			result.set(newFStr);
		}
		// logger.exiting("Formula", "cons", result);
		return result;
	}

	/**
	 * Cons
	 *
	 * @param f formula
	 * @return the LISP 'cons' of the formula, a new Formula, or the original Formula if the cons fails.
	 */
	public Formula cons(Formula f)
	{
		return cons(f.text);
	}

	/**
	 * Cdr, the LISP 'cdr' of the formula as a new Formula, if
	 * possible, else returns null.
	 *
	 * @return the cdr of the formula.
	 * Note that this operation has no side effect on the Formula.
	 */
	public Formula cdrAsFormula()
	{
		String cdr = this.cdr();
		if (listP(cdr))
		{
			Formula f = new Formula();
			f.set(cdr);
			return f;
		}
		return null;
	}

	/**
	 * Cadr
	 *
	 * @return the LISP 'cadr' (the second list element) of the
	 * formula, a String, or the empty string if the is no cadr.
	 * Note that this operation has no side effect on the Formula.
	 */
	public String cadr()
	{
		return this.getArgument(1);
	}

	/**
	 * Caddr
	 *
	 * @return the LISP 'caddr' of the formula, which is the third
	 * list element of the formula,a String, or the empty string if there is no caddr.
	 * Note that this operation has no side effect on the Formula.
	 */
	public String caddr()
	{
		return this.getArgument(2);
	}

	/**
	 * Append
	 *
	 * @param f formula
	 * @return the LISP 'append' of the formulas, a Formula
	 * Note that this operation has no side effect on the Formula.
	 */
	public Formula append(Formula f)
	{
		Formula newFormula = new Formula();
		newFormula.set(this.text);
		if (newFormula.equals("") || newFormula.atom())
		{
			System.err.println("ERROR in KB.append(): attempt to append to non-list: " + text);
			return this;
		}
		if (f == null || f.text == null || f.text.isEmpty() || f.text.equals("()"))
			return newFormula;
		f.text = f.text.trim();
		if (!f.atom())
			f.text = f.text.substring(1, f.text.length() - 1);
		int lastParen = text.lastIndexOf(")");
		String sep = "";
		if (lastParen > 1)
			sep = " ";
		newFormula.text = newFormula.text.substring(0, lastParen) + sep + f.text + ")";
		return newFormula;
	}

	/**
	 * Atom
	 *
	 * @param s formula string
	 * @return whether the String is a LISP atom.
	 */
	public static boolean atom(String s)
	{
		if (isNonEmpty(s))
		{
			String str = s.trim();
			return StringUtil.isQuotedString(s) || (!str.contains(")") && !str.matches(".*\\s.*"));
		}
		return false;
	}

	/**
	 * Atom
	 *
	 * @return whether the Formula is a LISP atom.
	 */
	public boolean atom()
	{
		return Formula.atom(this.text);
	}

	/**
	 * Empty
	 *
	 * @return whether the Formula is an empty list.
	 */
	public boolean empty()
	{
		return Formula.empty(this.text);
	}

	/**
	 * Empty
	 *
	 * @param s formula string
	 * @return whether the String is an empty formula.  Not to be
	 * confused with a null string or empty string.  There must be
	 * parentheses with nothing or whitespace in the middle.
	 */
	public static boolean empty(String s)
	{
		return listP(s) && s.matches("\\(\\s*\\)");
	}

	/**
	 * ListP
	 *
	 * @return whether the Formula is a list.
	 */
	public boolean listP()
	{
		return Formula.listP(text);
	}

	/**
	 * ListP
	 *
	 * @param s formula string
	 * @return whether the String is a list.
	 */
	public static boolean listP(String s)
	{
		if (isNonEmpty(s))
		{
			String str = s.trim();
			return str.startsWith("(") && str.endsWith(")");
		}
		return false;
	}

	/**
	 * Returns a non-negative int value indicating the top-level list
	 * length of this Formula if it is a proper listP(), else returns
	 * -1.  One caveat: This method assumes that neither null nor the
	 * empty string are legitimate list members in a wff.  The return
	 * value is likely to be wrong if this assumption is mistaken.
	 *
	 * @return A non-negative int, or -1.
	 */
	public int listLength()
	{
		int result = -1;
		if (this.listP())
		{
			result = 0;
			while (isNonEmpty(this.getArgument(result)))
				++result;
		}
		return result;
	}

	/**
	 * @return An List (ordered tuple) representation of the
	 * Formula, in which each top-level element of the Formula is
	 * either an atom (String) or another list.
	 */
	public List<String> literalToList()
	{
		List<String> tuple = new ArrayList<>();
		Formula f = this;
		if (f.listP())
		{
			while (!f.empty())
			{
				tuple.add(f.car());
				f = f.cdrAsFormula();
			}
		}
		return tuple;
	}

	// V A L I D A T I O N

	/**
	 * Returns true if the Formula contains no unbalanced parentheses
	 * or unbalanced quote characters, otherwise returns false.
	 *
	 * @return boolean
	 */
	public boolean isBalancedList()
	{
		boolean result = false;
		if (this.listP())
		{
			if (this.empty())
				result = true;
			else
			{
				List<Character> quoteChars = Arrays.asList('"', '\'');
				int pLevel = 0;
				int qLevel = 0;
				char prev = '0';
				boolean insideQuote = false;
				char quoteCharInForce = '0';

				String input = this.text.trim();
				int len = input.length();
				for (int i = 0; i < len; i++)
				{
					char ch = input.charAt(i);
					if (!insideQuote)
					{
						if (ch == '(')
							pLevel++;
						else if (ch == ')')
							pLevel--;
						else if (quoteChars.contains(ch) && (prev != '\\'))
						{
							insideQuote = true;
							quoteCharInForce = ch;
							qLevel++;
						}
					}
					else if (quoteChars.contains(ch) && (ch == quoteCharInForce) && (prev != '\\'))
					{
						insideQuote = false;
						quoteCharInForce = '0';
						qLevel--;
					}
					prev = ch;
				}
				result = ((pLevel == 0) && (qLevel == 0));
				// logger.finest("qLevel == " + qLevel);
				// logger.finest("pLevel == " + pLevel);
			}
		}
		return result;
	}

	/**
	 * @see #validArgs() validArgs below for documentation
	 */
	private String validArgsRecurse(Formula f, String filename, Integer lineNo)
	{
		// logger.finest("Formula: " + f.text);
		if (f.text.isEmpty() || !f.listP() || f.atom() || f.empty())
			return "";
		String pred = f.car();
		String rest = f.cdr();
		Formula restF = new Formula();
		restF.set(rest);
		int argCount = 0;
		while (!restF.empty())
		{
			argCount++;
			String arg = restF.car();
			Formula argF = new Formula();
			argF.set(arg);
			String result = validArgsRecurse(argF, filename, lineNo);
			if (!result.isEmpty())
				return result;
			restF.text = restF.cdr();
		}
		if (pred.equals(AND) || pred.equals(OR))
		{
			if (argCount < 2)
				return "Too few arguments for 'and' or 'or' in formula: \n" + f.toString() + "\n";
		}
		else if (pred.equals(UQUANT) || pred.equals(EQUANT))
		{
			if (argCount != 2)
				return "Wrong number of arguments for 'exists' or 'forall' in formula: \n" + f.toString() + "\n";
			else
			{
				Formula quantF = new Formula();
				quantF.set(rest);
				if (!listP(quantF.car()))
					return "No parenthesized variable list for 'exists' or 'forall' " + "in formula: \n" + f.toString() + "\n";
			}
		}
		else if (pred.equals(IFF) || pred.equals(IF))
		{
			if (argCount != 2)
				return "Wrong number of arguments for '<=>' or '=>' in formula: \n" + f.toString() + "\n";
		}
		else if (pred.equals(EQUAL))
		{
			if (argCount != 2)
				return "Wrong number of arguments for 'equals' in formula: \n" + f.toString() + "\n";
		}
		else if (!(isVariable(pred)) && (argCount > (MAX_PREDICATE_ARITY + 1)))
		{
			String location = "";
			if ((filename != null) && (lineNo != null))
			{
				location = (" near line " + lineNo + " in " + filename);
			}
			this.errors.add("Maybe too many arguments " + location + ": " + f.toString() + "\n");
		}
		return "";
	}

	/**
	 * Test whether the Formula uses logical operators and predicates
	 * with the correct number of arguments.  "equals", "&lt;=&gt;", and
	 * "=&gt;" are strictly binary.  "or", and "and" are binary or
	 * greater. "not" is unary.  "forall" and "exists" are unary with
	 * an argument list.  Warn if we encounter a formula that has more
	 * arguments than MAX_PREDICATE_ARITY.
	 *
	 * @param filename If not null, denotes the name of the file being
	 *                 parsed.
	 * @param lineNo   If not null, indicates the location of the
	 *                 expression (formula) being parsed in the file being read.
	 * @return an empty String if there are no problems or an error message
	 * if there are.
	 */
	public String validArgs(String filename, Integer lineNo)
	{
		if (this.text == null || this.text.isEmpty())
			return "";
		Formula f = new Formula();
		f.set(this.text);

		// logger.finest("Result: " + result);

		return validArgsRecurse(f, filename, lineNo);
	}

	/**
	 * Test whether the Formula uses logical operators and predicates
	 * with the correct number of arguments.  "equals", "&lt;=&gt;", and
	 * "=&gt;" are strictly binary.  "or", and "and" are binary or
	 * greater. "not" is unary.  "forall" and "exists" are unary with
	 * an argument list.  Warn if we encounter a formula that has more
	 * arguments than MAX_PREDICATE_ARITY.
	 *
	 * @return an empty String if there are no problems or an error message
	 * if there are.
	 */
	public String validArgs()
	{
		return this.validArgs(null, null);
	}

	/**
	 * Not yet implemented!  Test whether the Formula has variables that are not properly
	 * quantified.  The case tested for is whether a quantified variable
	 * in the antecedent appears in the consequent or vice versa.
	 *
	 * @return an empty String if there are no problems or an error message
	 * if there are.
	 */
	@SuppressWarnings("SameReturnValue") public String badQuantification()
	{
		return "";
	}

	// P A R S E

	/**
	 * Parse a String into an List of Formulas. The String must be
	 * a LISP-style list.
	 *
	 * @return an List of Formulas
	 */
	private List<Formula> parseList(String s)
	{
		List<Formula> result = new ArrayList<>();
		Formula f = new Formula();
		f.set("(" + s + ")");
		if (f.empty())
			return result;
		while (!f.empty())
		{
			String car = f.car();
			f.set(f.cdr());
			Formula newForm = new Formula();
			newForm.set(car);
			result.add(newForm);
		}
		return result;
	}

	// C O M P A R E

	/**
	 * Compare two lists of formulas, testing whether they are equal,
	 * without regard to order.  (B A C) will be equal to (C B A). The
	 * method iterates through one list, trying to find a match in the other
	 * and removing it if a match is found.  If the lists are equal, the
	 * second list should be empty once the iteration is complete.
	 * Note that the formulas being compared must be lists, not atoms, and
	 * not a set of formulas unenclosed by parentheses.  So, "(A B C)"
	 * and "(A)" are valid, but "A" is not, nor is "A B C".
	 */
	private boolean compareFormulaSets(String s)
	{
		List<Formula> list = parseList(this.text.substring(1, this.text.length() - 1));
		List<Formula> sList = parseList(s.substring(1, s.length() - 1));
		if (list.size() != sList.size())
			return false;

		for (Formula f : list)
		{
			for (int j = 0; j < sList.size(); j++)
			{
				if (f.logicallyEquals(sList.get(j).text))
				{
					sList.remove(j);
					j = sList.size();
				}
			}
		}
		return sList.size() == 0;
	}

	/**
	 * Test if the contents of the formula are equal to the argument
	 * at a deeper level than a simple string equals.  The only logical
	 * manipulation is to treat conjunctions and disjunctions as unordered
	 * bags of clauses. So (and A B C) will be logicallyEqual(s) for example,
	 * to (and B A C).  Note that this is a fairly time-consuming operation
	 * and should not generally be used for comparing large sets of formulas.
	 *
	 * @param s formula string
	 * @return whether the contents of the formula are equal to the argument.
	 */
	public boolean logicallyEquals(String s)
	{
		if (this.equals(s))
			return true;
		if (Formula.atom(s) && s.compareTo(this.text) != 0)
			return false;

		Formula form = new Formula();
		form.set(this.text);
		Formula sForm = new Formula();
		sForm.set(s);

		if ("and".equals(form.car().intern()) || "or".equals(form.car().intern()))
		{
			if (!sForm.car().intern().equals(sForm.car().intern()))
				return false;
			form.set(form.cdr());
			sForm.set(sForm.cdr());
			return form.compareFormulaSets(sForm.text);
		}
		else
		{
			Formula newForm = new Formula();
			newForm.set(form.car());
			Formula newSForm = new Formula();
			newSForm.set(sForm.cdr());
			return newForm.logicallyEquals(sForm.car()) && newSForm.logicallyEquals(form.cdr());
		}
	}

	// A R G U M E N T S

	/**
	 * Return the numbered argument of the given formula.  The first
	 * element of a formula (i.e. the predicate position) is number 0.
	 * Returns the empty string if there is no such argument position.
	 *
	 * @param argnum argument number
	 * @return numbered argument.
	 */
	public String getArgument(int argnum)
	{
		String result = "";
		Formula form = new Formula();
		form.set(this.text);
		for (int i = 0; form.listP(); i++)
		{
			result = form.car();
			if (i == argnum)
			{
				break;
			}
			form.set(form.cdr());
		}
		return result == null ? "" : result;
	}

	/**
	 * Return all the arguments in a simple formula as a list, starting
	 * at the given argument.  If formula is complex (i.e. an argument
	 * is a function or sentence), then return null.  If the starting
	 * argument is greater than the number of arguments, also return
	 * null.
	 *
	 * @param start start argument.
	 * @return all the arguments in a simple formula as a list.
	 */
	public List<String> argumentsToList(int start)
	{
		if (text.indexOf('(', 1) != -1)
			return null;
		int index = start;
		List<String> result = new ArrayList<>();
		String arg = getArgument(index);
		while (arg != null && !arg.isEmpty())
		{
			result.add(arg.intern());
			index++;
			arg = getArgument(index);
		}
		if (index == start)
			return null;
		return result;
	}

	// U N I F I C A T I O N

	/**
	 * Unify var
	 *
	 * @return a Map of variable substitutions if successful, null if not
	 */
	private SortedMap<String, String> unifyVar(String f1, String f2, SortedMap<String, String> m)
	{
		if (m.containsKey(f1))
			return unifyInternal(m.get(f1), f2, m);
		else if (m.containsKey(f2))
			return unifyInternal(m.get(f2), f1, m);
		else if (f2.contains(f1))
			return null;
		else
		{
			m.put(f1, f2);
			return m;
		}
	}

	/**
	 * Unify (internal)
	 *
	 * @return a Map of variable substitutions if successful, null if not
	 */
	private SortedMap<String, String> unifyInternal(String f1, String f2, SortedMap<String, String> m)
	{
		if (m == null)
			return null;
		else if (f1.equals(f2))
			return m;
		else if (isVariable(f1))
			return unifyVar(f1, f2, m);
		else if (isVariable(f2))
			return unifyVar(f2, f1, m);
		else if (listP(f1) && listP(f2))
		{
			Formula form1 = new Formula();
			form1.set(f1);

			Formula form2 = new Formula();
			form2.set(f2);

			SortedMap<String, String> res = unifyInternal(form1.car(), form2.car(), m);
			if (res == null)
				return null;
			else
				return unifyInternal(form1.cdr(), form2.cdr(), res);
		}
		else
		{
			return null;
		}
	}

	/**
	 * Attempt to unify one formula with another. Return a Map of
	 * variable substitutions if successful, null if not. If two
	 * formulas are identical the result will be an empty (but not
	 * null) SortedMap. Algorithm is after Russell and Norvig's AI: A
	 * Modern Approach p303. But R and N's algorithm assumes that
	 * variables are within the same scope, which is not the case
	 * when unifying clauses in resolution.  This needs to be
	 * corrected by renaming variables so each clause does not
	 * duplicate names from the other.
	 *
	 * @param f formula
	 * @return a Map of variable substitutions if successful, null if not
	 */
	public SortedMap<String, String> unify(Formula f)
	{
		SortedMap<String, String> result = new TreeMap<>();
		result = unifyInternal(f.text, this.text, result);
		return result;
	}

	/**
	 * Use a SortedMap of [varName, value] to substitute value in for
	 * varName wherever it appears in the formula.  This is
	 * iterative, since values can themselves contain varNames.
	 *
	 * @param m sorted map of [var, value] pairs
	 * @return formula
	 */
	public Formula substitute(SortedMap<String, String> m)
	{
		Formula result;
		String newForm = null;
		while (newForm == null || !newForm.equals(text))
		{
			newForm = text;
			result = substituteVariables(m);
			this.text = result.text;
		}
		return this;
	}

	/**
	 * A convenience method that collects all variables and returns
	 * a simple List of variables whether quantified or not.
	 *
	 * @return An List of String
	 */
	public List<String> simpleCollectVariables()
	{
		Tuple.Pair<List<String>, List<String>> ans = collectVariables();
		if (ans == null)
			return null;
		List<String> ans1 = ans.first;
		if (ans1 == null)
			return null;
		List<String> result = new ArrayList<>(ans1);
		List<String> ans2 = ans.second;
		if (ans2 == null)
			return result;
		result.addAll(ans2);
		return result;
	}

	/**
	 * Collects all variables in this Formula.  Returns an List
	 * containing a pair of Lists.  The first contains all
	 * explicitly quantified variables in the Formula.  The second
	 * contains all variables in Formula that are not within the scope
	 * of some explicit quantifier.
	 *
	 * @return A pair of Lists, each of which could be empty
	 */
	public Tuple.Pair<List<String>, List<String>> collectVariables()
	{
		Tuple.Pair<List<String>, List<String>> result = new Tuple.Pair<>();
		result.first = new ArrayList<>();
		result.second = new ArrayList<>();
		Set<String> unquantified = new HashSet<>(collectAllVariables());
		//Set<String> quantified = new HashSet<>();
		//quantified.allAll(collectQuantifiedVariables());
		//unquantified.removeAll(quantified);
		//result.first.addAll(quantified);
		result.second.addAll(unquantified);
		logger.exiting("Formula", "collectVariables", result);
		return result;
	}

	/**
	 * Collects all variables in this Formula.  Returns an List
	 * of String variable names (with initial '?').  Note that
	 * duplicates are not removed.
	 *
	 * @return An List of String variable names
	 */
	private List<String> collectAllVariables()
	{
		List<String> result = new ArrayList<>();
		if (listLength() < 1)
			return result;
		Formula fCar = new Formula();
		fCar.set(this.car());
		if (fCar.isVariable())
			result.add(fCar.text);
		else
		{
			if (fCar.listP())
				result.addAll(fCar.collectAllVariables());
		}
		Formula fCdr = new Formula();
		fCdr.set(this.cdr());
		if (fCdr.isVariable())
			result.add(fCdr.text);
		else
		{
			if (fCdr.listP())
				result.addAll(fCdr.collectAllVariables());
		}
		return result;
	}

	/**
	 * Collects all quantified variables in this Formula.  Returns an List
	 * of String variable names (with initial '?').  Note that
	 * duplicates are not removed.
	 *
	 * @return An List of String variable names
	 */
	public List<String> collectQuantifiedVariables()
	{
		List<String> result = new ArrayList<>();
		if (listLength() < 1)
			return result;
		Formula fCar = new Formula();
		fCar.set(this.car());
		if (fCar.text.equals(UQUANT) || fCar.text.equals(EQUANT))
		{
			Formula remainder = new Formula();
			remainder.set(this.cdr());
			if (!remainder.listP())
			{
				System.err.println("ERROR in Formula.collectQuantifiedVariables(): incorrect quantification: " + this.toString());
				return result;
			}
			Formula varList = new Formula();
			varList.set(remainder.car());
			result.addAll(varList.collectAllVariables());
			result.addAll(remainder.cdrAsFormula().collectQuantifiedVariables());
		}
		else
		{
			if (fCar.listP())
				result.addAll(fCar.collectQuantifiedVariables());
			result.addAll(this.cdrAsFormula().collectQuantifiedVariables());
		}
		return result;
	}

	/**
	 * Makes implicit quantification explicit.
	 *
	 * @param query controls whether to add universal or existential
	 *              quantification.  If true, add existential.
	 * @return the formula as a String, with explicit quantification
	 */
	public String makeQuantifiersExplicit(boolean query)
	{
		String result = this.text;
		this.car();
		Tuple.Pair<List<String>, List<String>> vPair = collectVariables();
		List<String> unquantVariables = vPair.second;

		if (!unquantVariables.isEmpty())
		{
			// Quantify all the unquantified variables
			StringBuilder sb = new StringBuilder();
			sb.append((query ? "(exists (" : "(forall ("));
			boolean afterTheFirst = false;
			for (String unquantVariable : unquantVariables)
			{
				if (afterTheFirst)
					sb.append(" ");
				sb.append(unquantVariable);
				afterTheFirst = true;
			}
			sb.append(") ");
			sb.append(this.text);
			sb.append(")");
			result = sb.toString();
			logger.exiting("Formula", "makeQuantifiersExplicit", result);
		}
		return result;
	}

	/**
	 * Test if this Formula contains any variable arity relations
	 *
	 * @param kb - The KB used to compute variable arity relations.
	 * @return Returns true if this Formula contains any variable
	 * arity relations, else returns false.
	 */
	protected boolean containsVariableArityRelation(KB kb)
	{
		boolean result = false;
		Set<String> relns = kb.getCachedRelationValues("instance", "VariableArityRelation", 2, 1);
		if (relns == null)
			relns = new HashSet<>();
		relns.addAll(KB.VA_RELNS);
		for (String reln : relns)
		{
			result = (this.text.contains(reln));
			if (result)
			{
				break;
			}
		}
		return result;
	}

	/**
	 * Gathers the row variable names in this.text and returns
	 * them in a SortedSet.
	 *
	 * @return a SortedSet, possibly empty, containing row variable
	 * names, each of which will start with the row variable
	 * designator '@'.
	 */
	private SortedSet<String> findRowVars()
	{
		SortedSet<String> result = new TreeSet<>();
		if (isNonEmpty(this.text) && this.text.contains(R_PREF))
		{
			Formula f = new Formula();
			f.set(this.text);

			while (f.listP() && !f.empty())
			{
				String arg = f.getArgument(0);
				if (arg.startsWith(R_PREF))
					result.add(arg);
				else
				{
					Formula argF = new Formula();
					argF.set(arg);
					if (argF.listP())
						result.addAll(argF.findRowVars());
				}
				f.set(f.cdr());
			}
		}
		return result;
	}

	/**
	 * Expand row variables, keeping the information about the original
	 * source formula.  Each variable is treated like a macro that
	 * expands to up to seven regular variables.  For example
	 * (=&gt;
	 * (and
	 * (subrelation ?REL1 ?REL2)
	 * (holds__ ?REL1 @ROW))
	 * (holds__ ?REL2 @ROW))
	 * would become
	 * (=&gt;
	 * (and
	 * (subrelation ?REL1 ?REL2)
	 * (holds__ ?REL1 ?ARG1))
	 * (holds__ ?REL2 ?ARG1))
	 * (=&gt;
	 * (and
	 * (subrelation ?REL1 ?REL2)
	 * (holds__ ?REL1 ?ARG1 ?ARG2))
	 * (holds__ ?REL2 ?ARG1 ?ARG2))
	 * etc.
	 *
	 * @param kb knowledge base
	 * @return an List of Formulas, or an empty List.
	 */
	public List<Formula> expandRowVars(KB kb)
	{
		logger.entering("Formula", "expandRowVars", kb.name);
		List<Formula> result = new ArrayList<>();
		SortedSet<String> rowVars = (this.text.contains(R_PREF) ? this.findRowVars() : null);
		// If this Formula contains no row vars to expand, we just add it to resultList and quit.
		if ((rowVars == null) || rowVars.isEmpty())
		{
			result.add(this);
		}
		else
		{
			Formula f = new Formula();
			f.set(this.text);

			Set<Formula> accumulator = new LinkedHashSet<>();
			accumulator.add(f);

			// Iterate through the row variables
			for (String rowVar : rowVars)
			{
				List<Formula> working = new ArrayList<>(accumulator);
				accumulator.clear();

				for (Formula f2 : working)
				{
					String f2Str = f2.text;
					if (!f2Str.contains(R_PREF) || (f2Str.contains("\"")))
					{
						f2.sourceFile = this.sourceFile;
						result.add(f2);
					}
					else
					{
						int[] range = f2.getRowVarExpansionRange(kb, rowVar);

						boolean hasVariableArityRelation = (range[0] == 0);
						range[1] = adjustExpansionCount(hasVariableArityRelation, range[1], rowVar);

						StringBuilder varRepl = new StringBuilder();
						for (int j = 1; j < range[1]; j++)
						{
							if (varRepl.length() > 0)
								varRepl.append(" ");
							varRepl.append("?");
							varRepl.append(rowVar.substring(1));
							varRepl.append(j);
							if (hasVariableArityRelation)
							{
								String f2Str2 = f2Str.replaceAll(rowVar, varRepl.toString());
								Formula newF = new Formula();
								newF.set(f2Str2);

								// Copy the source file information for each expanded formula.
								newF.sourceFile = this.sourceFile;
								if (newF.text.contains(R_PREF) && (!newF.text.contains("\"")))
								{
									accumulator.add(newF);
								}
								else
									result.add(newF);
							}
						}
						if (!hasVariableArityRelation)
						{
							String f2Str2 = f2Str.replaceAll(rowVar, varRepl.toString());
							Formula newF = new Formula();
							newF.set(f2Str2);

							// Copy the source file information for each expanded formula.
							newF.sourceFile = this.sourceFile;
							if (newF.text.contains(R_PREF) && (newF.text.indexOf('"') == -1))
							{
								accumulator.add(newF);
							}
							else
								result.add(newF);
						}
					}
				}
			}
		}
		logger.exiting("Formula", "expandRowVars", result);
		return result;
	}

	/**
	 * This method attempts to revise the number of row var expansions
	 * to be done, based on the occurrence of forms such as (<pred>
	 *
	 * @param variableArity Indicates whether the overall expansion
	 *                      count for the Formula is governed by a variable arity relation,
	 *                      or not.
	 * @param count         The default expected expansion count, possibly to
	 *                      be revised.
	 * @param var           The row variable to be expanded.
	 * @return An int value, the revised expansion count.  In most
	 * cases, the count will not change.
	 * @ROW1 ?ITEM).  Note that variables such as ?ITEM throw off the
	 * default expected expansion count, and so must be dealt with to
	 * prevent unnecessary expansions.
	 */
	private int adjustExpansionCount(boolean variableArity, int count, String var)
	{
		if (logger.isLoggable(Level.FINER))
		{
			String[] params = { "variableArity = " + variableArity, "count = " + count, "var = " + var };
			logger.entering("Formula", "adjustExpansionCount", params);
		}
		int revisedCount = count;
		if (isNonEmpty(var))
		{
			String rowVar = var;
			if (!var.startsWith("@"))
				rowVar = ("@" + var);
			List<Formula> accumulator = new ArrayList<>();
			List<Formula> working = new ArrayList<>();
			if (this.listP() && !this.empty())
				accumulator.add(this);
			while (!accumulator.isEmpty())
			{
				working.clear();
				working.addAll(accumulator);
				accumulator.clear();
				for (Formula f : working)
				{
					List<String> literal = f.literalToList();
					int len = literal.size();
					if (literal.contains(rowVar) && !isVariable(f.car()))
					{
						if (!variableArity && (len > 2))
							revisedCount = (count - (len - 2));
						else if (variableArity)
							revisedCount = (10 - len);
					}
					if (revisedCount < 2)
					{
						revisedCount = 2;
					}
					while (!f.empty())
					{
						String arg = f.car();
						Formula argF = new Formula();
						argF.set(arg);
						if (argF.listP() && !argF.empty())
							accumulator.add(argF);
						f = f.cdrAsFormula();
					}
				}
			}
		}
		logger.exiting("Formula", "adjustExpansionCount", revisedCount);
		return revisedCount;
	}

	/**
	 * Returns a two-place int[] indicating the low and high points of
	 * the expansion range (number of row var instances) for the input
	 * row var.
	 *
	 * @param kb     A KB required for processing.
	 * @param rowVar The row var (String) to be expanded.
	 * @return A two-place int[] object.  The int[] indicates a
	 * numeric range.  int[0] holds the start (lowest number) in the
	 * range, and int[1] holds the highest number.  The default is
	 * [1,8].  If the Formula does not contain
	 */
	private int[] getRowVarExpansionRange(KB kb, String rowVar)
	{
		if (logger.isLoggable(Level.FINER))
		{
			String[] params = { "kb = " + kb.name, "rowVar = " + rowVar };
			logger.entering("Formula", "getRowVarExpansionRange", params);
		}
		int[] result = new int[] { 1, 8 };
		if (isNonEmpty(rowVar))
		{
			String var = rowVar;
			if (!var.startsWith("@"))
				var = "@" + var;
			Map<String, int[]> minMaxMap = this.getRowVarsMinMax(kb);
			int[] newArr = minMaxMap.get(var);
			if (newArr != null)
				result = newArr;
		}
		logger.exiting("Formula", "getRowVarExpansionRange", result);
		return result;
	}

	/**
	 * Applied to a SUO-KIF Formula with row variables, this method
	 * returns a Map containing an int[] of length 2 for each row var
	 * that indicates the minimum and maximum number of row var
	 * expansions to perform.
	 *
	 * @param kb A KB required for processing.
	 * @return A Map in which the keys are distinct row variables and
	 * the values are two-place int[] objects.  The int[] indicates a
	 * numeric range.  int[0] is the start (lowest number) in the
	 * range, and int[1] is the end.  If the Formula contains no row
	 * vars, the Map is empty.
	 */
	private Map<String, int[]> getRowVarsMinMax(KB kb)
	{
		logger.entering("Formula", "getRowVarsMinMax", kb.name);
		Map<String, int[]> result = new HashMap<>();
		Tuple.Triple<List<Clause>, Formula, Map<String, String>> clauseData = this.getClausalForm();
		if (clauseData == null)
			return result;

		List<Clause> clauses = clauseData.first;
		if (clauses == null || clauses.isEmpty())
			return result;

		Map<String, String> varMap = clauseData.third;
		Map<String, SortedSet<String>> rowVarRelns = new HashMap<>();
		for (Clause clause : clauses)
		{
			if (clause != null)
			{
				// First we get the neg lits.  It may be that we should use *only* the neg lits for this
				// task, but we will start by combining the neg lits and pos lits into one list of literals
				// and see how that works.
				List<Formula> literals = clause.negativeLits;
				List<Formula> posLits = clause.positiveLits;
				literals.addAll(posLits);
				for (Formula litF : literals)
				{
					litF.computeRowVarsWithRelations(rowVarRelns, varMap);
				}
			}
			// logger.finest("rowVarRelns == " + rowVarRelns);
			if (!rowVarRelns.isEmpty())
			{
				for (String rowVar : rowVarRelns.keySet())
				{
					String origRowVar = Clausifier.getOriginalVar(rowVar, varMap);
					int[] minMax = result.computeIfAbsent(origRowVar, k -> new int[] { 0, 8 });
					SortedSet<String> val = rowVarRelns.get(rowVar);
					for (String reln : val)
					{
						int arity = kb.getValence(reln);
						if (arity >= 1)
						{
							minMax[0] = 1;
							int arityPlusOne = (arity + 1);
							if (arityPlusOne < minMax[1])
								minMax[1] = arityPlusOne;
						}
						//else
						//{
						// It's a VariableArityRelation or we
						// can't find an arity, so do nothing.
						//}
					}
				}
			}
		}
		logger.exiting("Formula", "getRowVarsMinMax", result);
		return result;
	}

	/**
	 * Finds all the relations in this Formula that are applied to row
	 * variables, and for which a specific arity might be computed.
	 * Note that results are accumulated in varsToRelns, and the
	 * variable correspondences (if any) in varsToVars are used to
	 * compute the results.
	 *
	 * @param varsToRelns A Map for accumulating row var data for one
	 *                    Formula literal.  The keys are row variables (Strings) and the
	 *                    values are SortedSets containing relations (Strings) that might
	 *                    help to constrain the row var during row var expansion.
	 * @param varsToVars  A Map of variable correspondences, the leaves
	 *                    of which might include row variables
	 */
	protected void computeRowVarsWithRelations(Map<String, SortedSet<String>> varsToRelns, Map<String, String> varsToVars)
	{
		Formula f = this;
		if (f.listP() && !f.empty())
		{
			String relation = f.car();
			if (!isVariable(relation) && !relation.equals(SKFN))
			{
				Formula newF = f.cdrAsFormula();
				while (newF.listP() && !newF.empty())
				{
					String term = newF.car();
					String rowVar = term;
					if (isVariable(rowVar))
					{
						if (rowVar.startsWith(V_PREF) && (varsToVars != null))
							rowVar = Clausifier.getOriginalVar(term, varsToVars);
					}
					if (rowVar.startsWith(R_PREF))
					{
						SortedSet<String> relns = varsToRelns.get(term);
						if (relns == null)
						{
							relns = new TreeSet<>();
							varsToRelns.put(term, relns);
							varsToRelns.put(rowVar, relns);
						}
						relns.add(relation);
					}
					else
					{
						Formula termF = new Formula();
						termF.set(term);
						termF.computeRowVarsWithRelations(varsToRelns, varsToVars);
					}
					newF = newF.cdrAsFormula();
				}
			}
		}
	}

	/**
	 * Returns a Set of all atomic KIF Relation constants that
	 * occur as Predicates or Functions (argument 0 terms) in this
	 * Formula.
	 *
	 * @return a Set containing the String constants that denote
	 * KIF Relations in this Formula, or an empty Set.
	 */
	public Set<String> gatherRelationConstants()
	{
		Set<String> relations = new HashSet<>();
		List<String> kifLists = new ArrayList<>();
		Set<String> accumulator = new HashSet<>();
		if (this.listP() && !this.empty())
			accumulator.add(this.text);
		while (!accumulator.isEmpty())
		{
			kifLists.clear();
			kifLists.addAll(accumulator);
			accumulator.clear();
			for (String kifList : kifLists)
			{
				if (listP(kifList))
				{
					Formula f = new Formula();
					f.set(kifList);
					for (int i = 0; !f.empty(); i++)
					{
						String arg = f.car();
						if (listP(arg))
						{
							if (!empty(arg))
								accumulator.add(arg);
						}
						else if (isQuantifier(arg))
						{
							accumulator.add(f.getArgument(2));
							break;
						}
						else if ((i == 0) && !isVariable(arg) && !isLogicalOperator(arg) && !arg.equals(SKFN) && !StringUtil.isQuotedString(arg) && !arg
								.matches(".*\\s.*"))
						{
							relations.add(arg);
						}
						f = f.cdrAsFormula();
					}
				}
			}
		}
		return relations;
	}

	// P R O P E R T I E S

	/**
	 * Test whether a Formula is a functional term.  Note this assumes
	 * the textual convention of all functions ending with "Fn".
	 *
	 * @return whether a Formula is a functional term.
	 */
	public boolean isFunctionalTerm()
	{
		if (this.listP())
		{
			String pred = this.car();
			return pred.length() > 2 && pred.endsWith(FN_SUFF);
		}
		return false;
	}

	/**
	 * Test whether a Formula is a functional term
	 *
	 * @param s formula string
	 * @return whether a Formula is a functional term.
	 */
	public static boolean isFunctionalTerm(String s)
	{
		Formula f = new Formula();
		f.set(s);
		return f.isFunctionalTerm();
	}

	/**
	 * Test whether a Formula contains a Formula as an argument to
	 * other than a logical operator.
	 *
	 * @return whether a Formula contains a Formula as an argument to other than a logical operator.
	 */
	public boolean isHigherOrder()
	{
		if (this.listP())
		{
			String pred = this.car();
			boolean logOp = isLogicalOperator(pred);
			List<String> al = literalToList();
			for (int i = 1; i < al.size(); i++)
			{
				String arg = al.get(i);
				Formula f = new Formula();
				f.set(arg);
				if (!atom(arg) && !f.isFunctionalTerm())
				{
					if (logOp)
					{
						if (f.isHigherOrder())
							return true;
					}
					else
						return true;
				}
			}
		}
		return false;
	}

	/**
	 * Test whether an Object is a variable
	 *
	 * @param term term
	 * @return whether an Object is a variable
	 */
	public static boolean isVariable(String term)
	{
		return isNonEmpty(term) && (term.startsWith(V_PREF) || term.startsWith(R_PREF));
	}

	/**
	 * Test whether the formula is a variable
	 *
	 * @return whether this formula is a variable
	 */
	public boolean isVariable()
	{
		return isVariable(this.text);
	}

	/**
	 * Returns true only if this Formula, explicitly quantified or
	 * not, starts with "=&gt;" or "&lt;=&gt;", else returns false.  It would
	 * be better to test for the occurrence of at least one positive
	 * literal with one or more negative literals, but this test would
	 * require converting the Formula to clausal form.
	 *
	 * @return whether this Formula is a rule.
	 */
	public boolean isRule()
	{
		boolean result = false;
		if (this.listP())
		{
			String arg0 = this.car();
			if (isQuantifier(arg0))
			{
				String arg2 = this.getArgument(2);
				if (Formula.listP(arg2))
				{
					Formula newF = new Formula();
					newF.set(arg2);
					result = newF.isRule();
				}
			}
			else
			{
				result = Arrays.asList(IF, IFF).contains(arg0);
			}
		}
		return result;
	}

	/**
	 * Test whether a list with a predicate is a quantifier list
	 *
	 * @param listPred     list with a predicate.
	 * @param previousPred previous predicate
	 * @return whether a list with a predicate is a quantifier list.
	 */
	@SuppressWarnings("BooleanMethodIsAlwaysInverted") static public boolean isQuantifierList(String listPred, String previousPred)
	{
		return (previousPred.equals(EQUANT) || previousPred.equals(UQUANT)) && (listPred.startsWith(R_PREF) || listPred.startsWith(V_PREF));
	}

	/**
	 * Test whether a Formula is a simple list of terms (including functional terms).
	 *
	 * @return whether a Formula is a simple list of terms
	 */
	public boolean isSimpleClause()
	{
		logger.entering("Formula", "isSimpleClause");
		Formula f = new Formula();
		f.set(text);
		while (!f.empty())
		{
			if (listP(f.car()))
			{
				Formula f2 = new Formula();
				f2.set(f.car());
				if (!Formula.isFunction(f2.car()))
					return false;
				else if (!f2.isSimpleClause())
					return false;
			}
			f.set(f.cdr());
		}
		return true;
	}

	/**
	 * Test whether a Formula is a simple clause wrapped in a negation.
	 *
	 * @return whether a Formula is a simple clause wrapped in a negation.
	 */
	public boolean isSimpleNegatedClause()
	{
		Formula f = new Formula();
		f.set(text);
		if (f.empty() || f.atom())
			return false;
		if (f.car().equals("not"))
		{
			f.set(f.cdr());
			if (empty(f.cdr()))
			{
				f.set(f.car());
				return f.isSimpleClause();
			}
			else
				return false;
		}
		else
			return false;
	}

	/**
	 * Test whether a formula is valid with no variable
	 *
	 * @param form formula string
	 * @return true if formula is a valid formula with no variables, else returns false.
	 */
	public static boolean isGround(String form)
	{
		if (isEmpty(form))
			return false;
		if (!form.contains("\""))
			return !form.contains("?") && !form.contains("@");
		boolean inQuote = false;
		for (int i = 0; i < form.length(); i++)
		{
			if (form.charAt(i) == '"')
				inQuote = !inQuote;
			if ((form.charAt(i) == '?' || form.charAt(i) == '@') && !inQuote)
				return false;
		}
		return true;
	}

	/**
	 * Test whether term is a logical quantifier
	 *
	 * @param term A String, assumed to be an atomic SUO-KIF term.
	 * @return true if term is a logical quantifier
	 */
	public static boolean isQuantifier(String term)
	{
		return isNonEmpty(term) && (term.equals(EQUANT) || term.equals(UQUANT));
	}

	/**
	 * Test whether term is a logical operator
	 *
	 * @param term A String, assumed to be an atomic SUO-KIF term.
	 * @return true if term is a standard FOL logical operator, else returns false.
	 */
	public static boolean isLogicalOperator(String term)
	{
		return isNonEmpty(term) && LOGICAL_OPERATORS.contains(term);
	}

	/**
	 * Test whether term is a comparison operator
	 *
	 * @param term A String, assumed to be an atomic SUO-KIF term.
	 * @return true if term is a SUO-KIF predicate for comparing two (typically numeric) terms, else returns false.
	 */
	@SuppressWarnings("BooleanMethodIsAlwaysInverted") public static boolean isComparisonOperator(String term)
	{
		return isNonEmpty(term) && COMPARISON_OPERATORS.contains(term);
	}

	/**
	 * Test whether term is a function
	 *
	 * @param term A String.
	 * @return true if term is a SUO-KIF function, else returns false.
	 * Note that this test is purely syntactic, and could fail for functions that do not adhere to the convention of ending all
	 * functions with "Fn".
	 */
	public static boolean isFunction(String term)
	{
		return isNonEmpty(term) && term.endsWith(FN_SUFF);
	}

	/**
	 * Test whether term is a math function
	 *
	 * @param term A String, assumed to be an atomic SUO-KIF term.
	 * @return true if term is a SUO-KIF mathematical function, else returns false.
	 */
	@SuppressWarnings("BooleanMethodIsAlwaysInverted") public static boolean isMathFunction(String term)
	{
		return isNonEmpty(term) && MATH_FUNCTIONS.contains(term);
	}

	/**
	 * Test whether term is commutative
	 *
	 * @param term A String, assumed to be an atomic SUO-KIF term.
	 * @return true if term is a SUO-KIF commutative logical operator, else false.
	 */
	public static boolean isCommutative(String term)
	{
		return isNonEmpty(term) && (term.equals(AND) || term.equals(OR));
	}

	/**
	 * @param term A String.
	 * @return true if term is a SUO-KIF Skolem term, else returns false.
	 */
	public static boolean isSkolemTerm(String term)
	{
		return isNonEmpty(term) && term.trim().matches("^.?" + SK_PREF + "\\S*\\s*\\d+");
	}

	// T Y P E

	/**
	 * A + is appended to the type if the parameter must be a class
	 *
	 * @return the type for each argument to the given predicate, where
	 * List element 0 is the result, if a function, 1 is the
	 * first argument, 2 is the second etc.
	 */
	private List<String> getTypeList(String pred, KB kb)
	{
		List<String> result;

		// build the sortalTypeCache key.
		String key = "gtl" + pred + kb.name;
		Map<String, List<String>> stc = kb.getSortalTypeCache();
		result = stc.get(key);
		if (result == null)
		{
			int valence = kb.getValence(pred);
			int len = MAX_PREDICATE_ARITY + 1;
			if (valence == 0)
			{
				len = 2;
			}
			else if (valence > 0)
			{
				len = valence + 1;
			}

			List<Formula> al = kb.askWithRestriction(0, "domain", 1, pred);
			List<Formula> al2 = kb.askWithRestriction(0, "domainSubclass", 1, pred);
			List<Formula> al3 = kb.askWithRestriction(0, "range", 1, pred);
			List<Formula> al4 = kb.askWithRestriction(0, "rangeSubclass", 1, pred);

			String[] r = new String[len];
			addToTypeList(pred, al, r, false);
			addToTypeList(pred, al2, r, true);
			addToTypeList(pred, al3, r, false);
			addToTypeList(pred, al4, r, true);
			result = new ArrayList<>(Arrays.asList(r));

			stc.put(key, result);
		}
		return result;
	}

	/**
	 * A utility helper method for computing predicate data types.
	 */
	@SuppressWarnings("UnusedReturnValue") private String[] addToTypeList(String pred, List<Formula> al, String[] result, boolean classP)
	{
		if (logger.isLoggable(Level.FINER))
		{
			String[] params = { "pred = " + pred, "al = " + al, "result = " + Arrays.toString(result), "classP = " + classP };
			logger.entering("Formula", "addToTypeList", params);
		}
		// If the relations in al start with "(range", argnum will be 0, and the arg position of the desired classnames will be 2.
		int argnum = 0;
		int clPos = 2;
		for (Formula f : al)
		{
			// logger.finest("text: " + f.text);
			if (f.text.startsWith("(domain"))
			{
				argnum = Integer.parseInt(f.getArgument(2));
				clPos = 3;
			}
			String cl = f.getArgument(clPos);
			if ((argnum < 0) || (argnum >= result.length))
			{
				String errStr = "Possible arity confusion for " + pred;
				logger.warning(errStr);
				errors.add(errStr);
			}
			else if (isEmpty(result[argnum]))
			{
				if (classP)
				{
					cl += "+";
				}
				result[argnum] = cl;
			}
			else
			{
				if (!cl.equals(result[argnum]))
				{
					String errStr = "Multiple types asserted for argument " + argnum + " of " + pred + ": " + cl + ", " + result[argnum];
					logger.warning(errStr);
					errors.add(errStr);
				}
			}
		}
		return result;
	}

	/**
	 * Find the argument type restriction for a given predicate and
	 * argument number that is inherited from one of its
	 * super-relations.  A "+" is appended to the type if the
	 * parameter must be a class.  Argument number 0 is used for the
	 * return type of a Function.
	 *
	 * @param argIdx argument index
	 * @param pred   predicate
	 * @param kb     knowledge base
	 * @return type restriction
	 */
	public static String findType(int argIdx, String pred, KB kb)
	{
		if (logger.isLoggable(Level.FINER))
		{
			String[] params = { "numarg = " + argIdx, "pred = " + pred, "kb = " + kb.name };
			logger.entering("Formula", "findType", params);
		}
		String result;

		// build the sortalTypeCache key.
		String key = "ft" + argIdx + pred + kb.name;

		Map<String, List<String>> stc = kb.getSortalTypeCache();
		List<String> results = stc.get(key);
		boolean isCached = results != null && !results.isEmpty();
		result = isCached ? results.get(0) : null;
		boolean cacheResult = !isCached;

		if (result == null)
		{
			List<String> relations = new ArrayList<>();
			boolean found = false;
			Set<String> accumulator = new HashSet<>();
			accumulator.add(pred);

			while (!found && !accumulator.isEmpty())
			{
				relations.clear();
				relations.addAll(accumulator);
				accumulator.clear();

				for (String relation : relations)
				{
					if (found)
						break;
					if (argIdx > 0)
					{
						List<Formula> formulas = kb.askWithRestriction(0, "domain", 1, relation);
						for (Formula f : formulas)
						{
							int argnum = Integer.parseInt(f.getArgument(2));
							if (argnum == argIdx)
							{
								result = f.getArgument(3);
								found = true;
								break;
							}
						}
						if (!found)
						{
							formulas = kb.askWithRestriction(0, "domainSubclass", 1, relation);
							for (Formula f : formulas)
							{
								int argnum = Integer.parseInt(f.getArgument(2));
								if (argnum == argIdx)
								{
									result = f.getArgument(3) + "+";
									found = true;
									break;
								}
							}
						}
					}
					else if (argIdx == 0)
					{
						List<Formula> formulas = kb.askWithRestriction(0, "range", 1, relation);
						if (!formulas.isEmpty())
						{
							Formula f = formulas.get(0);
							result = f.getArgument(2);
							found = true;
						}
						if (!found)
						{
							formulas = kb.askWithRestriction(0, "rangeSubclass", 1, relation);
							if (!formulas.isEmpty())
							{
								Formula f = formulas.get(0);
								result = f.getArgument(2) + "+";
								found = true;
							}
						}
					}
				}
				if (!found)
				{
					for (String r : relations)
						accumulator.addAll(kb.getTermsViaAskWithRestriction(1, r, 0, "subrelation", 2));
				}
			}
			if (cacheResult && (result != null))
				stc.put(key, Collections.singletonList(result));
		}
		logger.exiting("Formula", "findType", result);
		return result;
	}

	/**
	 * This method tries to remove all but the most specific relevant
	 * classes from a List of sortal classes.
	 *
	 * @param types A List of classes (class name Strings) that
	 *              constrain the value of a SUO-KIF variable.
	 * @param kb    The KB used to determine if any of the classes in the
	 *              List types are redundant.
	 */
	private void winnowTypeList(List<String> types, KB kb)
	{
		if (logger.isLoggable(Level.FINER))
		{
			String[] params = { "types = " + types, "kb = " + kb.name };
			logger.entering("Formula", "winnowTypeList", params);
		}
		if ((types != null) && (types.size() > 1))
		{
			String[] valArr = types.toArray(new String[0]);
			for (int i = 0; i < valArr.length; i++)
			{
				boolean stop = false;
				for (int j = 0; j < valArr.length; j++)
				{
					if (i != j)
					{
						String clX = valArr[i];
						String clY = valArr[j];
						if (kb.isSubclass(clX, clY))
						{
							types.remove(clY);
							if (types.size() < 2)
							{
								stop = true;
								break;
							}
						}
					}
				}
				if (stop)
					break;
			}
		}
		logger.exiting("Formula", "winnowTypeList");
	}

	/**
	 * Does much of the real work for addTypeRestrictions() by
	 * recursing through the Formula and collecting type constraint
	 * information for the variable var.
	 *
	 * @param ios A List of classes (class name Strings) of which any
	 *            binding for var must be an instance.
	 * @param scs A List of classes (class name Strings) of which any
	 *            binding for var must be a subclass.
	 * @param var A SUO-KIF variable.
	 * @param kb  The KB used to determine predicate and variable arg
	 *            types.
	 */
	private void computeTypeRestrictions(List<String> ios, List<String> scs, String var, KB kb)
	{
		if (logger.isLoggable(Level.FINER))
		{
			String[] params = { "ios = " + ios, "scs = " + scs, "var = " + var, "kb = " + kb.name };
			logger.entering("Formula", "computeTypeRestrictions", params);
		}
		if (!this.listP() || !this.text.contains(var))
			return;
		Formula f = new Formula();
		f.set(this.text);
		String pred = f.car();
		if (isQuantifier(pred))
		{
			String arg2 = f.getArgument(2);
			if (arg2.contains(var))
			{
				Formula nextF = new Formula();
				nextF.set(arg2);
				nextF.computeTypeRestrictions(ios, scs, var, kb);
			}
		}
		else if (isLogicalOperator(pred))
		{
			int len = f.listLength();
			for (int i = 1; i < len; i++)
			{
				String argI = f.getArgument(i);
				if (argI.contains(var))
				{
					Formula nextF = new Formula();
					nextF.set(argI);
					nextF.computeTypeRestrictions(ios, scs, var, kb);
				}
			}
		}
		else
		{
			int valence = kb.getValence(pred);
			List<String> types = getTypeList(pred, kb);
			int len = f.listLength();
			for (int i = 1; i < len; i++)
			{
				int argIdx = i;
				if (valence == 0) // pred is a VariableArityRelation
					argIdx = 1;
				String arg = f.getArgument(i);
				if (arg.contains(var))
				{
					if (listP(arg))
					{
						Formula nextF = new Formula();
						nextF.set(arg);
						nextF.computeTypeRestrictions(ios, scs, var, kb);
					}
					else if (var.equals(arg))
					{
						String type = null;
						if (argIdx < types.size())
							type = types.get(argIdx);
						if (type == null)
							type = findType(argIdx, pred, kb);
						if (isNonEmpty(type) && !type.startsWith("Entity"))
						{
							boolean sc = false;
							while (type.endsWith("+"))
							{
								sc = true;
								type = type.substring(0, type.length() - 1);
							}
							if (sc)
							{
								if (!scs.contains(type))
									scs.add(type);
							}
							else if (!ios.contains(type))
								ios.add(type);
						}
					}
				}
			}
			// Special treatment for equal
			if (pred.equals("equal"))
			{
				String arg1 = f.getArgument(1);
				String arg2 = f.getArgument(2);
				String term = null;
				if (var.equals(arg1))
				{
					term = arg2;
				}
				else if (var.equals(arg2))
				{
					term = arg1;
				}
				if (isNonEmpty(term))
				{
					if (listP(term))
					{
						Formula nextF = new Formula();
						nextF.set(term);
						if (nextF.isFunctionalTerm())
						{
							String fn = nextF.car();
							List<String> classes = getTypeList(fn, kb);
							String cl = null;
							if (!classes.isEmpty())
								cl = classes.get(0);
							if (cl == null)
								cl = findType(0, fn, kb);
							if (isNonEmpty(cl) && !cl.startsWith("Entity"))
							{
								boolean sc = false;
								while (cl.endsWith("+"))
								{
									sc = true;
									cl = cl.substring(0, cl.length() - 1);
								}
								if (sc)
								{
									if (!scs.contains(cl))
										scs.add(cl);
								}
								else if (!ios.contains(cl))
									ios.add(cl);
							}
						}
					}
					else
					{
						Set<String> instanceOfs = kb.getCachedRelationValues("instance", term, 1, 2);
						if ((instanceOfs != null) && !instanceOfs.isEmpty())
						{
							for (String io : instanceOfs)
							{
								if (!io.equals("Entity") && !ios.contains(io))
									ios.add(io);
							}
						}
					}
				}
			}
			// Special treatment for instance or subclass, only if var.equals(arg1) and arg2 is a functional term.
			else if (Arrays.asList("instance", "subclass").contains(pred))
			{
				String arg1 = f.getArgument(1);
				String arg2 = f.getArgument(2);
				if (var.equals(arg1) && listP(arg2))
				{
					Formula nextF = new Formula();
					nextF.set(arg2);
					if (nextF.isFunctionalTerm())
					{
						String fn = nextF.car();
						List<String> classes = getTypeList(fn, kb);
						String cl = null;
						if (!classes.isEmpty())
							cl = classes.get(0);
						if (cl == null)
							cl = findType(0, fn, kb);
						if (isNonEmpty(cl) && !cl.startsWith("Entity"))
						{
							while (cl.endsWith("+"))
								cl = cl.substring(0, cl.length() - 1);
							if (pred.equals("subclass"))
							{
								if (!scs.contains(cl))
									scs.add(cl);
							}
							else if (!ios.contains(cl))
								ios.add(cl);
						}
					}
				}
			}
		}
		logger.exiting("Formula", "computeTypeRestrictions");
	}

	/**
	 * When invoked on a Formula that begins with explicit universal
	 * quantification, this method returns a String representation of
	 * the Formula with type constraints added for the top level
	 * quantified variables, if possible.  Otherwise, a String
	 * representation of the original Formula is returned.
	 *
	 * @param shelf A List of quaternary Lists, each of which
	 *              contains type information about a variable
	 * @param kb    The KB used to determine predicate and variable arg
	 *              types.
	 * @return A String representation of a Formula, with type
	 * restrictions added.
	 */
	private String insertTypeRestrictionsU(List<Tuple.Quad<String, String, List<String>, List<String>>> shelf, KB kb)
	{
		if (logger.isLoggable(Level.FINER))
		{
			String[] params = { "shelf = " + shelf, "kb = " + kb.name };
			logger.entering("Formula", "insertTypeRestrictionsU", params);
		}
		String result;
		String varList = this.getArgument(1);
		Formula varListF = new Formula();
		varListF.set(varList);

		List<Tuple.Quad<String, String, List<String>, List<String>>> newShelf = makeNewShelf(shelf);
		int vLen = varListF.listLength();
		for (int i = 0; i < vLen; i++)
			addVarDataQuad(varListF.getArgument(i), "U", newShelf);

		String arg2 = this.getArgument(2);
		Formula nextF = new Formula();
		nextF.set(arg2);
		String processedArg2 = nextF.insertTypeRestrictionsR(newShelf, kb);
		Set<String> constraints = new LinkedHashSet<>();

		for (Tuple.Quad<String, String, List<String>, List<String>> quad : newShelf)
		{
			String var = quad.first;
			String token = quad.second;
			if (token.equals("U"))
			{
				List<String> ios = quad.third;
				List<String> scs = quad.fourth;
				if (!scs.isEmpty())
				{
					winnowTypeList(scs, kb);
					if (!scs.isEmpty())
					{
						if (!ios.contains("SetOrClass"))
							ios.add("SetOrClass");
						for (String sc : scs)
						{
							String constraint = "(subclass " + var + " " + sc + ")";
							if (!processedArg2.contains(constraint))
								constraints.add(constraint);
						}
					}
				}
				if (!ios.isEmpty())
				{
					winnowTypeList(ios, kb);
					for (String io : ios)
					{
						String constraint = "(instance " + var + " " + io + ")";
						if (!processedArg2.contains(constraint))
							constraints.add(constraint);
					}
				}
			}
		}
		StringBuilder sb = new StringBuilder();
		sb.append("(forall ");
		sb.append(varListF.text);
		if (constraints.isEmpty())
		{
			sb.append(" ");
			sb.append(processedArg2);
		}
		else
		{
			sb.append(" (=>");
			int cLen = constraints.size();
			if (cLen > 1)
				sb.append(" (and");
			for (String constraint : constraints)
			{
				sb.append(" ");
				sb.append(constraint);
			}
			if (cLen > 1)
				sb.append(")");
			sb.append(" ");
			sb.append(processedArg2);
			sb.append(")");
		}
		sb.append(")");
		result = sb.toString();
		logger.exiting("Formula", "insertTypeRestrictionsU", result);
		return result;
	}

	/**
	 * When invoked on a Formula that begins with explicit existential
	 * quantification, this method returns a String representation of
	 * the Formula with type constraints added for the top level
	 * quantified variables, if possible.  Otherwise, a String
	 * representation of the original Formula is returned.
	 *
	 * @param shelf A List of quaternary Lists, each of which
	 *              contains type information about a variable
	 * @param kb    The KB used to determine predicate and variable arg
	 *              types.
	 * @return A String representation of a Formula, with type
	 * restrictions added.
	 */
	private String insertTypeRestrictionsE(List<Tuple.Quad<String, String, List<String>, List<String>>> shelf, KB kb)
	{
		if (logger.isLoggable(Level.FINER))
		{
			String[] params = { "shelf = " + shelf, "kb = " + kb.name };
			logger.entering("Formula", "insertTypeRestrictionsE", params);
		}
		String result;
		String varList = this.getArgument(1);
		Formula varListF = new Formula();
		varListF.set(varList);

		List<Tuple.Quad<String, String, List<String>, List<String>>> newShelf = makeNewShelf(shelf);
		int vLen = varListF.listLength();
		for (int i = 0; i < vLen; i++)
			addVarDataQuad(varListF.getArgument(i), "E", newShelf);

		String arg2 = this.getArgument(2);
		Formula nextF = new Formula();
		nextF.set(arg2);

		String processedArg2 = nextF.insertTypeRestrictionsR(newShelf, kb);
		nextF.set(processedArg2);

		Set<String> constraints = new LinkedHashSet<>();
		StringBuilder sb = new StringBuilder();

		for (Tuple.Quad<String, String, List<String>, List<String>> quad : newShelf)
		{
			String var = quad.first;
			String token = quad.second;
			if (token.equals("E"))
			{
				List<String> ios = quad.third;
				List<String> scs = quad.fourth;
				if (!ios.isEmpty())
				{
					winnowTypeList(ios, kb);
					for (String io : ios)
					{
						sb.setLength(0);
						sb.append("(instance ").append(var).append(" ").append(io).append(")");
						String constraint = sb.toString();
						if (!processedArg2.contains(constraint))
						{
							constraints.add(constraint);
						}
					}
				}
				if (!scs.isEmpty())
				{
					winnowTypeList(scs, kb);
					for (String sc : scs)
					{
						sb.setLength(0);
						sb.append("(subclass ").append(var).append(" ").append(sc).append(")");
						String constraint = sb.toString();
						if (!processedArg2.contains(constraint))
						{
							constraints.add(constraint);
						}
					}
				}
			}
		}
		sb.setLength(0);
		sb.append("(exists ");
		sb.append(varListF.text);
		if (constraints.isEmpty())
		{
			sb.append(" ");
			sb.append(processedArg2);
		}
		else
		{
			sb.append(" (and");
			for (String constraint : constraints)
			{
				sb.append(" ");
				sb.append(constraint);
			}
			if (nextF.car().equals("and"))
			{
				int nextFLen = nextF.listLength();
				for (int k = 1; k < nextFLen; k++)
				{
					sb.append(" ");
					sb.append(nextF.getArgument(k));
				}
			}
			else
			{
				sb.append(" ");
				sb.append(nextF.text);
			}
			sb.append(")");
		}
		sb.append(")");
		result = sb.toString();
		logger.exiting("Formula", "insertTypeRestrictionsE", result);
		return result;
	}

	/**
	 * When invoked on a Formula, this method returns a String
	 * representation of the Formula with type constraints added for
	 * all explicitly quantified variables, if possible.  Otherwise, a
	 * String representation of the original Formula is returned.
	 *
	 * @param shelf A List, each element of which is a quaternary List
	 *              containing a SUO-KIF variable String, a token "U" or "E"
	 *              indicating how the variable is quantified, a List of instance
	 *              classes, and a List of subclass classes
	 * @param kb    The KB used to determine predicate and variable arg
	 *              types.
	 * @return A String representation of a Formula, with type
	 * restrictions added.
	 */
	private String insertTypeRestrictionsR(List<Tuple.Quad<String, String, List<String>, List<String>>> shelf, KB kb)
	{
		if (logger.isLoggable(Level.FINER))
		{
			String[] params = { "shelf = " + shelf, "kb = " + kb.name };
			logger.entering("Formula", "insertTypeRestrictionsR", params);
		}
		String result = this.text;
		if (listP(this.text) && !empty(this.text) && this.text.matches(".*\\?\\w+.*"))
		{
			StringBuilder sb = new StringBuilder();
			Formula f = new Formula();
			f.set(this.text);
			int len = f.listLength();
			String arg0 = f.car();
			if (isQuantifier(arg0) && (len == 3))
			{
				if (arg0.equals("forall"))
					sb.append(f.insertTypeRestrictionsU(shelf, kb));
				else
					sb.append(f.insertTypeRestrictionsE(shelf, kb));
			}
			else
			{
				sb.append("(");
				for (int i = 0; i < len; i++)
				{
					String argI = f.getArgument(i);
					if (i > 0)
					{
						sb.append(" ");
						if (isVariable(argI))
						{
							String type = findType(i, arg0, kb);
							if (isNonEmpty(type) && !type.startsWith("Entity"))
							{
								boolean sc = false;
								while (type.endsWith("+"))
								{
									sc = true;
									type = type.substring(0, type.length() - 1);
								}
								if (sc)
									addScForVar(argI, type, shelf);
								else
									addIoForVar(argI, type, shelf);
							}
						}
					}
					Formula nextF = new Formula();
					nextF.set(argI);
					sb.append(nextF.insertTypeRestrictionsR(shelf, kb));
				}
				sb.append(")");
			}
			result = sb.toString();
		}
		logger.exiting("Formula", "insertTypeRestrictionsR", result);
		return result;
	}

	/**
	 * Add var data quad
	 */
	private void addVarDataQuad(String var, String quantToken, List<Tuple.Quad<String, String, List<String>, List<String>>> shelf)
	{
		Tuple.Quad<String, String, List<String>, List<String>> quad = new Tuple.Quad<>();
		quad.first = var;                // e.g., "?X"
		quad.second = quantToken;        // "U" or "E"
		quad.third = new ArrayList<>();  // ios
		quad.fourth = new ArrayList<>();  // scs
		shelf.add(0, quad);
	}

	/**
	 * Ios
	 */
	private List<String> getIosForVar(String var, List<Tuple.Quad<String, String, List<String>, List<String>>> shelf)
	{
		List<String> result = null;
		for (Tuple.Quad<String, String, List<String>, List<String>> quad : shelf)
		{
			if (var.equals(quad.first))
			{
				result = quad.third;
				break;
			}
		}
		return result;
	}

	/**
	 * Scs
	 */
	private List<String> getScsForVar(String var, List<Tuple.Quad<String, String, List<String>, List<String>>> shelf)
	{
		List<String> result = null;
		for (Tuple.Quad<String, String, List<String>, List<String>> quad : shelf)
		{
			if (var.equals(quad.first))
			{
				result = quad.fourth;
				break;
			}
		}
		return result;
	}

	/**
	 * Add Io
	 */
	private void addIoForVar(String var, String io, List<Tuple.Quad<String, String, List<String>, List<String>>> shelf)
	{
		if (isNonEmpty(io))
		{
			List<String> ios = getIosForVar(var, shelf);
			if ((ios != null) && !ios.contains(io))
			{
				ios.add(io);
			}
		}
	}

	/**
	 * Add Sc
	 */
	private void addScForVar(String var, String sc, List<Tuple.Quad<String, String, List<String>, List<String>>> shelf)
	{
		if (isNonEmpty(sc))
		{
			List<String> scs = getScsForVar(var, shelf);
			if ((scs != null) && !scs.contains(sc))
			{
				scs.add(sc);
			}
		}
	}

	/**
	 * Copy shelf
	 */
	private List<Tuple.Quad<String, String, List<String>, List<String>>> makeNewShelf(List<Tuple.Quad<String, String, List<String>, List<String>>> shelf)
	{
		return new ArrayList<>(shelf);
	}

	/**
	 * Add clauses for every variable in the antecedent to restrict its
	 * type to the type restrictions defined on every relation in which
	 * it appears.  For example
	 * (=>
	 * (foo ?A B)
	 * (bar B ?A))
	 * (domain foo 1 Z)
	 * would result in
	 * (=>
	 * (instance ?A Z)
	 * (=>
	 * (foo ?A B)
	 * (bar B ?A)))
	 */
	String addTypeRestrictions(KB kb)
	{
		logger.entering("Formula", "addTypeRestrictions", kb.name);
		String result;
		Formula f = new Formula();
		f.set(this.makeQuantifiersExplicit(false));
		// logger.finest("f == " + f);
		result = f.insertTypeRestrictionsR(new ArrayList<>(), kb);
		logger.exiting("Formula", "addTypeRestrictions", result);
		return result;
	}

	/**
	 * A recursive utility method used to collect type information for
	 * the variables in this Formula.
	 *
	 * @param map A Map used to store type information for the
	 *            variables in this Formula.
	 * @param kb  The KB used to compute the sortal constraints for
	 *            each variable.
	 */
	public void computeVariableTypesR(Map<String, List<List<String>>> map, KB kb)
	{
		if (logger.isLoggable(Level.FINER))
		{
			String[] params = { "map = " + map, "kb = " + kb.name };
			logger.entering("Formula", "computeVariableTypesR", params);
		}
		if (this.listP() && !this.empty())
		{
			int len = this.listLength();
			String arg0 = this.car();
			if (isQuantifier(arg0) && (len == 3))
				this.computeVariableTypesQ(map, kb);
			else
			{
				for (int i = 0; i < len; i++)
				{
					Formula nextF = new Formula();
					nextF.set(this.getArgument(i));
					nextF.computeVariableTypesR(map, kb);
				}
			}
		}
		logger.exiting("Formula", "computeVariableTypesR");
	}

	/**
	 * A recursive utility method used to collect type information for
	 * the variables in this Formula, which is assumed to have forall
	 * or exists as its arg0.
	 *
	 * @param map A Map used to store type information for the
	 *            variables in this Formula.
	 * @param kb  The KB used to compute the sortal constraints for
	 *            each variable.
	 */
	private void computeVariableTypesQ(Map<String, List<List<String>>> map, KB kb)
	{
		if (logger.isLoggable(Level.FINER))
		{
			String[] params = { "map = " + map, "kb = " + kb.name };
			logger.entering("Formula", "computeVariableTypesQ", params);
		}
		Formula varListF = new Formula();
		varListF.set(this.getArgument(1));

		Formula nextF = new Formula();
		nextF.set(this.getArgument(2));

		int vLen = varListF.listLength();
		for (int i = 0; i < vLen; i++)
		{
			List<List<String>> types = new ArrayList<>();
			List<String> ios = new ArrayList<>();
			List<String> scs = new ArrayList<>();
			String var = varListF.getArgument(i);
			nextF.computeTypeRestrictions(ios, scs, var, kb);
			if (!scs.isEmpty())
			{
				winnowTypeList(scs, kb);
				if (!scs.isEmpty() && !ios.contains("SetOrClass"))
					ios.add("SetOrClass");
			}
			if (!ios.isEmpty())
				winnowTypeList(ios, kb);
			types.add(ios);
			types.add(scs);
			map.put(var, types);
		}
		nextF.computeVariableTypesR(map, kb);
		logger.exiting("Formula", "computeVariableTypesQ");
	}

	/**
	 * Tries to successively instantiate predicate variables and then
	 * expand row variables in this Formula, looping until no new
	 * Formulae are generated.
	 *
	 * @param kb             The KB to be used for processing this Formula
	 * @param addHoldsPrefix If true, predicate variables are not
	 *                       instantiated
	 * @return an List of Formula(s), which could be empty.
	 */
	List<Formula> replacePredVarsAndRowVars(KB kb, boolean addHoldsPrefix)
	{
		if (logger.isLoggable(Level.FINER))
		{
			String[] params = { "kb = " + kb.name, "addHoldsPrefix = " + addHoldsPrefix };
			logger.entering("Formula", "replacePredVarsAndRowVars", params);
		}
		Formula startF = new Formula();
		startF.set(this.text);

		int prevAccumulatorSize = 0;
		Set<Formula> accumulator = new LinkedHashSet<>();
		accumulator.add(startF);
		while (accumulator.size() != prevAccumulatorSize)
		{
			prevAccumulatorSize = accumulator.size();

			// Do pred var instantiations if we are not adding holds prefixes.
			if (!addHoldsPrefix)
			{
				List<Formula> working = new ArrayList<>(accumulator);
				accumulator.clear();

				for (Formula f : working)
				{
					try
					{
						List<Formula> instantiations = f.instantiatePredVars(kb);
						errors.addAll(f.getErrors());

						// logger.finest("instantiations == " + instantiations);
						if (instantiations.isEmpty())
						{
							// If the accumulator is empty -- no pred var instantiations were possible -- add
							// the original formula to the accumulator for possible row var expansion below.
							accumulator.add(f);
						}
						else
						{
							// It might not be possible to instantiate all pred vars until
							// after row vars have been expanded, so we loop until no new formulae
							// are being generated.
							accumulator.addAll(instantiations);
						}
					}
					catch (Reject r)
					{
						// If the formula can't be instantiated at all and so has been thrown "reject", don't add anything.
						String errStr = "No predicate instantiations for ";
						logger.warning(errStr + " " + f);
						errStr += f.text;
						errors.add(errStr);
					}
				}
			}
			// Row var expansion. Iterate over the instantiated predicate formulas,
			// doing row var expansion on each.  If no predicate instantiations can be generated, the accumulator
			// will contain just the original input formula.
			if (!accumulator.isEmpty() && (accumulator.size() < AXIOM_EXPANSION_LIMIT))
			{
				List<Formula> working = new ArrayList<>(accumulator);
				accumulator.clear();
				for (Formula f : working)
				{
					accumulator.addAll(f.expandRowVars(kb));
					// logger.finest("f == " + f);
					// logger.finest("accumulator == " + accumulator);
					if (accumulator.size() > AXIOM_EXPANSION_LIMIT)
					{
						logger.warning("  AXIOM_EXPANSION_LIMIT EXCEEDED: " + AXIOM_EXPANSION_LIMIT);
						break;
					}
				}
			}
		}
		List<Formula> result = new ArrayList<>(accumulator);
		logger.exiting("Formula", "replacePredVarsAndRowVars", result);
		return result;
	}

	/**
	 * Adds statements of the form (instance &lt;Entity> &lt;SetOrClass>) if
	 * they are not already in the KB.
	 *
	 * @param kb                   The KB to be used for processing the input Formulae
	 *                             in variableReplacements
	 * @param isQuery              If true, this method just returns the initial
	 *                             input List, variableReplacements, with no additions
	 * @param variableReplacements A List of Formulae in which
	 *                             predicate variables and row variables have already been
	 *                             replaced, and to which (instance <Entity> <SetOrClass>)
	 *                             Formulae might be added
	 * @return an List of Formula(s), which could be larger than
	 * the input List, variableReplacements, or could be empty.
	 */
	List<Formula> addInstancesOfSetOrClass(KB kb, boolean isQuery, List<Formula> variableReplacements)
	{
		List<Formula> result = new ArrayList<>();
		if ((variableReplacements != null) && !variableReplacements.isEmpty())
		{
			if (isQuery)
				result.addAll(variableReplacements);
			else
			{
				Set<Formula> formulae = new LinkedHashSet<>();
				for (Formula f : variableReplacements)
				{
					formulae.add(f);

					// Make sure every SetOrClass is stated to be such.
					if (f.listP() && !f.empty())
					{
						String arg0 = f.car();
						int start = -1;
						if (arg0.equals("subclass"))
							start = 0;
						else if (arg0.equals("instance"))
							start = 1;
						if (start > -1)
						{
							List<String> args = Arrays.asList(f.getArgument(1), f.getArgument(2));
							int argsLen = args.size();
							for (int i = start; i < argsLen; i++)
							{
								String arg = args.get(i);
								if (!isVariable(arg) && !arg.equals("SetOrClass") && atom(arg))
								{
									StringBuilder sb = new StringBuilder();
									sb.setLength(0);
									sb.append("(instance ");
									sb.append(arg);
									sb.append(" SetOrClass)");
									Formula ioF = new Formula();
									String ioStr = sb.toString().intern();
									ioF.set(ioStr);
									ioF.sourceFile = this.sourceFile;
									if (!kb.formulaMap.containsKey(ioStr))
									{
										Map<String, List<String>> stc = kb.getSortalTypeCache();
										if (stc.get(ioStr) == null)
										{
											stc.put(ioStr, Collections.singletonList(ioStr));
											formulae.add(ioF);
										}
									}
								}
							}
						}
					}
				}
				result.addAll(formulae);
			}
		}
		return result;
	}

	/**
	 * Returns true if this Formula appears not to have any of the
	 * characteristics that would cause it to be rejected during
	 * translation to TPTP form, or cause problems during inference.
	 * Otherwise, returns false.
	 *
	 * @param query true if this Formula represents a query, else
	 *              false.
	 * @return boolean
	 */
	boolean isOkForInference(boolean query)
	{
		// kb isn't used yet, because the checks below are purely
		// syntactic.  But it probably will be used in the future.
		// (<relation> ?X ...) - no free variables in an
		// atomic formula that doesn't contain a string
		// unless the formula is a query.
		// The formula does not contain a string.
		// The formula contains a free variable.
		// ... add more patterns here, as needed.
		return !(// (equal ?X ?Y ?Z ...) - equal is strictly binary.
				// No longer necessary?  NS: 2009-06-12
				// this.text.matches(".*\\(\\s*equal\\s+\\?*\\w+\\s+\\?*\\w+\\s+\\?*\\w+.*")

				// The formula contains non-ASCII characters.
				// was: this.ttext.matches(".*[\\x7F-\\xFF].*")
				// ||
				StringUtil.containsNonAsciiChars(this.text) || (!query && !isLogicalOperator(this.car()) && (this.text.indexOf('"') == -1) && this.text
						.matches(".*\\?\\w+.*")));
	}

	// A R I T Y

	/**
	 * Operator arity
	 *
	 * @param op operator
	 * @return the integer arity of the given logical operator
	 */
	public static int operatorArity(String op)
	{
		String[] kifOps = { UQUANT, EQUANT, NOT, AND, OR, IF, IFF };

		int translateIndex = 0;
		while (translateIndex < kifOps.length && !op.equals(kifOps[translateIndex]))
		{
			translateIndex++;
		}
		if (translateIndex <= 2)
		{
			return 1;
		}
		else
		{
			if (translateIndex < kifOps.length)
			{
				return 2;
			}
			else
			{
				return -1;
			}
		}
	}

	@SuppressWarnings("BooleanMethodIsAlwaysInverted") public boolean hasCorrectArity(KB kb)
	{
		return hasCorrectArity(this.text, kb);
	}

	public boolean hasCorrectArity(String formula, KB kb)
	{
		boolean arityCorrect = true;

		formula = formula.replaceAll("exists\\s+(\\([^(]+?\\))", "");
		formula = formula.replaceAll("forall\\s+(\\([^(]+?\\))", "");
		formula = formula.replaceAll("\".*?\"", "?MATCH");
		Pattern p = Pattern.compile("(\\([^(]+?\\))");

		Matcher m = p.matcher(formula);
		while (m.find() && arityCorrect)
		{
			String f = m.group(1);
			if (f.length() > 2)
				f = f.substring(1, f.length() - 1);
			String[] split = f.split(" ");
			if (split.length > 1)
			{
				String rel = split[0];
				if (!rel.startsWith("?"))
				{
					int arity;
					if (rel.equals("=>") || rel.equals("<=>"))
						arity = 2;
					else
						arity = kb.getValence(rel);

					boolean startsWith = false;
					// disregard statements using the @ROW variable as it
					// will more often than not resolve to a wrong arity
					for (int i = 1; i < split.length; i++)
						if (split[i].startsWith("@"))
						{
							startsWith = true;
							break;
						}
					if (!startsWith)
						if (arity >= 1 && split.length - 1 != arity)
							arityCorrect = false;
				}
			}
			formula = formula.replace("(" + f + ")", "?MATCH");
			m = p.matcher(formula);
		}
		return arityCorrect;
	}

	// I N S T A N T I A T E

	private static class Reject extends Exception
	{
		private static final long serialVersionUID = 5770027459770147573L;
	}

	/**
	 * Returns an List of the Formulae that result from replacing
	 * all arg0 predicate variables in the input Formula with
	 * predicate names.
	 *
	 * @param kb A KB that is used for processing the Formula.
	 * @return An List of Formulas, or an empty List if no instantiations can be generated.
	 * @throws Reject reject exception
	 */
	public List<Formula> instantiatePredVars(KB kb) throws Reject
	{
		List<Formula> result = new ArrayList<>();
		try
		{
			if (this.listP())
			{
				String arg0 = this.getArgument(0);
				// First we do some checks to see if it is worth processing the formula.
				if (isLogicalOperator(arg0) && this.text.matches(".*\\(\\s*\\?\\w+.*"))
				{
					// Get all pred vars, and then compute query lits for the pred vars, indexed by var.
					Map<String, List<String>> varsWithTypes = gatherPredVars(kb);
					if (!varsWithTypes.containsKey("arg0"))
					{
						// The formula has no predicate variables in arg0 position, so just return it.
						result.add(this);
					}
					else
					{
						List<Tuple.Pair<String, List<List<String>>>> indexedQueryLits = prepareIndexedQueryLiterals(kb, varsWithTypes);
						List<Tuple.Triple<List<List<String>>, List<String>, List<List<String>>>> substForms = new ArrayList<>();

						// First, gather all substitutions.
						for (Tuple.Pair<String, List<List<String>>> varQueryTuples : indexedQueryLits)
						{
							Tuple.Triple<List<List<String>>, List<String>, List<List<String>>> substTuples = computeSubstitutionTuples(kb, varQueryTuples);
							if (substTuples != null)
							{
								if (substForms.isEmpty())
									substForms.add(substTuples);
								else
								{
									int stSize = substTuples.third.size();

									int sfSize = substForms.size();
									int sfLast = (sfSize - 1);
									for (int i = 0; i < sfSize; i++)
									{
										int iSize = substForms.get(i).third.size();
										if (stSize < iSize)
										{
											substForms.add(i, substTuples);
											break;
										}
										if (i == sfLast)
											substForms.add(substTuples);
									}
								}
							}
						}

						if (!substForms.isEmpty())
						{
							// Try to simplify the Formula.
							Formula f = this;
							for (Tuple.Triple<List<List<String>>, List<String>, List<List<String>>> substTuples : substForms)
							{
								List<List<String>> litsToRemove = substTuples.first;
								for (List<String> lit : litsToRemove)
								{
									f = f.maybeRemoveMatchingLits(lit);
								}
							}

							// Now generate pred var instantiations from the possibly simplified formula.
							List<String> templates = new ArrayList<>();
							templates.add(f.text);

							// Iterate over all var plus query lits forms, getting a list of substitution literals.
							Set<String> accumulator = new HashSet<>();
							for (Tuple.Triple<List<List<String>>, List<String>, List<List<String>>> substTuples : substForms)
							{
								if ((substTuples != null))
								{
									// Iterate over all ground lits ...
									// Do not use litsToRemove, which we have already used above.
									// List<List<String>> litsToRemove = substTuples.first;

									// Remove and hold the tuple that indicates the variable substitution pattern.
									List<String> varTuple = substTuples.second;

									for (List<String> groundLit : substTuples.third)
									{
										// Iterate over all formula templates, substituting terms from each ground lit for vars in the template.
										Formula templateF = new Formula();
										for (String template : templates)
										{
											templateF.set(template);
											List<String> quantVars = templateF.collectVariables().first;
											for (int i = 0; i < varTuple.size(); i++)
											{
												String var = varTuple.get(i);
												if (isVariable(var))
												{
													String term = groundLit.get(i);
													// Don't replace variables that are explicitly quantified.
													if (!quantVars.contains(var))
													{
														List<Pattern> patterns = new ArrayList<>();
														List<String> patternStrings = Arrays.asList("(\\W*\\()(\\s*holds\\s+\\" + var + ")(\\W+)",
																// "(\\W*\\()(\\s*\\" + var + ")(\\W+)",
																"(\\W*)(\\" + var + ")(\\W+)");
														for (String patternString : patternStrings)
															patterns.add(Pattern.compile(patternString));
														for (Pattern pattern : patterns)
														{
															Matcher m = pattern.matcher(template);
															template = m.replaceAll("$1" + term + "$3");
														}
													}
												}
											}
											if (hasCorrectArity(template, kb))
												accumulator.add(template);
											else
											{
												logger.info("FORMULA REJECTED because of incorrect arity: " + template);
												break;
											}
										}
									}
									templates.clear();
									templates.addAll(accumulator);
									accumulator.clear();
								}
							}
							result.addAll(KB.stringsToFormulas(templates));
						}
						if (result.isEmpty())
							throw new Reject();
					}
				}
			}
		}
		catch (Reject r)
		{
			logger.info("FORMULA REJECTED");
			throw r;
		}
		return result;
	}

	/**
	 * Returns the number of SUO-KIF variables (only ? variables, not
	 *
	 * @param queryLiteral A List representing a Formula.
	 * @return An int.
	 * @ROW variables) in the input query literal.
	 */
	private static int getVarCount(List<String> queryLiteral)
	{
		int result = 0;
		if (queryLiteral != null)
		{
			for (String term : queryLiteral)
			{
				if (term.startsWith("?"))
					result++;
			}
		}
		return result;
	}

	/**
	 * This method returns an triplet of query answer literals.
	 * The first element is an List of query literals that might be
	 * used to simplify the Formula to be instantiated.
	 * The second element is the query literal (List) that will be used as a
	 * template for doing the variable substitutions.
	 * All subsequent elements are ground literals (Lists).
	 *
	 * @param kb        A KB to query for answers.
	 * @param queryLits A List of query literals.  The first item in
	 *                  the list will be a SUO-KIF variable (String), which indexes the
	 *                  list.  Each subsequent item is a query literal (List).
	 * @return An List of literals, or null if no query answers can be found.
	 */
	private static Tuple.Triple<List<List<String>>, List<String>, List<List<String>>> computeSubstitutionTuples(KB kb,
			Tuple.Pair<String, List<List<String>>> queryLits)
	{
		if (kb != null && queryLits != null)
		{
			// Variable
			String idxVar = queryLits.first;

			// Sort the query lits by number of variables.
			List<List<String>> sortedQLits = new ArrayList<>(queryLits.second);
			sortedQLits.remove(0);
			if (sortedQLits.size() > 1)
			{
				Comparator<List<String>> comp = (o1, o2) -> {
					Integer c1 = getVarCount(o1);
					Integer c2 = getVarCount(o2);
					return c1.compareTo(c2);
				};
				sortedQLits.sort(Collections.reverseOrder(comp));
			}

			// Put instance literals last.
			List<List<String>> ioLits = new ArrayList<>();
			List<List<String>> qLits = new ArrayList<>(sortedQLits);
			sortedQLits.clear();

			for (List<String> ql : qLits)
			{
				if (ql.get(0).equals("instance"))
					ioLits.add(ql);
				else
					sortedQLits.add(ql);
			}
			sortedQLits.addAll(ioLits);

			// Literals that will be used to try to simplify the formula before pred var instantiation.
			List<List<String>> simplificationLits = new ArrayList<>();

			// The literal that will serve as the pattern for extracting var replacement terms from answer/ literals.
			List<String> keyLit = null;

			// The list of answer literals retrieved using the query lits, possibly built up via a sequence of multiple queries.
			List<List<String>> answers = null;

			Set<String> working = new HashSet<>();

			boolean satisfiable = true;
			boolean tryNextQueryLiteral = true;

			// The first query lit for which we get an answer is the key lit.
			for (int i = 0; i < sortedQLits.size() && tryNextQueryLiteral; i++)
			{
				List<String> ql = sortedQLits.get(i);
				List<Formula> accumulator = kb.askWithLiteral(ql);
				satisfiable = ((accumulator != null) && !accumulator.isEmpty());
				tryNextQueryLiteral = (satisfiable || (getVarCount(ql) > 1));
				// !((String)(ql.get(0))).equals("instance")
				if (satisfiable)
				{
					simplificationLits.add(ql);
					if (keyLit == null)
					{
						keyLit = ql;
						answers = KB.formulasToLists(accumulator);
					}
					else
					{  // if (accumulator.size() < answers.size()) {
						List<List<String>> accumulator2 = KB.formulasToLists(accumulator);

						// Winnow the answers list.
						working.clear();
						int varPos = ql.indexOf(idxVar);
						for (List<String> ql2 : accumulator2)
						{
							String term = ql2.get(varPos);
							// if (!term.endsWith("Fn")) {
							working.add(term);
							// }
						}

						accumulator2.clear();
						accumulator2.addAll(answers);
						answers.clear();
						varPos = keyLit.indexOf(idxVar);
						for (List<String> ql2 : accumulator2)
						{
							String term = ql2.get(varPos);
							if (working.contains(term))
							{
								answers.add(ql2);
							}
						}
					}
				}
			}
			if (satisfiable && (keyLit != null))
			{
				Tuple.Triple<List<List<String>>, List<String>, List<List<String>>> result = new Tuple.Triple<>();
				result.first = simplificationLits;
				result.second = keyLit;
				result.third = answers;
				return result;
			}
		}
		return null;
	}

    /*
      This method returns an List in which each element is
      another List.  The head of each element is a variable.
      The subsequent objects in each element are query literals
      (Lists).

      @param kb The KB to use for computing variable type signatures.
     *
     * @return An List, or null if the input formula contains no
     * predicate variables.
     */
	//private List prepareIndexedQueryLiterals(KB kb) {
	//    return prepareIndexedQueryLiterals(kb, null);
	//}

	/**
	 * This method returns an List in which each element is
	 * a pair.  The first item of each pair is a variable.
	 * The second item in each pair is a list of query literals
	 * (Lists).
	 *
	 * @param kb         The KB to use for computing variable type signatures.
	 * @param varTypeMap A Map from variables to their types, as
	 *                   explained in the javadoc entry for gatherPredVars(kb)
	 * @return An List, or null if the input formula contains no
	 * predicate variables.
	 */
	private List<Tuple.Pair<String, List<List<String>>>> prepareIndexedQueryLiterals(KB kb, Map<String, List<String>> varTypeMap)
	{
		if (logger.isLoggable(Level.FINER))
		{
			String[] params = { "kb = " + kb.name, "varTypeMap = " + varTypeMap };
			logger.entering("Formula", "prepareIndexedQueryLiterals", params);
		}
		List<Tuple.Pair<String, List<List<String>>>> result = new ArrayList<>();
		Map<String, List<String>> varsWithTypes = varTypeMap != null ? varTypeMap : this.gatherPredVars(kb);
		// logger.finest("varsWithTypes = " + varsWithTypes);

		if (!varsWithTypes.isEmpty())
		{
			List<String> yOrN = varsWithTypes.get("arg0");
			if (yOrN.size() == 1 && "yes".equalsIgnoreCase(yOrN.get(0)))
			{
				// Try to simplify the formula.
				for (String var : varsWithTypes.keySet())
				{
					if (isVariable(var))
					{
						List<String> varWithTypes = varsWithTypes.get(var);
						Tuple.Pair<String, List<List<String>>> indexedQueryLits = gatherPredVarQueryLits(kb, varWithTypes);
						if (indexedQueryLits != null)
						{
							result.add(indexedQueryLits);
						}
					}
				}
			}
			// Else if the formula doesn't contain any arg0 pred vars, do nothing.
		}
		logger.exiting("Formula", "prepareIndexedQueryLiterals", result);
		return result;
	}

	/**
	 * This method collects and returns all predicate variables that
	 * occur in the Formula.
	 *
	 * @param kb The KB to be used for computations involving
	 *           assertions.
	 * @return a Map in which the keys are predicate variables,
	 * and the values are Lists containing one or more class
	 * names that indicate the type constraints tha apply to the
	 * variable.  If no predicate variables can be gathered from the
	 * Formula, the Map will be empty.  The first element in each
	 * List is the variable itself.  Subsequent elements are the
	 * types of the variable.  If no types for the variable can be
	 * determined, the List will contain just the variable.
	 */
	protected Map<String, List<String>> gatherPredVars(KB kb)
	{
		logger.entering("Formula", "gatherPredVars", kb.name);
		Map<String, List<String>> result = new HashMap<>();
		if (isNonEmpty(this.text))
		{
			List<Formula> working = new ArrayList<>();
			List<Formula> accumulator = new ArrayList<>();
			if (this.listP() && !this.empty())
				accumulator.add(this);
			while (!accumulator.isEmpty())
			{
				working.clear();
				working.addAll(accumulator);
				accumulator.clear();

				for (Formula f : working)
				{
					int len = f.listLength();
					String arg0 = f.getArgument(0);
					if (isQuantifier(arg0) || arg0.equals("holdsDuring") || arg0.equals("KappaFn"))
					{
						if (len > 2)
						{
							String arg2 = f.getArgument(2);
							Formula newF = new Formula();
							newF.set(arg2);
							if (f.listP() && !f.empty())
								accumulator.add(newF);
						}
						else
						{
							logger.warning("Is this malformed? " + f.text);
						}
					}
					else if (arg0.equals("holds"))
						accumulator.add(f.cdrAsFormula());
					else if (isVariable(arg0))
					{
						List<String> vals = result.get(arg0);
						if (vals == null)
						{
							vals = new ArrayList<>();
							result.put(arg0, vals);
							vals.add(arg0);
						}
						// Record the fact that we found at least one variable in the arg0 position.
						result.put("arg0", Collections.singletonList("yes"));
					}
					else
					{
						boolean[] signature = kb.getRelnArgSignature(arg0);
						for (int j = 1; j < len; j++)
						{
							String argN = f.getArgument(j);
							if ((signature != null) && (signature.length > j) && signature[j] && isVariable(argN))
							{
								List<String> vals = result.get(argN);
								if (vals == null)
								{
									vals = new ArrayList<>();
									result.put(argN, vals);
									vals.add(argN);
								}
								String argType = kb.getArgType(arg0, j);
								if (!((argType == null) || vals.contains(argType)))
									vals.add(argType);
							}
							else
							{
								Formula argF = new Formula();
								argF.set(argN);
								if (argF.listP() && !argF.empty())
									accumulator.add(argF);
							}
						}
					}
				}
			}
		}
		logger.exiting("Formula", "gatherPredVars", result);
		return result;
	}

	/**
	 * This method tries to remove literals from the Formula that
	 * match litArr.  It is intended for use in simplification of this
	 * Formula during predicate variable instantiation, and so only
	 * attempts removals that are likely to be safe in that context.
	 *
	 * @param litArr A List object representing a SUO-KIF atomic
	 *               formula.
	 * @return A new Formula with at least some occurrences of litF
	 * removed, or the original Formula if no removals are possible.
	 */
	private Formula maybeRemoveMatchingLits(List<String> litArr)
	{
		Formula f = KB.literalListToFormula(litArr);
		return maybeRemoveMatchingLits(f);
	}

	/**
	 * This method tries to remove literals from the Formula that
	 * match litF.  It is intended for use in simplification of this
	 * Formula during predicate variable instantiation, and so only
	 * attempts removals that are likely to be safe in that context.
	 *
	 * @param litF A SUO-KIF literal (atomic Formula).
	 * @return A new Formula with at least some occurrences of litF
	 * removed, or the original Formula if no removals are possible.
	 */
	private Formula maybeRemoveMatchingLits(Formula litF)
	{
		logger.entering("Formula", "maybeRemoveMatchingLits", litF);
		Formula result = null;
		Formula f = this;
		if (f.listP() && !f.empty())
		{
			StringBuilder litBuf = new StringBuilder();
			String arg0 = f.car();
			if (Arrays.asList(IF, IFF).contains(arg0))
			{
				String arg1 = f.getArgument(1);
				String arg2 = f.getArgument(2);
				if (arg1.equals(litF.text))
				{
					Formula arg2F = new Formula();
					arg2F.set(arg2);
					litBuf.append(arg2F.maybeRemoveMatchingLits(litF).text);
				}
				else if (arg2.equals(litF.text))
				{
					Formula arg1F = new Formula();
					arg1F.set(arg1);
					litBuf.append(arg1F.maybeRemoveMatchingLits(litF).text);
				}
				else
				{
					Formula arg1F = new Formula();
					arg1F.set(arg1);
					Formula arg2F = new Formula();
					arg2F.set(arg2);
					litBuf.append("(") //
							.append(arg0) //
							.append(" ") //
							.append(arg1F.maybeRemoveMatchingLits(litF).text) //
							.append(" ") //
							.append(arg2F.maybeRemoveMatchingLits(litF).text) //
							.append(")");
				}
			}
			else if (isQuantifier(arg0) || arg0.equals("holdsDuring") || arg0.equals("KappaFn"))
			{
				Formula arg2F = new Formula();
				arg2F.set(f.caddr());
				litBuf.append("(").append(arg0).append(" ").append(f.cadr()).append(" ").append(arg2F.maybeRemoveMatchingLits(litF).text).append(")");
			}
			else if (isCommutative(arg0))
			{
				List<String> litArr = f.literalToList();
				litArr.remove(litF.text);
				StringBuilder args = new StringBuilder();
				int len = litArr.size();
				for (int i = 1; i < len; i++)
				{
					Formula argF = new Formula();
					argF.set(litArr.get(i));
					args.append(" ").append(argF.maybeRemoveMatchingLits(litF).text);
				}
				if (len > 2)
					args = new StringBuilder(("(" + arg0 + args + ")"));
				else
					args = new StringBuilder(args.toString().trim());
				litBuf.append(args);
			}
			else
			{
				litBuf.append(f.text);
			}
			Formula newF = new Formula();
			newF.set(litBuf.toString());
			result = newF;
		}
		if (result == null)
			result = this;
		logger.exiting("Formula", "maybeRemoveMatchingLits", result);
		return result;
	}

	/**
	 * Return true if the input predicate can take relation names a
	 * arguments, else returns false.
	 */
	private boolean isPossibleRelnArgQueryPred(KB kb, String predicate)
	{
		return isNonEmpty(predicate) && ((kb.getRelnArgSignature(predicate) != null) || predicate.equals("instance"));
	}

	/**
	 * This method collects and returns literals likely to be of use
	 * as templates for retrieving predicates to be substituted for
	 * var.
	 *
	 * @param varWithTypes A List containing a variable followed,
	 *                     optionally, by class names indicating the type of the variable.
	 * @return An List of literals (Lists) with var at the head.
	 * The first element of the List is the variable (String).
	 * Subsequent elements are Lists corresponding to SUO-KIF
	 * formulas, which will be used as query templates.
	 */
	private Tuple.Pair<String, List<List<String>>> gatherPredVarQueryLits(KB kb, List<String> varWithTypes)
	{
		Tuple.Pair<String, List<List<String>>> result = new Tuple.Pair<>();
		String var = varWithTypes.get(0);
		Set<String> added = new HashSet<>();
		Map<String, String> varMap = getVarMap();

		// Get the clauses for this Formula.
		List<Clause> clauses = getClauses();
		if (clauses != null)
		{
			for (Clause clause : clauses)
			{
				List<Formula> negLits = clause.negativeLits;
				// List<Formula> posLits = clause.positiveLits;
				if (!negLits.isEmpty())
				{
					int cim = 1;
					for (int ci = 0; ci < cim; ci++)
					{
						// Try the negLits first.  Then try the posLits only if there still are no results.
						@SuppressWarnings("ConstantConditions") List<Formula> lit = ci == 0 ? clause.negativeLits : clause.positiveLits;
						for (Formula f : lit)
						{
							if (f.text.matches(".*SkFn\\s+\\d+.*") || f.text.matches(".*Sk\\d+.*"))
								continue;
							int fLen = f.listLength();
							String arg0 = f.getArgument(0);
							if (isNonEmpty(arg0))
							{
								// If arg0 corresponds to var, then var has to be of type Predicate, not of types Function or List.
								if (isVariable(arg0))
								{
									String origVar = Clausifier.getOriginalVar(arg0, varMap);
									if (origVar.equals(var) && !varWithTypes.contains("Predicate"))
									{
										varWithTypes.add("Predicate");
									}
								}
								else
								{
									List<String> queryLit = new ArrayList<>();
									queryLit.add(arg0);
									boolean foundVar = false;
									for (int i = 1; i < fLen; i++)
									{
										String arg = f.getArgument(i);
										if (!listP(arg))
										{
											if (isVariable(arg))
											{
												arg = Clausifier.getOriginalVar(arg, varMap);
												if (arg.equals(var))
													foundVar = true;
											}
											queryLit.add(arg);
										}
									}
									if (queryLit.size() != fLen)
										continue;
									// If the literal does not start with a variable or with "holds" and does not
									// contain Skolem terms, but does contain the variable in which we're interested,
									// it is probably suitable as a query template, or might serve as a starting
									// place.  Use it, or a literal obtained with it.
									if (isPossibleRelnArgQueryPred(kb, arg0) && foundVar)
									{
										// || arg0.equals("disjoint"))
										String term = "";
										if (queryLit.size() > 2)
											term = queryLit.get(2);
										if (!(arg0.equals("instance") && term.equals("Relation")))
										{
											String queryLitStr = queryLit.toString().intern();
											if (!added.contains(queryLitStr))
											{
												result.second.add(queryLit);
												added.add(queryLitStr);
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
		// If we have previously collected type info for the variable, convert that info query lits now.
		int vtLen = varWithTypes.size();
		if (vtLen > 1)
		{
			for (int j = 1; j < vtLen; j++)
			{
				String argType = varWithTypes.get(j);
				if (!argType.equals("Relation"))
				{
					List<String> queryLit = new ArrayList<>();
					queryLit.add("instance");
					queryLit.add(var);
					queryLit.add(argType);
					String qlString = queryLit.toString().intern();
					if (!added.contains(qlString))
					{
						result.second.add(queryLit);
						added.add(qlString);
					}
				}
			}
		}
		// Add the variable to the pair
		result.first = var;
		// return null if it does not contain any query literals.
		if (!result.second.isEmpty())
			return null;
		return result;
	}

	/**
	 * Replace variables with a value as given by the map argument
	 *
	 * @param m variable-value map
	 * @return formula with variables replaced by values
	 */
	public Formula substituteVariables(Map<String, String> m)
	{
		logger.entering("Formula", "substituteVariables", m);
		Formula newFormula = new Formula();
		newFormula.set("()");
		if (atom())
		{
			if (m.containsKey(text))
			{
				text = m.get(text);
				if (this.listP())
					text = "(" + text + ")";
			}
			return this;
		}
		if (!empty())
		{
			Formula f1 = new Formula();
			f1.set(this.car());
			if (f1.listP())
			{
				newFormula = newFormula.cons(f1.substituteVariables(m));
			}
			else
				newFormula = newFormula.append(f1.substituteVariables(m));
			Formula f2 = new Formula();
			f2.set(this.cdr());
			newFormula = newFormula.append(f2.substituteVariables(m));
		}
		logger.exiting("Formula", "substituteVariables", newFormula);
		return newFormula;
	}

	// R E P R E S E N T A T I O N

	/**
	 * Format a formula for either text or HTML presentation by inserting
	 * the proper hyperlink code, characters for indentation and end of line.
	 * A standard LISP-style pretty printing is employed where an open
	 * parenthesis triggers a new line and added indentation.
	 *
	 * @param hyperlink   - the URL to be referenced to a hyperlinked term.
	 * @param indentChars - the proper characters for indenting text.
	 * @param eolChars    - the proper character for end of line.
	 * @return a formula formatted for either text or HTML presentation.
	 */
	public String format(String hyperlink, String indentChars, String eolChars)
	{
		if (this.text == null)
			return "";
		String result;
		if (isNonEmpty(this.text))
			this.text = this.text.trim();
		String legalTermChars = "-:";
		String varStartChars = "?@";
		StringBuilder token = new StringBuilder();
		StringBuilder formatted = new StringBuilder();
		int indentLevel = 0;
		boolean inQuantifier = false;
		boolean inToken = false;
		boolean inVariable = false;
		boolean inVarList = false;
		boolean inComment = false;

		int fLen = this.text.length();
		char pch = '0';  // char at (i-1)
		for (int i = 0; i < fLen; i++)
		{
			// logger.finest("formatted string = " + formatted.toString());
			char ch = this.text.charAt(i);
			if (inComment)
			{
				// In a comment
				formatted.append(ch);
				if ((i > 70) && (ch == '/')) // add spaces to long URL strings
					formatted.append(" ");
				if (ch == '"')
					inComment = false;
			}
			else
			{
				if ((ch == '(') && !inQuantifier && ((indentLevel != 0) || (i > 1)))
				{
					if ((i > 0) && Character.isWhitespace(pch))
					{
						formatted.deleteCharAt(formatted.length() - 1);
					}
					formatted.append(eolChars);
					for (int j = 0; j < indentLevel; j++)
						formatted.append(indentChars);
				}
				if ((i == 0) && (indentLevel == 0) && (ch == '('))
					formatted.append(ch);
				if (!inToken && !inVariable && Character.isJavaIdentifierStart(ch))
				{
					token = new StringBuilder();
					token.append(ch);
					inToken = true;
				}
				if (inToken && (Character.isJavaIdentifierPart(ch) || (legalTermChars.indexOf(ch) > -1)))
					token.append(ch);
				if (ch == '(')
				{
					if (inQuantifier)
					{
						inQuantifier = false;
						inVarList = true;
						token = new StringBuilder();
					}
					else
						indentLevel++;
				}
				if (ch == '"')
					inComment = true;
				if (ch == ')')
				{
					if (!inVarList)
						indentLevel--;
					else
						inVarList = false;
				}
				if ((token.indexOf("forall") > -1) || (token.indexOf("exists") > -1))
					inQuantifier = true;
				if (inVariable && !Character.isJavaIdentifierPart(ch) && (legalTermChars.indexOf(ch) == -1))
					inVariable = false;
				if (varStartChars.indexOf(ch) > -1)
					inVariable = true;
				if (inToken && !Character.isJavaIdentifierPart(ch) && (legalTermChars.indexOf(ch) == -1))
				{
					inToken = false;
					if (isNonEmpty(hyperlink))
					{
						formatted.append("<a href=\"");
						formatted.append(hyperlink);
						formatted.append("&term=");
						formatted.append(token);
						formatted.append("\">");
						formatted.append(token);
						formatted.append("</a>");
					}
					else
						formatted.append(token);
					token = new StringBuilder();
				}
				if ((i > 0) && !inToken && !(Character.isWhitespace(ch) && (pch == '(')))
				{
					if (Character.isWhitespace(ch))
					{
						if (!Character.isWhitespace(pch))
							formatted.append(" ");
					}
					else
						formatted.append(ch);
				}
			}
			pch = ch;
		}
		if (inToken)
		{
			// A term which is outside of parenthesis, typically, a binding.
			if (isNonEmpty(hyperlink))
			{
				formatted.append("<a href=\"");
				formatted.append(hyperlink);
				formatted.append("&term=");
				formatted.append(token);
				formatted.append("\">");
				formatted.append(token);
				formatted.append("</a>");
			}
			else
				formatted.append(token);
		}
		result = formatted.toString();
		return result;
	}

	/**
	 * Format a formula for text presentation.
	 */
	public String toString()
	{
		return format("", "  ", Character.toString((char) 10));
	}

	/**
	 * Format a formula as a prolog statement.  Note that only tuples
	 * are converted properly at this time.  Statements with any embedded
	 * formulas or functions will be rejected with a null return.
	 *
	 * @return a prolog statement for the formula
	 */
	public String toProlog()
	{
		if (!listP())
		{
			logger.warning("Not a formula: " + text);
			return "";
		}
		if (empty())
		{
			logger.warning("Empty formula: " + text);
			return "";
		}
		StringBuilder result = new StringBuilder();
		String relation = car();
		Formula f = new Formula();
		f.text = cdr();
		if (!Formula.atom(relation))
		{
			logger.warning("Relation not an atom: " + relation);
			return "";
		}
		result.append(relation).append("('");
		while (!f.empty())
		{
			String arg = f.car();
			f.text = f.cdr();
			if (!Formula.atom(arg))
			{
				logger.warning("Argument not an atom: " + arg);
				return "";
			}
			result.append(arg).append("'");
			if (!f.empty())
				result.append(",'");
			else
				result.append(").");
		}
		return result.toString();
	}

	private static boolean isEmpty(String str)
	{
		return str == null || str.isEmpty();
	}

	private static boolean isNonEmpty(String str)
	{
		return str != null && !str.isEmpty();
	}
}
