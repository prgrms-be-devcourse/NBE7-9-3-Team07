package com.back.pinco.domain.likes.service;

import com.back.pinco.domain.likes.dto.PinLikedUserResponse;
import com.back.pinco.domain.likes.dto.PinLikesResponse;
import com.back.pinco.domain.likes.dto.PinsLikedByUserResponse;
import com.back.pinco.domain.likes.entity.Likes;
import com.back.pinco.domain.likes.repository.LikesRepository;
import com.back.pinco.domain.pin.entity.Pin;
import com.back.pinco.domain.pin.repository.PinRepository;
import com.back.pinco.domain.user.entity.User;
import com.back.pinco.domain.user.repository.UserRepository;
import com.back.pinco.global.exception.ErrorCode;
import com.back.pinco.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LikesService {

    private final LikesRepository likesRepository;
    private final PinRepository pinRepository;
    private final UserRepository userRepository;


    // 특정 핀에 대한 좋아요 수 조회
    @Transactional(readOnly = true)
    public int getLikesCount(Long pinId) {
        return (int) likesRepository.countByPinId(pinId);
    }


    // 좋아요 등록
    @Transactional
    public PinLikesResponse toggleLikeOn(Long pinId, Long userId) {
        User user = validateUser(userId);
        Pin pin = validatePin(pinId, userId);

        saveLike(pin, user);
        int likeCount = refreshPinLikeCount(pinId);

        return new PinLikesResponse(true, likeCount);
    }

    private Likes saveLike(Pin pin, User user) {
        try {
            return likesRepository.save(new Likes(pin, user));
        } catch (Exception e) {
            throw new ServiceException(ErrorCode.LIKES_CREATE_FAILED);
        }
    }


    // 좋아요 취소
    @Transactional
    public PinLikesResponse toggleLikeOff(Long pinId, Long userId) {
        User user = validateUser(userId);
        Pin pin = validatePin(pinId, userId);

        deleteLike(pin, user);
        int likeCount = refreshPinLikeCount(pinId);

        return new PinLikesResponse(false, likeCount);
    }

    private void deleteLike(Pin pin, User user) {
        Likes likes = likesRepository.findByPinIdAndUserId(pin.getId(), user.getId())
                .orElseThrow(() -> new ServiceException(ErrorCode.LIKES_NOT_FOUND));

        try {
            likesRepository.delete(likes);
        } catch (Exception e) {
            throw new ServiceException(ErrorCode.LIKES_REVOKE_FAILED);
        }
    }


    private Pin validatePin(Long pinId, Long userId) {
        Pin pin = pinRepository.findAccessiblePinById(pinId, userId);
        if(pin ==null ) throw new ServiceException(ErrorCode.LIKES_INVALID_PIN_INPUT);
        return pin;
    }

    private User validateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ServiceException(ErrorCode.LIKES_INVALID_USER_INPUT));
        return user;
    }


    @Transactional
    public int refreshPinLikeCount(Long pinId) {
        try {
            pinRepository.refreshLikeCount(pinId);
            return getLikesCount(pinId);
        } catch (Exception e) {
            throw new ServiceException(ErrorCode.LIKES_UPDATE_PIN_FAILED);
        }
    }


    // 해당 핀을 좋아요 누른 유저 ID 목록 전달
    public List<PinLikedUserResponse> getUsersWhoLikedPin(Long pinId) {
        if (!pinRepository.existsById(pinId)) {
            throw new ServiceException(ErrorCode.LIKES_INVALID_PIN_INPUT);
        }

        return likesRepository.findUsersByPinId(pinId)
                .stream()
                .map(PinLikedUserResponse::formEntry)
                .toList();
    }

    // 특정 사용자가 좋아요 누른 핀 목록 전달
    public List<PinsLikedByUserResponse> getPinsLikedByUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ServiceException(ErrorCode.LIKES_INVALID_USER_INPUT);
        }

        return likesRepository.findPinsByUserId(userId)
                .stream()
                .filter(pin -> pin.getUser().getId().equals(userId) || pin.isPublic())
                .map(PinsLikedByUserResponse::formEntry)
                .toList();
    }

    // 탈퇴한 사용자의 좋아요 삭제
    @Transactional
    public void deleteWithdrawnUserLikes(Long userId) {
        // 핀 조회 : 좋아요 갱신을 위해 -> 비 효율적?
        List<Pin> likedPinsList = likesRepository.findPinsByUserId(userId);

        if (likedPinsList.isEmpty()) return;

        try {
            likesRepository.deleteAllByUserId(userId);

            Long[] pinsId = likedPinsList.stream()
                    .map(Pin::getId)
                    .toArray(Long[]::new);

            pinRepository.refreshLikeCountBatch(pinsId);
        } catch (Exception e) {
            throw new ServiceException(ErrorCode.LIKES_UPDATE_PIN_FAILED);
        }
    }

}