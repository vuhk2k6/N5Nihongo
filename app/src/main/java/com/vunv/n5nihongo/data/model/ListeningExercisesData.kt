package com.vunv.n5nihongo.data.model

data class DialogueLine(
    val speaker: String,
    val text: String,
    val translation: String
)

data class ListeningExercise(
    val lessonId: Int,
    val title: String,
    val situation: String,
    val dialogue: List<DialogueLine>,
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

object ListeningExercisesData {
    val exercises = listOf(
        // Lesson 1 (Lesson ID 4)
        ListeningExercise(
            lessonId = 4,
            title = "Bài 1: Giới thiệu bản thân ngày đầu tiên 🤝",
            situation = "Anh Miller lần đầu gặp anh Yamada tại trường học.",
            dialogue = listOf(
                DialogueLine("Miller", "初めまして。マイク・ミラーです。アメリカから来ました。", "Rất hân hạnh được làm quen. Tôi là Mike Miller. Tôi đến từ Mỹ."),
                DialogueLine("Yamada", "初めまして、山田です。私は教師です。どうぞよろしく。", "Hân hạnh làm quen, tôi là Yamada. Tôi là giáo viên. Rất mong được giúp đỡ.")
            ),
            question = "Anh Miller đến từ nước nào?",
            options = listOf("Nhật Bản", "Mỹ", "Anh", "Đức"),
            correctIndex = 1,
            explanation = "Anh Miller tự giới thiệu: 'アメリカから来ました' (Tôi đến từ Mỹ)."
        ),
        // Lesson 2 (Lesson ID 5)
        ListeningExercise(
            lessonId = 5,
            title = "Bài 2: Đồ vật xung quanh ta 🎁",
            situation = "Santos và Tanaka hỏi nhau về chủ nhân của cái ô trên bàn.",
            dialogue = listOf(
                DialogueLine("Tanaka", "サントスさん、これはあなたの傘ですか。", "Anh Santos ơi, đây là ô của anh phải không?"),
                DialogueLine("Santos", "いいえ、違います。それはミラーさんの傘です。", "Không, không phải đâu. Đó là cái ô của anh Miller đấy.")
            ),
            question = "Cái ô là của ai?",
            options = listOf("Của Tanaka", "Của Santos", "Của Miller", "Không của ai cả"),
            correctIndex = 2,
            explanation = "Santos khẳng định: 'それはミラーさんの傘です' (Đó là cái ô của anh Miller)."
        ),
        // Lesson 3 (Lesson ID 6)
        ListeningExercise(
            lessonId = 6,
            title = "Bài 3: Hỏi đường đi vệ sinh 🚽",
            situation = "Cô Karina đang đi tìm nhà vệ sinh tại trung tâm thương mại.",
            dialogue = listOf(
                DialogueLine("Karina", "すみません、お手洗いはどこですか。", "Xin lỗi, nhà vệ sinh ở đâu vậy ạ?"),
                DialogueLine("Lễ tân", "あちらです。エレベーターの隣ですよ。", "Ở phía kia ạ. Ngay bên cạnh thang máy đấy.")
            ),
            question = "Nhà vệ sinh nằm ở đâu?",
            options = listOf("Ở tầng 2", "Bên cạnh thang máy", "Bên trong văn phòng", "Gần lối ra vào"),
            correctIndex = 1,
            explanation = "Nhân viên lễ tân hướng dẫn: 'エレベーターの隣ですよ' (Ngay bên cạnh thang máy đấy)."
        ),
        // Lesson 4 (Lesson ID 7)
        ListeningExercise(
            lessonId = 7,
            title = "Bài 4: Hỏi giờ hoạt động bảo tàng ⏰",
            situation = "Anh Miller hỏi nhân viên nhà ga về giờ giấc của bảo tàng mỹ thuật.",
            dialogue = listOf(
                DialogueLine("Miller", "すみません、美術館は何時から何時までですか。", "Xin lỗi, bảo tàng mỹ thuật mở cửa từ mấy giờ đến mấy giờ vậy?"),
                DialogueLine("Nhân viên", "午前９時から午後５時までです。休みは月曜日です。", "Từ 9 giờ sáng đến 5 giờ chiều ạ. Bảo tàng nghỉ vào thứ Hai.")
            ),
            question = "Bảo tàng mỹ thuật đóng cửa lúc mấy giờ?",
            options = listOf("9 giờ sáng", "5 giờ chiều", "7 giờ tối", "Đóng cửa thứ Hai"),
            correctIndex = 1,
            explanation = "Nhân viên trả lời: '午後５時までです' (Đến 5 giờ chiều)."
        ),
        // Lesson 5 (Lesson ID 8)
        ListeningExercise(
            lessonId = 8,
            title = "Bài 5: Đi chơi cuối tuần 🚄",
            situation = "Anh Yamada hỏi anh Santos về chuyến đi chơi cuối tuần vừa rồi.",
            dialogue = listOf(
                DialogueLine("Yamada", "サントスさん、日曜日にどこへ行きましたか。", "Anh Santos, chủ nhật vừa rồi anh đã đi đâu thế?"),
                DialogueLine("Santos", "家族と京都へ行きました。新幹線で行きましたよ。", "Tôi đã đi Kyoto cùng gia đình. Chúng tôi đi bằng tàu Shinkansen đấy.")
            ),
            question = "Anh Santos đã đi Kyoto bằng phương tiện gì?",
            options = listOf("Xe buýt", "Tàu điện ngầm", "Tàu Shinkansen", "Xe cá nhân"),
            correctIndex = 2,
            explanation = "Santos nói: '新幹線で行きました' (Tôi đi bằng tàu Shinkansen)."
        ),
        // Lesson 6 (Lesson ID 9)
        ListeningExercise(
            lessonId = 9,
            title = "Bài 6: Cùng đi ăn trưa 🍛",
            situation = "Anh Miller rủ cô Tanaka đi ăn trưa cùng nhau.",
            dialogue = listOf(
                DialogueLine("Miller", "田中さん、一緒に昼ご飯を食べませんか。", "Cô Tanaka, chúng ta cùng đi ăn trưa nhé?"),
                DialogueLine("Tanaka", "ええ、いいですね。何を食べますか。", "Vâng, được đấy ạ. Chúng ta ăn gì đây?"),
                DialogueLine("Miller", "駅の近くのレストランでカレーを食べましょう。", "Hãy ăn cà ri ở nhà hàng gần nhà ga nhé.")
            ),
            question = "Hai người quyết định sẽ ăn món gì?",
            options = listOf("Sushi", "Mì Ramen", "Cơm cà ri", "Thịt nướng"),
            correctIndex = 2,
            explanation = "Anh Miller đề xuất: 'カレーを食べましょう' (Chúng ta hãy ăn cà ri nhé)."
        ),
        // Lesson 7 (Lesson ID 10)
        ListeningExercise(
            lessonId = 10,
            title = "Bài 7: Tặng quà sinh nhật 🎁",
            situation = "Cô Yamada hỏi anh Santos về món quà tặng cho vợ.",
            dialogue = listOf(
                DialogueLine("Yamada", "サントスさん、奥さんの誕生日に何をあげましたか。", "Anh Santos, anh đã tặng gì cho vợ vào ngày sinh nhật thế?"),
                DialogueLine("Santos", "綺麗な花をあげました。カードも書きましたよ。", "Tôi đã tặng hoa đẹp. Tôi cũng viết cả thiệp nữa đấy.")
            ),
            question = "Anh Santos đã tặng gì cho vợ?",
            options = listOf("Một cái nhẫn", "Hoa và thiệp", "Một chiếc túi xách", "Một thỏi son"),
            correctIndex = 1,
            explanation = "Santos chia sẻ: '綺麗な花をあげました。カードも書きました' (Tặng hoa đẹp và viết thiệp)."
        ),
        // Lesson 8 (Lesson ID 11)
        ListeningExercise(
            lessonId = 11,
            title = "Bài 8: Đánh giá về Kyoto ⛩️",
            situation = "Anh Yamada và cô Karina nói chuyện về chuyến đi Kyoto của Karina.",
            dialogue = listOf(
                DialogueLine("Yamada", "京都はどうでしたか。面白かったですか。", "Kyoto thế nào rồi? Có thú vị không em?"),
                DialogueLine("Karina", "はい、とても綺麗でした。でも、人が多かったです。", "Vâng, rất đẹp luôn ạ. Nhưng mà người đông quá trời.")
            ),
            question = "Cô Karina nghĩ gì về Kyoto?",
            options = listOf("Rất chán và vắng", "Rất đẹp nhưng đông người", "Không đẹp lắm", "Yên tĩnh và thơ mộng"),
            correctIndex = 1,
            explanation = "Karina khen đẹp nhưng đông: 'とても綺麗でした。でも、人が多かったです'."
        ),
        // Lesson 9 (Lesson ID 12)
        ListeningExercise(
            lessonId = 12,
            title = "Bài 9: Sở thích thể thao ⚽",
            situation = "Anh Tanaka hỏi anh Miller về sở thích thể thao của anh.",
            dialogue = listOf(
                DialogueLine("Tanaka", "ミラーさんはどんなスポーツが好きですか。", "Anh Miller thích thể loại thể thao nào thế?"),
                DialogueLine("Miller", "サッカーが好きです。野球はあまり好きじゃありません。", "Tôi thích bóng đá. Còn bóng chày thì tôi không thích lắm.")
            ),
            question = "Môn thể thao nào anh Miller KHÔNG thích lắm?",
            options = listOf("Bóng đá", "Bóng chày", "Bơi lội", "Tennis"),
            correctIndex = 1,
            explanation = "Anh Miller bày tỏ: '野球はあまり好きじゃありません' (Bóng chày thì không thích lắm)."
        ),
        // Lesson 10 (Lesson ID 13)
        ListeningExercise(
            lessonId = 13,
            title = "Bài 10: Tìm đồ đạc trong phòng 🐱",
            situation = "Mẹ hỏi con trai về vị trí của chú mèo cưng.",
            dialogue = listOf(
                DialogueLine("Mẹ", "タカシ、猫のタマはどこにいますか。", "Takashi, chú mèo Tama đang ở đâu thế con?"),
                DialogueLine("Tadashi", "ベッドの上にいますよ。本棚の近くです。", "Nó ở trên giường đấy mẹ. Gần cái giá sách ấy ạ.")
            ),
            question = "Chú mèo Tama đang ở đâu?",
            options = listOf("Trong tủ quần áo", "Dưới gầm giường", "Trên giường", "Ngoài sân"),
            correctIndex = 2,
            explanation = "Tadashi trả lời: 'ベッドの上にいますよ' (Ở trên giường đấy ạ)."
        ),
        // Lesson 11 (Lesson ID 14)
        ListeningExercise(
            lessonId = 14,
            title = "Bài 11: Mua táo ở cửa hàng 🍎",
            situation = "Anh Miller đi siêu thị mua hoa quả tươi.",
            dialogue = listOf(
                DialogueLine("Miller", "すみません、このりんごを４つください。", "Xin lỗi, cho tôi mua 4 quả táo này với ạ."),
                DialogueLine("Bán hàng", "はい、４つですね。全部で６０0円になります。", "Vâng, 4 quả ạ. Tổng cộng hết 600 Yên nhé ạ.")
            ),
            question = "Anh Miller mua bao nhiêu quả táo và hết bao nhiêu tiền?",
            options = listOf("3 quả - 500 Yên", "4 quả - 600 Yên", "5 quả - 700 Yên", "4 quả - 400 Yên"),
            correctIndex = 1,
            explanation = "Anh Miller gọi 4 quả ('りんごを４つ') và người bán báo giá 600 Yên ('６０0円')."
        ),
        // Lesson 12 (Lesson ID 15)
        ListeningExercise(
            lessonId = 15,
            title = "Bài 12: So sánh thời tiết hôm nay ☀️",
            situation = "Hai người bạn trò chuyện về thời tiết ngày hôm qua và hôm nay.",
            dialogue = listOf(
                DialogueLine("A", "昨日は雨でしたね。寒かったです。", "Hôm qua trời mưa nhỉ. Lạnh ghê cơ."),
                DialogueLine("B", "ええ。でも、今日は天気が良くて、暖かくなりましたね。", "Ừ. Nhưng hôm nay thời tiết đẹp hẳn, ấm lên nhiều rồi.")
            ),
            question = "Thời tiết hôm nay như thế nào?",
            options = listOf("Lạnh và mưa", "U ám và có gió", "Thời tiết tốt và ấm áp", "Có tuyết rơi"),
            correctIndex = 2,
            explanation = "Người B nói: '今日は天気が良くて、暖かくなりましたね' (Hôm nay trời đẹp và ấm lên rồi)."
        ),
        // Lesson 13 (Lesson ID 16)
        ListeningExercise(
            lessonId = 16,
            title = "Bài 13: Mong muốn kỳ nghỉ hè 🏖️",
            situation = "Santos hỏi người bạn về dự định kỳ nghỉ hè sắp tới.",
            dialogue = listOf(
                DialogueLine("Santos", "夏休みにどこへ行きたいですか。", "Kỳ nghỉ hè này cậu muốn đi đâu chơi không?"),
                DialogueLine("Bạn", "海へ行きたいです。綺麗な海で泳ぎたいですね。", "Tớ muốn đi biển. Tớ cực kỳ muốn bơi ở bãi biển đẹp luôn.")
            ),
            question = "Người bạn của Santos muốn làm gì vào kỳ nghỉ hè?",
            options = listOf("Đi leo núi", "Đi tắm biển và bơi", "Ở nhà ngủ", "Đi du lịch nước ngoài"),
            correctIndex = 1,
            explanation = "Người bạn nói: '海へ行きたいです。綺麗な海で泳ぎたい' (Muốn đi biển và bơi)."
        ),
        // Lesson 14 (Lesson ID 17)
        ListeningExercise(
            lessonId = 17,
            title = "Bài 14: Nhờ vả chụp ảnh giúp 📸",
            situation = "Một du khách nhờ người qua đường chụp ảnh kỷ niệm.",
            dialogue = listOf(
                DialogueLine("Du khách", "すみません、写真を撮ってくださいませんか。", "Xin lỗi, anh có thể chụp hộ tôi tấm ảnh được không ạ?"),
                DialogueLine("Người qua đường", "いいですよ。そこに立ってください。はい、チーズ！", "Được chứ ạ. Bạn đứng ở chỗ kia đi nhé. Rồi, cười lên nào!")
            ),
            question = "Người qua đường đã làm gì giúp du khách?",
            options = listOf("Chỉ đường đi", "Chụp hộ ảnh", "Mua hộ vé", "Xách hộ hành lý"),
            correctIndex = 1,
            explanation = "Du khách nhờ: '写真を撮ってくださいませんか' (Chụp ảnh hộ tôi)."
        ),
        // Lesson 15 (Lesson ID 18)
        ListeningExercise(
            lessonId = 18,
            title = "Bài 15: Hỏi thăm công việc hiện tại 💻",
            situation = "Anh Yamada gặp lại người bạn cũ và hỏi thăm cuộc sống.",
            dialogue = listOf(
                DialogueLine("Yamada", "今、どんな仕事をしていますか。", "Bây giờ cậu đang làm công việc gì thế?"),
                DialogueLine("Bạn", "コンピューターの会社で働いています。プログラミングをしていますよ。", "Tớ đang làm việc ở công ty máy tính. Tớ làm lập trình viên đấy.")
            ),
            question = "Người bạn của anh Yamada làm nghề gì?",
            options = listOf("Bác sĩ", "Giáo viên", "Lập trình viên máy tính", "Nhân viên ngân hàng"),
            correctIndex = 2,
            explanation = "Người bạn nói đang làm ở công ty máy tính, lập trình: 'コンピューターの会社...プログラミングをしています'."
        ),
        // Lesson 16 (Lesson ID 19)
        ListeningExercise(
            lessonId = 19,
            title = "Bài 16: Hướng dẫn rút tiền ATM 🏦",
            situation = "Một người nước ngoài nhờ nhân viên ngân hàng chỉ cách rút tiền.",
            dialogue = listOf(
                DialogueLine("Khách", "すみません、お金のおろし方を教えてください。", "Xin lỗi, chỉ hộ tôi cách rút tiền mặt với ạ."),
                DialogueLine("Nhân viên", "まずここにカードを入れて、次に暗証番号を押してください。", "Đầu tiên quý khách cho thẻ vào đây, sau đó bấm mã pin nhé ạ.")
            ),
            question = "Bước đầu tiên cần làm để rút tiền là gì?",
            options = listOf("Bấm số tiền cần rút", "Cho thẻ vào máy ATM", "Bấm mã pin", "Nhấn nút màu xanh"),
            correctIndex = 1,
            explanation = "Nhân viên hướng dẫn: 'まずここにカードを入れて' (Đầu tiên cho thẻ vào đây)."
        ),
        // Lesson 17 (Lesson ID 20)
        ListeningExercise(
            lessonId = 20,
            title = "Bài 17: Lời khuyên của bác sĩ 🏥",
            situation = "Bác sĩ đang khám bệnh và dặn dò bệnh nhân bị sốt.",
            dialogue = listOf(
                DialogueLine("Bác sĩ", "風邪ですね。今夜はお風呂に入らないでください。", "Cậu bị cảm rồi nhé. Tối nay đừng có tắm bồn đấy."),
                DialogueLine("Bệnh nhân", "わかりました。薬はいつ飲みますか。", "Tôi hiểu rồi ạ. Thuốc thì uống khi nào thế bác sĩ?"),
                DialogueLine("Bác sĩ", "ご飯を食べた後で、飲んでください。", "Sau khi ăn cơm xong thì hãy uống thuốc nhé.")
            ),
            question = "Bác sĩ khuyên bệnh nhân KHÔNG nên làm gì tối nay?",
            options = listOf("Uống thuốc", "Ăn cơm tối", "Tắm bồn (tắm rửa)", "Đi ngủ muộn"),
            correctIndex = 2,
            explanation = "Bác sĩ dặn: 'お風呂に入らないでください' (Không tắm bồn/tắm rửa)."
        ),
        // Lesson 18 (Lesson ID 21)
        ListeningExercise(
            lessonId = 21,
            title = "Bài 18: Sở thích cá nhân 🎸",
            situation = "Cô Tanaka hỏi anh Miller về việc anh có biết chơi nhạc cụ không.",
            dialogue = listOf(
                DialogueLine("Tanaka", "ミラーさんはギターを弾くことができますか。", "Anh Miller có biết chơi đàn guitar không thế?"),
                DialogueLine("Miller", "ええ、少しできます。でも、歌うことは下手です。", "Vâng, tôi chơi được chút ít. Nhưng hát hò thì tệ lắm.")
            ),
            question = "Anh Miller có khả năng làm gì?",
            options = listOf("Hát rất hay", "Chơi guitar tốt", "Biết vẽ tranh", "Chơi guitar được một chút"),
            correctIndex = 3,
            explanation = "Anh Miller nói chơi được chút ít: 'ええ、少しできます' (đối với việc chơi guitar)."
        ),
        // Lesson 19 (Lesson ID 22)
        ListeningExercise(
            lessonId = 22,
            title = "Bài 19: Trải nghiệm leo núi Phú Sĩ 🗻",
            situation = "Santos kể về trải nghiệm thú vị ở Nhật Bản.",
            dialogue = listOf(
                DialogueLine("A", "富士山に登ったことがありますか。", "Cậu đã từng leo núi Phú Sĩ lần nào chưa?"),
                DialogueLine("Santos", "はい、一度あります。とても寒かったですが、楽しかったです。", "Vâng, tớ leo một lần rồi. Lạnh lắm luôn nhưng cực kỳ vui.")
            ),
            question = "Santos đã leo núi Phú Sĩ mấy lần?",
            options = listOf("Chưa từng leo", "Một lần", "Hai lần", "Rất nhiều lần"),
            correctIndex = 1,
            explanation = "Santos nói đã leo một lần: 'はい、一度あります' (Vâng, có một lần)."
        ),
        // Lesson 20 (Lesson ID 23)
        ListeningExercise(
            lessonId = 23,
            title = "Bài 20: Thể thông thường thân mật 🗣️",
            situation = "Hai người bạn thân hẹn hò cuối tuần nói chuyện bằng thể thông thường.",
            dialogue = listOf(
                DialogueLine("A", "明日、暇？一緒に映画見に行かない？", "Mai rảnh không? Đi xem phim chung không cậu?"),
                DialogueLine("B", "うーん、明日はちょっと用事があるから、行けない。ごめん。", "Ừm, mai tớ bận chút việc rồi nên không đi được. Xin lỗi nha.")
            ),
            question = "Tại sao người B không đi xem phim ngày mai được?",
            options = listOf("Không thích xem phim", "Phim hết vé", "Ngày mai bận việc", "Phải đi học"),
            correctIndex = 2,
            explanation = "Người B từ chối vì bận: '明日はちょっと用事があるから' (Vì mai có chút việc riêng)."
        ),
        // Lesson 21 (Lesson ID 24)
        ListeningExercise(
            lessonId = 24,
            title = "Bài 21: Suy nghĩ về tương lai 🤖",
            situation = "Hai đồng nghiệp thảo luận về sự phát triển của Robot.",
            dialogue = listOf(
                DialogueLine("A", "将来、ロボットが何でもすると思いますか。", "Cậu có nghĩ tương lai robot sẽ làm được mọi thứ không?"),
                DialogueLine("B", "はい、便利になりますが、仕事がなくなると思います。", "Vâng, sẽ tiện lợi hơn nhưng tớ nghĩ con người sẽ bị mất việc làm.")
            ),
            question = "Người B lo lắng điều gì về tương lai của Robot?",
            options = listOf("Robot quá đắt đỏ", "Robot sẽ làm hỏng máy móc", "Con người bị mất việc làm", "Robot không an toàn"),
            correctIndex = 2,
            explanation = "Người B nói: '仕事がなくなると思います' (Tôi nghĩ công việc/việc làm sẽ biến mất)."
        ),
        // Lesson 22 (Lesson ID 25)
        ListeningExercise(
            lessonId = 25,
            title = "Bài 22: Hỏi về chiếc áo đang mặc 🧥",
            situation = "Cô Yamada khen chiếc áo khoác của anh Miller.",
            dialogue = listOf(
                DialogueLine("Yamada", "そのミラーさんが着ているコート、素敵ですね。", "Chiếc áo khoác anh Miller đang mặc đẹp thế nhỉ."),
                DialogueLine("Miller", "ありがとうございます。これは誕生日に姉がくれたコートです。", "Cảm ơn cô. Đây là chiếc áo khoác chị gái tặng tôi dịp sinh nhật đấy.")
            ),
            question = "Chiếc áo khoác của anh Miller từ đâu mà có?",
            options = listOf("Tự mua ở cửa hàng", "Được chị gái tặng", "Được mẹ tặng", "Bạn thân cho mượn"),
            correctIndex = 1,
            explanation = "Anh Miller nói: '姉がくれたコート' (Áo khoác chị gái tặng)."
        ),
        // Lesson 23 (Lesson ID 26)
        ListeningExercise(
            lessonId = 26,
            title = "Bài 23: Khi đi du lịch gặp khó khăn 🗺️",
            situation = "Một du khách hỏi đường người dân bản địa khi bị lạc.",
            dialogue = listOf(
                DialogueLine("Khách", "道がわからないとき、どうしますか。", "Khi không biết đường thì cậu thường làm thế nào?"),
                DialogueLine("Bản địa", "スマホの地図を見るか、交番の警察官に聞きますよ。", "Tớ sẽ xem bản đồ trên điện thoại, hoặc hỏi cảnh sát ở bốt gác ấy.")
            ),
            question = "Người dân bản địa khuyên làm gì khi không biết đường?",
            options = listOf("Đứng đợi người thân", "Xem bản đồ điện thoại hoặc hỏi cảnh sát", "Đi taxi về nhà", "Hỏi bất kỳ ai trên đường"),
            correctIndex = 1,
            explanation = "Người dân nói: 'スマホの地図を見るか、交番の警察官に聞きます' (Xem bản đồ điện thoại hoặc hỏi cảnh sát ở bốt)."
        ),
        // Lesson 24 (Lesson ID 27)
        ListeningExercise(
            lessonId = 27,
            title = "Bài 24: Nhận và cho tặng 🍰",
            situation = "Cô Tanaka mang bánh ngọt tự tay làm đến văn phòng tặng đồng nghiệp.",
            dialogue = listOf(
                DialogueLine("Tanaka", "このケーキ、私が作りました。どうぞ食べてください。", "Chiếc bánh ngọt này do tự tay em làm đấy ạ. Mời mọi người ăn thử."),
                DialogueLine("A", "わあ、ありがとうございます！とても美味しいですね！", "Oa, cảm ơn cô nhé! Bánh ngon quá trời luôn!")
            ),
            question = "Chiếc bánh ngọt từ đâu ra?",
            options = listOf("Mua ở tiệm bánh", "Đồng nghiệp mang tặng", "Cô Tanaka tự tay làm", "Mua ở siêu thị"),
            correctIndex = 2,
            explanation = "Cô Tanaka tự giới thiệu: '私が作りました' (Em tự tay làm đấy ạ)."
        ),
        // Lesson 25 (Lesson ID 28)
        ListeningExercise(
            lessonId = 28,
            title = "Bài 25: Kế hoạch nếu trời mưa ngày mai 🌧️",
            situation = "Học viên thảo luận về chuyến dã ngoại ngày mai.",
            dialogue = listOf(
                DialogueLine("A", "明日、雨が降ったら、どうしますか。", "Mai nếu trời mưa thì chúng ta tính sao đây?"),
                DialogueLine("B", "雨が降ったら、旅行に行きません。うちで休みましょう。", "Nếu mưa thì chúng ta sẽ không đi du lịch nữa. Hãy nghỉ ngơi ở nhà nhé.")
            ),
            question = "Hai người sẽ làm gì nếu ngày mai trời đổ mưa?",
            options = listOf("Vẫn đi dã ngoại bình thường", "Đi xem phim trong rạp", "Hủy chuyến đi và nghỉ ngơi ở nhà", "Đi mua sắm ở trung tâm thương mại"),
            correctIndex = 2,
            explanation = "Người B bàn: '雨が降ったら、旅行に行きません。うちで休みましょう' (Nếu mưa thì không đi du lịch, nghỉ ở nhà)."
        )
    )

    fun getExerciseForLesson(lessonId: Int): ListeningExercise? {
        return exercises.find { it.lessonId == lessonId }
    }
}
