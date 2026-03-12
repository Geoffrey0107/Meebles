package MAD.Meebles;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NfcVillageView extends AppCompatActivity {

    LineChart chart;
    private Place place;
    private userObj user;
    private TextView meebleCount;
    final String TAG = "VILLAGE";
    private int USERID;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfc_village_view);

        //initialize chart
        chart = findViewById(R.id.populationChart);

        //maybe can remove in the future
        int placeId = getIntent().getIntExtra("place_id", 1);
        place = PlaceRepo.getPlace().getByPlaceId(placeId);


        userRepo repo = userRepo.getInstance();
        user = repo.getHashMap().get(0);
        if (user == null) {
            user = new userObj(0);
            repo.addToRepo(user);
        }

        USERID = getIntent().getIntExtra("userId", -1); // gets user ID


        TextView villageName = findViewById(R.id.villageName);
        villageName.setText(place.getName());

        TextView growthRate = findViewById(R.id.growthRate);
        growthRate.setText("Growth Rate: " + place.getGrowthRate());

        TextView villageCapacity = findViewById(R.id.villageCapacity);
        villageCapacity.setText("Max Capacity: " + place.getCapacity());

        meebleCount = findViewById(R.id.meebleCount);
        meebleCount.setText("Population: " + place.getPopulation());

        updateChart(place.getPopulationHistory());
        initButtons();
    };

    public void updatePopulation(int userId, int newPopulation) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> updates = new HashMap<>();
        updates.put("score", newPopulation);

        db.collection("users")
                .document(String.valueOf(userId))
                .set(updates, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Population updated"))
                .addOnFailureListener(e -> Log.d(TAG, "Failed to update population"));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);

        MenuItem logout = menu.getItem(R.id.action_log_out);
        logout.setVisible(false);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_close) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return true;
        }

        if (item.getItemId() == R.id.pickupDropPage) {
            Intent intent = new Intent(this, ScanActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra("userId", USERID); // transfers userId as well
            // if I do not transfer it here, USERID will become -1 and this will give us nullptr exceptions
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateChart(List<Integer> history) {
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < history.size(); i++) {
            entries.add(new Entry(i, history.get(i)));
        }
        LineDataSet dataSet = new LineDataSet(entries, "Population");
        dataSet.setColor(Color.BLUE);
        dataSet.setDrawCircles(false);
        dataSet.setLineWidth(2f);

        LineChart chart = findViewById(R.id.populationChart);
        chart.setData(new LineData(dataSet));
        chart.invalidate();
    }

    private void initButtons() {
        EditText meebleInput = findViewById(R.id.meebleInput);
        Button kidnapButton = findViewById(R.id.kidnapButton);
        Button releaseButton = findViewById(R.id.releaseButton);

        kidnapButton.setOnClickListener(v -> {
            String input = meebleInput.getText().toString();
            if (input.isEmpty()) {
                Toast.makeText(this, "Enter a number first", Toast.LENGTH_SHORT).show();
                return;
            }
            int amount = Integer.parseInt(input);
            int actualKidnapped = place.kidnap(amount);
            user.setScore(user.getScore() + actualKidnapped);

            updatePopulation(USERID, user.getScore());

            meebleCount.setText("Meebles: " + place.getPopulation());
            updateChart(place.getPopulationHistory());
            Toast.makeText(this, "Kidnapped " + actualKidnapped + " meebles!", Toast.LENGTH_SHORT).show();
            meebleInput.setText("");
        });

        releaseButton.setOnClickListener(v -> {
            String input = meebleInput.getText().toString();
            if (input.isEmpty()) {
                Toast.makeText(this, "Enter a number first", Toast.LENGTH_SHORT).show();
                return;
            }
            int amount = Integer.parseInt(input);
            if (amount > user.getScore()) {
                Toast.makeText(this, "You only have " + user.getScore() + " meebles!", Toast.LENGTH_SHORT).show();
                return;
            }
            place.release(amount);
            user.setScore(user.getScore() - amount);

            updatePopulation(USERID, user.getScore());

            meebleCount.setText("Meebles: " + place.getPopulation());
            updateChart(place.getPopulationHistory());
            Toast.makeText(this, "Released " + amount + " meebles!", Toast.LENGTH_SHORT).show();
            meebleInput.setText("");
        });
    }

    //handles the chart, chart updates itself every 10 seconds.
    private final Handler growthHandler = new Handler(Looper.getMainLooper());
    private final Runnable growthRunnable = new Runnable() {
        @Override
        public void run() {
            if (place != null) {
                place.grow();
                meebleCount.setText("Population: " + place.getPopulation());
                updateChart(place.getPopulationHistory());
            }
            growthHandler.postDelayed(this, 10000);
        }
    };

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
}