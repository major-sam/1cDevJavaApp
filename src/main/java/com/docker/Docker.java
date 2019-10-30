package com.docker;

import org.jasypt.util.text.StrongTextEncryptor;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.*;
import java.net.InetAddress;
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


public class Docker {
    private JButton backup_restore_db_button, quit;
    private JLabel source_base_size, wmi_space, severs_label, task;
    private JProgressBar progressbar1, progressbar2, c_task_bar;
    private JPanel main, current_tasks,
            current_bak_pan, myBases, my_servers, source_size_pane, server_disk_space, target_search_pan;
    private JList  target_list, source_list;
    private JScrollPane source_scroll, target_scroll;
    private JTextField source_search, target_search;
    private JComboBox server_list;
    private JCheckBox my_bases_only_check_box, create_new_db_check_box;
    private JTextField new_db_name_text_field, new_db_alias_name_text_field;
    private List source_buffer, target_buffer;
    private String  bak_thread_status, res_thread_status,
            cur_bak_database_name, cur_bak_database_finish_time, user_name, user_password;
    private Integer backup_progress, restore_progress, scheduler_counter1, scheduler_counter2;

    private void switch_ui_state(boolean state){
        server_list.setEnabled(state);
        source_list.setEnabled(state);
        target_list.setEnabled(state);
        backup_restore_db_button.setEnabled(state);
        quit.setEnabled(state);
        source_search.setEnabled(state);
        target_search.setEnabled(state);
    }
    private void switch_target_ui_state(boolean state){
        target_list.setEnabled(!state);
        target_search.setEnabled(!state);
        my_bases_only_check_box.setEnabled(!state);
        new_db_name_text_field.setEnabled(state);
        new_db_alias_name_text_field.setEnabled(state);

    }

