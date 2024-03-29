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

package org.harctoolbox.irscrutinizer.exporter;

import java.io.File;
import java.io.FileNotFoundException;
import org.harctoolbox.girr.Command;
import org.harctoolbox.guicomponents.AudioParametersBean;
import org.harctoolbox.harchardware.ir.Wave;
import org.harctoolbox.ircore.IrCoreException;
import org.harctoolbox.ircore.ModulatedIrSequence;
import org.harctoolbox.irp.IrpException;
import org.w3c.dom.DocumentFragment;

/**
 * This class exports a (single) Command to a wave file.
 */
public class WaveExporter extends CommandExporter {
    private static final DocumentFragment documentation = null;//XmlUtils.stringToDocumentFragment("Wave exporter documentation not yet written.");

    private int sampleFrequency;
    private int sampleSize;
    private int channels;
    private boolean bigEndian;
    private boolean omitTail;
    private boolean square;
    private boolean divideCarrier;

    public WaveExporter(
            int sampleFrequency,
            int sampleSize,
            int channels,
            boolean bigEndian,
            boolean omitTail,
            boolean square,
            boolean divideCarrier) {
        super();
        this.sampleFrequency = sampleFrequency;
        this.sampleSize = sampleSize;
        this.channels = channels;
        this.bigEndian = bigEndian;
        this.omitTail = omitTail;
        this.square = square;
        this.divideCarrier = divideCarrier;
    }

    public WaveExporter(AudioParametersBean exportAudioParametersBean) {
        this(exportAudioParametersBean.getSampleFrequency(),
                exportAudioParametersBean.getSampleSize(),
                exportAudioParametersBean.getChannels(),
                exportAudioParametersBean.getBigEndian(),
                exportAudioParametersBean.getOmitTrailingGap(),
                exportAudioParametersBean.getSquare(),
                exportAudioParametersBean.getDivideCarrier());
    }

    @Override
    public String[][] getFileExtensions() {
        return new String[][]{ new String[] { "Wave files (*.wav *.wave)", "wav", "wave" } };
    }

    @Override
    public String getName() {
        return "Wave";
    }

    @Override
    public String getPreferredFileExtension() {
        return "wav";
    }

    @Override
    public boolean considersRepetitions() {
        return true;
    }

    @Override
    public DocumentFragment getDocumentation() {
        return documentation;
    }

    @Override
    public void export(Command command, String source, String title, int repeatCount, File file, String charsetName) throws IrpException, IrCoreException, FileNotFoundException {
        ModulatedIrSequence seq = command.toIrSignal().toModulatedIrSequence(repeatCount);
        export(seq, file);
        possiblyMakeExecutable(file);
    }

    public void export(ModulatedIrSequence seq, File file) throws FileNotFoundException, IrpException, IrCoreException {
        Wave wave = new Wave(seq,
                sampleFrequency,
                sampleSize,
                channels,
                bigEndian,
                omitTail,
                square,
                divideCarrier);
        export(wave, file);
    }

    private void export(Wave wave, File file) {
        wave.export(file);
    }
}
