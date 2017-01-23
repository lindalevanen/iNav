package fi.aalto.tshalaa1.inav;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.Telephony;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import fi.aalto.tshalaa1.inav.entities.Landmark;
import fi.aalto.tshalaa1.inav.entities.Minimap;
import fi.aalto.tshalaa1.inav.entities.Point3D;
import fi.aalto.tshalaa1.inav.entities.Route;
import fi.aalto.tshalaa1.inav.entities.SensorRunnable;
import fi.aalto.tshalaa1.inav.server.LandmarkRequest;
import fi.aalto.tshalaa1.inav.server.LocationRequest;
import fi.aalto.tshalaa1.inav.server.MinimapRequest;
import fi.aalto.tshalaa1.inav.server.RouteRequest;
import fi.aalto.tshalaa1.inav.server.ServerInterface;

import fi.aalto.tshalaa1.inav.entities.Route;
import fi.aalto.tshalaa1.inav.server.ServerInterface;
import java.util.Random;

/**
 * Created by tshalaa1 on 7/20/16.
 */
public class ParticleFilter extends Thread implements SensorEventListener, Runnable {


    private String TAG = "ParticleFilter";
    private MinimapView[] minimapViews;
    private HashMap<String, Integer> areaIDs;
    private Navigator navigator;
    // The number of particles the system generates.
    private static final int N = 500;
    private static final double TURN_THRESHOLD = Math.PI/6;
    //Thresh for each step that identifies the user may make a turn
    private static final double MULTIPLE_STEP_TURN_THRESHOLD = Math.PI/18;
    public List<Particle> particles = new ArrayList<Particle>();

    SensorDataHandler dynamicPositionSensorReader = new SensorDataHandler();

    private Route testRoute = new Route();
    private List<Point3D> path;
    private List<Point3D> visitedPath = new ArrayList<Point3D>();

    private SensorManager sensorM;
    private boolean stepDetectionShown = false;
    private double oneStepRad = 0;
    private double accTurnRad = 0;
    private int turnDetectCount = 0;
    private boolean lastStepTurn = false;
    private boolean turnDetected = false;

    public ParticleFilter(){

    }

    public void setTestPath(Route r) {
//        testRoute.setPath(Arrays.asList(new Point3D(16.4526, 0, 19.1129),
//                new Point3D(18.4875, 0, 19.0125),
//                new Point3D(33.7500, 0, 19.0500),
//                new Point3D(35.9625, 0, 16.5375),
//                new Point3D(36.5250, 0, 15.6000),
//                new Point3D(37.8750, 0, 14.7000),
//                new Point3D(40.7250, 0, 14.2875),
//                new Point3D(46.6875, 0, 13.0875),
//                new Point3D(47.4750, 0, 11.2500),
//                new Point3D(49.4250, 0, 11.6250),
//                new Point3D(51.6000, 0, 11.4000),
//                new Point3D(56.0844, 0, 11.8410)));
// path = testRoute.getPath(); //testing/comparing to matlab code

        path = r.getPath();
//        System.out.println("what is the path: " + r.getInstructions());
//        path = testRoute.getPath();
//        getMinimapView().setPath(path);
        this.processWaypoints();
        this.initParticles();

    }

