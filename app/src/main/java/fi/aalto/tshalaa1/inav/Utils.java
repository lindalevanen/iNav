package fi.aalto.tshalaa1.inav;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.util.TypedValue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

import fi.aalto.tshalaa1.inav.entities.Point3D;

/**
 * Created by tshalaa1 on 6/17/16.
 */
public class Utils {

    private final static String TAG = "Utils";
    private static Handler handler;

    private static Context context;

    private static String deviceId;

    static {
        handler = new Handler(Looper.getMainLooper());
    }

    public static void runOnUiThread(Runnable r) {
        handler.post(r);
    }

    public static void setContext(MainActivity context) {
        Utils.context = context;

        if (deviceId == null)
            deviceId = Settings.Secure.getString(context.getContentResolver(),Settings.Secure.ANDROID_ID);
    }

    public static Context getContext() {
        return context;
    }

    public static String getDeviceId() {
        return deviceId;
    }

    public static void saveStringToFolder(Context context, String sFileName, String sBody) {
        try {
            File root = new File(Environment.getExternalStorageDirectory(), "Navigation Client");
            if (!root.exists()) {
                root.mkdirs();
            }
            File gpxfile = new File(root, sFileName);
            FileWriter writer = new FileWriter(gpxfile);
            writer.append(sBody);
            writer.flush();
            writer.close();
            Log.i(TAG, "saveStringToFolder suceed.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static byte[] bytesFromStream(InputStream is ) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int max = 512;
        byte[] buffer = new byte[max];
        int read = 0;
        while (read != -1) {
            read = is.read(buffer);
            if (read != -1) {
                bos.write(buffer, 0, read);
            }
        }
        return bos.toByteArray();
    }

    public static float dpToPx(Context c, float dp) {

        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                dp, c.getResources().getDisplayMetrics());
    }


    public static void galleryAddPic(Context context, File f) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);

        System.out.println("gallerpic: "+(f==null));
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        context.sendBroadcast(mediaScanIntent);
    }


    public static File renameFile(File f, String newName) {
        File returnFile = new File(f.getAbsolutePath().replace(f.getName(),newName));

        if (f.renameTo(returnFile))
            return returnFile;
        else
            return null;
    }

    public static void writeFile(File file, String content) {
        FileOutputStream fos = null;// = new FileOutputStream(file);
        try {
            fos = new FileOutputStream(file);
            fos.write(content.getBytes());
        } catch (Exception e) {
            // TODO Auto-generated catch block
        }
    }

    public static String readFile(File file) {
        StringBuilder sb = new StringBuilder();
        FileInputStream inputStream;
        try {
            inputStream = new FileInputStream(file);

            int i;
            while ( (i = inputStream.read()) != -1) {
                sb.append((char)i);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return sb.toString();
    }

    public static float Point3DtoDegrees(Point3D p) throws Exception {
        if (p.getRotX() != 0) {

            double d = Math.toDegrees(Math.atan(p.getRotZ() / p.getRotX()));
            d += ((p.getRotX()>0) ? 90f : 270f);

            return (float)d;
        } else if (p.getRotX()==0) {
            return (p.getRotZ()>0) ? 180f : 0f;
        } else {
            //both are zero
            throw new Exception("Rotation vector was the null vector.");
        }

    }
}
