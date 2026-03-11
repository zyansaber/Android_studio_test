package com.example.githubdemoapp;

import static org.junit.Assert.assertEquals;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() {
        String packageName = InstrumentationRegistry.getInstrumentation().getTargetContext().getPackageName();
        assertEquals("snowyriver.app", packageName);
    }
}
