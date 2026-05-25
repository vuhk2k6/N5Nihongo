package com.vunv.n5nihongo.data.repository

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerateContentResponse
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class AiRepository {

    // Khởi tạo mô hình Gemini 2.5 Flash bằng Firebase AI SDK mới nhất
    private val generativeModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = "gemini-2.5-flash",

            // System Instruction giúp định hình hành vi của AI Sensei
            systemInstruction = content {
                text(
                    "Bạn là một trợ lý giảng dạy tiếng Nhật trình độ N5 tên là 'AI Sensei' cực kỳ thân thiện và nhiệt huyết.\n" +
                            "Nhiệm vụ của bạn là đồng hành, hướng dẫn, giải thích ngữ pháp, từ vựng và chữ Hán N5 một cách đơn giản, dễ hiểu bằng tiếng Việt cho học viên.\n" +
                            "Luôn đi kèm ví dụ câu thực tế, phiên âm Hiragana/Furigana và nghĩa tiếng Việt.\n\n" +
                            "ĐẶC BIỆT: Bạn có khả năng 'dẫn đường' và đề xuất bài học trực tiếp cho học viên bằng cách chèn nút hành động điều hướng ở cuối câu trả lời nếu thấy phù hợp với ngữ cảnh học tập.\n" +
                            "Bạn hãy chèn nút điều hướng theo cú pháp chính xác: [Tên Nút Hiển Thị](navigate:route_name).\n" +
                            "Các đường dẫn (route_name) được hệ thống hỗ trợ gồm:\n" +
                            "- 'alphabet/1' : Học bảng chữ cái Hiragana (Ví dụ: [Luyện bảng chữ Hiragana](navigate:alphabet/1))\n" +
                            "- 'alphabet/2' : Học bảng chữ cái Katakana (Ví dụ: [Luyện bảng chữ Katakana](navigate:alphabet/2))\n" +
                            "- 'numbersTime' : Học chữ số và thời gian (Ví dụ: [Học Chữ số & Thời gian](navigate:numbersTime))\n" +
                            "- 'kanji' : Danh sách 80+ chữ Hán N5 (Ví dụ: [Học chữ Hán N5](navigate:kanji))\n" +
                            "- 'leaderboard' : Luyện tập kiểm tra trắc nghiệm tổng hợp (Ví dụ: [Luyện thi Trắc nghiệm](navigate:leaderboard))\n" +
                            "- 'lessonDetail/<id>' : Học chi tiết bài học từ bài 4 đến bài 25 (Ví dụ: [Học Từ vựng & Ngữ pháp bài 4](navigate:lessonDetail/4))\n" +
                            "- 'aiQuiz?prompt=<yêu_cầu>' : Tạo bài kiểm tra trắc nghiệm AI 5 câu theo yêu cầu thắc mắc/chủ đề ôn tập cụ thể (Ví dụ: [Làm bài kiểm tra từ vựng gia đình](navigate:aiQuiz?prompt=từ vựng gia đình) hoặc [Làm trắc nghiệm ngữ pháp bài 4](navigate:aiQuiz?prompt=ngữ pháp bài 4))\n\n" +
                            "Hãy chủ động đưa ra các nút gợi ý này để 'can thiệp sâu' và dẫn dắt học viên chuyển tiếp bài học một cách tự nhiên nhất!"
                )
            }
        )
    }

    /**
     * Gửi yêu cầu hỏi đáp thông thường và nhận phản hồi dạng luồng (Streaming)
     */
    fun askAiStream(prompt: String): Flow<GenerateContentResponse> = flow {
        val responseFlow = generativeModel.generateContentStream(prompt)
        responseFlow.collect { chunk ->
            emit(chunk)
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Sửa lỗi ngữ pháp cho câu tiếng Nhật của người dùng
     */
    fun correctGrammar(userSentence: String): Flow<GenerateContentResponse> = flow {
        val prompt = """
            Hãy phân tích câu tiếng Nhật sau đây của học viên trình độ N5: "$userSentence".
            Thực hiện các bước sau:
            1. Đánh giá câu này đúng hay sai ngữ pháp/ngữ cảnh.
            2. Nếu có lỗi sai (trợ từ, chia động từ, từ vựng...), hãy chỉ rõ lỗi đó ở đâu và giải thích chi tiết bằng tiếng Việt lý do tại sao sai.
            3. Đưa ra câu sửa lại đúng nhất và tự nhiên nhất kèm Furigana.
            4. Đặt 1-2 câu ví dụ tương tự để học viên luyện tập thêm.
        """.trimIndent()
        
        generativeModel.generateContentStream(prompt).collect { chunk ->
            emit(chunk)
        }
    }.flowOn(Dispatchers.IO)
}
