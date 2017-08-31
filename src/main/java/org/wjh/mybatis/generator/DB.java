package org.wjh.mybatis.generator;

import com.google.common.base.CaseFormat;
import java.io.InputStream;
import java.sql.*;
import java.util.*;

/**
 * Created by bjsunqinwen on 2016/3/24.
 */
public class DB {

    public static final Properties CONFIGS = new Properties();
    static {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            InputStream is = DB.class.getResourceAsStream("/base.properties");
            CONFIGS.load(is);
            is.close();
        }catch (Exception e){
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private String url;
    private String userName;
    private String password;

    public DB(){
        this.url = CONFIGS.getProperty("jdbc.url");
        this.userName = CONFIGS.getProperty("jdbc.user");
        this.password = CONFIGS.getProperty("jdbc.pwd");
    }

    public Map<String, Object> getColumns(String tableName) throws Throwable{
        Map<String, Object> result = new HashMap<String, Object>();
        Connection conn = DriverManager.getConnection(url, userName, password);
        List<Info> infoList = new ArrayList<Info>();
        try {
            String sql = "SHOW FULL FIELDS FROM " + tableName;
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            String pri_key = "";
            String pri_key_type = "";
            while (rs.next()) {
                if("PRI".equals(rs.getString(5))){
                    pri_key = rs.getString(1);
                    String type = rs.getString(2);
                    pri_key_type = type.trim().contains("(") ? type.substring(0, type.indexOf('(')).toUpperCase() : type.toUpperCase();
                }
                Info info = new Info();
                info.setOriginal_column_name(rs.getString(1));
                info.setDoamin_column_name(DB.firstUpperCase(rs.getString(1)));
                String type = rs.getString(2);
                info.setTable_type(type.trim().contains("(") ? type.substring(0, type.indexOf('(')).toUpperCase() : type.toUpperCase());
                info.setColumn_info(rs.getString(9));
                infoList.add(info);
            }

            /** 查找主键*/
            result.put("infos", infoList);
            result.put("pri_key", pri_key);
            result.put("pri_key_type", pri_key_type);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            conn.close();
        }
        return result;
    }

    /** 字段处理源，下划线去掉后第一个字母大写*/
    public static String firstUpperCase(String original_world){
        return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL,original_world);
    }
}