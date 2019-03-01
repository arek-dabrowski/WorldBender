package server;

import server.LogicMap.LogicMapHandler;
import server.bullets.AtackFabric;
import server.bullets.BulletList;
import server.connection.GameController;

import javax.jws.soap.SOAPBinding;
import java.awt.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.CopyOnWriteArrayList;

public class Player {

    private double x = 500;
    private double y = 500;
    private int width;
    private int height;
    private int hp = 100;
    public final int MAX_HP = 100;
    private final double MAX_SPEED = 0.5;
    private double moveSpeed = 0.0;
    private double acceleration = 0.0;
    private long shootCooldown = 100L;
    private long lastTimePlayerHasShot = 0L;
    private long shootSpeedModificator = 1L;
    private String activeMovementKey = "DOWN";
    private String bulletType = "Tear";
    private String weaponType = "Normal";
    private ArrayList<String> collectedItems;
    private boolean isMoving = false;
    public static final int PLAYER_TEXTURE_WIDTH = Integer.parseInt(Properties.loadConfigFile("PLAYER_TEXTURE_WIDTH"));
    public static final int PLAYER_TEXTURE_HEIGHT = Integer.parseInt(Properties.loadConfigFile("PLAYER_TEXTURE_HEIGHT"));
    private float scale = 2f;
    private User user;
    public boolean KEY_W = false;
    public boolean KEY_S = false;
    public boolean KEY_A = false;
    public boolean KEY_D = false;
    public boolean UP_ARROW = false;
    public boolean DOWN_ARROW = false;
    public boolean LEFT_ARROW = false;
    public boolean RIGHT_ARROW = false;
    private LogicMapHandler map;
    private BulletList bulletList;
    private CopyOnWriteArrayList<User> usersInRoom;
    private GameController gameController;

    public Player(User user) {
        this.setWidth((int) (PLAYER_TEXTURE_WIDTH * scale));
        this.setHeight((int) (PLAYER_TEXTURE_HEIGHT * scale));
        this.user = user;
        this.collectedItems = new ArrayList<String>();
    }

    public Player(User user, GameController gameController){
        this(user);
        this.map = gameController.logicMapHandler;
        this.bulletList = gameController.bulletList;
        this.usersInRoom = gameController.usersInRoom;
        this.gameController = gameController;
    }

    public void update(CopyOnWriteArrayList<User> usersInRoom, double deltaTime) {
        Rectangle playersNewBoundsRectangle;
        ArrayList<Player> players = new ArrayList<Player>();
        double currentShift;

        for (User user : usersInRoom) {
            if (user.getPlayer() != this) {
                players.add(user.getPlayer());
            }
        }
        if (this.isMoving) {
            currentShift = (deltaTime * this.calculateSpeed(deltaTime));
            if (this.KEY_W) {
                playersNewBoundsRectangle = new Rectangle((int)this.x, (int)(this.y + currentShift), this.getWidth(), this.getHeight());
                if (!isPlayersCollidesWithAnything(playersNewBoundsRectangle, players)) {
                    this.y += currentShift;
                }
            }
            if (this.KEY_S) {
                playersNewBoundsRectangle = new Rectangle((int)this.x, (int)(this.y - currentShift), this.getWidth(), this.getHeight());
                if (!isPlayersCollidesWithAnything(playersNewBoundsRectangle, players)) {
                    this.y -= currentShift;
                }
            }
            if (this.KEY_A) {
                playersNewBoundsRectangle = new Rectangle((int)(this.x - currentShift), (int)this.y, this.getWidth(), this.getHeight());
                if (!isPlayersCollidesWithAnything(playersNewBoundsRectangle, players)) {
                    this.x -= currentShift;
                }
            }
            if (this.KEY_D) {
                playersNewBoundsRectangle = new Rectangle((int)(this.x + currentShift), (int)this.y, this.getWidth(), this.getHeight());
                if (!isPlayersCollidesWithAnything(playersNewBoundsRectangle, players)) {
                    this.x += currentShift;
                }
            }
        }
        else {
            this.moveSpeed = (this.moveSpeed - (0.0003 * deltaTime)) < 0.0 ? 0.0 : this.moveSpeed - (0.0003 * deltaTime);
        }
    }

    public void shoot() {
        float angle = 0f;
        if(this.UP_ARROW){
            angle = (float)Math.PI/2;
        }
        if(this.DOWN_ARROW){
            angle = (float)(3 * Math.PI/2);
        }
        if(this.LEFT_ARROW){
            angle = (float)Math.PI;
        }
        if(this.RIGHT_ARROW){
            angle = 0f;
        }
        AtackFabric.createAtack(this, this.bulletList, angle, gameController);
    }

