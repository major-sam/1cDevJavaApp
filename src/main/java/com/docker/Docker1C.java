package com.docker;

import java.io.*;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

class Docker1C {
    static void create_1c_base(String server_1c, String server_sql, String ver,
                               String db_name, String path_to_1c,
                               String base_name, String description) throws IOException {
        String b = path_to_1c + ver + Docker.path_1c_exe;
        String base_1c = Paths.get(b).toString();
        String port = "";
        String rac_port= "";
        String[] server_1c_ver_with_ras = Docker.get_property(Docker.default_property, "server_1c_ver_with_ras", ",");
        for (String v : server_1c_ver_with_ras) {
            String[] v1 = v.split(":");
            if (v1[0].equals(ver)) {
                rac_port = v1[1];
                if (rac_port.length() > 4) {
                    port = ":"+rac_port.substring(0, 3) + "41";
                }
            }
        }
        String create_db = "CREATEINFOBASE Srvr=" + server_1c + port + ";Ref=" + db_name + ";DBMS=MSSQLServer;DBSrvr=" + server_sql +
                ";DB=" + db_name + ";CrSQLDB=Y";
        String[] command = new  String[]{base_1c, create_db};
        Docker.run_shell_command(command);
        String rac_base = server_1c + ":" + rac_port;
        String cluster_id = get_cluster_id(rac_base);
        String infobase_id = get_infobase_id(db_name,rac_base,cluster_id);
        set_infobase_description(infobase_id,rac_base,cluster_id,description);
    }
    private static String get_cluster_id(String rac_base) throws IOException {
        String cluster_list = " cluster list";
        String cluster_id = null;
        ArrayList<String> clusters = Docker.run_shell_command(new String[]{rac_base, cluster_list});
        for (String line : clusters) {
            if (line.startsWith("cluster")) {
                cluster_id = line.split(":")[1].replace(" ", "");
            }
        }
        return cluster_id;
    }
    private static String get_infobase_id(String base_1c, String rac_base, String cluster_id) throws IOException {
        String db_id_salt = " infobase --cluster="+cluster_id+" summary list";
        ArrayList<String> info_bases = Docker.run_shell_command(new String[]{rac_base,db_id_salt});
        String info_base_id = null;
        for (String line : info_bases) {
            if (line.startsWith("infobase")) {
                info_base_id = line.split(":")[1].replace(" ","");
            }
            if (line.startsWith("name")) {
                String n = line.split(":")[1].replace(" ","");
                if (n.equals(base_1c)){
                    break;
                }
            }
        }
        return info_base_id;
    }
    private static void set_infobase_description(String infobase_id, String rac_base, String cluster_id,
                                                 String description) throws IOException {
        Date date = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("YYMMdd");
        description = description + "," + dateFormat.format(date);
        String command_line = " infobase --cluster=" + cluster_id + " update --infobase=" + infobase_id
                + " --descr=\"" + description + "\"";
        Docker.run_shell_command(new String[]{rac_base,command_line});
    }
    private static String get_infobase_description(String infobase_id, String rac_base, String cluster_id
                                                    ) throws IOException {
        String command_line=" infobase --cluster=" + cluster_id + "info --infobase="+infobase_id;
        ArrayList<String> infobase = Docker.run_shell_command(new String[]{rac_base,command_line});
        String description = "";
        for (String line: infobase){
            if (line.startsWith("descr")) {
                description = line.split(":")[1].replace(" ", "");
            }
        }
        return description;
    }
    private void remove_1c_base(String server_1c, String base_name, String path_to_1c, String path_to_rac, String ver_1c) throws IOException {
        String rac_port = "";
        String[] server_1c_ver_with_port = Docker.get_property(Docker.default_property, "server_1c_ver_with_ras", ",");
        for (String v: server_1c_ver_with_port){
            String[] v1 = v.split(":");
            if (v1[0].equals(ver_1c)){
                rac_port = v1[1];
            }
        }
        String rac_base = path_to_1c + ver_1c + path_to_rac + " "+ server_1c +":"+rac_port;
        String cluster_id = get_cluster_id(rac_base);
        String infobase_id = get_infobase_id(base_name,rac_base,cluster_id);
        String remove_1c_db = " infobase --cluster="+cluster_id+ "drop --drop-database --infobase="+infobase_id;
        Docker.run_shell_command(new String[]{rac_base, remove_1c_db});
    }
}
