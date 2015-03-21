/*
Copyright (C) 2013 Bengt Martensson.

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

package org.harctoolbox.harchardware.beacon;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import org.harctoolbox.IrpMaster.IrpUtils;

// Ref: http://www.openremote.org/display/docs/Controller+2.0+HTTP-REST-XML

public class OpenremoteClient {
    public static final String multicastIp = "224.0.1.100";
    public static final int multicastPort = 3333;
    public static final int listenPort = 2346;
    public static final String token = "openremote";

    private final String baseUrl;

    public OpenremoteClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl : (baseUrl + "/");
    }

    public OpenremoteClient() {
        this(discover());
    }

    public OpenremoteClient(String host, int portnumber) {
        this("http://" + host + ":" + portnumber + "/controller");
    }

    public String getBaseurl() {
        return baseUrl;
    }

    /** Returns the base url
     *
     * @return url base as String.
     */
    public static String discover() {
        DatagramSocket sock = null;
        try {
            sock = new DatagramSocket();
            InetAddress addr = InetAddress.getByName(multicastIp);
            byte[] buf = token.getBytes("US-ASCII");
            DatagramPacket dp = new DatagramPacket(buf, buf.length, addr, multicastPort);
            sock.send(dp);
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
        } finally {
            if (sock != null)
                sock.close();
        }

        String result = null;
        ServerSocket srvSock = null;
        Socket s = null;
        try {
            srvSock = new java.net.ServerSocket(listenPort);
            //s = new Socket();
            s = srvSock.accept();
            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), IrpUtils.dumbCharset));
            result = in.readLine();
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
        } finally {
            try {
                if (s != null)
                    s.close();
                if (srvSock != null)
                    srvSock.close();
            } catch (IOException ex) {
                System.err.println(ex.getMessage());
            }
        }
        return result;
    }

    private int httpRequest(String shortUrl, String method) {
        int result = 0;
        try {
            String urlStr = baseUrl + shortUrl;
            System.err.println(urlStr);
            URL url = new URL(urlStr);

            HttpURLConnection hu = (HttpURLConnection) url.openConnection();
            //hu.connect();
            hu.setRequestMethod(method);
            //String ct = hu.getContentType();
            //System.out.println(ct);
            hu.getContent();
        } catch (java.net.UnknownServiceException ex) {
            System.err.println(ex.getMessage());
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
        }

        return result;
    }

    public boolean controlCommand(int controlNo, String param) {
        return httpRequest("/rest/control/" + Integer.toString(controlNo) + "/" + param, "POST") == HttpURLConnection.HTTP_OK;
    }

    public boolean getPanels() {
        return httpRequest("rest/panels", "GET") == HttpURLConnection.HTTP_OK;
    }

    public static void main(String[] args) {
        OpenremoteClient client = new OpenremoteClient();
        System.out.println("Discovered " + client.getBaseurl());
        /*if (args.length == 0)
            System.exit(harcutils.exit_success);
        int but = Integer.parseInt(args[0]);
        String cmd = args.length >= 2 ? args[1] : "click";
        client.control_command(but, cmd);*/
        //client.get_panels();
    }
}
