package org.sqlunet.sumo.objects;

import com.articulate.sigma.Formula;

import org.jetbrains.annotations.NotNull;
import org.sqlunet.sumo.*;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Objects;

public class SUMOFormula implements HasId, Insertable, Serializable, Comparable<SUMOFormula>, Resolvable<String, Integer>
{
	public static final Comparator<SUMOFormula> COMPARATOR = Comparator.comparing(SUMOFormula::getFormulaText);

	public static final SetCollector<SUMOFormula> COLLECTOR = new SetCollector<>(COMPARATOR);

	public final Formula formula;

	public final SUMOFile file;

	// C O N S T R U C T

	private SUMOFormula(final Formula formula, final SUMOFile file)
	{
		this.formula = formula;
		this.file = file;
	}

	public static SUMOFormula make(final Formula formula)
	{
		final String filename = formula.getSourceFile();
		final SUMOFormula f = new SUMOFormula(formula, SUMOFile.make(filename, null, null));
		COLLECTOR.add(f);
		return f;
	}

	// A C C E S S

	public Formula getFormula()
	{
		return formula;
	}

	public String getFormulaText()
	{
		return formula.text;
	}

	public String getFile()
	{
		return file.getFilename();
	}

	// I D E N T I T Y

	@Override
	public boolean equals(final Object o)
	{
		if (this == o)
		{
			return true;
		}
		if (o == null || getClass() != o.getClass())
		{
			return false;
		}
		SUMOFormula that = (SUMOFormula) o;
		return formula.text.equals(that.formula.text);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(formula);
	}

	// O R D E R

	@Override
	public int compareTo(@NotNull final SUMOFormula that)
	{
		return COMPARATOR.compare(this, that);
	}

	// T O S T R I N G

	@Override
	public String toString()
	{
		return this.formula.text;
	}

	// I N S E R T

	@Override
	public String dataRow()
	{
		return String.format("%d,%s,%s,%d", //
				resolve(), // id 1
				Utils.quotedEscapedString(toString()), // 2
				Utils.quotedEscapedString(file), // 3
				resolveFile(file) // 4
		);
	}

	// R E S O L V E

	public int resolve()
	{
		return getIntId();
	}

	protected int resolveFile(final SUMOFile file)
	{
		//return file.resolve();
		return -1;
	}

	@Override
	public Integer getIntId()
	{
		return COLLECTOR.get(this);
	}

	@Override
	public String resolving()
	{
		return formula.text;
	}
}
