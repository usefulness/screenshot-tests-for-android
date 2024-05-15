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

package com.facebook.testing.screenshot.internal;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import io.github.usefulness.testing.screenshot.ReportArtifactsManager;
import io.github.usefulness.testing.screenshot.ScreenshotDirectories;

/**
 * A "local" implementation of Album.
 */
public class AlbumImpl implements Album {
    private static final int COMPRESSION_QUALITY = 90;

    private final Set<String> mAllNames = new HashSet<>();
    private final MetadataRecorder mMetadataRecorder;
    private final ReportArtifactsManager mReportArtifactsManager;

    /* VisibleForTesting */
    AlbumImpl(ScreenshotDirectories screenshotDirectories) {
        mMetadataRecorder = new MetadataRecorder(screenshotDirectories);
        mReportArtifactsManager = new ReportArtifactsManager(screenshotDirectories);
    }

    /**
     * Creates a "local" album that stores all the images on device.
     */
    public static AlbumImpl create() {
        return new AlbumImpl(new ScreenshotDirectories());
    }

    @Override
    public void flush() {
        mMetadataRecorder.flush();
    }

    @Override
    public String writeBitmap(String name, int tilei, int tilej, Bitmap bitmap) {
        String tileName = generateTileName(name, tilei, tilej);
        String filename = getScreenshotFilenameInternal(tileName);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, COMPRESSION_QUALITY, os);
        mReportArtifactsManager.recordFile(filename, os.toByteArray());
        return tileName;
    }

    /**
     * Same as the public getScreenshotFile() except it returns the File even if the screenshot
     * doesn't exist.
     */
    private static String getScreenshotFilenameInternal(String name) {
        return name + ".png";
    }

    private static String getViewHierarchyFilename(String name) {
        return name + "_dump.json";
    }

    private static String getAxIssuesFilename(String name) {
        return name + "_issues.json";
    }

    @Override
    public void writeAxIssuesFile(String name, String data) {
        writeMetadataFile(getAxIssuesFilename(name), data);
    }

    @Override
    public void writeViewHierarchyFile(String name, String data) {
        writeMetadataFile(getViewHierarchyFilename(name), data);
    }

    public void writeMetadataFile(String name, String data) {
        byte[] out = data.getBytes();
        mReportArtifactsManager.recordFile(name, out);
    }

    /**
     * Add the given record to the album. This is called by RecordBuilderImpl#record() and so is an
     * internal detail.
     */
    @SuppressLint("SetWorldReadable")
    @Override
    public void addRecord(RecordBuilderImpl recordBuilder) throws IOException {
        recordBuilder.checkState();
        if (mAllNames.contains(recordBuilder.getName())) {
            if (recordBuilder.hasExplicitName()) {
                throw new AssertionError(
                    "Can't create multiple screenshots with the same name: " + recordBuilder.getName());
            }

            throw new AssertionError(
                "Can't create multiple screenshots from the same test, or "
                    + "use .setName() to name each screenshot differently");
        }

        Tiling tiling = recordBuilder.getTiling();

        MetadataRecorder.ScreenshotMetadataRecorder screenshotNode =
            mMetadataRecorder
                .addNewScreenshot()
                .withDescription(recordBuilder.getDescription())
                .withName(recordBuilder.getName())
                .withTestClass(recordBuilder.getTestClass())
                .withTestName(recordBuilder.getTestName())
                .withTileWidth(tiling.getWidth())
                .withTileHeight(tiling.getHeight())
                .withViewHierarchy(getViewHierarchyFilename(recordBuilder.getName()))
                .withAxIssues(getAxIssuesFilename(recordBuilder.getName()))
                .withExtras(recordBuilder.getExtras());

        if (recordBuilder.getError() != null) {
            screenshotNode.withError(recordBuilder.getError());
        }

        if (recordBuilder.getGroup() != null) {
            screenshotNode.withGroup(recordBuilder.getGroup());
        }

        mAllNames.add(recordBuilder.getName());

        screenshotNode.save();
    }

    /**
     * For a given screenshot, and a tile position, generates a name where we store the screenshot in
     * the album.
     *
     * <p>For backward compatibility with existing screenshot scripts, for the tile (0, 0) we use the
     * name directly.
     */
    private String generateTileName(String name, int i, int j) {
        if (i == 0 && j == 0) {
            return name;
        }

        return String.format("%s_%s_%s", name, i, j);
    }
}
