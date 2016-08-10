/*
Copyright (C) 2015 Bengt Martensson.

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

import java.io.Closeable;
import java.io.IOException;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.harctoolbox.IrpMaster.XmlUtils;
import org.harctoolbox.harchardware.Utils;
import org.harctoolbox.harchardware.comm.UdpSocketChannel;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * This class communicates with an Oppo 93, 9, 103, 105 over IP. Reference:
 * <a href="https://onedrive.live.com/view.aspx?resid=DC5488FD4AFE475C!63423&app=WordPdf">OPPO BDP-9X/10X Blu-ray Disc Player Network Remote Control Protocol
(Version 1.0, September 25, 2012)</a>.
 */
public class OppoIp implements Closeable {

    private final static int broadcastPort = 7624;
    private final static int defaultTimeout = 2000; // FIXME
    private final static String discoverString = "NOTIFY OREMOTE LOGIN";

    private final static String namespace = "http://schemas.xmlsoap.org/soap/oppo/oremote/";
    private final static String myId = "fffbebbfa7060200180373d48892"; // FIXME
    private final static String requesttag = "OSeWFcV78yJe8f9D"; // FIXME
    private final static String password = "oppo80031722";
    private final static String commandName = "command";
    private final static String signinName = "signin";
    private final static String signoutName = "signout";
    private final static String responseAckName = "response_ack";
    private final static String signintype = "1";
    private final static String signouttype = "0";
    private final static String requestName = "request";
    private final static String ackName = "ack";

    private static InetAddress myBroadcastAddress() {
        InetAddress myIp;
        try {
            myIp = InetAddress.getByName(Utils.getHostname());
            byte[] ipNumeric = myIp.getAddress();
            ipNumeric[3] = 0;
            return InetAddress.getByAddress(ipNumeric);
        } catch (UnknownHostException ex) {
            return null;
        }
    }

    private static String dom2String(Document doc) throws TransformerException {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        String output = writer.getBuffer().toString().replaceAll("\n|\r", "");
        return output;
    }

    /**
     * Just for testing.
     * @param args
     */
    public static void main(String[] args) {
        try (OppoIp oppoIp = new OppoIp(true)) {
            oppoIp.sendCommand(args[0]);
        } catch (IOException | TransformerException ex) {
            Logger.getLogger(OppoIp.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private boolean verbose;

    private InetAddress ip;
    private int port;

    private UdpSocketChannel udpSocketChannel;
    private final int timeout;

    public OppoIp(String broadcast, int timeout, boolean verbose) throws IOException, TransformerException {
        this(InetAddress.getByName(broadcast), timeout, verbose);
    }

    /**
     * This version of the constructor searches on the local LAN by using the
     * IP address of the host with last byte replaced by a 0 as broadcast address.
     * @throws IOException
     * @throws TransformerException
     */
    public OppoIp() throws IOException, TransformerException {
        this(myBroadcastAddress(), defaultTimeout, false);
    }

    public OppoIp(boolean verbose) throws IOException, TransformerException {
        this(myBroadcastAddress(), defaultTimeout, verbose);
    }

    /**
     * General constructor.
     * @param broadcast Broadcast address for searching for the Oppo.
     * @param timeout
     * @param verbose
     * @throws IOException
     * @throws TransformerException
     */
    public OppoIp(InetAddress broadcast, int timeout, boolean verbose) throws IOException, TransformerException {
        this.timeout = timeout;
        this.verbose = verbose;
        discover(broadcast);
        udpSocketChannel = new UdpSocketChannel(ip, port, timeout, verbose);
        signIn();
    }

    private void discover(InetAddress broadcast) throws IOException {
        UdpSocketChannel broadcaster = new UdpSocketChannel(broadcast, broadcastPort, timeout, verbose);
        broadcaster.sendString(discoverString);
        String answer = broadcaster.readString();
        String[] arr = answer.split(":");
        ip = InetAddress.getByName(arr[1].trim());
        port = Integer.parseInt(arr[2].trim());
    }

    private void signIn() throws TransformerException, IOException {
        udpSocketChannel.sendString(dom2String(mkDom(requestName, signinName, signintype, password, null)));
        String ans = udpSocketChannel.readString();
        System.out.println(ans);
        checkResponse(ans);
        udpSocketChannel.sendString(dom2String(mkDom(ackName, responseAckName, null, null, null)));
    }

    private void signOut() throws IOException, TransformerException {
        udpSocketChannel.sendString(dom2String(mkDom(requestName, signoutName, signouttype, null, null)));
        String ans = udpSocketChannel.readString();
        System.out.println(ans);
    }

    @Override
    public void close() {
        try {
            signOut();
            udpSocketChannel.close();
        } catch (IOException | TransformerException ex) {
            Logger.getLogger(OppoIp.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    //TODO
    private boolean checkResponse(String ans) {
        return true;
    }

    /**
     * Sends a command to the connected Oppo.
     * @param cmd String like "EJT" (for eject).
     * @throws IOException
     * @throws TransformerException
     */
    public void sendCommand(String cmd) throws IOException, TransformerException {
        udpSocketChannel.sendString(dom2String(mkDom(requestName, commandName, null, null, cmd)));
    }

    private Document mkDom(String typeName, String nameContent, String signinTypeContent,
            String passwordContent, String contentContent) {
        Document doc = XmlUtils.newDocument(true);
        Element root = doc.createElementNS(namespace, "s:Envelope");
        doc.appendChild(root);
        Element body = doc.createElement("body");
        root.appendChild(body);
        Element request = doc.createElement(typeName);
        body.appendChild(request);
        Element name = doc.createElement("name");
        request.appendChild(name);
        name.setTextContent(nameContent);
        Element uid = doc.createElement("uid");
        uid.setTextContent(myId);
        request.appendChild(uid);
        Element requestTag = doc.createElement("requesttag");
        requestTag.setTextContent(requesttag);
        request.appendChild(requestTag);
        if (passwordContent != null) {
            Element passWord = doc.createElement("password");
            passWord.setTextContent(passwordContent);
            request.appendChild(passWord);
        }
        if (contentContent != null) {
            Element content = doc.createElement("content");
            content.setTextContent(contentContent);
            request.appendChild(content);
        }
        if (signinTypeContent != null) {
            Element signinType = doc.createElement("signintype");
            signinType.setTextContent(signinTypeContent);
            request.appendChild(signinType);
        }
        return doc;
    }
}
