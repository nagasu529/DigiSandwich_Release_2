

import javax.swing.*;
import java.awt.*;
public class mainGui{

    private JFrame frame;

    public mainGui(){
        //Adding Main Frame (Gui)
        frame = new JFrame("Multi-Agent monitoring");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel1 = new JPanel();

        JButton button = new JButton("run");
        panel1.add(button);
        frame.add(panel1);
    }

    public void show(){
        frame.setSize(700, 400);
        frame.setVisible(true);
    }

    public static void main(String[] args){
        mainGui myGui = new mainGui();
        myGui.show();
    }
}
