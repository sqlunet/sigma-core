/* This code is copyrighted by Articulate Software (c) 2003.
It is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software in any
writings, briefings,publications, presentations, or other representations of any
software which incorporates, builds on, or uses this code.*/

package com.articulate.sigma;

import java.io.Serializable;

public class AVPair implements Comparable<AVPair>, Serializable
{
	private static final long serialVersionUID = 4130190954326414151L;

	public String attribute = "";  // this is the sort field for comparison

	public String value = "";

	public int compareTo(AVPair avp) throws ClassCastException
	{
		return attribute.compareTo(avp.attribute);
	}

	public String toString()
	{
		return "[" + attribute + "," + value + "]";
	}
}
