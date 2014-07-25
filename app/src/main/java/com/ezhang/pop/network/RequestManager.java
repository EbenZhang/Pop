package com.ezhang.pop.network;
/**
 * 2 * 2011 Foxykeep (http://datadroid.foxykeep.com)
 * <p>
 * Licensed under the Beerware License : <br />
 * As long as you retain this notice you can do whatever you want with this stuff. If we meet some
 * day, and you think this stuff is worth it, you can buy me a beer in return
 */


import android.content.Context;

/**
 * This class is used as a proxy to call the RequestService. It provides easy-to-use methods to call the
 * RequestService and manages the Intent creation. It also assures that a request will not be sent again if
 * an exactly identical one is already in progress.
 *
 * @author Foxykeep
 */
public final class RequestManager extends com.foxykeep.datadroid.requestmanager.RequestManager {

    // Singleton management
    private static RequestManager sInstance;

    public static RequestManager from(Context context) {
        if (sInstance == null) {
            sInstance = new RequestManager(context);
        }

        return sInstance;
    }

    // TODO change the SkeletonService to your RequestService subclass
    private RequestManager(Context context) {
        super(context, RequestService.class);
    }
}
