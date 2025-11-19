package com.back.pinco.domain.user.service

import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.SimpleMailMessage
import org.springframework.stereotype.Service
import kotlin.random.Random

@Service
class MailService(
    private val mailSender: JavaMailSender
) {
    fun generateVerificationCode(): String {
        return (100000..999999).random().toString()
    }

    fun sendVerificationEmail(to: String, verificationCode: String) {
        val subject = "PinCo 회원 가입 인증 코드"
        val text = "인증 코드는 ${verificationCode}입니다."
        
        sendEmail(to, subject, text)
    }

    fun sendEmail(
        to: String,
        subject: String,
        text: String
    ) {
        val message = SimpleMailMessage().apply {
            setTo(to)
            this.subject = subject
            this.text = text
        }

        mailSender.send(message)
    }
}