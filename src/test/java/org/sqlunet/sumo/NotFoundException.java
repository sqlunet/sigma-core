package org.sqlunet.sumo;

public class NotFoundException extends Throwable
{
	final String message;

	public NotFoundException(final String message)
	{
		this.message = message;
	}

	@Override
	public String toString()
	{
		return message;
	}
}
