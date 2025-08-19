package org.firedoge.models;

public class Tier {
    private final String name;
    private final double minScore;
    private final double maxScore;

    public Tier(String name, double minScore, double maxScore) {
        this.name = name;
        this.minScore = minScore;
        this.maxScore = maxScore;
    }

    public String getName() {
        return name;
    }

    public double getMinScore() {
        return minScore;
    }

    public double getMaxScore() {
        return maxScore;
    }

    @Override
    public String toString() {
        return "Tier{" +
                "name='" + name + '\'' +
                ", minScore=" + minScore +
                ", maxScore=" + maxScore +
                '}';
    }
    
}
