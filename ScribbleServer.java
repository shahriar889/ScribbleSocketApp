import java.awt.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.*;

public class ScribbleServer {
    private static final int PORT = 8200;
    private int boxFilled; // variable to store the number of boxes filled
    private int[] seatOccupied = {0, 0, 0, 0};
    private int numClients; // variable to store the number of clients
    private int numWaiting;
    private List<Socket> clients = new ArrayList<>(); // Arraylist to store the client sockets
    private List<Socket> waitingRoom = new ArrayList<>();
    private int[] drawingSocket = new int[64]; //Arraylist to handle locking of a box using player id
    private List<List<Point>> scribbledPixels; //Arraylist to store the scribble pixels list for each box
    private Map<Socket, Integer> clientIdList = new HashMap<>(); //Map to store Clients and id together
    private Map<Socket, List<Integer>> listWin = new HashMap<>(); //Map to store number of boxes filled by each client

    /*Constructor for the Server
    Initializes the scribbledPixels arraylist & and sets all the variables for counting to 0
    * */
    public ScribbleServer() {
        this.scribbledPixels = new ArrayList<>();
        this.numClients = 0;
        this.numWaiting = 0;
        this.boxFilled = 0;
        //For each box open the box for drawing and initialize arraylist to store scribbled pixels
        for (int i = 0; i < 64; i++) {
            scribbledPixels.add(new ArrayList<>());
            drawingSocket[i] = 0;
        }
    }

