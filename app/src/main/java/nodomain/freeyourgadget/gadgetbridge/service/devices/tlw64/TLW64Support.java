/*  Copyright (C) 2015-2020 Andreas Böhler, Andreas Shimokawa, Carsten
    Pfeiffer, Daniel Dakhno, Daniele Gobbetti, JohnnySun, José Rebelo,
    Erik Bloß

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package nodomain.freeyourgadget.gadgetbridge.service.devices.tlw64;

import android.bluetooth.BluetoothGattCharacteristic;
import android.net.Uri;
import android.text.format.DateFormat;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.SettingsActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.tlw64.TLW64Constants;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.CalendarEventSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CannedMessagesSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

import static org.apache.commons.lang3.math.NumberUtils.min;

public class TLW64Support extends AbstractBTLEDeviceSupport {

    private static final Logger LOG = LoggerFactory.getLogger(TLW64Support.class);

    public BluetoothGattCharacteristic ctrlCharacteristic = null;

    public TLW64Support() {
        super(LOG);
        addSupportedService(TLW64Constants.UUID_SERVICE_NO1);
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        LOG.info("Initializing");

        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZING, getContext()));

        ctrlCharacteristic = getCharacteristic(TLW64Constants.UUID_CHARACTERISTIC_CONTROL);

        setTime(builder);
        setDisplaySettings(builder);
        sendSettings(builder);

        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZED, getContext()));

        LOG.info("Initialization Done");

        return builder;
    }

    @Override
    public boolean useAutoConnect() {
        return false;
    }

    @Override
    public void onNotification(NotificationSpec notificationSpec) {
        switch (notificationSpec.type) {
            case GENERIC_SMS:
                showNotification(TLW64Constants.NOTIFICATION_SMS, notificationSpec.sender);
                setVibration(1, 1);
                break;
            case WECHAT:
                showIcon(TLW64Constants.ICON_WECHAT);
                setVibration(1, 1);
                break;
            default:
                showIcon(TLW64Constants.ICON_MAIL);
                setVibration(1, 1);
                break;
        }
    }

    @Override
    public void onDeleteNotification(int id) {

    }

    @Override
    public void onSetTime() {
        try {
            TransactionBuilder builder = performInitialized("setTime");
            setTime(builder);
            builder.queue(getQueue());
        } catch (IOException e) {
            GB.toast(getContext(), "Error setting time: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
        }
    }

    @Override
    public void onSetAlarms(ArrayList<? extends Alarm> alarms) {

    }

    @Override
    public void onSetCallState(CallSpec callSpec) {
        if (callSpec.command == CallSpec.CALL_INCOMING) {
            showNotification(TLW64Constants.NOTIFICATION_CALL, callSpec.name);
            setVibration(3, 5);
        } else {
            stopNotification();
            setVibration(0, 0);
        }
    }

    @Override
    public void onSetCannedMessages(CannedMessagesSpec cannedMessagesSpec) {

    }

    @Override
    public void onSetMusicState(MusicStateSpec stateSpec) {

    }

    @Override
    public void onSetMusicInfo(MusicSpec musicSpec) {

    }

    @Override
    public void onEnableRealtimeSteps(boolean enable) {

    }

    @Override
    public void onInstallApp(Uri uri) {

    }

    @Override
    public void onAppInfoReq() {

    }

    @Override
    public void onAppStart(UUID uuid, boolean start) {

    }

    @Override
    public void onAppDelete(UUID uuid) {

    }

    @Override
    public void onAppConfiguration(UUID appUuid, String config, Integer id) {

    }

    @Override
    public void onAppReorder(UUID[] uuids) {

    }

    @Override
    public void onFetchRecordedData(int dataTypes) {

    }

    @Override
    public void onReset(int flags) {

    }

    @Override
    public void onHeartRateTest() {

    }

    @Override
    public void onEnableRealtimeHeartRateMeasurement(boolean enable) {

    }

    @Override
    public void onFindDevice(boolean start) {
        if (start) {
            setVibration(1, 3);
        }
    }

    @Override
    public void onSetConstantVibration(int integer) {

    }

    @Override
    public void onScreenshotReq() {

    }

    @Override
    public void onEnableHeartRateSleepSupport(boolean enable) {

    }

    @Override
    public void onSetHeartRateMeasurementInterval(int seconds) {

    }

    @Override
    public void onAddCalendarEvent(CalendarEventSpec calendarEventSpec) {

    }

    @Override
    public void onDeleteCalendarEvent(byte type, long id) {

    }

    @Override
    public void onSendConfiguration(String config) {

    }

    @Override
    public void onReadConfiguration(String config) {

    }

    @Override
    public void onTestNewFunction() {

    }

    @Override
    public void onSendWeather(WeatherSpec weatherSpec) {

    }

    private void setVibration(int duration, int count) {
        try {
            TransactionBuilder builder = performInitialized("vibrate");
            byte[] msg = new byte[]{
                    TLW64Constants.CMD_ALARM,
                    0,
                    0,
                    0,
                    (byte) duration,
                    (byte) count,
                    7,                  // unknown, sniffed by original app
                    1
            };
            builder.write(ctrlCharacteristic, msg);
            builder.queue(getQueue());
        } catch (IOException e) {
            LOG.warn("Unable to set vibration", e);
        }
    }

    private void setTime(TransactionBuilder transaction) {
        Calendar c = GregorianCalendar.getInstance();
        byte[] datetimeBytes = new byte[]{
                TLW64Constants.CMD_DATETIME,
                (byte) (c.get(Calendar.YEAR) / 256),
                (byte) (c.get(Calendar.YEAR) % 256),
                (byte) (c.get(Calendar.MONTH) + 1),
                (byte) c.get(Calendar.DAY_OF_MONTH),
                (byte) c.get(Calendar.HOUR_OF_DAY),
                (byte) c.get(Calendar.MINUTE),
                (byte) c.get(Calendar.SECOND)
        };
        transaction.write(ctrlCharacteristic, datetimeBytes);
    }

    private void setDisplaySettings(TransactionBuilder transaction) {
        byte[] displayBytes = new byte[]{
                TLW64Constants.CMD_DISPLAY_SETTINGS,
                0x00,   // 1 - display distance in kilometers, 2 - in miles
                0x00    // 1 - display 24-hour clock, 2 - for 12-hour with AM/PM
        };
        String units = GBApplication.getPrefs().getString(SettingsActivity.PREF_MEASUREMENT_SYSTEM, getContext().getString(R.string.p_unit_metric));
        if (units.equals(getContext().getString(R.string.p_unit_metric))) {
            displayBytes[1] = 1;
        } else {
            displayBytes[1] = 2;
        }
        if (DateFormat.is24HourFormat(getContext())) {
            displayBytes[2] = 1;
        } else {
            displayBytes[2] = 2;
        }
        transaction.write(ctrlCharacteristic, displayBytes);
        return;
    }

    private void sendSettings(TransactionBuilder builder) {
        // TODO Create custom settings page for changing hardcoded values

        // set user data
        ActivityUser activityUser = new ActivityUser();
        byte[] userBytes = new byte[]{
                TLW64Constants.CMD_USER_DATA,
                0,  // unknown
                0,  // step length [cm]
                0,  // unknown
                (byte) activityUser.getWeightKg(),
                5,  // screen on time / display timeout
                0,  // unknown
                0,  // unknown
                (byte) (activityUser.getStepsGoal() / 256),
                (byte) (activityUser.getStepsGoal() % 256),
                1,  // raise hand to turn on screen, ON = 1, OFF = 0
                (byte) 0xff, // unknown
                0,  // unknown
                (byte) activityUser.getAge(),
                0,  // gender
                0,  // lost function, ON = 1, OFF = 0 TODO: find out what this does
                2   // unknown
        };

        if (activityUser.getGender() == ActivityUser.GENDER_FEMALE)
        {
            userBytes[14] = 2; // female
            // default and factor from https://livehealthy.chron.com/determine-stride-pedometer-height-weight-4518.html
            if (activityUser.getHeightCm() != 0)
                userBytes[2] = (byte) Math.ceil(activityUser.getHeightCm() * 0.413);
            else
                userBytes[2] = 70; // default
        }

        else
        {
            userBytes[14] = 1; // male
            if (activityUser.getHeightCm() != 0)
                userBytes[2] = (byte) Math.ceil(activityUser.getHeightCm() * 0.415);
            else
                userBytes[2] = 78; // default
        }

        builder.write(ctrlCharacteristic, userBytes);
    }

    private void showIcon(int iconId) {
        try {
            TransactionBuilder builder = performInitialized("showIcon");
            byte[] msg = new byte[]{
                    TLW64Constants.CMD_ICON,
                    (byte) iconId
            };
            builder.write(ctrlCharacteristic, msg);
            builder.queue(getQueue());
        } catch (IOException e) {
            GB.toast(getContext(), "Error showing icon: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
        }
    }

    private void showNotification(int type, String text) {
        try {
            TransactionBuilder builder = performInitialized("showNotification");
            int length;
            byte[] bytes;
            byte[] msg;

            // send text
            bytes = text.getBytes("EUC-JP");
            length = min(bytes.length, 18);
            msg = new byte[length + 2];
            msg[0] = TLW64Constants.CMD_NOTIFICATION;
            msg[1] = TLW64Constants.NOTIFICATION_HEADER;
            System.arraycopy(bytes, 0, msg, 2, length);
            builder.write(ctrlCharacteristic, msg);

            // send notification type
            msg = new byte[2];
            msg[0] = TLW64Constants.CMD_NOTIFICATION;
            msg[1] = (byte) type;
            builder.write(ctrlCharacteristic, msg);

            builder.queue(getQueue());
        } catch (IOException e) {
            GB.toast(getContext(), "Error showing notificaton: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
        }
    }

    private void stopNotification() {
        try {
            TransactionBuilder builder = performInitialized("clearNotification");
            byte[] msg = new byte[]{
                    TLW64Constants.CMD_NOTIFICATION,
                    TLW64Constants.NOTIFICATION_STOP
            };
            builder.write(ctrlCharacteristic, msg);
            builder.queue(getQueue());
        } catch (IOException e) {
            LOG.warn("Unable to stop notification", e);
        }
    }
}
