package io.github.vivibibi30.hyperpguard;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

import java.util.Map;

/**
 * 只读地暴露开关状态，供注入到目标进程的 hook 侧读取。
 *
 * hook 进程拿不到我们应用的私有目录，也没有稳定的 Context 通道，
 * 因此用 ContentProvider 传配置：查询一次得到全部 key/value，内容仅为开关布尔值。
 */
public class ConfigProvider extends ContentProvider {

    public static final String AUTHORITY = "io.github.vivibibi30.hyperpguard.config";
    public static final Uri URI = Uri.parse("content://" + AUTHORITY + "/all");

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        MatrixCursor cursor = new MatrixCursor(new String[]{"k", "v"});
        Context ctx = getContext();
        if (ctx == null) return cursor;
        SharedPreferences sp = Config.ui(ctx);
        for (Map.Entry<String, ?> e : sp.getAll().entrySet()) {
            Object v = e.getValue();
            if (v == null) continue;
            cursor.addRow(new Object[]{e.getKey(), String.valueOf(v)});
        }
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        return "vnd.android.cursor.dir/vnd.hyperpguard.config";
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("read-only");
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("read-only");
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("read-only");
    }
}
