package gnu.io.installer;

import java.io.File;
import java.util.Enumeration;
import java.util.prefs.*;

import gnu.io.installer.util.*;


public abstract class RXTXInstaller {
    
    static Preferences sysPref = Preferences.systemRoot();
    Preferences checkPref = null;
    protected File jarFolder,libFolder;
    InstallInstruction inst = new InstallInstruction();
    
    public abstract void runPreProcess(); 
    public abstract void runPostProcess(); 
    
    
    public void install(){
        initInstallation(RXTXInstaller.class);
        runPreProcess();
		if(inst != null){
    		for(Enumeration e = inst.getKeys();e.hasMoreElements();){
    		    File f = (File)e.nextElement();
    		    InstallUtil.installPart(sysPref,checkPref,inst.getPath(f),f);
    		}
        }
        runPostProcess();
        finishInstallation();
    }
    
    protected void finishInstallation(){
        if(checkPref == null) return;
		try{
		    checkPref.removeNode();
		}catch(Throwable t){}
    }
    
    public InstallInstruction getInstallInstruction(){
        return inst;
    }
    
    public static RXTXInstaller getInstance() {
        String osName = System.getProperty("os.name").toLowerCase();
        if(osName.startsWith("mac")){
            return new gnu.io.installer.macosx.MACOSXRXTXInstaller();
        }
        return null;
    }

    protected void initInstallation(Class rootClass){
        checkPref = null;
        if(rootClass == null) return;
        try{
            Preferences.importPreferences(rootClass.getResourceAsStream("installer.pref"));
            String checkPrefName = "/check";
            checkPref = (sysPref.nodeExists(checkPrefName))?sysPref.node(checkPrefName):null;
        }catch(Throwable t){
            checkPref = null;
            System.out.println("Throwable "+t);
        }
    }

    public void addLibResource(String path,String resource){
        if(libFolder == null || path == null || resource == null) return;
        File libFile = new File(libFolder,path);
        addResource(libFile,resource);
    }
    
    public void addJarResource(String path,String resource){
        if(jarFolder == null || path == null || resource == null) return;
        File jarFile = new File(jarFolder,path);
        addResource(jarFile,resource);
    }

    public void addResource(File path,String resource){
        if(inst == null) inst = new InstallInstruction();
        inst.addResource(path,resource);
    }
    
    public static void main(String args[]) {
        RXTXInstaller installer = RXTXInstaller.getInstance();
        if(installer != null) installer.install();
    }
    
}

