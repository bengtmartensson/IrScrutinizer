package gnu.io.installer;

import java.util.Hashtable;
import java.util.Enumeration;
import java.io.File;

public class InstallInstruction{
Hashtable dirpaths = new Hashtable();
    
    public void addResource(File path,String resource){
        if(path == null || resource == null) return;
        String fr = (String)dirpaths.get(path);
        if(fr != null && fr.equals(resource)) return;
        dirpaths.put(path,resource);
    }
    
    public Enumeration getKeys(){
        if(dirpaths == null) return null;
        return dirpaths.keys();
    }
    
    public String getPath(File f){
        if(f == null || dirpaths == null) return null;
        return (String)dirpaths.get(f);
    }
    
}


