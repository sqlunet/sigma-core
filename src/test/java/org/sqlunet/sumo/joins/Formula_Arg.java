/*
 * Copyright (c) 2022.
 * This code is copyright Bernard Bou <1313ou@gmail.com>
 * This software is released under the GNU Public License 3 <http://www.gnu.org/copyleft/gpl.html>.
 */

package org.sqlunet.sumo.joins;

import org.sigma.core.NotNull;

import org.sqlunet.sumo.FormulaParser;
import org.sqlunet.common.NotFoundException;
import org.sqlunet.common.Insertable;
import org.sqlunet.sumo.objects.Arg;
import org.sqlunet.sumo.objects.Formula;
import org.sqlunet.sumo.objects.Term;

import java.io.IOException;
import java.io.Serializable;
import java.text.ParseException;
import java.util.*;

public class Formula_Arg implements Insertable, Serializable, Comparable<Formula_Arg>
{
	private static final Comparator<Formula_Arg> COMPARATOR = Comparator.comparing(Formula_Arg::getArgNum);
	private final Formula formula;

	private final Term term;

	private final Arg arg;

	// C O N S T R U C T

	private Formula_Arg(final Formula formula, final Term term, final Arg arg)
	{
		this.formula = formula;
		this.term = term;
		this.arg = arg;
	}

	@NotNull
	public static List<Formula_Arg> make(@NotNull final Formula formula) throws IllegalArgumentException, ParseException, IOException
	{
		@NotNull final List<Formula_Arg> result = new ArrayList<>();
		@NotNull final Map<String, Arg> map = FormulaParser.parse(formula.formula);
		for (@NotNull final Map.Entry<String, Arg> entry : map.entrySet())
		{
			final String key = entry.getKey();
			@NotNull final Term term = Term.make(key);
			final Arg parse = entry.getValue();
			result.add(new Formula_Arg(formula, term, parse));
		}
		Collections.sort(result);
		return result;
	}

	// A C C E S S

	public Formula getFormula()
	{
		return formula;
	}

	public Term getTerm()
	{
		return term;
	}

	public Arg getArg()
	{
		return arg;
	}

	public int getArgNum()
	{
		return arg.argumentNum;
	}

	// O R D E R

	@Override
	public int compareTo(@NotNull final Formula_Arg that)
	{
		return COMPARATOR.compare(this, that);
	}

	// I N S E R T
	@Override
	public String dataRow() throws NotFoundException
	{
		return String.format("%s,%s,%s", //
				arg.dataRow(),// 1
				resolveFormula(formula), // 2
				resolveTerm(term) // 3
		);
	}

	@Override
	public String comment()
	{
		return String.format("%s, %s", term.term, formula.toShortString(32));
	}

	// R E S O L V E

	protected int resolve()
	{
		return -1;
	}

	protected int resolveTerm(@NotNull final Term term)
	{
		return term.resolve();
	}

	protected int resolveFormula(@NotNull final Formula formula)
	{
		return formula.resolve();
	}
}
