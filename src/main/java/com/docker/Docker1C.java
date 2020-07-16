package com.docker;


import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

class Docker1C {
    static ArrayList<String> run_shell_command(String command) throws IOException {
        ProcessBuilder builder = new ProcessBuilder("cmd", "/c","\""+command+"\"");
        builder.redirectErrorStream(true);
        Process process = builder.start();
        InputStream is = process.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line;
        ArrayList<String> lines = new ArrayList<>();
        while ((line = reader.readLine()) != null) {
            lines.add(line);
        }
        return lines;
    }
    static void create_1c_base(String server_1c, String server_sql, String ver,
                               String infobase_name, String path_to_1c, String description) throws IOException {
        String b = path_to_1c + ver + Docker.path_rac_exe;
        String rac_bin = Paths.get(b).toString();
        String rac_port= "";
        String[] server_1c_ver_with_ras = Docker.get_property(Docker.default_property, "server_1c_ver_with_ras", ",");
        for (String v : server_1c_ver_with_ras) {
            String[] v1 = v.split(":");
            if (v1[0].equals(ver)) {
                rac_port = v1[1];
            }
        }
        String rac_service = server_1c + ":" + rac_port;
        String cluster_id = get_cluster_id(rac_bin, rac_service);
        String create_db =  " infobase create --cluster="+cluster_id+" --create-database --name="+infobase_name+
              "  --dbms=MSSQLServer  --locale=ru_RU --db-server="+server_sql+"  --db-name="+infobase_name +
                "  --descr=\""+description +"\" " +rac_service ;
        String command ="\""+ rac_bin +"\""+ create_db;
        run_shell_command(command);
    }
    private static String get_cluster_id(String rac_bin, String rac_service) throws IOException {
        String cluster_id = null;
        String command= "\""+ rac_bin +"\""+ " " +rac_service +  " cluster list" ;
        ArrayList<String> clusters = run_shell_command(command);
        for (String line : clusters) {
            if (line.startsWith("cluster")) {
                cluster_id = line.split(":")[1].replace(" ", "");
                break;
            }
        }
        return cluster_id;
    }
    private static String get_infobase_id(String base_name, String rac_service, String rac_bin, String cluster_id)
            throws IOException {
        String command ="\""+ rac_bin +"\""+ " infobase --cluster="+cluster_id+" summary list "+rac_service;
        ArrayList<String> info_bases = run_shell_command(command);
        String info_base_id = null;
        for (String line : info_bases) {
            if (line.startsWith("infobase")) {
                info_base_id = line.split(":")[1].replace(" ","");
            }
            if (line.startsWith("name")) {
                String n = line.split(":")[1].replace(" ","");
                if (n.equals(base_name)){
                    break;
                }
            }
        }
        return info_base_id;
    }
    static void set_infobase_description(String server_1c, String ver, String path_to_1c, String infobase_name,
                                         String description) throws IOException {
        String b = path_to_1c + ver + Docker.path_rac_exe;
        String rac_bin = Paths.get(b).toString();
        String rac_port= "";
        String[] server_1c_ver_with_ras = Docker.get_property(Docker.default_property, "server_1c_ver_with_ras", ",");
        for (String v : server_1c_ver_with_ras) {
            String[] v1 = v.split(":");
            if (v1[0].equals(ver)) {
                rac_port = v1[1];
            }
        }

        String rac_service = server_1c + ":" + rac_port;

        String cluster_id = get_cluster_id(rac_bin, rac_service);
        String infobase_id = get_infobase_id(infobase_name, rac_service, rac_bin, cluster_id);
        String command=rac_bin+" infobase update --cluster=" + cluster_id + " --infobase="+infobase_id + " --descr=\"" +description+ "\"";
        run_shell_command(command);
    }
    static void remove_1c_base(String server_1c, String base_name, String path_to_1c,  String ver) throws IOException {
        String b = path_to_1c + ver + Docker.path_rac_exe;
        String rac_bin = Paths.get(b).toString();
        String rac_port= "";
        String[] server_1c_ver_with_ras = Docker.get_property(Docker.default_property, "server_1c_ver_with_ras", ",");
        for (String v : server_1c_ver_with_ras) {
            String[] v1 = v.split(":");
            if (v1[0].equals(ver)) {
                rac_port = v1[1];
            }
        }

        String rac_service = server_1c + ":" + rac_port;
        String cluster_id = get_cluster_id(rac_bin,rac_service);
        String infobase_id = get_infobase_id(base_name,rac_service,rac_bin,cluster_id);
        String command = "\""+ rac_bin +"\"" + " "+ rac_service+" infobase --cluster="+cluster_id+ " drop --infobase="+infobase_id;
        ArrayList<String> lines = run_shell_command(command);
        for (Object line : lines){
            System.out.println(line.toString());
        }
        DockerSQL.remove_db(server_1c,base_name);
        String[] share_for_1c_lists= Docker.get_property(Docker.default_property, "share_for_1c_lists", null);
        String path = Paths.get(share_for_1c_lists[0]).toString() + System.getProperty("user.name").toLowerCase() + ".v8i";
        remove_infobase_from_list(path, base_name);

    }
    static void add_infobase_to_list(String server_1c, String ver, String infobase, String infobase_name, String filename)
            throws IOException {
        String rac_port , server_1c_port = "";
        String[] server_1c_ver_with_ras = Docker.get_property(Docker.default_property, "server_1c_ver_with_ras", ",");
        String[] share = Docker.get_property(Docker.default_property, "share_for_1c_lists", null);
        String share_for_1c_lists = Paths.get(share[0]).toString();
        for (String v : server_1c_ver_with_ras) {
            String[] v1 = v.split(":");
            if (v1[0].equals(ver)) {
                rac_port = v1[1];
                if (rac_port.length() > 4) {
                    server_1c_port = ":" + rac_port.substring(0, 3) + 41;
                }
            }
        }
        String srvr = server_1c + server_1c_port;
        String infobase_settings;
        Path path;
        if (!filename.equals(Docker.get_property(Docker.default_property, "all_user_list", null)[0])) {
            infobase_settings = "[" + infobase_name + "]\r\n" +
                    "Connect=Srvr=\"" + srvr + "\";Ref=\"" + infobase + "\"\r\n" +
                    "Folder=/DEV/" + filename + "/\r\n";
            path = Paths.get(share_for_1c_lists + filename + ".v8i");
        }
        else{
            infobase_settings = "[" + infobase_name + "]\r\n" +
                    "Connect=Srvr=\"" + srvr + "\";Ref=\"" + infobase + "\"\r\n" +
                    "Folder=/TEST INFOBASE/" + "\r\n";
            path = Paths.get(filename );
        }
        Files.write(path, Collections.singletonList(infobase_settings), StandardCharsets.UTF_8,
                Files.exists(path) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE);
    }
    static void remove_infobase_from_list(String file,String infobase) throws IOException {
        BufferedReader in = new BufferedReader(new FileReader(file)) ;
        String line_in;
        StringBuilder line_out = new StringBuilder();
        int index_in = 0, index_out = 0;
        ArrayList<Integer> lines_index = new ArrayList<>();
        ArrayList<String> lines = new ArrayList<>();
        while ((line_in = in.readLine()) != null) {
            lines.add(line_in);
            if(line_in.contains(infobase)){
                lines_index.add(index_in -1);
                lines_index.add(index_in);
                lines_index.add(index_in +1);
                lines_index.add(index_in +2);
            }
            index_in++;
        }
        for (String line:lines){
            if (!lines_index.contains(index_out)){
                line_out.append(line).append("\r\n");
            }
            index_out++;
        }
        new File(file);
        String l = line_out.toString();
        byte[] strToBytes = l.getBytes();
        Files.write(Paths.get(file), strToBytes);
    }
    static String find_infobase_in_list(String file,String infobase) throws IOException {
        String is_base_found = null;
        BufferedReader in = new BufferedReader(new FileReader(file)) ;
        String line_in;
        String prev_line = null;
        while ((line_in = in.readLine()) != null) {
            if(line_in.contains(infobase)){
                is_base_found = prev_line;
            }
            prev_line = line_in;
        }
        if (is_base_found != null){
            return is_base_found.replace("[","").replace("]","");
        }
        else return null;
    }
    static void run_designer_command(String version, String server, String infobase, String path, String path_to_1c, String format, JButton quit, JLabel spinner){
        quit.setEnabled(false);
        spinner.setEnabled(true);
        ImageIcon icon = new ImageIcon("conf/spinner.gif");
        Image img = icon.getImage();
        Image image = img.getScaledInstance(54,54,Image.SCALE_REPLICATE);
        icon = new ImageIcon(image);
        spinner.setIcon(icon);
        String[] server_1c_ver_with_port=Docker.get_property(Docker.default_property, "server_1c_ver_with_ras", ",");
        String p =  server_1c_ver_with_port[0].split(":")[1];
        String port = p.substring(0, p.length() -1)+"1";
        String bin = path_to_1c + version +  Docker.path_1c_exe;
        String param = "";
        String f = "";
        String msg ="";
        if (format.equals("bak cf")){
            f = ".cf";
            param = "/DumpCfg";
            msg =".cf creating is done";
        }
        else if (format.equals("bak dt")){
            f = ".dt";
            param = "/DumpIB";
            msg =".dt creating is done";
        }
        if (format.equals("load cf")){
            f = ".cf";
            param = "/LoadCfg";
            msg =".cf uploading is done";
        }
        else if (format.equals("load dt")){
            f = ".dt";
            param = "/RestoreIB";
            msg =".dt uploading is done";
        }
        final boolean[] status= {false};
        final String command = "\""+ bin +"\"" +" DESIGNER /S\""+server+":"+port+"\\"+infobase + "\" " +param+" "
                + path+"\\"+infobase+ f;
        Thread run1c = new Thread(() -> {
            try {
                Docker1C.run_shell_command(command);
            } catch (IOException e) {
                e.printStackTrace();
            }
            status[0] = true;
        });
        ScheduledExecutorService scheduler_res = Executors.newSingleThreadScheduledExecutor();
        scheduler_res.schedule(run1c, 1, TimeUnit.SECONDS);
        final ScheduledExecutorService watcher = Executors.newSingleThreadScheduledExecutor();
        String finalMsg = msg;
        watcher.scheduleAtFixedRate(() -> {
            if (status[0]) {
                quit.setEnabled(true);
                spinner.setEnabled(false);
                spinner.setIcon(null);
                JOptionPane.showConfirmDialog(null,
                        finalMsg, "Done", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE);
                Thread.currentThread().interrupt();
                watcher.shutdown();
            }
        },0,2, TimeUnit.SECONDS);
    }
}

