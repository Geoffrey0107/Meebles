package MAD.Meebles;

import java.util.ArrayList;
import java.util.List;

public class Place {
    private String name;
    private int placeId;
    private double growthRate;
    private int capacity;
    private int population;
    private List<Integer> populationHistory;

    public Place(String name, int placeId, double growthRate, int capacity, int initialPopulation) {
        this.name = name;
        this.placeId = placeId;
        this.growthRate = growthRate;
        this.capacity = capacity;
        this.population = initialPopulation;
        this.populationHistory = new ArrayList<>();
        this.populationHistory.add(initialPopulation);
    }

    public void grow(){
        population = (int) (population * Math.exp(growthRate));
        if (population > capacity) population = capacity;
        populationHistory.add(population);
    }
    public int kidnap(int amount) {
        int actual = Math.min(amount, population);
        population -= actual;
        populationHistory.add(population);
        return actual;
    }

    public void release(int amount){
        int actual = Math.min(amount, capacity - population);
        population += actual;
        populationHistory.add(population);
    }

    public String getName(){
        return name;
    }

    public int getCapacity(){
        return this.capacity;
    }

    public int getPopulation(){
        return this.population;
    }

    public double getGrowthRate(){
        return this.growthRate;
    }

    public List<Integer> getPopulationHistory() {
        return populationHistory;
    }

    public int getPlaceId(){
        return placeId;
    }
}
