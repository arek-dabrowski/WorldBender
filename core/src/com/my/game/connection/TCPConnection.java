package com.my.game.connection;

import com.badlogic.gdx.Gdx;
import com.my.game.WBGame;
import com.my.game.bullets.ABullet;
import com.my.game.bullets.BulletFactory;
import com.my.game.bullets.BulletList;
import com.my.game.music.MusicManager;
import com.my.game.opponents.AOpponent;
import com.my.game.opponents.OpponentFactory;
import com.my.game.opponents.OpponentList;
import com.my.game.pickups.APickup;
import com.my.game.pickups.PickupFactory;
import com.my.game.pickups.PickupList;
import com.my.game.player.Player;
import com.my.game.player.PlayerDataWrapper;
import com.my.game.player.PlayerFactory;
import com.my.game.player.PlayerList;
import com.my.game.Properties;
import com.my.game.rooms.Room;
import com.my.game.screens.GameplayScreen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TCPConnection implements Runnable {
    private static Logger logger = LogManager.getLogger(TCPConnection.class.getName());
    private WBGame game;
    private final int PORT = Integer.parseInt(Properties.loadConfigFile("PORT_TCP"));
    private String hostname;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private static Map<String, Player> players;

    public TCPConnection(String hostName, WBGame game) throws IOException {
        this.game = game;
        this.hostname = hostName;
        socket = new Socket(hostname, PORT);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        players = PlayerList.getInstance();
    }

    @Override
    public void run() {
        String message;
        while (true) {
            try {
                if ((message = in.readLine()) != null){
                    readMessage(message);
                }
            } catch (IOException e) {
                logger.error(e.toString(), e);
            }
        }
    }

    public void sendMessage(JSONObject message) {
        out.println(message.toString());
    }

    public void readMessage(String message) {
        JSONObject json = new JSONObject(message);
        JSONObject contentJSON = (JSONObject) json.get("content");

        switch (json.getString("msg")) {
            case "startGame":
                startGame(contentJSON);
                break;
            case "createBullet":
                ABullet newBullet = BulletFactory.createBullet(contentJSON.getString("type"),contentJSON.getInt("id"), contentJSON.getFloat("angle"));
                BulletList.addBullet(newBullet); break;
            case "deleteBullet":
                BulletList.removeBulletById(contentJSON.getInt("id")); break;
            case "disconnect":
                players.remove(contentJSON.get("name").toString());
                System.out.println("Remove player: " + (contentJSON.getString("name")));
                break;
            case "deleteOpponent":
                OpponentList.removeOpponentById(contentJSON.getInt("id"));
                break;
            case "createPickup":
                APickup pickup = PickupFactory.createPickup(
                        contentJSON.getInt("x"),
                        contentJSON.getInt("y"),
                        contentJSON.getInt("id"),
                        contentJSON.getString("type")
                );
                PickupList.addPickup(pickup);
                break;
            case "deletePickup":
                PickupList.removePickupById(contentJSON.getInt("id"));
                MusicManager.playHpUpSound();
                break;
            case "createdRoom":
                System.out.println("Created room ID: " + contentJSON.getInt("id"));
                Gdx.app.postRunnable(() -> game.changeScreen(WBGame.ROOM_OWNER, contentJSON.getInt("id"), contentJSON.getString("userId"), parsePlayersList(contentJSON)));
                break;
            case "roomList":
                System.out.println("Got room list:");
                Gdx.app.postRunnable(() -> game.changeScreen(WBGame.ROOM_LIST, 0, parseRoomList(contentJSON)));
                break;
            case "joinedRoom":
                System.out.println("Joined room ID: " + contentJSON.getInt("id"));
                Gdx.app.postRunnable(() -> game.changeScreen(WBGame.ROOM, contentJSON.getInt("id"), contentJSON.getString("userId"), parsePlayersList(contentJSON)));
                break;
            case "ownerLeftRoom":
                System.out.println("Left room ID: " + contentJSON.getInt("id"));
                Gdx.app.postRunnable(() -> game.changeScreen(WBGame.ROOM_LIST, 1, parseRoomList(contentJSON)));
                break;
            case "fullRoom":
                System.out.println("Room is full: " + contentJSON.getInt("id"));
                Gdx.app.postRunnable(() -> game.changeScreen(WBGame.ROOM_LIST, 2, parseRoomList(contentJSON)));
                break;
            case "roomDoesNotExist":
                System.out.println("Room doesn't exist: " + contentJSON.getInt("id"));
                Gdx.app.postRunnable(() -> game.changeScreen(WBGame.ROOM_LIST, 3, parseRoomList(contentJSON)));
                break;
            case "refreshRoomData":
                WBGame.roomScreen.refreshPlayerCards(parsePlayersList(contentJSON));
                break;
            case "refreshRoomListData":
                WBGame.roomListScreen.refreshRoomList(parseRoomList(contentJSON));
                break;
            case "changeLevel":
                this.changeLevel(contentJSON);
                break;
            default:
        }
    }

    private void startGame(JSONObject contentJSON) {
        Gdx.app.postRunnable(() -> game.changeScreen(WBGame.PLAY));
        JSONArray opponents = contentJSON.getJSONArray("opponents");
        createOpponents(opponents);

        JSONArray playersJSON = contentJSON.getJSONArray("players");
        for (int i = 0; i < playersJSON.length(); i++) {
            JSONObject player = playersJSON.getJSONObject(i);
            Player newPlayer = PlayerFactory.createPlayer(player.getString("playerType"), player.getString("name"), player.getString("id"));
            if (player.getString("id").equals(contentJSON.getString("current"))) {
                newPlayer.setCurrentPlayer(true);
                GameplayScreen.currentPlayer = newPlayer;
            }
            players.put(player.getString("id"), newPlayer);
        }
    }

    private void changeLevel(JSONObject contentJSON){
        System.out.println(contentJSON);
        JSONArray opponents = contentJSON.getJSONArray("opponents");
        createOpponents(opponents);

        JSONArray playersFromServer = contentJSON.getJSONArray("players");

        for (Player player : players.values()) {
            for (int i = 0; i < playersFromServer.length(); i++) {
                JSONObject receivedPlayer = playersFromServer.getJSONObject(i);
                if (player.getId().equals(receivedPlayer.getString("id"))) {
                    player.setPosition(receivedPlayer.getInt("x"), receivedPlayer.getInt("y"));
                }
            }
        }

        game.getGameplayScreen().changeLevel(contentJSON.getString("map"));
    }

    private void createOpponents(JSONArray opponents){
        for (int i = 0; i < opponents.length(); i++) {
            JSONObject opponent = opponents.getJSONObject(i);
            AOpponent newOpponent = OpponentFactory.createOpponent(opponent.getString("type"), opponent.getInt("id"));
            OpponentList.addOpponent(newOpponent);
        }

    }

    private List<Room> parseRoomList(JSONObject contentJSON){
        List<Room> rooms = new ArrayList<>();
        System.out.println(contentJSON);
        JSONArray jsonRooms = contentJSON.getJSONArray("rooms");

        for (int i = 0; i < jsonRooms.length(); i++) {
            JSONObject room = jsonRooms.getJSONObject(i);
            rooms.add(new Room(room.getInt("id"), room.getString("owner"), room.getInt("numberOfPlayers"), room.getBoolean("gameStatus")));
        }

        return rooms;
    }

    private List<PlayerDataWrapper> parsePlayersList(JSONObject contentJSON){
        List<PlayerDataWrapper> players = new ArrayList<>();
        System.out.println(contentJSON);
        JSONArray jsonPlayers = contentJSON.getJSONArray("players");

        for (int i = 0; i < jsonPlayers.length(); i++) {
            JSONObject player = jsonPlayers.getJSONObject(i);
            players.add(new PlayerDataWrapper(player.getString("name"), player.getString("userId"), player.getString("character"), player.getBoolean("owner")));
        }

        return players;
    }
}
