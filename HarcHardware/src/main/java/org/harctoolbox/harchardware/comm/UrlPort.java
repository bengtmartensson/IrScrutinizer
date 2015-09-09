/*
Copyright (C) 2011, 2013 Bengt Martensson.

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

package org.harctoolbox.harchardware.comm;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import org.harctoolbox.harchardware.HarcHardwareException;
import org.harctoolbox.harchardware.ICommandLineDevice;

public class UrlPort implements ICommandLineDevice {

    String answer = null;
    private int timeout;
    private boolean verbose;
    private String hostIp;
    private int portNumber;
    private String prefix;
    private String suffix;
    private String protocol;

    public UrlPort(String protocol, String hostIp, int portNumber, String prefix, String suffix,
            int timeout, boolean verbose) {
        this.protocol = protocol;
        this.hostIp = hostIp;
        this.portNumber = portNumber;
        this.prefix = prefix;
        this.suffix = suffix;
        this.verbose = verbose;
        this.timeout = timeout;
    }

    public void connect() throws IOException {
    }

    @Override
    public void close() {
    }

    @Override
    public void sendString(String payload) throws IOException {
        //try {
            URL url = new URL(protocol, hostIp, portNumber, prefix + payload + suffix);
            if (verbose)
                System.err.println("Getting " + url);
            URLConnection connection = url.openConnection();
            connection.setConnectTimeout(timeout);
            answer = (String) connection.getContent();
        //} catch (IOException ex) {
        //    throw new HarcHardwareException(ex.getMessage());
        //}
    }

    @Override
    public String readString() {
        String s = answer;
        answer = null;
        return s;
    }

    @Override
    public String readString(boolean wait) {
        return readString();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        UrlPort port = new UrlPort("http", "t10", -1, "/preset?switch=", "&value=ON", 2000, true);
        try {
             port.sendString("4");
            String str = port.readString();
            System.out.println("-->" + str + "<--");
            str = port.readString();
            System.out.println("-->" + str + "<--");
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
            //ex.printStackTrace();
        }
    }

    @Override
    public String getVersion() throws IOException {
        return null;
    }

    @Override
    public void setVerbosity(boolean verbosity) {
        this.verbose = verbosity;
    }

    @Override
    public void setDebug(int debug) {
    }

    @Override
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void open() throws HarcHardwareException, IOException {
    }

    @Override
    public boolean ready() throws IOException {
        return answer != null;
    }
}
