/*
Copyright (C) 2009-2013 Bengt Martensson.

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

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;
import org.harctoolbox.IrpMaster.IrpUtils;

/**
 * This class listens for an AMX beacon and reports its findings.
 */
public class AmxBeaconListener /*implements Serializable*/ {
    private final static int reapInterval = 60*1000; // 60 seconds
    private final static int reapAge = 2*60*1000; // 2 minutes

    private ListenThread listenThread;
    private GrimReaperThread grimReaperThread;
    private Date startTime = null;
    private Callback callback = null;
    private String key = null;
    private String value = null;
    private boolean debug = false;
    private HashMap<InetAddress, Node>nodes = new HashMap<InetAddress, Node>();

    /**
     * This is called when a new node appears, or a node is removed.
     */
    public interface Callback {
        public void func(HashMap<InetAddress, Node> nodes);
    }

    /**
     * @param dbg the debug to set
     */
    public void setDebug(boolean dbg) {
        this.debug = dbg;
    }

    public AmxBeaconListener(Callback callback, String key, String value, boolean debug) {
        this.callback = callback;
        this.key = key;
        this.value = value;
        this.callback = callback;
        this.debug = debug;
        nodes = new HashMap<InetAddress, Node>();
        listenThread = new ListenThread(this);
        grimReaperThread = new GrimReaperThread(this);
    }

    public AmxBeaconListener(Callback callback, String key, String value) {
        this(callback, key, value, false);
    }

    public AmxBeaconListener() {
        this(null, null, null, false);
    }

    public AmxBeaconListener(Callback callback) {
        this(callback, null, null, false);
    }

    public void start() {
        if (!listenThread.isAlive())
            listenThread.start();

        if (!grimReaperThread.isAlive())
            grimReaperThread.start();

        startTime = new Date();
    }

    public void stop() {
        if (debug)
            System.err.println("Stopping...");
        grimReaperThread.interrupt();
        listenThread.interrupt();
    }

    private synchronized void printNodes() {
        System.out.println(nodes.size());
        for (InetAddress addr : nodes.keySet())
                System.out.println(debug ? nodes.get(addr) : (addr.getHostName() + " " + nodes.get(addr).lastAliveDate));
    }

    /**
     * Returns the found nodes.
     * @return Collection of nodes.
     */
    public synchronized Collection<Node> getNodes() {
        return nodes.values();
    }

    /**
     * Discard present nodes.
     */
    public synchronized void reset() {
        nodes.clear();
    }

    /**
     * Returns the number of microseconds elapsed since the object was created.
     * @return microseconds
     */
    public long getAge() {
        return (new Date()).getTime() - startTime.getTime();
    }

    /**
     * This class contains one node discovered by its AMX beacon.
     */
    public static class Node {
        private final InetAddress addr;
        private final int port;
        private final HashMap<String,String> table;
        private Date lastAliveDate;
        private Node(InetAddress addr, int port, HashMap<String, String>table) {
            this.addr = addr;
            this.port = port;
            this.table = table;
            lastAliveDate = new Date();
        }

        @Override
        public String toString() {
            StringBuilder res = new StringBuilder();
            for (String s : table.keySet()) {
                res.append("\n").append("table[").append(s).append("] = ").append(table.get(s));
            }
            return "IP = " + addr.getHostName() + "\n"
                    + "port = " + port
                    + res.toString()
                    + "\nLast alive: " + lastAliveDate;
        }

        public void refresh() {
            lastAliveDate = new Date();
        }

        public String get(String key) {
            return table.get(key);
        }

        public InetAddress getInetAddress() {
            return addr;
        }

        public HashMap<String,String> getTable() {
            return table;
        }
    }

    private static class ListenThread extends Thread {
        final AmxBeaconListener beaconListener;

        ListenThread(AmxBeaconListener beaconListener) {
            this.beaconListener = beaconListener;
        }

        @Override
        @SuppressWarnings("empty-statement")
        public void run() {
            while (beaconListener.listenWait(0))
                ;
            //synchronized (this) {
            //    notifyAll();
            //}
            if (beaconListener.debug)
                System.err.println("ListenThread exited.");
        }
    }

    private static class GrimReaperThread extends Thread {
        AmxBeaconListener beacon;

        GrimReaperThread(AmxBeaconListener beacon) {
            this.beacon = beacon;
        }

        @Override
        public void run() {
            boolean status = true;
            while (status) {
                beacon.reap();
                try {
                    Thread.sleep(reapInterval);
                } catch (InterruptedException ex) {
                    status = false;
                }
            }
            if (beacon.debug)
                System.err.println("GrimReaperThread reaped.");
        }
    }

    @SuppressWarnings("unchecked")
    private synchronized void reap() {
        boolean reaped = false;
        for (Entry<InetAddress, Node> kvp : ((HashMap<InetAddress, Node>)nodes.clone()).entrySet()) {
            //InetAddress addr = it.next();
            Node node = kvp.getValue();
            if (node.lastAliveDate.getTime() + reapAge < (new Date()).getTime()) {
                if (debug)
                    System.err.println("Reaped " + node.addr.getHostName());
                nodes.remove(kvp.getKey());
                reaped = true;
            } else {
                if (debug)
                    System.err.println("Not reaped " + node.addr.getHostName());
            }
        }
        if (reaped && callback != null)
            callback.func(nodes);
    }

