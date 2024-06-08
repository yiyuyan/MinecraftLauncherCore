package cn.ksmcbrigade.MLC;

import cn.ksmcbrigade.MLC.utils.DownloadUtils;
import cn.ksmcbrigade.MLC.utils.RunUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;

import java.io.IOException;

@Mod(MinecraftLauncherCore.MODID)
public class MinecraftLauncherCore {

    // Define mod id in a common place for everything to reference
    public static final String MODID = "mlc";

    public MinecraftLauncherCore() throws IOException, InterruptedException {
        MinecraftForge.EVENT_BUS.register(this);
        //DownloadUtils.Minecraft.downloadMinecraftClient("1.14.4","1.14.4");
        //System.out.println(RunUtils.getNowJavaw());
        /*DownloadUtils.Minecraft.downloadMinecraftClient("1.19.2","1.19.2");
        System.out.println("startiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiinnnnnnnnnnnnnnnggggggggggggggggg......................");
        RunUtils.runMinecraft("4096",null,"1.19.2",null,null,"MLC","1.0",null,null,null,false);*/
        /*DownloadUtils.Minecraft.downloadMinecraftClient("1.8.9","1.8.9");
        RunUtils.runMinecraft("2048",null,"1.8.9","E:\\java\\bin\\javaw.exe",null,"MLC","1.0",null,null,null,true);
        System.out.println("test done!");*/
    }
}
