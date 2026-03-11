package MAD.Meebles;

// store users in
public class userObj {
    private int id;
    private int score;
    private String name; // optional

    // Required empty constructor for Firestore
    public userObj() {}

    public userObj(int id) {
        this.id = id;
        this.score = 0;  // default score
        this.name = "Player" + id; // optional
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
}

