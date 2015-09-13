/*
Copyright (C) 2013, 2014 Bengt Martensson.

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

package org.harctoolbox.harchardware.ir;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import org.harctoolbox.IrpMaster.DecodeIR;
import org.harctoolbox.IrpMaster.IncompatibleArgumentException;
import org.harctoolbox.IrpMaster.IrSequence;
import org.harctoolbox.IrpMaster.IrpMasterException;
import org.harctoolbox.IrpMaster.IrpUtils;
import org.harctoolbox.IrpMaster.ModulatedIrSequence;
import org.harctoolbox.harchardware.HarcHardwareException;
import org.harctoolbox.harchardware.IHarcHardware;

/**
 * This class runs an external program, for example mode2 of LIRC, in a separate process,
 * and evaluates its output, which is assumed to be in the LIRC mode2 format.
 */
public final class LircMode2 implements IHarcHardware, ICapture, IReceive  {

    private boolean verbose;
    private int debug;
    private final ArrayList<Integer> data;
    private boolean stopRequest;
    private Date lastRead;
    private Date currentStart;
    private ProgThread progThread;
    private int beginTimeout;
    private int maxLearnedLength;
    private int endTimeout;
    private boolean ignoreSillyLines;
    private String cmd;
    private String[] cmdArray;

    private LircMode2(String cmd, String[] cmdArray, boolean verbose, int beginTimeout, int maxLearnLength, int endTimeout, boolean ignoreSillyLines) {
        this.data = new ArrayList<Integer>();
        this.verbose = verbose;
        this.beginTimeout = beginTimeout;
        this.maxLearnedLength = maxLearnLength;
        this.endTimeout = endTimeout;
        this.ignoreSillyLines = ignoreSillyLines;
        this.cmd = cmd;
        this.cmdArray = cmdArray;
    }

    /**
     *
     * @param cmd
     * @param verbose
     * @param beginTimeout
     * @param maxLearnLength
     * @param endTimeout
     */
    public LircMode2(String cmd, boolean verbose, int beginTimeout, int maxLearnLength, int endTimeout) {
        this(cmd, null, verbose, beginTimeout, maxLearnLength, endTimeout, false);
    }

    /**
     *
     * @param cmdArray
     * @param verbose
     * @param beginTimeout
     * @param maxLearnLength
     * @param endTimeout
     */
    public LircMode2(String[] cmdArray, boolean verbose, int beginTimeout, int maxLearnLength, int endTimeout) {
        this(null, cmdArray, verbose, beginTimeout, maxLearnLength, endTimeout, false);
    }

    @Override
    public String getVersion() {
        return null;
    }

    @Override
    public void setVerbosity(boolean verbose) {
        this.verbose = verbose;
    }

    @Override
    public void setDebug(int debug) {
        this.debug = debug;
    }

    public void setCommand(String command) {
        this.cmd = command;
        this.cmdArray = null;
    }

    @Override
    public boolean isValid() {
        return progThread != null;
    }

    @Override
    public void close() {
        stopRequest = true;
        progThread = null;
    }

    private static int parseMode2Line(String str) {
        return
                str == null ? 0
                : str.startsWith("pulse") ? Integer.parseInt(str.substring(6))
                : str.startsWith("space") ? -Integer.parseInt(str.substring(6))
                : 0;
    }

    @Override
    public void open() throws IOException {
        currentStart = new Date();
        progThread = new ProgThread(this);
        stopRequest = false;
    }

    @Override
    public ModulatedIrSequence capture() throws HarcHardwareException, IOException, IrpMasterException {
        return new ModulatedIrSequence(receive(), IrpUtils.defaultFrequency, IrpUtils.invalid);
    }

