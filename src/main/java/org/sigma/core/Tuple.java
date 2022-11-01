/*
 * Copyright (c) 2022.
 * This code is copyright Articulate Software (c) 2003.  Some portions copyright Teknowledge (c) 2003
 * and reused under the terms of the GNU license.
 * Significant portions of the code have been revised, trimmed, revamped,enhanced by
 * Bernard Bou <1313ou@gmail.com>
 * This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
 * Users of this code also consent, by use of this code, to credit Articulate Software and Teknowledge
 * in any writings, briefings, publications, presentations, or other representations of any software
 * which incorporates, builds on, or uses this code.
 */

package org.sigma.core;

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

		@NotNull
		@Override
		public List<?> toList()
		{
			return Arrays.asList(this.first, this.second);
		}

		@NotNull
		@Override
		public String toString()
		{
			return String.format("[1] %s [2] %s", this.first, this.second);
		}
	}

	static public class Triple<T1, T2, T3> implements Serializable, Listable
	{
		private static final long serialVersionUID = -6197049672053644314L;

		@Nullable
		public T1 first;
		public T2 second;
		public T3 third;

		@NotNull
		@Override
		public List<?> toList()
		{
			return Arrays.asList(this.first, this.second, this.third);
		}

		@NotNull
		@Override
		public String toString()
		{
			return String.format("[1] %s [2] %s [3] %s", this.first, this.second, this.third);
		}
	}

	static public class Quad<T1, T2, T3, T4> implements Serializable, Listable
	{
		private static final long serialVersionUID = 262086456391789513L;

		public T1 first;
		public T2 second;
		public T3 third;
		public T3 fourth;

		@NotNull
		@Override
		public List<?> toList()
		{
			return Arrays.asList(this.first, this.second, this.third, this.fourth);
		}

		@NotNull
		@Override
		public String toString()
		{
			return String.format("[1] %s [2] %s [3] %s [4] %s", this.first, this.second, this.third, this.fourth);
		}
	}
}
