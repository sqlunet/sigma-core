package org.sqlunet.sumo.iface;

import org.sqlunet.sumo.exception.NotFoundException;

public interface Insertable
{
	String dataRow() throws NotFoundException;

	default String comment()
	{
		return null;
	}
}