    @Override
    public boolean stopCapture() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static class ProgThread extends Thread {

        final BufferedReader outFromProc;
        final Process process;
        final LircMode2 lircMode2;

        ProgThread(LircMode2 lircMode2) throws IOException {
            process = lircMode2.cmd != null
                    ? Runtime.getRuntime().exec(lircMode2.cmd)
                    : Runtime.getRuntime().exec(lircMode2.cmdArray);
            this.lircMode2 = lircMode2;

            outFromProc = new BufferedReader(new InputStreamReader(process.getInputStream(), IrpUtils.dumbCharsetName));
            if (lircMode2.verbose) {
                if (lircMode2.cmd != null)
                    System.err.println("Now started shell command \"" + lircMode2.cmd + "\"");
                else {
                    System.err.print("Now started shell command ");
                    for (String s : lircMode2.cmdArray) {
                        System.err.print(s + " ");
                    }
                    System.err.println();
                }
            }
        }

        @Override
        public void run() {
            lircMode2.currentStart = new Date();
            boolean hasWarned = false;
            while (!lircMode2.stopRequest) {
                try {
                    String line = outFromProc.readLine();
                    int duration = parseMode2Line(line);
                    if (duration == 0) {
                        // silly line read
                        if (lircMode2.ignoreSillyLines)
                            continue;
                        else
                            break;
                    }
                    synchronized (lircMode2.data) {
                        if (lircMode2.data.isEmpty()) {
                            if (duration <= 0) // ignore starting spaces
                                continue;
                            // if two starting pulses, ignore the first
                            int next = parseMode2Line(outFromProc.readLine());
                            if (next < 0) // normal case
                                lircMode2.data.add(duration);

                            lircMode2.data.add(next);
                            lircMode2.currentStart = new Date();
                            hasWarned = false;
                        } else if (timeleft(lircMode2.maxLearnedLength, lircMode2.currentStart) > 0) {
                            lircMode2.data.add(duration);
                        } else {
                            if (!hasWarned && lircMode2.verbose) {
                                System.err.println("Warning. Max capture length = "
                                        + lircMode2.maxLearnedLength + "ms exceeded. Ignoring excess pairs. Capture will resume after next silence period.");
                                hasWarned = true;
                            }
                        }
                        lircMode2.lastRead = new Date();
                    }
                } catch (IOException ex) {
                    System.err.println(ex);
                    break;
                }
            }
            if (lircMode2.verbose)
                System.err.println("done, killing process");
            process.destroy();
        }
    }

    public boolean isAlive() {
        return progThread.isAlive();
    }

    private static int timeleft(int timeout, Date old) {
        return timeout - (int) (((new Date())).getTime() - old.getTime());
    }

    private void waitInitialSilence() {
        while (data.isEmpty() && timeleft(beginTimeout, currentStart) > 0) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {

            }
        }
    }

    private void waitEndingSilence() {
        while (timeleft(endTimeout, lastRead) > 0) {
            try {
                Thread.sleep(timeleft(endTimeout, lastRead));
            } catch (InterruptedException ex) {

            }
        }
    }

    private void waitMaxLengthOrEndTimeout() {
      while (timeleft(maxLearnedLength, currentStart) > 0 && timeleft(endTimeout, lastRead) > 0)
            try {
            Thread.sleep(Math.min(timeleft(maxLearnedLength, currentStart), timeleft(endTimeout, lastRead)));
        } catch (InterruptedException ex) {

        }
    }

    @Override
    public void setTimeout(int beginTimeout, int maxLearnedLength, int endTimeout) {
        this.beginTimeout = beginTimeout;
        this.maxLearnedLength = maxLearnedLength;
        this.endTimeout = endTimeout;
    }

    @Override
    public void setTimeout(int timeout) {
        setTimeout(timeout, timeout, 100);
    }

    @Override
    public IrSequence receive() throws HarcHardwareException {
        if (progThread == null)
            throw new HarcHardwareException("LircMode2 not open");

        if (!progThread.isAlive())
            progThread.start();
        reset();
        waitInitialSilence();
        if (data.isEmpty()) {
            // beginTimeout hits
            currentStart = new Date();
            return null;
        }
        waitMaxLengthOrEndTimeout();
        waitEndingSilence();
        int[] durations = getDurations();
        try {
            IrSequence seq = new IrSequence(durations);
            return seq.getLength() > 0 ? seq : null;
        } catch (IncompatibleArgumentException ex) {
            // cannot happen, while I explicitly made sure that durations has even length;
        }
        return null;
    }

    private synchronized int[] getDurations() {
        int length = data.size();
        if (length % 2 == 1) {
            length++;
        }
        int[] result = new int[length];
        for (int i = 0; i < data.size(); i++)
            result[i] = data.get(i);

        if (data.size() % 2 == 1)
            result[data.size()] = -100000;

        reset();
        return result;
    }

    public synchronized void reset() {
        data.clear();
        currentStart = new Date();
    }

    @Override
    public boolean stopReceive() {
        close();
        return true;
    }

    public static void main(String[] args) {
        String[] cmd = new String[]{"/usr/local/bin/mode2", "-H", "commandIR"};
        try {
            LircMode2 lircMode2 = new LircMode2(cmd, true, 3000, 1000, 300);
            lircMode2.open();
            int noNulls = 0;
            for (int i = 0; i < 20 && noNulls < 3; i++) {
                lircMode2.setTimeout(5000, 1000, 300);
                IrSequence seq = lircMode2.receive();
                if (seq == null) {
                    System.err.println("Got null");
                    noNulls++;
                } else {
                    noNulls = 0;
                    System.out.println(seq);
                    ModulatedIrSequence mseq = new ModulatedIrSequence(seq, IrpUtils.defaultFrequency, (double) IrpUtils.invalid);
                    DecodeIR.invoke(mseq);
                }
            }
            lircMode2.close();
            Thread.sleep(3000);
        } catch (InterruptedException | IOException | HarcHardwareException ex) {
            System.err.println(ex);
        }
    }
}
