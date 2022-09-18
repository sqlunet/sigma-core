package com.articulate.sigma;

public class MutableFormula extends Formula
{
	public MutableFormula(@NotNull final String form)
	{
		super(form);
	}

	public void pop()
	{
		form = cdr();
	}
}
