package org.sqlunet.sumo.objects;

import com.articulate.sigma.Formula;

import org.sqlunet.sumo.NotFoundException;
import org.sqlunet.sumo.SUMOKb;
import org.sqlunet.sumo.Utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SUMOTermAttr
{
	public final Character attr;

	private static final Character ISFUNCTION = 'y';

	private static final Character ISMATHFUNCTION = 'm';

	private static final Character ISCOMPARISONOP = '~';

	private static final Character ISLOGICALOP = 'l';

	private static final Character ISQUANTIFIER = 'q';

	private static final Character SUBCLASSOFRELATION = 'R';

	private static final Character SUBCLASSOFFUNCTION = 'F';

	private static final Character SUBCLASSOFPREDICATE = 'P';

	private static final Character SUBCLASSOFATTRIBUTE = 'A';

	private static final Character CHILDOFRELATION = 'r';

	private static final Character CHILDOFFUNCTION = 'f';

	private static final Character CHILDOFPREDICATE = 'p';

	private static final Character CHILDOFATTRIBUTE = 'a';

	// C O N S T R U C T

	private SUMOTermAttr(final Character attr)
	{
		this.attr = attr;
	}

	public static Collection<SUMOTermAttr> make(final SUMOKb kb, final String term) throws NotFoundException
	{
		final List<SUMOTermAttr> result = new ArrayList<>();

		if (Formula.isFunction(term))
		{
			result.add(new SUMOTermAttr(SUMOTermAttr.ISFUNCTION));
		}
		if (Formula.isMathFunction(term))
		{
			result.add(new SUMOTermAttr(SUMOTermAttr.ISMATHFUNCTION));
		}
		if (Formula.isComparisonOperator(term))
		{
			result.add(new SUMOTermAttr(SUMOTermAttr.ISCOMPARISONOP));
		}
		if (Formula.isLogicalOperator(term))
		{
			result.add(new SUMOTermAttr(SUMOTermAttr.ISLOGICALOP));
		}
		if (Formula.isQuantifier(term))
		{
			result.add(new SUMOTermAttr(SUMOTermAttr.ISQUANTIFIER));
		}

		if (kb.childOf(term, "Relation"))
		{
			result.add(new SUMOTermAttr(SUMOTermAttr.CHILDOFRELATION));
		}
		if (kb.childOf(term, "Predicate"))
		{
			result.add(new SUMOTermAttr(SUMOTermAttr.CHILDOFPREDICATE));
		}
		if (kb.childOf(term, "Function"))
		{
			result.add(new SUMOTermAttr(SUMOTermAttr.CHILDOFFUNCTION));
		}
		if (kb.childOf(term, "Attribute"))
		{
			result.add(new SUMOTermAttr(SUMOTermAttr.CHILDOFATTRIBUTE));
		}

		if (kb.isSubclass(term, "Relation"))
		{
			result.add(new SUMOTermAttr(SUMOTermAttr.SUBCLASSOFRELATION));
		}
		if (kb.isSubclass(term, "Predicate"))
		{
			result.add(new SUMOTermAttr(SUMOTermAttr.SUBCLASSOFPREDICATE));
		}
		if (kb.isSubclass(term, "Function"))
		{
			result.add(new SUMOTermAttr(SUMOTermAttr.SUBCLASSOFFUNCTION));
		}
		if (kb.isSubclass(term, "Attribute"))
		{
			result.add(new SUMOTermAttr(SUMOTermAttr.SUBCLASSOFATTRIBUTE));
		}

		if (result.isEmpty())
			throw new NotFoundException(term);

		return result;
	}

	// A C C E S S

	public Character getAttr()
	{
		return attr;
	}

	// T O S T R I N G

	@Override
	public String toString()
	{
		return this.attr.toString();
	}

	// I N S E R T

	public String dataRow()
	{
		return String.format("%d,%s", //
				resolve(), // 1 id
				Utils.nullableQuotedEscapedString(this.attr.toString())); // 2
	}

	// R E S O L V E

	protected int resolve()
	{
		return -1;
	}
}
