package com.back.pinco.domain.pin.service

import com.back.pinco.domain.pin.dto.PinRequest
import com.back.pinco.domain.pin.entity.Pin
import com.back.pinco.domain.pin.repository.PinRepository
import com.back.pinco.domain.user.entity.User
import com.back.pinco.global.exception.ErrorCode
import com.back.pinco.global.exception.ServiceException
import com.back.pinco.global.geometry.GeometryUtil.createPoint
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.util.function.Supplier

@Service
class PinService (private val pinRepository: PinRepository){


    fun count(): Long {
        return pinRepository.count()
    }

    fun write(actor: User?, pinReqbody: PinRequest): Pin {
        if(actor == null) throw ServiceException(ErrorCode.PIN_NO_PERMISSION)
        val point = createPoint(pinReqbody.longitude, pinReqbody.latitude)
        try {
            val pin = Pin(point, actor, pinReqbody.content)
            return pinRepository.save<Pin>(pin)
        } catch (_: Exception) {
            throw ServiceException(ErrorCode.PIN_CREATE_FAILED)
        }
    }

    fun findById(id: Long, actor: User?): Pin {
        return if (actor == null) {
             pinRepository.findPublicPinById(id)
                .orElseThrow(Supplier { ServiceException(ErrorCode.PIN_NOT_FOUND) })
        } else {
            pinRepository.findAccessiblePinById(id, actor.id)
                .orElseThrow(Supplier { ServiceException(ErrorCode.PIN_NOT_FOUND) })
        }
    }

    fun checkId(id: Long): Boolean = pinRepository.findById(id).isPresent


    fun findAll(actor: User?): List<Pin> {
        return if (actor == null) {
            pinRepository.findAllPublicPins()
        } else {
            pinRepository.findAllAccessiblePins(actor.id)
        }
    }

    fun findNearPins(latitude: Double, longitude: Double, radius: Double, actor: User?):List<Pin> {
        return if (actor == null) {
            pinRepository.findPublicPinsWithinRadius(latitude, longitude, radius)
        } else {
            pinRepository.findPinsWithinRadius(latitude, longitude, radius, actor.id)
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
            pinRepository.findScreenPins(latMax, lonMax, latMin, lonMin, actor.id)
        }
    }

    fun findByUserId(actor: User?, writer: User): List<Pin> {
        return if (actor == null) {
           pinRepository.findPublicByUser(writer.id)
        } else {
            pinRepository.findAccessibleByUser(writer.id, actor.id)
        }
    }

    fun findByUserIdDate(actor: User?, writer: User, year: Double, month: Double): List<Pin> {
        return if (actor == null) {
            pinRepository.findPublicByUserDate(writer.id, year.toInt(), month.toInt())
        } else {
            pinRepository.findAccessibleByUserDate(writer.id, actor.id, year.toInt(), month.toInt())
        }
    }

    @Transactional
    fun update(actor: User?, pinId: Long, updatePinContentRequest: PinRequest): Pin {
        val pin = pinRepository.findById(pinId)?: throw ServiceException(ErrorCode.PIN_NOT_FOUND)

        if (pin.user.id == actor?.id) {
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
        val pin = pinRepository.findById(pinId)
            .orElseThrow(ServiceException(ErrorCode.PIN_NOT_FOUND) )
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
        val pin = pinRepository.findById(pinId)
            .orElseThrow(ServiceException(ErrorCode.PIN_NOT_FOUND) )
        if (pin.user.id == actor?.id) {
            try {
                pin.setDeleted()
            } catch (_: Exception) {
                throw ServiceException(ErrorCode.PIN_DELETE_FAILED)
            }
        } else {
            throw ServiceException(ErrorCode.PIN_NO_PERMISSION)
        }

        pinRepository.save(pin)
    }

    @Transactional
    fun updateDeleteByUser(userId: Long): Int = pinRepository.updatePinsToDeletedByUserId(userId)

}
