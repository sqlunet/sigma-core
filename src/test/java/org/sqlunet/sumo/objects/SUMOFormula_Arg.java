package org.sqlunet.sumo.objects;

import org.sqlunet.sumo.Insertable;
import org.sqlunet.sumo.NotFoundException;
import org.sqlunet.sumo.SUMOParser;

import java.io.IOException;
import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SUMOFormula_Arg implements Insertable, Serializable
{
	private final SUMOFormula formula;

	private final SUMOTerm term;

	private final SUMOArg parse;

	// C O N S T R U C T

	private SUMOFormula_Arg(final SUMOFormula formula, final SUMOTerm term, final SUMOArg parse)
	{
		this.formula = formula;
		this.term = term;
		this.parse = parse;
	}

	public static List<SUMOFormula_Arg> make(final SUMOFormula formula) throws IllegalArgumentException, ParseException, IOException
	{
		final List<SUMOFormula_Arg> result = new ArrayList<>();
		final Map<String, SUMOArg> map = SUMOParser.parse(formula.formula);
		for (final Map.Entry<String, SUMOArg> entry : map.entrySet())
		{
			final String key = entry.getKey();
			final SUMOTerm term = SUMOTerm.make(key);
			final SUMOArg parse = entry.getValue();
			result.add(new SUMOFormula_Arg(formula, term, parse));
		}
		return result;
	}

	// A C C E S S

	public SUMOFormula getFormula()
	{
		return formula;
	}

	public SUMOTerm getTerm()
	{
		return term;
	}

	public SUMOArg getParse()
	{
		return parse;
	}

	// I N S E R T
	@Override
	public String dataRow() throws NotFoundException
	{
		return String.format("%s,%s,%s,%s", //
				parse.isArg ? parse.argumentNum : "NULL", // 1
				resolveFormula(formula), // 2
				resolveTerm(term), // 3
				parse.getType() //4
		);
	}

	@Override
	public String comment()
	{
		return term.term;
	}

	// R E S O L V E

	protected int resolve()
	{
		return -1;
	}

	protected int resolveTerm(final SUMOTerm term) throws NotFoundException
	{
		return term.resolve();
	}

	protected int resolveFormula(final SUMOFormula formula) throws NotFoundException
	{
		return formula.resolve();
	}
}
