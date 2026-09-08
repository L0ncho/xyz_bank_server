package cl.duoc.xyzbank.bffweb.dashboard.application.ports;

import cl.duoc.xyzbank.bffweb.dashboard.application.dto.InterestSummary;

public interface InterestPort {

    InterestSummary fetchSummary(String accountId, String year);
}
