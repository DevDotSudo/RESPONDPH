package com.ionres.respondph.common.interfaces;

public interface BulkProgressListener {
    void onProgress(int done, int total, int successCount, String method);
    void onFinished(int total, int successCount, String method);
}
