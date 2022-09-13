package org.sqlunet.sumo;

public interface HasId
{
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
