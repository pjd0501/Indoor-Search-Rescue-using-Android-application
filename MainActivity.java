

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.maps.model.UrlTileProvider;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainActivity extends Activity implements SensorEventListener, OnMapClickListener {
	
	private boolean debugMode = false;
	
	//categorize possible hallways numerically
	public enum Hall {MAIN, TOPSIDE, BOTSIDE};
	
	private final double DIST_THRESH = 8E-5;
	private boolean locationCalibrated = false;
	private int recalibrationCounter = 0;
	private final int RECALIBRATION_LIMIT = 5;
	
	private Toast toast;
	
	public static int landmarknum;
	private GoogleMap mMap;
	String mUrl = "http://percept.ecs.umass.edu/course/marcusbasement/{z}/{x}/{y}.png";
	
	private double[] curLatLng = new double[2];
	private double[] lastLatLng = new double[2];
	private double[] avgLatLng = new double[2];
	
	private final Object lock = new Object();
	private int numPri1 = 0;
	private int numPri2 = 0;
	private int numPri3 = 0;
	private int numPri4 = 0;
	private int numPri5 = 0;
	private LatLng[] rescueList;
	private LatLng[] deadList;
	private LatLng[] hazardList;
	private ArrayList<String> latLngPri = new ArrayList<String>(0);
	
	private double coordVals[][] = 
		{	{42.39354668258381, -72.52833187580109},	//Minor ID = 1
			{42.39359447166392, -72.52839054912329},	// 2
			{42.393661574404135, -72.52840027213097},	// 3
			{42.39370936339676, -72.52845861017704},	// 4
			{42.39377498034809, -72.52846665680408},	// 5
			{42.39382895952455, -72.52852533012629},	// 6
			{42.39389135741379, -72.52853605896235},	// 7
			{42.393932460751394, -72.52859741449356},	// 8 
			{42.393999315520105, -72.52860512584448},	// 9
			{42.39404537098601, -72.52866346389055},	// 10
			{42.394103806904866, -72.52867016941309},	// 11
			{42.39417412802318, -72.52873621881008},	// 12
			{42.39423256382214, -72.52873655408621},	// 13
			{42.39422736402869, -72.52877611666918},	// 14
			{42.39416001428392, -72.52882674336433},	// 15
			{42.39402060998703, -72.52868391573429},	// 16
			{42.39397727821532, -72.52871174365282},	// 17
		};
	
	private ArrayList<Polyline> lineList = new ArrayList<Polyline>(0);
	private int[] colorTable = new int[] {
			0xffFE0000,
			0xffF56300,
			0xffFF9D02,
			0xffFFCC01,
			0xffFFFF01,
			0xffCDFF00,
			0xff1CE000,
			0xff00C3B2,
			0xff395AFF,
			0xff7B00FF,
			0xffA900DE,
			0xffCA0185,
	};
	private float[] widthTable = new float[] { 16, 15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5};
	
	double marcusLat = 42.393985;
	double marcusLng = -72.528622;
	int knowlesZoom = 25;
	
	private boolean triageMode = true;	//default to triage mode since toggleButton defaults to off which is triage mode

	public String title = "Title ";
	public int titleNumber = 0;
	//private IOnLandmarkSelectedListener landmarkListener;
	public Uri imageUri;
	private Landmarks landmarks;	
	//private Activity activity;
	
	private boolean[] isActive = new boolean[17];
	
	private LatLngReceiver latlngReceiver;
	private TestReceiver testReceiver;
	
	private String hostname;
    private int triagePort = 4444;
    private int rescuePort = 4445;
    Socket socketClient;
    PrintWriter writer;
    BufferedReader reader;
    
    private Button dropdownMenuButton;
    private Button getDataButton;
    private Button sendDataButton;
    private Button findPathButton;
    
    private SensorManager mSensorManager;
    
    private Handler handler = new Handler(new Handler.Callback() {
		  @Override
		  public boolean handleMessage(Message msg) {
			  switch(msg.what){
			  case 0:
				  showToast("Data received from server");	//can't toast from within worker thread
				  break;
			  case 1:
				  showToast("Data sent to server");			//can't toast from within worker thread
				  break;
			  default:
				break;
			  }

			  return true;
		  }
    });
	
    private OnMenuItemClickListener menuClickListener = new OnMenuItemClickListener(){
		@Override
		public boolean onMenuItemClick(MenuItem item) {
			LatLng myPos = new LatLng(avgLatLng[0], avgLatLng[1]);
			MarkerOptions marker = null;
			String priority = null;
			String latString = Double.toString(myPos.latitude);
			String lonString = Double.toString(myPos.longitude);
			
			switch (item.getItemId()) {
			case R.id.action_addHazard:
				marker = new MarkerOptions().position(myPos).title("Priority_Four").icon(BitmapDescriptorFactory.fromResource(R.drawable.fire)).draggable(true);
	        	priority = "5.0";
	        	break;
			case R.id.action_addBlue:
	        	marker = new MarkerOptions().position(myPos).title("Priority_Four").icon(BitmapDescriptorFactory.fromResource(R.drawable.skull)).draggable(true);
	        	priority = "4.0";
	        	break;
	        case R.id.action_addGreen:
	        	marker = new MarkerOptions().position(myPos).title("Priority_Three").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)).draggable(true);
	        	priority = "3.0";
				break;
	        case R.id.action_addYellow:
	        	marker = new MarkerOptions().position(myPos).title("Priority_Two").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)).draggable(true);
	        	priority = "2.0";
	        	break;
	        case R.id.action_addRed:
	        	marker = new MarkerOptions().position(myPos).title("Priority_One").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)).draggable(true);
	        	priority = "1.0";
	        	break;
	        default:
	            return false;
			}
			mMap.addMarker(marker);
			synchronized (lock) {
				latLngPri.add(latString + "," + lonString + "," + priority);
			}
			return true;
		}
    };
    
    private OnClickListener buttonClickListener = new OnClickListener(){
		@Override
		public void onClick(View v) {
			switch(v.getId()){
			case R.id.buttonGetData:
				Thread rxDataThread = new Thread(null, connectAndReceiveData, "connectAndReceiveData");
	    		rxDataThread.start();
				break;
			case R.id.buttonSendData:
				Thread txDataThread = new Thread(null, connectAndTransmitData, "connectAndTransmitData");
	    		txDataThread.start();
				break;
			case R.id.buttonFindPath:
				findPath();
				drawLinesAndPlot();
				break;
			default:
				break;
			}	
		}
	};
    
	public void onToggleClicked(View view) {
	    boolean on = ((ToggleButton) view).isChecked();
	    if(!on) {
	    	//off = triage mode
			//if in triage mode, shouldn't be able to receive data so hide get data button
			dropdownMenuButton.setVisibility(View.VISIBLE);
			getDataButton.setVisibility(View.GONE);
			sendDataButton.setVisibility(View.VISIBLE);
			findPathButton.setVisibility(View.GONE);
		}
		else {
			//on = rescue mode
			//vice versa for rescue mode
			dropdownMenuButton.setVisibility(View.GONE);
			sendDataButton.setVisibility(View.GONE);
			getDataButton.setVisibility(View.VISIBLE);
			findPathButton.setVisibility(View.VISIBLE);
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		getActionBar().hide();
		Func.funcInit();
		
		for(int x=0; x<isActive.length; x++){
			isActive[x] = false;
		}
		
		//toggleButton = (ToggleButton) findViewById(R.id.toggleButton);
		dropdownMenuButton = (Button) findViewById(R.id.dropdownMenuButton);
		getDataButton=(Button) findViewById(R.id.buttonGetData);
		sendDataButton=(Button) findViewById(R.id.buttonSendData);
		findPathButton=(Button) findViewById(R.id.buttonFindPath);
		
		getDataButton.setOnClickListener(buttonClickListener);
		sendDataButton.setOnClickListener(buttonClickListener);
		findPathButton.setOnClickListener(buttonClickListener);

		Bundle extras = getIntent().getExtras();
		//triageMode = toggleButton.isChecked();	//triage vs rescue mode, changes app functionality
		hostname = extras.getString("IP");
		
		if(triageMode) {
			//if in triage mode, shouldn't be able to receive data so hide get data button
			dropdownMenuButton.setVisibility(View.VISIBLE);
			getDataButton.setVisibility(View.GONE);
			sendDataButton.setVisibility(View.VISIBLE);
			findPathButton.setVisibility(View.GONE);
		}
		else {
			//vice versa for rescue mode
			dropdownMenuButton.setVisibility(View.GONE);
			sendDataButton.setVisibility(View.GONE);
			getDataButton.setVisibility(View.VISIBLE);
			findPathButton.setVisibility(View.VISIBLE);
		}
		
		//used to tell what side of given line a point is on based on sign of reference point
		//same sign = same side of line, used to tell if given point is in main hall or one of the side halls for waypoint navigation
		
		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		
		setupMap();
		mMap.setOnMapClickListener(this);
	}
	
	void showToast(String text)
	{
	    if(toast != null)
	    {
	        toast.cancel();
	    }
	    toast = Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG);
	    toast.show();
	}
	
	private Runnable connectAndReceiveData = new Runnable(){
		@Override
		public void run() {
			try {
				connect(rescuePort);
				readResponse();
				disconnect();
				handler.sendEmptyMessage(0);
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	};
	
	private Runnable connectAndTransmitData = new Runnable(){
		@Override
		public void run() {
			try {
				connect(triagePort);
				writeResponse();
				disconnect();
				handler.sendEmptyMessage(1);
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	};
	
    public void connect(int port) throws UnknownHostException, IOException {
        Log.e("Client", "Attempting to connect to "+hostname+":"+port);
		socketClient = new Socket(hostname,port);
        Log.e("Client", "Connection Established");
        reader = new BufferedReader(new InputStreamReader(socketClient.getInputStream()));
		writer = new PrintWriter(socketClient.getOutputStream(), true);
    }
    
    private void disconnect() throws IOException {
    	Log.e("Client", "Disconnecting");
    	reader.close();
    	writer.close();
		socketClient.close();
		Log.e("Client", "Disconnected");
    }
    
    public void readResponse() throws IOException {
    	ArrayList<String> list = new ArrayList<String>(0);
    	String response = null;
    	while((response = reader.readLine()) != null){
    		if(response == "")
        		break;	//no data on server indicated by empty string being received
    		list.add(response);
    	}
    	synchronized(lock){
    		latLngPri.clear();
    		for(String s : list)
    			latLngPri.add(s);
    	}
    }
 
    public void findPath() {
    	synchronized(lock){
	    	if(latLngPri.isEmpty()){
	    		showToast("No data to show, get data from server first");
	    		return;
	    	}
    	}
    	ArrayList<double[]> priorityList1 = new ArrayList<double[]>(0);
    	ArrayList<double[]> priorityList2 = new ArrayList<double[]>(0);
    	ArrayList<double[]> priorityList3 = new ArrayList<double[]>(0);
    	ArrayList<double[]> priorityList4 = new ArrayList<double[]>(0); //Dead
    	ArrayList<double[]> priorityList5 = new ArrayList<double[]>(0); //Hazard - Fire
    	ArrayList<LatLng> allPoints = new ArrayList<LatLng>(0);
    	
        String delims = ",";
        String[] tokens;
        synchronized(lock){
	        for(String s : latLngPri) {
	        	 tokens = s.split(delims);
	        	 double[] values = new double[]{ Double.valueOf(tokens[0]), Double.valueOf(tokens[1]), Double.valueOf(tokens[2]),};
	        	 switch((int)values[2]) {
	        	 case 1:
	        		 priorityList1.add(values);
	        		 break;
	        	 case 2:
	        		 priorityList2.add(values);
	        		 break;
	        	 case 3:
	        		 priorityList3.add(values);
	        		 break;
	        	 case 4:
	        		 priorityList4.add(values);
	        		 break;
	        	 case 5:
	        		 priorityList5.add(values);
	        		 break;
	        	 default:
	        		 showToast("Unknown priority value = " + (int)values[2]);
	        		 break;
	        	 }
	        }
        }
        numPri1 = priorityList1.size();
    	numPri2 = priorityList2.size();
    	numPri3 = priorityList3.size();
    	numPri4 = priorityList4.size();
        numPri5 = priorityList5.size();
        if(numPri1 + numPri2 + numPri3 + numPri4 + numPri5 == 0) {
        	showToast("No data on server");
        	Log.e("get data", "No data to get from server");
        	return;
        }
        Func.sortData(allPoints, avgLatLng, priorityList1, priorityList2, priorityList3);
    	rescueList = new LatLng[allPoints.size()]; //put the results of the sorting into a global array of LatLng points to be used to draw PolyLine
    	for(int x=0; x<allPoints.size(); x++){
    		rescueList[x] = allPoints.get(x);
    	}
    	deadList = new LatLng[priorityList4.size()];
    	for(int x=0; x<priorityList4.size(); x++) {
    		deadList[x] = new LatLng(priorityList4.get(x)[0], priorityList4.get(x)[1]);
    	}
    	hazardList = new LatLng[priorityList5.size()];
    	for (int x=0; x<priorityList5.size();x++)
    	{
    		hazardList[x] = new LatLng(priorityList5.get(x)[0], priorityList5.get(x)[1]);
    	}
    }
    
    private void drawLinesAndPlot() {
    	if(rescueList != null) {
	    	if(!lineList.isEmpty()) {
	    		for(int x=0; x<lineList.size(); x++)
	    			lineList.get(x).remove();
	    	}
	    	lineList.clear();
	    	PolylineOptions options;
	    	for(int x=0; x<rescueList.length; x++){
	    		if(x != 0) {
	    			// our current position is the first point in the array, so ignore it when plotting the triage points
	    			LatLng curPos = rescueList[x];
	    			Marker curPosMarker = null;
	    			if(!Func.isConnectorLocation(curPos)) {
	    				//only put a marker if the point is an actual triage point
	    				if(numPri1 > 0) {
	    					//highest priority = red marker, know total num of priority 1 markers so decrement the count
		    				curPosMarker = mMap.addMarker(new MarkerOptions().position(curPos).title("TriagePt"+x).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)).draggable(true));
		    				numPri1--;
	    				}
	    				else if(numPri2 > 0) {
	    					//once there are no more priority 1 markers, know the next markers in the rescue list will be pri2
	    					curPosMarker = mMap.addMarker(new MarkerOptions().position(curPos).title("TriagePt"+x).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)).draggable(true));
	    					numPri2--;
	    				}
	    				else {
	    					//the rest in the rescueList should be pri3 so just set the rest to green
	    					curPosMarker = mMap.addMarker(new MarkerOptions().position(curPos).title("TriagePt"+x).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)).draggable(true));
	    					numPri3--;
	    					if(numPri3 < 0)
	    						showToast("Mismatched triage count; numPri3 = " + numPri3);
	    				}
	    			}
	    			landmarks.addMarker("TriagePt"+x, curPosMarker);
	    		}
	    		if(x < (rescueList.length-1)) {
	    			options = new PolylineOptions().width(widthTable[x%widthTable.length]).color(colorTable[x%colorTable.length]);
	    			options.add(rescueList[x], rescueList[x+1]);
	    			lineList.add(mMap.addPolyline(options));
	    			lineList.get(x).setZIndex(x+1);	//draw lines above the map tile overlay so they can be seen above the image of Marcus, and above one another to make it clear the order
	    		}
	    	}
    	}
    	if(deadList != null) {
	    	for(int x=0; x<deadList.length; x++){
	    		LatLng curPos = deadList[x];
    			Marker curPosMarker = null;
    			curPosMarker = mMap.addMarker(new MarkerOptions().position(curPos).title("DeadPt"+x).icon(BitmapDescriptorFactory.fromResource(R.drawable.skull)).draggable(true));
    			landmarks.addMarker("DeadPt"+x, curPosMarker);
	    	}
    	}
	    	if(hazardList != null) {
		    	for(int x=0; x<hazardList.length; x++){
		    		LatLng curPos = hazardList[x];
	    			Marker curPosMarker = null;
	    			curPosMarker = mMap.addMarker(new MarkerOptions().position(curPos).title("Hazardpt"+x).icon(BitmapDescriptorFactory.fromResource(R.drawable.fire)).draggable(true));
	    			landmarks.addMarker("Hazardpt"+x, curPosMarker);
		    	}
    	}
    }
    
    public void writeResponse() throws IOException {
    	PrintStream writer = new PrintStream (socketClient.getOutputStream(),true);
        synchronized (lock) {
        	for(int x=0; x<latLngPri.size(); x++) {
            	writer.println(latLngPri.get(x));
            	writer.flush();
            }
		}
    }
	
	public void showPopup(View v) {
	    PopupMenu popup = new PopupMenu(this, v);
	    MenuInflater inflater = popup.getMenuInflater();
	    popup.setOnMenuItemClickListener(menuClickListener);
	    inflater.inflate(R.menu.main, popup.getMenu());
	    popup.show();
	}
	
	@SuppressWarnings("deprecation")
	@Override
	protected void onResume() {
		IntentFilter latlngFilter = new IntentFilter(TagSearchingService.BROADCAST_LATLNG);
		IntentFilter testFilter = new IntentFilter("TEST");
		latlngReceiver = new LatLngReceiver();
		testReceiver = new TestReceiver();
		registerReceiver(latlngReceiver, latlngFilter);
		registerReceiver(testReceiver, testFilter);
		
		// for the system's orientation sensor registered listeners
		mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
				SensorManager.SENSOR_DELAY_GAME);
		
		Intent intent = new Intent(getApplicationContext(), TagSearchingService.class);
		startService(intent);
		super.onResume();
		
		avgLatLng[0] = marcusLat;
		avgLatLng[1] = marcusLng;
		showToast("Resumed");
	}

	@Override
	protected void onPause() {
		unregisterReceiver(latlngReceiver);
		unregisterReceiver(testReceiver);
		Intent intent = new Intent(getApplicationContext(), TagSearchingService.class);
		stopService(intent);
		mSensorManager.unregisterListener(this);
		super.onPause();
	}
	
	private void InitializeMarker(){
		/*
		String marcusBeacons = "" +
		"@Dis001,42.39354668258381,-72.52833187580109," +
		"@Dis002,42.39359447166392,-72.52839054912329," +
		"@Dis003,42.393661574404135,-72.52840027213097," +
		"@Dis004,42.39370936339676,-72.52845861017704," +
		"@Dis005,42.39377498034809,-72.52846665680408," +
		"@Dis006,42.39382895952455,-72.52852533012629," +
		"@Dis007,42.39389135741379,-72.52853605896235," +
		"@Dis008,42.393932460751394,-72.52859741449356," +
		"@Dis009,42.393999315520105,-72.52860512584448," +
		"@Dis010,42.39404537098601,-72.52866346389055," +
		"@Dis011,42.394103806904866,-72.52867016941309," +
		"@Dis012,42.39417412802318,-72.52873621881008," +
		"@Dis013,42.39423256382214,-72.52873655408621," +
		"@Dis014,42.39422736402869,-72.52877611666918," +
		"@Dis015,42.39416001428392,-72.52882674336433," +
		"@Dis016,42.39402060998703,-72.52868391573429," +
		"@Dis017,42.39397727821532,-72.52871174365282,";

		String[] marcusBeaconsArray = marcusBeacons.split("@");
		
		for(String marcusBeacon : marcusBeaconsArray){
			
			if(marcusBeacon.equals("")){
				continue;
			}
			int titleIndex = 0;
			int latitudeIndex = 1;
			int longitutdeIndex = 2;
			
			String[] beaconComponents = marcusBeacon.split(",");
			String beaconTitle = beaconComponents[titleIndex];
			double beaconLat = Double.parseDouble(beaconComponents[latitudeIndex]);
			double beaconLong = Double.parseDouble(beaconComponents[longitutdeIndex]);
			
			LatLng position = new LatLng(beaconLat,beaconLong);
			Marker marker = mMap.addMarker(new MarkerOptions().position(position).title(beaconTitle).draggable(true));
			landmarks.addMarker(beaconTitle, marker);
		}
		 */
		//put a first "dummy point" on the map, so when updateMap is called, there's a point to remove before re-adding our updated position
		LatLng myPos = new LatLng(42.39354668, -72.52833187);
		Marker myPosMarker = mMap.addMarker(new MarkerOptions().position(myPos).title("Current Position").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)).draggable(false));
		landmarks.addMarker("Current Position", myPosMarker);
		
		/*
		LatLng testPoint = new LatLng(42.39354668, -72.52833187);
		Marker testPointMarker = mMap.addMarker(new MarkerOptions().position(testPoint).title("TESTPOINT").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)).draggable(false));
		landmarks.addMarker("TESTPOINT", testPointMarker);
		*/
	}
	
	private void setupMap(){
		mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
		
		changeMapPositionAndZoom(new LatLng(marcusLat,marcusLng), knowlesZoom);
		MyUrlTileProvider mTileProvider = new MyUrlTileProvider(256, 256, mUrl);
		mMap.addTileOverlay(new TileOverlayOptions().tileProvider(mTileProvider).zIndex(0));
	    // display all the landmarks
		landmarks = new Landmarks();
		InitializeMarker();
	}

	@Override
	public boolean onOptionsItemSelected (MenuItem item){
		return true;
	}
	
	public class MyUrlTileProvider extends UrlTileProvider {

		private String baseUrl;

		public MyUrlTileProvider(int width, int height, String url) {
		    super(width, height);
		    this.baseUrl = url;
		}

		@Override
		public URL getTileUrl(int x, int y, int zoom) {
		    try {
		        return new URL(baseUrl.replace("{z}", ""+zoom).replace("{x}",""+x).replace("{y}",""+y));
		    } catch (MalformedURLException e) {
		        e.printStackTrace();
		    }
		    return null;
		}
	}
	
	private void changeMapPositionAndZoom(LatLng moveToPosition, int zoomLevel){
		changeMapPosition(moveToPosition);
		changeMapZoom(zoomLevel);
	}
	
	private void changeMapPosition(LatLng moveToPosition){
		CameraUpdate center = CameraUpdateFactory.newLatLng(moveToPosition);
		mMap.moveCamera(center);
	}
	
	private void changeMapZoom(int zoomLevel){
		CameraUpdate zoom=CameraUpdateFactory.zoomTo(zoomLevel);
		mMap.animateCamera(zoom);
	}
	
	public void updateMap() {
		landmarks.removeMarker("Current Position"); //removes from landmarks hashmap and removes marker from map
		LatLng myPos = new LatLng(avgLatLng[0], avgLatLng[1]);
		Marker myPosMarker = mMap.addMarker(new MarkerOptions().position(myPos).title("Current Position").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)).draggable(false));
		landmarks.addMarker("Current Position", myPosMarker);
	}
	
	private void plotBeacons(boolean[] activeBeacons) {
		//clear currently plotted beacons
		for(int x=0; x<isActive.length; x++){
			if(isActive[x]){
				//remove any currently plotted beacons
				landmarks.removeMarker("Beacon"+(x+1));
			}
		}
		isActive = activeBeacons;
		for(int x=0; x<isActive.length; x++){
			if(isActive[x]){
				LatLng myPos = new LatLng(coordVals[x][0], coordVals[x][1]);
				Marker myPosMarker = mMap.addMarker(new MarkerOptions().position(myPos).title("Beacon"+(x+1)).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA)).draggable(false));
				landmarks.addMarker("Beacon"+(x+1), myPosMarker);
			}
		}
	}

	public class TestReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			Bundle bundle = intent.getExtras();
			String threeClosest = bundle.getString("TESTRESULT");
			boolean[] activeBeacons = bundle.getBooleanArray("ACTIVE");
			if(debugMode)
				showToast("ClosestResult = " + threeClosest);
			plotBeacons(activeBeacons);
		}
	}
	
	public class LatLngReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			Bundle bundle = intent.getExtras();
			double[] tmpLatLng = bundle.getDoubleArray(TagSearchingService.BUNDLE_RESULT);
			if(tmpLatLng[0] == 0 && tmpLatLng[1] == 0) {
				showToast("GOT (0,0) LAT LNG");
				return;	//no beacons found (shouldn't get here anyway, if no beacons found, then handler shouldn't broadcast in the first place)
			}
			if(debugMode)
				return;
			lastLatLng[0] = curLatLng[0];
			lastLatLng[1] = curLatLng[1];
			curLatLng[0] = tmpLatLng[0];
			curLatLng[1] = tmpLatLng[1];
			double dist = Func.euclidDist(curLatLng, lastLatLng);
			if(locationCalibrated) {
				//should still be calibrated, ignore very different values unless it gets 5 bad values in a row, then set calibration flag to recalibrate with current location
				if(dist > DIST_THRESH) {
					//too big of a change between readings, must be an error so ignore the new value and use the last value as current value
					curLatLng[0] = lastLatLng[0];
					curLatLng[1] = lastLatLng[1];
					recalibrationCounter++;
					//showToast("recalCount = " + recalibrationCounter);
					if(recalibrationCounter == RECALIBRATION_LIMIT) {
						recalibrationCounter = 0;
						locationCalibrated = false;
						showToast("Too many bad vals, recalibrating...");
					}
					else {
						//Toast.makeText(getApplicationContext(), "Dist = " + dist + ", thresh = " + DIST_THRESH, Toast.LENGTH_SHORT).show();
					}
				}
				else {
					recalibrationCounter = 0;	//if good value, reset counter (only recalibrate when multiple bad values in a row)
				}
				avgLatLng[0] = (curLatLng[0]+lastLatLng[0])/2.0;
				avgLatLng[1] = (curLatLng[1]+lastLatLng[1])/2.0;
				double[] projectedLocation;
				Hall curLoc = Func.getLocation(avgLatLng);
				switch(curLoc){
				case MAIN:
					break;
				case TOPSIDE:
					projectedLocation = Func.projectionIntoHall(Hall.TOPSIDE, avgLatLng);
					avgLatLng = projectedLocation;
					break;
				case BOTSIDE:
					projectedLocation = Func.projectionIntoHall(Hall.BOTSIDE, avgLatLng);
					avgLatLng = projectedLocation;
					/*
					landmarks.removeMarker("TESTPOINT"); //removes from landmarks hashmap and removes marker from map
					LatLng testPoint = new LatLng(projectedLocation[0], projectedLocation[1]);
					Marker testPointMarker = mMap.addMarker(new MarkerOptions().position(testPoint).title("TESTPOINT").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)).draggable(false));
					landmarks.addMarker("TESTPOINT", testPointMarker);
					*/
					break;
				default:
					showToast("Unidentified Location");
					break;
				}
				
				updateMap();
			}
			else {
				//not calibrated, wait until two close values are gotten
				if(dist <= DIST_THRESH) {
					locationCalibrated = true;
					showToast("Location calibrated");
				}
			}
		}
	}
	
	@Override
	public void onMapClick(LatLng pos) {
		//when in debug mode, can manually set current location by touching map, and rotation with compass is disabled
		if(debugMode) {
			avgLatLng[0] = pos.latitude;
			avgLatLng[1] = pos.longitude;
			updateMap();
		}
		ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE); 
		ClipData clip = ClipData.newPlainText("POSITIONTHING", Double.toString(pos.latitude) + ", " + Double.toString(pos.longitude));
		clipboard.setPrimaryClip(clip);
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		
		CameraPosition currentPlace;
		if(debugMode) {
		currentPlace = new CameraPosition.Builder()
        	.target(new LatLng(avgLatLng[0], avgLatLng[1])).zoom(knowlesZoom+5).build();
		}
		else {
			float bearing = Math.round(event.values[0]) - 20;
			currentPlace = new CameraPosition.Builder()
		        .target(new LatLng(avgLatLng[0], avgLatLng[1]))
		        .bearing(bearing).zoom(knowlesZoom+5).build();
		}
		mMap.moveCamera(CameraUpdateFactory.newCameraPosition(currentPlace));
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		//do nothing
	}
}