    public void setDetectors(){

            PackageManager pm = Utils.getContext().getPackageManager();

            //Step detection
            if (!stepDetectionShown && !pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_STEP_COUNTER)) {
                Toast.makeText(Utils.getContext(), "This device does not support step counter detection", Toast.LENGTH_SHORT).show();
                stepDetectionShown = true;
            } else {
                sensorM = (SensorManager) Utils.getContext().getSystemService(Context.SENSOR_SERVICE);
                Sensor stepSensor = sensorM.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
                if (stepSensor != null) {
                    sensorM.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_FASTEST);
                } else {
                    Toast.makeText(Utils.getContext(), "Step sensor not available", Toast.LENGTH_SHORT).show();
                }
            }

            //Gyroscope
            dynamicPositionSensorReader.setGyroRunnable(new SensorRunnable() {
                @Override
                public void run() {
//                    System.out.println("cool story: " + z);
                    oneStepRad = oneStepRad + (double) (z * (interval / 1000f));
                }
            });
            dynamicPositionSensorReader.start(true, true, false, false);


    }

    public void stopGyroscope(){
        this.dynamicPositionSensorReader.stop();
    }

    public void setMinimapViews(MinimapView[] m, HashMap<String, Integer> IDs){
        this.minimapViews = m;
        this.areaIDs = IDs;
    }

    private MinimapView getMinimapView(int idx){
        return this.minimapViews[areaIDs.get(Integer.toString(idx))];
    }

    public void setNavigator(Navigator n){
        this.navigator = n;
    }

    public Navigator getNavigator(){ return this.navigator;}

    private boolean stepTaken = false;

    @Override
    public void onSensorChanged(SensorEvent event) {
        //Log.d(TAG, "Step detected!");
        stepTaken = true;
//        Log.d(TAG, "the acc turn rad 1 " + accTurnRad);
        // Do your long operations here and return the result
        if (Math.abs((oneStepRad)) > MULTIPLE_STEP_TURN_THRESHOLD && turnDetectCount == 0) {
            if(!lastStepTurn) {
                accTurnRad = oneStepRad;
//                Log.d(TAG, "the acc turn rad 2 " + accTurnRad);
                lastStepTurn = true;
            } else {
                accTurnRad += oneStepRad;
//                Log.d(TAG, "the acc turn rad 3 " + accTurnRad);
            }
            //System.out.println("Step detected! and accturn" + accTurnRad);
            if(Math.abs(accTurnRad) > TURN_THRESHOLD){
//                Log.d(TAG, "Turn detected!");
                Log.d(TAG, accTurnRad + " vs threshold: " + TURN_THRESHOLD);
                turnDetectCount = 3; //Turn is taken over three steps
                lastStepTurn = false;
                accTurnRad = 0;
                turnDetected = true; //set to false after updateParticles method
                Toast.makeText(Utils.getContext(), "Turn detected", Toast.LENGTH_SHORT).show();
            }
        } else {
            lastStepTurn = false;
        }

        if(turnDetectCount > 0) {
            turnDetectCount -= 1;
        }

        this.updateParticles();
        oneStepRad = 0;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

  /*
    Preprocess navigation path to find key waypoints and the distance between waypoints
   */
    private List<Integer> turnWaypointIdx = new ArrayList();
    private List<Double> distBetweenWaypoints = new ArrayList();
    private boolean alreadyGotTempA = false;
    Point3D tempA = new Point3D();
    int tempAIdx = 0;
    private void processWaypoints() {

            for (int i = 1; i < path.size()-1; i++) {
                Point3D a = new Point3D(path.get(i).getX() - path.get(i - 1).getX(), 0, path.get(i).getZ() - path.get(i - 1).getZ());
                Point3D b = new Point3D(path.get(i + 1).getX() - path.get(i).getX(), 0, path.get(i + 1).getZ() - path.get(i).getZ());
                if (Math.abs((Math.atan2(a.getX() * b.getZ() - a.getZ() * b.getX(), a.getX() * b.getX() + a.getZ() * b.getZ()))) >
                        TURN_THRESHOLD) {
                    turnWaypointIdx.add(i);
                    alreadyGotTempA = false;
                } else {
                    if (!alreadyGotTempA) {
                        tempA = a;
                        tempAIdx = i;
                        alreadyGotTempA = true;
                    } else {
                        a = tempA;
                        if(Math.abs((Math.atan2(a.getX() * b.getZ() - a.getZ() * b.getX(), a.getX() * b.getX() + a.getZ() * b.getZ()))) >
                                TURN_THRESHOLD) {
                            turnWaypointIdx.add(tempAIdx);
                            alreadyGotTempA = false;
                        }
                    }
                }
                double x1 = path.get(i - 1).getX();
                double y1 = path.get(i - 1).getZ();
                double x2 = path.get(i).getX();
                double y2 = path.get(i).getZ();
                distBetweenWaypoints.add(Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2)));

                //For the last points in the path
                if (i == path.size() - 2) {
                    x1 = path.get(i + 1).getX();
                    y1 = path.get(i + 1).getZ();
                    x2 = path.get(i).getX();
                    y2 = path.get(i).getZ();
                    distBetweenWaypoints.add(Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2)));


                }

            }

        System.out.println("turnidx: "+ turnWaypointIdx);


        if(areaIDs != null && !path.isEmpty()){

            int minimapIdx = path.get(0).areaID;
            getMinimapView(minimapIdx).updateUserParticle(path.get(0));
        }
//        System.out.println("the turnWayPoint indexes " + Arrays.toString(turnWaypointIdx.toArray()));
        System.out.println("the distBetweenWaypoints " + Arrays.toString(distBetweenWaypoints.toArray()));
