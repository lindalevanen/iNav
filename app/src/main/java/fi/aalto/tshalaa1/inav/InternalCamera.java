package fi.aalto.tshalaa1.inav;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.camera2.CaptureResult;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import fi.aalto.tshalaa1.inav.entities.Building;
import fi.aalto.tshalaa1.inav.entities.Point3D;
import fi.aalto.tshalaa1.inav.entities.Route;
import fi.aalto.tshalaa1.inav.entities.SensorRunnable;

import static android.widget.RelativeLayout.*;

/**
 * Created by tshalaa1 on 8/15/16.
 * Taken from tutorial...
 * http://inducesmile.com/android/android-camera2-api-example-tutorial/
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class InternalCamera extends Fragment implements Navigator.NavigatorListener {
    private static final String TAG = "AndroidCameraApi";
    public ImageButton takePictureButton;
    private AutoFitTextureView textureView;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }
    private String cameraId;
    protected CameraDevice cameraDevice;
    protected CameraCaptureSession cameraCaptureSessions;
    protected CaptureRequest captureRequest;
    protected CaptureRequest.Builder captureRequestBuilder;
    private Size imageDimension;
    private ImageReader imageReader;
    private File file;
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private boolean mFlashSupported;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;
    private View camView;
    private DisplayMetrics displayMetrics = Utils.getContext().getResources().getDisplayMetrics();
    int width = displayMetrics.widthPixels;
    int height = displayMetrics.heightPixels;
    private SensorDataHandler sda;

    @Override
    public void onRouteUpdated(Route route) {
        //mSectionsPagerAdapter.onRouteUpdated(route);
        //locateButton.setText(getString(R.string.action_relocate));
        //elsewhereButton.setText(getString(R.string.action_new_destination));
        System.out.print(" ");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        camView = inflater.inflate(R.layout.camera_layout, container, false);
        super.onCreateView(inflater, container, savedInstanceState);

        sda = new SensorDataHandler();
        sda.setAcceleroRunnable(new SensorRunnable() {
            @Override
            public void run() {
//                System.out.println("accelero stuff: " + x + ", " + y + ", "+ z);
            }
        });
        sda.setMagnRunnable(new SensorRunnable() {
            @Override
            public void run() {
//                System.out.println("magnet stuff: " + x + ", " + y + ", "+ z);
            }
        });
        sda.start(false, false, true, true);

        textureView = (AutoFitTextureView) camView.findViewById(R.id.texture);
        assert textureView != null;
        textureView.setSurfaceTextureListener(textureListener);
        takePictureButton = (ImageButton) camView.findViewById(R.id.btn_takepicture);
        assert takePictureButton != null;
        takePictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println("clicking to take pic");
                System.out.println("checking AF state: " + CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED);

                takePicture();
            }
        });
//        sda.setICam(this);

        adjustAspectRatio(displayMetrics.widthPixels,displayMetrics.heightPixels);

        return camView;
    }

    /**
     * Sets the TextureView transform to preserve the aspect ratio of the video.
     */
    private void adjustAspectRatio(int videoWidth, int videoHeight) {
        int viewWidth = textureView.getWidth();
        int viewHeight = textureView.getHeight();
        double aspectRatio = (double) videoHeight / videoWidth;

        int newWidth, newHeight;
        if (viewHeight > (int) (viewWidth * aspectRatio)) {
            // limited by narrow width; restrict height
            newWidth = viewWidth;
            newHeight = (int) (viewWidth * aspectRatio);
        } else {
            // limited by short height; restrict width
            newWidth = (int) (viewHeight / aspectRatio);
            newHeight = viewHeight;
        }
        int xoff = (viewWidth - newWidth) / 2;
        int yoff = (viewHeight - newHeight) / 2;

        Matrix txform = new Matrix();
        textureView.getTransform(txform);
        txform.setScale((float) newWidth / viewWidth, (float) newHeight / viewHeight);
        //txform.postRotate(10);          // just for fun
        txform.postTranslate(xoff, yoff);
        textureView.setTransform(txform);
    }

    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            //open your camera here
            openCamera();
        }
        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            // Transform you image captured size according to the surface width and height
        }
        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }
        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };
    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            //This is called when the camera is open
            Log.e(TAG, "onOpened");
            cameraDevice = camera;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            System.out.println("disconnecting");
            cameraDevice.close();
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            System.out.println("onError");
            cameraDevice.close();
            cameraDevice = null;
        }
    };
    final CameraCaptureSession.CaptureCallback captureCallbackListener = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
