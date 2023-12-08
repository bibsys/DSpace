<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="1.0"
      xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
      xmlns:fo="http://www.w3.org/1999/XSL/Format">
  <xsl:output method="xml" indent="yes"/>
  <xsl:template match="data">
    <fo:root
    xmlns:fo="http://www.w3.org/1999/XSL/Format">
      <fo:layout-master-set>
        <fo:simple-page-master master-name="A4" page-height="29.7cm" page-width="21cm">
          <fo:region-body margin-top="3.2cm" margin-bottom="2.2cm"/>
          <fo:region-before extent="3cm"/>
          <fo:region-after extent="2cm"/>
        </fo:simple-page-master>
      </fo:layout-master-set>
      <fo:page-sequence master-reference="A4">
        <!-- HEADER ============================================================= -->
        <fo:static-content flow-name="xsl-region-before">
          <fo:block>
            <fo:external-graphic>
              <xsl:attribute name="src">
                <xsl:value-of select="imagePath"/>
              </xsl:attribute>
            </fo:external-graphic>
          </fo:block>
        </fo:static-content>
        <!-- BODY =============================================================== -->
        <fo:flow flow-name="xsl-region-body" font-size="10pt" margin-left="0.5cm" margin-right="0.5cm" font-family="sans-serif" text-align="left">
          <fo:block-container overflow="hidden" height="25cm">
            <fo:block border-bottom="1px solid #444" color="#444" font-size="16pt" text-align="center" margin-top="1.5em">Attestation de dépôt en ligne du mémoire</fo:block>
            <fo:block margin-top="2em" font-family="arial-uni">Le <xsl:value-of select="today"/>,</fo:block>
            <xsl:for-each select="authors/author">
              <fo:block margin-top="1em" >
                Monsieur/Madame <xsl:value-of select="name"/>
              </fo:block>
            </xsl:for-each>  
            <fo:block margin-top="2em" font-family="arial-uni">Le mémoire suivant dont vous êtes l'auteur·rice (ou un·e des co-auteurs·rices), a été déposé dans le répertoire des mémoires de l'UCLouvain</fo:block>
            <fo:table table-layout="fixed" border-style="solid" margin-top="1em">
              <fo:table-body>
                <fo:table-row>
                  <fo:table-cell padding="0.25em" font-weight="bold" width="4.5cm" color="#999999" font-size="0.8em">
                    <fo:block>Titre :</fo:block>
                  </fo:table-cell>
                  <fo:table-cell padding="0.25em" border-bottom="1px solid #DDDDDD" font-style="italic">
                    <fo:block>"<xsl:value-of select="title"/>"</fo:block>
                  </fo:table-cell>
                </fo:table-row>
                <fo:table-row>
                  <fo:table-cell padding="0.25em" font-weight="bold" width="4.5cm" color="#999999" font-size="0.8em">
                    <fo:block>Auteur(s) :</fo:block>
                  </fo:table-cell>
                  <fo:table-cell padding="0.25em" border-bottom="1px solid #DDDDDD">
                    <fo:block>
                      <xsl:for-each select="authors/author">
                        <xsl:value-of select="name"/>
                        <xsl:if test="position() != last()"> ; </xsl:if> 
                      </xsl:for-each>
                    </fo:block>
                  </fo:table-cell>
                </fo:table-row>
                <fo:table-row>
                  <fo:table-cell padding="0.25em" font-weight="bold" width="4.5cm" color="#999999" font-size="0.8em">
                    <fo:block>Promoteur(s) :</fo:block>
                  </fo:table-cell>
                  <fo:table-cell padding="0.25em" border-bottom="1px solid #DDDDDD">
                    <fo:block>
                      <xsl:for-each select="advisors/advisor">
                        <xsl:value-of select="name"/>
                        <xsl:if test="position() != last()"> ; </xsl:if> 
                      </xsl:for-each>
                    </fo:block>
                  </fo:table-cell>
                </fo:table-row>
                <fo:table-row>
                  <fo:table-cell padding="0.25em" font-weight="bold" width="4.5cm" color="#999999" font-size="0.8em">
                    <fo:block>Programme(s) de cours :</fo:block>
                  </fo:table-cell>
                  <fo:table-cell padding="0.25em" border-bottom="1px solid #DDDDDD">
                    <fo:block>
                      <xsl:for-each select="programs/program">
                        <xsl:value-of select="name" />
                        <xsl:if test="position() != last()"> ; </xsl:if> 
                      </xsl:for-each>
                    </fo:block>
                  </fo:table-cell>
                </fo:table-row>
                <fo:table-row>
                  <fo:table-cell padding="0.25em" font-weight="bold" width="4.5cm" color="#999999" font-size="0.8em">
                    <fo:block>Déposant :</fo:block>
                  </fo:table-cell>
                  <fo:table-cell padding="0.25em" border-bottom="1px solid #DDDDDD">
                    <fo:block><xsl:value-of select="submitter"/></fo:block>
                  </fo:table-cell>
                </fo:table-row>
                <fo:table-row>
                  <fo:table-cell padding="0.25em" font-weight="bold" width="4.5cm" color="#999999" font-size="0.8em">
                    <fo:block>Identifiant :</fo:block>
                  </fo:table-cell>
                  <fo:table-cell padding="0.25em" border-bottom="1px solid #DDDDDD">
                    <fo:block><xsl:value-of select="handle"/></fo:block>
                  </fo:table-cell>
                </fo:table-row>
                <fo:table-row>
                  <fo:table-cell padding="0.25em" font-weight="bold" width="4.5cm" color="#999999" font-size="0.8em">
                    <fo:block>Résumé :</fo:block>
                  </fo:table-cell>
                  <fo:table-cell padding="0.25em">
                    <fo:block>voir verso</fo:block>
                  </fo:table-cell>
                </fo:table-row>
              </fo:table-body>
            </fo:table>
            <fo:block border-bottom="1pt solid #000000" margin-top="2em" margin-bottom="2em"/>
            <fo:block margin-top="1.5em" font-family="arial-uni">
            Vous (ou la personne ayant déposé le mémoire avec votre accord) avez souhaité que les fichiers déposés puissent être communiqués au public d'après les modalités suivantes :
            
              <fo:list-block provisional-label-separation="3mm" provisional-distance-between-starts="1cm" margin-top="1em" margin-bottom="1em" margin-left="1cm">
                <!-- <xsl:choose>
                  <xsl:when test="files/file"> -->
                    <xsl:apply-templates select="files/file" />
                  <!-- </xsl:when>
                  <xsl:otherwise>
                    <fo:list-item>
                      <fo:list-item-label end-indent="label-end()">
                        <fo:block>
                          *
                        </fo:block>  
                      </fo:list-item-label>
                      <fo:list-item-body start-indent="body-start()">
                        <fo:block>
                          No item provided
                        </fo:block>
                      </fo:list-item-body>
                    </fo:list-item>
                  </xsl:otherwise>    
                </xsl:choose>      -->
              </fo:list-block>
            À cette fin vous avez concédé une licence à l'UCLouvain dont les termes sont répétés ci-dessous.
            
            </fo:block>
            <fo:block margin-top="1cm" font-family="arial-uni" font-size="0.6em">LICENCE</fo:block>
            <fo:block font-family="arial-uni" font-size="0.6em">En déposant son mémoire (ci-après « l’œuvre ») dans le répertoire des mémoires de l’UCLouvain, l'auteur·rice concède à l’UCLouvain une licence d’archivage, de reproduction et de communication au public de l’œuvre et plus précisément :</fo:block>
            <fo:block font-family="arial-uni" font-size="0.6em">  - le droit de fixer et de reproduire l’œuvre sur support électronique.</fo:block>
            <fo:block font-family="arial-uni" font-size="0.6em">  - le droit de communiquer l’œuvre au public suivant les modalités définies au moment du dépôt.</fo:block>
            <fo:block margin-top="1em" font-family="arial-uni" font-size="0.6em">Cette licence, gratuite et non exclusive, est valable pour toute la durée de la propriété littéraire et artistique, y compris ses éventuelles prolongations, et pour le monde entier.</fo:block>
            <fo:block margin-top="1em" font-family="arial-uni" font-size="0.6em">L'auteur·rice conserve tous les autres droits pour la reproduction et la communication du mémoire, ainsi que le droit de l’utiliser dans de futurs travaux.</fo:block>
            <fo:block margin-top="1em" font-family="arial-uni" font-size="0.6em">L'auteur·rice garantit que l’œuvre est originale et qu’il ou elle détient le droit d’octroyer la présente licence, le cas échéant avec l’accord de co-auteurs·rices. L’auteur·rice certifie avoir obtenu, conformément
          à la législation sur le droit d’auteur et aux exigences du droit à l’image, et dans la seule mesure où il ou elle ne peut pas se prévaloir d'une exception, toutes les autorisations nécessaires à la reproduction dans l’œuvre d’images, de textes, et/ou de toute œuvre protégés par le droit d’auteur, et avoir obtenu les autorisations
          nécessaires à leur communication à des tiers. Au cas où un tiers est titulaire d’un droit de propriété intellectuelle sur tout ou partie de mon mémoire, l’auteur·rice certifie avoir obtenu son
          autorisation écrite des droits mentionnés ci-dessus.</fo:block>
            <fo:block margin-top="1em" font-family="arial-uni" font-size="0.6em">En cas de manquement de l’auteur aux obligations de la présente licence, l’UCLouvain se réserve le droit de retire l’œuvre du dépôt.</fo:block>
            <fo:block margin-top="1em" font-family="arial-uni" font-size="0.6em">La présente licence est soumise au droit belge.</fo:block>
            <fo:block margin-top="1em" font-family="arial-uni" font-size="0.6em" font-weight="bold">Dans le cas où un dépôt du mémoire en version 'papier' (ou sur tout autre support physique) est exigé par ma faculté, je m'engage à ce qu'il soit identique à la version électronique déposée en ligne.</fo:block>
            <fo:block border-bottom="1pt solid #000000" margin-top="2cm" margin-bottom="2em"/>
            <fo:block font-size="16pt" margin-bottom="3em">DATE ET SIGNATURE:</fo:block>
          </fo:block-container>
          <fo:block-container break-before="page">
            <fo:block border-bottom="1px solid #444" color="#444" font-size="12pt" text-align="center" margin-top="1.5em" margin-bottom="1em">Résumé</fo:block>
            <fo:block margin-left="2em" margin-right="2em" font-style="italic" text-align="justify">
              "<xsl:value-of select="abstract"/>"
            </fo:block>
          </fo:block-container>
        </fo:flow>
      </fo:page-sequence>
      <!-- closes the page-sequence -->
    </fo:root>
  </xsl:template>
  <xsl:template match="files/file">
    <fo:list-item>
      <fo:list-item-label end-indent="label-end()">
        <fo:block>
          *
        </fo:block>  
      </fo:list-item-label>
      <fo:list-item-body start-indent="body-start()">
        <fo:block>
          <xsl:value-of select="name"/>: <xsl:value-of select="accessRestriction" />
        </fo:block>
      </fo:list-item-body>
    </fo:list-item>
  </xsl:template>
</xsl:stylesheet>