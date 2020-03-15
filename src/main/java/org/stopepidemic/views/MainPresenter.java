/*
 * Copyright (c) 2016, 2020, Gluon
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *     * Neither the name of Gluon, any associated website, nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL GLUON BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.stopepidemic.views;

import com.gluonhq.attach.ble.BleService;
import com.gluonhq.attach.ble.Configuration;
import com.gluonhq.attach.ble.Proximity;
import com.gluonhq.attach.ble.ScanDetection;
import com.gluonhq.charm.glisten.afterburner.GluonPresenter;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import org.stopepidemic.Encounter;
import org.stopepidemic.StopEpidemic;
import org.stopepidemic.settings.Settings;

import javax.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MainPresenter extends GluonPresenter<StopEpidemic> {

    private static final Random random = new Random();
    private static final int MAX_VERSION = 65535;

    private MessageDigest digest;

    private final Map<String, Encounter> encounters = new ConcurrentHashMap<>();

    private Optional<BleService> bleService;

    @FXML
    private View main;

    @FXML
    private Label labelImmediate;

    @FXML
    private Label labelNear;

    @FXML
    private Label labelFar;

    @FXML
    private Label labelUnknown;

    @FXML
    private TextArea textAreaExposure;

    @Inject
    Settings settings;

    private Timer timer = new Timer();

    public void initialize() {
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not supported. The app must be closed.");
        }
        settings.setMinor(random.nextInt(MAX_VERSION + 1));
        settings.setMajor(random.nextInt(MAX_VERSION + 1));
        bleService = BleService.create();

        // schedule printout
        timer.scheduleAtFixedRate(new TimerTask() {
                                      @Override
                                      public void run() {

                                          StringBuilder b = new StringBuilder();
                                          for (Encounter e : encounters.values()) {
                                              e.checkSignalLost();
                                              if (!e.isExpired()) {
                                                  b.append(e.toString()).append("\n");
                                              }
                                          }
                                          textAreaExposure.setText(b.toString());
                                      }
                                  }
                , 1000L, 1000L);

        main.showingProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue) {
                AppBar appBar = getApp().getAppBar();
                appBar.setNavIcon(MaterialDesignIcon.MENU.button(e -> getApp().getDrawer().open()));
                appBar.setTitleText("Stop Epidemic");

                bleService.ifPresent(ble ->
                        ble.startBroadcasting(UUID.fromString(settings.getUUID()),
                                settings.getMajor(),
                                settings.getMinor(),
                                settings.getID()));

                bleService.ifPresent(ble ->
                        ble.startScanning(new Configuration(settings.getUUID()),
                                (ScanDetection t) -> javafx.application.Platform.runLater(() -> {
                                    byte[] encounterID = computeEncounterID(t.getMajor(), t.getMinor());
                                    String encounterKey = new String(encounterID);
                                    encounters.putIfAbsent(encounterKey, new Encounter(encounterID));
                                    Encounter encounter = encounters.get(encounterKey);
                                    encounter.seen(t.getRssi(), t.getProximity());

                                    int[] counts = new int[Proximity.values().length];
                                    for (Encounter e : encounters.values()) {
                                        if (e.hasSignal()) {
                                            Proximity proximity = e.getLastProximity();
                                            if (proximity != null) {
                                                counts[proximity.getProximity()] += 1;
                                            }
                                        }
                                    }

                                    for (Proximity value : Proximity.values()) {
                                        switch (value) {
                                            case FAR:
                                                labelFar.setText(String.valueOf(counts[value.getProximity()]));
                                                break;
                                            case NEAR:
                                                labelNear.setText(String.valueOf(counts[value.getProximity()]));
                                                break;
                                            case IMMEDIATE:
                                                labelImmediate.setText(String.valueOf(counts[value.getProximity()]));
                                                break;
                                            case UNKNOWN:
                                                labelUnknown.setText(String.valueOf(counts[value.getProximity()]));
                                                break;
                                        }
                                    }
                                })
                        ));

            }
        });

    }

    private byte[] computeEncounterID(int major, int minor) {
        int[] uniqueEncounter = new int[]{settings.getMajor(), settings.getMinor(), major, minor};
        // make the number stable across devices
        Arrays.sort(uniqueEncounter);
        String keyText = settings.getUUID() + "-" + uniqueEncounter[0] + uniqueEncounter[1] + uniqueEncounter[2] + uniqueEncounter[3];
        return digest.digest(keyText.getBytes(StandardCharsets.UTF_8));
    }

}

