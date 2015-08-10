/*
Copyright (C) 2013, 2014 Bengt Martensson.

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

package org.harctoolbox.guicomponents;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import org.harctoolbox.IrpMaster.IrpUtils;

// Interfaces to Desktop
public class GuiUtils implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int maxGuiMessageLength;
    private boolean usePopupsForErrors = false;
    private boolean usePopupsForHelp = false;
    private boolean offerStackTrace = false;
    private final String programName;
    private final JFrame frame;
    boolean verbose = false;

    public GuiUtils(JFrame frame, String programName, int maxGuiMessageLength) {
        this.maxGuiMessageLength = maxGuiMessageLength;
        this.programName = programName;
        this.frame = frame;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    /**
     * @param offerStackTrace the offerStackTrace to set
     */
    public void setOfferStackTrace(boolean offerStackTrace) {
        this.offerStackTrace = offerStackTrace;
    }

    public void setUsePopupsForErrors(boolean usePopupsForErrors) {
        this.usePopupsForErrors = usePopupsForErrors;
    }

    public void setUsePopupsForHelp(boolean usePopupsForHelp) {
        this.usePopupsForHelp = usePopupsForHelp;
    }

    private String truncate(String message) {
        return message == null || message.length() <= maxGuiMessageLength  ? message
                : message.substring(0, maxGuiMessageLength - 3) + "...";
    }

    public void info(String message) {
         if (usePopupsForErrors) {
            JOptionPane.showMessageDialog(frame, truncate(message), programName + " information",
                    JOptionPane.INFORMATION_MESSAGE,
            new ImageIcon(GuiUtils.class.getResource("/icons/Crystal-Clear/48x48/actions/info.png")));
        } else {
            System.err.println(message);
        }
    }

    public static void fatal(Exception ex, int errorcode) {
        fatal(ex.getClass().getSimpleName() + ": " + ex.getMessage(), errorcode);
    }

    public static void fatal(String message, int errorcode) {
        if (System.console() == null)
            JOptionPane.showMessageDialog(null, message, "Fatal error", JOptionPane.ERROR_MESSAGE);
        else
            System.err.println(message);
        System.exit(errorcode);
    }

    public void trace(String message) {
        System.err.println(message);
    }

    // A message is there to be used, not to be clicked away.
    // Do not use popups here.
    public void message(String message) {
        System.err.println(message);
    }

    public void warning(String message) {
         if (usePopupsForErrors) {
            JOptionPane.showMessageDialog(frame, truncate(message), programName + " warning",
                    JOptionPane.WARNING_MESSAGE,
                    new ImageIcon(GuiUtils.class.getResource("/icons/Crystal-Clear/48x48/apps/error.png")));
        } else {
            System.err.println("Warning: " + message);
        }
    }

    public void error(String message) {
        error(message, false);
    }

    private boolean error(String message, boolean offerStackTrace) {
        int ans = 0;
        if (usePopupsForErrors) {
            if (offerStackTrace) {
                Object[] options = {"OK", "Print stack trace"};
                ans = JOptionPane.showOptionDialog(frame, truncate(message), programName + " error",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.ERROR_MESSAGE,
                        new ImageIcon(GuiUtils.class.getResource("/icons/Crystal-Clear/48x48/apps/error.png")),
                        options, // the titles of buttons
                        options[0]); //default button title
            } else {
                JOptionPane.showMessageDialog(frame, truncate(message), programName + " error",
                        JOptionPane.ERROR_MESSAGE,
                        new ImageIcon(GuiUtils.class.getResource("/icons/Crystal-Clear/48x48/apps/error.png")));
            }
        } else {
            System.err.println("Error: " + message);
            ans = offerStackTrace ? 1 : 0;
        }
        return ans > 0;
    }

    public void error(Throwable ex) {
        error(ex, ex.getMessage());
    }

    public void error(Throwable ex, String message) {
        boolean result = error(ex.getClass().getSimpleName() + ": " + message, offerStackTrace);
        if (result)
            ex.printStackTrace();
    }

    public String getInput(String message, String title, String defaultAnswer) {
        return (String) JOptionPane.showInputDialog(frame, message, title, JOptionPane.QUESTION_MESSAGE, null, null, defaultAnswer);
    }

    public Integer getIntegerInput(String message, int oldValue) {
        String s = getInput(message, "Parameter input", Integer.toString(oldValue));
        return s != null ? Integer.parseInt(s) : null;
    }

    public Long getLongInput(String message, long oldValue) {
        String s = getInput(message, "Parameter input", Long.toString(oldValue));
        return s == null ? null
                : s.trim().isEmpty() ? IrpUtils.invalid
                : IrpUtils.parseLong(s, false);
    }

    public boolean confirm(String message) {
        return JOptionPane.showConfirmDialog(frame, message, "Confirmation requested", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION;
    }

    public void browse(String uri) {
        browse(URI.create(uri));
    }

    public void browse(URI uri) {
        if (! Desktop.isDesktopSupported()) {
            error("Desktop not supported");
            return;
        }
        if (uri == null || uri.toString().isEmpty()) {
            error("No URI.");
            return;
        }
        try {
            Desktop.getDesktop().browse(uri);
            if (verbose)
                trace("Browsing URI `" + uri.toString() + "'");
        } catch (IOException ex) {
            error("Could not start browser using uri `" + uri.toString() + "'.");
        }
    }

    public void open(File file) {
        if (! Desktop.isDesktopSupported()) {
            error("Desktop not supported");
            return;
        }

        if (!file.canRead()) {
            error("Cannot read file " + file.toString());
            return;
        }
        try {
            Desktop.getDesktop().open(file);
            if (verbose)
                trace("open file `" + file.toString() + "'");
       } catch (IllegalArgumentException ex) {
            error(ex);
       } catch (IOException ex) {
           // According to javadoc for Desktop.getDesktop().open
            error("File `" + file.toString() + "' has no associated application or the associated application failed.");
        }
    }

    public void edit(File file) {
        if (!Desktop.isDesktopSupported()) {
            error("Desktop not supported");
            return;
        }

        if (Desktop.getDesktop().isSupported(Desktop.Action.EDIT)) {
            try {
                Desktop.getDesktop().edit(file);
                if (verbose)
                    trace("edit file `" + file.toString() + "'");
            } catch (IOException ex) {
                if (verbose)
                    trace("edit file `" + file.toString() + "' failed, trying open...'");
                open(file);
            } catch (UnsupportedOperationException ex) {
                error("Edit not supported.");
            }
        } else
            open(file);
    }

    public void editOrOpen(File file) {
         if (!Desktop.isDesktopSupported()) {
            error("Desktop not supported");
            return;
        }

        if (Desktop.getDesktop().isSupported(Desktop.Action.EDIT))
            edit(file);
        else
            open(file);
    }

    public void browseOrEdit(String urlOrFilename) {
        try {
            URL url = new URL(urlOrFilename);
            browse(url.toURI());
        } catch (MalformedURLException ex) {
            editOrOpen(new File(urlOrFilename));
        } catch (URISyntaxException ex) {
            editOrOpen(new File(urlOrFilename));
        }
    }

    public void help(String helpText) {
        if (usePopupsForHelp)
            HelpPopup.newHelpPopup(frame, helpText);
        else
            System.err.println(helpText);
    }

    public boolean checkUpToDate(String currentVersionUrl, String versionString) {
        BufferedReader in = null;
        String current = null;
        try {
            URL url = new URL(currentVersionUrl);
            in = new BufferedReader(new InputStreamReader(url.openStream(), "US-ASCII"));
            String line = in.readLine();
            if (line == null || line.isEmpty()) {
                error("Problem getting current version");
                return false;
            }
            current = line.trim();
            info(current.equals(versionString)
                    ? "You are using the latest version of " + programName + ", " + versionString
                    : "Current official version is " + current + ", your version is " + versionString + ".");
        } catch (IOException ex) {
            error("Problem getting current version: " + ex.getMessage());
        } finally {
            try {
                if (in != null)
                    in.close();
            } catch (IOException ex) {
                error("Problem closing version check Url: " + ex.getMessage());
            }
        }
        return current != null && current.equals(versionString);
    }

    public static void main(String[] args) {
        fatal("Nasty thing happened", 42);
    }
}
