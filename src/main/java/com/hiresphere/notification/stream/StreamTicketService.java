package com.hiresphere.notification.stream;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import com.hiresphere.common.exception.AppException;
import com.hiresphere.common.util.FixedWindowLimiter;
import com.hiresphere.common.util.TokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
public class StreamTicketService {

    public static final Duration TTL = Duration.ofSeconds(30);

    private record Issued(Long userId, long expiresAtNanos) {
    }

    private final Ticker ticker;
    private final Cache<String, Issued> tickets;
    private final FixedWindowLimiter issueLimiter = new FixedWindowLimiter(20, Duration.ofMinutes(1));

    @Autowired
    public StreamTicketService() {
        this(Ticker.systemTicker());
    }

    public StreamTicketService(Ticker ticker) {
        this.ticker = ticker;
        this.tickets = Caffeine.newBuilder().ticker(ticker).expireAfterWrite(TTL.plusSeconds(30)).maximumSize(10_000).build();
    }

    public String issue(Long userId) {
        if (!issueLimiter.tryAcquire("user:" + userId)) {
            throw new AppException(HttpStatus.TOO_MANY_REQUESTS, "Too many requests. Please slow down.");
        }
        String ticket = TokenUtil.generateToken();
        tickets.put(ticket, new Issued(userId, ticker.read() + TTL.toNanos()));
        return ticket;
    }

    public Optional<Long> redeem(String ticket) {
        if (ticket == null || ticket.isBlank()) {
            return Optional.empty();
        }
        Issued issued = tickets.asMap().remove(ticket);
        if (issued == null || ticker.read() >= issued.expiresAtNanos()) {
            return Optional.empty();
        }
        return Optional.of(issued.userId());
    }
}