package cn.ksmcbrigade.MLC.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class DownloadUtils {

    public static String getUrlContext(String urlz) {
        String re = "";
        try {
            URL url = new URL(urlz);

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("GET");

            //int responseCode = connection.getResponseCode();

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            StringBuilder response = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            re = response.toString();

            connection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return re;
    }

    public static void downloadFile(String fileUrl, String fileName) throws IOException {
        if(!new File(fileName).exists()){
            System.out.println("Downloading: "+fileUrl);
            try (BufferedInputStream in = new BufferedInputStream(new URL(fileUrl).openStream());
                 FileOutputStream fileOutputStream = new FileOutputStream(fileName)) {
                byte[] dataBuffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                    fileOutputStream.write(dataBuffer, 0, bytesRead);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void downloadWithThreads(String fileUrl, String fileName) {
        ExecutorService executor = Executors.newFixedThreadPool(64);
        for (int i = 0; i < 64; i++) {
            executor.execute(() -> {
                try {
                    downloadFile(fileUrl, fileName);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static boolean download(File file, String url, int thread, boolean exists, boolean NoPrint) throws InterruptedException, IOException {
        if(!file.exists() || exists){
            if(!NoPrint){
                System.out.println("Downloading: "+url);
            }
            long fileSize = getFileSize(url);
            long blockSize = fileSize / thread;

            ExecutorService executor = Executors.newFixedThreadPool(thread);

            for (int i = 0; i < thread; i++) {
                long start = i * blockSize;
                long end = (i == thread - 1) ? fileSize : (i + 1) * blockSize;
                executor.submit(new DownloadTask(url, file, start, end));
            }

            executor.shutdown();
            return executor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        }
        return true;
    }

    public static long getFileSize(String url) throws IOException {
        URLConnection connection = new URL(url).openConnection();
        return connection.getContentLengthLong();
    }

    private static class DownloadTask implements Runnable {
        private String url;
        private File file;
        private long start;
        private long end;

        public DownloadTask(String url, File file, long start, long end) {
            this.url = url;
            this.file = file;
            this.start = start;
            this.end = end;
        }

        @Override
        public void run() {
            try {
                URLConnection connection = new URL(url).openConnection();
                connection.setRequestProperty("Range", "bytes=" + start + "-" + end);
                RandomAccessFile RAF = new RandomAccessFile(file, "rw");
                RAF.seek(start);
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = connection.getInputStream().read(buffer)) != -1) {
                    RAF.write(buffer, 0, bytesRead);
                }
                RAF.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public static class Minecraft {
        public static JsonArray versions = JsonParser.parseString(getUrlContext("http://launchermeta.mojang.com/mc/game/version_manifest_v2.json")).getAsJsonObject().getAsJsonArray("versions");

        public static JsonObject getVersionFromInt(String ver){
            for(JsonElement v:versions.asList()){
               if(v.isJsonObject() && v.getAsJsonObject().get("id").getAsString().equalsIgnoreCase(ver)){
                   return JsonParser.parseString(getUrlContext(v.getAsJsonObject().get("url").getAsString())).getAsJsonObject();
               }
            }
            return null;
        }

        public static void downloadVersionJar(String ver,String name) throws IOException, InterruptedException {
            /*new File(".minecraft").mkdirs();
            new File(".minecraft/versions").mkdirs();*/
            DirUtils.mkdirs(".minecraft/versions/"+name);
            download(new File(".minecraft/versions/"+name+"/"+name+".jar"),getVersionFromInt(ver).getAsJsonObject("downloads").getAsJsonObject("client").get("url").getAsString(),64,false,false);
            Files.writeString(Paths.get(".minecraft/versions/"+name+"/"+name+".json"),getVersionFromInt(ver).toString());
        }

        public static void downloadLibrariesAndNatives(String ver,String name) {
            DirUtils.mkdirs(".minecraft/versions/"+name+"/natives");
            DirUtils.mkdirs(".minecraft/libraries");
            JsonObject version = getVersionFromInt(ver);
            for(JsonElement library:version.getAsJsonArray("libraries")){
                if(library.isJsonObject()){
                    try {
                        if(library.getAsJsonObject().getAsJsonObject("downloads").get("classifiers")!=null){
                            //native
                            String os = "natives-"+(System.getProperty("os.name").toLowerCase().contains("windows")?"windows":(System.getProperty("os.name").toLowerCase().contains("linux")?"linux":"osx"));
                            if(library.getAsJsonObject().getAsJsonObject("downloads").getAsJsonObject("classifiers").get(os)!=null){
                                DirUtils.mkdirs("temp");
                                File temp = new File("temp/temp.jar");
                                download(temp,library.getAsJsonObject().getAsJsonObject("downloads").getAsJsonObject("classifiers").getAsJsonObject(os).get("url").getAsString(),64,true,false);

                                JarFile jarFile = new JarFile(temp);
                                Iterator<JarEntry> it = jarFile.entries().asIterator();
                                while (it.hasNext()){
                                    JarEntry entry = it.next();
                                    if(!entry.isDirectory() && !entry.getName().toLowerCase().contains("meta-inf")){
                                        Files.write(Paths.get(".minecraft/versions/"+name+"/natives/"+entry.getName()),IOUtils.toByteArray(jarFile.getInputStream(entry)));
                                    }
                                }

                                jarFile.close();
                                temp.delete();
                            }
                        }
                        JsonObject info = library.getAsJsonObject().getAsJsonObject("downloads").getAsJsonObject("artifact");
                        if(info==null) continue;
                        String[] path = info.get("path").getAsString().split("/");
                        DirUtils.mkdirs(".minecraft/libraries/"+info.get("path").getAsString().replace(path[path.length-1],""));
                        download(new File(".minecraft/libraries/"+info.get("path").getAsString()),info.get("url").getAsString(),64,false,false);
                    }
                    catch (Exception e){
                        System.out.println("Worry: "+e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        }

        public static void downloadAssets(String ver) throws IOException, InterruptedException {
            DirUtils.mkdirs(".minecraft/assets/indexes");
            DirUtils.mkdirs(".minecraft/assets/objects");
            JsonObject version = JsonParser.parseString(getUrlContext(getVersionFromInt(ver).getAsJsonObject("assetIndex").get("url").getAsString())).getAsJsonObject();
            Files.writeString(Paths.get(".minecraft/assets/indexes/"+getVersionFromInt(ver).getAsJsonObject("assetIndex").get("id").getAsString()+".json"),version.toString());
            for(String key:version.getAsJsonObject("objects").keySet()){
                String hash = version.getAsJsonObject("objects").getAsJsonObject(key).get("hash").getAsString();
                DirUtils.mkdirs(".minecraft/assets/objects/"+hash.substring(0,2));
                downloadFile("https://resources.download.minecraft.net/"+hash.substring(0,2)+"/"+hash,".minecraft/assets/objects/"+hash.substring(0,2)+"/"+hash);
            }
        }

        public static void downloadMinecraftClient(String ver,String name) throws IOException, InterruptedException {
            downloadVersionJar(ver,name);
            downloadLibrariesAndNatives(ver,name);
            downloadAssets(ver);
        }
    }
}
