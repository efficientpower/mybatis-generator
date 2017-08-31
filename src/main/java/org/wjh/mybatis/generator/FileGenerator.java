package org.wjh.mybatis.generator;

import com.google.common.base.Joiner;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.mybatis.generator.api.MyBatisGenerator;
import org.mybatis.generator.config.xml.ConfigurationParser;
import org.mybatis.generator.internal.DefaultShellCallback;

import java.io.*;
import java.util.*;

/**
 * Created by bjsunqinwen on 2016/3/24.
 */
public class FileGenerator {
    /** dao点式路径 */
    private String dao_url_p;
    /** domain点式路径 */
    private String domain_url_p;
    /** 表名称 */
    private String tableName;
    /** 源码生成目录 即opencourse-platform工程目录 */
    private String targetSourceDir;
    /** 完整实体类名 */
    private String domain;
    private boolean overrideIfExists;

    // 以下字段为处理原始字段得到的值
    /** 目标实体类名称 */
    private String simpleDomain;
    /** domain生成目录 */
    private String targetDomainFileDir;
    /** dao生成目录 */
    private String targetDaoFileDir;
    /** mybatis xml 生成目录 */
    private String targetIbatisXmlDir;
    /** dao测试用例目录 */
    private String targetDaoTestDir;

    public FileGenerator(String targetSourceDir, String table, String domain, boolean overrideIfExists) {
        this.targetSourceDir = targetSourceDir;
        this.tableName = table;
        this.domain = domain;
        this.overrideIfExists = overrideIfExists;
        String moduleBasePackage = domain.substring(0, domain.lastIndexOf("."));
        moduleBasePackage = moduleBasePackage.substring(0, moduleBasePackage.lastIndexOf("."));
        final List<String> moduleBaseDirList = new ArrayList<>(Arrays.asList("src", "main", "java"));
        Arrays.asList(StringUtils.split(moduleBasePackage, ".")).forEach(str -> moduleBaseDirList.add(str));
        this.targetDomainFileDir = targetSourceDir + File.separator + Joiner.on(File.separator).join(ListUtils.union(moduleBaseDirList, Arrays.asList("domain")));
        this.targetDaoFileDir = targetSourceDir + File.separator + Joiner.on(File.separator).join(ListUtils.union(moduleBaseDirList, Arrays.asList("dao")));
        this.simpleDomain = domain.substring(domain.lastIndexOf(".") + 1);
        this.targetIbatisXmlDir = Joiner.on(File.separator).join(targetSourceDir, "src", "main", "resources", "mybatis-mapping");
        this.targetDaoTestDir = Joiner.on(File.separator).join(targetSourceDir, "src", "test", "java") + File.separator + Joiner.on(File.separator).join(Arrays.asList(StringUtils.split(moduleBasePackage + ".dao", ".")));
        this.dao_url_p = moduleBasePackage + ".dao";
        this.domain_url_p = moduleBasePackage + ".domain";
    }

    /**
     * 生成文件主函数
     */
    public void generateFile() throws Throwable {
        System.out.println(String.format(">>>>>>>>>>>>>>>>>>>>>>>>>>>%s ---> %s", tableName, domain));
        this.processMBGXml();
        List<String> warnings = new ArrayList<>();
        File configFile = new File("src/main/resources/generatorConfig.xml");
        ConfigurationParser cp = new ConfigurationParser(warnings);
        new MyBatisGenerator(cp.parseConfiguration(configFile), new DefaultShellCallback(true), warnings).generate(null);
        this.editDomain();
        this.addDao();
        processXml();
        this.addTest();
        System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<");
    }

