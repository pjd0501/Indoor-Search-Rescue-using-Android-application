# Indoor-Search-Rescue-using-Android-application
Indoor Search &amp; Rescue using Android application

Code flow

 App launched (SplashScreen)

 ModeSelector activity starts
{
Allows user to enter IP address in the text field.
when "Go" button is pressed: IP address bundled with intent, sent to MainActivity
}

 MainActivity activity starts
{


 TagSearchingService is started.
Toggle button at bottom switches between "Triage mode" and "Rescue mode"


 Triage mode:
1. Triage mode allows the user two functions: add victims to the map, and send data to the server.
2. Add victim to map:
3. Clicking "Add..." will show a dropdown menu with four options, "Lowest Priority, Low Priority,
Medium Priority, and Highest Priority, Hazards"
4. Each priority is associated with a color (Skullface, Green, Yellow, Red, respectively).
5. Choosing a priority will place a marker of that priority at the user's current location.


 Send Data:
1. Clicking "Send data" will take whatever victims the user has added to the map and send it to
some centralized server.
Note: Only the most recently sent data is stored on the server.


 Rescue Mode:
Rescue mode has two different options: Getting data from the server and finding the best path to
victims


 Get data:
Clicking "Get data" will cause the app to connect to the server and get the data from it, storing it locally
on the device.



 Find path:
Clicking "Find path" will use whatever data is stored on the device, plot the victims, and find a priorityweighted
path.
The path will take the rescuer to all the highest priority victims first, in order of shortest path between
them.
Then it takes the rescuer to the next highest priority victims in the shortest path.
Then it takes the rescuer to the low priority victims in the shortest path.
The lowest priority victims are assumed to be dead or not saveable and are therefor plotted, but not
included in the rescuer's wayfinding.
The wayfinding path navigates around hallway corners to give an an accurate representation of the path
the rescuer should take.
}


 TagSearchingService
{
The TagSearchingService runs while the MainActivity is running, providing a best guess at the user's
current location by using nearby iBeacons.
Each time the beacons are scanned, the location-finding algorithm is run, and the results are broadcast
so that the MainActivity can see the results and update the map accordingly.
}




 Algorithms:


 Location-finding algorithm:
Every time the beacons are scanned (every 800ms):
The 'isActive' boolean array is updated to reflect which beacons in the most recent scan were found.
The distance in feet to each beacon is estimated using it's RSSI value and the empirical equation
provided in class.
A running average of distances to each beacon is updated using the last 5 distance values.
Up to the closest three beacons are found with a quick search through the average distance of each,
only considering current active beacons.
If no beacons are found, the current location is set to LatLng 0,0 to indicate no beacons found.
If one beacon is found, that beacon is used as the current location.
If two or three beacons are found, then the current location is given as the weighted average of the
coordinates of those beacons, inversely proportional to the distance to those beacons.
The result of the location algorithm as well as the 'isActive' list of active beacons is broadcast to the
MainActivity for further processing.

 MainActivity Broadcast receiver for current location:
When the TagSeachingService broadcasts the result of the location calculation:
The location is checked to see if it's (0,0). That indicates no beacons were found so the user is notified in
a Toast and the function returns so the user's location on the map is not affected.
The current and last locations are both kept, and the distanc between the two is checked.
If the distance is above a certain threshold, a counter is incremented. If too many values in a row are too
far from the last value, the current location is recalibrated.
If the distance is within the threshold range, a function is called on the given location to check which
hallway the user is in. The reason is that if in one of the side halls, sometimes the location algorithm
picks up beacons from around the corner which causes the current location to be reported as inside a
room.


If in the main hall, do nothing as the main hall has enough beacons to prevent the problem from
occuring. If in one of the side halls, project the reported location onto a line in the middle of whichever
side hallway it's in.
Update a global double[] that holds the user's current location based on the above location checks.
Call the updateMap function to update the map with the user's current location.


 getLocation algorithm:
Two points on the map are chosen and hardcoded into the app which creates a line down the main
hallway where the main hall meets the side halls. A reference point is chosen to be in the main hallway,
and the signed area of the line and reference point is calculated at program startup.
When a point is passed into the function, the signed area of the point and the line is calculated, and if
the sign of the areas is the same as the reference point's, then the point passed into the function is on
the same side of the line as the reference point (eg: in the main hallway).

If the result is 0, the given point is on the line, and the user is assumed to be in the main hallway.
If the sign is different from that of the reference point's, then the user is in either the top side hallway,
or the bottom side hallway. There are two points hardcoded in the app that represent a line running
down the middle of each hallway, so the orthogonal distance from the user's current location is
calculated to each of these lines, and the lower distance indicates which hallway is closer, and therefor
which hallway the user is most likely in.

The return value of this function is the location of the given point, represented as the enum "Hall".

 Adding victim to current location on map:
When the user adds a victim to the map:
A menuClickListener is triggered, and switch statement is used to determine which priority was chosen.
Each case of the switch statement generates a marker of a different color which is then placed on the
map.

A new string indicating the latitude, longitude, and priority number (comma-separated) is added to a
global ArrayList (latLngPri) which holds the the current list of victims on the map

 Send data:
A socket connection to the given server IP on the triagePort is opened
Each string from the global ArrayList latLngPri is sent one at a time to the server
Connection closes with a Toast indicating that the data was sent.

 Get Data:
A socket connection to the given server IP on the rescuePort is opened
Either a null string is sent from the server (indicating no data to send) in which case the user is notified
with a Toast message, or if there is data, the server will send it one victim at a time in the same format
as it was sent ("lat,lng,priority").
The current 'latLngPri' ArrayList is cleared, and the data from the server is added to it.

 Find path:
When the find path button is pressed, two functions are run:


findPath() and drawLinesAndPlot()

 findPath()
findPath takes the global ArrayList of strings representing all the victims and iterates over each element.
It tokenizes each string, delimited by commas and switches on the third element (the priority number)
Each priority number has it's own ArrayList where the coordinates are stored.
Once the entire latLngPri list is parsed, a count of how many victims of each priority there were. The a
function 'sortData' is called which takes all the different priority ArrayLists.
It goes through each list, starting with the highest priority list, and calculates the closest distance victim
based on the user's current location.

It takes the current location and the next closest location, and calls the "addConnectorPoints" method.
The wayfinding needs to accout for walls/building corners, so two hardcoded coordinate values are
stored that represent the intersection of the top side hall with the main hall, and the intersection of the
bottom side hall with the main hall.
addConnectors checks the location of the current point and the next point, and runs a set of case
statements against it.

If the two points are in the same hall, no connector points are added to the wayfinding list.
If one is in the main hall and the other is in the top side hall, then the topSideHallConnector point is
injected into the wayfinding path. likewise for the bottom hall. And if the current Point is in the top hall,
and the next point is in the bottom hall, then the top hall connector is added first, followed by the
bottom hall connector.
Vice versa for the current point in bottom hall and next point in top hall.

When the next closest point is found and the connectorPoints are injected to accout for wall navigation,
that next point is set as the current points and algorithm keeps running in this fashion untill all points
from all priorities are pathed to. (not including lowest priority points since they're assumed to be
beyond help). The end result of sortData is an ordered list of points the user should go to to navigate to
all victims, starting with highest priority to lowest priority in the order of the shortest path through each.


 drawLinesAndPlot()
This function simply takes the ordered list of points to wayfind to, and draws line from one to the next
while plotting the victims as colored markers. Each line is a progressively cooler color and thinner line,
starting from red. This color/size scheme makes it easier for the rescuer to see which victim to go to
next when there are several victims in the area.
