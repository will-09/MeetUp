package ca.ubc.cs.cpsc210.meetup.map;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.ResourceProxy;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.OverlayManager;
import org.osmdroid.views.overlay.PathOverlay;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import java.util.Random;



import ca.ubc.cs.cpsc210.meetup.R;
import ca.ubc.cs.cpsc210.meetup.model.Building;
import ca.ubc.cs.cpsc210.meetup.model.Course;
import ca.ubc.cs.cpsc210.meetup.model.CourseFactory;
import ca.ubc.cs.cpsc210.meetup.model.Place;
import ca.ubc.cs.cpsc210.meetup.model.PlaceFactory;
import ca.ubc.cs.cpsc210.meetup.model.Schedule;
import ca.ubc.cs.cpsc210.meetup.model.Section;
import ca.ubc.cs.cpsc210.meetup.model.Student;
import ca.ubc.cs.cpsc210.meetup.model.StudentManager;
import ca.ubc.cs.cpsc210.meetup.util.LatLon;
import ca.ubc.cs.cpsc210.meetup.util.SchedulePlot;

import static java.lang.Math.abs;
import static java.lang.Math.random;


/**
 * Fragment holding the map in the UI.
 */
public class MapDisplayFragment extends Fragment {

    /**
     * Log tag for LogCat messages
     */
    private final static String LOG_TAG = "MapDisplayFragment";
    /**
     * A central location in campus that might be handy.
     */
    private final static GeoPoint UBC_MARTHA_PIPER_FOUNTAIN = new GeoPoint(49.264865,
            -123.252782);
    /**
     * FourSquare URLs. You must complete the client_id and client_secret with values
     * you sign up for.
     */
    private static String FOUR_SQUARE_URL = "https://api.foursquare.com/v2/venues/explore";
    private static String FOUR_SQUARE_CLIENT_ID = "";
    private static String FOUR_SQUARE_CLIENT_SECRET = "";
    private static int ME_ID = 999999;
    /**
     * Meetup Service URL
     * CPSC 210 Students: Complete the string.
     */
    private final String getStudentURL = "";
    /**
     * Preference manager to access user preferences
     */
    private SharedPreferences sharedPreferences;
    /**
     * String to know whether we are dealing with MWF or TR schedule.
     * You will need to update this string based on the settings dialog at appropriate
     * points in time. See the project page for details on how to access
     * the value of a setting.
     */
    private String activeDay = "MWF";
    /**
     * Overlays for displaying my schedules, buildings, etc.
     */
    private List<PathOverlay> scheduleOverlay;
    private ItemizedIconOverlay<OverlayItem> buildingOverlay;
    private OverlayItem selectedBuildingOnMap;
    /**
     * View that shows the map
     */
    private MapView mapView;
    /**
     * Access to domain model objects. Only store "me" in the studentManager for
     * the base project (i.e., unless you are doing bonus work).
     */
    private StudentManager studentManager;
    private Student randomStudent = null;
    private Student me = null;
    private CourseFactory cf = CourseFactory.getInstance(); //created for the purpsoe of task5
    private Set<Student> randomStudents = new HashSet<Student>();

    /**
     * Map controller for zooming in/out, centering
     */
    private IMapController mapController;

    // ******************** Android methods for starting, resuming, ...

    // You should not need to touch this method
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
        sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(getActivity());
        scheduleOverlay = new ArrayList<PathOverlay>();

        // You need to setup the courses for the app to know about. Ideally
        // we would access a web service like the UBC student information system
        // but that is not currently possible
        initializeCourses();

        // Initialize the data for the "me" schedule. Note that this will be
        // hard-coded for now
        initializeMySchedule();

