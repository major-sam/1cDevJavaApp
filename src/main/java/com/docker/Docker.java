package com.docker;

import org.jasypt.util.text.StrongTextEncryptor;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;
import javax.swing.filechooser.FileView;

public class Docker {
    private JButton backup_restore_db_button, quit, useMyBackupButton, run1cButton, run1cDesignerButton, getDtButton,
            getCfButton, run1cAdminConsoleButton, run1cWithParamsButton, getSqlBakButton, removeDbButton, openLogsButton
            ,dev_base_renew, params_help;
    private JLabel source_base_size, wmi_space, source_base_creation_date,target_base_creation_date,target_base_size;
    private JProgressBar progressbar1, progressbar2, restore_my_bak_progress_bar, get_backup_progress_bar;
    private JPanel main;
    private JList  target_list, source_list;
    private JScrollPane source_scroll, target_scroll;
    private JTextField source_search, target_search, add_new_infobase_comment, add_new_infobase_alias_name,
            run_1c_custom_params;
    public  JComboBox server_list, server_1c_ver, dev_prod_switch;
    private JCheckBox my_bases_only_check_box, create_new_db_check_box;
    private List source_buffer, target_buffer;
    static String user_name, user_password;
    private Integer backup_progress, restore_progress;
    private static Path currentRelativePath = Paths.get("conf/default.properties");
    static String default_property = currentRelativePath.toAbsolutePath().toString();
    private static Path  localRelativePath = Paths.get(get_property(default_property,"local.property",null)[0]);
    static String local_property = localRelativePath.toAbsolutePath().toString();
    static String path_1c_exe = "\\bin\\1cv8.exe";
    static String path_rac_exe = "\\bin\\rac.exe";
    static String path_to_1c = get_property(default_property,"path_to_1c",null)[0];

