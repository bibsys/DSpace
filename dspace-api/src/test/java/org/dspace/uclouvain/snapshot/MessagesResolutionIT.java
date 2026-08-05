/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.snapshot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Locale;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.core.I18nUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Locks down how a message is resolved, because the expected chain is subtle and was already broken once.
 *
 * The contract is: <b>Messages_&lt;asked&gt; &rarr; Messages (base) &rarr; the key itself</b>.
 * The locale of the JVM must NEVER take part in it.
 *
 * DEV NOTE :: this relies on `I18nUtil` keeping `ResourceBundle.Control.getNoFallbackControl(...)`. Without it,
 *             `getBundle` resolves a locale that has no bundle of its own (say `de`) to the base bundle, notices the
 *             base bundle is not the asked locale, and THEN tries the JVM default locale before settling -- which
 *             silently serves French strings to a German (or English) reader.
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public class MessagesResolutionIT extends AbstractIntegrationTestWithDatabase {

    /** A core DSpace key defined in `Messages.properties` only, so it exercises the fallback to the base bundle */
    private static final String BASE_ONLY_KEY = "itemRequest.all";
    /** A UCLouvain key defined in both `Messages.properties` (English) and `Messages_fr.properties` */
    private static final String TRANSLATED_KEY = "snapshot.email.metadata.operation.add";

    private Locale savedDefaultLocale;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        savedDefaultLocale = Locale.getDefault();
    }

    @After
    public void restoreDefaultLocale() {
        Locale.setDefault(savedDefaultLocale);
    }

    /**
     * NON-REGRESSION TEST :: the JVM locale must not leak into a translation.
     *
     * A locale with no bundle of its own must fall back to the BASE bundle (English), whatever the server happens to
     * be configured with. Serving French to a German reader just because the server runs in French is the exact
     * defect this guards against.
     */
    @Test
    public void testLocaleWithoutBundleFallsBackToBaseBundleNotToJvmLocale() {
        for (String jvmLocale : new String[] {"fr", "en", "de"}) {
            Locale.setDefault(Locale.forLanguageTag(jvmLocale));
            assertEquals(
                "a locale without its own bundle must be served the base bundle, JVM locale being " + jvmLocale,
                "added",
                I18nUtil.getMessage(TRANSLATED_KEY, Locale.forLanguageTag("de"))
            );
        }
    }

    /** A key missing from the asked translation must fall back to the base bundle, not to the key */
    @Test
    public void testKeyMissingFromTranslationFallsBackToBaseBundle() {
        assertEquals("all", I18nUtil.getMessage(BASE_ONLY_KEY, Locale.FRENCH));
        assertEquals("all", I18nUtil.getMessage(BASE_ONLY_KEY, Locale.ENGLISH));
    }

    /** A translated key must be served in the asked language, including for a regional variant */
    @Test
    public void testTranslatedKeyIsServedInTheAskedLanguage() {
        assertEquals("ajoutée", I18nUtil.getMessage(TRANSLATED_KEY, Locale.FRENCH));
        assertEquals("ajoutée", I18nUtil.getMessage(TRANSLATED_KEY, Locale.forLanguageTag("fr-BE")));
        assertEquals("added", I18nUtil.getMessage(TRANSLATED_KEY, Locale.ENGLISH));
    }

    /**
     * NON-REGRESSION TEST :: asking for English on a French server must serve English.
     *
     * There is deliberately NO `Messages_en.properties`: in DSpace the base bundle IS the English one. So asking for
     * `en` resolves to the base bundle, and the JVM locale must not be consulted on the way. This is the exact case
     * that silently regressed, and it stayed green for a while only because a stale `Messages_en.properties` was
     * lingering in `target/classes` -- hence the explicit check below that the file really is absent.
     */
    @Test
    public void testAskingEnglishOnAFrenchServerServesEnglishFromTheBaseBundle() {
        assertNull("Messages_en.properties must NOT exist: Messages.properties is the English bundle",
            getClass().getClassLoader().getResource("Messages_en.properties"));

        Locale.setDefault(Locale.FRENCH);
        assertEquals("added", I18nUtil.getMessage(TRANSLATED_KEY, Locale.ENGLISH));
        assertEquals("all", I18nUtil.getMessage(BASE_ONLY_KEY, Locale.ENGLISH));
    }

    /** Last resort: a key defined nowhere is returned as-is rather than raising */
    @Test
    public void testUnknownKeyIsReturnedAsIs() {
        assertEquals("totally.unknown.key", I18nUtil.getMessage("totally.unknown.key", Locale.FRENCH));
    }
}
