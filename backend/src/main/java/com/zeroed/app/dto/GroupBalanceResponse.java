package com.zeroed.app.dto;

import java.time.Instant;
import java.util.List;

public record GroupBalanceResponse(
        Long groupId, List<GroupMemberResponse> members,
        List<CurrencyBalance> currencies, Instant generatedAt) {
    public record CurrencyBalance(String currency, List<MemberBalance> members,
                                  List<Transfer> transfers) {}
    public record MemberBalance(Long userId, String paid, String owed, String net) {}
    public record Transfer(GroupMemberResponse from, GroupMemberResponse to, String amount) {}
}