    public boolean canPlayerShoot() {
        boolean result = false;
        Date date = new Date();
        long time = date.getTime();
        if (time - this.lastTimePlayerHasShot > this.shootCooldown * this.shootSpeedModificator) {
            result = true;
            this.lastTimePlayerHasShot = time;
        }
        return result;
    }

    public Rectangle getBounds() {
        return new Rectangle((int)this.x, (int)this.y, this.getWidth(), this.getHeight());
    }

    public boolean isPlayerCollidesWithMap(Rectangle rec) {
        return this.map.isRectangleCollidesWithMap(rec);
    }

    public boolean isRectangleCollidesWithPlayers(Rectangle rec, ArrayList<Player> players) {
        boolean result = false;
        for (Player player : players) {
            if (rec.intersects(player.getBounds()) && player.getUser().hasConnection()) {
                result = true;
            }
        }
        return result;
    }

    public boolean isPlayersCollidesWithAnything(Rectangle rec, ArrayList<Player> players) {
        return isPlayerCollidesWithMap(rec) ||
                isRectangleCollidesWithPlayers(rec, players);
    }



    private double calculateSpeed(double deltaTime){
        double speed;
        this.moveSpeed = this.moveSpeed + (0.0003 * deltaTime) > (0.6 * this.MAX_SPEED) ? (0.6 * this.MAX_SPEED) : this.moveSpeed + (0.0003 * deltaTime);
        speed = (0.4 * this.MAX_SPEED) + this.moveSpeed;
        return speed;
    }

    public void doDamage(int damage) {
        this.setHp(this.getHp() - damage);
    }

    public void setWSAD(String wsad) {
        String splitedWsad[] = wsad.split(",");
        this.KEY_W = Boolean.parseBoolean(splitedWsad[0]);
        this.KEY_S = Boolean.parseBoolean(splitedWsad[1]);
        this.KEY_A = Boolean.parseBoolean(splitedWsad[2]);
        this.KEY_D = Boolean.parseBoolean(splitedWsad[3]);
        this.UP_ARROW = Boolean.parseBoolean(splitedWsad[4]);
        this.DOWN_ARROW = Boolean.parseBoolean(splitedWsad[5]);
        this.LEFT_ARROW = Boolean.parseBoolean(splitedWsad[6]);
        this.RIGHT_ARROW = Boolean.parseBoolean(splitedWsad[7]);
    }

    public int getHp() {
        return hp;
    }

    public void setHp(int hp) {
        this.hp = hp;
    }

    public void setShootCooldown(long shootCooldown) {
        this.shootCooldown = shootCooldown;
    }
    public long getShootCooldown() {
        return this.shootCooldown;
    }

    public String getActiveMovementKey() {
        return activeMovementKey;
    }

    public void setActiveMovementKey(String activeMovementKey) {
        this.activeMovementKey = activeMovementKey;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public boolean isMoving() {
        return isMoving;
    }

    public void setMoving(boolean moving) {
        isMoving = moving;
    }

    public int getCenterX() {
        return (int)this.getX() + (int) (this.getWidth() / 2.0);
    }

    public int getCenterY() {
        return (int)this.getY() + (int) (this.getHeight() / 2.0);
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getBulletType() {
        return bulletType;
    }

    public void setBulletType(String bulletType) {
        this.bulletType = bulletType;
    }

    public String getWeaponType() {
        return weaponType;
    }

    public void setWeaponType(String weaponType) {
        this.weaponType = weaponType;
    }

    public ArrayList<String> getCollectedItems() {
        return collectedItems;
    }

    public void setCollectedItems(ArrayList<String> collectedItems) {
        this.collectedItems = collectedItems;
    }
    public boolean hasPlayerItem(String item){
        boolean result = false;
        for(String colletedItem : this.collectedItems){
            result = colletedItem.equals(item);
        }
        return result;
    }

    public long getShootSpeedModificator() {
        return shootSpeedModificator;
    }

    public void setShootSpeedModificator(long shootSpeedModificator) {
        this.shootSpeedModificator = shootSpeedModificator;
    }
    public double getX() {
        return this.x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public double getY() {
        return this.y;
    }
    public GameController getGameController(){
        return this.gameController;
    }

    public double getAcceleration() {
        return acceleration;
    }

    public void setAcceleration(double acceleration) {
        this.acceleration = acceleration;
    }
}