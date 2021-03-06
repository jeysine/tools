package utils.code.generate.gentype.database;

import freemarker.template.Template;
import utils.code.generate.ColumnClass;
import utils.code.generate.FreeMarkerTemplateUtils;
import utils.code.generate.entity.DataBaseEntity;
import utils.code.generate.entity.ExtraData;
import utils.code.generate.entity.JavaObj;
import utils.code.generate.gentype.AbsGenType;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by yarus li on 2017/7/30.
 * 数据库对象生成Dao类型
 */
public class DataBaseGen extends AbsGenType<DataBaseEntity> {


    public DataBaseGen(DataBaseEntity entity) {
        super(entity);
    }

    @Override
    public void generate(ExtraData extraData) throws Exception {

        String suffix = extraData.suffix;

        String packageName = extraData.packageName;
        String name = extraData.fileName;

        String path = extraData.filePath+name+suffix;

//        final String templateName = "Model.ftl";
//        File mapperFile = new File(path);

        String templateName = extraData.templateName;
        File mapperFile = new File(path);
        List<ColumnClass> columnClassList = entity.createColumnClass(entity.getEntity());

        JavaObj javaObj = JavaObj.defaultObj();
        javaObj.setColumnClassList(columnClassList);
        javaObj.setPackageName(packageName);
        javaObj.setName(name);

        Map<String,Object> dataMap = new HashMap<>();
        dataMap.put("obj",javaObj);
        dataMap.put("extra",extraData);

        //String listType = (String) extraData.getDataMap("listType");

        //dataMap.put("model_column",columnClassList);
        generateFileByTemplate(templateName,mapperFile,dataMap);

    }


    public void generateFileByTemplate(final String templateName,File file,Map<String,Object> dataMap) throws Exception{
        Template template = FreeMarkerTemplateUtils.getTemplate(templateName);
        FileOutputStream fos = new FileOutputStream(file);
        Writer out = new BufferedWriter(new OutputStreamWriter(fos, "utf-8"),10240);
        template.process(dataMap,out);
    }


    public void generate(List<ExtraData> extraData) throws Exception {

        for(ExtraData e:extraData){
            generate(e);
        }


    }

//    public void generateFileByTemplate(final String templateName,File file,Map<String,Object> dataMap) throws Exception{
//        Template template = FreeMarkerTemplateUtils.getTemplate(templateName);
//        FileOutputStream fos = new FileOutputStream(file);
//        dataMap.put("table_name_small",tableName);
//        dataMap.put("table_name",changeTableName);
//        dataMap.put("author",AUTHOR);
//        dataMap.put("date",CURRENT_DATE);
//        dataMap.put("package_name",packageName);
//        dataMap.put("table_annotation",tableAnnotation);
//        Writer out = new BufferedWriter(new OutputStreamWriter(fos, "utf-8"),10240);
//        template.process(dataMap,out);
//    }


}