//            Toast.makeText(Utils.getContext(), "Saved:" + file, Toast.LENGTH_SHORT).show();

            createCameraPreview();
        }
    };
    protected void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }
    protected void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * the main folder in which the app stores data
     */
    private final File NAVIGATION_CLIENT_MAIN_FOLDER = new File(Environment.getExternalStorageDirectory(), "iNav Client");

    /* current photo file used by the app to temporarily store a photo before it is actually saved into the gallery
    */
    private File currentPhoto = null;

    /**
     * the last successful photo
     */
    private File lastPhoto = null;

    /**
     * a variable for storing the JSON data sent along with images in location requests.
     * This variable might be set by reading JSON from a file (gallery request), or by collecting new data when taking fresh pictures.
     * Either way, it will be passed to the navigator at a new request.
     */
    private String lastJSON = null;

    private Navigator navigator;

    private byte[] bs;

    public void setNavigator(Navigator nav){
        this.navigator = nav;
        this.navigator.setNavigatorListener(this);
    }
    private static final int STATE_PREVIEW = 0;
    private int mState = STATE_PREVIEW;
    protected void takePicture() {
        if(null == cameraDevice) {
            Log.e(TAG, "cameraDevice is null");
            return;
        }
        CameraManager manager = (CameraManager) Utils.getContext().getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
            Size[] jpegSizes = null;
            if (characteristics != null) {
                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
            }
            int width = 1280;
            int height = 720;
            int tempWidth = 1280;
            int tempHeight = 720;
            if (jpegSizes != null && 0 < jpegSizes.length) {

                //Sets JPEG size to 1280x720 or lower if phone does not support this wanted size
                //Choosing optimal size
                //https://developer.android.com/samples/Camera2Basic/src/com.example.android.camera2basic/Camera2BasicFragment.html#l326
                for(int i = 1; i < jpegSizes.length; i ++){
                   if(jpegSizes[(jpegSizes.length - i)].getWidth() <= tempWidth && jpegSizes[(jpegSizes.length - i)].getHeight() <= tempHeight) {
                        width = jpegSizes[(jpegSizes.length - i)].getWidth();
                        height = jpegSizes[(jpegSizes.length - i)].getHeight();
                    }
                }

                System.out.println("height and width: " + width + " " + height);
                System.out.println("height and width array: " + Arrays.toString(jpegSizes));
            }
            ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
            List<Surface> outputSurfaces = new ArrayList<Surface>(2);
            outputSurfaces.add(reader.getSurface());
            outputSurfaces.add(new Surface(textureView.getSurfaceTexture()));
            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());
            System.out.println("is cameraDevice null " + cameraDevice);
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
//            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE); //Testing
            captureBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START);
            // Orientation
            int rotation = Utils.getContext().getResources().getConfiguration().orientation;//(WindowManager) getContext().getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));
            System.out.println("is rotation null " + rotation);

           ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = null;
                    try {
//                        image = reader.acquireLatestImage();
                        image = reader.acquireNextImage();

                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[buffer.capacity()];
                        buffer.get(bytes);
                        bs = bytes;

//                        ((MainActivity) getActivity()).setVisible(true);

                        /**
                         * a new JSONDataCollector is initiated to collect the necessary data to be sent to the server along with the image
                         * read JSONDataCollector.java for specifics.
                         *
                         * A callback method sends the data to the server once it's all collected.
                         */
                        final Image finalImage = image;
                        new JSONDataCollector(false, false, false, true, false) {
                            @Override
                            void onSuccessCallback(JSONObject data) {
                                lastJSON = data.toString();
                                Log.d(TAG, "onActivityResult: got sensor data while taking a photo:" + lastJSON);
                                //save lastJSON to file
//                                Utils.saveStringToFolder(Utils.getContext(), sensorFileName, lastJSON);
                                Log.d(TAG, "sensors info passed onto Location request from NavFragm " + lastJSON);
//                            Toast.makeText(Utils.getContext(), "Wi-Fi data collected! " + lastJSON, Toast.LENGTH_SHORT).show();
                                System.out.println("not good but printing image bytes from cam intent: " + bs.length );

                                navigator.setStart(bs, lastJSON);

                            }
                        };

                        /////



                        save(bytes);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (image != null) {
                            image.close();
                        }
                    }
                }
                private void save(byte[] bytes) throws IOException {
                    OutputStream output = null;
                    String imgId = Utils.getDeviceId() + "_" + System.currentTimeMillis();
//                    Navigation Client
                    if (!NAVIGATION_CLIENT_MAIN_FOLDER.exists())
                        NAVIGATION_CLIENT_MAIN_FOLDER.mkdirs();

                    File currentPhoto=new File(NAVIGATION_CLIENT_MAIN_FOLDER, imgId + ".jpg");

//                    if (currentPhoto.exists()) {
//                        currentPhoto.delete();
//                    }


                    try {
                        output = new FileOutputStream(currentPhoto.getPath());
                        output.write(bytes);
                        output.flush();
                        output.close();
                    } finally {
                        if (null != output) {
                            output.close();
                            MediaScannerConnection.scanFile(Utils.getContext(), new String[] { currentPhoto.getPath() }, null, new MediaScannerConnection.OnScanCompletedListener() {
                                /*
                                 *   (non-Javadoc)
                                 * @see android.media.MediaScannerConnection.OnScanCompletedListener#onScanCompleted(java.lang.String, android.net.Uri)
                                 */
                                public void onScanCompleted(String path, Uri uri)
                                {
                                    Log.i("ExternalStorage", "Scanned " + path + ":");
                                    Log.i("ExternalStorage", "-> uri=" + uri);
                                }
                            });
                        }
                    }
                }

            };
            reader.setOnImageAvailableListener(readerListener, mBackgroundHandler);
            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {

//                private void process(CaptureResult result) {
//                    switch (mState) {
//                        case STATE_PREVIEW: {
//
//                            int afState = result.get(CaptureResult.CONTROL_AF_STATE);
//                            if (CaptureResult.CONTROL_AF_TRIGGER_START == afState) {
//                                if (areWeFocused) {
//                                    //Run specific task here
//                                }
//                            }
//                            if (CaptureResult.CONTROL_AF_STATE_PASSIVE_FOCUSED == afState) {
//                                areWeFocused = true;
//                            } else {
//                                areWeFocused = false;
//                            }
//
//                            break;
//                        }
//                    }
//                }

                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
//                    Toast.makeText(Utils.getContext(), "Saved:" + file, Toast.LENGTH_SHORT).show();
                    int afState = result.get(CaptureResult.CONTROL_AF_STATE);
                    System.out.println("afState: " + afState);
                    if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState) {
                        Log.d("SOME KIND OF FOCUS", "WE HAVE");
                    }
