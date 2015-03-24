package gnu.io.installer.util;

import java.io.File;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.util.prefs.*;

public class InstallUtil{
    
    public static String getSharedFolderPath(){
        return getFolderPath(kSharedUserDataFolder);
    }
    public static String getApplicationFolderPath(){
        return getFolderPath(kApplicationFolder);
    }
    public static String getGlobalApplicationFolderPath(){
        return getFolderPath(kSystemDomain,kApplicationFolder);
    }
    public static String  getFolderPath(int folderKind){
        try{//MAC OS X 1.4.1
            Class clazz = Class.forName("com.apple.eio.FileManager");
            java.lang.reflect.Method m = clazz.getMethod("findFolder",new Class[]{int.class});
            return (String)m.invoke(null,new Object []{new Integer(folderKind)});
        }catch(Throwable t){}
        return null;
    }
    public static String  getFolderPath(short domain,int folderKind){
        try{//MAC OS X 1.4.1
            Class clazz = Class.forName("com.apple.eio.FileManager");
            java.lang.reflect.Method m = clazz.getMethod("findFolder",new Class[]{short.class,int.class});
            return (String)m.invoke(null,new Object []{new Short(domain),new Integer(folderKind)});
        }catch(Throwable t){
            System.out.println("getFolderPath Throwable "+t);
        }
        return null;
    }
    public static void copyResourceToFile(String absResPath,java.io.File file){
        try{
            InputStream is = InstallUtil.class.getResourceAsStream(absResPath);
            byte []buffer = new byte[4096];
            int rb = 0;
            if(is != null){
                FileOutputStream fos = new FileOutputStream(file);
                while((rb = is.read(buffer,0,buffer.length)) > 0){
                    fos.write(buffer,0,rb);
                }
                fos.close();
            }
        }catch(Throwable t){
            t.printStackTrace();
        }
    }
    public static void installPart(Preferences sysPref,Preferences checkPref,String resPathName,File fileToInstall){
        if(fileToInstall == null || resPathName == null) return;
        boolean needInstall = (checkPref == null);
        char separator = File.separatorChar;
        long prevLength = 0;
        File  fooFile                   = new File(resPathName);
        String resName                  = fooFile.getName();
        String resParentName            = fooFile.getParent();
        if(separator != '/')            resParentName = resParentName.replace(separator,'/');
        Preferences installPref         = sysPref.node(resParentName);
        if(!needInstall){
            try{    
                prevLength = fileToInstall.length();
            }catch(Throwable t){}
        }     
        needInstall = (prevLength == 0);
        if(!needInstall){
            String checkResParentName       = resParentName.substring(1);// first slash removed
            Preferences checkInstallPref    = null;
            try{
                checkInstallPref = (checkPref.nodeExists(checkResParentName))?checkPref.node(checkResParentName):null;
            }catch(Throwable t){
                checkInstallPref = null;
            }
            long installValueLength         = installPref.getLong(resName,0);
            long checkValueLength           = (checkInstallPref == null)?0:checkInstallPref.getLong(resName,0);
            needInstall = (checkValueLength != 0) && 
                          ((installValueLength != prevLength) || (checkValueLength != prevLength) || (checkValueLength != installValueLength));
        }
        
        
        if(!needInstall){
		    System.out.println("File "+fileToInstall+" is uptodate");
        }else{
		    System.out.println("copy file "+fileToInstall);
            InstallUtil.copyResourceToFile(resPathName,fileToInstall);
            try{    
                long newLength = fileToInstall.length();
                installPref.putLong(resName,newLength);
                installPref.flush();
            }catch(Throwable t){}
        }
    }
     public static final short kSystemDomain                 = -32766; /* Read-only system hierarchy.*/
    public static final short kLocalDomain                  = -32765; /* All users of a single machine have access to these resources.*/
    public static final short kNetworkDomain                = -32764; /* All users configured to use a common network server has access to these resources.*/
    public static final short kUserDomain                   = -32763; /* Read/write. Resources that are private to the user.*/
    public static final short kClassicDomain                = -32762; /* Domain referring to the currently configured Classic System Folder*/

    public static final int   kPreferenceFolder			  = 0x70726566;//pref
    public static final int   kApplicationSupportFolder	  = 0x61737570;//asup
    public static final int   kSharedUserDataFolder		  = 0x73646174;//sdat
    public static final int   kApplicationFolder		  = 0x61707073;//apps 
    public static final int   kExtensionFolder            = 0x6578746E;//extn /* System extensions go here */
    public static final int   kDomainLibraryFolder        = 0x646C6962;//dlib /* the Library subfolder of a particular domain*/

}


