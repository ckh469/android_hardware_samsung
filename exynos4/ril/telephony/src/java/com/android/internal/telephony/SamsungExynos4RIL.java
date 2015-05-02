/*
 * Copyright (C) 2006 The Android Open Source Project
 * Copyright (C) 2011 The CyanogenMod Project <http://www.cyanogenmod.org>
 * Copyright (C) 2014 The OmniROM Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.internal.telephony;

import static com.android.internal.telephony.RILConstants.*;
import static android.telephony.TelephonyManager.NETWORK_TYPE_UNKNOWN;
import static android.telephony.TelephonyManager.NETWORK_TYPE_EDGE;
import static android.telephony.TelephonyManager.NETWORK_TYPE_GPRS;
import static android.telephony.TelephonyManager.NETWORK_TYPE_UMTS;
import static android.telephony.TelephonyManager.NETWORK_TYPE_HSDPA;
import static android.telephony.TelephonyManager.NETWORK_TYPE_HSUPA;
import static android.telephony.TelephonyManager.NETWORK_TYPE_HSPA;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.net.NetworkInfo;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.os.PowerManager.WakeLock;
import android.telephony.NeighboringCellInfo;
import android.telephony.PhoneNumberUtils;
import android.telephony.SignalStrength;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.text.TextUtils;
import android.telephony.Rlog;

import com.android.internal.telephony.gsm.SmsBroadcastConfigInfo;
import com.android.internal.telephony.gsm.SuppServiceNotification;
import com.android.internal.telephony.cdma.CdmaCallWaitingNotification;
import com.android.internal.telephony.cdma.CdmaInformationRecords;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.Runtime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

public class SamsungExynos4RIL extends RIL implements CommandsInterface {

    //SAMSUNG STATES
    static final int RIL_REQUEST_GET_CELL_BROADCAST_CONFIG = 10002;

    static final int RIL_REQUEST_SEND_ENCODED_USSD = 10005;
    static final int RIL_REQUEST_SET_PDA_MEMORY_STATUS = 10006;
    static final int RIL_REQUEST_GET_PHONEBOOK_STORAGE_INFO = 10007;
    static final int RIL_REQUEST_GET_PHONEBOOK_ENTRY = 10008;
    static final int RIL_REQUEST_ACCESS_PHONEBOOK_ENTRY = 10009;
    static final int RIL_REQUEST_DIAL_VIDEO_CALL = 10010;
    static final int RIL_REQUEST_CALL_DEFLECTION = 10011;
    static final int RIL_REQUEST_READ_SMS_FROM_SIM = 10012;
    static final int RIL_REQUEST_USIM_PB_CAPA = 10013;
    static final int RIL_REQUEST_LOCK_INFO = 10014;

    static final int RIL_REQUEST_DIAL_EMERGENCY = 10016;
    static final int RIL_REQUEST_GET_STOREAD_MSG_COUNT = 10017;
    static final int RIL_REQUEST_STK_SIM_INIT_EVENT = 10018;
    static final int RIL_REQUEST_GET_LINE_ID = 10019;
    static final int RIL_REQUEST_SET_LINE_ID = 10020;
    static final int RIL_REQUEST_GET_SERIAL_NUMBER = 10021;
    static final int RIL_REQUEST_GET_MANUFACTURE_DATE_NUMBER = 10022;
    static final int RIL_REQUEST_GET_BARCODE_NUMBER = 10023;
    static final int RIL_REQUEST_UICC_GBA_AUTHENTICATE_BOOTSTRAP = 10024;
    static final int RIL_REQUEST_UICC_GBA_AUTHENTICATE_NAF = 10025;
    static final int RIL_REQUEST_SIM_TRANSMIT_BASIC = 10026;
    static final int RIL_REQUEST_SIM_OPEN_CHANNEL = 10027;
    static final int RIL_REQUEST_SIM_CLOSE_CHANNEL = 10028;
    static final int RIL_REQUEST_SIM_TRANSMIT_CHANNEL = 10029;
    static final int RIL_REQUEST_SIM_AUTH = 10030;
    static final int RIL_REQUEST_PS_ATTACH = 10031;
    static final int RIL_REQUEST_PS_DETACH = 10032;
    static final int RIL_REQUEST_ACTIVATE_DATA_CALL = 10033;
    static final int RIL_REQUEST_CHANGE_SIM_PERSO = 10034;
    static final int RIL_REQUEST_ENTER_SIM_PERSO = 10035;
    static final int RIL_REQUEST_GET_TIME_INFO = 10036;
    static final int RIL_REQUEST_OMADM_SETUP_SESSION = 10037;
    static final int RIL_REQUEST_OMADM_SERVER_START_SESSION = 10038;
    static final int RIL_REQUEST_OMADM_CLIENT_START_SESSION = 10039;
    static final int RIL_REQUEST_OMADM_SEND_DATA = 10040;
    static final int RIL_REQUEST_CDMA_GET_DATAPROFILE = 10041;
    static final int RIL_REQUEST_CDMA_SET_DATAPROFILE = 10042;
    static final int RIL_REQUEST_CDMA_GET_SYSTEMPROPERTIES = 10043;
    static final int RIL_REQUEST_CDMA_SET_SYSTEMPROPERTIES = 10044;
    static final int RIL_REQUEST_SEND_SMS_COUNT = 10045;
    static final int RIL_REQUEST_SEND_SMS_MSG = 10046;
    static final int RIL_REQUEST_SEND_SMS_MSG_READ_STATUS = 10047;
    static final int RIL_REQUEST_MODEM_HANGUP = 10048;
    static final int RIL_REQUEST_SET_SIM_POWER = 10049;
    static final int RIL_REQUEST_SET_PREFERRED_NETWORK_LIST = 10050;
    static final int RIL_REQUEST_GET_PREFERRED_NETWORK_LIST = 10051;
    static final int RIL_REQUEST_HANGUP_VT = 10052;

    static final int RIL_UNSOL_RELEASE_COMPLETE_MESSAGE = 11001;
    static final int RIL_UNSOL_STK_SEND_SMS_RESULT = 11002;
    static final int RIL_UNSOL_STK_CALL_CONTROL_RESULT = 11003;
    static final int RIL_UNSOL_DUN_CALL_STATUS = 11004;

    static final int RIL_UNSOL_O2_HOME_ZONE_INFO = 11007;
    static final int RIL_UNSOL_DEVICE_READY_NOTI = 11008;
    static final int RIL_UNSOL_GPS_NOTI = 11009;
    static final int RIL_UNSOL_AM = 11010;
    static final int RIL_UNSOL_DUN_PIN_CONTROL_SIGNAL = 11011;
    static final int RIL_UNSOL_DATA_SUSPEND_RESUME = 11012;
    static final int RIL_UNSOL_SAP = 11013;

    static final int RIL_UNSOL_SIM_SMS_STORAGE_AVAILALE = 11015;
    static final int RIL_UNSOL_HSDPA_STATE_CHANGED = 11016;
    static final int RIL_UNSOL_WB_AMR_STATE = 11017;
    static final int RIL_UNSOL_TWO_MIC_STATE = 11018;
    static final int RIL_UNSOL_DHA_STATE = 11019;
    static final int RIL_UNSOL_UART = 11020;
    static final int RIL_UNSOL_RESPONSE_HANDOVER = 11021;
    static final int RIL_UNSOL_IPV6_ADDR = 11022;
    static final int RIL_UNSOL_NWK_INIT_DISC_REQUEST = 11023;
    static final int RIL_UNSOL_RTS_INDICATION = 11024;
    static final int RIL_UNSOL_OMADM_SEND_DATA = 11025;
    static final int RIL_UNSOL_DUN = 11026;
    static final int RIL_UNSOL_SYSTEM_REBOOT = 11027;
    static final int RIL_UNSOL_VOICE_PRIVACY_CHANGED = 11028;
    static final int RIL_UNSOL_UTS_GETSMSCOUNT = 11029;
    static final int RIL_UNSOL_UTS_GETSMSMSG = 11030;
    static final int RIL_UNSOL_UTS_GET_UNREAD_SMS_STATUS = 11031;
    static final int RIL_UNSOL_MIP_CONNECT_STATUS = 11032;

    protected HandlerThread mSamsungExynos4RILThread;
    private AudioManager audioManager;
    private boolean mIsGBModem = SystemProperties.getBoolean("ro.ril.gbmodem", false);

    public SamsungExynos4RIL(Context context, int preferredNetworkType,
            int cdmaSubscription, Integer instanceId) {
        super(context, preferredNetworkType, cdmaSubscription, instanceId);
        audioManager = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);
        mQANElements = 5;
    }

    static String
    requestToString(int request) {
        switch (request) {
            case RIL_REQUEST_DIAL_EMERGENCY: return "DIAL_EMERGENCY"; // We want to take care of this
            default: return RIL.requestToString(request); // Everything else comes from super class
        }
    }

    protected RILRequest
    processSolicited (Parcel p) {
        int serial, error;
        boolean found = false;

        serial = p.readInt();
        error = p.readInt();

        RILRequest rr;

        rr = findAndRemoveRequestFromList(serial);

        if (rr == null) {
            Rlog.w(RILJ_LOG_TAG, "Unexpected solicited response! sn: "
                            + serial + " error: " + error);
            return null;
        }

        Object ret = null;

        if (error == 0 || p.dataAvail() > 0) {
            // either command succeeds or command fails but with data payload
            try {switch (rr.mRequest) {
            /*
 cat libs/telephony/ril_commands.h \
 | egrep "^ *{RIL_" \
 | sed -re 's/\{([^,]+),[^,]+,([^}]+).+/case \1: ret = \2(p); break;/'
             */

            // We want to take care of this
            case RIL_REQUEST_DIAL_EMERGENCY: ret = responseVoid(p); break;

            // Everything below comes from super class
            case RIL_REQUEST_GET_SIM_STATUS: ret =  responseIccCardStatus(p); break;
            case RIL_REQUEST_ENTER_SIM_PIN: ret =  responseInts(p); break;
            case RIL_REQUEST_ENTER_SIM_PUK: ret =  responseInts(p); break;
            case RIL_REQUEST_ENTER_SIM_PIN2: ret =  responseInts(p); break;
            case RIL_REQUEST_ENTER_SIM_PUK2: ret =  responseInts(p); break;
            case RIL_REQUEST_CHANGE_SIM_PIN: ret =  responseInts(p); break;
            case RIL_REQUEST_CHANGE_SIM_PIN2: ret =  responseInts(p); break;
            case RIL_REQUEST_ENTER_DEPERSONALIZATION_CODE: ret =  responseInts(p); break;
            case RIL_REQUEST_GET_CURRENT_CALLS: ret =  responseCallList(p); break;
            case RIL_REQUEST_DIAL: ret =  responseVoid(p); break;
            case RIL_REQUEST_GET_IMSI: ret =  responseString(p); break;
            case RIL_REQUEST_HANGUP: ret =  responseVoid(p); break;
            case RIL_REQUEST_HANGUP_WAITING_OR_BACKGROUND: ret =  responseVoid(p); break;
            case RIL_REQUEST_HANGUP_FOREGROUND_RESUME_BACKGROUND: {
                if (mTestingEmergencyCall.getAndSet(false)) {
                    if (mEmergencyCallbackModeRegistrant != null) {
                        riljLog("testing emergency call, notify ECM Registrants");
                        mEmergencyCallbackModeRegistrant.notifyRegistrant();
                    }
                }
                ret =  responseVoid(p);
                break;
            }
            case RIL_REQUEST_SWITCH_WAITING_OR_HOLDING_AND_ACTIVE: ret =  responseVoid(p); break;
            case RIL_REQUEST_CONFERENCE: ret =  responseVoid(p); break;
            case RIL_REQUEST_UDUB: ret =  responseVoid(p); break;
            case RIL_REQUEST_LAST_CALL_FAIL_CAUSE: ret =  responseInts(p); break;
            case RIL_REQUEST_SIGNAL_STRENGTH: ret =  responseSignalStrength(p); break;
            case RIL_REQUEST_VOICE_REGISTRATION_STATE: ret =  responseStrings(p); break;
            case RIL_REQUEST_DATA_REGISTRATION_STATE: ret =  responseStrings(p); break;
            case RIL_REQUEST_OPERATOR: ret =  responseStrings(p); break;
            case RIL_REQUEST_RADIO_POWER: ret =  responseVoid(p); break;
            case RIL_REQUEST_DTMF: ret =  responseVoid(p); break;
            case RIL_REQUEST_SEND_SMS: ret =  responseSMS(p); break;
            case RIL_REQUEST_SEND_SMS_EXPECT_MORE: ret =  responseSMS(p); break;
            case RIL_REQUEST_SETUP_DATA_CALL: ret =  responseSetupDataCall(p); break;
            case RIL_REQUEST_SIM_IO: ret =  responseICC_IO(p); break;
            case RIL_REQUEST_SEND_USSD: ret =  responseVoid(p); break;
            case RIL_REQUEST_CANCEL_USSD: ret =  responseVoid(p); break;
            case RIL_REQUEST_GET_CLIR: ret =  responseInts(p); break;
            case RIL_REQUEST_SET_CLIR: ret =  responseVoid(p); break;
            case RIL_REQUEST_QUERY_CALL_FORWARD_STATUS: ret =  responseCallForward(p); break;
            case RIL_REQUEST_SET_CALL_FORWARD: ret =  responseVoid(p); break;
            case RIL_REQUEST_QUERY_CALL_WAITING: ret =  responseInts(p); break;
            case RIL_REQUEST_SET_CALL_WAITING: ret =  responseVoid(p); break;
            case RIL_REQUEST_SMS_ACKNOWLEDGE: ret =  responseVoid(p); break;
            case RIL_REQUEST_GET_IMEI: ret =  responseString(p); break;
            case RIL_REQUEST_GET_IMEISV: ret =  responseString(p); break;
            case RIL_REQUEST_ANSWER: ret =  responseVoid(p); break;
            case RIL_REQUEST_DEACTIVATE_DATA_CALL: ret =  responseVoid(p); break;
            case RIL_REQUEST_QUERY_FACILITY_LOCK: ret =  responseInts(p); break;
            case RIL_REQUEST_SET_FACILITY_LOCK: ret =  responseInts(p); break;
            case RIL_REQUEST_CHANGE_BARRING_PASSWORD: ret =  responseVoid(p); break;
            case RIL_REQUEST_QUERY_NETWORK_SELECTION_MODE: ret =  responseInts(p); break;
            case RIL_REQUEST_SET_NETWORK_SELECTION_AUTOMATIC: ret =  responseVoid(p); break;
            case RIL_REQUEST_SET_NETWORK_SELECTION_MANUAL: ret =  responseVoid(p); break;
            case RIL_REQUEST_QUERY_AVAILABLE_NETWORKS : ret =  responseOperatorInfos(p); break;
            case RIL_REQUEST_DTMF_START: ret =  responseVoid(p); break;
            case RIL_REQUEST_DTMF_STOP: ret =  responseVoid(p); break;
            case RIL_REQUEST_BASEBAND_VERSION: ret =  responseString(p); break;
            case RIL_REQUEST_SEPARATE_CONNECTION: ret =  responseVoid(p); break;
            case RIL_REQUEST_SET_MUTE: ret =  responseVoid(p); break;
            case RIL_REQUEST_GET_MUTE: ret =  responseInts(p); break;
            case RIL_REQUEST_QUERY_CLIP: ret =  responseInts(p); break;
            case RIL_REQUEST_LAST_DATA_CALL_FAIL_CAUSE: ret =  responseInts(p); break;
            case RIL_REQUEST_DATA_CALL_LIST: ret =  responseDataCallList(p); break;
            case RIL_REQUEST_RESET_RADIO: ret =  responseVoid(p); break;
            case RIL_REQUEST_OEM_HOOK_RAW: ret =  responseRaw(p); break;
            case RIL_REQUEST_OEM_HOOK_STRINGS: ret =  responseStrings(p); break;
            case RIL_REQUEST_SCREEN_STATE: ret =  responseVoid(p); break;
            case RIL_REQUEST_SET_SUPP_SVC_NOTIFICATION: ret =  responseVoid(p); break;
            case RIL_REQUEST_WRITE_SMS_TO_SIM: ret =  responseInts(p); break;
            case RIL_REQUEST_DELETE_SMS_ON_SIM: ret =  responseVoid(p); break;
            case RIL_REQUEST_SET_BAND_MODE: ret =  responseVoid(p); break;
            case RIL_REQUEST_QUERY_AVAILABLE_BAND_MODE: ret =  responseInts(p); break;
            case RIL_REQUEST_STK_GET_PROFILE: ret =  responseString(p); break;
            case RIL_REQUEST_STK_SET_PROFILE: ret =  responseVoid(p); break;
            case RIL_REQUEST_STK_SEND_ENVELOPE_COMMAND: ret =  responseString(p); break;
            case RIL_REQUEST_STK_SEND_TERMINAL_RESPONSE: ret =  responseVoid(p); break;
            case RIL_REQUEST_STK_HANDLE_CALL_SETUP_REQUESTED_FROM_SIM: ret =  responseInts(p); break;
            case RIL_REQUEST_EXPLICIT_CALL_TRANSFER: ret =  responseVoid(p); break;
            case RIL_REQUEST_SET_PREFERRED_NETWORK_TYPE: ret =  responseVoid(p); break;
            case RIL_REQUEST_GET_PREFERRED_NETWORK_TYPE: ret =  responseGetPreferredNetworkType(p); break;
            case RIL_REQUEST_GET_NEIGHBORING_CELL_IDS: ret = responseCellList(p); break;
            case RIL_REQUEST_SET_LOCATION_UPDATES: ret =  responseVoid(p); break;
            case RIL_REQUEST_CDMA_SET_SUBSCRIPTION_SOURCE: ret =  responseVoid(p); break;
            case RIL_REQUEST_CDMA_SET_ROAMING_PREFERENCE: ret =  responseVoid(p); break;
            case RIL_REQUEST_CDMA_QUERY_ROAMING_PREFERENCE: ret =  responseInts(p); break;
            case RIL_REQUEST_SET_TTY_MODE: ret =  responseVoid(p); break;
            case RIL_REQUEST_QUERY_TTY_MODE: ret =  responseInts(p); break;
            case RIL_REQUEST_CDMA_SET_PREFERRED_VOICE_PRIVACY_MODE: ret =  responseVoid(p); break;
            case RIL_REQUEST_CDMA_QUERY_PREFERRED_VOICE_PRIVACY_MODE: ret =  responseInts(p); break;
            case RIL_REQUEST_CDMA_FLASH: ret =  responseVoid(p); break;
            case RIL_REQUEST_CDMA_BURST_DTMF: ret =  responseVoid(p); break;
            case RIL_REQUEST_CDMA_SEND_SMS: ret =  responseSMS(p); break;
            case RIL_REQUEST_CDMA_SMS_ACKNOWLEDGE: ret =  responseVoid(p); break;
            case RIL_REQUEST_GSM_GET_BROADCAST_CONFIG: ret =  responseGmsBroadcastConfig(p); break;
            case RIL_REQUEST_GSM_SET_BROADCAST_CONFIG: ret =  responseVoid(p); break;
            case RIL_REQUEST_GSM_BROADCAST_ACTIVATION: ret =  responseVoid(p); break;
            case RIL_REQUEST_CDMA_GET_BROADCAST_CONFIG: ret =  responseCdmaBroadcastConfig(p); break;
            case RIL_REQUEST_CDMA_SET_BROADCAST_CONFIG: ret =  responseVoid(p); break;
            case RIL_REQUEST_CDMA_BROADCAST_ACTIVATION: ret =  responseVoid(p); break;
            case RIL_REQUEST_CDMA_VALIDATE_AND_WRITE_AKEY: ret =  responseVoid(p); break;
            case RIL_REQUEST_CDMA_SUBSCRIPTION: ret =  responseStrings(p); break;
            case RIL_REQUEST_CDMA_WRITE_SMS_TO_RUIM: ret =  responseInts(p); break;
            case RIL_REQUEST_CDMA_DELETE_SMS_ON_RUIM: ret =  responseVoid(p); break;
            case RIL_REQUEST_DEVICE_IDENTITY: ret =  responseStrings(p); break;
            case RIL_REQUEST_GET_SMSC_ADDRESS: ret = responseString(p); break;
            case RIL_REQUEST_SET_SMSC_ADDRESS: ret = responseVoid(p); break;
            case RIL_REQUEST_EXIT_EMERGENCY_CALLBACK_MODE: ret = responseVoid(p); break;
            case RIL_REQUEST_REPORT_SMS_MEMORY_STATUS: ret = responseVoid(p); break;
            case RIL_REQUEST_REPORT_STK_SERVICE_IS_RUNNING: ret = responseVoid(p); break;
            case RIL_REQUEST_CDMA_GET_SUBSCRIPTION_SOURCE: ret =  responseVoid(p); break;
            case RIL_REQUEST_GET_DATA_CALL_PROFILE: ret =  responseGetDataCallProfile(p); break;
            case RIL_REQUEST_ISIM_AUTHENTICATION: ret =  responseString(p); break;
            case RIL_REQUEST_ACKNOWLEDGE_INCOMING_GSM_SMS_WITH_PDU: ret = responseVoid(p); break;
            case RIL_REQUEST_STK_SEND_ENVELOPE_WITH_STATUS: ret = responseICC_IO(p); break;
            case RIL_REQUEST_VOICE_RADIO_TECH: ret = responseInts(p); break;
            case RIL_REQUEST_GET_CELL_INFO_LIST: ret = responseCellInfoList(p); break;
            case RIL_REQUEST_SET_UNSOL_CELL_INFO_LIST_RATE: ret = responseVoid(p); break;
            case RIL_REQUEST_SET_INITIAL_ATTACH_APN: ret = responseVoid(p); break;
            case RIL_REQUEST_SET_DATA_PROFILE: ret = responseVoid(p); break;
            case RIL_REQUEST_IMS_REGISTRATION_STATE: ret = responseInts(p); break;
            case RIL_REQUEST_IMS_SEND_SMS: ret =  responseSMS(p); break;
            case RIL_REQUEST_SIM_TRANSMIT_APDU_BASIC: ret =  responseICC_IO(p); break;
            case RIL_REQUEST_SIM_OPEN_CHANNEL: ret  = responseInts(p); break;
            case RIL_REQUEST_SIM_CLOSE_CHANNEL: ret  = responseVoid(p); break;
            case RIL_REQUEST_SIM_TRANSMIT_APDU_CHANNEL: ret = responseICC_IO(p); break;
            case RIL_REQUEST_SIM_GET_ATR: ret = responseString(p); break;
            case RIL_REQUEST_NV_READ_ITEM: ret = responseString(p); break;
            case RIL_REQUEST_NV_WRITE_ITEM: ret = responseVoid(p); break;
            case RIL_REQUEST_NV_WRITE_CDMA_PRL: ret = responseVoid(p); break;
            case RIL_REQUEST_NV_RESET_CONFIG: ret = responseVoid(p); break;
            case RIL_REQUEST_SET_UICC_SUBSCRIPTION: ret = responseVoid(p); break;
            case RIL_REQUEST_ALLOW_DATA: ret = responseVoid(p); break;
            case RIL_REQUEST_GET_HARDWARE_CONFIG: ret = responseHardwareConfig(p); break;
            case RIL_REQUEST_SIM_AUTHENTICATION: ret =  responseICC_IOBase64(p); break;
            case RIL_REQUEST_SHUTDOWN: ret = responseVoid(p); break;
            default:
                throw new RuntimeException("Unrecognized solicited response: " + rr.mRequest);
            //break;
            }} catch (Throwable tr) {
                // Exceptions here usually mean invalid RIL responses

                Rlog.w(RILJ_LOG_TAG, rr.serialString() + "< "
                        + requestToString(rr.mRequest)
                        + " exception, possible invalid RIL response", tr);

                if (rr.mResult != null) {
                    AsyncResult.forMessage(rr.mResult, null, tr);
                    rr.mResult.sendToTarget();
                }
                return rr;
            }
        }

        if (error != 0) {
            //ugly fix for Samsung messing up SMS_SEND request fail in binary RIL
            if(!(error == -1 && rr.mRequest == RIL_REQUEST_SEND_SMS))
            {
                rr.onError(error, ret);
                return rr;
            } else {
                try
                {
                    ret =  responseSMS(p);
                } catch (Throwable tr) {
                    Rlog.w(RILJ_LOG_TAG, rr.serialString() + "< "
                            + requestToString(rr.mRequest)
                            + " exception, Processing Samsung SMS fix ", tr);
                    rr.onError(error, ret);
                    return rr;
                }
            }
        }

        if (RILJ_LOGD) riljLog(rr.serialString() + "< " + requestToString(rr.mRequest)
            + " " + retToString(rr.mRequest, ret));

        if (rr.mResult != null) {
            AsyncResult.forMessage(rr.mResult, ret, null);
            rr.mResult.sendToTarget();
        }
        return rr;
    }

    @Override
    public void
    dial(String address, int clirMode, UUSInfo uusInfo, Message result) {

        // We want to take care of this
        if (PhoneNumberUtils.isEmergencyNumber(address)) {
            dialEmergencyCall(address, clirMode, result);
            return;
        }

        // Everything below comes from super class
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_DIAL, result);
        rr.mParcel.writeString(address);
        rr.mParcel.writeInt(clirMode);
        rr.mParcel.writeInt(0); // UUS information is absent

        if (uusInfo == null) {
            rr.mParcel.writeInt(0); // UUS information is absent
        } else {
            rr.mParcel.writeInt(1); // UUS information is present
            rr.mParcel.writeInt(uusInfo.getType());
            rr.mParcel.writeInt(uusInfo.getDcs());
            rr.mParcel.writeByteArray(uusInfo.getUserData());
        }

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        send(rr);
    }

    public void
    dialEmergencyCall(String address, int clirMode, Message result) {
        Rlog.v(RILJ_LOG_TAG, "Emergency dial: " + address);

        RILRequest rr = RILRequest.obtain(RIL_REQUEST_DIAL_EMERGENCY, result);
        rr.mParcel.writeString(address + "/");
        rr.mParcel.writeInt(clirMode);
        rr.mParcel.writeInt(0);
        rr.mParcel.writeInt(0);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        send(rr);
    }

    @Override
    protected void
    processUnsolicited (Parcel p) {
        Object ret;
        int dataPosition = p.dataPosition();
        int response = p.readInt();

        switch (response) {
            case RIL_UNSOL_RESPONSE_RADIO_STATE_CHANGED: ret =  responseVoid(p); break;
            case RIL_UNSOL_RESPONSE_NEW_BROADCAST_SMS: ret = responseString(p); break;
            case RIL_UNSOL_RIL_CONNECTED: ret = responseInts(p); break;
            // SAMSUNG STATES
            case RIL_UNSOL_AM: ret = responseString(p); break;
            case RIL_UNSOL_DUN_PIN_CONTROL_SIGNAL: ret = responseVoid(p); break;
            case RIL_UNSOL_DATA_SUSPEND_RESUME: ret = responseInts(p); break;
            case RIL_UNSOL_STK_CALL_CONTROL_RESULT: ret = responseVoid(p); break;
            case RIL_UNSOL_TWO_MIC_STATE: ret = responseInts(p); break;
            case RIL_UNSOL_WB_AMR_STATE: ret = responseInts(p); break;

            default:
                // Rewind the Parcel
                p.setDataPosition(dataPosition);

                // Forward responses that we are not overriding to the super class
                super.processUnsolicited(p);
                return;
        }

        switch (response) {
            case RIL_UNSOL_RESPONSE_RADIO_STATE_CHANGED:
                /* has bonus radio state int */
                int state = p.readInt();
                Rlog.d(RILJ_LOG_TAG, "Radio state: " + state);

                switch (state) {
                    case 2:
                        // RADIO_UNAVAILABLE
                        state = 1;
                        break;
                    case 3:
                        // RADIO_ON
                        state = 10;
                        break;
                    case 4:
                        // RADIO_ON
                        state = 10;
                        // When SIM is PIN-unlocked, RIL doesn't respond with RIL_UNSOL_RESPONSE_SIM_STATUS_CHANGED.
                        // We notify the system here.
                        Rlog.d(RILJ_LOG_TAG, "SIM is PIN-unlocked now");
                        if (mIccStatusChangedRegistrants != null) {
                            mIccStatusChangedRegistrants.notifyRegistrants();
                        }
                        break;
                }
                RadioState newState = getRadioStateFromInt(state);
                Rlog.d(RILJ_LOG_TAG, "New Radio state: " + state + " (" + newState.toString() + ")");
                switchToRadioState(newState);
                break;
            case RIL_UNSOL_RESPONSE_NEW_BROADCAST_SMS:
                if (RILJ_LOGD) unsljLogRet(response, ret);

                if (mGsmBroadcastSmsRegistrant != null) {
                    mGsmBroadcastSmsRegistrant
                        .notifyRegistrant(new AsyncResult(null, ret, null));
                }
                break;
            case RIL_UNSOL_RIL_CONNECTED:
                if (RILJ_LOGD) unsljLogRet(response, ret);

                // Initial conditions
                setRadioPower(false, null);
                setCdmaSubscriptionSource(mCdmaSubscription, null);
                notifyRegistrantsRilConnectionChanged(((int[])ret)[0]);
                break;
            // SAMSUNG STATES
            case RIL_UNSOL_AM:
                if (RILJ_LOGD) samsungUnsljLogRet(response, ret);
                String amString = (String) ret;
                Rlog.d(RILJ_LOG_TAG, "Executing AM: " + amString);

                try {
                    Runtime.getRuntime().exec("am " + amString);
                } catch (IOException e) {
                   e.printStackTrace();
                    Rlog.e(RILJ_LOG_TAG, "am " + amString + " could not be executed.");
                }
                break;
            case RIL_UNSOL_DUN_PIN_CONTROL_SIGNAL:
                if (RILJ_LOGD) samsungUnsljLogRet(response, ret);
                break;
            case RIL_UNSOL_DATA_SUSPEND_RESUME:
                if (RILJ_LOGD) samsungUnsljLogRet(response, ret);
                break;
            case RIL_UNSOL_STK_CALL_CONTROL_RESULT:
                if (RILJ_LOGD) samsungUnsljLogRet(response, ret);
                break;
            case RIL_UNSOL_TWO_MIC_STATE:
                if (RILJ_LOGD) samsungUnsljLogRet(response, ret);
                break;
            case RIL_UNSOL_WB_AMR_STATE:
                if (RILJ_LOGD) samsungUnsljLogRet(response, ret);
                setWbAmr(((int[])ret)[0]);
                break;
        }
    }

    static String
    samsungResponseToString(int request)
    {
        switch(request) {
            // SAMSUNG STATES
            case RIL_UNSOL_AM: return "RIL_UNSOL_AM";
            case RIL_UNSOL_DUN_PIN_CONTROL_SIGNAL: return "RIL_UNSOL_DUN_PIN_CONTROL_SIGNAL";
            case RIL_UNSOL_DATA_SUSPEND_RESUME: return "RIL_UNSOL_DATA_SUSPEND_RESUME";
            case RIL_UNSOL_STK_CALL_CONTROL_RESULT: return "RIL_UNSOL_STK_CALL_CONTROL_RESULT";
            case RIL_UNSOL_TWO_MIC_STATE: return "RIL_UNSOL_TWO_MIC_STATE";
            case RIL_UNSOL_WB_AMR_STATE: return "RIL_UNSOL_WB_AMR_STATE";
            default: return "<unknown response: "+request+">";
        }
    }

    protected void samsungUnsljLog(int response) {
        riljLog("[UNSL]< " + samsungResponseToString(response));
    }

    protected void samsungUnsljLogMore(int response, String more) {
        riljLog("[UNSL]< " + samsungResponseToString(response) + " " + more);
    }

    protected void samsungUnsljLogRet(int response, Object ret) {
        riljLog("[UNSL]< " + samsungResponseToString(response) + " " + retToString(response, ret));
    }

    protected void samsungUnsljLogvRet(int response, Object ret) {
        riljLogv("[UNSL]< " + samsungResponseToString(response) + " " + retToString(response, ret));
    }

    /**
     * Set audio parameter "wb_amr" for HD-Voice (Wideband AMR).
     *
     * @param state: 0 = unsupported, 1 = supported.
     */
    private void setWbAmr(int state) {
        if (state == 1) {
            Rlog.d(RILJ_LOG_TAG, "setWbAmr(): setting audio parameter - wb_amr=on");
            audioManager.setParameters("wb_amr=on");
        } else {
            Rlog.d(RILJ_LOG_TAG, "setWbAmr(): setting audio parameter - wb_amr=off");
            audioManager.setParameters("wb_amr=off");
        }
    }

    @Override
    protected Object
    responseCallList(Parcel p) {
        int num;
        boolean isVideo;
        ArrayList<DriverCall> response;
        DriverCall dc;
        int dataAvail = p.dataAvail();
        int pos = p.dataPosition();
        int size = p.dataSize();

        Rlog.d(RILJ_LOG_TAG, "Parcel size = " + size);
        Rlog.d(RILJ_LOG_TAG, "Parcel pos = " + pos);
        Rlog.d(RILJ_LOG_TAG, "Parcel dataAvail = " + dataAvail);

        //Samsung changes
        num = p.readInt();

        Rlog.d(RILJ_LOG_TAG, "num = " + num);
        response = new ArrayList<DriverCall>(num);

        for (int i = 0 ; i < num ; i++) {

            dc                      = new DriverCall();
            dc.state                = DriverCall.stateFromCLCC(p.readInt());
            dc.index                = p.readInt();
            dc.TOA                  = p.readInt();
            dc.isMpty               = (0 != p.readInt());
            dc.isMT                 = (0 != p.readInt());
            dc.als                  = p.readInt();
            dc.isVoice              = (0 != p.readInt());
            isVideo                 = (0 != p.readInt());
            dc.isVoicePrivacy       = (0 != p.readInt());
            dc.number               = p.readString();
            int np                  = p.readInt();
            dc.numberPresentation   = DriverCall.presentationFromCLIP(np);
            dc.name                 = p.readString();
            dc.namePresentation     = p.readInt();
            int uusInfoPresent      = p.readInt();

            Rlog.d(RILJ_LOG_TAG, "state = " + dc.state);
            Rlog.d(RILJ_LOG_TAG, "index = " + dc.index);
            Rlog.d(RILJ_LOG_TAG, "state = " + dc.TOA);
            Rlog.d(RILJ_LOG_TAG, "isMpty = " + dc.isMpty);
            Rlog.d(RILJ_LOG_TAG, "isMT = " + dc.isMT);
            Rlog.d(RILJ_LOG_TAG, "als = " + dc.als);
            Rlog.d(RILJ_LOG_TAG, "isVoice = " + dc.isVoice);
            Rlog.d(RILJ_LOG_TAG, "isVideo = " + isVideo);
            Rlog.d(RILJ_LOG_TAG, "number = " + dc.number);
            Rlog.d(RILJ_LOG_TAG, "np = " + np);
            Rlog.d(RILJ_LOG_TAG, "name = " + dc.name);
            Rlog.d(RILJ_LOG_TAG, "namePresentation = " + dc.namePresentation);
            Rlog.d(RILJ_LOG_TAG, "uusInfoPresent = " + uusInfoPresent);

            if (uusInfoPresent == 1) {
                dc.uusInfo = new UUSInfo();
                dc.uusInfo.setType(p.readInt());
                dc.uusInfo.setDcs(p.readInt());
                byte[] userData = p.createByteArray();
                dc.uusInfo.setUserData(userData);
                Rlog.v(RILJ_LOG_TAG, String.format("Incoming UUS : type=%d, dcs=%d, length=%d",
                        dc.uusInfo.getType(), dc.uusInfo.getDcs(),
                        dc.uusInfo.getUserData().length));
                Rlog.v(RILJ_LOG_TAG, "Incoming UUS : data (string)="
                        + new String(dc.uusInfo.getUserData()));
                Rlog.v(RILJ_LOG_TAG, "Incoming UUS : data (hex): "
                        + IccUtils.bytesToHexString(dc.uusInfo.getUserData()));
            } else {
                Rlog.v(RILJ_LOG_TAG, "Incoming UUS : NOT present!");
           }

            // Make sure there's a leading + on addresses with a TOA of 145
            dc.number = PhoneNumberUtils.stringFromStringAndTOA(dc.number, dc.TOA);

            response.add(dc);

            if (dc.isVoicePrivacy) {
                mVoicePrivacyOnRegistrants.notifyRegistrants();
                Rlog.d(RILJ_LOG_TAG, "InCall VoicePrivacy is enabled");
            } else {
                mVoicePrivacyOffRegistrants.notifyRegistrants();
                Rlog.d(RILJ_LOG_TAG, "InCall VoicePrivacy is disabled");
            }
        }

        Collections.sort(response);

        return response;
    }

    @Override
    protected Object responseGetPreferredNetworkType(Parcel p) {
        int [] response = (int[]) responseInts(p);

        if (response.length >= 1) {
            // Since this is the response for getPreferredNetworkType
            // we'll assume that it should be the value we want the
            // vendor ril to take if we reestablish a connection to it.
            mPreferredNetworkType = response[0];
        }

        // When the modem responds Phone.NT_MODE_GLOBAL, it means Phone.NT_MODE_WCDMA_PREF
        if (response[0] == Phone.NT_MODE_GLOBAL) {
            Rlog.d(RILJ_LOG_TAG, "Overriding network type response from GLOBAL to WCDMA preferred");
            response[0] = Phone.NT_MODE_WCDMA_PREF;
        }

        return response;
   }

    @Override
    protected Object
    responseSignalStrength(Parcel p) {
       int numInts = 12;
        int response[];
        boolean isGsm = true;

        // Get raw data
        response = new int[numInts];
        for (int i = 0 ; i < numInts ; i++) {
            response[i] = p.readInt();
        }

        /*
        Rlog.d(RILJ_LOG_TAG, "gsmSignalStrength=" + response[0]);
        Rlog.d(RILJ_LOG_TAG, "gsmBitErrorRate=" + response[1]);
        Rlog.d(RILJ_LOG_TAG, "cdmaDbm=" + response[2]);
        Rlog.d(RILJ_LOG_TAG, "cdmaEcio=" + response[3]);
        Rlog.d(RILJ_LOG_TAG, "evdoDbm=" + response[4]);
        Rlog.d(RILJ_LOG_TAG, "evdoEcio=" + response[5]);
        Rlog.d(RILJ_LOG_TAG, "evdoSnr=" + response[6]);
        Rlog.d(RILJ_LOG_TAG, "lteSignalStrength=" + response[7]);
        Rlog.d(RILJ_LOG_TAG, "lteRsrp=" + response[8]);
        Rlog.d(RILJ_LOG_TAG, "lteRsrq=" + response[9]);
        Rlog.d(RILJ_LOG_TAG, "lteRssnr=" + response[10]);
        Rlog.d(RILJ_LOG_TAG, "lteCqi=" + response[11]);
        */

        int mGsmSignalStrength = response[0]; // Valid values are (0-31, 99) as defined in TS 27.007 8.5
        Rlog.d(RILJ_LOG_TAG, "responseSignalStrength (raw): gsmSignalStrength=" + mGsmSignalStrength);
        mGsmSignalStrength = mGsmSignalStrength & 0xff; // Get the first 8 bits
        Rlog.d(RILJ_LOG_TAG, "responseSignalStrength (corrected): gsmSignalStrength=" + mGsmSignalStrength);

        /* if mGsmSignalStrength isn't a valid value, use mCdmaDbm as fallback */
        if (mGsmSignalStrength < 0 || (mGsmSignalStrength > 31 && response[0] != 99)) {
            int mCdmaDbm = response[2];
            Rlog.d(RILJ_LOG_TAG, "responseSignalStrength-fallback (raw): gsmSignalStrength=" + mCdmaDbm);

	        if (mCdmaDbm < 0) {
	            mGsmSignalStrength = 99;
	        } else if (mCdmaDbm > 31 && mCdmaDbm != 99) {
	            mGsmSignalStrength = 31;
	        } else {
	            mGsmSignalStrength = mCdmaDbm;
	        }
            Rlog.d(RILJ_LOG_TAG, "responseSignalStrength-fallback (corrected): gsmSignalStrength=" + mGsmSignalStrength);
        }

        SignalStrength signalStrength = new SignalStrength(mGsmSignalStrength, response[1], response[2],
                    response[3], response[4], response[5], response[6], isGsm);

        return signalStrength;
    }

    @Override public void
    getVoiceRadioTechnology(Message result) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_VOICE_RADIO_TECH, result);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        // RIL versions below 7 do not support this request
        if (mRilVersion >= 7)
            send(rr);
        else
            Rlog.d(RILJ_LOG_TAG, "RIL_REQUEST_VOICE_RADIO_TECH blocked!!!");
    }

    @Override
    public void getCdmaSubscriptionSource(Message response) {
        RILRequest rr = RILRequest.obtain(
                RILConstants.RIL_REQUEST_CDMA_GET_SUBSCRIPTION_SOURCE, response);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        Rlog.d(RILJ_LOG_TAG, "RIL_REQUEST_CDMA_GET_SUBSCRIPTION_SOURCE blocked!!!");
        //send(rr);
    }
 }
