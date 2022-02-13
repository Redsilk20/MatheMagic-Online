import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.util.Date;
import java.util.Scanner;

//Desc:The server function will receive the commands from the client and return information based on the response
//Pre: The server client must start first, and the client must login
//Post: The server will respond to the commands from the client and save the clients information for the SOLVE command
//for every time its used.
//The client can send the commands such as,
//LOGIN,This is the command a client must initiate in order to gain access to anything on the server
//SOLVE,This command will solve the area and perimeter for rectangles or circles
//LIST, This command returns a list of all the solutions requested by this particular user, or if its root -all reutrns all users soltuions
//SHUTDOWN, This command will shutdown the server
//LOGOUT, This command will terminate the current client that is logged in
//The server will continue to receive requests through the socket, acts on those requests, and returns the results to the requester.
public class Server {
    private static final int SERVER_PORT = 8765;

    public static void main(String[] args) {
        createCommunicationLoop();
    }//end main

    public static void createCommunicationLoop() {
        try {
            //create server socket
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);

            System.out.println("Server started at " + new Date() + "\n");
            Socket socket = serverSocket.accept();

            DataInputStream inputFromClient = new DataInputStream(socket.getInputStream());
            DataOutputStream outputToClient = new DataOutputStream(socket.getOutputStream());

            while (true) {
                //First input
                System.out.println("waiting for input..");
                String firstCommand = inputFromClient.readUTF();
                System.out.println(firstCommand);
                String[] tokenFirst = firstCommand.split(" ");

                File loginFile = new File("logins.txt");
                String username = null, password = null;

                //LOGIN
                boolean loggedIn = false;
                if (tokenFirst[0].equalsIgnoreCase("LOGIN") && tokenFirst.length == 3 && !loggedIn) {
                    //Validate login
                    try (Scanner scan = new Scanner(loginFile)) {
                        while (scan.hasNext()) {
                            username = scan.next();
                            password = scan.next();
                            if (tokenFirst[1].equals(username) && tokenFirst[2].equals(password)) {
                                loggedIn = true;
                                System.out.println("SUCCESS");
                                outputToClient.writeUTF("SUCCESS");
                                break;
                            }
                        }
                    }
                    //Login Accepted
                    if (loggedIn) {
                        while (true) {
                            //Taking input
                            System.out.println("waiting for input..");
                            String secondCommand = inputFromClient.readUTF();
                            System.out.println(secondCommand);
                            String[] tokenSecond = secondCommand.split(" ");

                            //SOLVE
                            try {
                                if (tokenSecond[0].equals("SOLVE") && tokenSecond.length > 1) {
                                    if (tokenSecond.length == 3 && (tokenSecond[1].equals("-c") || tokenSecond[1].equals("-r")))
                                        outputToClient.writeUTF(solve(username, tokenSecond[1], Integer.parseInt(tokenSecond[2]), Integer.parseInt(tokenSecond[2])));
                                    else if (tokenSecond.length == 4 && (tokenSecond[1].equals("-c") || tokenSecond[1].equals("-r")))
                                        outputToClient.writeUTF(solve(username, tokenSecond[1], Integer.parseInt(tokenSecond[2]), Integer.parseInt(tokenSecond[3])));
                                    else
                                        outputToClient.writeUTF("Incorrect command, try again!");
                                }
                            } catch (NumberFormatException e) {
                                outputToClient.writeUTF("Invalid entry!");
                            }
                            //LIST
                            if (tokenSecond[0].equals("LIST")) {
                                boolean root = tokenFirst[1].equals("root");
                                if (tokenSecond.length == 1)
                                    list(username, outputToClient, false);
                                else if (tokenSecond.length == 2 && tokenSecond[1].equals("-all")) {
                                    if (root)
                                        list(username, outputToClient, true);
                                    else
                                        outputToClient.writeUTF("Error: you are not the root user");
                                }
                            }
                            //SHUTDOWN
                            else if (tokenSecond[0].equals("SHUTDOWN")) {
                                System.out.println("Shutting down server...");
                                outputToClient.writeUTF("200 OK");
                                serverSocket.close();
                                socket.close();
                                return;
                            }
                            //LOGOUT
                            else if (tokenSecond[0].equals("LOGOUT")) {
                                outputToClient.writeUTF("200 OK");
                                loggedIn = false;
                                break;
                            }
                        }
                    } else
                        outputToClient.writeUTF("FAILURE: Please provide correct username and password. Try again.\n");
                }
                //SHUTDOWN
                else if (tokenFirst[0].equalsIgnoreCase("SHUTDOWN")) {
                    System.out.println("Shutting down server...");
                    outputToClient.writeUTF("200 OK");
                    serverSocket.close();
                    socket.close();
                    break;  //get out of loop
                }
                //NOT LOGGED IN YET
                else if (tokenFirst[0].equalsIgnoreCase("LOGOUT") || tokenFirst[0].equalsIgnoreCase("SOLVE") ||
                        tokenFirst[0].equalsIgnoreCase("LIST")) {
                    outputToClient.writeUTF("301 message format error");//FIX OUTPUT MESSAGE
                }
                //NOT RECOGNIZED
                else
                    outputToClient.writeUTF("300 invalid command”");//FIX OUTPUT MESSAGE
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    //SOLVE METHOD
    public static String solve(String user, String shape, Integer x, Integer y) throws IOException {
        String message = "";
        PrintWriter result = new PrintWriter(new FileWriter(user + "_solutions.txt", true));
        if (shape.equals("-c")) {
            double circumference = 2 * x * 3.14;
            double area = 3.14 * x * x;
            message = "radius " + x + ": The circle's circumference is " + String.format("%.2f", circumference) + " and " +
                    "the area is " + String.format("%.2f", area);
            result.println(message);
        } else if (shape.equals("-r")) {
            double perimeter = 2 * (x + y), rarea = x * y;
            message = "sides " + x + " " + y + ": Rectangle’s perimeter is " + String.format("%.2f", perimeter) + " and area is " + String.format("%.2f", rarea);
            result.println(message);
        }
        result.close();
        return message;
    }

    //LIST METHOD
    public static void list(String currentUser, DataOutputStream output, boolean all) {
        if (all) {
            try (Scanner logins = new Scanner(new File("logins.txt"))) {
                output.writeUTF("file");
                while (logins.hasNext()) {
                    String user = logins.next();
                    String dummy = logins.next();
                    File userFile = new File(user + "_solutions.txt");
                    if (userFile.exists()) {
                        try (Scanner userData = new Scanner(userFile).useDelimiter("\\Z")) {
                            output.writeUTF(user + "\n" + userData.next());
                        } catch (EOFException e) {
                            System.out.println("File Done");
                        }
                    } else {
                        output.writeUTF(user + "\nNo interactions yet");
                    }
                }
            } catch (IOException e) {
                System.out.println("Done reading");
            }
        } else {
            try (Scanner userFile = new Scanner(new File(currentUser + "_solutions.txt")).useDelimiter("\\Z")) {
                output.writeUTF(currentUser + "\n" + userFile.next());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}