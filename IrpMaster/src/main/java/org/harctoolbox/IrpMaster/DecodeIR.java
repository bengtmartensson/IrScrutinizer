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

package org.harctoolbox.IrpMaster;

import com.hifiremote.decodeir.DecodeIRCaller;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

// Note: Arrays represent microsecond data, unless the functions acts on Pronto data.

public class DecodeIR {
    // set debug = 2048 for Debug.debugDecodeIR

    public final static String appName = "DecodeIR";

    private static final String libraryName = "DecodeIR";
    private static final String libraryPathProperty = "harctoolbox.decodeir.library.path";
    private static boolean libIsLoaded = false;
    private static String version = null;

    public static DecodeIR newDecodeIR(int[] data, int lengthRepeat, int lengthEnding, int frequency) {
        DecodeIR decoder = new DecodeIR(data, lengthRepeat, lengthEnding, frequency);
        return decoder.valid ? decoder : null;
    }

    public static DecodeIR newDecodeIR(IrSignal irSignal) {
        DecodeIR decoder = new DecodeIR(irSignal);
        return decoder.valid ? decoder : null;
    }

    public static DecodeIR newDecodeIR(int[] CCF) throws IrpMasterException {
        DecodeIR decoder = new DecodeIR(CCF);
        return decoder.valid ? decoder : null;
    }

    public static DecodeIR create(IrSequence irSequence, double frequency) {
        ModulatedIrSequence modulatedIrSequence = new ModulatedIrSequence(irSequence, frequency, IrpUtils.unknownDutyCycle);
        DecodeIR decoder = new DecodeIR(modulatedIrSequence);
        return decoder.valid ? decoder : null;
    }

    public static DecodeIR newDecodeIR(ModulatedIrSequence irSequence) {
        DecodeIR decoder = new DecodeIR(irSequence);
        return decoder.valid ? decoder : null;
    }

    public static DecodeIR newDecodeIR(String ccf) throws ParseException, IncompatibleArgumentException, UnassignedException, DomainViolationException, InvalidRepeatException {
        DecodeIR decoder = new DecodeIR(ccf);
        return decoder.valid ? decoder : null;
    }

    /**
     * Call without argument to load the shared library either from a architecture dependent
     * subdirectory or from the system's java.library.path.
     * If the system property harctoolbox.decodeir.library.path is set, use that instead.
     *
     * @return Success of loading.
     */
    public static boolean loadLibrary() {
        if (libIsLoaded)
            return true;

        String userSpecifiedPath = System.getProperty(libraryPathProperty);
        boolean success = (userSpecifiedPath == null)
                ? (localLoadLibrary() || systemLoadLibrary())
                : loadAbsoluteLibrary(userSpecifiedPath);
        if (success)
            version = (new DecodeIRCaller()).getVersion();

        return success;
    }

    private static boolean loadAbsoluteLibrary(String path) {
        String absolutePath = (new File(path)).getAbsolutePath();
        try {
            System.load(absolutePath);
            Debug.debugDecodeIR("Loaded file " + absolutePath);
            libIsLoaded = true;
            return true;
        } catch (UnsatisfiedLinkError e) {
            Debug.debugDecodeIR("Loading of file " + absolutePath + " failed");
            return false;
        }
    }

    // First try the com.hifiremote.LoadLibrary way, i.e. with local,
    // architecture dependent subdirectories...
    private static boolean localLoadLibrary() {
        String folderName = (System.getProperty("os.name").startsWith("Windows")
                                ? "Windows"
                                : System.getProperty("os.name")
                            ) + '-' + System.getProperty("os.arch").toLowerCase(Locale.US);
        // supported values: Linux-{i386,amd64}, Mac OS X-{i386,x86_64}, Windows-{x86,amd64}
        // Mac: it appears that Snow Leopard says "X64_64" while Mountain Lion says "x86_64".

        String mappedName = System.mapLibraryName(libraryName);
        String libraryFile = folderName  + File.separator + mappedName;
        if (System.getProperty("harctoolbox.jniLibsHome") != null)
            libraryFile = System.getProperty("harctoolbox.jniLibsHome") + File.separator + libraryFile;
        if (loadAbsoluteLibrary(libraryFile))
            return true;

        // Mac OS X changed the extension of JNI libs recently, give it another try in this case
        if (System.getProperty("os.name").startsWith("Mac")) {
            mappedName = System.mapLibraryName(libraryName).replaceFirst("\\.dylib", ".jnilib");
            libraryFile = folderName + File.separator + mappedName;
            return loadAbsoluteLibrary(libraryFile);
        } else {
            return false;
        }
    }

