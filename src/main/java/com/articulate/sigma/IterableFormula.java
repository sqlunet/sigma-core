package com.articulate.sigma;

public class IterableFormula extends Formula
{
	public IterableFormula(@NotNull final String form)
	{
		super(form);
	}

	public void pop()
	{
		String form = cdr();
		if (form.isEmpty())
		{
			throw new IllegalArgumentException(form);
		}
		this.form = form;
	}
}
