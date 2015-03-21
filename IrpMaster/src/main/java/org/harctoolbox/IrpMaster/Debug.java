/*
Copyright (C) 2011 Bengt Martensson.

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

/**
 * This class implements a rather simpleminded fine grain debugging mechanism.
 * It cannot be instantiated, but only consists of static members.
 * 
 * Create a subclass if for example creating a GUI, and override debugMsg etc.
 *
 */
public class Debug {
    
    public static enum Item {

        Main,
        Configfile,
        IrpParser,
        ASTParser,
        NameEngine,
        BitFields,
        Parameters,
        Expressions,
        IrSignals,
        IrStreamItems,
        BitSpec,
        DecodeIR,
        IrStreams,
        BitStream,
        Evaluate;

        public static int power(Item it) {
            return 1 << it.ordinal();
        }
        
        public static String helpString(String separator) {
            StringBuilder result = new StringBuilder();
            for (Item it : Item.values())
                result.append(it).append("=").append(power(it)).append(separator);
            return result.substring(0, result.length()-separator.length());
        }
        
        public static String helpString() {
            return helpString(",");
        }

        public static String helpString(int index) {
            return values()[index].name();
        }
        
        public static int size() {
            return values().length;
        }
    };

    private int debug = 0;
    
    private UserComm userComm;
    
    private static Debug instance = new Debug(0);
    
    public static Debug getInstance() {
        return instance;
    }
    
    public static String helpString() {
        return helpString(",");
    }
    
    public static String helpString(String separator) {
        return Item.helpString(separator);
    }
    
    public static void setDebug(int d) {
        instance.debug = d;
    }
    
    public Debug(int debug) {
        this(debug, new UserComm());
    }
    
    public Debug(int debug, UserComm userComm) {
        this.debug = debug;
        this.userComm = userComm;
    }

    public void debugMsg(Item type, String msg) {
	if (debugOn(type))
            userComm.debugMsg(type.name(), msg);
    }
    
    public void debugMsg(String msg) {
        userComm.debugMsg("", msg);
    }
    
    public boolean debugOn(Item type) {
        return (Item.power(type) & debug) != 0;
    }
    
    @Override
    public String toString() {
        return toString(",");
    }
    
    public String toString(String separator) {
        StringBuilder result = new StringBuilder();
        for (Item it : Item.values())
            if (debugOn(it))
                result.append(it).append(separator);
        
        return "[" + (result.length() == 0 ? "" : result.substring(0, result.length()-separator.length())) + "]";
    }

    public static void debugMain(String msg) {
        instance.debugMsg(Item.Main, msg);
    }
    
    public static void debugConfigfile(String msg) {
        instance.debugMsg(Item.Configfile, msg);
    }
    
    public static void debugIrpParser(String msg) {
        instance.debugMsg(Item.IrpParser, msg);
    }
    
    public static void debugASTParser(String msg) {
        instance.debugMsg(Item.ASTParser, msg);
    }
    
    public static void debugNameEngine(String msg) {
        instance.debugMsg(Item.NameEngine, msg);
    }
    
    public static void debugBitFields(String msg) {
        instance.debugMsg(Item.BitFields, msg);
    }
    
    public static void debugParameters(String msg) {
        instance.debugMsg(Item.Parameters, msg);
    }
    
    public static void debugExpressions(String msg) {
        instance.debugMsg(Item.Expressions, msg);
    }
    
    public static void debugIrSignals(String msg) {
        instance.debugMsg(Item.IrSignals, msg);
    }
    
    public static void debugIrStreamItems(String msg) {
        instance.debugMsg(Item.IrStreamItems, msg);
    }
    
    public static void debugBitSpec(String msg) {
        instance.debugMsg(Item.BitSpec, msg);
    }
    
    public static void debugDecodeIR(String msg) {
        instance.debugMsg(Item.DecodeIR, msg);
    }
    
    public static void debugIrStreams(String msg) {
        instance.debugMsg(Item.IrStreams, msg);
    }
    
    public static void debugBitStream(String msg) {
        instance.debugMsg(Item.BitStream, msg);
    }
    
    public static void debugEvaluate(String msg) {
        instance.debugMsg(Item.Evaluate, msg);
    }
    
    public static void main(String[] args) {
        System.out.println(Item.helpString("\n"));
        
        if (args.length > 0) {
            Debug debug = new Debug(Integer.parseInt(args[0]));
            System.out.println(debug);
        }
    }
}
