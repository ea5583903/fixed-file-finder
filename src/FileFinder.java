import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Stack;

public class FileFinder extends JFrame {
    private JTable fileTable;
    private DefaultTableModel tableModel;
    private JLabel pathLabel;
    private JButton backButton;
    private JButton forwardButton;
    private JButton upButton;
    private File currentDirectory;
    private Stack<File> backHistory;
    private Stack<File> forwardHistory;

    public FileFinder() {
        setTitle("File Finder");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Initialize history stacks
        backHistory = new Stack<>();
        forwardHistory = new Stack<>();

        // Start at user home directory
        currentDirectory = new File(System.getProperty("user.home"));

        // Create main panel
        JPanel mainPanel = new JPanel(new BorderLayout());

        // Create toolbar
        JPanel toolbar = createToolbar();
        mainPanel.add(toolbar, BorderLayout.NORTH);

        // Create path label
        pathLabel = new JLabel();
        pathLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        pathLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        mainPanel.add(pathLabel, BorderLayout.SOUTH);

        // Create table for files
        String[] columnNames = {"Name", "Size", "Type", "Date Modified", "ActualName"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        fileTable = new JTable(tableModel);
        fileTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        fileTable.setRowHeight(25);
        fileTable.getTableHeader().setReorderingAllowed(false);

        // Set column widths
        TableColumnModel columnModel = fileTable.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(300);
        columnModel.getColumn(1).setPreferredWidth(100);
        columnModel.getColumn(2).setPreferredWidth(150);
        columnModel.getColumn(3).setPreferredWidth(200);
        columnModel.getColumn(4).setMinWidth(0);
        columnModel.getColumn(4).setMaxWidth(0);
        columnModel.getColumn(4).setWidth(0);

        // Add double-click listener
        fileTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = fileTable.getSelectedRow();
                    if (row >= 0) {
                        String fileName = (String) tableModel.getValueAt(row, 4);
                        File file = new File(currentDirectory, fileName);
                        if (file.isDirectory()) {
                            navigateToDirectory(file);
                        } else {
                            openFile(file);
                        }
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(fileTable);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        add(mainPanel);

        // Load initial directory
        loadDirectory(currentDirectory);
    }

    private JPanel createToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        toolbar.setBackground(new Color(240, 240, 240));

        // Back button
        backButton = new JButton("←");
        backButton.setToolTipText("Back");
        backButton.setEnabled(false);
        backButton.addActionListener(e -> goBack());
        toolbar.add(backButton);

        // Forward button
        forwardButton = new JButton("→");
        forwardButton.setToolTipText("Forward");
        forwardButton.setEnabled(false);
        forwardButton.addActionListener(e -> goForward());
        toolbar.add(forwardButton);

        // Up button
        upButton = new JButton("↑");
        upButton.setToolTipText("Up");
        upButton.addActionListener(e -> goUp());
        toolbar.add(upButton);

        // Home button
        JButton homeButton = new JButton("⌂");
        homeButton.setToolTipText("Home");
        homeButton.addActionListener(e -> navigateToDirectory(new File(System.getProperty("user.home"))));
        toolbar.add(homeButton);

        // Refresh button
        JButton refreshButton = new JButton("⟳");
        refreshButton.setToolTipText("Refresh");
        refreshButton.addActionListener(e -> loadDirectory(currentDirectory));
        toolbar.add(refreshButton);

        return toolbar;
    }

    private void loadDirectory(File directory) {
        if (!directory.exists() || !directory.isDirectory()) {
            JOptionPane.showMessageDialog(this, "Cannot access directory: " + directory.getPath(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Clear table
        tableModel.setRowCount(0);

        // Update path label
        pathLabel.setText(directory.getAbsolutePath());

        // Get files and folders
        File[] files = directory.listFiles();
        if (files != null) {
            // Filter out hidden files (starting with .) and files without read permissions
            ArrayList<File> filteredFiles = new ArrayList<>();
            for (File file : files) {
                if (!file.getName().startsWith(".") && file.canRead()) {
                    filteredFiles.add(file);
                }
            }

            // Sort: folders first, then files
            filteredFiles.sort((f1, f2) -> {
                if (f1.isDirectory() && !f2.isDirectory()) {
                    return -1;
                } else if (!f1.isDirectory() && f2.isDirectory()) {
                    return 1;
                } else {
                    return f1.getName().compareToIgnoreCase(f2.getName());
                }
            });

            SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm");

            for (File file : filteredFiles) {
                String name = file.getName();
                String displayName = name;
                String size = file.isDirectory() ? "--" : formatFileSize(file.length());
                String type = file.isDirectory() ? "Folder" : getFileType(file);
                String dateModified = dateFormat.format(new Date(file.lastModified()));

                // Add folder icon for directories
                if (file.isDirectory()) {
                    displayName = "📁 " + name;
                } else {
                    displayName = "📄 " + name;
                }

                tableModel.addRow(new Object[]{displayName, size, type, dateModified, name});
            }
        }

        // Update button states
        upButton.setEnabled(directory.getParentFile() != null);
    }

    private void navigateToDirectory(File directory) {
        if (currentDirectory != null) {
            backHistory.push(currentDirectory);
            backButton.setEnabled(true);
        }
        forwardHistory.clear();
        forwardButton.setEnabled(false);

        currentDirectory = directory;
        loadDirectory(directory);
    }

    private void goBack() {
        if (!backHistory.isEmpty()) {
            forwardHistory.push(currentDirectory);
            forwardButton.setEnabled(true);

            currentDirectory = backHistory.pop();
            backButton.setEnabled(!backHistory.isEmpty());

            loadDirectory(currentDirectory);
        }
    }

    private void goForward() {
        if (!forwardHistory.isEmpty()) {
            backHistory.push(currentDirectory);
            backButton.setEnabled(true);

            currentDirectory = forwardHistory.pop();
            forwardButton.setEnabled(!forwardHistory.isEmpty());

            loadDirectory(currentDirectory);
        }
    }

    private void goUp() {
        File parent = currentDirectory.getParentFile();
        if (parent != null) {
            navigateToDirectory(parent);
        }
    }

    private void openFile(File file) {
        try {
            Desktop.getDesktop().open(file);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Cannot open file: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", size / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0));
        }
    }

    private String getFileType(File file) {
        String name = file.getName();
        int lastDot = name.lastIndexOf('.');
        if (lastDot > 0 && lastDot < name.length() - 1) {
            return name.substring(lastDot + 1).toUpperCase() + " File";
        }
        return "File";
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }

            FileFinder finder = new FileFinder();
            finder.setVisible(true);
        });
    }
}
