import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.util.*;

class Client {

    String nick;
    String room;
    State status;

    static enum State{
        INIT,
        OUTSIDE,
        INSIDE
    }

    public Client() {
        this.nick = "";
        this.room = "";
        this.status = State.INIT;
    }

    public String getNick() {
        return nick;
    }

    public void setNick(String nick) {
        this.nick = nick;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public State getStatus() {
        return status;
    }

    public void setStatus(State status) {
        this.status = status;
    }
}
