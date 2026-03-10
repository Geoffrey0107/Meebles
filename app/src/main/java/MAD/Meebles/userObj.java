package MAD.Meebles;

// store users in
public class userObj {

    private int currMeebles = 0; // amount of meebles the user has
    private int placement = -1; // determines place
    private int userId = -1; // userId
    private int currLoc = -1; // where the user is at


    public userObj(int userId) {
        this.userId = userId;
    }

    public void setPlacement(int placement) {
        this.placement = placement;
    }

    public void setcurrLoc(int currLoc) {
        this.currLoc = currLoc;
    }

    public void setCurrMeebles(int currMeebles) {
        this.currMeebles = currMeebles;
    }

    public int getcurrMeebles() {
        return this.currMeebles;
    }

    public int getPlacement() {
        return this.placement;
    }

    public int getcurrLoc() {

        return this.currLoc;
    }

    public int getuserId() {

        return this.userId;
    }
}
