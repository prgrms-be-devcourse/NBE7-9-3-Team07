package com.back.pinco.global.aspect

import com.back.pinco.global.rsData.RsData
import jakarta.servlet.http.HttpServletResponse
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.stereotype.Component

@Aspect
@Component
class ResponseAspect(
    private val response: HttpServletResponse
) {

    @Around(
        """
            (
                within
                (
                    @org.springframework.web.bind.annotation.RestController *
                )
                &&
                (
                    @annotation(org.springframework.web.bind.annotation.GetMapping)
                    ||
                    @annotation(org.springframework.web.bind.annotation.PostMapping)
                    ||
                    @annotation(org.springframework.web.bind.annotation.PutMapping)
                    ||
                    @annotation(org.springframework.web.bind.annotation.DeleteMapping)
                )
            )
            ||
            @annotation(org.springframework.web.bind.annotation.ResponseBody)
            
            """
    )
    @Throws(Throwable::class)
    fun responseAspect(joinPoint: ProceedingJoinPoint): Any? {
        val rst = joinPoint.proceed() // 실제 수행 메서드

        if (rst is RsData<*>) {
            response.status=rst.statusCode
        }
        return rst
    }
}