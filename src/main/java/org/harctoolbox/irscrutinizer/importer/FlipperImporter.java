/*
 * Copyright (C) 2025 Bengt Martensson
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.harctoolbox.irscrutinizer.importer;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.harctoolbox.girr.Remote;
import org.harctoolbox.girr.CommandSet;
import org.harctoolbox.girr.GirrException;
import org.harctoolbox.girr.RemoteSet;
import org.harctoolbox.ircore.InvalidArgumentException;
import org.harctoolbox.irscrutinizer.Version;
import org.harctoolbox.remotelocator.FlipperParser;
import org.harctoolbox.xml.XmlUtils;

/**
 * Importer for Flipper IR files, as defined in https://developer.flipper.net/flipperzero/doxygen/infrared_file_format.html
 * Carries no name of the remote.
 * Has a duty cycle number, but this is almost surely just a trivial "0.33", not a measurement
 * (the Flipper hardware only has a demodulating TSOP-*38).
 */
public class FlipperImporter extends RemoteSetImporter implements IReaderImporter {

    public static final String homeUrl = "https://flipperzero.one";
    private static final String defaultCharsetName = "windows-1252";
    //private static final Double dummyEndingGap = 50000.0;
    private static final Logger logger = Logger.getLogger(FlipperImporter.class.getName());

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public static void main(String[] args) {
        FlipperImporter importer = new FlipperImporter();
        try {
            importer.load(new File(args[0]), defaultCharsetName);
            RemoteSet rs = importer.getRemoteSet();
            XmlUtils.printDOM(rs.toDocument("read file", false, true, false, true));
        } catch (IOException | ParseException | InvalidArgumentException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void load(Reader reader, String origin) throws IOException, ParseException {
        prepareLoad(origin);
        try {
            CommandSet commandSet = FlipperParser.parse(reader);
            Collection<CommandSet> commandSets = new ArrayList<>(4);
            commandSets.add(commandSet);
            Remote remote = new Remote(new Remote.MetaData(), null /* comment */, null /* notes */, commandSets, null /* applicationParameters */);

            remoteSet = new RemoteSet(getCreatingUser(),
                    origin, //java.lang.String source,
                    (new Date()).toString(), //java.lang.String creationDate,
                    Version.appName, //java.lang.String tool,
                    Version.version, //java.lang.String toolVersion,
                    null, //java.lang.String tool2,
                    null, //java.lang.String tool2Version,
                    null, //java.lang.String notes,
                    remote);
        } catch (GirrException ex) {
            throw new IOException(ex);
        }
    }
    
    @Override
    public String[][] getFileExtensions() {
        return new String[][]{ new String[]{ "Flipper IR files (*.ir)", "ir" }};
    }

    @Override
    public String getFormatName() {
        return "Flipper IR";
    }
}
