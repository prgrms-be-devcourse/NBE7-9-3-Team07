package com.back.pinco.domain.pin.service

import ch.hsr.geohash.GeoHash
import com.back.pinco.domain.pin.dto.CreatePinRequest
import com.back.pinco.domain.pin.dto.PinCacheDto
import com.back.pinco.domain.pin.dto.PinDto
import com.back.pinco.domain.pin.dto.UpdatePinContentRequest
import com.back.pinco.domain.pin.entity.Pin
import com.back.pinco.domain.pin.repository.PinRepository
import com.back.pinco.domain.user.entity.User
import com.back.pinco.global.exception.ErrorCode
import com.back.pinco.global.exception.ServiceException
import com.back.pinco.global.geometry.GeoHashUtil
import com.back.pinco.global.geometry.GeometryUtil.createPoint
import jakarta.transaction.Transactional
import org.springframework.data.geo.Point
import org.springframework.data.redis.core.GeoOperations
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.io.Console
import java.util.concurrent.TimeUnit
import kotlin.collections.map

@Service
class PinService(
    private val pinRepository: PinRepository,
    private val redisTemplate: RedisTemplate<String, Any>,
    private val geoOps: GeoOperations<String, Any>
) {

    private val valueOps: ValueOperations<String, Any> by lazy { redisTemplate.opsForValue() }

    private val GEO_KEY = "pins"

    private fun validateUser(actor: User?): User =
        actor ?: throw ServiceException(ErrorCode.PIN_NO_PERMISSION)

    private fun validateUserID(actor: User?): Long =
        actor?.id ?: throw ServiceException(ErrorCode.PIN_NO_PERMISSION)

    fun boundingBox(lat: Double, lng: Double, radiusKm: Double): DoubleArray {
        val latRadius = radiusKm / 110.574
        val lngRadius = radiusKm / (111.320 * Math.cos(Math.toRadians(lat)))

        val latMin = lat - latRadius
        val latMax = lat + latRadius
        val lngMin = lng - lngRadius
        val lngMax = lng + lngRadius

        return doubleArrayOf(lngMin, latMin, lngMax, latMax)
    }



    fun count(): Long = pinRepository.count()


    fun write(actor: User?, pinReqbody: CreatePinRequest): Pin {
        val point = createPoint(pinReqbody.longitude, pinReqbody.latitude)
        val pin = Pin(point, validateUser(actor), pinReqbody.content)

        // 1) DB에 먼저 저장
        val savedPin = try {
            pinRepository.save(pin)
        } catch (ex: Exception) {
            throw ServiceException(ErrorCode.PIN_CREATE_FAILED)
        }

        // 2) Redis 저장
        try {
            valueOps.set("pin:${savedPin.id}", PinCacheDto(savedPin))
        } catch (ex: Exception) {
            println("Redis 캐시 저장 실패: ${ex.message}")
        }

        return savedPin
    }


    fun findPinsByRedis(
        latMin: Double, lngMin: Double, latMax: Double, lngMax: Double
    ): List<PinCacheDto> {
        val precision = 5 // 키로 사용되는 문자열 길이가 5
        //조회해야할 구역들
        val coveringHashes = GeoHashUtil.getCoveringGeoHashes(latMin, lngMin, latMax, lngMax, precision)

        val resultSet = mutableSetOf<PinCacheDto>()



        // 구역들을 redis 조회 -> 없으면 DB 조회
        coveringHashes.forEach { hash ->
            val cacheKey = GeoHashUtil.generateGeoCacheKey(hash)
            val cachedList = valueOps.get(cacheKey) as? List<PinCacheDto>

            if (cachedList!= null) {
                resultSet.addAll(cachedList)
                println("캐시에 있음! key=$cacheKey, size=${cachedList.size}")
            } else {
                println("캐시에 없음 key=$cacheKey")
                // 캐시에 없으면 해당 GeoHash 영역만 DB 조회
                val bbox = GeoHashUtil.boundingBoxOfGeoHash(hash)
                val dbPins = pinRepository.findPinsInBoundingBox(bbox[0],bbox[1],bbox[2],bbox[3])
                val dtos = dbPins.map { PinCacheDto(it) }
                dtos.forEach { dto ->
                    resultSet.add(dto)
                }
                valueOps.set(cacheKey, dtos, 10, TimeUnit.MINUTES)
            }
        }


        return resultSet.toList()
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
                valueOps.set("pin:$pinId", PinCacheDto(pin))
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
                valueOps.set("pin:$pinId", PinCacheDto(pin))
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

                valueOps.getOperations().delete("pin:$pinId")
                geoOps.remove(GEO_KEY, pinId.toString())
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
