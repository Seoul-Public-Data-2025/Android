# 마음편

안전한 일상을 위한 Android 애플리케이션입니다.  
위험 알림, 위치 기반 신고, 실시간 안전 정보를 제공합니다.  
MVVM 아키텍처와 Hilt, Retrofit 기반으로 구성되어 있습니다.

![Web_App_Reference_Architecture_re3_nogrid](https://github.com/user-attachments/assets/612f4407-ae5f-4fbd-b4dc-329b9afc7b83)

---

## 🧩 주요 기능

- 위치 기반 위험 알림 제공
- 긴급 상황 사용자 신고 기능
- 지도 기반 위험 지역 표시

---

## 🛠️ 기술 스택

- Kotlin
- MVVM 아키텍처
- Hilt (DI)
- Retrofit (네트워크 통신)
- Jetpack (ViewModel, LiveData, Navigation)
- Google Maps SDK

---

## 📁 프로젝트 구조

마음편은 Android MVVM 아키텍처 패턴을 기반으로 모듈화되어 있으며, 각 계층은 다음과 같은 역할을 수행합니다:
- 📁 model - API 응답 및 로컬 데이터 클래스 정의
- 📁 network - Retrofit 인터페이스, Hilt 네트워크 모듈 구성
- 📁 repository - 데이터 소스(Retrofit, DB 등) 추상화 및 비즈니스 로직 처리
- 📁 viewModel - UI 상태 관리 및 로직 처리 (LiveData, StateFlow 활용)
- 📁 ui - 화면 구성 요소 (Activity, Fragment, Adapter 등)
- 📁 util - 공통 유틸 클래스 및 확장 함수

---

🙌 기여
기여는 언제나 환영입니다!
1. 이슈를 생성해 주세요.
2. Fork 후 새로운 브랜치에서 작업합니다.
3. Pull Request를 생성합니다.

---

💬 **사용 팁**  
- `MAPS_API_KEY` 외에도 환경 변수나 필요 설정이 있다면 `시작하기`에 명확히 작성하세요.  
- 기여 가이드는 팀에서 사용하는 PR 템플릿이 있다면 링크로 첨부하는 것도 좋습니다.

원하시면 "초기 템플릿 + 실제 SafeApp 내용 반영한 버전"도 제작해드릴게요. README를 바로 배포 가능한 상태로 맞춰드릴까요?

