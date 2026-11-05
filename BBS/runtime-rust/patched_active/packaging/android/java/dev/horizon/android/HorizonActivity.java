package dev.horizon.android;

import android.app.Dialog;
import android.app.NativeActivity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.system.Os;
import android.util.Log;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import dev.horizon.android.ui.ZalithMainActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * NativeActivity for the Vasysik/horizon-android + horizon-rust/horizon 26.3 engine.
 * Includes:
 * - Guaranteed single-shot launch & clean return to ZalithMainActivity on game exit.
 * - Full-Screen Separate-Window Dialog Splash Overlay (`HorizonSplashOverlayView`) so Android's
 *   SurfaceFlinger renders the splash screen ON TOP of `NativeActivity`'s `takeSurface()` window
 *   all the way until the Minecraft 26.3 Main Menu is rendered (zero black screen gap!).
 */
public final class HorizonActivity extends NativeActivity {
    private static final String TAG = "HorizonActivity";
    public static final int SAF_DOCUMENT_REQUEST_CODE = 4101;
    private volatile boolean watchdogRunning = true;
    private Dialog splashDialog;
    private HorizonSplashOverlayView splashOverlayView;
    private Dialog aiFloatingButtonDialog;
    private Dialog aiCommandPanelDialog;
    private volatile boolean aiTaskRunning = false;
    private volatile boolean aiTaskAbortRequested = false;
    private volatile String lastAiReplyText = "Ready. Enter any command or task for Minecraft 26.3.";