//                    process(result);
                    createCameraPreview();
                }
            };
            cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        session.capture(captureBuilder.build(), captureListener, mBackgroundHandler);
                        closeCamera();
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                }
            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }




    }

    /**
     * Callback method that fires after a succesful location request.
     *
     On successful location request go back to Navigation Fragment
     * @param location: the received location.
     */
    @Override
    public void onLocated(Point3D location) {

        if(location == null && textureView != null) {
            openCamera();
            System.out.println("opening camera");
        } else {
            if(getFragmentManager() != null)
                getFragmentManager().popBackStackImmediate();
        }
    }


    protected void createCameraPreview() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            Surface surface = new Surface(texture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback(){
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    //The camera is already closed
                    if (null == cameraDevice) {
                        return;
                    }
                    // When the session is ready, we start displaying the preview.
                    cameraCaptureSessions = cameraCaptureSession;
                    updatePreview();
                }
                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(Utils.getContext(), "Configuration change", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void openCamera() {
        CameraManager manager = (CameraManager) Utils.getContext().getSystemService(Context.CAMERA_SERVICE);
        Log.e(TAG, "is camera open");
        if(sda != null)
            sda.picTaken = true;
//        sda.updateOrientationAngles();
        try {
            cameraId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
            // Add permission for camera and let user grant the permission
            if (ActivityCompat.checkSelfPermission(Utils.getContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(Utils.getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions((Activity) Utils.getContext(), new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_PERMISSION);
                return;
            }
            manager.openCamera(cameraId, stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Log.e(TAG, "openCamera X");
    }
    protected void updatePreview() {
        if(null == cameraDevice) {
            Log.e(TAG, "updatePreview error, return");
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        try {
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    private void closeCamera() {
        if (null != cameraDevice) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (null != imageReader) {
            imageReader.close();
            imageReader = null;
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                // close the app
                Toast.makeText(Utils.getContext(), "Sorry!!!, you can't use this app without granting permission", Toast.LENGTH_LONG).show();
//                finish();
            }
        }
    }
    @Override
    public void onResume() {
        super.onResume();
        Log.e(TAG, "onResume");
        startBackgroundThread();
        if (textureView.isAvailable()) {
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }
    }
    @Override
    public void onPause() {
        Log.e(TAG, "onPause");
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }
}
