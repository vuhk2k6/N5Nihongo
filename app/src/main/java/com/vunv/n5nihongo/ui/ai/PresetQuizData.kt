package com.vunv.n5nihongo.ui.ai

object PresetQuizData {
    val PRESET_N5_QUESTIONS = listOf(
        AiQuestion(
            question = "Chọn nghĩa tiếng Việt của từ \"先生\" (せんせい):",
            options = listOf("Giáo viên", "Học sinh", "Bác sĩ", "Công nhân"),
            correctAnswer = "Giáo viên",
            explanation = "先生 (Tiên sinh) nghĩa là giáo viên, người dạy học."
        ),
        AiQuestion(
            question = "Cách đọc đúng của chữ Hán \"明日\" (Minh Nhật) là gì?",
            options = listOf("あした", "きょう", "きのう", "あさ"),
            correctAnswer = "あした",
            explanation = "明日 (Minh Nhật) nghĩa là ngày mai, đọc là あした (ashita)."
        ),
        AiQuestion(
            question = "Điền trợ từ thích hợp: \"わたしは にほんご ___ べんきょうします。\"",
            options = listOf("を", "が", "に", "で"),
            correctAnswer = "を",
            explanation = "Trợ từ を dùng để chỉ đối tượng chịu tác động trực tiếp của hành động べんきょうします (học)."
        ),
        AiQuestion(
            question = "Ý nghĩa của chữ Hán \"水\" (Thủy) là gì?",
            options = listOf("Nước", "Lửa", "Đất", "Vàng"),
            correctAnswer = "Nước",
            explanation = "水 (Thủy) nghĩa là nước, đọc là みず (mizu)."
        ),
        AiQuestion(
            question = "Chia động từ thích hợp: \"きのう、わたしは にほんへ ___。\"",
            options = listOf("いきました", "いきます", "いって", "いかない"),
            correctAnswer = "いきました",
            explanation = "Vì có trạng từ chỉ thời gian quá khứ きのう (hôm qua), ta chia động từ ở thể quá khứ lịch sự là いきました."
        ),
        AiQuestion(
            question = "Từ \"おねがいします\" thường được dùng trong ngữ cảnh nào?",
            options = listOf("Yêu cầu, nhờ vả lịch sự", "Chào buổi sáng", "Xin lỗi", "Cảm ơn"),
            correctAnswer = "Yêu cầu, nhờ vả lịch sự",
            explanation = "おねがいします (Xin vui lòng / Nhờ anh/chị giúp đỡ) dùng khi nhờ vả ai đó làm việc gì."
        ),
        AiQuestion(
            question = "Cách đọc tiêu biểu của chữ Hán \"車\" (Xa) là gì?",
            options = listOf("くるま", "でんしゃ", "じてんしゃ", "かわ"),
            correctAnswer = "くるま",
            explanation = "Chữ 車 (Xa - ô tô, xe hơi) có cách đọc Kunyomi là くるま (kuruma)."
        ),
        AiQuestion(
            question = "Mẫu ngữ pháp \"～てください\" biểu thị điều gì?",
            options = listOf("Yêu cầu, sai khiến lịch sự", "Xin phép làm gì đó", "Khuyên nhủ nên làm gì", "Cấm đoán làm gì"),
            correctAnswer = "Yêu cầu, sai khiến lịch sự",
            explanation = "Mẫu câu V-てください dùng để yêu cầu hoặc đề nghị ai đó làm gì một cách lịch sự."
        ),
        AiQuestion(
            question = "Chọn nghĩa tiếng Việt của từ \"時計\" (とけい):",
            options = listOf("Đồng hồ", "Điện thoại", "Sách", "Bút viết"),
            correctAnswer = "Đồng hồ",
            explanation = "時計 (Thời kế) nghĩa là đồng hồ chỉ thời gian."
        ),
        AiQuestion(
            question = "Cách đọc đúng của chữ Hán \"日本語\" (Nhật Bản Ngữ) là:",
            options = listOf("にほんご", "にほん", "にっぽん", "えいご"),
            correctAnswer = "にほんご",
            explanation = "日本語 (Nhật Bản Ngữ - tiếng Nhật) đọc là にほんご (nihongo)."
        ),
        AiQuestion(
            question = "Mẫu câu ngữ pháp \"A から B まで\" mang ý nghĩa là gì?",
            options = listOf("Từ A đến B (thời gian/không gian)", "Vì A nên kết quả B", "Trước khi làm A thì làm B", "Chỉ làm A chứ không làm B"),
            correctAnswer = "Từ A đến B (thời gian/không gian)",
            explanation = "から (từ) và まで (đến) dùng để chỉ điểm bắt đầu và điểm kết thúc của thời gian hoặc địa điểm."
        ),
        AiQuestion(
            question = "Ý nghĩa tiếng Việt của chữ Hán \"山\" (Sơn) là gì?",
            options = listOf("Núi", "Sông", "Ruộng", "Biển"),
            correctAnswer = "Núi",
            explanation = "山 (Sơn) nghĩa là ngọn núi, đọc là やま (yama)."
        ),
        AiQuestion(
            question = "Chọn nghĩa tiếng Việt của từ \"ありがとう\":",
            options = listOf("Cảm ơn", "Xin lỗi", "Chào tạm biệt", "Không có gì"),
            correctAnswer = "Cảm ơn",
            explanation = "ありがとう (Arigatou) là lời cảm ơn thân mật trong tiếng Nhật."
        ),
        AiQuestion(
            question = "Mẫu câu ngữ pháp \"V-たいです\" biểu thị điều gì?",
            options = listOf("Mong muốn làm gì của bản thân", "Đang trong quá trình làm gì", "Khả năng có thể làm gì", "Đã hoàn thành xong việc gì"),
            correctAnswer = "Mong muốn làm gì của bản thân",
            explanation = "Mẫu câu V-たいです diễn tả mong muốn, nguyện vọng làm gì đó của người nói."
        ),
        AiQuestion(
            question = "Chọn cách viết Kanji đúng của từ \"い（きます）\" trong \"行きます\":",
            options = listOf("行", "来", "帰", "食"),
            correctAnswer = "行",
            explanation = "Động từ 行きます (đi) có chữ Hán là 行 (Hành)."
        )
    )
}
