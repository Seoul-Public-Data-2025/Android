package com.maumpeace.safeapp.util

object PhoneFormatter {

    /** 전화번호를 010-1234-5678 형식으로 포맷 */
    fun format(phone: String): String {
        val digits = phone
            .replace("\\s".toRegex(), "")         // 공백 제거
            .replace("-", "")                      // 기존 하이픈 제거
            .replace("+82", "0")                   // +82 → 0
            .filter { it.isDigit() }               // 숫자만 추출

        return when (digits.length) {
            10 -> digits.replaceFirst("(\\d{3})(\\d{3})(\\d{4})".toRegex(), "$1-$2-$3")
            11 -> digits.replaceFirst("(\\d{3})(\\d{4})(\\d{4})".toRegex(), "$1-$2-$3")
            else -> phone // fallback: 변환 불가 시 원본 반환
        }
    }

    /** 전화번호에서 하이픈 제거 (+82 → 0 도 포함) */
    fun unformat(phone: String): String {
        return phone
            .replace("\\s".toRegex(), "")
            .replace("-", "")
            .replace("+82", "0")
    }
}