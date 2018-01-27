/**
 * Copyright (c) 2014,2018 by the respective copyright holders.
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.vitaled.handler;

import static org.openhab.binding.vitaled.vitaLEDBindingConstants.*;

import java.math.BigDecimal;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.vitaled.internal.vitaLEDConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link vitaLEDHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Marcel Salein - Initial contribution
 */
@NonNullByDefault
public class vitaLEDHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(vitaLEDHandler.class);

    private BigDecimal refreshInterval;

    private vitaLEDConfiguration connection;
    ScheduledFuture<?> refreshJob;

    @Override
    public void dispose() {
        refreshJob.cancel(true);
    }

    private void updateVitaLED(int zoneNumber) throws Exception {
        // read values from VitaLED LAN Master
        connection.getCurrentStateOfZone(zoneNumber);
        connection.getSceneDescription();
        Command command = RefreshType.REFRESH;
        ChannelUID channelUID;
        // update all channels of zone
        int i = zoneNumber;
        // get channel group
        String channelGroup = ZONE + Integer.toString(i + 1);
        // update channels of channel group
        // update achromaticLight
        channelUID = new ChannelUID(getThing().getUID(), channelGroup + "#" + ACHROMATIC_LIGHT);
        handleCommand(channelUID, command);
        // update intensity
        channelUID = new ChannelUID(getThing().getUID(), channelGroup + "#" + INTENSITY);
        handleCommand(channelUID, command);
        // update red
        channelUID = new ChannelUID(getThing().getUID(), channelGroup + "#" + RED);
        handleCommand(channelUID, command);
        // update green
        channelUID = new ChannelUID(getThing().getUID(), channelGroup + "#" + GREEN);
        handleCommand(channelUID, command);
        // update blue
        channelUID = new ChannelUID(getThing().getUID(), channelGroup + "#" + BLUE);
        handleCommand(channelUID, command);
        // update white
        channelUID = new ChannelUID(getThing().getUID(), channelGroup + "#" + WHITE);
        handleCommand(channelUID, command);
        // colourSaturation
        channelUID = new ChannelUID(getThing().getUID(), channelGroup + "#" + COLOUR_SATURATION);
        handleCommand(channelUID, command);
        // speed
        channelUID = new ChannelUID(getThing().getUID(), channelGroup + "#" + SPEED);
        handleCommand(channelUID, command);
        // colourGradientIntensity
        channelUID = new ChannelUID(getThing().getUID(), channelGroup + "#" + COLOUR_GRADIENT_INTENSITY);
        handleCommand(channelUID, command);
        // Active Mode
        channelUID = new ChannelUID(getThing().getUID(), channelGroup + "#" + ACTIVE_MODE);
        handleCommand(channelUID, command);
        // update x-Coordinate
        channelUID = new ChannelUID(getThing().getUID(), channelGroup + "#" + X_COORDINATE);
        handleCommand(channelUID, command);
        // update y-Coordinate
        channelUID = new ChannelUID(getThing().getUID(), channelGroup + "#" + Y_COORDINATE);
        handleCommand(channelUID, command);
        // update scene 1
        channelUID = new ChannelUID(getThing().getUID(), channelGroup + "#" + SCENE1);
        handleCommand(channelUID, command);
        // update scene 2
        channelUID = new ChannelUID(getThing().getUID(), channelGroup + "#" + SCENE2);
        handleCommand(channelUID, command);
        // update scene 3
        channelUID = new ChannelUID(getThing().getUID(), channelGroup + "#" + SCENE3);
        handleCommand(channelUID, command);
        // update scene 4
        channelUID = new ChannelUID(getThing().getUID(), channelGroup + "#" + SCENE4);
        handleCommand(channelUID, command);
        // update scene 5
        channelUID = new ChannelUID(getThing().getUID(), channelGroup + "#" + SCENE5);
        handleCommand(channelUID, command);
        // update scene 6
        channelUID = new ChannelUID(getThing().getUID(), channelGroup + "#" + SCENE6);
        handleCommand(channelUID, command);
    }

    public vitaLEDHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            // Refresh state of channel
            DecimalType newState;
            try {
                newState = connection.getCurrentState(channelUID);
            } catch (NumberFormatException e) {
                // Handle exception
                logger.error("Exception occurred during execution: {}", e.getMessage(), e);
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
                return;
            } catch (Exception e) {
                // Handle exception
                logger.error("Exception occurred during execution: {}", e.getMessage(), e);
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
                return;
            }
            // update state of channel
            updateState(channelUID, newState);
        } else {
            // handle update of channels
            logger.debug("Trying to update " + channelUID.getId().toString() + " with value " + command.toString());
            try {
                boolean success = connection.updateState(channelUID, command);
                if (success) {
                    // full update necessary for the zone
                    int zone = Integer.parseInt(channelUID.getGroupId().substring(4, 5)) - 1;
                    updateVitaLED(zone);
                }
                ;
            } catch (Exception e) {
                logger.error("Failed to update " + channelUID.getId().toString() + " with value " + command.toString());
            }
        }
    }

    private void startAutomaticRefresh() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    // refresh all zones
                    for (int i = 0; i < 8; i++) {
                        updateVitaLED(i);
                    }
                    // update zone description
                    connection.getZoneDescriptions();
                } catch (Exception e) {
                    logger.debug("Exception occurred during execution: {}", e.getMessage(), e);
                }
            }
        };
        refreshJob = scheduler.scheduleAtFixedRate(runnable, 0, refreshInterval.intValue(), TimeUnit.SECONDS);
    }

    @Override
    public void initialize() {
        logger.info("Initializing handler for VitaLED");
        if (this.getConfig().get(HOST_PARAMETER) != null) {
            String host = (String) this.getConfig().get(HOST_PARAMETER);
            Integer port = 80;
            Object portObj = this.getConfig().get(TCP_PORT_PARAMETER);
            if (portObj != null) {
                if (portObj instanceof Number) {
                    port = ((Number) portObj).intValue();
                } else if (portObj instanceof String) {
                    port = Integer.parseInt(portObj.toString());
                }
            }
            try {
                refreshInterval = ((BigDecimal) this.getConfig().get(REFRESH_INTERVAL));
            } catch (Exception e) {
                refreshInterval = new BigDecimal(60);
                logger.warn("No refresh Interval defined using {}s", refreshInterval);
            }
            logger.warn("No refresh Interval defined using {}s", refreshInterval);
            logger.info("Host " + host + " Port " + port);
            connection = new vitaLEDConfiguration(host, port);
            try {
                // update state of channels for all zones
                for (int i = 0; i < 8; i++) {
                    updateVitaLED(i);
                }
                // update zone description
                connection.getZoneDescriptions();
            } catch (Exception e) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.CONFIGURATION_ERROR,
                        "Cannot connect to VitaLED LAN Master. IP address not set.");
                return;
            }
            updateStatus(ThingStatus.ONLINE);
            // schedule automatic refresh
            // startAutomaticRefresh();
        } else {
            // Note: When initialization can NOT be done set the status with more details for further
            // analysis. See also class ThingStatusDetail for all available status details.
            // Add a description to give user information to understand why thing does not work
            // as expected. E.g.
            // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
            // "Can not access device as username and/or password are invalid");
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.CONFIGURATION_ERROR,
                    "Cannot connect to VitaLED LAN Master. IP address not set.");
        }
    }
}
