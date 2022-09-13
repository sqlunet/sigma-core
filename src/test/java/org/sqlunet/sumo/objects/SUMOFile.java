package org.sqlunet.sumo.objects;

import org.jetbrains.annotations.NotNull;
import org.sqlunet.sumo.*;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Date;
import java.util.Objects;

public class SUMOFile implements HasId, Insertable, Serializable, Comparable<SUMOFile>, Resolvable<String, Integer>
{
	public static final Comparator<SUMOFile> COMPARATOR = Comparator.comparing(SUMOFile::getFilename);

	public static final SetCollector<SUMOFile> COLLECTOR = new SetCollector<>(COMPARATOR);

	public final String filename;

	public final String fileVersion;

	private final Date fileDate;

	// C O N S T R U C T

	private SUMOFile(final String filename, final String fileVersion, final Date fileDate)
	{
		this.filename = filename;
		this.fileVersion = fileVersion;
		this.fileDate = fileDate;
	}

	public static SUMOFile make(final String filename, final String fileVersion, final Date fileDate)
	{
		final SUMOFile f =  new SUMOFile(filename, fileVersion, fileDate);
		COLLECTOR.add(f);
		return f;
	}

	// A C C E S S

	public String getFilename()
	{
		return filename;
	}

	public String getFileVersion()
	{
		return fileVersion;
	}

	public Date getFileDate()
	{
		return fileDate;
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
		SUMOFile sumoFile = (SUMOFile) o;
		return filename.equals(sumoFile.filename);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(filename);
	}

	// O R D E R

	@Override
	public int compareTo(@NotNull final SUMOFile that)
	{
		return COMPARATOR.compare(this, that);
	}

	// I N S E R T

	@Override
	public String dataRow()
	{
		return String.format("%d,%s,%s,%s", //
				resolve(), // id 1
				Utils.nullableQuotedEscapedString(filename), // 2
				Utils.nullableQuotedEscapedString(fileVersion), // 3
				Utils.nullableDate(fileDate) // 4
		);
	}

	// R E S O L V E

	protected int resolve()
	{
		return getIntId();
	}

	//@RequiresIdFrom(type = SUMOFile.class)
	@Override
	public Integer getIntId()
	{
		return COLLECTOR.get(this);
	}

	@Override
	public String resolving()
	{
		return filename;
	}
}
