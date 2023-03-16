

import javax.swing.*;
import java.awt.*;
import java.util.function.ObjDoubleConsumer;

public class mainGui extends JFrame{

    private JFrame frame;
    //GUI design preferences
    private JTextArea log;

    public mainGui(){
        //Adding Main Frame (Gui)
        frame = new JFrame("Multi-Agent monitoring");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel1 = new JPanel();
        JButton button = new JButton("run");
        panel1.add(button);
        frame.add(panel1);

        //log area create

        log = new JTextArea(10,50);
        log.setEditable(false);
        getContentPane().add(log, BorderLayout.CENTER);
        log.setMargin(new Insets(5,5,100,100));
        JScrollPane logScrollPane = new JScrollPane(log);
        getContentPane().add(logScrollPane, BorderLayout.CENTER);
        panel1.add(log);

    }

    public void show(){
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int centerX = (int)screenSize.getWidth() / 2;
        int centerY = (int)screenSize.getHeight() / 2;
        setLocation(centerX - getWidth() / 2, centerY - getHeight() / 2);
        frame.setSize(centerX, centerY);
        frame.setVisible(true);
    }

    public static void main(String[] args){
        mainGui myGui = new mainGui();
        myGui.show();
    }
}
