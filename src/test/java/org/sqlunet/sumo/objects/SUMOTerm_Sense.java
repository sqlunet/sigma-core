package org.sqlunet.sumo.objects;

import org.jetbrains.annotations.NotNull;
import org.sqlunet.sumo.AlreadyFoundException;
import org.sqlunet.sumo.Insertable;
import org.sqlunet.sumo.Utils;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public class SUMOTerm_Sense implements Insertable, Serializable, Comparable<SUMOTerm_Sense>
{
	private static final Comparator<SUMOTerm_Sense> COMPARATOR = Comparator.comparing(SUMOTerm_Sense::getTerm).thenComparing(SUMOTerm_Sense::getSynsetId).thenComparing(SUMOTerm_Sense::getPos).thenComparing(SUMOTerm_Sense::getMapType);

	public static final Set<SUMOTerm_Sense> SET = new TreeSet<>();

	public final long synsetId;

	public final char pos;

	public final SUMOTerm sUMOTerm;

	public final String mapType;

	// C O N S T R U C T

	private SUMOTerm_Sense(final SUMOTerm sUMOTerm, final long synsetId, char pos, final String mapType)
	{
		this.synsetId = synsetId;
		this.pos = pos;
		this.sUMOTerm = sUMOTerm;
		this.mapType = mapType;
	}

	public static SUMOTerm_Sense make(final SUMOTerm sUMOTerm, final long synsetId, char pos, final String mapType) throws AlreadyFoundException
	{
		SUMOTerm_Sense map = new SUMOTerm_Sense(sUMOTerm, synsetId, pos, mapType);
		boolean wasThere = !SET.add(map);
		if (wasThere)
		{
			throw new AlreadyFoundException(map.toString());
		}
		return map;
	}

	public static SUMOTerm_Sense parse(final String term, final String line, final char pos) throws IllegalArgumentException
	{
		// split into fields
		// Each SUMOTerm concept is designated with the prefix '&%'. Note
		// that each concept also has a suffix, '=', ':', '+', '[', ']' or '@', which indicates
		// the precise relationship between the SUMOTerm concept and the WordNet synset.
		// The symbols '=', '+', and '@' mean, respectively, that the WordNet synset
		// is equivalent in meaning to the SUMOTerm concept, is subsumed by the SUMOTerm
		// concept or is an instance of the SUMOTerm concept. ':', '[', and ']' are the
		// complements of those relations. For example, a mapping expressed as
		// ; (%ComplementFn &%Motion)+ now appears as &%Motion[
		// Note also that ']' has not currently been needed.

		final int breakPos = line.indexOf(' ');
		final String offsetField = line.substring(0, breakPos);
		final long synsetId = Long.parseLong(offsetField);
		final SUMOTerm sUMOTerm = SUMOTerm.make(term);
		final String mapType = line.substring(line.length() - 1);
		return SUMOTerm_Sense.make(sUMOTerm, synsetId, pos, mapType);
	}

	// A C C E S S

	public long getSynsetId()
	{
		return synsetId;
	}

	public char getPos()
	{
		return pos;
	}

	public SUMOTerm getTerm()
	{
		return sUMOTerm;
	}

	public String getMapType()
	{
		return mapType;
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
		SUMOTerm_Sense that = (SUMOTerm_Sense) o;
		return synsetId == that.synsetId && pos == that.pos && sUMOTerm.equals(that.sUMOTerm);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(synsetId, pos, sUMOTerm);
	}

	// O R D E R

	@Override
	public int compareTo(@NotNull final SUMOTerm_Sense that)
	{
		return COMPARATOR.compare(this, that);
	}

	// T O S T R I N G

	@Override
	public String toString()
	{
		return this.sUMOTerm + " -> " + this.synsetId + " [" + this.pos + "] (" + mapType + ")";
	}

	// I N S E R T

	@Override
	public String dataRow()
	{
		return String.format("%s,%s,'%s'", //
				Utils.nullableInt(resolveTerm(sUMOTerm)), // 1
				Utils.nullableLong(resolveSynsetId(synsetId)), // 2
				mapType); // 3
	}

	@Override
	public String comment()
	{
		return getTerm().term;
	}

	// R E S O L V E

	private Long resolveSynsetId(final long synsetId)
	{
		return synsetId;
	}

	private Integer resolveTerm(final SUMOTerm term)
	{
		return term.resolve();
	}
}
