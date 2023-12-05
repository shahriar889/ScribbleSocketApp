import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import javax.swing.border.Border;
import java.lang.*;

public class ScribbleClient extends JFrame{
    private final int BOX_WIDTH = 100;
    private final int BOX_HEIGHT = 100;
    private int id;
    private JPanel[] drawingPanels; //Array of boxes to draw
    private int [] drawingSocket= new int [64]; //Array to control locking of box using client ids
    private boolean[] isBoxFilled = new boolean[64]; //Boolean array to see if box is filled
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private List<List<Point>> scribbledPixels; //Arraylist to store the scribble pixels list for each box
    private Color clientColor;
    private long lastMessageTime;

    //Constructor
    public ScribbleClient(Color clientColor) {
        this.clientColor = clientColor;
        //get id by connecting to the server
        this.id = connectToServer();
        System.out.println("after connect to server: "+this.id);
        System.out.println("HOSt conec");
        setTitle("Scribble Client");
        System.out.println("In middle");
        setSize(BOX_WIDTH*8,BOX_HEIGHT*8);
        setResizable(false);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        scribbledPixels = new ArrayList<>();
        //For each box initialize new scribbled pixels list, open all the box and set box filled to false
        for(int i = 0; i < 64;i++){
            scribbledPixels.add(new ArrayList<>());
            drawingSocket[i] = 0;
            isBoxFilled[i] = false;
        }
        Container contentPane = getContentPane();
        contentPane.setLayout(new GridLayout(8,8)); //Show 64 drawing boxes in a 8x8 grid
        this.drawingPanels = new JPanel[64];//Initialize 64 drawing boxes

        //For each drawing box set mouse actions
        for(int i = 0; i < 64; i++){
            final int boxId = i; //boxId and index of the box in drawingPanels arraylist is same
            System.out.println(boxId);
            this.drawingPanels[i] =  new JPanel(); //create box to draw
            Border border = BorderFactory.createLineBorder(Color.BLACK, 2);
            drawingPanels[i].setBorder(border);
            drawingPanels[i].setBackground(Color.WHITE);

            drawingPanels[i].addMouseListener(new MouseAdapter() {
                //If mouse is pressed, check if box is not filled and box is open for drawing
                //If yes, send a string message, tokenized by "DRAW_BOX", and the info is separated by ","
                @Override
                public void mousePressed(MouseEvent e){
                    System.out.println("pressed");
                    if(drawingSocket[boxId] == 0 && !isBoxFilled[boxId]){
                        out.println("DRAW_BOX,"+boxId+","+id);
                    }
                }

                //If mouse is released, check if box is not filled and was the box locked by this client
                //If yes, send a string message, tokenized by "DRAWING_BOX_COMPLETED", and the info is separated by ","
                @Override
                public void mouseReleased(MouseEvent e){

                    System.out.println("REleased");
                    if(!isBoxFilled[boxId] && drawingSocket[boxId] == id)
                        out.println("DRAWING_BOX_COMPLETED,"+boxId+ "," + id);

                }
            });
            //If mouse is being dragged, check if box is not filled and box is open for drawing
            //If yes, send a string message, tokenized by "PIXEL_DATA", and the info is separated by ","
            drawingPanels[i].addMouseMotionListener(new MouseAdapter() {
                @Override
                public void mouseDragged(MouseEvent e){
                    long currentTime = System.currentTimeMillis();
                    if(currentTime > lastMessageTime){
                        if( !isBoxFilled[boxId] && drawingSocket[boxId] == id){
                            out.println("PIXEL_DATA,"+boxId+","+e.getX()+","+e.getY());
                            lastMessageTime = currentTime;
                        }

                    }
                }
            });
            contentPane.add(drawingPanels[i]);
        }
        //After setting mouse action listeners, client is set to listen from server to handle
        // appropriate real time scribbling
        listenToServer();
    }
    //Connect to server and receive id
    private int connectToServer(){
        int id2 = 0;
        try{
            socket = new Socket("127.0.0.1", 8200);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.println("client");
            ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
            outputStream.writeObject(clientColor);


            System.out.println("Before color");
            String inputLine = in.readLine();
            if(inputLine.startsWith("COLOR")){
                //if input string from server starts with "COLOR"
                System.out.println("In if");
                String [] parts = inputLine.split(","); //Split the input line using ","
                int id = Integer.parseInt(parts[1]); //parse the id into int
                System.out.println("Connect to server:"+parts[1]);
                id2 = id;
                System.out.println(clientColor);
            }

        }
        catch (IOException e) {
            e.printStackTrace();
        }
        //return client id
        return id2;
    }