    /** 编辑domain */
    private void editDomain() throws Throwable {
        String fileUrl = this.targetDomainFileDir + File.separator + this.simpleDomain + ".java";
        String fileUrl_Blobs = this.targetDomainFileDir + File.separator + this.simpleDomain + "WithBLOBs.java";
        List<Info> list = (List<Info>) new DB().getColumns(tableName).get("infos");
        List<String[]> fileRead = new ArrayList<>();
        List<String> importStr = new ArrayList<>();
        Map<String, Object> params = this.readDomain(fileUrl, fileRead, importStr);
        fileRead = (List<String[]>) params.get("fileRead");
        importStr = (List<String>) params.get("importStr");
        /**
         * 说明：由于Generator会将数据库大型字段分离成一个新的domainBlobs文件，
         * 导致一张表会出现两个domain文件，因此这里多解析了一步
         */
        File file_blobs = new File(fileUrl_Blobs);
        if (file_blobs.exists()) {
            params = this.readDomain(fileUrl_Blobs, fileRead, importStr);
            file_blobs.delete();// 删除Blobs文件
            /** 重新赋值 */
            fileRead = (List<String[]>) params.get("fileRead");
            importStr = (List<String>) params.get("importStr");
        }

        if (list.size() != fileRead.size()) {
            throw new RuntimeException("统计生成的domain字段与实际不符！");
        }
        List<Model> result = new ArrayList<>();

        for (String[] str : fileRead) {
            String fileInfoName = str[2].trim().substring(0, str[2].trim().indexOf(";"));
            Model model = new Model();
            model.setHead(str[0]);

            /** 目前generator生成的domain属性顺序与数据库不一致，所以目前只能嵌套循环----待优化 */
            for (Info info : list) {
                if (fileInfoName.equals(info.getDoamin_column_name())) {
                    String fieldComment = info.getColumn_info();
                    StringReader sr = new StringReader(fieldComment);
                    List<String> lines = new ArrayList<>();
                    IOUtils.readLines(sr).forEach(line -> lines.add(StringUtils.trim(line)));
                    IOUtils.closeQuietly(sr);
                    if (!StringUtils.isBlank(fieldComment)) {
                        if (lines.size() > 1) {
                            model.setInfo("/**\n     * " + Joiner.on("\n     * ").join(lines) + "\n     */");
                        } else {
                            model.setInfo("/** " + fieldComment + " */");
                        }
                    } else {
                        model.setInfo("");
                    }
                }
            }
            model.setName(fileInfoName);
            model.setUpper_name(fileInfoName.substring(0, 1).toUpperCase() + fileInfoName.substring(1, fileInfoName.length()));
            model.setType(str[1]);
            result.add(model);

        }
        File file = new File(fileUrl);
        file.delete();
        if (!file.exists()) {
            Map<String, Object> ctx = new HashMap<>();
            ctx.put("domainPath", this.domain_url_p);
            ctx.put("domainName", simpleDomain);
            ctx.put("list", result);
            ctx.put("importList", importStr);
            BufferedWriter out = new BufferedWriter(new FileWriter(file));
            IOUtils.write(getVM(ctx, "Domain.vm"), out);
            IOUtils.closeQuietly(out);
        } else {
            System.out.println("domain file exists ,skiped");
        }
    }

    /** 生成Dao层代码 */
    private void addDao() throws IOException {
        File daoFileDir = new File(this.targetDaoFileDir);
        if (!daoFileDir.exists() && !daoFileDir.isDirectory()) {
            daoFileDir.mkdirs();
        }
        File daoFile = new File(this.targetDaoFileDir + File.separator + simpleDomain + "Dao.java");
        if (!daoFile.exists() || overrideIfExists) {
            daoFile.createNewFile();
            Map<String, Object> ctx = new HashMap<>();
            ctx.put("daoPath", this.dao_url_p);
            ctx.put("className", simpleDomain);
            ctx.put("classNameLowCase", simpleDomain.substring(0, 1).toLowerCase() + simpleDomain.substring(1));
            ctx.put("entityPath", this.domain_url_p);
            BufferedWriter out = new BufferedWriter(new FileWriter(daoFile));
            IOUtils.write(getVM(ctx, "AddDao.vm"), out);
            IOUtils.closeQuietly(out);
        } else {
            System.out.println("dao file exists,skiped");
        }
    }

