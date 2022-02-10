package org.pixelexperience.faceunlock.vendor.impl.megvii;


import android.content.Context;
import android.util.Log;

import org.pixelexperience.faceunlock.vendor.R;
import org.pixelexperience.faceunlock.vendor.VendorFaceManager;
import org.pixelexperience.faceunlock.vendor.constants.AppConstants;
import org.pixelexperience.faceunlock.vendor.utils.CustomUnlockEncryptor;
import org.pixelexperience.faceunlock.vendor.utils.SharedUtil;

import java.io.File;

public class FacePPImpl extends VendorFaceManager {
    private static final String SDK_VERSION = "1";
    private static final String TAG = FacePPImpl.class.getSimpleName();
    private static final boolean DEBUG = true;
    private final Context mContext;
    private final SharedUtil mShareUtil;
    private SERVICE_STATE mCurrentState = SERVICE_STATE.INITING;

    public FacePPImpl(Context context) {
        mContext = context;
        mShareUtil = new SharedUtil(mContext);
    }

    @Override
    public void init() {
        synchronized (this) {
            if (mCurrentState != SERVICE_STATE.INITING) {
                Log.d(TAG, " Has been init, ignore");
                return;
            }
            if (DEBUG) {
                Log.i(TAG, "init start");
            }
            boolean z = !SDK_VERSION.equals(mShareUtil.getStringValueByKey(AppConstants.SHARED_KEY_SDK_VERSION));
            File dir = mContext.getDir("faceunlock_data", 0);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            String raw = ConUtil.getRaw(mContext, R.raw.model_file, "model", "model_file", z);
            if (raw == null) {
                Log.e(TAG, "Unavalibale memory, init failed, stop self");
                return;
            }
            String raw2 = ConUtil.getRaw(mContext, R.raw.panorama_mgb, "model", "panorama_mgb", z);
            MegviiFaceUnlockImpl.getInstance().initHandle(dir.getAbsolutePath(), new CustomUnlockEncryptor());
            long initAllWithPath = MegviiFaceUnlockImpl.getInstance().initAllWithPath(raw2, "", raw);
            if (DEBUG) {
                Log.i(TAG, "init stop");
            }
            if (initAllWithPath != 0) {
                Log.e(TAG, "init failed, stop self");
                return;
            }
            if (z) {
                restoreFeature();
                mShareUtil.saveStringValue(AppConstants.SHARED_KEY_SDK_VERSION, SDK_VERSION);
            }
            mCurrentState = SERVICE_STATE.IDLE;
        }
    }

    private void restoreFeature() {
        if (DEBUG) {
            Log.i(TAG, "RestoreFeature");
        }
        synchronized (this) {
            MegviiFaceUnlockImpl.getInstance().prepare();
            MegviiFaceUnlockImpl.getInstance().restoreFeature();
            MegviiFaceUnlockImpl.getInstance().reset();
        }
    }

    @Override
    public void compareStart() {
        synchronized (this) {
            if (mCurrentState == SERVICE_STATE.INITING) {
                init();
            }
            if (mCurrentState == SERVICE_STATE.UNLOCKING) {
                return;
            }
            if (mCurrentState != SERVICE_STATE.IDLE) {
                Log.e(TAG, "unlock start failed: current state: " + mCurrentState);
                return;
            }
            if (DEBUG) {
                Log.i(TAG, "compareStart");
            }
            MegviiFaceUnlockImpl.getInstance().prepare();
            mCurrentState = SERVICE_STATE.UNLOCKING;
        }
    }

    @Override
    public int compare(byte[] bArr, int i, int i2, int i3, boolean z, boolean z2, int[] iArr) {
        synchronized (this) {
            if (mCurrentState != SERVICE_STATE.UNLOCKING) {
                Log.e(TAG, "compare failed: current state: " + mCurrentState);
                return -1;
            }
            int compare = MegviiFaceUnlockImpl.getInstance().compare(bArr, i, i2, i3, z, z2, iArr);
            if (DEBUG) {
                Log.i(TAG, "compare finish: " + compare);
            }
            if (compare == 0) {
                compareStop();
            }
            return compare;
        }
    }

