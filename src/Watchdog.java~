package org.laneveraroja.watchdog;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import android.util.Log;

public class Watchdog extends CordovaPlugin {

	    
	@Override
	public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
		try {
		    if (action.equals("phonegapWatchdog")) {
		       String timer = args.getString(0);
		       
		       this.phonegapWatchdog(timer);
		 
		       callbackContext.success();
		       return true;
		    }
		    callbackContext.error("No such action defined");
		    return false;
		} catch(Exception e) {
		    callbackContext.error(e.getMessage());
		    return false;
		}

	}
	
    //--------------------------------------------------------------------------
    // LOCAL METHODS
    //--------------------------------------------------------------------------

	private String timer;
	
	public void phonegapWatchdog(String timer)
	{
		setTimerWatchdog(timer);
		Log.d("WatchDog", "Hora=" + timer); 				
	} 
	
	public void setTimerWatchdog(String timer)
	{
		this.timer = timer;
	}
	
	public String getTimerWatchdog()
	{
		return this.timer;
	}
}
