package MAD.Meebles;

import android.content.Intent;
import android.content.SharedPreferences;
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
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class ScanActivity extends AppCompatActivity {

    private static final String TAG = "ScanActivity";
    private NfcAdapter adapter;

    private int USERID;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_scan);

        adapter = NfcAdapter.getDefaultAdapter(this);

        ImageView meebleImage = findViewById(R.id.meebleImage);
        Animation pulse = AnimationUtils.loadAnimation(this, R.anim.pulse);
        meebleImage.startAnimation(pulse);

        USERID = getIntent().getIntExtra("userId", -1);
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

            intent.putExtra("place_id", value);
            intent.putExtra("userId", USERID); // transfers userId as well

            findViewById(android.R.id.content).postDelayed(() -> startActivity(intent), 500); // this gives the program enough time to disable reader
            // so then the new nfc tag pop up does not show up.
        });
    }
    private boolean isNumeric(String num) {
        try {
            int number = Integer.parseInt(num);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // uses SharedPreferences to mark if id has been taken
    private void markPlaceIdTaken(int value) {
        SharedPreferences prefs = getSharedPreferences("place_ids", MODE_PRIVATE);
        prefs.edit().putBoolean("taken_" + value, true).apply();
    }

    // checks if ID is taken
    private boolean isPlaceIdTaken(int value) {
        SharedPreferences prefs = getSharedPreferences("place_ids", MODE_PRIVATE);
        return prefs.getBoolean("taken_" + value, false);
    }

    private int findUnusedPlaceId() {
        ArrayList<Integer> ids = new ArrayList<>(Arrays.asList(1, 2, 3, 4));

        for (int i = ids.size() - 1; i >= 0; i--) {
            if (isPlaceIdTaken(ids.get(i))) {
                ids.remove(i);
            }
        }

        if (ids.isEmpty()) {
            return -1; // all IDs taken
        }
        Random rand = new Random();
        return ids.get(rand.nextInt(ids.size()));
    }

    private int readNumberFromTag(Tag tag) {
        Ndef ndef = Ndef.get(tag);
        try {
            ndef.connect();
            NdefMessage msg = ndef.getNdefMessage();
            String data = new String(msg.getRecords()[0].getPayload());
            Log.d(TAG, "Read from tag: " + data);
            data = data.substring(3);

            if (!isNumeric(data)) {

                int number = findUnusedPlaceId(); // gets unusedId

                if (number != - 1) {
                    writeNumberToTag(tag, number);
                    markPlaceIdTaken(number);
                    return number;
                } else {
                    Log.d(TAG, "NO PLACEIDs REMAINING");
                }
            }

            return Integer.valueOf(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Integer.MIN_VALUE;
    }
    private void writeNumberToTag(Tag tag, int value) {
        NdefRecord record = NdefRecord.createTextRecord("en", String.valueOf(value));
        NdefMessage message = new NdefMessage(record);

        Ndef ndef = Ndef.get(tag);

        if (ndef == null) {
            // Raw tag, needs formatting first
            formatAndWriteNumberToTag(tag, message);
        } else {
            try {
                ndef.writeNdefMessage(message);
                Log.d(TAG, "Successfully wrote " + value + " to tag!");
            } catch (Exception e) {
                Log.e(TAG, "Failed to write value to tag", e);
            } finally {
                try {
                    ndef.close();
                } catch (Exception e) {
                    Log.e(TAG, "Failed to close tag", e);
                }
            }
        }
    }

    private void formatAndWriteNumberToTag(Tag tag, NdefMessage message) {
        android.nfc.tech.NdefFormatable formatableTag = android.nfc.tech.NdefFormatable.get(tag);

        if (formatableTag == null) {
            Log.e(TAG, "Tag is not NDEF-formatable");
            return;
        }

        try {
            formatableTag.connect();
            formatableTag.format(message); // formats and writes in one step
            Log.d(TAG, "Tag formatted and written successfully!");
        } catch (Exception e) {
            Log.e(TAG, "Failed to format tag", e);
        } finally {
            try {
                formatableTag.close();
            } catch (Exception e) {
                Log.e(TAG, "Failed to close formatable tag", e);
            }
        }
    }

    private class MYNFCCallbackClass implements NfcAdapter.ReaderCallback {
        @Override
        public void onTagDiscovered(Tag tag) {
//            int value = readNumberFromTag(tag);
//
//            // If tag does not already contain a valid place number, assign one
//            if (value < 1 || value > 4) {
//                value = findUnusedPlaceId();
//
//                if (value == -1) {
//                    runOnUiThread(() ->
//                            Toast.makeText(ScanActivity.this, "All 4 place IDs are already taken!", Toast.LENGTH_SHORT).show()
//                    );
//                    return;
//                }
//
//                writeNumberToTag(tag, value);
//                markPlaceIdTaken(value);
//            } else {
//                // valid existing tag
//                markPlaceIdTaken(value);
//            }
//
//            int finalValue = value;
//            runOnUiThread(() -> {
//                Toast.makeText(ScanActivity.this, "Tag mapped to place " + finalValue, Toast.LENGTH_SHORT).show();
//                routeToPlace(finalValue);
//            });

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