    private void restore_db(final String server, final String target_base, final String data_path, final String log_path){
        Thread restore_db = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String logic_source_base= null ,logic_source_base_log=null;
                    res_thread_status ="WORKING";
                    Connection conn;
                    Statement stmt;
                    ResultSet rs;
                    String url ="jdbc:sqlserver://"+server+";user="+user_name+";password="+user_password+"";
                    conn = DriverManager.getConnection(url);
                    stmt = conn.createStatement();
                    String query= "RESTORE FILELISTONLY FROM DISK='current.bak' ";
                    rs = stmt.executeQuery(query);
                    if (stmt.execute(query)) {
                        rs = stmt.getResultSet();
                    }
                    while (rs.next()){
                        String type=rs.getString("Type");
                        String name=rs.getString("LogicalName");

                        if (type.matches(".*D.*"))
                        {
                            logic_source_base = name;
                        }
                        if (type.matches(".*L.*"))
                        {
                            logic_source_base_log = name;
                        }
                    }
                    String query_res="USE [master]\n" +
                            "        ALTER DATABASE ["+ target_base +"] SET SINGLE_USER WITH ROLLBACK IMMEDIATE\n" +
                            "        RESTORE DATABASE ["+ target_base +"] FROM  DISK = N'current.bak' WITH  FILE = 1," +
                            "        MOVE N'"+logic_source_base+"' TO N'"+ data_path + target_base +".mdf', " +
                            "        MOVE N'"+logic_source_base_log+"' TO N'"+ log_path + target_base +"_log.ldf'," +
                            "        NOUNLOAD,  REPLACE,  STATS = 5\n" +
                            "        ALTER DATABASE ["+ target_base +"] SET MULTI_USER";
                    conn = DriverManager.getConnection(url);
                    stmt = conn.createStatement();
                    stmt.executeQuery(query_res);
                    if (stmt.execute(query_res)) {
                        stmt.getResultSet();
                    }
                }
                catch (SQLException ex){
                    System.out.println("SQLException: " + ex.getMessage());
                    System.out.println("SQLState: " + ex.getSQLState());
                    System.out.println("VendorError: " + ex.getErrorCode());
                }
                res_thread_status ="DONE";
            }
        });
        scheduler_counter2 =0 ;
        ScheduledExecutorService scheduler_res = Executors.newSingleThreadScheduledExecutor();
        scheduler_res.schedule(restore_db, 1, TimeUnit.SECONDS);
        final ScheduledExecutorService scheduled_res_watch = Executors.newSingleThreadScheduledExecutor();
        scheduled_res_watch.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if (res_thread_status != null && res_thread_status.equals("DONE")) {
                    Thread.currentThread().interrupt();
                    scheduler_counter2++;
                    switch_ui_state(true);
                    if (scheduler_counter2 == 1)
                    {
                        progressbar2.setValue(100);
                    }
                    if (scheduler_counter2 == 2)
                    {
                        res_thread_status = "EXECUTED";
                        scheduled_res_watch.shutdown();
                        progressbar2.setValue(100);
                    }
                }
                if (res_thread_status != null && res_thread_status.equals("WORKING")) {
                    String query = "SELECT  r.session_id AS [Session_Id]\n" +
                            "             ,CONVERT(NUMERIC(6, 0), r.percent_complete) AS [Complete]\n" +
                            "             ,CONVERT(VARCHAR(1000), (\n" +
                            "             SELECT SUBSTRING(TEXT, r.statement_start_offset / 2,1000)  \n" +
                            "             FROM sys.dm_exec_sql_text(sql_handle)\n" +
                            "             ))as 'query'\n" +
                            "             FROM sys.dm_exec_requests r\n" +
                            "             WHERE   command like 'RESTORE%'";
                    restore_progress = progress(query, server, "");
                    progressbar2.setValue(restore_progress);
                }
            }
        },0,5, TimeUnit.SECONDS);
    }
    private int progress(String query, String server, String backup_name){
        Connection conn1;
        Statement stmt1;
        ResultSet rs1;
        int progress=0;
        try {
            String url ="jdbc:sqlserver://"+server+";user="+user_name+";password="+user_password+"";
            conn1 = DriverManager.getConnection(url);
            stmt1 = conn1.createStatement();
            rs1 = stmt1.executeQuery(query);
            if (stmt1.execute(query)) {
                rs1 = stmt1.getResultSet();
            }
            while (rs1.next()){
                if (rs1.getString("query").contains(backup_name)) {
                    progress = Integer.parseInt(rs1.getString("Complete"));
                }
                            }
        }
        catch (SQLException ex){
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }
        return  progress;
    }
    private String db_space(String basename, String server){
        Connection conn;
        Statement stmt;
        ResultSet rs;
        String res = null;
        try {
            String url ="jdbc:sqlserver://"+ server +";user="+user_name+";password="+user_password+"";
            conn =
                    DriverManager.getConnection(url);
            stmt = conn.createStatement();
            String query="SELECT\n" +
                    "    D.name,\n" +
                    "    Substring((F.physical_name),0,3)  AS Drive,\n" +
                    "    CAST((F.size*8)/1024 AS VARCHAR(26))  AS FileSize\n" +
                    "FROM \n" +
                    "    sys.master_files F\n" +
                    "    INNER JOIN sys.databases D ON D.database_id = F.database_id\n" +
                    "Where type=0 and F.database_id >4 and D.name like '" + basename+"'";
            rs = stmt.executeQuery(query);
            if (stmt.execute(query)) {
                rs = stmt.getResultSet();
            }
            while (rs.next()){
                String db_size=rs.getString("FileSize");
                res = db_size +" MB";
            }

        }
        catch (SQLException ex){
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }
        return res;
    }
    private String mssql_get_path(String query, String server){
        Connection conn;
        Statement stmt;
        ResultSet rs;
        String path = null;
        try {
            String url ="jdbc:sqlserver://"+ server +";user="+user_name+";password="+user_password+"";
            conn =
                    DriverManager.getConnection(url);
            stmt = conn.createStatement();
            rs = stmt.executeQuery(query);
            if (stmt.execute(query)) {
                rs = stmt.getResultSet();
            }

            while (rs.next()){
                path=rs.getString("a");
            }
        }
        catch (SQLException ex){
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }
        return path;
    }
    private String[] mssql_free_space(String server){
        Connection conn;
        Statement stmt;
        ResultSet rs;
        StringBuilder disk_space = new StringBuilder();
        String bak_disk_free = null;
        String sql_db_disk_free = null;
        String bak_disk = "K";
        String sqlDB_disk = "D";
        String disk_start_style;
        String disk_end_style;
        String size_start_style;
        String size_end_style;
        String query ="EXEC MASTER..xp_fixeddrives;";
        try {
            String url ="jdbc:sqlserver://"+ server +";user="+user_name+";password="+user_password+"";
            conn = DriverManager.getConnection(url);
            stmt = conn.createStatement();
            rs = stmt.executeQuery(query);
            if (stmt.execute(query)) {
                rs = stmt.getResultSet();
            }
            boolean is_true = true;
            while (rs.next()){
                if (rs.getString("drive").equals(bak_disk)){
                    disk_start_style = "<font color=#005ef5>";
                    disk_end_style = "</font>";
                    bak_disk_free = rs.getString("MB free");
                }
                else if ( rs.getString("drive").equals(sqlDB_disk)){
                    disk_start_style = "<font color=#067a00>";
                    disk_end_style = "</font>";
                    sql_db_disk_free = rs.getString("MB free");
                }
                else {
                    disk_start_style = "";
                    disk_end_style = "";
                }
                if (Integer.parseInt(rs.getString("MB free"))< 3000){
                    size_start_style = "<font color=#f54242>";
                    size_end_style = "</font>";
                }
                else {
                    size_start_style = "";
                    size_end_style = "";
                }
                if (is_true){
                    disk_space.append("\n").append(disk_start_style).append(rs.getString("drive"))
                            .append(disk_end_style).append(":\\ Free: ")
                            .append(size_start_style).append(rs.getString("MB free")).append(size_end_style)
                            .append(" MB   |  ");
                }
                else {
                    disk_space.append(disk_start_style).append(rs.getString("drive"))
                            .append(disk_end_style).append(":\\ Free: ")
                            .append(size_start_style).append(rs.getString("MB free")).append(size_end_style)
                            .append(" MB<br/>");
                }
                is_true = !is_true;
            }
        }
        catch (SQLException ex){
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }
        return new String[]{disk_space.toString(), bak_disk_free, sql_db_disk_free};
    }
    private List mssql_get_db_list(String server){
        Connection conn;
        Statement stmt;
        ResultSet rs;
        String query="SELECT name FROM master.dbo.sysdatabases  where dbid >4";
        List<String> work_base = new ArrayList<String>();

        try {
            String url ="jdbc:sqlserver://"+ server +";user="+user_name+";password="+user_password+"";
            conn = DriverManager.getConnection(url);
            stmt = conn.createStatement();
            rs = stmt.executeQuery(query);
            if (stmt.execute(query)) {
                rs = stmt.getResultSet();
            }
            while (rs.next()){
                String dbname=rs.getString("name");
                work_base.add(dbname);
            }
        }
        catch (SQLException ex){
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }
        return work_base;
    }
    private static List getItems(DefaultListModel model) {
        List list = new ArrayList();
        for (int i = 0; i < model.size(); i++) {
            list.add(model.elementAt(i).toString());
        }
        return list;
    }
    private Docker() {
        StrongTextEncryptor textEncryptor = new StrongTextEncryptor();
        textEncryptor.setPassword("$ecurePWD");
        JFrame frame = new JFrame("Docker");
        FileInputStream fis;
        Properties property = new Properties();
        DefaultComboBoxModel s_model = new DefaultComboBoxModel();
        try {
            fis = new FileInputStream("conf/default.properties");
            property.load(fis);
            String crypt_name = property.getProperty("user");
            String crypt_password = property.getProperty("password");
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
                            System.out.println("Your password is: " + password + "\nYour password confirm is: " + confirm);
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
            user_name = textEncryptor.decrypt(crypt_name);
            user_password = textEncryptor.decrypt(crypt_password);
        }
        catch(Exception ex){
            System.out.println (ex.toString());
        }
        String[] mssql_servers = property.getProperty("servers").split(",");
        final String dev_server = property.getProperty("dev_server");
        final List<List<String>> base_lists = new ArrayList<List<String>>();
        final Map<String, Integer> base_dictionary = new HashMap<String, Integer>();
        Integer id = 0;
        for(String s: mssql_servers){
            base_dictionary.put(s,id);
            id++;
            s_model.addElement(s);
            List<String> list;
            list = mssql_get_db_list(s);
            base_lists.add(list);
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
        source_buffer = getItems(sdb);
        target_buffer = getItems(tdb);
        source_list.setModel(sdb);
        target_list.setModel(tdb);
        final String[] source_base_name = {null};
        final String[] target_base = {null};
        server_list.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent itemEvent) {
                if (itemEvent.getStateChange() == ItemEvent.SELECTED){
                    selected_server[0] = (String) itemEvent.getItem();
                }
                sdb.clear();
                source_list.setModel(sdb);
                target_list.setModel(tdb);
                List<String> bases_on_server = base_lists.get(base_dictionary.get(selected_server[0]));
//                List<String> bases_on_dev_server = base_lists.get(base_dictionary.get(dev_server));
                for (String base: bases_on_server) {
                    sdb.addElement(base);
                }
                source_list.setModel(sdb);
                source_base_name[0] =(String) source_list.getSelectedValue();
            }
        });
        String free_space_on_disk = mssql_free_space(dev_server)[0];
        final int sql_disk =Integer.parseInt(mssql_free_space(dev_server)[1]);
        final int bak_disk =Integer.parseInt(mssql_free_space(dev_server)[2]);
        wmi_space.setText("<html>"+ free_space_on_disk + "<html>");
        String get_data_path= "select  SERVERPROPERTY('InstanceDefaultDataPath') as a";
        String get_log_path= "select  serverproperty('InstanceDefaultLogPath') as a";
       // String data_path = mssql_get_path(get_data_path, dev_server);
        //String log_path = mssql_get_path(get_log_path, dev_server);
        source_base_size.setText("");
        backup_restore_db_button.setEnabled(true);
        current_tasks.setVisible(false);
        final String[] warn_message = new String[1];
        backup_restore_db_button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                int extra = Integer.parseInt(source_base_size.getText().replaceAll(" MB", ""));
                int approve;
                if (sql_disk<extra){
                    warn_message[0] = "<html><font color=#f54242> НЕДОСТАТОЧНО МЕСТА НА ДИСКЕ БД</font>";
                     JOptionPane.showConfirmDialog(null,
                             warn_message[0], "WARNING", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
                    approve = 1;
                }
                else if (bak_disk<extra){
                    warn_message[0] = "<html><font color=#f54242> НЕДОСТАТОЧНО МЕСТА НА ДИСКЕ БЭКАПОВ</font>";
                    JOptionPane.showConfirmDialog(null,
                            warn_message[0], "WARNING", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
                    approve = 1;
                }
                else {
                    approve = JOptionPane.showConfirmDialog(null,
                        "Сервер: \t" + selected_server[0] + "\nИЗ: "+ source_base_name[0]
                                +"\nДополнительное место: "+extra+" MB"
                                +"\nСвободно места на диске бд: "+sql_disk+" MB"
                                +"\nСвободно места на диске бэапов: "+bak_disk+" MB"
                                +"\n",
                        "Подтверждение"
                        , JOptionPane.YES_NO_OPTION);
                }
                if(approve == 0){
                    Date date = new Date();
                    //TODO log_file_path must be in property file
                    String log_file_path = "\\\\90500-ws108\\log$\\app.log";
                    try {
                        FileWriter fileWriter  = new FileWriter(log_file_path,true);
                        PrintWriter log = new PrintWriter(fileWriter);
                        log.println(date+" "+System.getProperty("user.name")+" "+ InetAddress.getLocalHost().getHostName()+
                                " "+ selected_server[0] +" From: \t"+ source_base_name[0] +" To: \t"+ target_base[0]);
                        log.close();
                    }
                    catch (IOException e){
                        System.out.println("file not found");
                    }
                    switch_ui_state(false);
                    progressbar1.setMinimum(0);
                    progressbar1.setMaximum(100);
                    progressbar1.setValue(0);
                    progressbar2.setMinimum(0);
                    progressbar2.setMaximum(100);
                    progressbar2.setValue(0);
                    scheduler_counter1 =0;
                    scheduler_counter2 =0;
                    SimpleDateFormat dateFormat = new SimpleDateFormat("HH_ddMMYY");
                    final String backup_name = System.getProperty("user.name")+"_"+dateFormat.format(date)+"_"+ source_base_name[0];
                    Thread backup_db = new Thread(new Runnable()  {
                        @Override
                        public void run() {
                            try {
                                bak_thread_status ="WORKING";
                                Connection conn;
                                Statement stmt;
                                String query="SET NOCOUNT ON " +
                                        "BACKUP DATABASE ["+ source_base_name[0] +"] TO  DISK = N'"+backup_name+".bak' WITH NOFORMAT, INIT,  NAME = N'"+
                                        source_base_name[0] +"-Full Database Backup', SKIP, NOREWIND, NOUNLOAD,  STATS = 10";
                                String url ="jdbc:sqlserver://"+ selected_server[0] +";user="+user_name+";password="+user_password+"";
                                conn = DriverManager.getConnection(url);
                                stmt = conn.createStatement();
                                stmt.executeQuery(query);
                                if (stmt.execute(query)) {
                                    stmt.getResultSet();
                                }

                            }
                            catch (SQLException ex){
                                System.out.println("SQLException: " + ex.getMessage());
                                System.out.println("SQLState: " + ex.getSQLState());
                                System.out.println("VendorError: " + ex.getErrorCode());
                            }
                            bak_thread_status ="DONE";
                        }
                    });
                    ScheduledExecutorService scheduler_bak = Executors.newSingleThreadScheduledExecutor();
                    scheduler_bak.schedule(backup_db, 1, TimeUnit.SECONDS);
                    final ScheduledExecutorService scheduled_bak_watch = Executors.newSingleThreadScheduledExecutor();
                    scheduled_bak_watch.scheduleAtFixedRate(new Runnable() {
                        @Override
                        public void run() {
                            if (bak_thread_status != null && bak_thread_status.equals("DONE")) {
                                Thread.currentThread().interrupt();
                                scheduler_counter1++;
                                if (scheduler_counter1 == 2)
                                {
                                    bak_thread_status = "EXECUTED";
                                    scheduled_bak_watch.shutdown();
                                    progressbar1.setValue(100);
                                  //  restore_db(selected_server);
                                }
                            }
                            if (bak_thread_status != null && bak_thread_status.equals("WORKING")) {
                                String query="SELECT  r.session_id AS [Session_Id]\n" +
                    "             ,CONVERT(NUMERIC(6, 0), r.percent_complete) AS [Complete]\n" +
                    "             ,CONVERT(VARCHAR(1000), (\n" +
                    "             SELECT SUBSTRING(TEXT, r.statement_start_offset / 2,1000)  \n" +
                    "             FROM sys.dm_exec_sql_text(sql_handle)\n" +
                    "             ))as 'query'\n" +
                    "             FROM sys.dm_exec_requests r\n" +
                    "             WHERE   command like 'BACKUP%'";
                                backup_progress = progress(query, selected_server[0], backup_name);
                                progressbar1.setValue(backup_progress);
                            }
                        }
                    },0,5, TimeUnit.SECONDS);
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
                source_base_size.setText(db_space(source_base_name[0], selected_server[0]));
            }
        });
        target_list.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                target_base[0] = (String) target_list.getSelectedValue();
                db_space(target_base[0], selected_server[0]);
            }
        });
        create_new_db_check_box.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                    switch_target_ui_state(create_new_db_check_box.isSelected());
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
            }
        });

        ImageIcon img = new ImageIcon("conf/icon.png");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setIconImage(img.getImage());
        frame.setMinimumSize(new Dimension(800, 600));
        frame.setPreferredSize(new Dimension(800, 600));
        frame.add(main);
        frame.pack();
        frame.setVisible(true);
        current_tasks.setVisible(false);
    }

    public static void main(String[] args){
        new Docker();
    }

}