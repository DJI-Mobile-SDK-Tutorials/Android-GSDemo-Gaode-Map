package com.dji.GSDemo;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.AMap.OnMapClickListener;
import com.amap.api.maps2d.CameraUpdate;
import com.amap.api.maps2d.CameraUpdateFactory;
import com.amap.api.maps2d.MapView;
import com.amap.api.maps2d.model.BitmapDescriptorFactory;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.Marker;
import com.amap.api.maps2d.model.MarkerOptions;

import dji.sdk.api.DJIDrone;
import dji.sdk.api.DJIError;
import dji.sdk.api.DJIDroneTypeDef.DJIDroneType;
import dji.sdk.api.GroundStation.DJIGroundStationTask;
import dji.sdk.api.GroundStation.DJIGroundStationTypeDef.DJIGroundStationFinishAction;
import dji.sdk.api.GroundStation.DJIGroundStationTypeDef.DJIGroundStationMovingMode;
import dji.sdk.api.GroundStation.DJIGroundStationTypeDef.GroundStationResult;
import dji.sdk.api.GroundStation.DJIGroundStationWaypoint;
import dji.sdk.api.MainController.DJIMainControllerSystemState;
import dji.sdk.interfaces.DJIGeneralListener;
import dji.sdk.interfaces.DJIGroundStationExecuteCallBack;
import dji.sdk.interfaces.DJIMcuUpdateStateCallBack;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;



public class GSDemoActivity extends DemoBaseActivity implements OnClickListener, OnMapClickListener{
	protected static final String TAG = "GSDemoActivity";
    private MapView mapView;
	private AMap aMap;
	
	private Button locate, add, clear;
	private Button config, upload, start, stop;
	private ToggleButton tb;
	
	private DJIMcuUpdateStateCallBack mMcuUpdateStateCallBack = null;
	
	private boolean isAdd = false;
	
	private double droneLocationLat, droneLocationLng;
	private final Map<Integer, Marker> mMarkers = new ConcurrentHashMap<Integer, Marker>();
	private Marker droneMarker = null;
	private DJIGroundStationTask mGroundStationTask = null;
	
	private float altitude = 100.0f;
	private boolean repeatGSTask = false;
	private float speedGSTask;
	private DJIGroundStationFinishAction actionAfterFinishTask;
	private DJIGroundStationMovingMode heading;
	
	private int DroneCode;
	private final int SHOWDIALOG = 1;
	private final int SHOWTOAST = 2;
	
	private Timer mTimer;
	private TimerTask mTask;
	LatLng pos;
	
