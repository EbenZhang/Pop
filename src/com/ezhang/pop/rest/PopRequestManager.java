package com.ezhang.pop.rest;
/**
 * 2 * 2011 Foxykeep (http://datadroid.foxykeep.com)
 * <p>
 * Licensed under the Beerware License : <br />
 * As long as you retain this notice you can do whatever you want with this stuff. If we meet some
 * day, and you think this stuff is worth it, you can buy me a beer in return
 */


import com.foxykeep.datadroid.requestmanager.RequestManager;
import com.ezhang.pop.rest.PopService;

import android.content.Context;

/**
 * This class is used as a proxy to call the Service. It provides easy-to-use methods to call the
 * service and manages the Intent creation. It also assures that a request will not be sent again if
 * an exactly identical one is already in progress.
 *
 * @author Foxykeep
 */
public final class PopRequestManager extends RequestManager {

    // Singleton management
    private static PopRequestManager sInstance;

    public static PopRequestManager from(Context context) {
        if (sInstance == null) {
            sInstance = new PopRequestManager(context);
        }

        return sInstance;
    }

    // TODO change the SkeletonService to your RequestService subclass
    private PopRequestManager(Context context) {
        super(context, PopService.class);
    }
}
