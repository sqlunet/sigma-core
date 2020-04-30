/* This code is copyright Articulate Software (c) 2003.  Some portions
copyright Teknowledge (c) 2003 and reused under the terms of the GNU license.
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software
and Teknowledge in any writings, briefings, publications, presentations, or 
other representations of any software which incorporates, builds on, or uses this 
code.  Please cite the following article in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment, 
in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico.
*/

package com.articulate.sigma;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * A utility class that defines static methods for working with Sets and other Collections.
 */
public class SetUtil
{
	private SetUtil()
	{
		// This class should not have any instances.
	}

	/**
	 * Removes duplicates from collection based on its natural comparator or equality operator.
	 *
	 * @param collection The collection from which duplicate elements are to be removed.
	 * @param <T>        item type
	 */
	public static <T> void removeDuplicates(Collection<T> collection)
	{
		Set<T> hs = new HashSet<>();
		for (Iterator<T> it = collection.iterator(); it.hasNext(); )
		{
			T obj = it.next();
			if (hs.contains(obj))
			{
				it.remove();
			}
			else
			{
				hs.add(obj);
			}
		}
	}

	/**
	 * Returns true if obj is not a Collection (including if obj ==
	 * null) or if obj is an empty Collection.  Returns false if obj
	 * is a non-empty Collection.
	 *
	 * @param obj The Object to be tested, presumably with the
	 *            expectation that it could be a Collection
	 * @return true or false
	 */
	public static boolean isEmpty(Object obj)
	{
		return (!(obj instanceof Collection) || ((Collection<?>) obj).isEmpty());
	}
}
