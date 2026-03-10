package MAD.Meebles;

import android.content.Intent;
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {
    private TextView countdownText;


    final int[][] targetTimes = { {17, 0, 0} }; // 17:00:00

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        countdownText = (TextView) findViewById(R.id.timeDisplay);

        // create new user
        // store it in hashmap
        // store internal

        File root = getFilesDir();

        // if there is no file that contains userRepo
        if (!containsFile(root, "users.dat")) {
            // write to it
            File targetFile = new File(root, "users.dat");
            userObj user = new userObj(0);

            userRepo users = new userRepo();
            users.addToRepo(user);

            File rootDirOfMyApp = getFilesDir();
//            File targetFile = new File(rootDirOfMyApp, "pc.dat");

            try {
                FileOutputStream fileOut = new FileOutputStream(targetFile);
                ObjectOutputStream out = new ObjectOutputStream(fileOut);
                out.writeObject(pc);
            } catch (FileNotFoundException fnfe) {
                fnfe.printStackTrace();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }

            try {
                Log.d(TAG, "Wrote to: " + targetFile.getCanonicalPath());
            } catch(IOException ioe) {
                Log.d(TAG, "Wrote to abs path: " + targetFile.getAbsolutePath());
            }


        } else {
            File targetFile = new File(root, "users.dat");
        }

        // display user data on texts

        startNextCountdown();
    }

    public boolean containsFile(File dir, String targetName) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file: files) {
                // only grabs audio files
                String filename = file.getName();
                if (filename.equals(targetName) { // so it does not display this file
                    return true;
                }
            }
        }
        return false;
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
                nextTarget = temp;
                break;
            }
        }

        if (nextTarget == null) { // all times passed today, pick first target tomorrow
            nextTarget = (Calendar) now.clone();
            nextTarget.add(Calendar.DAY_OF_MONTH, 1);
            nextTarget.set(Calendar.HOUR_OF_DAY, targetTimes[0][0]);
            nextTarget.set(Calendar.MINUTE, targetTimes[0][1]);
            nextTarget.set(Calendar.SECOND, targetTimes[0][2]);
            nextTarget.set(Calendar.MILLISECOND, 0);
        }

        long diffMillis = nextTarget.getTimeInMillis() - now.getTimeInMillis();

        new CountDownTimer(diffMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long seconds = millisUntilFinished / 1000 % 60;
                long minutes = millisUntilFinished / (1000 * 60) % 60;
                long hours   = millisUntilFinished / (1000 * 60 * 60);

                countdownText.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
            }

            @Override
            public void onFinish() {
                countdownText.setText("00:00:00");
                // Start countdown to the next target automatically

                // create 4 new objects with default populations and then overwrite what
                // is in internal storage.
                startNextCountdown();
            }
        }.start();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        menu.findItem(R.id.action_close).setVisible(false);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        final int id = item.getItemId();
        if (id == R.id.pickupDropPage) {
            Intent i = new Intent(getApplicationContext(), ScanActivity.class);
            startActivity(i);
        } else if (id == R.id.action_close) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return true;
        } else {
            // system will handle if none were clicked
            return super.onOptionsItemSelected(item);
        }

        // menu item has been handled
        return true;
    }
}