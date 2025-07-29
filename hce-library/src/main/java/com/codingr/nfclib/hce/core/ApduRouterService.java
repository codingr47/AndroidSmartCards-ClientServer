package com.codingr.nfclib.hce.core;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.util.Log;

import com.codingr.nfclib.hce.annotations.ApduController;
import com.codingr.nfclib.hce.annotations.ApduMapping;
import com.codingr.nfclib.hce.util.ApduUtil;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dalvik.system.DexFile;

public class ApduRouterService extends HostApduService {

    private static final String TAG = "ApduRouterService";
    private static final byte[] SELECT_APDU_HEADER = {(byte) 0x00, (byte) 0xA4, (byte) 0x04, (byte) 0x00};

    private Object activeController;
    private Map<byte[], Method> apduHandlers = new HashMap<>();

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "HCE Service created.");
    }

    @Override
    public byte[] processCommandApdu(byte[] commandApdu, Bundle extras) {
        Log.d(TAG, "Received APDU: " + ApduUtil.bytesToHex(commandApdu));

        if (isSelectApdu(commandApdu)) {
            return handleSelectApdu(commandApdu);
        }

        if (activeController == null) {
            Log.e(TAG, "No active controller. Have you sent a SELECT APDU first?");
            return ApduUtil.SW_CONDITIONS_NOT_SATISFIED;
        }

        Method bestMatch = findBestMatch(commandApdu);

        if (bestMatch != null) {
            try {
                ApduResponse response = (ApduResponse) bestMatch.invoke(activeController, commandApdu);
                Log.d(TAG, "Responding with: " + ApduUtil.bytesToHex(response.toBytes()));
                return response.toBytes();
            } catch (Exception e) {
                Log.e(TAG, "Error invoking APDU handler", e);
                return ApduUtil.SW_CONDITIONS_NOT_SATISFIED;
            }
        } else {
            Log.w(TAG, "No handler found for APDU: " + ApduUtil.bytesToHex(commandApdu));
            return ApduUtil.SW_INS_NOT_SUPPORTED;
        }
    }

    @Override
    public void onDeactivated(int reason) {
        Log.i(TAG, "Service deactivated. Reason: " + reason);
        activeController = null;
        apduHandlers.clear();
    }

    private boolean isSelectApdu(byte[] commandApdu) {
        return commandApdu.length > 4 && Arrays.equals(Arrays.copyOfRange(commandApdu, 0, 4), SELECT_APDU_HEADER);
    }

    private byte[] handleSelectApdu(byte[] selectApdu) {
        int aidLength = selectApdu[4];
        byte[] aid = Arrays.copyOfRange(selectApdu, 5, 5 + aidLength);
        String aidHex = ApduUtil.bytesToHex(aid);
        Log.i(TAG, "SELECT APDU received for AID: " + aidHex);

        try {
            Class<?> controllerClass = findControllerForAid(aidHex);
            if (controllerClass != null) {
                activeController = controllerClass.getDeclaredConstructor().newInstance();
                loadHandlers(controllerClass);
                Log.i(TAG, "Activated controller: " + controllerClass.getName());
                return ApduUtil.SW_OK;
            } else {
                Log.w(TAG, "No controller found for AID: " + aidHex);
                return ApduUtil.SW_FILE_NOT_FOUND;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error activating controller for AID: " + aidHex, e);
            return ApduUtil.SW_CONDITIONS_NOT_SATISFIED;
        }
    }

    private Class<?> findControllerForAid(String aid) throws IOException, PackageManager.NameNotFoundException {
        for (String className : getAllClasses()) {
            try {
                Class<?> clazz = Class.forName(className);
                if (clazz.isAnnotationPresent(ApduController.class)) {
                    ApduController controllerAnnotation = clazz.getAnnotation(ApduController.class);
                    for (String supportedAid : controllerAnnotation.aids()) {
                        if (supportedAid.equalsIgnoreCase(aid)) {
                            return clazz;
                        }
                    }
                }
            } catch (ClassNotFoundException e) {
                // Ignore
            }
        }
        return null;
    }

    private void loadHandlers(Class<?> controllerClass) {
        apduHandlers.clear();
        for (Method method : controllerClass.getMethods()) {
            if (method.isAnnotationPresent(ApduMapping.class)) {
                ApduMapping mapping = method.getAnnotation(ApduMapping.class);
                apduHandlers.put(mapping.command(), method);
                Log.i(TAG, "Mapped command " + ApduUtil.bytesToHex(mapping.command()) + " to " + method.getName());
            }
        }
    }

    private List<String> getAllClasses() throws PackageManager.NameNotFoundException, IOException {
        List<String> classNames = new ArrayList<>();
        ApplicationInfo appInfo = getPackageManager().getApplicationInfo(getPackageName(), 0);
        String path = appInfo.sourceDir;
        DexFile dexfile = null;
        try {
            dexfile = new DexFile(path);
            java.util.Enumeration<String> entries = dexfile.entries();
            while (entries.hasMoreElements()) {
                String entry = entries.nextElement();
                if (entry.startsWith(getPackageName())) {
                    classNames.add(entry);
                }
            }
        } finally {
            if (dexfile != null) {
                dexfile.close();
            }
        }
        return classNames;
    }

    private Method findBestMatch(byte[] commandApdu) {
        Method bestMatch = null;
        int longestMatch = 0;

        for (Map.Entry<byte[], Method> entry : apduHandlers.entrySet()) {
            byte[] prefix = entry.getKey();
            if (commandApdu.length >= prefix.length && Arrays.equals(Arrays.copyOfRange(commandApdu, 0, prefix.length), prefix)) {
                if (prefix.length > longestMatch) {
                    longestMatch = prefix.length;
                    bestMatch = entry.getValue();
                }
            }
        }
        return bestMatch;
    }
}
