package com.jtstegeman.cs4518team5_3;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by zoraver on 2/6/18.
 */

public class EntryCounter {
    private Map<String, Integer> entryCounts;

    private EntryCounter() {
        entryCounts = new HashMap<>();
    }

    public int getEntryCount(String zoneId) {
        Integer i = entryCounts.get(zoneId);
        if (i == null)
            return 0;
        return i;
    }

    public void put(String zoneId, int entryCount) {
        entryCounts.put(zoneId, entryCount);
    }

    public static EntryCounter getInstance() {
        return GeoCounterHolder.instance;
    }

    private static class GeoCounterHolder {
        private static EntryCounter instance = new EntryCounter();
    }
}
