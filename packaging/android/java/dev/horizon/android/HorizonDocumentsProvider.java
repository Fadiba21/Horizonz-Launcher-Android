package dev.horizon.android;

import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract.Document;
import android.provider.DocumentsContract.Root;
import android.provider.DocumentsProvider;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Storage Access Framework (SAF) DocumentsProvider aligned with Vasysik/horizon-android 0.7.7
 * (`bef1eff277871f7be5bc0eed944eb76565188809...125c67284343d649b92ac5b3f9eb5a50f56328ca`):
 * - Exposes Worlds (`saves`), Mods (`horizon-mods`), Resource Packs, Shader Packs, and a virtual
 *   `Configuration` folder (accessing existing game, graphics, touch, and keypad settings in place
 *   without relocating them).
 * - Blocks access/edits to Worlds (`saves`) while a game session is actively running (`zalith-game-running.lock`).
 * - Strictly hides and blocks account, token, keystore, and local API-key files.
 */
public final class HorizonDocumentsProvider extends DocumentsProvider {
    private static final String TAG = "HorizonDocsProvider";
    private static final String ROOT_ID = "horizon_root";
    private static final String ROOT_DOC_ID = "root";
    private static final String CONFIG_VIRTUAL_DOC_ID = "root/Configuration";

    private static final List<String> ALLOWED_CONFIG_FILES = Arrays.asList(
        "options.txt",
        "horizon-extras.txt",
        "horizon-mods.txt",
        "horizon-shaders.txt",
        "horizon-keypad.json",
        "horizon-memory.txt",
        "zalith_config.json"
    );

    private static final Set<String> HIDDEN_SENSITIVE_NAMES = new HashSet<>(Arrays.asList(
        "horizon-gemini-secret.json",
        "zalith_accounts.json",
        "accounts.json",
        "android-auth.json",
        "horizon-control.json",
        "zalith-launch-token.txt",
        "zalith-game-running.lock",
        "zalith-game-exited.flag",
        ".local"
    ));

    private static final String[] DEFAULT_ROOT_PROJECTION = new String[]{
        Root.COLUMN_ROOT_ID,
        Root.COLUMN_FLAGS,
        Root.COLUMN_ICON,
        Root.COLUMN_TITLE,
        Root.COLUMN_SUMMARY,
        Root.COLUMN_DOCUMENT_ID
    };

    private static final String[] DEFAULT_DOCUMENT_PROJECTION = new String[]{
        Document.COLUMN_DOCUMENT_ID,
        Document.COLUMN_MIME_TYPE,
        Document.COLUMN_DISPLAY_NAME,
        Document.COLUMN_LAST_MODIFIED,
        Document.COLUMN_FLAGS,
        Document.COLUMN_SIZE
    };

    private static boolean nativeLoaded = false;

    private static synchronized void ensureNativeLoaded() {
        if (!nativeLoaded) {
            try {
                System.loadLibrary("horizon_android");
                nativeLoaded = true;
            } catch (UnsatisfiedLinkError e) {
                Log.w(TAG, "libhorizon_android.so not loaded in DocumentsProvider, using Java fallback: " + e.getMessage());
            }
        }
    }

    private static native String nativeCall(String home, String operation, String id, String arg) throws FileNotFoundException;
    private static native int nativeOpen(String home, String id, String mode) throws FileNotFoundException;

    @Override
    public boolean onCreate() {
        ensureNativeLoaded();
        return true;
    }

    private String getHomePath() {
        Context ctx = getContext();
        File filesDir = ctx != null ? ctx.getFilesDir() : null;
        return filesDir != null ? filesDir.getAbsolutePath() : "/data/user/0/dev.horizon.android/files";
    }

    private boolean isGameActivelyRunning() {
        File home = new File(getHomePath());
        File lockFile = new File(home, "zalith-game-running.lock");
        if (!lockFile.exists()) return false;
        long ageMs = System.currentTimeMillis() - lockFile.lastModified();
        return ageMs >= 0L && ageMs < 6L * 3600L * 1000L;
    }

