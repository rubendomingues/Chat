import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.util.*;
//usar porta 8008 ou 8080
public class ChatServer
{
  // A pre-allocated buffer for the received data
  static private final ByteBuffer buffer = ByteBuffer.allocate( 16384 );

  // Decoder for incoming text -- assume UTF-8
  static private final Charset charset = Charset.forName("UTF8");
  static private final CharsetDecoder decoder = charset.newDecoder();

  //HashSet to store all usernames used
  static private HashMap<SocketChannel,Client> clients = new HashMap<SocketChannel,Client>();
  static private HashSet<String> usernames = new HashSet<String>();
  static public void main( String args[] ) throws Exception {
    // Parse port from command line
    int port = Integer.parseInt( args[0] );
    usernames.add("");

    try {
      // Instead of creating a ServerSocket, create a ServerSocketChannel
      ServerSocketChannel ssc = ServerSocketChannel.open();

      // Set it to non-blocking, so we can use select
      ssc.configureBlocking( false );

      // Get the Socket connected to this channel, and bind it to the
      // listening port
      ServerSocket ss = ssc.socket();
      InetSocketAddress isa = new InetSocketAddress( port );
      ss.bind( isa );

      // Create a new Selector for selecting
      Selector selector = Selector.open();

      // Register the ServerSocketChannel, so we can listen for incoming
      // connections
      ssc.register( selector, SelectionKey.OP_ACCEPT );
      System.out.println( "Listening on port "+port );

      while (true) {
        // See if we've had any activity -- either an incoming connection,
        // or incoming data on an existing connection
        int num = selector.select();

        // If we don't have any activity, loop around and wait again
        if (num == 0) {
          continue;
        }

        // Get the keys corresponding to the activity that has been
        // detected, and process them one by one
        Set<SelectionKey> keys = selector.selectedKeys();
        Iterator<SelectionKey> it = keys.iterator();
        while (it.hasNext()) {
          // Get a key representing one of bits of I/O activity
          SelectionKey key = it.next();

          // What kind of activity is it?
          if (key.isAcceptable()) {

            // It's an incoming connection.  Register this socket with
            // the Selector so we can listen for input on it
            Socket s = ss.accept();
            System.out.println( "Got connection from "+s );

            // Make sure to make it non-blocking, so we can use a selector
            // on it.
            SocketChannel sc = s.getChannel();
            sc.configureBlocking( false );
            Client client = new Client();
            clients.put(sc,client);
            // Register it with the selector, for reading
            sc.register( selector, SelectionKey.OP_READ, client);
            //DONE-----------------------------------
          } else if (key.isReadable()) {

            SocketChannel sc = null;

            try {

              // It's incoming data on a connection -- process it
              sc = (SocketChannel)key.channel();
              // System.out.println("Received a message");
              buffer.clear();
              sc.read(buffer);
              buffer.flip();

              if(buffer.limit() == 0)
                continue;

              // boolean ok = processInput( sc );
              String message = decoder.decode(buffer).toString();
              clients.get(sc).setBuffer(clients.get(sc).getBuffer()+message);

              if(message.charAt(message.length()-1)!='\n')
                continue;

              String usermessage = clients.get(sc).getBuffer();
              clients.get(sc).setBuffer("");

              String userMessages[] = usermessage.split("\n");

              // If the connection is dead, remove it from the selector
              // and close it
              for(int i=0; i<userMessages.length; i++){
                boolean ok = processInput(sc, (Client) key.attachment(),userMessages[i]);
                if (!ok) {
                  key.cancel();
                  Socket s = null;
                  try {
                    s = sc.socket();
                    System.out.println( "Closing connection to "+s );
                    if(clients.get(sc).status == Client.State.INIT){
                      messageRoom("LEFT " + clients.get(sc).nick, clients.get(sc).room);
                    }
                    clients.remove(sc);
                    s.close();
                  } catch( IOException ie ) {
                    System.err.println( "Error closing socket "+s+": "+ie );
                  }
                }
              }

            } catch( IOException ie ) {

              // On exception, remove this channel from the selector
              key.cancel();

              try {
                sc.close();
              } catch( IOException ie2 ) { System.out.println( ie2 ); }

              System.out.println( "Closed "+sc );
            }
          }
        }

        // We remove the selected keys, because we've dealt with them.
        keys.clear();
      }
    } catch( IOException ie ) {
      System.err.println( ie );
    }
  }

  static private void messageRoom(String message, String room)throws IOException{
    for(SocketChannel sc : clients.keySet()){
      if(clients.get(sc).room.equals(room)){
        buffer.clear();
        buffer.put(message.getBytes());
        buffer.flip();
        while(buffer.hasRemaining()){
          sc.write(buffer);
        }
      }
    }
  }

  static private void messagePrivate(String message, String person)throws IOException{
    for(SocketChannel sc : clients.keySet()){
      if(clients.get(sc).nick.equals(person)){
        buffer.clear();
        buffer.put(message.getBytes());
        buffer.flip();
        while(buffer.hasRemaining()){
          sc.write(buffer);
        }
        break;
      }
    }
  }