    // ... then try the system path
    private static boolean systemLoadLibrary() {
        try {
            System.loadLibrary(libraryName);
            libIsLoaded = true;
            Debug.debugDecodeIR("Loading of " + libraryName + " from system path succeeded.");
            return true;
        } catch (UnsatisfiedLinkError e) {
            Debug.debugDecodeIR("Loading of " + libraryName + " from system path failed.");
            return false;
        }
    }


    /**
     * Returns the version.
     *
     * @return Version string, or null if instance not valid.
     */
    public static String getVersion() {
        loadLibrary();
        return version;
    }

    // Just dispose of signs, better yet would be to check.
    private static int[] parseRawString(String s) throws NumberFormatException {
	String[] work = s.split("\\s+");
	int[] result = new int[work.length];
	for (int i = 0; i < result.length; i++)
            result[i] = Math.abs(Integer.parseInt(work[i].replaceFirst("\\+", "")));

	return result;
    }

    /**
     * Static version of the constructor with the same arguments. Constructs a DecodeIR object, and applies the getDecodedSignals-function.
     *
     * @param data
     * @param lengthRepeat
     * @param lengthEnding
     * @param frequency
     * @return array of decodes
     */
    public static DecodedSignal[] decode(int[] data, int lengthRepeat, int lengthEnding, int frequency) {
	DecodeIR decoder = DecodeIR.newDecodeIR(data, lengthRepeat, lengthEnding, frequency);
        return decoder != null ? decoder.getDecodedSignals() : null;
    }

    /**
     * Static version of the constructor with the same arguments. Constructs a DecodeIR object, and applies the getDecodedSignals-function.
     * @param irSignal
     * @return array of decodes; null if instance not valid.
     */
    public static DecodedSignal[] decode(IrSignal irSignal) {
        DecodeIR decoder = DecodeIR.newDecodeIR(irSignal);
        return decoder != null ? decoder.getDecodedSignals() : null;
    }

    /**
     * Static version of the constructor with the same arguments. Constructs a DecodeIR object, and applies the getDecodedSignals-function.
     * @param ccf
     * @return array of decodes
     * @throws ParseException
     * @throws IncompatibleArgumentException
     * @throws UnassignedException
     * @throws DomainViolationException
     * @throws InvalidRepeatException
     */
    public static DecodedSignal[] decode(String ccf) throws IncompatibleArgumentException, ParseException, UnassignedException, DomainViolationException, InvalidRepeatException {
        DecodeIR decoder = DecodeIR.newDecodeIR(ccf);
        return decoder != null ? decoder.getDecodedSignals() : null;
    }

    /**
     * Static version of the constructor with the same arguments. Constructs a DecodeIR object, and applies the getDecodedSignals-function.
     *
     * @param CCF
     * @return array of decodes
     * @throws IrpMasterException
     */
    public static DecodedSignal[] decode(int[] CCF) throws IrpMasterException {
        DecodeIR decoder = DecodeIR.newDecodeIR(CCF);
        return decoder != null ? decoder.getDecodedSignals() : null;
    }

    /**
     * Static version of the constructor with the same arguments.
     * @param irSequence
     * @param frequency
     * @return array of decodes
     */
    public static DecodedSignal[] decode(IrSequence irSequence, double frequency) {
        DecodeIR decoder = DecodeIR.create(irSequence, frequency);
        return decoder != null ? decoder.getDecodedSignals() : null;
    }

