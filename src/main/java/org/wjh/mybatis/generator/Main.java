package org.wjh.mybatis.generator;

/**
 * Created by bjsunqinwen on 2016/3/24.
 */
public class Main {

    public static void main(String[] args) throws Throwable {
        final String targetSourceDir = "C:\\Users";

        new FileGenerator(targetSourceDir, "module_content", "com.netease.open.classbreak.domain.ModuleContent", true).generateFile();
    }
}
