package cn.ksmcbrigade.MLC.utils;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.Objects;

public class DirUtils {

    public static String getFile(){
        return System.getProperty("file.separator");
    }

    public static String getPath(){
        return System.getProperty("path.separator");
    }

    public static String getLine(){
        return System.getProperty("line.separator");
    }

    public static void mkdirs(String dir){
        String[] dirs = dir.replace("\"","").split("/");
        String last = "";
        for(String s:dirs){
            if(last.equals("")){
                last = s;
                new File(last).mkdirs();
            }else{
                last = last + "/" + s;
                new File(last).mkdirs();
            }
        }
    }

    public static void clean(String dir, @Nullable FileFilter fileFilter){
        File file = new File(dir);
        if(!file.isDirectory()) return;
        if(file.listFiles(fileFilter)==null) return;
        Arrays.stream(Objects.requireNonNull(file.listFiles(fileFilter))).forEach(File::delete);
        if(fileFilter==null){
            file.delete();
        }
    }
}
