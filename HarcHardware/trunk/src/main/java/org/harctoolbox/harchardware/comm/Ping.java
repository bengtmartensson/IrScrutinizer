/*
Copyright (C) 2014 Bengt Martensson.

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
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * This class pings a host using Java's isReachable.
 */
public class Ping {

    public static final int defaultTimeout = 2000;
    private int timeout;
    InetAddress inetAddress;

    public Ping(String hostname, int timeout) throws UnknownHostException {
        this.timeout = timeout;
        inetAddress = InetAddress.getByName(hostname);
    }

    public Ping(String hostname) throws UnknownHostException {
        this(hostname, defaultTimeout);
    }

    public boolean ping() throws IOException {
        return inetAddress.isReachable(timeout);
    }

    public static boolean ping(String hostname) throws UnknownHostException, IOException {
        return (new Ping(hostname)).ping();
    }

    public static boolean ping(String hostname, int timeout) throws UnknownHostException, IOException {
        return (new Ping(hostname, timeout)).ping();
    }

    public static void main(String[] args) {
        try {
            boolean result = ping(args[0]);
            System.out.println(args[0] + " is " + (result ? "alive" : "not alive"));
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
        }
    }
}
