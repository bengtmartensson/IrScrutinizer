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

package org.harctoolbox.irscrutinizer.importer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Serializable;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.harctoolbox.IrpMaster.IrSignal;
import org.harctoolbox.IrpMaster.IrpMasterException;
import org.harctoolbox.girr.Command;
import org.harctoolbox.girr.Remote;
import org.harctoolbox.girr.RemoteSet;
import org.harctoolbox.irscrutinizer.Version;

/**
 * This class imports CML files.
 *
 */
public class CmlImporter extends RemoteSetImporter implements IFileImporter, Serializable {
    private static final long serialVersionUID = 1L;

    // I have no idea of a/the correct character set in the CML files.
    // Therefore, select the largest of the 8 bit character sets.
    private final String charactersetName = "WINDOWS-1252";
    private static final int remoteToken = 0xbbbbbbbb;
    private static final int commandToken = 0xcccccccc;
    private static final int EOF = -1;

    public CmlImporter() {
        super();
    }

    //@Override
    //public void load(Reader reader, String origin) throws IOException, FileNotFoundException, ParseException, IrpMasterException {
    //    dumbLoad(reader, origin);
    //}

    @Override
    public void load(Reader reader, String origin) throws IOException, ParseException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void load(File file, String origin) throws IOException, ParseException {
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            load(fileInputStream, origin);
        }
    }

    //@Override
    //public void load(File file, String origin) throws IOException, ParseException, IrpMasterException {
    //    load(new InputStreamReader(new FileInputStream(file), IrpUtils.dumbCharset), origin);
    //}

    @Override
    public void load(InputStream reader, String origin) throws IOException, ParseException {
        prepareLoad(origin);
        remoteSet = parseRemoteSet(reader, origin);
        setupCommands();
    }

    private RemoteSet parseRemoteSet(InputStream inputStream, String origin) throws IOException, ParseException {
        HashMap<String, Remote> remotes = new HashMap<>();
        while (true) {
            int token = searchToken(inputStream);
            if (token == remoteToken)
                break;
            if (token == 0)
                return null;
        }
        while (inputStream.available() > 0) {
            Remote remote = parseRemote(inputStream);
            if (remote != null)
                remotes.put(remote.getName(), remote);
        }

        remoteSet = new RemoteSet(getCreatingUser(),
                 origin, //java.lang.String source,
                 (new Date()).toString(), //java.lang.String creationDate,
                 Version.appName, //java.lang.String tool,
                 Version.version, //java.lang.String toolVersion,
                 null, //java.lang.String tool2,
                 null, //java.lang.String tool2Version,
                 null, //java.lang.String notes,
                 remotes);
        return remoteSet;
    }

    private int searchToken(InputStream inputStream) throws IOException {
        int noHitsRemote = 0;
        int noHitsCommand = 0;
        while (true) {
            int b = inputStream.read();
            if (b == EOF)
                return 0;
            noHitsRemote = (b == 0xbb) ? noHitsRemote + 1 : 0;
            noHitsCommand = (b == 0xcc) ? noHitsCommand + 1 : 0;
            if (noHitsRemote == 4)
                return remoteToken;
            if (noHitsCommand == 4)
                return commandToken;
        }
    }

    private Remote parseRemote(InputStream inputStream) throws IOException {
        long status = inputStream.skip(12);
        if (status != 12)
            return null;//throw new IOException("to short skip");

        String vendor = getString(inputStream, 21);
        String kind = getString(inputStream, 21);
        String model = getString(inputStream, 21);
        String remoteName = vendor + "_" + kind + "_" + model;
        HashMap<String, Command> commands = new LinkedHashMap<>();
        while (true) {
            int token = searchToken(inputStream);
            if (token != commandToken)
                break;
            Command command = parseCommand(inputStream, remoteName);
            if (command != null)
                commands.put(command.getName(), command);
        }
        if (commands.isEmpty()) {
            System.err.println("Remote " + remoteName + " has no commands, ignored.");
            return null;
        }
        return new Remote(remoteName, vendor, model, kind, null, null, null, commands, null);
    }

    private Command parseCommand(InputStream inputStream, String remoteName) throws IOException {
        byte[] x = getBytes(inputStream, 23);
        int wav = byte2unsigned(x[9]) + 256 * byte2unsigned(x[10]);
        int frequency = wav != 0 ? 1000000000 / wav : 0;
        int noTimings = byte2unsigned(x[11]);
        int introLength = byte2unsigned(x[12]) + 256 * byte2unsigned(x[13]);
        int repeatLength = byte2unsigned(x[14]) + 256 * byte2unsigned(x[15]);

        String commandName = getString(inputStream, 21);
        if (noTimings == 0) {
            //out.println("error");
            //out.println("[" + freq + "]");
            //out.println();
            //throw new ErroneousDataException("In remote " + remoteName + ", command " + commandName + ", noTimings = 0");
            return null;
        }

        byte[][] timingData = new byte[noTimings][3];
        for (int i = 0; i < noTimings; i++) {
            int status = inputStream.read(timingData[i], 0, 3);
            if (status != 3)
                throw new IOException("too short read");
        }
        int timingsTable[] = new int[noTimings];
        for (int i = 0; i < noTimings; i++) {
            byte[] v = timingData[i];
            timingsTable[i] = (byte2unsigned(v[2]) + 1) * 32768 - byte2unsigned(v[0]) / 2 - 128 * byte2unsigned(v[1]);
        }

        int totalLength = introLength + repeatLength;
        int[] timingsMicroseconds = new int[totalLength];
        byte[] encodedDuration = getBytes(inputStream, totalLength);
        for (int i = 0; i < totalLength; i++) {
            int index = byte2unsigned(encodedDuration[i]) - 1;
            timingsMicroseconds[i] = timingsTable[index];
        }
        if (((introLength & 1) != 0) || ((repeatLength & 1) != 0)) {
            System.err.println(String.format("%s/%s: funny lengths (%d, %d), command ignored", remoteName, commandName, introLength, repeatLength));
            return null;
        }
        IrSignal irSignal = new IrSignal(timingsMicroseconds, introLength/2, repeatLength/2, frequency);
        Command command = new Command(commandName, null, irSignal);
        return command;
    }


    private byte[] getBytes(InputStream in, int length) throws IOException {
        byte buf[] = new byte[length];
        int status = in.read(buf, 0, length);
        if (status != length)
            throw new IOException("too short read");
        return buf;
    }

    private String getString(InputStream in, int length) throws IOException {
        byte buf[] = getBytes(in, length);
        String str = new String(buf, charactersetName); //throws UnsupportedEncodingException, subclass of IOException
        int n = str.indexOf(0);
        return n == -1 ? str.trim() : str.substring(0, n).trim();
    }

    // WHY on earth should this be necessary????????
    int byte2unsigned(byte x) {
        return x >= 0 ? x : x + 256;
    }

    @Override
    public boolean canImportDirectories() {
        return false;
    }

    @Override
    public String[][] getFileExtensions() {
        return new String[][]{
            new String[] {"CML files (*.cml)", "cml" },
            new String[] {"Zipped files (*.zip)", "zip" }
        };
    }

    @Override
    public String getFormatName() {
        return "CML";
    }

    public static void main(String[] args) {

        try {
            CmlImporter cmlImporter = new CmlImporter();
            cmlImporter.load(args[0]);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(CmlImporter.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(CmlImporter.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParseException | IrpMasterException ex) {
            Logger.getLogger(CmlImporter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
