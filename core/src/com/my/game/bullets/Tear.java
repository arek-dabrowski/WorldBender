package com.my.game.bullets;

public class Tear extends ABullet{
    public Tear(int id, float angle){
        this.setId(id);
        this.setX(-50);
        this.setY(-50);
        this.setAngle(angle);
    }
}