import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

//Desc: The client function will hold the port number, as well as send and receive information from and to the server and handle any execptions that occur
//Pre:  The server start first, then the client would connect and the client would send a command to the server
//Post: The client would receive the response from the server and allow the client to couniute sending commands as long as the client wants
public class Client {
    private static final int SERVER_PORT = 8765;

    public static void main(String[] args) {
        DataOutputStream toServer;
        DataInputStream fromServer;
        Scanner input = new Scanner(System.in);
        String message;

        try {
            Socket socket = new Socket("localhost", SERVER_PORT);

            fromServer = new DataInputStream(socket.getInputStream());
            toServer = new DataOutputStream(socket.getOutputStream());

            while (true) {
                System.out.print("C: ");
                //sending message
                message = input.nextLine();
                toServer.writeUTF(message);

                if (message.equalsIgnoreCase("SHUTDOWN")) {
                    System.out.print("S: ");
                    System.out.println(fromServer.readUTF());
                    break;
                }
                //receiving message:
                System.out.print("S: ");
                message = fromServer.readUTF();
                if (message.equals("file")) {
                    for (int i = 0; i < 4; i++)
                        System.out.println(fromServer.readUTF());
                    continue;
                }
                System.out.println(message);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}