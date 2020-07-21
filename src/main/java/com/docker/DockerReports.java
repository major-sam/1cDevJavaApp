package com.docker;

import org.jasypt.util.text.StrongTextEncryptor;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DockerReports extends JFrame{

    private final DefaultListModel<String> listModel = new DefaultListModel<>();
    private JScrollPane dbScroll;
    private JList<String> dbList;
    private JButton sendButton;
    private JTextArea dbComment;
    private JButton removeDBButton;
    private JPanel mainframe;
    private JLabel dbInfo;
    private JButton getDescription;
    private JCheckBox SendCopy;
    private JTextField mailAddresArr;
    static Map<String ,String> info = new HashMap<>();

    public static Map<String, String> getInfo() {
        return info;
    }

    public DockerReports() throws IOException {
        createGUI();
    }

    private void renewAndSendMail(JPanel panel, JTable table, String dev_server, String path_to_1c,
                                  String[] ver, String[] innerVer, Set<String> dirty_hack, Set<String> mailArr )
            throws IOException, MessagingException {
        String name = "";
        String password = "";
        System.out.println("change comments");
        JPanel pwPanel = new JPanel(new BorderLayout(5, 7));
        JPanel labels = new JPanel(new GridLayout(0, 1, 2, 2));
        labels.add(new JLabel("Логин:", SwingConstants.RIGHT));
        labels.add(new JLabel("Пароль:", SwingConstants.RIGHT));
        pwPanel.add(labels, BorderLayout.WEST);
        JPanel fields = new JPanel(new GridLayout(0, 1, 2, 2));
        TextFieldWithPrompt username = new TextFieldWithPrompt();
        JPasswordField pass = new JPasswordField(10);
        fields.add(username);
        fields.add(pass);
        pwPanel.add(fields, BorderLayout.CENTER);
        String[] opt = new String[]{"OK", "Отмена"};
        for (int row = 0; row < table.getRowCount(); row++) {
            System.out.println("run update description\n" +
                    "func param a = " + table.getValueAt(row, 0) +
                    "func param b ? is \"removed\" continue = " + table.getValueAt(row, 1));
            //dirty hack for ssvr
            if (dirty_hack.contains(table.getValueAt(row, 0).toString())){
                innerVer[0] = "8.3.12.1790";
            }
            else {
                innerVer[0] = ver[0];
            }
            ArrayList<String> description = null;
            try {
                description = Docker1C.get_infobase_description(dev_server, innerVer[0], path_to_1c, table.getValueAt(row, 0).toString());
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
            assert description != null;
            String dbComm="";
            if (description.size() >= 3){
                dbComm=description.get(3).split(":")[1].replace("\"", "");
            }
            if (!table.getValueAt(row, 1).toString().equals(dbComm)){
                int op = JOptionPane.showOptionDialog(panel, pwPanel,table.getValueAt(row, 0).toString() ,
                        JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
                        null, opt, opt[0]);
                if (op == 0) // pressing OK button
                {
                    name = username.getText();
                    password = String.valueOf(pass.getPassword());
                }
                try {
                    if (!table.getValueAt(row, 1).toString().equals("removed")) {
                        Docker1C.set_infobase_description_with_pass_65001(dev_server, innerVer[0], path_to_1c,
                                table.getValueAt(row, 0).toString(), table.getValueAt(row, 1).toString(),name,password);
                    }
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        }
        sendMail(table, dev_server, mailArr);
        System.exit(0);
    }
    private String getHTMLMail(JTable table, String server) throws UnknownHostException {
        System.out.println(table.getColumnName(0) + ":" + table.getColumnName(1));
        StringBuilder sb = new StringBuilder();
        sb.append("<html><head><title>Отчет</title></head><body><h1>Отчет по использованию сервера ").append(server).append("</h1><h2>Пользователь: ")
                .append(System.getProperty("user.name")).append("<br>Компьютер: ")
                .append(InetAddress.getLocalHost().getHostName())
                .append("</h2><table border=\"1\" cellspacing=\"0\" cellpadding=\"10\">");
        sb.append("<tr>" + "<th>").append(table.getColumnName(0)).append("</th>").append("<th>")
                .append(table.getColumnName(1)).append("</th></tr>");
        for (int row = 0; row < table.getRowCount(); row++) {
            System.out.println(table.getValueAt(row, 0) + ":" + table.getValueAt(row, 1));
            sb.append("<tr><td>").append(table.getValueAt(row, 0)).append("</td><td>")
                    .append(table.getValueAt(row, 1)).append("</td></tr>");
        }
        sb.append("</table></body></html>");
        return sb.toString();
    }
    private void sendMail(JTable table, String server, Set<String> mailArr) throws IOException, MessagingException {
        String htmlMail = getHTMLMail(table, server);
        Properties props = new Properties();
        props.put("mail.smtp.host", "mail.kzgroup.ru");
        props.put("mail.smtp.port", "25");
        props.put("mail.debug", "true");
        Session session = Session.getDefaultInstance(props);
        MimeMessage message = new MimeMessage(session);
        String admin_mail = Docker.get_property(Docker.default_property,"admin_mail",null)[0];
        String email_internal_domain = Docker.get_property(Docker.default_property,"email_internal_domain",null)[0];
        message.setFrom(new InternetAddress("dc-1c-dev-report@kzgroup.ru"));
        if (mailArr != null) {
            for (String mail : mailArr) {
                if (mail.endsWith("@"+email_internal_domain)){
                    message.addRecipient(Message.RecipientType.TO, new InternetAddress(mail));}
            }
        }
        message.addRecipient(Message.RecipientType.TO, new InternetAddress(admin_mail));
        message.setSubject("Report");
        MimeBodyPart mimeBodyPart = new MimeBodyPart();
        mimeBodyPart.setContent(htmlMail,"text/html;charset=UTF-8");
        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(mimeBodyPart);
        message.setContent(multipart);
        message.setSentDate(new Date());
        Transport.send(message);
        setProp();
        System.exit(0);
    }
    private void setProp() throws IOException {
        String local_property = Docker.get_property(Docker.default_property,"local.property",null)[0];
        SimpleDateFormat dateFormat=new SimpleDateFormat("dd/MM/yyyy");
        String lastReportStr = dateFormat.format(new Date());
        FileInputStream in = new FileInputStream(local_property);
        Properties properties = new Properties();
        properties.load(in);
        in.close();
        properties.setProperty("last_report", lastReportStr);
        properties.store(new FileOutputStream(local_property), "\n last report date");
    }

    private void createGUI() throws IOException {
        String crypt_name = Docker.get_property(Docker.default_property,"user",null)[0];
        String crypt_password = Docker.get_property(Docker.default_property,"password",null)[0];
        try {
            Docker.check_password_prop(crypt_name,crypt_password);
        } catch (IOException e) {
            e.printStackTrace();
        }
        StrongTextEncryptor textEncryptor = new StrongTextEncryptor();
        textEncryptor.setPassword("$ecurePWD");
        final String[] ver = {"8.3.16.1148"};
        final String[] innerVer = {null};
        String[] arr = {"rebiachikh_1c83n_ssvr_s2011_18-02-20_12-33-48-127"};
        Set<String> dirty_hack = new HashSet<>(Arrays.asList(arr));
        Docker.user_name = textEncryptor.decrypt(crypt_name);
        Docker.user_password = textEncryptor.decrypt(crypt_password);
        Docker.domain = "";
        final String all_user_list = Docker.get_property(Docker.default_property,"all_user_list",null)[0];
        Path  localRelativePath = Paths.get(Docker.get_property(Docker.default_property,"local.property",null)[0]);
        String local_property = localRelativePath.toAbsolutePath().toString();
        final String dev_server =  Docker.get_property(Docker.default_property,"dev_server",null)[0];
        final String path_to_1c = Docker.get_property(local_property,"path_to_1c",null)[0];
        List<String> allDb = DockerSQL.get_mssql_db_list("dc-1c-dev");
        if (allDb.size()==0){
            setProp();
        }
        else {
            for (String db : allDb) {
                if (db.toLowerCase().startsWith(System.getProperty("user.name").toLowerCase())) {
                    listModel.addElement(db);
                    info.put(db, null);
                }
            }
            dbList.setModel(listModel);
            dbList.setCellRenderer(new ListColorRendererByComment());
            dbScroll.setViewportView(dbList);
            dbComment.setSize(10, 100);
            dbComment.setLineWrap(true);
            dbComment.setWrapStyleWord(true);
            dbList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            final String[] dbName = {null};
            JFrame frame = new JFrame("Report Tool");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setMinimumSize(new Dimension(800, 600));
            frame.setPreferredSize(new Dimension(800, 600));
            frame.add(mainframe);
            frame.pack();
            frame.setVisible(true);


//listeners
            dbList.addListSelectionListener(e -> {
                getDescription.setEnabled(true);
                if (dbList.getSelectedIndex() == -1) {
                    dbList.setSelectedIndex(0);
                }
                dbName[0] = dbList.getSelectedValue();
                String userName = System.getProperty("user.name").toLowerCase();
                String[] ibShare = Docker.get_property(Docker.default_property, "share_for_1c_lists", null);
                String ibName = "Нет в списке";
                try {
                    ibName = Docker1C.find_infobase_in_list(ibShare[0] + userName + ".v8i", dbName[0]);
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
                String dbDate = null;
                Pattern p = Pattern.compile("_\\d\\d-\\d\\d-\\d\\d_");
                Matcher m = p.matcher(dbName[0]);
                if (m.find()) {
                    dbDate = m.group().replace("_", "");
                }
                dbInfo.setText("<html><div>Имя базы в списке:<br>" + ibName + "<br><hr>" +
                        "Имя базы на сервере:<br>" + dbName[0] + "<br><hr>" +
                        "Дата создания:<br>" + dbDate + "<br><div><html>");
            });
            removeDBButton.addActionListener(e -> {
                int approve;
                String infobase = dbList.getSelectedValue();
                approve = JOptionPane.showConfirmDialog(mainframe, "Удаляем базу " + infobase + " ?", "Удаление базы", JOptionPane.YES_NO_OPTION);
                if (approve == 0) {
                    try {
                        Docker1C.remove_1c_base(dev_server, infobase, path_to_1c, ver[0]);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    try {
                        Docker1C.remove_infobase_from_list(all_user_list, infobase);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    DefaultListModel<String> newListModel = new DefaultListModel<>();
                    List<String> allDb1 = DockerSQL.get_mssql_db_list("dc-1c-dev");
                    for (String db : allDb1) {
                        if (db.toLowerCase().startsWith(System.getProperty("user.name").toLowerCase())) {
                            newListModel.addElement(db);
                        }
                    }
                    info.replace(infobase, "removed");
                    dbList.setModel(newListModel);
                }
            });
            getDescription.addActionListener(e -> {
                ArrayList<String> description = null;
                if (dirty_hack.contains(dbName[0])) {
                    innerVer[0] = "8.3.12.1790";
                } else {
                    innerVer[0] = ver[0];
                }
                try {
                    description = Docker1C.get_infobase_description(dev_server, innerVer[0], path_to_1c, dbName[0]);
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
                assert description != null;
                String comment;
                if (description.get(1).startsWith("Insufficient user rights for infobase")) {
                    comment = "Не установлена авторизация по доменной учетной записи - комментарий не получить и не обновить" +
                            "на сервере 1с";
                    dbComment.setText(comment);
                } else {
                    comment = description.get(3).split(":")[1].replace("\"", "");
                    dbComment.setText(comment);
                    if (comment.startsWith("Комментарий,")) {
                        dbComment.setText(comment + " Комментрии установленные по умолчанию не принимаются");
                    } else
                        info.replace(dbList.getSelectedValue(), comment);
                }
            });
            dbComment.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent documentEvent) {
                    info.replace(dbList.getSelectedValue(), dbComment.getText());
                }

                @Override
                public void removeUpdate(DocumentEvent documentEvent) {
                    info.replace(dbList.getSelectedValue(), dbComment.getText());
                }

                @Override
                public void changedUpdate(DocumentEvent documentEvent) {
                    info.replace(dbList.getSelectedValue(), dbComment.getText());
                }
            });
            sendButton.addActionListener(e -> {
                Set<String> mailArr = null;
                if (SendCopy.isSelected()) {
                    mailArr = new HashSet<>(Arrays.asList(mailAddresArr.getText().split(",")));
                }
                AtomicBoolean flagSender = new AtomicBoolean(true);
                List<String> noCommentsBase = new ArrayList<>();
                info.forEach((key, value) -> {
                    System.out.println(key + " " + value);
                    if (value == null) {
                        flagSender.set(false);
                        noCommentsBase.add(key);
                    }
                });
                if (!flagSender.get()) {
                    JOptionPane.showMessageDialog(mainframe, new JList<>(noCommentsBase.toArray()), "Нет информации по базам", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    DefaultTableModel tableModel = new DefaultTableModel() {
                        @Override
                        public boolean isCellEditable(int row, int col) {
                            return col != 0;
                        }
                    };
                    tableModel.getDataVector().removeAllElements();
                    tableModel.addColumn("База");
                    tableModel.addColumn("Коментарий");
                    for (Map.Entry<?, ?> entry : info.entrySet()) {
                        tableModel.addRow(new Object[]{entry.getKey(), entry.getValue()});
                    }
                    JTable table = new JTable(tableModel);
                    JScrollPane jScrollPane = new JScrollPane(table);
                    jScrollPane.setPreferredSize(new Dimension(300, 200));
                    JPanel panel = new JPanel();
                    panel.add(jScrollPane);
                    Object[] options = {"Отправить отчет", "Отправить отчет и Обновить коментарии на сервере"};
                    int result = JOptionPane.showOptionDialog(mainframe, jScrollPane, "для сохранения надо встать на следующую строку", JOptionPane.YES_NO_OPTION,
                            JOptionPane.PLAIN_MESSAGE, null, options, null);
                    if (result == JOptionPane.YES_NO_OPTION) {
                        System.out.println("send mail");
                        try {
                            sendMail(table, dev_server, mailArr);
                        } catch (MessagingException | IOException unknownHostException) {
                            unknownHostException.printStackTrace();
                        }
                    } else if (result == JOptionPane.NO_OPTION) {
                        try {
                            renewAndSendMail(panel, table, dev_server, path_to_1c, ver, innerVer, dirty_hack, mailArr);
                        } catch (MessagingException | IOException unknownHostException) {
                            unknownHostException.printStackTrace();
                        }
                    }
                }
            });
            SendCopy.addActionListener(e ->
                    mailAddresArr.setEnabled(SendCopy.isSelected())
            );
        }
    }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                new DockerReports();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}