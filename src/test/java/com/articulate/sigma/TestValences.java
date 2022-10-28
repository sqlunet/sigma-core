package com.articulate.sigma;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.articulate.sigma.Utils.OUT;

@ExtendWith({SumoProvider.class})
public class TestValences
{
	@Test
	public void valencesTest()
	{
		//SumoProvider.sumo.buildRelationCaches();
		//SumoProvider.sumo.cacheRelationValences();
		Utils.getRelValences(new String[]{"instance", "subclass", "subset", "element", "parents", "partition", "range", "property", "attribute", "part", "piece", "holds", "PropertyFn", "ListFn", "MemberFn"}, SumoProvider.sumo, Utils.OUT);
	}

	@Test
	public void valencesCache()
	{
		//SumoProvider.sumo.buildRelationCaches();
		//SumoProvider.sumo.cacheRelationValences();
		SumoProvider.sumo.relationValences.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e -> {
			Utils.OUT.println(e.getKey() + " " + Arrays.toString(e.getValue()));
		});
	}
}