//        System.out.println("path: " + path.get(0).toString() + ", " +path.get(1).toString() );
    }


    private double setDistBetweenWaypoints(){

        if(distBetweenWaypoints.size() > 0) return distBetweenWaypoints.get(0);
        else return 0; //handle case where empty list
    }

    /*
    Particle initialization
     */
    private void initParticles(){
        for(int i = 0; i < N; i++){
            Particle p = new Particle();
            p.location = new Point3D(path.get(0).getX(), 0, path.get(0).getZ());
            p.passedWaypointIdx = 0;
            p.nextWaypointDist = setDistBetweenWaypoints();
            p.particleStride = Math.abs(new Random().nextDouble()*2-1) * 0.7 + 0.3; //*2-1 returns a range [0, 1], instead of [0, 1)
            particles.add(p);
        }
        System.out.println(particles.size() + " particles initialized!");
    }

    boolean recalcPromptSeen = false;
    Point3D oldEstimate;
    List<Point3D> trace = new ArrayList<Point3D>();

    private void updateParticles(){
        List<Particle> diedParticles =  new ArrayList();
        List<Particle> liveParticles = new ArrayList();
        List<Particle> remove = new ArrayList<Particle>();
        if(particles!= null)
        for(Particle p : particles) {

//            System.out.println("passed waypoint idx " + p.passedWaypointIdx);
            if(turnDetected) {
                //User should be very close to a waypoint
                if(p.nextWaypointDist <= p.particleStride && turnWaypointIdx.contains(p.passedWaypointIdx + 1)){
                    p.passedWaypointIdx += 1;
                    p.nextWaypointDist = p.nextWaypointDist - p.particleStride + distBetweenWaypoints.get(p.passedWaypointIdx);
                //} else if(turnWaypointIdx.contains(p.passedWaypointIdx) && distBetweenWaypoints.get(p.passedWaypointIdx) - p.nextWaypointDist <= p.particleStride) {
                    //p.nextWaypointDist -= p.particleStride;
                } else {
                    diedParticles.add(p);
//                    System.out.println("DIED PARTICLES 1");
                    remove.add(p);
                    continue;
                }


            } else {
                //no turn detected

                if(p.nextWaypointDist <= p.particleStride) { //check if the particle is going to pass next waypoint
                    if(turnWaypointIdx.contains(p.passedWaypointIdx+1)) {//if the waypoint is a key waypoint, it is wrong.+1
//                        System.out.println("nextWaypointDist vs particle stride " + p.nextWaypointDist+ " <<--vs-->> " + p.particleStride);
//                        System.out.println("should have turned but didn't " + p.passedWaypointIdx);
                        diedParticles.add(p);
//                        System.out.println("DEAD PARTICLES 2");
                        remove.add(p);
                        continue;
                    } else {
                        if(p.passedWaypointIdx  + 1 < distBetweenWaypoints.size()) { //check if particle reache destination
                            p.passedWaypointIdx += 1;
                            p.nextWaypointDist = p.nextWaypointDist - p.particleStride + distBetweenWaypoints.get(p.passedWaypointIdx);
                        } else {
                            diedParticles.add(p);
//                            System.out.println("Have they reached the destination: " + p.passedWaypointIdx + " waypoints " + distBetweenWaypoints.size());
//                            System.out.println("DEAD PARTICLES 3");
                            remove.add(p);
                            continue;
                        }
                    }
                } else {
                    p.nextWaypointDist -= p.particleStride;
                }
            }
            Point3D temp0 = path.get(p.passedWaypointIdx);
            Point3D temp1 = path.get(p.passedWaypointIdx + 1);
            double tempRate =  (distBetweenWaypoints.get(p.passedWaypointIdx) - p.nextWaypointDist) / distBetweenWaypoints.get(p.passedWaypointIdx);
            p.location.setX(temp0.getX() + (temp1.getX() - temp0.getX()) * tempRate);
            p.location.setZ(temp0.getZ() + (temp1.getZ() - temp0.getZ()) * tempRate);
            System.out.println("Live particles tempRate: " + tempRate);
            liveParticles.add(p);
        }

        if(turnDetected) turnDetected = false;
        System.out.println("--------------------------------------------------------------------------------------");
        //CALCULATE ESTIMATE POSITION
        if(liveParticles.size() > 1) {

            Point3D estimateP = new Point3D(0,0,0);
            int passedWayP = 0;
            double avgStrideLen = 0;
            for(Particle i : liveParticles) {
//                System.out.println("live steplen " + i.particleStride);
                System.out.println("live particle locations: " + i.location.toString());

                estimateP.setX(estimateP.getX() + i.location.getX());
                estimateP.setZ(estimateP.getZ() + i.location.getZ());
                avgStrideLen+= i.particleStride;
                passedWayP += i.passedWaypointIdx;
            }


            estimateP.setX(estimateP.getX()/liveParticles.size());
            estimateP.setZ(estimateP.getZ()/liveParticles.size());
            avgStrideLen/=liveParticles.size();

            Double estDistToWaypoint;
            Particle np = nearestLiveParticleSearch(estimateP, liveParticles);
            double vector = dotProduct(path.get(np.passedWaypointIdx), path.get(np.passedWaypointIdx+1));
            System.out.println("vector: " + vector);

            Point3D a,b,c;
            Point3D vectorAB, vectorBC;

            if (trace.size() >= 3) // Make sure you really have 3 elements
            {
                System.out.println("getting here!!");
                a = (trace.get(trace.size()-3)); // a
                b = (trace.get(trace.size()-2)); // b
                c = (trace.get(trace.size()-1)); // c

                vectorAB = new Point3D(b.getX() - a.getX(), 0, b.getZ() - a.getZ());
                vectorBC = new Point3D(c.getX() - b.getX(), 0, c.getZ() - b.getZ());
                double dot = dotProduct(oldEstimate, estimateP);
                if(Math.abs(Math.atan2(vectorAB.getX()*vectorBC.getZ()-vectorAB.getZ()*vectorBC.getX(),
                        vectorAB.getX()*vectorBC.getX()+vectorAB.getZ()*vectorBC.getZ())) > Math.PI*2/3){ //if jumps back
//                if( (vector > 0 && dot < 0 ) || (vector < 0 && dot > 0)) {

                    System.out.println("jumping based on matlab code!!");
                    Particle regen = nearestLiveParticleSearch(b, liveParticles);
                    Point3D temp0 = path.get(regen.passedWaypointIdx);
                    Point3D temp1 = path.get(regen.passedWaypointIdx + 1);
                    estDistToWaypoint = Math.sqrt(Math.pow((temp1.getX()-b.getX()), 2) + Math.pow((temp1.getZ()-b.getZ()), 2)) - avgStrideLen;
                    double tempRate =  (distBetweenWaypoints.get(regen.passedWaypointIdx) - estDistToWaypoint) / distBetweenWaypoints.get(regen.passedWaypointIdx);


                    if(estDistToWaypoint < 0) {
                        estimateP.setX(b.getX());
                        estimateP.setZ(b.getZ());
                    } else { //jump fixed
                        System.out.println("jump fixed");
                        estimateP.setX(temp0.getX() + (temp1.getX() - temp0.getX()) * tempRate);
                        estimateP.setZ(temp0.getZ() + (temp1.getZ() - temp0.getZ()) * tempRate);
                    }

                }

            }



            //TODO:
            //check if estimateP is going back or forward using estimateP to find nearest
            //p neareast live particle to estimateP to find correct index

//If distance to next waypoint is bigger than before OR particle moving backwards
            if(oldEstimate != null) {



                double dot = dotProduct(oldEstimate, estimateP);
                System.out.println("dotProduct from old to new estimate" + dot);

                if( (vector > 0 && dot < 0 ) || (vector < 0 && dot > 0)) {//(vector > 0 && dot < 0 ) || (vector < 0 && dot > 0)
                    System.out.println("jumping based on dotProduct code!!");

//System.out.println("jumping!! " + estimateP.toString());
//
//                    //            double tempRate =  (distBetweenWaypoints.get(p.passedWaypointIdx) - p.nextWaypointDist) / distBetweenWaypoints.get(p.passedWaypointIdx);
//                    Point3D temp0 = path.get(np.passedWaypointIdx);
//                    Point3D temp1 = path.get(np.passedWaypointIdx + 1);
//                    estDistToWaypoint = Math.sqrt(Math.pow((temp1.getX()-estimateP.getX()), 2) + Math.pow((temp1.getZ()-estimateP.getZ()), 2)) - avgStrideLen;
//                    double tempRate =  (distBetweenWaypoints.get(np.passedWaypointIdx) - estDistToWaypoint) / distBetweenWaypoints.get(np.passedWaypointIdx);
//                    estimateP.setX(temp0.getX() + (temp1.getX() - temp0.getX()) * tempRate);
//                    estimateP.setZ(temp0.getZ() + (temp1.getZ() - temp0.getZ()) * tempRate);
//
//
//                    System.out.println("jumping fixed!! " + estimateP.toString());
                }
            }

//            System.out.println("liveparticles avg step len " + n);

            float p = (float) passedWayP/ liveParticles.size();
//            System.out.println("passed waypoint : " +p);

            if(visitedPath != null)
            if(!visitedPath.contains(path.get(Math.round(p)))){
//                System.out.println("nxtdist: " + p);
//                System.out.println("nxtdist: " + Math.round(p));
                visitedPath.add((path.get(Math.round(p))));
            }

//            visitedPath.add(estimateP)
//            System.out.println("passedway point: " + n);
//            System.out.println("estimateP pos: " + estimateP.toString());
            trace.add(estimateP);
            getMinimapView(path.get(Math.round(p)).areaID).updateUserParticle(estimateP);
            oldEstimate = estimateP.copy();
            getMinimapView(path.get(Math.round(p)).areaID).setVisitedPath(visitedPath);

            //TODO: remove navigation toggle, set destination and startpos to null from navigator
            if (path.get(path.size()-1).getX() + 0.5 > estimateP.getX() && estimateP.getX() > path.get(path.size()-1).getX() - 0.5) {
                if (path.get(path.size()-1).getZ() + 0.5 > estimateP.getZ() && estimateP.getZ() > path.get(path.size()-1).getZ() - 0.5) {
                    Toast t2 = Toast.makeText(Utils.getContext(), R.string.dest_reached, Toast.LENGTH_SHORT);
                    t2.setGravity(Gravity.CENTER, 0, 0);
                    t2.show();
                    recalcPromptSeen = true;
                    getNavigator().recalculate = false;
                    getNavigator().toggleNavigate.setChecked(false);
                    visitedPath = null;
//                    getMinimapView().setVisitedPath(null);
//                    getMinimapView().updateUserParticle(null);
                }
            }

        } else {

            //Prompt user to take a new picture and automatically recalculate route with this
            getNavigator().recalculate = true;

            if(!recalcPromptSeen ){
                navigator.openOffCourseDialog();
//                Toast.makeText(Utils.getContext(), R.string.recalculate_msg, Toast.LENGTH_SHORT).show();
                recalcPromptSeen = true;
            }
        }
        particles.removeAll(remove);

//        System.out.println("dead particle number after: " + diedParticles.size());
        //Regenerate dead particles
        System.out.println("------------------------the dead partiles-------------------------------");
        if(!diedParticles.isEmpty() && !liveParticles.isEmpty() ){

            for(Particle regenerated : diedParticles) {
                System.out.println("the dead particle location: " + regenerated.location.toString());
                Particle nearestLive = nearestLiveParticleSearch(regenerated.location, liveParticles);
                regenerated.location = nearestLive.location;
                regenerated.setPassedWaypointIdx(nearestLive.passedWaypointIdx);
//            regenerated.passedWaypointIdx = nearestLive.passedWaypointIdx;
//            System.out.println("nearestlive waypoint idx " + nearestLive.passedWaypointIdx);
                regenerated.nextWaypointDist = nearestLive.nextWaypointDist;
                regenerated.particleStride = nearestLive.particleStride + (new Random().nextGaussian() * 0.0025);//0.0005
//            System.out.println("regenerate before " + particles.size());
                particles.add(regenerated);
            }
            System.out.println("------------------------the dead partiles-------------------------------");
            diedParticles.clear();
//            getMinimapView(path.get(Math.round(1)).areaID).setParticles(particles);

//            Particle regenerated = diedParticles.get(0);
//            diedParticles.remove(regenerated);

//            System.out.println("regenerated after " + particles.size());
        }

//        System.out.println("Total particles should match live ones or stay the same... " + particles.size());
//        System.out.println("Live particles should not end... " + liveParticles.size());

    }




    //Directin from param a to b
    public double dotProduct(Point3D a, Point3D b){
        return (a.getX()-b.getX()) + (a.getZ()-b.getZ());
    }


//    public final double nextGaussian(double mu, double sigma) {
//        return new Random().nextGaussian() * sigma + mu;
//    }

    public Particle nearestLiveParticleSearch(Point3D dead, List<Particle> liveParticles) {
        assert liveParticles.size() >0;
        Point3D dLocation = dead;
        double distance = dLocation.distance(liveParticles.get(0).location);
        int idx = 0;
        for(int c = 1; c < liveParticles.size(); c++){
            double cdistance = dLocation.distance(liveParticles.get(c).location);
            if(cdistance < distance){
                idx = c;
                distance = cdistance;
            }
        }

//        System.out.println("dead particle old location: " + dead.location.toString());
//        System.out.println("live regenerated location: " + liveParticles.get(idx).location.toString());

        return liveParticles.get(idx);
    }


}
