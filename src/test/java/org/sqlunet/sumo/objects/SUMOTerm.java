package org.sqlunet.sumo.objects;

import org.jetbrains.annotations.NotNull;
import org.sqlunet.sumo.*;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Objects;

public class SUMOTerm implements HasId, Insertable, Serializable, Comparable<SUMOTerm>, Resolvable<String, Integer>
{
	public static final Comparator<SUMOTerm> COMPARATOR = Comparator.comparing(SUMOTerm::getTerm);

	public static final SetCollector<SUMOTerm> COLLECTOR = new SetCollector<>(COMPARATOR);

	protected static final String[] wellKnownTerms = new String[]{"subclass", "subrelation", "instance", "disjoint", //
			"domain", "partition", //
			"attribute", "property", //
			"subAttribute", "subProcess", //
			"equal", "inverse", //
			"=>", "<=>", //
			"contains", "element", "subset", "component", "part", "piece", //
			"format", "documentation", //
			"Relation", "Predicate", "Function", "Class"};

	public final String term;

	// C O N S T R U C T

	private SUMOTerm(final String term)
	{
		this.term = term;
	}

	public static SUMOTerm make(final String term)
	{
		final SUMOTerm t =new SUMOTerm(term);
		COLLECTOR.add(t);
		return t;
	}

	public static String parse(final String line) throws IllegalArgumentException, StringIndexOutOfBoundsException
	{
		// split into fields
		// Each SUMO concept is designated with the prefix '&%'. Note
		// that each concept also has a suffix, '=', ':', '+', '[', ']' or '@', which indicates
		// the precise relationship between the SUMOTerm concept and the WordNet synset.
		// The symbols '=', '+', and '@' mean, respectively, that the WordNet synset
		// is equivalent in meaning to the SUMOTerm concept, is subsumed by the SUMOTerm
		// concept or is an instance of the SUMOTerm concept. ':', '[', and ']' are the
		// complements of those relations. For example, a mapping expressed as
		// ; (%ComplementFn &%Motion)+ now appears as &%Motion[
		// Note also that ']' has not currently been needed.

		int breakPos = line.lastIndexOf("&%");
		if (breakPos == -1)
		{
			throw new IllegalArgumentException(line);
		}
		try
		{
			final int position = breakPos + 2;
			return line.substring(position, line.length() - 1);
		}
		catch (Exception e)
		{
			System.err.println(line);
			throw new IllegalArgumentException(line);
		}
	}

	// A C C E S S

	public String getTerm()
	{
		return term;
	}

	// I D E N T I T Y

	@Override
	public boolean equals(final Object o)
	{
		if (this == o)
		{
			return true;
		}
		if (o == null || getClass() != o.getClass())
		{
			return false;
		}
		SUMOTerm sumoTerm = (SUMOTerm) o;
		return term.equals(sumoTerm.term);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(term);
	}

	// O R D E R

	@Override
	public int compareTo(@NotNull final SUMOTerm that)
	{
		return COMPARATOR.compare(this, that);
	}

	// T O S T R I N G

	@Override
	public String toString()
	{
		return this.term;
	}

	// I N S E R T

	@Override
	public String dataRow()
	{
		return String.format("%d,%s", //
				resolve(), // 1 id
				Utils.quotedEscapedString(term)); // 2
	}

	// R E S O L V E

	public int resolve()
	{
		return getIntId();
	}

	@Override
	public Integer getIntId()
	{
		return COLLECTOR.get(this);
	}

	@Override
	public String resolving()
	{
		return term;
	}
}
