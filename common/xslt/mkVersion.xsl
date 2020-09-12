<?xml version="1.0" encoding="UTF-8"?>
<!--
Copyright (C) 2011, 2012, 2013, 2014, 2015 Bengt Martensson.

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
    <xsl:param name="version"/>
    <xsl:param name="url"/>
    <xsl:param name="appName"/>
    <xsl:param name="commitId"/>

    <xsl:output method="text"/>

    <xsl:template match="/version">/* This file was automatically generated, do not edit. Do not check in in version management. */

package <xsl:value-of select="@package"/>;

/**
 * This class contains version and license information and constants.
 */
public final class Version {
    /** Verbal description of the license of the current work. */
    public final static String licenseString = "<xsl:value-of select="translate(licenseString/., '&#xA;', '')"/>";

    /** Verbal description of licenses of third-party components. */
    public final static String thirdPartyString = "<xsl:value-of select="normalize-space(thirdPartyString/.)"/>";

    public final static String appName = "<xsl:value-of select='$appName'/>";
    public final static String version = "<xsl:value-of select='$version'/>";
    public final static String commitId = "<xsl:value-of select='$commitId'/>";
    public final static String versionString = appName + " version " + version;

    /** Project home page. */
    public final static String homepageUrl = "<xsl:value-of select='$url'/>";

    /** Documentation URL. */
    public final static String documentationUrl = "<xsl:value-of select='$url'/>" + "/" + appName + ".html";

    /** URL containing current official version as text. */
    public final static String currentVersionUrl = homepageUrl + "/downloads/" + appName + ".version";
    <xsl:apply-templates select="publicKey"/>
    public static void main(String[] args) {
        System.out.println(versionString);
        System.out.println(commitId);
    }

    private Version() {
    }
}
</xsl:template>

    <xsl:template match="publicKey">
    /** Author&quot;s public PGP key. */
    public final static String publicKey = <xsl:value-of select='.'/>;
</xsl:template>

</xsl:stylesheet>
