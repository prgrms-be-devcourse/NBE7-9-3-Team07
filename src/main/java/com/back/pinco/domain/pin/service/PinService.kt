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
import kotlin.collections.map

@Service
class PinService(
    private val pinRepository: PinRepository,
    private val PinRedisTemplate: RedisTemplate<String, PinCacheDto>,
    private val GeoRedisTemplate: RedisTemplate<String, Long>
) {

    private fun validateUser(actor: User?): User =
        actor ?: throw ServiceException(ErrorCode.PIN_NO_PERMISSION)

    private fun validateUserID(actor: User?): Long =
        actor?.id ?: throw ServiceException(ErrorCode.PIN_NO_PERMISSION)


    //-----------redis-----------
    private fun makePinCache(pin : Pin){
        val key = RedisKey.ID_PIN.key(pin.id.toString())
        val dto = PinCacheDto(pin)
        PinRedisTemplate.delete(key)
        PinRedisTemplate.opsForSet().add(key, dto)
    }

    private fun makeGeoCache(hash : String, pins : List<Pin>){
        if (! pins.isEmpty()) {
            val key =RedisKey.GEO_ID.key(hash)
            val pinIds = pins.map { it.id }
            GeoRedisTemplate.delete(key)
            GeoRedisTemplate.opsForList().rightPushAll(key, pinIds)
        }
    }
    private fun getPinCache(id : Long) : PinCacheDto? {
        val key = RedisKey.ID_PIN.key(id.toString())
        return PinRedisTemplate.opsForSet().members(key)?.firstOrNull()
    }

    private fun getGeoCache(hash: String): List<Long>{
        val key = RedisKey.GEO_ID.key(hash)
        val list: List<Long>? = GeoRedisTemplate.opsForList().range(key, 0, -1)

        return list ?: emptyList()
    }
    private fun deleteCache(pin : Pin){
        val hash = GeoHashUtil.getCoveringGeoHashe(pin.point.y, pin.point.x)

        val geoKey= RedisKey.GEO_ID.key(hash)

        val pinIds: List<Long> = GeoRedisTemplate.opsForList().range(geoKey, 0, -1) ?: emptyList()

        pinIds.forEach { id ->
            val pinKey = RedisKey.ID_PIN.key(id.toString())
            PinRedisTemplate.delete(pinKey)
        }

        // Geo cache 자체 삭제
        GeoRedisTemplate.delete(geoKey)
    }

    fun findPinsByRedis(
        latMin: Double, lngMin: Double, latMax: Double, lngMax: Double
    ): List<PinCacheDto> {
        val coveringHashes = GeoHashUtil.getCoveringGeoHashes(latMin, lngMin, latMax, lngMax)
        val resultSet = mutableSetOf<PinCacheDto>()

        coveringHashes.forEach { hash ->

            val pinIds = getGeoCache(hash)

            //영역이 캐시에 있음.
            if (pinIds.isNotEmpty()) {
                //캐시에서 id로 핀 조회
                pinIds.forEach { id ->
                    val pinCache = getPinCache(id)

                    if (pinCache != null) {
                        resultSet.add(pinCache)
                    } else {
                        val pin = pinRepository.findByIdOrNull(id)

                        if (pin != null) {
                            resultSet.add(PinCacheDto(pin))
                            makePinCache(pin)
                        }
                    }
                }
            } else {
                // 3) DB 조회 후 단일 캐시와 영역 캐시 생성
                val bbox = GeoHashUtil.boundingBoxOfGeoHash(hash)
                val dbPins = pinRepository.findPinsInBoundingBox(bbox[0], bbox[1], bbox[2], bbox[3])


                // 단일 캐시 생성
                dbPins.forEach {
                    resultSet.add(PinCacheDto(it))
                    makePinCache(it)
                }

                // 영역 캐시 생성 (ID 리스트만 저장)
                makeGeoCache(hash, dbPins)
            }
        }

        return resultSet.toList()
    }

    fun findPinByRedis(
       id: Long
    ): PinCacheDto{
        val pinCache = getPinCache(id)
        if(pinCache != null){
            return pinCache
        }
        val pin = pinRepository.finCachePinById(id)
        if(pin != null){
            val dto =  PinCacheDto(pin)
            makePinCache(pin)
            return dto
        }else throw ServiceException(ErrorCode.PIN_NOT_FOUND)

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

    fun findCachePinById(id: Long, actor: User?): PinCacheDto {
        val cache : PinCacheDto = findPinByRedis(id)
        if(cache.public || (actor != null && cache.userId == actor.id)) return cache

        throw ServiceException(ErrorCode.PIN_NOT_FOUND)
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
                makePinCache(pin)
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
