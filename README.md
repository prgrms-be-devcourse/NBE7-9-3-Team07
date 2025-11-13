# Pinco
> 나만의 스팟을 pick! 하는 **Pinco**

## 💡서비스 소개

**Pinco**는 내가 기록하고 싶은 위치를 등록하고,

등록한 위치에 메모를 남기고,

다른 사람과 공유할 수 있습니다.

---

## **🎯 서비스 목표**

1. **위치 등록 및 공유**
    - 장소에만 국한되지않고 위치 좌표를 통해 **어디든지** 나만의 위치 등록 가능
    - 등록한 위치에 메모를 남기고, 다른 사람들과 공유 가능
      
2. **기록 관리**
    - 마이페이지에서 내가 등록한 장소, 북마크한 장소 확인 가능
    - 내가 받은 좋아요 수 확인 가능
---

## 🧑‍💻 개발 기간 & 팀원

### **개발 기간**
> 2025.10.10 (금) 09:00 ~ 2025.10.27 (월) 18:00

### **팀원**
| <a href="https://github.com/ys0221"><img src="https://github.com/ys0221.png" width="100"/></a> | <a href="https://github.com/kheeyoung"><img src="https://github.com/kheeyoung.png" width="100"/></a> | <a href="https://github.com/77r77r"><img src="https://github.com/77r77r.png" width="100"/></a> | <a href="https://github.com/lh922"><img src="https://github.com/lh922.png" width="100"/></a> |
| :---: | :---: | :---: | :---: |
| **정윤서** | **김희영** | **노미경** | **이현** 
| 팀장 | 팀원 | 팀원 | 팀원 |


---

## 🧩 핵심 기능

**1. pin(위치 좌표) 등록**

**2. 위치 별 메모, 태그 등록**

**3. 현재 위치 기반 지도 탐색**

**4. pin 에 좋아요, 북마크 등록**

**5. 일자별 등록 위치 트래킹 서비스**

**6. 마이페이지**

---

## ⚙️ 환경 변수 설정

**BACKEND (IntelliJ 환경 변수 설정)**

```java
# Database
URL: jdbc:postgresql://localhost:5432/pinco
USERNAME: user
PASSWORD: password
DRIVER-CLASS-NAME: org.postgresql.Driver

# User
EMAIL=user1@example.com
PASSWORD=12345678

# JWT
SECRET_PATTERN=aVeryLongSecretKey_ChangeMe_2025!
```

**FRONTEND (.env)**

```
NEXT_PUBLIC_API_BASE_URL ="http://localhost:8080"

# Kakao Map API
NEXT_PUBLIC_KAKAO_APP_KEY=kakaoAPIKEY
```

---

## 🧾 API 명세서 (Swagger)

http://localhost:8080/swagger-ui/index.html

---

## 🔗 ERD

<img width="1462" height="668" alt="image" src="https://github.com/user-attachments/assets/04a2b0cc-c829-4a6e-916a-e900347cab66" />

 
---

## ☁️ 시스템 아키텍처

<img width="252" height="781" alt="시스템 구성도  07팀_2차 프로젝트" src="https://github.com/user-attachments/assets/6c2c5732-3fdd-47fb-bf1f-76d934225fd2" />



---

## 🧱 기술 스택
**Frontend**

![Next.js](https://img.shields.io/badge/Next.js-000000?style=for-the-badge&logo=nextdotjs&logoColor=white)
![React](https://img.shields.io/badge/React-61DAFB?style=for-the-badge&logo=react&logoColor=black)
![TypeScript](https://img.shields.io/badge/TypeScript-3178C6?style=for-the-badge&logo=typescript&logoColor=white)
![TailwindCSS](https://img.shields.io/badge/TailwindCSS-38B2AC?style=for-the-badge&logo=tailwindcss&logoColor=white)

---

**Backend**

![Java](https://img.shields.io/badge/Java%2021-007396?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot%203.5.6-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring%20Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)
![Spring Data JPA](https://img.shields.io/badge/Spring%20Data%20JPA-007396?style=for-the-badge&logo=hibernate&logoColor=white)
![Hibernate Spatial](https://img.shields.io/badge/Hibernate%20Spatial-59666C?style=for-the-badge&logo=hibernate&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white)
![Swagger](https://img.shields.io/badge/SpringDoc%20OpenAPI-85EA2D?style=for-the-badge&logo=swagger&logoColor=black)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![H2 Database](https://img.shields.io/badge/H2%20Database-003B57?style=for-the-badge&logo=h2&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)

