package com.example.githubdemoapp

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.net.URL

class MainActivity : AppCompatActivity() {

    private lateinit var rootLayout: View
    private lateinit var titleText: TextView
    private lateinit var contentText: TextView
    private lateinit var hintText: TextView
    private lateinit var inspireButton: Button
    private lateinit var githubButton: Button

    private var clickCount = 0

    private val githubRawUrl = "https://raw.githubusercontent.com/zyansaber/Android_studio_test/main/message.txt"

    private val ideas = listOf(
        "🚀 试试 25 分钟专注 + 5 分钟休息，效率会提升很多。",
        "🎯 今天只做一件最重要的事：先把它完成。",
        "📚 学一个新知识点，并用一句话讲给别人听。",
        "💡 把你现在的想法写下来，它可能就是下个项目起点。",
        "🧪 给自己的 App 新增一个小功能，哪怕只是一行代码。",
        "🌱 昨天的你比，今天只要进步 1% 就很棒。"
    )

    private val backgroundColors = listOf(
        "#FFF3E0", "#E3F2FD", "#E8F5E9", "#F3E5F5", "#FCE4EC", "#E0F7FA"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rootLayout = findViewById(R.id.rootLayout)
        titleText = findViewById(R.id.titleText)
        contentText = findViewById(R.id.contentText)
        hintText = findViewById(R.id.hintText)
        inspireButton = findViewById(R.id.inspireButton)
        githubButton = findViewById(R.id.githubButton)

        inspireButton.setOnClickListener { showRandomIdea() }
        githubButton.setOnClickListener { loadGithubMessage() }
    }

    private fun showRandomIdea() {
        clickCount++
        val randomIndex = ideas.indices.random()
        contentText.text = ideas[randomIndex]
        hintText.text = "你已经获得 $clickCount 条灵感 ✨"
        titleText.text = "今日灵感机"
        rootLayout.setBackgroundColor(Color.parseColor(backgroundColors[randomIndex]))
    }

    private fun loadGithubMessage() {
        titleText.text = "正在读取 GitHub..."
        contentText.text = "请稍候，正在从仓库读取 message.txt"

        Thread {
            try {
                val result = URL(githubRawUrl).readText()
                runOnUiThread {
                    titleText.text = "GitHub 读取成功 ✅"
                    contentText.text = result
                    hintText.text = "这段内容来自 GitHub Raw 文件"
                }
            } catch (e: Exception) {
                runOnUiThread {
                    titleText.text = "GitHub 读取失败"
                    contentText.text = "错误信息：${e.message}"
                    hintText.text = "请检查网络、链接和代理设置"
                }
            }
        }.start()
    }
}
