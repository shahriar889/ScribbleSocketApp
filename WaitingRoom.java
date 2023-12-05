import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.Socket;

public class WaitingRoom extends JFrame {
    private boolean lock;
    private int color_order;
    private boolean[] seatsStatus; // To keep track of seat status (true for occupied, false for vacant)
    private int[] seats;
    private JButton[] seatButtons; // Array to hold the seat buttons
    private JButton start_button;
    private Color clientColor;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    public WaitingRoom() {
        lock = false;
        color_order = -1;
        seats = new int[]{0, 0, 0, 0};
        connectToServer();
        this.setTitle("Seat Reservation");
        this.setSize(1000, 700);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLayout(new GridLayout(1, 4));
        seatsStatus = new boolean[4]; // Initialize seat status array
        seatButtons = new JButton[4]; // Initialize seat button array

        // Create buttons for each seat position
        for (int i = 0; i < 4; i++) {
            // Panel to hold button and description label
            JPanel seatPanel = new JPanel(new BorderLayout());

            // Description label above the button
            JLabel descriptionLabel = new JLabel("Player " + (i + 1));
            descriptionLabel.setHorizontalAlignment(JLabel.CENTER);
            descriptionLabel.setFont(new Font("Arial", Font.BOLD, 22));
            if (i == 0) {
                descriptionLabel.setForeground(Color.RED);
            } else if (i == 1) {
                descriptionLabel.setForeground(Color.ORANGE);
            } else if (i == 2) {
                descriptionLabel.setForeground(Color.CYAN);
            } else if (i == 3) {
                descriptionLabel.setForeground(Color.GREEN);
            }
            seatPanel.add(descriptionLabel, BorderLayout.NORTH);

            // Button in the center
            JButton button = new JButton("Join");
            button.setFont(new Font("Arial", Font.BOLD, 30));
            button.setForeground(Color.BLACK); // Set font color (optional)
            button.setBorderPainted(false); // Remove the button border
            button.setFocusPainted(false);  // Remove the focus border
            button.setContentAreaFilled(false); // Set the content area transparent
            button.addActionListener(new SeatButtonListener(i, this)); // Add ActionListener to each button
            seatButtons[i] = button;

            seatPanel.add(button, BorderLayout.CENTER);

            this.add(seatPanel); // Add the seat panel to the JFrame
        }
        start_button = new JButton("Start");
        start_button.setFont(new Font("Arial", Font.BOLD, 30));
        start_button.setForeground(Color.BLACK); // Set font color (optional)
        start_button.setBorderPainted(false); // Remove the button border
        start_button.setFocusPainted(false);  // Remove the focus border
        start_button.setContentAreaFilled(false); // Set the content area transparent
        start_button.addActionListener(new SeatButtonListener(10, this));

        this.add(start_button, BorderLayout.CENTER);
        start_button.setEnabled(false);
        this.setVisible(true);
        listenToServer();
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (color_order == -1) {
                    //do nothing
                } else {
                    out.println("Cancel," + color_order);
                }
            }
        });
    }

    // ActionListener for seat buttons
    private class SeatButtonListener implements ActionListener {
        private int seatIndex;
        private WaitingRoom waitingRoom;

        public SeatButtonListener(int seatIndex, WaitingRoom waitingRoom) {
            this.seatIndex = seatIndex;
            this.waitingRoom = waitingRoom;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (seatIndex == 10) {
                closeConnection();
                waitingRoom.dispose();
                System.out.println("Start to create playbroad");
                ScribbleClient client = new ScribbleClient(clientColor);
                client.setVisible(true);
                client.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        client.closeConnection();
                    }
                });
            }
            for (int i = 0; i < seatButtons.length; i++) {
                if (i == seatIndex) {
                    // The clicked button becomes "Ready"
                    if (i == 0) {
                        lock = true;
                        out.println("Open," + i);
                        color_order = i;
                        seatButtons[i].setText("Ready");
                        seatButtons[i].setFont(new Font("Arial", Font.BOLD, 32));
                        seatButtons[i].setForeground(Color.RED);
                        clientColor = Color.RED;
                        seats[i] = 1;
                        start_button.setEnabled(true);
                    } else if (i == 1) {
                        lock = true;
                        out.println("Open," + i);
                        color_order = i;
                        seatButtons[i].setText("Ready");
                        seatButtons[i].setFont(new Font("Arial", Font.BOLD, 32));
                        seatButtons[i].setForeground(Color.ORANGE);
                        clientColor = Color.ORANGE;
                        seats[i] = 1;
                        start_button.setEnabled(true);
                    } else if (i == 2) {
                        lock = true;
                        out.println("Open," + i);
                        color_order = i;
                        seatButtons[i].setText("Ready");
                        seatButtons[i].setFont(new Font("Arial", Font.BOLD, 32));
                        seatButtons[i].setForeground(Color.CYAN);
                        clientColor = Color.CYAN;
                        seats[i] = 1;
                        start_button.setEnabled(true);
                    } else if (i == 3) {
                        lock = true;
                        out.println("Open," + i);
                        color_order = i;
                        seatButtons[i].setText("Ready");
                        seatButtons[i].setFont(new Font("Arial", Font.BOLD, 32));
                        seatButtons[i].setForeground(Color.GREEN);
                        clientColor = Color.GREEN;
                        seats[i] = 1;
                        start_button.setEnabled(true);
                    }
                } else {
                    // Disable other buttons
                    seatButtons[i].setEnabled(false);
                }
            }

        }
    }

    private void listenToServer() {
        Thread serverListener = new Thread(new WaitingRoom.ServerListener());
        serverListener.start();
    }

    private class ServerListener implements Runnable {
        @Override
        public void run() {
            try {
                String inputLine;
                while (true) {
                    while ((inputLine = in.readLine()) != null) {
                        System.out.println(inputLine);
                        if (inputLine.startsWith("Seat")) {
                            String[] parts = inputLine.split(",");
                            seats[0] = Integer.parseInt(parts[1]);
                            seats[1] = Integer.parseInt(parts[2]);
                            seats[2] = Integer.parseInt(parts[3]);
                            seats[3] = Integer.parseInt(parts[4]);

                            if (seats[0] == 1) {
                                seatButtons[0].setEnabled(false);
                                seatButtons[0].setText("Ready");
                                seatButtons[0].setFont(new Font("Arial", Font.BOLD, 32));
                                seatButtons[0].setForeground(Color.RED);
                            } else {
                                seatButtons[0].setEnabled(true);
                                seatButtons[0].setText("Join");
                                seatButtons[0].setFont(new Font("Arial", Font.BOLD, 30));
                                seatButtons[0].setForeground(Color.BLACK);
                            }

                            if (seats[1] == 1) {
                                seatButtons[1].setEnabled(false);
                                seatButtons[1].setText("Ready");
                                seatButtons[1].setFont(new Font("Arial", Font.BOLD, 32));
                                seatButtons[1].setForeground(Color.ORANGE);
                            } else {
                                seatButtons[1].setEnabled(true);
                                seatButtons[1].setText("Join");
                                seatButtons[1].setFont(new Font("Arial", Font.BOLD, 30));
                                seatButtons[1].setForeground(Color.BLACK);
                            }

                            if (seats[2] == 1) {
                                seatButtons[2].setEnabled(false);
                                seatButtons[2].setText("Ready");
                                seatButtons[2].setFont(new Font("Arial", Font.BOLD, 32));
                                seatButtons[2].setForeground(Color.CYAN);
                            } else {
                                seatButtons[2].setEnabled(true);
                                seatButtons[2].setText("Join");
                                seatButtons[2].setFont(new Font("Arial", Font.BOLD, 30));
                                seatButtons[2].setForeground(Color.BLACK);
                            }

                            if (seats[3] == 1) {
                                seatButtons[3].setEnabled(false);
                                seatButtons[3].setText("Ready");
                                seatButtons[3].setFont(new Font("Arial", Font.BOLD, 32));
                                seatButtons[3].setForeground(Color.GREEN);
                            } else {
                                seatButtons[3].setEnabled(true);
                                seatButtons[3].setText("Join");
                                seatButtons[3].setFont(new Font("Arial", Font.BOLD, 30));
                                seatButtons[3].setForeground(Color.BLACK);
                            }
                            if (lock == true)
                            {
                                seatButtons[0].setEnabled(false);
                                seatButtons[1].setEnabled(false);
                                seatButtons[2].setEnabled(false);
                                seatButtons[3].setEnabled(false);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private int connectToServer() {
        int id2 = 0;
        try {
            socket = new Socket("127.0.0.1", 8200);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            OutputStream outputStream = socket.getOutputStream();
            PrintWriter out = new PrintWriter(outputStream, true);

            String message = "WaitingRoom";
            out.println(message);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return id2;
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
//    public static void main(String[] args) {
//        SwingUtilities.invokeLater(() -> new WaitingRoom());
//    }
}