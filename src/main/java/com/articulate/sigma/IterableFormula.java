package com.articulate.sigma;

public class IterableFormula
{
	String form;

	public IterableFormula(@NotNull final String form)
	{
		this.form = form;
	}

	public void pop()
	{
		@NotNull String form2 = Lisp.cdr(form);
		if (form2.isEmpty())
		{
			throw new IllegalArgumentException(form2);
		}
		this.form = form2;
	}

	public String car()
	{
		return Lisp.car(form);
	}

	public String cdr()
	{
		return Lisp.cdr(form);
	}

	public boolean listP()
	{
		return Lisp.listP(form);
	}

	public boolean empty()
	{
		return Lisp.empty(form);
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
		return Lisp.getArgument(form, argNum);
	}
}
