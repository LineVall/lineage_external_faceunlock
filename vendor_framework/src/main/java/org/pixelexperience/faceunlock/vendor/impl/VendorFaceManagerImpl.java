package org.pixelexperience.faceunlock.vendor.impl;

import android.content.Context;

import org.pixelexperience.faceunlock.vendor.VendorFaceManager;
import com.motorola.faceunlock.vendor.ArcImpl;
import org.pixelexperience.faceunlock.vendor.impl.megvii.FacePPImpl;

public class VendorFaceManagerImpl extends VendorFaceManager {

    private final VendorFaceManager mFaceManager;

    public VendorFaceManagerImpl(Context context, boolean useAlternativeImpl){
        mFaceManager = useAlternativeImpl ? new FacePPImpl(context) : new ArcImpl(context);
    }

    @Override
    public int compare(byte[] bArr, int i, int i2, int i3, boolean z, boolean z2, int[] iArr) {
        return mFaceManager.compare(bArr, i, i2, i3, z, z2, iArr);
    }

    @Override
    public void compareStart() {
        mFaceManager.compareStart();
    }

    @Override
    public void compareStop() {
        mFaceManager.compareStop();
    }

    @Override
    public void deleteFeature(int i) {
        mFaceManager.deleteFeature(i);
    }

    @Override
    public int getFeatureCount() {
        return mFaceManager.getFeatureCount();
    }

    @Override
    public void init() {
        mFaceManager.init();
    }

    @Override
    public void release() {
        mFaceManager.release();
    }

    @Override
    public int saveFeature(byte[] bArr, int i, int i2, int i3, boolean z, byte[] bArr2, byte[] bArr3, int[] iArr) {
        return mFaceManager.saveFeature(bArr, i, i2, i3, z, bArr2, bArr3, iArr);
    }

    @Override
    public void saveFeatureStart() {
        mFaceManager.saveFeatureStart();
    }

    @Override
    public void saveFeatureStop() {
        mFaceManager.saveFeatureStop();
    }

    @Override
    public void setDetectArea(int i, int i2, int i3, int i4) {
        mFaceManager.setDetectArea(i, i2, i3, i4);
    }
}
