package vn.mar.common.time;

import java.time.Instant;

public interface TimeProvider {

    Instant now();
}