    /**
     * Static version of the constructor with the same arguments.
     * @param irSequence
     * @return array of decodes
     */
    public static DecodedSignal[] decode(ModulatedIrSequence irSequence) {
        DecodeIR decoder = DecodeIR.newDecodeIR(irSequence);
        return decoder != null ? decoder.getDecodedSignals() : null;
    }

    public static void invoke(ModulatedIrSequence seq) {
        invoke(InterpretString.interpretIrSequence(seq, true, true));
    }

    /**
     * Invokes a DecodeIR object on the argument, and print the decodes to stdout.
     * @param irSignal
     */
    public static void invoke(IrSignal irSignal) {
        invoke(irSignal, System.out);
    }

    /**
     * Invokes a DecodeIR object on the argument, and print the decodes to the PrintStream in the second argument.
     * @param irSignal
     * @param out
     */
    public static void invoke(IrSignal irSignal, PrintStream out) {
        invoke(irSignal, null, null, null, false, out);
    }

    /**
     * Invokes a DecodeIR object on the first argument, checks if compatible with the data in the rest of the arguments.
     *
     * @param irSignal
     * @param protocolName
     * @param protocol
     * @param actualParameters
     * @param verbose
     * @param out
     * @return result of the comparison.
     */
    public static boolean invoke(IrSignal irSignal, String protocolName, Protocol protocol, Map<String, Long> actualParameters, boolean verbose, PrintStream out) {
        if (irSignal == null)
            return false;

        boolean verified = false;

        try {
            DecodedSignal[] decodes = decode(irSignal.toIntArray(), irSignal.getRepeatLength(), irSignal.getEndingLength(), (int) irSignal.getFrequency());
            if (decodes == null)
                ;
            else if (decodes.length == 0) {
                if (verbose)
                    out.println("-");
            } else {
                boolean hasReportedSuccess = false;
                for (DecodedSignal decode : decodes) {
                    boolean isOk = decode.isOk(protocolName, protocol, actualParameters);
                    if (isOk) {
                        verified = true;
                        Debug.debugDecodeIR(decode + ": success");
                        // If there is a log file, don't babble.
                        if (verbose && ! hasReportedSuccess) {
                            out.println("success");
                            hasReportedSuccess = true;
                        }
                    } else
                        out.println(decode);
                }
            }
        } catch (UnsatisfiedLinkError ex) {
            UserComm.error("DecodeIR not found in java.library.path");
        }

        return verified;
    }

    private static void usage(int exitcode) {
	System.err.println("Usage:");
	System.err.println("DecodeIR [OPTIONS] -v | --version | --help |  CCF-code | raw-code| - | <filename>");
        System.err.println("where OPTIONS = -d|--debug <n>, -i|--intro <n>, -r|--repetition <n>, -e|--ending <n>, -s|--skip <n>, -f|--frequency <frequency in Hz>");
	System.exit(exitcode);
    }

