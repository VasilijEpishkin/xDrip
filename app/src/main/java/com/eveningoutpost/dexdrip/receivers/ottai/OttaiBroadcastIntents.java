package com.eveningoutpost.dexdrip.receivers.ottai;

/**
 * Ottai CGM companion app broadcast protocol constants.
 *
 * TODO: Verify these constants against the actual Ottai app by capturing broadcasts:
 *   adb logcat | grep -i "ottai\|broadcast\|intent" -A 3
 * Or decompile the Ottai APK to find the actual action strings and extras.
 */
public interface OttaiBroadcastIntents {

    // TODO: Replace with actual Ottai package/action strings discovered from the Ottai app
    String ACTION_NEW_BG_ESTIMATE = "com.ottai.cgm.action.BgEstimate";
    String ACTION_SENSOR_NEW      = "com.ottai.cgm.action.SensorNew";
    String ACTION_SENSOR_STOP     = "com.ottai.cgm.action.SensorStop";

    // Extras — TODO: verify actual key names from the Ottai app
    String OTTAI_BG_VALUE   = "com.ottai.cgm.BgValue";    // double — glucose value
    String OTTAI_BG_UNIT    = "com.ottai.cgm.BgUnit";     // String — "mg/dl" or "mmol/l"
    String OTTAI_TIMESTAMP  = "com.ottai.cgm.Time";        // long — epoch ms
    String OTTAI_SENSOR_ID  = "com.ottai.cgm.SensorId";   // String — sensor serial / UUID

    String UNIT_MMOL_L = "mmol/l";
    String UNIT_MG_DL  = "mg/dl";
}
