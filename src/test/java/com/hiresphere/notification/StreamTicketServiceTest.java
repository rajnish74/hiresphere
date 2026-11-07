package com.hiresphere.notification;

import com.hiresphere.common.exception.AppException;
import com.hiresphere.notification.stream.StreamTicketService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StreamTicketServiceTest {

    private final AtomicLong nanos = new AtomicLong(1_000);
    private final StreamTicketService service = new StreamTicketService(nanos::get);

    private void pass(Duration time) {
        nanos.addAndGet(time.toNanos());
    }

    @Test
    void aTicketShouldGiveBackTheUserOnce() {
        String ticket = service.issue(7L);

        assertThat(service.redeem(ticket)).contains(7L);
        assertThat(service.redeem(ticket)).isEmpty();   // doosri baar nahi
    }

    @Test
    void unknownBlankOrMissingTicketsShouldGiveNothing() {
        assertThat(service.redeem("nope")).isEmpty();
        assertThat(service.redeem("  ")).isEmpty();
        assertThat(service.redeem(null)).isEmpty();
    }

    @Test
    void aTicketShouldWorkJustBeforeItsExpiryAndNeverAfter() {
        String early = service.issue(7L);
        pass(Duration.ofSeconds(29));
        assertThat(service.redeem(early)).contains(7L);

        String late = service.issue(8L);
        pass(Duration.ofSeconds(31));
        assertThat(service.redeem(late)).isEmpty();
    }

    @Test
    void eachTicketShouldBelongToItsOwnUser() {
        String first = service.issue(1L);
        String second = service.issue(2L);

        assertThat(service.redeem(second)).contains(2L);
        assertThat(service.redeem(first)).contains(1L);
    }

    @Test
    void ticketsShouldBeLongAndUnique() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 15; i++) {
            String ticket = service.issue(1L);
            assertThat(ticket).hasSizeGreaterThanOrEqualTo(40);
            assertThat(seen.add(ticket)).isTrue();
        }
    }

    @Test
    void issuingShouldBeRateLimitedPerUser() {
        for (int i = 0; i < 20; i++) {
            service.issue(5L);
        }

        assertThatThrownBy(() -> service.issue(5L))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS));
        assertThat(service.issue(6L)).isNotBlank();   // doosra user theek
    }
}