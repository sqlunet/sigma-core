/*
 * Copyright (c) 2022.
 * This code is copyright Bernard Bou <1313ou@gmail.com>
 * This software is released under the GNU Public License 3 <http://www.gnu.org/copyleft/gpl.html>.
 */

package org.sqlunet.sumo.objects;

import org.sigma.core.NotNull;

import org.sigma.core.Nullable;
import org.sqlunet.sumo.*;
import org.sqlunet.common.SetCollector;
import org.sqlunet.common.HasId;
import org.sqlunet.common.Insertable;
import org.sqlunet.common.Resolvable;

import java.io.File;
import java.io.Serializable;
import java.util.Comparator;
import java.util.Date;
import java.util.Objects;

public class SUFile implements HasId, Insertable, Serializable, Comparable<SUFile>, Resolvable<String, Integer>
{
	public static final Comparator<SUFile> COMPARATOR = Comparator.comparing(SUFile::getFilename);

	public static final SetCollector<SUFile> COLLECTOR = new SetCollector<>(COMPARATOR);

	public final String filename;

	public final String fileVersion;

	private final Date fileDate;

	// C O N S T R U C T

	private SUFile(final String filename, final String fileVersion, final Date fileDate)
	{
		this.filename = filename;
		this.fileVersion = fileVersion;
		this.fileDate = fileDate;
	}

	@NotNull
	public static SUFile make(@NotNull final String filepath)
	{
		@NotNull final File file = new File(filepath);
		@NotNull final String filename = file.getName();
		@Nullable final String version = null;
		@Nullable final Date date = null;

		@NotNull final SUFile f = new SUFile(filename, version, date);
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
		@NotNull SUFile sumoFile = (SUFile) o;
		return filename.equals(sumoFile.filename);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(filename);
	}

	// O R D E R

	@Override
	public int compareTo(@NotNull final SUFile that)
	{
		return COMPARATOR.compare(this, that);
	}

	// I N S E R T

	@Override
	public String dataRow()
	{
		return String.format("%d,%s,%s,%s", //
				resolve(), // id 1
				SqlUtils.nullableQuotedEscapedString(filename), // 2
				SqlUtils.nullableQuotedEscapedString(fileVersion), // 3
				SqlUtils.nullableDate(fileDate) // 4
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
