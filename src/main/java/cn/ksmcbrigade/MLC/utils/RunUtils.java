package cn.ksmcbrigade.MLC.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import static cn.ksmcbrigade.MLC.utils.DownloadUtils.download;

public class RunUtils {

    public static String getNowJavaw(){
        return (System.getProperty("java.home")+DirUtils.getFile()+"bin"+DirUtils.getFile()+"javaw").replace("\\","\\\\")+(System.getProperty("os.name").contains("Windows")?".exe":"");
    }

    public static String getClassPaths(JsonObject version,String name){
        ArrayList<String> arrayList = new ArrayList<>();
        for(JsonElement library:version.getAsJsonArray("libraries")){
            if(library.isJsonObject()){
                try {
                    if(library.getAsJsonObject().getAsJsonObject("downloads").get("classifiers")==null){
                        JsonObject info = library.getAsJsonObject().getAsJsonObject("downloads").getAsJsonObject("artifact");
                        if(info==null) continue;
                        String[] path = info.get("path").getAsString().split("/");
                        DirUtils.mkdirs(".minecraft/libraries/"+info.get("path").getAsString().replace(path[path.length-1],""));
                        File file = new File(".minecraft/libraries/"+info.get("path").getAsString());
                        if(!file.exists()){
                            download(file,info.get("url").getAsString(),64,false,false);
                        }
                        arrayList.add(file.getPath());
                    }

                }
                catch (Exception e){
                    System.out.println("Worry: "+e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        arrayList.add(".minecraft/versions/"+name+"/"+name+".jar");
        StringBuilder builder = new StringBuilder();
        arrayList.forEach(a -> {
            if(builder.isEmpty()){
                builder.append(a);
            }
            else{
                builder.append(DirUtils.getPath()).append(a);
            }
        });
        return builder.toString();
    }

    public static String getArgs(@Nullable String xmx,@Nullable String xmn,String name, @Nullable String javaw,@Nullable String userName,String clientName,String clientVersion,@Nullable String uuid,@Nullable String token,@Nullable String type) throws IOException {
        JsonObject json = JsonParser.parseString(Files.readString(Paths.get(".minecraft/versions/"+name+"/"+name+".json"))).getAsJsonObject();
        ArrayList<String> args = new ArrayList<>();
        args.add(javaw==null?getNowJavaw():javaw);
        Minecraft MC = Minecraft.getInstance();
        //>1.12 arguments game jvm
        //<= 1.12 minecraftArguments
        args.add("-XX:+UseG1GC");
        args.add("-XX:-UseAdaptiveSizePolicy");
        args.add("-XX:-OmitStackTraceInFastThrow");
        args.add("-Dfml.ignoreInvalidMinecraftCertificates=True");
        args.add("-Dfml.ignorePatchDiscrepancies=True");
        args.add("-Dlog4j2.formatMsgNoLookups=true");
        boolean l;
        if(json.get("minecraftArguments")!=null){
            l = false;
            args.add("-XX:HeapDumpPath=MojangTricksIntelDriversForPerformance_javaw.exe_minecraft.exe.heapdump");
            args.add("-Xmn"+(xmn==null?"614m":xmn+"m"));
            args.add("-Xmx"+(xmn==null?"2048m":xmn+"m"));
            args.add("-Djava.library.path=\""+".minecraft/versions/"+name+"/natives\"");
            args.add("-cp");
            args.add("\""+getClassPaths(json,name)+"\"");
            args.add(json.get("mainClass").getAsString());
            String game_args = json.get("minecraftArguments").getAsString();
            game_args = game_args.replace("${auth_player_name}",userName==null?MC.getUser().getName():userName);
            game_args = game_args.replace("${auth_uuid}",uuid==null?MC.getUser().getUuid().replace("-",""):uuid);
            game_args = game_args.replace("${auth_access_token}",token==null?MC.getUser().getAccessToken():token);
            game_args = game_args.replace("${user_type}",type==null?MC.getUser().getType().getName():type);
            game_args = game_args.replace("${version_type}",clientName);
            game_args = game_args.replace("${version_name}",name);
            game_args = game_args.replace("${game_directory}","\".minecraft/versions/"+name+"\"");
            game_args = game_args.replace("${assets_root}","\".minecraft/assets\"");
            game_args = game_args.replace("${assets_index_name}",json.getAsJsonObject("assetIndex").get("id").getAsString());
            args.add(game_args);
        }
        else{
            l = true;
            args.add("-Xmx"+(xmn==null?"2048m":xmn+"m"));
            for(JsonElement jvm:json.getAsJsonObject("arguments").getAsJsonArray("jvm")){
                if(jvm.isJsonObject()){
                    if(jvm.getAsJsonObject().get("value").isJsonArray()){
                        for(JsonElement ga:jvm.getAsJsonObject().getAsJsonArray("value")){
                            args.add(ga.getAsString().replace(" ","").replace(",","，"));
                        }
                    }
                    else{
                        args.add(jvm.getAsJsonObject().get("value").getAsString().replace(" ","").replace(",","，"));
                    }
                }
                else{
                    args.add(jvm.getAsString().replace(" ","").replace(",","，").replace("${natives_directory}","\""+".minecraft/versions/"+name+"/natives\"").replace("${launcher_name}",clientName).replace("${launcher_version}",clientVersion).replace("${classpath}","\""+getClassPaths(json,name)+"\""));
                }
            }
            args.add(json.get("mainClass").getAsString());
            for(JsonElement game:json.getAsJsonObject("arguments").getAsJsonArray("game")){
                if(game.isJsonObject()){
                    if(game.getAsJsonObject().get("value").isJsonArray()){
                        for(JsonElement ga:game.getAsJsonObject().getAsJsonArray("value")){
                            args.add(ga.getAsString());
                        }
                    }
                    else{
                        args.add(game.getAsJsonObject().get("value").getAsString());
                    }
                }
                else{
                    args.add(game.getAsString());
                }
            }
        }
        StringBuilder builder = new StringBuilder();
        args.forEach(a -> {
            if(builder.isEmpty()){
                builder.append(a);
            }
            else{
                builder.append(" ").append(a);
            }
        });
        String allArgs = builder.toString();
        allArgs = allArgs.replace("${auth_player_name}",userName==null?MC.getUser().getName():userName);
        allArgs = allArgs.replace("${auth_uuid}",uuid==null?MC.getUser().getUuid().replace("-",""):uuid);
        allArgs = allArgs.replace("${auth_access_token}",token==null?MC.getUser().getAccessToken():token);
        allArgs = allArgs.replace("${user_type}",type==null?MC.getUser().getType().getName():type);
        allArgs = allArgs.replace("${version_type}",clientName);
        allArgs = allArgs.replace("${version_name}",name);
        allArgs = allArgs.replace("${game_directory}","\".minecraft/versions/"+name+"\"");
        allArgs = allArgs.replace("${assets_root}","\".minecraft/assets\"");
        allArgs = allArgs.replace("${assets_index_name}",json.getAsJsonObject("assetIndex").get("id").getAsString());
        allArgs = allArgs.replace("${resolution_height}","508");
        allArgs = allArgs.replace("${resolution_width}","873");
        allArgs = allArgs.replace("${user_properties}","{}");
        allArgs = allArgs.replace(" -XstartOnFirstThread","");
        allArgs = allArgs.replace(" --demo","");
        if(l){
            allArgs+= "--userProperties {}";
        }
        Files.writeString(Paths.get("temp/start.cmd"),allArgs);
        return allArgs;
    }

    public static void runMinecraft(@Nullable String xmx,@Nullable String xmn,String name, @Nullable String javaw,@Nullable String userName,String clientName,String clientVersion,@Nullable String uuid,@Nullable String token,@Nullable String type,boolean wait){
        try {
            Process process = Runtime.getRuntime().exec(getArgs(xmx, xmn, name, javaw, userName, clientName, clientVersion, uuid, token, type));
            if(wait){
                process.waitFor();
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