    private void processXml() throws Throwable {
        String xmlPath = targetIbatisXmlDir + File.separator + simpleDomain + "Mapper.xml";
        String processedXmlPath = targetIbatisXmlDir + File.separator + "mybatis-" + tableName.replace('_', '-') + ".xml";
        InputStream is = new FileInputStream(xmlPath);
        List<String> removeCommentList = new ArrayList<>();
        boolean[] flag = { false };
        IOUtils.readLines(is).forEach(line -> {
            if (line.contains("namespace=\"/." + simpleDomain)) {
                line = StringUtils.replace(line, "/." + simpleDomain + "Mapper", dao_url_p + "." + simpleDomain + "Dao");
            }
            if (line.contains(simpleDomain + "WithBLOBs")) {
                line = StringUtils.replace(line, simpleDomain + "WithBLOBs", simpleDomain);
            }
            if (line.contains("\"selectByPrimaryKey\"")) {
                line = StringUtils.replace(line, domain, "java.lang.Integer");// bad
                line = StringUtils.replace(line, "selectByPrimaryKey", "getById");// bad
            }
            if (line.contains("\"deleteByPrimaryKey\"")) {
                line = StringUtils.replace(line, domain, "java.lang.Integer");// bad
                line = StringUtils.replace(line, "deleteByPrimaryKey", "deleteById");// bad
            }
            if (line.contains("\"updateByPrimaryKey\"")) {
                line = StringUtils.replace(line, domain, "java.lang.Integer");// bad
                line = StringUtils.replace(line, "updateByPrimaryKey", "update");// bad
            }
            if (line.contains("\"updateByPrimaryKeySelective\"")) {
                line = StringUtils.replace(line, domain, "java.lang.Integer");// bad
                line = StringUtils.replace(line, "updateByPrimaryKeySelective", "updateSelectively");// bad
            }
            if (line.trim().startsWith("<!--")) {
                flag[0] = true;
            }
            if (flag[0]) {
                if (line.trim().endsWith("-->")) {
                    flag[0] = false;
                }
            } else {
                removeCommentList.add(StringUtils.replace(line, tableName + ".", simpleDomain + "."));
            }
        });
        IOUtils.closeQuietly(is);
        removeCommentList.addAll(removeCommentList.size() - 1, addListByTypeToDoc());

        File processedXmlFile = new File(processedXmlPath);
        if (processedXmlFile.exists()) {
            processedXmlFile.delete();
        }
        OutputStream os = new FileOutputStream(processedXmlPath);
        IOUtils.writeLines(removeCommentList, "\n", os, "UTF-8");

        File oldFile = new File(xmlPath);
        if (oldFile.exists()) {
            oldFile.delete();
        }
    }

    private List<String> addListByTypeToDoc() throws Throwable {
        Map<String, Object> ctx = new HashMap<>();
        Map<String, Object> result = new DB().getColumns(tableName);
        ctx.put("infoList", result.get("infos"));
        ctx.put("domain_url_p", this.domain_url_p);
        ctx.put("domainName", simpleDomain);
        ctx.put("tableName", tableName);
        ctx.put("pri_key", result.get("pri_key"));
        ctx.put("pri_key_upper", DB.firstUpperCase((String) result.get("pri_key")));
        ctx.put("pri_key_type", result.get("pri_key_type"));
        StringReader sr = new StringReader(getVM(ctx, "xmlList.vm"));
        List<String> lines = IOUtils.readLines(sr);
        IOUtils.closeQuietly(sr);
        return lines;
    }