    static String[] get_property(String property_file, String property_name, String splitter){
        String[] result = new String[0];
        FileInputStream fis;
        Properties prop = new Properties();
        try {
            fis = new FileInputStream(property_file);
            prop.load(fis);
            if (splitter == null){
                result = new String[]{prop.getProperty(property_name)};
            }
            else {
                result = prop.getProperty(property_name).split(splitter);
            }
        } catch (IOException e){
            e.printStackTrace();
        }
        return result;
    }
    private void enable_ui(boolean state){
        create_new_db_check_box.setEnabled(state);
        run1cAdminConsoleButton.setEnabled(state);
        run1cDesignerButton.setEnabled(state);
        run1cWithParamsButton.setEnabled(state);
        run_1c_custom_params.setEnabled(state);
        server_1c_ver.setEnabled(state);
        run1cButton.setEnabled(state);
        getCfButton.setEnabled(state);
        getDtButton.setEnabled(state);
        getSqlBakButton.setEnabled(state);
        removeDbButton.setEnabled(state);
        useMyBackupButton.setEnabled(state);
        server_list.setEnabled(state);
        source_list.setEnabled(state);
        target_list.setEnabled(state);
        backup_restore_db_button.setEnabled(state);
        quit.setEnabled(state);
        source_search.setEnabled(state);
        target_search.setEnabled(state);
        add_new_infobase_comment.setEnabled(state);
        add_new_infobase_alias_name.setEnabled(state);
        my_bases_only_check_box.setEnabled(state);
        server_1c_ver.setEnabled(state);
        add_new_infobase_alias_name.setEnabled(state);
        add_new_infobase_comment.setEnabled(state);
    }
    private void enable_ui_target(boolean state){
        target_list.setEnabled(!state);
        target_search.setEnabled(!state);
        my_bases_only_check_box.setEnabled(!state);
        add_new_infobase_comment.setEnabled(state);
        add_new_infobase_alias_name.setEnabled(state);
        server_1c_ver.setEnabled(state);
    }
    private void enable_ui_prod_dev(String state){
        if (state.equals("dev")){
            enable_ui(true);
        }
        else if (state.equals("prod")){
            enable_ui(false);
            source_list.setEnabled(true);
            server_list.setEnabled(true);
            dev_prod_switch.setEnabled(true);
            run1cButton.setEnabled(true);
            run_1c_custom_params.setEnabled(true);
            run1cWithParamsButton.setEnabled(true);
            run1cAdminConsoleButton.setEnabled(true);
            run1cDesignerButton.setEnabled(true);
            quit.setEnabled(true);
        }
    }
    private void wright_log(String  date, String selected_server, String source_base_name ,String target_base_name){
        //TODO log_file_path must be in property file
        String log_file_path = "\\\\90500-ws108\\log$\\app.log";
        try {
            FileWriter fileWriter  = new FileWriter(log_file_path,true);
            PrintWriter log = new PrintWriter(fileWriter);
            log.println(date+" "+System.getProperty("user.name")+" "+InetAddress.getLocalHost().getHostName()+
                    " "+selected_server+" From: \t"+source_base_name+" To: \t"+target_base_name);
            log.close();
        }
        catch (IOException e){
            System.out.println("file not found");
        }

    }
    private void backup_db(final String selected_server, final String source_base_name, final String backup_name,
                           final String target_base_name , final String dev_server,
                           final String backup_dir, final String server_1c){
        enable_ui(false);
        progressbar1.setMinimum(0);
        progressbar1.setMaximum(100);
        progressbar1.setValue(0);
        progressbar2.setMinimum(0);
        progressbar2.setMaximum(100);
        progressbar2.setValue(0);
        final boolean[] bak_thread_status = {false};
        Thread backup_db = new Thread(new Runnable()  {
            @Override
            public void run() {
                try {
                    Connection conn;
                    Statement stmt;
                    String query="SET NOCOUNT ON "+
                            "BACKUP DATABASE ["+source_base_name+"] TO  DISK = N'"+backup_name+".bak' WITH RETAINDAYS = 3, NOFORMAT, INIT,  NAME = N'"+
                            source_base_name+"-Full Database Backup', SKIP, NOREWIND, NOUNLOAD,  STATS = 10";
                    String url ="jdbc:sqlserver://"+selected_server+";user="+user_name+";password="+user_password+"";
                    conn = DriverManager.getConnection(url);
                    stmt = conn.createStatement();
                    stmt.executeQuery(query);
                    if (stmt.execute(query)) {
                        stmt.getResultSet();
                    }
                }
                catch (SQLException ex){
                    System.out.println("SQLException: "+ex.getMessage());
                    System.out.println("SQLState: "+ex.getSQLState());
                    System.out.println("VendorError: "+ex.getErrorCode());
                }
                bak_thread_status[0] = true;
            }
        });
        ScheduledExecutorService scheduler_bak = Executors.newSingleThreadScheduledExecutor();
        scheduler_bak.schedule(backup_db, 1, TimeUnit.SECONDS);
        final ScheduledExecutorService scheduled_bak_watch = Executors.newSingleThreadScheduledExecutor();
        scheduled_bak_watch.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if (bak_thread_status[0]) {
                    String ver =(String) server_1c_ver.getSelectedItem();
                    Date updated_description = new Date();
                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-YYYY HH-mm");
                    String description = add_new_infobase_comment.getText()+","+dateFormat.format(updated_description);
                    progressbar1.setValue(100);
                    if (create_new_db_check_box.isSelected()){
                        try {
                            String infobase_name = add_new_infobase_alias_name.getText();
                            Docker1C.create_1c_base(server_1c, dev_server, ver, backup_name, description);
                            Docker1C.add_infobase_to_list(server_1c,ver,backup_name,infobase_name);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        restore_db(selected_server, dev_server, backup_name, backup_name, backup_dir,progressbar2);
                    }
                    else {
                        restore_db(selected_server, dev_server, target_base_name, backup_name, backup_dir,progressbar2);
                        try {
                            description ="Обновлено базой "+ backup_name+" пользователем "+
                                    System.getProperty("user.name")+","+dateFormat.format(updated_description);
                            Docker1C.set_infobase_description(server_1c,ver,path_to_1c,target_base_name,description);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    Thread.currentThread().interrupt();
                    scheduled_bak_watch.shutdown();

                }
                else {
                    String query_salt ="'BACKUP%'";
                    backup_progress = DockerSQL.get_mssql_progress(query_salt, selected_server, backup_name);
                    progressbar1.setValue(backup_progress);
                }
            }
        },0,2, TimeUnit.SECONDS);
    }
    private void restore_db(final String source_server, final String target_server, final String target_base,
                            final String backup_name, final String backup_sh_folder, final JProgressBar bar) {
        String get_data_path= "select  SERVERPROPERTY('InstanceDefaultDataPath') as a",
                get_log_path= "select  serverproperty('InstanceDefaultLogPath') as a";
        final String data_path = DockerSQL.get_mssql_path(get_data_path, target_server);
        final String log_path = DockerSQL.get_mssql_path(get_log_path, target_server);
        final boolean[] res_thread_status = {false};
        String path = "";
        if (!source_server.equals(target_server)) {
            path = "\\\\" + source_server + "\\" + backup_sh_folder + "\\";
        }
        final String finalPath = path;
        Thread restore_db = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String logic_source_base= null ,logic_source_base_log=null;
                    Connection conn;
                    Statement stmt;
                    ResultSet rs;
                    String url ="jdbc:sqlserver://"+target_server+";user="+user_name+";password="+user_password+"";
                    conn = DriverManager.getConnection(url);
                    stmt = conn.createStatement();
                    String query= "RESTORE FILELISTONLY FROM DISK='"+ finalPath +backup_name+".bak' ";
                    rs = stmt.executeQuery(query);
                    if (stmt.execute(query)) {
                        rs = stmt.getResultSet();
                    }
                    while (rs.next()){
                        String type=rs.getString("Type"),
                                name=rs.getString("LogicalName");

                        if (type.matches(".*D.*"))
                        {
                            logic_source_base = name;
                        }
                        if (type.matches(".*L.*"))
                        {
                            logic_source_base_log = name;
                        }
                    }
                    String query_res;
                    if (create_new_db_check_box.isSelected()) {
                        query_res = "USE [master]\n"+
                                "RESTORE DATABASE ["+backup_name+"] FROM DISK = N'"+ finalPath +backup_name+".bak' WITH FILE = 1,"+
                                "MOVE N'"+logic_source_base+"' TO N'"+data_path+backup_name+".mdf', "+
                                "MOVE N'"+logic_source_base_log+"' TO N'"+log_path+backup_name+"_log.ldf',"+
                                "NOUNLOAD,  REPLACE,  STATS = 5\n";
                    }
                    else {
                        query_res = "USE [master]\n"+
                                "ALTER DATABASE ["+target_base+"] SET SINGLE_USER WITH ROLLBACK IMMEDIATE\n"+
                                "RESTORE DATABASE ["+target_base+"] FROM DISK = N'"+ finalPath +backup_name+".bak' WITH FILE = 1,"+
                                "MOVE N'"+logic_source_base+"' TO N'"+data_path+target_base+".mdf', "+
                                "MOVE N'"+logic_source_base_log+"' TO N'"+log_path+target_base+"_log.ldf',"+
                                "NOUNLOAD,  REPLACE,  STATS = 5\n"+
                                "ALTER DATABASE ["+target_base+"] SET MULTI_USER";

                    }
                    conn = DriverManager.getConnection(url);
                    stmt = conn.createStatement();
                    stmt.executeQuery(query_res);
                    if (stmt.execute(query_res)) {
                        stmt.getResultSet();
                    }
                }
                catch (SQLException ex){
                    System.out.println("SQLException: "+ex.getMessage());
                    System.out.println("SQLState: "+ex.getSQLState());
                    System.out.println("VendorError: "+ex.getErrorCode());
                }
                res_thread_status[0] = true;
            }
        });
        ScheduledExecutorService scheduler_res = Executors.newSingleThreadScheduledExecutor();
        scheduler_res.schedule(restore_db, 1, TimeUnit.SECONDS);
        final ScheduledExecutorService scheduled_res_watch = Executors.newSingleThreadScheduledExecutor();
        scheduled_res_watch.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if (res_thread_status[0]) {
                    Thread.currentThread().interrupt();
                    List<String> list;
                    list = DockerSQL.get_mssql_db_list(target_server);
                    final DefaultListModel tdb = new DefaultListModel();
                    for (String base: list) {
                        tdb.addElement(base);
                    }
                    target_buffer = get_items_for_filter(tdb);
                    target_list.setModel(tdb);
                    target_list.setCellRenderer(new ListColorRenderer());
                    target_list.setSelectedIndex(0);
                    bar.setValue(100);
                    DockerSQL.remove_backup(source_server,backup_name);
                    enable_ui(true);
                    create_new_db_check_box.setSelected(false);
                    scheduled_res_watch.shutdown();
                }
                else  {
                    String query_salt = "'RESTORE%'";
                    restore_progress = DockerSQL.get_mssql_progress(query_salt, target_server, backup_name);
                    bar.setValue(restore_progress);
                }
            }
        },0,2, TimeUnit.SECONDS);
    }
    private List get_items_for_filter(DefaultListModel model) {
        List list = new ArrayList();
        for (int i = 0; i < model.size(); i++) {
            list.add(model.elementAt(i).toString());
        }
        return list;
    }
    private void check_password_prop(String crypt_name, String crypt_password) throws IOException {
        StrongTextEncryptor textEncryptor = new StrongTextEncryptor();
        textEncryptor.setPassword("$ecurePWD");
        while (true) {
            if (crypt_name == null | crypt_password == null) {
                JPanel panel = new JPanel(new BorderLayout(5, 7));
                JPanel labels = new JPanel(new GridLayout(0, 1, 2, 2));
                labels.add(new JLabel("Login:", SwingConstants.RIGHT));
                labels.add(new JLabel("Pass:", SwingConstants.RIGHT));
                labels.add(new JLabel("Confirm:", SwingConstants.RIGHT));
                panel.add(labels, BorderLayout.WEST);
                JPanel fields = new JPanel(new GridLayout(0, 1, 2, 2));
                JTextField username = new JTextField(textEncryptor.decrypt(crypt_name));
                JPasswordField pass = new JPasswordField(10);
                JPasswordField pass_confirm = new JPasswordField(10);
                fields.add(username);
                fields.add(pass);
                fields.add(pass_confirm);
                panel.add(fields, BorderLayout.CENTER);
                String[] options = new String[]{"OK", "Cancel"};
                int option = JOptionPane.showOptionDialog(null, panel, "Enter Credentials For MSSQL Server",
                        JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
                        null, options, options[0]);
                if (option == 0) // pressing OK button
                {
                    String name = username.getText();
                    String password = new String( pass.getPassword());
                    String confirm = new String( pass_confirm.getPassword());
                    if (!password.equals(confirm) | password.isEmpty()){
                        JOptionPane  bad_confirm = new JOptionPane ("Bad password confirm or empty password", JOptionPane.ERROR_MESSAGE);
                        JDialog dialog = bad_confirm.createDialog("Confirm Not Match");
                        dialog.setAlwaysOnTop(true);
                        dialog.setVisible(true);
                    }
                    else {
                        Properties prop = new Properties();
                        crypt_name = textEncryptor.encrypt(name);
                        crypt_password = textEncryptor.encrypt(password);
                        prop.setProperty("user",crypt_name);
                        prop.setProperty("password",crypt_password);
                        String conf_path = ".\\conf\\default.properties";
                        prop.store(new FileOutputStream(conf_path,true), "\nremove lines if password changes or wrong");
                        System.out.println("Your password is: "+password+"\nYour password confirm is: "+confirm);
                        break;
                    }
                }
                else {
                    System.exit(0);
                }
            }
            else {
                break;
            }
        }

    }
    private void get_backup(final String selected_server, final String source_base_name, final String backup_name){
        final boolean[] bak_thread_status = {false};
        Thread backup_db = new Thread(new Runnable()  {
            @Override
            public void run() {

                get_backup_progress_bar.setMinimum(0);
                get_backup_progress_bar.setMaximum(100);
                get_backup_progress_bar.setValue(0);
                try {
                    Connection conn;
                    Statement stmt;
                    String query="SET NOCOUNT ON "+
                            "BACKUP DATABASE ["+source_base_name+"] TO  DISK = N'"+backup_name+".bak' WITH RETAINDAYS = 3, NOFORMAT, INIT,  NAME = N'"+
                            source_base_name+"-Full Database Backup', SKIP, NOREWIND, NOUNLOAD,  STATS = 10";
                    String url ="jdbc:sqlserver://"+selected_server+";user="+user_name+";password="+user_password+"";
                    conn = DriverManager.getConnection(url);
                    stmt = conn.createStatement();
                    stmt.executeQuery(query);
                    if (stmt.execute(query)) {
                        stmt.getResultSet();
                    }
                }
                catch (SQLException ex){
                    System.out.println("SQLException: "+ex.getMessage());
                    System.out.println("SQLState: "+ex.getSQLState());
                    System.out.println("VendorError: "+ex.getErrorCode());
                }
                bak_thread_status[0] = true;
            }
        });
        ScheduledExecutorService scheduler_bak = Executors.newSingleThreadScheduledExecutor();
        scheduler_bak.schedule(backup_db, 1, TimeUnit.SECONDS);
        final ScheduledExecutorService scheduled_bak_watch = Executors.newSingleThreadScheduledExecutor();
        scheduled_bak_watch.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if (bak_thread_status[0]) {
                    get_backup_progress_bar.setValue(100);
                    JOptionPane.showConfirmDialog(main,
                            "Backup done", "Done", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE);
                    Thread.currentThread().interrupt();
                    scheduled_bak_watch.shutdown();
                }
                else {
                    String query_salt ="'BACKUP%'";
                    int backup_progress = DockerSQL.get_mssql_progress(query_salt, selected_server, backup_name);
                    get_backup_progress_bar.setValue(backup_progress);
                }
            }
        },0,2, TimeUnit.SECONDS);
    }
    private Docker() throws IOException {
        String crypt_name = get_property(default_property,"user",null)[0];
        String crypt_password = get_property(default_property,"password",null)[0];
        check_password_prop(crypt_name,crypt_password);
        StrongTextEncryptor textEncryptor = new StrongTextEncryptor();
        textEncryptor.setPassword("$ecurePWD");
        user_name = textEncryptor.decrypt(crypt_name);
        user_password = textEncryptor.decrypt(crypt_password);
        String[] mssql_servers = get_property(default_property,"servers",",");
        final String dev_server =  get_property(default_property,"dev_server",null)[0];
        final String backup_dir =  get_property(default_property,"bak_dir_name",null)[0];
        final String server_1c = get_property(default_property,"1c_server",null)[0];
        final String admin_account = get_property(default_property,"admin_account",null)[0];
        final String[] server_1c_ver_with_port =get_property(default_property,"server_1c_ver_with_ras",",");
        final List<List<String>> base_lists = new ArrayList<>();
        final Map<String, Integer> base_dictionary = new HashMap<>();
        Integer id = 0;
        DefaultComboBoxModel s_model = new DefaultComboBoxModel();
        DefaultComboBoxModel c_model = new DefaultComboBoxModel();
        for(String srv: mssql_servers){
            base_dictionary.put(srv,id);
            id++;
            s_model.addElement(srv);
            List<String> list;
            list = DockerSQL.get_mssql_db_list(srv);
            base_lists.add(list);
        }
        for(String c: server_1c_ver_with_port){
            String[] server_1c_ver = c.split(":");
            c_model.addElement(server_1c_ver[0]);
        }
//TODO    ALL LIST STILL IN DEV
//        s_model.addElement("ALL");
//        base_dictionary.put("ALL", id);
//        reverse_base_dictionary.put(id,"ALL");
//        List<String> all_list =  new ArrayList<String>();
//        for (List<String> base_list : base_lists) {
//            all_list.addAll(base_list);
//        }
//        base_lists.add(all_list);
        server_list.setModel(s_model);
        server_1c_ver.setModel(c_model);
        final String[] selected_server = {mssql_servers[0]};
        source_scroll.setViewportView(source_list);
        target_scroll.setViewportView(target_list);
        final DefaultListModel sdb  = new DefaultListModel();
        final DefaultListModel tdb = new DefaultListModel();
        List<String> bases_on_server = base_lists.get(base_dictionary.get(selected_server[0]));
        List<String> bases_on_dev_server = base_lists.get(base_dictionary.get(dev_server));
        for (String base: bases_on_server) {
            sdb.addElement(base);
        }
        for (String base: bases_on_dev_server) {
            tdb.addElement(base);
        }
        source_buffer = get_items_for_filter(sdb);
        target_buffer = get_items_for_filter(tdb);
        source_list.setModel(sdb);
        source_list.setSelectedIndex(0);
        target_list.setModel(tdb);
        target_list.setCellRenderer(new ListColorRenderer());
        final String[] source_base_name = {null};
        final String[] target_base_name = {null};
        source_base_name[0] = (String) source_list.getSelectedValue();
        target_base_name[0] = (String) target_list.getSelectedValue();
        String source_db_size= DockerSQL.get_mssql_db_size(source_base_name[0], selected_server[0]);
        String source_creation_date= DockerSQL.get_mssql_db_creation_date(source_base_name[0], selected_server[0]);
        source_base_size.setText(source_db_size+" MB");
        source_base_creation_date.setText(source_creation_date);
        String target_db_size= DockerSQL.get_mssql_db_size(target_base_name[0], dev_server);
        String target_creation_date= DockerSQL.get_mssql_db_creation_date(target_base_name[0], dev_server);
        target_base_size.setText(target_db_size+" MB");
        target_base_creation_date.setText(target_creation_date);
        boolean test_admin = System.getProperty("user.name").equals(admin_account);
        boolean test_owner = false;
        if  (target_base_name[0]!=null){
            test_owner = target_base_name[0].contains(System.getProperty("user.name"));
        }
        if (!test_admin & !test_owner){
            removeDbButton.setEnabled(false);
        }
        ImageIcon img = new ImageIcon("conf/icon.png");
        ImageIcon refresh_png = new ImageIcon("conf/refresh.png");
        Image refresh_image = refresh_png.getImage();
        Image refresh_img = refresh_image.getScaledInstance(24,24,  java.awt.Image.SCALE_SMOOTH);
        dev_base_renew.setIcon(new ImageIcon(refresh_img));
        ImageIcon help_png = new ImageIcon("conf/info.png");
        Image help_image = help_png.getImage();
        Image help_img = help_image.getScaledInstance(24,24,  java.awt.Image.SCALE_SMOOTH);
        final Image h_icon = help_image.getScaledInstance(56,56,  java.awt.Image.SCALE_SMOOTH);
        final ImageIcon help_icon = new ImageIcon(h_icon);
        params_help.setIcon(new ImageIcon(help_img));
        JFrame frame = new JFrame("Docker");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setIconImage(img.getImage());
        frame.setMinimumSize(new Dimension(1000, 700));
        frame.setPreferredSize(new Dimension(1100, 700));
        frame.add(main);
        frame.pack();
        frame.setVisible(true);
// listeners
        server_list.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent itemEvent) {
                if (itemEvent.getStateChange() == ItemEvent.SELECTED){
                    selected_server[0] = (String) itemEvent.getItem();
                }
                sdb.clear();
                source_list.setModel(sdb);
                List<String> bases_on_server = base_lists.get(base_dictionary.get(selected_server[0]));
                for (String base: bases_on_server) {
                    sdb.addElement(base);
                }
                source_list.setModel(sdb);
                source_base_name[0] =(String) source_list.getSelectedValue();
                source_list.setSelectedIndex(0);
            }
        });
        String free_space_on_disk = DockerSQL.get_mssql_free_space(dev_server)[0];
        final int sql_disk =Integer.parseInt(DockerSQL.get_mssql_free_space(dev_server)[1]);
        final int bak_disk =Integer.parseInt(DockerSQL.get_mssql_free_space(dev_server)[2]);
        wmi_space.setText("<html>"+free_space_on_disk+"<html>");
        final String[] warn_message = new String[1];
        backup_restore_db_button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                final Date d = new Date();
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-YY_HH-mm-ss-S");
                final String date = dateFormat.format(d);
                source_base_name[0] = (String) source_list.getSelectedValue();
                int extra = Integer.parseInt(DockerSQL.get_mssql_db_size((String)source_list.getSelectedValue(),(String)server_list.getSelectedItem()));
                int approve = 1;
                String alias = add_new_infobase_alias_name.getText();
                String comment = add_new_infobase_comment.getText();
                if (sql_disk<extra){
                    warn_message[0] = "<html><font color=#f54242> НЕДОСТАТОЧНО МЕСТА НА ДИСКЕ БД</font>";
                     JOptionPane.showConfirmDialog(null,
                             warn_message[0], "WARNING", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
                }
                else if (bak_disk<extra){
                    warn_message[0] = "<html><font color=#f54242> НЕДОСТАТОЧНО МЕСТА НА ДИСКЕ БЭКАПОВ</font>";
                    JOptionPane.showConfirmDialog(null,
                            warn_message[0], "WARNING", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
                }
                else if (create_new_db_check_box.isSelected() &&
                        ((alias.equals("Имя базы 1с")|| alias.equals(""))
                                &&(comment.equals("Комментарий"))||comment.equals(""))){
                    warn_message[0] = "<html><font color=#f52248>При создании новой базы <br/>необходимо указать имя базы и коментарий</font>";
                    JOptionPane.showConfirmDialog(null,
                            warn_message[0], "WARNING", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
                }
                else {
                    String title, additional_string;
                    if (create_new_db_check_box.isSelected()){
                        title = "Создание новой базы";
                        additional_string = "";
                    }
                    else {
                        title = "Обновление базы";
                        additional_string = "\nВ: "+ target_base_name[0];

                    }
                    if (source_base_name[0]== null){
                        source_base_name[0] = (String) source_list.getSelectedValue();
                    }
                    approve = JOptionPane.showConfirmDialog(null,
                                title
                                +"\nСервер: \t"+selected_server[0]
                                +"\nИЗ: "+source_base_name[0]
                                +additional_string
                                +"\nДополнительное место: "+extra+" MB"
                                +"\nСвободно места на диске бд: "+sql_disk+" MB"
                                +"\nСвободно места на диске бэкапов: "+bak_disk+" MB"
                                +"\n",
                            title+" Подтверждение"
                        , JOptionPane.YES_NO_OPTION);
                }
                if(approve == 0){
                    final String backup_name = System.getProperty("user.name")+"_"+source_base_name[0]+"_"+date;
                    wright_log(date,selected_server[0],source_base_name[0],target_base_name[0]);
                    backup_db(selected_server[0], source_base_name[0], backup_name, target_base_name[0]
                            ,dev_server, backup_dir, server_1c);
                }
            }
        });
        removeDbButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    Docker1C.remove_1c_base(dev_server,target_base_name[0],(String) server_1c_ver.getSelectedItem());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        quit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                System.exit(0);
            }
        });
        source_list.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                source_base_name[0] = (String) source_list.getSelectedValue();
                String source_db_size= DockerSQL.get_mssql_db_size(source_base_name[0], selected_server[0]);
                String source_creation_date= DockerSQL.get_mssql_db_creation_date(source_base_name[0], selected_server[0]);
                source_base_size.setText(source_db_size+" MB");
                source_base_creation_date.setText(source_creation_date);
            }
        });
        target_list.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                target_base_name[0] = (String) target_list.getSelectedValue();
                String target_db_size= DockerSQL.get_mssql_db_size(target_base_name[0], dev_server);
                String target_creation_date= DockerSQL.get_mssql_db_creation_date(target_base_name[0], dev_server);
                target_base_size.setText(target_db_size+" MB");
                target_base_creation_date.setText(target_creation_date);
                boolean test_admin = System.getProperty("user.name").equals(admin_account);
                boolean test_owner = false;
                if  (target_base_name[0]!=null){
                    test_owner = target_base_name[0].contains(System.getProperty("user.name"));
                }
                if (!test_admin & !test_owner){
                    removeDbButton.setEnabled(false);
                }
            }
        });
        create_new_db_check_box.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                    enable_ui_target(create_new_db_check_box.isSelected());
            }
        });
        my_bases_only_check_box.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if(e.getStateChange() == ItemEvent.SELECTED){
                    target_search.setText(System.getProperty("user.name"));
                }
                else {
                    target_search.setText("");
                }
            }
        });
        source_search.getDocument().addDocumentListener(new DocumentListener(){
            @Override public void insertUpdate(DocumentEvent e) { filter(); }
            @Override public void removeUpdate(DocumentEvent e) { filter(); }
            @Override public void changedUpdate(DocumentEvent e) { filter();}

            private void filter() {
                DefaultListModel model = (DefaultListModel) source_list.getModel();
                model.clear();
                String s = source_search.getText();
                for (Object o : source_buffer) {
                    if (o.toString().contains(s)) {
                        model.addElement(o.toString());
                    }
                }
                source_list.setModel(model);
            }
        });
        target_search.getDocument().addDocumentListener(new DocumentListener(){
            @Override public void insertUpdate(DocumentEvent e) { filter(); }
            @Override public void removeUpdate(DocumentEvent e) { filter(); }
            @Override public void changedUpdate(DocumentEvent e) { filter();}
            private void filter() {
                DefaultListModel model1 = (DefaultListModel) target_list.getModel();
                model1.clear();
                String s = target_search.getText();
                for (Object o : target_buffer) {
                    if (o.toString().contains(s)) {
                        model1.addElement(o.toString());
                    }
                }
                target_list.setModel(model1);
                target_list.setCellRenderer(new ListColorRenderer());
            }
        });
        dev_base_renew.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
            List<String> list = DockerSQL.get_mssql_db_list(dev_server);
            tdb.clear();
            for (String base : list){
                tdb.addElement(base);
            }
            target_list.setModel(tdb);
            target_list.setSelectedIndex(0);
            target_list.setCellRenderer(new ListColorRenderer());
            }
        });
        dev_prod_switch.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent itemEvent) {
                if (itemEvent.getStateChange() == ItemEvent.SELECTED){
                    itemEvent.getItem();
                }
                String res;
                res =(String) dev_prod_switch.getSelectedItem();
                assert res != null;
                enable_ui_prod_dev(res);
            }
        });
        openLogsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    Runtime.getRuntime().exec("notepad \\\\90500-ws108\\share\\log\\app.log");
                }
                catch (IOException ex){
                    ex.printStackTrace();
                }
            }
        });
        params_help.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                Properties props = new Properties();
                FileInputStream input = null;
                try {
                    input = new FileInputStream(new File("conf/info.properties"));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                try {
                    assert input != null;
                    props.load(new InputStreamReader(input, StandardCharsets.UTF_8));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                String text = props.getProperty("info");
                JEditorPane textArea = new JEditorPane("text/html", text);
                JScrollPane scrollPane = new JScrollPane(textArea);
                scrollPane.setPreferredSize( new Dimension( 500, 500 ) );
                JOptionPane.showMessageDialog(null,scrollPane
                        ,
                        "Информация по парамерам запуска"
                        , JOptionPane.INFORMATION_MESSAGE, help_icon);
            }
        });
        add_new_infobase_alias_name.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                super.focusGained(e);
                add_new_infobase_alias_name.selectAll();
            }
        });
        add_new_infobase_comment.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                super.focusGained(e);
                add_new_infobase_comment.selectAll();
            }
        });
        run_1c_custom_params.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                super.focusGained(e);
                run_1c_custom_params.selectAll();
            }
        });
        run1cButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                String infobase =(String) target_list.getSelectedValue();
                String version = (String) server_1c_ver.getSelectedItem();
                String p =  server_1c_ver_with_port[0].split(":")[1];
                String port = p.substring(0, p.length() -1)+"1";
                String bin = path_to_1c + version + path_1c_exe;
                final String command = bin +" ENTERPRISE /S\""+dev_server+":"+port+"\\"+infobase + "\"";
                Thread run1c = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Docker1C.run_shell_command(command);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
                ScheduledExecutorService scheduler_res = Executors.newSingleThreadScheduledExecutor();
                scheduler_res.schedule(run1c, 1, TimeUnit.SECONDS);
            }
        });
        run1cDesignerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                String infobase =(String) target_list.getSelectedValue();
                String version = (String) server_1c_ver.getSelectedItem();
                String p =  server_1c_ver_with_port[0].split(":")[1];
                String port = p.substring(0, p.length() -1)+"1";
                String bin = path_to_1c + version + path_1c_exe;
                final String command = bin +" DESIGNER /S\""+dev_server+":"+port+"\\"+infobase + "\"";
                Thread run1c = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Docker1C.run_shell_command(command);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
                ScheduledExecutorService scheduler_res = Executors.newSingleThreadScheduledExecutor();
                scheduler_res.schedule(run1c, 1, TimeUnit.SECONDS);
            }
        });
        run1cAdminConsoleButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                final String command = get_property(default_property,"1c_console_command",null)[0];
                Thread run1c = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Docker1C.run_shell_command(command);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
                ScheduledExecutorService scheduler_res = Executors.newSingleThreadScheduledExecutor();
                scheduler_res.schedule(run1c, 1, TimeUnit.SECONDS);
            }
        });
        run1cWithParamsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                String infobase =(String) target_list.getSelectedValue();
                String version = (String) server_1c_ver.getSelectedItem();
                String p =  server_1c_ver_with_port[0].split(":")[1];
                String port = p.substring(0, p.length() -1)+"1";
                String bin = path_to_1c + version + path_1c_exe;
                String params = run_1c_custom_params.getText();
                final String command = bin +" ENTERPRISE /S\""+dev_server+":"+port+"\\"+infobase + "\" " + params;
                Thread run1c = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Docker1C.run_shell_command(command);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
                ScheduledExecutorService scheduler_res = Executors.newSingleThreadScheduledExecutor();
                scheduler_res.schedule(run1c, 1, TimeUnit.SECONDS);

            }
        });
        getDtButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                String infobase =(String) target_list.getSelectedValue();
                String version = (String) server_1c_ver.getSelectedItem();
                JFileChooser f = new JFileChooser();
                f.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                f.setDialogTitle("Choose directory for DT from base " +infobase);
                f.setFileView(new FileView() {
                    @Override
                    public String getName(File f) {
                        String name = FileSystemView.getFileSystemView().getSystemDisplayName(f);
                        if(name.equals("")) {
                            name = FileSystemView.getFileSystemView().getSystemTypeDescription(f);
                        }
                        return name;
                    }
                });
                f.showSaveDialog(null);
                Path  localRelativePath = Paths.get(String.valueOf(f.getSelectedFile()));
                String path = localRelativePath.toAbsolutePath().toString();
                Docker1C.run_designer_command(version,dev_server,infobase,path, "bak dt");
            }
        });
        getCfButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                String infobase =(String) target_list.getSelectedValue();
                String version = (String) server_1c_ver.getSelectedItem();
                JFileChooser f = new JFileChooser();
                f.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                f.setDialogTitle("Choose directory for CF from base " +infobase);
                f.setFileView(new FileView() {
                    @Override
                    public String getName(File f) {
                        String name = FileSystemView.getFileSystemView().getSystemDisplayName(f);
                        if(name.equals("")) {
                            name = FileSystemView.getFileSystemView().getSystemTypeDescription(f);
                        }
                        return name;
                    }
                });
                f.showSaveDialog(null);
                Path  localRelativePath = Paths.get(String.valueOf(f.getSelectedFile()));
                String path = localRelativePath.toAbsolutePath().toString();
                Docker1C.run_designer_command(version,dev_server,infobase,path, "bak cf");
            }
        });
        getSqlBakButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                String base =(String) target_list.getSelectedValue();
                String folder = "\\\\"+dev_server+"\\"+backup_dir;
                get_backup(dev_server,base, base);
                try {
                    Desktop.getDesktop().open(new File(folder));
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });
        useMyBackupButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                String infobase =(String) target_list.getSelectedValue();
                String version = (String) server_1c_ver.getSelectedItem();
                JFileChooser f = new JFileChooser();
                f.setFileSelectionMode(JFileChooser.FILES_ONLY);
                FileNameExtensionFilter filter1c = new FileNameExtensionFilter("1c backups", "dt","cf","bak");
                f.setAcceptAllFileFilterUsed(false);
                f.setFileFilter(filter1c);
                f.setDialogTitle("Choose your backup" );
                f.setFileView(new FileView() {
                    @Override
                    public String getName(File f) {
                        String name = FileSystemView.getFileSystemView().getSystemDisplayName(f);
                        if(name.equals("")) {
                            name = FileSystemView.getFileSystemView().getSystemTypeDescription(f);
                        }
                        return name;
                    }
                });
                f.showOpenDialog(null);
                Path  localRelativePath = Paths.get(String.valueOf(f.getSelectedFile()));
                String path = localRelativePath.toAbsolutePath().toString();
                if(path.endsWith(".dt")){
                    Docker1C.run_designer_command(version,dev_server,infobase,path, "load dt");
                }
                else if (path.endsWith(".cf")){
                    Docker1C.run_designer_command(version,dev_server,infobase,path, "load cf");
                }
                else if (path.endsWith(".bak")){
                    Path in = Paths.get(path);
                    String filename = in.getFileName().toString().split("\\.")[0];
                    Path out = Paths.get("\\\\"+dev_server+"\\"+backup_dir+"\\"+filename+".bak");
                    try {
                        Files.copy(Paths.get(path),out);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    restore_db(dev_server,dev_server,infobase,filename,"",restore_my_bak_progress_bar);
                }
            }
        });
    }

    public static void main(String[] args) throws IOException {
        new Docker();
    }
}
class ListColorRenderer extends DefaultListCellRenderer {
    public Component getListCellRendererComponent( JList list, Object value, int index, boolean isSelected, boolean cellHasFocus ) {
        Component c = super.getListCellRendererComponent( list, value, index, isSelected, cellHasFocus );
        String db_name = value.toString();
        final String dev_server =  Docker.get_property(Docker.default_property,"dev_server",null)[0];
        String db_cd = DockerSQL.get_mssql_db_creation_date(db_name, dev_server);
        String[] db_creation_date = db_cd.split("\\.");
        int db_creation_year = Integer.parseInt(db_creation_date[0]);
        int db_creation_month = Integer.parseInt(db_creation_date[1]);
        Date d = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("YYYY.MM.dd");
        String[] date = dateFormat.format(d).split("\\.");
        int year =Integer.parseInt(date[0]);
        int month = Integer.parseInt(date[1]);
        if ((year - db_creation_year )*12 +db_creation_month - month <=3){
            c.setBackground(Color.GREEN);
        }
        else{
            c.setBackground( Color.RED );
        }
        if(isSelected){
            c.setBackground( Color.LIGHT_GRAY);
        }
        return c;
    }
}