/*
Copyright (C) 2011-2013 Bengt Martensson.

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

package org.harctoolbox.IrpMaster;

import com.hifiremote.exchangeir.Analyzer;
import java.io.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Set;
import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.harctoolbox.IrpMaster.Iterate.InputVariableSetValues;
import org.harctoolbox.IrpMaster.Iterate.RandomValueSet;

/**
 * This class is a data bases manager for the data base of IRP protocols.
 * It reads a configuration file containing definitions for IR format in the IRP-Notation.
 *
 */

// Prefered order for protocol parameters:
// D, S, F, T, and then the rest in alphabetical order.

public class IrpMaster implements Serializable {
    private static final long serialVersionUID = 1L;

    private static class UnparsedProtocol implements Serializable {

        public static final String unnamed = "unnamed_protocol";
        private static final long serialVersionUID = 1L;
        public String name;
        public String documentation;
        public String irp;
        public String efcTranslation;
        public ArrayList<Short> ueiProtocol;

        UnparsedProtocol(String irp) {
            this.irp = irp;
            this.name = unnamed;
            documentation = null;
            efcTranslation = null;
            ueiProtocol = new ArrayList<>();
        }

        UnparsedProtocol() {
            this(null);
        }

        @Override
        public String toString() {
            return name + "\t" + irp;
        }
    }

    private UserComm userComm;
    private final static int max_recursion_depth_expanding = 5;
    private String configFileVersion = "not found";
    private static boolean testParse = false;
    //private static String defaultConfigFilename = "IrpProtocols.ini";
    private static String usageMessage = "Usage: one of\n"
            + "\tIrpMaster --help\n"
            + "\tIrpMaster [--decodeir] [--analyze] [-c|--config <configfilename>] --version\n"
            + "\tIrpMaster [OPTIONS] -n|--name <protocolname> [?]\n"
            + "\tIrpMaster [OPTIONS] --dump <dumpfilename> [-n|--name <protocolname>]\n"
            + "\tIrpMaster [OPTIONS] [--ccf] <CCF-SIGNAL>|<RAW-SEQUENCE>\n"
            + "\tIrpMaster [OPTIONS] [--ccf] \"<INTRO-SEQUENCE>\" [\"<REPEAT-SEQUENCE>\" [\"<ENDING-SQUENCE>\"]]\n"
            + "\tIrpMaster [OPTIONS] [-n|--name] <protocolname> [PARAMETERASSIGNMENT]\n"
            + "\tIrpMaster [OPTIONS] [-i|--irp] <IRP-Protocol> [PARAMETERASSIGNMENT]\n"
            + "\n"
            + "where OPTIONS=--stringtree <filename>,--dot <dotfilename>,--xmlprotocol <xmlprotocolfilename>,-c|--config <configfile>,-d|--debug <debugcode>|?,-s|--seed <seed>,"
            + "-q|--quiet,-P|--pass <intro|repeat|ending|all>,--interactive,--decodeir,--analyze,--lirc <lircfilename>,"
            + "-o|--outfile <outputfilename>, -x|--xml, -I|--ict, -r|--raw, -p|--pronto, -u|--uei, --disregard-repeat-mins, -#|--repetitions <number_repetitions>.\n\n"
            + "Any filename can be given as `-', meaning stdin or stdout.\n"
            + "PARAMETERASSIGNMENT is one or more expressions like `name=value' (without spaces!). "
            + "One value without name defaults to `F`, two values defaults to `D` and `F`, three values defaults to `D`, `S`, and `F`, four values to `D`, `S`, `F', and `T`, in the order given.\n\n"
            + "All integer values are nonnegative and can be given in base 10, 16 (prefix `0x'), 8 (leading 0), or 2 (prefix `0b' or `%'). "
            + "They must be less or equal to 2^63-1 = 9223372036854775807.\n\n"
            + "All parameter assignment, both with explicit name and without, can be given as intervals, like `0..255' or '0:255', causing the program to generate all signals within the interval. "
            + "Also * can be used for parameter intervals, in which case min and max are taken from the parameterspecs in the (extended) IRP notation. "
            + "The notation #<number> can also be used for parameter intervals, in which case <number> random values between min and max are generated.";

    // The key is the protocol name folded to lower case. Case preserved name is in UnparsedProtocol.name.
    private LinkedHashMap<String, UnparsedProtocol> protocols;

    private void dump(PrintStream ps, String name) {
        ps.println(protocols.get(name));
    }

    private void dump(PrintStream ps) {
        for (String s : protocols.keySet())
            dump(ps, s);
    }

    private void dump(String filename) throws FileNotFoundException {
        dump(IrpUtils.getPrintSteam(filename));
    }

    private void dump(String filename, String name) throws FileNotFoundException {
        dump(IrpUtils.getPrintSteam(filename), name);
    }

    //private String get(String name) {
    //    return protocols.get(name).toString();
    //}

