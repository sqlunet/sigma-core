/*
 * Copyright (c) 2022.
 * This code is copyright Bernard Bou <1313ou@gmail.com>
 * This software is released under the GNU Public License 3 <http://www.gnu.org/copyleft/gpl.html>.
 */

package org.sqlunet.common;

public interface Insertable
{
	String dataRow() throws NotFoundException;

	default String comment()
	{
		return null;
	}
}
