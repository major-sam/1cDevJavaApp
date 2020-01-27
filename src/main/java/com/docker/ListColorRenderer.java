package com.docker;


import javax.swing.*;
import java.awt.*;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

class ListColorRenderer extends DefaultListCellRenderer {
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus ) {
        Component c = super.getListCellRendererComponent( list, value, index, isSelected, cellHasFocus );
        String db_name = value.toString();
        final String dev_server =  Docker.get_property(Docker.default_property,"dev_server",null)[0];
        String db_cd = DockerSQL.get_mssql_db_creation_date(db_name, dev_server);
        String[] db_creation_date = {"0","0"};
        if (db_cd != null){
            db_creation_date = db_cd.split("\\.");
        }
        int db_creation_year = Integer.parseInt(db_creation_date[0]);
        int db_creation_month = Integer.parseInt(db_creation_date[1]);
        Date d = new Date();
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Europe/Moscow"));
        cal.setTime(d);
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        int base_max_age_in_month =Integer.parseInt(Docker.get_property(Docker.default_property, "base_max_age_in_month", null)[0]);
        int i = (year - db_creation_year) * 12 - db_creation_month + month;
        if (i <= base_max_age_in_month){
            c.setBackground(Color.GREEN);
        }
        else{
            c.setBackground( Color.PINK );
        }
        if(isSelected){
            c.setBackground( Color.LIGHT_GRAY);
        }
        return c;
    }
}