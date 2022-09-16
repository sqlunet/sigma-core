package com.articulate.sigma;

public class MutableFormula extends Formula
{
	public MutableFormula()
	{
	}

	public MutableFormula(@NotNull final String form)
	{
		super(form);
	}

	public void pop()
	{
		form = cdr();
	}

	public void set2(@NotNull final String form)
	{
		this.form = form.intern();
	}
}
