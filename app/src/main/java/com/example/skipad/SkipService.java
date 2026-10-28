package com.example.skipad;

import android.accessibilityservice.AccessibilityService;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayDeque;
import java.util.Locale;

/**
 * 核心逻辑只有一句话：
 * 每当屏幕内容变化时，在所有可见控件里找“跳过 / 跳过广告 / Skip”这类文字，
 * 找到就替用户点一下。不 root、不 hook、不修改任何系统文件。
 */
public class SkipService extends AccessibilityService {

    /**
     * 关键词列表，匹配规则是“包含”且文字长度不超过 16 个字。
     * 想适配更多 App，直接往这里加词即可（比如某些 App 写的是“跳过 3”）。
     */
    private static final String[] KEYWORDS = {
            "跳过", "跳過",
            "跳过广告", "跳過廣告",
            "关闭广告", "關閉廣告",
            "skip", "skip ad", "skip ads"
    };

    private static final int MAX_NODES = 500;        // 每次最多检查 500 个控件，防止卡顿
    private static final long CLICK_GAP_MS = 600L;   // 同一个按钮 0.6 秒内不重复点

    private final Handler handler = new Handler(Looper.getMainLooper());
    private long lastClickAt = 0L;
    private String lastClickLabel = "";

    private final Runnable scanTask = new Runnable() {
        @Override
        public void run() {
            scanNow();
        }
    };

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) {
            return;
        }
        int type = event.getEventType();
        if (type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && type != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                && type != AccessibilityEvent.TYPE_WINDOWS_CHANGED) {
            return;
        }
        CharSequence pkg = event.getPackageName();
        if (pkg != null && getPackageName().contentEquals(pkg)) {
            return; // 忽略自己
        }

        // 开屏广告的“跳过”按钮常常比窗口晚 0.2~1 秒才画出来，多点几次更稳
        handler.removeCallbacks(scanTask);
        handler.post(scanTask);
        handler.postDelayed(scanTask, 400L);
        handler.postDelayed(scanTask, 900L);
    }

    private void scanNow() {
        AccessibilityNodeInfo root;
        try {
            root = getRootInActiveWindow();
        } catch (Exception e) {
            return;
        }
        if (root == null) {
            return;
        }
        AccessibilityNodeInfo target = findSkipNode(root);
        if (target != null) {
            clickTarget(target);
        }
    }

    /** 广度优先遍历当前窗口，找到第一个看起来像“跳过”的控件。 */
    private AccessibilityNodeInfo findSkipNode(AccessibilityNodeInfo root) {
        ArrayDeque<AccessibilityNodeInfo> queue = new ArrayDeque<>();
        queue.addLast(root);
        int visited = 0;

        while (!queue.isEmpty() && visited < MAX_NODES) {
            AccessibilityNodeInfo node = queue.pollFirst();
            visited++;
            if (node == null) {
                continue;
            }
            if (isSkipNode(node)) {
                return node;
            }
            int childCount = node.getChildCount();
            for (int i = 0; i < childCount; i++) {
                AccessibilityNodeInfo child = node.getChild(i);
                if (child != null) {
                    queue.addLast(child);
                }
            }
        }
        return null;
    }

    private boolean isSkipNode(AccessibilityNodeInfo node) {
        if (!node.isVisibleToUser()) {
            return false;
        }
        String label = readLabel(node);
        if (label.isEmpty()) {
            return false;
        }
        String normalized = label.toLowerCase(Locale.ROOT).replace(" ", "");
        if (normalized.length() > 16) {
            return false; // 太长的句子通常是正文，不是按钮
        }
        for (String keyword : KEYWORDS) {
            String k = keyword.toLowerCase(Locale.ROOT).replace(" ", "");
            if (normalized.contains(k)) {
                return true;
            }
        }
        return false;
    }

    private String readLabel(AccessibilityNodeInfo node) {
        StringBuilder sb = new StringBuilder();
        CharSequence text = node.getText();
        if (!TextUtils.isEmpty(text)) {
            sb.append(text);
        }
        CharSequence desc = node.getContentDescription();
        if (!TextUtils.isEmpty(desc)) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(desc);
        }
        return sb.toString().trim();
    }

    /** 文字本身往往不可点击，要往上找 1~3 层父控件再点。 */
    private void clickTarget(AccessibilityNodeInfo node) {
        long now = SystemClock.uptimeMillis();
        String label = readLabel(node);

        if (now - lastClickAt < CLICK_GAP_MS && label.equals(lastClickLabel)) {
            return;
        }

        AccessibilityNodeInfo current = node;
        for (int depth = 0; current != null && depth < 4; depth++) {
            if (current.isEnabled()
                    && current.isClickable()
                    && current.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                lastClickAt = now;
                lastClickLabel = label;
                return;
            }
            current = current.getParent();
        }

        // 兜底：有些控件只有文字节点能响应点击
        if (node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
            lastClickAt = now;
            lastClickLabel = label;
        }
    }

    @Override
    public void onInterrupt() {
        // 系统要求实现，这里不需要做任何事
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(scanTask);
        super.onDestroy();
    }
}
