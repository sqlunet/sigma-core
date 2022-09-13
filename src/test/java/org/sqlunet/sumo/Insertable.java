package org.sqlunet.sumo;

public interface Insertable
{
	String dataRow() throws NotFoundException;

	default String comment()
	{
		return null;
	}
}