    public boolean isKnown(String protocol) {
        return protocols.containsKey(protocol.toLowerCase(Locale.US));
    }

    public static boolean isKnown(String protocolsPath, String protocol) throws FileNotFoundException, IncompatibleArgumentException {
        return (new IrpMaster(protocolsPath)).isKnown(protocol);
    }

    public String getIrp(String name) {
        UnparsedProtocol prot = protocols.get(name);
        return prot == null ? null : prot.irp;
    }

    public Set<String> getNames() {
        return protocols.keySet();
    }

    public String getDocumentation(String name) {
        UnparsedProtocol prot = protocols.get(name);
        return prot == null ? null : prot.documentation;
    }

    public String getEfcTranslation(String name) {
        UnparsedProtocol prot = protocols.get(name);
        return prot == null ? null : prot.efcTranslation;
    }

    public ArrayList<Short> getUeiProtocol(String name) {
        UnparsedProtocol prot = protocols.get(name);
        return prot == null ? null : prot.ueiProtocol;
    }

    /**
     * Constructs a new Protocol with requested name, taken from the configuration
     * file/data base within the current IrpMaster.
     *
     * @param name protocol name in the configuration file/data base
     * @return newly parsed protocol
     * @throws UnassignedException
     * @throws ParseException
     * @throws org.harctoolbox.IrpMaster.UnknownProtocolException
     */

    public Protocol newProtocol(String name) throws UnassignedException, ParseException, UnknownProtocolException {
        UnparsedProtocol protocol = protocols.get(name.toLowerCase(IrpUtils.dumbLocale));
        if (protocol == null)
            throw new UnknownProtocolException(name);
        return new Protocol(protocol.name.toLowerCase(IrpUtils.dumbLocale), protocol.irp, protocol.documentation);
    }

    private void expand() throws IncompatibleArgumentException {
        for (String protocol : protocols.keySet()) {
            expand(0, protocol);
        }
    }

    private void expand(int depth, String name) throws IncompatibleArgumentException {
        UnparsedProtocol p = protocols.get(name);
        if (!p.irp.contains("{"))
            throw new IncompatibleArgumentException("IRP `" + p.irp + "' does not contain `{'.");

        if (!p.irp.startsWith("{")) {
            String p_name = p.irp.substring(0, p.irp.indexOf('{')).trim();
            UnparsedProtocol ancestor = protocols.get(p_name.toLowerCase(IrpUtils.dumbLocale));
            if (ancestor != null) {
                String replacement = ancestor.irp.lastIndexOf('[') == -1 ? ancestor.irp
                        : ancestor.irp.substring(0, ancestor.irp.lastIndexOf('['));
                Debug.debugConfigfile("Protocol " + name + ": `" + p_name + "' replaced by `" + replacement + "'.");
                p.irp = p.irp.replaceAll(p_name, replacement);
                protocols.put(name, p);
                if (depth < max_recursion_depth_expanding)
                    expand(depth + 1, name);
                else
                    System.err.println("Recursion depth in expanding " + name + " exceeded.");
            }
        }
    }

    private void addProtocol(UnparsedProtocol current) {
        // if no irp or name, ignore
        if (current == null || current.irp == null || current.name == null)
            return;

        if (current.documentation != null)
            current.documentation = current.documentation.trim();

        if (protocols.containsKey(current.name.toLowerCase(IrpUtils.dumbLocale)))
            userComm.warningMsg("Multiple definitions of protocol `" + current.name.toLowerCase(IrpUtils.dumbLocale) + "'. Keeping the last.");
        protocols.put(current.name.toLowerCase(IrpUtils.dumbLocale), current);

        if (testParse) {
            IrpLexer lex = new IrpLexer(new ANTLRStringStream(current.irp));
            CommonTokenStream tokens = new CommonTokenStream(lex);

            IrpParser parser = new IrpParser(tokens);
            try {
                parser.protocol();
            } catch (RecognitionException ex) {
                System.out.println(ex.getMessage());
            }
        }
    }

    private IrpMaster() {
        protocols = new LinkedHashMap<>();
        userComm = new UserComm();
    }

    /**
     * Like the other version, but reads from an InputStream instead.
     *
     * @param inputStream
     * @throws IncompatibleArgumentException
     */
   public IrpMaster(InputStream inputStream) throws IncompatibleArgumentException {
        this(new InputStreamReader(inputStream, IrpUtils.dumbCharset));
    }