    private boolean listenWait(int timeout) {
        if (timeout < 0)
            return false;
        if (debug)
            System.err.println("listening to beacon...");

        byte buf[] = new byte[1000];
        MulticastSocket sock = null;
        try {
            InetAddress group = java.net.InetAddress.getByName(AmxBeacon.broadcastIp);
            sock = new MulticastSocket(AmxBeacon.broadcastPort);
            sock.joinGroup(group);
            sock.setSoTimeout(timeout);
            DatagramPacket pack = new DatagramPacket(buf, buf.length);
            if (debug)
                System.err.println("listening...");
            sock.receive(pack);
            //System.err.println("got something...");
            String payload = (new String(pack.getData(), 0, pack.getLength(), IrpUtils.dumbCharset)).trim();
            InetAddress a = pack.getAddress();
            int port = pack.getPort();
            if (debug)
                System.err.print("Got |" + payload + "| from " + a.getHostName() + ":" + port + "...");
            //HashMap<String, String> t = new HashMap<String, String>();
            if (payload.startsWith(AmxBeacon.beaconPreamble))
                payload = payload.substring(5, payload.length() - 1);
            if (debug)
                System.err.println(payload);
            String[] pairs = payload.split("><");
            HashMap<String, String> table = new HashMap<String, String>(pairs.length);
            for (String pair : pairs) {
                String[] x = pair.split("=");
                if (x.length >= 2)
                    table.put(x[0], x[1]);
            }

            Node r = new Node(a, port, table);
            if (key != null && ! r.get(key).equals(value)) {
                    if (debug)
                        System.err.println("Wrong value of `" + key + "', discarded.");
            } else if (nodes.containsKey(a))  {
                if (debug)
                    System.err.println("already in table, just refreshing.");
                nodes.get(a).refresh();
            } else {
                if (debug)
                    System.err.println("not in table, entered, invoking callback.");
                synchronized (this) {
                    nodes.put(a, r);
                }
                if (callback != null)
                    callback.func(nodes);
            }
        } catch (IOException ex) {
            if (!SocketTimeoutException.class.isInstance(ex))
                System.err.println(ex.getMessage());

            return false;
        } finally {
            if (sock != null)
                sock.close();
        }
        return !Thread.currentThread().isInterrupted();
    }

    private synchronized Node getFirstKey(String key, String value) {
        for (Node r : nodes.values())
            if (r.table.containsKey(key) && r.table.get(key).equals(value))
                return r;

        return null;
    }

    private Node listenForKey(int timeout) {
        Date startTime = new Date();
        Node r = null;
        while (r == null) {
            r = getFirstKey(key, value);
            int millisecondsLeft = (int)(startTime.getTime() + timeout - (new Date()).getTime());
            if (r == null) {
                //System.err.println(new Date());
                boolean gotResponse = listenWait(millisecondsLeft);
                if (!gotResponse)
                    return null;
            }
        }
        return r;
    }

    /**
     * Searches for a node with the prescribed value of the key.
     *
     * @param key Key to search for, e.g. "-Make".
     * @param value Value of the key to search for.
     * @param timeout Timeout in microseconds.
     * @return
     */
    public static Node listenFor(String key, String value, int timeout) {
        AmxBeaconListener ab = new AmxBeaconListener(null, key, value, false);
        ab.listenForKey(timeout);
        return ab.getFirstKey(key, value);
    }

    /**
     * Listen for the amount of time given in the argument, then return then nodes found.
     *
     * @param time Time to listen, in microseconds.
     * @param debug
     * @return
     */
    public static Collection<Node> listen(int time, boolean debug) {
        AmxBeaconListener abl = new AmxBeaconListener(null, null, null, debug);
        long endTime = (new Date()).getTime() + (long) time;
        while ((new Date()).getTime() < endTime)
            abl.listenWait((int)(endTime - (new Date()).getTime()));

        return abl.getNodes();
    }

    // Just for testing
    private static class PrintCallback implements Callback {

        @Override
        public void func(HashMap<InetAddress, Node> nodes) {
            for (Node node : nodes.values())
                System.out.println(node);
        }

    }

    /**
     * mainly for testing.
     * @param args
     */
    public static void main(String args[]) {
        if (args.length == 0) {
            AmxBeaconListener listener = new AmxBeaconListener(new PrintCallback());
            listener.setDebug(true);
            listener.start();
        } else if (args.length == 3) {
            AmxBeaconListener ab = new AmxBeaconListener();
            ab.setDebug(true);
            ab.start();
            int n = 0;
            while (true) {
                System.out.println(new Date());
                ab.printNodes();
                try {
                    Thread.sleep(Integer.parseInt(args[0]));
                } catch (InterruptedException ex) {
                    System.err.println(ex.getMessage());
                }
            }
        } else if (args.length == 1) {
            Collection<Node> result = listen(Integer.parseInt(args[0]), true);
            for (Node r : result) {
                System.out.println(r);
            }
        } else if (args.length == 2) {
            Node r = listenFor("-Make", args[0], Integer.parseInt(args[1]));
            System.err.println(r);
        }
    }
}
