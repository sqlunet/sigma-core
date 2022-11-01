/*
 * Copyright (c) 2022.
 * This code is copyright Articulate Software (c) 2003.  Some portions copyright Teknowledge (c) 2003
 * and reused under the terms of the GNU license.
 * Significant portions of the code have been revised, trimmed, revamped,enhanced by
 * Bernard Bou <1313ou@gmail.com>
 * This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
 * Users of this code also consent, by use of this code, to credit Articulate Software and Teknowledge
 * in any writings, briefings, publications, presentations, or other representations of any software
 * which incorporates, builds on, or uses this code.
 */

package org.sigma.core;

/**
 * Iterable formula.
 * Offers a Lisp-like interface.
 * pop() places the iteration in the next stage by popping the head
 */
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

	/**
	 * Pops head in list
	 *
	 * @throws IllegalStateException illegal state exception if invoked when the form is empty
	 */
	public void pop()
	{
		@NotNull String form2 = Lisp.cdr(form);
		if (form2.isEmpty())
		{
			throw new IllegalStateException(form2);
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
	 * Return the numbered argument of the given formula
	 * (in the state it is in).
	 * The first element is number 0.
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
