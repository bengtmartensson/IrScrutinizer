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
import java.net.URL;
import org.harctoolbox.IrpMaster.IrpMasterException;
import org.harctoolbox.IrpMaster.ModulatedIrSequence;
import org.harctoolbox.IrpMaster.Wave;
import org.harctoolbox.girr.Command;
import org.harctoolbox.guicomponents.AudioParametersBean;

/**
 *
 */
public class WaveExporter extends CommandExporter implements ICommandExporter {

    public final static String formatName = "Wave";
    public final static String[][] fileExtensions = new String[][]{new String[]{"Wave files (*.wav *.wave)", "wav", "wave"}};
    public final static String preferredFileExtension = "wav";
    public final static String documentation = "One IrSequence packed as an audion file.";
    public final static URL url = null;

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
        super(formatName, fileExtensions, preferredFileExtension, documentation, url,
                null /* options */, true /* simpleSequence */, true /* binary */);
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
/*
    @Override
    public String[][] getFileExtensions() {
        return new String[][]{ new String[] { "Wave files (*.wav *.wave)", "wav", "wave" } };
    }

    @Override
    public String getFormatName() {
        return "Wave";
    }

    @Override
    public String getPreferredFileExtension() {
        return "wav";
    }*/

    //public void export(Command command, String source, String title, int repeatCount, File exportFile, String creatingUser) throws IrpMasterException, FileNotFoundException {
    //    export(command, source, title, repeatCount, exportFile, null, creatingUser);
    //}

    @Override
    public void export(Command command, String source /* ignored */, String title /* ignored */,
            int repeatCount, File exportFile, String charsetName /* ignored */, String creatingUser)
            throws IrpMasterException, FileNotFoundException {
        ModulatedIrSequence seq = command.toModulatedIrSequence(repeatCount);
        Wave wave = new Wave(seq,
                sampleFrequency,
                sampleSize,
                channels,
                bigEndian,
                omitTail,
                square,
                divideCarrier);
        wave.export(exportFile);
    }
}
