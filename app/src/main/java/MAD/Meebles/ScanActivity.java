package MAD.Meebles;

import android.content.Intent;
import android.graphics.Color;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.Toast;

public class ScanActivity extends AppCompatActivity {

    private static final String TAG = "ScanActivity";
    private NfcAdapter adapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_scan);

        adapter = NfcAdapter.getDefaultAdapter(this);

        ImageView meebleImage = findViewById(R.id.meebleImage);
        Animation pulse = AnimationUtils.loadAnimation(this, R.anim.pulse);
        meebleImage.startAnimation(pulse);
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

    @Override
    protected void onResume() {
        super.onResume();
        Bundle options = new Bundle();
        MYNFCCallbackClass myCallback = new MYNFCCallbackClass();
        adapter.enableReaderMode(this,
                myCallback,
                NfcAdapter.FLAG_READER_NFC_A,
                options);
    }

    @Override
    protected void onPause() {
        super.onPause();
        adapter.disableReaderMode(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        adapter.disableReaderMode(this);
    }

    private void routeToPlace(int value) {
        runOnUiThread(() -> {
            Intent intent;
            switch (value) {
                case 4:
                    intent = new Intent(this, NfcCityView.class);
                    break;
                case 3:
                    intent = new Intent(this, NfcTownView.class);
                    break;
                case 2:
                    intent = new Intent(this, NfcVillageView.class);
                    break;
                case 1:
                    intent = new Intent(this, NfcHouseView.class);
                    break;
                default:
                    Toast.makeText(this, "Unknown location!", Toast.LENGTH_SHORT).show();
                    return;
            }

            findViewById(android.R.id.content).postDelayed(() -> startActivity(intent), 500); // this gives the program enough time to disable reader
            // so then the new nfc tag pop up does not show up.
        });
    }

    private int readNumberFromTag(Tag tag) {
        Ndef ndef = Ndef.get(tag);
        try {
            ndef.connect();
            NdefMessage msg = ndef.getNdefMessage();
            String data = new String(msg.getRecords()[0].getPayload());
            Log.d(TAG, "Read from tag: " + data);
            data = data.substring(3);
            return Integer.valueOf(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Integer.MIN_VALUE;
    }

    private class MYNFCCallbackClass implements NfcAdapter.ReaderCallback {
        @Override
        public void onTagDiscovered(Tag tag) {
            int value = readNumberFromTag(tag);

            String placeName = "";
            switch (value) {
                case 4: placeName = "New Meeble City"; break;
                case 3: placeName = "South Meeburg"; break;
                case 2: placeName = "Meeblage"; break;
                case 1: placeName = "The Meebouse"; break;
            }

            Log.d(TAG, placeName + " Discovered!");
            routeToPlace(value);
        }
    }
}


