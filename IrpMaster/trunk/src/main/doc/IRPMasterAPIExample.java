/*
 * Author: Bengt Martensson, barf@bengt-martensson.de
 * This snippet is in the public domain.
 */
package irpmasterapiexample;

import java.util.HashMap;
import java.io.IOException;
import java.net.UnknownHostException;
import org.antlr.runtime.RecognitionException;
import org.harctoolbox.globalcache;
import IrpMaster.IrSignal;
import IrpMaster.IrpMaster;
import IrpMaster.IrpMasterException;
import IrpMaster.Protocol;

public class IRPMasterAPIExample {
    // Parameters used to communicate with the particular GC (could also be read from the command line)
    public static final String globalcacheIP = "192.168.1.70";
    public static final int globalcachePort = 1;
    
    // Usage: IRPMasterAPIExample <configfilename> <protocolname> <device> <function>
    public static void main(String[] args) throws RecognitionException {
        
        // This hashmap holds the actual parameter values used to render the signal
        HashMap<String, Long> parameters = new HashMap<String, Long>();
        
        try {
            int arg_i = 0;
            
            // Create an IrpMaster instance from the configuration file
            IrpMaster irpMaster = new IrpMaster(args[arg_i++]);
            
            String protocolName = args[arg_i++];
            
            // Create protocol, i.e. parse the IRP string
            Protocol protocol = irpMaster.newProtocol(protocolName);
            
            // Fill the values into the parameter table
            parameters.put("D", Long.parseLong(args[arg_i++]));
            parameters.put("F", Long.parseLong(args[arg_i++]));
            
            // Render the signal using current actual parameters
            IrSignal irSignal = protocol.renderIrSignal(parameters);
            
            // Create a GlobalCache object
            globalcache globalCache = new globalcache(globalcacheIP, globalcache.gc_model.gc_100_06, true);
            
            // ... and send it the ccf version of the recently rendered IR signal
            globalCache.send_ir(irSignal.ccfString(), globalcachePort);
            // ... done!!
        } catch (UnknownHostException ex) {
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
        } catch (InterruptedException ex) {
            System.err.println(ex.getMessage());
        } catch (IrpMasterException ex) {
            System.err.println(ex.getMessage());
        }
    }
}
