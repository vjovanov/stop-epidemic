package org.stoppandemic;

import com.gluonhq.attach.ble.Proximity;

import java.util.HashMap;
import java.util.Map;

public class Encounter {
    private final String encounterID;
    private Map<Integer, Long> signalDuration;
    private int lastSignalStrength = Integer.MIN_VALUE;
    private long lastEncounterDuration;
    private Proximity lastProximity;

    public Encounter(String encounterID) {
        this.encounterID = encounterID;
        this.signalDuration = new HashMap<>();
    }

    public void seen(int signalStrength, Proximity proximity) {
        long currentTime = System.nanoTime();
        add(signalStrength, currentTime - lastEncounterDuration);
        lastEncounterDuration = currentTime;
        lastSignalStrength = signalStrength;
        lastProximity = proximity;
    }

    public void leftRange() {
        if (lastSignalStrength != Integer.MIN_VALUE) {
            add(lastSignalStrength, lastEncounterDuration);
        }

        lastSignalStrength = Integer.MIN_VALUE;
    }

    private void add(int signalStrength, long duration) {
        long lastValue = this.signalDuration.getOrDefault(signalStrength, 0L);
        this.signalDuration.put(signalStrength, lastValue + duration);
    }

    public String getEncounterID() {
        return encounterID;
    }
}
