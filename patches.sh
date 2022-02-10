sed -i "s/com.motorola.faceunlock.util.MotoUnlockEncryptor/faceunlock\/util\/FaceUnlockEncryptorDependencyLib/" prebuilt/lib64/libarcsoft-lib.so
sed -i "s/android.os.Build/faceunlock\/Build/" prebuilt/lib64/libarcsoft-lib.so
