<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
                xmlns:mods="http://www.loc.gov/mods/v3" version="1.0">
    <xsl:output indent="yes" method="xml"/>


    <!-- CONSTANTS ================================================================================ -->
    <xsl:variable name="PLACEHOLDER">#PLACEHOLDER_PARENT_METADATA_VALUE#</xsl:variable>
    <xsl:variable name="UPPER_CHARACTERS">ABCDEFGHIJKLMNOPQRSTUVWXYZ</xsl:variable>
    <xsl:variable name="LOWER_CHARACTERS">abcdefghijklmnopqrstuvwxyz</xsl:variable>
    <xsl:variable name="CHARACTERS">abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ</xsl:variable>


    <!-- FUNCTIONS ==================================================== -->
    <!-- Performing and `@lang` attribute from any `mods` tag -->
    <xsl:template match="text()"/>
    <xsl:template match="@authority">
        <xsl:if test="string(.) != ''">
            <xsl:attribute name="authority"><xsl:value-of select="."/></xsl:attribute>
            <xsl:attribute name="confidence">ACCEPTED</xsl:attribute>
        </xsl:if>
    </xsl:template>
    <xsl:template match="@lang">
        <xsl:variable name="translated">
            <xsl:choose>
                <xsl:when test="normalize-space(.)='fre'">fr</xsl:when>
                <xsl:when test="normalize-space(.)='eng'">en</xsl:when>
                <xsl:when test="normalize-space(.)='dut'">nl</xsl:when>
                <xsl:when test="normalize-space(.)='gre'">el</xsl:when>
                <xsl:when test="normalize-space(.)='ger'">de</xsl:when>
                <xsl:when test="normalize-space(.)='spa'">es</xsl:when>
                <xsl:when test="normalize-space(.)='ita'">it</xsl:when>
                <xsl:when test="normalize-space(.)='gre'">el</xsl:when>
                <xsl:when test="normalize-space(.)='por'">pt</xsl:when>
                <xsl:when test="normalize-space(.)='rus'">ru</xsl:when>
            </xsl:choose>
        </xsl:variable>
        <xsl:if test="string($translated) != ''">
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
    <xsl:template name="toLowerCase">
        <xsl:param name="value"/>
        <xsl:value-of select="translate($value, $UPPER_CHARACTERS, $LOWER_CHARACTERS)"/>
    </xsl:template>


    <!-- ROOT ========================================================= -->
    <xsl:template match="//mods:mods">
        <xsl:element name="dim:dim">
            <xsl:apply-templates/>
        </xsl:element>
    </xsl:template>


    <!-- GENRE -> dc.type.maintype & dc.type.subtype ================== -->
    <xsl:template match="/mods:mods/mods:genre">
        <xsl:variable name="value">
            <xsl:choose>
                <xsl:when test="@valueURI='http://purl.org/coar/resource_type/c_15cd'">text::patent</xsl:when>
                <xsl:when test="@valueURI='http://purl.org/coar/resource_type/c_8042'">text::working-paper</xsl:when>
                <xsl:when test="@valueURI='http://purl.org/coar/resource_type/c_93fc'">text::report</xsl:when>
                <xsl:when test="@valueURI='http://purl.org/coar/resource_type/c_2f33'">text::book</xsl:when>
                <xsl:when test="@valueURI='http://purl.org/coar/resource_type/c_3248'">text::book-part</xsl:when>
                <xsl:when test="@valueURI='http://purl.org/coar/resource_type/c_6501'">text::journal-article</xsl:when>
                <xsl:when test="@valueURI='http://purl.org/coar/resource_type/c_18cp'">text::conference-speech</xsl:when>
                <xsl:when test="@valueURI='http://purl.org/coar/resource_type/c_f744'">text::conference-speech</xsl:when>
                <xsl:when test="@valueURI='http://purl.org/coar/resource_type/R60J-J5BD'">text::conference-speech</xsl:when>
                <xsl:when test="@valueURI='http://purl.org/coar/resource_type/c_db06'">text::thesis</xsl:when>
                <xsl:otherwise>normalize-space(.)</xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:element name="dim:field">
            <xsl:attribute name="mdschema">dc</xsl:attribute>
            <xsl:attribute name="element">type</xsl:attribute>
            <xsl:attribute name="qualifier">maintype</xsl:attribute>
            <xsl:value-of select="normalize-space($value)"/>
        </xsl:element>
    </xsl:template>
    <xsl:template match="/mods:mods/mods:note[@type='document subtype']">
        <xsl:variable name="subtype">
            <xsl:choose>
                <!-- book subtypes -->
                <xsl:when test="text()='Actes de colloque'">conference-proceedings</xsl:when>
                <xsl:when test="text()='Dictionnaire /encyclopédie'">dictionary/encyclopaedia</xsl:when>
                <xsl:when test="text()='Monographie'">book</xsl:when>
                <xsl:when test="text()='Manuel / précis / textbook'">handbook/textbook</xsl:when>
                <xsl:when test="text()='Mélanges'">mixed-content</xsl:when>
                <!-- report subtype -->
                <xsl:when test="text()='Interne'">internal</xsl:when>
                <xsl:when test="text()='Externe'">external</xsl:when>
                <!-- book-chapter subtype -->
                <xsl:when test="text()='Autre'">other</xsl:when>
                <xsl:when test="text()='Chapitre'">book-chapter</xsl:when>
                <xsl:when test="text()='Avant-propos / préface / postface'">preface/postface/foreword</xsl:when>
                <!-- journal-article subtype -->
                <xsl:when test="text()='Article de recherche'">research-article</xsl:when>
                <xsl:when test="text()='Article de vulgarisation'">popularising-article</xsl:when>
                <xsl:when test="text()='Compte-rendu'">report</xsl:when>
                <xsl:when test="text()='Dossier dans une revue'">feature-article</xsl:when>
                <xsl:when test="text()='Éditorial'">editorial</xsl:when>
                <xsl:when test="text()='Essai clinique'">clinical-study</xsl:when>
                <xsl:when test="starts-with(text(), 'Lettre à l')">letter-to-the-editor</xsl:when>
                <xsl:when test="text()='Note ou chronique de jurisprudence'">law-case-note</xsl:when>
                <xsl:when test="text()='Numéro entier'">full-issue</xsl:when>
                <xsl:when test="text()='Synthèse de littérature'">literature-review</xsl:when>
                <!-- conference-speech subtype -->
                <xsl:when test="text()='Conférence invitée / Keynote'">keynote</xsl:when>
                <xsl:when test="text()='Présentation orale avec comité de sélection'">with-selection-speech</xsl:when>
                <xsl:when test="text()='Présentation orale sans comité de sélection'">without-selection-speech</xsl:when>
                <xsl:when test="text()='Poster'">conference-poster</xsl:when>
            </xsl:choose>
        </xsl:variable>
        <xsl:if test="string-length($subtype) > 0">
            <xsl:element name="dim:field">
                <xsl:attribute name="mdschema">dc</xsl:attribute>
                <xsl:attribute name="element">type</xsl:attribute>
                <xsl:attribute name="qualifier">subtype</xsl:attribute>
                <xsl:value-of select="normalize-space($subtype)"/>
            </xsl:element>
        </xsl:if>
    </xsl:template>
    <!-- AUTHORS ====================================================== -->
    <!--   * namePart                      -> dc.contributor.author -->
    <!--   * nameIdentifier[@type='email'] -> authors.email -->
    <!--   * nameIdentifier[@type='orcid'] -> authors.identifier.orcid -->
    <!--   * nameIdentifier[@type='fgs']   -> authors.identifier.fgs -->
    <!--   * role/roleTerm[@type='text']   -> authors.role -->
    <!--   * affiliation                   -> authors.institution.code -->
    <xsl:template match="/mods:mods/mods:name[@type='personal' and mods:role/mods:roleTerm[@type='text'] != 'supervisor']">
        <!-- author-name ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ -->
        <xsl:element name="dim:field">
            <xsl:attribute name="mdschema">dc</xsl:attribute>
            <xsl:attribute name="element">contributor</xsl:attribute>
            <xsl:attribute name="qualifier">author</xsl:attribute>
            <xsl:apply-templates select="@authority"/>
            <xsl:value-of select="normalize-space(./mods:namePart)"/>
        </xsl:element>
        <!-- author-email ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ -->
        <xsl:element name="dim:field">
            <xsl:attribute name="mdschema">authors</xsl:attribute>
            <xsl:attribute name="element">email</xsl:attribute>
            <xsl:apply-templates select="@authority"/>
            <xsl:call-template name="valueOrDefault">
                <xsl:with-param name="value" select="./mods:nameIdentifier[@type='email']"/>
            </xsl:call-template>
        </xsl:element>
        <!-- author-orcid-id ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ -->
        <xsl:element name="dim:field">
            <xsl:attribute name="mdschema">authors</xsl:attribute>
            <xsl:attribute name="element">identifier</xsl:attribute>
            <xsl:attribute name="qualifier">orcid</xsl:attribute>
            <xsl:apply-templates select="@authority"/>
            <xsl:call-template name="valueOrDefault">
                <xsl:with-param name="value" select="substring-after(./mods:nameIdentifier[@type='orcid'], '.org/')"/>
            </xsl:call-template>
        </xsl:element>
        <!-- author-fgs-id ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ -->
        <xsl:element name="dim:field">
            <xsl:attribute name="mdschema">authors</xsl:attribute>
            <xsl:attribute name="element">identifier</xsl:attribute>
            <xsl:attribute name="qualifier">fgs</xsl:attribute>
            <xsl:apply-templates select="@authority"/>
            <xsl:call-template name="valueOrDefault">
                <xsl:with-param name="value" select="./mods:nameIdentifier[@type='fgs']"/>
            </xsl:call-template>
        </xsl:element>
        <!-- author-role ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ -->
        <xsl:element name="dim:field">
            <xsl:attribute name="mdschema">authors</xsl:attribute>
            <xsl:attribute name="element">role</xsl:attribute>
            <xsl:call-template name="valueOrDefault">
                <xsl:with-param name="value" select="normalize-space(./mods:role/mods:roleTerm[@type='text'])"/>
                <xsl:with-param name="defaultValue" select="'author'"/>
            </xsl:call-template>
        </xsl:element>
        <!-- author-institution ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ -->
        <xsl:element name="dim:field">
            <xsl:attribute name="mdschema">authors</xsl:attribute>
            <xsl:attribute name="element">institution</xsl:attribute>
            <xsl:attribute name="qualifier">code</xsl:attribute>
            <xsl:call-template name="valueOrDefault">
                <xsl:with-param name="value" select="./mods:affiliation"/>
            </xsl:call-template>
        </xsl:element>
    </xsl:template>
    <xsl:template match="/mods:mods/mods:name[@type='personal' and mods:role/mods:roleTerm[@type='text'] = 'supervisor']">
        <!-- supervisor-name ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ -->
        <xsl:element name="dim:field">
            <xsl:attribute name="mdschema">dc</xsl:attribute>
            <xsl:attribute name="element">contributor</xsl:attribute>
            <xsl:attribute name="qualifier">advisor</xsl:attribute>
            <xsl:value-of select="normalize-space(./mods:namePart)"/>
        </xsl:element>
        <!-- supervisor-email ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ -->
        <xsl:element name="dim:field">
            <xsl:attribute name="mdschema">advisors</xsl:attribute>
            <xsl:attribute name="element">email</xsl:attribute>
            <xsl:call-template name="valueOrDefault">
                <xsl:with-param name="value" select="./mods:nameIdentifier[@type='email']"/>
            </xsl:call-template>
        </xsl:element>
        <!-- supervisor-orcid-id ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ -->
        <xsl:element name="dim:field">
            <xsl:attribute name="mdschema">advisors</xsl:attribute>
            <xsl:attribute name="element">identifier</xsl:attribute>
            <xsl:attribute name="qualifier">orcid</xsl:attribute>
            <xsl:apply-templates select="@authority"/>
            <xsl:call-template name="valueOrDefault">
                <xsl:with-param name="value" select="substring-after(./mods:nameIdentifier[@type='orcid'], '.org/')"/>
            </xsl:call-template>
        </xsl:element>
        <!-- supervisor-fgs-id ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ -->
        <xsl:element name="dim:field">
            <xsl:attribute name="mdschema">advisors</xsl:attribute>
            <xsl:attribute name="element">identifier</xsl:attribute>
            <xsl:attribute name="qualifier">fgs</xsl:attribute>
            <xsl:apply-templates select="@authority"/>
            <xsl:call-template name="valueOrDefault">
                <xsl:with-param name="value" select="./mods:nameIdentifier[@type='fgs']"/>
            </xsl:call-template>
        </xsl:element>
        <!-- supervisor-institution ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ -->
        <xsl:element name="dim:field">
            <xsl:attribute name="mdschema">advisors</xsl:attribute>
            <xsl:attribute name="element">institution</xsl:attribute>
            <xsl:attribute name="qualifier">code</xsl:attribute>
            <xsl:call-template name="valueOrDefault">
                <xsl:with-param name="value" select="./mods:affiliation"/>
            </xsl:call-template>
        </xsl:element>
    </xsl:template>
    <xsl:template match="/mods:mods/mods:name/mods:etal">
        <xsl:element name="dim:field">
            <xsl:attribute name="mdschema">dc</xsl:attribute>
            <xsl:attribute name="element">contributor</xsl:attribute>
            <xsl:attribute name="qualifier">etal</xsl:attribute>
            <xsl:text>true</xsl:text>
        </xsl:element>
    </xsl:template>
    <!-- TITLE -> dc.title ============================================ -->
    <xsl:template match="/mods:mods/mods:titleInfo">
        <xsl:element name="dim:field">
            <xsl:attribute name="mdschema">dc</xsl:attribute>
            <xsl:attribute name="element">title</xsl:attribute>
            <xsl:apply-templates select="./mods:title/@lang"/>
            <xsl:choose>
                <xsl:when test="./mods:subtitle">
                    <xsl:value-of select="concat(normalize-space(./mods:title), ':', normalize-space(./mods:subtitle))"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="normalize-space(./mods:title)"/>
                </xsl:otherwise>
            </xsl:choose>
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
    <!-- LANGUAGE -> dc.language.iso ================================== -->
    <xsl:template match="/mods:mods/mods:language/mods:languageTerm[@type='code' and @authority='iso639-2b']">
        <xsl:element name="dim:field">
            <xsl:attribute name="mdschema">dc</xsl:attribute>
            <xsl:attribute name="element">language</xsl:attribute>
            <xsl:attribute name="qualifier">iso</xsl:attribute>
            <xsl:value-of select="normalize-space(.)"/>
        </xsl:element>
    </xsl:template>
    <!-- SUBJECT -> dc.subject[.internal|.mesh] ======================= -->
    <xsl:template match="/mods:mods/mods:subject">
        <xsl:choose>
            <xsl:when test="@authority = 'internal'">
                <xsl:element name="dim:field">
                    <xsl:attribute name="mdschema">dc</xsl:attribute>
                    <xsl:attribute name="element">subject</xsl:attribute>
                    <xsl:attribute name="qualifier">internal</xsl:attribute>
                    <xsl:value-of select="normalize-space(./mods:topic)"/>
                </xsl:element>
            </xsl:when>
            <xsl:when test="@authority = 'MeSH'">
                <xsl:element name="dim:field">
                    <xsl:attribute name="mdschema">dc</xsl:attribute>
                    <xsl:attribute name="element">subject</xsl:attribute>
                    <xsl:attribute name="qualifier">mesh</xsl:attribute>
                    <xsl:value-of select="normalize-space(./mods:topic)"/>
                </xsl:element>
            </xsl:when>
            <xsl:otherwise>
                <xsl:element name="dim:field">
                    <xsl:attribute name="mdschema">dc</xsl:attribute>
                    <xsl:attribute name="element">subject</xsl:attribute>
                    <xsl:value-of select="normalize-space(./mods:topic)"/>
                </xsl:element>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    <!-- LOCATION -> dc.relation[.url|.dataset] ======================= -->
    <xsl:template match="/mods:mods/mods:location/mods:url">
        <xsl:variable name="urlType">
            <xsl:call-template name="toLowerCase">
                <xsl:with-param name="value"><xsl:value-of select="@note"/></xsl:with-param>
            </xsl:call-template>
        </xsl:variable>
        <xsl:choose>
            <xsl:when test="string($urlType) = ''">
                <xsl:element name="dim:field">
                    <xsl:attribute name="mdschema">dc</xsl:attribute>
                    <xsl:attribute name="element">relation</xsl:attribute>
                    <xsl:value-of select="normalize-space(.)"/>
                </xsl:element>
            </xsl:when>
            <xsl:when test="string($urlType) = 'dataset'">
                <xsl:element name="dim:field">
                    <xsl:attribute name="mdschema">dc</xsl:attribute>
                    <xsl:attribute name="element">relation</xsl:attribute>
                    <xsl:attribute name="qualifier">dataset</xsl:attribute>
                    <xsl:value-of select="normalize-space(.)"/>
                </xsl:element>
            </xsl:when>
            <xsl:when test="string($urlType) = 'pubmed'">
                <xsl:variable name="pubmedID" select="substring-after(normalize-space(.), 'https://www.ncbi.nlm.nih.gov/pubmed/')"/>
                <xsl:choose>
                    <xsl:when test="string-length($pubmedID) > 0">
                        <xsl:element name="dim:field">
                            <xsl:attribute name="mdschema">dc</xsl:attribute>
                            <xsl:attribute name="element">identifier</xsl:attribute>
                            <xsl:attribute name="qualifier">pmid</xsl:attribute>
                            <xsl:value-of select="$pubmedID"/>
                        </xsl:element>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:element name="dim:field">
                            <xsl:attribute name="mdschema">dc</xsl:attribute>
                            <xsl:attribute name="element">relation</xsl:attribute>
                            <xsl:value-of select="normalize-space(.)"/>
                        </xsl:element>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:when>
            <xsl:when test="string($urlType) = 'arxiv'">
                <xsl:variable name="arxivID" select="substring-after(substring-after(normalize-space(.), 'http://arxiv.org/pdf/arXiv:'), 'https://arxiv.org/abs/')"/>
                <xsl:choose>
                    <xsl:when test="string-length($arxivID) > 0">
                        <xsl:element name="dim:field">
                            <xsl:attribute name="mdschema">dc</xsl:attribute>
                            <xsl:attribute name="element">identifier</xsl:attribute>
                            <xsl:attribute name="qualifier">arxiv</xsl:attribute>
                            <xsl:value-of select="$arxivID"/>
                        </xsl:element>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:element name="dim:field">
                            <xsl:attribute name="mdschema">dc</xsl:attribute>
                            <xsl:attribute name="element">relation</xsl:attribute>
                            <xsl:value-of select="normalize-space(.)"/>
                        </xsl:element>
                    </xsl:otherwise>
                </xsl:choose>

            </xsl:when>
        </xsl:choose>

    </xsl:template>
    <!-- AFFILIATION -> oairecerif.affiliation ======================== -->
    <xsl:template match="/mods:mods/mods:relatedItem[@otherType='affiliation']">
        <xsl:element name="dim:field">
            <xsl:attribute name="mdschema">oairecerif</xsl:attribute>
            <xsl:attribute name="element">affiliation</xsl:attribute>
            <xsl:attribute name="qualifier">orgunit</xsl:attribute>
            <xsl:apply-templates select="./mods:name[@type='corporate']/@authority"/>
            <xsl:value-of select="normalize-space(./mods:name[@type='corporate'])"/>
        </xsl:element>
        <xsl:choose>
            <xsl:when test="./mods:name[@type='corporate']/text() = 'UCLouvain' and
                           (./mods:titleInfo/mods:title/text() = 'Louvain School of Management' or
                            ./mods:titleInfo/mods:title/text() = 'Ecole Polytechnique de Louvain' or
                            ./mods:titleInfo/mods:title/text() = 'Ingénierie biologique, agronomique et environnementale' or
                            starts-with(./mods:titleInfo/mods:title/text(), 'Faculté'))">
                <xsl:element name="dim:field">
                    <xsl:attribute name="mdschema">dissertation</xsl:attribute>
                    <xsl:attribute name="element">faculty</xsl:attribute>
                    <xsl:apply-templates select="@authority"/>
                    <xsl:value-of select="normalize-space(./mods:titleInfo/mods:title)"/>
                </xsl:element>
            </xsl:when>
            <xsl:otherwise>
                <xsl:element name="dim:field">
                    <xsl:attribute name="mdschema">oairecerif</xsl:attribute>
                    <xsl:attribute name="element">affiliation</xsl:attribute>
                    <xsl:attribute name="qualifier">orgunitDepartment</xsl:attribute>
                    <xsl:apply-templates select="@authority"/>
                    <xsl:call-template name="valueOrDefault">
                        <xsl:with-param name="value" select="normalize-space(./mods:titleInfo/mods:title)"/>
                    </xsl:call-template>
                </xsl:element>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    <!-- NUMBER OF PAGES -> publication.numberOfPages ================= -->
    <xsl:template match="/mods:mods/mods:note[@type='number of pages']">
        <xsl:element name="dim:field">
            <xsl:attribute name="mdschema">publication</xsl:attribute>
            <xsl:attribute name="element">numberOfPages</xsl:attribute>
            <xsl:value-of select="normalize-space(.)"/>
        </xsl:element>
    </xsl:template>
    <!-- COLLECTION =================================================== -->
    <!--   * titleInfo/title          -> publication.collection.name -->
    <!--   * titleInfo/partNumber     -> publication.collection.number -->
    <!--   * identifier[@type='issn'] -> publication.collection.issn -->
    <xsl:template match="/mods:mods/mods:relatedItem[@otherType='collection host']">
        <xsl:if test="./mods:titleInfo">
            <xsl:element name="dim:field">
                <xsl:attribute name="mdschema">publication</xsl:attribute>
                <xsl:attribute name="element">collection</xsl:attribute>
                <xsl:attribute name="qualifier">name</xsl:attribute>
                <xsl:value-of select="./mods:titleInfo/mods:title[1]"/>
            </xsl:element>
            <xsl:if test="./mods:titleInfo/mods:partNumber">
                <xsl:element name="dim:field">
                    <xsl:attribute name="mdschema">publication</xsl:attribute>
                    <xsl:attribute name="element">collection</xsl:attribute>
                    <xsl:attribute name="qualifier">number</xsl:attribute>
                    <xsl:value-of select="./mods:titleInfo/mods:partNumber[1]"/>
                </xsl:element>
            </xsl:if>
        </xsl:if>
        <xsl:if test="./mods:identifier[@type='issn']">
            <xsl:element name="dim:field">
                <xsl:attribute name="mdschema">publication</xsl:attribute>
                <xsl:attribute name="element">collection</xsl:attribute>
                <xsl:attribute name="qualifier">issn</xsl:attribute>
                <xsl:value-of select="./mods:identifier[@type='issn'][1]"/>
            </xsl:element>
        </xsl:if>
    </xsl:template>
    <!-- REPORT REFERENCE ============================================= -->
    <!--   * titleInfo/title                      -> publication.report.organization -->
    <!--   * originInfo/dateOther[@type='period'] -> publication.report.period -->
    <!--   * originInfo/publisher                 -> publication.editor.name -->
    <!--   * originInfo/place                     -> publication.editor.location -->
    <!--   * identifier[@type='document ID']      -> dc.identifier.reportID -->
    <xsl:template match="/mods:mods/mods:relatedItem[@otherType='report reference']">
        <xsl:if test="./mods:titleInfo">
            <xsl:element name="dim:field">
                <xsl:attribute name="mdschema">publication</xsl:attribute>
                <xsl:attribute name="element">report</xsl:attribute>
                <xsl:attribute name="qualifier">organization</xsl:attribute>
                <xsl:value-of select="./mods:titleInfo/mods:title[1]"/>
            </xsl:element>
            <xsl:if test="./mods:originInfo/mods:dateOther[@type='period']">
                <xsl:element name="dim:field">
                    <xsl:attribute name="mdschema">publication</xsl:attribute>
                    <xsl:attribute name="element">report</xsl:attribute>
                    <xsl:attribute name="qualifier">period</xsl:attribute>
                    <xsl:value-of select="normalize-space(./mods:originInfo/mods:dateOther[@type='period'][1])"/>
                </xsl:element>
            </xsl:if>
        </xsl:if>
        <xsl:if test="./mods:originInfo/mods:publisher">
            <xsl:element name="dim:field">
                <xsl:attribute name="mdschema">publication</xsl:attribute>
                <xsl:attribute name="element">editor</xsl:attribute>
                <xsl:attribute name="qualifier">name</xsl:attribute>
                <xsl:value-of select="normalize-space(./mods:originInfo/mods:publisher[1])"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="./mods:originInfo/mods:place">
            <xsl:element name="dim:field">
                <xsl:attribute name="mdschema">publication</xsl:attribute>
                <xsl:attribute name="element">editor</xsl:attribute>
                <xsl:attribute name="qualifier">location</xsl:attribute>
                <xsl:value-of select="normalize-space(./mods:originInfo/mods:place[1])"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="../mods:identifier[@type='document ID']">
            <xsl:element name="dim:field">
                <xsl:attribute name="mdschema">dc</xsl:attribute>
                <xsl:attribute name="element">identifier</xsl:attribute>
                <xsl:attribute name="qualifier">reportID</xsl:attribute>
                <xsl:value-of select="normalize-space(../mods:identifier[@type='document ID'][1])"/>
            </xsl:element>
        </xsl:if>
    </xsl:template>
    <!-- ORIGIN_INFO ================================================== -->
    <!--   * dateIssued                      -> dc.date.issued -->
    <!--   * publisher                       -> publication.editor.name OR crispatent.patentOffice -->
    <!--   * place                           -> publication.editor.location -->
    <!--   * dateOther[@type='defense date'] -> dissertation.defenseDate -->
    <xsl:template match="/mods:mods/mods:originInfo/mods:dateIssued">
        <xsl:element name="dim:field">
            <xsl:attribute name="mdschema">dc</xsl:attribute>
            <xsl:attribute name="element">date</xsl:attribute>
            <xsl:attribute name="qualifier">issued</xsl:attribute>
            <xsl:value-of select="normalize-space(.)"/>
        </xsl:element>
    </xsl:template>
    <xsl:template match="/mods:mods/mods:originInfo/mods:publisher">
        <xsl:choose>
            <!-- special mapping if documentType is `Patent` -->
            <xsl:when test="//mods:mods/mods:genre/@valueURI='http://purl.org/coar/resource_type/c_15cd'">
                <xsl:element name="dim:field">
                    <xsl:attribute name="mdschema">crispatent</xsl:attribute>
                    <xsl:attribute name="element">patentOffice</xsl:attribute>
                    <xsl:value-of select="normalize-space(.)"/>
                </xsl:element>
            </xsl:when>
            <!-- default behavior -->
            <xsl:otherwise>
                <xsl:element name="dim:field">
                    <xsl:attribute name="mdschema">publication</xsl:attribute>
                    <xsl:attribute name="element">editor</xsl:attribute>
                    <xsl:attribute name="qualifier">name</xsl:attribute>
                    <xsl:value-of select="normalize-space(.)"/>
                </xsl:element>
            </xsl:otherwise>
        </xsl:choose>

    </xsl:template>
    <xsl:template match="/mods:mods/mods:originInfo/mods:place">
        <xsl:element name="dim:field">
            <xsl:attribute name="mdschema">publication</xsl:attribute>
            <xsl:attribute name="element">editor</xsl:attribute>
            <xsl:attribute name="qualifier">location</xsl:attribute>
            <xsl:value-of select="normalize-space(.)"/>
        </xsl:element>
    </xsl:template>
    <xsl:template match="/mods:mods/mods:originInfo/mods:dateOther[@type='defense date']">
        <xsl:element name="dim:field">
            <xsl:attribute name="mdschema">dissertation</xsl:attribute>
            <xsl:attribute name="element">defenseDate</xsl:attribute>
            <xsl:value-of select="normalize-space(.)"/>
        </xsl:element>
    </xsl:template>
    <!-- FUNDING ===================================================== -->
    <!--   * identifier[@type='organization'] -> funding.organization -->
    <!--   * identifier[@type='program']      -> funding.program -->
    <!--   * identifier[@type='project']      -> funding.project -->
    <!--   * identifier[@type='subvention']   -> funding.number -->
    <xsl:template match="/mods:mods/mods:relatedItem[@otherType='funding']">
        <xsl:element name="dim:field">
            <xsl:attribute name="mdschema">funding</xsl:attribute>
            <xsl:attribute name="element">organization</xsl:attribute>
            <xsl:call-template name="valueOrDefault">
                <xsl:with-param name="value" select="./mods:identifier[@type='organization']"/>
            </xsl:call-template>
        </xsl:element>
        <xsl:element name="dim:field">
            <xsl:attribute name="mdschema">funding</xsl:attribute>
            <xsl:attribute name="element">program</xsl:attribute>
            <xsl:call-template name="valueOrDefault">
                <xsl:with-param name="value" select="./mods:identifier[@type='program']"/>
            </xsl:call-template>
        </xsl:element>
        <xsl:element name="dim:field">
            <xsl:attribute name="mdschema">funding</xsl:attribute>
            <xsl:attribute name="element">project</xsl:attribute>
            <xsl:call-template name="valueOrDefault">
                <xsl:with-param name="value" select="./mods:identifier[@type='project']"/>
            </xsl:call-template>
        </xsl:element>
        <xsl:element name="dim:field">
            <xsl:attribute name="mdschema">funding</xsl:attribute>
            <xsl:attribute name="element">number</xsl:attribute>
            <xsl:call-template name="valueOrDefault">
                <xsl:with-param name="value" select="./mods:identifier[@type='subvention']"/>
            </xsl:call-template>
        </xsl:element>
    </xsl:template>
    <!-- SUBMITTER -> dc.contributor.submitter ======================= -->
    <xsl:template match="/mods:mods/mods:name[@type='corporate' and mods:role/mods:roleTerm[@type='text']='patent_submitter']">
        <xsl:element name="dim:field">
            <xsl:attribute name="mdschema">dc</xsl:attribute>
            <xsl:attribute name="element">contributor</xsl:attribute>
            <xsl:attribute name="qualifier">submitter</xsl:attribute>
            <xsl:value-of select="normalize-space(./mods:namePart)"/>
        </xsl:element>
    </xsl:template>
    <!-- PATENT DATA ================================================ -->
    <!--   * identifier[@type='ECLA'] -> dc.identifier.ecla -->
    <!--   * identifier[@type='IPC'] -> dc.identifier.ipc -->
    <!--   * identifier[@type='priority number'] -> dc.identifier.priorityNumber -->
    <!--   * identifier[@type='patent id'] -> dc.identifier.patentID -->
    <!--   * note[@type='deposit date'] -> crispatent.deposit.date -->
    <xsl:template match="/mods:mods/mods:identifier[@type='ECLA']">
        <xsl:element name="dim:field">
            <xsl:attribute name="mdschema">dc</xsl:attribute>
            <xsl:attribute name="element">identifier</xsl:attribute>
            <xsl:attribute name="qualifier">ecla</xsl:attribute>
            <xsl:value-of select="normalize-space(.)"/>
        </xsl:element>
    </xsl:template>
    <xsl:template match="/mods:mods/mods:identifier[@type='IPC']">
        <xsl:element name="dim:field">
            <xsl:attribute name="mdschema">dc</xsl:attribute>
            <xsl:attribute name="element">identifier</xsl:attribute>
            <xsl:attribute name="qualifier">ipc</xsl:attribute>
            <xsl:value-of select="normalize-space(.)"/>
        </xsl:element>
    </xsl:template>
    <xsl:template match="/mods:mods/mods:identifier[@type='priority number']">
        <xsl:element name="dim:field">
            <xsl:attribute name="mdschema">dc</xsl:attribute>
            <xsl:attribute name="element">identifier</xsl:attribute>
            <xsl:attribute name="qualifier">priorityNumber</xsl:attribute>
            <xsl:value-of select="normalize-space(.)"/>
        </xsl:element>
    </xsl:template>
    <xsl:template match="/mods:mods/mods:identifier[@type='patent id']">
        <xsl:element name="dim:field">
            <xsl:attribute name="mdschema">dc</xsl:attribute>
            <xsl:attribute name="element">identifier</xsl:attribute>
            <xsl:attribute name="qualifier">patentID</xsl:attribute>
            <xsl:value-of select="normalize-space(.)"/>
        </xsl:element>
    </xsl:template>
    <xsl:template match="/mods:mods/mods:note[@type='deposit date']">
        <xsl:element name="dim:field">
            <xsl:attribute name="mdschema">crispatent</xsl:attribute>
            <xsl:attribute name="element">deposit</xsl:attribute>
            <xsl:attribute name="qualifier">date</xsl:attribute>
            <xsl:value-of select="normalize-space(.)"/>
        </xsl:element>
    </xsl:template>
    <!-- BOOK HOST DOCUMENT ========================================== -->
    <!--   * titleInfo/title                 -> publication.host.title -->
    <!--   * name/namePart                   -> publication.host.authors -->
    <!--   * note[@type='pages']             -> publication.host.pages -->
    <!--   * note[@type='edition statement'] -> publication.host.editionStatement -->
    <!--   * note[@type='host type']         -> publication.host.type -->
    <!--   * identifier[@type='isbn']        -> publication.host.isbn -->
    <!--   * originInfo/dateIssued           -> publication.host.dateIssued -->
    <!--   * originInfo/publisher            -> publication.editor.name -->
    <!--   * originInfo/place                -> publication.editor.location -->
    <!--   * note[@type='peer review']       -> publication.host.peerReviewed -->
    <xsl:template match="/mods:mods/mods:relatedItem[@otherType='host' and mods:genre/text() = 'book']">
        <xsl:element name="dim:field">
            <xsl:attribute name="mdschema">publication</xsl:attribute>
            <xsl:attribute name="element">host</xsl:attribute>
            <xsl:attribute name="qualifier">title</xsl:attribute>
            <xsl:value-of select="normalize-space(./mods:titleInfo/mods:title)"/>
        </xsl:element>
        <xsl:if test="./mods:name/mods:namePart">
            <xsl:element name="dim:field">
                <xsl:attribute name="mdschema">publication</xsl:attribute>
                <xsl:attribute name="element">host</xsl:attribute>
                <xsl:attribute name="qualifier">authors</xsl:attribute>
                <xsl:value-of select="normalize-space(./mods:name/mods:namePart)"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="./mods:note[@type='pages']">
            <xsl:element name="dim:field">
                <xsl:attribute name="mdschema">publication</xsl:attribute>
                <xsl:attribute name="element">host</xsl:attribute>
                <xsl:attribute name="qualifier">pages</xsl:attribute>
                <xsl:value-of select="normalize-space(./mods:note[@type='pages'])"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="./mods:note[@type='edition statement']">
            <xsl:element name="dim:field">
                <xsl:attribute name="mdschema">publication</xsl:attribute>
                <xsl:attribute name="element">host</xsl:attribute>
                <xsl:attribute name="qualifier">editionStatement</xsl:attribute>
                <xsl:value-of select="normalize-space(./mods:note[@type='edition statement'])"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="./mods:note[@type='host type']">
            <xsl:element name="dim:field">
                <xsl:attribute name="mdschema">publication</xsl:attribute>
                <xsl:attribute name="element">host</xsl:attribute>
                <xsl:attribute name="qualifier">type</xsl:attribute>
                <xsl:value-of select="normalize-space(./mods:note[@type='host type'])"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="./mods:identifier[@type='isbn']">
            <xsl:element name="dim:field">
                <xsl:attribute name="mdschema">publication</xsl:attribute>
                <xsl:attribute name="element">host</xsl:attribute>
                <xsl:attribute name="qualifier">isbn</xsl:attribute>
                <xsl:value-of select="normalize-space(./mods:identifier[@type='isbn'])"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="./mods:originInfo/mods:dateIssued">
            <xsl:element name="dim:field">
                <xsl:attribute name="mdschema">publication</xsl:attribute>
                <xsl:attribute name="element">host</xsl:attribute>
                <xsl:attribute name="qualifier">dateIssued</xsl:attribute>
                <xsl:value-of select="normalize-space(./mods:originInfo/mods:dateIssued)"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="./mods:originInfo/mods:publisher">
            <xsl:element name="dim:field">
                <xsl:attribute name="mdschema">publication</xsl:attribute>
                <xsl:attribute name="element">editor</xsl:attribute>
                <xsl:attribute name="qualifier">name</xsl:attribute>
                <xsl:value-of select="normalize-space(./mods:originInfo/mods:publisher)"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="./mods:originInfo/mods:place">
            <xsl:element name="dim:field">
                <xsl:attribute name="mdschema">publication</xsl:attribute>
                <xsl:attribute name="element">editor</xsl:attribute>
                <xsl:attribute name="qualifier">location</xsl:attribute>
                <xsl:value-of select="normalize-space(./mods:originInfo/mods:place)"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="./mods:note[@type='peer review' and text() = 'Peer-reviewed']">
            <xsl:element name="dim:field">
                <xsl:attribute name="mdschema">publication</xsl:attribute>
                <xsl:attribute name="element">host</xsl:attribute>
                <xsl:attribute name="qualifier">peerReviewed</xsl:attribute>
                <xsl:text>true</xsl:text>
            </xsl:element>
        </xsl:if>
    </xsl:template>
    <!-- SERIAL HOST DOCUMENT ======================================== -->
    <!--   * titeInfo/title              -> dc.relation.journal -->
    <!--   * note[@type='peer review']   -> publication.serial.peerReviewed -->
    <!--   * identifier[@type='issn']    -> publication.serial.issn -->
    <!--   * identifier[@type='e-issn']   -> publication.serial.eissn -->
    <!--   * part/detail[@type='volume'] -> publication.serial.volume -->
    <!--   * part/detail[@type='number'] -> publication.serial.issue -->
    <!--   * part/extent[@unit='page']   -> publication.serial.pages -->
    <!--   * part/date                   -> publication.serial.dateIssued -->
    <!--   * originInfo/publisher        -> publication.editor.name -->
    <!--   * originInfo/place            -> publication.editor.location -->
    <xsl:template match="/mods:mods/mods:relatedItem[@otherType='host' and mods:genre/text() = 'journal']">
        <xsl:element name="dim:field">
            <xsl:attribute name="mdschema">dc</xsl:attribute>
            <xsl:attribute name="element">relation</xsl:attribute>
            <xsl:attribute name="qualifier">journal</xsl:attribute>
            <xsl:apply-templates select="@authority"/>
            <xsl:value-of select="normalize-space(./mods:titleInfo/mods:title)"/>
        </xsl:element>
        <xsl:if test="./mods:note[@type='peer review']">
            <xsl:element name="dim:field">
                <xsl:attribute name="mdschema">publication</xsl:attribute>
                <xsl:attribute name="element">serial</xsl:attribute>
                <xsl:attribute name="qualifier">peerReviewed</xsl:attribute>
                <xsl:apply-templates select="@authority"/>
                <xsl:text>true</xsl:text>
            </xsl:element>
        </xsl:if>
        <xsl:if test="./mods:identifier[@type='issn']">
            <xsl:element name="dim:field">
                <xsl:attribute name="mdschema">publication</xsl:attribute>
                <xsl:attribute name="element">serial</xsl:attribute>
                <xsl:attribute name="qualifier">issn</xsl:attribute>
                <xsl:apply-templates select="@authority"/>
                <xsl:value-of select="./mods:identifier[@type='issn']"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="./mods:identifier[@type='e-issn']">
            <xsl:element name="dim:field">
                <xsl:attribute name="mdschema">publication</xsl:attribute>
                <xsl:attribute name="element">serial</xsl:attribute>
                <xsl:attribute name="qualifier">eissn</xsl:attribute>
                <xsl:apply-templates select="@authority"/>
                <xsl:value-of select="./mods:identifier[@type='e-issn']"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="./mods:part/mods:detail[@type='volume']/mods:number">
            <xsl:element name="dim:field">
                <xsl:attribute name="mdschema">publication</xsl:attribute>
                <xsl:attribute name="element">serial</xsl:attribute>
                <xsl:attribute name="qualifier">volume</xsl:attribute>
                <xsl:value-of select="./mods:part/mods:detail[@type='volume']/mods:number"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="./mods:part/mods:detail[@type='number']/mods:number">
            <xsl:element name="dim:field">
                <xsl:attribute name="mdschema">publication</xsl:attribute>
                <xsl:attribute name="element">serial</xsl:attribute>
                <xsl:attribute name="qualifier">issue</xsl:attribute>
                <xsl:value-of select="./mods:part/mods:detail[@type='number']/mods:number"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="./mods:part/mods:extent[@unit='page']/mods:list">
            <xsl:element name="dim:field">
                <xsl:attribute name="mdschema">publication</xsl:attribute>
                <xsl:attribute name="element">serial</xsl:attribute>
                <xsl:attribute name="qualifier">pages</xsl:attribute>
                <xsl:value-of select="./mods:part/mods:extent[@unit='page']/mods:list"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="./mods:part/mods:date">
            <xsl:element name="dim:field">
                <xsl:attribute name="mdschema">publication</xsl:attribute>
                <xsl:attribute name="element">serial</xsl:attribute>
                <xsl:attribute name="qualifier">dateIssued</xsl:attribute>
                <xsl:value-of select="./mods:part/mods:date"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="./mods:originInfo/mods:publisher">
            <xsl:element name="dim:field">
                <xsl:attribute name="mdschema">publication</xsl:attribute>
                <xsl:attribute name="element">editor</xsl:attribute>
                <xsl:attribute name="qualifier">name</xsl:attribute>
                <xsl:value-of select="normalize-space(./mods:originInfo/mods:publisher)"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="./mods:originInfo/mods:place">
            <xsl:element name="dim:field">
                <xsl:attribute name="mdschema">publication</xsl:attribute>
                <xsl:attribute name="element">editor</xsl:attribute>
                <xsl:attribute name="qualifier">location</xsl:attribute>
                <xsl:value-of select="normalize-space(./mods:originInfo/mods:place)"/>
            </xsl:element>
        </xsl:if>
    </xsl:template>
    <!-- CONFERENCE ================================================== -->
    <!--   * relatedItem[@type='host']            -> publication.speech.status -->
    <!--   * titleInfo/title                      -> publication.conference.name -->
    <!--   * originInfo/place                     -> publication.conference.location -->
    <!--   * originInfo/dateOther[@point='start'] -> publication.conference.startDate -->
    <!--   * originInfo/dateOther[@point='end']   -> publication.conference.endDate -->
    <xsl:template match="/mods:mods/mods:relatedItem[@otherType='conference']">
        <xsl:variable name="status">
            <xsl:choose>
                <xsl:when test="../mods:relatedItem[@otherType='host']/mods:genre/text() = 'journal'">published_in_serial</xsl:when>
                <xsl:when test="../mods:relatedItem[@otherType='host']/mods:genre/text() = 'book'">published_in_book</xsl:when>
                <xsl:otherwise>not_published</xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:element name="dim:field">
            <xsl:attribute name="mdschema">publication</xsl:attribute>
            <xsl:attribute name="element">speech</xsl:attribute>
            <xsl:attribute name="qualifier">status</xsl:attribute>
            <xsl:value-of select="$status"/>
        </xsl:element>
        <xsl:element name="dim:field">
            <xsl:attribute name="mdschema">publication</xsl:attribute>
            <xsl:attribute name="element">conference</xsl:attribute>
            <xsl:attribute name="qualifier">name</xsl:attribute>
            <xsl:value-of select="./mods:titleInfo/mods:title"/>
        </xsl:element>
        <xsl:if test="./mods:originInfo/mods:place">
            <xsl:element name="dim:field">
                <xsl:attribute name="mdschema">publication</xsl:attribute>
                <xsl:attribute name="element">conference</xsl:attribute>
                <xsl:attribute name="qualifier">location</xsl:attribute>
                <xsl:value-of select="./mods:originInfo/mods:place"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="./mods:originInfo/mods:dateOther[@point='start']">
            <xsl:element name="dim:field">
                <xsl:attribute name="mdschema">publication</xsl:attribute>
                <xsl:attribute name="element">conference</xsl:attribute>
                <xsl:attribute name="qualifier">startDate</xsl:attribute>
                <xsl:value-of select="./mods:originInfo/mods:dateOther[@point='start']"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="./mods:originInfo/mods:dateOther[@point='end']">
            <xsl:element name="dim:field">
                <xsl:attribute name="mdschema">publication</xsl:attribute>
                <xsl:attribute name="element">conference</xsl:attribute>
                <xsl:attribute name="qualifier">endDate</xsl:attribute>
                <xsl:value-of select="./mods:originInfo/mods:dateOther[@point='end']"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="../mods:note[@type='is abstract'] = 'true'">
            <xsl:element name="dim:field">
                <xsl:attribute name="mdschema">publication</xsl:attribute>
                <xsl:attribute name="element">isAbstract</xsl:attribute>
                <xsl:text>true</xsl:text>
            </xsl:element>
        </xsl:if>
    </xsl:template>
    <!-- ISBN -> dc.identifier.isbn ================================== -->
    <xsl:template match="/mods:mods/mods:identifier[@type='isbn']">
        <xsl:element name="dim:field">
            <xsl:attribute name="mdschema">dc</xsl:attribute>
            <xsl:attribute name="element">identifier</xsl:attribute>
            <xsl:attribute name="qualifier">isbn</xsl:attribute>
            <xsl:value-of select="normalize-space(.)"/>
        </xsl:element>
    </xsl:template>
    <!-- LEGACY RECORD INFO ========================================== -->
    <!--   * recordInfo                     -> dc.description.provenance -->
    <xsl:template match="/mods:mods/mods:recordInfo">
        <xsl:variable name="creationDate" select="./mods:recordCreationDate"/>
        <xsl:variable name="submitter">
            <xsl:choose>
                <xsl:when test="./mods:recordInfoNote[@type='submitter name'] and ./mods:recordInfoNote[@type='submitter email']">
                    <xsl:value-of select="concat(normalize-space(./mods:recordInfoNote[@type='submitter name']), ' -- ', normalize-space(./mods:recordInfoNote[@type='submitter email']))"/>
                </xsl:when>
                <xsl:when test="./mods:recordInfoNote[@type='submitter name']">
                    <xsl:value-of select="normalize-space(./mods:recordInfoNote[@type='submitter name'])"/>
                </xsl:when>
                <xsl:when test="./mods:recordInfoNote[@type='submitter email']">
                    <xsl:value-of select="normalize-space(./mods:recordInfoNote[@type='submitter email'])"/>
                </xsl:when>
            </xsl:choose>
        </xsl:variable>
        <xsl:if test="string($submitter) != '' or string($creationDate) != ''">
            <xsl:element name="dim:field">
                <xsl:attribute name="mdschema">dc</xsl:attribute>
                <xsl:attribute name="element">description</xsl:attribute>
                <xsl:attribute name="qualifier">provenance</xsl:attribute>
                <xsl:text>Submitted by [</xsl:text>
                <xsl:call-template name="valueOrDefault">
                    <xsl:with-param name="value" select="$submitter"/>
                    <xsl:with-param name="defaultValue" select="'unknown'"/>
                </xsl:call-template>
                <xsl:text>] at [</xsl:text>
                <xsl:call-template name="valueOrDefault">
                    <xsl:with-param name="value" select="$creationDate"/>
                    <xsl:with-param name="defaultValue" select="'unknown'"/>
                </xsl:call-template>
                <xsl:text>]</xsl:text>
            </xsl:element>
        </xsl:if>
    </xsl:template>
    <!-- LEGACY HANDLE -> dc.description.provenance ================== -->
    <xsl:template match="/mods:mods/mods:identifier[@type='hdl']">
        <xsl:element name="dim:field">
            <xsl:attribute name="mdschema">dc</xsl:attribute>
            <xsl:attribute name="element">description</xsl:attribute>
            <xsl:attribute name="qualifier">provenance</xsl:attribute>
            <xsl:text>Original handle value [</xsl:text><xsl:value-of select="normalize-space(.)"/><xsl:text>]</xsl:text>
        </xsl:element>
        <xsl:element name="dim:field">
            <xsl:attribute name="mdschema">dc</xsl:attribute>
            <xsl:attribute name="element">identifier</xsl:attribute>
            <xsl:attribute name="qualifier">handle</xsl:attribute>
            <xsl:value-of select="normalize-space(.)"/>
        </xsl:element>
    </xsl:template>
    <!-- DOI -> dc.identifier.doi ==================================== -->
    <xsl:template match="/mods:mods/mods:identifier[@type='doi']">
        <xsl:element name="dim:field">
            <xsl:attribute name="mdschema">dc</xsl:attribute>
            <xsl:attribute name="element">identifier</xsl:attribute>
            <xsl:attribute name="qualifier">doi</xsl:attribute>
            <xsl:value-of select="normalize-space(.)"/>
        </xsl:element>
    </xsl:template>
    <!-- PUBLICATION STATUS -> publication.publicationStatus ========= -->
    <xsl:template match="/mods:mods/mods:note[@type='publication status']">
        <xsl:if test="text() = 'submitted' or text() = 'accepted/in-press' or text() = 'published'">
            <xsl:element name="dim:field">
                <xsl:attribute name="mdschema">publication</xsl:attribute>
                <xsl:attribute name="element">publicationStatus</xsl:attribute>
                <xsl:value-of select="normalize-space(text())"/>
            </xsl:element>
        </xsl:if>
    </xsl:template>
    <!-- EDITION STATEMENT -> publication.editionStatement =========== -->
    <xsl:template match="/mods:mods/mods:note[@type='edition statement']">
        <xsl:element name="dim:field">
            <xsl:attribute name="mdschema">publication</xsl:attribute>
            <xsl:attribute name="element">editionStatement</xsl:attribute>
            <xsl:value-of select="normalize-space(.)"/>
        </xsl:element>
    </xsl:template>
    <!-- DISSERTATION ================================================ -->
    <xsl:template match="/mods:mods/mods:note[@type='dissertation degree']">
        <xsl:element name="dim:field">
            <xsl:attribute name="mdschema">dissertation</xsl:attribute>
            <xsl:attribute name="element">degree</xsl:attribute>
            <xsl:attribute name="qualifier">name</xsl:attribute>
            <xsl:value-of select="normalize-space(.)"/>
        </xsl:element>
        <xsl:element name="dim:field">
            <xsl:attribute name="mdschema">dissertation</xsl:attribute>
            <xsl:attribute name="element">institution</xsl:attribute>
            <xsl:attribute name="qualifier">name</xsl:attribute>
            <xsl:text>UCLouvain</xsl:text>
        </xsl:element>
    </xsl:template>

</xsl:stylesheet>