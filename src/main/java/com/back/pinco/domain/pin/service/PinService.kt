package com.back.pinco.domain.pin.service

import com.back.pinco.domain.pin.dto.CreatePinRequest
import com.back.pinco.domain.pin.dto.PinCacheDto
import com.back.pinco.domain.pin.dto.UpdatePinContentRequest
import com.back.pinco.domain.pin.entity.Pin
import com.back.pinco.domain.pin.repository.PinRepository
import com.back.pinco.domain.user.entity.User
import com.back.pinco.global.exception.ErrorCode
import com.back.pinco.global.exception.ServiceException
import com.back.pinco.global.geometry.GeoHashUtil
import com.back.pinco.global.geometry.GeometryUtil.createPoint
import com.back.pinco.global.redisConfig.RedisKey
import jakarta.transaction.Transactional
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.Duration
import kotlin.collections.map

@Service
class PinService(
    private val pinRepository: PinRepository,
    private val GeoRedisTemplate: RedisTemplate<String, PinCacheDto>
) {

    private fun validateUser(actor: User?): User =
        actor ?: throw ServiceException(ErrorCode.PIN_NO_PERMISSION)

    private fun validateUserID(actor: User?): Long =
        actor?.id ?: throw ServiceException(ErrorCode.PIN_NO_PERMISSION)


    //-----------redis-----------


    private fun makeGeoCache(hash : String, pins : List<PinCacheDto>){
        if (! pins.isEmpty()) {
            val key =RedisKey.GEO_ID.key(hash)
            GeoRedisTemplate.delete(key)
            GeoRedisTemplate.opsForList().rightPushAll(key, pins)
            GeoRedisTemplate.expire(key, Duration.ofMinutes(1))
        }
    }

    private fun getGeoCache(hash: String): List<PinCacheDto>{
        val key = RedisKey.GEO_ID.key(hash)
        val list: List<PinCacheDto>? = GeoRedisTemplate.opsForList().range(key, 0, -1)

        return list ?: emptyList()
    }
    private fun deleteCache(pin : Pin){
        val hash = GeoHashUtil.getCoveringGeoHashe(pin.point.y, pin.point.x)

        val geoKey= RedisKey.GEO_ID.key(hash)

        // Geo cache 자체 삭제
        GeoRedisTemplate.delete(geoKey)
    }

    fun findPinsByRedis(
        latMin: Double, lngMin: Double, latMax: Double, lngMax: Double
    ): List<PinCacheDto> {
        val coveringHashes = GeoHashUtil.getCoveringGeoHashes(latMin, lngMin, latMax, lngMax)
        val resultSet = mutableSetOf<PinCacheDto>()

        coveringHashes.forEach { hash ->

            val pinDtos = getGeoCache(hash)

            //영역이 캐시에 있음.
            if (pinDtos.isNotEmpty()) {
                pinDtos.forEach { resultSet.add(it) }
            } else {
                // DB 조회 후 단일 캐시와 영역 캐시 생성
                val bbox = GeoHashUtil.boundingBoxOfGeoHash(hash)
                val dbPins = pinRepository.findPinsInBoundingBox(bbox[0], bbox[1], bbox[2], bbox[3])
                    .map { PinCacheDto(it) }

                makeGeoCache(hash, dbPins)


                dbPins.forEach {
                    resultSet.add(it)
                }

            }
        }

        return resultSet.toList()
    }



    //-----------서비스 함수-----------

    fun count(): Long = pinRepository.count()


    fun write(actor: User?, pinReqbody: CreatePinRequest): Pin {
        val point = createPoint(pinReqbody.longitude, pinReqbody.latitude)
        val pin = Pin(point, validateUser(actor), pinReqbody.content)

        val savedPin = try {
            pinRepository.save(pin)
        } catch (ex: Exception) {
            throw ServiceException(ErrorCode.PIN_CREATE_FAILED)
        }

        // Redis에서 해당 구역의 캐시를 삭제하여 다음 조회 때 가져오게 함
        deleteCache(pin)

        return savedPin
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
    ): List<PinCacheDto> {
        val result = findPinsByRedis(latMin, lonMin, latMax, lonMax)
            .filter { dto ->
                dto.latitude < latMax && dto.latitude > latMin && dto.longitude < lonMax && dto.longitude > lonMin }
            .sortedBy { it.id }


        return if (actor == null) {
            result.filter { it.public }
        } else {
            result.filter { it.public || it.userId==actor.id }
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

                deleteCache(pin)

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
                deleteCache(pin)
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
                pinRepository.save(pin)

                deleteCache(pin)

            } catch (_: Exception) {
                throw ServiceException(ErrorCode.PIN_DELETE_FAILED)
            }

        } else {
            throw ServiceException(ErrorCode.PIN_NO_PERMISSION)
        }


    }

    @Transactional
    fun updateDeleteByUser(userId: Long): Int = pinRepository.updatePinsToDeletedByUserId(userId)

}
