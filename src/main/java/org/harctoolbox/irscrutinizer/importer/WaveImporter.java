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

package org.harctoolbox.irscrutinizer.importer;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import javax.sound.sampled.UnsupportedAudioFileException;
import org.harctoolbox.harchardware.ir.Wave;
import org.harctoolbox.ircore.InvalidArgumentException;
import org.harctoolbox.ircore.ModulatedIrSequence;

/**
 * This class wraps the IrpMaster Wave class to an importer.
 */
public class WaveImporter extends FileImporter implements IModulatedIrSequenceImporter,IFileImporter {

    private boolean divideCarrier;
    private ModulatedIrSequence sequence;

    public WaveImporter(boolean divideCarrier) {
        super();
        this.divideCarrier = divideCarrier;
    }

    /**
     * @param divideCarrier the divideCarrier to set
     */
    public void setDivideCarrier(boolean divideCarrier) {
        this.divideCarrier = divideCarrier;
    }

    @Override
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public void load(File file, String origin, String charsetName /* ignored */) throws IOException {
        try {
            Wave wave = new Wave(file);
            sequence = wave.analyze(System.err, divideCarrier);
        } catch (UnsupportedAudioFileException ex) {
            throw new IOException(ex.getMessage());
        }
    }

    public void load(File file) throws IOException, ParseException, InvalidArgumentException {
        load(file, "US-ASCII");
    }

    @Override
    public String[][] getFileExtensions() {
        return new String[][]{new String[]{"Wave files (*.wav *.wave)", "wave", "wav" }};
    }

    @Override
    public String getFormatName() {
        return "Wave";
    }

    @Override
    public ModulatedIrSequence getModulatedIrSequence() {
        return sequence;
    }
}
