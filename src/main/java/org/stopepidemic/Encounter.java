package org.stopepidemic;

import com.gluonhq.attach.ble.Proximity;

import java.util.HashMap;
import java.util.Map;

public class Encounter {
    private static final long NANOS_IN_SEC = 1000000000;
    private static final long EXPIRE_PERIOD_NANOS = 3600 * NANOS_IN_SEC;
    private static final int SIGNAL_AVAILABLE_MARKER = -1;
    private static final long SIGNAL_LOST_TIME = 60_000_000_000L;

    private final byte[] encounterID;
    private Map<Integer, Long> signalDuration;
    private Map<Proximity, Long> proximityDuration;
    private long lastEncounter;
    private Proximity lastProximity = Proximity.UNKNOWN;
    private int lastSignalStrength;
    private long timeSignalLost = -1;

    public Encounter(byte[] encounterID) {
        this.encounterID = encounterID;
        this.signalDuration = new HashMap<>();
        this.proximityDuration = new HashMap<>();
        for (Proximity proximity : Proximity.values()) {
            proximityDuration.put(proximity, 0L);
        }
    }

    /**
     * Marks the encounter as seen: signal strength and proximity distributions are updated with the duration from the last time seen.
     *
     * @param signalStrength current signal strength
     * @param proximity      current proximity
     */
    public void seen(int signalStrength, Proximity proximity) {
        long currentTime = System.nanoTime();
        if (lastEncounter != 0) {
            add(lastSignalStrength, lastProximity, currentTime - lastEncounter);
        }
        lastEncounter = currentTime;
        lastProximity = proximity;
        lastSignalStrength = signalStrength;
        timeSignalLost = SIGNAL_AVAILABLE_MARKER;
    }

    /**
     * Check whether signal has not been seen for more than {@link SIGNAL_LOST_TIME}.
     * <p>
     * When loss is detected, all distributions are updated with the last values and duration.
     *
     * @return whether signal is considered lost
     */
    public boolean checkSignalLost() {
        long currentTime = System.nanoTime();
        if (currentTime - lastSignalStrength > SIGNAL_LOST_TIME) {
            if (timeSignalLost != SIGNAL_AVAILABLE_MARKER) {
                add(lastSignalStrength, lastProximity, currentTime - lastEncounter);
                lastEncounter = 0;
                lastProximity = Proximity.UNKNOWN;
                lastSignalStrength = 0;
                timeSignalLost = currentTime;
            }
            return true;
        }
        return false;
    }

    /**
     * Check whether the encounter has expired: the signal has been lost for {@link EXPIRE_PERIOD_NANOS}.
     */
    public boolean isExpired() {
        return timeSignalLost > 0 && (System.nanoTime() - timeSignalLost) > EXPIRE_PERIOD_NANOS;
    }

    public boolean hasSignal() {
        return timeSignalLost == SIGNAL_AVAILABLE_MARKER;
    }

    private void add(int signalStrength, Proximity proximity, long duration) {
        long lastValue = signalDuration.getOrDefault(signalStrength, 0L);
        signalDuration.put(signalStrength, lastValue + duration);

        lastValue = proximityDuration.get(proximity);
        proximityDuration.put(proximity, lastValue + duration);
    }

    public byte[] getEncounterID() {
        return encounterID;
    }

    public long getLastEncounter() {
        return lastEncounter;
    }

    public Proximity getLastProximity() {
        return lastProximity;
    }

    @Override
    public String toString() {
        return byteArrayToHex(encounterID) + " -> I: " + proximityDuration.get(Proximity.IMMEDIATE) / NANOS_IN_SEC + "s " +
                "N: " + proximityDuration.get(Proximity.NEAR) / NANOS_IN_SEC + "s " +
                "F: " + proximityDuration.get(Proximity.FAR) / NANOS_IN_SEC + "s";
    }

    public static String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for(byte b: a)
            sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
