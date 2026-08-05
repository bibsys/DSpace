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
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.dspace.AbstractUnitTest;
import org.dspace.uclouvain.administer.SnapshotCLI;
import org.dspace.uclouvain.core.NotificationType;
import org.junit.Test;

/**
 * Unit tests about how {@link SnapshotCLI} reads its command line.
 *
 * DEV NOTE :: options and helpers are reached through reflection, since they are private to the CLI. The class
 *             extends {@link AbstractUnitTest} only because `extraValidationCLIArgument` is an instance method:
 *             building a `SnapshotCLI` needs a live DSpace kernel, even though argument parsing itself reads no
 *             state.
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public class SnapshotCLIArgumentsTest extends AbstractUnitTest {

    /** Rebuild the very same option set the CLI declares, without instantiating it */
    private Options serviceOptions() throws Exception {
        Options options = new Options();
        for (String field : new String[] {"OPT_SILENT", "OPT_NOTIFY", "OPT_LIMIT"}) {
            java.lang.reflect.Field declared = SnapshotCLI.class.getDeclaredField(field);
            declared.setAccessible(true);
            options.addOption((org.apache.commons.cli.Option) declared.get(null));
        }
        return options;
    }

    private CommandLine parse(String... args) throws Exception {
        return new DefaultParser().parse(serviceOptions(), args, true);
    }

    private <T> T invokeStatic(String method, CommandLine cl) throws Exception {
        Method m = SnapshotCLI.class.getDeclaredMethod(method, CommandLine.class);
        m.setAccessible(true);
        return (T) m.invoke(null, cl);
    }

    private void validate(CommandLine cl) throws Throwable {
        java.lang.reflect.Constructor<SnapshotCLI> constructor = SnapshotCLI.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        Method m = SnapshotCLI.class.getDeclaredMethod("extraValidationCLIArgument", CommandLine.class);
        m.setAccessible(true);
        try {
            m.invoke(constructor.newInstance(), cl);
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw e.getCause();
        }
    }

    private int itemsLimit(CommandLine cl, int fallback) throws Throwable {
        Method m = SnapshotCLI.class.getDeclaredMethod("parseItemsLimit", CommandLine.class, int.class);
        m.setAccessible(true);
        try {
            return (int) m.invoke(null, cl, fallback);
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw e.getCause();
        }
    }

    /** Warning recipients is the norm: no option at all must still notify, by e-mail */
    @Test
    public void testNotifyingByEmailIsTheDefault() throws Exception {
        assertEquals(NotificationType.EMAIL, invokeStatic("parseNotificationType", parse()));
    }

    /** `--silent` is the only way to warn nobody */
    @Test
    public void testSilentDisablesEveryNotification() throws Exception {
        assertNull(invokeStatic("parseNotificationType", parse("--silent")));
        assertNull(invokeStatic("parseNotificationType", parse("-s")));
    }

    /** `--notify` selects another channel, case-insensitively */
    @Test
    public void testNotifySelectsTheRequestedChannel() throws Exception {
        assertEquals(NotificationType.PHONE, invokeStatic("parseNotificationType", parse("--notify", "PHONE")));
        assertEquals(NotificationType.EMAIL, invokeStatic("parseNotificationType", parse("-n", "email")));
    }

    /** Asking to stay silent AND to notify is contradictory, and must be reported as such */
    @Test
    public void testSilentAndNotifyAreMutuallyExclusive() throws Exception {
        Throwable error = assertThrows(ParseException.class, () -> validate(parse("--silent", "--notify", "EMAIL")));
        assertTrue(error.getMessage().contains("mutually exclusive"));
    }

    /** An unknown channel is reported with the accepted values, not as a raw stack trace */
    @Test
    public void testUnknownNotificationTypeIsRejected() throws Exception {
        Throwable error = assertThrows(ParseException.class, () -> validate(parse("--notify", "CARRIER_PIGEON")));
        assertTrue(error.getMessage().contains("CARRIER_PIGEON"));
        assertTrue("the message must list what is accepted", error.getMessage().contains("EMAIL"));
    }

    /** A mistyped UUID is reported by name, rather than blowing up later */
    @Test
    public void testMalformedItemIdIsRejected() throws Exception {
        Throwable error = assertThrows(ParseException.class, () -> validate(parse("not-a-uuid")));
        assertTrue(error.getMessage().contains("not-a-uuid"));
    }

    /** A non numeric limit is reported too */
    @Test
    public void testMalformedLimitIsRejected() throws Exception {
        assertThrows(ParseException.class, () -> validate(parse("--limit", "many")));
    }

    /** An explicit `--limit` always wins, including -1 to lift the limit for a bootstrap run */
    @Test
    public void testExplicitLimitOverridesTheConfiguredOne() throws Throwable {
        assertEquals(250, itemsLimit(parse("--limit", "250"), 1000));
        assertEquals(-1, itemsLimit(parse("--limit", "-1"), 1000));
        assertEquals(1000, itemsLimit(parse(), 1000));
    }

    /** Valid item UUIDs are passed through, in the order they were given */
    @Test
    public void testItemIdsArePassedThrough() throws Throwable {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        CommandLine cl = parse(first.toString(), second.toString());

        validate(cl);
        assertEquals(List.of(first, second), invokeStatic("parseItemIds", cl));
    }
}
