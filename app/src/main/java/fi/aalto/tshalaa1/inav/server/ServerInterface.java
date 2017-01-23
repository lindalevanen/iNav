package fi.aalto.tshalaa1.inav.server;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import fi.aalto.tshalaa1.inav.Utils;

/**
 * Created by tshalaa1 on 6/17/16.
 */
public class ServerInterface {

    public interface ServerCallback<R> {
        void onResult(R response);
    }

    private ExecutorService exec;

    public ServerInterface() {
        exec = Executors.newFixedThreadPool(4);
    }

    public void disconnect() {
        exec.shutdownNow();
    }

    public <R, T extends SpringRequest<R>> void process(final T request, final ServerCallback<R> callback) {

        exec.submit(new Runnable() {
            @Override
            public void run() {
                processRequest(request, callback);
            }
        });

    }

    private <R, T extends SpringRequest<R>>
    void processRequest(final T request, final ServerCallback<R> callback) {
//        Future<R> result = exec.submit(request);

        System.out.println("processing request...");

        R res = null;
        try {
            System.out.println("processing request call1...");
            res = request.call();
            System.out.println("processing request call2...");
        } catch (Exception e) {
            System.out.println("error....");
            System.out.println("what is the error: " + e.toString());
            e.printStackTrace();
        }


        final R cRes = res;
        System.out.println("callback initiated...");
        Utils.runOnUiThread(new Runnable(){
            @Override
            public void run() {
                callback.onResult(cRes);

            }
        });
    }
}
