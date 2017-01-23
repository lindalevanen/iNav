package fi.aalto.tshalaa1.inav;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import fi.aalto.tshalaa1.inav.entities.Bounds;
import fi.aalto.tshalaa1.inav.entities.Landmark;
import fi.aalto.tshalaa1.inav.entities.Minimap;
import fi.aalto.tshalaa1.inav.entities.PathPoint;
import fi.aalto.tshalaa1.inav.entities.Point3D;
import fi.aalto.tshalaa1.inav.entities.SensorRunnable;

/**
 * MinimapView class creates the view that shows the minimap and its contents.
 * It also handles all the onTouch-events and many other events that affect the minimapview.
 */
public class MinimapView extends ImageView implements Cloneable,SensorEventListener {

    private SensorManager sensorM;
    boolean activityRunning;

    private boolean isMapSet;
    //private Drawable map;//
    Bitmap map;
    private Bitmap userBitmap, unknownLandmark;
    SharedPreferences favPrefs;
    String prefsName;
    Minimap minimapClone;
    Landmark updatedLandmark;
    MinimapViewListener listener;

    public MinimapView(Context context) {
        super(context);
        init();
    }

    public MinimapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MinimapView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }



    private int height;
    private int width;// = 1080;

    private List<Point3D> path, visitedPath;
    private Paint paint, linePaint, visitedLine, visitedPoint, highlightPaint, bmpPaint;

    private List<Landmark> landmarks, favLandmarks;
    private Landmark destination;

    private Matrix scaleMatrix = new Matrix();
    private Matrix userMatrix = new Matrix();
    private Matrix lmMatrix = new Matrix();     //okay to remove?

    private float userRotation = 0f;
    public double offSetAngle;
    //private Point3D userPos;
    public Point3D userPos;
    public Point3D locationResponse;    //okay to remove?
    private PathPoint userPosOnScreen;  //okay to remove?
    public DrawerLayout drawerLO;
    public boolean draggingNavDrawer = false;
    public int areaID, buildingID;

    //variables used by the dynamic positioning system.
    final double stepthresh_min = 4;
    final double stepthresh_max = 9;
    double lastThreshHit = 0;
    float userRotationTemp;
    int accInterval = 0;
    int accIntervalCounter = 0;
    int semiThreshCounter = 0;
    int timeSinceThreshHit = 501;
    public float steplen = 0.7f; //meters
    SensorDataHandler dynamicPositionSensorReader = new SensorDataHandler();

    public static LinearLayout floorLO;
    static SlidingUpPanelLayout slideLO;
    static boolean errorOpen = false;
    static boolean markerEditOpen = false;

    private void init() {

        setScaleType(ScaleType.MATRIX);

        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        paint.setColor(Color.BLUE & 0x80ffffff);
        paint.setStrokeWidth(5.5f);

        linePaint = new Paint();
        linePaint.setAntiAlias(true);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setColor(Color.BLUE & 0x80ffffff);
        linePaint.setStrokeWidth(5.5f);

        visitedPoint = new Paint();
        visitedPoint.setAntiAlias(true);
        visitedPoint.setStyle(Paint.Style.FILL_AND_STROKE);
        visitedPoint.setColor(Color.GRAY & 0x80ffffff);
        visitedPoint.setStrokeWidth(5.5f);

        visitedLine = new Paint();
        visitedLine.setAntiAlias(true);
        visitedLine.setStyle(Paint.Style.STROKE);
        visitedLine.setPathEffect(new DashPathEffect(new float[]{10,20}, 0));
        visitedLine.setColor(Color.GRAY);
        visitedLine.setStrokeWidth(5.5f);

        highlightPaint = new Paint();
        highlightPaint.setAntiAlias(true);
        highlightPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        highlightPaint.setPathEffect(new DashPathEffect(new float[]{15, 5}, 0));
        highlightPaint.setColor(Color.RED & 0x80ffffff);
        highlightPaint.setStrokeWidth(4f);

        bmpPaint = new Paint();
        bmpPaint.setAntiAlias(true);

        userBitmap = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.ic_loca);
        unknownLandmark = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.ic_markerred);

        prefsName = ((MainActivity) getContext()).favPrefsName;
        updateLandmarks();
        //System.out.println("landmarklist in init: "+favLandmarks);

        SharedPreferences booleanPrefs = getContext().getSharedPreferences("booleanPrefs", Context.MODE_PRIVATE);

        PackageManager pm = Utils.getContext().getPackageManager();

        if (!booleanPrefs.contains("stepDetectionShown") && !pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_STEP_COUNTER)) {
            Toast.makeText(Utils.getContext(), "This device does not support step counter deteciton", Toast.LENGTH_SHORT).show();
            SharedPreferences.Editor editor = booleanPrefs.edit();
            editor.putBoolean("stepDetectionShown", true).apply();
        } else if (!booleanPrefs.contains("stepDetectionShown")) {
            sensorM = (SensorManager) Utils.getContext().getSystemService(Context.SENSOR_SERVICE);
            Sensor stepSensor = sensorM.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            if (stepSensor != null) {
                sensorM.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_FASTEST);
            } else {
                Toast.makeText(Utils.getContext(), "Step sensor not available", Toast.LENGTH_SHORT).show();
            }
        }

        //TODO: could the following lines be in their own methods?
        dynamicPositionSensorReader.setGyroRunnable(new SensorRunnable() {
            @Override
            public void run() {
                //System.out.println("cool story");
                double degrees = Math.toDegrees((double) (-z * (interval / 1000f)));

                if(userPos == null) {
                    offSetAngle += degrees;
                }

                if (Math.abs(degrees) > 0.07)
                    rotateUser((float) degrees);
            }
        });

        dynamicPositionSensorReader.setAccRunnable(new SensorRunnable() {
            @Override
            public void run() {

                if (accIntervalCounter < 10) {
                    accIntervalCounter++;
                    accInterval += this.interval;
                    return;
                } else if (accIntervalCounter == 10) {
                    accIntervalCounter = 11;
                    accInterval /= 10;
                }

                double acc = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2));

                if (acc >= stepthresh_min && acc <= stepthresh_max && (lastThreshHit == 0 || acc > lastThreshHit)) {
                    lastThreshHit = acc;
                    timeSinceThreshHit = 0;
                    semiThreshCounter = 0;

                    userRotationTemp = userRotation;

                    //System.out.println("acc: "+acc);
                } else if (lastThreshHit > 0) {
                    if (timeSinceThreshHit < 250 && semiThreshCounter < 3 && acc < lastThreshHit * 0.9 && acc > 0.5 * lastThreshHit && acc > 0.5 * stepthresh_min) {
                        semiThreshCounter += 1;
                        lastThreshHit = acc;
                    }

                    if (semiThreshCounter == 3) {
                        semiThreshCounter = 0;
                        lastThreshHit = 0;
                        stepFound();
                    }
                }

                if (timeSinceThreshHit > 500) {
                    lastThreshHit = 0;
                    semiThreshCounter = 0;
                }

                timeSinceThreshHit += this.interval;
            }
        });
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (activityRunning) {
            //System.out.println("STEP FOUND");
            stepFound();
        }
    }

    public interface MinimapViewListener {
        public void removeOtherLocalLMs(MinimapView mmv);
    }

    public void setMinimapViewListener(MinimapViewListener l) {
        this.listener = l;
    }

    // okay to remove?
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    // okay to remove?
    boolean isDynamicPositioningEnabled = false;

    public void startDynamicPositioning() {
        activityRunning = true;
        dynamicPositionSensorReader.start(true,true,false, false);
    }

    // okay to remove?
    public void stopDynamicPositioning() {
        activityRunning = false;
        dynamicPositionSensorReader.stop();
    }

    private void rotateUser(float rotation) {

        //TODO: remove below after testing
        rotation = 0;

        userRotation += rotation;
        //scaleMatrix.setRotate(rotation + 10);

        this.invalidate();

    }

    //maybe remove commented lines from the following methods if not needed?

    private void moveUser(float distance, float rotation) {
        float x = distance*(float)Math.cos(Math.toRadians(rotation));
        float y = distance*(float)Math.sin(Math.toRadians(rotation));

        userPos.setX(userPos.getX()+x);
        userPos.setZ(userPos.getZ()+y);
        //System.out.println("AFTER STEP TAKEN: " + userPos.getX() + " , " + userPos.getY());
        this.invalidate();

        //visitedPoints.add(0,new Point2D(userPos.getX(),userPos.getY()));
    }

    private void stepFound() {
        //stepCountTextView.setText("steps: "+stepCount++);
        if(userPos != null) {
            //45 degrees to account for initial rotation of map
            moveUser(steplen,userRotation + 45 + 180); //45
            //toggleDynamicPositioning();
        }

    }

