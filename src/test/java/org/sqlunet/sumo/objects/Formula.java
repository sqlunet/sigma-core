/*
 * Copyright (c) 2022.
 * This code is copyright Bernard Bou <1313ou@gmail.com>
 * This software is released under the GNU Public License 3 <http://www.gnu.org/copyleft/gpl.html>.
 */

package org.sqlunet.sumo.objects;

import org.sigma.core.NotNull;

import org.sigma.core.Nullable;
import org.sqlunet.sumo.SqlUtils;
import org.sqlunet.common.SetCollector;
import org.sqlunet.common.HasId;
import org.sqlunet.common.Insertable;
import org.sqlunet.common.Resolvable;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Objects;

public class Formula implements HasId, Insertable, Serializable, Comparable<Formula>, Resolvable<String, Integer>
{
	public static final Comparator<Formula> COMPARATOR = Comparator.comparing(Formula::getFormulaText);

	public static final SetCollector<Formula> COLLECTOR = new SetCollector<>(COMPARATOR);

	public final org.sigma.core.Formula formula;

	public final SUFile file;

	// C O N S T R U C T

	private Formula(final org.sigma.core.Formula formula, final SUFile file)
	{
		this.formula = formula;
		this.file = file;
	}

	@NotNull
	public static Formula make(@NotNull final org.sigma.core.Formula formula)
	{
		@NotNull final String filename = formula.getSourceFile();
		@NotNull final Formula f = new Formula(formula, SUFile.make(filename));
		COLLECTOR.add(f);
		return f;
	}

	// A C C E S S

	public org.sigma.core.Formula getFormula()
	{
		return formula;
	}

	@NotNull
	public String getFormulaText()
	{
		return formula.form;
	}

	public String getFile()
	{
		return file.getFilename();
	}

	// I D E N T I T Y

	@Override
	public boolean equals(@Nullable final Object o)
	{
		if (this == o)
		{
			return true;
		}
		if (o == null || getClass() != o.getClass())
		{
			return false;
		}
		@NotNull Formula that = (Formula) o;
		return formula.form.equals(that.formula.form);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(formula);
	}

	// O R D E R

	@Override
	public int compareTo(@NotNull final Formula that)
	{
		return COMPARATOR.compare(this, that);
	}

	// T O S T R I N G

	@NotNull
	@Override
	public String toString()
	{
		return this.formula.form;
	}

	@NotNull
	public String toShortString(final int ellipsizeAfter)
	{
		if (this.formula.form.length() > ellipsizeAfter)
		{
			return this.formula.form.substring(0, ellipsizeAfter) + "...";
		}
		return this.formula.form;
	}

	// I N S E R T

	@Override
	public String dataRow()
	{
		return String.format("%d,%s,%s,%d", //
				resolve(), // id 1
				SqlUtils.quotedEscapedString(toString()), // 2
				SqlUtils.quotedEscapedString(file.filename), // 3
				resolveFile(file) // 4
		);
	}

	// R E S O L V E

	public int resolve()
	{
		return getIntId();
	}

	protected int resolveFile(@NotNull final SUFile file)
	{
		return file.resolve();
	}

	@Override
	public Integer getIntId()
	{
		return COLLECTOR.get(this);
	}

	@NotNull
	@Override
	public String resolving()
	{
		return formula.form;
	}
}
