package com.vunv.n5nihongo.ui.home

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.vunv.n5nihongo.data.WordRepository
import com.vunv.n5nihongo.data.local.AppDatabase
import com.vunv.n5nihongo.databinding.ActivityMainBinding
import com.vunv.n5nihongo.ui.adapter.LessonAdapter
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repository: WordRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 1. Khởi tạo ViewBinding (Giúp gọi ID từ XML dễ dàng, không bị null)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 2. Khởi tạo Database và Repository
        val database = AppDatabase.getDatabase(this)
        repository = WordRepository(database.wordDao())

        // 3. Nạp dữ liệu từ JSON vào Database (Chỉ chạy lần đầu)
        lifecycleScope.launch {
            repository.populateDatabaseFromJSON(this@MainActivity)
            setupRecyclerView()
        }
    }

    private fun setupRecyclerView() {
        // Tạo danh sách 50 bài học (tương ứng Minna no Nihongo)
        val lessonList = (1..50).toList()

        val adapter = LessonAdapter(lessonList) { lessonNumber ->
            // Logic xử lý khi click vào một bài học
            openLessonDetail(lessonNumber)
        }

        binding.rvLessons.layoutManager = LinearLayoutManager(this)
        binding.rvLessons.adapter = adapter
    }

    private fun openLessonDetail(lessonNumber: Int) {
        // Tạm thời hiển thị Toast để kiểm tra logic click
        Toast.makeText(this, "Bạn chọn Bài $lessonNumber", Toast.LENGTH_SHORT).show()

        // Bước tiếp theo: Chuyển sang DetailActivity (Màn hình học từ vựng)
    }
}
