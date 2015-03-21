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

package org.harctoolbox.harchardware.ir;

import java.io.IOException;
import javax.sound.sampled.LineUnavailableException;
import org.harctoolbox.IrpMaster.IrSignal;
import org.harctoolbox.IrpMaster.IrpMasterException;
import org.harctoolbox.IrpMaster.ModulatedIrSequence;
import org.harctoolbox.IrpMaster.Wave;
import org.harctoolbox.harchardware.IHarcHardware;

/**
 * This class does something interesting and useful. Or not...
 */
public class IrAudioDevice implements IHarcHardware, IRawIrSender {
    private int sampleFrequency;
    private int sampleSize;
    private int channels;
    private boolean bigEndian;
    private boolean omitTail;
    private boolean square;
    private boolean divide;

    private boolean verbose;

    /**
     *
     * @param sampleFrequency
     * @param sampleSize
     * @param channels
     * @param bigEndian
     * @param omitTail
     * @param square
     * @param divide
     * @param verbose
     */
    public IrAudioDevice(int sampleFrequency, int sampleSize, int channels, boolean bigEndian,
            boolean omitTail, boolean square, boolean divide, boolean verbose) {
        this.sampleFrequency = sampleFrequency;
        this.sampleSize = sampleSize;
        this.channels = channels;
        this.bigEndian = bigEndian;
        this.omitTail = omitTail;
        this.square = square;
        this.divide = divide;
        this.verbose = verbose;
    }

    /**
     *
     * @param sampleFrequency
     * @param channels
     * @param omitTail
     * @param verbose
     */
    public IrAudioDevice(int sampleFrequency, int channels, boolean omitTail, boolean verbose) {
        this(sampleFrequency, 8, channels, false, omitTail, true, true, verbose);
    }

    @Override
    public boolean sendIr(IrSignal irSignal, int count, Transmitter transmitter) throws NoSuchTransmitterException, IrpMasterException, IOException {
        ModulatedIrSequence seq = irSignal.toModulatedIrSequence(count);
        Wave wave = new Wave(seq, sampleFrequency, sampleSize, channels, bigEndian, omitTail, square, divide);
        try {
            wave.play();
            if (verbose)
                System.err.println("Sent IrSignal @ " + sampleFrequency + "Hz, " + sampleSize + "bits to audio device");
            return true;
        } catch (LineUnavailableException ex) {
            return false;
        }
    }

    @Override
    public String getVersion() throws IOException {
        return null;
    }

    @Override
    public void setVerbosity(boolean verbose) {
        this.verbose = verbose;
    }

    @Override
    public void setTimeout(int timeout) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void close() throws IOException {
        // nothing to do
    }

    @Override
    public void open() throws IOException {
        // nothing to do
    }

    @Override
    public Transmitter getTransmitter() {
        return null;
    }

    @Override
    public void setDebug(int debug) {
    }
}
