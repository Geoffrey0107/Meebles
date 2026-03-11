package MAD.Meebles;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import java.util.Calendar;
import java.util.Random;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;


public class MainActivity extends AppCompatActivity {
    private TextView countdownText;
    final String TAG = "MAIN";
    final int[][] targetTimes = { {17, 0, 0} };// 17:00:00

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

        countdownText = (TextView) findViewById(R.id.timeDisplay);

        createNewUser(0);

        startNextCountdown();
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
                        int id = Integer.parseInt(doc.getString("id"));
                        int score = Integer.parseInt(doc.getString("score"));
                        if (id == userId) { // found the user
                            userPopulation = score;
                            break;
                        }
                        placement++;
                    }
                    // Update the placement TextView
                    TextView placementText = findViewById(R.id.placeDisplay);
                    TextView popDisplay = findViewById(R.id.popDisplay);

                    // display score and population
                    String placementS = placement + "";
                    String userPopS = userPopulation + "";

                    placementText.setText(placementS);
                    popDisplay.setText(userPopS); });
    }

    public void createNewUser(int initialPopulation) {
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
        userObj user = new userObj(userId, initialPopulation, "Player" + userId);

        TextView placementText = findViewById(R.id.placeDisplay);
        TextView popDisplay = findViewById(R.id.popDisplay);

        // display score and population
        String placementS = -1 + "";
        String userPopS = 0 + "";

        placementText.setText(placementS);
        popDisplay.setText(userPopS);

        // Save to Firebase
        db.collection("users")
                .document(String.valueOf(userId))
                .set(user) .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User created or loaded: " + userId);
                    // Start real-time placement listener
                    startRealTimePlacementListener(userId);
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
        menu.findItem(R.id.action_close).setVisible(false);
        return true;

    } public boolean onOptionsItemSelected(MenuItem item) {
        final int id = item.getItemId();
        if (id == R.id.pickupDropPage) {
            Intent i = new Intent(getApplicationContext(), ScanActivity.class);
            startActivity(i);
        } else if (id == R.id.action_close) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return true;
        } else { // system will handle if none were clicked
            return super.onOptionsItemSelected(item);
        } // menu item has been handled
        return true;
    }
}
