package com.docker;

import org.jasypt.util.text.StrongTextEncryptor;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;
import javax.swing.filechooser.FileView;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import static java.nio.charset.StandardCharsets.UTF_8;

@SuppressWarnings("ALL")
public class Docker {
    private JButton run_task_button, quit, useMyBackupButton, run1cButton, run1cDesignerButton, getDtButton,
            getCfButton, run1cAdminConsoleButton, run1cWithParamsButton, getSqlBakButton, removeDbButton, openLogsButton
            ,dev_base_renew, params_help, addToAllUserButton, removeFromAllUserButton;
    private JLabel source_base_size, wmi_space, source_base_creation_date,target_base_creation_date,target_base_size;
    private JProgressBar progressbar1, progressbar2, restore_my_bak_progress_bar, get_backup_progress_bar;
    private JPanel main_frame;
    private JList  target_list, source_list;
    private JScrollPane source_scroll, target_scroll;
    private JTextField source_search, target_search, add_new_infobase_comment, add_new_infobase_alias_name,
            run_1c_custom_params;
    public  JComboBox server_list, server_1c_ver, dev_prod_switch;
    private JCheckBox my_bases_only_check_box, create_new_db_check_box;
    private JLabel spinner;
    private List source_buffer, target_buffer;
    static String user_name, user_password, domain;
    private DefaultListModel sdb = new DefaultListModel() ,  tdb = new DefaultListModel();
    private static Path currentRelativePath = Paths.get("conf/default.properties");
    static String default_property = currentRelativePath.toAbsolutePath().toString();
    static String path_1c_exe = get_property(default_property,"path_1c_exe", null)[0];
    static String path_rac_exe = get_property(default_property,"path_rac_exe", null)[0];

    private void enable_spinner() {
        spinner.setEnabled(true);
        ImageIcon icon = new ImageIcon("conf/spinner.gif");
        Image img = icon.getImage();
        Image image = img.getScaledInstance(54, 54, Image.SCALE_REPLICATE);
        icon = new ImageIcon(image);
        spinner.setIcon(icon);
    }
    private void disable_spinner(){
        spinner.setIcon(null);
        spinner.setEnabled(false);
    }

