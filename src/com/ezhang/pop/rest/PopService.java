package com.ezhang.pop.rest;
/**
 * 2011 Foxykeep (http://datadroid.foxykeep.com)
 * <p>
 * Licensed under the Beerware License : <br />
 * As long as you retain this notice you can do whatever you want with this stuff. If we meet some
 * day, and you think this stuff is worth it, you can buy me a beer in return
 */


import com.foxykeep.datadroid.service.RequestService;
import com.ezhang.pop.rest.PopRequestManager;

import android.content.Intent;

/**
 * This class is called by the {@link PopRequestManager} through the {@link Intent} system.
 *
 * @author Foxykeep
 */
public final class PopService extends RequestService {
	
	@Override  
    public void onCreate() {  
        super.onCreate();  
    }
	
	@Override  
    public void onStart(Intent intent, int startId) {  
        super.onStart(intent, startId);
	}

    // TODO by default only one concurrent worker thread will be used. If you want to change that,
    // override the getMaximumNumberOfThreads() method
	@Override
    protected int getMaximumNumberOfThreads() {
        return 5;
    }

    @Override
    public Operation getOperationForType(int requestType) {
        switch (requestType) {
            // TODO : Add a case per worker where you do the following things :
            // - create the corresponding Operation and return it
            // See the PoC if you need more information.
	        case PopRequestFactory.REQ_TYPE_DISTANCE_MATRIX:
	            return new DistanceMatrixQueryOperation();
	        case PopRequestFactory.REQ_TYPE_GET_CUR_SUBURB:
	        	return new CurrentSuburbQueryOpertion();
	        case PopRequestFactory.REQ_TYPE_FUEL:
	        	return new FuelInfoOperation();
        }
        return null;
    }
}
