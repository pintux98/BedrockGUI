package it.pintux.life.essentialsaddon.util;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandAliasesTest {

    @Test
    void matchesConfiguredAliasIgnoringCaseAndSlash() {
        CommandAliases aliases = CommandAliases.of(List.of("/Home", "homes", " homelist "));

        assertTrue(aliases.matches("home"));
        assertTrue(aliases.matches("HOMES"));
        assertTrue(aliases.matches("/homelist"));
        assertFalse(aliases.matches("sethome"));
    }

    @Test
    void emptyListMatchesNothing() {
        CommandAliases aliases = CommandAliases.of(List.of());

        assertTrue(aliases.isEmpty());
        assertFalse(aliases.matches("home"));
        assertFalse(aliases.matchesMessage("/home"));
    }

    @Test
    void nullAndBlankEntriesAreDropped() {
        CommandAliases aliases = CommandAliases.of(Arrays.asList("home", null, "", "   ", "/"));

        assertEquals(1, aliases.roots().size());
        assertTrue(aliases.matches("home"));
    }

    @Test
    void rootIsTakenFromTheMessageWithoutArguments() {
        assertEquals("home", CommandAliases.rootOf("/home"));
        assertEquals("home", CommandAliases.rootOf("/home base"));
        assertEquals("home", CommandAliases.rootOf("  /Home   base  "));
        assertEquals("tpaccept", CommandAliases.rootOf("/tpaccept Steve"));
    }

    @Test
    void namespacedCommandsResolveToTheirRoot() {
        assertEquals("homelist", CommandAliases.rootOf("/huskhomes:homelist"));
        assertTrue(CommandAliases.of("homelist").matchesMessage("/huskhomes:homelist"));
        assertTrue(CommandAliases.of("/essentials:home").matches("home"));
    }

    @Test
    void messagesWithoutACommandYieldNoRoot() {
        assertNull(CommandAliases.rootOf(null));
        assertNull(CommandAliases.rootOf("/"));
        assertNull(CommandAliases.rootOf("   "));
    }

    @Test
    void argumentsAreSplitOnWhitespace() {
        assertArrayEquals(new String[0], CommandAliases.argsOf("/home"));
        assertArrayEquals(new String[0], CommandAliases.argsOf("/home   "));
        assertArrayEquals(new String[]{"base"}, CommandAliases.argsOf("/home base"));
        assertArrayEquals(new String[]{"all"}, CommandAliases.argsOf("/sell   all"));
        assertArrayEquals(new String[]{"Steve", "nodebuff", "3"},
                CommandAliases.argsOf("/duel Steve nodebuff 3"));
        assertArrayEquals(new String[0], CommandAliases.argsOf(null));
    }

    @Test
    void varargsFactoryBehavesLikeTheListFactory() {
        assertTrue(CommandAliases.of("warp", "warps", "warplist").matches("warplist"));
        assertTrue(CommandAliases.of((String[]) null).isEmpty());
    }
}
