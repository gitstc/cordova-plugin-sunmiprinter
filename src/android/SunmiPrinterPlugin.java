package com.stc;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.RemoteException;
import android.util.Base64;
import android.util.Log;

import com.sunmi.peripheral.printer.InnerPrinterCallback;
import com.sunmi.peripheral.printer.InnerPrinterManager;
import com.sunmi.peripheral.printer.InnerResultCallback;
import com.sunmi.peripheral.printer.SunmiPrinterService;

public class SunmiPrinterPlugin {
    private static final String TAG = "SunmiPrinterPlugin";
    private static SunmiPrinterService sunmiPrinterService = null;
    private static Context context;
    private static SunmiPrinterPlugin instance;

    public static synchronized void getInstance(Context ctx, InstanceCallback callback) {
        if(instance == null) {
            context = ctx;
            instance = new SunmiPrinterPlugin();

            bindPrintService(new BindCallback() {
                @Override
                public void onConnect() {
                    callback.onInstance(instance);
                }

                @Override
                public void onDisconnect() {
                    callback.onInstance(null);
                }
            });

            return;
        }

        if(sunmiPrinterService != null) {
            callback.onInstance(instance);
        } else {
            callback.onInstance(null);
        }
    }

    private static void bindPrintService(BindCallback callback) {
        try {
            InnerPrinterManager.getInstance().bindService(context, new InnerPrinterCallback() {
                @Override
                protected void onConnected(SunmiPrinterService service) {
                    sunmiPrinterService = service;
                    callback.onConnect();
                    Log.d(TAG, "Printer Connected :)");
                }

                @Override
                protected void onDisconnected() {
                    sunmiPrinterService = null;
                    callback.onDisconnect();
                    Log.d(TAG, "Printer Disconnected :(");
                }
            });
        }
        catch (Exception ex) {
            Log.e(TAG, ex.getMessage());
            callback.onDisconnect();
        }
    }

    public void printBase64(String base64Image, PrintCallback callback) {
        if(sunmiPrinterService == null) {
            Log.e(TAG, "Printer Service is not initialized");
            return;
        }

        try {
            int printerState = sunmiPrinterService.updatePrinterState();
            Log.d(TAG, "Printer state " + printerState);
            if(printerState == 1) {
                byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
                Bitmap bitmapImage = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

                sunmiPrinterService.printBitmap(bitmapImage, new InnerResultCallback() {
                    //sunmiPrinterService.printText("Test Text", new InnerResultCallback() {
                    @Override
                    public void onRunResult(boolean isSuccess) throws RemoteException {
                        Log.d(TAG, "onRunResult " + isSuccess);
                        if(isSuccess) {
                            callback.onSuccess();
                        } else {
                            callback.onError("RUN_RESULT_ERROR");
                        }
                    }

                    @Override
                    public void onReturnString(String result) throws RemoteException {
                        Log.d(TAG, "onReturnString " + result);
                    }

                    @Override
                    public void onRaiseException(int code, String msg) throws RemoteException {
                        Log.d(TAG, "onRaiseException " + code + ", " + msg);
                        callback.onError(msg + "(" + code + ")");
                    }

                    @Override
                    public void onPrintResult(int code, String msg) throws RemoteException {
                        Log.d(TAG, "onPrintResult " + code + ", " + msg);
                    }
                });
                sunmiPrinterService.autoOutPaper(null);
            }
            else {
                switch(printerState) {
                    case 2: //Preparing printer
                        callback.onError("STATE_PREPARING_PRINTER");
                        break;
                    case 3: //Abnormal communication
                        callback.onError("STATE_ABNORMAL_COMMUNICATION");
                        break;
                    case 4: //Out of paper
                        callback.onError("STATE_OUT_OF_PAPER");
                        break;
                    case 5: //Overheated
                        callback.onError("STATE_OVERHEATED");
                        break;
                    case 6: //Open the lid
                        callback.onError("STATE_LID_OPEN");
                        break;
                    case 7: //The paper cutter is abnormal
                        callback.onError("STATE_PAPER_CUTTER_ABNORMAL");
                        break;
                    case 8: //The paper cutter has been recovered
                        callback.onError("STATE_PAPER_CUTTER_RECOVERED");
                        break;
                    case 9: //No black mark has been detected
                        callback.onError("STATE_NO_BLACK_MARK_DETECTED");
                        break;
                    case 505: //No printer has been detected
                        callback.onError("STATE_NO_PRINTER_DETECTED");
                        break;
                    case 507: //Failed to upgrade the printer firmware
                        callback.onError("STATE_FIRMWARE_UPGRADE_FAILURE");
                        break;
                }
            }
        }
        catch (RemoteException ex) {
            Log.e(TAG, "Remote Exception " + ex.getMessage());
            callback.onError("REMOTE_EXCEPTION " + ex.getMessage());
        }
        catch (Exception ex) {
            Log.e(TAG, "Unhandled Exception " + ex.getMessage());
            callback.onError("UNHANDLED_EXCEPTION " + ex.getMessage());
        }
    }

    public interface InstanceCallback {
        void onInstance(SunmiPrinterPlugin instance);
    }

    public interface BindCallback {
        public void onConnect();
        public void onDisconnect();
    }

    public interface PrintCallback {
        public void onSuccess();
        public void onError(String error);
    }
}