    @Override
    public void compareStop() {
        synchronized (this) {
            if (mCurrentState != SERVICE_STATE.UNLOCKING) {
                Log.e(TAG, "compareStop failed: current state: " + mCurrentState);
                return;
            }
            if (DEBUG) {
                Log.i(TAG, "compareStop");
            }
            MegviiFaceUnlockImpl.getInstance().reset();
            mCurrentState = SERVICE_STATE.IDLE;
        }
    }

    @Override
    public void saveFeatureStart() {
        synchronized (this) {
            if (mCurrentState == SERVICE_STATE.INITING) {
                init();
            } else if (mCurrentState == SERVICE_STATE.UNLOCKING) {
                Log.e(TAG, "save feature, stop unlock");
                compareStop();
            }
            if (mCurrentState != SERVICE_STATE.IDLE) {
                Log.e(TAG, "saveFeatureStart failed: current state: " + mCurrentState);
            }
            if (DEBUG) {
                Log.i(TAG, "saveFeatureStart");
            }
            MegviiFaceUnlockImpl.getInstance().prepare();
            mCurrentState = SERVICE_STATE.ENROLLING;
        }
    }

    @Override
    public int saveFeature(byte[] bArr, int i, int i2, int i3, boolean z, byte[] bArr2, byte[] bArr3, int[] iArr) {
        synchronized (this) {
            if (mCurrentState != SERVICE_STATE.ENROLLING) {
                Log.e(TAG, "save feature failed , current state : " + mCurrentState);
                return -1;
            }
            if (DEBUG) {
                Log.i(TAG, "saveFeature");
            }
            return MegviiFaceUnlockImpl.getInstance().saveFeature(bArr, i, i2, i3, z, bArr2, bArr3, iArr);
        }
    }

    @Override
    public void saveFeatureStop() {
        synchronized (this) {
            if (mCurrentState != SERVICE_STATE.ENROLLING) {
                Log.d(TAG, "saveFeatureStop failed: current state: " + mCurrentState);
            }
            if (DEBUG) {
                Log.i(TAG, "saveFeatureStop");
            }
            MegviiFaceUnlockImpl.getInstance().reset();
            mCurrentState = SERVICE_STATE.IDLE;
        }
    }

    @Override
    public void setDetectArea(int i, int i2, int i3, int i4) {
        synchronized (this) {
            if (DEBUG) {
                Log.i(TAG, "setDetectArea start");
            }
            MegviiFaceUnlockImpl.getInstance().setDetectArea(i, i2, i3, i4);
        }
    }

    @Override
    public void deleteFeature(int i) {
        synchronized (this) {
            if (DEBUG) {
                Log.i(TAG, "deleteFeature start");
            }
            MegviiFaceUnlockImpl.getInstance().deleteFeature(i);
            if (DEBUG) {
                Log.i(TAG, "deleteFeature stop");
            }
            release();
        }
    }

    @Override
    public int getFeatureCount() {
        return 0;
    }

    @Override
    public void release() {
        synchronized (this) {
            if (mCurrentState == SERVICE_STATE.INITING) {
                if (DEBUG) {
                    Log.i(TAG, "has been released, ignore");
                }
                return;
            }
            if (DEBUG) {
                Log.i(TAG, "release start");
            }
            MegviiFaceUnlockImpl.getInstance().release();
            mCurrentState = SERVICE_STATE.INITING;
            if (DEBUG) {
                Log.i(TAG, "release stop");
            }
        }
    }

    private enum SERVICE_STATE {
        INITING,
        IDLE,
        ENROLLING,
        UNLOCKING,
        ERROR
    }
}
