package edu.group9.group9diorama;

import java.util.ArrayList;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import edu.group9.group9diorama.MainActivity.Hall;

public class Func {
	
	private static int signOfRefPoint;
	
	private static double[] mainHall_p1 = 		{42.39424222058029, -72.52879053354263};
	private static double[] mainHall_p2 = 		{42.39356277735578, -72.52837378531694};
	private static double[] topSideHall_p1 = 	{42.39416471886405, -72.52885054796934};
	private static double[] topSideHall_p2 = 	{42.39421349791067, -72.5287700816989};
	private static double[] botSideHall_p1 = 	{42.39397678299491, -72.52873655408621};
	private static double[] botSideHall_p2 = 	{42.39402754306773, -72.52865675836802};
	private static double[] refPoint = 		{42.39405131362432, -72.52861451357603};
	
	private static double[] topHallConnector = {42.39421869770528, -72.5287613645196};
	private static double[] botHallConnector = {42.39403348570772, -72.52864871174097};
	
	
	public static void funcInit() {
		signOfRefPoint = signOfArea(mainHall_p1, mainHall_p2, refPoint);
	}
	
	/*
	 * check signed area of line made up of p1, p2, and a third point ref. Can be used to check what side of a given line a point is one by comparing sign with signed are of a known point
	 */
	public static int signOfArea(double[] p1, double[] p2, double[] ref) {
		double signedArea = 0.5*((-p2[0]*p1[1]) + (ref[0]*p1[1]) + (p1[0]*p2[1]) - (ref[0]*p2[1]) - (p1[0]*ref[1]) + (p2[0]*ref[1]));
		if(signedArea > 0)
			return 1;
		else if (signedArea < 0)
			return -1;
		else
			return 0;
	}
	
	/*
	 * given a point, return the enum type of the location of that point
	 */
	public static Hall getLocation(double[] point) {
    	int signOfCurPos = signOfArea(mainHall_p1, mainHall_p2, point);
		if(signOfCurPos == signOfRefPoint) {
			return Hall.MAIN;
		}
		else{
			//current Pos is either directly on the line (sign = 0, assume this means in side hall) or in one of the two side halls
			double distToTopHall = distFromPointToLine(topSideHall_p1, topSideHall_p2, point);
			double distToBotHall = distFromPointToLine(botSideHall_p1, botSideHall_p2, point);
			if(distToTopHall <= distToBotHall)
				return Hall.TOPSIDE; //closer to top side hall
			else
				return Hall.BOTSIDE;
		}
    }
	
	/*
	 * calculate the point that is the projection of a given point onto a given hall
	 */
	public static double[] projectionIntoHall(Hall hall, double[] point) {
		double[] l0;
		double[] l1;
		if(hall == Hall.TOPSIDE) {
			l0 = topSideHall_p1;
			l1 = topSideHall_p2;
		}
		else if(hall == Hall.BOTSIDE){
			l0 = botSideHall_p1;
			l1 = botSideHall_p2;
		}
		else{
			System.out.println("Unsupported hall for projection onto line");
			return null;
		}
		double[] projectedPoint = new double[2];
		double y1 = l0[1] + (l1[1] - l0[1]) * ((point[0] - l0[0]) / (l1[0] - l0[0]));
		double x1 = point[0];
		
		double y2 = point[1];
        double x2 = l0[0] + (l1[0] - l0[0]) * ((point[1] - l0[1]) / (l1[1] - l0[1]));

        projectedPoint[0] = (x1 + x2) / 2.0;
        projectedPoint[1] = (y1 + y2) / 2.0;
        
		return projectedPoint;
	}
	
	/*
	 * calculate the orthogonal distance from a given point to a line given by two other points
	 */
	private static double distFromPointToLine(double[]l0, double[] l1, double[] point) {
		double A = l0[1] - l1[1];
		double B = l1[0] - l0[0];
		double C = (l0[0]*l1[1]) - (l1[0]*l0[1]);
		//now have Ax + By + C = 0 form of line formed by l0, l1
		
		double dist = Math.abs(A*point[0] + B*point[1] + C)/Math.sqrt(Math.pow(A, 2.0) + Math.pow(B, 2.0));
		return dist;
	}
	