    /** 生成测试用例代码 */
    private void addTest() throws IOException {
        File daoFileDir = new File(this.targetDaoTestDir);
        if (!daoFileDir.exists() && !daoFileDir.isDirectory()) {
            daoFileDir.mkdirs();
        }
        File daoFile = new File(this.targetDaoTestDir + File.separator + simpleDomain + "DaoTest.java");
        if (!daoFile.exists()) {
            daoFile.createNewFile();
            Map<String, Object> ctx = new HashMap<>();
            ctx.put("daoPath", this.dao_url_p);
            ctx.put("domainName", simpleDomain);
            ctx.put("s_domainName", simpleDomain.substring(0, 1).toLowerCase() + simpleDomain.substring(1));
            ctx.put("entityPath", this.domain_url_p);
            BufferedWriter out = new BufferedWriter(new FileWriter(daoFile));
            IOUtils.write(getVM(ctx, "Test.vm"), out);
            out.close();
        } else {
            System.out.println("daoTest file exists,skiped");
        }
    }

    /** 模板触发公共类，返回生成的内容 */
    private String getVM(Map<String, Object> params, String tplStr) {
        VelocityEngine ve = new VelocityEngine();
        ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
        ve.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
        ve.init();
        Template tpl = ve.getTemplate(tplStr);
        VelocityContext ctx = new VelocityContext();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            ctx.put(entry.getKey(), entry.getValue());
        }
        StringWriter sw = new StringWriter();
        tpl.merge(ctx, sw);
        return sw.toString();
    }

    /** 用来初始化generatorConfig.xml文件 */
    private void processMBGXml() throws Exception {
        String filePath = System.getProperty("user.dir") + File.separator + "/src/main/resources/generatorConfig.xml";
        SAXReader reader = new SAXReader();
        // 为了防止网络问题导致dtd无法下载而解析失败,禁用dtd,前提是确定xml文件一定正确
        reader.setValidation(false);
        reader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        Document doc = reader.read(new File(filePath));
        Element root = doc.getRootElement();

        Element classPathEntry = root.element("classPathEntry");
        classPathEntry.attribute("location").setValue(Joiner.on(File.separator).join(System.getProperty("user.dir"), "src", "main", "resources"));

        Element context = root.element("context");

        Element jdbcConnection = context.element("jdbcConnection");
        jdbcConnection.attribute("connectionURL").setValue(DB.CONFIGS.getProperty("jdbc.url"));
        jdbcConnection.attribute("userId").setValue(DB.CONFIGS.getProperty("jdbc.user"));
        jdbcConnection.attribute("password").setValue(DB.CONFIGS.getProperty("jdbc.pwd"));

        Element javaModelGenerator = context.element("javaModelGenerator");
        javaModelGenerator.attribute("targetPackage").setValue(domain.substring(0, domain.lastIndexOf(".")));
        javaModelGenerator.attribute("targetProject").setValue(Joiner.on(File.separator).join(targetSourceDir, "src", "main", "java"));

        Element sqlMapGenerator = context.element("sqlMapGenerator");
        sqlMapGenerator.attribute("targetPackage").setValue("/");
        sqlMapGenerator.attribute("targetProject").setValue(Joiner.on(File.separator).join(targetSourceDir, "src", "main", "resources", "mybatis-mapping"));

        Element table = context.element("table");
        table.attribute("tableName").setValue(tableName);
        table.attribute("domainObjectName").setValue(simpleDomain);

        OutputFormat format = OutputFormat.createPrettyPrint();
        format.setEncoding("UTF-8");
        XMLWriter writer = new XMLWriter(new FileOutputStream(filePath), format);
        writer.write(doc);
        writer.close();
    }

    /** 读取domain内容 */
    private Map<String, Object> readDomain(String url, List<String[]> fileRead, List<String> importStr)
            throws Exception {
        InputStream is = new FileInputStream(url);
        IOUtils.readLines(is).forEach(line -> {
            if (line.contains("private")) {
                String[] str = line.trim().split(" ");
                fileRead.add(str);
            } else if (line.contains("import")) {
                importStr.add(line);
            }
        });
        IOUtils.closeQuietly(is);
        Map<String, Object> result = new HashMap<>();
        result.put("fileRead", fileRead);
        result.put("importStr", importStr);
        return result;
    }

}
