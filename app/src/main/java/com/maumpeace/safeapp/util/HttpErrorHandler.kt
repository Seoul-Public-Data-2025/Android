package com.maumpeace.safeapp.util

import org.json.JSONObject
import retrofit2.HttpException

/**
 * ✅ 서버 응답 에러(HttpException)에서 사용자 메시지를 추출하는 유틸 함수
 */
object HttpErrorHandler {

    fun parseErrorMessage(e: HttpException): String {
        return try {
            val errorBody = e.response()?.errorBody()?.string()
            if (!errorBody.isNullOrBlank()) {
                val json = JSONObject(errorBody)
                json.optString("message", "서버 오류가 발생했습니다.")
            } else {
                "서버 오류가 발생했습니다."
            }
        } catch (e: Exception) {
            "서버 응답 파싱 실패"
        }
    }
}