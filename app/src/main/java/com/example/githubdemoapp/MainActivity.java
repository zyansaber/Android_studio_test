package com.example.githubdemoapp;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private View rootLayout;
    private TextView titleText;
    private TextView contentText;
    private TextView hintText;
    private Button inspireButton;
    private Button githubButton;

    private int clickCount = 0;

    private static final String GITHUB_RAW_URL =
            "https://raw.githubusercontent.com/zyansaber/Android_studio_test/main/message.txt";

    private final List<String> ideas = Arrays.asList(
            "🚀 试试 25 分钟专注 + 5 分钟休息，效率会提升很多。",
            "🎯 今天只做一件最重要的事：先把它完成。",
            "📚 学一个新知识点，并用一句话讲给别人听。",
            "💡 把你现在的想法写下来，它可能就是下个项目起点。",
            "🧪 给自己的 App 新增一个小功能，哪怕只是一行代码。",
            "🌱 昨天的你比，今天只要进步 1% 就很棒。"
    );

    private final List<String> backgroundColors = Arrays.asList(
            "#FFF3E0", "#E3F2FD", "#E8F5E9", "#F3E5F5", "#FCE4EC", "#E0F7FA"
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rootLayout = findViewById(R.id.rootLayout);
        titleText = findViewById(R.id.titleText);
        contentText = findViewById(R.id.contentText);
        hintText = findViewById(R.id.hintText);
        inspireButton = findViewById(R.id.inspireButton);
        githubButton = findViewById(R.id.githubButton);

        inspireButton.setOnClickListener(v -> showRandomIdea());
        githubButton.setOnClickListener(v -> loadGithubMessage());
    }

    private void showRandomIdea() {
        clickCount++;
        int randomIndex = new Random().nextInt(ideas.size());
        contentText.setText(ideas.get(randomIndex));
        hintText.setText("你已经获得 " + clickCount + " 条灵感 ✨");
        titleText.setText("今日灵感机");
        rootLayout.setBackgroundColor(Color.parseColor(backgroundColors.get(randomIndex)));
    }

    private void loadGithubMessage() {
        titleText.setText("正在读取 GitHub...");
        contentText.setText("请稍候，正在从仓库读取 message.txt");

        new Thread(() -> {
            try {
                URL url = new URL(GITHUB_RAW_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(8000);
                connection.setReadTimeout(8000);

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream())
                );
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line).append("\n");
                }
                reader.close();
                connection.disconnect();

                runOnUiThread(() -> {
                    titleText.setText("GitHub 读取成功 ✅");
                    contentText.setText(result.toString().trim());
                    hintText.setText("这段内容来自 GitHub Raw 文件");
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    titleText.setText("GitHub 读取失败");
                    contentText.setText("错误信息：" + e.getMessage());
                    hintText.setText("请检查网络、链接和代理设置");
                });
            }
        }).start();
    }
}