    private void set_access(String basename){
        String admin_account = get_property(default_property,"admin_account",null)[0];
        boolean test_admin = System.getProperty("user.name").toLowerCase().equals(admin_account);
        boolean test_owner = basename.toLowerCase().startsWith(System.getProperty("user.name").toLowerCase());
        if (!test_admin & !test_owner){
            removeDbButton.setEnabled(false);
            run_task_button.setEnabled(false);
        }
        else{
            removeDbButton.setEnabled(true);
            run_task_button.setEnabled(true);
        }
    }
    private static String check_local_1c_bin(){
        boolean x64_has_client = false, x86_has_client = false;
        String[] v = get_property(default_property,"server_1c_ver_with_ras",",");
        final File  X86_BIN = new File("C:\\Program Files (x86)\\1cv8"),
                X64_BIN =new File("C:\\Program Files\\1cv8");
        for (String ver:v){
            if(!x64_has_client) {
                String test_path = "C:\\Program Files\\1cv8\\" + ver.split(":")[0];
                File test = new File(test_path);
                x64_has_client = (test.exists() && test.isDirectory());
            }
            if(!x86_has_client) {
                String test_path = "C:\\Program Files (x86)\\1cv8\\" + ver.split(":")[0];
                File test = new File(test_path);
                x86_has_client = (test.exists() && test.isDirectory());
            }
        }
        String lp;
        if(X64_BIN.exists() && X64_BIN.isDirectory()&&x64_has_client){
            lp = X64_BIN.getPath();
        }
        else if(X86_BIN.exists() && X86_BIN.isDirectory()&&x86_has_client){
            lp = X86_BIN.getPath();
            JOptionPane.showMessageDialog(null, "Не установлен рекомундуемый 64 битный клиент 1с, будет использоваться 32 битный клиент", "Найден 86х клиент 1с",JOptionPane.INFORMATION_MESSAGE);
        }
        else{
            JOptionPane.showMessageDialog(null, "Не найден клиент 1с. Укажите путь до папки 1cv8", "Не найден клиент 1с",JOptionPane.INFORMATION_MESSAGE);
            JFileChooser f = new JFileChooser();
            f.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            f.setDialogTitle("Choose path to 1cv8 folder");
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
            lp = localRelativePath.toAbsolutePath().toString();
        }
        if (!lp.endsWith("\\")){
            lp = lp +"\\";
        }
        return lp;
    }
    static void check_local_conf(String local_property) throws IOException {
        String path_to_1c;
        File local_config = new File(local_property);
        if(local_config.exists() && !local_config.isDirectory()) {
            path_to_1c = get_property(local_property,"path_to_1c",null)[0];
            if (path_to_1c == null || path_to_1c.equals("")){
                path_to_1c = check_local_1c_bin();
                Properties prop = new Properties();
                prop.setProperty("path_to_1c",path_to_1c);
                prop.store(new FileOutputStream(local_property,true), "\nyour  1c files");
            }
        }
        else{
            Path path = Paths.get(local_property);
            try {
                Files.createDirectories(path.getParent());
                Files.createFile(path);
                path_to_1c = check_local_1c_bin();
                Properties prop = new Properties();
                prop.setProperty("path_to_1c",path_to_1c);
                prop.store(new FileOutputStream(local_property,true), "\nyour  1c files");
            } catch (IOException e) {
                System.err.println("Cannot create directories - " + e);
            }
        }
    }
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
        useMyBackupButton.setEnabled(state);
        server_list.setEnabled(state);
        source_list.setEnabled(state);
        target_list.setEnabled(state);
        run_task_button.setEnabled(state);
        quit.setEnabled(state);
        source_search.setEnabled(state);
        target_search.setEnabled(state);
        add_new_infobase_comment.setEnabled(state);
        add_new_infobase_alias_name.setEnabled(state);
        my_bases_only_check_box.setEnabled(state);
        server_1c_ver.setEnabled(state);
        add_new_infobase_alias_name.setEnabled(state);
        add_new_infobase_comment.setEnabled(state);
        addToAllUserButton.setEnabled(state);
        removeFromAllUserButton.setEnabled(state);
        if(state){
            create_new_db_check_box.setSelected(false);
            add_new_infobase_alias_name.setText("");
            add_new_infobase_comment.setText("");
        }
    }
    private void enable_ui_target(boolean state){
        target_list.setEnabled(!state);
        target_search.setEnabled(!state);
        my_bases_only_check_box.setEnabled(!state);
        add_new_infobase_comment.setEnabled(state);
        add_new_infobase_alias_name.setEnabled(state);
        server_1c_ver.setEnabled(state);
        run_task_button.setEnabled(state);
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
    void wright_log(String date, String selected_server, String source_base_name,
                    String target_base_name, String new_base, Boolean remove_state, String ver) throws IOException {
        String log_file_path = get_property(default_property,"log_file",null)[0].replace("\\log","\\log$");
        String tab_sep = "#######";
        String line_sep = ("=============================================================================\r\n");
        String line =date +"\r\n"
                +tab_sep+String.format("%-20s","User:"+System.getProperty("user.name").toLowerCase()+" Host:"+InetAddress.getLocalHost().getHostName() +" Action: ");
        if (remove_state){
            line = line +"REMOVE"+ "\r\n"
                    + tab_sep + "REMOVED DB:" + target_base_name + "\r\n" +
                    line_sep;
        }
        else if (create_new_db_check_box.isSelected()){
            line = line + "CREATE"+"\r\n"
                    + tab_sep + "NEW DB: "+new_base+ " 1c version:"+ ver + "\r\n"+
                    line_sep;
        }
        else {
            line = line +
                    "UPDATE"+"\r\n"
                    + tab_sep + "Source server:" + selected_server + " Source base: " + source_base_name + " Target Base: "
                    + target_base_name + "\r\n"+
                    line_sep;
        }
        new File(log_file_path);
        byte[] strToBytes = line.getBytes();
        Files.write(Paths.get(log_file_path), strToBytes, StandardOpenOption.APPEND);
    }
    private void backup_db(final String selected_server, final String source_base_name, final String backup_name,
                           final String target_base_name , final String dev_server,
                           final String backup_dir, final String server_1c,final String path_to_1c){
        enable_ui(false);
        progressbar1.setMinimum(0);
        progressbar1.setMaximum(100);
        progressbar1.setValue(0);
        progressbar2.setMinimum(0);
        progressbar2.setMaximum(100);
        progressbar2.setValue(0);
        enable_spinner();
        final boolean[] bak_thread_status = {false};
        Thread backup_db = new Thread(() -> {
            try {
                Connection conn;
                Statement stmt;
                String query="SET NOCOUNT ON "+
                        "BACKUP DATABASE ["+source_base_name+"] TO  DISK = N'"+backup_name+".bak' WITH RETAINDAYS = 3, NOFORMAT, INIT,  NAME = N'"+
                        source_base_name+"-Full Database Backup', SKIP, NOREWIND, NOUNLOAD,  STATS = 10";
                String url ="jdbc:sqlserver://"+selected_server+";user="+user_name+";password="+user_password+domain+"";
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
                if (ex.getErrorCode()!=0){
                    DockerSQL.show_message(ex.getMessage());
                }
            }
            bak_thread_status[0] = true;
        });
        ScheduledExecutorService scheduler_bak = Executors.newSingleThreadScheduledExecutor();
        scheduler_bak.schedule(backup_db, 1, TimeUnit.SECONDS);
        final ScheduledExecutorService scheduled_bak_watch = Executors.newSingleThreadScheduledExecutor();
        scheduled_bak_watch.scheduleAtFixedRate(() -> {
            if (bak_thread_status[0]) {
                String ver =(String) server_1c_ver.getSelectedItem();
                Date updated_description = new Date();
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-YYYY HH-mm");
                String description = add_new_infobase_comment.getText()+","+dateFormat.format(updated_description);
                progressbar1.setValue(100);
                if (create_new_db_check_box.isSelected()){
                    try {
                        String infobase_name = add_new_infobase_alias_name.getText();
                        Docker1C.create_1c_base(server_1c, dev_server, ver, backup_name,path_to_1c, description);
                        Docker1C.add_infobase_to_list(server_1c,ver,backup_name,infobase_name, System.getProperty("user.name").toLowerCase());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    restore_db(selected_server, dev_server, backup_name, backup_name, backup_dir,progressbar2);
                }
                else {
                    restore_db(selected_server, dev_server, target_base_name, backup_name, backup_dir,progressbar2);
                    try {
                        description ="Обновлено базой "+ backup_name+" пользователем "+
                                System.getProperty("user.name").toLowerCase()+","+dateFormat.format(updated_description);
                        Docker1C.set_infobase_description(server_1c,ver,path_to_1c,target_base_name,description);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                Thread.currentThread().interrupt();
                disable_spinner();
                scheduled_bak_watch.shutdown();

            }
            else {
                String query_salt ="'BACKUP%'";
                int backup_progress = DockerSQL.get_mssql_progress(query_salt, selected_server, backup_name);
                progressbar1.setValue(backup_progress);
            }
        },0,2, TimeUnit.SECONDS);
    }
    private void restore_db(String source_server, final String target_server, final String target_base,
                            final String backup_name, final String backup_sh_folder, final JProgressBar bar) {
        String get_data_path= "select  SERVERPROPERTY('InstanceDefaultDataPath') as a",
                get_log_path= "select  serverproperty('InstanceDefaultLogPath') as a";
        final String data_path = DockerSQL.get_mssql_path(get_data_path, target_server);
        final String log_path = DockerSQL.get_mssql_path(get_log_path, target_server);
        final boolean[] res_thread_status = {false};
        String path = "";
        String source_server_name = source_server.split("\\\\")[0];
        if (!source_server.equals(target_server)) {
            path = "\\\\" + source_server_name + "\\" + backup_sh_folder + "\\";
        }
        final String finalPath = path;
        Thread restore_db = new Thread(() -> {
            try {
                String logic_source_base= null ,logic_source_base_log=null;
                Connection conn;
                Statement stmt;
                ResultSet rs;
                String url ="jdbc:sqlserver://"+target_server+";user="+user_name+";password="+user_password+domain+"";
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
                if (ex.getErrorCode()!=0){
                    DockerSQL.show_message(ex.getMessage());
                }
            }
            res_thread_status[0] = true;
        });
        ScheduledExecutorService scheduler_res = Executors.newSingleThreadScheduledExecutor();
        scheduler_res.schedule(restore_db, 1, TimeUnit.SECONDS);
        final ScheduledExecutorService scheduled_res_watch = Executors.newSingleThreadScheduledExecutor();
        scheduled_res_watch.scheduleAtFixedRate(() -> {
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
                scheduled_res_watch.shutdown();
            }
            else  {
                String query_salt = "'RESTORE%'";
                int restore_progress = DockerSQL.get_mssql_progress(query_salt, target_server, backup_name);
                bar.setValue(restore_progress);
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
    static void check_password_prop(String crypt_name, String crypt_password) throws IOException {
        StrongTextEncryptor textEncryptor = new StrongTextEncryptor();
        textEncryptor.setPassword("$ecurePWD");
        while (true) {
            if (crypt_name == null | crypt_password == null) {
                JPanel panel = new JPanel(new BorderLayout(5, 7));
                JPanel labels = new JPanel(new GridLayout(0, 1, 2, 2));
                labels.add(new JLabel("Login:", SwingConstants.RIGHT));
                labels.add(new JLabel("Pass:", SwingConstants.RIGHT));
                labels.add(new JLabel("Confirm:", SwingConstants.RIGHT));
                labels.add(new JLabel("Domain:", SwingConstants.RIGHT));
                panel.add(labels, BorderLayout.WEST);
                JPanel fields = new JPanel(new GridLayout(0, 1, 2, 2));
                JTextField username = new JTextField(textEncryptor.decrypt(crypt_name));
                JPasswordField pass = new JPasswordField(10);
                JPasswordField pass_confirm = new JPasswordField(10);
                JTextField domain = new JTextField("");
                fields.add(username);
                fields.add(pass);
                fields.add(pass_confirm);
                fields.add(domain);
                JCheckBox checkbox = new JCheckBox("Domain user?");
                panel.add(fields, BorderLayout.CENTER);
                panel.add(checkbox,BorderLayout.SOUTH);
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
                        if (checkbox.isSelected()){
                            prop.setProperty("domain", domain.getText());
                        }
                        else {
                            prop.setProperty("domain", "");
                        }
                        String conf_path = ".\\conf\\default.properties";
                        prop.store(new FileOutputStream(default_property,true), "\nremove lines if password changes or wrong");
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
        Thread backup_db = new Thread(() -> {
            get_backup_progress_bar.setMinimum(0);
            get_backup_progress_bar.setMaximum(100);
            get_backup_progress_bar.setValue(0);
            try {
                Connection conn;
                Statement stmt;
                String query="SET NOCOUNT ON "+
                        "BACKUP DATABASE ["+source_base_name+"] TO  DISK = N'"+backup_name+".bak' WITH RETAINDAYS = 3, NOFORMAT, INIT,  NAME = N'"+
                        source_base_name+"-Full Database Backup', SKIP, NOREWIND, NOUNLOAD,  STATS = 10";
                String url ="jdbc:sqlserver://"+selected_server+";user="+user_name+";password="+user_password+domain+"";
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
        });
        ScheduledExecutorService scheduler_bak = Executors.newSingleThreadScheduledExecutor();
        scheduler_bak.schedule(backup_db, 1, TimeUnit.SECONDS);
        final ScheduledExecutorService scheduled_bak_watch = Executors.newSingleThreadScheduledExecutor();
        scheduled_bak_watch.scheduleAtFixedRate(() -> {
            if (bak_thread_status[0]) {
                get_backup_progress_bar.setValue(100);
                JOptionPane.showConfirmDialog(main_frame,
                        "Backup done", "Done", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE);
                Thread.currentThread().interrupt();
                scheduled_bak_watch.shutdown();
            }
            else {
                String query_salt ="'BACKUP%'";
                int backup_progress = DockerSQL.get_mssql_progress(query_salt, selected_server, backup_name);
                get_backup_progress_bar.setValue(backup_progress);
            }
        },0,2, TimeUnit.SECONDS);
    }
    private static void copy(Path source, Path dest) {
        try {
            if (!Files.exists(dest)){
                Files.copy(source, dest);}
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
    static void check_rac(String path, String ver) throws IOException {
        String rac_exe = path+ver+path_rac_exe;
        File rac = new File(rac_exe);
        if (!rac.exists()){
            String src = get_property(default_property,"rac_share",null)[0]+ ver + "\\";
            Path target = Paths.get(path + ver + "\\bin\\");
            Files.walk(Paths.get(src))
                        .filter(Files::isRegularFile)
                        .forEach(source -> copy(source,target.resolve(Paths.get(src).relativize(source))));
        }
    }
    private void set_base_list(String server_name, DefaultListModel listModel){
        List<String> list = DockerSQL.get_mssql_db_list(server_name);
        listModel.clear();
        for (String base: list) {
            listModel.addElement(base);
        }
    }
    private Docker() throws IOException {
        boolean debug = get_property(default_property,"debug",null)[0].equals("true");
        if (debug) {
            new FileOutputStream("C:\\docker\\debug.log", true).close();
            PrintStream debug_file = new PrintStream(new BufferedOutputStream(new FileOutputStream("C:\\docker\\debug.log", true)), true);
            System.setErr(debug_file);
            System.setOut(debug_file);
        }
        String crypt_name = get_property(default_property,"user",null)[0];
        String crypt_password = get_property(default_property,"password",null)[0];
        check_password_prop(crypt_name,crypt_password);
        StrongTextEncryptor textEncryptor = new StrongTextEncryptor();
        textEncryptor.setPassword("$ecurePWD");
        user_name = textEncryptor.decrypt(crypt_name);
        user_password = textEncryptor.decrypt(crypt_password);
        String domain_string = get_property(default_property, "domain", null)[0];
        if (!domain_string.equals("")){
            domain = ";domain="+domain_string+"";
        }
        else {
            domain = domain_string;
        }
        String[] mssql_servers = get_property(default_property,"servers",",");
        final String dev_server =  get_property(default_property,"dev_server",null)[0];
        final String backup_dir =  get_property(default_property,"bak_dir_name",null)[0];
        final String server_1c = get_property(default_property,"1c_server",null)[0];
        final String all_user_list = get_property(default_property,"all_user_list",null)[0];
        final String[] server_1c_ver_with_port =get_property(default_property,"server_1c_ver_with_ras",",");
        final Path  localRelativePath = Paths.get(get_property(default_property,"local.property",null)[0]);
        final String local_property = localRelativePath.toAbsolutePath().toString();
        check_local_conf(local_property);
        final String path_to_1c = get_property(local_property,"path_to_1c",null)[0];
        DefaultComboBoxModel c_model = new DefaultComboBoxModel();
        for(String c: server_1c_ver_with_port){
            String[] server_1c_ver = c.split(":");
            c_model.addElement(server_1c_ver[0]);
        }
        server_1c_ver.setModel(c_model);
        DefaultComboBoxModel s_model = new DefaultComboBoxModel();
        server_list.setModel(s_model);
        for(String srv: mssql_servers){
            s_model.addElement(srv);
        }
        final String[] selected_server = {mssql_servers[0]};
        source_scroll.setViewportView(source_list);
        target_scroll.setViewportView(target_list);
        source_list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        target_list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        set_base_list(selected_server[0],sdb);
        source_buffer = get_items_for_filter(sdb);
        source_list.setModel(sdb);
        source_list.setSelectedIndex(0);
        set_base_list(dev_server,tdb);
        target_buffer = get_items_for_filter(tdb);
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
        if (target_base_name[0] != null){
            set_access(target_base_name[0]);
        }
        else{
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
        frame.add(main_frame);
        frame.pack();
        frame.setVisible(true);
// listeners
        server_list.addItemListener(itemEvent -> {
            enable_spinner();
            if (itemEvent.getStateChange() == ItemEvent.SELECTED){
                selected_server[0] = (String) itemEvent.getItem();
            }
            sdb.clear();
            source_buffer.clear();
            source_list.setModel(sdb);
            List<String> bases_on_server1 = DockerSQL.get_mssql_db_list(selected_server[0]);
            for (String base: bases_on_server1) {
                sdb.addElement(base);
            }
            source_list.setModel(sdb);
            source_buffer = get_items_for_filter(sdb);
            source_base_name[0] =(String) source_list.getSelectedValue();
            source_list.setSelectedIndex(0);
            disable_spinner();
        });
        String free_space_on_disk = DockerSQL.get_mssql_free_space(dev_server)[0];
        final int sql_disk_free_space =Integer.parseInt(DockerSQL.get_mssql_free_space(dev_server)[2]);
        final int bak_disk_free_space =Integer.parseInt(DockerSQL.get_mssql_free_space(dev_server)[1]);
        wmi_space.setText("<html>"+free_space_on_disk+"<html>");
        final String[] warn_message = new String[1];
        run_task_button.addActionListener(actionEvent -> {
            enable_spinner();
            try {
                check_rac(path_to_1c, (String) server_1c_ver.getSelectedItem());
            } catch (IOException e) {
                e.printStackTrace();
            }
            final Date d = new Date();
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-YY_HH-mm-ss-S");
            final String date = dateFormat.format(d);
            source_base_name[0] = (String) source_list.getSelectedValue();
            int extra = Integer.parseInt(DockerSQL.get_mssql_db_size((String)source_list.getSelectedValue(),(String)server_list.getSelectedItem()));
            if (!create_new_db_check_box.isSelected()){
                extra = extra - Integer.parseInt(DockerSQL.get_mssql_db_size((String)target_list.getSelectedValue(), dev_server));
            }
            int approve = 1;
            String alias = add_new_infobase_alias_name.getText();
            String comment = add_new_infobase_comment.getText();
            if (sql_disk_free_space<extra){
                warn_message[0] = "<html><font color=#f54242> НЕДОСТАТОЧНО МЕСТА НА ДИСКЕ БД</font>";
                 JOptionPane.showConfirmDialog(null,
                         warn_message[0], "WARNING", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
            }
            else if (bak_disk_free_space<extra){
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
                            +"\nСервер: \t"+ selected_server[0]
                            +"\nИЗ: "+source_base_name[0]
                            +additional_string
                            +"\nДополнительное место: "+extra+" MB"
                            +"\nСвободно места на диске бд: "+sql_disk_free_space+" MB"
                            +"\nСвободно места на диске бэкапов: "+bak_disk_free_space+" MB"
                            +"\n",
                        title+" Подтверждение"
                    , JOptionPane.YES_NO_OPTION);
            }
            if(approve == 0){
                final String backup_name = System.getProperty("user.name").toLowerCase()+"_"+source_base_name[0]+"_"+date;
                try {
                    wright_log(date, selected_server[0],source_base_name[0],target_base_name[0],backup_name,
                            false,(String) server_1c_ver.getSelectedItem());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                backup_db(selected_server[0], source_base_name[0], backup_name, target_base_name[0]
                        ,dev_server, backup_dir, server_1c, path_to_1c);
            }
            my_bases_only_check_box.setSelected(false);
            target_search.setText(null);
            disable_spinner();
        });
        removeDbButton.addActionListener(actionEvent -> {
            enable_spinner();
            int approve;
            try {
                check_rac(path_to_1c, (String) server_1c_ver.getSelectedItem());
            } catch (IOException e) {
                e.printStackTrace();
            }
            approve = JOptionPane.showConfirmDialog(main_frame,"Удаляем базу "+target_base_name[0]+" ?","Удаление базы", JOptionPane.YES_NO_OPTION);
            String infobase =(String) target_list.getSelectedValue();
            if(approve == 0){
                try {
                    Docker1C.remove_1c_base(dev_server,target_base_name[0],path_to_1c,(String) server_1c_ver.getSelectedItem());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                final Date d = new Date();
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-YY_HH-mm-ss-S");
                final String date = dateFormat.format(d);
                try {
                    wright_log(date, selected_server[0],source_base_name[0],target_base_name[0],
                            null,true,(String) server_1c_ver.getSelectedItem());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    Docker1C.remove_infobase_from_list(all_user_list, infobase);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                my_bases_only_check_box.setSelected(false);
                target_search.setText(null);
                set_base_list(dev_server,tdb);
                target_buffer = get_items_for_filter(tdb);
                target_list.setModel(tdb);
            }
            disable_spinner();
        });
        quit.addActionListener(actionEvent -> System.exit(0));
        source_list.addListSelectionListener(e -> {
            source_base_name[0] = (String) source_list.getSelectedValue();
            String source_db_size1 = DockerSQL.get_mssql_db_size(source_base_name[0], selected_server[0]);
            String source_creation_date1 = DockerSQL.get_mssql_db_creation_date(source_base_name[0], selected_server[0]);
            source_base_size.setText(source_db_size1 +" MB");
            source_base_creation_date.setText(source_creation_date1);
        });
        target_list.addListSelectionListener(e -> {
            target_base_name[0] = (String) target_list.getSelectedValue();
            String target_db_size1 = DockerSQL.get_mssql_db_size(target_base_name[0], dev_server);
            String target_creation_date1 = DockerSQL.get_mssql_db_creation_date(target_base_name[0], dev_server);
            target_base_size.setText(target_db_size1 +" MB");
            target_base_creation_date.setText(target_creation_date1);
            if(target_base_name[0] != null){
                set_access(target_base_name[0]);
            }
        });
        create_new_db_check_box.addItemListener(e ->
                enable_ui_target(create_new_db_check_box.isSelected()));
        my_bases_only_check_box.addItemListener(e -> {
            if(e.getStateChange() == ItemEvent.SELECTED){
                target_search.setText(System.getProperty("user.name").toLowerCase());
            }
            else {
                target_search.setText(null);
            }
            SwingUtilities.updateComponentTreeUI(main_frame);
        });
        source_search.getDocument().addDocumentListener(new DocumentListener(){
            @Override public void insertUpdate(DocumentEvent e) { filter(); }
            @Override public void removeUpdate(DocumentEvent e) { filter(); }
            @Override public void changedUpdate(DocumentEvent e) { filter();}

            private void filter() {
                enable_spinner();
                DefaultListModel model = (DefaultListModel) source_list.getModel();
                model.clear();
                String s = source_search.getText();
                for (Object o : source_buffer) {
                    if (o.toString().toLowerCase().contains(s)) {
                        model.addElement(o.toString());
                    }
                }
                source_list.setModel(model);
                disable_spinner();
            }
        });
        target_search.getDocument().addDocumentListener(new DocumentListener(){
            @Override public void insertUpdate(DocumentEvent e) { filter(); }
            @Override public void removeUpdate(DocumentEvent e) { filter(); }
            @Override public void changedUpdate(DocumentEvent e) { filter();}
            private void filter() {
                enable_spinner();
                DefaultListModel model1 = (DefaultListModel) target_list.getModel();
                model1.clear();
                String s = target_search.getText();
                if (s == null) {
                    for (Object o : target_buffer) {
                        model1.addElement(o.toString());
                    }
                }
                else {
                    for (Object o : target_buffer) {
                        if (o.toString().toLowerCase().contains(s)) {
                            model1.addElement(o.toString());
                        }
                    }
                }
                target_list.setModel(model1);
                target_list.setCellRenderer(new ListColorRenderer());
                disable_spinner();
            }
        });
        dev_base_renew.addActionListener(actionEvent -> {
            enable_spinner();
            List<String> renewed_list = DockerSQL.get_mssql_db_list(dev_server);
            tdb.clear();
            for (String base : renewed_list){
                tdb.addElement(base);
            }
            target_list.setModel(tdb);
            target_list.setSelectedIndex(0);
            target_list.setCellRenderer(new ListColorRenderer());
            disable_spinner();
        });
        dev_prod_switch.addItemListener(itemEvent -> {
            if (itemEvent.getStateChange() == ItemEvent.SELECTED){
                itemEvent.getItem();
            }
            String res;
            res =(String) dev_prod_switch.getSelectedItem();
            assert res != null;
            enable_ui_prod_dev(res);
        });
        openLogsButton.addActionListener(actionEvent -> {
            StringBuilder text = new StringBuilder();
            try {
                Files.lines(new File(get_property(default_property,"log_file",null)[0]).toPath())
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .forEach(s -> {
                            text.append(s);
                            text.append(System.getProperty("line.separator"));
                        });
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            JEditorPane textArea = new JEditorPane("text", text.toString());
            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setPreferredSize( new Dimension( 800, 500 ) );
            JOptionPane.showMessageDialog(null,scrollPane
                    ,
                    "log"
                    , JOptionPane.INFORMATION_MESSAGE, help_icon);
//            try {
//                Runtime.getRuntime().exec("notepad "+ get_property(default_property,"log_file",null)[0]);
//            }
//            catch (IOException ex){
//                ex.printStackTrace();
//            }
        });
        params_help.addActionListener(actionEvent -> {
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
            scrollPane.setPreferredSize( new Dimension( 800, 500 ) );
            JOptionPane.showMessageDialog(null,scrollPane
                    ,
                    "Информация по парамерам запуска"
                    , JOptionPane.INFORMATION_MESSAGE, help_icon);
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
        run1cButton.addActionListener(actionEvent -> {
            String infobase =(String) target_list.getSelectedValue();
            String version = (String) server_1c_ver.getSelectedItem();
            String rac_port = "";
            for (String v : server_1c_ver_with_port) {
                String[] v1 = v.split(":");
                if (v1[0].equals(version)) {
                    rac_port = v1[1];
                }
            }
            String port =rac_port.substring(0, rac_port.length() -1)+"1";
            String bin = path_to_1c + version + path_1c_exe;
            final String command ="\""+ bin +"\" ENTERPRISE /S\""+dev_server+":"+port+"\\"+infobase + "\"";
            Thread run1c = new Thread(() -> {
                try {
                    Docker1C.run_shell_command(command);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            ScheduledExecutorService scheduler_res = Executors.newSingleThreadScheduledExecutor();
            scheduler_res.schedule(run1c, 1, TimeUnit.SECONDS);
        });
        run1cDesignerButton.addActionListener(actionEvent -> {
            String infobase =(String) target_list.getSelectedValue();
            String version = (String) server_1c_ver.getSelectedItem();
            String rac_port = "";
            for (String v : server_1c_ver_with_port) {
                String[] v1 = v.split(":");
                if (v1[0].equals(version)) {
                    rac_port = v1[1];
                }
            }
            String port =rac_port.substring(0, rac_port.length() -1)+"1";
            String bin = path_to_1c + version + path_1c_exe;
            final String command ="\""+ bin +"\" DESIGNER /S\""+dev_server+":"+port+"\\"+infobase + "\"";
            Thread run1c = new Thread(() -> {
                try {
                    Docker1C.run_shell_command(command);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            ScheduledExecutorService scheduler_res = Executors.newSingleThreadScheduledExecutor();
            scheduler_res.schedule(run1c, 1, TimeUnit.SECONDS);
        });
        run1cAdminConsoleButton.addActionListener(actionEvent -> {
            final String command = get_property(default_property,"1c_console_command",null)[0];
            Thread run1c = new Thread(() -> {
                try {
                    Docker1C.run_shell_command(command);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            ScheduledExecutorService scheduler_res = Executors.newSingleThreadScheduledExecutor();
            scheduler_res.schedule(run1c, 1, TimeUnit.SECONDS);
        });
        run1cWithParamsButton.addActionListener(actionEvent -> {
            String infobase =(String) target_list.getSelectedValue();
            String version = (String) server_1c_ver.getSelectedItem();
            String rac_port = "";
            for (String v : server_1c_ver_with_port) {
                String[] v1 = v.split(":");
                if (v1[0].equals(version)) {
                    rac_port = v1[1];
                }
            }
            String port =rac_port.substring(0, rac_port.length() -1)+"1";
            String bin = path_to_1c + version + path_1c_exe;
            String params = run_1c_custom_params.getText();
            final String command ="\""+ bin +"\" ENTERPRISE /S\""+dev_server+":"+port+"\\"+infobase + "\" "
                    + params;
            Thread run1c = new Thread(() -> {
                try {
                    Docker1C.run_shell_command(command);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            ScheduledExecutorService scheduler_res = Executors.newSingleThreadScheduledExecutor();
            scheduler_res.schedule(run1c, 1, TimeUnit.SECONDS);

        });
        getDtButton.addActionListener(actionEvent -> {
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
            Path localRelativePath13 = Paths.get(String.valueOf(f.getSelectedFile()));
            String path = localRelativePath13.toAbsolutePath().toString();
            Docker1C.run_designer_command(version,dev_server,infobase,path,path_to_1c, "bak dt", quit,spinner);
        });
        addToAllUserButton.addActionListener(actionEvent -> {
            String infobase =(String) target_list.getSelectedValue();
            String base_in_list = null;
            try {
                base_in_list= Docker1C.find_infobase_in_list(all_user_list, infobase);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (base_in_list != null){
                String base_utf8 = new String(base_in_list.getBytes(Charset.defaultCharset()), UTF_8);
                String value = "Base " + base_utf8+" already in list";
                JOptionPane.showMessageDialog(main_frame, value);
            }
            else {
                JFrame f = new JFrame("InputDialog Example #2");
                String base_name = JOptionPane.showInputDialog(
                        f,
                        "Введите имя базы        ",
                        "Имя базы",
                        JOptionPane.QUESTION_MESSAGE);
                //String base_name = new String(b_n.getBytes(Charset.defaultCharset()), UTF_8);
                if (!base_name.equals("")){
                    String ver =(String) server_1c_ver.getSelectedItem();
                    try {
                        Docker1C.add_infobase_to_list(dev_server, ver, infobase, base_name, all_user_list);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        removeFromAllUserButton.addActionListener(actionEvent -> {
            String infobase =(String) target_list.getSelectedValue();
            try {
                Docker1C.remove_infobase_from_list(all_user_list, infobase);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        getCfButton.addActionListener(actionEvent -> {
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
            Path localRelativePath12 = Paths.get(String.valueOf(f.getSelectedFile()));
            String path = localRelativePath12.toAbsolutePath().toString();
            Docker1C.run_designer_command(version,dev_server,infobase,path,path_to_1c, "bak cf", quit,spinner);
        });
        getSqlBakButton.addActionListener(actionEvent -> {
            String base =(String) target_list.getSelectedValue();
            String folder = "\\\\"+dev_server+"\\"+backup_dir;
            get_backup(dev_server,base, base);
            try {
                Desktop.getDesktop().open(new File(folder));
            } catch (IOException e) {
                e.printStackTrace();
            }

        });
        useMyBackupButton.addActionListener(actionEvent -> {
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
            Path localRelativePath1 = Paths.get(String.valueOf(f.getSelectedFile()));
            String path = localRelativePath1.toAbsolutePath().toString();
            if(path.endsWith(".dt")){
                Docker1C.run_designer_command(version,dev_server,infobase,path,path_to_1c, "load dt", quit,spinner);
            }
            else if (path.endsWith(".cf")){
                Docker1C.run_designer_command(version,dev_server,infobase,path,path_to_1c, "load cf", quit,spinner);
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
        });
    }


    public static void main(String[] args) throws IOException, ParseException {
        final Path  localRelativePath = Paths.get(get_property(default_property,"local.property",null)[0]);
        final String local_property = localRelativePath.toAbsolutePath().toString();
        SimpleDateFormat localPropDateFormat=new SimpleDateFormat("dd/MM/yyyy");
        final String path_to_1c = get_property(local_property,"path_to_1c",null)[0];
        String ver = "8.3.16.1148";
        try {
            check_rac(path_to_1c, ver);
        } catch (IOException e) {
            e.printStackTrace();
        }
        check_local_conf(local_property);
        final String lastReportStr = Docker.get_property(local_property,"last_report",null)[0];
        Date now = new Date();
        Date startRepots = localPropDateFormat.parse("30/07/2020");
        if(lastReportStr==null&now.compareTo(startRepots)>0){
            new DockerReports();
        }
        else if(lastReportStr!=null){
            Date lastReport = localPropDateFormat.parse(lastReportStr);
            long diffInMilliesec = Math.abs(lastReport.getTime() - now.getTime());
            long diff  = TimeUnit.DAYS.convert(diffInMilliesec, TimeUnit.MILLISECONDS);
            //int diff = (year - lastReport.getYear())*365+(month - lastReport.getMonth())+(day - lastReport.getDay());
            if (diff>7&now.compareTo(startRepots)>0){
                new DockerReports();
            } else new Docker();
        }
        else new Docker();
    }
}