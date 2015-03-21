<?xml version="1.0" encoding="UTF-8"?>
<!--
Copyright (C) 2011, 2012, 2013, 2014 Bengt Martensson.

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

    <xsl:template match="/version">/* This file was automatically generated, do not edit. Do not check in in version management. */

package <xsl:value-of select="@package"/>;

/**
 * This class contains version and license information and constants.
 */
public class Version {
    /** Verbal description of the license of the current work. */
    public final static String licenseString = "<xsl:value-of select="translate(licenseString/., '&#xA;', '')"/>";

    /** Verbal description of licenses of third-party components. */
    public final static String thirdPartyString = "<xsl:value-of select="normalize-space(thirdPartyString/.)"/>";

    public final static String appName = "<xsl:value-of select='@appName'/>";
    public final static int mainVersion = <xsl:value-of select='@mainVersion'/>;
    public final static int subVersion = <xsl:value-of select='@subVersion'/>;
    public final static int subminorVersion = <xsl:value-of select='@subminorVersion'/>;
    public final static String versionSuffix = "<xsl:value-of select='@versionSuffix'/>";
    public final static String version = mainVersion + "." + subVersion + "." + subminorVersion + versionSuffix;
    public final static String versionString = appName + " version " + version;

    /** Project home page. */
    public final static String homepageUrl = "<xsl:value-of select='@homepageUrl'/>";

    /** URL containing current official version. */
    public final static String currentVersionUrl = homepageUrl + "/downloads/" + appName + ".version";

    private Version() {
    }

    public static void main(String[] args) {
        System.out.println(versionString);
    }
}
    </xsl:template>

</xsl:stylesheet>
