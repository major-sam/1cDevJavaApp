package com.docker;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

class DockerSQL {


    private static String user_name = Docker.user_name;
    private static String user_password = Docker.user_password;

    static int get_mssql_progress(String q, String server, String backup_name){
        Connection conn1;
        Statement stmt1;
        ResultSet rs1;
        String query = "SELECT r.session_id AS [Session_Id]\n"+
                ",CONVERT(NUMERIC(6, 0), r.percent_complete) AS [Complete]\n"+
                ",CONVERT(VARCHAR(1000), (\n"+
                "SELECT SUBSTRING(TEXT, r.statement_start_offset / 2,1000)  \n"+
                "FROM sys.dm_exec_sql_text(sql_handle)\n"+
                "))as 'query'\n"+
                "FROM sys.dm_exec_requests r\n"+
                "WHERE command like "+q;
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
            System.out.println("SQLException: "+ex.getMessage());
            System.out.println("SQLState: "+ex.getSQLState());
            System.out.println("VendorError: "+ex.getErrorCode());
        }
        return  progress;
    }
    static String get_mssql_db_creation_date(String basename, String server) {
        Connection conn;
        Statement stmt;
        ResultSet rs;
        String file_creation_date = null;
        try {
            String url = "jdbc:sqlserver://" + server + ";user=" + user_name + ";password=" + user_password + "";
            conn = DriverManager.getConnection(url);
            stmt = conn.createStatement();
            String query = "SELECT create_date\n" +
                    "FROM sys.databases\n" +
                    "WHERE name = '" + basename + "'";
            rs = stmt.executeQuery(query);
            if (stmt.execute(query)) {
                rs = stmt.getResultSet();
            }
            while (rs.next()) {
                file_creation_date = rs.getString("create_date").split(" ")[0].replace("-", ".");
            }
        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }
        return file_creation_date;
    }
    static String get_mssql_db_size(String basename, String server){
        Connection conn;
        Statement stmt;
        ResultSet rs;
        String file_size = null;
        try {
            String url ="jdbc:sqlserver://"+server+";user="+user_name+";password="+user_password+"";
            conn = DriverManager.getConnection(url);
            stmt = conn.createStatement();
            String query="SELECT\n"+
                    "    D.name,\n"+
                    "    Substring((F.physical_name),0,3)  AS Drive,\n"+
                    "    CAST((F.size*8)/1024 AS VARCHAR(26))  AS FileSize\n"+
                    "FROM \n"+
                    "    sys.master_files F\n"+
                    "    INNER JOIN sys.databases D ON D.database_id = F.database_id\n"+
                    "Where type=0 and F.database_id >4 and D.name like '"+basename+"'";
            rs = stmt.executeQuery(query);
            if (stmt.execute(query)) {
                rs = stmt.getResultSet();
            }
            while (rs.next()){
                file_size=rs.getString("FileSize");
            }
        }
        catch (SQLException ex){
            System.out.println("SQLException: "+ex.getMessage());
            System.out.println("SQLState: "+ex.getSQLState());
            System.out.println("VendorError: "+ex.getErrorCode());
        }
        return file_size;
    }
    static String get_mssql_path(String query, String server){
    Connection conn;
    Statement stmt;
    ResultSet rs;
    String path = null;
    try {
        String url ="jdbc:sqlserver://"+server+";user="+user_name+";password="+user_password+"";
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
        System.out.println("SQLException: "+ex.getMessage());
        System.out.println("SQLState: "+ex.getSQLState());
        System.out.println("VendorError: "+ex.getErrorCode());
    }
    return path;
    }
    static String[] get_mssql_free_space(String server){
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
        String url ="jdbc:sqlserver://"+server+";user="+user_name+";password="+user_password+"";
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
        System.out.println("SQLException: "+ex.getMessage());
        System.out.println("SQLState: "+ex.getSQLState());
        System.out.println("VendorError: "+ex.getErrorCode());
    }
    return new String[]{disk_space.toString(), bak_disk_free, sql_db_disk_free};
    }
    static List get_mssql_db_list(String server){
    Connection conn;
    Statement stmt;
    ResultSet rs;
    String query="SELECT name FROM master.dbo.sysdatabases  where dbid >4";
    List<String> work_base = new ArrayList<>();

    try {
        String url ="jdbc:sqlserver://"+server+";user="+user_name+";password="+user_password+"";
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
        System.out.println("SQLException: "+ex.getMessage());
        System.out.println("SQLState: "+ex.getSQLState());
        System.out.println("VendorError: "+ex.getErrorCode());
    }
    return work_base;
    }
    static void remove_backup(String server,String backup_name ){
        Connection conn1;
        Statement stmt1;
        String query = "DECLARE @path NVARCHAR(4000)\n" +
                "DECLARE @bak NVARCHAR(4000)\n" +
                "EXEC master.dbo.xp_instance_regread\n" +
                "            N'HKEY_LOCAL_MACHINE',\n" +
                "            N'Software\\Microsoft\\MSSQLServer\\MSSQLServer',N'BackupDirectory',\n" +
                "            @path OUTPUT, \n" +
                "            'no_output'\n" +
                "set @bak = @path +'\\"+backup_name+".bak'\n" +
                "EXECUTE master.dbo.xp_delete_file 0,@bak";
        try {
            String url ="jdbc:sqlserver://"+server+";user="+user_name+";password="+user_password+"";
            conn1 = DriverManager.getConnection(url);
            stmt1 = conn1.createStatement();
            stmt1.executeQuery(query);
        }
        catch (SQLException ex){
            System.out.println("SQLException: "+ex.getMessage());
            System.out.println("SQLState: "+ex.getSQLState());
            System.out.println("VendorError: "+ex.getErrorCode());
        }
    }
    static void remove_db(String server, String db_name){
        Connection conn1;
        Statement stmt1;
        String query = "EXEC msdb.dbo.sp_delete_database_backuphistory @database_name = N'"+ db_name +"'\n" +
                "USE [master]\n" +
                "DROP DATABASE ["+db_name+"];";
        try {
            String url ="jdbc:sqlserver://"+server+";user="+user_name+";password="+user_password+"";
            conn1 = DriverManager.getConnection(url);
            stmt1 = conn1.createStatement();
            stmt1.executeQuery(query);
        }
        catch (SQLException ex){
            System.out.println("SQLException: "+ex.getMessage());
            System.out.println("SQLState: "+ex.getSQLState());
            System.out.println("VendorError: "+ex.getErrorCode());
        }
    }

}
