package MAD.Meebles;

import static MAD.Meebles.PlaceRepo.place;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

public class NfcCityView extends AppCompatActivity {

    LineChart chart = findViewById(R.id.populationChart);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_nfc_city_view);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.cityView), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        int placeId = getIntent().getIntExtra("place_id",4);
        Place place = PlaceRepo.getPlace().getByPlaceId(placeId);

        TextView cityName = (TextView) findViewById(R.id.cityName);
        cityName.setText(place.getName());

        TextView growthRate = (TextView) findViewById(R.id.growthRate);
        cityName.setText(place.getName());

        TextView cityCapacity = (TextView) findViewById(R.id.cityCapacity);
        cityName.setText(place.getName());

        TextView meebleCount = (TextView) findViewById(R.id.meebleCount);
        cityName.setText(place.getName());

        updateChart(place.getPopulationHistory());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
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
}