package de.idnow.sampleproject;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

class DownloadFileFromUrlRunner {
    static class Input {
        Input(String url, String outPath) {
            this.url = url;
            this.outPath = outPath;
        }

        String url;
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
                URL url = new URL(input.url);
                URLConnection conection = url.openConnection();
                conection.connect();

                Log.d("dsadasaere",  url.toString());

                // input stream to read file - with 8k buffer
                InputStream inputStream = new BufferedInputStream(url.openStream(), 8192);

                // Output stream to write file
                OutputStream outputStream = new FileOutputStream(input.outPath);

                byte[] data = new byte[1024];

                long total = 0;

                while ((count = inputStream.read(data)) != -1) {
                    total += count;
                    // writing data to file
                    outputStream.write(data, 0, count);
                }

                // flushing output
                outputStream.flush();

                // closing streams
                outputStream.close();
                inputStream.close();

            } catch (Exception e) {
                Log.e("Error: ", e.getMessage());
                throw e;
            }

            return input.outPath;
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