        // You are going to need an overlay to draw buildings and locations on the map
        buildingOverlay = createBuildingOverlay();
    }

    // You should not need to touch this method
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK)
            return;
    }

    // You should not need to touch this method
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        if (mapView == null) {
            mapView = new MapView(getActivity(), null);

            mapView.setTileSource(TileSourceFactory.MAPNIK);
            mapView.setClickable(true);
            mapView.setBuiltInZoomControls(true);
            mapView.setMultiTouchControls(true);

            mapController = mapView.getController();
            mapController.setZoom(mapView.getMaxZoomLevel() - 2);
            mapController.setCenter(UBC_MARTHA_PIPER_FOUNTAIN);
        }

        return mapView;
    }

    // You should not need to touch this method
    @Override
    public void onDestroyView() {
        Log.d(LOG_TAG, "onDestroyView");
        ((ViewGroup) mapView.getParent()).removeView(mapView);
        super.onDestroyView();
    }

    // You should not need to touch this method
    @Override
    public void onDestroy() {
        Log.d(LOG_TAG, "onDestroy");
        super.onDestroy();
    }

    // You should not need to touch this method
    @Override
    public void onResume() {
        Log.d(LOG_TAG, "onResume");
        super.onResume();
    }

    // You should not need to touch this method
    @Override
    public void onPause() {
        Log.d(LOG_TAG, "onPause");
        super.onPause();
    }

    /**
     * Save map's zoom level and centre. You should not need to
     * touch this method
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.d(LOG_TAG, "onSaveInstanceState");
        super.onSaveInstanceState(outState);

        if (mapView != null) {
            outState.putInt("zoomLevel", mapView.getZoomLevel());
            IGeoPoint cntr = mapView.getMapCenter();
            outState.putInt("latE6", cntr.getLatitudeE6());
            outState.putInt("lonE6", cntr.getLongitudeE6());
            Log.i("MapSave", "Zoom: " + mapView.getZoomLevel());
        }
    }

    // ****************** App Functionality

    /**
     * Show my schedule on the map. Every time "me"'s schedule shows, the map
     * should be cleared of all existing schedules, buildings, meetup locations, etc.
     */
    public void showMySchedule() {

        // CPSC 210 Students: You must complete the implementation of this method.
        // The very last part of the method should call the asynchronous
        // task (which you will also write the code for) to plot the route
        // for "me"'s schedule for the day of the week set in the Settings

        // Asynchronous tasks are a bit onerous to deal with. In order to provide
        // all information needed in one object to plot "me"'s route, we
        // create a SchedulePlot object and pass it to the asynchrous task.
        // See the project page for more details.


        // Get a routing between these points. This line of code creates and calls
        // an asynchronous task to do the calls to MapQuest to determine a route
        // and plots the route.
        // Assumes mySchedulePlot is a create and initialized SchedulePlot object

        // UNCOMMENT NEXT LINE ONCE YOU HAVE INSTANTIATED mySchedulePlot
        //  new GetRoutingForSchedule().execute(mySchedulePlot);

        clearSchedules();

        activeDay = sharedPreferences.getString("dayOfWeek", "MWF");

        SchedulePlot mySchedulePlot = new SchedulePlot(me.getSchedule().getSections(activeDay), me.getFirstName()+" "+me.getLastName(), "#FF0000", 17301620);
        new GetRoutingForSchedule().execute(mySchedulePlot);
    }

    /**
     * Retrieve a random student's schedule from the Meetup web service and
     * plot a route for the schedule on the map. The plot should be for
     * the given day of the week as determined when "me"'s schedule
     * was plotted.
     */
    public void showRandomStudentsSchedule() {
        // To get a random student's schedule, we have to call the MeetUp web service.
        // Calling this web service requires a network access to we have to
        // do this in an asynchronous task. See below in this class for where
        // you need to implement methods for performing the network access
        // and plotting.
        new GetRandomSchedule().execute();
    }

    /**
     * Clear all schedules on the map
     */
    public void clearSchedules() {
        randomStudent = null;
        randomStudents.clear();
        OverlayManager om = mapView.getOverlayManager();
        om.clear();
        scheduleOverlay.clear();
        buildingOverlay.removeAllItems();
        om.addAll(scheduleOverlay);
        om.add(buildingOverlay);
        mapView.invalidate();

    }

    /**
     * Find all possible locations at which "me" and random student could meet
     * up for the set day of the week and the set time to meet and the set
     * distance either "me" or random is willing to travel to meet.
     * A meetup is only possible if both "me" and random are free at the
     * time specified in the settings and each of us must have at least an hour
     * (>= 60 minutes) free. You should display dialog boxes if there are
     * conditions under which no meetup can happen (e.g., me or random is
     * in class at the specified time)
     */
    public void findMeetupPlace() {

        // CPSC 210 students: you must complete this method
        Schedule rSchedule;
        Set<String> bt;
        boolean rs = false; //= randomStudent.getSchedule().containBreak(bt1, timeOfDay);
        boolean acc = true;
        boolean last = false;


        Set<String> bt2 = me.getSchedule().getOneHourBreak(activeDay);
        PlaceFactory pf = PlaceFactory.getInstance();
        String timeOfDay = sharedPreferences.getString("timeOfDay", "12");
        boolean ms = me.getSchedule().containBreak(bt2,timeOfDay);

        for(Student s: randomStudents){

            rSchedule = s.getSchedule();
            bt = rSchedule.getOneHourBreak(activeDay);
            rs = s.getSchedule().containBreak(bt, timeOfDay);
            last = acc && rs;
            acc = last;
        }



        Set<Place> placeAcc = null;

        if (last && ms){

            String wantedDistance = sharedPreferences.getString("placeDistance", "250");
            LatLon meLL =me.getSchedule().whereAmI(activeDay, timeOfDay).getLatLon();
            Set<Place> meetPlacesMe = pf.findPlacesWithinDistance(meLL, Integer.parseInt(wantedDistance));


            for(Student s: randomStudents){
                LatLon rsLL = s.getSchedule().whereAmI(activeDay, timeOfDay).getLatLon();
                Set<Place> meetPlaceRs = pf.findPlacesWithinDistance(rsLL, Integer.parseInt(wantedDistance));

                if (placeAcc == null){
                    placeAcc = meetPlaceRs;
                }
                else{
                    placeAcc.retainAll(meetPlaceRs);
                }

            }



            boolean changed = placeAcc.retainAll(meetPlacesMe);

            if (placeAcc.isEmpty()){
                AlertDialog aDialog1 = createSimpleDialog("Cannot find a place to meet up. Try changing the Place Distance Option or Food Type");
                aDialog1.show();
            }
            else{


            for(Place p: meetPlacesMe) {

                AlertDialog aDialog1 = createSimpleDialog("Places to meet up have been plotted");
                aDialog1.show();
                plotABuilding(new Building(p.getName(), p.getLatLon()), p.getName(), p.getPrice() , 17301618);
            }
            }


        }
        else {
            AlertDialog aDialog1 = createSimpleDialog("You and random student(s) cannot meet up at " + timeOfDay+ " because there is no common breaktime");
            aDialog1.show();

        }




    }

    /**
     * Initialize the PlaceFactory with information from FourSquare
     */
    public void initializePlaces() {
        // CPSC 210 Students: You should not need to touch this method, but
        // you will have to implement GetPlaces below.
        new GetPlaces().execute();
    }


    /**
     * Plot all buildings referred to in the given information about plotting
     * a schedule.
     * @param schedulePlot All information about the schedule and route to plot.
     */
    private void plotBuildings(SchedulePlot schedulePlot) {

        // CPSC 210 Students: Complete this method by plotting each building in the
        // schedulePlot with an appropriate message displayed





        SortedSet<Section> sections = schedulePlot.getSections();
        for (Section s : sections){

            String name = schedulePlot.getName();
            String courseCode = s.getCourse().getCode();
            int courseNumber = s.getCourse().getNumber();
            String sectionName = s.getName();
            String courseStartTime = s.getCourseTime().getStartTime();
            String courseEndTime = s.getCourseTime().getEndTime();


            String msg =
                    "Name: "+name+"\n"+
                    "Section: "+sectionName+"\n"+
                    "Course code: "+ courseCode+"\n"+
                    "Course number: "+courseNumber+"\n"+
                    "Time: "+courseStartTime+" - "+courseEndTime;




            plotABuilding(s.getBuilding(), s.getBuilding().getName(), msg ,schedulePlot.getIcon());
        }






   

        // CPSC 210 Students: You will need to ensure the buildingOverlay is in
        // the overlayManager. The following code achieves this. You should not likely
        // need to touch it
        OverlayManager om = mapView.getOverlayManager();
        om.add(buildingOverlay);

    }

    /**
     * Plot a building onto the map
     * @param building The building to put on the map
     * @param title The title to put in the dialog box when the building is tapped on the map
     * @param msg The message to display when the building is tapped
     * @param drawableToUse The icon to use. Can be R.drawable.ic_action_place (or any icon in the res/drawable directory)
     */
    private void plotABuilding(Building building, String title, String msg, int drawableToUse) {
        // CPSC 210 Students: You should not need to touch this method
        OverlayItem buildingItem = new OverlayItem(title, msg,
                new GeoPoint(building.getLatLon().getLatitude(), building.getLatLon().getLongitude()));

        //Create new marker
        Drawable icon = this.getResources().getDrawable(drawableToUse);

        //Set the bounding for the drawable
        icon.setBounds(
                0 - icon.getIntrinsicWidth() / 2, 0 - icon.getIntrinsicHeight(),
                icon.getIntrinsicWidth() / 2, 0);

        //Set the new marker to the overlay
        buildingItem.setMarker(icon);
        buildingOverlay.addItem(buildingItem);
    }



    /**
     * Initialize your schedule by coding it directly in. This is the schedule
     * that will appear on the map when you select "Show My Schedule".
     */
    private void initializeMySchedule() {

        me = new Student ("Lee", "Woo", 123);


        CourseFactory courseFactory = CourseFactory.getInstance();


        Building dmpBuilding = new Building("DMP", new LatLon(49.261474, -123.248060));




        Course engl222 = courseFactory.getCourse("ENGL", 222);
        Section aSection = new Section("007", "MWF", "14:00", "14:50", new Building("Buchanan", new LatLon(49.269258, -123.254784)));
        engl222.addSection(aSection);
        aSection.setCourse(engl222);
        //me.getSchedule().add(aSection);


        Course cpsc210 = courseFactory.getCourse("CPSC", 210);
        aSection = new Section("202", "MWF", "10:00", "11:50", dmpBuilding);
        cpsc210.addSection(aSection);
        aSection.setCourse(cpsc210);
        me.getSchedule().add(aSection);

        Course scie220 = courseFactory.getCourse("SCIE", 220);
        aSection = new Section("200", "MWF", "15:00", "15:50", new Building("Swing", new LatLon(49.262786, -123.255044)));
        scie220.addSection(aSection);
        aSection.setCourse(scie220);
        //me.getSchedule().add(aSection);


        Course fren102 = courseFactory.getCourse("FREN", 102);
        aSection = new Section("202", "MWF", "11:00", "11:50", new Building("Barber", new LatLon(49.267442,-123.252471)));
        fren102.addSection(aSection);
        aSection.setCourse(fren102);
        //me.getSchedule().add(aSection);



        Course eosc222 = courseFactory.getCourse("EOSC", 222);
        aSection = new Section("200", "TR", "11:00", "12:20", new Building("ESB", new LatLon(49.262866, -123.25323)));
        eosc222.addSection(aSection);
        aSection.setCourse(eosc222);
        me.getSchedule().add(aSection);

        Course biol201 = courseFactory.getCourse("BIOL", 201);
        aSection = new Section("201", "TR", "14:00", "15:20", new Building("BioSci", new LatLon(49.263920, -123.251552)));
        biol201.addSection(aSection);
        aSection.setCourse(biol201);
        me.getSchedule().add(aSection);

        // CPSC 210 Students; Implement this method

    }

    /**
     * Helper to create simple alert dialog to display message
     *
     * @param msg message to display in alert dialog
     * @return the alert dialog
     */
    private AlertDialog createSimpleDialog(String msg) {
        // CPSC 210 Students; You should not need to modify this method
        AlertDialog.Builder dialogBldr = new AlertDialog.Builder(getActivity());
        dialogBldr.setMessage(msg);
        dialogBldr.setNeutralButton(R.string.ok, null);

        return dialogBldr.create();
    }

    /**
     * Create the overlay used for buildings. CPSC 210 students, you should not need to
     * touch this method.
     * @return An overlay
     */
    private ItemizedIconOverlay<OverlayItem> createBuildingOverlay() {
        ResourceProxy rp = new DefaultResourceProxyImpl(getActivity());

        ItemizedIconOverlay.OnItemGestureListener<OverlayItem> gestureListener =
                new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {

            /**
             * Display building description in dialog box when user taps stop.
             *
             * @param index
             *            index of item tapped
             * @param oi
             *            the OverlayItem that was tapped
             * @return true to indicate that tap event has been handled
             */
            @Override
            public boolean onItemSingleTapUp(int index, OverlayItem oi) {

                new AlertDialog.Builder(getActivity())
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {
                                if (selectedBuildingOnMap != null) {
                                    mapView.invalidate();
                                }
                            }
                        }).setTitle(oi.getTitle()).setMessage(oi.getSnippet())
                        .show();

                selectedBuildingOnMap = oi;
                mapView.invalidate();
                return true;
            }

            @Override
            public boolean onItemLongPress(int index, OverlayItem oi) {
                // do nothing
                return false;
            }
        };

        return new ItemizedIconOverlay<OverlayItem>(
                new ArrayList<OverlayItem>(), getResources().getDrawable(
                R.drawable.ic_action_place), gestureListener, rp);
    }


    /**
     * Create overlay with a specific color
     * @param colour A string with a hex colour value
     */
    private PathOverlay createPathOverlay(String colour) {
        // CPSC 210 Students, you should not need to touch this method
        PathOverlay po = new PathOverlay(Color.parseColor(colour),
                getActivity());
        Paint pathPaint = new Paint();
        pathPaint.setColor(Color.parseColor(colour));
        pathPaint.setStrokeWidth(4.0f);
        pathPaint.setStyle(Paint.Style.STROKE);
        po.setPaint(pathPaint);
        return po;
    }

   // *********************** Asynchronous tasks

    /**
     * Initialize the CourseFactory with some courses.
     */
    private void initializeCourses() {
        // CPSC 210 Students: You can change this data if you desire.
        CourseFactory courseFactory = CourseFactory.getInstance();

        Building dmpBuilding = new Building("DMP", new LatLon(49.261474, -123.248060));

        Course cpsc210 = courseFactory.getCourse("CPSC", 210);
        Section aSection = new Section("202", "MWF", "12:00", "12:50", dmpBuilding);
        cpsc210.addSection(aSection);
        aSection.setCourse(cpsc210);
        aSection = new Section("201", "MWF", "16:00", "16:50", dmpBuilding);
        cpsc210.addSection(aSection);
        aSection.setCourse(cpsc210);
        aSection = new Section("BCS", "MWF", "12:00", "12:50", dmpBuilding);
        cpsc210.addSection(aSection);
        aSection.setCourse(cpsc210);

        Course engl222 = courseFactory.getCourse("ENGL", 222);
        aSection = new Section("007", "MWF", "14:00", "14:50", new Building("Buchanan", new LatLon(49.269258, -123.254784)));
        engl222.addSection(aSection);
        aSection.setCourse(engl222);

        Course scie220 = courseFactory.getCourse("SCIE", 220);
        aSection = new Section("200", "MWF", "15:00", "15:50", new Building("Swing", new LatLon(49.262786, -123.255044)));
        scie220.addSection(aSection);
        aSection.setCourse(scie220);

        Course math200 = courseFactory.getCourse("MATH", 200);
        aSection = new Section("201", "MWF", "09:00", "09:50", new Building("Buchanan", new LatLon(49.269258, -123.254784)));
        math200.addSection(aSection);
        aSection.setCourse(math200);

        Course fren102 = courseFactory.getCourse("FREN", 102);
        aSection = new Section("202", "MWF", "11:00", "11:50", new Building("Barber", new LatLon(49.267442,-123.252471)));
        fren102.addSection(aSection);
        aSection.setCourse(fren102);

        Course japn103 = courseFactory.getCourse("JAPN", 103);
        aSection = new Section("002", "MWF", "10:00", "11:50", new Building("Buchanan", new LatLon(49.269258, -123.254784)));
        japn103.addSection(aSection);
        aSection.setCourse(japn103);

        Course scie113 = courseFactory.getCourse("SCIE", 113);
        aSection = new Section("213", "MWF", "13:00", "13:50", new Building("Swing", new LatLon(49.262786, -123.255044)));
        scie113.addSection(aSection);
        aSection.setCourse(scie113);

        Course micb308 = courseFactory.getCourse("MICB", 308);
        aSection = new Section("201", "MWF", "12:00", "12:50", new Building("Woodward", new LatLon(49.264704,-123.247536)));
        micb308.addSection(aSection);
        aSection.setCourse(micb308);

        Course math221 = courseFactory.getCourse("MATH", 221);
        aSection = new Section("202", "TR", "11:00", "12:20", new Building("Klinck", new LatLon(49.266112, -123.254776)));
        math221.addSection(aSection);
        aSection.setCourse(math221);

        Course phys203 = courseFactory.getCourse("PHYS", 203);
        aSection = new Section("201", "TR", "09:30", "10:50", new Building("Hennings", new LatLon(49.266400,-123.252047)));
        phys203.addSection(aSection);
        aSection.setCourse(phys203);

        Course crwr209 = courseFactory.getCourse("CRWR", 209);
        aSection = new Section("002", "TR", "12:30", "13:50", new Building("Geography", new LatLon(49.266039,-123.256129)));
        crwr209.addSection(aSection);
        aSection.setCourse(crwr209);

        Course fnh330 = courseFactory.getCourse("FNH", 330);
        aSection = new Section("002", "TR", "15:00", "16:20", new Building("MacMillian", new LatLon(49.261167,-123.251157)));
        fnh330.addSection(aSection);
        aSection.setCourse(fnh330);

        Course cpsc499 = courseFactory.getCourse("CPSC", 430);
        aSection = new Section("201", "TR", "16:20", "17:50", new Building("Liu", new LatLon(49.267632,-123.259334)));
        cpsc499.addSection(aSection);
        aSection.setCourse(cpsc499);

        Course chem250 = courseFactory.getCourse("CHEM", 250);
        aSection = new Section("203", "TR", "10:00", "11:20", new Building("Klinck", new LatLon(49.266112, -123.254776)));
        chem250.addSection(aSection);
        aSection.setCourse(chem250);

        Course eosc222 = courseFactory.getCourse("EOSC", 222);
        aSection = new Section("200", "TR", "11:00", "12:20", new Building("ESB", new LatLon(49.262866, -123.25323)));
        eosc222.addSection(aSection);
        aSection.setCourse(eosc222);

        Course biol201 = courseFactory.getCourse("BIOL", 201);
        aSection = new Section("201", "TR", "14:00", "15:20", new Building("BioSci", new LatLon(49.263920, -123.251552)));
        biol201.addSection(aSection);
        aSection.setCourse(biol201);

        cf = courseFactory;

    }

    /**
     * This asynchronous task is responsible for contacting the Meetup web service
     * for the schedule of a random student. The task must plot the retrieved
     * student's route for the schedule on the map in a different colour than the "me" schedule
     * or must display a dialog box that a schedule was not retrieved.
     */
    private class GetRandomSchedule extends AsyncTask<Void, Void, SchedulePlot> {

        // Some overview explanation of asynchronous tasks is on the project web page.

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected SchedulePlot doInBackground(Void... params) {

            // CPSC 210 Students: You must complete this method. It needs to
            // contact the Meetup web service to get a random student's schedule.
            // If it is successful in retrieving a student and their schedule,
            // it needs to remember the student in the randomStudent field
            // and it needs to create and return a schedulePlot object with
            // all relevant information for being ready to retrieve the route
            // and plot the route for the schedule. If no random student is
            // retrieved, return null.
            //
            // Note, leave all determination of routing and plotting until
            // the onPostExecute method below.

            //String myUrl = "http://kramer.nss.cs.ubc.ca:8081/getStudent";



            String myUrl = "http://kramer.nss.cs.ubc.ca:8082/getStudent";
            //String myUrl = "http://kramer.nss.cs.ubc.ca:8081/getStudentById/222222";  //test URL



            String hexNumber = "";

            try {
                String input = routingCall(myUrl);
                JSONObject obj = (JSONObject) new JSONTokener(input).nextValue();
                String fn = obj.getString("FirstName");
                String stringId = obj.getString("Id");
                int id = Integer.parseInt(stringId);
                String ln = obj.getString("LastName");
                JSONArray sections = obj.getJSONArray("Sections");

                randomStudent = new Student(ln, fn, id);

                for (int i = 0; i < sections.length(); i = i+1){
                    String cName = sections.getJSONObject(i).getString("CourseName");
                    String stringCourseNumber = sections.getJSONObject(i).getString("CourseNumber");
                    int cNumber = Integer.parseInt(stringCourseNumber);
                    String sName = sections.getJSONObject(i).getString("SectionName");

                    Course course = cf.getCourse(cName, cNumber);
                    Section section = course.getSection(sName);

                    randomStudent.getSchedule().add(section);


                }


            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            activeDay = sharedPreferences.getString("dayOfWeek", "MWF");

            if(randomStudents.add(randomStudent)){

                if (randomStudents.size() == 1){

                    hexNumber = "#ff008d";
                }
                else if (randomStudents.size()==2){
                    hexNumber = "#346699";
                }
                else if (randomStudents.size() == 3){
                    hexNumber = "#b4fdc0";
                }
                else if (randomStudents.size() >= 4){
                    hexNumber = "#ffd700";
                }

                SchedulePlot randomSchedulePlot = new SchedulePlot(randomStudent.getSchedule().getSections(activeDay), randomStudent.getFirstName()+" "+randomStudent.getLastName(), hexNumber, 17301506);

                return randomSchedulePlot;
            }


            return null;


        }


        @Override
        protected void onPostExecute(SchedulePlot schedulePlot) {
            // CPSC 210 students: When this method is called, it will be passed
            // whatever schedulePlot object you created (if any) in doBackground
            // above. Use it to plot the route.

            if(schedulePlot == null){
                AlertDialog aDialog = createSimpleDialog("The same random schedule has been added already");
                aDialog.show();

            }


            else if (!schedulePlot.getSections().isEmpty()){
                new GetRoutingForSchedule().execute(schedulePlot);
            }
            else{
                AlertDialog aDialog = createSimpleDialog("your random schedule has no section to plot");
                aDialog.show();

            }





        }
        private String routingCall(String httpRequest) throws MalformedURLException, IOException {
            URL url = new URL(httpRequest);
            HttpURLConnection client = (HttpURLConnection) url.openConnection();
            InputStream in = client.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String returnString = br.readLine();
            client.disconnect();
            return returnString;
        }
    }

    /**
     * This asynchronous task is responsible for contacting the MapQuest web service
     * to retrieve a route between the buildings on the schedule and for plotting any
     * determined route on the map.
     */
    private class GetRoutingForSchedule extends AsyncTask<SchedulePlot, Void, SchedulePlot> {



        @Override
        protected void onPreExecute() {
        }

        @Override
        protected SchedulePlot doInBackground(SchedulePlot... params) {

            // The params[0] element contains the schedulePlot object
            SchedulePlot scheduleToPlot = params[0];

            // CPSC 210 Students: Complete this method. This method should
            // call the MapQuest webservice to retrieve a List<GeoPoint>
            // that forms the routing between the buildings on the
            // schedule. The List<GeoPoint> should be put into
            // scheduleToPlot object.

            SortedSet<Section> sections = scheduleToPlot.getSections();
            List<GeoPoint> geoPoints = new ArrayList<GeoPoint>();

            String myURL;
            String input;
            double accLon = 0;
            double accLat = 0;

            if (scheduleToPlot.getSections().size() != 0) {


                for (Section s : sections) {

                    if (accLat != 0 && accLon != 0) {

                        myURL = "http://open.mapquestapi.com/directions/v2/route?key=Fmjtd%7Cluu82l0y2d%2C8w%3Do5-94z0gy&outFormat=json&routeType=pedestrian&timeType=0&enhancedNarrative=false&shapeFormat=raw&generalize=0&locale=en_US&unit=k&from="
                                + accLat + "," + accLon + "&to=" + s.getBuilding().getLatLon().getLatitude() + ","
                                + s.getBuilding().getLatLon().getLongitude() + "&drivingStyle=2&highwayEfficiency=21.0";


                        try {

                            input = makeRoutingCall(myURL);

                            JSONObject obj1 = (JSONObject) new JSONTokener(input).nextValue();
                            JSONObject routeObj = (JSONObject) obj1.get("route");
                            JSONObject shapeObj = (JSONObject) routeObj.get("shape");
                            JSONArray pointsArray = (JSONArray) shapeObj.get("shapePoints");


                            for (int i = 0; i < pointsArray.length(); i = i + 2) {

                                double geoLat = pointsArray.getDouble(i);
                                double geoLon = pointsArray.getDouble(i + 1);
                                GeoPoint newGp = new GeoPoint(geoLat, geoLon);
                                geoPoints.add(newGp);
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        } catch (MalformedURLException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }

                    accLat = s.getBuilding().getLatLon().getLatitude();
                    accLon = s.getBuilding().getLatLon().getLongitude();


                }

                scheduleToPlot.setRoute(geoPoints);

                return scheduleToPlot;
            } else {
                return null;
            }
        }


        private void fourSquareHelper(LatLon l){

            String fourSquare;
            Double pLat = 0.0;
            Double pLng = 0.0;
            Place place;


            fourSquare = "https://api.foursquare.com/v2/venues/explore?client_id=ZY1H1YJ1NUVEVQI3ABU4IYPJXTUZZ5Z5JY0TNCB5FRLAFZYY&client_secret=VP0EUJIVOKNRAL1CGA4KCEC3VY4DLVHY0BH3YZWHLYRXGGFM&ll="
                    + pLat
                    + "," +pLng+ "&section=food&v=20150303&m=foursquare&radius=300";


            try {
                JSONObject obj2 = (JSONObject) new JSONTokener((makeRoutingCall(fourSquare))).nextValue();
                JSONObject response = obj2.getJSONObject("response");
                JSONArray groups = response.getJSONArray("groups");
                JSONObject groupObj = groups.getJSONObject(0);
                JSONArray items = groupObj.getJSONArray("items");


                for (int i = 0; i < items.length(); i = i + 1){
                    String pName = items.getJSONObject(i).getJSONObject("venue").getString("name");
                    pLat = items.getJSONObject(i).getJSONObject("venue").getJSONObject("location").getDouble("lat");
                    pLng = items.getJSONObject(i).getJSONObject("venue").getJSONObject("location").getDouble("lng");

                    place = new Place(pName, new LatLon(pLat, pLng));

                }

            } catch (JSONException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }





        }


        /**
         * An example helper method to call a web service
         */
        private String makeRoutingCall(String httpRequest) throws MalformedURLException, IOException {
            URL url = new URL(httpRequest);
            HttpURLConnection client = (HttpURLConnection) url.openConnection();
            InputStream in = client.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String returnString = br.readLine();
            client.disconnect();
            return returnString;
        }



        @Override
        protected void onPostExecute(SchedulePlot schedulePlot) {

            // CPSC 210 Students: This method should plot the route onto the map
            // with the given line colour specified in schedulePlot. If there is
            // no route to plot, a dialog box should be displayed.

            // To actually make something show on the map, you can use overlays.
            // For instance, the following code should show a line on a map
            // PathOverlay po = createPathOverlay("#FFFFFF");
            // po.addPoint(point1); // one end of line
            // po.addPoint(point2); // second end of line
            // scheduleOverlay.add(po);
            // OverlayManager om = mapView.getOverlayManager();
            // om.addAll(scheduleOverlay);
            // mapView.invalidate(); // cause map to redraw

/*
            PathOverlay popo = createPathOverlay("#FF0000");
            popo.addPoint(new GeoPoint(49.269258, -123.254784)); // one end of line
            popo.addPoint(new GeoPoint(49.263920, -123.251552)); // second end of line
            scheduleOverlay.add(popo);
            OverlayManager om1 = mapView.getOverlayManager();
            om1.addAll(scheduleOverlay);
            mapView.invalidate(); // cause map to redraw
*/

            if (schedulePlot.equals(null)) {
                AlertDialog aDialog = createSimpleDialog("you have no sections to plot");
                aDialog.show();
            }

            else{
                AlertDialog aDialog = createSimpleDialog("New point has been plotted on the map");
                aDialog.show();

                GeoPoint accG = null;
                for(GeoPoint gp: schedulePlot.getRoute()){
                    if (accG != null){

                        PathOverlay po = createPathOverlay(schedulePlot.getColourOfLine());
                        po.addPoint(accG); // one end of line
                        po.addPoint(gp); // second end of line
                        scheduleOverlay.add(po);
                        OverlayManager om = mapView.getOverlayManager();
                        om.addAll(scheduleOverlay);
                        mapView.invalidate(); // cause map to redraw

                    }
                    accG = gp;

                }


            }

            plotBuildings(schedulePlot);

        }

    }

    /**
     * This asynchronous task is responsible for contacting the FourSquare web service
     * to retrieve all places around UBC that have to do with food. It should load
     * any determined places into PlaceFactory and then display a dialog box of how it did
     */
    private class GetPlaces extends AsyncTask<Void, Void, String> {

        protected String doInBackground(Void... params) {



            // CPSC 210 Students: Complete this method to retrieve a string
            // of JSON from FourSquare. Return the string from this method

            String fourSquare = "https://api.foursquare.com/v2/venues/explore?client_id=ZY1H1YJ1NUVEVQI3ABU4IYPJXTUZZ5Z5JY0TNCB5FRLAFZYY&client_secret=VP0EUJIVOKNRAL1CGA4KCEC3VY4DLVHY0BH3YZWHLYRXGGFM&ll=49.261474,-123.248060&section=food&v=20150303&m=foursquare";


            try {
                URL url = new URL(fourSquare);
                HttpURLConnection client = (HttpURLConnection) url.openConnection();
                InputStream in = client.getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(in));
                String returnString = br.readLine();
                client.disconnect();
                return returnString;
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        protected void onPostExecute(String jSONOfPlaces) {

            // CPSC 210 Students: Given JSON from FourQuest, parse it and load
            // PlaceFactory

            double pLat = 0.0;
            double pLng = 0.0;
            Place place;
            PlaceFactory pf = PlaceFactory.getInstance();
            int counter = 0;

            pf.clearMap();




            try {
                JSONObject obj2 = (JSONObject) new JSONTokener(jSONOfPlaces).nextValue();
                JSONObject response = obj2.getJSONObject("response");
                JSONArray groups = response.getJSONArray("groups");
                JSONObject groupObj = groups.getJSONObject(0);
                JSONArray items = groupObj.getJSONArray("items");


                for (int j = 0; j < items.length(); j = j + 1){
                    String pName = items.getJSONObject(j).getJSONObject("venue").getString("name");
                    pLat = items.getJSONObject(j).getJSONObject("venue").getJSONObject("location").getDouble("lat");
                    pLng = items.getJSONObject(j).getJSONObject("venue").getJSONObject("location").getDouble("lng");
                    int tier = items.getJSONObject(j).getJSONObject("venue").optJSONObject("price").optInt("tier");
                    String message = items.getJSONObject(j).getJSONObject("venue").optJSONObject("price").optString("message");
                    String currency = items.getJSONObject(j).getJSONObject("venue").optJSONObject("price").optString("currency");
                    String tip = items.getJSONObject(j).getJSONArray("tips").getJSONObject(0).optString("text");




                    String newMessage =

                            "Tier: "+tier+"\n"+
                                    "Message: "+message+"\n" +
                                    "Currency: "+currency+"\n"+
                                    "Review: "+tip+"\n";



                    String foodType = items.getJSONObject(j).getJSONObject("venue").getJSONArray("categories").getJSONObject(0).getString("shortName");

                    String settingType = sharedPreferences.getString("foodType", "all");

                    if(settingType.equals("Japanese")){
                        if(foodType.equals("Japanese")){

                            place = new Place(pName, new LatLon(pLat, pLng), newMessage);

                            pf.add(place);


                            counter = counter + 1;


                        }
                    }

                    if(settingType.equals("Seafood")) {
                        if (foodType.equals("Seafood")) {

                            place = new Place(pName, new LatLon(pLat, pLng), newMessage);

                            pf.add(place);


                            counter = counter + 1;

                        }
                    }
                    if(settingType.equals("Others")){
                        if(!foodType.equals("Seafood") && !foodType.equals("Japanese")){

                            place = new Place(pName, new LatLon(pLat, pLng), newMessage);

                            pf.add(place);


                            counter = counter + 1;

                        }
                    }

                    if(settingType.equals("All")) {


                        place = new Place(pName, new LatLon(pLat, pLng), newMessage);

                        pf.add(place);


                        counter = counter + 1;
                    }

                }

            }
            catch (JSONException e) {
                e.printStackTrace();
            }

            AlertDialog aDialog = createSimpleDialog("You have added "+counter+" places");
            aDialog.show();

        }
    }

}
