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
import android.widget.EditText;
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
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;



public class MainActivity extends AppCompatActivity {
    protected ArrayList<Integer> placeIds = new ArrayList<>(Arrays.asList(1,2,3,4));
    private TextView countdownText;
    final String TAG = "MAIN";
    final int[][] targetTimes = { {17, 0, 0} };// 17:00:00

    private ArrayList<Integer> userPopulationHistory = new ArrayList<>();
    double[] possibleRates = {0.04, 0.06, 0.08, 0.10};

    private double userGrowthRate;

    private android.os.Handler growthHandler = new android.os.Handler();

    private TextView populationView;
    LineChart chart;

    private int USERID = -1;

    private boolean growthRateSolved = false;

    private final Runnable growthRunnable = new Runnable() {
        @Override
        public void run() {
            if (USERID != -1) {
                growUserPopulation();
            }
            growthHandler.postDelayed(this, 10000); // every 10 seconds
        }
    };

    private void growUserPopulation() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users")
                .document(String.valueOf(USERID))
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    userObj user = doc.toObject(userObj.class);
                    if (user == null) return;

                    int currentScore = user.getScore();

                    // no growth if user has no meebles
                    if (currentScore <= 0) return;

                    double deltaT = 1.0; // one growth step
                    int newScore = (int) Math.round(currentScore * Math.exp(userGrowthRate * deltaT));

                    user.setScore(newScore);

                    if (userPopulationHistory.isEmpty()) {
                        userPopulationHistory.add(currentScore);
                    }
                    userPopulationHistory.add(newScore);

                    db.collection("users")
                            .document(String.valueOf(USERID))
                            .update("score", newScore)
                            .addOnSuccessListener(aVoid -> {
                                populationView.setText("Meebles: \n" + newScore);
                                updateChart(userPopulationHistory);
                            });
                });
    }

    private void initializeChart() {
        ArrayList<Entry> entries = new ArrayList<>();
        entries.add(new Entry(0, 0));

        LineDataSet dataSet = new LineDataSet(entries, "Your Meebles");
        dataSet.setDrawCircles(true);
        dataSet.setLineWidth(2f);

        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);
        chart.invalidate();
    }

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
        chart = findViewById(R.id.userPopulationChart);
        initializeChart();
        createOrLoadUser();
        startNextCountdown();
        populationView = findViewById(R.id.but_2);

        userGrowthRate = possibleRates[new Random().nextInt(possibleRates.length)];

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

                                    "2. Every place has a population and its own growth rate.\n" +
                                    "Population grows exponentially over time:\n" +
                                    "P(t) = P₀ · e^(rt)\n\n" +

                                    "3. Kidnap meebles from places to add them to your own population.\n\n" +

                                    "4. Your meebles also grow at a fixed growth rate.\n" +
                                    "Cities and towns have different growth rates to explore.\n\n" +

                                    "5. Be careful not to exceed the maximum capacity of a place!\n\n" +

                                    "6. Try to grow as many meebles as possible before time runs out\n\n" +

                                    "7. When you are done, tap the exit button on the top left to delete user."
                    )
                    .setPositiveButton("Got it", null)
                    .show();
        });
        Button guessButton = findViewById(R.id.guessButton);
        EditText guessInput = findViewById(R.id.guessInput);
        TextView guessResult = findViewById(R.id.guessResult);

        guessButton.setOnClickListener(v -> {
            if (growthRateSolved) {
                guessResult.setText("You already solved the growth rate!");
                return;
            }

            String text = guessInput.getText().toString().trim();

            if (text.isEmpty()) {
                guessResult.setText("Enter a growth rate first.");
                return;
            }

            try {
                double guess = Double.parseDouble(text);
                double diff = Math.abs(guess - userGrowthRate);

                if (diff < 0.005) {
                    guessResult.setText("Wow now you know exponential growth, please accept our gift");

                    growthRateSolved = true;
                    guessButton.setEnabled(false);
                    guessInput.setEnabled(false);

                    FirebaseFirestore db = FirebaseFirestore.getInstance();

                    db.collection("users")
                            .document(String.valueOf(USERID))
                            .get()
                            .addOnSuccessListener(doc -> {

                                if (!doc.exists()) return;

                                int currentScore = ((Long) doc.getLong("score")).intValue();

                                int newScore = currentScore + 500;

                                updatePopulation(USERID, newScore);

                                populationView.setText("Meebles:\n" + newScore);

                            });
                } else if (diff < 0.02) {
                    guessResult.setText("Almost, try again!");
                } else if (guess < userGrowthRate) {
                    guessResult.setText("Too low, You can do better than that!");
                } else {
                    guessResult.setText("Too high, look more closely");
                }
            } catch (NumberFormatException e) {
                guessResult.setText("Enter a valid decimal number like 0.06");
            }
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

                    // updates the score of this particular user
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

                    placementText.setText("Rank: \n" +placementS);
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
                        IdDisplay.setText(String.valueOf("User ID: \n" + user.getId()));

                        TextView popDisplay = findViewById(R.id.but_2);

                        popDisplay.setText(String.valueOf("Meebles: \n" + user.getScore()));

                        startRealTimePlacementListener(USERID);
                    } else { // if the user does not exist
                        userObj user = new userObj(userId);
                        USERID = user.getId();

                        Log.d(TAG, "Loaded existing user: " + USERID);

                        TextView IdDisplay = (TextView)findViewById(R.id.but_1);
                        IdDisplay.setText("User ID: \n" + user.getId());

                        TextView placementText = findViewById(R.id.but_3);
                        TextView popDisplay = findViewById(R.id.but_2);

                        // display score and population
                        placementText.setText(String.valueOf(-1));
                        popDisplay.setText("Meebles: \n" + 0);

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

        MenuItem action_close = menu.findItem(R.id.action_close);
        action_close.setVisible(false);

        return true;

    } public boolean onOptionsItemSelected(MenuItem item) {
        final int id = item.getItemId();

        if (id == R.id.pickupDropPage) {
            Intent i = new Intent(getApplicationContext(), ScanActivity.class);
            i.putExtra("userId", USERID);
            startActivity(i);
        } else if (id == R.id.action_log_out) { // when the user exits delete
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users")
                    .document(String.valueOf(USERID))
                    .delete()
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "User deleted successfully"))
                    .addOnFailureListener(e -> Log.d(TAG, "Failed to delete user: " + e.getMessage()));

            finishAffinity();
        } else {
            // system will handle if none were clicked
            return super.onOptionsItemSelected(item);
        }

        // menu item has been handled
        return true;
    }
    @Override
    protected void onResume() {
        super.onResume();
        growthHandler.postDelayed(growthRunnable, 10000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        growthHandler.removeCallbacks(growthRunnable);
    }
    private void updateChart(ArrayList<Integer> history) {
        ArrayList<com.github.mikephil.charting.data.Entry> entries = new ArrayList<>();

        for (int i = 0; i < history.size(); i++) {
            entries.add(new com.github.mikephil.charting.data.Entry(i, history.get(i)));
        }

        com.github.mikephil.charting.data.LineDataSet dataSet =
                new com.github.mikephil.charting.data.LineDataSet(entries, "Your Meebles");

        dataSet.setDrawCircles(false);
        dataSet.setLineWidth(2f);
        dataSet.setColor(android.graphics.Color.BLUE);

        chart.setData(new com.github.mikephil.charting.data.LineData(dataSet));
        chart.invalidate();
    }
}
