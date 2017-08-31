package org.wjh.mybatis.generator;

/**
 * Created by bjsunqinwen on 2016/3/24.
 */
public class Info {

    private String original_column_name;
    private String doamin_column_name;
    private String table_type;
    private String column_info;

    public String getOriginal_column_name() {
        return original_column_name;
    }

    public void setOriginal_column_name(String original_column_name) {
        this.original_column_name = original_column_name;
    }

    public String getDoamin_column_name() {
        return doamin_column_name;
    }

    public void setDoamin_column_name(String doamin_column_name) {
        this.doamin_column_name = doamin_column_name;
    }

    public String getTable_type() {
        return table_type;
    }

    public void setTable_type(String table_type) {
        this.table_type = table_type;
    }

    public String getColumn_info() {
        return column_info;
    }

    public void setColumn_info(String column_info) {
        this.column_info = column_info;
    }
}
