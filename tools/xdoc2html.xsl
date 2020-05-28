<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
    <xsl:output method="html"/>

    <!-- Default rule: copy verbatim -->
    <xsl:template match="node()|@*" >
      <xsl:copy>
         <xsl:apply-templates select="node()|@*"/>
      </xsl:copy>
    </xsl:template>

    <!-- Elements to be unwrapped -->
    <xsl:template match="section|header|title" >
         <xsl:apply-templates select="node()|@*"/>
    </xsl:template>

    <!-- Nuke comments -->
    <xsl:template match="comment()"/>

    <xsl:template match="/">
        <html>
            <head>
                <title>
                    <xsl:apply-templates select="/document/header"/>
                </title>
            </head>
            <body>
                <h1>
                    <xsl:apply-templates select="/document/header"/>
                </h1>
                <ul>
                    <xsl:apply-templates select="/document/body/section" mode="toc"/>
                </ul>
                <xsl:apply-templates select="/document/body/*"/>
            </body>
        </html>
    </xsl:template>

    <xsl:template match="warning">
        <div>
            <span>
                <xsl:attribute name="style">font-weight: bold;</xsl:attribute>
                Warning:
            </span>
            <em>
                <xsl:value-of select="."/>
            </em>
        </div>
    </xsl:template>

    <xsl:template match="note">
        <div>
            <span>
                <xsl:attribute name="style">font-weight: bold;</xsl:attribute>
                Note:
            </span>
            <em>
                <xsl:apply-templates/>
            </em>
        </div>
   </xsl:template>

   <xsl:template match="/document/body/table">
       <p>For the revision history of this document, see
           the original document on the
           <a><xsl:attribute name="href">http://www.harctoolbox.org</xsl:attribute>web site</a>.
       </p>
   </xsl:template>

    <xsl:template match="section" mode="toc">
        <li>
        <a><xsl:attribute name="href">#<xsl:value-of select="translate(title,' ','+')"/></xsl:attribute>
        <xsl:value-of select="title"/></a>
        </li>
        <ul>
            <xsl:apply-templates select="section" mode="toc"/>
        </ul>
    </xsl:template>

    <xsl:template match="body/section/title">
        <h2>
            <xsl:attribute name="id">
                <xsl:value-of select="translate(.,' ','+')"/>
            </xsl:attribute>
            <xsl:apply-templates/>
        </h2>
    </xsl:template>

    <xsl:template match="body/section/section/title">
        <h3>
            <xsl:attribute name="id">
                <xsl:value-of select="translate(.,' ','+')"/>
            </xsl:attribute>
            <xsl:apply-templates/>
        </h3>
    </xsl:template>

    <xsl:template match="body/section/section/section/title">
        <h4>
            <xsl:attribute name="id">
                <xsl:value-of select="translate(.,' ','+')"/>
            </xsl:attribute>
            <xsl:apply-templates/>
        </h4>
    </xsl:template>

    <xsl:template match="body/section/section/section/section/title">
        <h5>
            <xsl:attribute name="id">
                <xsl:value-of select="translate(.,' ','+')"/>
            </xsl:attribute>
            <xsl:apply-templates/>
        </h5>
    </xsl:template>

    <xsl:template match="strong">
        <span>
            <xsl:attribute name="style">font-weight: bold;</xsl:attribute>
            <xsl:apply-templates/>
        </span>
    </xsl:template>

    <xsl:template match="source">
        <pre>
            <xsl:apply-templates/>
        </pre>
    </xsl:template>

    <xsl:template match="dt">
        <dt>
            <xsl:attribute name="id">
                <xsl:value-of select="@id"/>
            </xsl:attribute>
            <b> <!-- I know, should be using CSS -->
                <xsl:apply-templates/>
            </b>
        </dt>
    </xsl:template>

    <xsl:template match="@href[not(starts-with(.,'http')
                                or starts-with(.,'#')
                                or starts-with(.,'IrScrutinizer')
                                or starts-with(.,'IrpTransmogrifier')
                                or starts-with(.,'Glossary'))]">
        <xsl:attribute name="href">
            <xsl:text>http://www.harctoolbox.org/</xsl:text>
            <xsl:value-of select="."/>
        </xsl:attribute>
    </xsl:template>

</xsl:stylesheet>
