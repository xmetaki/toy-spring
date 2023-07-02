package org.xmetaki.core;

import org.junit.Before;
import org.junit.Test;
import org.xmetaki.core.config.AppConfig;

import static org.junit.Assert.*;

public class DemoApplicationContextTest {
    private DemoApplicationContext context;
    @Before
    public void setUp() throws Exception {
        this.context = new DemoApplicationContext(AppConfig.class);
    }
    @Test
    public void testPrototypeBean() {
        Object u1 = context.getBean("userService");
        Object u2 = context.getBean("userService");
        assertNotEquals(u1, u2);
    }

    @Test
    public void testSingletonBean() {
        Object u1 = context.getBean("kidService");
        Object u2 = context.getBean("kidService");
        assertEquals(u1, u2);
    }
}