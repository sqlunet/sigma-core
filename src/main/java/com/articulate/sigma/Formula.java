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
	private static final long serialVersionUID = 1001L;

	private static final String LOG_SOURCE = "Formula";

	private static final Logger logger = Logger.getLogger(Formula.class.getName());

	// special

	protected static final Formula EMPTY_LIST = Formula.of("()");

	// logical

	protected static final String AND = "and";
	protected static final String OR = "or";
	protected static final String NOT = "not";
	protected static final String IF = "=>";
	protected static final String IFF = "<=>";
	protected static final String UQUANT = "forall";
	protected static final String EQUANT = "exists";

	/**
	 * The SUO-KIF logical operators.
	 */
	public static final List<String> LOGICAL_OPERATORS = Arrays.asList(UQUANT, EQUANT, AND, OR, NOT, IF, IFF);

	public static final List<String> IF_OPERATORS = Arrays.asList(IF, IFF);

	// comparison

	protected static final String EQUAL = "equal";
	protected static final String GT = "greaterThan";
	protected static final String GTE = "greaterThanOrEqualTo";
	protected static final String LT = "lessThan";
	protected static final String LTE = "lessThanOrEqualTo";

	/**
	 * SUO-KIF mathematical comparison predicates.
	 */
	protected static final List<String> COMPARISON_OPERATORS = Arrays.asList(EQUAL, GT, GTE, LT, LTE);

	// arithmetic

	protected static final String PLUSFN = "AdditionFn";
	protected static final String MINUSFN = "SubtractionFn";
	protected static final String TIMESFN = "MultiplicationFn";
	protected static final String DIVIDEFN = "DivisionFn";

	/**
	 * The SUO-KIF mathematical functions are implemented in Vampire.
	 */
	protected static final List<String> MATH_FUNCTIONS = Arrays.asList(PLUSFN, MINUSFN, TIMESFN, DIVIDEFN);

	// functions
	protected static final String SK_PREF = "Sk";
	protected static final String FN_SUFF = "Fn";

	protected static final String SKFN = "SkFn";

	// variables

	protected static final String V_PREF = "?";
	protected static final String VX = V_PREF + "X";
	protected static final String VVAR = V_PREF + "VAR";

	protected static final String R_PREF = "@";
	protected static final String RVAR = R_PREF + "ROW";

	// list

	protected static final String LP = "(";
	protected static final String RP = ")";

	// other

	protected static final String SPACE = " ";

	protected static final List<Character> QUOTE_CHARS = Arrays.asList('"', '\'');

	protected static final String LOGICAL_FALSE = "False";


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
	 * The formula text form.
	 */
	@NotNull
	public final String form;

	/**
	 * A list of clausal (resolution) forms generated from this Formula, a Map of variable renaming and the original Formula
	 */
	@Nullable
	private Tuple.Triple<List<Clause>, Map<String, String>, Formula> clausalForms = null;

	/**
	 * The source file in which the formula appears.
	 */
	@NotNull
	public String sourceFile = "";

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

	@NotNull
	public static Formula of(@NotNull final String form)
	{
		return new Formula(form);
	}

	@NotNull
	public static Formula copy(@NotNull final Formula that)
	{
		return new Formula(that);
	}

	/**
	 * Constructor
	 *
	 * @param form formula string
	 */
	protected Formula(@NotNull final String form)
	{
		if (form.isEmpty())
		{
			throw new IllegalArgumentException(form);
		}
		this.form = form.intern();
	}

	/**
	 * Copy constructor. This is in effect a deep copy.
	 *
	 * @param that other formula
	 */
	protected Formula(@NotNull final Formula that)
	{
		this(that.form);
		this.sourceFile = that.sourceFile;
		this.startLine = that.startLine;
		this.endLine = that.endLine;
		this.clausalForms = that.clausalForms;
	}

	// A C C E S S

	/**
	 * Get source file
	 *
	 * @return source file
	 */
	@NotNull
	public String getSourceFile()
	{
		return sourceFile;
	}

	/**
	 * Set source filename
	 *
	 * @param filename source filename
	 */
	public void setSourceFile(@NotNull String filename)
	{
		sourceFile = filename;
	}

	/**
	 * Get error log
	 *
	 * @return errors
	 */
	@NotNull
	public List<String> getErrors()
	{
		return errors;
	}

	// C L A U S A L   F O R M S

	/**
	 * Returns a List of the clauses that together constitute the
	 * resolution form of this Formula.  The list could be empty if
	 * the clausal form has not yet been computed.
	 *
	 * @return Tuple
	 */
	@Nullable
	public Tuple.Triple<List<Clause>, Map<String, String>, Formula> getClausalForms()
	{
		if (clausalForms == null)
		{
			logger.entering(LOG_SOURCE, "getClausalForm");
			clausalForms = Clausifier.clausify(this);
			logger.exiting(LOG_SOURCE, "getClausalForm", clausalForms);
		}
		return clausalForms;
	}

	/**
	 * Returns a List of Clause objects.  Each such Clause contains, in
	 * turn, a pair of List objects.  Each List object in a pair
	 * contains Formula objects.  The Formula objects contained in the
	 * first List object (first) of a pair represent negative literals
	 * (antecedent conjuncts).  The Formula objects contained in the
	 * second List object (second) of a pair represent positive literals
	 * (consequent conjuncts).  Taken together, all the clauses
	 * constitute the resolution form of this Formula.
	 *
	 * @return A List of Clauses.
	 */
	@Nullable
	public List<Clause> getClauses()
	{
		@Nullable Tuple.Triple<List<Clause>, Map<String, String>, Formula> clausalForms = getClausalForms();
		if (clausalForms == null)
		{
			return null;
		}
		return clausalForms.first;
	}

	/**
	 * Returns a map of the variable renames that occurred during the
	 * translation of this Formula into the clausal (resolution) form
	 * accessible via getClauses().
	 *
	 * @return A Map of String (SUO-KIF variable) key-value pairs.
	 */
	@Nullable
	public Map<String, String> getVarMap()
	{
		@Nullable Tuple.Triple<List<Clause>, Map<String, String>, Formula> clausalForms = getClausalForms();
		if (clausalForms != null)
		{
			return clausalForms.second;
		}
		return null;
	}

	// N O R M A L I Z E D

	/**
	 * Normalized unformatted form
	 *
	 * @param form form
	 * @return Normalized unformatted form
	 */
	@NotNull
	private static String normalized(@NotNull final String form)
	{
		return Variables.normalizeVariables(form).trim();
	}

	/**
	 * Normalized formatted form
	 *
	 * @param form form
	 * @return Normalized formatted form
	 */
	@NotNull
	private static String normalizedFormatted(@NotNull final String form)
	{
		@NotNull String normalizedText = Variables.normalizeVariables(form);
		@NotNull Formula f = Formula.of(normalizedText);
		return f.toFlatString().trim();
	}

	// I D E N T I T Y

	/**
	 * Test if the contents of the formula are equal to the argument.
	 * Normalize all variables.
	 *
	 * @param that other formula to compare to.
	 * @return whether the contents of the formula are equal to the argument.
	 */
	public boolean equals(@NotNull final Formula that)
	{
		@NotNull String form = normalizedFormatted(this.form);
		@NotNull String form2 = normalizedFormatted(that.form);
		return form.equals(form2);
	}

	/**
	 * If equals is overridden, hashCode must use the same "significant" fields.
	 */
	public int hashCode()
	{
		return normalizedFormatted(form).hashCode();
	}

	/**
	 * Create ID
	 *
	 * @return a unique ID by appending the hashCode() of the formula String to the file name in which it appears
	 */
	@NotNull
	public String createID()
	{
		@NotNull String fileName = FileUtil.basename(sourceFile);
		int hc = normalizedFormatted(form).hashCode();
		if (hc < 0)
		{
			// replace minus sign with N
			return "N" + Integer.toString(hc).substring(1) + fileName;
		}
		return hc + fileName;
	}

	// O R D E R I N G

	/**
	 * Implement the Comparable interface by defining the compareTo
	 * method.  Formulas are equal if their formula strings are equal.
	 *
	 * @return compare code
	 */
	public int compareTo(@NotNull final Formula that)
	{
		return toFlatString().compareTo(that.toFlatString());
	}

	// L I S P - L I K E

	/**
	 * Car
	 *
	 * @return the LISP 'car' of the formula as a String - the first
	 * element of the list. Note that this operation has no side
	 * effect on the Formula.
	 * Currently (10/24/2007), this method returns the empty string
	 * ("") when invoked on an empty list.  Technically, this is
	 * wrong.  In most LISPS, the car of the empty list is the empty
	 * list (or nil).  But some parts of the Sigma code apparently
	 * expect this method to return the empty string when invoked on
	 * an empty list.
	 */
	@NotNull
	public String car()
	{
		return Lisp.car(form);
	}

	/**
	 * Cdr
	 *
	 * @return the LISP 'cdr' of the formula - the rest of a list minus its
	 * first element.
	 * Note that this operation has no side effect on the Formula.
	 */
	@NotNull
	public String cdr()
	{
		return Lisp.cdr(form);
	}

	/**
	 * Cdr, the LISP 'cdr' of the formula as a new Formula, if
	 * possible, else returns null.
	 *
	 * @return the cdr of the formula.
	 * Note that this operation has no side effect on the Formula.
	 */
	@Nullable
	public Formula cdrAsFormula()
	{
		@NotNull String cdr = cdr();
		if (Lisp.listP(cdr))
		{
			return Formula.of(cdr);
		}
		return null;
	}

	/**
	 * Cdr, the LISP 'cdr' of the formula as a new Formula.
	 * This assumes the formula is a list.
	 *
	 * @return the cdr of the formula.
	 * Note that this operation has no side effect on the Formula.
	 */
	@NotNull
	public Formula cdrOfListAsFormula()
	{
		@NotNull String cdr = cdr();
		assert Lisp.listP(cdr);
		return Formula.of(cdr);
	}

	/**
	 * Cons
	 *
	 * @param head The String object that will become the 'car' (or
	 *             head) of the resulting Formula (list).
	 * @return a new Formula, or the original Formula if the cons fails.
	 * A new Formula which is the result of 'consing' a String
	 * into this Formula, similar to the LISP procedure of the same
	 * name.  This procedure is a bit of a kludge, since this
	 * Formula is treated simply as a LISP object (presumably, a LISP
	 * list), and could be degenerate or malformed as a Formula.
	 * Note that this operation has no side effect on the original Formula.
	 */
	@NotNull
	public Formula cons(@NotNull final String head)
	{
		// logger.entering(LOG_SOURCE, "cons", head);
		@NotNull Formula result = Formula.of(Lisp.cons(head, form));
		// logger.exiting(LOG_SOURCE, "cons", result);
		return result;
	}

	/**
	 * Cons
	 *
	 * @param head formula
	 * @return the LISP 'cons' of the formula, a new Formula, or the original Formula if the cons fails.
	 */
	@NotNull
	public Formula cons(@NotNull final Formula head)
	{
		return cons(head.form);
	}

	/**
	 * Append
	 *
	 * @param tail formula
	 * @return the LISP 'append' of the formulas, a Formula
	 * Note that this operation has no side effect on the Formula.
	 */
	@NotNull
	public Formula append(@NotNull final Formula tail)
	{
		return Formula.of(Lisp.append(form, tail.form));
	}

	/**
	 * Cadr
	 *
	 * @return the LISP 'cadr' (the second list element) of the
	 * formula, a String, or the empty string if the is no cadr.
	 * Note that this operation has no side effect on the Formula.
	 */
	@NotNull
	public String cadr()
	{
		return getArgument(1);
	}

	/**
	 * Caddr
	 *
	 * @return the LISP 'caddr' of the formula, which is the third
	 * list element of the formula,a String, or the empty string if there is no caddr.
	 * Note that this operation has no side effect on the Formula.
	 */
	@NotNull
	public String caddr()
	{
		return getArgument(2);
	}

	/**
	 * Atom
	 *
	 * @return whether the Formula is a LISP atom.
	 */
	public boolean atom()
	{
		return Lisp.atom(form);
	}

	/**
	 * Empty
	 *
	 * @return whether the Formula is an empty list.
	 */
	public boolean empty()
	{
		return Lisp.empty(form);
	}

	/**
	 * ListP
	 *
	 * @return whether the Formula is a list.
	 */
	public boolean listP()
	{
		return Lisp.listP(form);
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
		return Lisp.listLength(form);
	}

	// B R E A K   D O W N

	/**
	 * Elements
	 *
	 * @return A List (ordered tuple) representation of the
	 * Formula, in which each top-level element of the Formula is
	 * either an atom (String) or another list.
	 */
	@NotNull
	public static List<String> elements(@NotNull final String form)
	{
		@NotNull List<String> tuple = new ArrayList<>();
		if (Lisp.listP(form))
		{
			for (@NotNull IterableFormula f = new IterableFormula(form); !f.empty(); f.pop())
			{
				tuple.add(f.car());
			}
		}
		return tuple;
	}

	/**
	 * Elements
	 *
	 * @return A List (ordered tuple) representation of the
	 * Formula, in which each top-level element of the Formula is
	 * either an atom (String) or another list.
	 */
	@NotNull
	public List<String> elements()
	{
		return elements(form);
	}

	// A R G U M E N T S

	/**
	 * Return the numbered argument of the given formula.  The first
	 * element of a formula (i.e. the predicate position) is number 0.
	 * Returns the empty string if there is no such argument position.
	 *
	 * @param argNum argument number
	 * @return numbered argument.
	 */
	@NotNull
	public String getArgument(int argNum)
	{
		return Lisp.getArgument(form, argNum);
	}

	/**
	 * Return false if formula is complex (i.e. an argument
	 * is a function or sentence).
	 */
	private boolean hasSimpleArguments()
	{
		@NotNull List<String> elements = elements();
		for (@NotNull String e : elements)
		{
			if (e.startsWith("("))
			{
				return false;
			}
		}
		return true;
	}

	/**
	 * Return all the arguments in a simple formula as a list, starting
	 * at the given argument.  If formula is complex (i.e. an argument
	 * is a function or sentence), then return null.  If the starting
	 * argument is greater than the number of arguments, also return
	 * null.
	 *
	 * @param start start argument.			logger.entering(LOG_SOURCE, "getClausalForm");
	 * @return all the arguments in a simple formula as a list.
	 */
	@Nullable
	public List<String> simpleArgumentsToList(int start)
	{
		if (!hasSimpleArguments())
		{
			return null;
		}

		@NotNull List<String> result = new ArrayList<>();
		int index = start;
		for (@NotNull String arg = getArgument(index); !arg.isEmpty(); arg = getArgument(index))
		{
			result.add(arg);
			index++;
		}
		if (index == start)
		{
			return null;
		}
		return result;
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
		return isBalancedList(form);
	}

	/**
	 * Returns true if form contains no unbalanced parentheses
	 * or unbalanced quote characters, otherwise returns false.
	 *
	 * @param form0 form
	 * @return boolean
	 */
	public static boolean isBalancedList(@NotNull final String form0)
	{
		boolean result = false;
		@NotNull String form2 = form0.trim();
		if (Lisp.listP(form2))
		{
			if (Lisp.empty(form2))
			{
				result = true;
			}
			else
			{
				int pLevel = 0;
				int qLevel = 0;
				char prev = '0';
				boolean insideQuote = false;
				char quoteCharInForce = '0';

				for (int i = 0, len = form2.length(); i < len; i++)
				{
					char ch = form2.charAt(i);
					if (!insideQuote)
					{
						if (ch == '(')
						{
							pLevel++;
						}
						else if (ch == ')')
						{
							pLevel--;
						}
						else if (QUOTE_CHARS.contains(ch) && prev != '\\')
						{
							insideQuote = true;
							quoteCharInForce = ch;
							qLevel++;
						}
					}
					else if (QUOTE_CHARS.contains(ch) && ch == quoteCharInForce && prev != '\\')
					{
						insideQuote = false;
						quoteCharInForce = '0';
						qLevel--;
					}
					prev = ch;
				}
				result = pLevel == 0 && qLevel == 0;
			}
		}
		return result;
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
	 * @return null if there are no problems or an error message
	 * if there are.
	 */
	@Nullable
	public String hasValidArgs(@Nullable final String filename, @Nullable final Integer lineNo)
	{
		if (form.isEmpty())
		{
			return null;
		}
		@NotNull Formula f = Formula.of(form);
		return hasValidArgs(f, filename, lineNo);
	}

	/**
	 * Test whether the Formula uses logical operators and predicates
	 * with the correct number of arguments.  "equals", "&lt;=&gt;", and
	 * "=&gt;" are strictly binary.  "or", and "and" are binary or
	 * greater. "not" is unary.  "forall" and "exists" are unary with
	 * an argument list.  Warn if we encounter a formula that has more
	 * arguments than MAX_PREDICATE_ARITY.
	 *
	 * @return null if there are no problems or an error message
	 * if there are.
	 */
	@Nullable
	public String hasValidArgs()
	{
		return hasValidArgs(null, null);
	}

	/**
	 * Test whether the Formula uses logical operators and predicates
	 * with the correct number of arguments.  "equals", "&lt;=&gt;", and
	 * "=&gt;" are strictly binary.  "or", and "and" are binary or
	 * greater. "not" is unary.  "forall" and "exists" are unary with
	 * an argument list.  Warn if we encounter a formula that has more
	 * arguments than MAX_PREDICATE_ARITY.
	 *
	 * @param f        formula
	 * @param filename If not null, denotes the name of the file being
	 *                 parsed.
	 * @param lineNo   If not null, indicates the location of the
	 *                 expression (formula) being parsed in the file being read.
	 * @return an empty String if there are no problems or an error message
	 * if there are.
	 * @see #hasValidArgs() validArgs below for documentation
	 */
	@Nullable
	private static String hasValidArgs(@NotNull final Formula f, @Nullable final String filename, @Nullable final Integer lineNo)
	{
		// logger.finest("Formula: " + f.form);
		if (f.form.isEmpty() || !f.listP() || f.atom() || f.empty())
		{
			return null;
		}

		// args
		int argCount = 0;
		@NotNull String args = f.cdr();
		for (@NotNull IterableFormula argsF = new IterableFormula(args); !argsF.empty(); argsF.pop())
		{
			argCount++;
			@NotNull String arg = argsF.car();
			@NotNull Formula argF = Formula.of(arg);
			@Nullable String error = hasValidArgs(argF, filename, lineNo);
			if (error != null)
			{
				return error;
			}
		}

		// pred
		@NotNull String pred = f.car();
		if (pred.equals(AND) || pred.equals(OR))
		{
			if (argCount < 2)
			{
				return "Too few arguments for 'and' or 'or' in formula: \n" + f + "\n";
			}
		}
		else if (pred.equals(UQUANT) || pred.equals(EQUANT))
		{
			if (argCount != 2)
			{
				return "Wrong number of arguments for 'exists' or 'forall' in formula: \n" + f + "\n";
			}
			else
			{
				@NotNull Formula quantF = Formula.of(args);
				if (!Lisp.listP(quantF.car()))
				{
					return "No parenthesized variable list for 'exists' or 'forall' " + "in formula: \n" + f + "\n";
				}
			}
		}
		else if (pred.equals(IFF) || pred.equals(IF))
		{
			if (argCount != 2)
			{
				return "Wrong number of arguments for '<=>' or '=>' in formula: \n" + f + "\n";
			}
		}
		else if (pred.equals(EQUAL))
		{
			if (argCount != 2)
			{
				return "Wrong number of arguments for 'equals' in formula: \n" + f + "\n";
			}
		}
		else if (!isVariable(pred) && argCount > (MAX_PREDICATE_ARITY + 1))
		{
			@NotNull String location = "";
			if (filename != null && lineNo != null)
			{
				location = " near line " + lineNo + " in " + filename;
			}
			f.errors.add("Maybe too many arguments " + location + ": " + f + "\n");
		}
		return null;
	}

	/**
	 * Not yet implemented!  Test whether the Formula has variables that are not properly
	 * quantified.  The case tested for is whether a quantified variable
	 * in the antecedent appears in the consequent or vice versa.
	 *
	 * @return an empty String if there are no problems or an error message
	 * if there are.
	 */
	@Nullable
	@SuppressWarnings("SameReturnValue")
	public String hasValidQuantification()
	{
		// TODO
		return null;
	}

	// P R O P E R T I E S

	/**
	 * Test whether a term is a functional term
	 *
	 * @param term term
	 * @return whether a term is a functional term.
	 */
	public static boolean isFunctionalTerm(@NotNull final String term)
	{
		if (Lisp.listP(term))
		{
			@NotNull String pred = Lisp.car(term);
			return pred.length() > 2 && pred.endsWith(FN_SUFF);
		}
		return false;
	}

	/**
	 * Test whether a Formula is a functional term.  Note this assumes
	 * the textual convention of all functions ending with "Fn".
	 *
	 * @return whether a Formula is a functional term.
	 */
	public boolean isFunctionalTerm()
	{
		return isFunctionalTerm(form);
	}

	/**
	 * Test whether a form contains a Formula as an argument to
	 * other than a logical operator.
	 *
	 * @param form formula string
	 * @return whether a form contains a Formula as an argument to other than a logical operator.
	 */
	public static boolean isHigherOrder(@NotNull final String form)
	{
		if (Lisp.listP(form))
		{
			@NotNull String pred = Lisp.car(form);
			boolean logOp = isLogicalOperator(pred);
			@NotNull List<String> elements = elements(form);
			for (int i = 1; i < elements.size(); i++)
			{
				String arg = elements.get(i);
				if (!Lisp.atom(arg) && !isFunctionalTerm(arg))
				{
					if (logOp)
					{
						if (isHigherOrder(arg))
						{
							return true;
						}
					}
					else
					{
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * Test whether a Formula contains a Formula as an argument to
	 * other than a logical operator.
	 *
	 * @return whether a Formula contains a Formula as an argument to other than a logical operator.
	 */
	public boolean isHigherOrder()
	{
		return isHigherOrder(form);
	}

	/**
	 * Test whether a term is a variable
	 *
	 * @param term term
	 * @return whether a term is a variable
	 */
	public static boolean isVariable(@NotNull final String term)
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
		return isVariable(form);
	}

	/**
	 * Returns true only if this form, explicitly quantified or
	 * not, starts with "=&gt;" or "&lt;=&gt;", else returns false.  It would
	 * be better to test for the occurrence of at least one positive
	 * literal with one or more negative literals, but this test would
	 * require converting the Formula to clausal form.
	 *
	 * @param form formula string
	 * @return whether this Formula is a rule.
	 */
	public static boolean isRule(@NotNull final String form)
	{
		boolean result = false;
		if (Lisp.listP(form))
		{
			@NotNull String arg0 = Lisp.car(form);
			if (isQuantifier(arg0))
			{
				@NotNull String arg2 = Lisp.getArgument(form, 2);
				if (Lisp.listP(arg2))
				{
					result = isRule(arg2);
				}
			}
			else
			{
				result = IF_OPERATORS.contains(arg0);
			}
		}
		return result;
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
		return isRule(form);
	}

	/**
	 * Test whether a form is a simple list of terms (including functional terms).
	 *
	 * @param form formula string
	 * @return whether a Formula is a simple list of terms
	 */
	public static boolean isSimpleClause(@NotNull final String form)
	{
		logger.entering(LOG_SOURCE, "isSimpleClause");

		for (@NotNull IterableFormula f = new IterableFormula(form); !f.empty(); f.pop())
		{
			@NotNull String head = f.car();
			if (Lisp.listP(head))
			{
				if (!Formula.isFunction(Lisp.car(head)))
				{
					logger.exiting(LOG_SOURCE, "isSimpleClause", false);
					return false;
				}
				else if (!isSimpleClause(head))
				{
					logger.exiting(LOG_SOURCE, "isSimpleClause", false);
					return false;
				}
			}
		}
		logger.exiting(LOG_SOURCE, "isSimpleClause", true);
		return true;
	}

	/**
	 * Test whether a Formula is a simple list of terms (including functional terms).
	 *
	 * @return whether a Formula is a simple list of terms
	 */
	public boolean isSimpleClause()
	{
		return isSimpleClause(form);
	}

	/**
	 * Test whether a form is a simple clause wrapped in a negation.
	 *
	 * @param form formula string
	 * @return whether a Formula is a simple clause wrapped in a negation.
	 */
	public static boolean isSimpleNegatedClause(@NotNull final String form)
	{
		if (Lisp.empty(form) || Lisp.atom(form))
		{
			return false;
		}
		if (NOT.equals(Lisp.car(form)))
		{
			@NotNull String cdr = Lisp.cdr(form);
			if (!cdr.isEmpty() && Lisp.empty(Lisp.cdr(cdr)))
			{
				return isSimpleClause(Lisp.car(cdr));
			}
		}
		return false;
	}

	/**
	 * Test whether a Formula is a simple clause wrapped in a negation.
	 *
	 * @return whether a Formula is a simple clause wrapped in a negation.
	 */
	public boolean isSimpleNegatedClause()
	{
		return isSimpleNegatedClause(form);
	}

	/**
	 * Test whether a list with a predicate is a quantifier list
	 *
	 * @param listPred     list with a predicate.
	 * @param previousPred previous predicate
	 * @return whether a list with a predicate is a quantifier list.
	 */
	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	public static boolean isQuantifierList(@NotNull final String listPred, @NotNull final String previousPred)
	{
		return (previousPred.equals(EQUANT) || previousPred.equals(UQUANT)) && (listPred.startsWith(R_PREF) || listPred.startsWith(V_PREF));
	}

	/**
	 * Test whether term is a logical quantifier
	 *
	 * @param term A String, assumed to be an atomic SUO-KIF term.
	 * @return true if term is a logical quantifier
	 */
	public static boolean isQuantifier(@NotNull final String term)
	{
		return isNonEmpty(term) && (term.equals(EQUANT) || term.equals(UQUANT));
	}

	/**
	 * Test whether term is a logical operator
	 *
	 * @param term A String, assumed to be an atomic SUO-KIF term.
	 * @return true if term is a standard FOL logical operator, else returns false.
	 */
	public static boolean isLogicalOperator(@NotNull final String term)
	{
		return isNonEmpty(term) && LOGICAL_OPERATORS.contains(term);
	}

	/**
	 * Test whether term is a comparison operator
	 *
	 * @param term A String, assumed to be an atomic SUO-KIF term.
	 * @return true if term is a SUO-KIF predicate for comparing two (typically numeric) terms, else returns false.
	 */
	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	public static boolean isComparisonOperator(@NotNull final String term)
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
	public static boolean isFunction(@NotNull final String term)
	{
		return isNonEmpty(term) && term.endsWith(FN_SUFF);
	}

	/**
	 * Test whether term is a math function
	 *
	 * @param term A String, assumed to be an atomic SUO-KIF term.
	 * @return true if term is a SUO-KIF mathematical function, else returns false.
	 */
	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	public static boolean isMathFunction(@NotNull final String term)
	{
		return isNonEmpty(term) && MATH_FUNCTIONS.contains(term);
	}

	/**
	 * Test whether term is commutative
	 *
	 * @param term A String, assumed to be an atomic SUO-KIF term.
	 * @return true if term is a SUO-KIF commutative logical operator, else false.
	 */
	public static boolean isCommutative(@NotNull final String term)
	{
		return isNonEmpty(term) && (term.equals(AND) || term.equals(OR));
	}

	/**
	 * @param term A String.
	 * @return true if term is a SUO-KIF Skolem term, else returns false.
	 */
	public static boolean isSkolemTerm(@NotNull final String term)
	{
		return isNonEmpty(term) && term.trim().matches("^.?" + SK_PREF + "\\S*\\s*\\d+");
	}

	/**
	 * Test whether a formula is valid with no variable
	 *
	 * @param form formula string
	 * @return true if formula is a valid formula with no variables, else returns false.
	 */
	public static boolean isGround(@NotNull final String form)
	{
		if (isEmpty(form))
		{
			return false;
		}
		if (!form.contains("\""))
		{
			return !form.contains("?") && !form.contains("@");
		}
		boolean inQuote = false;
		for (int i = 0; i < form.length(); i++)
		{
			if (form.charAt(i) == '"')
			{
				inQuote = !inQuote;
			}
			if (!inQuote && (form.charAt(i) == '?' || form.charAt(i) == '@'))
			{
				return false;
			}
		}
		return true;
	}

	// P A R S E

	/**
	 * Parse a String into a List of Formulas.
	 * The String must be a LISP-style list.
	 *
	 * @return a List of Formulas
	 */
	@NotNull
	private static List<Formula> parseList(final String form)
	{
		@NotNull List<Formula> result = new ArrayList<>();
		@NotNull IterableFormula f = new IterableFormula(LP + form + RP);
		if (f.empty())
		{
			return result;
		}
		for (; !f.empty(); f.pop())
		{
			@NotNull Formula f2 = Formula.of(f.car());
			result.add(f2);
		}
		return result;
	}

	// C O M P A R E

	/**
	 * Test if forms are equal at a deeper level than a simple string equals.
	 * The only logical manipulation is to treat conjunctions and disjunctions as unordered
	 * bags of clauses. So (and A B C) will be logicallyEqual(s) for example,
	 * to (and B A C).  Note that this is a fairly time-consuming operation
	 * and should not generally be used for comparing large sets of formulas.
	 *
	 * @param form  formula string
	 * @param form2 formula string
	 * @return whether the contents of the formula are equal to the argument.
	 */
	public static boolean logicallyEquals(@NotNull final String form, @NotNull final String form2)
	{
		@NotNull String normalized = normalizedFormatted(form);
		@NotNull String normalized2 = normalizedFormatted(form2);
		if (normalized.equals(normalized2))
		{
			return true;
		}
		if (Lisp.atom(form) && form2.compareTo(form) != 0)
		{
			return false;
		}
		if (Lisp.atom(form2) && form2.compareTo(form) != 0)
		{
			return false;
		}

		@NotNull IterableFormula f = new IterableFormula(form);
		@NotNull IterableFormula f2 = new IterableFormula(form2);
		@NotNull String head = f.car();
		@NotNull String head2 = f2.car();
		if (AND.equals(head) || OR.equals(head))
		{
			if (!head2.equals(head))
			{
				return false;
			}
			f.pop();
			f2.pop();
			return compareFormulaSets(f.form, f2.form);
		}
		else
		{
			@NotNull Formula headF = Formula.of(head);
			@NotNull Formula tail2F = Formula.of(f2.cdr());
			return headF.logicallyEquals(f2.car()) && tail2F.logicallyEquals(f.cdr());
		}
	}

	/**
	 * Test if the contents of the formula are equal to the argument
	 * at a deeper level than a simple string equals.  The only logical
	 * manipulation is to treat conjunctions and disjunctions as unordered
	 * bags of clauses. So (and A B C) will be logicallyEqual(s) for example,
	 * to (and B A C).  Note that this is a fairly time-consuming operation
	 * and should not generally be used for comparing large sets of formulas.
	 *
	 * @param form2 formula string
	 * @return whether the contents of the formula are equal to the argument.
	 */
	public boolean logicallyEquals(@NotNull final String form2)
	{
		return logicallyEquals(form, form2);
	}

	/**
	 * Test if the contents of the formula are equal to the argument
	 * at a deeper level than a simple string equals.  The only logical
	 * manipulation is to treat conjunctions and disjunctions as unordered
	 * bags of clauses. So (and A B C) will be logicallyEqual(s) for example,
	 * to (and B A C).  Note that this is a fairly time-consuming operation
	 * and should not generally be used for comparing large sets of formulas.
	 *
	 * @param f2 formula
	 * @return whether the contents of the formula are equal to the argument.
	 */
	public boolean logicallyEquals(@NotNull final Formula f2)
	{
		return logicallyEquals(form, f2.form);
	}

	/**
	 * Compare two lists of formulas, testing whether they are equal,
	 * without regard to order.  (B A C) will be equal to (C B A). The
	 * method iterates through one list, trying to find a match in the other
	 * and removing it if a match is found.  If the lists are equal, the
	 * second list should be empty once the iteration is complete.
	 * Note that the formulas being compared must be lists, not atoms, and
	 * not a set of formulas unenclosed by parentheses.  So, "(A B C)"
	 * and "(A)" are valid, but "A" is not, nor is "A B C".
	 *
	 * @param form  form
	 * @param form2 other form
	 * @return true if equals without regard to order
	 */
	protected static boolean compareFormulaSets(@NotNull final String form, @NotNull final String form2)
	{
		@NotNull List<Formula> fs = parseList(form.substring(1, form.length() - 1));
		@NotNull List<Formula> f2s = parseList(form2.substring(1, form2.length() - 1));
		if (fs.size() != f2s.size())
		{
			return false;
		}
		for (@NotNull Formula f : fs)
		{
			for (int j = 0; j < f2s.size(); j++)
			{
				if (f.logicallyEquals(f2s.get(j).form))
				{
					f2s.remove(j);
					j = f2s.size();
				}
			}
		}
		return f2s.size() == 0;
	}

	// V A R I A B L E S

	/**
	 * Collects all variables in this form.  Returns
	 * a pair of sets.
	 * The first contains all explicitly quantified variables in the Formula.
	 * The second contains all variables in Formula that are not within the scope
	 * of some explicit quantifier.
	 *
	 * @param form formula string
	 * @return A pair of Lists, each of which could be empty
	 */
	@NotNull
	public static Tuple.Pair<Set<String>, Set<String>> collectVariables(@NotNull final String form)
	{
		@NotNull Set<String> quantified = collectQuantifiedVariables(form);
		@NotNull Set<String> unquantified = collectAllVariables(form);
		unquantified.removeAll(quantified);

		@NotNull Tuple.Pair<Set<String>, Set<String>> result = new Tuple.Pair<>();
		result.first = quantified;
		result.second = unquantified;
		logger.exiting(LOG_SOURCE, "collectVariables", result);
		return result;
	}

	/**
	 * Collects all variables in this Formula.  Returns
	 * a pair of sets.
	 * The first contains all explicitly quantified variables in the Formula.
	 * The second contains all variables in Formula that are not within the scope
	 * of some explicit quantifier.
	 *
	 * @return A pair of Lists, each of which could be empty
	 */
	@NotNull
	public Tuple.Pair<Set<String>, Set<String>> collectVariables()
	{
		return collectVariables(form);
	}

	/**
	 * Collects all variables in this Formula.  Returns a set
	 * of String variable names (with initial '?').
	 * Note that duplicates are not removed.
	 *
	 * @return A List of String variable names
	 */
	@NotNull
	public static Set<String> collectAllVariables(@NotNull final String form)
	{
		@NotNull Set<String> result = new HashSet<>();
		if (Lisp.listLength(form) < 1)
		{
			return result;
		}
		@NotNull String car = Lisp.car(form);
		if (isVariable(car))
		{
			result.add(car);
		}
		else
		{
			if (Lisp.listP(car))
			{
				result.addAll(collectAllVariables(car));
			}
		}

		@NotNull String cdr = Lisp.cdr(form);
		if (isVariable(cdr))
		{
			result.add(cdr);
		}
		else
		{
			if (Lisp.listP(cdr))
			{
				result.addAll(collectAllVariables(cdr));
			}
		}
		return result;
	}

	/**
	 * Collects all variables in this Formula.  Returns a set
	 * of String variable names (with initial '?').
	 * Note that duplicates are not removed.
	 *
	 * @return A List of String variable names
	 */
	@NotNull
	public Set<String> collectAllVariables()
	{
		return collectAllVariables(form);
	}

	/**
	 * Collects all variables in this Formula.  Returns a List
	 * of String variable names (with initial '?').
	 * Note that duplicates are not removed.
	 *
	 * @param form formula string
	 * @return A List of String variable names
	 */
	@NotNull
	public static List<String> collectAllVariablesOrdered(@NotNull final String form)
	{
		@NotNull List<String> result = new ArrayList<>();
		if (Lisp.listLength(form) < 1)
		{
			return result;
		}
		@NotNull String car = Lisp.car(form);
		if (isVariable(car))
		{
			result.add(car);
		}
		else
		{
			if (Lisp.listP(car))
			{
				result.addAll(collectAllVariablesOrdered(car));
			}
		}
		@NotNull String cdr = Lisp.cdr(form);
		if (isVariable(cdr))
		{
			result.add(cdr);
		}
		else
		{
			if (Lisp.listP(cdr))
			{
				result.addAll(collectAllVariablesOrdered(cdr));
			}
		}
		return result;
	}

	/**
	 * Collects all variables in this Formula.  Returns a List
	 * of String variable names (with initial '?').
	 * Note that duplicates are not removed.
	 *
	 * @return A List of String variable names
	 */
	@NotNull
	public List<String> collectAllVariablesOrdered()
	{
		return collectAllVariablesOrdered(form);
	}

	/**
	 * Collects all quantified variables in a form.  Returns a set
	 * of String variable names (with initial '?').  Note that
	 * duplicates are not removed.
	 *
	 * @param form formula string
	 * @return The set of quantified variable names
	 */
	@NotNull
	public static Set<String> collectQuantifiedVariables(@NotNull final String form)
	{
		@NotNull Set<String> result = new HashSet<>();
		if (Lisp.listLength(form) < 1)
		{
			return result;
		}
		@NotNull String car = Lisp.car(form);
		if (UQUANT.equals(car) || EQUANT.equals(car))
		{
			@NotNull String cdr = Lisp.cdr(form);
			if (!Lisp.listP(cdr))
			{
				System.err.println("ERROR in Formula.collectQuantifiedVariables(): incorrect quantification: " + form);
				return result;
			}
			@NotNull String vars = Lisp.car(cdr);
			result.addAll(collectAllVariables(vars));

			@Nullable String cdrCdr = Lisp.cdr(cdr);
			if (Lisp.listP(cdrCdr))
			{
				result.addAll(collectQuantifiedVariables(cdrCdr));
			}
		}
		else
		{
			if (Lisp.listP(car))
			{
				result.addAll(collectQuantifiedVariables(car));
			}
			@Nullable String cdr = Lisp.cdr(form);
			if (Lisp.listP(cdr))
			{
				result.addAll(collectQuantifiedVariables(cdr));
			}
		}
		return result;
	}

	/**
	 * Collects all quantified variables in this Formula.  Returns a set
	 * of String variable names (with initial '?').  Note that
	 * duplicates are not removed.
	 *
	 * @return The set of quantified variable names
	 */
	@NotNull
	public Set<String> collectQuantifiedVariables()
	{
		return collectQuantifiedVariables(form);
	}

	/**
	 * Collect all the unquantified variables in this Formula
	 *
	 * @return The set of unquantified variable names
	 */
	public Set<String> collectUnquantifiedVariables()
	{
		return collectVariables().second;
	}

	/**
	 * Gathers the row variable names in form and returns
	 * them in a SortedSet.
	 *
	 * @param form formula string
	 * @return a SortedSet, possibly empty, containing row variable
	 * names, each of which will start with the row variable
	 * designator '@'.
	 */
	@NotNull
	public static SortedSet<String> collectRowVariables(@NotNull final String form)
	{
		@NotNull SortedSet<String> result = new TreeSet<>();
		if (!form.isEmpty() && form.contains(R_PREF))
		{
			for (@NotNull IterableFormula f = new IterableFormula(form); f.listP() && !f.empty(); f.pop())
			{
				@NotNull String arg = f.getArgument(0);
				if (arg.startsWith(R_PREF))
				{
					result.add(arg);
				}
				else
				{
					if (Lisp.listP(arg))
					{
						result.addAll(collectRowVariables(arg));
					}
				}
			}
		}
		return result;
	}

	/**
	 * Gathers the row variable names in Formula and returns
	 * them in a SortedSet.
	 *
	 * @return a SortedSet, possibly empty, containing row variable
	 * names, each of which will start with the row variable
	 * designator '@'.
	 */
	@NotNull
	public SortedSet<String> collectRowVariables()
	{
		return collectRowVariables(form);
	}

	/**
	 * Collect all the terms in a formula
	 *
	 * @param form formula string
	 * @return set of terms
	 */
	@NotNull
	public static Set<String> collectTerms(@NotNull final String form)
	{
		@NotNull Set<String> terms = new HashSet<>();

		if (Lisp.empty(form))
		{
			return terms;
		}
		if (Lisp.atom(form))
		{
			terms.add(form);
		}
		else
		{
			for (@NotNull IterableFormula f = new IterableFormula(form); !f.empty(); f.pop())
			{
				terms.addAll(collectTerms(f.car()));
			}
		}
		return terms;
	}

	// T E R M S

	/**
	 * Collect all the terms in a formula
	 *
	 * @return set of terms
	 */
	@NotNull
	public Set<String> collectTerms()
	{
		return collectTerms(form);
	}

	/**
	 * Returns a Set of all atomic KIF Relation constants that
	 * occur as Predicates or Functions (argument 0 terms) in this
	 * form.
	 *
	 * @param form formula string
	 * @return a Set containing the String constants that denote
	 * KIF Relations in this Formula, or an empty Set.
	 */
	@NotNull
	public static Set<String> collectRelationConstants(@NotNull final String form)
	{
		@NotNull Set<String> result = new HashSet<>();
		@NotNull Set<String> todo = new HashSet<>();
		if (Lisp.listP(form) && !Lisp.empty(form))
		{
			todo.add(form);
		}
		while (!todo.isEmpty())
		{
			@NotNull List<String> forms = new ArrayList<>(todo);
			todo.clear();

			for (@NotNull String form2 : forms)
			{
				if (Lisp.listP(form2))
				{
					int i = 0;
					for (@Nullable IterableFormula f = new IterableFormula(form2); !f.empty(); f.pop(), i++)
					{
						@NotNull String arg = f.car();
						if (Lisp.listP(arg))
						{
							if (!Lisp.empty(arg))
							{
								todo.add(arg);
							}
						}
						else if (isQuantifier(arg))
						{
							todo.add(f.getArgument(2));
							break;
						}
						else if (i == 0 && !isVariable(arg) && !isLogicalOperator(arg) && !arg.equals(SKFN) && !StringUtil.isQuotedString(arg) && !arg.matches(".*\\s.*"))
						{
							result.add(arg);
						}
					}
				}
			}
		}
		return result;
	}

	/**
	 * Returns a Set of all atomic KIF Relation constants that
	 * occur as Predicates or Functions (argument 0 terms) in this
	 * Formula.
	 *
	 * @return a Set containing the String constants that denote
	 * KIF Relations in this Formula, or an empty Set.
	 */
	@NotNull
	public Set<String> collectRelationConstants()
	{
		return collectRelationConstants(form);
	}

	// U N I F I C A T I O N

	/**
	 * Unify var
	 *
	 * @return a Map of variable substitutions if successful, null if not
	 */
	@Nullable
	private static Map<String, String> unifyVar(@NotNull final String form1, final @NotNull String form2, final @NotNull Map<String, String> m)
	{
		if (m.containsKey(form1))
		{
			return unify(m.get(form1), form2, m);
		}
		else if (m.containsKey(form2))
		{
			return unify(m.get(form2), form1, m);
		}
		else if (form2.contains(form1))
		{
			return null;
		}
		else
		{
			m.put(form1, form2);
			return m;
		}
	}

	/**
	 * Unify (internal)
	 *
	 * @return a Map of variable substitutions if successful, null if not
	 */
	private static Map<String, String> unify(final @NotNull String form1, final @NotNull String form2, final @Nullable Map<String, String> m)
	{
		if (m == null)
		{
			return null;
		}
		else if (form1.equals(form2))
		{
			return m;
		}
		else if (isVariable(form1))
		{
			return unifyVar(form1, form2, m);
		}
		else if (isVariable(form2))
		{
			return unifyVar(form2, form1, m);
		}
		else if (Lisp.listP(form1) && Lisp.listP(form2))
		{
			Map<String, String> m2 = unify(Lisp.car(form1), Lisp.car(form2), m);
			if (m2 == null)
			{
				return null;
			}
			else
			{
				return unify(Lisp.cdr(form1), Lisp.cdr(form2), m2);
			}
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
	public Map<String, String> unify(@NotNull Formula f)
	{
		@NotNull Map<String, String> result = new TreeMap<>();
		return unify(f.form, form, result);
	}

	/**
	 * Makes implicit quantification explicit.
	 *
	 * @param exist controls whether to add universal or existential
	 *              quantification.  If true, add existential.
	 * @param form  formula string
	 * @return the formula as a String, with explicit quantification
	 */
	@NotNull
	public static String makeQuantifiersExplicit(boolean exist, @NotNull final String form)
	{
		@NotNull String result = form;

		@NotNull Tuple.Pair<Set<String>, Set<String>> vars = collectVariables(form);
		Set<String> uqVars = vars.second;
		if (!uqVars.isEmpty())
		{
			// Quantify all the unquantified variables
			@NotNull StringBuilder sb = new StringBuilder();
			sb.append(LP) //
					.append(exist ? EQUANT : UQUANT) //
					.append(' ') //
					.append(LP);
			// list of quantified variables
			boolean afterFirst = false;
			for (String uqVar : uqVars)
			{
				if (afterFirst)
				{
					sb.append(' ');
				}
				sb.append(uqVar);
				afterFirst = true;
			}
			sb.append(RP + " ");

			// body
			sb.append(form);
			sb.append(RP);
			result = sb.toString();
			logger.exiting(LOG_SOURCE, "makeQuantifiersExplicit", result);
		}
		return result;
	}

	/**
	 * Makes implicit quantification explicit.
	 *
	 * @param exist controls whether to add universal or existential
	 *              quantification.  If true, add existential.
	 * @return the formula as a String, with explicit quantification
	 */
	@NotNull
	public String makeQuantifiersExplicit(boolean exist)
	{
		return makeQuantifiersExplicit(exist, form);
	}

	/**
	 * Test if this Formula contains any variable arity relations
	 *
	 * @param kb - The KB used to compute variable arity relations.
	 * @return Returns true if this Formula contains any variable
	 * arity relations, else returns false.
	 */
	protected boolean containsVariableArityRelation(@NotNull KB kb)
	{
		boolean result = false;
		@NotNull Set<String> relns = kb.getCachedRelationValues("instance", "VariableArityRelation", 2, 1);
		relns.addAll(KB.VA_RELNS);
		for (@NotNull String reln : relns)
		{
			result = (form.contains(reln));
			if (result)
			{
				break;
			}
		}
		return result;
	}

	// R O W   V A R S

	/**
	 * This method attempts to revise the number of row var expansions
	 * to be done, based on the occurrence of forms such as (<pred>
	 * Note that variables such as ?ITEM throw off the
	 * default expected expansion count, and so must be dealt with to
	 * prevent unnecessary expansions.
	 *
	 * @param variableArity Indicates whether the overall expansion
	 *                      count for the Formula is governed by a variable arity relation,
	 *                      or not.
	 * @param count         The default expected expansion count, possibly to
	 *                      be revised.
	 * @param var           The row variable to be expanded.
	 * @return An int value, the revised expansion count.  In most
	 * cases, the count will not change.
	 */
	private int adjustExpansionCount(boolean variableArity, int count, @NotNull String var)
	{
		if (logger.isLoggable(Level.FINER))
		{
			@NotNull String[] params = {"variableArity = " + variableArity, "count = " + count, "var = " + var};
			logger.entering(LOG_SOURCE, "adjustExpansionCount", params);
		}
		int revisedCount = count;
		if (isNonEmpty(var))
		{
			@NotNull String rowVar = var;
			if (!var.startsWith("@"))
			{
				rowVar = ("@" + var);
			}
			@NotNull List<Formula> accumulator = new ArrayList<>();
			if (listP() && !empty())
			{
				accumulator.add(this);
			}
			while (!accumulator.isEmpty())
			{
				@NotNull List<Formula> fs = new ArrayList<>(accumulator);
				accumulator.clear();
				for (@NotNull final Formula f : fs)
				{
					@NotNull List<String> literal = f.elements();
					int len = literal.size();
					if (literal.contains(rowVar) && !isVariable(f.car()))
					{
						if (!variableArity && (len > 2))
						{
							revisedCount = (count - (len - 2));
						}
						else if (variableArity)
						{
							revisedCount = (10 - len);
						}
					}
					if (revisedCount < 2)
					{
						revisedCount = 2;
					}
					@Nullable Formula f2 = f;
					while (f2 != null && !f2.empty())
					{
						@NotNull Formula argF = Formula.of(f2.car());
						if (argF.listP() && !argF.empty())
						{
							accumulator.add(argF);
						}
						f2 = f2.cdrAsFormula();
					}
				}
			}
		}
		logger.exiting(LOG_SOURCE, "adjustExpansionCount", revisedCount);
		return revisedCount;
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
	 * @return a List of Formulas, or an empty List.
	 */
	@NotNull
	public List<Formula> expandRowVars(@NotNull KB kb)
	{
		logger.entering(LOG_SOURCE, "expandRowVars", kb.name);
		@NotNull List<Formula> result = new ArrayList<>();
		@Nullable SortedSet<String> rowVars = form.contains(R_PREF) ? collectRowVariables() : null;

		// If this Formula contains no row vars to expand, we just add it to resultList and quit.
		if ((rowVars == null) || rowVars.isEmpty())
		{
			result.add(this);
		}
		else
		{
			@NotNull Formula f = Formula.of(form);

			@NotNull Set<Formula> accumulator = new LinkedHashSet<>();
			accumulator.add(f);

			// Iterate through the row variables
			for (@NotNull String rowVar : rowVars)
			{
				@NotNull List<Formula> working = new ArrayList<>(accumulator);
				accumulator.clear();

				for (@NotNull Formula f2 : working)
				{
					@NotNull String f2Str = f2.form;
					if (!f2Str.contains(R_PREF) || (f2Str.contains("\"")))
					{
						f2.sourceFile = sourceFile;
						result.add(f2);
					}
					else
					{
						int[] range = f2.getRowVarExpansionRange(kb, rowVar);

						boolean hasVariableArityRelation = (range[0] == 0);
						range[1] = adjustExpansionCount(hasVariableArityRelation, range[1], rowVar);

						@NotNull StringBuilder varRepl = new StringBuilder();
						for (int j = 1; j < range[1]; j++)
						{
							if (varRepl.length() > 0)
							{
								varRepl.append(" ");
							}
							varRepl.append("?");
							varRepl.append(rowVar.substring(1));
							varRepl.append(j);
							if (hasVariableArityRelation)
							{
								@NotNull String f2Str2 = f2Str.replaceAll(rowVar, varRepl.toString());
								@NotNull Formula newF = Formula.of(f2Str2);

								// Copy the source file information for each expanded formula.
								newF.sourceFile = sourceFile;
								if (newF.form.contains(R_PREF) && (!newF.form.contains("\"")))
								{
									accumulator.add(newF);
								}
								else
								{
									result.add(newF);
								}
							}
						}
						if (!hasVariableArityRelation)
						{
							@NotNull String f2Str2 = f2Str.replaceAll(rowVar, varRepl.toString());
							@NotNull Formula newF = Formula.of(f2Str2);

							// Copy the source file information for each expanded formula.
							newF.sourceFile = sourceFile;
							if (newF.form.contains(R_PREF) && (newF.form.indexOf('"') == -1))
							{
								accumulator.add(newF);
							}
							else
							{
								result.add(newF);
							}
						}
					}
				}
			}
		}
		logger.exiting(LOG_SOURCE, "expandRowVars", result);
		return result;
	}

	/**
	 * Returns a two-place int[] indicating the low and high points of
	 * the expansion range (number of row var instances) for the input
	 * row var.
	 *
	 * @param kb     A KB required for processing.
	 * @param rowVar The row var (String) to be expanded.
	 * @return A two-place int[] object.  The int[] indicates a
	 * numeric range.  int[0] holds the start (the lowest number) in the
	 * range, and int[1] holds the highest number.  The default is
	 * [1,8].  If the Formula does not contain
	 */
	private int[] getRowVarExpansionRange(@NotNull KB kb, String rowVar)
	{
		if (logger.isLoggable(Level.FINER))
		{
			@NotNull String[] params = {"kb = " + kb.name, "rowVar = " + rowVar};
			logger.entering(LOG_SOURCE, "getRowVarExpansionRange", params);
		}
		@NotNull int[] result = new int[]{1, 8};
		if (isNonEmpty(rowVar))
		{
			@NotNull String var = rowVar;
			if (!var.startsWith("@"))
			{
				var = "@" + var;
			}
			@NotNull Map<String, int[]> minMaxMap = getRowVarsMinMax(kb);
			int[] newArr = minMaxMap.get(var);
			if (newArr != null)
			{
				result = newArr;
			}
		}
		logger.exiting(LOG_SOURCE, "getRowVarExpansionRange", result);
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
	 * numeric range.  int[0] is the start (the lowest number) in the
	 * range, and int[1] is the end.  If the Formula contains no row
	 * vars, the Map is empty.
	 */
	@NotNull
	private Map<String, int[]> getRowVarsMinMax(@NotNull KB kb)
	{
		logger.entering(LOG_SOURCE, "getRowVarsMinMax", kb.name);
		@NotNull Map<String, int[]> result = new HashMap<>();
		@Nullable Tuple.Triple<List<Clause>, Map<String, String>, Formula> clauseData = getClausalForms();
		if (clauseData == null)
		{
			return result;
		}

		@Nullable List<Clause> clauses = clauseData.first;
		if (clauses == null || clauses.isEmpty())
		{
			return result;
		}

		Map<String, String> varMap = clauseData.second;
		@NotNull Map<String, SortedSet<String>> rowVarRelns = new HashMap<>();
		for (@Nullable Clause clause : clauses)
		{
			if (clause != null)
			{
				// First we get the neg lits.  It may be that we should use *only* the neg lits for this
				// task, but we will start by combining the neg lits and pos lits into one list of literals
				// and see how that works.
				List<Formula> literals = clause.negativeLits;
				List<Formula> posLits = clause.positiveLits;
				literals.addAll(posLits);
				for (@NotNull Formula litF : literals)
				{
					litF.computeRowVarsWithRelations(rowVarRelns, varMap);
				}
			}
			// logger.finest("rowVarRelns == " + rowVarRelns);
			if (!rowVarRelns.isEmpty())
			{
				for (String rowVar : rowVarRelns.keySet())
				{
					@Nullable String origRowVar = Variables.getOriginalVar(rowVar, varMap);
					@NotNull int[] minMax = result.computeIfAbsent(origRowVar, k -> new int[]{0, 8});
					SortedSet<String> val = rowVarRelns.get(rowVar);
					for (@NotNull String reln : val)
					{
						int arity = kb.getValence(reln);
						if (arity >= 1)
						{
							minMax[0] = 1;
							int arityPlusOne = (arity + 1);
							if (arityPlusOne < minMax[1])
							{
								minMax[1] = arityPlusOne;
							}
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
		logger.exiting(LOG_SOURCE, "getRowVarsMinMax", result);
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
	protected void computeRowVarsWithRelations(@NotNull Map<String, SortedSet<String>> varsToRelns, @Nullable Map<String, String> varsToVars)
	{
		@NotNull Formula f = this;
		if (f.listP() && !f.empty())
		{
			@NotNull String relation = f.car();
			if (!isVariable(relation) && !relation.equals(SKFN))
			{
				@Nullable Formula newF = f.cdrAsFormula();
				while (newF != null && newF.listP() && !newF.empty())
				{
					@NotNull String term = newF.car();
					@Nullable String rowVar = term;
					if (isVariable(rowVar))
					{
						if (rowVar.startsWith(V_PREF) && (varsToVars != null))
						{
							rowVar = Variables.getOriginalVar(term, varsToVars);
						}
					}
					if (rowVar != null && rowVar.startsWith(R_PREF))
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
						@NotNull Formula termF = Formula.of(term);
						termF.computeRowVarsWithRelations(varsToRelns, varsToVars);
					}
					newF = newF.cdrAsFormula();
				}
			}
		}
	}

	// T Y P E

	/**
	 * A + is appended to the type if the parameter must be a class
	 *
	 * @return the type for each argument to the given predicate, where
	 * List element 0 is the result, if a function, 1 is the
	 * first argument, 2 is the second etc.
	 */
	@NotNull
	private List<String> getTypeList(@NotNull final String pred, @NotNull final KB kb)
	{
		List<String> result;

		// build the sortalTypeCache key.
		@NotNull String key = "gtl" + pred + kb.name;
		@NotNull Map<String, List<String>> stc = kb.getSortalTypeCache();
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

			@NotNull List<Formula> al = kb.askWithRestriction(0, "domain", 1, pred);
			@NotNull List<Formula> al2 = kb.askWithRestriction(0, "domainSubclass", 1, pred);
			@NotNull List<Formula> al3 = kb.askWithRestriction(0, "range", 1, pred);
			@NotNull List<Formula> al4 = kb.askWithRestriction(0, "rangeSubclass", 1, pred);

			@NotNull String[] r = new String[len];
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
	@NotNull
	@SuppressWarnings("UnusedReturnValue")
	private String[] addToTypeList(@NotNull final String pred, @NotNull final List<Formula> al, @NotNull final String[] result, boolean classP)
	{
		if (logger.isLoggable(Level.FINER))
		{
			@NotNull String[] params = {"pred = " + pred, "al = " + al, "result = " + Arrays.toString(result), "classP = " + classP};
			logger.entering(LOG_SOURCE, "addToTypeList", params);
		}
		// If the relations in al start with "range", argnum will be 0, and the arg position of the desired classnames will be 2.
		int argnum = 0;
		int clPos = 2;
		for (@NotNull Formula f : al)
		{
			// logger.finest("text: " + f.form);
			if (f.form.startsWith("(domain"))
			{
				argnum = Integer.parseInt(f.getArgument(2));
				clPos = 3;
			}
			@NotNull String cl = f.getArgument(clPos);
			if ((argnum < 0) || (argnum >= result.length))
			{
				@NotNull String errStr = "Possible arity confusion for " + pred;
				errors.add(errStr);
				logger.warning(errStr);
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
					@NotNull String errStr = "Multiple types asserted for argument " + argnum + " of " + pred + ": " + cl + ", " + result[argnum];
					errors.add(errStr);
					logger.warning(errStr);
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
	 * return the type of a Function.
	 *
	 * @param argIdx argument index
	 * @param pred   predicate
	 * @param kb     knowledge base
	 * @return type restriction
	 */
	@Nullable
	public static String findType(int argIdx, @NotNull final String pred, @NotNull final KB kb)
	{
		if (logger.isLoggable(Level.FINER))
		{
			@NotNull String[] params = {"numarg = " + argIdx, "pred = " + pred, "kb = " + kb.name};
			logger.entering(LOG_SOURCE, "findType", params);
		}

		// build the sortalTypeCache key.
		@NotNull String key = "ft" + argIdx + pred + kb.name;

		@NotNull Map<String, List<String>> stc = kb.getSortalTypeCache();
		List<String> results = stc.get(key);
		boolean isCached = results != null && !results.isEmpty();
		boolean cacheResult = !isCached;
		@Nullable String result = isCached ? results.get(0) : null;
		if (result == null)
		{
			@NotNull List<String> relations = new ArrayList<>();
			boolean found = false;
			@NotNull Set<String> accumulator = new HashSet<>();
			accumulator.add(pred);

			while (!found && !accumulator.isEmpty())
			{
				relations.clear();
				relations.addAll(accumulator);
				accumulator.clear();

				for (@NotNull String relation : relations)
				{
					if (found)
					{
						break;
					}
					if (argIdx > 0)
					{
						@NotNull List<Formula> formulas = kb.askWithRestriction(0, "domain", 1, relation);
						for (@NotNull Formula f : formulas)
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
							for (@NotNull Formula f : formulas)
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
						@NotNull List<Formula> formulas = kb.askWithRestriction(0, "range", 1, relation);
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
					for (@NotNull String r : relations)
					{
						accumulator.addAll(kb.getTermsViaAskWithRestriction(1, r, 0, "subrelation", 2));
					}
				}
			}
			if (cacheResult && (result != null))
			{
				stc.put(key, Collections.singletonList(result));
			}
		}
		logger.exiting(LOG_SOURCE, "findType", result);
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
	private void winnowTypeList(@Nullable final List<String> types, @NotNull final KB kb)
	{
		if (logger.isLoggable(Level.FINER))
		{
			@NotNull String[] params = {"types = " + types, "kb = " + kb.name};
			logger.entering(LOG_SOURCE, "winnowTypeList", params);
		}
		if ((types != null) && (types.size() > 1))
		{
			@NotNull String[] valArr = types.toArray(new String[0]);
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
				{
					break;
				}
			}
		}
		logger.exiting(LOG_SOURCE, "winnowTypeList");
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
	private void computeTypeRestrictions(@NotNull final List<String> ios, @NotNull final List<String> scs, @NotNull final String var, @NotNull final KB kb)
	{
		if (logger.isLoggable(Level.FINER))
		{
			@NotNull String[] params = {"ios = " + ios, "scs = " + scs, "var = " + var, "kb = " + kb.name};
			logger.entering(LOG_SOURCE, "computeTypeRestrictions", params);
		}
		if (!listP() || !form.contains(var))
		{
			return;
		}
		@NotNull Formula f = Formula.of(form);
		@NotNull String pred = f.car();
		if (isQuantifier(pred))
		{
			@NotNull String arg2 = f.getArgument(2);
			if (arg2.contains(var))
			{
				@NotNull Formula nextF = Formula.of(arg2);
				nextF.computeTypeRestrictions(ios, scs, var, kb);
			}
		}
		else if (isLogicalOperator(pred))
		{
			int len = f.listLength();
			for (int i = 1; i < len; i++)
			{
				@NotNull String argI = f.getArgument(i);
				if (argI.contains(var))
				{
					@NotNull Formula nextF = Formula.of(argI);
					nextF.computeTypeRestrictions(ios, scs, var, kb);
				}
			}
		}
		else
		{
			int valence = kb.getValence(pred);
			@NotNull List<String> types = getTypeList(pred, kb);
			int len = f.listLength();
			for (int i = 1; i < len; i++)
			{
				int argIdx = i;
				if (valence == 0) // pred is a VariableArityRelation
				{
					argIdx = 1;
				}
				@NotNull String arg = f.getArgument(i);
				if (arg.contains(var))
				{
					if (Lisp.listP(arg))
					{
						@NotNull Formula nextF = Formula.of(arg);
						nextF.computeTypeRestrictions(ios, scs, var, kb);
					}
					else if (var.equals(arg))
					{
						@Nullable String type = null;
						if (argIdx < types.size())
						{
							type = types.get(argIdx);
						}
						if (type == null)
						{
							type = findType(argIdx, pred, kb);
						}
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
								{
									scs.add(type);
								}
							}
							else if (!ios.contains(type))
							{
								ios.add(type);
							}
						}
					}
				}
			}
			// Special treatment for equal
			if (pred.equals("equal"))
			{
				@NotNull String arg1 = f.getArgument(1);
				@NotNull String arg2 = f.getArgument(2);
				@Nullable String term = null;
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
					if (Lisp.listP(term))
					{
						@NotNull Formula nextF = Formula.of(term);
						if (nextF.isFunctionalTerm())
						{
							@NotNull String fn = nextF.car();
							@NotNull List<String> classes = getTypeList(fn, kb);
							@Nullable String cl = null;
							if (!classes.isEmpty())
							{
								cl = classes.get(0);
							}
							if (cl == null)
							{
								cl = findType(0, fn, kb);
							}
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
									{
										scs.add(cl);
									}
								}
								else if (!ios.contains(cl))
								{
									ios.add(cl);
								}
							}
						}
					}
					else
					{
						@NotNull Set<String> instanceOfs = kb.getCachedRelationValues("instance", term, 1, 2);
						if (!instanceOfs.isEmpty())
						{
							for (@NotNull String io : instanceOfs)
							{
								if (!io.equals("Entity") && !ios.contains(io))
								{
									ios.add(io);
								}
							}
						}
					}
				}
			}
			// Special treatment for instance or subclass, only if var.equals(arg1) and arg2 is a functional term.
			else if (Arrays.asList("instance", "subclass").contains(pred))
			{
				@NotNull String arg1 = f.getArgument(1);
				@NotNull String arg2 = f.getArgument(2);
				if (var.equals(arg1) && Lisp.listP(arg2))
				{
					@NotNull Formula nextF = Formula.of(arg2);
					if (nextF.isFunctionalTerm())
					{
						@NotNull String fn = nextF.car();
						@NotNull List<String> classes = getTypeList(fn, kb);
						@Nullable String cl = null;
						if (!classes.isEmpty())
						{
							cl = classes.get(0);
						}
						if (cl == null)
						{
							cl = findType(0, fn, kb);
						}
						if (isNonEmpty(cl) && !cl.startsWith("Entity"))
						{
							while (cl.endsWith("+"))
							{
								cl = cl.substring(0, cl.length() - 1);
							}
							if (pred.equals("subclass"))
							{
								if (!scs.contains(cl))
								{
									scs.add(cl);
								}
							}
							else if (!ios.contains(cl))
							{
								ios.add(cl);
							}
						}
					}
				}
			}
		}
		logger.exiting(LOG_SOURCE, "computeTypeRestrictions");
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
	public void computeVariableTypesR(@NotNull final Map<String, List<List<String>>> map, @NotNull final KB kb)
	{
		if (logger.isLoggable(Level.FINER))
		{
			@NotNull String[] params = {"map = " + map, "kb = " + kb.name};
			logger.entering(LOG_SOURCE, "computeVariableTypesR", params);
		}
		if (listP() && !empty())
		{
			int len = listLength();
			@NotNull String arg0 = car();
			if (isQuantifier(arg0) && (len == 3))
			{
				computeVariableTypesQ(map, kb);
			}
			else
			{
				for (int i = 0; i < len; i++)
				{
					@NotNull Formula nextF = Formula.of(getArgument(i));
					nextF.computeVariableTypesR(map, kb);
				}
			}
		}
		logger.exiting(LOG_SOURCE, "computeVariableTypesR");
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
	private void computeVariableTypesQ(@NotNull final Map<String, List<List<String>>> map, @NotNull final KB kb)
	{
		if (logger.isLoggable(Level.FINER))
		{
			@NotNull String[] params = {"map = " + map, "kb = " + kb.name};
			logger.entering(LOG_SOURCE, "computeVariableTypesQ", params);
		}
		@NotNull Formula varListF = Formula.of(getArgument(1));
		@NotNull Formula nextF = Formula.of(getArgument(2));

		int vLen = varListF.listLength();
		for (int i = 0; i < vLen; i++)
		{
			@NotNull List<List<String>> types = new ArrayList<>();
			@NotNull List<String> ios = new ArrayList<>();
			@NotNull List<String> scs = new ArrayList<>();
			@NotNull String var = varListF.getArgument(i);
			nextF.computeTypeRestrictions(ios, scs, var, kb);
			if (!scs.isEmpty())
			{
				winnowTypeList(scs, kb);
				if (!scs.isEmpty() && !ios.contains("SetOrClass"))
				{
					ios.add("SetOrClass");
				}
			}
			if (!ios.isEmpty())
			{
				winnowTypeList(ios, kb);
			}
			types.add(ios);
			types.add(scs);
			map.put(var, types);
		}
		nextF.computeVariableTypesR(map, kb);
		logger.exiting(LOG_SOURCE, "computeVariableTypesQ");
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
	@NotNull
	private String insertTypeRestrictionsU(@NotNull final List<Tuple.Quad<String, String, List<String>, List<String>>> shelf, @NotNull final KB kb)
	{
		if (logger.isLoggable(Level.FINER))
		{
			@NotNull String[] params = {"shelf = " + shelf, "kb = " + kb.name};
			logger.entering(LOG_SOURCE, "insertTypeRestrictionsU", params);
		}
		String result;
		@NotNull String varList = getArgument(1);
		@NotNull Formula varListF = Formula.of(varList);

		@NotNull List<Tuple.Quad<String, String, List<String>, List<String>>> newShelf = makeNewShelf(shelf);
		int vLen = varListF.listLength();
		for (int i = 0; i < vLen; i++)
		{
			addVarDataQuad(varListF.getArgument(i), "U", newShelf);
		}

		@NotNull String arg2 = getArgument(2);
		@NotNull Formula nextF = Formula.of(arg2);
		@NotNull String processedArg2 = nextF.insertTypeRestrictionsR(newShelf, kb);
		@NotNull Set<String> constraints = new LinkedHashSet<>();

		for (@NotNull Tuple.Quad<String, String, List<String>, List<String>> quad : newShelf)
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
						{
							ios.add("SetOrClass");
						}
						for (String sc : scs)
						{
							@NotNull String constraint = "(subclass " + var + " " + sc + ")";
							if (!processedArg2.contains(constraint))
							{
								constraints.add(constraint);
							}
						}
					}
				}
				if (!ios.isEmpty())
				{
					winnowTypeList(ios, kb);
					for (String io : ios)
					{
						@NotNull String constraint = "(instance " + var + " " + io + ")";
						if (!processedArg2.contains(constraint))
						{
							constraints.add(constraint);
						}
					}
				}
			}
		}
		@NotNull StringBuilder sb = new StringBuilder();
		sb.append("(forall ");
		sb.append(varListF.form);
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
			{
				sb.append(" (and");
			}
			for (String constraint : constraints)
			{
				sb.append(" ");
				sb.append(constraint);
			}
			if (cLen > 1)
			{
				sb.append(")");
			}
			sb.append(" ");
			sb.append(processedArg2);
			sb.append(")");
		}
		sb.append(")");
		result = sb.toString();
		logger.exiting(LOG_SOURCE, "insertTypeRestrictionsU", result);
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
	@NotNull
	private String insertTypeRestrictionsE(@NotNull final List<Tuple.Quad<String, String, List<String>, List<String>>> shelf, @NotNull final KB kb)
	{
		if (logger.isLoggable(Level.FINER))
		{
			@NotNull String[] params = {"shelf = " + shelf, "kb = " + kb.name};
			logger.entering(LOG_SOURCE, "insertTypeRestrictionsE", params);
		}
		String result;
		@NotNull String varList = getArgument(1);
		@NotNull Formula varListF = Formula.of(varList);

		@NotNull List<Tuple.Quad<String, String, List<String>, List<String>>> newShelf = makeNewShelf(shelf);
		int vLen = varListF.listLength();
		for (int i = 0; i < vLen; i++)
		{
			addVarDataQuad(varListF.getArgument(i), "E", newShelf);
		}

		@NotNull String arg2 = getArgument(2);
		@NotNull Formula nextF = Formula.of(arg2);
		@NotNull String processedArg2 = nextF.insertTypeRestrictionsR(newShelf, kb);
		nextF = Formula.of(processedArg2);

		@NotNull Set<String> constraints = new LinkedHashSet<>();
		@NotNull StringBuilder sb = new StringBuilder();

		for (@NotNull Tuple.Quad<String, String, List<String>, List<String>> quad : newShelf)
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
						@NotNull String constraint = sb.toString();
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
						@NotNull String constraint = sb.toString();
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
		sb.append(varListF.form);
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
				sb.append(nextF.form);
			}
			sb.append(")");
		}
		sb.append(")");
		result = sb.toString();
		logger.exiting(LOG_SOURCE, "insertTypeRestrictionsE", result);
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
	@NotNull
	protected String insertTypeRestrictionsR(@NotNull final List<Tuple.Quad<String, String, List<String>, List<String>>> shelf, @NotNull final KB kb)
	{
		if (logger.isLoggable(Level.FINER))
		{
			@NotNull String[] params = {"shelf = " + shelf, "kb = " + kb.name};
			logger.entering(LOG_SOURCE, "insertTypeRestrictionsR", params);
		}
		@NotNull String result = form;
		if (Lisp.listP(form) && !Lisp.empty(form) && form.matches(".*\\?\\w+.*"))
		{
			@NotNull StringBuilder sb = new StringBuilder();
			@NotNull Formula f = Formula.of(form);
			int len = f.listLength();
			@NotNull String arg0 = f.car();
			if (isQuantifier(arg0) && (len == 3))
			{
				if (arg0.equals("forall"))
				{
					sb.append(f.insertTypeRestrictionsU(shelf, kb));
				}
				else
				{
					sb.append(f.insertTypeRestrictionsE(shelf, kb));
				}
			}
			else
			{
				sb.append("(");
				for (int i = 0; i < len; i++)
				{
					@NotNull String argI = f.getArgument(i);
					if (i > 0)
					{
						sb.append(" ");
						if (isVariable(argI))
						{
							@Nullable String type = findType(i, arg0, kb);
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
									addScForVar(argI, type, shelf);
								}
								else
								{
									addIoForVar(argI, type, shelf);
								}
							}
						}
					}
					@NotNull Formula nextF = Formula.of(argI);
					sb.append(nextF.insertTypeRestrictionsR(shelf, kb));
				}
				sb.append(")");
			}
			result = sb.toString();
		}
		logger.exiting(LOG_SOURCE, "insertTypeRestrictionsR", result);
		return result;
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
	@NotNull
	String addTypeRestrictions(@NotNull final KB kb)
	{
		logger.entering(LOG_SOURCE, "addTypeRestrictions", kb.name);
		@NotNull String form = makeQuantifiersExplicit(false);
		@NotNull Formula f = Formula.of(form);
		@NotNull String result = f.insertTypeRestrictionsR(new ArrayList<>(), kb);
		logger.exiting(LOG_SOURCE, "addTypeRestrictions", result);
		return result;
	}

	// S H E L F

	/**
	 * Add var data quad
	 */
	private void addVarDataQuad(@NotNull final String var, @NotNull final String quantToken, @NotNull final List<Tuple.Quad<String, String, List<String>, List<String>>> shelf)
	{
		@NotNull Tuple.Quad<String, String, List<String>, List<String>> quad = new Tuple.Quad<>();
		quad.first = var;                // e.g., "?X"
		quad.second = quantToken;        // "U" or "E"
		quad.third = new ArrayList<>();  // ios
		quad.fourth = new ArrayList<>();  // scs
		shelf.add(0, quad);
	}

	/**
	 * Ios
	 */
	@Nullable
	private List<String> getIosForVar(@NotNull final String var, @NotNull final List<Tuple.Quad<String, String, List<String>, List<String>>> shelf)
	{
		@Nullable List<String> result = null;
		for (@NotNull Tuple.Quad<String, String, List<String>, List<String>> quad : shelf)
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
	@Nullable
	private List<String> getScsForVar(@NotNull final String var, @NotNull final List<Tuple.Quad<String, String, List<String>, List<String>>> shelf)
	{
		@Nullable List<String> result = null;
		for (@NotNull Tuple.Quad<String, String, List<String>, List<String>> quad : shelf)
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
	private void addIoForVar(@NotNull final String var, @NotNull final String io, @NotNull final List<Tuple.Quad<String, String, List<String>, List<String>>> shelf)
	{
		if (isNonEmpty(io))
		{
			@Nullable List<String> ios = getIosForVar(var, shelf);
			if ((ios != null) && !ios.contains(io))
			{
				ios.add(io);
			}
		}
	}

	/**
	 * Add Sc
	 */
	private void addScForVar(@NotNull final String var, @NotNull final String sc, @NotNull final List<Tuple.Quad<String, String, List<String>, List<String>>> shelf)
	{
		if (isNonEmpty(sc))
		{
			@Nullable List<String> scs = getScsForVar(var, shelf);
			if ((scs != null) && !scs.contains(sc))
			{
				scs.add(sc);
			}
		}
	}

	/**
	 * Copy shelf
	 */
	@NotNull
	private List<Tuple.Quad<String, String, List<String>, List<String>>> makeNewShelf(@NotNull final List<Tuple.Quad<String, String, List<String>, List<String>>> shelf)
	{
		return new ArrayList<>(shelf);
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
	 * @return a List of Formula(s), which could be larger than
	 * the input List, variableReplacements, or could be empty.
	 */
	@NotNull
	List<Formula> addInstancesOfSetOrClass(@NotNull final KB kb, boolean isQuery, @Nullable List<Formula> variableReplacements)
	{
		@NotNull List<Formula> result = new ArrayList<>();
		if ((variableReplacements != null) && !variableReplacements.isEmpty())
		{
			if (isQuery)
			{
				result.addAll(variableReplacements);
			}
			else
			{
				@NotNull Set<Formula> formulae = new LinkedHashSet<>();
				for (@NotNull Formula f : variableReplacements)
				{
					formulae.add(f);

					// Make sure every SetOrClass is stated to be such.
					if (f.listP() && !f.empty())
					{
						@NotNull String arg0 = f.car();
						int start = -1;
						if (arg0.equals("subclass"))
						{
							start = 0;
						}
						else if (arg0.equals("instance"))
						{
							start = 1;
						}
						if (start > -1)
						{
							@NotNull List<String> args = Arrays.asList(f.getArgument(1), f.getArgument(2));
							int argsLen = args.size();
							for (int i = start; i < argsLen; i++)
							{
								String arg = args.get(i);
								if (!isVariable(arg) && !arg.equals("SetOrClass") && Lisp.atom(arg))
								{
									@NotNull StringBuilder sb = new StringBuilder();
									sb.setLength(0);
									sb.append("(instance ");
									sb.append(arg);
									sb.append(" SetOrClass)");
									@NotNull String ioStr = sb.toString();
									@NotNull Formula ioF = Formula.of(ioStr);
									ioF.sourceFile = sourceFile;
									if (!kb.formulaMap.containsKey(ioStr))
									{
										@NotNull Map<String, List<String>> stc = kb.getSortalTypeCache();
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
				// text.matches(".*\\(\\s*equal\\s+\\?*\\w+\\s+\\?*\\w+\\s+\\?*\\w+.*")

				// The formula contains non-ASCII characters.
				// was: ttext.matches(".*[\\x7F-\\xFF].*")
				// ||
				StringUtil.containsNonAsciiChars(form) || (!query && !isLogicalOperator(car()) && form.indexOf('"') == -1 && form.matches(".*\\?\\w+.*")));
	}

	// I N S T A N T I A T E

	private static class RejectException extends Exception
	{
		private static final long serialVersionUID = 5770027459770147573L;
	}

	/**
	 * This method returns a triple of query answer literals.
	 * The first element is a List of query literals that might be
	 * used to simplify the Formula to be instantiated.
	 * The second element is the query literal (List) that will be used as a
	 * template for doing the variable substitutions.
	 * All subsequent elements are ground literals (Lists).
	 *
	 * @param kb        A KB to query for answers.
	 * @param queryLits A List of query literals.  The first item in
	 *                  the list will be a SUO-KIF variable (String), which indexes the
	 *                  list.  Each subsequent item is a query literal (List).
	 * @return A triple of literals, or null if no query answers can be found.
	 */
	private static Tuple.Triple<List<List<String>>, List<String>, List<List<String>>> computeSubstitutionTuples(@Nullable final KB kb, @Nullable final Tuple.Pair<String, List<List<String>>> queryLits)
	{
		if (kb != null && queryLits != null)
		{
			// Variable
			String idxVar = queryLits.first;

			// Sort the query lits by number of variables.
			@NotNull List<List<String>> sortedQLits = new ArrayList<>(queryLits.second);
			sortedQLits.remove(0);
			if (sortedQLits.size() > 1)
			{
				@NotNull Comparator<List<String>> comp = (o1, o2) -> {
					@NotNull Integer c1 = Variables.getVarCount(o1);
					@NotNull Integer c2 = Variables.getVarCount(o2);
					return c1.compareTo(c2);
				};
				sortedQLits.sort(Collections.reverseOrder(comp));
			}

			// Put instance literals last.
			@NotNull List<List<String>> ioLits = new ArrayList<>();
			@NotNull List<List<String>> qLits = new ArrayList<>(sortedQLits);
			sortedQLits.clear();

			for (@NotNull List<String> ql : qLits)
			{
				if (ql.get(0).equals("instance"))
				{
					ioLits.add(ql);
				}
				else
				{
					sortedQLits.add(ql);
				}
			}
			sortedQLits.addAll(ioLits);

			// Literals that will be used to try to simplify the formula before pred var instantiation.
			@NotNull List<List<String>> simplificationLits = new ArrayList<>();

			// The literal that will serve as the pattern for extracting var replacement terms from answer/ literals.
			@Nullable List<String> keyLit = null;

			// The list of answer literals retrieved using the query lits, possibly built up via a sequence of multiple queries.
			@Nullable List<List<String>> answers = null;

			@NotNull Set<String> working = new HashSet<>();

			boolean satisfiable = true;
			boolean tryNextQueryLiteral = true;

			// The first query lit for which we get an answer is the key lit.
			for (int i = 0; i < sortedQLits.size() && tryNextQueryLiteral; i++)
			{
				List<String> ql = sortedQLits.get(i);
				@NotNull List<Formula> accumulator = kb.askWithLiteral(ql);
				satisfiable = !accumulator.isEmpty();
				tryNextQueryLiteral = (satisfiable || (Variables.getVarCount(ql) > 1));
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
						@NotNull List<List<String>> accumulator2 = KB.formulasToLists(accumulator);

						// Winnow the answers list.
						working.clear();
						int varPos = ql.indexOf(idxVar);
						for (@NotNull List<String> ql2 : accumulator2)
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
						for (@NotNull List<String> ql2 : accumulator2)
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
				@NotNull Tuple.Triple<List<List<String>>, List<String>, List<List<String>>> result = new Tuple.Triple<>();
				result.first = simplificationLits;
				result.second = keyLit;
				result.third = answers;
				return result;
			}
		}
		return null;
	}

	/**
	 * This method returns a List in which each element is
	 * a pair.  The first item of each pair is a variable.
	 * The second item in each pair is a list of query literals
	 * (Lists).
	 *
	 * @param kb         The KB to use for computing variable type signatures.
	 * @param varTypeMap A Map from variables to their types, as
	 *                   explained in the javadoc entry for gatherPredVars(kb)
	 * @return A List, or null if the input formula contains no
	 * predicate variables.
	 */
	@NotNull
	private List<Tuple.Pair<String, List<List<String>>>> prepareIndexedQueryLiterals(@NotNull KB kb, @Nullable Map<String, List<String>> varTypeMap)
	{
		if (logger.isLoggable(Level.FINER))
		{
			@NotNull String[] params = {"kb = " + kb.name, "varTypeMap = " + varTypeMap};
			logger.entering(LOG_SOURCE, "prepareIndexedQueryLiterals", params);
		}
		@NotNull List<Tuple.Pair<String, List<List<String>>>> result = new ArrayList<>();
		@NotNull Map<String, List<String>> varsWithTypes = varTypeMap != null ? varTypeMap : gatherPredVars(kb);
		// logger.finest("varsWithTypes = " + varsWithTypes);

		if (!varsWithTypes.isEmpty())
		{
			List<String> yOrN = varsWithTypes.get("arg0");
			if (yOrN.size() == 1 && "yes".equalsIgnoreCase(yOrN.get(0)))
			{
				// Try to simplify the formula.
				for (@NotNull String var : varsWithTypes.keySet())
				{
					if (isVariable(var))
					{
						List<String> varWithTypes = varsWithTypes.get(var);
						@Nullable Tuple.Pair<String, List<List<String>>> indexedQueryLits = gatherPredVarQueryLits(kb, varWithTypes);
						if (indexedQueryLits != null)
						{
							result.add(indexedQueryLits);
						}
					}
				}
			}
			// Else if the formula doesn't contain any arg0 pred vars, do nothing.
		}
		logger.exiting(LOG_SOURCE, "prepareIndexedQueryLiterals", result);
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
	@NotNull
	protected Map<String, List<String>> gatherPredVars(@NotNull KB kb)
	{
		logger.entering(LOG_SOURCE, "gatherPredVars", kb.name);
		@NotNull Map<String, List<String>> result = new HashMap<>();
		if (isNonEmpty(form))
		{
			@NotNull List<Formula> working = new ArrayList<>();
			@NotNull List<Formula> accumulator = new ArrayList<>();
			if (listP() && !empty())
			{
				accumulator.add(this);
			}
			while (!accumulator.isEmpty())
			{
				working.clear();
				working.addAll(accumulator);
				accumulator.clear();

				for (@NotNull Formula f : working)
				{
					int len = f.listLength();
					@NotNull String arg0 = f.getArgument(0);
					if (isQuantifier(arg0) || arg0.equals("holdsDuring") || arg0.equals("KappaFn"))
					{
						if (len > 2)
						{
							@NotNull String arg2 = f.getArgument(2);
							@NotNull Formula newF = Formula.of(arg2);
							if (f.listP() && !f.empty())
							{
								accumulator.add(newF);
							}
						}
						else
						{
							logger.warning("Malformed?: " + f.form);
						}
					}
					else if (arg0.equals("holds"))
					{
						accumulator.add(f.cdrAsFormula());
					}
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
							@NotNull String argN = f.getArgument(j);
							if ((signature != null) && (signature.length > j) && signature[j] && isVariable(argN))
							{
								List<String> vals = result.get(argN);
								if (vals == null)
								{
									vals = new ArrayList<>();
									result.put(argN, vals);
									vals.add(argN);
								}
								@Nullable String argType = kb.getArgType(arg0, j);
								if (!((argType == null) || vals.contains(argType)))
								{
									vals.add(argType);
								}
							}
							else
							{
								@NotNull Formula argF = Formula.of(argN);
								if (argF.listP() && !argF.empty())
								{
									accumulator.add(argF);
								}
							}
						}
					}
				}
			}
		}
		logger.exiting(LOG_SOURCE, "gatherPredVars", result);
		return result;
	}

	/**
	 * This method collects and returns literals likely to be of use
	 * as templates for retrieving predicates to be substituted for
	 * var.
	 *
	 * @param varWithTypes A List containing a variable followed,
	 *                     optionally, by class names indicating the type of the variable.
	 * @return A pair of literals (Lists) with var as first.
	 * The element of the pair is the variable (String).
	 * The second element is a List corresponding to SUO-KIF
	 * formulas, which will be used as query templates.
	 */
	@Nullable
	private Tuple.Pair<String, List<List<String>>> gatherPredVarQueryLits(@NotNull KB kb, @NotNull List<String> varWithTypes)
	{
		@NotNull Tuple.Pair<String, List<List<String>>> result = new Tuple.Pair<>();
		String var = varWithTypes.get(0);
		@NotNull Set<String> added = new HashSet<>();
		@Nullable Map<String, String> varMap = getVarMap();

		// Get the clauses for this Formula.
		@Nullable List<Clause> clauses = getClauses();
		if (clauses != null)
		{
			for (@NotNull Clause clause : clauses)
			{
				List<Formula> negLits = clause.negativeLits;
				// List<Formula> posLits = clause.positiveLits;
				if (!negLits.isEmpty())
				{
					int cim = 1;
					for (int ci = 0; ci < cim; ci++)
					{
						// Try the negLits first.  Then try the posLits only if there still are no results.
						@NotNull @SuppressWarnings("ConstantConditions") List<Formula> lit = ci == 0 ? clause.negativeLits : clause.positiveLits;
						for (@NotNull Formula f : lit)
						{
							if (f.form.matches(".*SkFn\\s+\\d+.*") || f.form.matches(".*Sk\\d+.*"))
							{
								continue;
							}
							int fLen = f.listLength();
							@NotNull String arg0 = f.getArgument(0);
							if (isNonEmpty(arg0))
							{
								// If arg0 corresponds to var, then var has to be of type Predicate, not of types Function or List.
								if (isVariable(arg0))
								{
									@Nullable String origVar = Variables.getOriginalVar(arg0, varMap);
									if (origVar != null && origVar.equals(var) && !varWithTypes.contains("Predicate"))
									{
										varWithTypes.add("Predicate");
									}
								}
								else
								{
									@NotNull List<String> queryLit = new ArrayList<>();
									queryLit.add(arg0);
									boolean foundVar = false;
									for (int i = 1; i < fLen; i++)
									{
										@Nullable String arg = f.getArgument(i);
										if (!Lisp.listP(arg))
										{
											if (isVariable(arg))
											{
												arg = Variables.getOriginalVar(arg, varMap);
												if (arg != null && arg.equals(var))
												{
													foundVar = true;
												}
											}
											queryLit.add(arg);
										}
									}
									if (queryLit.size() != fLen)
									{
										continue;
									}
									// If the literal does not start with a variable or with "holds" and does not
									// contain Skolem terms, but does contain the variable in which we're interested,
									// it is probably suitable as a query template, or might serve as a starting
									// place.  Use it, or a literal obtained with it.
									if (isPossibleRelnArgQueryPred(kb, arg0) && foundVar)
									{
										// || arg0.equals("disjoint"))
										String term = "";
										if (queryLit.size() > 2)
										{
											term = queryLit.get(2);
										}
										if (!(arg0.equals("instance") && term.equals("Relation")))
										{
											@NotNull String queryLitStr = queryLit.toString();
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
					@NotNull List<String> queryLit = new ArrayList<>();
					queryLit.add("instance");
					queryLit.add(var);
					queryLit.add(argType);
					@NotNull String qlString = queryLit.toString();
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
		{
			return null;
		}
		return result;
	}

	/**
	 * Returns a List of the Formulae that result from replacing
	 * all arg0 predicate variables in the input Formula with
	 * predicate names.
	 *
	 * @param kb A KB that is used for processing the Formula.
	 * @return A List of Formulas, or an empty List if no instantiations can be generated.
	 * @throws RejectException reject exception
	 */
	@NotNull
	public List<Formula> instantiatePredVars(@NotNull KB kb) throws RejectException
	{
		@NotNull List<Formula> result = new ArrayList<>();
		try
		{
			if (listP())
			{
				@NotNull String arg0 = getArgument(0);
				// First we do some checks to see if it is worth processing the formula.
				if (isLogicalOperator(arg0) && form.matches(".*\\(\\s*\\?\\w+.*"))
				{
					// Get all pred vars, and then compute query lits for the pred vars, indexed by var.
					@NotNull Map<String, List<String>> varsWithTypes = gatherPredVars(kb);
					if (!varsWithTypes.containsKey("arg0"))
					{
						// The formula has no predicate variables in arg0 position, so just return it.
						result.add(this);
					}
					else
					{
						@NotNull List<Tuple.Pair<String, List<List<String>>>> indexedQueryLits = prepareIndexedQueryLiterals(kb, varsWithTypes);
						@NotNull List<Tuple.Triple<List<List<String>>, List<String>, List<List<String>>>> substForms = new ArrayList<>();

						// First, gather all substitutions.
						for (Tuple.Pair<String, List<List<String>>> varQueryTuples : indexedQueryLits)
						{
							Tuple.Triple<List<List<String>>, List<String>, List<List<String>>> substTuples = computeSubstitutionTuples(kb, varQueryTuples);
							if (substTuples != null)
							{
								if (substForms.isEmpty())
								{
									substForms.add(substTuples);
								}
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
										{
											substForms.add(substTuples);
										}
									}
								}
							}
						}

						if (!substForms.isEmpty())
						{
							// Try to simplify the Formula.
							@NotNull Formula f = this;
							for (@NotNull Tuple.Triple<List<List<String>>, List<String>, List<List<String>>> substTuples : substForms)
							{
								@Nullable List<List<String>> litsToRemove = substTuples.first;
								if (litsToRemove != null)
								{
									for (List<String> lit : litsToRemove)
									{
										f = f.maybeRemoveMatchingLits(lit);
									}
								}
							}

							// Now generate pred var instantiations from the possibly simplified formula.
							@NotNull List<String> templates = new ArrayList<>();
							templates.add(f.form);

							// Iterate over all var plus query lits forms, getting a list of substitution literals.
							@NotNull Set<String> accumulator = new HashSet<>();
							for (@Nullable Tuple.Triple<List<List<String>>, List<String>, List<List<String>>> substTuples : substForms)
							{
								if ((substTuples != null))
								{
									// Iterate over all ground lits ...
									// Do not use litsToRemove, which we have already used above.
									// List<List<String>> litsToRemove = substTuples.first;

									// Remove and hold the tuple that indicates the variable substitution pattern.
									List<String> varTuple = substTuples.second;

									for (@NotNull List<String> groundLit : substTuples.third)
									{
										// Iterate over all formula templates, substituting terms from each ground lit for vars in the template.
										for (@NotNull String template : templates)
										{
											@NotNull Formula templateF = Formula.of(template);
											Set<String> quantVars = templateF.collectVariables().first;
											for (int i = 0; i < varTuple.size(); i++)
											{
												String var = varTuple.get(i);
												if (isVariable(var))
												{
													String term = groundLit.get(i);
													// Don't replace variables that are explicitly quantified.
													if (!quantVars.contains(var))
													{
														@NotNull List<Pattern> patterns = new ArrayList<>();
														@NotNull List<String> patternStrings = Arrays.asList("(\\W*\\()(\\s*holds\\s+\\" + var + ")(\\W+)",
																// "(\\W*\\()(\\s*\\" + var + ")(\\W+)",
																"(\\W*)(\\" + var + ")(\\W+)");
														for (@NotNull String patternString : patternStrings)
														{
															patterns.add(Pattern.compile(patternString));
														}
														for (@NotNull Pattern pattern : patterns)
														{
															@NotNull Matcher m = pattern.matcher(template);
															template = m.replaceAll("$1" + term + "$3");
														}
													}
												}
											}
											if (hasCorrectArity(template, kb))
											{
												accumulator.add(template);
											}
											else
											{
												logger.warning("Rejected formula because of incorrect arity: " + template);
												break;
											}
										}
									}
									templates.clear();
									templates.addAll(accumulator);
									accumulator.clear();
								}
							}
							result.addAll(KB.formsToFormulas(templates));
						}
						if (result.isEmpty())
						{
							throw new RejectException();
						}
					}
				}
			}
		}
		catch (RejectException r)
		{
			logger.warning("Rejected formula because " + r.getMessage());
			throw r;
		}
		return result;
	}

	/**
	 * Tries to successively instantiate predicate variables and then
	 * expand row variables in this Formula, looping until no new
	 * Formulae are generated.
	 *
	 * @param kb             The KB to be used for processing this Formula
	 * @param addHoldsPrefix If true, predicate variables are not
	 *                       instantiated
	 * @return a List of Formula(s), which could be empty.
	 */
	@NotNull
	List<Formula> replacePredVarsAndRowVars(@NotNull KB kb, boolean addHoldsPrefix)
	{
		if (logger.isLoggable(Level.FINER))
		{
			@NotNull String[] params = {"kb = " + kb.name, "addHoldsPrefix = " + addHoldsPrefix};
			logger.entering(LOG_SOURCE, "replacePredVarsAndRowVars", params);
		}
		@NotNull Formula startF = Formula.of(form);
		int prevAccumulatorSize = 0;
		@NotNull Set<Formula> accumulator = new LinkedHashSet<>();
		accumulator.add(startF);
		while (accumulator.size() != prevAccumulatorSize)
		{
			prevAccumulatorSize = accumulator.size();

			// Do pred var instantiations if we are not adding holds prefixes.
			if (!addHoldsPrefix)
			{
				@NotNull List<Formula> working = new ArrayList<>(accumulator);
				accumulator.clear();

				for (@NotNull Formula f : working)
				{
					try
					{
						@NotNull List<Formula> instantiations = f.instantiatePredVars(kb);
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
							// after row vars have been expanded, so we loop until no new Formulae
							// are being generated.
							accumulator.addAll(instantiations);
						}
					}
					catch (RejectException r)
					{
						// If the formula can't be instantiated at all and so has been thrown "reject", don't add anything.
						@NotNull String errStr = "No predicate instantiations";
						errors.add(errStr);
						errStr += " for " + f.form;
						logger.warning(errStr);
					}
				}
			}
			// Row var expansion. Iterate over the instantiated predicate formulas,
			// doing row var expansion on each.  If no predicate instantiations can be generated, the accumulator
			// will contain just the original input formula.
			if (!accumulator.isEmpty() && (accumulator.size() < AXIOM_EXPANSION_LIMIT))
			{
				@NotNull List<Formula> working = new ArrayList<>(accumulator);
				accumulator.clear();
				for (@NotNull Formula f : working)
				{
					accumulator.addAll(f.expandRowVars(kb));
					if (accumulator.size() > AXIOM_EXPANSION_LIMIT)
					{
						logger.warning("Axiom expansion limit (" + AXIOM_EXPANSION_LIMIT + ") exceeded");
						break;
					}
				}
			}
		}
		@NotNull List<Formula> result = new ArrayList<>(accumulator);
		logger.exiting(LOG_SOURCE, "replacePredVarsAndRowVars", result);
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
	@NotNull
	private Formula maybeRemoveMatchingLits(List<String> litArr)
	{
		@Nullable Formula f = KB.literalListToFormula(litArr);
		if (f != null)
		{
			return maybeRemoveMatchingLits(f);
		}
		else
		{
			return this;
		}
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
	@NotNull
	private Formula maybeRemoveMatchingLits(@NotNull Formula litF)
	{
		logger.entering(LOG_SOURCE, "maybeRemoveMatchingLits", litF);
		@Nullable Formula result = null;
		@NotNull Formula f = this;
		if (f.listP() && !f.empty())
		{
			@NotNull StringBuilder litBuf = new StringBuilder();
			@NotNull String arg0 = f.car();
			if (Arrays.asList(IF, IFF).contains(arg0))
			{
				@NotNull String arg1 = f.getArgument(1);
				@NotNull String arg2 = f.getArgument(2);
				if (arg1.equals(litF.form))
				{
					@NotNull Formula arg2F = Formula.of(arg2);
					litBuf.append(arg2F.maybeRemoveMatchingLits(litF).form);
				}
				else if (arg2.equals(litF.form))
				{
					@NotNull Formula arg1F = Formula.of(arg1);
					litBuf.append(arg1F.maybeRemoveMatchingLits(litF).form);
				}
				else
				{
					@NotNull Formula arg1F = Formula.of(arg1);
					@NotNull Formula arg2F = Formula.of(arg2);
					litBuf.append("(") //
							.append(arg0) //
							.append(" ") //
							.append(arg1F.maybeRemoveMatchingLits(litF).form) //
							.append(" ") //
							.append(arg2F.maybeRemoveMatchingLits(litF).form) //
							.append(")");
				}
			}
			else if (isQuantifier(arg0) || arg0.equals("holdsDuring") || arg0.equals("KappaFn"))
			{
				@NotNull Formula arg2F = Formula.of(f.caddr());
				litBuf.append("(").append(arg0).append(" ").append(f.cadr()).append(" ").append(arg2F.maybeRemoveMatchingLits(litF).form).append(")");
			}
			else if (isCommutative(arg0))
			{
				@NotNull List<String> lits = f.elements();
				lits.remove(litF.form);
				@NotNull StringBuilder args = new StringBuilder();
				int len = lits.size();
				for (int i = 1; i < len; i++)
				{
					@NotNull Formula argF = Formula.of(lits.get(i));
					args.append(" ").append(argF.maybeRemoveMatchingLits(litF).form);
				}
				if (len > 2)
				{
					args = new StringBuilder(("(" + arg0 + args + ")"));
				}
				else
				{
					args = new StringBuilder(args.toString().trim());
				}
				litBuf.append(args);
			}
			else
			{
				litBuf.append(f.form);
			}
			result = Formula.of(litBuf.toString());
		}
		if (result == null)
		{
			result = this;
		}
		logger.exiting(LOG_SOURCE, "maybeRemoveMatchingLits", result);
		return result;
	}

	/**
	 * Return true if the input predicate can take relation names as
	 * arguments, else returns false.
	 */
	private boolean isPossibleRelnArgQueryPred(@NotNull KB kb, @NotNull String predicate)
	{
		return isNonEmpty(predicate) && ((kb.getRelnArgSignature(predicate) != null) || predicate.equals("instance"));
	}

	/**
	 * Replace variables with a value as given by the map argument
	 *
	 * @param map variable-value map
	 * @return formula with variables replaced by values
	 */
	@NotNull
	public Formula substituteVariables(@NotNull Map<String, String> map)
	{
		logger.entering(LOG_SOURCE, "substituteVariables", map);
		@NotNull Formula newFormula = Formula.EMPTY_LIST;
		if (atom())
		{
			if (map.containsKey(form))
			{
				String value = map.get(form);
				// TODO if (listP(value))
				// TODO {
				// TODO 	value = "(" + value + ")";
				// TODO }
				return Formula.of(value);
			}
			return this;
		}
		if (!empty())
		{
			@NotNull Formula f1 = Formula.of(car());
			if (f1.listP())
			{
				newFormula = newFormula.cons(f1.substituteVariables(map));
			}
			else
			{
				newFormula = newFormula.append(f1.substituteVariables(map));
			}
			@NotNull Formula f2 = Formula.of(cdr());
			newFormula = newFormula.append(f2.substituteVariables(map));
		}
		logger.exiting(LOG_SOURCE, "substituteVariables", newFormula);
		return newFormula;
	}

	/**
	 * Use a SortedMap of [varName, value] to substitute value in for
	 * varName wherever it appears in the formula.  This is
	 * iterative, since values can themselves contain varNames.
	 *
	 * @param map sorted map of [var, value] pairs
	 * @return formula
	 */
	@NotNull
	public Formula substitute(@NotNull final SortedMap<String, String> map)
	{
		@NotNull String form = this.form;
		@Nullable String newForm = null;
		while (!form.equals(newForm))
		{
			newForm = form;
			form = substituteVariables(map).form;
		}
		return Formula.of(form);
	}

	/**
	 * Replace var with term.
	 *
	 * @param var  variable
	 * @param term term
	 * @return formula with term substituted for variable
	 */
	@NotNull
	public Formula replaceVar(@NotNull final String var, @NotNull final String term)
	{
		if (form.isEmpty() || empty())
		{
			return this;
		}
		if (isVariable())
		{
			if (form.equals(var))
			{
				return Formula.of(term);
			}
			return this;
		}
		if (atom())
		{
			return this;
		}
		@NotNull Formula newF = Formula.EMPTY_LIST;
		if (!empty())
		{
			@NotNull Formula headF = Formula.of(car());
			headF = headF.replaceVar(var, term);
			if (headF.listP())
			{
				newF = newF.cons(headF);
			}
			else
			{
				newF = newF.append(headF);
			}

			@NotNull Formula tailF = Formula.of(cdr());
			tailF = tailF.replaceVar(var, term);
			return newF.append(tailF);
		}
		return newF;
	}

	// A R I T Y

	public static class ArityException extends Exception
	{
		private static final long serialVersionUID = 5770027459770147573L;

		final String rel;

		final int expectedArity;

		final int foundArity;

		public ArityException(final String rel, final int expectedArity, final int foundArity)
		{
			this.rel = rel;
			this.expectedArity = expectedArity;
			this.foundArity = foundArity;
		}

		@NotNull
		@Override
		public String toString()
		{
			return "ArityException{" + "rel='" + rel + '\'' + ", expected=" + expectedArity + ", found=" + foundArity + '}';
		}
	}

	/**
	 * Operator arity
	 *
	 * @param op operator
	 * @return the integer arity of the given logical operator
	 */
	public static int operatorArity(@NotNull final String op)
	{
		@NotNull String[] kifOps = {UQUANT, EQUANT, NOT, AND, OR, IF, IFF};

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

	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	public boolean hasCorrectArity(@NotNull final KB kb)
	{
		return hasCorrectArity(form, kb);
	}

	public void hasCorrectArityThrows(@NotNull final KB kb) throws ArityException
	{
		hasCorrectArityThrows(form, kb);
	}

	public static boolean hasCorrectArity(final String formula, @NotNull final KB kb)
	{
		try
		{
			hasCorrectArityThrows(formula, kb);
		}
		catch (ArityException ae)
		{
			return false;
		}
		return true;
	}

	public static void hasCorrectArityThrows(final String form0, @NotNull final KB kb) throws ArityException
	{
		String form = form0;
		form = form.replaceAll("exists\\s+(\\([^(]+?\\))", "");
		form = form.replaceAll("forall\\s+(\\([^(]+?\\))", "");
		form = form.replaceAll("\".*?\"", "?MATCH");

		@NotNull Pattern p = Pattern.compile("(\\([^(]+?\\))");
		@NotNull Matcher m = p.matcher(form);
		while (m.find())
		{
			String f = m.group(1);
			if (f.length() > 2)
			{
				f = f.substring(1, f.length() - 1);
			}
			@NotNull String[] split = f.split(" ");
			if (split.length > 1)
			{
				String rel = split[0];
				if (!rel.startsWith("?"))
				{
					int arity;
					if (rel.equals("=>") || rel.equals("<=>"))
					{
						arity = 2;
					}
					else
					{
						arity = kb.getValence(rel);
					}

					boolean startsWith = false;
					// disregard statements using the @ROW variable as it
					// will more often than not resolve to a wrong arity
					for (int i = 1; i < split.length; i++)
					{
						if (split[i].startsWith("@"))
						{
							startsWith = true;
							break;
						}
					}
					if (!startsWith)
					{
						int foundArity = split.length - 1;
						if (arity >= 1 && foundArity != arity)
						{
							throw new ArityException(rel, arity, foundArity);
						}
					}
				}
			}
			form = form.replace("(" + f + ")", "?MATCH");
			m = p.matcher(form);
		}
	}

	// R E P R E S E N T A T I O N

	@NotNull
	private static final String legalTermChars = "-:";
	@NotNull
	private static final String varStartChars = "?@";

	/**
	 * The URL to be referenced to a hyperlinked term.
	 */
	@NotNull
	private static final String hyperlink = "";

	/**
	 * Format a formula for either text or HTML presentation by inserting
	 * the proper hyperlink code, characters for indentation and end of line.
	 * A standard LISP-style pretty printing is employed where an open
	 * parenthesis triggers a new line and added indentation.
	 *
	 * @param indentChars - the proper characters for indenting text.
	 * @param eolChars    - the proper character for end of line.
	 * @return a formula formatted for either text or HTML presentation.
	 */
	@NotNull
	public String format(@NotNull final String indentChars, @NotNull final String eolChars)
	{
		// accumulators
		@NotNull final StringBuilder token = new StringBuilder();
		@NotNull final StringBuilder formatted = new StringBuilder();

		// state
		boolean inQuantifier = false;
		boolean inToken = false;
		boolean inVariable = false;
		boolean inVarList = false;
		boolean inComment = false;
		int indentLevel = 0;

		char pch = '0';  // previous char at (i-1)
		String form2 = form.trim();
		for (int i = 0, len = form2.length(); i < len; i++)
		{
			// current char
			char ch = form2.charAt(i);

			// in string
			if (inComment)
			{
				formatted.append(ch);

				// add spaces to long URL strings
				if (i > 70 && ch == '/')
				{
					formatted.append(" ");
				}

				// end of string
				if (ch == '"')
				{
					inComment = false;
				}
			}
			else
			{
				// indent
				if (ch == '(' && !inQuantifier && (indentLevel != 0 || i > 1))
				{
					if (Character.isWhitespace(pch))
					{
						formatted.deleteCharAt(formatted.length() - 1);
					}
					formatted.append(eolChars);
					for (int j = 0; j < indentLevel; j++)
					{
						formatted.append(indentChars);
					}
				}
				if (i == 0 && ch == '(')
				{
					formatted.append(ch);
				}

				// token
				if (!inToken && !inVariable && Character.isJavaIdentifierStart(ch))
				{
					token.setLength(0); // = new StringBuilder();
					inToken = true;
				}
				if (inToken && (Character.isJavaIdentifierPart(ch) || (legalTermChars.indexOf(ch) > -1)))
				{
					token.append(ch);
				}

				// special
				switch (ch)
				{
					// start of list
					case '(':
					{
						if (inQuantifier)
						{
							inQuantifier = false;
							inVarList = true;
							token.setLength(0); // new StringBuilder();
						}
						else
						{
							indentLevel++;
						}
					}
					break;

					// end of list
					case ')':
					{
						if (!inVarList)
						{
							indentLevel--;
						}
						else
						{
							inVarList = false;
						}
					}
					break;

					// string delim
					case '"':
					{
						inComment = true;
						if (i == 0)
						{
							formatted.append(ch);
						}
					}
					break;

					// single quote
					case '\'':
					{
						if (i == 0)
						{
							formatted.append(ch);
						}
					}
					break;
				}

				// in quantifier
				if ((token.indexOf("forall") > -1) || (token.indexOf("exists") > -1))
				{
					inQuantifier = true;
				}

				// in variable
				if (inVariable && !Character.isJavaIdentifierPart(ch) && (legalTermChars.indexOf(ch) == -1))
				{
					inVariable = false;
				}
				if (varStartChars.indexOf(ch) > -1)
				{
					inVariable = true;
				}

				// in token
				if (inToken && !Character.isJavaIdentifierPart(ch) && (legalTermChars.indexOf(ch) == -1))
				{
					inToken = false;
					if (isNonEmpty(hyperlink))
					{
						formatted.append("<a href=\"") //
								.append(hyperlink) //
								.append("&term=") //
								.append(token) //
								.append("\">") //
								.append(token) //
								.append("</a>");
					}
					else
					{
						formatted.append(token);
					}
					token.setLength(0); // = new StringBuilder();
				}

				// character
				if (i > 0 && !inToken && !(Character.isWhitespace(ch) && pch == '('))
				{
					if (Character.isWhitespace(ch))
					{
						if (!Character.isWhitespace(pch))
						{
							formatted.append(" ");
						}
					}
					else
					{
						formatted.append(ch);
					}
				}
			}
			// next
			pch = ch;
		}

		if (inToken)
		{
			// a term which is outside of parenthesis, typically, a binding.
			if (isNonEmpty(hyperlink))
			{
				formatted.append("<a href=\"") //
						.append(hyperlink) //
						.append("&term=") //
						.append(token) //
						.append("\">") //
						.append(token) //
						.append("</a>");
			}
			else
			{
				formatted.append(token);
			}
		}

		//
		if (inComment)
		{
			throw new IllegalArgumentException(formatted.toString());
		}

		return formatted.toString();
	}

	/**
	 * Format a formula for text presentation.
	 *
	 * @return formatted string representation
	 */
	@NotNull
	public String toString()
	{
		return format("  ", "\n");
	}

	/**
	 * Flat Format a formula for text presentation.
	 *
	 * @return flat formatted string representation
	 */
	@NotNull
	public String toFlatString()
	{
		return format("", " ");
	}

	/**
	 * Format a formula for text presentation.
	 *
	 * @return original string representation
	 */
	@NotNull
	public String toOrigString()
	{
		return form.trim();
	}

	/**
	 * Format a formula as a prolog statement.  Note that only tuples
	 * are converted properly at this time.  Statements with any embedded
	 * formulas or functions will be rejected with a null return.
	 *
	 * @return a prolog statement for the formula
	 */
	@NotNull
	public String toProlog()
	{
		if (!listP())
		{
			logger.warning("Not a formula: " + form);
			return "";
		}
		if (empty())
		{
			logger.warning("Empty formula: " + form);
			return "";
		}
		@NotNull StringBuilder result = new StringBuilder();
		@NotNull String relation = car();
		if (!Lisp.atom(relation))
		{
			logger.warning("Relation not an atom: " + relation);
			return "";
		}
		result.append(relation).append("('");

		for (@NotNull IterableFormula f = new IterableFormula(cdr()); !f.empty(); )
		{
			@NotNull String arg = f.car();
			if (!Lisp.atom(arg))
			{
				logger.warning("Argument not an atom: " + arg);
				return "";
			}
			result.append(arg).append("'");

			f.pop();
			if (!f.empty())
			{
				result.append(",'");
			}
			else
			{
				result.append(").");
			}
		}
		return result.toString();
	}

	// H E L P E R S

	private static boolean isEmpty(@Nullable String str)
	{
		return str == null || str.isEmpty();
	}

	private static boolean isNonEmpty(@Nullable String str)
	{
		return str != null && !str.isEmpty();
	}
}
