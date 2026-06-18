# BẢNG PHÂN CHIA CÔNG VIỆC DỰ ÁN DACS3 (ĐỒ ÁN CƠ SỞ 3)
## Tên đề tài: Ứng dụng Học Tiếng Nhật N5 Tích Hợp Trợ Lý AI (N5 Nihongo)

Dự án được thực hiện bởi nhóm gồm **02 thành viên**. Công việc được phân chia dựa trên chuyên môn phát triển: một thành viên tập trung vào phần **UI/UX & Core Learning (Giao diện & Tính năng học tập cốt lõi)** và thành viên còn lại tập trung vào phần **System, Backend & AI Logic (Hệ thống, Dữ liệu, Firebase & Trợ lý AI)**.

---

### I. THÔNG TIN CHUNG
* **Nền tảng phát triển:** Android (Kotlin, Jetpack Compose)
* **Kiến trúc ứng dụng:** MVVM (Model-View-ViewModel) kết hợp với Repository Pattern
* **Công nghệ cốt lõi:** 
  * **Frontend:** Jetpack Compose (Material 3, Glassmorphism, Custom Canvas drawing)
  * **Local Database:** Room Database & SQLite
  * **Backend & Sync:** Firebase Authentication, Cloud Firestore, WorkManager
  * **AI Integration:** Firebase AI SDK (Gemini 2.5 Flash)
  * **Audio:** Text-to-Speech (TTS) hỗ trợ phát âm tiếng Nhật.

---

### II. BẢNG PHÂN CHIA CÔNG VIỆC CHI TIẾT

| STT | Hạng mục công việc / Tính năng | Mô tả chi tiết nhiệm vụ | File source code chính liên quan | Người đảm nhận | Trạng thái |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **1** | **Thiết kế UI/UX & Design System** | Xây dựng theme ứng dụng, bảng màu, typography, các component dùng chung (GlassCard, Custom Loading, Canvas...). | `ui/theme/Color.kt`, `Theme.kt`, `Type.kt`, `ui/components/GlassCard.kt`, `LessonScreenLoading.kt` | **Sinh viên A** (Trưởng nhóm) | Hoàn thành |
| **2** | **Bảng chữ cái & Luyện viết nét (Alphabet Master)** | Xây dựng màn hình bảng chữ Hiragana/Katakana. Vẽ canvas cho phép vẽ đè (Draw Over) nét chữ, hiển thị thứ tự nét động. | `AlphabetMasterScreen.kt`, `AlphabetViewModel.kt`, `DrawPracticeCanvas.kt`, `StrokeOrderAnimatedSection.kt` | **Sinh viên A** (Trưởng nhóm) | Hoàn thành |
| **3** | **Danh mục & Chi tiết Kanji** | Xây dựng danh sách Kanji N5, màn hình chi tiết Kanji, hỗ trợ vẽ nét Kanji theo đúng thứ tự. | `KanjiListScreen.kt`, `KanjiDetailScreen.kt`, `KanjiWritingPractice.kt`, `KanjiListViewModel.kt` | **Sinh viên A** (Trưởng nhóm) | Hoàn thành |
| **4** | **Lộ trình học tập & Trang chủ (Learning Path & Home)** | Xây dựng cây sơ đồ lộ trình học tập (Roadmap Node), các màn hình học từ vựng, ngữ pháp theo từng bài khóa (Minna no Nihongo). | `HomeLearningScreen.kt`, `LearningPathViewModel.kt`, `RoadmapNode.kt`, `LessonDetailScreen.kt` | **Sinh viên A** (Trưởng nhóm) | Hoàn thành |
| **5** | **Chuẩn bị Dữ liệu & Python Scripts** | Viết scripts crawl và tối ưu hóa dữ liệu KanjiVG, font chữ và xuất cơ sở dữ liệu mẫu tiếng Nhật dưới dạng JSON/SQLite. | `script/generate_lessons.py`, `script/enhance_kanjivg.py`, `download_kanjivg.py` | **Sinh viên A** (Trưởng nhóm) | Hoàn thành |
| **6** | **Xác thực người dùng (Auth System)** | Tích hợp Firebase Auth cho phép đăng nhập qua Email, Google, Facebook. Xây dựng màn hình đăng nhập và trang cá nhân. | `LoginScreen.kt`, `ProfileAuthScreen.kt`, `ProfileScreen.kt`, `AuthViewModel.kt` | **Sinh viên B** (Thành viên) | Hoàn thành |
| **7** | **Cơ sở dữ liệu cục bộ & Repository** | Thiết lập cấu trúc Room Database (Entities, DAOs) cho Word, UserProgress. Xây dựng Repository quản lý dữ liệu tập trung. | `data/local/*`, `data/repository/DataRepository.kt`, `data/repository/QuizRepository.kt` | **Sinh viên B** (Thành viên) | Hoàn thành |
| **8** | **Hệ thống ôn tập & Thi thử (Quiz & Mock Exam)** | Lập trình logic tạo câu hỏi trắc nghiệm tự động theo bài học và tạo đề thi thử mô phỏng kỳ thi JLPT N5 thực tế. | `QuizScreen.kt`, `QuizViewModel.kt`, `MockExamScreen.kt`, `MockExamViewModel.kt`, `data/quiz/*` | **Sinh viên B** (Thành viên) | Hoàn thành |
| **9** | **Trợ lý học tập AI Sensei & AI Quiz** | Tích hợp Firebase AI SDK (Gemini 2.5 Flash). Xây dựng chatbot AI Sensei giải đáp ngữ pháp và AI Quiz tự sinh câu hỏi theo yêu cầu học viên. | `AiAssistantScreen.kt`, `AiAssistantViewModel.kt`, `AiQuizScreen.kt`, `AiQuizViewModel.kt`, `AiRepository.kt` | **Sinh viên B** (Thành viên) | Hoàn thành |
| **10** | **Đồng bộ tiến trình & Nhắc nhở (Sync & Worker)** | Sử dụng WorkManager lập trình các tác vụ chạy ngầm để đồng bộ tiến trình học tập lên Cloud Firestore và hiển thị thông báo nhắc nhở hàng ngày. | `work/UserProgressSyncWorker.kt`, `work/DailyReminderWorker.kt`, `data/repository/UserProgressSyncRepository.kt` | **Sinh viên B** (Thành viên) | Hoàn thành |
| **11** | **Bảng xếp hạng (Leaderboard)** | Tích hợp Cloud Firestore để lưu điểm số và xếp hạng của tất cả người dùng trong hệ thống theo thời gian thực. | `LeaderboardScreen.kt`, `LeaderboardViewModel.kt` | **Sinh viên B** (Thành viên) | Hoàn thành |

