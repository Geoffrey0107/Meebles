package MAD.Meebles;

import java.util.HashMap;

public class userRepo {
    HashMap<Integer, userObj> hashMap = new HashMap<>();

    private static userRepo instance;

    public void setHashMap(HashMap<Integer, userObj> hm) {
        this.hashMap = hm;
    }

    public HashMap<Integer, userObj> getHashMap() {
        return this.hashMap;
    }

    public static userRepo getInstance() {
        if (instance == null) {
            instance = new userRepo();
        }

        return instance;
    }

    public void addToRepo(userObj user) {
        if (!hashMap.containsKey(user.getId())) {
            hashMap.put(user.getId(), user);
        }
    }
}
