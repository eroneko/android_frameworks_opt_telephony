/*
 * Copyright (C) 2006 The Android Open Source Project
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

package com.android.internal.telephony.dataconnection;

import android.app.PendingIntent;
import android.telephony.Rlog;

import com.android.internal.telephony.DctConstants;
import com.android.internal.telephony.Phone;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Maintain the Apn context
 */
public class ApnContext {

    public final String LOG_TAG;

    protected static final boolean DBG = false;

    private final String mDataProfileType;

    private DctConstants.State mState;

    private ArrayList<DataProfile> mWaitingDataProfiles = null;

    /** A zero indicates that all waiting APNs had a permanent error */
    private AtomicInteger mWaitingApnsPermanentFailureCountDown;

    private DataProfile mDataProfile;

    DcAsyncChannel mDcAc;

    String mReason;

    PendingIntent mReconnectAlarmIntent;

    /**
     * user/app requested connection on this APN
     */
    AtomicBoolean mDataEnabled;

    /**
     * carrier requirements met
     */
    AtomicBoolean mDependencyMet;

    public ApnContext(String DataProfileType, String logTag) {
        mDataProfileType = DataProfileType;
        mState = DctConstants.State.IDLE;
        setReason(Phone.REASON_DATA_ENABLED);
        mDataEnabled = new AtomicBoolean(false);
        mDependencyMet = new AtomicBoolean(true);
        mWaitingApnsPermanentFailureCountDown = new AtomicInteger(0);
        LOG_TAG = logTag;
    }

    public String getDataProfileType() {
        return mDataProfileType;
    }

    public synchronized DcAsyncChannel getDcAc() {
        return mDcAc;
    }

    public synchronized void setDataConnectionAc(DcAsyncChannel dcac) {
        if (DBG) {
            log("setDataConnectionAc: old dcac=" + mDcAc + " new dcac=" + dcac
                    + " this=" + this);
        }
        mDcAc = dcac;
    }

    public synchronized PendingIntent getReconnectIntent() {
        return mReconnectAlarmIntent;
    }

    public synchronized void setReconnectIntent(PendingIntent intent) {
        mReconnectAlarmIntent = intent;
    }

    public synchronized DataProfile getDataProfile() {
        log("getApnSetting: mDataProfile=" + mDataProfile);
        return mDataProfile;
    }

    public synchronized void setDataProfile(DataProfile dataProfile) {
        log("setApnSetting: mDataProfile=" + dataProfile);
        mDataProfile = dataProfile;
    }

    public synchronized void setWaitingDataProfiles(ArrayList<DataProfile> waitingDataProfiles) {
        mWaitingDataProfiles = waitingDataProfiles;
        mWaitingApnsPermanentFailureCountDown.set(mWaitingDataProfiles.size());
    }

    public int getWaitingApnsPermFailCount() {
        return mWaitingApnsPermanentFailureCountDown.get();
    }

    public void decWaitingApnsPermFailCount() {
        mWaitingApnsPermanentFailureCountDown.decrementAndGet();
    }

    public synchronized DataProfile getNextWaitingApn() {
        ArrayList<DataProfile> list = mWaitingDataProfiles;
        DataProfile apn = null;

        if (list != null) {
            if (!list.isEmpty()) {
                apn = list.get(0);
            }
        }
        return apn;
    }

    public synchronized void removeWaitingApn(DataProfile apn) {
        if (mWaitingDataProfiles != null) {
            mWaitingDataProfiles.remove(apn);
        }
    }

    public synchronized ArrayList<DataProfile> getWaitingApns() {
        return mWaitingDataProfiles;
    }

    public synchronized void setState(DctConstants.State s) {
        if (DBG) {
            log("setState: " + s + ", previous state:" + mState);
        }

        mState = s;

        if (mState == DctConstants.State.FAILED) {
            if (mWaitingDataProfiles != null) {
                mWaitingDataProfiles.clear(); // when teardown the connection and set to IDLE
            }
        }
    }

    public synchronized DctConstants.State getState() {
        return mState;
    }

    public boolean isDisconnected() {
        DctConstants.State currentState = getState();
        return ((currentState == DctConstants.State.IDLE) ||
                    currentState == DctConstants.State.FAILED);
    }

    public synchronized void setReason(String reason) {
        if (DBG) {
            log("set reason as " + reason + ",current state " + mState);
        }
        mReason = reason;
    }

    public synchronized String getReason() {
        return mReason;
    }

    public boolean isReady() {
        return mDataEnabled.get() && mDependencyMet.get();
    }

    public boolean isConnectable() {
        return isReady() && ((mState == DctConstants.State.IDLE)
                                || (mState == DctConstants.State.SCANNING)
                                || (mState == DctConstants.State.RETRYING)
                                || (mState == DctConstants.State.FAILED));
    }

    public void setEnabled(boolean enabled) {
        if (DBG) {
            log("set enabled as " + enabled + ", current state is " + mDataEnabled.get());
        }
        mDataEnabled.set(enabled);
    }

    public boolean isEnabled() {
        return mDataEnabled.get();
    }

    public void setDependencyMet(boolean met) {
        if (DBG) {
            log("set mDependencyMet as " + met + " current state is " + mDependencyMet.get());
        }
        mDependencyMet.set(met);
    }

    public boolean getDependencyMet() {
       return mDependencyMet.get();
    }

    @Override
    public synchronized String toString() {
        // We don't print mDataConnection because its recursive.
        return "{mApnType=" + mDataProfileType + " mState=" + getState() +
                " mWaitingDataProfiles={" + mWaitingDataProfiles +
                "} mWaitingApnsPermanentFailureCountDown=" + mWaitingApnsPermanentFailureCountDown +
                " mDataProfile={" + mDataProfile + "} mReason=" + mReason +
                " mDataEnabled=" + mDataEnabled + " mDependencyMet=" + mDependencyMet + "}";
    }

    protected void log(String s) {
        Rlog.d(LOG_TAG, "[ApnContext:" + mDataProfileType + "] " + s);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("ApnContext: " + this.toString());
    }
}
