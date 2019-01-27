package com.my.game.screens.dialogs;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.my.game.WBGame;

public class JoinRoomDialog extends Dialog {
    private Skin skin;
    private Stage stage;

    public JoinRoomDialog(Skin skin, Stage stage){
        super("Join Room", skin);

        this.skin = skin;
        this.stage = stage;

        Table newTable = new Table();
        newTable.setFillParent(true);
        newTable.setDebug(false);
        addActor(newTable);

        TextField tf = new TextField("", skin);
        tf.setMessageText("Room ID");
        tf.setPosition(30, 30);
        newTable.add(tf).fillX().uniformX();
        button("Join Room", tf);
        button("Cancel");

        this.show(stage);
    }

    @Override
    protected void result(Object object) {
        TextField newTf = (TextField) object;
        int id;
        try{
            id = Integer.parseInt(newTf.getText());
            WBGame.connection.tcp.sendMessage("joinRoom:" +
                    WBGame.connection.socket.getLocalPort() + ":" +
                    id);
        } catch (NumberFormatException e) {
            new ErrorDialog(skin, stage, "Room ID has to be a number!");
        }
    }

}