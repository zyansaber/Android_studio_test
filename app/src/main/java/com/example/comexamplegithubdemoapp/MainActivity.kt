package com.example.githubdemoapp

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var titleText: TextView
    private lateinit var contentText: TextView
    private lateinit var hintText: TextView
    private lateinit var actionButton: Button

    private var clickCount = 0

    private val ideas = listOf(
        "🚀 试试 25 分钟专注 + 5 分钟休息，效率会提升很多。",
        "🎯 今天只做一件最重要的事：先把它完成。",
        "📚 学一个新知识点，并用一句话讲给别人听。",
        "💡 把你现在的想法写下来，它可能就是下个项目起点。",
        "🧪 给自己的 App 新增一个小功能，哪怕只是一行代码。",
        "🌱 昨天的你比，今天只要进步 1% 就很棒。"
    )

    private val backgroundColors = listOf(
        "#FFF3E0", // orange 50
        "#E3F2FD", // blue 50
        "#E8F5E9", // green 50
        "#F3E5F5", // purple 50
        "#FCE4EC", // pink 50
        "#E0F7FA"  // cyan 50
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        titleText = findViewById(R.id.titleText)
        contentText = findViewById(R.id.contentText)
        hintText = findViewById(R.id.hintText)
        actionButton = findViewById(R.id.actionButton)

        actionButton.setOnClickListener {
            clickCount++
            val randomIndex = ideas.indices.random()

            contentText.text = ideas[randomIndex]
            hintText.text = "你已经获得 $clickCount 条灵感 ✨"
            titleText.text = "今日灵感机"

            findViewById<android.view.View>(R.id.rootLayout)
                .setBackgroundColor(Color.parseColor(backgroundColors[randomIndex]))
        }
    }
}
