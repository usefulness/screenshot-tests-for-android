/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.testing.screenshot;

import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import io.github.usefulness.testing.screenshot.Screenshot;

/**
 * This is not really a test, this test is just a "fixture" for all the tests for the scripts
 * related to running tests and getting screenshots.
 */
@RunWith(AndroidJUnit4.class)
public class ScriptsFixtureTest {
    private static final int HEIGHT = 100;
    private static final int WIDTH = 200;

    private TextView mTextView;

    @Before
    public void setUp() {
        mTextView = new TextView(ApplicationProvider.getApplicationContext());
        mTextView.setText("foobar");

        // Unfortunately TextView needs a LayoutParams for onDraw
        mTextView.setLayoutParams(
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        measureAndLayout();
    }

    @Test
    public void testGetTextViewScreenshot() {
        Screenshot.snap(mTextView).record();
    }

    @Test
    public void testSecondScreenshot() {
        mTextView.setText("foobar3");
        measureAndLayout();
        Screenshot.snap(mTextView).record();
    }

    private void measureAndLayout() {
        final Throwable[] exceptions = new Throwable[1];
        InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(() -> {
                    try {
                        mTextView.measure(
                                View.MeasureSpec.makeMeasureSpec(WIDTH, View.MeasureSpec.EXACTLY),
                                View.MeasureSpec.makeMeasureSpec(HEIGHT, View.MeasureSpec.EXACTLY));
                        mTextView.layout(
                                0, 0, mTextView.getMeasuredWidth(), mTextView.getMeasuredHeight());
                    } catch (Throwable throwable) {
                        exceptions[0] = throwable;
                    }
                });
        if (exceptions[0] != null) {
            throw new RuntimeException(exceptions[0]);
        }
    }
}