    /**
     * Allows for calling DecodeIR from the command line
     * @param args -- IR signal in Pronto format to be decoded.
     */
    public static void main(String[] args) {
        int debug = 0;
        int frequency = 0;
        int introLength = (int) IrpUtils.invalid;
        int repetitionLength = (int) IrpUtils.invalid;
        int endingLength = 0;
        try {
            int skip = 0;
            int arg_i = 0;

            BufferedReader reader = null;
            String ccfString = null;
            String rawString = null;

            while (arg_i < args.length && args[arg_i].length() > 1 && args[arg_i].charAt(0) == '-') {
                switch (args[arg_i]) {
                    case "--help":
                        usage(IrpUtils.exitSuccess);
                        break;
                    case "-v":
                    case "--version":
                        System.out.println("DecodeIR version " + DecodeIR.getVersion());
                        System.exit(IrpUtils.exitSuccess);
                    case "-s":
                    case "--skip":
                        arg_i++;
                        skip = Integer.parseInt(args[arg_i++]);
                        break;
                    case "-d":
                    case "--debug":
                        arg_i++;
                        debug = Integer.parseInt(args[arg_i++]);
                        Debug.setDebug(debug);
                        break;
                    case "-f":
                    case "--frequency":
                        arg_i++;
                        frequency = Integer.parseInt(args[arg_i++]);
                        break;
                    case "-i":
                    case "--intro":
                        arg_i++;
                        introLength = Integer.parseInt(args[arg_i++]);
                        break;
                    case "-r":
                    case "--repetition":
                        arg_i++;
                        repetitionLength = Integer.parseInt(args[arg_i++]);
                        break;
                    case "-e":
                    case "--ending":
                        arg_i++;
                        endingLength = Integer.parseInt(args[arg_i++]);
                        break;
                    default:
                        usage(IrpUtils.exitUsageError);
                }
            }

            if (args[arg_i].equals("-"))
                reader = new BufferedReader(new InputStreamReader(System.in, IrpUtils.dumbCharset));
            else if (args.length - arg_i == 1) {
                if (args[arg_i].startsWith("0000") || args[arg_i].startsWith("0100"))
                    ccfString = args[arg_i];
                else if (args[arg_i].startsWith("+"))
                    rawString = args[arg_i];
                else
                    reader = new BufferedReader(new InputStreamReader(new FileInputStream(args[arg_i]), IrpUtils.dumbCharset));
            }

            if (reader != null) {
                for (int i = 0; i < skip; i++)
                    reader.readLine();

                String line = reader.readLine();
                if (line != null && (line.startsWith("0000") || line.startsWith("0100")))
                    ccfString = line;
                else
                    rawString = line;
            }

            DecodedSignal[] result;
            if (ccfString != null) {
                result = decode(ccfString);
            } else if (rawString != null) {
                int[] data = parseRawString(rawString);
                if (introLength != IrpUtils.invalid && repetitionLength == IrpUtils.invalid)
                    repetitionLength = data.length/2 - introLength - endingLength;
                result = decode(data, repetitionLength, endingLength, frequency);
            } else {
                int[] data = new int[args.length - arg_i];
                if (introLength != IrpUtils.invalid && repetitionLength == IrpUtils.invalid)
                    repetitionLength = data.length/2 - introLength - endingLength;
                if (args[arg_i].startsWith("+")) {
                    for (int i = 0; i < args.length - arg_i; i++)
                        data[i] = Integer.parseInt(args[i + arg_i].replaceFirst("[\\+-]", ""));
                    result = decode(data, frequency, repetitionLength, endingLength); // ??
                } else {
                    for (int i = 0; i < args.length - arg_i; i++)
                        data[i] = Integer.parseInt(args[i + arg_i], 16);
                    result = decode(data);
                }
            }
            if (result != null)
                for (DecodedSignal result1 : result)
                    System.out.println(result1);

       	} catch (IrpMasterException ex) {
            System.err.println("IrpMaster exception: " + ex.getMessage());
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
        } catch (NumberFormatException ex) {
            System.err.println("Number format exception " + ex.getMessage());
        } catch (ArrayIndexOutOfBoundsException e) {
	    if (debug > 0)
                System.err.println("ArrayIndexOutOfBoundsException occured");
	    usage(IrpUtils.exitFatalProgramFailure);
	}
    }
    private boolean valid = false;
    private DecodeIRCaller dirc;
    private DecodedSignal[] decodedSignals;

