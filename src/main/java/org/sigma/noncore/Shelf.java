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

package org.sigma.noncore;

import org.sigma.core.NotNull;
import org.sigma.core.Nullable;
import org.sigma.core.Tuple;

import java.util.ArrayList;
import java.util.List;

/**
 * Class to store var types
 * Array of structures with each containing:
 * first = var name e.g., "?X
 * second = quantToken: 'U' or 'E'
 * third = list of classes var must be an instance of
 * fourth = list of superclasses var must be a subclass of
 */
public class Shelf extends ArrayList<Shelf.Data>
{
	/**
	 * A Quad structure to old variable data
	 * first = var name e.g., "?X
	 * second = quantToken: 'U' or 'E'
	 * third = list of classes var must be an instance of
	 * fourth = list of superclasses var must be a subclass of
	 */
	public static class Data extends Tuple.Quad<String, Character, List<String>, List<String>>
	{
		@NotNull
		@Override
		public String toString()
		{
			return String.format("%s %c classes={%s} superclasses={%s}", first, second, third, fourth);
		}
	}

	public Shelf()
	{
		super();
	}

	public Shelf(@NotNull final Shelf shelf)
	{
		super(shelf);
	}

	/**
	 * Add var data quad
	 *
	 * @param var        variable
	 * @param quantToken quantifier token
	 */
	public void addVarData(@NotNull final String var, @NotNull final Character quantToken)
	{
		@NotNull Data quad = new Data();
		quad.first = var;                // e.g., "?X"
		quad.second = quantToken;        // 'U' or 'E'
		quad.third = new ArrayList<>();  // classes
		quad.fourth = new ArrayList<>(); // superclasses
		add(0, quad);
	}

	/**
	 * Classes var must be an instance of
	 *
	 * @param var variable
	 */
	@Nullable
	private List<String> getClassesForVar(@NotNull final String var)
	{
		@Nullable List<String> result = null;
		for (@NotNull Tuple.Quad<String, Character, List<String>, List<String>> quad : this)
		{
			if (var.equals(quad.first))
			{
				result = quad.third;
				break;
			}
		}
		return result;
	}

	/**
	 * Superclasses var must be a subclass of
	 *
	 * @param var variable
	 */
	@Nullable
	private List<String> getSuperclassesForVar(@NotNull final String var)
	{
		@Nullable List<String> result = null;
		for (@NotNull Tuple.Quad<String, Character, List<String>, List<String>> quad : this)
		{
			if (var.equals(quad.first))
			{
				result = quad.fourth;
				break;
			}
		}
		return result;
	}

	/**
	 * Add class var must be an instance of
	 *
	 * @param var   variable
	 * @param clazz class var must be an instance of
	 */
	public void addClassForVar(@NotNull final String var, @NotNull final String clazz)
	{
		if (!clazz.isEmpty())
		{
			@Nullable List<String> classes = getClassesForVar(var);
			if (classes != null && !classes.contains(clazz))
			{
				classes.add(clazz);
			}
		}
	}

	/**
	 * Add superclass var must be a subclass of
	 *
	 * @param var        variable
	 * @param superclass superclass var must be subclass of
	 */
	public void addSuperclassForVar(@NotNull final String var, @NotNull final String superclass)
	{
		if (!superclass.isEmpty())
		{
			@Nullable List<String> superclasses = getSuperclassesForVar(var);
			if (superclasses != null && !superclasses.contains(superclass))
			{
				superclasses.add(superclass);
			}
		}
	}

	/**
	 * Copy shelf
	 *
	 * @param shelf shelf
	 * @return shelf
	 */
	@NotNull
	public static Shelf makeNewShelf(@NotNull final Shelf shelf)
	{
		return new Shelf(shelf);
	}
}