  // Just read the message from the socket and send it to stdout
  static private boolean processInput(SocketChannel sc , Client client, String message) throws IOException {
    // Read the message to the buffer
    buffer.clear();
    sc.read(buffer);
    buffer.flip();

    // If no data, close the sssdconnection
    // if (buffer.limit()==0) {
    //   return false;
    // }

    String totalMessage[] = message.split(" ");

    if(totalMessage[0].equals("/nick")){
      if(!usernames.contains(totalMessage[1]) && clients.get(sc).status == Client.State.INIT){
        clients.get(sc).nick = totalMessage[1];
        clients.get(sc).status = Client.State.OUTSIDE;
        usernames.add(totalMessage[1]);
        buffer.clear();
        buffer.put("OK\n".getBytes());
        buffer.flip();
        sc.write(buffer);
      }
      else if(usernames.contains(totalMessage[1]) && clients.get(sc).status == Client.State.INIT){
        buffer.clear();
        buffer.put("ERROR\n".getBytes());
        buffer.flip();
        sc.write(buffer);
      }
      else if(!usernames.contains(totalMessage[1]) && clients.get(sc).status == Client.State.OUTSIDE){
        clients.get(sc).nick = totalMessage[1];
        usernames.add(totalMessage[1]);
        buffer.clear();
        buffer.put("OK\n".getBytes());
        buffer.flip();
        sc.write(buffer);
      }
      else if(usernames.contains(totalMessage[1]) && clients.get(sc).status == Client.State.OUTSIDE){
        buffer.clear();
        buffer.put("ERROR\n".getBytes());
        buffer.flip();
        sc.write(buffer);
      }
      else if(!usernames.contains(totalMessage[1]) && clients.get(sc).status == Client.State.INSIDE){
        messageRoom("NEWNICK " + clients.get(sc).nick +" "+totalMessage[1]+"\n", clients.get(sc).room);
        clients.get(sc).nick = totalMessage[1];
        usernames.add(totalMessage[1]);
        buffer.clear();
        buffer.put("OK\n".getBytes());
        buffer.flip();
        sc.write(buffer);
      }
      else if(usernames.contains(totalMessage[1]) && clients.get(sc).status == Client.State.INSIDE){
        buffer.clear();
        buffer.put("ERROR\n".getBytes());
        buffer.flip();
        sc.write(buffer);
      }
    }
    else if(totalMessage[0].equals("/join")){
      if(clients.get(sc).status == Client.State.OUTSIDE){
        clients.get(sc).status = Client.State.INSIDE;
        buffer.clear();
        buffer.put("OK\n".getBytes());
        buffer.flip();
        sc.write(buffer);
        messageRoom("JOINED " + clients.get(sc).nick+"\n", totalMessage[1]);
        clients.get(sc).room = totalMessage[1];
      }
      else if(clients.get(sc).status == Client.State.INSIDE){
        buffer.clear();
        buffer.put("OK\n".getBytes());
        buffer.flip();
        sc.write(buffer);
        String room = clients.get(sc).room;
        clients.get(sc).room = "";
        messageRoom("LEFT " + clients.get(sc).nick+"\n", room);
        messageRoom("JOINED " + clients.get(sc).nick+"\n", totalMessage[1]);
        clients.get(sc).room = totalMessage[1];
      }
      else{
        buffer.clear();
        buffer.put("ERROR\n".getBytes());
        buffer.flip();
        sc.write(buffer);
      }
    }
    else if(totalMessage[0].equals("/leave")){
      if(clients.get(sc).status == Client.State.INSIDE){
        buffer.clear();
        buffer.put("OK\n".getBytes());
        buffer.flip();
        sc.write(buffer);
        String room = clients.get(sc).room;
        clients.get(sc).room = "";
        messageRoom("LEFT " + clients.get(sc).nick+"\n", room);
        clients.get(sc).status = Client.State.OUTSIDE;
      }
    }
    else if(totalMessage[0].equals("/priv")){
      int flag = 0;
      for(SocketChannel sct : clients.keySet()){
        if(clients.get(sct).nick.equals(totalMessage[1])){
          buffer.clear();
          buffer.put("OK\n".getBytes());
          buffer.flip();
          sc.write(buffer);
          flag=1;
          String mensagem ="";
          for(int i=2; i<totalMessage.length; i++)
            mensagem+=totalMessage[i];
          messagePrivate("PRIVATE " + clients.get(sc).nick+" "+mensagem+"\n",totalMessage[1]);
          break;
        }
      }
      if(flag==0){
        buffer.clear();
        buffer.put("ERROR\n".getBytes());
        buffer.flip();
        sc.write(buffer);
      }
    }
    else if(totalMessage[0].equals("/bye")){
      if(clients.get(sc).status != Client.State.INSIDE){
        buffer.clear();
        buffer.put("BYE\n".getBytes());
        buffer.flip();
        sc.write(buffer);
        usernames.remove(clients.get(sc).nick);
        clients.get(sc).room="";
        return false;
      }
      else if(clients.get(sc).status == Client.State.INSIDE){
        buffer.clear();
        buffer.put("BYE\n".getBytes());
        buffer.flip();
        sc.write(buffer);
        usernames.remove(clients.get(sc).nick);
        return false;
      }
    }
    else {
      if(clients.get(sc).status == Client.State.INSIDE)
        messageRoom("MESSAGE "+ clients.get(sc).nick +" "+message+"\n",clients.get(sc).room);
      else{
        buffer.clear();
        buffer.put("ERROR\n".getBytes());
        buffer.flip();
        sc.write(buffer);
      }
    }
    return true;
  }
}
