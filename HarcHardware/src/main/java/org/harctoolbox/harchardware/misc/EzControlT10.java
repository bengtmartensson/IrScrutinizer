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

import java.io.*;
import java.net.*;
import java.util.Locale;
import org.harctoolbox.IrpMaster.IrpUtils;
import org.harctoolbox.IrpMaster.XmlUtils;
import org.harctoolbox.harchardware.HarcHardwareException;
import org.harctoolbox.harchardware.IHarcHardware;
import org.harctoolbox.harchardware.comm.IWeb;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class EzControlT10 implements IHarcHardware, IWeb {

    // UDP control presently not implemented for manual selection.

    private String ezcontrolIP;
    public final static String defaultEzcontrolIP = "192.168.1.42";
    private int soTimeout = 1000;
    private final static int ezcontrolPortno = 7042;
    private final static int ezcontrolQueryPortno = 7044;
    private boolean verbose = true;
    private int debug = 0;

    private final static int bufferSize = 352;

    @Override
    public void close() {
    }

    @Override
    public URI getUri(String user, String password) {
        try {
            return new URI("http", ezcontrolIP, null, null);
        } catch (URISyntaxException ex) {
            return null;
        }
    }

    @Override
    public void open() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setDebug(int debug) {
        this.debug = debug;
    }

    /** Interfaces that can be used to command an T10 */
    public enum Interface {
        udp,
        http
    }

    private Interface interfaze = Interface.http;

    private static class Status {

        public final static int on = 1;
        public final static int off = 0;
        public final static int unknown = -1;
        public String name;
        public int state;

        Status(String n, int s) {
            name = n;
            state = s;
        }

        public String stateStr() {
            return state == on ? "on" : state == off ? "off" : "?";
        }

        @Override
        public String toString() {
            return name + ": " + stateStr();
        }
    }
    public static final int t10NumberPresets = 32;
    private Status[] state = null; // Note: element 0 unused,
    //starts with 0, T10 starts with 1

    public enum EZSystem {
        /** The FS-10 system, now obsolete. */
        FS10, // 1

        /** The FS-20 system from ELV or Conrad. */
        FS20, // 2

        /** The RS200 system, once sold by Conrad. */
        RS200,

        /** ELRO AB400 */
        AB400,

        /** ELRO AB601 */
        AB601,

        /** Intertechno, and similar (Düwi, One 4 all, etc) "with codewheel" */
        IT, // Intertechno, 6

        /** REV Ritter */
        REV,

        /** Brennenstuhl und Quigg */
        BS_QU,

        /** The X10 system, for example by Marmitek. */
        MARMI, // X10, 9

        /** InScenio OASE FM-Master */
        OA_FM,

        /** Kopp first control, only first generation */
        KO_FC,

        /** Europe Supplies Ltd. */
        RS862;

        public static EZSystem parse(String systemName) {
            return systemName.toLowerCase(IrpUtils.dumbLocale).equals("intertechno") ? IT
                    : systemName.toLowerCase(IrpUtils.dumbLocale).equals("conrad") ? RS200
                    : systemName.toLowerCase(IrpUtils.dumbLocale).equals("x10") ? MARMI
                    : valueOf(systemName.replace('-', '_').toUpperCase(IrpUtils.dumbLocale));
        }

        /**
         * Returns the system number as used in the official documentation.
         * @return
         */
        public int systemNumber() {
            return ordinal() + 1;
        }

        public boolean hasHouseLetter() {
            //int sysno = systemNumber(system);
            return this == AB601 || this == IT || this == MARMI;
        }

        public boolean hasDeviceLetter() {
            //int sysno = systemNumber(system);
            return this == AB400;
        }
    }

    /**
     * An enum consisting of the commands this class understands.
     */
    public enum Command {
        power_on,
        power_off,
        power_toggle,
        dim_up,
        dim_down,
        dim_max_time,
        dim_off_time,
        set_time,

        /** Takes two arguments, the goal power, and the time to be taken. */
        dim_value_time,

        /** Takes an argument, power in percents. */
        set_power,
        get_status,

        /** Marker for invalid command. */
        invalid;

        /**
         * Returns true iff the command can be used as a present command.
         * @return
         */
        public boolean isPresetCommand() {
            return this == get_status || this == set_power // Requires FW 2.26
                || this == power_toggle || this == power_on
                || this == power_off;
        }

        /**
         * Returns true iff the command uses a time argument.
         * @return
         */
        public boolean hasTimeArgument() {
            return this == dim_max_time || this == dim_off_time || this == set_time;
        }
    }

    @Override
    public void setTimeout(int timeout) {
        this.soTimeout = timeout;
    }

    /**
     * Dummy implementation.
     * @return The String "unknown".
     */
    @Override
    public String getVersion() {
        return "unknown";
    }

    /**
     * Dummy implementation, always returns true
     * @return true
     */
    @Override
    public boolean isValid() {
        return true;
    }

    private final static String[] daynames = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};

    private class Timer {

        boolean[] presets;
        boolean[] days;
        boolean enabled;

        private class Clock {

            int hour;
            int minute;

            Clock(int h, int m) {
                hour = h;
                minute = m;
            }

            @Override
            public String toString() {
                return "" + (hour >= 0
                        ? (hour < 10 ? "0" : "") + (hour + ":" + (minute < 10 ? "0" : "") + minute)
                        : "     ");
            }
        }
        Clock onTime;
        Clock offTime;

        Timer(boolean[] presets, boolean[] days, boolean enabled, int onH, int onM, int offH, int offM) {
            this.presets = presets;
            this.days = days;
            this.enabled = enabled;
            onTime = new Clock(onH, onM);
            offTime = new Clock(offH, offM);
        }

        @Override
        public String toString() {
            StringBuilder result = new StringBuilder(onTime.toString() + "-" + offTime.toString() + " ");

            boolean virgin = true;
            for (int i = 0; i < 7; i++) {
                if (days[i]) {
                    if (((i == 0) || !days[i - 1]) || ((i == 6) || !days[i + 1])) {
                        result.append(virgin ? "" : (days[i - 1] ? "-" : ",")).append(daynames[i]);
                    }
                    virgin = false;
                }
            }

            for (int i = result.length(); i < 30; i++) {
                result.append(" ");
            }

            result.append(" ");
            virgin = true;
            for (int i = 1; i < t10NumberPresets; i++) {
                if (presets[i]) {
                    result.append(virgin ? "" : ", ").append(state[i].name);
                    virgin = false;
                }
            }
            for (int i = result.length(); i < 65; i++) {
                result.append(" ");
            }
            return result.append(" ").append(enabled ? "(enabled)" : "(disabled)").toString();
        }
    }
    public static final int t10NumberTimers = 26;
    private Timer[] timers = null;

    public EzControlT10(String hostname, boolean verbose, Interface interfaze) {
        ezcontrolIP = (hostname != null && ! hostname.isEmpty()) ? hostname : defaultEzcontrolIP;
        this.verbose = verbose;
        this.interfaze = interfaze;
    }

    public EzControlT10(String hostname) {
        this(hostname, false, Interface.http);
    }

    @Override
    public void setVerbosity(boolean verbosity) {
        this.verbose = verbosity;
    }

    public void setInterface(Interface interfaze) {
        this.interfaze = interfaze;
    }

    public boolean sendManual(EZSystem system, String house, int device,
            int value, int arg, int n) throws HarcHardwareException {
        try {
            getUrl(urlManual(system, house, device, value, arg, n));
            return true;
        } catch (MalformedURLException ex) {
            throw new HarcHardwareException(ex);
        } catch (IOException ex) {
            throw new HarcHardwareException(ex);
        }
    }

    private static int fs20Time(double secs) {
        int quartersecs = (int) (4*secs);
        int qMin = (int)(java.lang.Math.log(quartersecs/15)/java.lang.Math.log(2))+1;
        if (qMin < 0)
            qMin = 0;

        int mult = (((qMin & 1) == 1) ? 2 : 1)
                * (((qMin & 2) == 2) ? 4 : 1)
                * (((qMin & 4) == 4) ? 16 : 1)
                * (((qMin & 8) == 8) ? 256 : 1);

        int m = (int)(quartersecs/mult);
        int res = (qMin << 4) + m;
        return res;
    }

    public boolean sendManual(EZSystem system, String house, int device,
            Command cmd, int power, int arg, int n) throws HarcHardwareException {
        return cmd == Command.set_power ? sendManual(system, house, device, power*16/100, -1, n)
                : cmd == Command.dim_max_time ? sendManual(system, house, device, 48, fs20Time(arg), n)
                : cmd == Command.dim_off_time ? sendManual(system, house, device, 32, fs20Time(arg), n)
                : cmd == Command.dim_value_time ? sendManual(system, house, device, 32+power*16/100, fs20Time(arg), n)
                : sendManual(system, house, device, encodeCommand(system, cmd), arg, n);
    }

    public boolean sendManual(EZSystem system, String house, int device,
            Command cmd, int arg, int n) throws HarcHardwareException {
        return sendManual(system, house, device, encodeCommand(system, cmd), arg, n);
    }

    // throws IllegalArgumentException
    public String urlManual(String systemName, String house, int device,
            int value, int arg, int n) {
        return urlManual(EZSystem.parse(systemName), house, device, value, arg, n);
    }

    public String urlManual(EZSystem system, String house, int device,
            int value, int arg, int n) {
        String url = null;
        switch (system) {
            case FS20:
                url = fs20Url(house, device, value, arg, n);
                break;
            case RS200:
                url = rs200Url(house, device, value, n);
                break;
            case AB400:
                url = ab400Url(house, device, value, n);
                break;
            case IT:
                url = intertechnoUrl(house.charAt(0), device, value, n);
                break;
            case MARMI:
                url = x10Url(house.charAt(0), device, value, n);
                break;
            default:
                System.err.println("Sorry, system " + system + " is not yet implemented.");
                break;
        }
        return url;
    }

    // Callable as special command
    public String powerToggle(String preset) throws HarcHardwareException {
        return sendPreset(Integer.parseInt(preset), Command.power_toggle) ? "" : null;
    }

    public boolean sendPreset(int switchNumber, Command cmd) throws HarcHardwareException {
        Command actualCommand = cmd != Command.power_toggle ? cmd
                : (getStatus(switchNumber) == 0 ? Command.power_on : Command.power_off);
        try {
            return interfaze == Interface.udp ? udpSendPreset(switchNumber, actualCommand) : getUrl(urlPreset(switchNumber, actualCommand));
        } catch (MalformedURLException ex) {
            throw new HarcHardwareException(ex);
        } catch (IOException ex) {
            throw new HarcHardwareException(ex);
        }
    }

    public boolean sendPreset(int switchNumber, Command cmd, int count) throws HarcHardwareException {
        boolean result = true;
        for (int i = 0; i < count; i++) {
            boolean stat = sendPreset(switchNumber, cmd);
            result &= stat;
        }
        return result;
    }

    public boolean sendPreset(int switchNumber, int value) throws IllegalArgumentException, HarcHardwareException {
        try {
            // Not possible with udp.
            return getUrl(urlPreset(switchNumber, value));
        } catch (MalformedURLException ex) {
            throw new HarcHardwareException(ex);
        } catch (IOException ex) {
            throw new HarcHardwareException(ex);
        }
    }

    public boolean sendPreset(int switchNumber, int value, int count) throws HarcHardwareException {
        boolean result = true;
        for (int i = 0; i < count; i++) {
            boolean stat = sendPreset(switchNumber, value);
            result &= stat;
        }
        return result;
    }

    private boolean udpSendPreset(int switchNumber, Command cmd) {
        if (verbose)
            System.err.println("Sending command `" + cmd + "' to preprogrammed " + switchNumber + " to T10 `" + ezcontrolIP + "' over UDP.");

        InetAddress addr;
        try {
            addr = InetAddress.getByName(ezcontrolIP);
        } catch (UnknownHostException ex) {
            System.err.println("Unknown host: " + ezcontrolIP);
            return false;
        }
        byte[] buf = new byte[8];
        buf[2] = 1;
        buf[3] = (byte) 171;
        buf[4] = (byte) (switchNumber - 1);
        buf[5] = 0;
        buf[6] = (byte) (cmd == Command.power_off ? 0 : 255);
        buf[7] = 0;
        int sum = checksum(buf);
        buf[0] = (byte) (sum & 0xff);
        buf[1] = (byte) (sum >> 8);
        DatagramPacket dp = new DatagramPacket(buf, buf.length, addr, ezcontrolPortno);
        return udpSendCheck(dp);
    }


    private boolean udpSendCheck(DatagramPacket dp) {
        boolean success = false;
        DatagramSocket sock = null;
        try {
            sock = new DatagramSocket();
            sock.setSoTimeout(soTimeout);
            sock.send(dp);
            byte[] error = new byte[6];
            DatagramPacket errorPacket = new DatagramPacket(error, error.length);
            sock.receive(errorPacket);
            int sum = checksum(error);
            if (!(((sum & 0xff) == error[0]) && ((sum >> 8) == error[1]) && (error[4] == 0) && (error[5] == 0))) {
                System.err.println("Erroneous response from T10");
            } else
                success = true;
        } catch (IOException e) {
            if (e.getClass() == SocketTimeoutException.class)
                System.err.println("UDP socket timeout from " + ezcontrolIP);
            else
                System.err.println(e.getMessage());
        } finally {
            if (sock != null)
                sock.close();
        }
        return success;
    }

    private int checksum(byte[] buf) {
        int sum = 0;
        for (int i = 2; i < buf.length; i += 2)
            sum += ubyte(buf[i]) + 256 * ubyte(buf[i + 1]);

        return sum;
    }

    private int ubyte(byte b) {
        return b < 0 ? b + 256 : b;
    }

    private boolean udpExtractState(byte[] buf) {
        return buf != null ? (buf[66] != 0) : false;
    }

    private String udpExtractName(byte[] buf) {
        try {
            return buf != null ? (new String(buf, 6, 32, "US-ASCII")).replaceAll("\u0000", "") : null;
        } catch (UnsupportedEncodingException ex) {
            assert false;
            return null;
        }
    }

    private byte[] udpStatusInquiry(int n) {
        if (verbose)
            System.err.println("Inquiring state from T10 `" + ezcontrolIP + "' on preset " + n + " using UDP.");
        byte[] buf = new byte[bufferSize];

        buf[0] = 0x11;
        buf[1] = 0x67;
        buf[2] = 2;
        buf[3] = 0;
        buf[4] = (byte) (n - 1);
        buf[5] = 0;
        buf[bufferSize - 4] = (byte) 0xff;
        buf[bufferSize - 3] = (byte) 0xff;

        InetAddress addr;
        try {
            addr = InetAddress.getByName(ezcontrolIP);
        } catch (UnknownHostException ex) {
            System.err.println("Unknown host: " + ezcontrolIP);
            return null;
        }

        DatagramPacket dp = new DatagramPacket(buf, buf.length, addr, ezcontrolQueryPortno);
        DatagramSocket sock = null;
        try {
            sock = new DatagramSocket();
            sock.setSoTimeout(soTimeout);
            sock.send(dp);
            sock.receive(dp);
        } catch (IOException e) {
            if (e.getClass() == SocketTimeoutException.class)
                System.err.println("UDP socket timeout from " + ezcontrolIP);
            else
                System.err.println(e.getMessage());
            buf = null;
        } finally {
            if (sock != null)
                sock.close();
        }
        return buf;
    }

    private String fs20Url(String houseRaw, int device, int value, int arg, int n) {
        try {
            String house = houseRaw.replaceAll("\\s+", "");
            int hc1 = Integer.parseInt(house.substring(0, 4));
            int hc2 = Integer.parseInt(house.substring(4));
            return urlManual(EZSystem.FS20, hc1, hc2, device, value, arg, n);
        } catch (NumberFormatException ex) {
            throw new RuntimeException("String \"" + houseRaw + "\" is not a valid housenumber for FS20.");
        }
    }

    private String ab400Url(String house, int device, int value, int n) {
        try {
            int hc1 = Integer.parseInt(house);
            return urlManual(EZSystem.AB400, hc1, 0, device + 1, value, -1, n);
        } catch (NumberFormatException ex) {
            throw new RuntimeException("String \"" + house + "\" is not a valid housenumber for AB400.");
        }
    }

    private String intertechnoUrl(char house, int device, int value, int n) {
        return intertechnoUrl(Character.toUpperCase(house) - 'A' + 1,
                (device - 1) / 4 + 1,
                (device - 1) % 4 + 1,
                value, n);
    }

    private String intertechnoUrl(int hc1, int hc2, int addr, int value, int n) {
        return urlManual(EZSystem.IT, hc1, hc2, addr, value, -1, n);
    }

    private String x10Url(char house, int device, int value, int n) {
        return x10Url(Character.toUpperCase(house) - 'A' + 1, device, value, n);
    }

    private String x10Url(int hc1, int addr, int value, int n) {
        return urlManual(EZSystem.MARMI, hc1, 0, addr, value, -1, n);
    }

    private String rs200Url(String house, int device, int value, int n) {
        return urlManual(EZSystem.RS200, Integer.parseInt(house), 0, device, value, -1, n);
    }

    private String urlManual(EZSystem system, int hc1, int hc2, int device, int value, int arg, int n) {
        return "http://" + ezcontrolIP + "/send?system=" + system.systemNumber()
                + "&hc1=" + hc1
                + (hc2 != 0 ? "&hc2=" + hc2 : "")
                + "&addr=" + device + "&value=" + value
                + (arg >= 0 ? "&arg=" + arg : "")
                + (n > 1 ? "&n=" + n : "");
    }

    public String urlPreset(int switchNumber, Command cmd) {
        if (cmd != Command.power_on && cmd != Command.power_off) {
            throw new IllegalArgumentException("Command " + cmd + " not allowed here.");
        }
        return urlPreset(switchNumber, cmd == Command.power_on ? "on" : "off");
    }

    public String urlPreset(int switchNumber, int value) throws IllegalArgumentException {
        if (value < 0 || value > 100)
            throw new IllegalArgumentException("value out of range");
        return urlPreset(switchNumber, Integer.toString(value));
    }

    private String urlPreset(int switchNumber, String val) {
        return "http://" + ezcontrolIP + "/preset?switch=" + switchNumber + "&value=" + val;
    }

    private static int encodeCommand(EZSystem system, Command cmd) {
        return system == EZSystem.FS20 ? fs20EncodeCommand(cmd) : simpleEncodeCommand(cmd);
    }

    private static int fs20EncodeCommand(Command cmd) {
        return cmd == Command.power_on ? 255
                : cmd == Command.power_off ? 0
                : cmd == Command.power_toggle ? 18
                : cmd == Command.dim_up ? 19
                : cmd == Command.dim_down ? 20
                : cmd == Command.dim_max_time ? 48
                : cmd == Command.dim_off_time ? 32
                : cmd == Command.set_time ? 54
                : -1;
    }

    private static int simpleEncodeCommand(Command cmd) {
        return cmd == Command.power_on ? 1
                : cmd == Command.power_off ? 0
                : -1;
    }

    private boolean getUrl(String url) throws MalformedURLException, IOException {
        if (verbose)
            System.err.println("Getting URL " + url);

        (new URL(url)).openStream();
        return true;
    }

    public String getStatus() {
        setupStatus();
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < t10NumberPresets; i++) {
            if (state[i] != null) {
                result.append(i).append(".\t").append(state[i].toString()).append("\n");
            }
        }
        return result.toString();
    }

    public int getStatus(int n) {
        if (interfaze == Interface.udp)
            return udpExtractState(udpStatusInquiry(n)) ? 1 : 0;
        else {
            setupStatus();
            return state[n].state;
        }
    }

    public String getPresetStatus(int n) {
        if (interfaze == Interface.udp)
            return udpExtractState(udpStatusInquiry(n)) ? "on" : "off";
        else {
            setupStatus();
            return state[n] != null ? state[n].stateStr() : "n/a";
        }
    }

    public String getPresetName(int n) {
        if (interfaze == Interface.udp) {
            byte[] buf = udpStatusInquiry(n);
            return udpExtractName(buf);
        } else {
            setupStatus();
            return state[n] != null ? state[n].name : "**not assigned**";
        }
    }

    public String getPresetString(int n) {
        if (interfaze == Interface.udp) {
            byte[] buf = udpStatusInquiry(n);
            return buf != null ? (udpExtractName(buf) + (udpExtractState(buf) ? ": on" : ": off")) : "**error**";
        } else {
            setupStatus();
            return state[n] != null ? state[n].toString() : "Preset " + n + " not assigned.";
        }
    }

    private boolean setupStatus() {
        if (state != null) {
            return true;
        }

        String url = "http://" + ezcontrolIP + "/";
        if (verbose) {
            System.err.println("Getting URL " + url);
        }
        StringBuilder data = new StringBuilder();

        BufferedReader r = null;
        boolean success = true;
        try {
            InputStream is = (new URL(url)).openStream();
            r = new BufferedReader(new InputStreamReader(is, IrpUtils.dumbCharset));
            String str;
            do {
                str = r.readLine();
                data.append(str);
            } while (str != null);
        } catch (java.net.MalformedURLException e) {
            System.err.println(e.getMessage());
            success = false;
        } catch (java.io.IOException e) {
            System.err.println("IOException: " + e.getMessage());
            success = false;
        } finally {
            try {
                if (r != null)
                    r.close();
            } catch (IOException ex) {
                System.err.println(ex.getMessage());
            }
        }
        if (!success)
            return false;

        String[] snork = data.toString().split("<tr>");
        state = new Status[t10NumberPresets + 1];

        for (int i = 1; i < snork.length; i++) {
            int p1 = snork[i].indexOf('>');
            int p2 = snork[i].indexOf('<', p1);
            int n = Integer.parseInt(snork[i].substring(p1 + 1, p2));
            p1 = snork[i].indexOf('>', p2 + 5);
            p2 = snork[i].indexOf('<', p1);
            String name = snork[i].substring(p1 + 1, p2);
            int stat =
                    snork[i].matches(".*background:lime.*") ? Status.on
                    : snork[i].matches(".*background:red.*") ? Status.off
                    : Status.unknown;
            //System.out.println("" + n + ": " + name + stat);
            state[n] = new Status(name, stat);
        }
        return true;
    }

    private int extractValue(String str) {
        int res = -1;
        if (str.matches(".*value.*")) {
            int p1 = str.indexOf("value");
            int p2 = str.indexOf('"', p1 + 7);
            res = Integer.parseInt(str.substring(p1 + 7, p2));
        }
        return res;
    }

    @SuppressWarnings("empty-statement")
    private boolean setupTimers() {
        if (timers != null) {
            return true;
        }

        String url = "http://" + ezcontrolIP + "/timer.html";
        if (verbose) {
            System.err.println("Getting URL " + url);
        }
        StringBuilder data = new StringBuilder();

        BufferedReader r = null;
        try {
            InputStream is = (new URL(url)).openStream();
            r = new BufferedReader(new InputStreamReader(is, IrpUtils.dumbCharset));
            String str;
            do {
                str = r.readLine();
                data.append(str);
            } while (str != null);
        } catch (java.net.MalformedURLException e) {
            System.err.println(e.getMessage());
            return false;
        } catch (java.io.IOException e) {
            System.err.println("IOException: " + e.getMessage());
            return false;
        } finally {
            try {
                if (r != null)
                    r.close();
            } catch (IOException ex) {
                System.err.println(ex.getMessage());
            }
        }

        String[] snork = data.toString().split("<table");
        timers = new Timer[t10NumberTimers];

        for (int i = 1; i < t10NumberTimers + 1; i++) {
            boolean enabled = snork[i].matches(".*background:lime.*");
            //int p1 = snork[i].indexOf("<b>");
            //int n = (int) snork[i].charAt(p1 + 4) - (int) 'A';
            String[] inputs = snork[i].split(">");
            boolean[] presets = new boolean[t10NumberPresets + 1];
            boolean[] days = new boolean[7];
            int indx = 0;

            do
                ;
            while (!inputs[indx++].matches("Switches.*"));
            for (int j = 1; j <= t10NumberPresets;) {
                if (inputs[indx].matches(".*<input.*")) {
                    presets[j] = inputs[indx].matches(".*checked.*");
                    //System.out.println(inputs[indx] + j+ presets[j]);
                    j++;
                }
                indx++;
            }

            do {

            } while (!inputs[indx++].matches("Weekdays.*"));
            for (int j = 0; j < 7;) {
                if (inputs[indx].matches(".*<input.*")) {
                    days[j] = inputs[indx].matches(".*checked.*");
                    //System.out.println(inputs[indx] + j+ days[j]);
                    j++;
                }
                indx++;
            }

            do {

            } while (!inputs[indx++].matches("ON Time.*"));
            int onH = extractValue(inputs[indx++]);
            int onM = extractValue(inputs[indx++]);

            do {

            } while (!inputs[indx++].matches("OFF Time.*"));
            int offH = extractValue(inputs[indx++]);
            int offM = extractValue(inputs[indx++]);
            timers[i - 1] = new Timer(presets, days, enabled, onH, onM, offH, offM);
        }
        return true;
    }

    public String getTimers() {
        boolean ok = setupStatus() && setupTimers();
        if (!ok) {
            System.err.println("Could not get timers");
            return null;
        }
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < t10NumberTimers; i++) {
            if (timers[i].enabled) {
                result.append((char) (i + (int) 'A')).append(": ").append(timers[i].toString()).append("\n");
            }
        }

        return result.toString();
    }

    public String getTimer(int n) {
        boolean ok = setupStatus() && setupTimers();
        if (!ok)
            return null;
        return timers[n].toString();
    }

    public String getTimer(String name) {
        int n = ((int) name.charAt(0) - (int) 'A') % 32;
        String result = "";
        if (n >= 0 && n < t10NumberTimers) {
            result = getTimer(((int) name.charAt(0) - (int) 'A') % 32);
        } else {
            System.err.println("Erroneous timer name \"" + name + "\".");
        }
        return result;
    }

    public static String getTimers(String hostname) {
        return (new EzControlT10(hostname)).getTimers();
    }

    public static String getTimer(String hostname, int n) {
        return (new EzControlT10(hostname)).getTimer(n);
    }

    public static String getTimer(String hostname, String name) {
        return (new EzControlT10(hostname)).getTimer(name);
    }

    public static String getStatus(String hostname) {
        return (new EzControlT10(hostname)).getStatus();
    }

    public Document xmlConfig() {
        Document doc = XmlUtils.newDocument();
        Element root = doc.createElement("ezcontrol_t10");
        root.setAttribute("hostname", ezcontrolIP);
        doc.appendChild(root);
        Element presets = doc.createElement("presets");
        root.appendChild(presets);
        setupStatus();
        for (int i = 1; i <= t10NumberPresets; i++) {
            if (state[i] != null) {
                Element p = doc.createElement("preset");
                p.setAttribute("id", "preset_" + i);
                p.setAttribute("number", "" + i);
                p.setAttribute("state", state[i].stateStr());
                p.setTextContent(state[i].name);
                presets.appendChild(p);
            }
        }

        Element presetConfiguration = doc.createElement("preset_configuration");
        root.appendChild(presetConfiguration);
        Element timersEle = doc.createElement("timers");
        root.appendChild(timersEle);
        setupTimers();
        for (int i = 0; i < t10NumberTimers; i++) {
            if (timers[i] != null) {
                Element t = doc.createElement("timer");
                t.setAttribute("name", "" + (char) (i + (int) 'A'));
                t.setAttribute("enabled", timers[i].enabled ? "yes" : "no");
                if (timers[i].onTime.hour != -1) {
                    Element on = doc.createElement("on");
                    on.setAttribute("hour", "" + timers[i].onTime.hour);
                    on.setAttribute("minute", "" + timers[i].onTime.minute);
                    t.appendChild(on);
                }
                if (timers[i].offTime.hour != -1) {
                    Element off = doc.createElement("off");
                    off.setAttribute("hour", "" + timers[i].offTime.hour);
                    off.setAttribute("minute", "" + timers[i].offTime.minute);
                    t.appendChild(off);
                }
                boolean atLeastOneDay = false;
                for (int j = 0; j < 7; j++) {
                    atLeastOneDay = atLeastOneDay || timers[i].days[j];
                }
                if (atLeastOneDay) {
                    Element days = doc.createElement("days");
                    for (int j = 0; j < 7; j++) {
                        if (timers[i].days[j]) {
                            Element day = doc.createElement("day");
                            day.setAttribute("weekday", "" + (j + 1));
                            day.setAttribute("name", daynames[j]);
                            days.appendChild(day);
                        }
                    }
                    t.appendChild(days);
                }

                boolean atLeastOnePreset = false;
                for (int j = 1; j <= t10NumberPresets; j++) {
                    atLeastOnePreset = atLeastOnePreset || timers[i].presets[j];
                }
                if (atLeastOnePreset) {
                    Element presetrefs = doc.createElement("presetrefs");
                    for (int j = 1; j <= t10NumberPresets; j++) {
                        if (timers[i].presets[j]) {
                            Element presetref = doc.createElement("presetref");
                            presetref.setAttribute("preset", "preset_" + j);
                            presetref.setTextContent(state[j].name);
                            presetrefs.appendChild(presetref);
                        }
                    }
                    t.appendChild(presetrefs);
                }
                timersEle.appendChild(t);
            }
        }

        Element network = doc.createElement("network");
        root.appendChild(network);
        return doc;
    }

    private void generateXml() {
        XmlUtils.printDOM(System.out, xmlConfig(), "ezcontrol_t10_config.dtd", null);
    }

    public void generateXml(File file) throws FileNotFoundException {
        XmlUtils.printDOM(file, xmlConfig(), "ezcontrol_t10_config.dtd", null);
    }

    public void getConfiguration(File file) {
        try {
            XmlUtils.printDOM(file, xmlConfig(), "ezcontrol_t10_config.dtd", null);
        } catch (FileNotFoundException e) {
            System.err.println(e.getMessage());
        }
    }

    private static void usage() {
        System.err.println("Usage:\n" + "ezcontrol [<options>] get_status [<presetNumber>]\n"
                + "or\n" + "ezcontrol [<options>] get_timer [<timername>]\n"
                + "or\n" + "ezcontrol [<options>] <presetNumber> <command>\n"
                + "or\n" + "ezcontrol [<options>] <presetNumber> <value_in_percent>\n"
                + "or\n" + "ezcontrol [<options>] <systemName> <housecode> <deviceNumber> <command> [<arg>]\n"
                + "or\n" + "ezcontrol [<options>] xml\n"
                + "\nwhere options=-h <hostname>,-d <debugcode>,-# <count>,-v, -u\n"
                + "and command=power_on,power_off,power_toggle,get_status,...");
        doExit(IrpUtils.exitUsageError);
    }

    private static void doExit(int exitcode) {
        System.exit(exitcode);
    }

    public static void main(String args[]) {
        boolean verbose = false;
        String ezcontrolHost = defaultEzcontrolIP;
        //int debug = 0;
        int arg_i = 0;
        int count = 1;
        boolean presetMode = false;
        boolean doGetStatus = false;
        boolean doGetTimers = false;
        boolean doXml = false;
        Command cmd = Command.invalid;
        //String commandName = null;
        String timerName = null;
        EZSystem system = null;
        String housecode = null;
        int deviceNumber = -1;
        int numArg = -1;
        int percentValue = -1;
        int value = -1;
        int arg = -1;
        boolean udp = false;

        try {
            while (arg_i < args.length && (args[arg_i].length() > 0) && args[arg_i].charAt(0) == '-') {

                if (args[arg_i].equals("-v")) {
                    verbose = true;
                    arg_i++;
                } else if (args[arg_i].equals("-u")) {
                    arg_i++;
                    udp = true;
                } else if (args[arg_i].equals("-h")) {
                    arg_i++;
                    ezcontrolHost = args[arg_i++];
                } else if (args[arg_i].equals("-d")) {
                    arg_i++;
                    //debug = Integer.parseInt(args[arg_i++]);
                } else if (args[arg_i].equals("-#")) {
                    arg_i++;
                    count = Integer.parseInt(args[arg_i++]);
                } else {
                    usage();
                }
            }

            if (args[arg_i].equals("getStatus")) {
                doGetStatus = true;
                if (args.length - arg_i > 1) {
                    numArg = Integer.parseInt(args[arg_i + 1]);
                }
            } else if (args[arg_i].equals("getTimer")) {
                doGetTimers = true;
                if (args.length - arg_i > 1) {
                    timerName = args[arg_i + 1];
                }
            } else if (args[arg_i].equals("xml")) {
                doXml = true;
            } else if (args.length - arg_i == 2) {// Preset command
                presetMode = true;
                numArg = Integer.parseInt(args[arg_i]);
                try {
                    cmd = Command.valueOf(args[arg_i + 1]);
                } catch (IllegalArgumentException e) { // FIXME
                    cmd = Command.set_power;
                    percentValue = Integer.parseInt(args[arg_i + 1]);
                }
            } else {
                system = EZSystem.valueOf(args[arg_i].toUpperCase(Locale.US));
                housecode = args[arg_i + 1];
                String devString = args[arg_i + 2];

                deviceNumber = (int) devString.charAt(0) > (int) '9'
                        ? (int) devString.toUpperCase(Locale.US).charAt(0) - (int) 'A'
                        : Integer.parseInt(devString);
                cmd = Command.valueOf(args[arg_i + 3].toLowerCase(Locale.US));

                value = encodeCommand(system, cmd);
                arg = args.length > arg_i + 4 ? Integer.parseInt(args[arg_i + 4]) : -1;
                if (cmd.hasTimeArgument())
                    arg = fs20Time(arg);
            }
        } catch (ArrayIndexOutOfBoundsException | NumberFormatException ex) {
            usage();
        }

        if (numArg != -1 && (numArg < 1 || numArg > t10NumberPresets)) {
            System.err.println("Numerical argument not valid.");
            System.exit(45);
        }
        if (deviceNumber != -1 && value == -1) {
            System.err.println("Only commands power_on and power_off allowed.");
            System.exit(46);
        }
        EzControlT10 ez = new EzControlT10(ezcontrolHost, verbose, udp ? Interface.udp : Interface.http);

        try {
            if (doGetStatus) {
                if (numArg > 0) {
                    System.out.println(ez.getPresetString(numArg));
                    System.out.println(ez.getPresetName(numArg));
                } else {
                    System.out.println(ez.getStatus());
                }
            } else if (doGetTimers) {
                if (timerName != null) {
                    System.out.println(ez.getTimer(timerName));
                } else {
                    System.out.println(ez.getTimers());
                }
            } else if (doXml) {
                ez.generateXml();
            } else if (presetMode) {
                if (cmd == Command.set_power)
                    ez.sendPreset(numArg, percentValue);
                else
                    ez.sendPreset(numArg, cmd);
            } else {
                ez.sendManual(system, housecode, deviceNumber, value, arg, count);
            }
        } catch (HarcHardwareException ex) {
            System.err.println(ex.getMessage());
        }
    }
}
