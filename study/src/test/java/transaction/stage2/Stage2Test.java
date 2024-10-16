package transaction.stage2;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 트랜잭션 전파(Transaction Propagation)란?
 * 트랜잭션의 경계에서 이미 진행 중인 트랜잭션이 있을 때 또는 없을 때 어떻게 동작할 것인가를 결정하는 방식을 말한다.
 *
 * FirstUserService 클래스의 메서드를 실행할 때 첫 번째 트랜잭션이 생성된다.
 * SecondUserService 클래스의 메서드를 실행할 때 두 번째 트랜잭션이 어떻게 되는지 관찰해보자.
 *
 * https://docs.spring.io/spring-framework/docs/current/reference/html/data-access.html#tx-propagation
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Stage2Test {

    private static final Logger log = LoggerFactory.getLogger(Stage2Test.class);

    @Autowired
    private FirstUserService firstUserService;

    @Autowired
    private UserRepository userRepository;

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    /**
     * 생성된 트랜잭션이 몇 개인가? 왜 그런 결과가 나왔을까?
     * -> 1 개이다.
     * -> propagation의 기본 설정은 REQUIRED는 외부 (논리적) 트랜잭션과 내부 (논리적) 트랜잭션이 동일한 물리적 트랜잭션에 매핑된다.
     * 때문에 기본의 saveFirstTransactionWithRequired 트랜잭션에 병합되어 1개만 존재하게 되었다.
     */
    @Test
    void testRequired() {
        final var actual = firstUserService.saveFirstTransactionWithRequired();

        log.info("transactions : {}", actual);
        assertThat(actual)
                .hasSize(1)
                .containsExactly("transaction.stage2.FirstUserService.saveFirstTransactionWithRequired");
    }

    /**
     * 생성된 트랜잭션이 몇 개인가? 왜 그런 결과가 나왔을까?
     * -> 2개이다.
     * -> REQUIRED_NEW의 경우 트랜잭션의 경계에서 이미 진행 중인 트랜잭션이 있을 때와 없을 때와 관련없이 독립된 물리적 트랜잭션에 매핑된다.
     */
    @Test
    void testRequiredNew() {
        final var actual = firstUserService.saveFirstTransactionWithRequiredNew();

        log.info("transactions : {}", actual);
        assertThat(actual)
                .hasSize(2)
                .containsExactly("transaction.stage2.SecondUserService.saveSecondTransactionWithRequiresNew",
                        "transaction.stage2.FirstUserService.saveFirstTransactionWithRequiredNew");
    }

    /**
     * firstUserService.saveAndExceptionWithRequiredNew()에서 강제로 예외를 발생시킨다. REQUIRES_NEW 일 때 예외로 인한 롤백이 발생하면서 어떤 상황이 발생하는
     * 지 확인해보자.
     * -> 서로 독립된 물리적 트랜잭션에 매핑되어 있어 이미 커밋이 진행된 내부 트랜잭션으로 인해 DB에 레코드가 1개 존재한다.
     */
    @Test
    void testRequiredNewWithRollback() {
        assertThat(firstUserService.findAll()).isEmpty();

        assertThatThrownBy(() -> firstUserService.saveAndExceptionWithRequiredNew())
                .isInstanceOf(RuntimeException.class);

        assertThat(firstUserService.findAll()).hasSize(1);
    }

    /**
     * FirstUserService.saveFirstTransactionWithSupports() 메서드를 보면 @Transactional이 주석으로 되어 있다. 주석인 상태에서 테스트를 실행했을 때와 주석을
     * 해제하고 테스트를 실행했을 때 어떤 차이점이 있는지 확인해보자.
     * -> 주석인 상태에서 테스트 한 경우 트랜잭션이 출력은 되나 활성화가 되지 않는다. 즉, 비트랜잭션으로 로직을 수행한다.
     * -> 주석을 해제한 경우 외부 트랜잭션과 하나의 물리 트랜잭션으로 매핑되어 로직이 수행된다.
     */
    @Test
    void testSupports() {
        final var actual = firstUserService.saveFirstTransactionWithSupports();

        log.info("transactions : {}", actual);
        assertThat(actual)
                .hasSize(1)
                .containsExactly("transaction.stage2.FirstUserService.saveFirstTransactionWithSupports");
    }

    /**
     * FirstUserService.saveFirstTransactionWithMandatory() 메서드를 보면 @Transactional이 주석으로 되어 있다. 주석인 상태에서 테스트를 실행했을 때와
     * 주석을 해제하고 테스트를 실행했을 때 어떤 차이점이 있는지 확인해보자. SUPPORTS와 어떤 점이 다른지도 같이 챙겨보자.
     * -> 주석인 상태에서 테스트 한 경우 IllegalTransactionStateException 예외가 발생한다. (SUPPORTS와 다른 점)
     * -> 주석을 해제한 경우 외부 트랜잭션과 하나의 물리 트랜잭션으로 매핑되어 로직이 수행된다.
     */
    @Test
    void testMandatory() {
        final var actual = firstUserService.saveFirstTransactionWithMandatory();

        log.info("transactions : {}", actual);
        assertThat(actual)
                .hasSize(1)
                .containsExactly("transaction.stage2.FirstUserService.saveFirstTransactionWithMandatory");
    }

    /**
     * 아래 테스트는 몇 개의 물리적 트랜잭션이 동작할까? FirstUserService.saveFirstTransactionWithNotSupported() 메서드의 @Transactional을 주석
     * 처리하자. 다시 테스트를 실행하면 몇 개의 물리적 트랜잭션이 동작할까?
     * -> 둘 다 2 개의 물리 트랜잭션이 동작한다.
     * -> NOT_SUPPORTED 옵션은 비트랜잭션으로 동작하여 각각을 물리 트랜잭션으로 동작한다.
     *
     * 스프링 공식 문서에서 물리적 트랜잭션과 논리적 트랜잭션의 차이점이 무엇인지 찾아보자.
     * -> 리뷰어 아토가 제공해준 공식문서를 참고하여 논리적 트랜잭션은 Transactional이 붙은 메서드 단위, 물리적 트랜잭션은 최종적으로 DB에 커밋/롤백되는 트랜잭션을 간접적으로 그림으로 설명해주었다.
     */
    @Test
    void testNotSupported() {
        final var actual = firstUserService.saveFirstTransactionWithNotSupported();

        log.info("transactions : {}", actual);
        assertThat(actual)
                .hasSize(2)
                .containsExactly("transaction.stage2.SecondUserService.saveSecondTransactionWithNotSupported",
                        "transaction.stage2.FirstUserService.saveFirstTransactionWithNotSupported");
    }

    /**
     * 아래 테스트는 왜 실패할까? FirstUserService.saveFirstTransactionWithNested() 메서드의 @Transactional을 주석 처리하면 어떻게 될까?
     * -> Nested의 경우 외부 트랜잭션 안에서 논리적 트랜잭션을 생성한다. 떄문에 전체 트랜잭션을 롤백하는 대신 savepoint라는 개념으로
     * -> 특정 지점까지만 롤백할 수 있다. 하지만 JPA는 이러한 기능을 지원하지 않아 예외가 발생한다.
     * -> 기존 트랜잭션이 없다면 REQUIRED와 동일하게 동작하여 정상 동작한다.
     */
    @Test
    void testNested() {
        final var actual = firstUserService.saveFirstTransactionWithNested();

        log.info("transactions : {}", actual);
        assertThat(actual)
                .hasSize(1)
                .containsExactly("transaction.stage2.SecondUserService.saveSecondTransactionWithNested");
    }

    /**
     * 마찬가지로 @Transactional을 주석처리하면서 관찰해보자.
     * -> Propagation NEVER 설정은 비트랜잭션으로 동작하되 트랜잭션이 존재하면 IllegalTransactionStateException 예외가 발생한다.
     */
    @Test
    void testNever() {
        final var actual = firstUserService.saveFirstTransactionWithNever();

        log.info("transactions : {}", actual);
        assertThat(actual)
                .hasSize(1)
                .containsExactly("transaction.stage2.SecondUserService.saveSecondTransactionWithNever");
    }
}