    /**
     * The most general constructor.
     * @param data Microsecond data, all positive, with odd indexes representing flashes, and even indexes gaps.
     * @param lengthRepeat Length of the repeat sequence, in pairs; thus not always even.
     * @param lengthEnding Length of the ending sequence, in pairs.
     * @param frequency Carrier frequency in Hz.
     */
    private DecodeIR(int[] data, int lengthRepeat, int lengthEnding, int frequency) {
        setup(data, lengthRepeat, lengthEnding, frequency);
    }
    /**
     * General constructor.
     * @param irSignal
     */
    private DecodeIR(IrSignal irSignal) {
        setup(irSignal);
    }
    /**
     * Constructs a decoder from an array of integers with the Pronto semantics.
     * @param CCF Array of integers with Pronto semantics.
     * @throws IrpMasterException
     */
    private DecodeIR(int[] CCF) throws IrpMasterException {
        this(Pronto.ccfSignal(CCF));
    }
    /* *
    * Constructs a decoder by invoking ExchangeIR to interpret the sequence.
    * @param irSequence
    * @param frequency
    * /
    private DecodeIR(IrSequence irSequence, double frequency) {
    this(InterpretString.interpretIrSequence(irSequence, frequency, true, true));
    }*/

    /**
     * Constructs a decoder by invoking ExchangeIR to interpret the sequence.
     * @param irSequence
     */
    private DecodeIR(ModulatedIrSequence irSequence) {
        this(InterpretString.interpretIrSequence(irSequence, true, true));
    }
    /**
     * Constructs a decoder from a string, having one of the known formats.
     * @param ccf String.
     * @throws ParseException
     * @throws IncompatibleArgumentException
     * @throws UnassignedException
     * @throws DomainViolationException
     * @throws InvalidRepeatException
     */
    private DecodeIR(String ccf) throws ParseException, IncompatibleArgumentException, UnassignedException, DomainViolationException, InvalidRepeatException {
        IrSignal irSignal = InterpretString.interpretString(ccf, IrpUtils.defaultFrequency, false, true);
        if (irSignal == null)
            throw new ParseException("Could not interpret string `" + ccf + "'");
        setup(irSignal);
    }
    private boolean setup(int[] us_data, int lengthRepeat, int lengthEnding, int frequency) {
        boolean success = loadLibrary();
        if (!success)
            return false;

        valid = true;

        dirc = new DecodeIRCaller();
        dirc.setBursts(us_data, lengthRepeat, lengthEnding);
        dirc.setFrequency(frequency);
        dirc.initDecoder();
        Debug.debugDecodeIR("DecodeIR was setup with f=" + frequency + ", repeat=" + lengthRepeat
                + ", ending=" + lengthEnding + ", data=" + IrpUtils.stringArray(us_data));

        ArrayList<DecodedSignal> work = new ArrayList<>(4);
        while (dirc.decode()) {
            DecodedSignal decodedSignal = new DecodedSignal(dirc.getProtocolName(),
                    dirc.getDevice(),
                    dirc.getSubDevice(),
                    dirc.getOBC(),
                    dirc.getHex(),
                    dirc.getMiscMessage(),
                    dirc.getErrorMessage());
            work.add(decodedSignal);
        }

        decodedSignals = work.toArray(new DecodedSignal[work.size()]);
        return true;
    }
    private boolean setup(IrSignal irSignal) {
        return setup(irSignal.toIntArray(), irSignal.getRepeatLength(), irSignal.getEndingLength(),
                (int)Math.round(irSignal.getFrequency()));
    }
    /**
     *
     * @return Array of DecodedSignal (possibly of length zero if no decodes), or null if the instance is not valid.
     */

    public DecodedSignal[] getDecodedSignals() {
        return valid ? decodedSignals : null;
    }
    /**
     * Thrown if no sensible decode is found.
     */
    public static class DecodeIrException extends IrpMasterException {
        public DecodeIrException(String string) {
            super(string);
        }
    }
    public static class DecodedSignal {
        /**
         * Makes a nice string from the decodes.
         * If argument censor is true, the function returns the the first decode (if present), then a note on the others, that were omitted.
         * @param decodes
         * @param censor true if the decodes after the first should be "censored out".
         * @return Nicely formatted string.
         */
        public static String toPrintString(DecodedSignal[] decodes, boolean censor) {
            if (decodes == null)
                return "";

            if (censor && decodes.length > 1) {
                return decodes[0].toString() + (decodes.length == 2 ? " (+ one other decode)"
                        : (" (+ " + decodes.length + " other decodes)"));
            } else {
                StringBuilder result = new StringBuilder(64);
                for (DecodedSignal decode : decodes) {
                    if (result.length() > 0)
                        result.append("; ");
                    result.append(decode.toString());
                }
                return result.toString();
            }
        }

