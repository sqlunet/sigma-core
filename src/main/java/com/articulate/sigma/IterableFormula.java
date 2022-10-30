package com.articulate.sigma;

public class IterableFormula
{
	private String form;

	public IterableFormula(@NotNull final String form)
	{
		this.form = form;
	}

	public String getForm()
	{
		return form;
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

	@NotNull
	public String car()
	{
		return Lisp.car(form);
	}

	@NotNull
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
	 * @param argPos argument position
	 * @return argument at argPos.
	 */
	@NotNull
	public String getArgument(int argPos)
	{
		return Lisp.getArgument(form, argPos);
	}
}
