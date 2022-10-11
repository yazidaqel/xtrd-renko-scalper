package io.xtrd.trading;

public class Symbol implements Comparable<Symbol>{
    private String name;
    private int sizePower;
    private int pricePower;

    public Symbol(String name, int sizePower, int pricePower) {
        this.name = name;
        this.sizePower = sizePower;
        this.pricePower = pricePower;
    }

    public String getName() {
        return name;
    }

    public int getSizePower() {
        return sizePower;
    }

    public int getPricePower() {
        return pricePower;
    }

    public int compareTo(Symbol p){
        return name.compareTo(p.getName());
    }

    @Override
    public String toString() {
        return name;
    }
}
