/*
Copyright (C) 2013,2014 Bengt Martensson.

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

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import org.harctoolbox.girr.Command;
import org.harctoolbox.ircore.IrCoreException;
import org.harctoolbox.ircore.ModulatedIrSequence;
import org.harctoolbox.irp.IrpException;
import org.harctoolbox.irscrutinizer.Props;

/**
 * This class implements functionality common to importers. It does not know to read stuff.
 *
 * <p>The present version is designed only to be used within IrScrutinizer. It is in the present form not meant as a reusable component.
 * Nevertheless, derived classes are supposed to get properties with the protected functions getCreatingUser and such.
 */
public abstract class Importer {

    // now we take stuff like creatingUser directly from properties; no need to keep out own versions,
    // causing considerable effort to keep consistent with the global version.
    private static Props properties = null;

    public static void setProperties(Props newProperties) {
        properties = newProperties;
    }

    // This can contain multiple commands with the same name
    private final ArrayList<Command> commands;

    // This has, of course, unique names
    private final LinkedHashMap<String,Command>commandIndex;

    // origin is set by load, not the constructor.
    protected String origin = null;

    protected Importer() {
        commands = new ArrayList<>(32);
        commandIndex = new LinkedHashMap<>(32);
    }


    /**
     * @return the creatingUser
     */
    protected String getCreatingUser() {
        return properties != null ? properties.getCreatingUser() : null;
    }

    /**
     * @return the invokeDecodeIr
     */
    protected boolean isInvokeDecoder() {
        return properties != null  && properties.getInvokeDecoder();
    }

    /**
     * @return the invokeRepeatFinder
     */
    protected boolean isInvokeRepeatFinder() {
        return properties != null && properties.getInvokeRepeatFinder();
    }

    /**
     * @return the invokeAnalyzer
     */
    protected boolean isInvokeAnalyzer() {
        return properties != null && properties.getInvokeAnalyzer();
    }

    protected boolean isInvokeCleaner() {
        return properties != null && properties.getInvokeCleaner();
    }

    protected boolean isVerbose() {
        return properties != null && properties.getVerbose();
    }

    /**
     * @return the generateRaw
     */
    protected boolean isGenerateRaw() {
        return properties != null && properties.getGenerateRaw();
    }

    /**
     * @return the generateCcf
     */
    protected boolean isGenerateCcf() {
        return properties != null && properties.getGenerateCcf();
    }

    /**
     * @return the fallbackFrequency
     */
    protected Double getFallbackFrequency() {
        return properties != null ? properties.getFallbackFrequency() : null;
    }

    protected int getEndingTimeout() {
        return properties != null ? properties.getCaptureEndingTimeout() : null;
    }

    protected Double getAbsoluteTolerance() {
        return properties != null ? properties.getAbsoluteTolerance() : null;
    }

    protected Double getRelativeTolerance() {
        return properties != null ? properties.getRelativeTolerance() : null;
    }

    protected Double getDummyGap() {
        return properties != null ? properties.getDummyGap() : null;
    }

    protected Double getMinRepeatLastGap() {
        return properties != null ? properties.getMinRepeatLastGap() : null;
    }

    protected Double getChopThreshold() {
        return properties != null ? properties.getChopThreshold() : null;
    }

    protected void prepareLoad(String origin) {
        this.origin = origin;
        clearCommands();
    }

    protected String uniqueName(String name) {
        String uniqueName = name;
        int index = 1;
        while (commandIndex.containsKey(uniqueName)) {
            uniqueName = name + '$' + Integer.toString(index);
            index++;
        }

        return uniqueName;
    }

    protected boolean addCommand(Command command) {
        commands.add(command);
        if (commandIndex.containsKey(command.getName()))
            return false;

        commandIndex.put(command.getName(), command);
        return true;
    }

    protected boolean addCommands(Collection<Command> commands) {
        boolean totalStatus = true;
        for (Command command : commands) {
            boolean status = addCommand(command);
            totalStatus = totalStatus && status;
        }
        return totalStatus;
    }

    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public ArrayList<Command> getCommands() {
        return commands;
    }

    public Command getCommand(String name) {
        return commandIndex.get(name);
    }

    public ModulatedIrSequence getConcatenatedCommands() throws IrpException, IrCoreException {
        ModulatedIrSequence[] array = new ModulatedIrSequence[commands.size()];
        int index = 0;
        for (Command command : commands) {
            array[index] = command.toIrSignal().toModulatedIrSequence(1);
            index++;
        }
        return ModulatedIrSequence.concatenate(array);
    }

    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public Map<String,Command> getCommandIndex() {
        return commandIndex;
    }

    protected void clearCommands() {
        commands.clear();
        commandIndex.clear();
    }

    public String getOrigin() {
        return origin;
    }

    public URL getHomeUrl() {
        return null;
    }
}
