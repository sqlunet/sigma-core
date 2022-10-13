package com.articulate.sigma.noncore;

import com.articulate.sigma.NotNull;
import com.articulate.sigma.Nullable;
import com.articulate.sigma.Tuple;

import java.util.ArrayList;
import java.util.List;

public class Shelf
{
	/**
	 * Add var data quad
	 */
	public static void addVarDataQuad(@NotNull final String var, @NotNull final String quantToken, @NotNull final List<Tuple.Quad<String, String, List<String>, List<String>>> shelf)
	{
		@NotNull Tuple.Quad<String, String, List<String>, List<String>> quad = new Tuple.Quad<>();
		quad.first = var;                // e.g., "?X"
		quad.second = quantToken;        // "U" or "E"
		quad.third = new ArrayList<>();  // ios
		quad.fourth = new ArrayList<>(); // scs
		shelf.add(0, quad);
	}

	/**
	 * Ios
	 */
	@Nullable
	private static List<String> getIosForVar(@NotNull final String var, @NotNull final List<Tuple.Quad<String, String, List<String>, List<String>>> shelf)
	{
		@Nullable List<String> result = null;
		for (@NotNull Tuple.Quad<String, String, List<String>, List<String>> quad : shelf)
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
	 * Scs
	 */
	@Nullable
	private static List<String> getScsForVar(@NotNull final String var, @NotNull final List<Tuple.Quad<String, String, List<String>, List<String>>> shelf)
	{
		@Nullable List<String> result = null;
		for (@NotNull Tuple.Quad<String, String, List<String>, List<String>> quad : shelf)
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
	 * Add Io
	 */
	public static void addIoForVar(@NotNull final String var, @NotNull final String io, @NotNull final List<Tuple.Quad<String, String, List<String>, List<String>>> shelf)
	{
		if (!io.isEmpty())
		{
			@Nullable List<String> ios = getIosForVar(var, shelf);
			if ((ios != null) && !ios.contains(io))
			{
				ios.add(io);
			}
		}
	}

	/**
	 * Add Sc
	 */
	public static void addScForVar(@NotNull final String var, @NotNull final String sc, @NotNull final List<Tuple.Quad<String, String, List<String>, List<String>>> shelf)
	{
		if (!sc.isEmpty())
		{
			@Nullable List<String> scs = getScsForVar(var, shelf);
			if ((scs != null) && !scs.contains(sc))
			{
				scs.add(sc);
			}
		}
	}

	/**
	 * Copy shelf
	 */
	@NotNull
	public static List<Tuple.Quad<String, String, List<String>, List<String>>> makeNewShelf(@NotNull final List<Tuple.Quad<String, String, List<String>, List<String>>> shelf)
	{
		return new ArrayList<>(shelf);
	}
}
