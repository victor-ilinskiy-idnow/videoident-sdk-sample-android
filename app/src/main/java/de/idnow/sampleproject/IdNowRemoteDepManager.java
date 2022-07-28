package de.idnow.sampleproject;

import android.content.Context;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.annotation.NonNull;

public class IdNowRemoteDepManager {
    private final UnzipArchiveRunner unzipper = new UnzipArchiveRunner();
    private final DownloadFileFromUrlRunner downloader = new DownloadFileFromUrlRunner();

    private static String baseServerUrl = "http://192.168.178.58:80";

    public interface Callback {
        void onComplete();
        void onFail(Exception e);
    }

    enum DependencyType {
        ICE_LINK
    }

    public void ensureDependency(DependencyType dep, Context context, Callback callback) {
        boolean allExist = true;
        String[] libFileNames = filePathsForDependency(dep, context);
        for (String name: libFileNames) {
            File currentFile = new File(name);
            if (!currentFile.isFile()) {
                allExist = false;
            }
        }

        if (allExist) {
            callback.onComplete();
            return;
        }

        for(String path: libFileNames) {
            File file = new File(path);
            if (file.exists()) {
                file.delete();
            }
        }

        try {
            downloader.executeAsync(
                    new DownloadFileFromUrlRunner.Task(
                            new DownloadFileFromUrlRunner.Input(getUrlForDependency(dep), getTempFilePath()
                            )), new DownloadFileFromUrlRunner.Callback() {
                        @Override
                        public void onComplete(String zipPath) {
                            unzipper.executeAsync(new UnzipArchiveRunner.Task(
                                            new UnzipArchiveRunner.Input(zipPath, getLibsPath(context))
                                    ), new UnzipArchiveRunner.Callback() {
                                        @Override
                                        public void onComplete(String result) {
                                            callback.onComplete();
                                        }

                                        @Override
                                        public void onFail(Exception e) {
                                            callback.onFail(e);
                                        }
                                    }
                            );
                        }

                        @Override
                        public void onFail(Exception e) {
                            callback.onFail(e);
                        }
                    }
            );
        } catch (Exception e) {
            callback.onFail(e);
        }


    }

    public void loadDependency(DependencyType dep, Context context, Callback callback) {
        ensureDependency(dep, context, new Callback() {
            @Override
            public void onComplete() {
                try {
                    for(String path: filePathsForDependency(dep, context)) {
                        System.load(path);
                    }

                    callback.onComplete();
                } catch (Exception e) {
                    callback.onFail(e);
                }
            }

            @Override
            public void onFail(Exception e) {
                callback.onFail(e);
            }
        });
    }

    private static String getLibsPath(@NonNull Context context) {
        return context.getFilesDir().getPath();
    }

    private static String getTempFilePath() throws IOException {
        return File.createTempFile("idnow-dep", ".zip").getAbsolutePath();
    }

    private String[] fileNamesForDependency(DependencyType dep) {
        switch (dep) {
            case ICE_LINK:
                return new String[] {
                        "libaudioprocessingfmJNI.so",
                        "libopusfmJNI.so",
                        "libvpxfmJNI.so",
                        "libyuvfmJNI.so",
                };
        }

        return new String[] {};
    }

    private String getUrlForDependency(DependencyType dep) {
        String path = "";
        switch (dep) {
            case ICE_LINK:
                path = "fm-ice-link.zip";
        }

        return baseServerUrl + "/" + path;
    }

    private String[] filePathsForDependency(DependencyType dep, @NonNull Context context) {
        ArrayList<String> res = new ArrayList<>();
        for (String e: fileNamesForDependency(dep)) {
            res.add(getLibsPath(context) + "/" + e);
        }

        return res.toArray(new String[0]);
    }
}
