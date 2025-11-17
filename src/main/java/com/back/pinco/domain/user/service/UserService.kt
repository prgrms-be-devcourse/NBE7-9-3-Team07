package com.back.pinco.domain.user.service

import com.back.pinco.domain.bookmark.dto.BookmarkDto
import com.back.pinco.domain.bookmark.service.BookmarkService
import com.back.pinco.domain.likes.service.LikesService
import com.back.pinco.domain.pin.dto.PinDto
import com.back.pinco.domain.pin.entity.Pin
import com.back.pinco.domain.pin.service.PinService
import com.back.pinco.domain.user.dto.UserResBody.MyPinResponse
import com.back.pinco.domain.user.entity.User
import com.back.pinco.domain.user.repository.UserRepository
import com.back.pinco.global.exception.ErrorCode
import com.back.pinco.global.exception.ServiceException
import com.back.pinco.global.rq.Rq
import com.back.pinco.global.security.JwtTokenProvider
import lombok.RequiredArgsConstructor
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@RequiredArgsConstructor
class UserService(
    private val passwordEncoder: PasswordEncoder,
    private val userRepository: UserRepository,
    private val tokenProvider: JwtTokenProvider,
    private val bookmarkService: BookmarkService,
    private val likesService: LikesService,
    private val pinService: PinService,
    private val rq: Rq
) {


    @Transactional
    fun ensureApiKey(user: User): String {
        if (user.apiKey.isNullOrBlank()) {
            var key = UUID.randomUUID().toString()

            // 혹시 중복이면 새로 생성
            while (userRepository.existsByApiKey(key)) {
                key = UUID.randomUUID().toString()
            }

            user.apiKey = key
        }
        return user.apiKey!!
    }



    @Transactional
    fun createUser(email: String, password: String, userName: String): User {
        if (email.isBlank() || !email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$".toRegex())
        ) {
            throw ServiceException(ErrorCode.INVALID_EMAIL_FORMAT)
        }
        if (password.isBlank() || password.length < 8) {
            throw ServiceException(ErrorCode.INVALID_PASSWORD_FORMAT)
        }
        if (userName.isBlank() || userName.length < 2 || userName.length > 20) {
            throw ServiceException(ErrorCode.INVALID_USERNAME_FORMAT)
        }
        if (userRepository.existsByEmail(email)) {
            throw ServiceException(ErrorCode.EMAIL_ALREADY_EXISTS)
        }
        if (userRepository.existsByUserName(userName)) {
            throw ServiceException(ErrorCode.NICKNAME_ALREADY_EXISTS)
        }
        val hashedPwd = passwordEncoder.encode(password)
        val user = User(email, hashedPwd, userName)
        userRepository.save(user)
        ensureApiKey(user)
        return user
    }

    @Transactional(readOnly = true)
    fun login(email: String, rawPwd: String) {
        val user: User = userRepository.findByEmail(email) ?: throw ServiceException(ErrorCode.USER_NOT_FOUND)

        if (!passwordEncoder.matches(rawPwd, user.password)) {
            throw ServiceException(ErrorCode.PASSWORD_NOT_MATCH)
        }
    }

    @Transactional
    fun editName(user: User, newUserName: String) {
        if (newUserName.isBlank() || newUserName.length < 2 || newUserName.length > 20) {
            throw ServiceException(ErrorCode.INVALID_USERNAME_FORMAT)
        }
        if (newUserName == user.userName) {
            return  // 변경 없음
        }
        // 자신 제외 중복 체크
        if (userRepository.existsByUserNameAndIdNot(newUserName, user.id!!)) {
            throw ServiceException(ErrorCode.NICKNAME_ALREADY_EXISTS)
        }
        user.userName = newUserName
    }

    // 회원 정보 패스워드 수정
    @Transactional
    fun editPwd(user: User, newPassword: String) {
        if (newPassword.isBlank() || newPassword.length < 8) {
            throw ServiceException(ErrorCode.INVALID_PASSWORD_FORMAT)
        }
        if (passwordEncoder.matches(newPassword, user.password)) {
            return  // 변경 없음
        }
        val hashedPwd = passwordEncoder.encode(newPassword)
        user.password = hashedPwd
    }

    // 회원 정보 모두 수정
    @Transactional
    fun editAll(user: User, newUserName: String, newPassword: String) {
        if (newUserName.isBlank() || newUserName.length < 2 || newUserName.length > 20) {
            throw ServiceException(ErrorCode.INVALID_USERNAME_FORMAT)
        }
        if (userRepository.existsByUserNameAndIdNot(newUserName, user.id!!)) {
            throw ServiceException(ErrorCode.NICKNAME_ALREADY_EXISTS)
        }
        if (newPassword.isBlank() || newPassword.length < 8) {
            throw ServiceException(ErrorCode.INVALID_PASSWORD_FORMAT)
        }
        if (passwordEncoder.matches(newPassword, user.password) && newUserName == user.userName) {
            return  // 변경 없음
        }
        user.userName = newUserName
        user.password = passwordEncoder.encode(newPassword)
    }

    // 회원 정보 삭제
    @Transactional
    fun delete(user: User) {
        val managed = userRepository.findById(user.id!!)
            .orElseThrow { ServiceException(ErrorCode.USER_NOT_FOUND) }
        managed.isDeleted = true
        pinService.updateDeleteByUser(managed.id!!)
        likesService.deleteWithdrawnUserLikes(managed.id!!)
    }


    // 비밀번호 확인
    @Transactional
    fun checkPwd(user: User, pwd: String) {
        if (!passwordEncoder.matches(pwd, user.password)) {
            throw ServiceException(ErrorCode.PASSWORD_NOT_MATCH)
        }
    }

    // 이메일로 사용자 찾기
    @Transactional(readOnly = true)
    fun findByEmail(email: String): User =
        userRepository.findByEmail(email) ?: throw ServiceException(ErrorCode.USER_NOT_FOUND)


    @Transactional(readOnly = true)
    fun findById(id: Long): User = userRepository.findById(id)
            .orElseThrow { ServiceException(ErrorCode.USER_NOT_FOUND) }

    private fun nameChanged(currentUser: User, newUserName: String): Boolean =
        newUserName.trim().isNotBlank() && newUserName.trim() != currentUser.userName

    private fun passwordChanged(currentUser: User, newPassword: String): Boolean =
        newPassword.isNotBlank() && !passwordEncoder.matches(newPassword, currentUser.password)


    @Transactional
    fun editUserInfo(userId: Long, newUserName: String, newPassword: String) {
        val currentUser = userRepository.findById(userId)
            .orElseThrow { ServiceException(ErrorCode.USER_NOT_FOUND) }
        val nameChanged = nameChanged(currentUser, newUserName)
        val pwdChanged = passwordChanged(currentUser, newPassword)
        if (nameChanged && pwdChanged) {
            editAll(currentUser, newUserName, newPassword)
        } else if (nameChanged) {
            editName(currentUser, newUserName)
        } else if (pwdChanged) {
            editPwd(currentUser, newPassword)
        } else {
            throw ServiceException(ErrorCode.NO_FIELDS_TO_UPDATE)
        }
    }

    @Transactional(readOnly = true)
    fun findByApiKey(apiKey: String): User =
        userRepository.findByApiKey(apiKey) ?: throw ServiceException(ErrorCode.USER_INFO_NOT_FOUND)


    // accessToken 생성
    fun genAccessToken(user: User): String =
        tokenProvider.generateAccessToken(user.id, user.email, user.userName)


    @Transactional(readOnly = true)
    fun findByIdOptional(id: Long): Optional<User> =
        userRepository.findById(id)


    @Transactional(readOnly = true)
    fun getMyPins() : List<Pin> {
        val user = rq.getActorOrNull()
        return pinService.findByUserId(user, user)
    }

    @Transactional(readOnly = true)
    fun getMyBookmarks() : List<BookmarkDto> {
        val user = rq.getActorOrNull()
        return bookmarkService.getMyBookmarks(user.id!!)
    }

    @Transactional
    fun likesCount(listPin: List<Pin>): Int {
        val totalLikesReceived = listPin.stream()
            .mapToInt { pin: Pin -> likesService.getLikesCount(pin.id!!) }
            .sum()
        return totalLikesReceived
    }

    @Transactional(readOnly = true)
    fun listPublicAndPrivate(): MyPinResponse {
        val user = rq.getActorOrNull()

        // DB 한 번
        val accessible = pinService.findByUserId(user, user)

        // 공개: isPublic == true
        val publicDtos = accessible.stream()
            .filter { p: Pin -> java.lang.Boolean.TRUE == p.isPublic }
            .map { pin: Pin? -> PinDto(pin!!) }
            .toList()

        // 비공개: isPublic != true && "내 것"만
        val privateDtos = accessible.stream()
            .filter { p: Pin -> java.lang.Boolean.TRUE != p.isPublic }
            .filter { p: Pin -> p.user != null && p.user.id == user.id }
            .map { pin: Pin? -> PinDto(pin!!) }
            .toList()

        return MyPinResponse(publicDtos, privateDtos)
    }

    @Transactional(readOnly = true)
    fun bookmarkList(): List<PinDto> {
        val bookmarkList = getMyBookmarks().stream()
            .map(BookmarkDto::pin) // BookmarkDto::getPin 으로 수정 - 이현 님 커밋
            .toList()
        return bookmarkList
    }
}
