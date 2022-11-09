/*
 * Copyright (c) 2022.
 * This code is copyright Bernard Bou <1313ou@gmail.com>
 * This software is released under the GNU Public License 3 <http://www.gnu.org/copyleft/gpl.html>.
 */

package org.sqlunet.common;

import org.sigma.core.NotNull;
import org.sigma.core.Nullable;

import java.util.function.Function;

public interface Resolvable<T, R> extends Insertable
{
	@Nullable
	default R resolve(@NotNull final Function<T, R> resolver)
	{
		T resolving = resolving();
		if (resolving == null)
		{
			return null;
		}
		return resolver.apply(resolving);
	}

	T resolving();
}
