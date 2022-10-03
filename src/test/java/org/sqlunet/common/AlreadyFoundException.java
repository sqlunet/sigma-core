package org.sqlunet.common;

public class AlreadyFoundException extends RuntimeException
{
	public AlreadyFoundException(final String message)
	{
		super(message);
	}
}
