<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
                xmlns:mods="http://www.loc.gov/mods/v3" version="1.0"
                xmlns:xlink="http://www.w3.org/1999/xlink">
    <xsl:output indent="yes" method="xml"/>

    <xsl:variable name="PLACEHOLDER">#PLACEHOLDER_PARENT_METADATA_VALUE#</xsl:variable>
    <xsl:template match="text()">
    </xsl:template>

    <!-- FUNCTIONS ==================================================== -->
    <!-- Performing and `@lang` attribute from any `mods` tag -->
    <xsl:template match="@lang">
        <xsl:variable name="translated">
            <xsl:choose>
                <xsl:when test="normalize-space(.)='fre'">fr</xsl:when>
                <xsl:when test="normalize-space(.)='eng'">en</xsl:when>
                <xsl:when test="normalize-space(.)='dut'">nl</xsl:when>
                <xsl:when test="normalize-space(.)='ger'">de</xsl:when>
                <xsl:when test="normalize-space(.)='spa'">es</xsl:when>
                <xsl:when test="normalize-space(.)='ita'">it</xsl:when>
                <xsl:when test="normalize-space(.)='gre'">el</xsl:when>
            </xsl:choose>
        </xsl:variable>
        <xsl:if test="$translated">
            <xsl:attribute name="lang">
                <xsl:value-of select="$translated"/>
            </xsl:attribute>
        </xsl:if>
    </xsl:template>
    <xsl:template name="valueOrDefault">
        <xsl:param name="value"/>
        <xsl:param name="defaultValue">
            <xsl:value-of select="$PLACEHOLDER"/>
        </xsl:param>
        <xsl:choose>
            <xsl:when test="$value">
                <xsl:value-of select="normalize-space($value)"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$defaultValue"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- ROOT ========================================================= -->
    <xsl:template match="//mods:mods">
        <xsl:element name="dim:dim">
            <xsl:apply-templates/>
        </xsl:element>
    </xsl:template>

    <!-- GENRE -> dc.type ============================================= -->
    <xsl:template match="/mods:mods/mods:genre">
        <xsl:variable name="value">
            <!-- todo : normaliser !!! -->
            <xsl:value-of select="normalize-space(.)"/>
        </xsl:variable>
        <xsl:element name="dim:field">
            <xsl:attribute name="mdschema">dc</xsl:attribute>
            <xsl:attribute name="element">type</xsl:attribute>
            <xsl:value-of select="normalize-space($value)"/>
        </xsl:element>
    </xsl:template>
    <!-- TITLE -> dc.title ============================================ -->
    <xsl:template match="/mods:mods/mods:titleInfo/mods:title">
        <xsl:element name="dim:field">
            <xsl:attribute name="mdschema">dc</xsl:attribute>
            <xsl:attribute name="element">title</xsl:attribute>
            <xsl:apply-templates select="@lang"/>
            <xsl:value-of select="normalize-space(.)"/>
        </xsl:element>
    </xsl:template>
    <!-- TITLE Alternative -> dc.title.alternative ============================================ -->
    <xsl:template match="/mods:mods/mods:titleInfo[@type='alternative']">
        <xsl:element name="dim:field">
            <xsl:attribute name="mdschema">dc</xsl:attribute>
            <xsl:attribute name="element">title</xsl:attribute>
            <xsl:attribute name="qualifier">alternative</xsl:attribute>
            <xsl:apply-templates select="@lang"/>
            <xsl:value-of select="normalize-space(.)"/>
        </xsl:element>
    </xsl:template>
    <!-- ABSTRACT -> dc.description.abstract ========================== -->
    <xsl:template match="/mods:mods/mods:abstract">
        <xsl:element name="dim:field">
            <xsl:attribute name="mdschema">dc</xsl:attribute>
            <xsl:attribute name="element">description</xsl:attribute>
            <xsl:attribute name="qualifier">abstract</xsl:attribute>
            <xsl:apply-templates select="@lang"/>
            <xsl:value-of select="normalize-space(.)"/>
        </xsl:element>
    </xsl:template>
    <!-- SUBJECT -> dc.subject ======================================== -->
    <xsl:template match="/mods:mods/mods:subject/mods:topic">
        <xsl:element name="dim:field">
            <xsl:attribute name="mdschema">dc</xsl:attribute>
            <xsl:attribute name="element">subject</xsl:attribute>
            <xsl:value-of select="normalize-space(.)"/>
        </xsl:element>
    </xsl:template>
    <!-- ORIGIN_INFO ================================================== -->
    <!--   * dateIssued -> dc.date.issued -->
    <xsl:template match="/mods:mods/mods:originInfo/mods:dateIssued">
        <xsl:element name="dim:field">
            <xsl:attribute name="mdschema">dc</xsl:attribute>
            <xsl:attribute name="element">date</xsl:attribute>
            <xsl:attribute name="qualifier">issued</xsl:attribute>
            <xsl:value-of select="normalize-space(.)"/>
        </xsl:element>
    </xsl:template>
    <!-- Publisher ====================================================== -->
    <xsl:template match="/mods:mods/mods:originInfo/mods:publisher">
        <xsl:element name="dim:field">
            <xsl:attribute name="mdschema">dc</xsl:attribute>
            <xsl:attribute name="element">publisher</xsl:attribute>
            <xsl:value-of select="normalize-space(.)"/>
        </xsl:element>
    </xsl:template>
    <!-- IDENTIFIER =================================================== -->
    <!--   * Manage `oer` identifier -->
    <!--   * Manage `issn` identifier -->
    <!--   * Manage `isbn` identifier -->
    <!--   * Manage `uri` identifier -->
    <!--   * Manage `other` identifier :: legacy application identifier (virtua, RERO) -->
   <xsl:template match="/mods:mods/mods:identifier[@type='legacyOER']">
        <xsl:element name="dim:field">
            <xsl:attribute name="mdschema">dc</xsl:attribute>
            <xsl:attribute name="element">identifier</xsl:attribute>
            <xsl:attribute name="qualifier">legacyOER</xsl:attribute>
            <xsl:value-of select="normalize-space(.)"/>
        </xsl:element>
    </xsl:template>
    <xsl:template match="/mods:mods/mods:identifier[@type='issn']">
        <xsl:element name="dim:field">
            <xsl:attribute name="mdschema">dc</xsl:attribute>
            <xsl:attribute name="element">identifier</xsl:attribute>
            <xsl:attribute name="qualifier">issn</xsl:attribute>
            <xsl:value-of select="normalize-space(.)"/>
        </xsl:element>
    </xsl:template>
    <xsl:template match="/mods:mods/mods:identifier[@type='isbn']">
        <xsl:element name="dim:field">
            <xsl:attribute name="mdschema">dc</xsl:attribute>
            <xsl:attribute name="element">identifier</xsl:attribute>
            <xsl:attribute name="qualifier">isbn</xsl:attribute>
            <xsl:value-of select="normalize-space(.)"/>
        </xsl:element>
    </xsl:template>
    <xsl:template match="/mods:mods/mods:identifier[@type='legacyUri']">
        <xsl:element name="dim:field">
            <xsl:attribute name="mdschema">dc</xsl:attribute>
            <xsl:attribute name="element">identifier</xsl:attribute>
            <xsl:attribute name="qualifier">legacyUri</xsl:attribute>
            <xsl:value-of select="normalize-space(.)"/>
        </xsl:element>
    </xsl:template>
    <xsl:template match="/mods:mods/mods:identifier[@type='local']">
        <xsl:element name="dim:field">
            <xsl:attribute name="mdschema">dc</xsl:attribute>
            <xsl:attribute name="element">identifier</xsl:attribute>
            <xsl:attribute name="qualifier">other</xsl:attribute>
            <xsl:value-of select="normalize-space(.)"/>
        </xsl:element>
    </xsl:template>
    <!-- Language ====================================================== -->
    <xsl:template match="/mods:mods/mods:language/mods:languageTerm[@type='code' and @authority='iso639-2b']">
        <xsl:element name="dim:field">
            <xsl:attribute name="mdschema">dc</xsl:attribute>
            <xsl:attribute name="element">language</xsl:attribute>
            <xsl:attribute name="qualifier">iso-639-2</xsl:attribute>
            <xsl:value-of select="normalize-space(.)"/>
        </xsl:element>
    </xsl:template>
    <!-- AUTHORS ====================================================== -->
    <xsl:template match="/mods:mods/mods:name">
        <xsl:element name="dim:field">
            <xsl:attribute name="mdschema">dc</xsl:attribute>
            <xsl:attribute name="element">contributor</xsl:attribute>
            <xsl:attribute name="qualifier">author</xsl:attribute>
            <xsl:value-of select="normalize-space(./mods:namePart)"/>
        </xsl:element>
    </xsl:template>
    <!-- Rights ====================================================== -->
    <xsl:template match="/mods:mods/mods:accessCondition[@type='useAndReproduction']">
        <xsl:element name="dim:field">
            <xsl:attribute name="mdschema">dc</xsl:attribute>
            <xsl:attribute name="element">rights</xsl:attribute>
            <xsl:value-of select="normalize-space(.)"/>
        </xsl:element>
    </xsl:template>
    <xsl:template match="/mods:mods/mods:accessCondition[@xlink:simpleLink]">
        <xsl:element name="dim:field">
            <xsl:attribute name="mdschema">dc</xsl:attribute>
            <xsl:attribute name="element">rights</xsl:attribute>
            <xsl:attribute name="qualifier">uri</xsl:attribute>
            <xsl:value-of select="normalize-space(.)"/>
        </xsl:element>
    </xsl:template>
    <!-- Format -> dc.format ====================================================== -->
    <xsl:template match="/mods:mods/mods:physicalDescription/mods:form">
        <xsl:element name="dim:field">
            <xsl:attribute name="mdschema">dc</xsl:attribute>
            <xsl:attribute name="element">format</xsl:attribute>
            <xsl:value-of select="normalize-space(.)"/>
        </xsl:element>
    </xsl:template>
    <!-- COLLECTION =================================================== -->
    <xsl:template match="/mods:mods/mods:note[@type='parentCollectionName']">
        <xsl:element name="dim:field">
            <xsl:attribute name="mdschema">publication</xsl:attribute>
            <xsl:attribute name="element">collection</xsl:attribute>
            <xsl:attribute name="qualifier">name</xsl:attribute>
            <xsl:value-of select="normalize-space(.)"/>
        </xsl:element>
    </xsl:template>
    <!-- LOM ====================================================== -->
    <xsl:template match="/mods:mods/mods:note[@type='typicalAgeRange']">
        <xsl:element name="dim:field">
            <xsl:attribute name="mdschema">lom</xsl:attribute>
            <xsl:attribute name="element">educational</xsl:attribute>
            <xsl:attribute name="qualifier">typicalAgeRange</xsl:attribute>
            <xsl:value-of select="normalize-space(.)"/>
        </xsl:element>
    </xsl:template>
</xsl:stylesheet>