---

### III. BÁO CÁO ĐÓNG GÓP CHI TIẾT THEO TỪNG THÀNH VIÊN

#### 1. SINH VIÊN A (Trưởng nhóm) - Chuyên trách UI/UX, Core Learning & Assets
* **Nhiệm vụ chính:** Phát triển toàn bộ các màn hình học tập cốt lõi trực quan, đảm bảo tính tương tác cao (Canvas Drawing, Stroke Animation) và chuẩn bị cơ sở dữ liệu học tập phong phú.
* **Chi tiết đóng góp mã nguồn:**
  * **Màn hình bảng chữ cái (Hiragana/Katakana):** Xây dựng `AlphabetMasterScreen.kt` kết hợp với `DrawPracticeCanvas.kt` hỗ trợ người dùng học cách viết chữ cái trực tiếp bằng cử chỉ vuốt ngón tay trên màn hình. Thiết lập hoạt ảnh hiển thị nét chạy từng bước qua `StrokeOrderAnimatedSection.kt`.
  * **Màn hình Kanji:** Thiết kế `KanjiListScreen.kt` hiển thị hơn 80 chữ Hán căn bản và `KanjiWritingPractice.kt` giúp học viên tập viết các nét Kanji phức tạp dựa trên quy tắc nét chuẩn.
  * **Trang chủ & Lộ trình học (Roadmap):** Xây dựng `HomeLearningScreen.kt` sử dụng sơ đồ nhánh `RoadmapNode.kt` để người dùng dễ dàng theo dõi tiến độ từ bài 1 đến bài 25.
  * **Học từ vựng qua Flashcard:** Thiết kế tính năng trượt thẻ từ vựng trực quan thông qua `FlashcardLearningScreen.kt`.
  * **Hệ thống dữ liệu học tập:** Viết các công cụ Python trong thư mục `script/` để tự động hóa việc tải và chuẩn bị dữ liệu học tập từ vựng, ngữ pháp tiếng Nhật N5, cũng như sơ đồ vector nét chữ KanjiVG.

#### 2. SINH VIÊN B (Thành viên) - Chuyên trách Core Backend, Database, AI & Cloud Services
* **Nhiệm vụ chính:** Đảm bảo hệ thống vận hành mượt mà, lưu trữ dữ liệu đồng bộ, bảo mật thông tin người dùng và áp dụng công nghệ trí tuệ nhân tạo (Generative AI) để nâng cao trải nghiệm tự học.
* **Chi tiết đóng góp mã nguồn:**
  * **Hệ thống đăng nhập & Hồ sơ:** Tích hợp Firebase Auth để quản lý danh tính người dùng qua tài khoản Google/Facebook. Viết logic quản lý trạng thái phiên đăng nhập trong `AuthViewModel.kt` và giao diện `ProfileScreen.kt`.
  * **Hệ thống Phòng thi & Kiểm tra (Quiz & Mock Exam):** Thiết lập thuật toán trích xuất câu hỏi ngẫu nhiên trong `LessonQuizGenerator.kt` và cấu trúc bài thi JLPT chuẩn trong `MockExamGenerator.kt`. Phát triển logic chấm điểm và hiển thị kết quả kiểm tra.
  * **Tích hợp Trí tuệ nhân tạo AI:** Triển khai `AiRepository.kt` sử dụng Firebase AI SDK với mô hình **Gemini 2.5 Flash** để phát triển trợ lý ảo **AI Sensei** thân thiện, có khả năng giải thích ngữ pháp tiếng Nhật bằng tiếng Việt và đề xuất đường dẫn bài học phù hợp. Tích hợp AI tạo đề thi nhanh tức thời dựa trên từ khóa người dùng nhập vào.
  * **Đồng bộ đám mây & Chạy ngầm:** Sử dụng `UserProgressSyncRepository.kt` kết nối Firestore đồng bộ hóa tiến trình học cục bộ lên đám mây. Cài đặt các tiến trình nền `UserProgressSyncWorker.kt` và thông báo đẩy nhắc nhở học tập hàng ngày qua `DailyReminderWorker.kt` (WorkManager API).
  * **Bảng xếp hạng trực tuyến:** Lấy dữ liệu từ Cloud Firestore để cập nhật thứ hạng của học viên theo thời gian thực trên màn hình `LeaderboardScreen.kt`, thúc đẩy động lực học tập nhóm.