    private boolean isSensitiveOrBlockedPath(String docId) {
        if (docId == null || docId.isEmpty()) return false;
        String rel = docId.startsWith("root/") ? docId.substring(5) : docId;
        String[] parts = rel.split("/");
        for (String p : parts) {
            String lower = p.toLowerCase(Locale.US);
            if (HIDDEN_SENSITIVE_NAMES.contains(p) || HIDDEN_SENSITIVE_NAMES.contains(lower)) {
                return true;
            }
            if (lower.contains("secret") || lower.contains("account") || lower.contains("keystore") || lower.contains("token")) {
                return true;
            }
        }
        return false;
    }

    private boolean isWorldDocumentId(String docId) {
        if (docId == null) return false;
        String rel = docId.startsWith("root/") ? docId.substring(5) : docId;
        return rel.equals("saves") || rel.startsWith("saves/")
            || (rel.startsWith("profiles/") && (rel.endsWith("/saves") || rel.contains("/saves/")));
    }

    private void enforceAccessGuards(String docId) throws FileNotFoundException {
        if (isSensitiveOrBlockedPath(docId)) {
            throw new FileNotFoundException("Access to account or credential files is not permitted.");
        }
        if (isWorldDocumentId(docId) && isGameActivelyRunning()) {
            throw new FileNotFoundException("World access is blocked while playing. Return to the launcher before viewing or transferring worlds.");
        }
    }

    @Override
    public Cursor queryRoots(String[] projection) throws FileNotFoundException {
        final MatrixCursor result = new MatrixCursor(projection != null ? projection : DEFAULT_ROOT_PROJECTION);
        final MatrixCursor.RowBuilder row = result.newRow();
        row.add(Root.COLUMN_ROOT_ID, ROOT_ID);
        row.add(Root.COLUMN_FLAGS,
            Root.FLAG_SUPPORTS_CREATE | Root.FLAG_LOCAL_ONLY | Root.FLAG_SUPPORTS_IS_CHILD);
        row.add(Root.COLUMN_ICON, R.drawable.ic_launcher);
        row.add(Root.COLUMN_TITLE, "HorizonZ Launcher (26.3)");
        row.add(Root.COLUMN_SUMMARY, isGameActivelyRunning()
            ? "Configuration, Mods & Packs (Worlds locked while playing)"
            : "Worlds, Configuration, Resource Packs, Mods & Logs");
        row.add(Root.COLUMN_DOCUMENT_ID, ROOT_DOC_ID);
        return result;
    }

