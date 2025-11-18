package com.back.pinco.global.initData

import com.back.pinco.domain.bookmark.service.BookmarkService
import com.back.pinco.domain.likes.service.LikesService
import com.back.pinco.domain.pin.dto.CreatePinRequest
import com.back.pinco.domain.pin.service.PinService
import com.back.pinco.domain.tag.service.PinTagService
import com.back.pinco.domain.tag.service.TagService
import com.back.pinco.domain.user.service.UserService
import jakarta.transaction.Transactional
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class InitData(
    private val pinService: PinService,
    private val userService: UserService,
    private val bookmarkService: BookmarkService,
    private val likesService: LikesService,
    private val tagService: TagService,
    private val pinTagService: PinTagService,
) {

    @Bean
    fun baseInitData(): ApplicationRunner {
        return ApplicationRunner { _: ApplicationArguments? ->
            work()
        }
    }

    @Transactional
    fun work() {
//        if (pinService.count() > 0) return;
        /**
         * application.yml의 ddl-auto: create 설정으로
         * 애플리케이션 시작 시 초기 데이터로 재생성 -> 시퀀스도 초기화
         */

        val baseLat = 37.5665 // 서울시청 기준 위도
        val baseLng = 126.9780 // 서울시청 기준 경도

        // 유저 생성
        val user1 = userService.createUser("user1@example.com", "12345678", "유저1")
        val user2 = userService.createUser("user2@example.com", "12341234", "유저2")
        val user3 = userService.createUser("no@example.com", "12345678", "노미경")

        // 반복되는 id!! 호출을 줄이기 위해 유저 ID를 미리 추출
        val u1Id = user1.id!!
        val u2Id = user2.id!!
        val u3Id = user3.id!!


        // 핀 생성 (서울 시청 기준 반경 1km 이내 임의 좌표)
        val pinA = pinService.write(user1, CreatePinRequest(baseLat + 0.0012, baseLng + 0.0015, "서울 시청 근처 카페 ☕"))
        val pinB = pinService.write(user1, CreatePinRequest(baseLat - 0.0008, baseLng + 0.0010, "덕수궁 돌담길 산책 중 🌳"))
        val pinC = pinService.write(user1, CreatePinRequest(baseLat + 0.0006, baseLng - 0.0013, "청계천 산책로 발견 👣"))
        val pinD = pinService.write(user2, CreatePinRequest(baseLat - 0.0005, baseLng - 0.0010, "광화문에서 커피 한 잔 ☕"))
        val pinE = pinService.write(user2, CreatePinRequest(baseLat + 0.0003, baseLng + 0.0002, "서울시청 옆 공원 벤치 휴식 🍃"))

        val pin6 = pinService.write(user3, CreatePinRequest(37.56652851254232, 126.99170316409894, "김밥 맛집"))
        val pin7 = pinService.write(user3, CreatePinRequest(37.56552838426607, 127.00861365307968, "환승 지옥"))
        val pin8 = pinService.write(user3, CreatePinRequest(37.548877786806514, 126.98935132168111, "1차 방문"))
        val pin9 = pinService.write(user3, CreatePinRequest(37.548778723722855, 126.9898832031673, "2차 방문"))
        val pin10 = pinService.write(user3, CreatePinRequest(37.54874263496653, 126.9893287082637, "3차 방문"))
        val pin11 = pinService.write(user3, CreatePinRequest(37.57759977323471, 126.97689730646299, "경복궁"))

        // 핀 ID 추출
        val pAId = pinA.id!!
        val pBId = pinB.id!!
        val pCId = pinC.id!!
        val pDId = pinD.id!!
        val pEId = pinE.id!!
        val p6Id = pin6.id!!
        val p7Id = pin7.id!!
        val p8Id = pin8.id!!
        val p9Id = pin9.id!!
        val p10Id = pin10.id!!
        val p11Id = pin11.id!!


        // 샘플 북마크 생성
        // user1: pinA, pinD, pin6, pin7, pin9 북마크
        bookmarkService.addBookmark(u1Id, pAId)
        bookmarkService.addBookmark(u1Id, pDId)
        bookmarkService.addBookmark(u1Id, p6Id)
        bookmarkService.addBookmark(u1Id, p7Id)
        bookmarkService.addBookmark(u1Id, p9Id)

        // user2: pinB, pin7, pin8, pin9 북마크
        bookmarkService.addBookmark(u2Id, pBId)
        bookmarkService.addBookmark(u2Id, p7Id)
        bookmarkService.addBookmark(u2Id, p8Id)
        bookmarkService.addBookmark(u2Id, p9Id)


        // 좋아요 등록
        // A(2), B(2), C(1), D(0), E(1)
        likesService.toggleLikeOn(pAId, u1Id)
        likesService.toggleLikeOn(pAId, u2Id)

        likesService.toggleLikeOn(pBId, u1Id)
        likesService.toggleLikeOn(pBId, u2Id)

        likesService.toggleLikeOn(pCId, u1Id)
        likesService.toggleLikeOn(pCId, u2Id)
        likesService.toggleLikeOff(pCId, u1Id) // 취소

        likesService.toggleLikeOn(pDId, u1Id)
        likesService.toggleLikeOff(pDId, u1Id) // 취소

        likesService.toggleLikeOn(pEId, u1Id)

        likesService.toggleLikeOn(p6Id, u1Id)
        likesService.toggleLikeOn(p6Id, u2Id)

        likesService.toggleLikeOn(p7Id, u1Id)
        likesService.toggleLikeOn(p8Id, u1Id)
        likesService.toggleLikeOn(p9Id, u1Id)
        likesService.toggleLikeOn(p10Id, u1Id)


        // 샘플 태그 등록
        val t1 = tagService.createTag("카페")
        val t2 = tagService.createTag("감성")
        val t3 = tagService.createTag("반려동물")
        val t4 = tagService.createTag("데이트")
        val t5 = tagService.createTag("야경")
        val t6 = tagService.createTag("산책로")
        val t7 = tagService.createTag("전망좋은")
        val t8 = tagService.createTag("최애식당")
        val t9 = tagService.createTag("지하철")

        // 샘플 핀-태그 연결 (PinTag)
        pinTagService.createPinTag(pinA, t1)
        pinTagService.createPinTag(pinA, t2)
        pinTagService.createPinTag(pinA, t4)
        pinTagService.createPinTag(pinB, t2)
        pinTagService.createPinTag(pinB, t3)
        pinTagService.createPinTag(pinC, t5)
        pinTagService.createPinTag(pinC, t6)
        pinTagService.createPinTag(pinD, t4)
        pinTagService.createPinTag(pinD, t5)
        pinTagService.createPinTag(pinD, t7)
        pinTagService.createPinTag(pinE, t2)
        pinTagService.createPinTag(pinE, t1)

        pinTagService.createPinTag(pin6, t8)
        pinTagService.createPinTag(pin7, t9)
        pinTagService.createPinTag(pin8, t5)
        pinTagService.createPinTag(pin9, t5)
        pinTagService.createPinTag(pin10, t5)
        pinTagService.createPinTag(pin11, t7)
        pinTagService.createPinTag(pin11, t6)

        // user1의 하루 일상 트래킹 핀 생성 및 태그 등록
        val morning1 = pinService.write(user1, CreatePinRequest(37.497942, 127.027621, "☀️ 출근 시작 - 오늘도 화이팅!"))
        val morning2 = pinService.write(user1, CreatePinRequest(37.566826, 126.978388, "🚇 광화문역 환승 - 사람 진짜 많다"))
        val morning3 = pinService.write(user1, CreatePinRequest(37.570196, 126.976849, "🏢 회사 도착 - 커피부터"))
        val morning4 = pinService.write(user1, CreatePinRequest(37.570180, 126.976920, "💼 오전 회의 중 - 프로젝트 진행 상황 공유"))
        val lunch = pinService.write(user1, CreatePinRequest(37.569500, 126.977500, "🍜 점심은 칼국수 맛집 - 존맛탱"))
        val afternoon1 = pinService.write(user1, CreatePinRequest(37.571234, 126.975678, "☕ 카페에서 작업 중 - 집중 모드"))
        val afternoon2 = pinService.write(user1, CreatePinRequest(37.570500, 126.976234, "🍰 디저트 카페 발견 - 케이크가 예술"))
        val evening1 = pinService.write(user1, CreatePinRequest(37.570196, 126.976849, "🌆 퇴근 완료 - 오늘 하루도 수고했어"))
        val evening2 = pinService.write(user1, CreatePinRequest(37.580450, 126.977041, "🍺 친구들과 저녁 - 삼겹살 파티"))
        val evening3 = pinService.write(user1, CreatePinRequest(37.579617, 126.976950, "🌙 청계천 야경 산책 - 분위기 좋다"))
        val night = pinService.write(user1, CreatePinRequest(37.497942, 127.027621, "🏠 집 도착 - 오늘 하루 완료!"))

        val t10 = tagService.createTag("출근")
        val t11 = tagService.createTag("회사")
        val t12 = tagService.createTag("점심")
        val t13 = tagService.createTag("퇴근")
        val t14 = tagService.createTag("저녁약속")
        val t15 = tagService.createTag("야경산책")
        val t16 = tagService.createTag("일상")

        pinTagService.createPinTag(morning1, t10)
        pinTagService.createPinTag(morning1, t16)
        pinTagService.createPinTag(morning2, t10)
        pinTagService.createPinTag(morning2, t9)
        pinTagService.createPinTag(morning3, t11)
        pinTagService.createPinTag(morning3, t1)
        pinTagService.createPinTag(morning4, t11)
        pinTagService.createPinTag(morning4, t16)
        pinTagService.createPinTag(lunch, t12)
        pinTagService.createPinTag(lunch, t8)
        pinTagService.createPinTag(afternoon1, t1)
        pinTagService.createPinTag(afternoon1, t2)
        pinTagService.createPinTag(afternoon2, t1)
        pinTagService.createPinTag(afternoon2, t8)
        pinTagService.createPinTag(evening1, t13)
        pinTagService.createPinTag(evening1, t16)
        pinTagService.createPinTag(evening2, t14)
        pinTagService.createPinTag(evening2, t8)
        pinTagService.createPinTag(evening3, t15)
        pinTagService.createPinTag(evening3, t5)
        pinTagService.createPinTag(evening3, t6)
        pinTagService.createPinTag(night, t16)

        // 추가 좋아요 및 북마크 등록
        likesService.toggleLikeOn(morning1.id!!, u2Id)
        likesService.toggleLikeOn(lunch.id!!, u2Id)
        likesService.toggleLikeOn(lunch.id!!, u3Id)
        likesService.toggleLikeOn(afternoon2.id!!, u3Id)
        likesService.toggleLikeOn(evening2.id!!, u2Id)
        likesService.toggleLikeOn(evening3.id!!, u2Id)
        likesService.toggleLikeOn(evening3.id!!, u3Id)

        bookmarkService.addBookmark(u2Id, lunch.id!!) // 칼국수 맛집
        bookmarkService.addBookmark(u3Id, afternoon1.id!!) // 작업하기 좋은 카페
        bookmarkService.addBookmark(u2Id, afternoon2.id!!) // 디저트 카페
        bookmarkService.addBookmark(u3Id, evening2.id!!) // 삼겹살집
    }
}
