import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;

public class ChatClient {

    // Variáveis relacionadas com a interface gráfica --- * NÃO MODIFICAR *
    JFrame frame = new JFrame("Chat Client");
    private JTextField chatBox = new JTextField();
    private JTextArea chatArea = new JTextArea();
    // --- Fim das variáveis relacionadas coma interface gráfica

    // Se for necessário adicionar variáveis ao objecto ChatClient, devem
    // ser colocadas aqui
    // A pre-allocated buffer for the received data
    static private ByteBuffer buffer = ByteBuffer.allocate( 16384 );
    private SocketChannel sc = null;

    // Decoder for incoming text -- assume UTF-8
    static private final Charset charset = Charset.forName("UTF8");
    static private final CharsetDecoder decoder = charset.newDecoder();

    // Método a usar para acrescentar uma string à caixa de texto
    // * NÃO MODIFICAR *
    public void printMessage(final String message) {
        chatArea.append(message);
    }


    // Construtor
    public ChatClient(String server, int port) throws IOException {

        // Inicialização da interface gráfica --- * NÃO MODIFICAR *
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(chatBox);
        frame.setLayout(new BorderLayout());
        frame.add(panel, BorderLayout.SOUTH);
        frame.add(new JScrollPane(chatArea), BorderLayout.CENTER);
        frame.setSize(500, 300);
        frame.setVisible(true);
        chatArea.setEditable(false);
        chatBox.setEditable(true);
        chatBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    newMessage(chatBox.getText());
                } catch (IOException ex) {
                } finally {
                    chatBox.setText("");
                }
            }
        });
        frame.addWindowListener(new WindowAdapter() {
            public void windowOpened(WindowEvent e) {
                chatBox.requestFocus();
            }
        });
        // --- Fim da inicialização da interface gráfica

        // Se for necessário adicionar código de inicialização ao
        // construtor, deve ser colocado aqui

        Thread t = new Thread();
        t.run();
        InetSocketAddress sa = new InetSocketAddress(server,port);
        sc = SocketChannel.open(sa);
    }


    // Método invocado sempre que o utilizador insere uma mensagem
    // na caixa de entrada
    public void newMessage(String message) throws IOException {
        // PREENCHER AQUI com código que envia a mensagem ao servidor
        buffer.clear();
        // buffer = charset.encode(message+"\n");
        sc.write(charset.encode(message+"\n"));
    }


    // Método principal do objecto
    public void run() throws IOException {
        // PREENCHER AQUI
        while(true){
          try{
            buffer.clear();
            sc.read(buffer);
            buffer.flip();

            if(buffer.limit() == 0)
              continue;

            // boolean ok = processInput( sc );
            String message = decoder.decode(buffer).toString();

            if(message.charAt(message.length()-1)!='\n')
              continue;

            String totalMessage[] = message.split(" ");
            if(totalMessage[0].equals("MESSAGE")){
              String newMessage = totalMessage[1]+": ";
              for(int i=2; i<totalMessage.length;i++){
                newMessage+=" "+totalMessage[i];
              }
              printMessage(newMessage);
            }
            else if(totalMessage[0].equals("LEFT")){
              String newTemp[] = totalMessage[1].split("\n");
              String newMessage = newTemp[0] + " saiu da sala\n";
              printMessage(newMessage);
            }
            else if(totalMessage[0].equals("JOINED")){
              String newTemp[] = totalMessage[1].split("\n");
              String newMessage = newTemp[0] + " entrou na sala\n";
              printMessage(newMessage);
            }
            else
              printMessage(message);
          } catch( IOException ie ){
            System.out.println("ERROR");
          }
        }

    }


    // Instancia o ChatClient e arranca-o invocando o seu método run()
    // * NÃO MODIFICAR *
    public static void main(String[] args) throws IOException {
        ChatClient client = new ChatClient(args[0], Integer.parseInt(args[1]));
        client.run();
    }

}