    @Override
    public Cursor queryDocument(String documentId, String[] projection) throws FileNotFoundException {
        final MatrixCursor result = new MatrixCursor(projection != null ? projection : DEFAULT_DOCUMENT_PROJECTION);
        String docId = (documentId == null || documentId.isEmpty()) ? ROOT_DOC_ID : documentId;
        enforceAccessGuards(docId);

        if (CONFIG_VIRTUAL_DOC_ID.equals(docId)) {
            includeVirtualConfigDirectoryRow(result);
            return result;
        }

        if (nativeLoaded && !docId.startsWith(CONFIG_VIRTUAL_DOC_ID + "/")) {
            try {
                String jsonStr = nativeCall(getHomePath(), "stat", docId, "");
                if (jsonStr != null && !jsonStr.isEmpty()) {
                    JSONObject obj = new JSONObject(jsonStr);
                    if (!obj.has("error")) {
                        includeJsonRow(result, obj);
                        return result;
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        includeFallbackFile(result, docId);
        return result;
    }

    @Override
    public Cursor queryChildDocuments(String parentDocumentId, String[] projection, String sortOrder) throws FileNotFoundException {
        final MatrixCursor result = new MatrixCursor(projection != null ? projection : DEFAULT_DOCUMENT_PROJECTION);
        String parentId = (parentDocumentId == null || parentDocumentId.isEmpty()) ? ROOT_DOC_ID : parentDocumentId;
        enforceAccessGuards(parentId);

        if (CONFIG_VIRTUAL_DOC_ID.equals(parentId)) {
            File home = new File(getHomePath());
            for (String cfgName : ALLOWED_CONFIG_FILES) {
                File cfgFile = new File(home, cfgName);
                if (cfgFile.isFile()) {
                    includeFallbackFile(result, CONFIG_VIRTUAL_DOC_ID + "/" + cfgName);
                }
            }
            return result;
        }

        if (ROOT_DOC_ID.equals(parentId)) {
            includeVirtualConfigDirectoryRow(result);
        }

        if (nativeLoaded && !parentId.startsWith(CONFIG_VIRTUAL_DOC_ID)) {
            try {
                String jsonStr = nativeCall(getHomePath(), "list", parentId, "");
                if (jsonStr != null && !jsonStr.isEmpty()) {
                    JSONObject obj = new JSONObject(jsonStr);
                    JSONArray arr = obj.optJSONArray("entries");
                    if (arr != null) {
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject entry = arr.getJSONObject(i);
                            String entryDocId = entry.optString("document_id", "");
                            if (isSensitiveOrBlockedPath(entryDocId)) continue;
                            if (isWorldDocumentId(entryDocId) && isGameActivelyRunning()) continue;
                            includeJsonRow(result, entry);
                        }
                        return result;
                    }
                }
            } catch (Throwable ignored) {
            }
        }

        File parent = resolveFallbackFile(parentId);
        File[] children = parent.listFiles();
        if (children != null) {
            for (File child : children) {
                String name = child.getName();
                String childDocId = parentId.equals(ROOT_DOC_ID)
                    ? (ROOT_DOC_ID + "/" + name)
                    : (parentId + "/" + name);
                if (isSensitiveOrBlockedPath(childDocId)) continue;
                if (isWorldDocumentId(childDocId) && isGameActivelyRunning()) continue;
                includeFallbackFile(result, childDocId);
            }
        }
        return result;
    }

    @Override
    public ParcelFileDescriptor openDocument(String documentId, String mode, CancellationSignal signal) throws FileNotFoundException {
        enforceAccessGuards(documentId);
        if (nativeLoaded && documentId != null && !documentId.startsWith(CONFIG_VIRTUAL_DOC_ID)) {
            try {
                int fd = nativeOpen(getHomePath(), documentId, mode);
                if (fd >= 0) {
                    return ParcelFileDescriptor.adoptFd(fd);
                }
            } catch (Throwable ignored) {
            }
        }
        File target = resolveFallbackFile(documentId);
        return ParcelFileDescriptor.open(target, ParcelFileDescriptor.parseMode(mode));
    }

    @Override
    public String createDocument(String parentDocumentId, String mimeType, String displayName) throws FileNotFoundException {
        enforceAccessGuards(parentDocumentId);
        if (isSensitiveOrBlockedPath(displayName)) {
            throw new FileNotFoundException("Cannot create protected account or credential file.");
        }
        File parent = resolveFallbackFile(parentDocumentId);
        File target = new File(parent, displayName);
        try {
            if (Document.MIME_TYPE_DIR.equals(mimeType)) {
                target.mkdirs();
            } else {
                target.getParentFile().mkdirs();
                target.createNewFile();
            }
        } catch (Exception e) {
            throw new FileNotFoundException("Failed to create document: " + e.getMessage());
        }
        return parentDocumentId.equals(ROOT_DOC_ID)
            ? (ROOT_DOC_ID + "/" + displayName)
            : (parentDocumentId + "/" + displayName);
    }

    @Override
    public void deleteDocument(String documentId) throws FileNotFoundException {
        enforceAccessGuards(documentId);
        if (CONFIG_VIRTUAL_DOC_ID.equals(documentId)) {
            throw new FileNotFoundException("Configuration folder cannot be deleted.");
        }
        File target = resolveFallbackFile(documentId);
        deleteRecursive(target);
    }

    @Override
    public boolean isChildDocument(String parentDocumentId, String documentId) {
        if (parentDocumentId == null || documentId == null) return false;
        if (isSensitiveOrBlockedPath(documentId)) return false;
        return documentId.startsWith(parentDocumentId);
    }

    private void deleteRecursive(File f) {
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) {
                for (File c : children) deleteRecursive(c);
            }
        }
        f.delete();
    }

    private File resolveFallbackFile(String docId) throws FileNotFoundException {
        File home = new File(getHomePath());
        if (!home.exists()) home.mkdirs();
        if (docId == null || docId.isEmpty() || docId.equals(ROOT_DOC_ID)) {
            return home;
        }
        if (docId.equals(CONFIG_VIRTUAL_DOC_ID)) {
            return home;
        }
        if (docId.startsWith(CONFIG_VIRTUAL_DOC_ID + "/")) {
            String cfgName = docId.substring((CONFIG_VIRTUAL_DOC_ID + "/").length());
            if (!ALLOWED_CONFIG_FILES.contains(cfgName)) {
                throw new FileNotFoundException("Unrecognized configuration file: " + cfgName);
            }
            return new File(home, cfgName);
        }
        String rel = docId.startsWith("root/") ? docId.substring(5) : docId;
        return new File(home, rel);
    }

    private void includeVirtualConfigDirectoryRow(MatrixCursor result) {
        MatrixCursor.RowBuilder row = result.newRow();
        row.add(Document.COLUMN_DOCUMENT_ID, CONFIG_VIRTUAL_DOC_ID);
        row.add(Document.COLUMN_MIME_TYPE, Document.MIME_TYPE_DIR);
        row.add(Document.COLUMN_DISPLAY_NAME, "Configuration (Game, Graphics, Touch & Keypad)");
        row.add(Document.COLUMN_LAST_MODIFIED, System.currentTimeMillis());
        row.add(Document.COLUMN_FLAGS, Document.FLAG_DIR_SUPPORTS_CREATE);
        row.add(Document.COLUMN_SIZE, 0L);
    }

    private void includeJsonRow(MatrixCursor result, JSONObject obj) {
        boolean isDir = obj.optBoolean("is_dir", false);
        int flags = Document.FLAG_SUPPORTS_DELETE | Document.FLAG_SUPPORTS_WRITE;
        if (isDir) flags |= Document.FLAG_DIR_SUPPORTS_CREATE;

        MatrixCursor.RowBuilder row = result.newRow();
        row.add(Document.COLUMN_DOCUMENT_ID, obj.optString("document_id", ROOT_DOC_ID));
        row.add(Document.COLUMN_MIME_TYPE,
            isDir ? Document.MIME_TYPE_DIR : obj.optString("mime_type", "application/octet-stream"));
        row.add(Document.COLUMN_DISPLAY_NAME, obj.optString("display_name", "HorizonZ"));
        row.add(Document.COLUMN_LAST_MODIFIED, obj.optLong("last_modified", System.currentTimeMillis()));
        row.add(Document.COLUMN_FLAGS, flags);
        row.add(Document.COLUMN_SIZE, obj.optLong("size", 0L));
    }

    private void includeFallbackFile(MatrixCursor result, String docId) throws FileNotFoundException {
        File file = resolveFallbackFile(docId);
        boolean isDir = file.isDirectory() || docId.equals(ROOT_DOC_ID);
        int flags = Document.FLAG_SUPPORTS_DELETE | Document.FLAG_SUPPORTS_WRITE;
        if (isDir) flags |= Document.FLAG_DIR_SUPPORTS_CREATE;

        MatrixCursor.RowBuilder row = result.newRow();
        row.add(Document.COLUMN_DOCUMENT_ID, docId);
        row.add(Document.COLUMN_MIME_TYPE, isDir ? Document.MIME_TYPE_DIR : "application/octet-stream");
        row.add(Document.COLUMN_DISPLAY_NAME, docId.equals(ROOT_DOC_ID) ? "HorizonZ" : file.getName());
        row.add(Document.COLUMN_LAST_MODIFIED, file.lastModified());
        row.add(Document.COLUMN_FLAGS, flags);
        row.add(Document.COLUMN_SIZE, file.length());
    }
}
