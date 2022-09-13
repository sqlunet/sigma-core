package org.sqlunet.sumo.objects;

import org.sqlunet.sumo.Utils;

public class SUMOTerm_Sense
{
	public final long synsetId;

	public final char pos;

	public final SUMOTerm sUMOTerm;

	public final String mapType;

	// C O N S T R U C T

	private SUMOTerm_Sense(final long synsetId, char pos, final SUMOTerm sUMOTerm, final String mapType)
	{
		this.synsetId = synsetId;
		this.pos = pos;
		this.sUMOTerm = sUMOTerm;
		this.mapType = mapType;
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
		return new SUMOTerm_Sense(synsetId, pos, sUMOTerm, mapType);
	}

	// T O S T R I N G

	@Override
	public String toString()
	{
		return this.synsetId + " - " + this.mapType + " - " + this.sUMOTerm;
	}

	// I N S E R T

	public String dataRow()
	{
		return String.format("%d,%s,%s,'%s'", //
				resolve(), // 1 id
				Utils.nullableInt(resolveSynsetId(synsetId)), // 2
				Utils.nullableInt(resolveSumoId(sUMOTerm)), // 3
				mapType); // 4
	}

	// R E S O L V E

	protected int resolve()
	{
		return -1;
	}

	private Integer resolveSynsetId(final long synsetId)
	{
		return -1;
	}

	private Integer resolveSumoId(final SUMOTerm sUMOTerm)
	{
		return -1;
	}
}
