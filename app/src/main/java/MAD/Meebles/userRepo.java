package MAD.Meebles;

import java.util.HashMap;

public class userRepo {
    HashMap<Integer, userObj> hashMap = new HashMap<>();

    public void setHashMap(HashMap<Integer, userObj> hm) {
        this.hashMap = hm;
    }

    public HashMap<Integer, userObj> getHashMap() {
        return this.hashMap;
    }

//    public void addToRepo(userObj user) {
//        if (!hashMap.containsKey(user.getuserId())) {
//            hashMap.put(user.getuserId(), user);
//        }
//    }
}
