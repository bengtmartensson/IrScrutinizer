/*
Copyright (C) 2009-2012 Bengt Martensson.

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

package org.harctoolbox.harchardware.misc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.HashMap;

/**
 * This class is a read interface to /etc/ethers, or any file with that format.
 * If the file is not residing in /etc/ethers, the user has to call the init(String filename) function first.
 *
 * @see <a href="http://linux.die.net/man/5/ethers">man /etc/ethers</a>
 */
public class Ethers {

    public final static String defaultPathname = System.getProperty("os.name").startsWith("Windows")
            ? System.getenv("WINDIR") + File.separator + "ethers"
            : "/etc/ethers";

    private static String pathname = defaultPathname;

    public static void setPathname(String pathname_) {
        pathname = pathname_;
    }

    public static String getEtherAddress(String hostname, String ethersPathname) {
        try {
            return (new Ethers(ethersPathname)).getMac(hostname);
        } catch (FileNotFoundException ex) {
        }
        return null;
    }

    public static String getEtherAddress(String hostname) {
        return getEtherAddress(hostname, defaultPathname);
    }

    /**
     * Command line interface to getMac.
     * @param args hostname to be resolved
     */
    public static void main(String[] args) {


        if (args.length == 1) {
            String result = getEtherAddress(args[0]);
            System.out.println(result != null ? result : "Not found");
        } else
            System.err.println("Usage:\n\tethers host");
    }
    private HashMap<String, String> table = null;
    public Ethers(String filename) throws FileNotFoundException {
        table = new HashMap<>(64);
        BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(filename), Charset.defaultCharset()));

        try {
            for (String line = r.readLine(); line != null; line = r.readLine()) {
                if (!line.startsWith("#")) {
                    String[] str = line.split("[\\s]+");
                    if (str.length == 2) {
                        String mac = str[0];
                        String hostname = str[1];
                        table.put(hostname, mac);
                    }
                }
            }
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
        } finally {
            try {
                r.close();
            } catch (IOException ex) {
                System.err.println(ex.getMessage());
            }
        }
    }
    public Ethers() throws FileNotFoundException {
        this(pathname);
    }
    /**
     * Returns MAC address.
     * @param hostname
     * @return Mac-address belonging to the host in the argument, if found.
     */
    public String getMac(String hostname) {
        return table.get(hostname);
    }
}
