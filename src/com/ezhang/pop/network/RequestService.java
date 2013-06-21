package com.ezhang.pop.network;
/**
 * 2011 Foxykeep (http://datadroid.foxykeep.com)
 * <p>
 * Licensed under the Beerware License : <br />
 * As long as you retain this notice you can do whatever you want with this stuff. If we meet some
 * day, and you think this stuff is worth it, you can buy me a beer in return
 */

import android.content.Intent;
import com.ezhang.pop.RequestOperations.CurrentAddressQueryOperation;
import com.ezhang.pop.RequestOperations.DistanceMatrixQueryOperation;
import com.ezhang.pop.RequestOperations.FuelInfoOperation;

/**
 * This class is called by the {@link RequestManager} through the {@link Intent} system.
 *
 * @author Foxykeep
 */
public final class RequestService extends com.foxykeep.datadroid.service.RequestService {
    // TODO by default only one concurrent worker thread will be used. If you want to change that,
    // override the getMaximumNumberOfThreads() method
	@Override
    protected int getMaximumNumberOfThreads() {
        return 2;
    }

    @Override
    public Operation getOperationForType(int requestType) {
        switch (requestType) {
            // - create the corresponding Operation and return it
            // See the PoC if you need more information.
	        case RequestFactory.REQ_TYPE_DISTANCE_MATRIX:
	            return new DistanceMatrixQueryOperation();
	        case RequestFactory.REQ_TYPE_GET_CUR_SUBURB:
	        	return new CurrentAddressQueryOperation();
	        case RequestFactory.REQ_TYPE_FUEL:
	        	return new FuelInfoOperation();
        }
        return null;
    }
}
