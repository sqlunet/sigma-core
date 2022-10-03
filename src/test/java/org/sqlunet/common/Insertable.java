package org.sqlunet.common;

public interface Insertable
{
	String dataRow() throws NotFoundException;

	default String comment()
	{
		return null;
	}
}
