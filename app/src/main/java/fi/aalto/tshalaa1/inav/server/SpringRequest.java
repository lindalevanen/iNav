package fi.aalto.tshalaa1.inav.server;

import java.util.concurrent.Callable;

/**
 * Created by tshalaa1 on 6/17/16.
 */
public abstract class SpringRequest<R> implements Callable<R> {

    private static final String SERVER_URL = "http://saimaa.netlab.hut.fi:5004";

    public String getUrl(String node) {
        String url = SERVER_URL;
        if (node.charAt(0) != '/') {
            url += "/";
        }
        return url + node;
    }

}