    /**
     * Like the other version, but reads from a Reader instead.
     *
     * @param reader
     * @throws IncompatibleArgumentException
     */
    public IrpMaster(Reader reader) throws IncompatibleArgumentException {
        this();
        BufferedReader in = new BufferedReader(reader);
        UnparsedProtocol currentProtocol = null;
        int lineNo = 0;
        try {
            for (String lineRead = in.readLine(); lineRead != null; lineRead = in.readLine()) {
                lineNo++;
                String line = lineRead.trim();
                String[] kw = line.split("=", 2);
                String keyword = kw[0];
                String payload = kw.length > 1 ? kw[1].trim() : null;
                while (payload != null && payload.endsWith("\\")) {
                    payload = payload.substring(0, payload.length()-1)/* + "\n"*/;
                    payload += in.readLine();
                }
                if (line.startsWith("#")) {
                    // comment, ignore
                } else if (line.equals("[version]")) {
                    configFileVersion = in.readLine();
                } else if (line.equals("[protocol]")) {
                    addProtocol(currentProtocol);
                    currentProtocol = new UnparsedProtocol();
                } else if (currentProtocol != null && currentProtocol.documentation != null) {
                    // Everything is added to the documentation
                    currentProtocol.documentation += currentProtocol.documentation.isEmpty() ? line
                            : line.isEmpty() ? "\n\n"
                            : ((currentProtocol.documentation.endsWith("\n") ? "" : " ") + line);
                } else if (line.equals("[documentation]")) {
                    if (currentProtocol != null)
                        currentProtocol.documentation = "";
                } else if (keyword.equals("name")) {
                    if (currentProtocol != null)
                        currentProtocol.name = payload;
                } else if (keyword.equals("irp")) {
                    if (currentProtocol != null)
                        currentProtocol.irp = payload;
                } else if (keyword.equals("EFC_translation")) {
                    if (currentProtocol != null)
                        currentProtocol.efcTranslation = payload;
                } else if (keyword.equals("usable")) {
                    if (payload == null || !payload.equals("yes"))
                        currentProtocol = null;
                } else if (keyword.equals("UEI_protocol")) {
                    if (currentProtocol != null) {
                        String[] str = payload != null ? payload.split("[\\s,;or]+") : new String[0];
                        boolean hasComplained = false;
                        for (String s : str) {
                            try {
                                currentProtocol.ueiProtocol.add(Short.parseShort(s, 16));
                            } catch (NumberFormatException ex) {
                                if (!hasComplained) {
                                    Debug.debugConfigfile("Unparsable UEI protocol in line " + lineNo + ": " + line);
                                    hasComplained = true;
                                }
                            }
                        }
                    }
                } else if (keyword.length() > 1) {
                    //if (Debug.debugConfigfile())
                    //    System.out.println("Unknown keyword:" + keyword + " = " + payload);
                } else {
                    if (!line.isEmpty())
                        Debug.debugConfigfile("Ignored line: " + line);
                }
            }
            addProtocol(currentProtocol);
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
        }
        expand();
        Debug.debugConfigfile(protocols.size() + " protocols read.");
    }

    /**
     * Sets up a new IrpMaster from its first argument.
     *
     * @param datafile Configuration file for IRP protocols.
     * @throws FileNotFoundException
     * @throws IncompatibleArgumentException
     */
    public IrpMaster(String datafile) throws FileNotFoundException, IncompatibleArgumentException {
        this(IrpUtils.getInputSteam(datafile));
    }

    private static void usage(int returncode) {
        ((returncode == 0) ? System.out : System.err).println(usageMessage);
        System.exit(returncode);
    }