	/*
	 * figure out if given LatLng position is actually just a "connector" position and not a triage victim point
	 */
    public static boolean isConnectorLocation(LatLng posToCheck) {
    	LatLng topHallConnectorLatLng = new LatLng(topHallConnector[0], topHallConnector[1]);
    	LatLng botHallConnectorLatLng = new LatLng(botHallConnector[0], botHallConnector[1]);
    	if(posToCheck.equals(topHallConnectorLatLng) || posToCheck.equals(botHallConnectorLatLng))
    		return true;
    	else
    		return false;
    }
    
    
    public static void sortData(ArrayList<LatLng> allPoints, double[] startingLocation,
    							ArrayList<double[]> pri1, ArrayList<double[]> pri2, ArrayList<double[]> pri3) {
    	allPoints.add(new LatLng(startingLocation[0], startingLocation[1]));
    	double tmpDist;
    	
    	double curClosestDist = 100;
    	int curClosestInd = 0;
    	double[] curPos = startingLocation;
    	
    	while(!pri1.isEmpty()) {
    		curClosestDist = 100;
    		curClosestInd = 0;
    		for(int x=0; x<pri1.size(); x++) {
    			tmpDist = euclidDist(curPos, new double[]{pri1.get(x)[0], pri1.get(x)[1]});
    			if(tmpDist < curClosestDist) {
    				curClosestDist = tmpDist;
    				curClosestInd = x;
    			}
    		}
    		addConnectorPoints(allPoints, curPos, new double[]{pri1.get(curClosestInd)[0], pri1.get(curClosestInd)[1]});
    		allPoints.add(new LatLng(
    								pri1.get(curClosestInd)[0],
    								pri1.get(curClosestInd)[1]));
    		curPos = new double[]{pri1.get(curClosestInd)[0], pri1.get(curClosestInd)[1]};
    		pri1.remove(curClosestInd);
    	}
    	
    	while(!pri2.isEmpty()) {
    		curClosestDist = 100;
    		curClosestInd = 0;
    		for(int x=0; x<pri2.size(); x++) {
    			tmpDist = euclidDist(curPos, new double[]{pri2.get(x)[0], pri2.get(x)[1]});
    			if(tmpDist < curClosestDist) {
    				curClosestDist = tmpDist;
    				curClosestInd = x;
    			}
    		}
    		//add intermediate navigation points to get around walls
    		addConnectorPoints(allPoints, curPos, new double[]{pri2.get(curClosestInd)[0], pri2.get(curClosestInd)[1]});
    		allPoints.add(new LatLng(
    								pri2.get(curClosestInd)[0],
    								pri2.get(curClosestInd)[1]));
    		curPos = new double[]{pri2.get(curClosestInd)[0], pri2.get(curClosestInd)[1]};
    		pri2.remove(curClosestInd);
    	}
    	
    	while(!pri3.isEmpty()) {
    		curClosestDist = 100;
    		curClosestInd = 0;
    		for(int x=0; x<pri3.size(); x++) {
    			tmpDist = euclidDist(curPos, new double[]{pri3.get(x)[0], pri3.get(x)[1]});
    			if(tmpDist < curClosestDist) {
    				curClosestDist = tmpDist;
    				curClosestInd = x;
    			}
    		}
    		addConnectorPoints(allPoints, curPos, new double[]{pri3.get(curClosestInd)[0], pri3.get(curClosestInd)[1]});
    		allPoints.add(new LatLng(
    								pri3.get(curClosestInd)[0],
    								pri3.get(curClosestInd)[1]));
    		curPos = new double[]{pri3.get(curClosestInd)[0], pri3.get(curClosestInd)[1]};
    		pri3.remove(curClosestInd);
    	}
    }
    
    /*
     * returns number of connector points added to list
     */
    public static int addConnectorPoints(ArrayList<LatLng> allPoints, double[] point1, double[] point2) {
    	Hall loc1 = getLocation(point1); //from this location
    	Hall loc2 = getLocation(point2); //to this location
    	Log.e("LOCATION", "loc1 = " + loc1.toString() + ", loc2 = " + loc2.toString());
    	if(loc1 == loc2) {
    		return 0;
    	}
    	else if(((loc1 == Hall.TOPSIDE) && (loc2 == Hall.MAIN)) || ((loc1 == Hall.MAIN) && (loc2 == Hall.TOPSIDE))) {
    		allPoints.add(new LatLng(topHallConnector[0], topHallConnector[1]));
    		return 1;
    	}
    	else if(((loc1 == Hall.BOTSIDE) && (loc2 == Hall.MAIN)) || ((loc1 == Hall.MAIN) && (loc2 == Hall.BOTSIDE))) {
    		allPoints.add(new LatLng(botHallConnector[0], botHallConnector[1]));
    		return 1;
    	}
    	else if((loc1 == Hall.BOTSIDE) && (loc2 == Hall.TOPSIDE)) {
    		allPoints.add(new LatLng(botHallConnector[0], botHallConnector[1]));
    		allPoints.add(new LatLng(topHallConnector[0], topHallConnector[1]));
    		return 2;
    	}
    	else if((loc1 == Hall.TOPSIDE) && (loc2 == Hall.BOTSIDE)) {
    		allPoints.add(new LatLng(topHallConnector[0], topHallConnector[1]));
    		allPoints.add(new LatLng(botHallConnector[0], botHallConnector[1]));
    		return 2;
    	}
    	else {
    		System.out.println("addConnectorPoints: unchecked location combo");
    		return 0;
    	}
    }
    
	public static double euclidDist(double[] p1, double[] p2) {
		return Math.sqrt(Math.pow((p1[0] - p2[0]), 2.0) + Math.pow((p1[1] - p2[1]),2.0));
	}
	
}
