<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:pom="http://maven.apache.org/POM/4.0.0" version="1.0">
    <xsl:output method="text"/>

    <xsl:param name="SYSTEM"/>

    <xsl:template match="/">
        <xsl:apply-templates select="pom:project/pom:properties"/>
   </xsl:template>

   <xsl:template match="pom:properties">
       <xsl:value-of select="pom:bundledjdk_url_sans_file"/>
       <xsl:choose>
            <xsl:when test="$SYSTEM = 'linux'">
                <xsl:value-of select="//pom:project/pom:properties/pom:bundledjdk.linux"/>
            </xsl:when>
            <xsl:when test="$SYSTEM = 'windows'">
                <xsl:value-of select="//pom:project/pom:properties/pom:bundledjdk.windows"/>
            </xsl:when>
            <xsl:when test="$SYSTEM = 'mac'">
                <xsl:value-of select="//pom:project/pom:properties/pom:bundledjdk.mac"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:text>ERROR</xsl:text>
            </xsl:otherwise>
        </xsl:choose>
   </xsl:template>
</xsl:stylesheet>