    static {
        try {
            System.loadLibrary("horizon_android");
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "libhorizon_android.so could not be preloaded: " + e.getMessage());
        }
    }

    private native void nativeDocumentResult(int request, int result, String uri);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        File homeDir = getFilesDir();
        File tokenFile = new File(homeDir, "zalith-launch-token.txt");
        File exitFlag = new File(homeDir, "zalith-game-exited.flag");

        if (savedInstanceState != null || exitFlag.exists() || !tokenFile.exists()) {
            Log.i(TAG, "No active launch token (or exit flag present) — returning to ZalithMainActivity.");
            exitFlag.delete();
            tokenFile.delete();
            returnToLauncherAndFinish();
            super.onCreate(null);
            return;
        }

        tokenFile.delete();
        exitFlag.delete();
        try {
            File runningLock = new File(homeDir, "zalith-game-running.lock");
            try (FileOutputStream fos = new FileOutputStream(runningLock, false)) {
                fos.write("running".getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception ignored) {
        }

        writeBootMilestone(homeDir, "15|Initializing HorizonZ Native Runtime...");

        appendCrossProcessLog(homeDir, "[GameProcess] Starting HorizonActivity (:game process) for Minecraft 26.3.");
        ensureEngineDataExtracted();
        applyZalithLaunchEnvironment(getIntent());
        applyRequestedOrientationFromIntent(getIntent());
        enableImmersiveFullscreen();

        // Show the Separate-Window Splash Dialog BEFORE super.onCreate so there is zero black flash!
        showHorizonSplashDialog(homeDir);

        startExitWatchdog(homeDir);
        startGeminiAiAgentBridgeIfEnabled(homeDir, getIntent());
        super.onCreate(savedInstanceState);
    }

    private void showHorizonSplashDialog(final File homeDir) {
        try {
            splashDialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
            splashDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            splashDialog.setCancelable(false);

            splashOverlayView = new HorizonSplashOverlayView(this, homeDir, () -> {
                dismissSplashDialogSafely();
            });
            splashDialog.setContentView(
                splashOverlayView,
                new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            );

            Window win = splashDialog.getWindow();
            if (win != null) {
                win.setBackgroundDrawable(new ColorDrawable(Color.BLACK));
                win.setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT
                );
                win.addFlags(
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_FULLSCREEN
                        | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                );
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    WindowManager.LayoutParams lp = win.getAttributes();
                    lp.layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
                    win.setAttributes(lp);
                }
                win.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                );
            }
            splashDialog.show();
        } catch (Exception e) {
            Log.w(TAG, "Could not show splash dialog: " + e.getMessage());
        }
    }

    private void dismissSplashDialogSafely() {
        try {
            if (splashOverlayView != null) {
                splashOverlayView.stopPolling();
                if (splashOverlayView.getParent() instanceof ViewGroup) {
                    ((ViewGroup) splashOverlayView.getParent()).removeView(splashOverlayView);
                }
            }
            if (splashDialog != null && splashDialog.isShowing()) {
                splashDialog.dismiss();
            }
        } catch (Exception ignored) {
        }
        splashOverlayView = null;
        splashDialog = null;
        enableImmersiveFullscreen();
    }

    private void writeBootMilestone(File homeDir, String content) {
        try {
            File bootFile = new File(homeDir, "zalith-game-boot.txt");
            try (FileOutputStream fos = new FileOutputStream(bootFile, false)) {
                fos.write(content.getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception ignored) {
        }
    }

    private void startExitWatchdog(final File homeDir) {
        Thread watchdog = new Thread(() -> {
            File exitFlag = new File(homeDir, "zalith-game-exited.flag");
            while (watchdogRunning) {
                try {
                    if (exitFlag.exists()) {
                        exitFlag.delete();
                        appendCrossProcessLog(homeDir, "[GameProcess] Quit Game detected — closing :game activity and returning to HorizonZ Launcher.");
                        runOnUiThread(this::returnToLauncherAndFinish);
                        break;
                    }
                    Thread.sleep(400L);
                } catch (InterruptedException ignored) {
                    break;
                }
            }
        }, "HorizonZ-ExitWatchdog");
        watchdog.setDaemon(true);
        watchdog.setPriority(Thread.MIN_PRIORITY);
        watchdog.start();
    }

    private String formatModelDisplayName(String modelId) {
        if (modelId == null || modelId.trim().isEmpty()) return "Gemini 4 Argon";
        String clean = modelId.trim();
        if (clean.startsWith("models/")) clean = clean.substring(7);
        switch (clean.toLowerCase()) {
            case "gemini-4-argon": return "Gemini 4 Argon";
            case "gemini-4.0-ultra": return "Gemini 4.0 Ultra";
            case "gemini-4.0-pro": return "Gemini 4.0 Pro";
            case "gemini-4.0-flash": return "Gemini 4.0 Flash";
            case "gemini-4.0-flash-lite": return "Gemini 4.0 Flash-Lite";
            case "gemini-3.8-pro": return "Gemini 3.8 Pro";
            case "gemini-3.8-flash": return "Gemini 3.8 Flash";
            case "gemini-3.8-flash-lite": return "Gemini 3.8 Flash-Lite";
            case "gemini-3.5-pro": return "Gemini 3.5 Pro";
            case "gemini-3.5-flash": return "Gemini 3.5 Flash";
            case "gemini-3.5-flash-lite": return "Gemini 3.5 Flash-Lite";
            case "gemini-3.1-pro-preview": return "Gemini 3.1 Pro Preview";
            case "gemini-3.1-flash-lite-preview": return "Gemini 3.1 Flash-Lite Preview";
            case "gemini-3.0-pro": return "Gemini 3.0 Pro";
            case "gemini-3.0-flash": return "Gemini 3.0 Flash";
            case "gemini-2.5-pro": return "Gemini 2.5 Pro";
            case "gemini-2.5-flash": return "Gemini 2.5 Flash";
            case "gemini-2.5-flash-lite": return "Gemini 2.5 Flash-Lite";
            case "gemini-2.0-flash": return "Gemini 2.0 Flash";
            case "gemini-2.0-flash-lite": return "Gemini 2.0 Flash-Lite";
            case "gemma-3-27b-it": return "Gemma 3 27B";
            default: return clean;
        }
    }

    /**
     * In-Game AI Agent System running directly inside the `:game` process (`HorizonActivity`):
     * - Minimalist monochrome in-game HUD pill (`AI`) integrated into the top-left in-game control bar.
     * - Minimalist monochrome popup with zero gimmick buttons (`Stop`, `Clear`, `Close`, output view, prompt input + `Run`).
     * - Full autonomous execution across Minecraft 26.3 (`command ...`, `goto`, `find-block`, `break`, `place`,
     *   `move`, `jump`, `sneak`, `sprint`, `look`, `attack`, `interact`, `use-item`, `select-slot`, `click-slot`).
     */
    private void startGeminiAiAgentBridgeIfEnabled(final File homeDir, final Intent intent) {
        if (homeDir == null) return;
        final String localKeyCheck = readLocalGeminiKey(homeDir);
        final boolean intentEnabled = intent != null && intent.getBooleanExtra("HORIZON_AI_AGENT_ENABLED", false);
        if (!intentEnabled && localKeyCheck.isEmpty()) {
            return;
        }
        final String rawModelId = (intent != null && intent.getStringExtra("HORIZON_GEMINI_MODEL") != null)
            ? intent.getStringExtra("HORIZON_GEMINI_MODEL")
            : "gemini-4-argon";
        final String modelId = rawModelId.startsWith("models/") ? rawModelId.substring(7).trim() : rawModelId.trim();
        final boolean autoAssistChat = intent == null || intent.getBooleanExtra("HORIZON_AI_AGENT_AUTO_CHAT", true);
        final boolean autonomousActions = intent == null || intent.getBooleanExtra("HORIZON_AI_AGENT_AUTONOMOUS", true);
        final String taskPrompt = (intent != null && intent.getStringExtra("HORIZON_AI_AGENT_TASK") != null)
            ? intent.getStringExtra("HORIZON_AI_AGENT_TASK")
            : "Bantu player melakukan apapun di dalam Minecraft 26.3.";

        // Attach minimalist monochrome in-game system "AI" button once splash finishes
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (watchdogRunning && !isFinishing()) {
                showInGameAiFloatingButton(homeDir, modelId, taskPrompt, autonomousActions);
            }
        }, 3600L);

        Thread agentThread = new Thread(() -> {
            String localApiKey = readLocalGeminiKey(homeDir);
            if (!localApiKey.isEmpty()) {
                appendCrossProcessLog(homeDir, "[GeminiAgent] In-game AI system active (model=" + modelId + ").");
            }
            String lastHandledChatLine = "";
            boolean greetedInWorld = false;
            File openTriggerFlag = new File(homeDir, "horizon-ai-open.flag");

            while (watchdogRunning) {
                try {
                    // Check if native Rust HUD wrote `horizon-ai-open.flag` to open the monochrome AI panel
                    if (openTriggerFlag.exists()) {
                        openTriggerFlag.delete();
                        runOnUiThread(() -> showInGameAiCommandPanel(homeDir, modelId, taskPrompt, autonomousActions));
                    }

                    if (!greetedInWorld) {
                        String stateResp = sendControlCommandToGame(homeDir, "state");
                        if (stateResp != null && !stateResp.isEmpty() && !stateResp.contains("\"error\"")) {
                            greetedInWorld = true;
                            sendControlCommandToGame(
                                homeDir,
                                "chat [AI] " + formatModelDisplayName(modelId) + " ready. Tap 'AI' at top-left or type @ai <task>."
                            );
                        }
                    }

                    if (autoAssistChat && !aiTaskRunning) {
                        String keyNow = readLocalGeminiKey(homeDir);
                        if (!keyNow.isEmpty()) {
                            String triggerMsg = pollLatestChatTriggerFromSocketOrLog(homeDir, lastHandledChatLine);
                            if (triggerMsg != null && !triggerMsg.isEmpty()) {
                                lastHandledChatLine = triggerMsg;
                                executeGeminiPromptInBackground(homeDir, keyNow, modelId, taskPrompt, triggerMsg, autonomousActions, null, null);
                            }
                        }
                    }
                    Thread.sleep(1600L);
                } catch (Exception ignored) {
                    try { Thread.sleep(2200L); } catch (InterruptedException ie) { break; }
                }
            }
        }, "HorizonZ-GeminiAgentBridge");
        agentThread.setDaemon(true);
        agentThread.setPriority(Thread.MIN_PRIORITY);
        agentThread.start();
    }

    private int dp(int v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics()));
    }

    /**
     * Minimalist, monochrome in-game system button ("AI") integrated directly into the `:game` HUD bar
     * right beside the native `...` button, matching the native monochrome HUD geometry.
     */
    private void showInGameAiFloatingButton(
        final File homeDir,
        final String modelId,
        final String taskPrompt,
        final boolean autonomousActions
    ) {
        try {
            if (aiFloatingButtonDialog != null && aiFloatingButtonDialog.isShowing()) return;
            aiFloatingButtonDialog = new Dialog(this, android.R.style.Theme_Translucent_NoTitleBar);
            aiFloatingButtonDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            aiFloatingButtonDialog.setCancelable(false);

            TextView btn = new TextView(this);
            btn.setText("AI");
            btn.setTextColor(Color.parseColor("#F4F4F5"));
            btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
            btn.getPaint().setFakeBoldText(true);
            btn.setGravity(Gravity.CENTER);
            btn.setPadding(dp(10), dp(5), dp(10), dp(5));

            GradientDrawable bg = new GradientDrawable();
            bg.setColor(Color.parseColor("#CC0C0C0E"));
            bg.setCornerRadius(dp(4));
            bg.setStroke(dp(1), Color.parseColor("#44FFFFFF"));
            btn.setBackground(bg);

            btn.setOnClickListener(v -> {
                showInGameAiCommandPanel(homeDir, modelId, taskPrompt, autonomousActions);
            });

            aiFloatingButtonDialog.setContentView(btn);
            Window win = aiFloatingButtonDialog.getWindow();
            if (win != null) {
                win.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                WindowManager.LayoutParams lp = win.getAttributes();
                lp.width = WindowManager.LayoutParams.WRAP_CONTENT;
                lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
                lp.gravity = Gravity.TOP | Gravity.START;
                lp.x = dp(64);
                lp.y = dp(6);
                lp.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
                lp.dimAmount = 0f;
                win.setAttributes(lp);
            }
            aiFloatingButtonDialog.show();
        } catch (Exception e) {
            Log.w(TAG, "Failed to show in-game AI button: " + e.getMessage());
        }
    }

    /**
     * Minimalist, Monochrome In-Game AI Popup:
     * - Strictly monochrome palette (#0C0C0E surface, #16161A inner box, #2E2E34 1px border, #F4F4F5 / #A1A1AA text).
     * - No gimmick buttons or emoji chips — only functional controls:
     *   Header: Model Name + Status | [Stop] [Clear] [✕]
     *   Body: Clean scrollable execution & response log
     *   Footer: Prompt/Command input field + [Run] button
     */
    private void showInGameAiCommandPanel(
        final File homeDir,
        final String modelId,
        final String taskPrompt,
        final boolean autonomousActions
    ) {
        try {
            if (aiCommandPanelDialog != null && aiCommandPanelDialog.isShowing()) {
                aiCommandPanelDialog.dismiss();
            }
            aiCommandPanelDialog = new Dialog(this, android.R.style.Theme_Translucent_NoTitleBar);
            aiCommandPanelDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            aiCommandPanelDialog.setCancelable(true);

            LinearLayout root = new LinearLayout(this);
            root.setOrientation(LinearLayout.VERTICAL);
            root.setPadding(dp(14), dp(12), dp(14), dp(12));
            GradientDrawable panelBg = new GradientDrawable();
            panelBg.setColor(Color.parseColor("#F20C0C0E"));
            panelBg.setCornerRadius(dp(10));
            panelBg.setStroke(dp(1), Color.parseColor("#FF2E2E34"));
            root.setBackground(panelBg);

            // Header row: Monochrome Title + [Stop] + [Clear] + [✕]
            LinearLayout header = new LinearLayout(this);
            header.setOrientation(LinearLayout.HORIZONTAL);
            header.setGravity(Gravity.CENTER_VERTICAL);

            final TextView titleTv = new TextView(this);
            titleTv.setText("AI AGENT  •  " + formatModelDisplayName(modelId) + (aiTaskRunning ? "  [RUNNING]" : ""));
            titleTv.setTextColor(Color.parseColor("#F4F4F5"));
            titleTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
            titleTv.getPaint().setFakeBoldText(true);
            LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            header.addView(titleTv, titleLp);

            final TextView replyTv = new TextView(this);

            // [Stop] button (stops any running autonomous action or movement immediately)
            Button stopBtn = new Button(this);
            stopBtn.setText("Stop");
            stopBtn.setAllCaps(false);
            stopBtn.setTextColor(Color.parseColor("#F4F4F5"));
            stopBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
            stopBtn.setPadding(dp(8), 0, dp(8), 0);
            GradientDrawable stopBg = new GradientDrawable();
            stopBg.setColor(Color.parseColor("#FF1C1C20"));
            stopBg.setCornerRadius(dp(6));
            stopBg.setStroke(dp(1), Color.parseColor("#FF3F3F46"));
            stopBtn.setBackground(stopBg);
            stopBtn.setOnClickListener(v -> {
                aiTaskAbortRequested = true;
                aiTaskRunning = false;
                titleTv.setText("AI AGENT  •  " + formatModelDisplayName(modelId));
                new Thread(() -> {
                    sendControlCommandToGame(homeDir, "sneak false");
                    sendControlCommandToGame(homeDir, "sprint false");
                }).start();
                lastAiReplyText = "Stopped active AI task.";
                replyTv.setText(lastAiReplyText);
            });
            LinearLayout.LayoutParams utilBtnLp = new LinearLayout.LayoutParams(dp(52), dp(26));
            utilBtnLp.rightMargin = dp(6);
            header.addView(stopBtn, utilBtnLp);

            // [Clear] button
            Button clearBtn = new Button(this);
            clearBtn.setText("Clear");
            clearBtn.setAllCaps(false);
            clearBtn.setTextColor(Color.parseColor("#A1A1AA"));
            clearBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
            clearBtn.setPadding(dp(8), 0, dp(8), 0);
            GradientDrawable clearBg = new GradientDrawable();
            clearBg.setColor(Color.parseColor("#FF16161A"));
            clearBg.setCornerRadius(dp(6));
            clearBg.setStroke(dp(1), Color.parseColor("#FF27272A"));
            clearBtn.setBackground(clearBg);
            clearBtn.setOnClickListener(v -> {
                lastAiReplyText = "Ready.";
                replyTv.setText(lastAiReplyText);
            });
            LinearLayout.LayoutParams clearLp = new LinearLayout.LayoutParams(dp(52), dp(26));
            clearLp.rightMargin = dp(6);
            header.addView(clearBtn, clearLp);

            // [✕] Close button
            Button closeBtn = new Button(this);
            closeBtn.setText("✕");
            closeBtn.setAllCaps(false);
            closeBtn.setTextColor(Color.parseColor("#F4F4F5"));
            closeBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
            closeBtn.setPadding(0, 0, 0, 0);
            GradientDrawable closeBg = new GradientDrawable();
            closeBg.setColor(Color.parseColor("#FF1C1C20"));
            closeBg.setCornerRadius(dp(6));
            closeBg.setStroke(dp(1), Color.parseColor("#FF2E2E34"));
            closeBtn.setBackground(closeBg);
            closeBtn.setOnClickListener(v -> {
                aiCommandPanelDialog.dismiss();
                enableImmersiveFullscreen();
            });
            header.addView(closeBtn, new LinearLayout.LayoutParams(dp(28), dp(26)));
            root.addView(header);

            // Monochrome Output / Execution Log Box
            replyTv.setText(lastAiReplyText);
            replyTv.setTextColor(Color.parseColor("#E4E4E7"));
            replyTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
            replyTv.setPadding(dp(10), dp(8), dp(10), dp(8));
            GradientDrawable replyBg = new GradientDrawable();
            replyBg.setColor(Color.parseColor("#FF121215"));
            replyBg.setCornerRadius(dp(8));
            replyBg.setStroke(dp(1), Color.parseColor("#FF232329"));
            replyTv.setBackground(replyBg);

            ScrollView replyScroll = new ScrollView(this);
            replyScroll.addView(replyTv);
            LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(96)
            );
            scrollLp.topMargin = dp(8);
            scrollLp.bottomMargin = dp(8);
            root.addView(replyScroll, scrollLp);

            // Monochrome Prompt Input & [Run] Button Row
            LinearLayout inputRow = new LinearLayout(this);
            inputRow.setOrientation(LinearLayout.HORIZONTAL);
            inputRow.setGravity(Gravity.CENTER_VERTICAL);

            final EditText promptInput = new EditText(this);
            promptInput.setHint("Instruct AI or enter command (e.g. build shelter, mine oak_log, give diamond, gamemode creative)...");
            promptInput.setHintTextColor(Color.parseColor("#71717A"));
            promptInput.setTextColor(Color.parseColor("#F4F4F5"));
            promptInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
            promptInput.setInputType(InputType.TYPE_CLASS_TEXT);
            promptInput.setPadding(dp(10), dp(7), dp(10), dp(7));
            GradientDrawable inputBg = new GradientDrawable();
            inputBg.setColor(Color.parseColor("#FF16161A"));
            inputBg.setCornerRadius(dp(8));
            inputBg.setStroke(dp(1), Color.parseColor("#FF2E2E34"));
            promptInput.setBackground(inputBg);
            LinearLayout.LayoutParams inLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            inLp.rightMargin = dp(8);
            inputRow.addView(promptInput, inLp);

            Button sendBtn = new Button(this);
            sendBtn.setText("Run");
            sendBtn.setAllCaps(false);
            sendBtn.setTextColor(Color.parseColor("#09090B"));
            sendBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
            sendBtn.getPaint().setFakeBoldText(true);
            GradientDrawable sendBg = new GradientDrawable();
            sendBg.setColor(Color.parseColor("#F4F4F5"));
            sendBg.setCornerRadius(dp(8));
            sendBtn.setBackground(sendBg);
            sendBtn.setOnClickListener(v -> {
                String q = promptInput.getText().toString().trim();
                if (q.isEmpty()) return;
                promptInput.setText("");

                // Support direct raw horizon-ctl or slash commands immediately even without API call
                if (q.startsWith("/")) {
                    final String mcCmd = q.substring(1).trim();
                    replyTv.setText("> command " + mcCmd);
                    new Thread(() -> {
                        String res = sendControlCommandToGame(homeDir, "command " + mcCmd);
                        final String out = "> /" + mcCmd + "\n" + (res != null && !res.isEmpty() ? res : "Executed.");
                        lastAiReplyText = out;
                        runOnUiThread(() -> replyTv.setText(out));
                    }).start();
                    return;
                }

                String key = readLocalGeminiKey(homeDir);
                if (key.isEmpty()) {
                    replyTv.setText("Google AI Studio API Key is not set. Add your API Key in Launcher Settings > AI Agent (or type /<command> for direct execution).");
                    return;
                }
                titleTv.setText("AI AGENT  •  " + formatModelDisplayName(modelId) + "  [RUNNING]");
                replyTv.setText("Planning & executing with " + formatModelDisplayName(modelId) + "...");
                executeGeminiPromptInBackground(homeDir, key, modelId, taskPrompt, q, autonomousActions, replyTv, titleTv);
            });
            inputRow.addView(sendBtn, new LinearLayout.LayoutParams(dp(68), dp(34)));
            root.addView(inputRow);

            aiCommandPanelDialog.setContentView(root);
            Window win = aiCommandPanelDialog.getWindow();
            if (win != null) {
                win.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                WindowManager.LayoutParams lp = win.getAttributes();
                lp.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.76f);
                lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
                lp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
                lp.y = dp(18);
                win.setAttributes(lp);
            }
            aiCommandPanelDialog.show();
        } catch (Exception e) {
            Log.w(TAG, "Failed to show in-game AI command panel: " + e.getMessage());
        }
    }

    private void executeGeminiPromptInBackground(
        final File homeDir,
        final String apiKey,
        final String modelId,
        final String taskPrompt,
        final String userPrompt,
        final boolean autonomousActions,
        final TextView targetReplyView,
        final TextView targetTitleView
    ) {
        new Thread(() -> {
            aiTaskAbortRequested = false;
            aiTaskRunning = true;
            try {
                String stateJson = sendControlCommandToGame(homeDir, "state");
                String invJson = sendControlCommandToGame(homeDir, "inventory");
                String lookJson = sendControlCommandToGame(homeDir, "looking-at");

                String geminiReply = callGeminiAgentApi(
                    apiKey,
                    modelId,
                    taskPrompt,
                    stateJson != null ? stateJson : "{}",
                    invJson != null ? invJson : "{}",
                    lookJson != null ? lookJson : "{}",
                    userPrompt,
                    autonomousActions
                );

                if (geminiReply != null && !geminiReply.isEmpty()) {
                    String formatted = dispatchGeminiAgentResponse(homeDir, geminiReply, autonomousActions, targetReplyView);
                    lastAiReplyText = formatted;
                    if (targetReplyView != null) {
                        runOnUiThread(() -> targetReplyView.setText(formatted));
                    }
                } else {
                    String fallbackMsg = "No response from Gemini API (" + formatModelDisplayName(modelId) + "). Check internet connection and API Key.";
                    lastAiReplyText = fallbackMsg;
                    if (targetReplyView != null) {
                        runOnUiThread(() -> targetReplyView.setText(fallbackMsg));
                    }
                }
            } finally {
                aiTaskRunning = false;
                if (targetTitleView != null) {
                    runOnUiThread(() -> targetTitleView.setText("AI AGENT  •  " + formatModelDisplayName(modelId)));
                }
            }
        }, "HorizonZ-GeminiRequestWorker").start();
    }

    private String readLocalGeminiKey(File homeDir) {
        try {
            File f = new File(homeDir, "horizon-gemini-secret.json");
            if (!f.exists()) return "";
            String txt = readSmallTextFile(f);
            JSONObject obj = new JSONObject(txt);
            return obj.optString("gemini_api_key", "").trim();
        } catch (Exception e) {
            return "";
        }
    }

    private String readSmallTextFile(File f) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString().trim();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Connects to the game's `$HORIZON_CONTROL` endpoint (`horizon-control.json`) using Horizon's
     * exact TCP JSON-file transport (`{"transport":"tcp","port":N,"token":"..."}`):
     * 1. Sends `auth <TOKEN>\n` as the first line.
     * 2. Sends the CLI command line (`state\n`, `inventory\n`, `looking-at\n`, `chat ...\n`, `command ...\n`, `goto ...\n`, etc.).
     * 3. Reads and returns the JSON response line from the game.
     */
    private String sendControlCommandToGame(File homeDir, String commandLine) {
        try {
            File ctrlFile = new File(homeDir, "horizon-control.json");
            if (!ctrlFile.exists()) return null;
            String ctrlRaw = readSmallTextFile(ctrlFile);
            if (ctrlRaw.isEmpty()) return null;
            JSONObject ctrlObj = new JSONObject(ctrlRaw);
            String host = "127.0.0.1";
            int port = ctrlObj.optInt("port", 0);
            if (port <= 0) {
                String addr = ctrlObj.optString("address", ctrlObj.optString("addr", ""));
                int colon = addr.lastIndexOf(':');
                if (colon > 0) {
                    host = addr.substring(0, colon).trim();
                    port = Integer.parseInt(addr.substring(colon + 1).trim());
                }
            }
            if (port <= 0) return null;
            String token = ctrlObj.optString("token", "").trim();

            try (Socket sock = new Socket()) {
                sock.connect(new InetSocketAddress(host, port), 1500);
                sock.setSoTimeout(3500);
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream(), StandardCharsets.UTF_8));
                BufferedReader reader = new BufferedReader(new InputStreamReader(sock.getInputStream(), StandardCharsets.UTF_8));

                if (!token.isEmpty()) {
                    writer.write("auth " + token + "\n");
                    writer.flush();
                }
                writer.write(commandLine.replace("\n", " ").trim() + "\n");
                writer.flush();

                String line1 = reader.readLine();
                if (line1 != null && line1.contains("\"authenticated\"")) {
                    String line2 = reader.readLine();
                    if (line2 != null) return line2;
                }
                return line1;
            }
        } catch (Exception e) {
            return null;
        }
    }

    private String pollLatestChatTriggerFromSocketOrLog(File homeDir, String lastHandled) {
        String msgsJson = sendControlCommandToGame(homeDir, "messages");
        if (msgsJson != null && !msgsJson.isEmpty()) {
            try {
                JSONObject root = new JSONObject(msgsJson);
                JSONArray arr = root.optJSONArray("messages");
                if (arr != null) {
                    for (int i = arr.length() - 1; i >= 0; i--) {
                        String itemStr = arr.optString(i, "").trim();
                        if (itemStr.contains("[AI]") || itemStr.contains("[HorizonZ Gemini AI]")) continue;
                        String lower = itemStr.toLowerCase();
                        if (lower.contains("@ai ") || lower.contains("!ai ")) {
                            if (!itemStr.equals(lastHandled)) return itemStr;
                            return null;
                        }
                    }
                }
            } catch (Exception ignored) {}
        }
        File logFile = new File(homeDir, "logs/latest.log");
        if (!logFile.exists()) {
            logFile = new File(homeDir, ".local/horizon-client/26.3/logs/latest.log");
        }
        if (logFile.exists()) {
            try {
                String tail = readSmallTextFile(logFile);
                String[] lines = tail.split("\n");
                for (int i = lines.length - 1; i >= Math.max(0, lines.length - 25); i--) {
                    String l = lines[i].trim();
                    if (l.contains("[AI]") || l.contains("[HorizonZ Gemini AI]")) continue;
                    String lower = l.toLowerCase();
                    if ((lower.contains("[chat]") || lower.contains("<")) && (lower.contains("@ai ") || lower.contains("!ai "))) {
                        if (!l.equals(lastHandled)) return l;
                        return null;
                    }
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    private String callGeminiAgentApi(
        String apiKey,
        String modelId,
        String taskPrompt,
        String stateJson,
        String invJson,
        String lookJson,
        String userTrigger,
        boolean allowActions
    ) {
        String cleanModel = modelId.startsWith("models/") ? modelId.substring(7) : modelId;
        String lower = cleanModel.toLowerCase();
        String fallback1 = (lower.contains("argon") || lower.contains("ultra") || lower.contains("pro"))
            ? "gemini-2.5-pro"
            : (lower.contains("lite") ? "gemini-2.5-flash-lite" : "gemini-2.5-flash");
        String[] candidates = new String[] { cleanModel, fallback1, "gemini-2.5-flash", "gemini-2.0-flash" };

        for (String candidate : candidates) {
            try {
                String urlStr = "https://generativelanguage.googleapis.com/v1beta/models/" + candidate + ":generateContent?key=" + apiKey;
                HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(14000);
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");

                String sysPrompt = "You are the autonomous in-game AI Agent (" + formatModelDisplayName(cleanModel) + " [" + cleanModel + "]) inside Minecraft 26.3 (HorizonZ). "
                    + "You have FULL control to perform ANY action in Minecraft 26.3 on the player's behalf.\n"
                    + "System role: " + taskPrompt + "\n"
                    + "Live player/world state JSON: " + stateJson + "\n"
                    + "Live inventory JSON: " + invJson + "\n"
                    + "Live looking-at target JSON: " + lookJson + "\n"
                    + "Player instruction: " + userTrigger + "\n\n"
                    + "AVAILABLE IN-GAME COMMANDS (you can chain multiple steps in the \"commands\" array):\n"
                    + "1. Any Minecraft 26.3 command via `command <mc_command_without_leading_slash>`, for example:\n"
                    + "   - `command gamemode creative` / `command gamemode survival` / `command gamemode spectator`\n"
                    + "   - `command give @s diamond_sword 1` / `command give @s oak_planks 64`\n"
                    + "   - `command time set day` / `command weather clear`\n"
                    + "   - `command tp @s <x> <y> <z>` / `command effect give @s regeneration 60 2` / `command enchant @s sharpness 5`\n"
                    + "   - `command fill <x1> <y1> <z1> <x2> <y2> <z2> <block>` or `command setblock <x> <y> <z> <block>` (use the player's live coordinates from state JSON to build houses, shelters, bridges, platforms, or clear areas!)\n"
                    + "   - `command summon <entity> <x> <y> <z>` / `command kill @e[type=zombie,distance=..25]` / `command locate structure village_plains` / `command gamerule keepInventory true`\n"
                    + "2. Direct player body, pathfinding, mining, and inventory controls via horizon-ctl:\n"
                    + "   - `find-and-mine <block_id>` (automatically locates the nearest block of that type, walks to it, and mines it)\n"
                    + "   - `find-block <block_id>`\n"
                    + "   - `goto <x> <y> <z> --near 2.0`\n"
                    + "   - `break <x> <y> <z>` / `place <x> <y> <z> up`\n"
                    + "   - `move forward 2` / `move back 1` / `move left 1` / `move right 1`\n"
                    + "   - `jump` / `sneak true` / `sneak false` / `sprint true` / `sprint false`\n"
                    + "   - `look <yaw> <pitch>` / `look-at <x> <y> <z>`\n"
                    + "   - `attack` / `interact` / `use-item` / `drop` / `select-slot <0..8>`\n"
                    + "   - `open-inventory` / `click-slot <slot>` / `close-screen`\n"
                    + "   - `wait 400` (wait milliseconds between steps)\n\n"
                    + "Respond ONLY with a single valid JSON object (no markdown fences): "
                    + "{\"reply\":\"<concise response in the player's language>\",\"commands\":[\"<step1>\",\"<step2>\",...]} "
                    + (allowActions ? "Populate \"commands\" with all necessary commands to completely fulfill the player's request." : "Keep \"commands\" empty [].");

                JSONObject payload = new JSONObject();
                JSONArray contents = new JSONArray();
                JSONObject contentItem = new JSONObject();
                JSONArray parts = new JSONArray();
                parts.put(new JSONObject().put("text", sysPrompt));
                contentItem.put("parts", parts);
                contents.put(contentItem);
                payload.put("contents", contents);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
                }
                if (conn.getResponseCode() >= 200 && conn.getResponseCode() < 300) {
                    StringBuilder sb = new StringBuilder();
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = br.readLine()) != null) sb.append(line);
                    }
                    JSONObject resp = new JSONObject(sb.toString());
                    return resp.optJSONArray("candidates")
                        .optJSONObject(0)
                        .optJSONObject("content")
                        .optJSONArray("parts")
                        .optJSONObject(0)
                        .optString("text", "")
                        .trim();
                } else if (conn.getResponseCode() == 400 || conn.getResponseCode() == 401 || conn.getResponseCode() == 403) {
                    break;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    /**
     * Executes a single normalized control command or composite macro (`find-and-mine <block>`, `wait <ms>`, `/cmd`).
     */
    private String executeSingleControlStep(File homeDir, String rawCmd) {
        if (rawCmd == null) return "";
        String cmd = rawCmd.trim();
        if (cmd.isEmpty()) return "";

        if (cmd.startsWith("/")) {
            cmd = "command " + cmd.substring(1).trim();
        } else if (cmd.startsWith("command /")) {
            cmd = "command " + cmd.substring(9).trim();
        }

        String lower = cmd.toLowerCase();
        if (lower.startsWith("wait ")) {
            try {
                long ms = Long.parseLong(cmd.substring(5).trim());
                Thread.sleep(Math.max(50L, Math.min(5000L, ms)));
            } catch (Exception ignored) {}
            return "waited";
        }

        if (lower.startsWith("find-and-mine ")) {
            String blockId = cmd.substring(14).trim().replace("minecraft:", "");
            String foundJson = sendControlCommandToGame(homeDir, "find-block " + blockId);
            if (foundJson != null && !foundJson.contains("\"error\"")) {
                try {
                    JSONObject fObj = new JSONObject(foundJson);
                    int bx = fObj.optInt("x", Integer.MIN_VALUE);
                    int by = fObj.optInt("y", Integer.MIN_VALUE);
                    int bz = fObj.optInt("z", Integer.MIN_VALUE);
                    if (bx != Integer.MIN_VALUE) {
                        sendControlCommandToGame(homeDir, "goto " + bx + " " + by + " " + bz + " --near 2.5");
                        sendControlCommandToGame(homeDir, "look-at " + bx + " " + by + " " + bz);
                        return sendControlCommandToGame(homeDir, "break " + bx + " " + by + " " + bz);
                    }
                } catch (Exception ignored) {}
            }
            return foundJson != null ? foundJson : "block not found";
        }

        return sendControlCommandToGame(homeDir, cmd);
    }

    private String dispatchGeminiAgentResponse(File homeDir, String rawText, boolean allowActions, final TextView liveLogView) {
        try {
            String cleaned = rawText.trim();
            if (cleaned.startsWith("```")) {
                int firstNl = cleaned.indexOf('\n');
                if (firstNl > 0) cleaned = cleaned.substring(firstNl + 1);
                if (cleaned.endsWith("```")) cleaned = cleaned.substring(0, cleaned.length() - 3).trim();
            }
            String replyText = cleaned;
            JSONArray commandsArr = null;
            int firstBrace = cleaned.indexOf('{');
            int lastBrace = cleaned.lastIndexOf('}');
            if (firstBrace >= 0 && lastBrace > firstBrace) {
                String jsonCandidate = cleaned.substring(firstBrace, lastBrace + 1);
                JSONObject obj = new JSONObject(jsonCandidate);
                replyText = obj.optString("reply", obj.optString("chat", cleaned));
                commandsArr = obj.optJSONArray("commands");
                if (commandsArr == null) {
                    commandsArr = obj.optJSONArray("actions");
                }
            }

            StringBuilder logBuilder = new StringBuilder();
            if (replyText != null && !replyText.isEmpty()) {
                logBuilder.append(replyText.trim());
                sendControlCommandToGame(homeDir, "chat [AI] " + replyText.replace("\n", " "));
            }

            if (allowActions && commandsArr != null && commandsArr.length() > 0) {
                for (int i = 0; i < commandsArr.length(); i++) {
                    if (aiTaskAbortRequested) {
                        logBuilder.append("\n[Stopped by user]");
                        break;
                    }
                    Object item = commandsArr.opt(i);
                    String cmdStr = "";
                    if (item instanceof String) {
                        cmdStr = ((String) item).trim();
                    } else if (item instanceof JSONObject) {
                        JSONObject act = (JSONObject) item;
                        String op = act.optString("op", act.optString("command", "")).trim();
                        if ("command".equals(op) && act.has("command")) {
                            cmdStr = "command " + act.optString("command", "");
                        } else {
                            cmdStr = op;
                        }
                    }
                    if (!cmdStr.isEmpty()) {
                        logBuilder.append("\n> ").append(cmdStr);
                        final String progressSnapshot = logBuilder.toString();
                        if (liveLogView != null) {
                            runOnUiThread(() -> liveLogView.setText(progressSnapshot));
                        }
                        executeSingleControlStep(homeDir, cmdStr);
                        try { Thread.sleep(120L); } catch (InterruptedException ignored) { break; }
                    }
                }
            }
            return logBuilder.length() > 0 ? logBuilder.toString() : cleaned;
        } catch (Exception e) {
            String fallback = rawText.replace("\n", " ").substring(0, Math.min(200, rawText.length()));
            sendControlCommandToGame(homeDir, "chat [AI] " + fallback);
            return fallback;
        }
    }

    private void dismissAiDialogsSafely() {
        try {
            if (aiCommandPanelDialog != null) {
                if (aiCommandPanelDialog.isShowing()) {
                    aiCommandPanelDialog.dismiss();
                }
                aiCommandPanelDialog = null;
            }
        } catch (Exception ignored) {
            aiCommandPanelDialog = null;
        }
        try {
            if (aiFloatingButtonDialog != null) {
                if (aiFloatingButtonDialog.isShowing()) {
                    aiFloatingButtonDialog.dismiss();
                }
                aiFloatingButtonDialog = null;
            }
        } catch (Exception ignored) {
            aiFloatingButtonDialog = null;
        }
    }

    private void returnToLauncherAndFinish() {
        watchdogRunning = false;
        try {
            File homeDir = getFilesDir();
            if (homeDir != null) {
                new File(homeDir, "zalith-game-running.lock").delete();
            }
        } catch (Exception ignored) {
        }
        dismissSplashDialogSafely();
        dismissAiDialogsSafely();
        try {
            Intent backIntent = new Intent(this, ZalithMainActivity.class);
            backIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(backIntent);
        } catch (Exception ignored) {
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                finishAndRemoveTask();
            } else {
                finish();
            }
        } catch (Exception ignored) {
            finish();
        }
        new Thread(() -> {
            try {
                Thread.sleep(180L);
            } catch (InterruptedException ignored) {
            }
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(0);
        }).start();
    }

    @Override
    protected void onDestroy() {
        watchdogRunning = false;
        try {
            File homeDir = getFilesDir();
            if (homeDir != null) {
                new File(homeDir, "zalith-game-running.lock").delete();
            }
        } catch (Exception ignored) {
        }
        dismissSplashDialogSafely();
        dismissAiDialogsSafely();
        super.onDestroy();
    }

    private void applyRequestedOrientationFromIntent(Intent intent) {
        if (intent == null) return;
        String orient = intent.getStringExtra("HORIZON_ORIENTATION");
        if (orient == null) return;
        switch (orient) {
            case "landscape":
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                break;
            case "reverse-landscape":
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                break;
            case "portrait":
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                break;
            case "reverse-portrait":
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                break;
            case "auto":
            default:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
                break;
        }
    }

    private void appendCrossProcessLog(File homeDir, String msg) {
        try {
            File logFile = new File(homeDir, "zalith-system.log");
            String line = msg + "\n";
            try (FileOutputStream fos = new FileOutputStream(logFile, true)) {
                fos.write(line.getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception ignored) {
        }
    }

    private void ensureEngineDataExtracted() {
        try {
            File homeDir = getFilesDir();
            if (homeDir == null) return;
            if (!homeDir.exists()) homeDir.mkdirs();
            final String expectedStamp = "2.5-0.7.7-150a414";
            File stampFile = new File(homeDir, "engine-data-version.txt");
            String currentStamp = stampFile.exists() ? readSmallTextFile(stampFile) : "";
            File engineZip = new File(homeDir, "engine-data.zip");
            if (!engineZip.exists() || engineZip.length() < 1024 || !expectedStamp.equals(currentStamp)) {
                try (InputStream in = getAssets().open("engine-data.zip");
                     FileOutputStream out = new FileOutputStream(engineZip)) {
                    byte[] buf = new byte[65536];
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                }
                // Invalidate stale derived block-behaviour cache files across engine updates (0.7.7 cache compatibility)
                File root26 = new File(homeDir, ".local/horizon-client/26.3");
                if (root26.isDirectory()) {
                    String[] staleCaches = new String[] {
                        "block-behaviour.bin",
                        "block-behaviour-cache.bin",
                        "derived-block-behaviour.bin"
                    };
                    for (String sc : staleCaches) {
                        File f = new File(root26, sc);
                        if (f.exists()) f.delete();
                    }
                }
                try (FileOutputStream fos = new FileOutputStream(stampFile, false)) {
                    fos.write(expectedStamp.getBytes(StandardCharsets.UTF_8));
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not pre-extract engine-data.zip: " + e.getMessage());
        }
    }

    private void applyZalithLaunchEnvironment(Intent intent) {
        try {
            File homeDir = getFilesDir();
            if (homeDir != null) {
                Os.setenv("HORIZON_HOME", homeDir.getAbsolutePath(), true);
                Os.setenv("HORIZON_DIRECT_GAME", "1", true);
                File appearanceFile = new File(homeDir, "horizon-appearance.json");
                if (appearanceFile.exists()) {
                    Os.setenv("HORIZON_APPEARANCE_CONFIG", appearanceFile.getAbsolutePath(), true);
                }
                File wardrobeFile = new File(new File(homeDir, "wardrobe"), "wardrobe.json");
                if (wardrobeFile.exists()) {
                    Os.setenv("HORIZON_WARDROBE_CONFIG", wardrobeFile.getAbsolutePath(), true);
                }
            }
            if (intent != null) {
                boolean publicMode = intent.getBooleanExtra("HORIZON_ANDROID_PUBLIC", true);
                Os.setenv("HORIZON_ANDROID_PUBLIC", publicMode ? "1" : "0", true);

                boolean disableAudio = intent.getBooleanExtra("HORIZON_NO_AUDIO", false);
                if (disableAudio) {
                    Os.setenv("HORIZON_NO_AUDIO", "1", true);
                    Os.setenv("ALSOFT_DISABLE_EVENTS", "1", true);
                }

                boolean disableDiscord = intent.getBooleanExtra("HORIZON_NO_DISCORD", false);
                if (disableDiscord) {
                    Os.setenv("HORIZON_NO_DISCORD", "1", true);
                }

                String playerName = intent.getStringExtra("HORIZON_PLAYER_NAME");
                if (playerName != null && !playerName.isEmpty()) {
                    Os.setenv("HORIZON_PLAYER_NAME", playerName, true);
                }

                String renderBackend = intent.getStringExtra("HORIZON_WGPU_BACKEND");
                if (renderBackend != null && !renderBackend.isEmpty()) {
                    Os.setenv("WGPU_BACKEND", renderBackend, true);
                }

                int ramAllocMb = intent.getIntExtra("HORIZON_RAM_ALLOCATION_MB", 0);
                int deviceTotalRamMb = intent.getIntExtra("HORIZON_DEVICE_TOTAL_RAM_MB", 8192);
                boolean unlimitedRam = (ramAllocMb <= 0);
                int effectiveRamMb = unlimitedRam ? Math.max(2048, (int) (deviceTotalRamMb * 0.85f)) : Math.max(1024, ramAllocMb);
                Os.setenv("HORIZON_RAM_ALLOCATION_MB", String.valueOf(ramAllocMb), true);
                Os.setenv("HORIZON_RAM_UNLIMITED", unlimitedRam ? "1" : "0", true);
                Os.setenv("HORIZON_EFFECTIVE_RAM_MB", String.valueOf(effectiveRamMb), true);
                Os.setenv("WGPU_MEMORY_BUDGET_MB", String.valueOf(effectiveRamMb), true);

                String shaderPack = intent.getStringExtra("HORIZON_SHADER_PACK");
                if (shaderPack != null && !shaderPack.trim().isEmpty()) {
                    Os.setenv("HORIZON_SHADER_PACK", shaderPack.trim(), true);
                } else {
                    try { Os.unsetenv("HORIZON_SHADER_PACK"); } catch (Exception ignored) {}
                }

                boolean aiAgentEnabled = intent.getBooleanExtra("HORIZON_AI_AGENT_ENABLED", false);
                boolean hasLocalKey = homeDir != null && !readLocalGeminiKey(homeDir).isEmpty();
                String aiProvider = intent.getStringExtra("HORIZON_AI_AGENT_PROVIDER");
                String geminiModel = intent.getStringExtra("HORIZON_GEMINI_MODEL");
                if ((aiAgentEnabled || hasLocalKey) && homeDir != null) {
                    File controlFile = new File(homeDir, "horizon-control.json");
                    Os.setenv("HORIZON_CONTROL", controlFile.getAbsolutePath(), true);
                    Os.setenv("HORIZON_AI_AGENT_ENABLED", "1", true);
                    Os.setenv("HORIZON_AI_AGENT_PROVIDER", (aiProvider != null && !aiProvider.isEmpty()) ? aiProvider : "gemini", true);
                    Os.setenv("HORIZON_GEMINI_MODEL", (geminiModel != null && !geminiModel.isEmpty()) ? geminiModel : "gemini-4-argon", true);
                } else {
                    Os.setenv("HORIZON_AI_AGENT_ENABLED", "0", true);
                }

                String touchScheme = intent.getStringExtra("HORIZON_TOUCH_SCHEME");
                if (touchScheme != null && !touchScheme.isEmpty()) {
                    Os.setenv("HORIZON_TOUCH_SCHEME", touchScheme, true);
                }
                int sensX = intent.getIntExtra("HORIZON_TOUCH_SENSITIVITY_X", 100);
                int sensY = intent.getIntExtra("HORIZON_TOUCH_SENSITIVITY_Y", 100);
                boolean joy360 = intent.getBooleanExtra("HORIZON_TOUCH_360_JOYSTICK", false);
                boolean swapHands = intent.getBooleanExtra("HORIZON_TOUCH_SWAP_HANDS", true);
                boolean virtMouse = intent.getBooleanExtra("HORIZON_INVENTORY_VIRTUAL_MOUSE", false);
                Os.setenv("HORIZON_TOUCH_SENSITIVITY_X", String.valueOf(sensX), true);
                Os.setenv("HORIZON_TOUCH_SENSITIVITY_Y", String.valueOf(sensY), true);
                Os.setenv("HORIZON_TOUCH_360_JOYSTICK", joy360 ? "1" : "0", true);
                Os.setenv("HORIZON_TOUCH_SWAP_HANDS", swapHands ? "1" : "0", true);
                Os.setenv("HORIZON_INVENTORY_VIRTUAL_MOUSE", virtMouse ? "1" : "0", true);

                String sessionFile = intent.getStringExtra("HORIZON_SESSION_FILE");
                if (sessionFile != null && !sessionFile.trim().isEmpty() && new File(sessionFile.trim()).isFile()) {
                    Os.setenv("HORIZON_SESSION_FILE", sessionFile.trim(), true);
                } else {
                    try { Os.unsetenv("HORIZON_SESSION_FILE"); } catch (Exception ignored) {}
                }

                try {
                    Class<?> vmRuntimeClass = Class.forName("dalvik.system.VMRuntime");
                    Object vmRuntime = vmRuntimeClass.getDeclaredMethod("getRuntime").invoke(null);
                    vmRuntimeClass.getDeclaredMethod("setTargetHeapUtilization", float.class)
                        .invoke(vmRuntime, unlimitedRam ? 0.85f : 0.75f);
                } catch (Throwable ignored) {
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to set environment variables for Horizon native engine", e);
        }
    }

    private void enableImmersiveFullscreen() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setAttributes(lp);
        }
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
        );
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == SAF_DOCUMENT_REQUEST_CODE) {
            String uriString = null;
            if (data != null && data.getData() != null) {
                Uri uri = data.getData();
                try {
                    final int takeFlags = data.getFlags()
                        & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    getContentResolver().takePersistableUriPermission(uri, takeFlags);
                } catch (SecurityException ignored) {
                }
                uriString = uri.toString();
            }
            try {
                nativeDocumentResult(request, result, uriString);
            } catch (UnsatisfiedLinkError err) {
                Log.e(TAG, "nativeDocumentResult JNI symbol not linked: " + err.getMessage());
            }
        }
    }

    /**
     * Pure Black Minecraft Java Edition Style Loading Splash Overlay:
     * - Compact, standard-proportioned 8x8 blocky Minecraft logo "HORIZONZ" where the first 'O'
     *   is an iconic 8x8 Minecraft Creeper Face block!
     * - Stays visible on its own Dialog Window surface above NativeActivity until the Rust engine
     *   signals `100|Main Menu Ready` AND the swapchain has presented the Main Menu frame!
     */
    private static final class HorizonSplashOverlayView extends View {
        private final File bootFile;
        private final Runnable onDismissCallback;
        private final Handler handler = new Handler(Looper.getMainLooper());
        private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint pixelPaint = new Paint();
        private final Paint barBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint barFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint subTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF tmpRect = new RectF();

        private float displayedProgress = 0.22f;
        private float targetProgress = 0.28f;
        private String stageLabel = "Initializing HorizonZ Rust Engine...";
        private boolean engineReady = false;
        private long createdTimeMs = System.currentTimeMillis();
        private long engineReadyFirstSeenMs = 0L;
        private float overlayAlpha = 1.0f;
        private boolean active = true;

        // Compact 8x8 Chunky Minecraft Title Block Glyphs (1 = foreground block, 2 = Creeper face feature)
        private static final int[][] GLYPH_H = {
            {1,1,0,0,0,0,1,1},
            {1,1,0,0,0,0,1,1},
            {1,1,0,0,0,0,1,1},
            {1,1,1,1,1,1,1,1},
            {1,1,1,1,1,1,1,1},
            {1,1,0,0,0,0,1,1},
            {1,1,0,0,0,0,1,1},
            {1,1,0,0,0,0,1,1}
        };
        // Iconic 8x8 Minecraft Creeper Face integrated as the letter 'O'!
        // 1 = Creeper Green Block, 2 = Creeper Black Eyes/Mouth, 3 = Lighter Creeper Pixel Highlight
        private static final int[][] GLYPH_CREEPER_O = {
            {1,1,3,1,1,3,1,1},
            {1,3,1,1,3,1,1,1},
            {1,2,2,1,1,2,2,1},
            {1,2,2,1,1,2,2,1},
            {1,1,1,2,2,1,1,1},
            {1,1,2,2,2,2,1,1},
            {1,1,2,2,2,2,1,1},
            {1,1,2,1,1,2,1,1}
        };
        private static final int[][] GLYPH_R = {
            {1,1,1,1,1,1,1,0},
            {1,1,0,0,0,0,1,1},
            {1,1,0,0,0,0,1,1},
            {1,1,1,1,1,1,1,0},
            {1,1,1,1,1,0,0,0},
            {1,1,0,0,1,1,0,0},
            {1,1,0,0,0,1,1,0},
            {1,1,0,0,0,0,1,1}
        };
        private static final int[][] GLYPH_I = {
            {1,1,1,1,1,1,1,1},
            {0,0,0,1,1,0,0,0},
            {0,0,0,1,1,0,0,0},
            {0,0,0,1,1,0,0,0},
            {0,0,0,1,1,0,0,0},
            {0,0,0,1,1,0,0,0},
            {0,0,0,1,1,0,0,0},
            {1,1,1,1,1,1,1,1}
        };
        private static final int[][] GLYPH_Z = {
            {1,1,1,1,1,1,1,1},
            {0,0,0,0,0,1,1,1},
            {0,0,0,0,1,1,1,0},
            {0,0,0,1,1,1,0,0},
            {0,0,1,1,1,0,0,0},
            {0,1,1,1,0,0,0,0},
            {1,1,1,0,0,0,0,0},
            {1,1,1,1,1,1,1,1}
        };
        private static final int[][] GLYPH_O = {
            {0,1,1,1,1,1,1,0},
            {1,1,1,0,0,1,1,1},
            {1,1,0,0,0,0,1,1},
            {1,1,0,0,0,0,1,1},
            {1,1,0,0,0,0,1,1},
            {1,1,0,0,0,0,1,1},
            {1,1,1,0,0,1,1,1},
            {0,1,1,1,1,1,1,0}
        };
        private static final int[][] GLYPH_N = {
            {1,1,0,0,0,0,1,1},
            {1,1,1,0,0,0,1,1},
            {1,1,1,1,0,0,1,1},
            {1,1,0,1,1,0,1,1},
            {1,1,0,0,1,1,1,1},
            {1,1,0,0,0,1,1,1},
            {1,1,0,0,0,0,1,1},
            {1,1,0,0,0,0,1,1}
        };

        private static final int[][][] WORD_HORIZONZ = {
            GLYPH_H, GLYPH_CREEPER_O, GLYPH_R, GLYPH_I, GLYPH_Z, GLYPH_O, GLYPH_N, GLYPH_Z
        };

        private final Runnable frameLoop = new Runnable() {
            @Override
            public void run() {
                if (!active) return;
                pollBootMilestone();
                updateAnimationState();
                invalidate();
                if (active) {
                    handler.postDelayed(this, 16L);
                }
            }
        };

        HorizonSplashOverlayView(Context context, File homeDir, Runnable onDismissCallback) {
            super(context);
            this.bootFile = new File(homeDir, "zalith-game-boot.txt");
            this.onDismissCallback = onDismissCallback;

            bgPaint.setColor(Color.BLACK);
            bgPaint.setStyle(Paint.Style.FILL);
            pixelPaint.setStyle(Paint.Style.FILL);

            barBorderPaint.setColor(Color.WHITE);
            barBorderPaint.setStyle(Paint.Style.STROKE);

            barFillPaint.setColor(Color.WHITE);
            barFillPaint.setStyle(Paint.Style.FILL);

            textPaint.setColor(Color.parseColor("#D5DEE5"));
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setFakeBoldText(true);

            subTextPaint.setColor(Color.parseColor("#9EA8B0"));
            subTextPaint.setTextAlign(Paint.Align.CENTER);
            subTextPaint.setFakeBoldText(true);

            handler.post(frameLoop);
        }

        void stopPolling() {
            active = false;
            handler.removeCallbacksAndMessages(null);
        }

        private void pollBootMilestone() {
            if (!bootFile.exists()) return;
            try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(bootFile), StandardCharsets.UTF_8)
            )) {
                String line = br.readLine();
                if (line != null && !line.isEmpty()) {
                    int sep = line.indexOf('|');
                    if (sep > 0) {
                        int pct = Integer.parseInt(line.substring(0, sep).trim());
                        String msg = line.substring(sep + 1).trim();
                        if (!msg.isEmpty()) {
                            stageLabel = msg;
                        }
                        if (pct >= 100) {
                            if (!engineReady) {
                                engineReady = true;
                                engineReadyFirstSeenMs = System.currentTimeMillis();
                            }
                        } else {
                            targetProgress = Math.max(targetProgress, Math.min(0.92f, pct / 100.0f));
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        private void updateAnimationState() {
            long now = System.currentTimeMillis();
            long elapsed = now - createdTimeMs;

            if (!engineReady) {
                if (elapsed > 400L && targetProgress < 0.46f) {
                    targetProgress = 0.46f;
                    stageLabel = "Creating Vulkan / WGPU Surface...";
                } else if (elapsed > 1200L && targetProgress < 0.70f) {
                    targetProgress = 0.70f;
                    stageLabel = "Loading Minecraft 26.3 Registries & Atlases...";
                } else if (elapsed > 2200L && targetProgress < 0.88f) {
                    targetProgress = 0.88f;
                    stageLabel = "Applying Custom Player Skin & Audio Engine...";
                } else if (elapsed > 3100L && targetProgress < 0.96f) {
                    targetProgress = Math.min(0.96f, targetProgress + 0.004f);
                    stageLabel = "Preparing Panorama & Main Menu...";
                }
                if (elapsed >= 4000L) {
                    engineReady = true;
                    engineReadyFirstSeenMs = now;
                }
                displayedProgress += (targetProgress - displayedProgress) * 0.09f;
            } else {
                // Once `100|Main Menu Ready` is written by Rust, wait ~1.25s while smoothly advancing
                // to 100% so the Vulkan swapchain has already presented the TitleScreen underneath!
                long sinceEngineReady = now - engineReadyFirstSeenMs;
                targetProgress = 1.0f;
                displayedProgress += (1.0f - displayedProgress) * 0.10f;
                stageLabel = "Minecraft 26.3 Ready";

                if (sinceEngineReady >= 650L && displayedProgress >= 0.97f && elapsed >= 2200L) {
                    displayedProgress = 1.0f;
                    float fadeProgress = (sinceEngineReady - 650L) / 320.0f;
                    overlayAlpha = Math.max(0.0f, 1.0f - fadeProgress);
                    if (overlayAlpha <= 0.01f) {
                        active = false;
                        if (onDismissCallback != null) {
                            onDismissCallback.run();
                        }
                    }
                }
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) return;

            int alphaInt = Math.max(0, Math.min(255, (int) (overlayAlpha * 255.0f)));
            bgPaint.setAlpha(alphaInt);
            canvas.drawRect(0, 0, w, h, bgPaint);

            float density = getResources().getDisplayMetrics().density;
            int letterCols = 8;
            int letterRows = 8;
            int gapCols = 1;
            int totalCols = WORD_HORIZONZ.length * letterCols + (WORD_HORIZONZ.length - 1) * gapCols;

            // Compact, standard Minecraft title proportions (never stretched vertically!)
            float pxSize = Math.max(4f, Math.min(w * 0.50f / totalCols, 6.2f * density));
            float logoWidth = totalCols * pxSize;
            float logoHeight = letterRows * pxSize;
            float startX = (w - logoWidth) / 2f;
            float startY = (h * 0.41f) - (logoHeight / 2f);
            float depthOff = Math.max(2f, pxSize * 0.48f);

            // Pass 1: 3D Blocky Bottom/Side Extrusion Shadow for all letters
            for (int l = 0; l < WORD_HORIZONZ.length; l++) {
                int[][] glyph = WORD_HORIZONZ[l];
                float lx = startX + l * (letterCols + gapCols) * pxSize;
                int shadowColor = (l == 1) ? Color.parseColor("#1B4D14")
                    : (l == WORD_HORIZONZ.length - 1) ? Color.parseColor("#1D4E63")
                    : Color.parseColor("#2A3238");
                pixelPaint.setColor(shadowColor);
                pixelPaint.setAlpha(alphaInt);

                for (int r = 0; r < letterRows; r++) {
                    for (int c = 0; c < letterCols; c++) {
                        if (glyph[r][c] != 0) {
                            float px = lx + c * pxSize;
                            float py = startY + r * pxSize;
                            canvas.drawRect(
                                px + depthOff * 0.6f,
                                py + depthOff,
                                px + pxSize + depthOff * 0.6f + 0.5f,
                                py + pxSize + depthOff + 0.5f,
                                pixelPaint
                            );
                        }
                    }
                }
            }

            // Pass 2: Crisp Foreground Pixel Blocks (with Creeper Face on O and Top Bevel Highlight)
            for (int l = 0; l < WORD_HORIZONZ.length; l++) {
                int[][] glyph = WORD_HORIZONZ[l];
                float lx = startX + l * (letterCols + gapCols) * pxSize;

                for (int r = 0; r < letterRows; r++) {
                    for (int c = 0; c < letterCols; c++) {
                        int cell = glyph[r][c];
                        if (cell == 0) continue;
                        int blockColor;
                        if (l == 1) {
                            // Creeper Face 'O' letter!
                            if (cell == 2) {
                                blockColor = Color.parseColor("#111712"); // Creeper black eyes & mouth
                            } else if (cell == 3) {
                                blockColor = Color.parseColor("#72E052"); // Bright Creeper green pixel
                            } else {
                                blockColor = (r == 0) ? Color.parseColor("#6CE04E") : Color.parseColor("#49AC30");
                            }
                        } else if (l == WORD_HORIZONZ.length - 1) {
                            // Final 'Z' in Sky-Blue accent with bright top edge
                            blockColor = (r == 0) ? Color.parseColor("#C2F0FF") : Color.parseColor("#8BE0FF");
                        } else {
                            // Standard Minecraft Title White/Stone with bright top edge & subtle lower shade
                            blockColor = (r == 0) ? Color.WHITE
                                : (r >= 6) ? Color.parseColor("#D0D8DE")
                                : Color.parseColor("#F2F6F8");
                        }
                        pixelPaint.setColor(blockColor);
                        pixelPaint.setAlpha(alphaInt);
                        float px = lx + c * pxSize;
                        float py = startY + r * pxSize;
                        canvas.drawRect(px, py, px + pxSize + 0.5f, py + pxSize + 0.5f, pixelPaint);
                    }
                }
            }

            // Subtitle under HORIZONZ logo
            subTextPaint.setTextSize(10.5f * density);
            subTextPaint.setAlpha((int) (alphaInt * 0.85f));
            canvas.drawText(
                "MINECRAFT 26.3  •  RUST EDITION",
                w / 2f,
                startY + logoHeight + depthOff + (18f * density),
                subTextPaint
            );

            // Classic Minecraft Java Edition Progress Bar
            float barWidth = Math.min(w * 0.54f, 400f * density);
            float barHeight = 14f * density;
            float barLeft = (w - barWidth) / 2f;
            float barTop = h * 0.72f;
            float borderStroke = Math.max(2f, 1.8f * density);
            float innerPad = Math.max(3f, 2.8f * density);

            barBorderPaint.setStrokeWidth(borderStroke);
            barBorderPaint.setAlpha(alphaInt);
            tmpRect.set(barLeft, barTop, barLeft + barWidth, barTop + barHeight);
            canvas.drawRect(tmpRect, barBorderPaint);

            float maxFillWidth = (barWidth - innerPad * 2f);
            float fillWidth = Math.max(0f, Math.min(maxFillWidth, maxFillWidth * displayedProgress));
            if (fillWidth > 0f) {
                barFillPaint.setAlpha(alphaInt);
                tmpRect.set(
                    barLeft + innerPad,
                    barTop + innerPad,
                    barLeft + innerPad + fillWidth,
                    barTop + barHeight - innerPad
                );
                canvas.drawRect(tmpRect, barFillPaint);
            }

            int pctInt = Math.max(1, Math.min(100, Math.round(displayedProgress * 100f)));
            textPaint.setTextSize(11f * density);
            textPaint.setAlpha((int) (alphaInt * 0.85f));
            canvas.drawText(
                stageLabel + " (" + pctInt + "%)",
                w / 2f,
                barTop + barHeight + (18f * density),
                textPaint
            );
        }
    }
}
