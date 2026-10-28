package com.example.skipad;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 只有一个界面：告诉你服务有没有开、并帮你跳到设置页。
 * 界面全部用代码写，省掉布局文件，方便照着改。
 */
public class MainActivity extends Activity {

    private TextView statusView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildUi());
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
    }

    private View buildUi() {
        int pad = dp(20);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);
        root.setBackgroundColor(Color.WHITE);

        TextView title = new TextView(this);
        title.setText("跳过开屏广告");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        title.setTextColor(Color.BLACK);
        root.addView(title, matchWrap(0));

        TextView intro = new TextView(this);
        intro.setText("原理：检测屏幕上的“跳过 / 跳过广告 / Skip”按钮，替你点一下。\n"
                + "不 root、不刷机、不修改系统文件，只用安卓自带的无障碍功能。\n"
                + "本应用没有联网权限，不会上传任何数据。");
        intro.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        intro.setTextColor(Color.DKGRAY);
        intro.setLineSpacing(dp(5), 1f);
        root.addView(intro, matchWrap(dp(14)));

        statusView = new TextView(this);
        statusView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        statusView.setPadding(dp(14), dp(14), dp(14), dp(14));
        root.addView(statusView, matchWrap(dp(18)));

        Button openAccessibility = new Button(this);
        openAccessibility.setText("第 1 步：去开启无障碍服务");
        openAccessibility.setAllCaps(false);
        openAccessibility.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openAccessibilitySettings();
            }
        });
        root.addView(openAccessibility, matchWrap(dp(14)));

        Button openAppInfo = new Button(this);
        openAppInfo.setText("第 2 步：应用信息（安卓 13+ 先允许受限设置）");
        openAppInfo.setAllCaps(false);
        openAppInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openAppInfoSettings();
            }
        });
        root.addView(openAppInfo, matchWrap(dp(10)));

        TextView tips = new TextView(this);
        tips.setText("开启后不用一直开着这个界面，退回桌面即可。\n\n"
                + "· 安卓 13 及以上：如果“无障碍”里的开关是灰的，先在第 2 步的页面点右上角“⋮”，选择“允许受限设置”。\n"
                + "· 想关掉：回到 系统设置 → 无障碍，把本服务关掉即可；不想用了直接卸载，不会留任何东西。\n"
                + "· 部分 App 的广告没有“跳过”文字（或画在特殊控件里），这类点不了，属于正常现象。");
        tips.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tips.setTextColor(Color.GRAY);
        tips.setLineSpacing(dp(3), 1f);
        root.addView(tips, matchWrap(dp(22)));

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.WHITE);
        scroll.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));
        return scroll;
    }

    private void refreshStatus() {
        boolean enabled = isServiceEnabled();
        if (enabled) {
            statusView.setText("✅ 服务已开启，去打开任意 App 试试");
            statusView.setTextColor(0xFF1B5E20);
            statusView.setBackgroundColor(0xFFE8F5E9);
        } else {
            statusView.setText("⚠️ 服务未开启，请完成第 1 步");
            statusView.setTextColor(0xFFB71C1C);
            statusView.setBackgroundColor(0xFFFFEBEE);
        }
    }

    private boolean isServiceEnabled() {
        String enabled = Settings.Secure.getString(
                getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (TextUtils.isEmpty(enabled)) {
            return false;
        }
        ComponentName me = new ComponentName(this, SkipService.class);
        TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');
        splitter.setString(enabled);
        while (splitter.hasNext()) {
            ComponentName cn = ComponentName.unflattenFromString(splitter.next());
            if (me.equals(cn)) {
                return true;
            }
        }
        return false;
    }

    private void openAccessibilitySettings() {
        try {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            Toast.makeText(this, "在列表里找到“跳过开屏广告”并打开", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "打不开，请手动进入 系统设置 → 无障碍", Toast.LENGTH_LONG).show();
        }
    }

    private void openAppInfoSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "打不开，请手动进入 设置 → 应用 → 跳过开屏广告", Toast.LENGTH_LONG).show();
        }
    }

    private int dp(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }

    private LinearLayout.LayoutParams matchWrap(int topMargin) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = topMargin;
        return lp;
    }
}
