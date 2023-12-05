import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class StartMenu extends JFrame {

    private int start;

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public StartMenu() {

        this.setPreferredSize(new Dimension(1000, 700));
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel backgroundPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                String imagePath = "/Users/shahriar/Desktop/cmpt272/ASN/NCTApp/src/assets/Images/NCT.png";
                try {
                    // 读取图片
                    BufferedImage image = ImageIO.read(new File(imagePath));
                    g.drawImage(image, 0, 0, getWidth(), getHeight(), this);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        backgroundPanel.setLayout(new FlowLayout());
        this.getContentPane().add(backgroundPanel);

        // 创建按钮
        JButton jlStart = new JButton("START");
        jlStart.setPreferredSize(new Dimension(400, 100));
        jlStart.setFont(new Font("Arial", Font.BOLD, 30)); // Set the font and size
        jlStart.setForeground(Color.BLACK); // Set font color (optional)
        jlStart.setBorderPainted(false); // Remove the button border
        jlStart.setFocusPainted(false);  // Remove the focus border
        jlStart.setContentAreaFilled(false); // Set the content area transparent
        backgroundPanel.add(jlStart);

        JButton jlExit = new JButton("EXIT");
        jlExit.setPreferredSize(new Dimension(400, 100));
        jlExit.setFont(new Font("Arial", Font.BOLD, 30)); // Set the font and size
        jlExit.setForeground(Color.BLACK); // Set font color (optional)
        jlExit.setBorderPainted(false); // Remove the button border
        jlExit.setFocusPainted(false);  // Remove the focus border
        jlExit.setContentAreaFilled(false); // Set the content area transparent
        backgroundPanel.add(jlExit);
        jlStart.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                jlStart.setFont(new Font("Arial", Font.BOLD, 35)); // Set the font and size
                jlStart.setForeground(Color.RED); // Change text color to red on hover
            }

            @Override
            public void mouseExited(MouseEvent e) {
                jlStart.setFont(new Font("Arial", Font.BOLD, 30)); // Set the font and size
                jlStart.setForeground(Color.BLACK); // Revert text color to black on exit
            }
        });

        jlExit.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                jlExit.setFont(new Font("Arial", Font.BOLD, 35)); // Set the font and size
                jlExit.setForeground(Color.RED); // Change text color to red on hover
            }

            @Override
            public void mouseExited(MouseEvent e) {
                jlExit.setFont(new Font("Arial", Font.BOLD, 30)); // Set the font and size
                jlExit.setForeground(Color.BLACK); // Revert text color to black on exit
            }
        });
        jlStart.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                StartMenu.this.setStart(1);
                closeThis(); // Close the StartMenu JFrame
                openWaitingRoom(); // Open the WaitingRoom JFrame
            }
        });

        jlExit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                StartMenu.this.setStart(2);
                closeThis(); // Close the StartMenu JFrame
            }
        });

        this.pack();
        this.setVisible(true);
    }

    /**
     * Closes the menu
     */
    public void closeThis() {
        this.dispose(); // Close the StartMenu JFrame
    }

    /**
     * Opens the waiting room
     */
    public void openWaitingRoom() {
        WaitingRoom waitingRoom = new WaitingRoom(); // Open the WaitingRoom JFrame
    }

    public static void main(String[] args) {
        StartMenu startMenu = new StartMenu();
    }

}