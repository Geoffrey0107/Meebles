package MAD.Meebles;

import java.util.ArrayList;
import java.util.List;

public class PlaceRepo {
    static PlaceRepo place;
    private List<Place> places;

    private PlaceRepo() {
        places = new ArrayList<>();
        places.add(new Place("The Meebous",   1, 0.10, 20,  5));
        places.add(new Place("Meeblage",       2, 0.08, 100, 20));
        places.add(new Place("South Meeburg",      3, 0.06, 500, 80));
        places.add(new Place("New Meeble City",    4, 0.05, 2000, 300));
    }

    public static PlaceRepo getPlace() {
        if (place == null) place = new PlaceRepo();
        return place;
    }

    public Place getByPlaceId(int placeId) {
        for (Place p : places) {
            if (p.getPlaceId() == placeId) return p;
        }
        return null;
    }
}
