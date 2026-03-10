package com.example.githubdemoapp

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var contentText: TextView
    private lateinit var loadButton: Button

    private val client = OkHttpClient()

    // 把这里换成你自己的 GitHub Raw 文件地址
    private val githubRawUrl = "https://raw.githubusercontent.com/zyansaber/Android_studio_test/main/message.txt"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        contentText = findViewById(R.id.contentText)
        loadButton = findViewById(R.id.loadButton)

        loadButton.setOnClickListener {
            contentText.text = "正在读取 GitHub 内容..."
            loadGithubContent()
        }
    }

    private fun loadGithubContent() {
        val request = Request.Builder()
            .url(githubRawUrl)
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                runOnUiThread {
                    contentText.text = "读取失败：${e.message}"
                }
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val result = response.body?.string() ?: "没有读取到内容"
                runOnUiThread {
                    contentText.text = result
                }
            }
        })
    }
}