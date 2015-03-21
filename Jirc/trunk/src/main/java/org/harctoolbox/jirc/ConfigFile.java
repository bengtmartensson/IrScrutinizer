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

package org.harctoolbox.jirc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import org.antlr.runtime.ANTLRFileStream;
import org.antlr.runtime.ANTLRReaderStream;
import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;

public class ConfigFile {
    /**
     * Character set assumed for input files.
     * Most LIRC files come from the time when ISO-8859-1 was the coolest thing on the planet.
     */
    public final static String lircCharactersetName = "ISO-8859-1";

    private LinkedHashMap<String, IrRemote> remotes = new LinkedHashMap<String, IrRemote>();

    private ConfigFile(File configFileName, int debug) throws IOException {
        this(new ANTLRFileStream(configFileName.getCanonicalPath(), lircCharactersetName), debug, configFileName.toString());
    }

    private ConfigFile(InputStream inputStream, int debug, String source) throws IOException {
        this(new ANTLRReaderStream(new InputStreamReader(inputStream, lircCharactersetName)), debug, source);
    }

    private ConfigFile(Reader reader, int debug, String source) throws IOException {
        this(new ANTLRReaderStream(reader), debug, source);
    }

    private ConfigFile(ANTLRStringStream inputStream, int debug, String source) throws IOException {
        ConfigFileLexer lex = new ConfigFileLexer(inputStream);
        CommonTokenStream tokens = new CommonTokenStream(lex);
        ConfigFileParser parser = new ConfigFileParser(tokens);
        ConfigFileParser.remotes_return r;
        try {
            r = parser.remotes();
            remotes = r.remotes;//parser.remotes();

            IrRemote last = null;
            for (IrRemote rem : remotes.values()) {
                rem.setSource(source);
                rem.next = null;
                if (last != null)
                    last.next = rem;
                last = rem;
            }
        } catch (RecognitionException ex) {
            System.err.println("Error [" + source + "]: Parse error occured, continuing");
        }
    }

    public static LinkedHashMap<String, IrRemote> readConfig(File filename, int debug) throws IOException {
        if (filename.isFile()) {
            ConfigFile config = new ConfigFile(filename, debug);
            return config.remotes;
        } else if (filename.isDirectory()) {
            File[] files = filename.listFiles();
            LinkedHashMap<String, IrRemote>dictionary = new LinkedHashMap<String, IrRemote>();
            for (File file : files) {
                // The program handles nonsensical files fine, however rejecting some
                // obviously irrelevant files saves time and log entries.
                if (file.getName().endsWith(".jpg") || file.getName().endsWith(".png")
                        || file.getName().endsWith(".gif") || file.getName().endsWith(".html")) {
                    System.err.println("Rejecting file " + file.getCanonicalPath());
                    continue;
                }
                LinkedHashMap<String, IrRemote> map = readConfig(file, debug);

                for (Entry<String, IrRemote> kvp : map.entrySet()) {
                    String remoteName = kvp.getKey();
                    int n = 1;
                    while (dictionary.containsKey(remoteName))
                        remoteName = kvp.getKey() + "$" + n++;

                    if (n > 1)
                        System.err.println("Warning: remote name " + kvp.getKey()
                                    + " (source: " + kvp.getValue().getSource()
                                    + ") already present, renaming to " + remoteName);
                    dictionary.put(remoteName, kvp.getValue());
                }
            }
            return dictionary;
        } else if (!filename.canRead())
            throw new FileNotFoundException(filename.getCanonicalPath());
        else
            return null;
    }

    public static LinkedHashMap<String, IrRemote> readConfig(Reader reader, int debug, String source) throws IOException {
        ConfigFile config = new ConfigFile(reader, debug, source);
        return config.remotes;
    }

    public static LinkedHashMap<String, IrRemote> readConfig(InputStream inputStream, int debug, String source) throws IOException {
        ConfigFile config = new ConfigFile(inputStream, debug, source);
        return config.remotes;
    }

    public static LinkedHashMap<String, IrRemote> readConfig(String input, int debug, String source) throws IOException {
        ConfigFile config = new ConfigFile(new ANTLRStringStream(input), debug, source);
        return config.remotes;
    }
}
