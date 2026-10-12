package com.hypergryph.arknights;

import com.hypergryph.arknights.game.quest;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArknightsApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    void resolveOccPercentHandlesAlmost() throws Exception {
        Method method = quest.class.getDeclaredMethod("resolveOccPercent", String.class);
        method.setAccessible(true);

        assertEquals(1, method.invoke(new quest(null), "ALMOST"));
        assertEquals(4, method.invoke(new quest(null), "SOMETIMES"));
        assertEquals(0, method.invoke(new quest(null), "ALWAYS"));
    }
}