    /**
     * @param args See the usage message.
     */
    public static void main(String[] args) {
        InputVariableSetValues inputVariableSetValues = null;
        LinkedHashMap<String, String> usersParameters = new LinkedHashMap<>();
        String configFilename = null;
        String xmlProtocolFilename = null;
        String dotFilename = null;
        String stringtreeFilename = null;
        String dumpFilename = null;
        String irp = null;
        String protocolName = null;
        String outFileName = "-";
        String logFileName = null;
        String lircFileName = null;
        boolean quiet = false;
        boolean doICT = false;
        boolean doRaw = false;
        boolean doPronto = false;
        boolean doXML = false;
        boolean doCcf = false;
        boolean doUei = false;
        boolean didSomethingUseful = false;
        boolean invokeDecodeIR = false;
        boolean invokeAnalyzeIR = false;
        boolean generateProtocolInfo = false;
        int pass = (int) IrpUtils.all;
        int seed = (int) IrpUtils.invalid;
        int arg_i = 0;
        int no_repetitions = 1;
        boolean considerRepeatMins = true;
        boolean interactive = false;
        String irpString = null;
        IrpMaster irpMaster = null;

        if (args.length == 0)
            usage(IrpUtils.exitUsageError);

        try {
            // Parse options
            while (arg_i < args.length && !args[arg_i].isEmpty() && args[arg_i].charAt(0) == '-') {
                if (args[arg_i].equals("--help")) {
                    usage(IrpUtils.exitSuccess);
                }
                if (args[arg_i].equals("--version")) {

                    System.out.println(Version.versionString);
                    if (configFilename != null) {
                        try {
                            IrpMaster irpmaster = new IrpMaster(configFilename);
                            System.out.println("Configfile version: " + irpmaster.configFileVersion);
                        } catch (FileNotFoundException ex) {
                            System.out.println("Configfile not found");
                        }

                    }
                    if (invokeDecodeIR)
                        try {
                            System.out.println("DecodeIR version: " + DecodeIR.getVersion());
                        } catch (UnsatisfiedLinkError ex) {
                            System.err.println("DecodeIR not found in java.library.path.");
                        }
                    if (invokeAnalyzeIR)
                        System.out.println("Analyzer version: " + Analyzer.getVersion());
                    System.out.println("JVM: "+ System.getProperty("java.vendor") + " " + System.getProperty("java.version") + " " + System.getProperty("os.name") + "-" + System.getProperty("os.arch"));
                    System.out.println();
                    System.out.println(Version.licenseString);
                    System.exit(IrpUtils.exitSuccess);
                }

                // Keep this thing alphabetized after the (short) options
                if (args[arg_i].equals("-#") || args[arg_i].startsWith("--repetitions")) {
                    arg_i++;
                    no_repetitions = (int) IrpUtils.parseLong(args[arg_i++], false);
                } else if (args[arg_i].equalsIgnoreCase("--Analyze")) {
                    arg_i++;
                    invokeAnalyzeIR = true;
                } else if (args[arg_i].equals("-c") || args[arg_i].startsWith("--config")) {
                    arg_i++;
                    configFilename = args[arg_i++];
                } else if (args[arg_i].startsWith("--ccf")) {
                    arg_i++;
                    doCcf = true;
                } else if (args[arg_i].equals("-D") || args[arg_i].equalsIgnoreCase("--DecodeIR")) {
                    arg_i++;
                    invokeDecodeIR = true;
                } else if (args[arg_i].equals("-d") || args[arg_i].equals("--debug")) {
                    // Takes effect immediately
                    arg_i++;
                    if (args[arg_i].equals("?")) {
                        UserComm.print("Debug options: " + Debug.helpString(", "));
                        System.exit(IrpUtils.exitSuccess);
                    }
                    Debug.setDebug((int) IrpUtils.parseLong(args[arg_i++], false));
                } else if (args[arg_i].equals("--disregard-repeat-mins")) {
                    arg_i++;
                    considerRepeatMins = false;
                } else if (args[arg_i].equals("--dot")) {
                    arg_i++;
                    dotFilename = args[arg_i++];
                } else if (args[arg_i].equals("--dump")) {
                    arg_i++;
                    dumpFilename = args[arg_i++];
                } else if (args[arg_i].equals("-I") || args[arg_i].equals("--ict")) {
                    arg_i++;
                    doICT = true;
                } else if (args[arg_i].equals("-i") || args[arg_i].startsWith("--irp")) {
                    arg_i++;
                    irp = args[arg_i++];
                } else if (args[arg_i].startsWith("--interactive")) {
                    arg_i++;
                    interactive = true;
                } else if (args[arg_i].equals("-l") || args[arg_i].startsWith("--log")) {
                    arg_i++;
                    logFileName = args[arg_i++];
                } else if (args[arg_i].startsWith("--lirc")) {
                    arg_i++;
                    lircFileName = args[arg_i++];
                } else if (args[arg_i].equals("-n") || args[arg_i].startsWith("--name")) {
                    arg_i++;
                    protocolName = args[arg_i++].toLowerCase(IrpUtils.dumbLocale);
                } else if (args[arg_i].equals("-o") || args[arg_i].startsWith("--out")) {
                    arg_i++;
                    outFileName = args[arg_i++];
                } else if (args[arg_i].equals("-p") || args[arg_i].equals("--pronto")) {
                    arg_i++;
                    doPronto = true;
                } else if (args[arg_i].equals("-P") || args[arg_i].equals("--pass")) {
                    arg_i++;
                    String arg = args[arg_i++];
                    if (arg.matches("[0-9]+")) {
                        pass = Integer.parseInt(arg);
                        considerRepeatMins = true;
                    } else {
                        pass = Pass.valueOf(arg).toInt();
                        considerRepeatMins = false;
                    }
                } else if (args[arg_i].equals("-q") || args[arg_i].equals("--quiet")) {
                    arg_i++;
                    quiet = true;
                } else if (args[arg_i].equals("-r") || args[arg_i].equals("--raw")) {
                    arg_i++;
                    doRaw = true;
                } else if (args[arg_i].equals("-s") || args[arg_i].equals("--seed")) {
                    arg_i++;
                    seed = Integer.parseInt(args[arg_i++]);
                } else if (args[arg_i].startsWith("--string")) {
                    arg_i++;
                    stringtreeFilename = args[arg_i++];
                } else if (args[arg_i].equals("-u") || args[arg_i].equals("--uei")) {
                    arg_i++;
                    doUei = true;
                } else if (args[arg_i].equals("-x") || args[arg_i].equals("--xml")) {
                    arg_i++;
                    doXML = true;
                } else if (args[arg_i].startsWith("--xmlp")) {
                    arg_i++;
                    xmlProtocolFilename = args[arg_i++];
                } else {
                    usage(IrpUtils.exitUsageError);
                }
            }

            UserComm.setQuiet(quiet);

            if (doCcf) {
                boolean prontoPlausible = true;
                boolean ueiPlausible = true;
                boolean rawPlausible = false;
                try {
                    IrSignal irSignal = null;
                    if (args.length == arg_i + 1)
                        // Either one sequence, or a CCF
                        irSignal = args[arg_i].trim().startsWith("+")
                                ? new IrSignal(IrpUtils.defaultFrequency, IrpUtils.invalid, new IrSequence(args[arg_i]), null, null)
                                : Pronto.ccfSignal(args[arg_i]);
                    else if (args.length == arg_i + 2)
                        // intro, repeat
                       irSignal = new IrSignal(IrpUtils.defaultFrequency, IrpUtils.invalid, new IrSequence(args[arg_i]), new IrSequence(args[arg_i + 1]), null);
                    else if (args.length == arg_i + 3)
                        // intro, repeat, ending
                       irSignal = new IrSignal(IrpUtils.defaultFrequency, IrpUtils.invalid, new IrSequence(args[arg_i]), new IrSequence(args[arg_i + 1]), new IrSequence(args[arg_i + 2]));
                    else {
                        int[] ccf = new int[args.length - arg_i];
                        rawPlausible = args[arg_i].startsWith("+");
                        if (rawPlausible) {
                            prontoPlausible = false;
                            ueiPlausible = false;
                            for (int i = 0; i < args.length - arg_i; i++) {
                                ccf[i] = Integer.parseInt(args[i + arg_i].replaceFirst("\\+", ""));
                            }
                        } else {
                            for (int i = 0; i < args.length - arg_i; i++) {
                                prontoPlausible = prontoPlausible && args[i + arg_i].length() == Pronto.charsInDigit;
                                ueiPlausible = ueiPlausible && args[i + arg_i].length() == ExchangeIR.ueiLearnedCharsInDigit;
                                ccf[i] = Integer.parseInt(args[i + arg_i], 16);
                            }
                        }
                        if (!prontoPlausible && !ueiPlausible && !rawPlausible) {
                            UserComm.error("Signal neither raw, CCF, nor UEI learned.");
                            System.exit(IrpUtils.exitSemanticUsageError);
                        }
                        irSignal = prontoPlausible ? Pronto.ccfSignal(ccf)
                                : rawPlausible ? ExchangeIR.interpretIrSequence(ccf, true)
                                : ExchangeIR.parseUeiLearned(ccf);
                    }
                    if (doRaw)
                        System.out.println(irSignal);
                    if (doPronto)
                        System.out.println(irSignal.ccfString());
                    if (doUei)
                        System.out.println(ExchangeIR.newUeiLearned(irSignal));
                    if (invokeDecodeIR)
                        DecodeIR.invoke(irSignal);
                    if (invokeAnalyzeIR)
                        System.out.println("AnalyzeIR: " + ExchangeIR.newAnalyzer(irSignal).getIrpWithAltLeadout());

                } catch (NumberFormatException | IrpMasterException ex) {
                    System.err.println(ex);
                }
                System.exit(IrpUtils.exitSuccess);
            }

            // Accept protocol name even without the "-n" or "--name", unless -i has been given
            if (irp == null && protocolName == null && !doCcf && arg_i < args.length && !args[arg_i].isEmpty() && !args[arg_i].contains("="))
                protocolName = args[arg_i++];

            // Force protocolName lowercase since it is stored that way
            if (protocolName != null)
                protocolName = protocolName.toLowerCase(IrpUtils.dumbLocale);

            // Parse name = value assignments
            while (arg_i < args.length && !args[arg_i].isEmpty() && args[arg_i].contains("=")) {
                String[] kv = args[arg_i++].split("=");
                if (kv.length != 2)
                    usage(IrpUtils.exitUsageError);
                usersParameters.put(kv[0], kv[1]);
            }

            // Arguments left are shorthand assignments, or "?"
            switch (args.length - arg_i) {
                case 0:
                    if (usersParameters.isEmpty() && protocolName != null)
                        generateProtocolInfo = true;
                    break;
                case 1:
                    if (args[arg_i].equals("?"))
                        generateProtocolInfo = true;
                    else
                        usersParameters.put("F", args[arg_i]);
                    break;
                case 2:
                    usersParameters.put("D", args[arg_i]);
                    usersParameters.put("F", args[arg_i + 1]);
                    break;
                case 3:
                    usersParameters.put("D", args[arg_i]);
                    usersParameters.put("S", args[arg_i + 1]);
                    usersParameters.put("F", args[arg_i + 2]);
                    break;
                case 4:
                    usersParameters.put("D", args[arg_i]);
                    usersParameters.put("S", args[arg_i + 1]);
                    usersParameters.put("F", args[arg_i + 2]);
                    usersParameters.put("T", args[arg_i + 3]);
                    break;
                default:
                    usage(IrpUtils.exitUsageError);
                    break;
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            Debug.debugMain("ArrayIndexOutOfBoundsException: " + e.getMessage());
            usage(IrpUtils.exitUsageError);
        } catch (NumberFormatException e) {
            Debug.debugMain("NumberFormatException: " + e.getMessage());
            usage(IrpUtils.exitUsageError);
        } catch (IncompatibleArgumentException e) {
            UserComm.error(e.getMessage());
            System.exit(IrpUtils.exitInternalFailure);
        } catch (IllegalArgumentException e) {
            System.err.println("IllegalArgumentException: " + e.getMessage());
            System.exit(IrpUtils.exitFatalProgramFailure);
        }

        // Since the com.hifiremote.LibraryLoader.loadLibrary produces some
        // annoying messages on stderr, load it here.
        if (invokeDecodeIR) {
            try {
                DecodeIR.loadLibrary();
            } catch (UnsatisfiedLinkError ex) {
                System.err.println("DecodeIR not found.");
            }
        }

        RandomValueSet.initRng(seed);

        try {
            if (irp != null) {
                if (configFilename != null)
                    UserComm.warning("Specifying both irp and configuration file is not sensible. Ignoring configuration file.");
                if (protocolName != null)
                    UserComm.warning("Specifying both irp and protocol name is not sensible. Ignoring protocol name.");
                protocolName = UnparsedProtocol.unnamed;
                UnparsedProtocol p = new IrpMaster.UnparsedProtocol(irp);
                irpMaster = new IrpMaster();
                irpMaster.addProtocol(p);
            } else {
                if (configFilename == null) {
                    System.err.println("Either -i or -c option must be used.");
                    usage(IrpUtils.exitUsageError);
                }
                irpMaster = new IrpMaster(configFilename); // may throw Exceptions, in which case IprMaster == null.
            }
            if (dumpFilename != null) {
                if (protocolName != null) {
                    irpMaster.dump(dumpFilename, protocolName);
                    System.err.println("Protocol " + irpMaster.protocols.get(protocolName).name + " dumped to " + dumpFilename + ".");
                } else {
                    irpMaster.dump(dumpFilename);
                    System.err.println("All " + irpMaster.protocols.entrySet().size() + " protocols in " + configFilename + " dumped to " + dumpFilename + ".");
                }
                didSomethingUseful = true;
            }

            // Up until now, no protocol made sense. But not now any longer.
            if (protocolName == null) {
                System.err.println("No protocol name given, nothing to do. Exiting.");
                if (didSomethingUseful)
                    System.exit(IrpUtils.exitSuccess);
                else
                    usage(IrpUtils.exitUsageError);
            }

            /*String*/ irpString = irpMaster.getIrp(protocolName);
            if (irpString == null) {
                System.err.println("No protocol named `" + protocolName + "' found in " + configFilename + " (case insensitive), exiting.");
                System.exit(IrpUtils.exitSemanticUsageError);
            }

            Debug.debugIrpParser("IRP = " + irpString);

            if (generateProtocolInfo) {
                System.out.println(irpString);
                System.exit(IrpUtils.exitSuccess);
            }

            //Protocol protocol = new Protocol(irpMaster.protocols.get(protocolName).name, irpString, irpMaster.getDocumentation(protocolName));
            Protocol protocol = irpMaster.newProtocol(protocolName);
            if (dotFilename != null)
                IrpUtils.getPrintSteam(dotFilename).println(protocol.toDOT());

            if (xmlProtocolFilename != null)
                (new XmlExport(protocol.toDOM())).printDOM(IrpUtils.getPrintSteam(xmlProtocolFilename), null);

            if (stringtreeFilename != null)
                IrpUtils.getPrintSteam(stringtreeFilename).println(protocol.toString());

            // Up until now, no parameters made sense. But not any longer.
            if (usersParameters.isEmpty()) {
                System.err.println("No parameters given, nothing to do. Exiting.");
                System.exit(IrpUtils.exitUsageError);
            }

            if (doXML)
                protocol.setupDOM();

            try {
                inputVariableSetValues = new InputVariableSetValues(usersParameters, true, protocol);
            } catch (NumberFormatException ex) {
                Debug.debugMain("Unparsable number: " + ex.getMessage());
                usage(IrpUtils.exitUsageError);
            }
            Debug.debugMain(inputVariableSetValues != null ? inputVariableSetValues.toString() : "");

            PrintStream printStream = IrpUtils.getPrintSteam(outFileName);
            PrintStream logFile = IrpUtils.getPrintSteam(logFileName);
            UserComm.setLogging(logFile);

            LircExport lircExport = lircFileName != null
                    ? new LircExport(protocolName, "Generated by IrpMaster", protocol.getFrequency())
                    : null;

            if (interactive) {
                LinkedHashMap actualParameters = inputVariableSetValues != null ? inputVariableSetValues.iterator().next() : null;
                //UserComm.print(actualParameters.toString());
                protocol.interactiveRender(irpMaster.userComm, actualParameters);
                UserComm.print("Bye!");
                System.exit(IrpUtils.exitSuccess);
            }

            for (LinkedHashMap<String, Long> actualParameters : inputVariableSetValues) {
                // Iterate no_repetitions (for testing toggle updates etc.)
                for (int iterate = 0; iterate < no_repetitions; iterate++) {
                    IrSequence irSequence = null;
                    IrSignal irSignal = null;

                    if (pass != IrpUtils.all) {
                        irSequence = protocol.render(actualParameters, pass, considerRepeatMins, true);
                        System.out.println(irSequence == null ? "null" : irSequence.toString());
                        System.out.println(irSequence == null ? "null" : irSequence.toPrintString());
                    } else {
                        irSignal = protocol.renderIrSignal(actualParameters, pass, considerRepeatMins);
                    }

                    if (doXML)
                        protocol.addSignal(actualParameters);

                    if (lircExport != null)
                        lircExport.addSignal(actualParameters, irSignal);

                    if (irSignal != null) {
                        Debug.debugMain(irSignal.toString());
                        Debug.debugIrSignals("Total signal duration (us): " + Math.round(irSignal.getDuration()));
                    }

                    boolean writtenHeader = false;
                    if (doRaw && irSignal != null) {
                        if (doXML) {
                            protocol.addRawSignalRepresentation(irSignal);
                        } else {
                            printStream.println(IrpUtils.variableHeader(actualParameters));
                            writtenHeader = true;
                            printStream.println(irSignal.toPrintString());
                        }
                    }
                    if (doPronto && irSignal != null) {
                        if (doXML) {
                            protocol.addXmlNode("pronto", irSignal.ccfString());
                        } else {
                            if (!writtenHeader)
                                printStream.println(IrpUtils.variableHeader(actualParameters));
                            writtenHeader = true;
                            printStream.println(irSignal.ccfString());
                        }
                    }
                    if (doUei && irSignal != null) {
                        if (doXML) {
                            protocol.addXmlNode("uei-learned", ExchangeIR.newUeiLearned(irSignal).toString());
                        } else {
                            if (!writtenHeader)
                                printStream.println(IrpUtils.variableHeader(actualParameters));
                            writtenHeader = true;
                            printStream.println(ExchangeIR.newUeiLearned(irSignal).toString());
                        }
                    }
                    if (doICT && !doXML && irSignal != null)
                        printStream.println(ICT.ictString(irSignal.toModulatedIrSequence(true, 1, true)));

                    if (invokeDecodeIR) {
                        // If there is a log file, don't babble.
                        if (logFile == null)
                            System.out.print("DecodeIR result: ");

                        boolean verified = DecodeIR.invoke(irSignal, protocolName, protocol, actualParameters, logFile == null, System.out);
                        if (logFile != null)
                            logFile.println(irpMaster.protocols.get(protocolName).name + ": " + IrpUtils.variableHeader(actualParameters)
                                + ": " + (verified ? "passed" : "failed"));
                    }
                    if (invokeAnalyzeIR) {
                        Analyzer analyzer = ExchangeIR.newAnalyzer(irSignal);
                        System.out.println("AnalyzeIR: " + analyzer.getIrpWithAltLeadout());
                    }
                } // for (... repetitions ...)
            } // for (LinkedHashMap actualParameters ...)
            if (doXML)
                protocol.printDOM(printStream);
            if (lircExport != null)
                lircExport.write(new File(lircFileName));
            printStream.close();
            if (logFile != null)
                logFile.close();
        } catch (IrpParseException ex) {
            if (irpMaster != null) {
                irpMaster.userComm.errorMsg("IRP parse error: ");
                irpMaster.userComm.printMsg(irpString);
                irpMaster.userComm.printMsg(IrpUtils.spaces(ex.charPositionInLine) + "^");
            } else {
                UserComm.error("IRP parse error: ");
                UserComm.print(irpString);
                UserComm.print(IrpUtils.spaces(ex.charPositionInLine) + "^");
            }
        } catch (FileNotFoundException | IrpMasterException ex) {
            if (irpMaster != null)
                irpMaster.userComm.exceptionMsg(ex);
            else
                UserComm.exception(ex);
        }
    }

    // A number of static access functions, in particular to be called from C or such.
    // Name "Makehex" is misleading -- it has nothing to do with the makehex program,
    // just the API is makehex-like.

    /**
     * Static version of getIrp.
     *
     * @param configFilename
     * @param protocolName
     * @return String with IRP representation
     */
    public static String getIrp(String configFilename, String protocolName) {
        IrpMaster irpMaster = null;
        try {
            irpMaster = new IrpMaster(configFilename);
        } catch (FileNotFoundException | IncompatibleArgumentException ex) {
        }
        return irpMaster == null ? null : irpMaster.getIrp(protocolName);
    }

    /**
     * This function closely mimics the API of the MakeHex dll entry function.
     * It returns its payload contained in the file given as first argument.
     *
     * @param outFile Path to the file to which the Pronto form is to be exported.
     * @param append True iff appending to Outfile, otherwise it is truncated.
     * @param configFileName Pathname to configuration file, typically IrpProtocols.ini.
     * @param preamble If non-null, a header string that is prefixed (followed by newline) to the Pronto export.  If null, no header is prefixed.
     * @param protocolName Name of protocol
     * @param device The device and subdevice as a string, with a dot separating the two.
     * @param OBC The OBC value as a string.
     * @return return code: 1:  Success, 0:  Unable to open output file, -1: Unable to open configuration file, -2: Error in IRP processing, -3: Other error
     */

    public static int makeHex(String outFile, boolean append, String configFileName,
            String preamble, String protocolName, String device, String OBC) {
        String irp = getIrp(configFileName, protocolName);

        return irp == null ? -1 : makeHexIRP(outFile, append, irp, preamble, protocolName, device, OBC);
    }

    /**
     * Returns the Pronto form of the IR signal.
     *
     * @param configFileName
     * @param protocolName
     * @param device
     * @param subdevice
     * @param obc
     * @return String in Pronto format representing the IR signal.
     */
    public static String makeHex(String configFileName, String protocolName, int device, int subdevice, int obc) {
        IrpMaster irpMaster;
        try {
            irpMaster = new IrpMaster(configFileName);
        } catch (FileNotFoundException | IncompatibleArgumentException ex) {
            return null;
        }

        return makeHexIRP(irpMaster.getIrp(protocolName), device, subdevice, obc);
    }

    /**
     *
     * @param outFile
     * @param append
     * @param irp
     * @param preamble
     * @param protocolName
     * @param device
     * @param OBC
     * @return return code: 1:  Success, 0:  Unable to open output file, -1: Unable to open configuration file, -2: Error in IRP processing, -3: Other error
     */
    public static int makeHexIRP(String outFile, boolean append, String irp,
            String preamble, String protocolName, String device, String OBC) {
        if (irp == null || irp.isEmpty())
            return -2;
        String[] param = device.split("\\.");
        int dev = Integer.parseInt(param[0]);
        int subdev = param.length > 1 ? Integer.parseInt(param[1]) : (int) IrpUtils.invalid;
        int obc = Integer.parseInt(OBC);

        return makeHexIRP(outFile, append, irp, preamble, protocolName, dev, subdev, obc);
    }

    /**
     *
     * @param outFile
     * @param append
     * @param irp
     * @param preamble
     * @param protocolName
     * @param device
     * @param subdevice
     * @param obc
     * @return return code: 1:  Success, 0:  Unable to open output file, -1: Unable to open configuration file, -2: Error in IRP processing, -3: Other error
     */
    public static int makeHexIRP(String outFile, boolean append, String irp,
            String preamble, String protocolName, int device, int subdevice, int obc) {
        PrintStream out = null;
        int status = 1;
        try {
            String ccf = makeHexIRPPriv(irp, device, subdevice, obc);

            out = IrpUtils.getPrintSteam(append ? ("+" + outFile) : outFile);
            if (preamble != null && !preamble.isEmpty())
                out.println(preamble);

            out.println(ccf);
        } catch (IncompatibleArgumentException | InvalidRepeatException | DomainViolationException ex) {
            status = -3;
        } catch (FileNotFoundException ex) {
           status = 0;
        } catch (UnassignedException | ParseException ex) {
            status = -2;
        } finally {
            if (out != null)
                out.close();
        }
        return status;
    }

    /**
     *
     * @param irp
     * @param device
     * @param subdevice
     * @param obc
     * @return return code: 1:  Success, 0:  Unable to open output file, -1: Unable to open configuration file, -2: Error in IRP processing, -3: Other error
     */
    public static String makeHexIRP(String irp, int device, int subdevice, int obc) {
        try {
            return makeHexIRPPriv(irp, device, subdevice, obc);
        } catch (ParseException | DomainViolationException | IncompatibleArgumentException | InvalidRepeatException | UnassignedException ex) {
        }
        return null;
    }

    private static String makeHexIRPPriv(String irp, int device, int subdevice, int obc) throws UnassignedException, DomainViolationException, IncompatibleArgumentException, InvalidRepeatException, ParseException {
        Protocol protocol = new Protocol(null, irp, null);
        IrSignal irSignal = protocol.renderIrSignal(device, subdevice, obc);
        return irSignal.ccfString();
    }
}