    //Creates a new thread to listen from server
    private void listenToServer(){
        Thread serverListener = new Thread(new ServerListener());
        serverListener.start();
    }
    // Scribble in appropriate box using boxId, String of coordinates of pixels separated using ";" and the color
    private void handleScribbleData(int boxId, String serializedData, Color color){

        Graphics g = drawingPanels[boxId].getGraphics();
        g.setColor(color); //Set the color to the correct clients color
        String[] pixelStrings = serializedData.split(";"); // Each coordinate is separated using ";"
        List<Point> pixels = new ArrayList<>(); // Create new Point Arraylist to store scribbling info
        for (String pixelString: pixelStrings){
            String[] coords = pixelString.split(",");  // Each point is separated using ","
            int x = Integer.parseInt(coords[0]);
            int y = Integer.parseInt(coords[1]);
            pixels.add(new Point(x,y)); // add point pixels arraylist
            g.fillRect(x,y,5,5); // color a box from that coordinate with height and width 5

        }
        scribbledPixels.set(boxId,pixels); // set the list of scribbled pixels for that box

    }
    //Function to handle mouse pressed and locking of box
    private void handleDrawBox(int boxId, int id){
        //checks if box is open and not filled, if yes lock the box using the id
        if(!isBoxFilled[boxId] && drawingSocket[boxId] == 0) {
            drawingSocket[boxId] = id;
        }
    }
    void closeConnection(){
        try {

            if(out != null)
                out.close();
            if(in != null)
                in.close();
            if(socket != null)
                socket.close();
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    //Thread for listening to server until client is terminated
    private class ServerListener implements Runnable{
        @Override
        public void run(){
            try{
                String inputLine;
                while(true){
                    while ((inputLine = in.readLine()) != null){
                        if(inputLine.startsWith("DRAW_BOX")){
                            //if input line is tokenized by "DRAW_BOX"
                            String[] parts = inputLine.split(","); //Split the input string using ","
                            int boxId = Integer.parseInt(parts[1]); // get index of the bpx
                            int id = Integer.parseInt(parts[2]); //get id of the client responsible for issuing the lock
                            handleDrawBox(boxId, id); // call function handle locking of box
                        }
                        else if(inputLine.startsWith("SCRIBBLE_DATA")){
                            //if input line is tokenized by "SCRIBBLE_DATA"
                            String[] parts = inputLine.split("!"); //separate input line using "!"
                            int boxId = Integer.parseInt(parts[1]); //get index of box
                            String serializedData = parts[2]; // String variable containing all the pixels to be scribbled
                            int  RGB = Integer.parseInt(parts[3]); // get the color of the client responsible for scribbling
                            handleScribbleData(boxId, serializedData, new Color(RGB)); //call function to handle scribbling
                        }
                        else if(inputLine.startsWith("BOX_FILLED")){
                            //if input line is tokenized by "BOX_FILLED"
                            System.out.println("In Fill client");
                            String [] parts = inputLine.split(","); //separate the input line using ","
                            int boxId = Integer.parseInt(parts[1]);// get index of the box
                            boolean isFilled = Boolean.parseBoolean(parts[2]); //boolean value to check if box is filled
                            int RGB = Integer.parseInt(parts[3]); // get the color of the client responsible for mouse released
                            if(isFilled){
                                //if box is filled
                                System.out.println("In Fill client true");
                                System.out.println(new Color(RGB));
                                isBoxFilled[boxId] = true; // set the box filled boolean for that box to true
                                drawingPanels[boxId].setBackground(new Color(RGB)); // Fill the box with the client's color

                            }
                            else {
                                //if not filled
                                drawingPanels[boxId].setBackground(Color.WHITE);
                                List<Point> pix = scribbledPixels.get(boxId);
                                Graphics g = drawingPanels[boxId].getGraphics();
                                //clear scribbling
                                for(Point pixx:pix){
                                    g.setColor(Color.WHITE);
                                    g.fillOval(pixx.x,pixx.y,5,5);
                                }
                                g.dispose();
                                Border border = BorderFactory.createLineBorder(Color.BLACK, 2);
                                drawingPanels[boxId].setBorder(border);

                                pix.clear();//clear the arraylist of points from the box
                                drawingSocket[boxId] = 0; //open the box for drawing
                            }
                        }
                        else if(inputLine.startsWith("GAME_OVER")){
                            //If input line is tokenized by "GAME_OVER"
                            String [] parts = inputLine.split(","); //separate the string using ","
                            System.out.println("Game over, winner is "+parts[1]+" with ID: "+ parts[2]);
                            //Display winner's name and id
                            JOptionPane.showMessageDialog(null, "Game over, winner is "+parts[1]+" with ID: "+ parts[2], "THE WINNER IS", JOptionPane.PLAIN_MESSAGE);
                        }
                    }
                }
            }
            catch (IOException e){
                e.printStackTrace();
            }
        }
    }
    public static void main(String[] args){
        SwingUtilities.invokeLater(()->{
            StartMenu startMenu = new StartMenu();
        });
    }
}
