package org.intellij;

import burp.api.montoya.MontoyaApi;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RootPanel {
    private final MontoyaApi api;
    private final AutoUtil autoUtil;

    private DefaultTableModel tableModel;
    private javax.swing.JPanel JPanel;
    private javax.swing.JPanel buttonJPanel;
    private JButton addButton;
    private JButton deleteButton;
    private JScrollPane scrollpane;
    private JTextArea textArea;
    private JTextArea resTextArea;
    private JButton regexButton;
    private JTextField regexTextField;
    private JCheckBox tagCheckBox;
    private JTable jsTable;
    private String regex = "\\(-(.*?)-\\)";
    private ArrayList<String[]> jsList = new ArrayList<>();

    public String[] findConf(String host, String path) {
        if (host != "" && path != "") {
            for (int i = 0; i < jsList.size(); i++) {
                if (host.equals(jsList.get(i)[0]) && path.startsWith(jsList.get(i)[1])) {
                    return jsList.get(i);
                }
            }
        }
        return null;
    }

    public void addList(String host, String path) {
        Object[] data = {false, host, path, regex, false,false, "JSFile", ""};
        tableModel.addRow(data);
    }

    public RootPanel(MontoyaApi montoyaApi, AutoUtil autoUtil) {
        this.api = montoyaApi;
        this.autoUtil = autoUtil;

        $$$setupUI$$$();
        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if(jsTable.isEditing()){
                    jsTable.getCellEditor().stopCellEditing();
                }
                Object[] data = {false, "", "", regex, false , false, "JSFile", ""};
                tableModel.addRow(data);
            }
        });
        deleteButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                int selectedRow = jsTable.getSelectedRow();
                if(jsTable.isEditing()){
                    jsTable.getCellEditor().stopCellEditing();
                }

                if (tableModel.getValueAt(selectedRow, 5).equals("WebSocket")) {
                    String target = tableModel.getValueAt(selectedRow, 6).toString();
                    autoUtil.deleteWSServer(target);
                }
                jsTable.clearSelection();
                tableModel.removeRow(selectedRow);
                }
        });
        regexButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String regexTest = regexTextField.getText();
                if (regexTest != null || regexTest.length() > 0) {
                    Pattern pattern = Pattern.compile(regexTest);
                    String text = textArea.getText();
                    Matcher matcher = pattern.matcher(text);
                    resTextArea.replaceRange("", 0, resTextArea.getText().length());
                    int tag = tagCheckBox.isSelected() ? 0 : 1;
                    while (matcher.find()) {
                        resTextArea.append(matcher.group(tag) + "\n");
                    }
                }
            }
        });

        tableModel.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {

                jsList.clear();
                for (int i = 0; i < tableModel.getRowCount(); i++) {
//                    启用时加入List
                    if (tableModel.getValueAt(i, 0).equals(true)) {
                        jsList.add(new String[]{
                                tableModel.getValueAt(i, 1).toString(),
                                tableModel.getValueAt(i, 2).toString(),
                                tableModel.getValueAt(i, 3).toString(),
                                (tableModel.getValueAt(i, 4).toString() == "true") ? "1" : "0",
                                (tableModel.getValueAt(i, 5).toString() == "true") ? "0" : "1",
                                tableModel.getValueAt(i, 6).toString(),
                                tableModel.getValueAt(i, 7).toString(),
                        });
                        if (tableModel.getValueAt(i, 5).equals("WebSocket")) {
                            String target = tableModel.getValueAt(i, 6).toString();
                            autoUtil.addWSServer(target);
                        }
                    } else {
                        if (tableModel.getValueAt(i, 5).equals("WebSocket")) {
//                            api.logging().logToOutput(autoUtil.checkWSServer(tableModel.getValueAt(i, 6).toString()) ? "ws存活" : "ws未存活");
                            autoUtil.deleteWSServer(tableModel.getValueAt(i, 6).toString());
                        }
                    }
                }
                jsTable.clearSelection();
                jsTable.repaint();
            }
        });
    }
    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        createUIComponents();
        JPanel = new JPanel();
        JPanel.setLayout(new GridLayoutManager(1, 1, new Insets(10, 10, 10, 10), -1, -1));
        final javax.swing.JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(4, 3, new Insets(0, 0, 0, 0), -1, -1));
        JPanel.add(panel1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        scrollpane = new JScrollPane();
        panel1.add(scrollpane, new GridConstraints(0, 0, 2, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        scrollpane.setViewportView(jsTable);
        final javax.swing.JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel2, new GridConstraints(2, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        regexButton = new JButton();
        regexButton.setText("匹配");
        panel2.add(regexButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        regexTextField = new JTextField();
        regexTextField.setHorizontalAlignment(2);
        regexTextField.setText("\\(-(.*?)-\\)");
        regexTextField.setToolTipText("正则表达式");
        panel2.add(regexTextField, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        tagCheckBox = new JCheckBox();
        tagCheckBox.setText("保留前|后缀");
        panel2.add(tagCheckBox, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final javax.swing.JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel3, new GridConstraints(3, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        scrollPane1.setHorizontalScrollBarPolicy(31);
        panel3.add(scrollPane1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        textArea = new JTextArea();
        textArea.setLineWrap(true);
        textArea.setRows(20);
        textArea.setText("");
        scrollPane1.setViewportView(textArea);
        final JScrollPane scrollPane2 = new JScrollPane();
        scrollPane2.setHorizontalScrollBarPolicy(31);
        panel3.add(scrollPane2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        resTextArea = new JTextArea();
        resTextArea.setLineWrap(true);
        resTextArea.setRows(20);
        resTextArea.setText("");
        scrollPane2.setViewportView(resTextArea);
        buttonJPanel = new JPanel();
        buttonJPanel.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(buttonJPanel, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        addButton = new JButton();
        addButton.setHorizontalAlignment(0);
        addButton.setHorizontalTextPosition(0);
        addButton.setText("新增");
        buttonJPanel.add(addButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        deleteButton = new JButton();
        deleteButton.setHorizontalTextPosition(0);
        deleteButton.setText("删除");
        buttonJPanel.add(deleteButton, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return JPanel;
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        String[] columnNames = {"状态", "Host", "Path", "正则表达式", "保留前后缀","处理响应体", "调用模式", "JSFile/API/WebSocket"};
        // 创建默认的表格模型
        tableModel = new DefaultTableModel(null, columnNames);
        jsTable = new JTable(tableModel);
        // 设置 Enabled 列的渲染器和编辑器
        jsTable.getColumnModel().getColumn(0).setCellRenderer(jsTable.getDefaultRenderer(Boolean.class));
        jsTable.getColumnModel().getColumn(0).setCellEditor(jsTable.getDefaultEditor(Boolean.class));
        jsTable.getColumnModel().getColumn(4).setCellRenderer(jsTable.getDefaultRenderer(Boolean.class));
        jsTable.getColumnModel().getColumn(4).setCellEditor(jsTable.getDefaultEditor(Boolean.class));
        jsTable.getColumnModel().getColumn(5).setCellRenderer(jsTable.getDefaultRenderer(Boolean.class));
        jsTable.getColumnModel().getColumn(5).setCellEditor(jsTable.getDefaultEditor(Boolean.class));
        JComboBox<String> comboBox = new JComboBox<>(new String[]{"JSFile", "API", "WebSocket"});
        jsTable.getColumnModel().getColumn(6).setCellEditor(new DefaultCellEditor(comboBox));
    }
}
