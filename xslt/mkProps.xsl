<?xml version="1.0" encoding="UTF-8"?>
<!--
Copyright (C) 2011, 2012, 2014 Bengt Martensson.

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
-->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
    <xsl:output method="text"/>

    <xsl:template match="/properties">
        <xsl:text>/* This file was automatically generated, do not edit. Do not check in in version management. */

package </xsl:text><xsl:value-of select="@package"/><xsl:text>;

import java.awt.Rectangle;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
</xsl:text>
        <xsl:apply-templates select="import"/>
        <xsl:text>
/**
 * This class handles the properties of the program, saved to a file between program invocations.
 */
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public final class Props {
    private final static boolean useXml = </xsl:text><xsl:value-of select="@useXml"/><xsl:text><![CDATA[;

    /**
     * Main routine for testing and debugging.
     * @param args filename
     */
    public static void main(String[] args) {
        String filename = args.length > 0 ? args[0] : null;
        try {
            Props p = new Props(filename);
            p.list();
            p.save();
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    private Properties props;
    private String filename;
    private String applicationHome;
    private boolean needSave;
    private boolean wasReset = false;
    private final boolean isWindows = System.getProperty("os.name").startsWith("Windows");
    private HashMap<String,ArrayList<IPropertyChangeListener>> changeListeners;

    /**
     * Sets up a Props instance from a given file name.
     * @param filename File to read from and, later, save to. Need not exist.
     * @param applicationHome
     */
    public Props(String filename, String applicationHome) {
        this.applicationHome = applicationHome;
        changeListeners = new HashMap<>(16);
        this.filename = filename;
        if (filename == null || filename.isEmpty()) {
            if (isWindows) {
                String dir = System.getenv("LOCALAPPDATA"); // Win Vista and later
                if (dir == null) {
                    dir = System.getenv("APPDATA"); // Win < Vista
                }
                if (dir != null) {
                    dir = dir + File.separator + Version.appName;
                    if (!(new File(dir)).isDirectory()) {
                        boolean status = (new File(dir)).mkdirs();
                        if (!status) {
                            System.err.println("Cannot create directory " + dir + ", using home directory instead.");
                        }
                    }
                }
                this.filename = (dir != null)
                        ? (dir + File.separator + Version.appName + ".properties.xml")
                        : System.getProperty("user.home") + File.separator + "." + Version.appName + ".properties.xml";
            } else {
                // Adhere to Freedesktop standard
                // http://standards.freedesktop.org/basedir-spec/basedir-spec-latest.html
                String base = System.getenv("XDG_CONFIG_HOME");
                if (base == null || base.isEmpty() || !(new File(base)).isAbsolute())
                    base = System.getProperty("user.home") + File.separator + ".config";
                File baseFile = new File(base + File.separator + Version.appName);
                if (!baseFile.exists()) {
                    boolean status = baseFile.mkdirs();
                    if (!status) {
                        System.err.println("Could not create directory " + baseFile.getAbsolutePath()
                                + ", saving properites in home direcory");
                        baseFile = new File(System.getProperty("user.home")); // emergency
                    }
                }
                this.filename = baseFile.getAbsolutePath() + File.separator + "properties.xml";
            }
        }

        needSave = false;
        props = new Properties();
        FileInputStream f = null;

        try {
            f = new FileInputStream(this.filename);
            if (useXml)
                props.loadFromXML(f);
            else
                props.load(f);
        } catch (FileNotFoundException ex) {
            System.err.println("Property File " + this.filename + " not found, using builtin defaults.");
            setupDefaults();
            needSave = true;
        } catch (IOException ex) {
            System.err.println("Property File " + this.filename + " could not be read, using builtin defaults.");
            setupDefaults();
            needSave = true;
        } finally {
            if (f != null) {
                try {
                    f.close();
                } catch (IOException ex) {
                    System.err.println(ex.getMessage());
                }
            }
        }
        setupDefaults();
    }

    /**
     * Sets up a Props instance from system default file name.
     * @param applicationHome
     */
    public Props(String applicationHome) {
        this(null, applicationHome);
    }

    private String ifWindows(String windows, String nonWindows) {
        return isWindows ? windows : nonWindows;
    }

    private void update(String key, String value) {
        if (!props.containsKey(key)) {
            if (value != null) {
                props.setProperty(key, value);
                needSave = true;
            }
        }
        changeListeners.put(key, new ArrayList<>(16));
    }

    public boolean getWasReset() {
        return wasReset;
    }

    private void setupDefaults() {
            ]]></xsl:text>
<xsl:apply-templates select="property" mode="defaults"/>
<xsl:text><![CDATA[
    }

    /**
     * Resets all properties to defaults.
     * This will probably leave the program in an inconsistent state,
     * so it should be restarted directly.
     */
    public void reset() {
        props = new Properties();
        changeListeners = new HashMap<>(16);
        setupDefaults();
        needSave = true;
        wasReset = true;
    }

    public String getFilename() {
        return filename;
    }

    private String mkPathRelative(String pathName) {
        File path = new File(pathName);
        String parent = path.getParent();
        return parent != null && parent.equals(applicationHome) ? path.getName() : pathName;
    }

    public String mkPathAbsolute(String path) {
        return new File(path).isAbsolute() ? path
               : new File(new File(applicationHome), path).getAbsolutePath();
    }

    /**
     * Save instance to given file name.
     *
     * @param filename Filename to be saved to.
     * @return success of operation
     * @throws IOException
     */
    public boolean save(File filename) throws IOException {
        if (!needSave && filename.getAbsolutePath().equals((new File(this.filename)).getAbsolutePath()))
            return false;

        FileOutputStream f;
        boolean success = false;
        try {
            f = new FileOutputStream(filename);
        } catch (FileNotFoundException ex) {
            throw (ex);
        }

        try {
            if (useXml)
                props.storeToXML(f, ]]></xsl:text>
<xsl:value-of select="@appName"/> + " properties, feel free to hand edit if desired");
            else
                props.store(f, <xsl:value-of select="@appName"/>
<xsl:text><![CDATA[ + " properties, feel free to hand edit if desired");

            success = true;
            needSave = false;
        } catch (IOException ex) {
            try {
                f.close();
            } catch (IOException exx) {
                System.err.println(exx.getMessage());
            }
            throw (ex);
        }

        try {
            f.close();
        } catch (IOException ex) {
            throw (ex);
        }

        return success;
    }

    /**
     * Saves the properties to the default, stored, file name.
     *
     * @return success of operation
     * @throws IOException
     */
    public String save() throws IOException {
        boolean result = save(new File(filename));
        return result ? filename : null;
    }

    // For debugging
    private void list() {
        props.list(System.err);
    }

    private void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        ArrayList<IPropertyChangeListener> presentListeners = changeListeners.get(propertyName);
        presentListeners.forEach((listener) -> {
            listener.propertyChange(propertyName, oldValue, newValue);
        });
    }

    private void addPropertyChangeListener(String propertyName, IPropertyChangeListener listener) {
        ArrayList<IPropertyChangeListener> presentListeners = changeListeners.get(propertyName);
        if (!presentListeners.contains(listener))
            presentListeners.add(listener);
    }

    private void removePropertyChangeListener(String propertyName, IPropertyChangeListener listener) {
        ArrayList<IPropertyChangeListener> presentListeners = changeListeners.get(propertyName);
        if (presentListeners.contains(listener))
            presentListeners.remove(listener);
    }
]]>
</xsl:text>
    <xsl:apply-templates select="property"/>
    <xsl:text>

    public interface IPropertyChangeListener {
        public void propertyChange(String name, Object oldValue, Object newValue);
    }
}
</xsl:text>
    </xsl:template>

    <xsl:template match="import">
        <xsl:text>import </xsl:text>
        <xsl:value-of select="@class"/>
        <xsl:text>;
</xsl:text>
    </xsl:template>

    <xsl:template match="property" mode="defaults">
        <xsl:text>        update("</xsl:text>
        <xsl:value-of select="@name"/>
        <xsl:text>", </xsl:text>
        <xsl:value-of select="@default"/>
        <xsl:text>);
</xsl:text>
    </xsl:template>

    <xsl:template match="@name" mode="capitalize">
        <xsl:value-of select="translate(substring(.,1,1), 'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ')"/>
        <xsl:value-of select="substring(.,2)"/>
    </xsl:template>

    <xsl:template match="@doc" mode="getter">
        <xsl:text>    /** @return </xsl:text>
        <xsl:value-of select="."/>
        <xsl:text> */</xsl:text>
    </xsl:template>

    <xsl:template match="@doc" mode="int-setter">
        <xsl:text>    /** @param n </xsl:text>
        <xsl:value-of select="."/>
        <xsl:text> */</xsl:text>
    </xsl:template>

    <xsl:template match="@doc" mode="boolean-setter">
        <xsl:text>    /** @param val </xsl:text>
        <xsl:value-of select="."/>
        <xsl:text> */</xsl:text>
    </xsl:template>

    <xsl:template match="@doc" mode="string-setter">
        <xsl:text>    /** @param str </xsl:text>
        <xsl:value-of select="."/>
        <xsl:text> */</xsl:text>
    </xsl:template>

    <xsl:template match="@doc" mode="rectangle-setter">
        <xsl:text>    /** @param bounds </xsl:text>
        <xsl:value-of select="."/>
        <xsl:text> */</xsl:text>
    </xsl:template>

    <xsl:template match="property[@type='int']">
    <xsl:apply-templates select="@doc" mode="getter"/>
    public int get<xsl:apply-templates select="@name" mode="capitalize"/>() {
        return Integer.parseInt(props.getProperty("<xsl:value-of select="@name"/>"));
    }

<xsl:apply-templates select="@doc" mode="int-setter"/>
    public void set<xsl:apply-templates select="@name" mode="capitalize"/>(int n) {
        int oldValue = Integer.parseInt(props.getProperty("<xsl:value-of select="@name"/>"));
        if (oldValue != n) {
            props.setProperty("<xsl:value-of select="@name"/>", Integer.toString(n));
            needSave = true;
            firePropertyChange("<xsl:value-of select="@name"/>", oldValue, n);
        }
    }

    public void add<xsl:apply-templates select="@name" mode="capitalize"/>ChangeListener(IPropertyChangeListener listener) {
        addPropertyChangeListener("<xsl:value-of select="@name"/>", listener);
    }

    public void remove<xsl:apply-templates select="@name" mode="capitalize"/>ChangeListener(IPropertyChangeListener listener) {
        removePropertyChangeListener("<xsl:value-of select="@name"/>", listener);
    }
</xsl:template>

    <xsl:template match="property[@type='double']">
    <xsl:apply-templates select="@doc" mode="getter"/>
    public double get<xsl:apply-templates select="@name" mode="capitalize"/>() {
        return Double.parseDouble(props.getProperty("<xsl:value-of select="@name"/>"));
    }

<xsl:apply-templates select="@doc" mode="int-setter"/>
    public void set<xsl:apply-templates select="@name" mode="capitalize"/>(double n) {
        double oldValue = Double.parseDouble(props.getProperty("<xsl:value-of select="@name"/>"));
        if (!IrCoreUtils.approximatelyEquals(n, oldValue, 0, 1E-12)) {
            props.setProperty("<xsl:value-of select="@name"/>", Double.toString(n));
            needSave = true;
            firePropertyChange("<xsl:value-of select="@name"/>", oldValue, n);
        }
    }

    public void add<xsl:apply-templates select="@name" mode="capitalize"/>ChangeListener(IPropertyChangeListener listener) {
        addPropertyChangeListener("<xsl:value-of select="@name"/>", listener);
    }

    public void remove<xsl:apply-templates select="@name" mode="capitalize"/>ChangeListener(IPropertyChangeListener listener) {
        removePropertyChangeListener("<xsl:value-of select="@name"/>", listener);
    }
</xsl:template>

    <xsl:template match="property[@type='boolean']">
    <xsl:apply-templates select="@doc" mode="getter"/>
    public boolean get<xsl:apply-templates select="@name" mode="capitalize"/>() {
        return Boolean.parseBoolean(props.getProperty("<xsl:value-of select="@name"/>"));
    }

<xsl:apply-templates select="@doc" mode="boolean-setter"/>
    public void set<xsl:apply-templates select="@name" mode="capitalize"/>(boolean val) {
        boolean oldValue = Boolean.parseBoolean(props.getProperty("<xsl:value-of select="@name"/>"));
        if (oldValue != val) {
            props.setProperty("<xsl:value-of select="@name"/>", Boolean.toString(val));
            needSave = true;
            firePropertyChange("<xsl:value-of select="@name"/>", oldValue, val);
        }
    }

    public void add<xsl:apply-templates select="@name" mode="capitalize"/>ChangeListener(IPropertyChangeListener listener) {
        addPropertyChangeListener("<xsl:value-of select="@name"/>", listener);
    }

    public void remove<xsl:apply-templates select="@name" mode="capitalize"/>ChangeListener(IPropertyChangeListener listener) {
        removePropertyChangeListener("<xsl:value-of select="@name"/>", listener);
    }
</xsl:template>

    <xsl:template match="property[@type='string']">
    <xsl:apply-templates select="@doc" mode="getter"/>
    public String get<xsl:apply-templates select="@name" mode="capitalize"/>() {
        return props.getProperty("<xsl:value-of select="@name"/>");
    }

<xsl:apply-templates select="@doc" mode="string-setter"/>
    public void set<xsl:apply-templates select="@name" mode="capitalize"/>(String str) {
        String oldValue = props.getProperty("<xsl:value-of select="@name"/>");
        String newValue = mkPathRelative(str);
        if (!oldValue.equals(newValue)) {
            props.setProperty("<xsl:value-of select="@name"/>", newValue);
            firePropertyChange("<xsl:value-of select="@name"/>", oldValue, newValue);
            needSave = true;
        }
    }

    public void add<xsl:apply-templates select="@name" mode="capitalize"/>ChangeListener(IPropertyChangeListener listener) {
        addPropertyChangeListener("<xsl:value-of select="@name"/>", listener);
    }

    public void remove<xsl:apply-templates select="@name" mode="capitalize"/>ChangeListener(IPropertyChangeListener listener) {
        removePropertyChangeListener("<xsl:value-of select="@name"/>", listener);
    }
</xsl:template>

    <xsl:template match="property[@type='URL']">

    <xsl:apply-templates select="@doc" mode="getter"/>
    public String get<xsl:apply-templates select="@name" mode="capitalize"/>() {
        return props.getProperty("<xsl:value-of select="@name"/>");
    }

    <xsl:apply-templates select="@doc" mode="string-setter"/>
    public void set<xsl:apply-templates select="@name" mode="capitalize"/>(String str) {
        String oldValue = props.getProperty("<xsl:value-of select="@name"/>");
        if (!oldValue.equals(str)) {
            props.setProperty("<xsl:value-of select="@name"/>", str);
            firePropertyChange("<xsl:value-of select="@name"/>", oldValue, str);
            needSave = true;
        }
    }

    public void add<xsl:apply-templates select="@name" mode="capitalize"/>ChangeListener(IPropertyChangeListener listener) {
        addPropertyChangeListener("<xsl:value-of select="@name"/>", listener);
    }

    public void remove<xsl:apply-templates select="@name" mode="capitalize"/>ChangeListener(IPropertyChangeListener listener) {
        removePropertyChangeListener("<xsl:value-of select="@name"/>", listener);
    }
</xsl:template>

    <xsl:template match="property[@type='rectangle']">
    <xsl:apply-templates select="@doc" mode="getter"/>
    public Rectangle get<xsl:apply-templates select="@name" mode="capitalize"/>() {
        String str = props.getProperty("<xsl:value-of select="@name"/>");
        if (str == null || str.isEmpty())
            return null;
        String[] arr = str.trim().split(" +");
        return new Rectangle(Integer.parseInt(arr[0]), Integer.parseInt(arr[1]),
                Integer.parseInt(arr[2]), Integer.parseInt(arr[3]));
    }

<xsl:apply-templates select="@doc" mode="rectangle-setter"/>
    public void set<xsl:apply-templates select="@name" mode="capitalize"/>(Rectangle bounds) {
        String oldValue = props.getProperty("<xsl:value-of select="@name"/>");
        if (bounds == null)
            return;
        String newValue = String.format("%d %d %d %d", bounds.x, bounds.y, bounds.width, bounds.height);
        if (!newValue.equals(oldValue)) {
            props.setProperty("<xsl:value-of select="@name"/>", newValue);
            needSave = true;
            firePropertyChange("<xsl:value-of select="@name"/>", oldValue, newValue);
        }
    }

    public void add<xsl:apply-templates select="@name" mode="capitalize"/>ChangeListener(IPropertyChangeListener listener) {
        addPropertyChangeListener("<xsl:value-of select="@name"/>", listener);
    }

    public void remove<xsl:apply-templates select="@name" mode="capitalize"/>ChangeListener(IPropertyChangeListener listener) {
        removePropertyChangeListener("<xsl:value-of select="@name"/>", listener);
    }
</xsl:template>
</xsl:stylesheet>
