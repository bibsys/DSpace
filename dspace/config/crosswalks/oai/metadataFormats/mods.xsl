<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        xmlns:doc="http://www.lyncode.com/xoai"
        xmlns:mods="http://www.loc.gov/mods/v3"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xmlns:xlink="http://www.w3.org/1999/xlink"
        version="1.0" xmlns:xl="http://www.w3.org/1999/XSL/Transform"
        xsi:schemaLocation="http://www.loc.gov/mods/v3 http://www.loc.gov/standards/mods/v3/mods-3-1.xsd">
	<xsl:output omit-xml-declaration="yes" method="xml" indent="yes" />

    <xsl:variable name="EMPTY_VALUE" select="'#PLACEHOLDER_PARENT_METADATA_VALUE#'"/>

    <!-- ROOT DOCUMENT ============================================================================================= -->
    <xsl:template match="/">
        <xsl:variable name="documentType" select="doc:metadata/doc:element[@name='dc']/doc:element[@name='type']/doc:element[@name='maintype']/doc:element/doc:field[@name='value'][1]"/>
		<mods:mods>
            <!-- AUTHORS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ -->
            <xsl:apply-templates select="doc:metadata/doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name='author']/doc:element/doc:field[@name='value']"/>
            <xsl:apply-templates select="doc:metadata/doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name='advisor']/doc:element/doc:field[@name='value']"/>
            <!-- BASIC FIELDS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ -->
            <xsl:apply-templates select="doc:metadata/doc:element[@name='dc']/doc:element[@name='title']"/>
            <xsl:apply-templates select="doc:metadata/doc:element[@name='dc']/doc:element[@name='description']/doc:element[@name='abstract']"/>
            <xsl:apply-templates select="doc:metadata/doc:element[@name='dc']/doc:element[@name='language']/doc:element[@name='iso']"/>
            <xsl:apply-templates select="doc:metadata/doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name='available']"/>
            <xsl:apply-templates select="doc:metadata/doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name='accessioned']"/>
            <xsl:apply-templates select="doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element"/>
            <xsl:apply-templates select="doc:metadata/doc:element[@name='dc']/doc:element[@name='subject']/doc:element/doc:field[@name='value']"/>
            <xsl:apply-templates select="doc:metadata/doc:element[@name='dc']/doc:element[@name='subject']/doc:element[@name!='internal']/doc:element/doc:field[@name='value']"/>
            <xsl:apply-templates select="doc:metadata/doc:element[@name='dc']/doc:element[@name='relation']/doc:element[@name='url']"/>
            <xsl:apply-templates select="doc:metadata/doc:element[@name='dc']/doc:element[@name='relation']/doc:element[@name='dataset']"/>
            <xsl:apply-templates select="doc:metadata/doc:element[@name='oairecerif']/doc:element[@name='affiliation']/doc:element[@name='orgunit']/doc:element/doc:field[@name='value']"/>
            <!-- FUNDING ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ -->
            <xsl:apply-templates select="doc:metadata/doc:element[@name='funding']/doc:element[@name='organisation']/doc:element/doc:field[@name='value']"/>
            <!-- ACCESS CONDITIONS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ -->
            <xsl:apply-templates select="doc:metadata/doc:element[@name='dcterms']/doc:element[@name='accessRights']"/>

            <!-- SPECIFIC FIELDS DEPENDING ON MAIN DOCUMENT TYPE ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ -->
            <xsl:choose>
                <xsl:when test="$documentType='text::book'"> <!-- book ............................................. -->
                    <mods:genre authority="coar" valueURI="http://purl.org/coar/resource_type/c_2f33">book</mods:genre>
                    <xsl:call-template name="originInfo">
                        <xsl:with-param name="date" select="doc:metadata/doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name='issued']/doc:element/doc:field[@name='value']/text()"/>
                        <xsl:with-param name="editorName" select="doc:metadata/doc:element[@name='publication']/doc:element[@name='editor']/doc:element[@name='name']/doc:element/doc:field[@name='value']/text()"/>
                        <xsl:with-param name="editorLocation" select="doc:metadata/doc:element[@name='publication']/doc:element[@name='editor']/doc:element[@name='location']/doc:element/doc:field[@name='value']/text()"/>
                        <xsl:with-param name="editionStatement" select="doc:metadata/doc:element[@name='publication']/doc:element[@name='editionStatement']/doc:element/doc:field[@name='value']/text()"/>
                    </xsl:call-template>
                    <xsl:apply-templates select="doc:metadata/doc:element[@name='publication']/doc:element[@name='publicationStatus']"/>
                    <xsl:apply-templates select="doc:metadata/doc:element[@name='publication']/doc:element[@name='numberOfPages']"/>
                    <xsl:apply-templates select="doc:metadata/doc:element[@name='publication']/doc:element[@name='book']/doc:element[@name='peerReviewed']"/>
                    <xsl:call-template name="collectionHost"/>
                </xsl:when>
                <xsl:when test="$documentType='text::book-part'"> <!-- book-part ................................... -->
                    <mods:genre authority="coar" valueURI="http://purl.org/coar/resource_type/c_3248">book part</mods:genre>
                    <xsl:call-template name="originInfo">
                        <xsl:with-param name="date" select="doc:metadata/doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name='issued']/doc:element/doc:field[@name='value']/text()"/>
                    </xsl:call-template>
                    <xsl:apply-templates select="doc:metadata/doc:element[@name='publication']/doc:element[@name='publicationStatus']"/>
                    <xsl:call-template name="hostDocument"/>
                </xsl:when>
                <xsl:when test="$documentType='text::conference-speech'"> <!-- speech .............................. -->
                    <mods:genre authority="coar" valueURI="http://purl.org/coar/resource_type/c_c94f">conference output</mods:genre>
                    <xsl:apply-templates select="doc:metadata/doc:element[@name='publication']/doc:element[@name='isAbstract']"/>
                    <xsl:call-template name="originInfo">
                        <xsl:with-param name="date" select="doc:metadata/doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name='issued']/doc:element/doc:field[@name='value']/text()"/>
                    </xsl:call-template>
                    <xsl:apply-templates select="doc:metadata/doc:element[@name='publication']/doc:element[@name='publicationStatus']"/>
                    <xsl:call-template name="conferenceData"/>
                    <xsl:call-template name="hostDocument"/>
                    <xsl:call-template name="serialHostDocument"/>
                </xsl:when>
                <!-- TODO :: TEST it -->
                <xsl:when test="$documentType='text::journal-article'"> <!-- article ............................... -->
                    <mods:genre authority="coar" valueURI="http://purl.org/coar/resource_type/c_6501">journal article</mods:genre>
                    <xsl:call-template name="originInfo">
                        <xsl:with-param name="date" select="doc:metadata/doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name='issued']/doc:element/doc:field[@name='value']/text()"/>
                        <xsl:with-param name="editorName" select="doc:metadata/doc:element[@name='publication']/doc:element[@name='editor']/doc:element[@name='name']/doc:element/doc:field[@name='value']/text()"/>
                        <xsl:with-param name="editorLocation" select="doc:metadata/doc:element[@name='publication']/doc:element[@name='editor']/doc:element[@name='location']/doc:element/doc:field[@name='value']/text()"/>
                    </xsl:call-template>
                    <xsl:apply-templates select="doc:metadata/doc:element[@name='publication']/doc:element[@name='publicationStatus']"/>
                    <xsl:apply-templates select="doc:metadata/doc:element[@name='publication']/doc:element[@name='serial']/doc:element[@name='peerReviewed']"/>
                    <xsl:call-template name="serialHostDocument"/>
                </xsl:when>
                <!-- TODO :: TEST it -->
                <xsl:when test="$documentType='text::report'"> <!-- report ......................................... -->
                    <mods:genre authority="coar" valueURI="http://purl.org/coar/resource_type/c_93fc">report</mods:genre>
                    <xsl:call-template name="originInfo">
                        <xsl:with-param name="date" select="doc:metadata/doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name='issued']/doc:element/doc:field[@name='value']/text()"/>
                        <xsl:with-param name="editorName" select="doc:metadata/doc:element[@name='publication']/doc:element[@name='editor']/doc:element[@name='name']/doc:element/doc:field[@name='value']/text()"/>
                        <xsl:with-param name="editorLocation" select="doc:metadata/doc:element[@name='publication']/doc:element[@name='editor']/doc:element[@name='location']/doc:element/doc:field[@name='value']/text()"/>
                    </xsl:call-template>
                    <xsl:apply-templates select="doc:metadata/doc:element[@name='publication']/doc:element[@name='numberOfPages']"/>
                    <xsl:call-template name="reportReference"/>
                </xsl:when>
                <xsl:when test="$documentType='text::working-paper'"> <!-- working-paper ........................... -->
                    <mods:genre authority="coar" valueURI="http://purl.org/coar/resource_type/c_8042">working paper</mods:genre>
                    <xsl:apply-templates select="doc:metadata/doc:element[@name='publication']/doc:element[@name='numberOfPages']"/>
                    <xsl:call-template name="collectionHost"/>
                </xsl:when>
                <xsl:when test="$documentType='text::patent'"> <!-- patent ......................................... -->
                    <mods:genre authority="coar" valueURI="http://purl.org/coar/resource_type/c_15cd">patent</mods:genre>
                    <xsl:call-template name="originInfo">
                        <xsl:with-param name="date" select="doc:metadata/doc:element[@name='crispatent']/doc:element[@name='deposit']/doc:element[@name='date']/doc:element/doc:field[@name='value']/text()"/>
                        <xsl:with-param name="editorName" select="doc:metadata/doc:element[@name='crispatent']/doc:element[@name='patentOffice']/doc:element/doc:field[@name='value']/text()"/>
                    </xsl:call-template>
                    <xsl:apply-templates select="doc:metadata/doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name='submitter']"/>
                </xsl:when>
                <xsl:when test="$documentType='text::thesis'"> <!-- dissertation ................................... -->
                    <mods:genre authority="coar" valueURI="http://purl.org/coar/resource_type/c_db06">doctoral thesis</mods:genre>
                    <!-- TODO :: Write it -->
                </xsl:when>
            </xsl:choose>
         </mods:mods>
	</xsl:template>

    <!-- AUTHORS SPECIFIC TEMPLATES ================================================================================ -->
    <xsl:template match="doc:metadata/doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name='author']/doc:element/doc:field[@name='value']">
        <xsl:variable name="pos" select="position()"/>
        <xsl:variable name="role" select="//doc:metadata/doc:element[@name='authors']/doc:element[@name='role']/doc:element/doc:field[@name='value'][$pos]" />
        <xsl:variable name="institution" select="//doc:metadata/doc:element[@name='authors']/doc:element[@name='institution']/doc:element[@name='code']/doc:element/doc:field[@name='value'][$pos]" />
        <xsl:variable name="orcidID" select="//doc:metadata/doc:element[@name='authors']/doc:element[@name='identifier']/doc:element[@name='orcid']/doc:element/doc:field[@name='value'][$pos]" />
        <mods:name>
            <mods:namePart><xsl:value-of select="." /></mods:namePart>
            <xsl:call-template name="author-affiliation">
                <xsl:with-param name="name" select="$institution"/>
            </xsl:call-template>
            <xsl:call-template name="author-identifier">
                <xsl:with-param name="identifier" select="$orcidID"/>
                <xsl:with-param name="type" select="'orcid'"/>
            </xsl:call-template>
            <xsl:call-template name="author-role">
                <xsl:with-param name="name" select="$role"/>
            </xsl:call-template>
        </mods:name>
    </xsl:template>
    <xsl:template match="doc:metadata/doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name='advisor']/doc:element/doc:field[@name='value']">
        <xsl:variable name="pos" select="position()"/>
        <xsl:variable name="institution" select="//doc:metadata/doc:element[@name='advisors']/doc:element[@name='institution']/doc:element[@name='code']/doc:element/doc:field[@name='value'][$pos]"/>
        <xsl:variable name="orcidID" select="//doc:metadata/doc:element[@name='advisors']/doc:element[@name='identifier']/doc:element[@name='orcid']/doc:element/doc:field[@name='value'][$pos]"/>
        <mods:name>
            <mods:namePart><xsl:value-of select="." /></mods:namePart>
            <xsl:call-template name="author-affiliation">
                <xsl:with-param name="name" select="$institution"/>
            </xsl:call-template>
            <xsl:call-template name="author-identifier">
                <xsl:with-param name="identifier" select="$orcidID"/>
                <xsl:with-param name="type" select="'orcid'"/>
            </xsl:call-template>
            <mods:role>
                <mods:roleTerm type="text" authroity="marcrelator">degree supervisor</mods:roleTerm>
                <mods:roleTerm type="code" authority="marcrelator">dgs</mods:roleTerm>
                <mods:roleTerm valueURI="http://id.loc.gov/vocabulary/relators/dgs"/>
            </mods:role>
        </mods:name>
    </xsl:template>
    <xsl:template name="author-affiliation">
        <xsl:param name="name"/>
        <xsl:if test="$name!=$EMPTY_VALUE">
            <mods:affiliation>
                <xsl:choose>
                    <xsl:when test="$name='UCLouvain'">
                        <xsl:attribute name="authority">ROR</xsl:attribute>
                        <xsl:attribute name="valueURI">https://ror.org/02495e989</xsl:attribute>
                    </xsl:when>
                    <xsl:when test="$name='UNamur'">
                        <xsl:attribute name="authority">ROR</xsl:attribute>
                        <xsl:attribute name="valueURI">https://ror.org/03d1maw17</xsl:attribute>
                    </xsl:when>
                    <xsl:when test="$name='USLB' or $name='USL-B'">
                        <xsl:attribute name="authority">ROR</xsl:attribute>
                        <xsl:attribute name="valueURI">https://ror.org/02ygek028</xsl:attribute>
                    </xsl:when>
                </xsl:choose>
                <xsl:value-of select="$name"/>
            </mods:affiliation>
        </xsl:if>
    </xsl:template>
    <xsl:template name="author-role">
        <xsl:param name="name"/>
        <xsl:if test="$name!=$EMPTY_VALUE">
            <mods:role>
                <xsl:choose>
                    <!-- author, co-author -->
                    <xsl:when test="$name='author' or $name='co_first_author' or $name='co_last_author'">
                        <mods:roleTerm type="text" authroity="marcrelator">author</mods:roleTerm>
                        <mods:roleTerm type="code" authority="marcrelator">aut</mods:roleTerm>
                        <mods:roleTerm valueURI="http://id.loc.gov/vocabulary/relators/aut"/>
                    </xsl:when>
                    <!-- collaborator -->
                    <xsl:when test="$name='collaborator'">
                        <mods:roleTerm type="text" authroity="marcrelator">contributor</mods:roleTerm>
                        <mods:roleTerm type="code" authority="marcrelator">ctb</mods:roleTerm>
                        <mods:roleTerm valueURI="http://id.loc.gov/vocabulary/relators/ctb"/>
                    </xsl:when>
                    <!-- scientific director/editor -->
                    <xsl:when test="$name='scientific_director_editor'">
                        <mods:roleTerm type="text" authroity="marcrelator">editorial director</mods:roleTerm>
                        <mods:roleTerm type="code" authority="marcrelator">edd</mods:roleTerm>
                        <mods:roleTerm valueURI="http://id.loc.gov/vocabulary/relators/edd"/>
                    </xsl:when>
                    <!-- translator -->
                    <xsl:when test="$name='translator'">
                        <mods:roleTerm type="text" authroity="marcrelator">translator</mods:roleTerm>
                        <mods:roleTerm type="code" authority="marcrelator">trl</mods:roleTerm>
                        <mods:roleTerm valueURI="http://id.loc.gov/vocabulary/relators/trl"/>
                    </xsl:when>
                    <!-- writer of preface -->
                    <xsl:when test="$name='preface_writer'">
                        <mods:roleTerm type="text" authroity="marcrelator">writer of preface</mods:roleTerm>
                        <mods:roleTerm type="code" authority="marcrelator">wpr</mods:roleTerm>
                        <mods:roleTerm valueURI="http://id.loc.gov/vocabulary/relators/wpr"/>
                    </xsl:when>
                    <!-- inventor -->
                    <xsl:when test="$name='inventor'">
                        <mods:roleTerm type="text" authroity="marcrelator">inventor</mods:roleTerm>
                        <mods:roleTerm type="code" authority="marcrelator">inv</mods:roleTerm>
                        <mods:roleTerm valueURI="http://id.loc.gov/vocabulary/relators/inv"/>
                    </xsl:when>
                    <!-- other case ? -->
                    <xsl:otherwise>
                        <mods:roleTerm type="text"><xsl:value-of select="."/></mods:roleTerm>
                    </xsl:otherwise>
                </xsl:choose>
            </mods:role>
        </xsl:if>
    </xsl:template>
    <xsl:template name="author-identifier">
        <xsl:param name="identifier"/>
        <xsl:param name="type"/>
        <xsl:if test="$identifier!=$EMPTY_VALUE">
            <mods:nameIdentifier>
                <xsl:choose>
                    <xsl:when test="$type='orcid'">
                        <xsl:attribute name="type"><xsl:value-of select="$type"/></xsl:attribute>
                        <xsl:attribute name="typeURI">http://id.loc.gov/vocabulary/identifiers/orcid</xsl:attribute>
                        <xsl:choose>
                            <xsl:when test="contains($identifier, 'orcid.org/')"><xsl:value-of select="substring-after($identifier, 'orcid.org/')"/></xsl:when>
                            <xsl:otherwise><xsl:value-of select="$identifier"/></xsl:otherwise>
                        </xsl:choose>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:if test="string-length($type) > 0">
                            <xsl:attribute name="type"><xsl:value-of select="$type"/></xsl:attribute>
                        </xsl:if>
                        <xsl:value-of select="$identifier"/>
                    </xsl:otherwise>
                </xsl:choose>
            </mods:nameIdentifier>
        </xsl:if>
    </xsl:template>
    <!-- AFFILIATIONS ============================================================================================== -->
    <xsl:template match="doc:metadata/doc:element[@name='oairecerif']/doc:element[@name='affiliation']/doc:element[@name='orgunit']/doc:element/doc:field[@name='value']">
        <xsl:variable name="pos" select="position()"/>
        <xsl:variable name="entityName" select="//doc:metadata/doc:element[@name='oairecerif']/doc:element[@name='affiliation']/doc:element[@name='orgunitDepartment']/doc:element/doc:field[@name='value'][$pos]"/>
        <xsl:if test="text()!=$EMPTY_VALUE">
            <mods:relatedItem otherType="affiliation">
                <mods:name type="corporate">
                    <mods:namePart><xsl:value-of select="text()"/></mods:namePart>
                    <xsl:if test="$entityName!=$EMPTY_VALUE">
                        <mods:namePart><xsl:value-of select="$entityName"/></mods:namePart>
                    </xsl:if>
                </mods:name>
            </mods:relatedItem>
        </xsl:if>
    </xsl:template>
    <!-- ORIGIN INFO (issued date, publisher name & location, edition statement) =================================== -->
    <xsl:template name="originInfo">
        <xsl:param name="date"/>
        <xsl:param name="editorName"/>
        <xsl:param name="editorLocation"/>
        <xsl:param name="editionStatement"/>
        <xsl:if test="string-length($date)>0 or string-length($editorName)>0 or string-length($editorLocation)>0 or string-length($editionStatement)>0">
            <mods:originInfo>
                <xsl:if test="string-length($date)>0">
                    <mods:dateIssued encoding="iso8601"><xsl:value-of select="$date"/></mods:dateIssued>
                </xsl:if>
                <xsl:if test="string-length($editorName)>0">
                    <mods:publisher><xsl:value-of select="$editorName"/></mods:publisher>
                </xsl:if>
                <xsl:if test="string-length($editorLocation)>0">
                    <mods:place>
                        <mods:placeTerm type="text"><xsl:value-of select="$editorLocation"/></mods:placeTerm>
                    </mods:place>
                </xsl:if>
                <xsl:if test="string-length($editionStatement)>0">
                    <mods:edition><xsl:value-of select="$editionStatement"/></mods:edition>
                </xsl:if>
            </mods:originInfo>
        </xsl:if>
    </xsl:template>
    <!-- ACCESS RIGHTS ============================================================================================= -->
    <xsl:template match="doc:metadata/doc:element[@name='dcterms']/doc:element[@name='accessRights']">
        <xsl:variable name="value" select="doc:element/doc:field[@name='value']/text()"/>
        <xsl:choose>
            <xsl:when test="$value='administrator'">
                <mods:accessCondition type="restriction on access" xlink:href="http://purl.org/eprint/accessRights/ClosedAccess">Closed access</mods:accessCondition>
            </xsl:when>
            <xsl:when test="$value='restricted' or $value='embargo'">
                <mods:accessCondition type="restriction on access" xlink:href="http://purl.org/eprint/accessRights/RestrictedAccess">Restricted access</mods:accessCondition>
            </xsl:when>
            <xsl:when test="$value='openaccess'">
                <mods:accessCondition type="restriction on access" xlink:href="http://purl.org/eprint/accessRights/OpenAccess">Open access</mods:accessCondition>
            </xsl:when>
        </xsl:choose>
    </xsl:template>
    <!-- FUNDING =================================================================================================== -->
    <xsl:template match="doc:metadata/doc:element[@name='funding']/doc:element[@name='organisation']/doc:element/doc:field[@name='value']">
        <xsl:variable name="pos" select="position()"/>
        <xsl:variable name="program" select="//doc:metadata/doc:element[@name='funding']/doc:element[@name='program']/doc:element/doc:field[@name='value'][$pos]"/>
        <xsl:variable name="project" select="//doc:metadata/doc:element[@name='funding']/doc:element[@name='project']/doc:element/doc:field[@name='value'][$pos]"/>
        <xsl:variable name="grantID" select="//doc:metadata/doc:element[@name='funding']/doc:element[@name='number']/doc:element/doc:field[@name='value'][$pos]"/>
        <mods:note type="funding">
            <xsl:text>This work was supported by </xsl:text><xsl:value-of select="text()"/>
            <xsl:if test="$program!=$EMPTY_VALUE or $project!=$EMPTY_VALUE">
                <xsl:text> [</xsl:text>
                <xsl:if test="$program!=$EMPTY_VALUE">
                    <xsl:value-of select="$program"/>
                </xsl:if>
                <xsl:if test="$project!=$EMPTY_VALUE">
                    <xsl:if test="$program!=$EMPTY_VALUE">
                        <xsl:text>/</xsl:text>
                    </xsl:if>
                    <xsl:value-of select="$project"/>
                </xsl:if>
                <xsl:text>]</xsl:text>
            </xsl:if>
            <xsl:if test="$grantID!=$EMPTY_VALUE">
                <xsl:text> [grant ID: </xsl:text>
                <xsl:value-of select="$grantID"/>
                <xsl:text>]</xsl:text>
            </xsl:if>
            <xsl:text>;</xsl:text>
        </mods:note>
    </xsl:template>
    <!-- PUBLICATION TITLE ========================================================================================= -->
    <xsl:template match="doc:metadata/doc:element[@name='dc']/doc:element[@name='title']">
        <mods:titleInfo>
            <mods:title><xsl:value-of select="doc:element/doc:field[@name='value']" /></mods:title>
        </mods:titleInfo>
    </xsl:template>
    <!-- ABSTRACT ================================================================================================== -->
    <xsl:template match="doc:metadata/doc:element[@name='dc']/doc:element[@name='description']/doc:element[@name='abstract']">
        <mods:abstract><xsl:value-of select="doc:element/doc:field[@name='value']" /></mods:abstract>
    </xsl:template>
    <!-- LANGUAGE ================================================================================================== -->
    <xsl:template match="doc:metadata/doc:element[@name='dc']/doc:element[@name='language']/doc:element[@name='iso']">
        <xsl:variable name="value" select="doc:element/doc:field[@name='value']"/>
        <mods:language>
            <mods:languageTerm type="code" authority="iso639-2b"><xsl:value-of select="$value" /></mods:languageTerm>
            <xsl:choose>
                <xsl:when test="$value='dut'"><mods:languageTerm type="text">Dutch</mods:languageTerm></xsl:when>
                <xsl:when test="$value='eng'"><mods:languageTerm type="text">English</mods:languageTerm></xsl:when>
                <xsl:when test="$value='fre'"><mods:languageTerm type="text">French</mods:languageTerm></xsl:when>
                <xsl:when test="$value='ita'"><mods:languageTerm type="text">Italian</mods:languageTerm></xsl:when>
                <xsl:when test="$value='ger'"><mods:languageTerm type="text">German</mods:languageTerm></xsl:when>
                <xsl:when test="$value='gre'"><mods:languageTerm type="text">Greek</mods:languageTerm></xsl:when>
                <xsl:when test="$value='lat'"><mods:languageTerm type="text">Latin</mods:languageTerm></xsl:when>
                <xsl:when test="$value='pol'"><mods:languageTerm type="text">Polish</mods:languageTerm></xsl:when>
                <xsl:when test="$value='por'"><mods:languageTerm type="text">Portuguese</mods:languageTerm></xsl:when>
                <xsl:when test="$value='rus'"><mods:languageTerm type="text">Russian</mods:languageTerm></xsl:when>
                <xsl:when test="$value='spa'"><mods:languageTerm type="text">Spanish</mods:languageTerm></xsl:when>
                <xsl:when test="$value='und'"><mods:languageTerm type="text">Undefined</mods:languageTerm></xsl:when>
            </xsl:choose>
        </mods:language>
    </xsl:template>
    <!-- DATES ===================================================================================================== -->
    <xsl:template match="doc:metadata/doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name='available']">
        <mods:extension>
            <mods:dateAvailable encoding="iso8601"><xsl:value-of select="doc:element/doc:field[@name='value']/text()"/></mods:dateAvailable>
        </mods:extension>
    </xsl:template>
    <xsl:template match="doc:metadata/doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name='accessioned']">
        <mods:extension>
            <mods:dateAccessioned encoding="iso8601"><xsl:value-of select="doc:element/doc:field[@name='value']/text()"/></mods:dateAccessioned>
        </mods:extension>
    </xsl:template>
    <!-- IDENTIFIERS =============================================================================================== -->
    <xsl:template match="doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element">
        <xsl:variable name="type" select="@name"/>
        <xsl:variable name="value" select="doc:element/doc:field[@name='value']"/>
        <xsl:choose>
            <xsl:when test="$type='doi'">
                <mods:identifier type="doi">doi:<xsl:value-of select="$value"/></mods:identifier>
            </xsl:when>
            <xsl:when test="$type='hdl'">
                <mods:identifier type="hdl">hdl:<xsl:value-of select="$value"/></mods:identifier>
            </xsl:when>
            <xsl:when test="$type='uri' and contains($value, 'handle.net/')">
                <mods:identifier type="hdl">hdl:<xsl:value-of select="substring-after($value, 'handle.net/')"/></mods:identifier>
                <mods:identifier type="uri"><xsl:value-of select="$value"/></mods:identifier>
            </xsl:when>
            <xsl:otherwise>
                <mods:identifier>
                    <xsl:if test="string-length($type)>0">
                        <xsl:attribute name="type"><xsl:value-of select="$type"/></xsl:attribute>
                    </xsl:if>
                    <xsl:value-of select="$value"/>
                </mods:identifier>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    <!-- TOPIC ===================================================================================================== -->
    <xsl:template match="doc:metadata/doc:element[@name='dc']/doc:element[@name='subject']/doc:element/doc:field[@name='value']">
        <mods:subject>
            <mods:topic><xsl:value-of select="text()"/></mods:topic>
        </mods:subject>
    </xsl:template>
    <xsl:template match="doc:metadata/doc:element[@name='dc']/doc:element[@name='subject']/doc:element/doc:element/doc:field[@name='value']">
        <mods:subject>
            <xsl:attribute name="authority"><xsl:value-of select="../../@name"/></xsl:attribute>
            <mods:topic><xsl:value-of select="text()"/></mods:topic>
        </mods:subject>
    </xsl:template>
    <!-- RELATIONS ================================================================================================= -->
    <xsl:template match="doc:metadata/doc:element[@name='dc']/doc:element[@name='relation']/doc:element[@name='dataset']">
        <mods:location><mods:url note="dataset"><xsl:value-of select="doc:element/doc:field[@name='value']/text()"/></mods:url></mods:location>
    </xsl:template>
    <xsl:template match="doc:metadata/doc:element[@name='dc']/doc:element[@name='relation']/doc:element[@name='url']">
        <mods:location><mods:url access="object in context"><xsl:value-of select="doc:element/doc:field[@name='value']/text()"/></mods:url></mods:location>
    </xsl:template>
    <!-- PUBLICATION STATUS ======================================================================================== -->
    <xsl:template match="doc:metadata/doc:element[@name='publication']/doc:element[@name='publicationStatus']">
        <mods:accessCondition type="publicationStatus"><xsl:value-of select="doc:element/doc:field[@name='value']/text()"/></mods:accessCondition>
    </xsl:template>
    <!-- NUMBER OF PAGES =========================================================================================== -->
    <xsl:template match="doc:metadata/doc:element[@name='publication']/doc:element[@name='numberOfPages']">
        <mods:physicalDescription>
            <mods:extent unit="pages"><xsl:value-of select="doc:element/doc:field[@name='value']/text()"/></mods:extent>
        </mods:physicalDescription>
    </xsl:template>
    <!-- PEER-REVIEWED ============================================================================================= -->
    <xsl:template match="doc:metadata/doc:element[@name='publication']/doc:element[@name='book' or @name='serial']/doc:element[@name='peerReviewed']">
        <xsl:variable name="value" select="doc:element/doc:field[@name='value']/text()"/>
        <xsl:if test="$value='true'">
            <mods:note type="action" xml:lang="en">peer reviewed</mods:note>
        </xsl:if>
    </xsl:template>
    <!-- COLLECTION ================================================================================================ -->
    <xsl:template name="collectionHost">
        <xl:variable name="name" select="doc:metadata/doc:element[@name='publication']/doc:element[@name='collection']/doc:element[@name='name']/doc:element/doc:field[@name='value']"/>
        <xl:variable name="number" select="doc:metadata/doc:element[@name='publication']/doc:element[@name='collection']/doc:element[@name='number']/doc:element/doc:field[@name='value']"/>
        <xl:variable name="issn" select="doc:metadata/doc:element[@name='publication']/doc:element[@name='collection']/doc:element[@name='issn']/doc:element/doc:field[@name='value']"/>
        <xsl:if test="string-length($name)>0">
            <mods:relatedItem otherType="collection">
                <mods:titleInfo>
                    <mods:title><xsl:value-of select="$name"/></mods:title>
                    <xsl:if test="string-length($number)>0">
                        <mods:partNumber><xsl:value-of select="$number"/></mods:partNumber>
                    </xsl:if>
                </mods:titleInfo>
                <xsl:if test="string-length($issn)>0">
                    <mods:identifier type="issn"><xsl:value-of select="$issn"/></mods:identifier>
                </xsl:if>
            </mods:relatedItem>
        </xsl:if>
    </xsl:template>
    <!-- HOST DOCUMENT ============================================================================================= -->
    <xsl:template name="hostDocument">
        <xsl:variable name="title" select="//doc:metadata/doc:element[@name='publication']/doc:element[@name='host']/doc:element[@name='title']/doc:element/doc:field[@name='value']"/>
        <xsl:variable name="authors" select="//doc:metadata/doc:element[@name='publication']/doc:element[@name='host']/doc:element[@name='type']/doc:element/doc:field[@name='value']"/>
        <xsl:variable name="type" select="//doc:metadata/doc:element[@name='publication']/doc:element[@name='host']/doc:element[@name='authors']/doc:element/doc:field[@name='value']"/>
        <xsl:variable name="pagination" select="//doc:metadata/doc:element[@name='publication']/doc:element[@name='host']/doc:element[@name='pages']/doc:element/doc:field[@name='value']"/>
        <xsl:variable name="isbn" select="//doc:metadata/doc:element[@name='publication']/doc:element[@name='host']/doc:element[@name='isbn']/doc:element/doc:field[@name='value']"/>
        <xsl:variable name="peerReviewed" select="//doc:metadata/doc:element[@name='publication']/doc:element[@name='host']/doc:element[@name='peerReviewed']/doc:element/doc:field[@name='value']"/>
        <xsl:if test="string-length($title)>0">
            <mods:relatedItem type="host" otherType="parentDocument">
                <mods:titleInfo>
                    <mods:title><xsl:value-of select="$title"/></mods:title>
                </mods:titleInfo>
                <xsl:if test="string-length($authors)>0">
                    <mods:name>
                        <mods:namePart><xsl:value-of select="$authors"/></mods:namePart>
                    </mods:name>
                </xsl:if>
                <xsl:if test="string-length($type)>0">
                    <mods:genre><xsl:value-of select="$type"/></mods:genre>
                </xsl:if>
                <xsl:if test="string-length($pagination)>0">
                    <mods:physicalDescription>
                        <mods:note type="pagination"><xsl:value-of select="$pagination"/></mods:note>
                    </mods:physicalDescription>
                </xsl:if>
                <xsl:if test="string-length($isbn)>0">
                    <mods:identifier type="isbn"><xsl:value-of select="$isbn"/></mods:identifier>
                </xsl:if>
                <xsl:if test="$peerReviewed='true'">
                    <mods:note type="action" xml:lang="en">peer reviewed</mods:note>
                </xsl:if>
                <xsl:call-template name="originInfo">
                    <xsl:with-param name="date" select="//doc:metadata/doc:element[@name='publication']/doc:element[@name='host']/doc:element[@name='dateIssued']/doc:element/doc:field[@name='value']"/>
                    <xsl:with-param name="editorName" select="//doc:metadata/doc:element[@name='publication']/doc:element[@name='editor']/doc:element[@name='name']/doc:element/doc:field[@name='value']"/>
                    <xsl:with-param name="editorLocation" select="//doc:metadata/doc:element[@name='publication']/doc:element[@name='editor']/doc:element[@name='location']/doc:element/doc:field[@name='value']"/>
                    <xsl:with-param name="editionStatement" select="//doc:metadata/doc:element[@name='publication']/doc:element[@name='host']/doc:element[@name='editionStatement']/doc:element/doc:field[@name='value']"/>
                </xsl:call-template>
                <xsl:call-template name="collectionHost"/>
            </mods:relatedItem>
        </xsl:if>
    </xsl:template>
    <xsl:template name="serialHostDocument">
        <xsl:variable name="journalTitle" select="//doc:metadata/doc:element[@name='dc']/doc:element[@name='relation']/doc:element[@name='journal']/doc:element/doc:field[@name='value']"/>
        <xsl:variable name="issn" select="//doc:metadata/doc:element[@name='publication']/doc:element[@name='serial']/doc:element[@name='issn']/doc:element/doc:field[@name='value']"/>
        <xsl:variable name="eissn" select="//doc:metadata/doc:element[@name='publication']/doc:element[@name='serial']/doc:element[@name='eissn']/doc:element/doc:field[@name='value']"/>
        <xsl:variable name="volume" select="//doc:metadata/doc:element[@name='publication']/doc:element[@name='serial']/doc:element[@name='volume']/doc:element/doc:field[@name='value']"/>
        <xsl:variable name="issue" select="//doc:metadata/doc:element[@name='publication']/doc:element[@name='serial']/doc:element[@name='issue']/doc:element/doc:field[@name='value']"/>
        <xsl:variable name="pages" select="//doc:metadata/doc:element[@name='publication']/doc:element[@name='serial']/doc:element[@name='pages']/doc:element/doc:field[@name='value']"/>
        <xsl:variable name="date" select="//doc:metadata/doc:element[@name='publication']/doc:element[@name='serial']/doc:element[@name='dateIssued']/doc:element/doc:field[@name='value']"/>
        <xsl:if test="string-length($journalTitle)>0">
            <mods:relatedItem type="host" otherType="parentDocument">
                <mods:genre authority="coar" valueURI="http://purl.org/coar/resource_type/c_0640">journal</mods:genre>
                <mods:titleInfo>
                    <mods:title><xsl:value-of select="$journalTitle"/></mods:title>
                </mods:titleInfo>
                <xsl:if test="string-length($issn)>0">
                    <mods:identifier type="issn"><xsl:value-of select="$issn"/></mods:identifier>
                </xsl:if>
                <xsl:if test="string-length($eissn)>0">
                    <mods:identifier type="eissn"><xsl:value-of select="$eissn"/></mods:identifier>
                </xsl:if>
                <xsl:if test="string-length($volume)>0 or string-length($issue)>0 or string-length($pages)>0 or string-length($date)>0">
                    <mods:part>
                        <xsl:if test="string-length($volume)>0">
                            <mods:detail type="volume"><xsl:value-of select="$volume"/></mods:detail>
                        </xsl:if>
                        <xsl:if test="string-length($issue)>0">
                            <mods:detail type="issue"><xsl:value-of select="$issue"/></mods:detail>
                        </xsl:if>
                        <xsl:if test="string-length($pages)>0">
                            <mods:extent unit="pages">
                                <xsl:choose>
                                    <xsl:when test="contains($pages,'-')">
                                        <mods:start><xsl:value-of select="substring-before($pages, '-')"/></mods:start>
                                        <mods:end><xsl:value-of select="substring-after($pages, '-')"/></mods:end>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <mods:start><xsl:value-of select="$volume"/></mods:start>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </mods:extent>
                        </xsl:if>
                        <xsl:if test="string-length($date)>0">
                            <mods:date><xsl:value-of select="$date"/></mods:date>
                        </xsl:if>
                    </mods:part>
                </xsl:if>
            </mods:relatedItem>
        </xsl:if>
    </xsl:template>
    <!-- REPORT REFERENCE ========================================================================================== -->
    <xsl:template name="reportReference">
        <xsl:variable name="orgName" select="//doc:metadata/doc:element[@name='publication']/doc:element[@name='report']/doc:element[@name='organisation']/doc:element/doc:field[@name='value']"/>
        <xsl:variable name="period" select="//doc:metadata/doc:element[@name='publication']/doc:element[@name='report']/doc:element[@name='period']/doc:element/doc:field[@name='value']"/>
        <xsl:if test="string-length($orgName)>0">
            <mods:relatedItem otherType="report reference">
                <mods:name type="corporate">
                    <mods:namePart><xsl:value-of select="$orgName"/></mods:namePart>
                </mods:name>
                <xsl:if test="string-length($period)>0">
                    <mods:originInfo>
                        <mods:dateOther type="period"><xsl:value-of select="$period"/></mods:dateOther>
                    </mods:originInfo>
                </xsl:if>
            </mods:relatedItem>
        </xsl:if>
    </xsl:template>
    <!-- PATENT ==================================================================================================== -->
    <xsl:template match="doc:metadata/doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name='submitter']">
        <mods:name>
            <mods:namePart><xsl:value-of select="doc:element/doc:field[@name='value']/text()"/></mods:namePart>
            <mods:role>
                <mods:roleTerm type="code" authority="marcrelator">app</mods:roleTerm>
                <mods:roleTerm type="text">Applicant</mods:roleTerm>
            </mods:role>
        </mods:name>
    </xsl:template>
    <!-- CONFERENCE DATA =========================================================================================== -->
    <xsl:template name="conferenceData">
        <xsl:variable name="name" select="//doc:metadata/doc:element[@name='publication']/doc:element[@name='conference']/doc:element[@name='name']/doc:element/doc:field[@name='value']"/>
        <xsl:variable name="location" select="//doc:metadata/doc:element[@name='publication']/doc:element[@name='conference']/doc:element[@name='location']/doc:element/doc:field[@name='value']"/>
        <xsl:variable name="start" select="//doc:metadata/doc:element[@name='publication']/doc:element[@name='conference']/doc:element[@name='startDate']/doc:element/doc:field[@name='value']"/>
        <xsl:variable name="end" select="//doc:metadata/doc:element[@name='publication']/doc:element[@name='conference']/doc:element[@name='endDate']/doc:element/doc:field[@name='value']"/>
        <xsl:if test="string-length($name)>0">
            <mods:relatedItem type="host" otherType="conference">
                <mods:titleInfo>
                    <mods:title><xsl:value-of select="$name"/></mods:title>
                </mods:titleInfo>
                <xsl:if test="string-length($location)>0 or string-length($start)>0 or string-length($end)>0">
                    <mods:originInfo>
                        <xsl:if test="string-length($location)>0">
                            <mods:place>
                                <mods:placeTerm type="text"><xsl:value-of select="$location"/></mods:placeTerm>
                            </mods:place>
                        </xsl:if>
                        <xsl:if test="string-length($start)>0">
                            <mods:dateOther point="start" encoding="iso8601"><xsl:value-of select="$start"/></mods:dateOther>
                        </xsl:if>
                        <xsl:if test="string-length($end)>0">
                            <mods:dateOther point="end" encoding="iso8601"><xsl:value-of select="$end"/></mods:dateOther>
                        </xsl:if>
                    </mods:originInfo>
                </xsl:if>
            </mods:relatedItem>
        </xsl:if>
    </xsl:template>
    <xsl:template match="doc:metadata/doc:element[@name='publication']/doc:element[@name='isAbstract']">
        <xsl:if test="doc:element/doc:field[@name='value']/text()='true'">
            <mods:genre>abstract</mods:genre>
        </xsl:if>
    </xsl:template>

</xsl:stylesheet>