//    public void setDrawableMiniMap(Drawable bm) {
//        isMapSet = true;
//        //map = bm;
//        scaleMatrix.setTranslate(400,60);
//        scaleMatrix.postRotate(45);
//        rotationDegrees = 0f;
//        scaleFactor = 1f;
//        startDynamicPositioning();
//        //this.setBackground(map);
//        this.invalidate();
//    }

    public void setMiniMap(Bitmap bm) {
        isMapSet = true;
        map = bm;
        scaleMatrix.setTranslate(200,10);
        scaleMatrix.postRotate(45);
        rotationDegrees = 0f;
        scaleFactor = 1f;
        startDynamicPositioning();
        this.invalidate();
    }

    // okay to remove?
    public Bitmap getMinimap() { return this.map; }

    public void passSlideLO(SlidingUpPanelLayout sLO) {
       slideLO = sLO;
    }

    public void passAreaID(int areaid) {
        this.areaID = areaid;
    }

    public void passBuildingID(int buildingID) { this.buildingID = buildingID; }

    public void passFloorLO(LinearLayout floorlo) { floorLO = floorlo; }

    // Minimapclone for the error and new landmark creation fragment.
    public void setMinimapClone(Minimap m) {
        minimapClone = m;
    }

    public void setLandmarks(List<Landmark> l, boolean openInfo) {
        this.landmarks = l;
        if(openInfo && !l.isEmpty()) { showMarkerInfo(l.get(0)); System.out.println("landmark location: "+l.get(0).getLocation()); }
        if(markerEditOpen) updatedLandmark = l.get(0);
        this.invalidate();
    }

    private PathPoint resolve(Point3D pt) {
        return new PathPoint(pt.getX() * pxPerMeterX + offsetX, pt.getZ() * pxPerMeterY + offsetY);
//        return new PathPoint(pt.getX()*10 + offsetX, pt.getZ()*10 + offsetY);
    }

    private Point3D unresolveLM(PathPoint pp) {
        return new Point3D((pp.getX() - offsetX) / pxPerMeterX,1,(pp.getY() - offsetY) / pxPerMeterY);
    }

    private double pxPerMeterX, pxPerMeterY;
    private double offsetX, offsetY;

    Bounds bounds;
    public void setMinimapBounds(Bounds b) {
        bounds = b;
        pxPerMeterX = (b.pxBottomRightX - b.pxTopLeftX) / (double)(b.bottomRight.getX() - b.topLeft.getX());
        pxPerMeterY = (b.pxBottomRightY - b.pxTopLeftY) / (double)(b.bottomRight.getZ() - b.topLeft.getZ());
        //hardcoding pxPerMeterX and Y
        pxPerMeterX = 11;
        pxPerMeterY = pxPerMeterX;
        offsetX = -b.topLeft.getX()*pxPerMeterY;
        offsetY = -b.topLeft.getZ()*pxPerMeterY;
        System.out.println("pxPerMeter values: " + pxPerMeterX + " offsetX: " + offsetX + " offsetY: " + offsetY);
    }

    public Landmark newLocalLM;

    boolean longpress = false;
    private Point3D mainP = null;
    public void updateUserParticle(Point3D pos) {
        if(pos!=null)
            System.out.println("updateUserPos: " + pos.toString());
        mainP = pos;
        this.invalidate();
//        System.out.println("updating user pos");
    }

    public void setVisitedPath(List<Point3D> vPath) {
        this.visitedPath = vPath;
        this.invalidate();
    }

    /**
     * The breathtaking onDraw-method. Draws all minimap components (landmarks, map, user location) to the canvas.
     * @param canvas the canvas to be drawn on
     */

    @Override
    protected void onDraw(Canvas canvas) {
        if(map != null) {
            canvas.drawBitmap(map, scaleMatrix, null);
            PathPoint previous = null;
            PathPoint previousVisited = null;
            if(mainP != null) {
                PathPoint userpt2 = resolve(mainP);

                float[] scaledUser = new float[2];
                scaleMatrix.mapPoints(scaledUser, new float[]{(float) userpt2.getX(), (float) userpt2.getY()});

                Paint point = new Paint();
                point.setColor(Color.MAGENTA);
                canvas.drawCircle(scaledUser[0], scaledUser[1], 13, point);
           }

            if (path != null) {
                for (Point3D pt : path) {
                    PathPoint pt2 = resolve(pt);

                    float[] scaledPrev = new float[2];
                    float[] scaledPt2 = new float[2];

                    if (previous != null) {
                        scaleMatrix.mapPoints(scaledPrev, new float[]{(float) previous.getX(), (float) previous.getY()});
                        scaleMatrix.mapPoints(scaledPt2, new float[]{(float) pt2.getX(), (float) pt2.getY()});
                        canvas.drawLine(scaledPrev[0], scaledPrev[1], scaledPt2[0], scaledPt2[1], linePaint);
                    }

                    canvas.drawCircle(scaledPt2[0], scaledPt2[1], 10, paint);
                    previous = pt2;

                }
            }

            if (visitedPath != null) {
                if(visitedPath.size() > 0) {
                    for (Point3D pt : visitedPath) {
                        PathPoint pt2 = resolve(pt);

                        float[] scaledPrev = new float[2];
                        float[] scaledPt2 = new float[2];

                        if (previousVisited != null) {
                            scaleMatrix.mapPoints(scaledPrev, new float[]{(float) previousVisited.getX(), (float) previousVisited.getY()});
                            scaleMatrix.mapPoints(scaledPt2, new float[]{(float) pt2.getX(), (float) pt2.getY()});
                            canvas.drawLine(scaledPrev[0], scaledPrev[1], scaledPt2[0], scaledPt2[1], visitedLine);
                        }

                        canvas.drawCircle(scaledPt2[0], scaledPt2[1], 10, visitedPoint);
                        previousVisited = pt2;

                    }
                }
            }

            if (landmarks != null) {
                //System.out.println("The downx: " + downx + " and the downy: " + downy);
                for (Landmark l : landmarks) {
                    PathPoint pos = resolve(l.getLocation());
                    Bitmap btmp = unknownLandmark;
                    float[] dest = new float[2];
                    scaleMatrix.mapPoints(dest, new float[]{(float) pos.getX(), (float) pos.getY()});
                    //System.out.println("landmark dest: " + Arrays.toString(dest) + " landmark resolved location: " + pos.toString() + " landmark loc: " + l.getLocation()  );
                    canvas.drawBitmap(btmp,dest[0]-btmp.getWidth()/2,dest[1]-btmp.getHeight(),null);
                }
            }

            if (newLocalLM != null) {
                PathPoint pos = resolve(newLocalLM.getLocation());
                Bitmap btmp =  unknownLandmark;
                float[] dest = new float[2];
                scaleMatrix.mapPoints(dest, new float[]{(float) pos.getX(), (float) pos.getY()});
                canvas.drawBitmap(btmp,dest[0]-btmp.getWidth()/2,dest[1]-btmp.getHeight(),null);
            }

            if(userPos != null) {
                PathPoint p = resolve(userPos);
                float[] dst = new float[2];
                scaleMatrix.mapPoints(dst, new float[]{(float) p.getX(), (float) p.getY()});

                float px = dst[0];
                float py = dst[1];

                userMatrix.reset();
                userMatrix.postTranslate(-userBitmap.getWidth() / 2, -userBitmap.getHeight() / 2);
                userMatrix.postRotate(userRotation + rotationDegrees);
                userMatrix.postTranslate(px, py);

                canvas.drawBitmap(userBitmap, userMatrix, null);
            }
        }
        super.onDraw(canvas);
    }

    private float rotationDegrees = 0;

    private void rotateImage(float degrees,float pointX, float pointY) {
        if (degrees!=0) {
            rotationDegrees = (rotationDegrees + degrees) % 360f;
            scaleMatrix.postRotate(degrees, pointX, pointY);
        }
    }

    private float scaleFactor = 1f;
    private float scaleMin = 0.8f;
    private float scaleMax = 6f;

    private void scaleImage(float ratio, float pointX, float pointY) {
        float n = scaleFactor*ratio;
        if (n<=scaleMax && n>=scaleMin) {
            scaleFactor = n;
            scaleMatrix.postScale(ratio, ratio, pointX, pointY);
        }
    }

    private void moveImage(float dx, float dy) {
        scaleMatrix.postTranslate(dx,dy);
    }

    public void alignCenter(int milliseconds) {
        float width = (float) (bounds.topLeft.getX() - bounds.bottomRight.getX());
        float height = (float) (bounds.topLeft.getZ() - bounds.bottomRight.getZ());
        autoAlign(milliseconds,width,height,3,0);
//        autoAlign(milliseconds,(float)getMeasuredWidth(),(float)getMeasuredHeight(),3,0);
    }

    //work in progress
    private int timerCount = 1;
    Timer stepper;

    private void autoAlign(float ms, float targetX, float targetY, float targetScale, float targetRot) {
        final int steps = Math.round(ms/22.0f)+1;

        //System.out.println("Steps: "+steps);

        final float dx;
        final float dy;
        final float[] dest = new float[2];

        if(markerEditOpen) {
            Point3D landmarkPoint = landmarks.get(0).getLocation();
            PathPoint pos = resolve(landmarkPoint);

            scaleMatrix.mapPoints(dest, new float[]{(float) pos.getX(), (float) pos.getY()});

            dx = (targetX-(float) (landmarkPoint.getX()-200))/steps;
            dy = (targetY-(float) (landmarkPoint.getZ())-10)/steps;
        } else {
            dx = (targetX-(float)userPosOnScreen.getX())/steps;
            dy = (targetY-(float)userPosOnScreen.getY())/steps;
        }

        float currentRot = rotationDegrees + userRotation;

        final float degrees = ((Math.abs(targetRot-currentRot)%360)>180) ? ((360f - (currentRot-targetRot))%360f)/steps : ((targetRot-currentRot)%360f)/steps;
        final float scalefactor = (float) Math.pow(targetScale/scaleFactor,1f/(steps));

        stepper = new Timer("Minimap Autoalign Timer",true);

        stepper.schedule(new TimerTask() {
            @Override
            public void run() {
                if(markerEditOpen) {
                    scaleImage(scalefactor,dest[0],dest[1]);
                } else {
                    rotateImage(degrees,(float)userPosOnScreen.getX(),(float)userPosOnScreen.getY());
                    scaleImage(scalefactor,(float)userPosOnScreen.getX(),(float)userPosOnScreen.getY());
                }

                moveImage(dx,dy);

                Utils.runOnUiThread(invalidateRunnable);

                if (timerCount++ >= steps) {
                    timerCount = 1;
                    this.cancel();
                }
            }
        }, 0L, 22L);
    }

    private Runnable invalidateRunnable = new Runnable() {
        public void run() {
            invalidate();
        }
    };

    final Handler longpresshandler = new Handler();

    Runnable mLongPressed = new Runnable() {
        public void run() {

            if(!markerEditOpen && !draggingNavDrawer) {
                timerrunning = false;
                setLocalLandmark();
            }
        }
    };

    private int longPressDuration = 800;

    /**
     * Sets a local landmark with template values on the map to the point that was last pressed by the user.
     */

    private void setLocalLandmark() {
        Landmark newLm = new Landmark();
        newLm.setPosition(new Point3D(downx, 1, downy));
        Point3D newPos = newLm.getLocation().copy();
        newLocalLM = newLm;
        Point3D map = screenPointToMapPoint((float) newPos.getX(), (float) newPos.getZ());
        newLocalLM.setPosition(map.copy());
        newLocalLM.setTitle("New Landmark");
        newLocalLM.setDescription(" ");
        newLocalLM.setAreaID(areaID);
        newLocalLM.buildingID = buildingID;
        int id = hash(formStringFromLocation(newPos));
        newLocalLM.setID(id);

        longpress = true;
        Vibrator v = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(50);
        invalidate();
        if(listener != null) listener.removeOtherLocalLMs(this);
    }

    public Landmark getLocalLandmark() {
        return this.newLocalLM;
    }

    public String formStringFromLocation(Point3D point) {
        String x = Integer.toString((int) (point.getX()*100));
        String y = Integer.toString((int) (point.getY()*100));
        String z = Integer.toString((int) (point.getZ()*100));
        System.out.println("x+y+z: "+x+y+z);
        return x+y+z;
    }

    /**
     * Generates and id for a new landmark from string.
     * @param s the source string
     * @return the generated id
     */

    public static int hash(String s) {
        int h = 0;
        for (int i = 0; i < s.length(); i++) {
            h = 31 * h + s.charAt(i);
        }
        return h;
    }

    private void shortPress() {

        System.out.println("shortpress");

        if(floorLO.getVisibility() == VISIBLE) floorLO.setVisibility(View.GONE);

        // Checks whether the user has pressed a local landmark (a landmark that the user has just created)
        if (this.isMapSet && newLocalLM != null && !markerEditOpen) {
            PathPoint p = resolve(newLocalLM.getLocation());

            float[] dst = new float[2];
            scaleMatrix.mapPoints(dst, new float[]{(float) p.getX(), (float) p.getY()});

            int threshold = unknownLandmark.getWidth();
            if (Math.abs(dst[0] - downx) <= threshold && Math.abs(downy - dst[1]) <= threshold) {
                showContextMenu();
            } else {
                newLocalLM = null;
                this.invalidate();
            }
        }

        // Checks if the user has pressed any landmark that's not a newly created local landmark.
        Landmark pressed = null;
        if (this.isMapSet && this.landmarks != null && this.landmarks.size() > 0) {

            if (this.isMapSet && this.landmarks != null && this.landmarks.size() > 0) {

                for (Landmark l : landmarks) {
                    PathPoint p = resolve(l.getLocation());

                    float[] dst = new float[2];
                    scaleMatrix.mapPoints(dst, new float[]{(float) p.getX(), (float) p.getY()});

                    int threshold = unknownLandmark.getWidth();
                    if (Math.abs(dst[0] - downx) <= threshold && Math.abs(downy - dst[1]) <= threshold) {
                        pressed = l;
                        break;
                    }
                }
            }
        }

        // Shows or hides the landmark info depending on where the user has pressed (on a landmark or not)
        // or sets a new landmark if the landmark location edit is open.
        if (pressed != null) {
            if (!markerEditOpen && slideLO != null) {
                showMarkerInfo(pressed);
            }
        } else {
            if (!markerEditOpen && slideLO != null && slideLO.getPanelState() != SlidingUpPanelLayout.PanelState.DRAGGING) {
                hideLandmarkInfo();
            }
        }
        if(markerEditOpen) setEditLandmark();
    }

    /**
     * Sets a landmark in landmark location editing view.
     */

    public void setEditLandmark() {
        Point3D newPos = new Point3D(downx, 1, downy);
        Point3D map = screenPointToMapPoint((float) newPos.getX(), (float) newPos.getZ());
        updatedLandmark.setPosition(map);

        setLandmarks(Arrays.asList(updatedLandmark), false);
    }

    /**
     * Inits a drawer listener that is for fixing a big that sets a new landmark when the drawer is opened.
     */

    public void initDrawerListener() {
        drawerLO.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                if(slideOffset > 0) {
                    if(!draggingNavDrawer) draggingNavDrawer = true;
                } else {
                    draggingNavDrawer = false;
                }
            }

            @Override
            public void onDrawerOpened(View drawerView) {}
            @Override
            public void onDrawerClosed(View drawerView) {}
            @Override
            public void onDrawerStateChanged(int newState) {}

        });
    }

    public void setDrawerLO(DrawerLayout drawer) {
        this.drawerLO = drawer;
        if(!markerEditOpen) {
            initDrawerListener();
        }
    }

    /**
     * Opens an InfoFragment with the pressed landmark's info on it.
     * @param pressed the landmark that was pressed
     */

    public void showMarkerInfo(Landmark pressed) {
        FragmentManager manager = ((FragmentActivity) this.getContext()).getSupportFragmentManager();
        FragmentTransaction trans = manager.beginTransaction();

        String wholeDescription;
        if(pressed.getDescription().length() != 0) {
            wholeDescription = pressed.getDescription();
        } else {
            wholeDescription = "Lorem ipsum dolor sit amet, consectetur adipiscing elit," +
                    " sed do eiusmod tempor incididunt ut labore et dolore magna aliqua." +
                    " Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat." +
                    " Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. " +
                    "Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";
        }

        Boolean fav = false;
        if(findLandmarFromList(favLandmarks, pressed.getID()) != null) fav = true;

        Fragment possibleFrag = manager.findFragmentByTag("landmarkInfo");
        if(possibleFrag != null) {
            trans.remove(possibleFrag);
            manager.popBackStack();
        }

        slideLO.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);

        // When we don't have to use the temp description anymore, wholeDescription as an attribute
        // in the instance is not needed. (can be gotten from the landmark)

        InfoFragment infoFrag =
                InfoFragment.newInstance(pressed, wholeDescription, fav);
        trans.add(R.id.info_frag, infoFrag, "landmarkInfo");
        trans.addToBackStack(null);
        trans.commit();
    }

    // okay to remove?
    public Point3D getDownPressMapPoint() {
        return screenPointToMapPoint(downx,downy);
    }

    /**
     * Converts the 2D screen point to minimap coordinates.
     * @param screenx x-coordinate
     * @param screeny y-coordinate
     * @return the Point3D of the coords in minimap coords
     */

    private Point3D screenPointToMapPoint(float screenx,float screeny) {
        Matrix n = new Matrix();
        scaleMatrix.invert(n);

        float[] mapPoint = new float[2];
        n.mapPoints(mapPoint,new float[]{screenx,screeny});

        Point3D r = unresolveLM(new PathPoint(mapPoint[0],mapPoint[1]));
        r.setRotX(0f);
        r.setRotY(0f);
        r.setRotZ(0f);

        return r;
    }

    /**
     * Sets path and lowers info pop-up.
     * @param path the route/path to be set
     */

    public void setPath(List<Point3D> path) {
        hideLandmarkInfo();
        this.path = path;
        this.invalidate();
    }

    private float sx, sy;

    private float rx1 = -1, ry1, rx2, ry2;

    private float downx,downy;
    private boolean multitouch = false;

    private boolean timerrunning = false;

    /**
     * Simply defines what happens when the user touches the screen.
     * @param event the touch event
     * @return
     */

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (!multitouch && event.getPointerCount() == 1) {

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    sx = downx = event.getX();
                    sy = downy = event.getY();

                    if (!timerrunning) {
                        timerrunning = true;
                        longpresshandler.postDelayed(mLongPressed,longPressDuration);
                    }

                    break;

                case MotionEvent.ACTION_UP:
                    if (Math.abs(downx-sx)<15 && Math.abs(downy-sy)<15 && timerrunning)
                    { shortPress(); longpresshandler.removeCallbacks(mLongPressed); timerrunning = false;}

                case MotionEvent.ACTION_CANCEL:
                    longpresshandler.removeCallbacks(mLongPressed);
                    timerrunning = false;
                    return false;

                case MotionEvent.ACTION_MOVE:
                    //System.out.println("cancel.");

                    if (timerrunning && Math.abs(downx-sx)>=15 && Math.abs(downy-sy)>=15) {
                        longpresshandler.removeCallbacks(mLongPressed);
                        timerrunning = false;
                    }

                    if (sx == -1) {
                        break;
                    }
                    float dx = event.getX() - sx;
                    float dy = event.getY() - sy;

                    sx = event.getX();
                    sy = event.getY();
                    moveImage(dx, dy);
                    invalidate();

                    break;
            }
            return true;
        } else if (event.getPointerCount() > 1) {

            if (event.getPointerCount() == 2) {
                longpresshandler.removeCallbacks(mLongPressed);
                timerrunning = false;

                switch (event.getAction()) {
                    case MotionEvent.ACTION_POINTER_2_DOWN:
                    case MotionEvent.ACTION_POINTER_1_DOWN:
                        multitouch = true;

                        rx1 = event.getX(0);
                        rx2 = event.getX(1);
                        ry1 = event.getY(0);
                        ry2 = event.getY(1);

                        sx = (rx1+rx2)/2;
                        sy = (ry1+ry2)/2;

                        break;

                    case MotionEvent.ACTION_MOVE:
                        //Log.d("onTouchEvent", "Action Event detected");
                        if (rx1 == -1)
                            break;

                        float k1 = (ry2-ry1)/(rx2-rx1);
                        float len1 = (float)Math.hypot((ry2-ry1),(rx2-rx1));

                        rx1 = event.getX(0);
                        rx2 = event.getX(1);
                        ry1 = event.getY(0);
                        ry2 = event.getY(1);

                        float k2 = (ry2-ry1)/(rx2-rx1);
                        float len2 = (float)Math.hypot((ry2-ry1),(rx2-rx1));




                        float dx = (rx1+rx2)/2 - sx;
                        float dy = (ry1+ry2)/2 - sy;

                        sx = (rx1+rx2)/2;
                        sy = (ry1+ry2)/2;

                        float scale = len2/len1;
                        scaleImage(scale,sx,sy);
                        moveImage(dx, dy);
                        //moveUser2(dx, dy);

                        float degrees = (float)Math.toDegrees(Math.atan((k2-k1)/(1+k2*k1)));

                        if (degrees==degrees) //fixes occasional NaN values
                            rotateImage(degrees,sx,sy);

                        invalidate();

                        break;

                    case MotionEvent.ACTION_POINTER_2_UP:
                    case MotionEvent.ACTION_POINTER_1_UP:
                        sx = -1;
                        multitouch = false;
                        break;
                }
            } else sx = -1;

        }
        return super.onTouchEvent(event);
    }

    public void showUserPos(Point3D position) {
        //locationResponse = position;
        userPos = position;
        PathPoint p = resolve(userPos);
        float[] dst = new float[2];
        scaleMatrix.mapPoints(dst, new float[]{(float) p.getX(), (float) p.getY()});
        float px = dst[0];
        float py = dst[1];

        userMatrix.setTranslate(px,py);
        //userMatrix.postRotate(45);

        try {
            System.out.println("what is currentAngleAccumulated before userpos set " + offSetAngle);
            userRotation = Utils.Point3DtoDegrees(userPos) + 45;// + (float) offSetAngle; //will throw error if the userPos rotation is invalid (null vector) b
        } catch (Exception e) {
            //e.printStackTrace();
        }
        invalidate();
    }

    private EditText titleInput;
    private EditText descriptionInput;

    /**
     * Defines what happens when the user presses a button on landmark info fragment.
     * @param button the button pressed
     * @param landmark the landmark that the info is about
     */

    public void onButtonPressed(View button, final Landmark landmark) {
        switch ((String) button.getTag()) {
            case "1":       //Favorite
                onFavoriteButtonPressed(button, landmark);
                break;
            case "2":       //Navigate to
                ((MainActivity) getContext()).navigateToDestination(landmark);
                destination = landmark;
                break;
            case "3":       //Error
                if(!errorOpen) {
                    onErrorButtonPressed(landmark);
                }
                break;
            case "4":       //Edit landmark
                onEditButtonPressed(landmark, false);
                break;
        }
    }

    public void toggleError(boolean open) {
        errorOpen = open;
    }

    public void markerEditOpened(boolean open) {
        markerEditOpen = open;
    }

    /**
     * Opens up an ErrorFragment that lets the user send an error report.
     * @param landmark the landmark to be reported
     */

    public void onErrorButtonPressed(Landmark landmark) {
        Bitmap bmp = overlay(map, landmark);

        FragmentManager manager = ((FragmentActivity) this.getContext()).getSupportFragmentManager();
        FragmentTransaction trans = manager.beginTransaction();

        Landmark newLandmark = new Landmark(landmark.getLocation());
        newLandmark.setID(landmark.getID());
        newLandmark.setTitle(landmark.getName());
        newLandmark.setAreaID(landmark.areaID);
        newLandmark.setImageData(landmark.getImageData());

        ErrorFragment errorFrag =
                ErrorFragment.newInstance(newLandmark, minimapClone, bmp);
        trans.replace(R.id.error_fragment, errorFrag, "errorFragment");
        trans.addToBackStack(null);
        trans.commit();
        toggleError(true);
        drawerLO.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
    }

    /**
     * Shows a dialog that lets the user edit the landmark given as a parameter
     * @param landmark the landmark to be edited
     */

    public void onEditButtonPressed(final Landmark landmark, final boolean localLandmark) {

        LayoutInflater li = LayoutInflater.from(getContext());
        final View editDialog = li.inflate(R.layout.dialog_edit_landmark, null);

        // Creates the dialog
        final AlertDialog d = new AlertDialog.Builder(getContext())
                .setView(editDialog)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();

        // Fetches editTexts and sets current data in them.
        titleInput = (EditText) editDialog
                .findViewById(R.id.landmark_edit);
        titleInput.setText(landmark.getNicerTitle());
        titleInput.setSelectAllOnFocus(true);

        descriptionInput = (EditText) editDialog
                .findViewById(R.id.description_edit);
        descriptionInput.setText(landmark.getDescription());
        descriptionInput.setSelectAllOnFocus(true);

        // Opens the keyboard
        InputMethodManager inputMethodManager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) { inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0); }

        // Determines what happenes when the alert is shown and when the buttons are pressed
        d.setOnShowListener(new DialogInterface.OnShowListener() {

            @Override
            public void onShow(DialogInterface dialog) {

                Button p = d.getButton(AlertDialog.BUTTON_POSITIVE);
                Button n = d.getButton(AlertDialog.BUTTON_NEGATIVE);

                p.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        // Fetches new title and description from the edittexts.
                        String newTitle = titleInput.getText().toString().trim();
                        String newDescription = descriptionInput.getText().toString().trim();

                        if(newTitle.trim().length() == 0) {
                            Toast.makeText(Utils.getContext(), "Don't leave an empty title!", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Remove and add, "change" the title and description of the landmark.
                        Landmark possibleLM = findLandmarFromList(favLandmarks, landmark.getID());
                        if(possibleLM != null && !localLandmark) {
                            favLandmarks.remove(possibleLM);
                            landmark.setTitle(newTitle);
                            landmark.setDescription(newDescription);
                            favLandmarks.add(landmark);
                            favPrefs = getContext().getSharedPreferences(prefsName, Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = favPrefs.edit();
                            editor.putString("landmarks", convertToJson(favLandmarks)).apply();
                        }

                        // If the landmark is newly created, it's just edited and added to favorites.
                        if(localLandmark) {
                            landmark.setTitle(newTitle);
                            landmark.setDescription(newDescription);
                            favLandmarks.add(landmark);
                            favPrefs = getContext().getSharedPreferences(prefsName, Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = favPrefs.edit();
                            editor.putString("landmarks", convertToJson(favLandmarks)).apply();
                            newLocalLM = null;
                            setLandmarks(Arrays.asList(landmark), true);
                        }

                        updateLandmarkInfo(newTitle, newDescription);

                        d.dismiss();
                    }
                });
                n.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        d.dismiss();
                    }
                });
            }
        });
        d.show();
    }

    /**
     * Toggles landmark favorite.
     * @param button the heart button
     * @param landmark the landmark that's favoritism is toggled
     */

    public void onFavoriteButtonPressed(View button, Landmark landmark) {
        favPrefs = getContext().getSharedPreferences(prefsName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = favPrefs.edit();
        Landmark possibleLM = findLandmarFromList(favLandmarks, landmark.getID());

        // If the landmark is found from favorites, it's removed from there
        if(possibleLM != null) {
            Toast toast = Toast.makeText(Utils.getContext(),
                    landmark.getNicerTitle()+" removed from favorites.", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            favLandmarks.remove(possibleLM);
            String json = convertToJson(favLandmarks);
            editor.putString("landmarks", json);
            editor.apply();

            changeHeartColor(false, false);

        } else {
            Toast toast = Toast.makeText(Utils.getContext(),
                    landmark.getNicerTitle()+" added to favorites.", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            favLandmarks.add(landmark);
            String json = convertToJson(favLandmarks);
            editor.putString("landmarks", json);
            editor.apply();
            changeHeartColor(true, false);
        }
    }

    /**
     * Updates the landmark's info that's on the info fragment atm. Useful after editing the landmark locally.
     * @param newtitle the new title of the lm
     * @param newdesc the new description of the lm
     */

    public void updateLandmarkInfo(String newtitle, String newdesc) {
        FragmentManager manager = ((FragmentActivity) getContext()).getSupportFragmentManager();
        Fragment frag = manager.findFragmentByTag("landmarkInfo");
        if(frag != null) {
            Bundle args = frag.getArguments();
            TextView title = (TextView) frag.getView().findViewById(R.id.title);
            TextView description = (TextView) frag.getView().findViewById(R.id.description);

            title.setText(newtitle);
            title.setText(newtitle);
            description.setText(newdesc);

            args.putString("title", newtitle);
            args.putString("wholeDescription", newdesc);
        }
    }

    /**
     * Updates the favLandmarks-variable correspond the Shared Preferences.
     * Also updates the favorite button (heart).
     */

    public void updateLandmarks() {
        favLandmarks = getLandMarkList(
                getContext().getSharedPreferences(prefsName, Context.MODE_PRIVATE));
        if(favLandmarks == null) {
            favLandmarks = new ArrayList();
        }
        changeHeartColor(false, true);
    }

    public String convertToJson(List<Landmark> landmarks) {
        Gson gson = new Gson();
        return gson.toJson(landmarks);
    }

    /**
     * Fetches the landmark list from shared preferences given as parameter.
     * @param prefs the Shared Preferences
     * @return the list of landmarks found from SP. An empty list if no list found.
     */

    public List<Landmark> getLandMarkList(SharedPreferences prefs) {
        Gson gson = new Gson();
        String jsonPreferences = prefs.getString("landmarks", "");
        Type type = new TypeToken<List<Landmark>>() {}.getType();
        List<Landmark> productFromShared = gson.fromJson(jsonPreferences, type);
        if(productFromShared == null) return new ArrayList();
        return productFromShared;
    }

    /**
     * Changes the heart color on the landmark info fragment or just updates the heart color.
     * @param on true -> change heart to selected (red), false -> change heart to unselected (gray)
     * @param justUpdate if this is true, the method doesn't take into account the parameter "on" and
     *                   just udpates the heart color according to fragment's argument "on".
     */

    public void changeHeartColor(boolean on, boolean justUpdate) {
        FragmentManager manager = ((FragmentActivity) getContext()).getSupportFragmentManager();
        Fragment frag = manager.findFragmentByTag("landmarkInfo");
        boolean setOn = on;
        if(frag != null) {
            ImageButton heart = (ImageButton) frag.getView().findViewById(R.id.button_favorite);
            if(justUpdate) {
                Bundle args = frag.getArguments();
                setOn = false;
                if( findLandmarFromList(favLandmarks, args.getInt("id")) != null ) setOn = true;
            } else {
                Bundle args = frag.getArguments();
                args.putBoolean("fave", setOn);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if(setOn) heart.setImageDrawable(getResources().getDrawable(R.drawable.ic_heartred,
                        ((MainActivity) getContext()).getTheme()));
                else heart.setImageDrawable(getResources().getDrawable(R.drawable.ic_heart3,
                        ((MainActivity) getContext()).getTheme()));
            } else {
                if(setOn) heart.setImageDrawable(getResources().getDrawable(R.drawable.ic_heartred));
                else heart.setImageDrawable(getResources().getDrawable(R.drawable.ic_heart3));
            }
        }

    }

    /**
     * Finds if the landmark given as a parameter has the same coords as any of the landmarks in the list given as a parameter
     * @param lmlist the landmark list
     * @param landmarkID the landmark's id that is to be found
     * @return the landmark from the list if it is found
     */

    public Landmark findLandmarFromList(List<Landmark> lmlist, int landmarkID) {
        Landmark rlm = null;
        for (Landmark lm : lmlist) {
            if(lm.getID() == landmarkID) { rlm = lm; }
        }
        return rlm;
    }

    public void onBackPressed() {
        FragmentManager manager = ((FragmentActivity) this.getContext()).getSupportFragmentManager();
        if(slideLO != null && slideLO.getPanelState() == SlidingUpPanelLayout.PanelState.DRAGGING) {
            // If the panel is being dragged and back button pressed at the same time, this prevents
            // a bug that leaves the panel but deletes the info view.
            FragmentTransaction trans = manager.beginTransaction();
            trans.addToBackStack(null);
            trans.commit();
        } else {
            if (markerEditOpen) markerEditOpened(false);
            else if (errorOpen) {
                drawerLO.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
                toggleError(false);
            }
            else slideLO.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
        }
    }

    /**
     * Hides the landmark info fragment.
     */

    public void hideLandmarkInfo() {
        FragmentManager manager = ((FragmentActivity) this.getContext()).getSupportFragmentManager();
        FragmentTransaction trans = manager.beginTransaction();
        Fragment possibleFrag = manager.findFragmentByTag("landmarkInfo");

        // Pops the infofragment out if there is one
        if(possibleFrag != null) {
            trans.remove(possibleFrag).commit();
            manager.popBackStack();
        }
        // Closes the info slider
        slideLO.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
    }

    /**
     * Combines a bitmap to a landmark's bitmap.
     * @param bmp1 the first bitmap (map)
     * @param lm the landmark to be drawn on the canvas
     * @return a bitmap that's a combined bitmap of the two bitmaps given as parameters
     */

    public Bitmap overlay(Bitmap bmp1, Landmark lm) {
        Bitmap bmOverlay = Bitmap.createBitmap(bmp1.getWidth(), bmp1.getHeight(), bmp1.getConfig());
        Canvas canvas = new Canvas(bmOverlay);

        //set minimap to canvas
        Matrix newScaleMatrix = new Matrix();
        newScaleMatrix.setTranslate(-180f, 130f);
        canvas.drawBitmap(bmp1, newScaleMatrix, null);

        //set marker to canvas
        PathPoint pos = resolve(lm.getLocation());
        Bitmap btmp = unknownLandmark;
        float[] dest = new float[2];
        newScaleMatrix.mapPoints(dest, new float[]{(float) pos.getX(), (float) pos.getY()});
        canvas.drawBitmap(btmp, dest[0]-btmp.getWidth()/2, dest[1]-btmp.getHeight(), null);

        return bmOverlay;
    }

    /**
     * Resets the minimap's scaling and rotation.
     */

    public void reset() {
        scaleMatrix = new Matrix();
    }

    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

}
