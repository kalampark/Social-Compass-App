package edu.ucsd.cse110.cse_110_project_cse_110_team_9;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import edu.ucsd.cse110.cse_110_project_cse_110_team_9.database.SocialCompassDatabase;
import edu.ucsd.cse110.cse_110_project_cse_110_team_9.database.SocialCompassRepository;
import edu.ucsd.cse110.cse_110_project_cse_110_team_9.database.User;
import edu.ucsd.cse110.cse_110_project_cse_110_team_9.view.CompassView;
import edu.ucsd.cse110.cse_110_project_cse_110_team_9.view.FriendViewItem;
import edu.ucsd.cse110.cse_110_project_cse_110_team_9.services.LocationService;
import edu.ucsd.cse110.cse_110_project_cse_110_team_9.services.OrientationService;
import edu.ucsd.cse110.cse_110_project_cse_110_team_9.services.TimeService;
import edu.ucsd.cse110.cse_110_project_cse_110_team_9.view.RingView;
import kotlin.Triple;


public class MainActivity extends AppCompatActivity {

    private TimeService timeService;
    private OrientationService orientationService;
    private float orientation = 0f;

    private List<String> emojiStrings;
    private LiveData<Triple<Double, Double, Long>> locationData;

    private MutableLiveData<Constants.scale> zoomLevel;
    private SocialCompassRepository repo; //in previous labs this was final.
    private LocationService locationService;
    private String user_public_code = "";

    private User userCache = null; //cache an instance of the users so we don't have to get
    // the user's public_uid, private_code, and label every time we want to update the users
    // location on the server.
    private ActivityResultLauncher<Intent> activityResultLauncher; //We use this to launch
    // NameAcitity and DataEntryActivity which allows us to get a result from an activty. For
    // NameActivity the result is the user's name and for DataEntryActivity the result is the
    // public_uid of the friend you want to add. SEE LINE 78 for this is used.
    // See: https://developer.android.com/training/basics/intents/result


    //List of references to our friendViewItems on the map
    private List<FriendViewItem> friendItems;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        friendItems = new ArrayList<>();

        emojiStrings = new ArrayList<>();

        emojiStrings = Utilities.getEmojis();

        //SCALE SETUP
        zoomLevel = new MutableLiveData<>();


        RingView ringView = findViewById(R.id.ringView);

        ringView.setZoomObserver(zoomLevel, this);

        //SETUP RING VIEW

        Log.d("Main Activity", "main activity launched");

        activityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Constants.NAME_ACTIVITY_REQUEST_CODE) {
                        // There are no request codes
                        Intent data = result.getData();
                        String name = data.getStringExtra("name");

                        if (name != null) {
                            if (repo.userExists()) {

                                //THE user already exists in the database so just update the name
                                // of the user locally, and on the server
                                User user = repo.getUser();
                                userCache = user;
                                user.setLabel(name);
                                repo.upsertLocalUser(user);
                                repo.upsertUserRemote(user);

                            } else {
                                //The user does not exist in the database (first time app is
                                // opened from clean install) so we generate a public_code and
                                // private_code for the user, and save it locally, and to the
                                // server;
                                user_public_code = UUID.randomUUID().toString();
                                String private_code = UUID.randomUUID().toString();

                                User user = new User(name, private_code, user_public_code,
                                        0d, 0d, Instant.now().getEpochSecond());

                                //insert user into user database
                                repo.upsertLocalUser(user);
                                repo.upsertUserRemote(user);

                                //SET the public_uid of the user to the Textview
                                TextView public_uid_view = findViewById(R.id.public_uid_textView);
                                public_uid_view.setText(user_public_code);
                                //update user cache
                                userCache = user;
                            }
                        }
                    } else if (result.getResultCode() == Constants.ADD_FRIEND_ACTIVITY_REQUEST_CODE) {
                        Intent data = result.getData();

                        if (data != null) {
                            String public_code = data.getStringExtra("public_code");


                            // Log.d("got public uid from activity", public_code);
                            //check if friend exisits on remote

                            if (public_code != null) {
                                addFriend(public_code);
                                repo.upsertLocalFriend(public_code);
                            }
                        }
                    }
                });

        //SETUP the location service
        locationService = LocationService.singleton(this);
        locationData = locationService.getLocation();
        locationData.observe(this, this::onLocationChanged);


        //SETUP DATABASE
        var db = SocialCompassDatabase.provide(getApplicationContext());
        var dao = db.getDao();
        repo = new SocialCompassRepository(dao);


        //if the user has never opened the app before and entered their name request it.
        if (!repo.userExists()) {
            Intent intent = new Intent(this, NameActivity.class);
            activityResultLauncher.launch(intent);
        } else {
            TextView public_uid_textView = findViewById(R.id.public_uid_textView);
            public_uid_textView.setText(repo.getUser().get_public_code());
        }

        //SETUP TIME SERVICE (I don't think we are really using this rn).
        timeService = TimeService.singleton();
        var timeData = timeService.getTimeData();
        timeData.observe(this, this::onTimeChanged);

        CompassView compass = findViewById(R.id.compass);
        compass.setRangeDegrees(360);


        //SETUP ORIENTATION SERVICE
        orientationService = OrientationService.singleton(this);
        var azimuthData = orientationService.getAzimuthData();
        azimuthData.observe(this, this::OnOrientationChanged);


        //ADD ALL FRIENDS STORED IN THE LOCAL DATABASE TO THE MAP
        addFriendsFromDatabaseToMap();

        // set default zoom level
        //As a user I want the zoom level at app launch to be the inner two levels so that I can
        // initially be focused on the friends who are closest to me (Figures (a) and (b))
        zoomLevel.postValue(Constants.scale.TEN);


        userCache = repo.getUser();

        //SET LOCATION TO LAST KNOW LOCATION

        if (userCache != null) {
            locationService.forceUpdate(new Triple<>(userCache.getLatitude(),
                    userCache.getLongitude(), userCache.getUpdated_at()));
        } else {
            Log.e("Main OnCreate", "User cache is null!!!");
        }


        Switch devSwitch = (Switch) findViewById(R.id.devSwitch);
        devSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {

                EditText urlView = (EditText) findViewById(R.id.serverUrlEntry);
                Button urlEnterbtn = (Button) findViewById(R.id.saveServerUrlBtn);
                Button addallFriends = (Button) findViewById(R.id.addAllPublicFriends);


                urlView.setText(Constants.serverEndpoint);

                if (isChecked) {
                    urlView.setVisibility(View.VISIBLE);
                    urlEnterbtn.setVisibility(View.VISIBLE);
                    addallFriends.setVisibility(View.VISIBLE);
                } else {
                    urlView.setVisibility(View.INVISIBLE);
                    urlEnterbtn.setVisibility(View.INVISIBLE);
                    addallFriends.setVisibility(View.INVISIBLE);
                }

            }
        });


        //setup dev go button


    }


    private void addFriendsFromDatabaseToMap() {
        var friends = repo.getAllLocalFriends();
        friends.forEach(friend -> {

            if (repo.friendExistsRemote(friend.public_code)) {

                addFriend(friend.public_code);
            } else {

                Log.e("Server",
                        "Saved friend does not exists on REMOTE" + friend.public_code);
            }
        });
    }

    private void OnOrientationChanged(Float azimuth) {
        CompassView compass = findViewById(R.id.compass);
        //     SharedPreferences preferences = getSharedPreferences("my_prefs", MODE_PRIVATE);

        // float degree = preferences.getFloat("degree", 0);
//        float lat_n = preferences.getFloat("lat", 0);
//        float long_n = preferences.getFloat("long", 0);

        compass.setDegrees(azimuth, true);

        orientation = azimuth;

        ImageView compassImageView = findViewById(R.id.compassImg);
        float rotation = (-orientation);
        // System.out.println("This is the rotation "+ degree);
        ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) compassImageView.getLayoutParams();
        layoutParams.circleAngle = rotation;
        compassImageView.setLayoutParams(layoutParams);

        compassImageView.setRotation(rotation);
        // truncateLabels();


    }

    private void onLocationChanged(Triple<Double, Double, Long> locationUpdate) {


//        friendItems.forEach(friendViewItem -> {
//            friendViewItem.flip
//            TextSide();
//        });
        runOnUiThread(() -> {
            repositionLabels();
            truncateLabels();
        });

        TextView locationText = findViewById(R.id.locationText);
        locationText.setText(Utilities.formatLocation(locationUpdate.getFirst(),
                locationUpdate.getSecond()));
        //UPDATE THE LOCATION IN our cached user object.
        if (userCache == null) {

            userCache = repo.getUser();
            if (userCache == null) {
                Log.e("ERROR", "NO USER FOUND IN DATABASE");
                return;
            }
        }

        userCache.setUpdated_at(locationUpdate.getThird());
        userCache.setLatitude(locationUpdate.getFirst());
        userCache.setLongitude(locationUpdate.getSecond());
        repo.upsertUserRemote(userCache); //update the server
        repo.upsertLocalUser(userCache); //update the local db

    }

    /**
     * Add a friend to the map given a public uid
     *
     * @param public_code
     */
    public void addFriend(String public_code) {
        ConstraintLayout layout = (ConstraintLayout) findViewById(R.id.constraintLayout);
        ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_CONSTRAINT, ConstraintLayout.LayoutParams.WRAP_CONTENT);


        //Constrain friend to compassIMG
        params.circleConstraint = R.id.compassImg;

        FriendViewItem newFriend = new FriendViewItem(this);
        newFriend.setLayoutParams(params);

        layout.addView(newFriend);

        //get the friend from remote and setup polling. Pass the liveDAta object into
        // FriendViewobject
        newFriend.setData(repo.getFriendFromRemoteLive(public_code), this);

        friendItems.add(newFriend);
        //set the location and orientation services for the friend view item, so the friendView
        // item can adjust it's angle and radius whenever the users' orientation and location change
        newFriend.setLocationDataSource(locationData, this);
        newFriend.setOrientationService(orientationService, this);

        //SET the friends
        int hashcode = public_code.hashCode() & 0xfffffff; // &0xfff... makes hash code positive
        int index = hashcode % emojiStrings.size();
        newFriend.setFriendIcon(emojiStrings.get(index));
        newFriend.setScaleObserver(zoomLevel, this);

    }

    /**
     * This method run's every 3 seconds
     *
     * @param time
     */
    void onTimeChanged(Long time) {

        //THIS METHOD RUNS EVERY 3 SECONDS TO CHANGE IT CHANGE IT IN CONSTANTS CLASS

        var user = repo.getUser();

        if (user != null) {
            long timeNow = Instant.now().getEpochSecond();
            long diff = timeNow - user.getUpdated_at();
            Log.d("Seconds since last update", Long.toString(diff));
            ImageView gpsLive = findViewById(R.id.gpsLive);
            ImageView gpsnotLive = findViewById(R.id.gpsnotLive);
            TextView lastLive = (TextView) findViewById(R.id.lastLive);

            if (diff > 4) {
                gpsLive.setVisibility(View.INVISIBLE);
                gpsnotLive.setVisibility(View.VISIBLE);
                double timeInMinutes = Math.floor((double) diff / 30) / 2;
                Log.d("Minutes test", Double.toString(timeInMinutes));
                lastLive.setVisibility(View.VISIBLE);
                lastLive.setText(timeInMinutes + " minutes");
            } else {
                gpsLive.setVisibility(View.VISIBLE);
                gpsnotLive.setVisibility(View.INVISIBLE);
                lastLive.setText("");
                Log.d("failed", Long.toString(diff));
                lastLive.setVisibility(View.INVISIBLE);
            }

        }

    }

    @Override
    protected void onPause() {
        //TODO: Stop polling threads for friends
        super.onPause();
        orientationService.unregisterSensorListeners();
        locationService.unregisterLocationListener();

    }

    @Override
    protected void onResume() {
        //TODO: restart polling threads for friends
        super.onResume();
        orientationService.registerSensorListeners();
        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        } else {
            locationService.registerLocationListener();
        }

    }


    //TODO : optimzie
    private void repositionLabels() {

        friendItems.sort(new Comparator<FriendViewItem>() {
            @Override
            public int compare(FriendViewItem friendViewItem, FriendViewItem t1) {
                return Integer.compare(friendViewItem.getRadius(), t1.getRadius());
            }
        });

        for (int i = 0; i < friendItems.size() - 1; i++) {
            // Log.d("radi", Integer.toString(friendItems.get(i).getRadius()));


            if (isViewOverlapping(friendItems.get(i).getCurrentTextView(),
                    friendItems.get(i + 1).getCurrentTextView())) {
                friendItems.get(i).setTextSide(true);
                friendItems.get(i + 1).setTextSide(false);
            }

        }


    }

    private void truncateLabels() {


        //  friendItems.forEach(friendViewItem -> {friendViewItem.setTruncate(false);});


        for (int i = 0; i < friendItems.size(); i++) {
            for (int j = 0; j < friendItems.size(); j++) {

                if (i != j) {
                    var a = friendItems.get(i);
                    var b = friendItems.get(j);
                    // a.setTruncate(true);
                    //Log.d("truncae","truncate");
                    if (isViewOverlapping(a.getCurrentTextView(), b.getCurrentTextView())) {
                        if (a.getTruncate()) {
                            b.setTruncate(true);
                            //  Log.d("Truncate", b.setNameLabel();/
                        } else {
                            a.setTruncate(true);
                        }
                    }
                }
            }
        }

        friendItems.forEach(friendViewItem -> {
            friendViewItem.updateTextView();
        });
    }

    public void onLaunchDataEntry(View view) {

        Intent intent = new Intent(this, DataEntryActivity.class);
        activityResultLauncher.launch(intent);
    }

    public void onLaunchDegree(View view) {
        Intent intent = new Intent(this, DegreeActivity.class);
        startActivity(intent);
    }

    public void onLaunchName(View view) {
        Intent intent = new Intent(this, NameActivity.class);
        activityResultLauncher.launch(intent);
    }

    private boolean isViewOverlapping(View firstView, View secondView) {
        int[] firstPosition = new int[2];
        int[] secondPosition = new int[2];


        if (firstView.getVisibility() == View.INVISIBLE || secondView.getVisibility() == View.INVISIBLE) {
            return false;
        }
        firstView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        firstView.getLocationOnScreen(firstPosition);
        secondView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        secondView.getLocationOnScreen(secondPosition);

        return firstPosition[0] < secondPosition[0] + secondView.getMeasuredWidth()
                && firstPosition[0] + firstView.getMeasuredWidth() > secondPosition[0]
                && firstPosition[1] < secondPosition[1] + secondView.getMeasuredHeight()
                && firstPosition[1] + firstView.getMeasuredHeight() > secondPosition[1];
    }

    public void onZoomOut(View view) {

        friendItems.forEach(friendViewItem -> {
            friendViewItem.setTruncate(false);
        });
        Constants.scale currentZoom = zoomLevel.getValue();
        Log.d("current zoom level", currentZoom.toString());

        if (currentZoom != null) {

            switch (currentZoom) {
                case ONE:
                    zoomLevel.postValue(Constants.scale.TEN);

                    ImageView zoomIn = findViewById(R.id.zoom_in);
                    zoomIn.setClickable(true);
                    zoomIn.setAlpha(1.0f);
                    //ungrey zoom in
                    break;
                case TEN:
                    zoomLevel.postValue(Constants.scale.FIVE_HUNDRED);
                    break;
                case FIVE_HUNDRED:
                    zoomLevel.postValue(Constants.scale.FIVE_HUNDRED_PLUS);
                    ImageView zoomOutBtn = findViewById(R.id.zoom_out);
                    zoomOutBtn.setClickable(false);
                    zoomOutBtn.setAlpha(0.5f);
                    break;
                case FIVE_HUNDRED_PLUS:
                    //WE SHOULD NEVER GET HERE
                    Log.d("Zoom out ", "this case should not be possible");
                    break;
            }
        }
        repositionLabels();
    }

    public void onZoomIn(View view) {

        friendItems.forEach(friendViewItem -> {
            friendViewItem.setTruncate(false);
        });
        Constants.scale currentZoom = zoomLevel.getValue();

        if (currentZoom != null) {

            switch (currentZoom) {
                case ONE:
                    Log.d("Zoom in ", "this case should not be possible");
                    break;
                case TEN:
                    zoomLevel.postValue(Constants.scale.ONE);
                    ImageView zoomIn = findViewById(R.id.zoom_in);
                    zoomIn.setClickable(false);
                    zoomIn.setAlpha(0.5f);
                    break;
                case FIVE_HUNDRED:
                    zoomLevel.postValue(Constants.scale.TEN);
                    break;
                case FIVE_HUNDRED_PLUS:
                    zoomLevel.postValue(Constants.scale.FIVE_HUNDRED);
                    ImageView zoomOut = findViewById(R.id.zoom_out);
                    zoomOut.setClickable(true);
                    zoomOut.setAlpha(1.0f);
                    break;
            }
        }
        repositionLabels();
    }


    public void onChangeServerUrlClicked(View view) {
        EditText urlView = (EditText) findViewById(R.id.serverUrlEntry);
        String url = urlView.getText().toString();
        Utilities.showAlert(this, "DELETING ALL FRIENDS AND CHANGING SERVER URL to: " + url);

        repo.nukeFriends();

        ConstraintLayout layout = (ConstraintLayout) findViewById(R.id.constraintLayout);
        friendItems.forEach(friendViewItem -> {
            layout.removeView(friendViewItem);
        });
        repo.shutdownPool();
        Constants.serverEndpoint = url;
        repo.restartThreadPool();


        //CLEAR FRIENDS
        // DATABASE


    }

    public void onAddAllPublicLocations(View view) {


        AlertDialog.Builder builder = new AlertDialog.Builder(this);


        //Setting message manually and performing action on button click
        builder.setMessage("Extreme Lag May Occur")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        var friends = repo.getAllPubliclyListedFriends();

                        if (friends != null) {
                            for (int i = 0; i < friends.length; i++) {
                                addFriend(friends[i].public_code);
                                repo.upsertLocalFriend(friends[i].public_code);
                            }
                        }

                        Toast.makeText(getApplicationContext(), "Please wait",
                                Toast.LENGTH_SHORT).show();

                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //  Action for 'NO' Button
                        dialog.cancel();
                        Toast.makeText(getApplicationContext(), "Operation canceled",
                                Toast.LENGTH_SHORT).show();
                    }
                });
        //Creating dialog box
        AlertDialog alert = builder.create();
        //Setting the title manually
        alert.setTitle("Add All publicly listed locations?");
        alert.show();

    }
}