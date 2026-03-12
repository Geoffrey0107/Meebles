package MAD.Meebles;

import java.util.ArrayList;
import java.util.List;

// store users in
public class userObj {
    private int id;
    private int score;
    private String name; // optional

    private double growthRate;

    private List<Integer> scoreHistory;

    // Required empty constructor for Firestore
    public userObj() {}

    public userObj(int id) {
        this.id = id;
        this.score = 0;  // default score
        this.name = "Player" + id; // optional
        this.growthRate = 0.05;   // example default
        this.scoreHistory = new ArrayList<>();
        this.scoreHistory.add(0);
    }

    public userObj(int id, int score, String name) {
        this.id = id;
        this.score = score;
        this.name = name;
    }



    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getGrowthRate() { return growthRate; }
    public void setGrowthRate(double growthRate) { this.growthRate = growthRate; }

    public List<Integer> getScoreHistory() { return scoreHistory; }
    public void setScoreHistory(List<Integer> scoreHistory) { this.scoreHistory = scoreHistory; }
}