    public void start() {
        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("Server is running and listening on Port " + PORT);
            //Server will run until terminated keep on accepting clients and start appropriate threads
            while (true) {
                System.out.println();
                Socket clientSocket = serverSocket.accept();

                InputStream inputStream = clientSocket.getInputStream();
                BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
                // check whether the client is a playbroad or a waiting room
                String message = in.readLine();
                if (message.equals("WaitingRoom")) {
                    this.numWaiting++;
                    // add this client to the list of waiting room
                    waitingRoom.add(clientSocket);
                    Thread waitingHandler = new Thread(new WaitingHandler(clientSocket));
                    waitingHandler.start();
                }
                //Connected by clicking start in the waiting room to launch client side of game
                else {
                    System.out.println(message);
                    // get the color from the client and assign the color to this client.
                    inputStream = new ObjectInputStream(clientSocket.getInputStream());
                    Color color = (Color) ((ObjectInputStream) inputStream).readObject();


                    this.numClients++; // Increases the number of clients (Initial value was 0, so the first client will increase to 1)
                    clients.add(clientSocket); // Add clientSocket to client list
                    listWin.put(clientSocket, new ArrayList<>());
                    clientIdList.put(clientSocket, numClients); // Put client with id which is the number of clients
                    System.out.println("New client connected: " + clientSocket.getInetAddress().getHostAddress() + " " + numClients);
                    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                    /*Send the id of client assigned by server back to the client as a string message
                    the string is tokenized by staring with "COLOR"
                    * */
                    out.println("COLOR," + numClients);
                    //Create a thread for the client and start the thread
                    Thread clientHandler = new Thread(new ClientHandler(clientSocket, color));
                    clientHandler.start();
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }


    /* Broadcasts the scribbled pixels to all the clients
    List of pixels is converted and tokenized to string by serializedScribbledData function
    The message to broadcasted is tokenized by starting with "SCRIBBLE_DATA" and separated using "!"
    The message includes the index of the box, list of pixels to be drawn and the color to be used
    * */
    private void broadcastScribbleData(int boxId, List<Point> pixels, Color color) {
        String message = "SCRIBBLE_DATA!" + boxId + "!" + serializeScribbleData(pixels) + "!" + color.getRGB();
        for (Socket client : clients) {
            try {
                PrintWriter out = new PrintWriter(client.getOutputStream(), true);
                out.println(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /* This Function uses string builder class to convert int value from
     the points to string and separates the points using "," & coordinate using ";"
    * */
    private String serializeScribbleData(List<Point> pixels) {
        StringBuilder sb = new StringBuilder();
        for (Point pixel : pixels) {
            sb.append(pixel.x).append(",").append(pixel.y).append(";");
        }
        return sb.toString();
    }
    //Broadcasts the tokenized message to all clients
    private void broadcastDrawing(String message) {
        for (Socket client : clients) {
            try {
                PrintWriter out = new PrintWriter(client.getOutputStream(), true);
                out.println(message);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
    /* Broadcasts the fill message as a string to all the clients
   The message to broadcasted is tokenized by starting with "BOX_FILLED" and separated using ","
   The message includes the box index, if the box is filled and color to be filled with if filled is true
   * */
    private void broadcastFillMessage(int boxId, boolean isFilled, Color color) {
        String message = "BOX_FILLED," + boxId + "," + isFilled + "," + color.getRGB();
        for (Socket client : clients) {

            try {
                PrintWriter out = new PrintWriter(client.getOutputStream(), true);
                out.println(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    // broadcast a message to all waiting room in the waiting room list
    private void broadcastWaiting(String message) {
        System.out.println("broadcast");
        for (Socket client : waitingRoom) {
            try {
                PrintWriter out = new PrintWriter(client.getOutputStream(), true);
                out.println(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // create a thread to handle the message from waiting room and send the message to the waiting room
    private class WaitingHandler implements Runnable {
        private Socket clientSocket;
        private BufferedReader in;
        private PrintWriter out;
        private int id;
        private Color clientColor;
        // initialize the handler and create the output and input stream
        public WaitingHandler(Socket WaitingSocket) {
            this.clientSocket = WaitingSocket;
            this.clientColor = Color.WHITE;
            try {
                this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                this.out = new PrintWriter(clientSocket.getOutputStream(), true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            System.out.println("waiting room in run");
            String message =  "Seat,"+seatOccupied[0]+","+seatOccupied[1]+","+seatOccupied[2]+","+seatOccupied[3];
            // tell all waiting rooms the conditions of these seats.
            broadcastWaiting(message);
            try {
                String inputLine;
                while (true) {
                    System.out.println("receiving");
                    inputLine = in.readLine();
                    System.out.println("content"+ inputLine);
                    System.out.println("receiving end");
                    if (inputLine != null) {
                        // tell all waiting room one of the seats is occupied now
                        if(inputLine.startsWith("Open")) {
                            String [] parts = inputLine.split(",");
                            seatOccupied[Integer.parseInt(parts[1])] = 1;
                            message = "Seat," + seatOccupied[0] + "," + seatOccupied[1] + "," + seatOccupied[2] + "," + seatOccupied[3];
                            broadcastWaiting(message);
                        }else if(inputLine.startsWith("Cancel"))// tell all waiting room one of the seats is empty now
                        {
                            String [] parts = inputLine.split(",");
                            seatOccupied[Integer.parseInt(parts[1])] = 0;
                            message = "Seat," + seatOccupied[0] + "," + seatOccupied[1] + "," + seatOccupied[2] + "," + seatOccupied[3];
                            broadcastWaiting(message);
                        }
                    } else {
                        break;
                    }
                }
                in.close();
                out.close();
                clientSocket.close();
                numWaiting--;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class ClientHandler implements Runnable {
        private Socket clientSocket;
        private BufferedReader in;
        private PrintWriter out;
        private Color clientColor;

        public ClientHandler(Socket clientSocket, Color clientColor) {
            this.clientSocket = clientSocket;
            this.clientColor = clientColor;
            try {
                this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                this.out = new PrintWriter(clientSocket.getOutputStream(), true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            System.out.println("in run");
            //Server listens for tokenized message from clients
            try {
                String inputLine;
                while (true) {
                    inputLine = in.readLine();
                    System.out.println(inputLine);
                    if (inputLine != null) {
                        if (inputLine.startsWith("DRAW_BOX")) {
                            //If input line is tokenized by "DRAW_BOX", therefore action is mouse clicked on a box
                            System.out.println("in draw box");
                            String[] parts = inputLine.split(",");// separate the string using "," and store in array
                            //get index of the box and id of the client from message
                            int boxId = Integer.parseInt(parts[1]);
                            int id = Integer.parseInt(parts[2]);
                            //check if drawing box is open
                            if (drawingSocket[boxId] == 0) {
                                //if open then lock the box with the client's id
                                drawingSocket[boxId] = id;
                                //broadcast to all clients that box is locked for the client, and the message is tokenized
                                //by "DRAW_BOX" and the information is separated using ","
                                String message = "DRAW_BOX," + boxId + "," + id;
                                broadcastDrawing(message);
                            }
                        } else if (inputLine.startsWith("DRAWING_BOX_COMPLETED")) {
                            //If input line is tokenized by "DRAWING_BOX_COMPLETED", therefore action is mouse released on a box
                            System.out.println("In draw box comp");
                            String[] parts = inputLine.split(","); // separate the string using "," and store in array
                            //get index of the box and id of the client from message
                            int boxId = Integer.parseInt(parts[1]);
                            int id = Integer.parseInt(parts[2]);
                            //check if the box was locked by this client
                            if (drawingSocket[boxId] == id){
                                //call isFilledEnough function to check if the client scribbled appropriate amount of pixels
                                if (isFilledEnough(boxId)) {
                                    //if appropriate amount is scribbled then box is filled by client
                                    System.out.println("In Fill");
                                    System.out.println(clientColor);
                                    listWin.get(clientSocket).add(boxId); // find the appropriate arraylist of int associated with the client
                                    // and add the index of the box to the arraylist
                                    boxFilled++;//increase the number of box filled
                                    if (boxFilled >= 64) {
                                        //if all boxes are filled, then find winner and broadcast to all clients
                                        Socket winner = findWinner();
                                        broadCastWinner(winner);
                                    }
                                    //broadcast the fill message to all clients using isFilled as true
                                    broadcastFillMessage(boxId, true, clientColor);
                                } else {
                                    //box is not filled
                                    System.out.println("In clear");
                                    scribbledPixels.get(boxId).clear();// clear scribbled pixels for that box
                                    drawingSocket[boxId] = 0;// open the box back for drawing
                                    //broadcast the fill message to all clients using isFilled as false
                                    broadcastFillMessage(boxId, false, clientColor);

                                }
                            }
                        } else if (inputLine.startsWith("PIXEL_DATA")) {
                            System.out.println("Scribbling");
                            // separate the string using "," and store in array
                            //get index of the box, coordinates for pixels from the array
                            String[] parts = inputLine.split(",");
                            int boxId = Integer.parseInt(parts[1]);
                            int x = Integer.parseInt(parts[2]);
                            int y = Integer.parseInt(parts[3]);
                            //add the coordinates for scribbling to that box in the list
                            Point newPixel = new Point(x, y);
                            scribbledPixels.get(boxId).add(newPixel);
                            //broadcast scribbling to all clients
                            broadcastScribbleData(boxId, scribbledPixels.get(boxId), clientColor);
                        }

                    } else {
                        break;
                    }
                }

                in.close();
                out.close();
                clientSocket.close();
                numClients--;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /*This function broadcasts the winner to all the clients
    The message is tokenized using "GAME_OVER", and the info is separated using ","
    * */
    private void broadCastWinner(Socket winner) {
        for (Socket client : clients) {
            try {
                PrintWriter out = new PrintWriter(client.getOutputStream(), true);
                out.println("GAME_OVER," + winner.getInetAddress().getHostName() + "," + clientIdList.get(winner));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /*
    This function uses the listwin map to get the arraylist of filled boxes index, then compares the size of the array
    with each client to find the maximum number of box filled
    */
    private Socket findWinner() {
        Socket winner = clients.get(0);
        int testSize = 0;
        for (Socket test : listWin.keySet()) {
            List<Integer> box = listWin.get(test);
            if (box.size() > testSize) {
                winner = test;
                testSize = box.size();
            }
        }
        return winner;
    }

    /*
    This function sets the total pixels number to 750 since a box height and width of 5 is colored at a time
    Then finds total number pixels colored from scribbled pixels list and checks if at least half of the box
     is filled
     */
    private boolean isFilledEnough(int boxId) {
        int totalPixels = 750;
        int scribbledPixelCount = scribbledPixels.get(boxId).size();
        System.out.println(scribbledPixelCount);
        boolean ans = scribbledPixelCount / (double) totalPixels >= 0.5;
        System.out.println(ans);
        return ans;
    }

    public static void main(String[] args) {
        ScribbleServer server = new ScribbleServer();
        server.start();
    }
}
