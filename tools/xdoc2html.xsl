<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
    <xsl:output method="html"/>

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
                    <xsl:apply-templates select="/document/body/section[position()>1]" mode="toc"/>
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
            <!--xsl:value-of select="."/-->
            </em>
            </div>
   </xsl:template>

    <xsl:template match="/document/body/section[position()=1]">
      <p>For the revision history of this document, see
      <a><xsl:attribute name="href">http://www.harctoolbox.org</xsl:attribute>the original document on the web site</a>.
    </p>
    </xsl:template>
    
    <!--xsl:template match="body/section[position()=1]/title" mode="toc">sfsfdfd</xsl:template-->
    
    <!--xsl:template match="/document/body/section[position()>1]">
    <xsl:apply-templates/>
    </xsl:template-->

    <xsl:template match="body/section[position()>1]/title">
        <a><xsl:attribute name="name"><xsl:value-of select="translate(.,' ','+')"/></xsl:attribute></a>
        <h2><xsl:value-of select="."/></h2>
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

    <xsl:template match="body/section/section/title">
        <a><xsl:attribute name="name"><xsl:value-of select="translate(.,' ','+')"/></xsl:attribute></a>
        <h3><xsl:value-of select="."/></h3>
    </xsl:template>

    <xsl:template match="body/section/section/section/title">
        <a><xsl:attribute name="name"><xsl:value-of select="translate(.,' ','+')"/></xsl:attribute></a>
        <h4><xsl:value-of select="."/></h4>
    </xsl:template>

    <xsl:template match="body/section/section/section/section/title">
        <h5><xsl:value-of select="."/></h5>
    </xsl:template>

    <xsl:template match="p">
        <p>
            <xsl:apply-templates/>
        </p>
    </xsl:template>

     <xsl:template match="strong">
         <span>
                <xsl:attribute name="style">font-weight: bold;</xsl:attribute>
            <xsl:value-of select="."/>
        </span>
    </xsl:template>

    <xsl:template match="code">
         <code>
              <xsl:apply-templates/>
         </code>
    </xsl:template>

    <xsl:template match="source">
         <pre>
              <xsl:apply-templates/>
         </pre>
    </xsl:template>

    <xsl:template match="a">
        <a>
            <xsl:attribute name="href"><xsl:value-of select="@href"/></xsl:attribute>
            <xsl:value-of select="."/>
        </a>
    </xsl:template>

    <xsl:template match="img">
        <img>
            <xsl:attribute name="src"><xsl:value-of select="@src"/></xsl:attribute>
            <xsl:attribute name="alt"><xsl:value-of select="@alt"/></xsl:attribute>
        </img>
    </xsl:template>

    <xsl:template match="ol">
        <ol><xsl:apply-templates/></ol>
    </xsl:template>

    <xsl:template match="li">
        <li><xsl:apply-templates/></li>
    </xsl:template>

</xsl:stylesheet>
