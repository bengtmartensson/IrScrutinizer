<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:pom="http://maven.apache.org/POM/4.0.0" version="1.0">
    <xsl:output method="text"/>

    <xsl:template match="/">
        <xsl:value-of select="pom:project/pom:version"/>
   </xsl:template>

</xsl:stylesheet>
