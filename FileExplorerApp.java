import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.nio.file.Files;
import javax.swing.*;

public class FileExplorerApp extends JFrame
        implements ActionListener, MouseListener {

    // ===== UI COMPONENTS =====
    private JTextField pathField, searchField;
    private JButton browseBtn, upBtn, searchBtn, refreshBtn, hideBtn;
    private JList<File> fileList;
    private DefaultListModel<File> listModel;

    private File currentDirectory;

    // ===== CONSTRUCTOR =====
    public FileExplorerApp() {
        setTitle("File Explorer Mini Tool");
        setSize(850, 520);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        initUI();
        setupUnhideShortcut();   // ✅ Key Binding (FIXED)
        loadDirectory(new File(System.getProperty("user.home")));
    }

    // ===== UI SETUP =====
    private void initUI() {
        pathField = new JTextField();
        pathField.setEditable(false);

        browseBtn = new JButton("Browse");
        upBtn = new JButton("Up");
        searchBtn = new JButton("Search");
        refreshBtn = new JButton("Refresh");
        hideBtn = new JButton("Hide Selected");

        searchField = new JTextField(18);

        // Centralized listener
        browseBtn.addActionListener(this);
        upBtn.addActionListener(this);
        searchBtn.addActionListener(this);
        refreshBtn.addActionListener(this);
        hideBtn.addActionListener(this);

        JPanel top = new JPanel(new BorderLayout(5, 5));
        top.add(pathField, BorderLayout.CENTER);

        JPanel topRight = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        topRight.add(upBtn);
        topRight.add(browseBtn);
        top.add(topRight, BorderLayout.EAST);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottom.add(new JLabel("Search:"));
        bottom.add(searchField);
        bottom.add(searchBtn);
        bottom.add(refreshBtn);
        bottom.add(hideBtn);

        listModel = new DefaultListModel<>();
        fileList = new JList<>(listModel);
        fileList.addMouseListener(this);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(fileList), BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }

    // ===== CENTRALIZED EVENT HANDLING =====
    @Override
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();

        if (src == browseBtn) browseDirectory();
        else if (src == upBtn) goUpDirectory();
        else if (src == searchBtn) searchFiles();
        else if (src == refreshBtn) loadDirectory(currentDirectory);
        else if (src == hideBtn) hideFile();
    }

    // ===== DOUBLE CLICK HANDLING =====
    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2) {
            File f = fileList.getSelectedValue();
            if (f == null) return;

            if (f.isDirectory()) {
                loadDirectory(f);
            } else {
                openFileWithStats(f);
            }
        }
    }

    // Required empty overrides
    public void mousePressed(MouseEvent e) {}
    public void mouseReleased(MouseEvent e) {}
    public void mouseEntered(MouseEvent e) {}
    public void mouseExited(MouseEvent e) {}

    // ===== KEY BINDING (CORRECT WAY) =====
    private void setupUnhideShortcut() {
        KeyStroke ks = KeyStroke.getKeyStroke(
                KeyEvent.VK_U,
                InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK
        );

        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(ks, "UNHIDE_FILES");

        getRootPane().getActionMap()
                .put("UNHIDE_FILES", new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        unhideHiddenFiles();
                    }
                });
    }

    // ===== CORE FUNCTIONALITY =====
    private void browseDirectory() {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            loadDirectory(fc.getSelectedFile());
        }
    }

    private void goUpDirectory() {
        if (currentDirectory == null) return;
        File parent = currentDirectory.getParentFile();
        if (parent != null) loadDirectory(parent);
    }

    // 🔥 HIDDEN FILES FILTERED HERE
    private void loadDirectory(File dir) {
        if (dir == null || !dir.isDirectory()) return;

        currentDirectory = dir;
        pathField.setText(dir.getAbsolutePath());
        listModel.clear();

        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                try {
                    if (!Files.isHidden(f.toPath())) {
                        listModel.addElement(f);
                    }
                } catch (Exception ignored) {}
            }
        }

        upBtn.setEnabled(dir.getParentFile() != null);
    }

    private void searchFiles() {
        String key = searchField.getText().trim().toLowerCase();
        if (key.isEmpty()) return;

        listModel.clear();
        searchRecursive(currentDirectory, key);
    }

    private void searchRecursive(File dir, String key) {
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File f : files) {
            try {
                if (Files.isHidden(f.toPath())) continue;

                if (f.getName().toLowerCase().contains(key)) {
                    listModel.addElement(f);
                }
                if (f.isDirectory()) {
                    searchRecursive(f, key);
                }
            } catch (Exception ignored) {}
        }
    }

    private void openFileWithStats(File f) {
        try {
            Desktop.getDesktop().open(f);

            long size = Files.size(f.toPath());
            String stats =
                    "File Name: " + f.getName() +
                    "\nSize: " + size + " bytes" +
                    "\nPath: " + f.getAbsolutePath();

            JOptionPane.showMessageDialog(this, stats,
                    "File Statistics", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Cannot open file");
        }
    }

    private void hideFile() {
        File f = fileList.getSelectedValue();
        if (f == null) return;

        try {
            Files.setAttribute(f.toPath(), "dos:hidden", true);
            JOptionPane.showMessageDialog(this, "File hidden successfully");
            loadDirectory(currentDirectory);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Hide failed");
        }
    }

    // 🔐 UNHIDE ALL HIDDEN FILES IN CURRENT DIRECTORY
    private void unhideHiddenFiles() {
        File[] files = currentDirectory.listFiles();
        if (files == null) return;

        int count = 0;
        for (File f : files) {
            try {
                if (Files.isHidden(f.toPath())) {
                    Files.setAttribute(f.toPath(), "dos:hidden", false);
                    count++;
                }
            } catch (Exception ignored) {}
        }

        if (count > 0) {
            JOptionPane.showMessageDialog(this,
                    count + " file(s) unhidden");
        }

        loadDirectory(currentDirectory);
    }

    // ===== MAIN =====
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() ->
                new FileExplorerApp().setVisible(true));
    }
}