        /**
         * Makes a nice string from the decodes.
         * @param decodes
         * @return Nicely formatted string.
         */
        public static String toPrintString(DecodedSignal[] decodes) {
            return toPrintString(decodes, false);
        }

        private String protocolName;
        private int device;
        private int subDevice;
        private int OBC;
        //public int[] hex;
        private String miscMessage;
        private String errorMessage;
        private final Map<String, Integer>miscNames = new LinkedHashMap<>(4);

        public DecodedSignal(String protocolName,
                int device,
                int subDevice,
                int OBC,
                int[] hex,
                String miscMessage,
                String errorMessage) {
            this.protocolName = protocolName;
            this.device = device;
            this.subDevice = subDevice;
            this.OBC = OBC;
            //this.hex = hex;
            this.miscMessage = miscMessage;
            this.errorMessage = errorMessage;
            String[] names = miscMessage.replaceAll("\\s*=\\s*", "=").split("[ ,]");
            for (String name : names) {
                String[] q = name.split("=");
                if (q.length == 2) {
                    try {
                        miscNames.put(q[0], Integer.parseInt(q[1]));
                    } catch (NumberFormatException ex) {
                        UserComm.warning("Output from DecodeIR: `" + name + "' could not be parsed.");
                    }
                }
            }
        }

        public DecodedSignal(String protocolName,
                int device,
                int subDevice,
                int OBC) {
            this(protocolName, device, subDevice, OBC, null, "", "");
        }

        public DecodedSignal(String protocolName,
                int device,
                int OBC) {
            this(protocolName, device, (int) IrpUtils.invalid, OBC);
        }

        /**
         * Return the parameters in the IrpMaster way.
         * @return Map containing the parameters.
         */
        public Map<String, Long> getParameters() {
            Map<String, Long> result = new LinkedHashMap<>(4);
            if (device >= 0)
                result.put("D", (long)device);
            if (subDevice >=0)
                result.put("S", (long)subDevice);
            if (OBC >= 0)
                result.put("F", (long)OBC);

            for (String name : this.miscNames.keySet())
                result.put(name, (long)miscNames.get(name));

            return result;
        }

        /**
         * Returns the protocol name.
         * @return protocol name
         */
        public String getProtocol() {
            return protocolName;
        }

