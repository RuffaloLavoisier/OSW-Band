package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.agps;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.bp.Instant;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;

import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdatePreferences;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.GarminSupport;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GBTarFile;

public class AgpsHandler {
    private static final Logger LOG = LoggerFactory.getLogger(AgpsHandler.class);
    private static final String QUERY_CONSTELLATIONS = "constellations";
    private final GarminSupport deviceSupport;

    public AgpsHandler(GarminSupport deviceSupport) {
        this.deviceSupport = deviceSupport;
    }

    public byte[] handleAgpsRequest(final String path, final Map<String, String> query) {
        try {
            if (!query.containsKey(QUERY_CONSTELLATIONS)) {
                LOG.debug("Query does not contain information about constellations; skipping request.");
                return null;
            }
            final File agpsFile = deviceSupport.getAgpsFile();
            if (!agpsFile.exists() || !agpsFile.isFile()) {
                LOG.info("File with AGPS data does not exist.");
                return null;
            }
            try(InputStream agpsIn = new FileInputStream(agpsFile)) {
                final byte[] rawBytes = FileUtils.readAll(agpsIn, 1024 * 1024); // 1MB, they're usually ~60KB
                final GBTarFile tarFile = new GBTarFile(rawBytes);
                final String[] requestedConstellations = Objects.requireNonNull(query.get(QUERY_CONSTELLATIONS)).split(",");
                for (final String constellation: requestedConstellations) {
                    try {
                        final GarminAgpsDataType garminAgpsDataType = GarminAgpsDataType.valueOf(constellation);
                        if (!tarFile.containsFile(garminAgpsDataType.getFileName())) {
                            LOG.error("AGPS archive is missing requested file: {}", garminAgpsDataType.getFileName());
                            deviceSupport.evaluateGBDeviceEvent(new GBDeviceEventUpdatePreferences(
                                    DeviceSettingsPreferenceConst.PREF_AGPS_STATUS, GarminAgpsStatus.ERROR.name()
                            ));
                            return null;
                        }
                    } catch (IllegalArgumentException e) {
                        LOG.error("Device requested unsupported AGPS data type: {}", constellation);
                        deviceSupport.evaluateGBDeviceEvent(new GBDeviceEventUpdatePreferences(
                                DeviceSettingsPreferenceConst.PREF_AGPS_STATUS, GarminAgpsStatus.ERROR.name()
                        ));
                        return null;
                    }
                }
                LOG.info("Sending new AGPS data to the device.");
                return rawBytes;
            }
        } catch (IOException e) {
            LOG.error("Unable to obtain AGPS data.", e);
            deviceSupport.evaluateGBDeviceEvent(new GBDeviceEventUpdatePreferences(
                    DeviceSettingsPreferenceConst.PREF_AGPS_STATUS, GarminAgpsStatus.ERROR.name()
            ));
            return null;
        }
    }

    public Callable<Void> getOnDataSuccessfullySentListener() {
        return () -> {
            LOG.info("AGPS data successfully sent to the device.");
            deviceSupport.evaluateGBDeviceEvent(new GBDeviceEventUpdatePreferences(
                    DeviceSettingsPreferenceConst.PREF_AGPS_UPDATE_TIME, Instant.now().toEpochMilli()
            ));
            deviceSupport.evaluateGBDeviceEvent(new GBDeviceEventUpdatePreferences(
                    DeviceSettingsPreferenceConst.PREF_AGPS_STATUS, GarminAgpsStatus.CURRENT.name()
            ));
            if (deviceSupport.getAgpsFile().delete()) {
                LOG.info("AGPS data was deleted from the cache folder.");
            }
            return null;
        };
    }
}