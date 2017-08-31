package org.wjh.mybatis.generator;

/**
 * Created by bjsunqinwen on 2016/3/25.
 */
public class Model {
    /** 修饰符*/
    private String head;
    /** 对应java类型*/
    private String type;
    /** 普通类属性*/
    private String name;
    /** 首字母大写属性*/
    private String upper_name;
    /** 对应字段注释*/
    private String info;

    public String getHead() {
        return head;
    }

    public void setHead(String head) {
        this.head = head;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public String getUpper_name() {
        return upper_name;
    }

    public void setUpper_name(String upper_name) {
        this.upper_name = upper_name;
    }
}