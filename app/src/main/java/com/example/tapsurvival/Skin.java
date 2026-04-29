package com.example.tapsurvival;

import android.graphics.Color;

public class Skin {
    public enum Shape { SQUARE, CIRCLE, TRIANGLE, HEXAGON }
    
    public String id;
    public String name;
    public Shape shape;
    public int color;
    public int price;
    public boolean unlocked;

    public Skin(String id, String name, Shape shape, int color, int price, boolean unlocked) {
        this.id = id;
        this.name = name;
        this.shape = shape;
        this.color = color;
        this.price = price;
        this.unlocked = unlocked;
    }
}
