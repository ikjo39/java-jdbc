# JDBC 라이브러리 구현하기

## 1단계 - JDBC 라이브러리 구현하기

### 구현할 기능 목록

- [x] JDBC API를 이용한 DAO 로직을 구현한다.
    - [x] findAll 로직 구현
    - [x] update 구현
    - [x] 테스트 코드 작성
- [x] DAO를 추상화한다.
- [x] ResultSet을 객체로 변환하는 인터페이스 RowMapper를 구현한다.
    - [x] Domain들의 RowMapper 구현
- [x] 반복적인 DB 관련 작업을 대신 수행할 JdbcTemplate 구현하기

## 2단계 - 리팩토링

- [x] 학습테스트 제출하기
- [x] RowMapper 템플릿 콜백 패턴 적용하기
- [x] KeyHolder 를 이용하는 메서드 생성하기 
