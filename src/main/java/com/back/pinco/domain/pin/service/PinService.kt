package com.back.pinco.domain.pin.service

import com.back.pinco.domain.pin.dto.CreatePinRequest
import com.back.pinco.domain.pin.dto.UpdatePinContentRequest
import com.back.pinco.domain.pin.entity.Pin
import com.back.pinco.domain.pin.repository.PinRepository
import com.back.pinco.domain.user.entity.User
import com.back.pinco.global.exception.ErrorCode
import com.back.pinco.global.exception.ServiceException
import com.back.pinco.global.geometry.GeometryUtil.createPoint
import jakarta.transaction.Transactional
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class PinService(private val pinRepository: PinRepository) {

    private fun validateUser(actor: User?): User =
        actor ?: throw ServiceException(ErrorCode.PIN_NO_PERMISSION)

    private fun validateUserID(actor: User?): Long =
        actor?.id ?: throw ServiceException(ErrorCode.PIN_NO_PERMISSION)


    fun count(): Long = pinRepository.count()


    fun write(actor: User?, pinReqbody: CreatePinRequest): Pin {
        val point = createPoint(pinReqbody.longitude, pinReqbody.latitude)
        try {
            val pin = Pin(point, validateUser(actor), pinReqbody.content)
            return pinRepository.save<Pin>(pin)
        } catch (_: Exception) {
            throw ServiceException(ErrorCode.PIN_CREATE_FAILED)
        }
    }

    fun findById(id: Long, actor: User?): Pin {

        return if (actor == null) {
            pinRepository.findPublicPinById(id) ?: throw ServiceException(ErrorCode.PIN_NOT_FOUND)
        } else {
            pinRepository.findAccessiblePinById(id, validateUserID(actor))
                ?: throw ServiceException(ErrorCode.PIN_NOT_FOUND)
        }
    }

    fun checkId(id: Long): Boolean = pinRepository.findById(id).isPresent


    fun findAll(actor: User?): List<Pin> {
        return if (actor == null) {
            pinRepository.findAllPublicPins()
        } else {
            pinRepository.findAllAccessiblePins(validateUserID(actor))
        }
    }

    fun findNearPins(latitude: Double, longitude: Double, radius: Double, actor: User?): List<Pin> {
        return if (actor == null) {
            pinRepository.findPublicPinsWithinRadius(latitude, longitude, radius)
        } else {
            pinRepository.findPinsWithinRadius(latitude, longitude, radius, validateUserID(actor))
        }
    }

    fun findScreenPins(
        latMax: Double,
        lonMax: Double,
        latMin: Double,
        lonMin: Double,
        actor: User?
    ): List<Pin> {

        return if (actor == null) {
            pinRepository.findPublicScreenPins(latMax, lonMax, latMin, lonMin)
        } else {
            pinRepository.findScreenPins(latMax, lonMax, latMin, lonMin, validateUserID(actor))
        }
    }

    fun findByUserId(actor: User?, writer: User): List<Pin> {
        return if (actor == null) {
            pinRepository.findPublicByUser(validateUserID(writer))
        } else {
            pinRepository.findAccessibleByUser(validateUserID(writer), validateUserID(actor))
        }
    }

    fun findByUserIdDate(actor: User?, writer: User, year: Double, month: Double): List<Pin> {
        return if (actor == null) {
            pinRepository.findPublicByUserDate(validateUserID(writer), year.toInt(), month.toInt())
        } else {
            pinRepository.findAccessibleByUserDate(
                validateUserID(writer),
                validateUserID(actor),
                year.toInt(),
                month.toInt()
            )
        }
    }

    @Transactional
    fun update(actor: User?, pinId: Long, updatePinContentRequest: UpdatePinContentRequest): Pin {
        val pin = pinRepository.findByIdOrNull(pinId)?: throw ServiceException(ErrorCode.PIN_NOT_FOUND)

        if (validateUserID(pin.user) == validateUserID(actor)) {
            try {
                pin.update(updatePinContentRequest)
            } catch (_: Exception) {
                throw ServiceException(ErrorCode.PIN_UPDATE_FAILED)
            }
        } else {
            throw ServiceException(ErrorCode.PIN_NO_PERMISSION)
        }


        return pin
    }

    @Transactional
    fun changePublic(actor: User?, pinId: Long): Pin {
        val pin = pinRepository.findByIdOrNull(pinId)?: throw ServiceException(ErrorCode.PIN_NOT_FOUND)
        if (pin.user.id == actor?.id) {
            try {
                pin.togglePublic()
            } catch (_: Exception) {
                throw ServiceException(ErrorCode.PIN_UPDATE_FAILED)
            }
        } else {
            throw ServiceException(ErrorCode.PIN_NO_PERMISSION)
        }

        return pin
    }

    fun deleteById(pinId: Long, actor: User?) {
        val pin = pinRepository.findByIdOrNull(pinId) ?: throw ServiceException(ErrorCode.PIN_NOT_FOUND)

        if (validateUserID(pin.user) == validateUserID(actor)) {
            try {
                pin.setDeleted()
            } catch (_: Exception) {
                throw ServiceException(ErrorCode.PIN_DELETE_FAILED)
            }

            pinRepository.save(pin)
        } else {
            throw ServiceException(ErrorCode.PIN_NO_PERMISSION)
        }


    }

    @Transactional
    fun updateDeleteByUser(userId: Long): Int = pinRepository.updatePinsToDeletedByUserId(userId)

}
