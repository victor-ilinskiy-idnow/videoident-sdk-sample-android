package de.idnow.sampleproject;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

class UnzipArchiveRunner {
    static class Input {
        Input(String zipFilePath, String outPath) {
            this.zipFilePath = zipFilePath;
            this.outPath = outPath;
        }

        String zipFilePath;
        String outPath;
    }

    static class Task implements Callable<String> {
        private final Input input;

        public Task(Input input) {
            this.input = input;
        }

        @Override
        public String call() throws IOException {
            int count;
            try {
                final File inputFile = new File(input.zipFilePath);
                final File outputFile = new File(input.outPath);
                unzip(inputFile, outputFile);

            } catch (Exception e) {
                Log.e("Error: ", e.getMessage());
                throw e;
            }

            return input.outPath;
        }

        private void unzip(File zipFile, File targetDirectory) throws IOException {
            try (ZipInputStream zis = new ZipInputStream(
                    new BufferedInputStream(new FileInputStream(zipFile)))) {
                ZipEntry ze;
                int count;
                byte[] buffer = new byte[8192];
                while ((ze = zis.getNextEntry()) != null) {
                    File file = new File(targetDirectory, ze.getName());
                    File dir = ze.isDirectory() ? file : file.getParentFile();
                    assert dir != null;
                    if (!dir.isDirectory() && !dir.mkdirs())
                        throw new FileNotFoundException("Failed to ensure directory: " +
                                dir.getAbsolutePath());
                    if (ze.isDirectory())
                        continue;
                    try (FileOutputStream fout = new FileOutputStream(file)) {
                        while ((count = zis.read(buffer)) != -1)
                            fout.write(buffer, 0, count);
                    }
                }
            }
        }
    }

    private final Executor executor = Executors.newSingleThreadExecutor(); // change according to your requirements
    private final Handler handler = new Handler(Looper.getMainLooper());

    public interface Callback {
        void onComplete(String result);
        void onFail(Exception e);
    }

    public void executeAsync(Callable<String> callable, Callback callback) {
        executor.execute(() -> {
            try {
                final String result = callable.call();
                handler.post(() -> {
                    callback.onComplete(result);
                });
            } catch (Exception e) {
                handler.post(() -> {
                    callback.onFail(e);
                });
            }
        });
    }
}