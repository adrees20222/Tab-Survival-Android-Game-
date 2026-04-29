package com.example.tapsurvival;

public class ThemeColor {
    public String id;
    public String name;
    public int color;
    public int price;
    public boolean unlocked;

    public ThemeColor(String id, String name, int color, int price, boolean unlocked) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.price = price;
        this.unlocked = unlocked;
    }
}
