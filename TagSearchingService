package edu.group9.group9diorama;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public class TagSearchingService extends Service implements BeaconConsumer{
	
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
	
	public static final String BROADCAST_LATLNG = "BROADCAST_LATLNG";
	public static final String BUNDLE_RESULT = "BUNDLE_RESULT";
	
	private static boolean studentTagMode = false;
	public static double[] latlng = new double[2];
	public static String POS = "position";
	private final int AVG_WINDOW_SIZE = 5;
	private double[][] landmarkDistLog = new double[17][AVG_WINDOW_SIZE];
	private int[] landmarkAvg = new int[17];
	private int[] closestInds = new int[3];
	private int curIndex = 0;
	
	private boolean[] isActive = new boolean[17];
	
	/** Log for TagSearchingActivity. */
	private static final String TAG_SEARCHING_ACTIVITY_LOG = "TAG_SEA_ACT_LOG";
	
	private final String landmarkTagIdentifier = "00100001";
	private final String studentTagIdentifier = "10011101";
	private List<Beacon> discoveredBeaconList;
	
	/** The map used for storing discovered beacons */
	protected HashMap<String, Beacon> discoveredBeaconMap;
	
	/** Declare and initiate the a BeaconManager object.*/
	private BeaconManager beaconManager = BeaconManager
			.getInstanceForApplication(this);
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.w("Tag searching service", "onStartCommand");
		discoveredBeaconMap = new HashMap<String, Beacon>();
		discoveredBeaconList = new ArrayList<Beacon>();
		beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25"));
		beaconManager.setForegroundScanPeriod(800);
		beaconManager.bind(this);
		
		for(int y=0; y<17; y++){
			for(int x=0; x<AVG_WINDOW_SIZE; x++){
				landmarkDistLog[y][x] = 31.0;	//start with all dists far away so the first avg calc isnt' using a bunch of 0s
			}
			landmarkAvg[y] = 31;
		}
		
		return Service.START_NOT_STICKY;
	}
	
	@Override
	public void onDestroy() {
		Log.w("Tag searching service", "onDestroy");
		super.onDestroy();
		beaconManager.unbind(this);
	}
	
	@Override
	public IBinder onBind(Intent intent) { return null; }

	private double rssiToFeet(int rssi) {
		return (double)(-0.00903*Math.pow(rssi, 2.0)) - (2.171*rssi) - 94.0;
	}
	
	private double arrayAverage(double[] values) {
		double sum = 0;
		for(int x=0; x<values.length; x++)
			sum+=values[x];
		return sum/values.length;
	}
	
	private boolean noDuplicates(int[] arrayToCheck, int indexToCheck) {
		for(int x=0; x<arrayToCheck.length; x++) {
			if(arrayToCheck[x] == indexToCheck)
				return false;	//index is already in the array
		}
		return true;
	}
	
	/*
	 * returns number of beacons still required for proper location calculation if three are not found, otherwise it updates the indices of the three closest beacons
	 */
	private int findClosest() {
		int[] closestVals = {500, 500, 500};
		for(int x=0; x<closestInds.length; x++)
			closestInds[x] = -1;
		for(int c=0; c<closestInds.length; c++) {
			for(int x=0; x<landmarkAvg.length; x++) {
				if(isActive[x]){
					//only check if beacon is one of the closest if it's active, otherwise ignore it
					if((landmarkAvg[x] < closestVals[c]) && noDuplicates(closestInds, x)) {
						//if this value hasn't already been found to one of the closest already, then it's currently the next closest at the moment
						closestVals[c] = landmarkAvg[x];
						closestInds[c] = x;
					}
				}
			}
		}
		
		if(closestInds[0] == -1) {
			Log.w("findClosest", "No closest beacons found, need 3 more");
			return 3;
		}
		else if(closestInds[1] == -1) {
			Log.w("findClosest", "One beacon found, need 2 more");
			return 2;
		}
		else if(closestInds[2] == -1) {
			Log.w("findClosest", "Two beacons found, need 1 more");
			return 1;
		}
		else
			return 0;
	}

	private void weightedAvg() {
		int id;
		double curDist;
		
		if(curIndex == AVG_WINDOW_SIZE)
			curIndex = 0;
		
		for(int x=0; x<isActive.length; x++){
			isActive[x] = false;	//clear active beacon list so only currently in-range ones are used in location calc
		}
		
		//iterate over found beacons, update distances/average distance to each one found
		for(int x=0; x<discoveredBeaconList.size(); x++) {
			
			id = discoveredBeaconList.get(x).getId3().toInt();
			id-=1;	//ID is 1-indexed, arrays are 0-indexed
			curDist = rssiToFeet(discoveredBeaconList.get(x).getRssi());
			
			landmarkDistLog[id][curIndex] = curDist;
			landmarkAvg[id] = (int)Math.round(arrayAverage(landmarkDistLog[id]));
			
			isActive[id] = true;
			//latlng[0] = 42.39370936339676 + 0.00001*curIndex;
			//latlng[1] = -72.52845861017704;
		}
		
		String closestBeacons = null;
		//find the three closest beacons, or indicate that three not found
		int closestResult = findClosest();
		if (closestResult != 0) {
			if(closestResult == 3) {
				Log.e("doCalculations", "No beacons found..."); //shouldn't get here anyway due to for loop iterating over discovered beacon list
				latlng[0] = 0;
				latlng[1] = 0;
				closestBeacons = "No beacons in range";
			}
			else if (closestResult == 2) {
				Log.w("doCalculations", "One beacon found, using closest beacon as location");
				latlng[0] = coordVals[closestInds[0]][0];
				latlng[1] = coordVals[closestInds[0]][1];
				closestBeacons = Integer.toString(closestInds[0]+1);
			}
			else if (closestResult == 1) {
				double distA = (double)landmarkAvg[closestInds[0]];
				double distB = (double)landmarkAvg[closestInds[1]];
				double totalDist = (double)(distA + distB);
				double invRatioA = totalDist/distA;
				double invRatioB = totalDist/distB;
				double totalRatio = invRatioA + invRatioB;
				double weightA = invRatioA/totalRatio;
				double weightB = invRatioB/totalRatio;
				
				latlng[0] = coordVals[closestInds[0]][0]*weightA + coordVals[closestInds[1]][0]*weightB;
				latlng[1] = coordVals[closestInds[0]][1]*weightA + coordVals[closestInds[1]][1]*weightB;
				closestBeacons = (closestInds[0]+1) + "," + (closestInds[1]+1);
			}
			else {
				Log.e("CLOSESTRESULT USAGE", "something wrong, should never get this");
			}
		}
		else {
			double totalDist = (double)(landmarkAvg[closestInds[0]] + landmarkAvg[closestInds[1]] + landmarkAvg[closestInds[2]]);
			double distA = (double)landmarkAvg[closestInds[0]];
			double distB = (double)landmarkAvg[closestInds[1]];
			double distC = (double)landmarkAvg[closestInds[2]];
			double invRatioA = totalDist/distA;
			double invRatioB = totalDist/distB;
			double invRatioC = totalDist/distC;
			double totalRatio = invRatioA + invRatioB + invRatioC;
			double weightA = invRatioA/totalRatio;
			double weightB = invRatioB/totalRatio;
			double weightC = invRatioC/totalRatio;
			
			latlng[0] = coordVals[closestInds[0]][0]*weightA + coordVals[closestInds[1]][0]*weightB + coordVals[closestInds[2]][0]*weightC;
			latlng[1] = coordVals[closestInds[0]][1]*weightA + coordVals[closestInds[1]][1]*weightB + coordVals[closestInds[2]][1]*weightC;
			closestBeacons = (closestInds[0]+1) + "," + (closestInds[1]+1) + "," + (closestInds[2]+1);
		}
		
		Intent i = new Intent("TEST");
		i.putExtra("TESTRESULT", closestBeacons);
		i.putExtra("ACTIVE", isActive);
		sendBroadcast(i);
		
		//by now, trilateration should have been done or our position defaulted to the nearest landmark if not enough beacons for trilateration
		//broadcast results for map activity to update postion
		Intent intent = new Intent(BROADCAST_LATLNG);
		intent.putExtra(BUNDLE_RESULT, latlng);
		sendBroadcast(intent);
		
		curIndex++;
	}
	
	private Runnable doCalculationsThread = new Runnable(){
		@Override
		public void run() {
			weightedAvg();
		}};
	
	
	/**
	 * Refresh the list of beacon according to current values in the map and
	 * then notify the list UI to change.
	 */
	private void updateDiscoveredList() {
		discoveredBeaconList.clear();
		Iterator<Beacon> bIter = discoveredBeaconMap.values().iterator();
		while (bIter.hasNext()) {
			discoveredBeaconList.add(bIter.next());
		}
		Thread thread = new Thread(null, doCalculationsThread, "doCalThread");
		thread.start();
	}
	
	@Override
	public void onBeaconServiceConnect() {
		Log.w("TagSearchingService", "onBeaconServiceConnect");
		
		beaconManager.setRangeNotifier(new RangeNotifier() {
			@Override
			public void didRangeBeaconsInRegion(Collection<Beacon> beacons,
					Region region) {
				if (beacons.size() > 0) {
					Log.i(TAG_SEARCHING_ACTIVITY_LOG, "Found " + beacons.size()
							+ "beacons");
					for (Iterator<Beacon> bIterator = beacons.iterator(); bIterator
							.hasNext();) {
						final Beacon beacon = bIterator.next();
						if (isGimbalTag(beacon)) {
							// generate the HashMap key, which is the
							// combination of tag's UUID, Major and Minor; But
							// you can always choose your own key
							final String key = new StringBuilder()
									.append(beacon.getId1())
									.append(beacon.getId2())
									.append(beacon.getId3()).toString();
							discoveredBeaconMap.put(key, beacon);
						}
					}
					updateDiscoveredList();
				}
			}
		});

		try {
			beaconManager.startRangingBeaconsInRegion(new Region(
					"myRangingUniqueId", null, null, null));
		} catch (RemoteException e) {
		}
	}
	
	/**
	 * A filter check whether the detected beacon is a Gimbal tag used for
	 * project.
	 * 
	 * @param beacon
	 *            The detected beacon
	 * @return Whether the beacon is a Gimbal tag for project or not.
	 */
	private boolean isGimbalTag(Beacon beacon) {
		final String uuid = beacon.getId1().toString();
		final String tagIdentifier = uuid.split("-")[0];
		if (tagIdentifier.equals(studentTagMode ? studentTagIdentifier : landmarkTagIdentifier)) {
			return true;
		}
		return false;
	}

}
