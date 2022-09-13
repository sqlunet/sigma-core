package org.sqlunet.sumo;

public interface Insertable
{
	String dataRow();

	default String comment()
	{
		return null;
	}
}