	private Handler handler = new Handler(new Handler.Callback() {
        
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case SHOWDIALOG:
                    showMessage(getString(R.string.demo_activation_message_title),(String)msg.obj); 
                    break;
                case SHOWTOAST:
                    setResultToToast((String)msg.obj);
                    break;
                default:
                    break;
            }
            return false;
        }
    });
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gsdemo);
        mapView = (MapView) findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);
        init();
        
        locate = (Button) findViewById(R.id.locate);
        add = (Button) findViewById(R.id.add);
        clear = (Button) findViewById(R.id.clear);
        config = (Button) findViewById(R.id.config);
        upload = (Button) findViewById(R.id.upload);
        start = (Button) findViewById(R.id.start);
        stop = (Button) findViewById(R.id.stop);
        tb = (ToggleButton) findViewById(R.id.tb);
        
        locate.setOnClickListener(this);
        add.setOnClickListener(this);
        clear.setOnClickListener(this);
        config.setOnClickListener(this);
        upload.setOnClickListener(this);
        start.setOnClickListener(this);
        stop.setOnClickListener(this);
        
        tb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					// Use the satellite map
					aMap.setMapType(AMap.MAP_TYPE_SATELLITE);
				}else{
					//Use the normal map
					aMap.setMapType(AMap.MAP_TYPE_NORMAL);
				}
				
			}
		});
        
        DroneCode = 1; // Initiate Inspire 1's SDK in function onInitSDK
        mGroundStationTask = new DJIGroundStationTask(); // Initiate an object for GroundStationTask
        
        onInitSDK(DroneCode);  // Initiate the SDK for Insprie 1
        DJIDrone.connectToDrone(); // Connect to Drone
        
        mMcuUpdateStateCallBack = new DJIMcuUpdateStateCallBack(){

            @Override
            public void onResult(DJIMainControllerSystemState state) {
            	droneLocationLat = state.droneLocationLatitude;
            	droneLocationLng = state.droneLocationLongitude;
            	Log.e(TAG, "drone lat "+state.droneLocationLatitude);
            	Log.e(TAG, "drone lat "+state.homeLocationLatitude);
            	Log.e(TAG, "drone lat "+state.droneLocationLongitude);
            	Log.e(TAG, "drone lat "+state.homeLocationLongitude);
            }     
        };        
        new Thread(){
            public void run() {
                try {
                    DJIDrone.checkPermission(getApplicationContext(), new DJIGeneralListener() {
                        
                        @Override
                        public void onGetPermissionResult(int result) {
                            // TODO Auto-generated method stub
                            if (result == 0) {
                                handler.sendMessage(handler.obtainMessage(SHOWDIALOG, DJIError.getCheckPermissionErrorDescription(result)));
                                
                                Log.e(TAG,"setMcuUpdateState");
                                DJIDrone.getDjiMC().setMcuUpdateStateCallBack(mMcuUpdateStateCallBack);
                                
                                DJIDrone.getDjiMC().startUpdateTimer(1000); // Start the update timer for MC to update info
                                updateDroneLocation(); // Obtain the drone's lat and lng from MCU.
                            } else {
                                handler.sendMessage(handler.obtainMessage(SHOWDIALOG, getString(R.string.demo_activation_error)+DJIError.getCheckPermissionErrorDescription(result)+"\n"+getString(R.string.demo_activation_error_code)+result));
                        
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
          
        mTimer = new Timer();
        class Task extends TimerTask {
            //int times = 1;

            @Override
            public void run() 
            {
                //Log.d(TAG ,"==========>Task Run In!");
                updateDroneLocation(); 
            }

        };
        mTask = new Task();
        
    }
    
    // Initializing Amap object
    private void init(){
        if (aMap == null) {
            aMap = mapView.getMap();
            setUpMap();
        }
    }
    
    private void setUpMap() {
        aMap.setOnMapClickListener(this);// add the listener for click for amap object 

    }
    
    // Function for initiating SDKs for the drone according to the drone type.
    private void onInitSDK(int type){
        switch(type){
            case 0 : {
                DJIDrone.initWithType(this.getApplicationContext(),DJIDroneType.DJIDrone_Vision);
                break;
            }
            case 1 : {
                DJIDrone.initWithType(this.getApplicationContext(),DJIDroneType.DJIDrone_Inspire1);
                break;
            }
            case 2 : {
                DJIDrone.initWithType(this.getApplicationContext(),DJIDroneType.DJIDrone_Phantom3_Advanced);
                break;
            }
            case 3 : {
                DJIDrone.initWithType(this.getApplicationContext(),DJIDroneType.DJIDrone_M100);
                break;
            }
            default : {
                break;
            }
        }
        
    }
      
    // Update the drone location based on states from MCU.
    private void updateDroneLocation(){
        // Set the McuUpdateSateCallBack
        pos = new LatLng(droneLocationLat, droneLocationLng);
        //Create MarkerOptions object
        final MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(pos);
        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.aircraft));
        
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (droneMarker != null) {
                    droneMarker.remove();
                }
             
                droneMarker = aMap.addMarker(markerOptions);
            }
          });
    }
   
    
    @Override
    public void onMapClick(LatLng point) {
    	if (isAdd == true){
    		markWaypoint(point);
    		DJIGroundStationWaypoint mDJIGroundStationWaypoint = new DJIGroundStationWaypoint(point.latitude, point.longitude);
    		mGroundStationTask.addWaypoint(mDJIGroundStationWaypoint);
    		//Add waypoints to Waypoint arraylist;
    	}else{
    		// Do not add waypoint;
    	}
    		
    }
    
    private void markWaypoint(LatLng point){
    	//Create MarkerOptions object
    	MarkerOptions markerOptions = new MarkerOptions();
    	markerOptions.position(point);
    	markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
    	Marker marker = aMap.addMarker(markerOptions);
    	mMarkers.put(mMarkers.size(),marker);
    }
    
    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub
        switch (v.getId()) {
            case R.id.locate:{           	     	
                locateDrone();  // Locate the drone's place         	
                break;
            }
            case R.id.add:{
                enableDisableAdd(); 
                break;
            }
            case R.id.clear:{
                runOnUiThread(new Runnable(){
                    @Override
                    public void run() {
                        aMap.clear();
                    }
                    
                });
                mGroundStationTask.RemoveAllWaypoint(); // Remove all the waypoints added to the task
                break;
            }
            case R.id.config:{
                showSettingDialog();
                break;
            }
            case R.id.upload:{
                uploadGroundStationTask();
                break;
            }
            case R.id.start:{
                startGroundStationTask();
                mTimer.schedule(mTask, 0, 1000);
                break;
            }
            case R.id.stop:{
                stopGroundStationTask();
                mTimer.cancel();
                break;
            }
            default:
                break;
        }
    }
    
    private void locateDrone(){
        pos = new LatLng(droneLocationLat, droneLocationLng);
        CameraUpdate cu = CameraUpdateFactory.changeLatLng(pos);
        aMap.moveCamera(cu);
        
        //Create MarkerOptions object
        final MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(pos);
        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.aircraft));
        
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (droneMarker != null) {
                    droneMarker.remove();
                }
             
                droneMarker = aMap.addMarker(markerOptions);
            }
          });
    }
    
    private void enableDisableAdd(){
        if (isAdd == false) {
            isAdd = true; // the switch for enabling or disabling adding waypoint function
            add.setText("Exit");
        }else{
            isAdd = false;
            add.setText("Add");
        }
    }
    
    private void showSettingDialog(){
        LinearLayout wayPointSettings = (LinearLayout)getLayoutInflater().inflate(R.layout.dialog_waypointsetting, null);

        final TextView wpAltitude_TV = (TextView) wayPointSettings.findViewById(R.id.altitude);
        final Switch repeatEnable_SW = (Switch) wayPointSettings.findViewById(R.id.repeat);
        RadioGroup speed_RG = (RadioGroup) wayPointSettings.findViewById(R.id.speed);
        RadioGroup actionAfterFinished_RG = (RadioGroup) wayPointSettings.findViewById(R.id.actionAfterFinished);
        RadioGroup heading_RG = (RadioGroup) wayPointSettings.findViewById(R.id.heading);
        
        repeatEnable_SW.setChecked(repeatGSTask);
        repeatEnable_SW.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener(){

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // TODO Auto-generated method stub
                if (isChecked){
                    repeatGSTask = true;
                    repeatEnable_SW.setChecked(true);
                } else {
                    repeatGSTask = false;
                    repeatEnable_SW.setChecked(false);
                }
            }
            
        });
        
        speed_RG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener(){

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // TODO Auto-generated method stub
                if (checkedId == R.id.lowSpeed){
                    speedGSTask = 1.0f;
                } else if (checkedId == R.id.MidSpeed){
                    speedGSTask = 3.0f;
                } else if (checkedId == R.id.HighSpeed){
                    speedGSTask = 5.0f;
                }
            }
            
        });
        
        actionAfterFinished_RG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // TODO Auto-generated method stub
                if (checkedId == R.id.finishNone){
                    actionAfterFinishTask = DJIGroundStationFinishAction.None;
                } else if (checkedId == R.id.finishGoHome){
                    actionAfterFinishTask = DJIGroundStationFinishAction.Go_Home;
                } else if (checkedId == R.id.finishLanding){
                    actionAfterFinishTask = DJIGroundStationFinishAction.Land;
                } else if (checkedId == R.id.finishToFirst){
                    actionAfterFinishTask = DJIGroundStationFinishAction.Back_To_First_Way_Point;
                }
            }
        });
        
        heading_RG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // TODO Auto-generated method stub
                if (checkedId == R.id.headingNext){
                    heading = DJIGroundStationMovingMode.GSHeadingTowardNextWaypoint;
                } else if (checkedId == R.id.headingInitDirec){
                    heading = DJIGroundStationMovingMode.GSHeadingUsingInitialDirection;
                } else if (checkedId == R.id.headingRC){
                    heading = DJIGroundStationMovingMode.GSHeadingControlByRemoteController;
                } else if (checkedId == R.id.headingWP){
                    heading = DJIGroundStationMovingMode.GSHeadingUsingWaypointHeading;
                }
            }
        });
        
        
        new AlertDialog.Builder(this)
        .setTitle("")
        .setView(wayPointSettings)
        .setPositiveButton("Finish",new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dialog, int id) {
                altitude = Integer.parseInt(wpAltitude_TV.getText().toString());
                Log.e(TAG,"altitude "+altitude);
                Log.e(TAG,"repeat "+repeatGSTask);
                Log.e(TAG,"speed "+speedGSTask);
                Log.e(TAG, "actionAfterFinishTask "+actionAfterFinishTask);
                Log.e(TAG, "heading "+heading);
                configGroundStationTask();
            }
            
        })
        .setNegativeButton("Cancel", new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
            
        })
        .create()
        .show();
    }
    
    private void configGroundStationTask(){
        mGroundStationTask.isLoop = repeatGSTask;
        mGroundStationTask.finishAction=actionAfterFinishTask;
        mGroundStationTask.movingMode = heading;
        for (int i=0; i<mGroundStationTask.wayPointCount; i++){
            mGroundStationTask.getWaypointAtIndex(i).speed = speedGSTask;
            mGroundStationTask.getWaypointAtIndex(i).altitude = altitude;
        }
    }
    
    private void uploadGroundStationTask(){
        DJIDrone.getDjiGroundStation().openGroundStation(new DJIGroundStationExecuteCallBack(){

            @Override
            public void onResult(GroundStationResult result) {
                // TODO Auto-generated method stub
                String ResultsString = "return code =" + result.toString();
                handler.sendMessage(handler.obtainMessage(SHOWTOAST, ResultsString));
                
                DJIDrone.getDjiGroundStation().uploadGroundStationTask(mGroundStationTask, new DJIGroundStationExecuteCallBack(){

                    @Override
                    public void onResult(GroundStationResult result) {
                        // TODO Auto-generated method stub
                        String ResultsString = "return code =" + result.toString();
                        handler.sendMessage(handler.obtainMessage(SHOWTOAST, ResultsString));
                    }
                    
                });
            }
            
        });
    }
    
    private void startGroundStationTask(){
        DJIDrone.getDjiGroundStation().startGroundStationTask(new DJIGroundStationExecuteCallBack(){

            @Override
            public void onResult(GroundStationResult result) {
                // TODO Auto-generated method stub
                String ResultsString = "return code =" + result.toString();
                handler.sendMessage(handler.obtainMessage(SHOWTOAST, ResultsString));
            }
        });
    }
    
    private void stopGroundStationTask(){
        DJIDrone.getDjiGroundStation().pauseGroundStationTask(new DJIGroundStationExecuteCallBack(){

            @Override
            public void onResult(GroundStationResult result) {
                // TODO Auto-generated method stub
                String ResultsString = "return code =" + result.toString();
                handler.sendMessage(handler.obtainMessage(SHOWTOAST, ResultsString));
                
                DJIDrone.getDjiGroundStation().closeGroundStation(new DJIGroundStationExecuteCallBack(){

                    @Override
                    public void onResult(GroundStationResult result) {
                        // TODO Auto-generated method stub
                        String ResultsString = "return code =" + result.toString();
                        handler.sendMessage(handler.obtainMessage(SHOWTOAST, ResultsString));
                    }

                });
            }
        });
    }

    
    public void showMessage(String title, String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title)
                .setMessage(msg)
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }
    
    private void setResultToToast(String result){
        Toast.makeText(GSDemoActivity.this, result, Toast.LENGTH_SHORT).show();
    }
    
    @Override
    protected void onResume(){
        super.onResume();
        mapView.onResume();
        Log.e(TAG, "startUpdateTimer");
        DJIDrone.getDjiMC().startUpdateTimer(1000); // Start the update timer for MC to update info
    }
    
    @Override
    protected void onPause(){
        super.onPause();
        mapView.onPause();
        Log.e(TAG, "stopUpdateTimer");
        DJIDrone.getDjiMC().stopUpdateTimer(); // Stop the update timer for MC to update info
    }
    
    @Override
    protected void onSaveInstanceState (Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }
    
    @Override
    protected void onDestroy(){
        super.onDestroy();
        mapView.onDestroy();
        Process.killProcess(Process.myPid());
    }

}
