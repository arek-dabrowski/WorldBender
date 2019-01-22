package com.my.game.opponents;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class Poe extends AOpponent {
    public static Texture texture;
    public Poe(int id){
        super(id);
        this.setHp(100);
        this.setMaxHp(100);
    }
    @Override
    public void draw(SpriteBatch spriteBatch) {
        spriteBatch.draw(Poe.texture, this.getX(), this.getY());
        this.drawHp(spriteBatch, Poe.texture.getHeight());
        this.drawName(spriteBatch, "Poe", Poe.texture.getHeight());
    }

}