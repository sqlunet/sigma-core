/* This code is copyrighted by Articulate Software (c) 2003.
It is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software in any
writings, briefings,publications, presentations, or other representations of any
software which incorporates, builds on, or uses this code.*/

package com.articulate.sigma;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Attribute-value pair
 */
public class AVPair implements Comparable<AVPair>, Serializable
{
	private static final long serialVersionUID = 4130190954326414151L;

	public final Comparator<AVPair> COMPARATOR = Comparator.comparing(AVPair::getAttribute).thenComparing(AVPair::getValue);

	public final String attribute;

	public final String value;

	public AVPair(final String attribute, final String value)
	{
		this.attribute = attribute;
		this.value = value;
	}

	public String getAttribute()
	{
		return attribute;
	}

	public String getValue()
	{
		return value;
	}

	public int compareTo(@NotNull final AVPair that)
	{
		return COMPARATOR.compare(this, that);
	}

	public String toString()
	{
		return "[" + attribute + "," + value + "]";
	}
}
