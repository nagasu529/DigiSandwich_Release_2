import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class mainGui extends JFrame {
    private JTextField inputField1, inputField2, inputField3;
    private JLabel resultLabel1, resultLabel2, resultLabel3;

    public mainGui() {
        super("Input Monitor");

        // create the input fields
        inputField1 = new JTextField(10);
        inputField2 = new JTextField(10);
        inputField3 = new JTextField(10);

        // create the result labels
        resultLabel1 = new JLabel("Result 1: ");
        resultLabel2 = new JLabel("Result 2: ");
        resultLabel3 = new JLabel("Result 3: ");

        // create the button
        JButton addButton = new JButton("Add");
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // get the input values
                int num1 = Integer.parseInt(inputField1.getText());
                int num2 = Integer.parseInt(inputField2.getText());
                int num3 = Integer.parseInt(inputField3.getText());

                // calculate the results
                int result1 = num1 + num2;
                int result2 = num1 + num3;
                int result3 = num2 + num3;

                // update the result labels
                resultLabel1.setText("Result 1: " + result1);
                resultLabel2.setText("Result 2: " + result2);
                resultLabel3.setText("Result 3: " + result3);
            }
        });

        // create a panel for the input fields and button
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new FlowLayout());
        inputPanel.add(new JLabel("Input 1: "));
        inputPanel.add(inputField1);
        inputPanel.add(new JLabel("Input 2: "));
        inputPanel.add(inputField2);
        inputPanel.add(new JLabel("Input 3: "));
        inputPanel.add(inputField3);
        inputPanel.add(addButton);

        // create a panel for the result labels
        JPanel resultPanel = new JPanel();
        resultPanel.setLayout(new GridLayout(3, 1));
        resultPanel.add(resultLabel1);
        resultPanel.add(resultLabel2);
        resultPanel.add(resultLabel3);

        // add the panels to the frame
        setLayout(new BorderLayout());
        add(inputPanel, BorderLayout.CENTER);
        add(resultPanel, BorderLayout.SOUTH);

        // set the size and visibility of the frame
        setSize(400, 200);
        setVisible(true);

        // add a WindowListener to stop the program when the GUI is closed
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
    }

    public static void main(String[] args) {
        mainGui inputMonitor = new mainGui();
    }
}
