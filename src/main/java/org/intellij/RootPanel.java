package org.intellij;

import burp.api.montoya.MontoyaApi;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;

import javax.swing.*;
import javax.swing.DefaultCellEditor;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * RootPanel - Burp Suite插件主面板类
 * 
 * <p>提供加解密配置的图形界面管理功能，包括：
 * <ul>
 *   <li>配置表格的创建和管理</li>
 *   <li>添加、删除、修改加解密配置</li>
 *   <li>正则表达式测试功能</li>
 *   <li>配置数据的存储和检索</li>
 * </ul>
 * 
 * <p>配置表格包含以下列：
 * <ul>
 *   <li>状态：是否启用该配置</li>
 *   <li>Host：目标主机</li>
 *   <li>Path：目标路径</li>
 *   <li>正则表达式：用于匹配需要加解密的数据</li>
 *   <li>JSFile/API/WebSocket：加解密处理方式</li>
 *   <li>调用模式：处理模式（API、JSFile或WebSocket）</li>
 *   <li>保留前后缀：是否保留匹配的前后缀</li>
 *   <li>处理响应体：是否处理响应体</li>
 *   <li>参数聚合：是否启用参数聚合功能</li>
 * </ul>
 * 
 * @author RaindropEcho
 * @version 1.0
 */
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
    private JTable jTable;
    private String regex = "\\(-(.*?)-\\)";
    private ArrayList<String[]> jsList = new ArrayList<>();

    /**
     * 构造函数
     * 
     * @param montoyaApi Burp Suite Montoya API实例
     * @param autoUtil 自动化加解密工具实例
     */
    public RootPanel(MontoyaApi montoyaApi, AutoUtil autoUtil) {
        this.api = montoyaApi;
        this.autoUtil = autoUtil;

        $$$setupUI$$$();
        setupEventListeners();
    }

    /**
     * 设置事件监听器
     * 
     * <p>为按钮和表格模型添加事件监听器：
     * <ul>
     *   <li>新增按钮：添加新的配置行</li>
     *   <li>删除按钮：删除选中的配置行</li>
     *   <li>匹配按钮：测试正则表达式匹配</li>
     *   <li>表格模型变化：更新jsList配置数据</li>
     * </ul>
     */
    private void setupEventListeners() {
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleAddButton();
            }
        });

        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleDeleteButton();
            }
        });

        regexButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleRegexButton();
            }
        });

        tableModel.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                handleTableChanged();
            }
        });
    }

    /**
     * 处理新增按钮点击事件
     * 
     * <p>在表格中添加一个新的配置行，使用默认值：
     * <ul>
     *   <li>状态：false（未启用）</li>
     *   <li>正则表达式：默认标记正则</li>
     *   <li>调用模式：JSFile</li>
     *   <li>保留前后缀：false</li>
     *   <li>处理响应体：false</li>
     *   <li>参数聚合：false</li>
     * </ul>
     */
    private void handleAddButton() {
        if (jTable.isEditing()) {
            jTable.getCellEditor().stopCellEditing();
        }
        Object[] data = {false, "", "", regex, "", "JSFile", false, false, false};
        tableModel.addRow(data);
    }

    /**
     * 处理删除按钮点击事件
     * 
     * <p>删除当前选中的配置行，如果该配置使用WebSocket模式，
     * 则同时删除对应的WebSocket服务器连接。
     */
    private void handleDeleteButton() {
        int selectedRow = jTable.getSelectedRow();
        if (jTable.isEditing()) {
            jTable.getCellEditor().stopCellEditing();
        }

        if (selectedRow >= 0) {
            if ("WebSocket".equals(tableModel.getValueAt(selectedRow, ConfigIndex.MODE))) {
                String target = tableModel.getValueAt(selectedRow, ConfigIndex.ENDE_CONFIG).toString();
                autoUtil.deleteWSServer(target);
            }
            jTable.clearSelection();
            tableModel.removeRow(selectedRow);
        }
    }

    /**
     * 处理正则表达式匹配按钮点击事件
     * 
     * <p>使用输入的正则表达式在测试文本中进行匹配，
     * 并将匹配结果显示在结果文本区域中。
     * 
     * <p>如果勾选了"保留前后缀"复选框，则显示完整的匹配结果；
     * 否则只显示正则表达式的第一个捕获组。
     */
    private void handleRegexButton() {
        String regexTest = regexTextField.getText();
        if (regexTest != null && regexTest.length() > 0) {
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

    /**
     * 处理表格模型变化事件
     * 
     * <p>当表格数据发生变化时，更新jsList配置数据：
     * <ul>
     *   <li>只收集已启用的配置（状态为true）</li>
     *   <li>将布尔值转换为字符串"1"或"0"</li>
     *   <li>对于WebSocket模式，自动管理服务器连接</li>
     * </ul>
     */
    private void handleTableChanged() {
        jsList.clear();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if (Boolean.TRUE.equals(tableModel.getValueAt(i, ConfigIndex.ENABLED))) {
                jsList.add(new String[]{
                        tableModel.getValueAt(i, ConfigIndex.HOST).toString(),
                        tableModel.getValueAt(i, ConfigIndex.PATH).toString(),
                        tableModel.getValueAt(i, ConfigIndex.REGEX).toString(),
                        tableModel.getValueAt(i, ConfigIndex.ENDE_CONFIG).toString(),
                        tableModel.getValueAt(i, ConfigIndex.MODE).toString(),
                        Boolean.TRUE.equals(tableModel.getValueAt(i, ConfigIndex.KEEP_PREFIX_SUFFIX)) ? "1" : "0",
                        Boolean.TRUE.equals(tableModel.getValueAt(i, ConfigIndex.PROCESS_RESPONSE)) ? "1" : "0",
                        Boolean.TRUE.equals(tableModel.getValueAt(i, ConfigIndex.AGGREGATE)) ? "1" : "0",
                });
                if ("WebSocket".equals(tableModel.getValueAt(i, ConfigIndex.MODE))) {
                    String target = tableModel.getValueAt(i, ConfigIndex.ENDE_CONFIG).toString();
                    autoUtil.addWSServer(target);
                }
            } else {
                if ("WebSocket".equals(tableModel.getValueAt(i, ConfigIndex.MODE))) {
                    autoUtil.deleteWSServer(tableModel.getValueAt(i, ConfigIndex.ENDE_CONFIG).toString());
                }
            }
        }
        jTable.clearSelection();
        jTable.repaint();
    }

    /**
     * 根据主机和路径查找匹配的配置
     * 
     * <p>查找所有与给定主机和路径匹配的已启用配置。
     * 路径匹配使用startsWith，即配置的路径是请求路径的前缀。
     * 
     * @param host 目标主机
     * @param path 目标路径
     * @return 匹配的配置列表，如果没有匹配则返回空列表
     */
    public ArrayList<String[]> findConf(String host, String path) {
        ArrayList<String[]> result = new ArrayList<>();

        if (host != null && !host.isEmpty() && path != null && !path.isEmpty()) {
            for (int i = 0; i < jsList.size(); i++) {
                if (host.equals(jsList.get(i)[ConfigIndex.JS_LIST_HOST]) && 
                    path.startsWith(jsList.get(i)[ConfigIndex.JS_LIST_PATH])) {
                    result.add(jsList.get(i));
                }
            }
        }
        return result;
    }

    /**
     * 添加配置到表格
     * 
     * <p>使用给定的主机和路径创建一个新的配置行，
     * 其他字段使用默认值。
     * 
     * @param host 目标主机
     * @param path 目标路径
     */
    public void addList(String host, String path) {
        Object[] data = {false, host, path, regex, "", "JSFile", false, false, false};
        tableModel.addRow(data);
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
        panel1.add(scrollpane, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        jTable.setAutoCreateColumnsFromModel(false);
        jTable.setAutoResizeMode(1);
        scrollpane.setViewportView(jTable);
        final javax.swing.JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel2, new GridConstraints(1, 0, 2, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
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
        String[] columnNames = {"状态", "Host", "Path", "正则表达式", "JSFile/API/WebSocket", "调用模式", "保留前后缀", "处理响应体","参数聚合"};
        // 创建默认的表格模型
        tableModel = new DefaultTableModel(null, columnNames);
        jTable = new JTable(tableModel);
        // 设置 Enabled 列的渲染器和编辑器
        jTable.getColumnModel().getColumn(0).setCellRenderer(jTable.getDefaultRenderer(Boolean.class));
        jTable.getColumnModel().getColumn(0).setCellEditor(jTable.getDefaultEditor(Boolean.class));
        JComboBox<String> comboBox = new JComboBox<>(new String[]{"JSFile", "API", "WebSocket"});
        jTable.getColumnModel().getColumn(5).setCellEditor(new DefaultCellEditor(comboBox));
        jTable.getColumnModel().getColumn(6).setCellRenderer(jTable.getDefaultRenderer(Boolean.class));
        jTable.getColumnModel().getColumn(6).setCellEditor(jTable.getDefaultEditor(Boolean.class));
        jTable.getColumnModel().getColumn(7).setCellRenderer(jTable.getDefaultRenderer(Boolean.class));
        jTable.getColumnModel().getColumn(7).setCellEditor(jTable.getDefaultEditor(Boolean.class));
        jTable.getColumnModel().getColumn(8).setCellRenderer(jTable.getDefaultRenderer(Boolean.class));
        jTable.getColumnModel().getColumn(8).setCellEditor(jTable.getDefaultEditor(Boolean.class));
        // 设置默认列宽
        jTable.getColumnModel().getColumn(0).setMaxWidth(75);
        jTable.getColumnModel().getColumn(5).setMaxWidth(75);
        jTable.getColumnModel().getColumn(6).setMaxWidth(75);
        jTable.getColumnModel().getColumn(7).setMaxWidth(75);
        jTable.getColumnModel().getColumn(8).setMaxWidth(75);

    }
}
