import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class InputMonitor extends JFrame {
    private JTextField inputField1, inputField2, inputField3;
    private JTextArea resultArea1, resultArea2, resultArea3;

    public InputMonitor() {
        super("Input Monitor");

        // create the input fields
        inputField1 = new JTextField(10);
        inputField2 = new JTextField(10);
        inputField3 = new JTextField(10);

        // create the result areas
        resultArea1 = new JTextArea(5, 20);
        resultArea1.setEditable(false);
        resultArea1.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Result 1"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        resultArea2 = new JTextArea(5, 20);
        resultArea2.setEditable(false);
        resultArea2.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Result 2"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        resultArea3 = new JTextArea(5, 20);
        resultArea3.setEditable(false);
        resultArea3.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Result 3"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        // create the add button
        JButton addButton = new JButton("Add");
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // get the input values
                int num1 = Integer.parseInt(inputField1.getText());
                int num2 = Integer.parseInt(inputField2.getText());
                int num3 = Integer.parseInt(inputField3.getText());

                // calculate the results
                int result1 = num1 * 2;
                int result2 = num2 + 5;
                int result3 = num3 - 10;

                // update the result areas
                resultArea1.setText(Integer.toString(result1));
                resultArea2.setText(Integer.toString(result2));
                resultArea3.setText(Integer.toString(result3));
            }
        });

        // create the save button
        JButton saveButton = new JButton("Save Results");
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // prompt the user for a filename
                JFileChooser fileChooser = new JFileChooser();
                FileNameExtensionFilter filter = new FileNameExtensionFilter("CSV Files", "csv");
                fileChooser.setFileFilter(filter);
                int result = fileChooser.showSaveDialog(InputMonitor.this);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File file = fileChooser.getSelectedFile();
                    String filename = file.getAbsolutePath();
                    if (!filename.endsWith(".csv")) {
                        filename += ".csv";
                    }

                    // write the results to the file
                    try {
                        FileWriter writer = new FileWriter(filename);
                        writer.write("Result 1,Result 2,Result 3\n");
                        writer.write(resultArea1.getText() + "," + resultArea2.getText() + "," + resultArea3.getText() + "\n");
                        writer.close();
                        JOptionPane.showMessageDialog(InputMonitor.this, "Results saved to " + filename);
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(InputMonitor.this, "Error saving results to " + filename);
                    }
                }
            }
        });

        // create the input panel
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new GridLayout(4, 2, 10, 10));
        inputPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Enter Input Values"),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        inputPanel.add(new JLabel("Input 1:"));
        inputPanel.add(inputField1);
        inputPanel.add(new JLabel("Input 2:"));
        inputPanel.add(inputField2);
        inputPanel.add(new JLabel("Input 3:"));
        inputPanel.add(inputField3);
        inputPanel.add(addButton);
        inputPanel.add(saveButton);

        // add the components to the frame
        setLayout(new GridLayout(2, 1, 10, 10));
        add(inputPanel);

        JPanel resultPanel = new JPanel();
        resultPanel.setLayout(new GridLayout(1, 3, 10, 10));
        resultPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        resultPanel.add(resultArea1);
        resultPanel.add(resultArea2);
        resultPanel.add(resultArea3);
        add(resultPanel);

        // set the size and visibility of the frame
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 400);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public static void main(String[] args) {
        new InputMonitor();
    }
}


