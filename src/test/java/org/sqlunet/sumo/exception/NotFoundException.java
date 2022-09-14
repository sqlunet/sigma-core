package org.sqlunet.sumo.exception;

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
