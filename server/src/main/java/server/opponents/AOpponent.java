package server.opponents;

import server.logicmap.LogicMapHandler;
import server.User;
import server.bullets.ABullet;
import server.bullets.BulletFactory;
import server.bullets.BulletList;
import server.connection.GameController;
import server.opponents.opponentai.IOpponentAI;
import server.opponents.opponentai.OpponentAIFactory;
import server.pickups.PickupFactory;
import server.pickups.PickupList;
import java.awt.*;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class AOpponent {
    private double x = 300;
    private double y = 300;
    private double speed = 0.25;
    private double viewRange = 600;
    private int width;
    private int height;
    private int id;
    private String type;
    private int hp;
    private long shootCooldown = 1000L;
    private long lastTimePlayerHasShot = 0L;
    protected IOpponentAI opponentAI;
    private boolean isDead = false;
    private String idOfChasedPlayer = "";
    private String bulletType;
    protected LogicMapHandler map;
    protected CopyOnWriteArrayList<User> usersInRoom;
    protected BulletList bulletList;
    protected OpponentList opponentList;
    protected PickupList pickupList;
    protected GameController gameController;

    protected AOpponent(GameController gameController){
        this.map = gameController.logicMapHandler;
        this.usersInRoom = gameController.usersInRoom;
        this.bulletList = gameController.bulletList;
        this.opponentList = gameController.opponentList;
        this.pickupList = gameController.pickupList;
        this.gameController = gameController;
    }

    public void update(double deltaTime){
        checkIfOpponentShouldDie();
    }

    protected void handleOpponentShoot(){
        double distance;
        float angle;
        if(this.canOpponentShoot()){
            for (User user : this.usersInRoom) {
                distance = Math.sqrt((double)(Math.abs(user.getPlayer().getCenterY() - this.getCenterY())) * (Math.abs(user.getPlayer().getCenterY() - this.getCenterY())) +
                        (Math.abs(this.getCenterX() - user.getPlayer().getCenterX()) * (Math.abs(this.getCenterX() - user.getPlayer().getCenterX()))));
                if (distance < this.getViewRange()) {
                    angle = (float) (Math.atan2((float)user.getPlayer().getCenterY() - this.getCenterY(), (double)this.getCenterX() - user.getPlayer().getCenterX()));
                    ABullet newBullet = BulletFactory.createBullet(this.getBulletType(), this.getCenterX(), this.getCenterY(), -angle + (float) Math.PI, true, this.gameController);
                    this.bulletList.addBullet(newBullet);
                }
            }
        }
    }

    public boolean isOpponentCollidesWithOpponents(Rectangle rectangle){
        boolean result = false;

        for(AOpponent opponent : this.opponentList.getOpponents()){
            if(opponent != this && opponent.getBounds().intersects(rectangle)){
                result = true;
            }
        }
        return result;
    }

    public void setOpponentAI(String type){
        this.opponentAI = OpponentAIFactory.createOpponentAI("Chaser",this, gameController, false);
    }

    public boolean isOpponentCollidesWithMap(Rectangle rec){
        return this.map.isRectangleCollidesWithMap(rec);
    }

    public Rectangle getBounds(){
        return new Rectangle((int)this.x, (int)this.y, this.width, this.height);
    }

    public void doDamage(int damage){
        this.hp -= damage;
    }
    public boolean canOpponentShoot(){
        boolean result = false;
        Date date= new Date();
        long time = date.getTime();
        if(time - this.lastTimePlayerHasShot > this.shootCooldown){
            result = true;
            this.lastTimePlayerHasShot = time;
        }
        return result;
    }

    protected void handleOpponentDeath(){
        this.opponentList.deleteOpponent(this);
    }

    public void dropRandomPickup(){
        int randomInt = Math.abs(new Random().nextInt()%3);
        switch (randomInt){
            case 0:
                this.pickupList.addPickup(PickupFactory.createPickup(this.getCenterX(), this.getCenterY(),"Hp"));
                break;
            case 1:
                this.pickupList.addPickup(PickupFactory.createPickup(this.getCenterX(), this.getCenterY(),"InnerEye"));
                break;
            case 2:
                this.pickupList.addPickup(PickupFactory.createPickup(this.getCenterX(), this.getCenterY(),"SadOnion"));
                break;
            default:
        }
    }

    private void checkIfOpponentShouldDie(){
        if(this.getHp() <= 0){
            this.isDead = true;
        }
        if(this.isDead){
            handleOpponentDeath();
        }
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public int getHp() {
        return hp;
    }

    public void setHp(int hp) {
        this.hp = hp;
    }

    public int getCenterX(){
        return (int)this.getX() + (int)(this.getWidth()/2.0);
    }

    public int getCenterY(){
        return (int)this.getY() + (int)(this.getHeight()/2.0);
    }

    public double getViewRange() {
        return viewRange;
    }

    public void setViewRange(double viewRange) {
        this.viewRange = viewRange;
    }

    public String getIdOfChasedPlayer() {
        return idOfChasedPlayer;
    }

    public void setIdOfChasedPlayer(String idOfChasedPlayer) {
        this.idOfChasedPlayer = idOfChasedPlayer;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public String getBulletType() {
        return bulletType;
    }

    public void setBulletType(String bulletType) {
        this.bulletType = bulletType;
    }

    public IOpponentAI getOpponentAI() {
        return opponentAI;
    }

    public void setOpponentAI(IOpponentAI opponentAI) {
        this.opponentAI = opponentAI;
    }
}
