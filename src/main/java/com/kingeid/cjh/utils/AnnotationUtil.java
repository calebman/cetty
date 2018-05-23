package com.kingeid.cjh.utils;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

/**
 * @author calebman
 * @Date 2018-4-28
 * 注解工具类
 */
public class AnnotationUtil {

    /**
     * 包扫描工具
     * @param iPackage 根级包名
     * @param iWhat 处理回调
     */
    public static void scanPackage(String iPackage, IWhat iWhat) {
        String path = iPackage.replace(".", "/");
        URL url = Thread.currentThread().getContextClassLoader().getResource(path);
        try {
            if (url != null && url.toString().startsWith("file")) {
                String filePath = URLDecoder.decode(url.getFile(), "utf-8");
                File dir = new File(filePath);
                List<File> fileList = new ArrayList<File>();
                fetchFileList(dir, fileList);
                for (File f : fileList) {
                    String fileName = f.getAbsolutePath();
                    if (fileName.endsWith(".class")) {
                        String nosuffixFileName = fileName.substring(8 + fileName.lastIndexOf("classes"), fileName.indexOf(".class"));
                        String filePackage = nosuffixFileName.replaceAll("\\\\", ".");
                        Class<?> clazz = Class.forName(filePackage);
                        iWhat.execute(f, clazz);
                    } else {
                        iWhat.execute(f, null);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void fetchFileList(File dir, List<File> fileList) {
        if (dir.isDirectory()) {
            for (File f : dir.listFiles()) {
                fetchFileList(f, fileList);
            }
        } else {
            fileList.add(dir);
        }
    }


    public interface IWhat {
        void execute(File file, Class<?> clazz) throws Exception;
    }
}
