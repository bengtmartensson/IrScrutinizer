package gnu.io.installer.macosx;

import java.net.*;
import java.io.*;
import java.util.*;

import glguerin.authkit.*;
import gnu.io.installer.*;
import gnu.io.installer.util.*;
/*
 * Greg Guerin AuthKit package was used
 * see http://www.amug.org/~glguerin/sw/#authkit    
 * it uses "Artistic" license
 */
public class MACOSXRXTXInstaller extends gnu.io.installer.RXTXInstaller{
    private static final String JavaExtensions = "Java/Extensions";
    
    public MACOSXRXTXInstaller(){
        createJarFolder();
        createLibFolder();
        installAuthKit();
        addJarResource("RXTXcomm.jar","/gnu/io/installer/resources/macosx/jar/RXTXcomm.dat");
        addLibResource("librxtxSerial.jnilib","/gnu/io/installer/resources/macosx/lib/librxtxSerial.jnilib");
    }
    
    
    public void runPreProcess() {
        
         Authorization auth = new glguerin.authkit.imp.macosx.MacOSXAuthorization();   
         Privilege priv = new Privilege("system.privilege.admin");
         //see kAuthorizationRightExecute in the AuthorizationTags.h from Security.framework
         int count = 0;
         boolean succeed = false;
         do {
             try{
                 auth.authorize( priv, true );
                 succeed = true;
                 break;
             } catch (Throwable t) {
                 System.out.println("Throwable "+t);
             }
             count++;
         } while (count <= 3);
         
         if(succeed){
             String preinstallPath = createTemporaryPreinstallFile();
             if(preinstallPath == null) return;
             String [] progArray = {preinstallPath,System.getProperty("user.name")};
              try {
                Process p = auth.execPrivileged( progArray );                 
                Thread.sleep( 1000L );
             } catch (Throwable t) {
                 System.out.println("Throwable "+t);
                 t.printStackTrace();
             }
         }
         
    }

    public void runPostProcess() {}

    
    protected void createJarFolder(){
        File libFile = new File(InstallUtil.getFolderPath(InstallUtil.kLocalDomain,InstallUtil.kDomainLibraryFolder));
        jarFolder = new File(libFile,JavaExtensions);
        if(!jarFolder.canWrite()){
            libFile = new File(InstallUtil.getFolderPath(InstallUtil.kUserDomain,InstallUtil.kDomainLibraryFolder));
            jarFolder = new File(libFile,JavaExtensions);
            if(!jarFolder.exists()){
                jarFolder.mkdirs();
            }
        }
    }
    
    protected void createLibFolder(){
        File libFile = new File(InstallUtil.getFolderPath(InstallUtil.kLocalDomain,InstallUtil.kDomainLibraryFolder));
        libFolder = new File(libFile,JavaExtensions);
        if(!libFolder.canWrite()){
            libFile = new File(InstallUtil.getFolderPath(InstallUtil.kUserDomain,InstallUtil.kDomainLibraryFolder));
            libFolder = new File(libFile,JavaExtensions);
            if(!libFolder.exists()){
                libFolder.mkdirs();
            }
        }
    }
    
    private String createTemporaryPreinstallFile() {
        try {
            File tempFile = File.createTempFile("preinstall",null);
            tempFile.deleteOnExit();
            InstallUtil.copyResourceToFile("/gnu/io/installer/resources/macosx/preinstall",tempFile);
            String absPath = tempFile.getAbsolutePath(); 
            Process p = Runtime.getRuntime().exec(new String[]{"chmod","a+x",absPath});
            p.waitFor();
            return absPath;
        } catch (Throwable t) {}
        return null;
    }
    
    private void installAuthKit() {
        boolean usingJWS = false;
        final ClassLoader originalClassLoader=Thread.currentThread().getContextClassLoader();
        if(originalClassLoader.toString().toLowerCase().indexOf("jnlp")!=-1) usingJWS=true;
        if(usingJWS) return;
        try {
            File tempFile = new File(jarFolder,"libAuthKit.jnilib");
            tempFile.deleteOnExit();
            InstallUtil.copyResourceToFile("/gnu/io/installer/resources/macosx/lib/libAuthKit.jnilib",tempFile);
        } catch (Throwable t) {
            System.out.println("installAuthKit Throwable "+t);
        }
    }
    
}

