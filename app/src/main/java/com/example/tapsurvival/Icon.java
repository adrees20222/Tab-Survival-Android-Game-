package com.example.tapsurvival;

public class Icon {
    public enum Type { SQUARE, CIRCLE, TRIANGLE, HEXAGON, DIAMOND, HEART, PENTAGON, EMOJI }
    
    public String id;
    public String name;
    public Type type;
    public String emoji; 
    public int price;
    public boolean unlocked;

    public Icon(String id, String name, Type type, String emoji, int price, boolean unlocked) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.emoji = emoji;
        this.price = price;
        this.unlocked = unlocked;
    }
}
