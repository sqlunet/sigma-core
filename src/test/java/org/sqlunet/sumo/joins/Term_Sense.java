/*
 * Copyright (c) 2022.
 * This code is copyright Bernard Bou <1313ou@gmail.com>
 * This software is released under the GNU Public License 3 <http://www.gnu.org/copyleft/gpl.html>.
 */

package org.sqlunet.sumo.joins;

import org.sigma.core.NotNull;

import org.sigma.core.Nullable;
import org.sqlunet.common.AlreadyFoundException;
import org.sqlunet.common.Insertable;
import org.sqlunet.sumo.SqlUtils;
import org.sqlunet.sumo.objects.Term;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public class Term_Sense implements Insertable, Serializable, Comparable<Term_Sense>
{
	private static final Comparator<Term_Sense> COMPARATOR = Comparator.comparing(Term_Sense::getTerm).thenComparing(Term_Sense::getSynsetId).thenComparing(Term_Sense::getPos).thenComparing(Term_Sense::getMapType);

	public static final Set<Term_Sense> SET = new TreeSet<>();

	public final long synsetId;

	public final char pos;

	public final Term term;

	public final String mapType;

	// C O N S T R U C T

	private Term_Sense(final Term term, final long synsetId, char pos, final String mapType)
	{
		this.synsetId = synsetId;
		this.pos = pos;
		this.term = term;
		this.mapType = mapType;
	}

	@NotNull
	public static Term_Sense make(final Term term, final long synsetId, char pos, final String mapType) throws AlreadyFoundException
	{
		@NotNull Term_Sense map = new Term_Sense(term, synsetId, pos, mapType);
		boolean wasThere = !SET.add(map);
		if (wasThere)
		{
			throw new AlreadyFoundException(map.toString());
		}
		return map;
	}

	@NotNull
	public static Term_Sense parse(final String termstr, @NotNull final String line, final char pos) throws IllegalArgumentException
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
		@NotNull final String offsetField = line.substring(0, breakPos);
		final long synsetId = Long.parseLong(offsetField);
		@NotNull final Term term = Term.make(termstr);
		@NotNull final String mapType = line.substring(line.length() - 1);
		return Term_Sense.make(term, synsetId, pos, mapType);
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

	public Term getTerm()
	{
		return term;
	}

	public String getMapType()
	{
		return mapType;
	}

	// I D E N T I T Y

	@Override
	public boolean equals(@Nullable final Object o)
	{
		if (this == o)
		{
			return true;
		}
		if (o == null || getClass() != o.getClass())
		{
			return false;
		}
		@NotNull Term_Sense that = (Term_Sense) o;
		return synsetId == that.synsetId && pos == that.pos && term.equals(that.term);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(synsetId, pos, term);
	}

	// O R D E R

	@Override
	public int compareTo(@NotNull final Term_Sense that)
	{
		return COMPARATOR.compare(this, that);
	}

	// T O S T R I N G

	@NotNull
	@Override
	public String toString()
	{
		return this.term + " -> " + this.synsetId + " [" + this.pos + "] (" + mapType + ")";
	}

	// I N S E R T

	@Override
	public String dataRow()
	{
		return String.format("%s,%s,'%s'", //
				SqlUtils.nullableInt(resolveTerm(term)), // 1
				SqlUtils.nullableLong(resolveSynsetId(synsetId)), // 2
				mapType); // 3
	}

	@Override
	public String comment()
	{
		return getTerm().term;
	}

	// R E S O L V E

	@NotNull
	private Long resolveSynsetId(final long synsetId)
	{
		return synsetId;
	}

	@NotNull
	private Integer resolveTerm(@NotNull final Term term)
	{
		return term.resolve();
	}
}
