<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
                xmlns:mods="http://www.loc.gov/mods/v3" version="1.0">
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
    <xsl:template match="/mods:mods/mods:genre[@authority='coar']">
        <xsl:variable name="docType">
            <xsl:choose>
                <xsl:when test="contains(@valueURI,'resource_type/c_bdcc')">text::thesis::master thesis</xsl:when>
            </xsl:choose>
        </xsl:variable>
        <xsl:if test="$docType">
            <xsl:element name="dim:field">
                <xsl:attribute name="mdschema">dc</xsl:attribute>
                <xsl:attribute name="element">type</xsl:attribute>
                <xsl:value-of select="normalize-space($docType)"/>
            </xsl:element>
        </xsl:if>
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
    <!--   * dateOther[@type='session'] -> masterthesis.session -->
    <xsl:template match="/mods:mods/mods:originInfo/mods:dateIssued">
        <xsl:element name="dim:field">
            <xsl:attribute name="mdschema">dc</xsl:attribute>
            <xsl:attribute name="element">date</xsl:attribute>
            <xsl:attribute name="qualifier">issued</xsl:attribute>
            <xsl:value-of select="normalize-space(.)"/>
        </xsl:element>
    </xsl:template>
    <xsl:template match="/mods:mods/mods:originInfo/mods:dateOther[@type='session']">
        <xsl:element name="dim:field">
            <xsl:attribute name="mdschema">masterthesis</xsl:attribute>
            <xsl:attribute name="element">session</xsl:attribute>
            <xsl:attribute name="lang">fr</xsl:attribute>
            <xsl:value-of select="normalize-space(.)"/>
        </xsl:element>
    </xsl:template>
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
        <xsl:variable name="mdschema">
            <xsl:choose>
                <xsl:when test="./mods:role/mods:roleTerm='author'">authors</xsl:when>
                <xsl:when test="./mods:role/mods:roleTerm='advisor'">advisors</xsl:when>
            </xsl:choose>
        </xsl:variable>
        <xsl:element name="dim:field">
            <xsl:attribute name="mdschema">dc</xsl:attribute>
            <xsl:attribute name="element">contributor</xsl:attribute>
            <xsl:attribute name="qualifier">
                <xsl:value-of select="./mods:role/mods:roleTerm"/>
            </xsl:attribute>
            <xsl:value-of select="normalize-space(./mods:namePart)"/>
        </xsl:element>
        <xsl:if test="$mdschema">
            <xsl:element name="dim:field">
                <xsl:attribute name="mdschema">
                    <xsl:value-of select="$mdschema"/>
                </xsl:attribute>
                <xsl:attribute name="element">institution</xsl:attribute>
                <xsl:attribute name="qualifier">name</xsl:attribute>
                <xsl:call-template name="valueOrDefault">
                    <xsl:with-param name="value" select="./mods:affiliation[1]"/>
                </xsl:call-template>
            </xsl:element>
            <xsl:element name="dim:field">
                <xsl:attribute name="mdschema">
                    <xsl:value-of select="$mdschema"/>
                </xsl:attribute>
                <xsl:attribute name="element">email</xsl:attribute>
                <xsl:call-template name="valueOrDefault">
                    <xsl:with-param name="value" select="./mods:nameIdentifier[@type='email']"/>
                </xsl:call-template>
            </xsl:element>
            <!-- special `authors` schema ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ -->
            <xsl:if test="$mdschema='authors'">
                <xsl:element name="dim:field">
                    <xsl:attribute name="mdschema">
                        <xsl:value-of select="$mdschema"/>
                    </xsl:attribute>
                    <xsl:attribute name="element">identifier</xsl:attribute>
                    <xsl:attribute name="qualifier">fgs</xsl:attribute>
                    <xsl:call-template name="valueOrDefault">
                        <xsl:with-param name="value" select="./mods:nameIdentifier[@type='fgs']"/>
                    </xsl:call-template>
                </xsl:element>
                <xsl:element name="dim:field">
                    <xsl:attribute name="mdschema">masterthesis</xsl:attribute>
                    <xsl:attribute name="element">degree</xsl:attribute>
                    <xsl:attribute name="qualifier">label</xsl:attribute>
                    <xsl:call-template name="valueOrDefault">
                        <xsl:with-param name="value" select="./mods:nameIdentifier[@type='degree_label']"/>
                    </xsl:call-template>
                </xsl:element>
                <xsl:element name="dim:field">
                    <xsl:attribute name="mdschema">masterthesis</xsl:attribute>
                    <xsl:attribute name="element">degree</xsl:attribute>
                    <xsl:attribute name="qualifier">code</xsl:attribute>
                    <xsl:call-template name="valueOrDefault">
                        <xsl:with-param name="value" select="./mods:nameIdentifier[@type='degree_code']"/>
                    </xsl:call-template>
                </xsl:element>
            </xsl:if>
        </xsl:if>
    </xsl:template>
    <!-- RELATEDITEM[@otherType="root_degree"] -> masterthesis.rootdegree -->
    <xsl:template match="/mods:mods/mods:relatedItem[@otherType='root_degree']/mods:titleInfo">
        <xsl:element name="dim:field">
            <xsl:attribute name="mdschema">masterthesis</xsl:attribute>
            <xsl:attribute name="element">rootdegree</xsl:attribute>
            <xsl:attribute name="qualifier">label</xsl:attribute>
            <xsl:call-template name="valueOrDefault">
                <xsl:with-param name="value" select="./mods:title"/>
            </xsl:call-template>
        </xsl:element>
        <xsl:element name="dim:field">
            <xsl:attribute name="mdschema">masterthesis</xsl:attribute>
            <xsl:attribute name="element">rootdegree</xsl:attribute>
            <xsl:attribute name="qualifier">code</xsl:attribute>
            <xsl:call-template name="valueOrDefault">
                <xsl:with-param name="value" select="./mods:partName"/>
            </xsl:call-template>
        </xsl:element>
    </xsl:template>
    <!-- RELATEDITEM[@otherType="root_degree"] -> masterthesis.rootdegree -->
    <xsl:template match="/mods:mods/mods:relatedItem[@otherType='faculty']/mods:titleInfo/mods:title">
        <xsl:element name="dim:field">
            <xsl:attribute name="mdschema">masterthesis</xsl:attribute>
            <xsl:attribute name="element">faculty</xsl:attribute>
            <xsl:attribute name="qualifier">name</xsl:attribute>
            <xsl:value-of select="normalize-space(.)"/>
        </xsl:element>
    </xsl:template>
    <!-- RELATEDITEM[@otherType="note"] -> dc.description.[tag|provenance] -->
    <xsl:template match="/mods:mods/mods:relatedItem[@otherType='note']">
        <xsl:variable name="type" select="normalize-space(./mods:titleInfo/mods:title)"/>
        <xsl:variable name="creator" select="normalize-space(./mods:name/mods:namePart[1])"/>
        <xsl:variable name="date" select="normalize-space(./mods:originInfo/mods:dateCreated)"/>
        <xsl:variable name="comment" select="normalize-space(./mods:note[1])"/>
        <xsl:variable name="note-content">
            <xsl:if test="$type">
                <xsl:value-of select="concat('[', $type, '] ')"/>
            </xsl:if>
            <xsl:if test="$creator">
                <xsl:value-of select="concat('by ', $creator, ' ')"/>
            </xsl:if>
            <xsl:if test="$date">
                <xsl:value-of select="concat('on ', $date, ' ')"/>
            </xsl:if>
            <xsl:if test="$comment">
                <xsl:if test="$type or $creator or $date">
                    <xsl:text>:: </xsl:text>
                </xsl:if>
                <xsl:value-of select="$comment"/>
            </xsl:if>
        </xsl:variable>
        <xsl:element name="dim:field">
            <xsl:attribute name="mdschema">dc</xsl:attribute>
            <xsl:attribute name="element">description</xsl:attribute>
            <xsl:attribute name="qualifier">provenance</xsl:attribute>
            <xsl:value-of select="$note-content"/>
        </xsl:element>
        <xsl:element name="dim:field">
            <xsl:attribute name="mdschema">dc</xsl:attribute>
            <xsl:attribute name="element">description</xsl:attribute>
            <xsl:attribute name="qualifier">tag</xsl:attribute>
            <xsl:value-of select="$type"/>
        </xsl:element>
    </xsl:template>
</xsl:stylesheet>
