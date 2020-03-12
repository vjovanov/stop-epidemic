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
package org.stoppandemic.views;

import com.gluonhq.attach.ble.BleService;
import com.gluonhq.attach.ble.Configuration;
import com.gluonhq.attach.ble.ScanDetection;
import com.gluonhq.charm.glisten.afterburner.GluonPresenter;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.stoppandemic.Encounter;
import org.stoppandemic.StopPandemic;
import org.stoppandemic.settings.Settings;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MainPresenter extends GluonPresenter<StopPandemic> {

    private static final Random random = new Random();
    private static final int MAX_VERSION = 65535;
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

    @Inject
    Settings settings;

    public void initialize() {

        settings.setMinor(random.nextInt(MAX_VERSION + 1));
        settings.setMajor(random.nextInt(MAX_VERSION + 1));
        bleService = BleService.create();

        main.showingProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue) {
                AppBar appBar = getApp().getAppBar();
                appBar.setNavIcon(MaterialDesignIcon.MENU.button(e -> getApp().getDrawer().open()));
                appBar.setTitleText("Stop Pandemic");

                bleService.ifPresent(ble ->
                        ble.startBroadcasting(UUID.fromString(settings.getUUID()),
                                settings.getMajor(),
                                settings.getMinor(),
                                settings.getID()));

                bleService.ifPresent(ble -> {
                    ble.startScanning(new Configuration(settings.getUUID()), (ScanDetection t) -> javafx.application.Platform.runLater(() -> {
                                int[] uniqueEncounter = new int[]{settings.getMajor(), settings.getMajor(), t.getMajor(), t.getMinor()};
                                Arrays.sort(uniqueEncounter);
                                String encounterID = "" + uniqueEncounter[0] + uniqueEncounter[1] + uniqueEncounter[2] + uniqueEncounter[3];
                                Encounter encounter = encounters.putIfAbsent(encounterID, new Encounter(encounterID));
                                if (encounter != null) {
                                    encounter.seen(t.getRssi(), t.getProximity());
                                }
                                labelImmediate.setText("" + encounters.size());
                            })
                    );
                });
            }
        });

    }


}

