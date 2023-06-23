
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import Analisador.AnalisadorLexico;
import Analisador.TokenController;
import Exceptions.AnaliseException;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import static java.awt.event.KeyEvent.*;
import java.awt.event.KeyListener;
import java.io.*;

public class Interface extends JFrame {

    private static final String ARQUIVO_COM_CODIGO = "sample.minipascal";

    private JTextPane area = new JTextPane();
    private JTextArea codigo = new JTextArea("");
    private JButton compilar = new JButton("COMPILAR");

    private SimpleAttributeSet inputSAS = new SimpleAttributeSet();
    private SimpleAttributeSet output = new SimpleAttributeSet();
    private SimpleAttributeSet error = new SimpleAttributeSet();

    private Thread compilador = new Thread();
    private String inputCompilador = "";

    public static void main(String[] args) throws IOException {
        new Interface();
    }

    private void backspaceCompilador() {
        try {
            area.getStyledDocument().remove(area.getStyledDocument().getLength() - 1, 1);
        } catch (BadLocationException e1) {
            write(error, e1.getMessage());
        }
        inputCompilador = inputCompilador.substring(0, inputCompilador.length() - 1);
    }

    private void insereLetraCompilador(String trecho) {
        try {
            area.getStyledDocument().insertString(area.getStyledDocument().getLength(), trecho, inputSAS);
        } catch (BadLocationException e1) {
            write(error, e1.getMessage());
        }
        inputCompilador += trecho;
    }

    public boolean isPrintableChar(char c) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
        return (!Character.isISOControl(c)) &&
                c != KeyEvent.CHAR_UNDEFINED &&
                block != null &&
                block != Character.UnicodeBlock.SPECIALS;
    }

    public Interface() throws IOException {
        super("COMPILADOR MINI PASCAL");

        getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.X_AXIS));

        StyleConstants.setForeground(inputSAS, Color.GRAY);
        StyleConstants.setBackground(inputSAS, Color.BLACK);

        StyleConstants.setForeground(output, Color.GREEN);
        StyleConstants.setBackground(output, Color.BLACK);

        StyleConstants.setForeground(error, Color.RED);
        StyleConstants.setBackground(error, Color.BLACK);

        area.addKeyListener(new KeyListener() {
            @Override
            public void keyPressed(KeyEvent e) {
                int key = e.getKeyCode();
                boolean isPrintavel = isPrintableChar(e.getKeyChar());
                boolean inputAcces = Compilador.getInputAcces();
                if (inputAcces && key == VK_ENTER && !inputCompilador.isBlank()) {
                    Compilador.setInput(inputCompilador);
                    inputCompilador = "";
                } else if (inputAcces && key == VK_BACK_SPACE && !inputCompilador.isEmpty()) {
                    backspaceCompilador();
                } else if (inputAcces && (key != VK_ENTER) && isPrintavel) {
                    insereLetraCompilador(e.getKeyChar() + "");
                }
            }

            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyReleased(KeyEvent e) {
            }

        });

        compilar.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                try {

                    if (compilador.isAlive()) {
                        if (compilador.getState().equals(Thread.State.TIMED_WAITING)
                                || compilador.getState().equals(Thread.State.WAITING)) {
                            throw new Exception("\n Waiting input... not possible to stop");
                        } else {
                            compilador.interrupt();
                        }
                    }
                    area.setText(" ");

                    String command = codigo.getText();

                    Writer escritor = new BufferedWriter(
                            new OutputStreamWriter(new FileOutputStream(ARQUIVO_COM_CODIGO), "utf-8"));
                    escritor.write(command);
                    escritor.close();

                    compilador = new Thread(new Runnable() {
                        public void run() {
                            try {
                                new AnalisadorLexico(ARQUIVO_COM_CODIGO);
                                new TokenController();
                                new Compilador(TokenController.getListaIdentificadores(), area, output, error)
                                        .executaPrograma();
                            } catch (AnaliseException e) {
                                write(error, "\n" + e.getMessage());
                            }
                        }
                    });
                    compilador.start();

                } catch (Exception exc) {
                    setInputCompilador("");
                    write(error, exc.getMessage());
                    exc.printStackTrace();
                }
            }

        });

        area.setBackground(Color.black);
        area.setCaretColor(Color.GREEN);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        area.setEditable(false);

        JScrollPane pane = new JScrollPane(area);
        pane.setBorder(BorderFactory.createLineBorder(Color.GREEN));
        pane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        pane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        pane.setPreferredSize(new Dimension(640, 460));

        LineNumberTextAreaTest();
        codigo.setBackground(Color.GRAY);
        codigo.setForeground(Color.white);
        codigo.setCaretColor(Color.GREEN);
        codigo.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        codigo.setBorder(BorderFactory.createLineBorder(Color.GREEN));
        codigo.setSize(new Dimension(640, 460));
        codigo.setCursor(getCursor());
        codigo.setText(getFileContent(new FileInputStream(new File(ARQUIVO_COM_CODIGO)), "UTF-8"));

        add(codigo);
        add(compilar);
        add(pane);

        Dimension DIM = new Dimension(1280, 720);
        setPreferredSize(DIM);
        setSize(DIM);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(true);
        pack();
        setVisible(true);

        codigo.requestFocus();
    }

    public static String getFileContent(FileInputStream fis, String encoding) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(fis, encoding))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append('\n');
            }
            return sb.toString();
        }
    }

    private void write(SimpleAttributeSet attributeSet, String... lines) {
        try {
            if (lines.length == 0)
                return;
            for (String line : lines) {
                area.getStyledDocument().insertString(area.getStyledDocument().getLength(), line + "\n", attributeSet);
            }
            area.getStyledDocument().insertString(area.getStyledDocument().getLength(), "\n", attributeSet);
        } catch (Exception e) {
            error(e);
        }
    }

    private void error(Exception e) {
        write(error, "An error has occured: " + e.getLocalizedMessage());
        e.printStackTrace();
    }

    public void LineNumberTextAreaTest() {
        JTextArea lines;
        JScrollPane jsp;
        jsp = new JScrollPane();
        codigo = new JTextArea();
        lines = new JTextArea("1");
        lines.setBackground(Color.LIGHT_GRAY);
        lines.setEditable(false);
        lines.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        codigo.getDocument().addDocumentListener(new DocumentListener() {
            public String getText() {
                int caretPosition = codigo.getDocument().getLength();
                Element root = codigo.getDocument().getDefaultRootElement();
                String text = "1" + System.getProperty("line.separator");
                for (int i = 2; i < root.getElementIndex(caretPosition) + 2; i++) {
                    text += i + System.getProperty("line.separator");
                }
                return text;
            }

            @Override
            public void changedUpdate(DocumentEvent de) {
                lines.setText(getText());
            }

            @Override
            public void insertUpdate(DocumentEvent de) {
                lines.setText(getText());
            }

            @Override
            public void removeUpdate(DocumentEvent de) {
                lines.setText(getText());
            }
        });
        jsp.getViewport().add(codigo);
        jsp.setRowHeaderView(lines);
        add(jsp);
        setSize(400, 275);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public String getInputCompilador() {
        return inputCompilador;
    }

    public void setInputCompilador(String inputCompilador) {
        this.inputCompilador = inputCompilador;
    }
}