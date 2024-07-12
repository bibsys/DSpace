<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
                xmlns:dc="http://purl.org/dc/elements/1.1/" version="1.0">
    <xsl:output indent="yes" method="xml"/>

    <xsl:template match="text()">
    </xsl:template>

    <!-- ROOT ========================================================= -->
    <xsl:template match="/*">
        <xsl:element name="dim:dim">
            <xsl:apply-templates/>
        </xsl:element>
    </xsl:template>

    <!-- DESCRIPTION =================================================== -->
    <xsl:template match="dc:description">
        <xsl:element name="dim:field">
            <xsl:attribute name="mdschema">dc</xsl:attribute>
            <xsl:attribute name="element">description</xsl:attribute>
            <xsl:value-of select="normalize-space(.)"/>
        </xsl:element>
    </xsl:template>

    <!-- RIGHTS ======================================================== -->
    <xsl:template match="dc:rights">
        <xsl:element name="dim:field">
            <xsl:attribute name="mdschema">dc</xsl:attribute>
            <xsl:attribute name="element">rights</xsl:attribute>
            <xsl:attribute name="qualifier">license</xsl:attribute>
            <xsl:value-of select="normalize-space(.)"/>
        </xsl:element>
    </xsl:template>

</xsl:stylesheet>