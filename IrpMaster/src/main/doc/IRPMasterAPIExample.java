/*
 * Author: Bengt Martensson, barf@bengt-martensson.de
 * This snippet is in the public domain.
 */
package irpmasterapiexample;

import java.io.IOException;
import java.util.HashMap;
import org.harctoolbox.IrpMaster.IrSignal;
import org.harctoolbox.IrpMaster.IrpMaster;
import org.harctoolbox.IrpMaster.IrpMasterException;
import org.harctoolbox.harchardware.ir.GlobalCache;
import org.harctoolbox.harchardware.ir.NoSuchTransmitterException;

public class IRPMasterAPIExample {
    // Parameters used to communicate with the particular GC
    public static final String globalcacheIP = "192.168.1.70";

    private static void usage() {
        System.err.println("Usage:");
        System.err.println("\tIRPMasterAPIExample IrpMaster.ini-path protocolname parameterassignments...");
        System.exit(1);
    }

    // Usage: IRPMasterAPIExample <configfilename> <protocolname> <device> <function>
    // Example: IRPMasterAPIExample /usr/local/irscrutinizer/IrpProtocols.ini rc5 D=0 F=12
    public static void main(String[] args) {
        // This hashmap holds the actual parameter values used to render the signal
        HashMap<String, Long> parameters = new HashMap<>();

        if (args.length < 3)
            usage();

        try {
            // Create an IrpMaster instance from the configuration file
            IrpMaster irpMaster = new IrpMaster(args[0]);

            String protocolName = args[1];

            // parse the parameters and enter them into the dictionary
            for (int i = 2; i < args.length; i++) {
                String[] s = args[i].split("=");
                parameters.put(s[0], Long.parseLong(s[1]));
            }

            // Render the signal using current parameters
            IrSignal irSignal = new IrSignal(irpMaster, protocolName, parameters);

            // Create a GlobalCache object
            GlobalCache globalCache = new GlobalCache(globalcacheIP, true);

            // ... and send it the recently rendered IR signal
            globalCache.sendIr(irSignal);
            // ... done!!
        } catch (IOException | IrpMasterException | NoSuchTransmitterException ex) {
            System.err.println(ex.getMessage());
        }
    }
}
