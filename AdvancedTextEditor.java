package com.company;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.autocomplete.CompletionProvider;
import org.fife.ui.autocomplete.DefaultCompletionProvider;
import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import javax.swing.*;
import javax.tools.JavaCompiler;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import javax.tools.ToolProvider;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class AdvancedTextEditor extends JFrame {

    private static RSyntaxTextArea textArea;
    private JTextPane outputArea;
    private JFileChooser fileChooser;
    private JLabel statusLabel;
    private JRadioButtonMenuItem textOutputMenuItem;
    private JRadioButtonMenuItem htmlOutputMenuItem;
    private JMenuItem clearOutputMenuItem;

    public AdvancedTextEditor() {
        setTitle("ADVANCED CODE HUB");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Initialize RSyntaxTextArea
        textArea = new RSyntaxTextArea();
        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
        textArea.setCodeFoldingEnabled(true);
        textArea.setFocusTraversalPolicyProvider(true);

        // Focus listener for the text area
        textArea.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                System.out.println("TextArea gained focus");
            }

            @Override
            public void focusLost(FocusEvent e) {
                System.out.println("TextArea lost focus");
            }
        });

        // Scroll pane for the text area
        RTextScrollPane scrollPane = new RTextScrollPane(textArea);
        textArea.requestFocusInWindow();

        // Initialize AutoCompletion
        CompletionProvider provider = createCompletionProvider();
        AutoCompletion ac = new AutoCompletion(provider);
        ac.install(textArea);

        // Output area for program output
        outputArea = new JTextPane();
        outputArea.setEditable(false);
        JScrollPane outputScrollPane = new JScrollPane(outputArea);

        // File chooser
        fileChooser = new JFileChooser();

        // Status label
        statusLabel = new JLabel("Ready");

        // Output options
        textOutputMenuItem = new JRadioButtonMenuItem("Text Output");
        htmlOutputMenuItem = new JRadioButtonMenuItem("HTML Output");
        clearOutputMenuItem = new JMenuItem("Clear Output");

        // Button group for output options
        ButtonGroup outputGroup = new ButtonGroup();
        outputGroup.add(textOutputMenuItem);
        outputGroup.add(htmlOutputMenuItem);

        // Default output option
        textOutputMenuItem.setSelected(true);

        // Menu bar
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenu editMenu = new JMenu("Edit");

        // File menu items
        JMenuItem openMenuItem = new JMenuItem("Open");
        JMenuItem saveMenuItem = new JMenuItem("Save");
        JMenuItem saveAsMenuItem = new JMenuItem("Save As");
        JMenuItem exitMenuItem = new JMenuItem("Exit");

        // Edit menu items
        JMenuItem zoomInMenuItem = new JMenuItem("Zoom In");
        JMenuItem zoomOutMenuItem = new JMenuItem("Zoom Out");

        // Split pane to separate text area and output area
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollPane, outputScrollPane);
        splitPane.setOneTouchExpandable(true);
        splitPane.setDividerLocation(400);

        // Add components to the frame
        setLayout(new BorderLayout());
        add(splitPane, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.PAGE_END);

        // Menu items for edit actions
        JMenuItem cutMenuItem = new JMenuItem(new DefaultEditorKit.CutAction());
        cutMenuItem.setText("Cut");
        JMenuItem copyMenuItem = new JMenuItem(new DefaultEditorKit.CopyAction());
        copyMenuItem.setText("Copy");
        JMenuItem pasteMenuItem = new JMenuItem(new DefaultEditorKit.PasteAction());
        pasteMenuItem.setText("Paste");

        // Undo and redo (basic functionality)
        JMenuItem undoMenuItem = new JMenuItem("Undo");
        JMenuItem redoMenuItem = new JMenuItem("Redo");

        // Run menu item
        JMenuItem runMenuItem = new JMenuItem("Run");

        // Add items to the file menu
        fileMenu.add(openMenuItem);
        fileMenu.add(saveMenuItem);
        fileMenu.add(saveAsMenuItem);
        fileMenu.addSeparator();
        fileMenu.add(exitMenuItem);

        // Add items to the edit menu
        editMenu.add(undoMenuItem);
        editMenu.add(redoMenuItem);
        editMenu.addSeparator();
        editMenu.add(cutMenuItem);
        editMenu.add(copyMenuItem);
        editMenu.add(pasteMenuItem);
        editMenu.addSeparator();
        editMenu.add(zoomInMenuItem);
        editMenu.add(zoomOutMenuItem);

        // Add menus to the menu bar
        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(runMenuItem);

        // Set the menu bar
        setJMenuBar(menuBar);

        // Make the frame visible
        setVisible(true);

        // Zoom in and out functionality
        zoomInMenuItem.addActionListener(e -> {
            Font currentFont = textArea.getFont();
            float newSize = currentFont.getSize2D() + 2f;
            textArea.setFont(currentFont.deriveFont(newSize));
        });

        zoomOutMenuItem.addActionListener(e -> {
            Font currentFont = textArea.getFont();
            float newSize = Math.max(1f, currentFont.getSize2D() - 2f);
            textArea.setFont(currentFont.deriveFont(newSize));
        });

        zoomInMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, KeyEvent.CTRL_DOWN_MASK));
        zoomOutMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, KeyEvent.CTRL_DOWN_MASK));

        // Update status label on document changes
        textArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateStatus();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateStatus();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateStatus();
            }
        });

        // File operations
        openMenuItem.addActionListener(e -> openFile());
        saveMenuItem.addActionListener(e -> saveFile());
        saveAsMenuItem.addActionListener(e -> saveFileAs());
        exitMenuItem.addActionListener(e -> System.exit(0));

        // Undo and redo actions
        undoMenuItem.addActionListener(e -> textArea.undoLastAction());
        redoMenuItem.addActionListener(e -> textArea.redoLastAction());

        // Run code action
        runMenuItem.addActionListener(e -> {
            File currentFile = fileChooser.getSelectedFile();
            if (currentFile != null) {
                String className = currentFile.getName().replaceFirst("[.][^.]+$", "");
                runJavaCode(currentFile, className);
            } else {
                JOptionPane.showMessageDialog(this, "No file selected to run!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // Output options
        textOutputMenuItem.addActionListener(e -> {
            outputArea.setContentType("text/plain");
        });

        htmlOutputMenuItem.addActionListener(e -> {
            outputArea.setContentType("text/html");
        });

        clearOutputMenuItem.addActionListener(e -> outputArea.setText(""));
    }

    // Create completion provider for auto-completion
    private CompletionProvider createCompletionProvider() {
        DefaultCompletionProvider provider = new DefaultCompletionProvider();
        provider.addCompletion(new BasicCompletion(provider, "apple"));
        provider.addCompletion(new BasicCompletion(provider, "String"));
        provider.addCompletion(new BasicCompletion(provider, "System.out.println"));
        provider.addCompletion(new BasicCompletion(provider, "for"));
        provider.addCompletion(new BasicCompletion(provider, "if"));
        provider.addCompletion(new BasicCompletion(provider, "else"));
        provider.addCompletion(new BasicCompletion(provider, "public"));
        provider.addCompletion(new BasicCompletion(provider, "private"));
        provider.addCompletion(new BasicCompletion(provider, "protected"));
        provider.addCompletion(new BasicCompletion(provider, "import"));
        provider.addCompletion(new BasicCompletion(provider, "class"));
        provider.addCompletion(new BasicCompletion(provider, "interface"));
        provider.addCompletion(new BasicCompletion(provider, "enum"));
        // Add more completions as needed
        return provider;
    }

    // Open a file and read its content into the text area
    private void openFile() {
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(selectedFile), StandardCharsets.UTF_8))) {
                textArea.read(reader, null);
                fileChooser.setSelectedFile(selectedFile); // Store the current file
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Save the content of the text area to a file
    private void saveFile() {
        File selectedFile = fileChooser.getSelectedFile();
        if (selectedFile != null) {
            saveToFile(selectedFile);
        } else {
            saveFileAs();
        }
    }

    // Save the content of the text area to a new file
    private void saveFileAs() {
        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            saveToFile(selectedFile);
        }
    }

    // Write the content of the text area to a file
    private void saveToFile(File selectedFile) {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(selectedFile), StandardCharsets.UTF_8))) {
            textArea.write(writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Compile and run Java code from the selected file
    private void runJavaCode(File javaFile, String className) {
        Process process = null;

        try {
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            int compilationResult = compiler.run(null, null, null, javaFile.getAbsolutePath());

            if (compilationResult == 0) {
                ProcessBuilder processBuilder = new ProcessBuilder("java", "-cp", javaFile.getParentFile().getPath(), className);
                process = processBuilder.start();

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                StringBuilder output = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }

                BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                StringBuilder errorOutput = new StringBuilder();
                String errorLine;
                while ((errorLine = errorReader.readLine()) != null) {
                    errorOutput.append(errorLine).append("\n");
                }

                int exitCode = process.waitFor();

                if (exitCode == 0) {
                    outputArea.setText("Program Output:\n" + output.toString());
                    JOptionPane.showMessageDialog(this, "Program Output:\n" + output.toString(), "Output", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    outputArea.setText("Runtime Error:\n" + errorOutput.toString());
                    JOptionPane.showMessageDialog(this, "Runtime Error!\n" + errorOutput.toString(), "Error", JOptionPane.ERROR_MESSAGE);
                }

            } else {
                JOptionPane.showMessageDialog(this, "Compilation Failed!", "Error", JOptionPane.ERROR_MESSAGE);
                outputArea.setText("Compilation Failed!");
            }

            if (process != null) {
                process.destroy();
            }
        } catch (IOException | InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    // Update the status label with the current line and column number
    private void updateStatus() {
        try {
            int caretPosition = textArea.getCaretPosition();
            int lineNumber = textArea.getLineOfOffset(caretPosition) + 1;
            int columnNumber = caretPosition - textArea.getLineStartOffset(lineNumber - 1) + 1;
            statusLabel.setText("Line: " + lineNumber + " | Column: " + columnNumber);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    // Main method to launch the application
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AdvancedTextEditor());
    }
}
