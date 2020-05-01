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
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.ParseException;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import org.harctoolbox.ircore.IrCoreUtils;

// Interfaces to Desktop
public class GuiUtils implements Serializable {

    public static void fatal(Exception ex, int errorcode) {
        fatal(ex, errorcode, null);
    }

    @SuppressWarnings("CallToPrintStackTrace")
    public static void fatal(Exception ex, int errorcode, EmergencyFixer fixer) {
        String message = ex.getClass().getSimpleName() + ": " + ex.getMessage();
        if (System.console() != null)
            System.err.println(message);

        // Is headless an option?
        JOptionPane.showMessageDialog(null, message, "Fatal error", JOptionPane.ERROR_MESSAGE);

        if (fixer != null) {
            boolean answer = confirm(null, fixer.getQuestion());
            if (answer)
                fixer.fix();
            String finalMessage = answer ? fixer.getYesMessage() : fixer.getNoMessage();
            if (finalMessage != null)
                JOptionPane.showMessageDialog(null, finalMessage, null,
                        JOptionPane.INFORMATION_MESSAGE,
                        new ImageIcon(GuiUtils.class.getResource("/icons/Crystal-Clear/48x48/actions/info.png")));
        }
        ex.printStackTrace();
        System.exit(errorcode);
    }

    private static boolean confirm(JFrame frame, String message, int optionType) {
        return JOptionPane.showConfirmDialog(frame, message, "Confirmation requested", optionType) == JOptionPane.OK_OPTION;
    }

    private static boolean confirm(JFrame frame, String message) {
        return confirm(frame, message, JOptionPane.OK_CANCEL_OPTION);
    }

    private final int maxGuiMessageLength;
    private boolean usePopupsForErrors = false;
    private boolean usePopupsForHelp = false;
    private boolean offerStackTrace = false;
    private final String programName;
    private final JFrame frame;
    private boolean verbose = false;

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

    public void error(Throwable ex, String msg) {
        String message = msg.replaceFirst("^java.lang.RuntimeException: ", "");
        String errorMessage = ex instanceof ParseException
                ? ex.getClass().getSimpleName() + " on line " + ((ParseException) ex).getErrorOffset() + ": " + message
                : ex.getClass().getSimpleName() + ": " + message;
        boolean result = error(errorMessage, offerStackTrace);
        if (result)
            ex.printStackTrace(System.err);
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
                : s.trim().isEmpty() ? IrCoreUtils.INVALID
                : IrCoreUtils.parseLong(s, false);
    }

    public Double getDoubleInput(String message, double oldValue) {
        String s = getInput(message, "Parameter input", Double.toString(oldValue));
        return s != null ? Double.parseDouble(s) : null;
    }

    public boolean confirm(String message) {
        return confirm(message, JOptionPane.OK_CANCEL_OPTION);
    }

    public boolean confirm(String message, int optionType) {
        return confirm(frame, message, optionType);
    }

    public void mail(String address, String subject, String body) throws URISyntaxException, IOException {
        URI uri = new URI("mailto:" + address + "?subject=" + subject + "&body=" + body);
        Desktop.getDesktop().mail(uri);
    }

    // There is, deliberately, no public void browse(String string)
    // rationale: too easy to confuse file names and URLs,
    // thereby too error prone.

    public void browse(File file) throws MalformedURLException, URISyntaxException {
        browse(file.toURI());
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
            if (verbose)
                trace("Browsing URI \"" + uri.toString() + "\"");
            Desktop.getDesktop().browse(uri);
        } catch (IOException ex) {
            boolean stacktrace = error("Could not start browser using uri \"" + uri.toString() + "\".", offerStackTrace);
            if (stacktrace)
                ex.printStackTrace(System.err);
        }
    }

    // Do NOT add an edit(...) function!

    /**
     * For maximal portability and robustness, and since the support of edit is
     * ... not omnipresent, use only this function.
     *
     * @param file file or directory to be opened/edited.
     */
    public void open(File file) {
        if (!Desktop.isDesktopSupported()) {
            error("Desktop not supported");
            return;
        }

        if (Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            try {
                Desktop.getDesktop().open(file);
                if (verbose)
                    trace("open file \"" + file.toString() + "\" succeeded");
                return;
            } catch (IOException ex) {
                if (verbose)
                    trace("open file \"" + file.toString() + "\" failed: " + ex.getLocalizedMessage());
            }
        }

        if (Desktop.getDesktop().isSupported(Desktop.Action.EDIT)) {
            try {
                Desktop.getDesktop().edit(file);
                if (verbose)
                    trace("edit file \"" + file.toString() + "\" succeeded");
                return;
            } catch (IOException ex) {
                if (verbose)
                    trace("edit file \"" + file.toString() + "\" failed: " + ex.getLocalizedMessage());
            }
        }

        error("Neither edit nor open supported/working");
    }

    public void browseOrEdit(String urlOrFilename) {
        try {
            URL url = new URL(urlOrFilename);
            browse(url.toURI());
        } catch (MalformedURLException | URISyntaxException ex) {
            open(new File(urlOrFilename));
        }
    }

    public void help(String helpText) {
        if (usePopupsForHelp)
            HelpPopup.newHelpPopup(frame, helpText);
        else
            System.err.println(helpText);
    }

    public boolean checkUpToDate(String currentVersionUrl, String versionString, Proxy proxy) {
        URL url;
        try {
            url = new URL(currentVersionUrl);
        } catch (MalformedURLException ex) {
            //throw new ThisCannotHappenException(ex);
            return false;
        }

        if (verbose)
            trace("Opening " + url.toString() + " using proxy " + proxy);

        String current = null;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(url.openConnection(proxy).getInputStream(), "US-ASCII"))) {
            String line = in.readLine();
            if (verbose)
                trace("got: \"" + line + "\"");
            if (line == null || line.isEmpty()) {
                error("Problem getting current version");
                return false;
            }
            current = line.trim();
            info(current.equals(versionString)
                    ? "You are using the latest version of " + programName + ", " + versionString
                    : "Current official version is " + current + ", your version is " + versionString + ".");
        } catch (UnknownHostException ex) {
            error("Unknown host (proxy): " + ex.getMessage());
        } catch (IOException ex) {
            error("Problem getting current version: " + ex.getMessage());
        }
        return current != null && current.equals(versionString);
    }

    public interface EmergencyFixer {
        public void fix();

        public String getQuestion();

        public String getYesMessage();

        public String getNoMessage();

    }
}
