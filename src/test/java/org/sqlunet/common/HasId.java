/*
 * Copyright (c) 2022.
 * This code is copyright Bernard Bou <1313ou@gmail.com>
 * This software is released under the GNU Public License 3 <http://www.gnu.org/copyleft/gpl.html>.
 */

package org.sqlunet.common;

import org.sigma.core.NotNull;

public interface HasId
{
	@NotNull
	default String getSqlId()
	{
		Integer id = getIntId();
		if (id != null)
		{
			return id.toString();
		}
		return "NULL";
	}

	Integer getIntId();
}
