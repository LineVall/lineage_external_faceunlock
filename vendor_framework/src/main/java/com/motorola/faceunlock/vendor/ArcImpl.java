package com.motorola.faceunlock.vendor;

import static org.pixelexperience.faceunlock.vendor.constants.FaceConstants.MG_UNLOCK_ATTR_EYE_CLOSE;
import static org.pixelexperience.faceunlock.vendor.constants.FaceConstants.MG_UNLOCK_COMPARE_FAILURE;
import static org.pixelexperience.faceunlock.vendor.constants.FaceConstants.MG_UNLOCK_DARKLIGHT;
import static org.pixelexperience.faceunlock.vendor.constants.FaceConstants.MG_UNLOCK_FACE_BLUR;
import static org.pixelexperience.faceunlock.vendor.constants.FaceConstants.MG_UNLOCK_FACE_MULTI;
import static org.pixelexperience.faceunlock.vendor.constants.FaceConstants.MG_UNLOCK_FACE_NOT_COMPLETE;
import static org.pixelexperience.faceunlock.vendor.constants.FaceConstants.MG_UNLOCK_FACE_NOT_FOUND;
import static org.pixelexperience.faceunlock.vendor.constants.FaceConstants.MG_UNLOCK_FACE_SCALE_TOO_LARGE;
import static org.pixelexperience.faceunlock.vendor.constants.FaceConstants.MG_UNLOCK_KEEP;
import static org.pixelexperience.faceunlock.vendor.constants.FaceConstants.MG_UNLOCK_LIVENESS_WARNING;

import android.content.Context;
import android.util.Log;

import org.pixelexperience.faceunlock.vendor.constants.AppConstants;
import org.pixelexperience.faceunlock.vendor.utils.SharedUtil;
import org.pixelexperience.faceunlock.vendor.VendorFaceManager;

import java.io.File;
import java.util.UUID;

public class ArcImpl extends VendorFaceManager {
    private static final String TAG = ArcImpl.class.getSimpleName();
    private static final boolean DEBUG = true;

    static {
        try {
            System.loadLibrary("arcsoft-lib");
        } catch (Throwable th) {
            Log.e("TAG", "load arcsoft error " + th);
        }
    }

    private final Context mContext;
    private final SharedUtil mShareUtil;
    private SERVICE_STATE mCurrentState = SERVICE_STATE.INITING;

    public ArcImpl(Context context) {
        mContext = context;
        mShareUtil = new SharedUtil(mContext);
    }

    private native int arcCompare(byte[] bArr, int i, int i2);

    private native int arcCompareStart();

    private native int arcCompareStop();

    private native int arcGetFeatureCount();

    private native int arcInit(String str, String str2);

    private native int arcRelease();

    private native int arcRemoveFeature();

    private native int arcSaveFeature(byte[] bArr, int i, int i2);

    private native int arcSaveFeatureStart();

    private native int arcSaveFeatureStop();

    private int convertErrorCode(int i) {
        if (i == 0) {
            return 0;
        }
        if (i == 28690) {
            return MG_UNLOCK_FACE_NOT_COMPLETE;
        }
        switch (i) {
            case ArcConstant.ARC_FI_MERR_MULTIFACE /* 28673 */:
                return MG_UNLOCK_FACE_MULTI;
            case ArcConstant.ARC_FI_MERR_NO_FACE /* 28674 */:
                return MG_UNLOCK_FACE_NOT_FOUND;
            case ArcConstant.ARC_FI_MERR_NOT_ALIVE /* 28675 */:
                return MG_UNLOCK_LIVENESS_WARNING;
            case ArcConstant.ARC_FI_MERR_ABNORMAL_FACE /* 28676 */:
            case ArcConstant.ARC_FI_MERR_FEATURE_DETECTFAIL /* 28677 */:
                return MG_UNLOCK_FACE_BLUR;
            case ArcConstant.ARC_FI_MERR_RECOGNIZE_FAIL /* 28678 */:
                return MG_UNLOCK_COMPARE_FAILURE;
            default:
                switch (i) {
                    case ArcConstant.ARC_FI_MERR_NOT_FRONTAL_FACE /* 28680 */:
                        return MG_UNLOCK_FACE_BLUR;
                    case ArcConstant.ARC_FI_MERR_DARK_FACE /* 28681 */:
                        return MG_UNLOCK_DARKLIGHT;
                    case ArcConstant.ARC_FI_MERR_EYE_CLOSED /* 28682 */:
                        return MG_UNLOCK_ATTR_EYE_CLOSE;
                    case ArcConstant.ARC_FI_MERR_FACE_TOO_CLOSE /* 28683 */:
                        return MG_UNLOCK_FACE_SCALE_TOO_LARGE;
                    case ArcConstant.ARC_FI_MERR_PART_FACE /* 28684 */:
                    case ArcConstant.ARC_FI_MERR_MOUTH_SHELTERED /* 28686 */:
                    case ArcConstant.ARC_FI_MERR_EYES_SHELTERED /* 28687 */:
                        return MG_UNLOCK_FACE_NOT_COMPLETE;
                    case ArcConstant.ARC_FI_MERR_PROCESSING /* 28685 */:
                        return MG_UNLOCK_KEEP;
                    default:
                        return i;
                }
        }
    }

    @Override
    public void init() {
        String stringValueByKey = mShareUtil.getStringValueByKey(AppConstants.SHARED_KEY_UUID);
        if (stringValueByKey == null) {
            stringValueByKey = UUID.randomUUID().toString();
            mShareUtil.saveStringValue(AppConstants.SHARED_KEY_UUID, stringValueByKey);
        }
        File file = new File(mContext.getFilesDir(), "face_data");
        if (file.exists() || file.mkdirs()) {
            File file2 = new File(file, "facedata.bin");
            if (!file2.exists() && mShareUtil.getIntValueByKey(AppConstants.SHARED_KEY_FACE_ID) > 0) {
                Log.e(TAG, "face data missing!");
            } else if (arcInit(stringValueByKey, file2.getAbsolutePath()) == 0) {
                mCurrentState = SERVICE_STATE.IDLE;
            }
        } else {
            Log.e(TAG, "face data dir create failed!");
        }
    }

    @Override
    public int getFeatureCount() {
        return arcGetFeatureCount();
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
            arcCompareStart();
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
            int arcCompare = arcCompare(bArr, i, i2);
            if (DEBUG) {
                Log.i(TAG, "compare finish: " + arcCompare);
            }
            if (arcCompare == 0) {
                compareStop();
            }
            return convertErrorCode(arcCompare);
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
            arcCompareStop();
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
            arcSaveFeatureStart();
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
            int arcSaveFeature = arcSaveFeature(bArr, i, i2);
            if (arcSaveFeature == 0) {
                iArr[0] = 0;
            }
            if (DEBUG) {
                Log.i(TAG, "saveFeature");
            }
            return convertErrorCode(arcSaveFeature);
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
            arcSaveFeatureStop();
            mCurrentState = SERVICE_STATE.IDLE;
        }
    }

    @Override
    public void setDetectArea(int i, int i2, int i3, int i4) {
        synchronized (this) {
            if (DEBUG) {
                Log.i(TAG, "setDetectArea start");
            }
        }
    }

    @Override
    public void deleteFeature(int i) {
        synchronized (this) {
            if (DEBUG) {
                Log.i(TAG, "deleteFeature start");
            }
            arcRemoveFeature();
            if (DEBUG) {
                Log.i(TAG, "deleteFeature stop");
            }
            release();
        }
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
            arcRelease();
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
        UNLOCKING
    }
}