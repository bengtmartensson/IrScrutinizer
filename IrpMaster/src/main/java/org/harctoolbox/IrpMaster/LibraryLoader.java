/*
Copyright (C) 2016 Bengt Martensson.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 3 of the License, or (at
your option) any later version.

This program is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License along with
this program. If not, see http://www.gnu.org/licenses/.
*/

package org.harctoolbox.IrpMaster;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 *
 */
public class LibraryLoader {

    private static List<String> loadedLibs = new ArrayList<>();

    // supported values: Linux-{i386,amd64}, Mac OS X-{i386,x86_64}, Windows-{x86,amd64}
    // Mac: it appears that Snow Leopard says "X64_64" while Mountain Lion says "x86_64".
    private static String subFolderName;

    static {
        subFolderName = (System.getProperty("os.name").startsWith("Windows")
                ? "Windows"
                : System.getProperty("os.name"))
                + '-' + System.getProperty("os.arch").toLowerCase(Locale.US);
    }

    public static void loadLibrary(String appHome, String libraryName) throws UnsatisfiedLinkError {
        if (loadedLibs.contains(libraryName))
            return;

        try {
            System.load(fileName(appHome, libraryName).getCanonicalPath());
        } catch (UnsatisfiedLinkError ex) {
            System.loadLibrary(libraryName);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        loadedLibs.add(libraryName);
    }

    public static File fileName(String appHome, String libraryName) {
        String stem = System.getProperty("harctoolbox.jniLibsHome") != null
                ? System.getProperty("harctoolbox.jniLibsHome")
                : appHome;
        String mappedName = System.mapLibraryName(libraryName);
        return new File(new File(stem, subFolderName), mappedName);
    }

    private LibraryLoader() {
    }
}