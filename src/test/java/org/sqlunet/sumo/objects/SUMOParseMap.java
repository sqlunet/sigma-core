package org.sqlunet.sumo.objects;

import org.sqlunet.sumo.NotFoundException;
import org.sqlunet.sumo.SUMOParser;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SUMOParseMap
{
	private final SUMOFormula formula;

	private final SUMOTerm term;

	private final SUMOParse parse;

	// C O N S T R U C T

	private SUMOParseMap(final SUMOFormula formula, final SUMOTerm term, final SUMOParse parse)
	{
		this.formula = formula;
		this.term = term;
		this.parse = parse;
	}

	public static List<SUMOParseMap> make(final SUMOFormula formula) throws IllegalArgumentException, ParseException, IOException
	{
		final List<SUMOParseMap> result = new ArrayList<>();
		final Map<String, SUMOParse> map = SUMOParser.parse(formula.formula);
		for (final Map.Entry<String, SUMOParse> entry : map.entrySet())
		{
			final String key = entry.getKey();
			final SUMOTerm term = SUMOTerm.make(key);
			final SUMOParse parse = entry.getValue();
			result.add(new SUMOParseMap(formula, term, parse));
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

	public SUMOParse getParse()
	{
		return parse;
	}

	// I N S E R T

	public String dataRow() throws NotFoundException
	{
		return String.format("%d,%s,%s,%s,%s", //
				resolve(), // 1 id
				resolveFormula(formula), // 2
				resolveTerm(term), // 3
				parse.getType(), //4
				parse.isArg ? parse.argumentNum : "NULL" // 5
		);
	}

	// R E S O L V E

	protected int resolve()
	{
		return -1;
	}

	protected int resolveTerm(final SUMOTerm term) throws NotFoundException
	{
		return -1;
	}

	protected int resolveFormula(final SUMOFormula formula) throws NotFoundException
	{
		return -1;
	}
}
