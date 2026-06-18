package com.eveningoutpost.dexdrip.receivers.ottai;

import static com.eveningoutpost.dexdrip.models.BgReading.bgReadingInsertFromJson;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.LibreOOPAlgorithm;
import com.eveningoutpost.dexdrip.models.Sensor;
import com.eveningoutpost.dexdrip.models.UserError.Log;
import com.eveningoutpost.dexdrip.utilitymodels.Intents;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utilitymodels.PumpStatus;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

public class OttaiAppReceiver extends BroadcastReceiver {

    private static final String TAG = "OttaiAppReceiver";
    private static SharedPreferences prefs;
    private static final Object lock = new Object();

    @Override
    public void onReceive(final Context context, final Intent intent) {
        new Thread() {
            @Override
            public void run() {
                PowerManager.WakeLock wl = JoH.getWakeLock("ottai-receiver", 60000);
                synchronized (lock) {
                    try {
                        Log.d(TAG, "onReceive: " + intent.getAction());
                        JoH.benchmark(null);
                        if (prefs == null)
                            prefs = PreferenceManager.getDefaultSharedPreferences(context);

                        final Bundle bundle = intent.getExtras();
                        final String action = intent.getAction();
                        if (action == null) return;

                        switch (action) {
                            case Intents.XDRIP_PLUS_OTTAI_APP:
                                if (!Home.get_follower() && DexCollectionType.getDexCollectionType() != DexCollectionType.Ottai &&
                                        !Pref.getBooleanDefaultFalse("external_blukon_algorithm")) {
                                    Log.e(TAG, "Received Ottai data but collection type is not Ottai");
                                    return;
                                }
                                if (!Home.get_follower()) {
                                    if (!Sensor.isActive()) {
                                        Home.toaststaticnext("Please use: Start Sensor from the menu for best results!");
                                    }
                                }
                                if (bundle == null) break;
                                final String collection = bundle.getString("collection");
                                if (collection == null) return;
                                switch (collection) {
                                    case "entries":
                                        final String data = bundle.getString("data");
                                        if (data != null && data.length() > 0) {
                                            try {
                                                final JSONArray json_array = new JSONArray(data);
                                                if (json_array.length() > 1) {
                                                    final JSONObject json_object = json_array.getJSONObject(0);
                                                    int process_id = -1;
                                                    try {
                                                        process_id = json_object.getInt("ROW_ID");
                                                    } catch (JSONException e) {
                                                        // intentionally ignored
                                                    }
                                                    if (process_id == -1 || process_id == android.os.Process.myPid()) {
                                                        LibreOOPAlgorithm.handleData(json_array.getString(1));
                                                    } else {
                                                        Log.d(TAG, "Ignoring OOP result: wrong process id " + process_id);
                                                    }
                                                } else {
                                                    final JSONObject json_object = json_array.getJSONObject(0);
                                                    final String type = json_object.getString("type");
                                                    if ("sgv".equals(type)) {
                                                        double slope = 0;
                                                        try {
                                                            slope = BgReading.slopefromName(json_object.getString("direction"));
                                                        } catch (JSONException e) {
                                                            // no direction field
                                                        }
                                                        bgReadingInsertFromData(
                                                                json_object.getLong("date"),
                                                                json_object.getDouble("sgv"),
                                                                slope, true);
                                                    } else {
                                                        Log.e(TAG, "Unknown entries type: " + type);
                                                    }
                                                }
                                            } catch (JSONException e) {
                                                Log.e(TAG, "JSON exception: " + e);
                                            }
                                        }
                                        break;
                                    case "devicestatus":
                                        final String ddata = bundle.getString("data");
                                        if (ddata != null && ddata.length() > 0) {
                                            try {
                                                final JSONArray json_array = new JSONArray(ddata);
                                                final JSONObject json_object = json_array.getJSONObject(0);
                                                final JSONObject json_pump = json_object.getJSONObject("pump");
                                                try {
                                                    PumpStatus.setReservoir(json_pump.getDouble("reservoir"));
                                                } catch (JSONException e) { /* optional field */ }
                                                try {
                                                    PumpStatus.setBattery(json_pump.getJSONObject("battery").getDouble("percent"));
                                                } catch (JSONException e) { /* optional field */ }
                                                try {
                                                    PumpStatus.setBolusIoB(json_pump.getJSONObject("iob").getDouble("bolusiob"));
                                                } catch (JSONException e) { /* optional field */ }
                                            } catch (Exception e) {
                                                Log.e(TAG, "Exception processing devicestatus: " + e);
                                            }
                                            PumpStatus.syncUpdate();
                                        }
                                        break;
                                    default:
                                        Log.d(TAG, "Unprocessed collection: " + collection);
                                }
                                break;
                            default:
                                Log.e(TAG, "Unknown action: " + action);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Exception handling intent", e);
                    } finally {
                        JoH.benchmark("ottai process");
                        JoH.releaseWakeLock(wl);
                    }
                }
            }
        }.start();
    }

    public static BgReading bgReadingInsertFromData(long timestamp, double sgv, double slope, boolean do_notification) {
        Log.d(TAG, "bgReadingInsertFromData: timestamp=" + timestamp + " bg=" + sgv);
        final JSONObject faux_bgr = new JSONObject();
        try {
            faux_bgr.put("timestamp", timestamp);
            faux_bgr.put("calculated_value", sgv);
            faux_bgr.put("filtered_calculated_value", sgv);
            faux_bgr.put("calculated_value_slope", slope);
            faux_bgr.put("source_info", "Ottai Follow");
            faux_bgr.put("raw_data", sgv);
            faux_bgr.put("age_adjusted_raw_value", sgv);
            faux_bgr.put("filtered_data", sgv);
            faux_bgr.put("noise", 1);
            faux_bgr.put("uuid", UUID.randomUUID().toString());
        } catch (JSONException e) {
            Log.e(TAG, "JSON exception: " + e);
            return null;
        }
        Sensor.createDefaultIfMissing();
        return bgReadingInsertFromJson(faux_bgr.toString(), do_notification, true);
    }
}