        // There are some idiosyncracises in the output of DecodeIr. I try to take care of them here.
        public boolean isOk(String protocolName, Protocol protocol, Map<String, Long> vars) {

            if (protocolName == null || protocol == null || vars == null)
                return false;

            // First do the protocol specific stuff here ...
            boolean specialOk = true;
            boolean protocolOk;
            HashSet<String> exceptionVars = new HashSet(4);
            if (protocolName.equalsIgnoreCase("x10.n")) {
                protocolOk = this.protocolName.toLowerCase(IrpUtils.dumbLocale).startsWith("x10:");
                specialOk = Integer.parseInt(this.protocolName.substring(4)) == vars.get("N");
                exceptionVars.add("N");
            } else if (protocolName.endsWith("-???-???")) {
                int length = this.protocolName.lastIndexOf('-') + 1;
                protocolOk = protocolName.startsWith(this.protocolName.toLowerCase(IrpUtils.dumbLocale).substring(0, length));
                if (!protocolOk)
                    return false;
                String MNstring = this.protocolName.substring(length);
                String[] s = MNstring.split("\\.");
                specialOk = Integer.parseInt(s[0]) == vars.get("M") && Integer.parseInt(s[1]) == vars.get("N");
                exceptionVars.add("M");
                exceptionVars.add("N");
            } else if (protocolName.equalsIgnoreCase("tivo")) {
                protocolOk = this.protocolName.toLowerCase(IrpUtils.dumbLocale).startsWith("tivo ");
                specialOk = Integer.parseInt(this.protocolName.substring(10)) == vars.get("U")
                        && this.device == 133
                        && this.subDevice == 48;
                exceptionVars.add("U");
            } else if (this.protocolName.equals("XMP-1/2")) {
                protocolOk = protocolName.equalsIgnoreCase("XMP-1") || protocolName.equalsIgnoreCase("XMP-2");
            } else if (this.protocolName.equals("Ad Notam")) {
                protocolOk = protocolName.equalsIgnoreCase("adnotam");
            } else if (this.protocolName.equals("G.I. Cable")) {
                protocolOk = protocolName.equalsIgnoreCase("G.I.Cable");
            } else if (this.protocolName.equals("Pace MSS")) {
                protocolOk = protocolName.equalsIgnoreCase("PaceMSS");
            } else if (this.protocolName.equalsIgnoreCase("RECS80")) {
                protocolOk = protocolName.substring(0, 6).equalsIgnoreCase("RECS80");
            } else {
                protocolOk = protocolName.equalsIgnoreCase(this.protocolName/*.replaceAll("\\s", "")*/);
            }

            // ... then the rest is generic
            for (Entry<String, Long> kvp : vars.entrySet()) {
                String var = kvp.getKey();
                long deflt = IrpUtils.invalid;
                try {
                    deflt = protocol.getParameterDefault(var, vars);
                } catch (UnassignedException ex) {
                    // This is not an error
                } catch (DomainViolationException ex) {
                    System.err.println(ex.getMessage());
                }

                if (exceptionVars.contains(var)) {
                    // ignore, due to special properties
                } else if (kvp.getValue() == deflt) {
                    // Variable has its default value, nothing should be checked
                } else {
                    // check
                    int decodeIrValue = var.equals("D") ? this.device
                            : var.equals("S") ? this.subDevice
                            : var.endsWith("F") ? this.OBC
                            //: var.equals("I") ? (miscNames.containsKey("ID") ? miscNames.get("ID") : (int) IrpUtils.invalid)
                            : miscNames.containsKey(var) ? miscNames.get(var)
                            : (int) IrpUtils.invalid;
                    if (decodeIrValue == IrpUtils.invalid || kvp.getValue().intValue() != decodeIrValue) {
                        // Variable not there or has wrong value, interrupt check as failed
                        return false;
                    }
                }
            }
            return protocolOk && specialOk;
        }

        @Override
        public String toString() {
            return "protocol = " + protocolName
                    + ((device >= 0)    ? (", device = " + device) : "")
                    + ((subDevice >= 0) ? (", subdevice = " + subDevice) : "")
                    + ", obc = " + OBC
                    //+ ", hex = " + getHex
                    + (!miscMessage.isEmpty() ? (", misc = " + miscMessage) : "")
                    + (!errorMessage.isEmpty() ? (", error = " + errorMessage) : "");
        }


        /**
         * Generates an element, belonging to the Document in the argument, containing the decode in readable form.
         * @param doc
         * @return Just create element, having tagname "decode".
         */
        public Element xmlElement(Document doc) {
            Element el = doc.createElement("decode");
            el.setAttribute("protocol", protocolName);
            if (device >= 0)
                el.setAttribute("device", Integer.toString(device));
            if (subDevice >= 0)
                el.setAttribute("subdevice", Integer.toString(subDevice));
            el.setAttribute("obc", Integer.toString(OBC));
            if (!miscMessage.isEmpty())
                el.setAttribute("misc", miscMessage);
            if (!errorMessage.isEmpty())
                el.setAttribute("error", errorMessage);
            return el;
        }
    }
}
