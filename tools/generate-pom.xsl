<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet xmlns:pom="http://maven.apache.org/POM/4.0.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                      http://maven.apache.org/xsd/maven-4.0.0.xsd"
                version="1.0">
    <xsl:output method="xml"/>

    <!--xsl:variable name="mainPomDocument" select="document('../pom.xml')"/-->
    <!--xsl:variable name="IrScrutinizerVersion" select="document('../IrScrutinizer/pom.xml')/project/version/text()"/-->
    <xsl:variable name="IrScrutinizerVersion" select="pattar"/>

    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="/">
        <xsl:comment>This is a generated file, do not edit.</xsl:comment>
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="pom:properties/pom:IrScrutinizer.version">
        <xsl:copy>
        <xsl:apply-templates select="document('../IrScrutinizer/pom.xml')/pom:project/pom:version/text()"/>
        </xsl:copy>
    </xsl:template>
    <xsl:template match="pom:properties/pom:IrpMaster.version">
        <xsl:copy>
        <xsl:apply-templates select="document('../IrpMaster/pom.xml')/pom:project/pom:version/text()"/>
        </xsl:copy>
    </xsl:template>
    <xsl:template match="pom:properties/pom:Girr.version">
        <xsl:copy>
        <xsl:apply-templates select="document('../Girr/pom.xml')/pom:project/pom:version/text()"/>
        </xsl:copy>
    </xsl:template>
    <xsl:template match="pom:properties/pom:Jirc.version">
        <xsl:copy>
        <xsl:apply-templates select="document('../Jirc/pom.xml')/pom:project/pom:version/text()"/>
        </xsl:copy>
    </xsl:template>
    <xsl:template match="pom:properties/pom:HarcHardware.version">
        <xsl:copy>
        <xsl:apply-templates select="document('../HarcHardware/pom.xml')/pom:project/pom:version/text()"/>
        </xsl:copy>
    </xsl:template>

    <!--xsl:template match="pom:artifactItem[pom:artifactId[text()='IrpMaster']]/pom:version
| pom:dependency[pom:artifactId[text()='IrpMaster']]/pom:version">
        <xsl:apply-templates select="document('../IrpMaster/pom.xml')/pom:project/pom:version"/>
    </xsl:template>

    <xsl:template match="pom:artifactItem[pom:artifactId[text()='Jirc']]/pom:version
| pom:dependency[pom:artifactId[text()='Jirc']]/pom:version">
        <xsl:apply-templates select="document('../Jirc/pom.xml')/pom:project/pom:version"/>
    </xsl:template>

    <xsl:template match="pom:artifactItem[pom:artifactId[text()='Girr']]/pom:version
| pom:dependency[pom:artifactId[text()='Girr']]/pom:version">
        <xsl:apply-templates select="document('../Girr/pom.xml')/pom:project/pom:version"/>
    </xsl:template>

    <xsl:template match="pom:artifactItem[pom:artifactId[text()='HarctoolboxBundle']]/pom:version
| pom:dependency[pom:artifactId[text()='HarctoolboxBundle']]/pom:version
| pom:parent/pom:version">
        <xsl:apply-templates select="document('../pom.xml')/pom:project/pom:version"/>
    </xsl:template-->


</xsl:stylesheet>
