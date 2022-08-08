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

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.core.app.ApplicationProvider;
import com.usefulness.testing.screenshot.tests.test.R;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertEquals;

/**
 * Tests {@link ViewHelpers}
 */
@RunWith(AndroidJUnit4.class)
public class ViewHelpersTest {
    private TextView mTextView;
    private Context targetContext;

    @Before
    public void setUp() {
        targetContext = ApplicationProvider.getApplicationContext();
        mTextView = new TextView(targetContext);
        mTextView.setText("foobar");
    }

    @Test
    public void testPreconditions() {
        assertEquals(0, mTextView.getMeasuredHeight());
    }

    @Test
    public void testMeasureWithoutHeight() {
        ViewHelpers.setupView(mTextView).setExactWidthDp(100).layout();

        assertThat(mTextView.getMeasuredHeight()).isGreaterThan(0);
    }

    @Test
    public void testMeasureWithoutHeightPx() {
        ViewHelpers.setupView(mTextView).setExactWidthPx(100).layout();

        assertThat(mTextView.getMeasuredHeight()).isGreaterThan(0);
    }

    @Test
    public void testMeasureForOnlyWidth() {
        ViewHelpers.setupView(mTextView).setExactHeightPx(100).layout();

        assertThat(mTextView.getMeasuredHeight()).isEqualTo(100);
        assertThat(mTextView.getMeasuredWidth()).isGreaterThan(0);
    }

    @Test
    public void testBothWrapContent() {
        ViewHelpers.setupView(mTextView).layout();

        assertThat(mTextView.getMeasuredHeight()).isGreaterThan(0);
        assertThat(mTextView.getMeasuredWidth()).isGreaterThan(0);
    }

    @Test
    public void testHeightAndWidthCorrectlyPropagated() {
        ViewHelpers.setupView(mTextView).setExactHeightDp(100).setExactWidthDp(1000).layout();

        assertThat(mTextView.getMeasuredWidth()).isGreaterThan(mTextView.getMeasuredHeight());
    }

    @Test
    public void testListViewHeight() {
        ListView view = new ListView(targetContext);
        view.setDividerHeight(0);
        ArrayAdapter<String> adapter =
                new ArrayAdapter<String>(targetContext, R.layout.testing_simple_textview);
        view.setAdapter(adapter);

        for (int i = 0; i < 20; i++) {
            adapter.add("foo");
        }

        ViewHelpers.setupView(view).guessListViewHeight().setExactWidthDp(200).layout();

        assertThat(view.getMeasuredHeight()).isGreaterThan(10);

        int oneHeight = view.getChildAt(0).getMeasuredHeight();
        assertThat(view.getMeasuredHeight()).isEqualTo(oneHeight * 20);
    }

    @Test
    public void testMaxHeightLessThanHeight() {
        ViewHelpers.setupView(mTextView).setMaxHeightPx(100).layout();
        assertThat(mTextView.getMeasuredHeight()).isLessThan(100);
    }

    @Test
    public void testMaxHeightUsesFullHeight() {
        ViewHelpers.setupView(mTextView).setMaxHeightPx(1).layout();
        assertThat(mTextView.getMeasuredHeight()).isEqualTo(1);
    }
}
