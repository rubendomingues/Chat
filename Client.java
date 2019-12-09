import java.nio.*;

class Client {

    String nick;
    String room;
    State status;
    SocketChannel sc;

    static enum State{
        INIT,
        OUTSIDE,
        INSIDE
    }

    public Client(String nick, String room, State status, SocketChannel sc) {
        this.nick = nick;
        this.room = room;
        this.status = status;
        this.sc = sc;
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

    public SocketChannel getSc() {
        return sc;
    }

    public void setSc(SocketChannel sc) {
        this.sc = sc;
    }
}
