package com.articulate.sigma;

import javax.swing.*;

public class IterableFormula
{
	String form;

	public IterableFormula(@NotNull final String form)
	{
		this.form = form;
	}

	public void pop()
	{
		@NotNull String form2 = Formula.cdr(form);
		if (form2.isEmpty())
		{
			throw new IllegalArgumentException(form2);
		}
		this.form = form2;
	}

	public String car()
	{
		return Formula.car(form);
	}

	public String cdr()
	{
		return Formula.cdr(form);
	}

	public boolean listP()
	{
		return Formula.listP(form);
	}

	public boolean empty()
	{
		return Formula.empty(form);
	}

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
		return Formula.getArgument(form, argNum);
	}
}
