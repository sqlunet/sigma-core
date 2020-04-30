package com.articulate.sigma;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

public class Tuple
{
	interface Listable
	{
		List<?> toList();
	}

	static public class Pair<T1, T2> implements Serializable, Listable
	{
		private static final long serialVersionUID = 6439305385756409745L;

		public T1 first;
		public T2 second;

		@Override public List<?> toList()
		{
			return Arrays.asList(this.first, this.second);
		}
	}

	static public class Triple<T1, T2, T3> implements Serializable, Listable
	{
		private static final long serialVersionUID = -6197049672053644314L;

		public T1 first;
		public T2 second;
		public T3 third;

		@Override public List<?> toList()
		{
			return Arrays.asList(this.first, this.second, this.third);
		}
	}

	static public class Quad<T1, T2, T3, T4> implements Serializable, Listable
	{
		private static final long serialVersionUID = 262086456391789513L;

		public T1 first;
		public T2 second;
		public T3 third;
		public T3 fourth;

		@Override public List<?> toList()
		{
			return Arrays.asList(this.first, this.second, this.third, this.fourth);
		}
	}
}
