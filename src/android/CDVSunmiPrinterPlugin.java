package com.stc;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class CDVSunmiPrinterPlugin extends CordovaPlugin {

    private static final String LOG_TAG = "CDVSunmiPrinterPlugin";

    private static final String ACTION_PRINT_BASE64 = "printBase64";

    /**
     * Constructor.
     */
    public CDVSunmiPrinterPlugin() {
    }

    /**
     * Executes the request and returns PluginResult.
     *
     * @param action            The action to execute.
     * @param args              JSONArray of arguments for the plugin.
     * @param callbackContext   The callback context used when calling back into JavaScript.
     * @return                  True when the action was valid, false otherwise.
     */
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    	if (action.equals(ACTION_PRINT_BASE64)) {
            this.printBase64(args.getString(0), callbackContext);
        }
        else {
            return false;
        }
        
        return true;
    }

    public synchronized void printBase64(final String base64, final CallbackContext callbackContext) {
        final CordovaInterface cordova = this.cordova;

        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                SunmiPrinterPlugin.getInstance(cordova.getActivity(), new SunmiPrinterPlugin.InstanceCallback() {
                    @Override
                    public void onInstance(SunmiPrinterPlugin instance) {
                        if(instance == null) {
                            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, "Could not get instance"));
                            return;
                        }

                        instance.printBase64(base64, new SunmiPrinterPlugin.PrintCallback() {
                            @Override
                            public void onSuccess() {
                                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, "Print Finished"));
                            }

                            @Override
                            public void onError(String error) {
                                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, error));
                            }
                        });
                    }
                });
            }
        });
    }
}
