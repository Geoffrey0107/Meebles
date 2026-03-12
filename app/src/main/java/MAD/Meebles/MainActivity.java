package MAD.Meebles;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Random;

import com.github.mikephil.charting.charts.LineChart;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;



public class MainActivity extends AppCompatActivity {
    protected ArrayList<Integer> placeIds = new ArrayList<>(Arrays.asList(1,2,3,4));
    private TextView countdownText;
    final String TAG = "MAIN";
    final int[][] targetTimes = { {17, 0, 0} };// 17:00:00

    private TextView populationView;
    LineChart chart;

    private int USERID = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (
                v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets; });

        countdownText = (TextView)findViewById(R.id.timeDisplay);

        createOrLoadUser();
        startNextCountdown();
        populationView = findViewById(R.id.but_2);
        userRepo repo = userRepo.getInstance();
        userObj user = repo.getHashMap().get(0);
//        if (user != null) {
//            populationView.setText("Your Meebles: " + user.getScore());
//        }
        Button instructionsButton = findViewById(R.id.instructions);
        instructionsButton.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this)
                    .setTitle("How to Play")
                    .setMessage(
                            "1. Scan an NFC tag to visit a place.\n\n" +

                                    "2. Each place has a population and a growth rate.\n" +
                                    "The population grows exponentially over time.\n\n" +

                                    "Growth formula:\n" +
                                    "P(t) = P₀ · e^(r t)\n\n" +

                                    "P₀ = initial population\n" +
                                    "r = growth rate\n" +
                                    "t = time\n\n" +

                                    "3. Kidnap meebles from places to add them to your total.\n\n" +
                                    "4. Release meebles back into places if you want.\n\n" +
                                    "5. Try to collect as many meebles as possible before time runs out!"
                    )
                    .setPositiveButton("Got it", null)
                    .show();
        });
    }

    public void startRealTimePlacementListener(int userId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance(); // Listen to all users ordered by population (score) descending

        db.collection("users")
                .orderBy("score", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {

                    if (e != null) {
                        Log.w(TAG, "Listen failed.", e);
                        return;
                    }

                    if (snapshots == null || snapshots.isEmpty()) return;
                    int placement = 1; // start at first place
                    int userPopulation = 0;

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        int id =  (int)(long) doc.getLong("id"); // converts from Long to long and then to int.
                        // must convert Long to primitive value long before we can convert to int
                        int score = (int)(long) doc.getLong("score");
                        if (id == userId) { // found the user
                            userPopulation = score;
                            break;
                        }
                        placement++;
                    }
                    // Update the placement TextView
                    TextView placementText = findViewById(R.id.but_3);
                    //TextView popDisplay = findViewById(R.id.popDisplay);

                    // display score and population
                    String placementS = placement + "";
                    String userPopS = userPopulation + "";

                    placementText.setText(placementS);
                    //popDisplay.setText(userPopS);
                });
    }

    public void createOrLoadUser() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE); // Check if device already has a userId
        int storedId = prefs.getInt("userId", -1);
        final int userId; // must be final for lambdas

        if (storedId == -1) {
            // No userId stored then generate one
            userId = new Random().nextInt(1000); // bigger range to reduce collisions
            prefs.edit().putInt("userId", userId).apply();
        } else {
            userId = storedId;
        }
        // Create user object

        // Save to Firebase
        db.collection("users")
                .document(String.valueOf(userId))
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) { // if the user exists
                        userObj user = doc.toObject(userObj.class);
                        Log.d(TAG, "Loaded existing user: " + user.getScore());

                        USERID = user.getId();

                        TextView IdDisplay = (TextView)findViewById(R.id.but_1);
                        IdDisplay.setText(String.valueOf(user.getId()));

                        TextView popDisplay = findViewById(R.id.but_2);

                        popDisplay.setText(String.valueOf(user.getScore()));

                        startRealTimePlacementListener(USERID);
                    } else { // if the user does not exist
                        userObj user = new userObj(userId);
                        USERID = user.getId();

                        Log.d(TAG, "Loaded existing user: " + USERID);

                        TextView IdDisplay = (TextView)findViewById(R.id.but_1);
                        IdDisplay.setText(String.valueOf(user.getId()));

                        TextView placementText = findViewById(R.id.but_3);
                        TextView popDisplay = findViewById(R.id.but_2);

                        // display score and population
                        placementText.setText(String.valueOf(-1));
                        popDisplay.setText(String.valueOf(0));

                        db.collection("users").document(String.valueOf(userId)).set(user)
                                .addOnSuccessListener(aVoid -> Log.d(TAG, "Created New User"));

                        startRealTimePlacementListener(USERID);
                    }
                })
                .addOnFailureListener(e -> Log.d(TAG, "Failed to create/load user: " + e.getMessage()));
    }

    public void updatePopulation(int userId, int newPopulation) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users")
                .document(String.valueOf(userId))
                .update("score", newPopulation)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Population updated"))
                .addOnFailureListener(e -> Log.d(TAG, "Failed to update population"));
    }

    public void startNextCountdown() {
        Calendar now = Calendar.getInstance();
        Calendar nextTarget = null;
        for (int[] t : targetTimes) {
            Calendar temp = (Calendar) now.clone();
            temp.set(Calendar.HOUR_OF_DAY, t[0]);
            temp.set(Calendar.MINUTE, t[1]);
            temp.set(Calendar.SECOND, t[2]);
            temp.set(Calendar.MILLISECOND, 0);
            if (temp.after(now)) {
                nextTarget = temp; break;
            }
        }

        if (nextTarget == null) { // all times passed today, pick first target tomorrow
            nextTarget = (Calendar) now.clone(); nextTarget.add(Calendar.DAY_OF_MONTH, 1);
            nextTarget.set(Calendar.HOUR_OF_DAY, targetTimes[0][0]);
            nextTarget.set(Calendar.MINUTE, targetTimes[0][1]); nextTarget.set(Calendar.SECOND, targetTimes[0][2]);
            nextTarget.set(Calendar.MILLISECOND, 0);
        }
        long diffMillis = nextTarget.getTimeInMillis() - now.getTimeInMillis();

        new CountDownTimer(diffMillis, 1000) {
            @Override public void onTick(long millisUntilFinished) {
                long seconds = millisUntilFinished / 1000 % 60;
                long minutes = millisUntilFinished / (1000 * 60) % 60;
                long hours = millisUntilFinished / (1000 * 60 * 60);
                countdownText.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
            }

            @Override public void onFinish() {
                countdownText.setText("00:00:00");
            }
        }.start();

    } public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();

        inflater.inflate(R.menu.main_menu, menu);

        return true;

    } public boolean onOptionsItemSelected(MenuItem item) {
        final int id = item.getItemId();

        if (id == R.id.pickupDropPage) {
            Intent i = new Intent(getApplicationContext(), ScanActivity.class);
            i.putExtra("userId", USERID);
            startActivity(i);
        } else if (id == R.id.action_close) { // when the user exits delete
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users")
                    .document(String.valueOf(USERID))
                    .delete()
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "User deleted successfully"))
                    .addOnFailureListener(e -> Log.d(TAG, "Failed to delete user: " + e.getMessage()));

            System.exit(0); // stops the app entirely
        } else {
            // system will handle if none were clicked
            return super.onOptionsItemSelected(item);
        }

        // menu item has been handled
        return true;
    }
}
