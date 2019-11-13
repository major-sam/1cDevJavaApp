package com.docker;


import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

class ListColorRenderer extends DefaultListCellRenderer {
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus ) {
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
        if ((year - db_creation_year )*12 +db_creation_month - month <=1){
            c.setBackground(Color.GREEN);
        }//TODO archive list
        else{
            c.setBackground( Color.RED );
        }
        if(isSelected){
            c.setBackground( Color.LIGHT_GRAY);
        }
        return c;
    }
}