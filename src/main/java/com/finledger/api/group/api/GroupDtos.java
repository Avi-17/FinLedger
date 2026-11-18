package com.finledger.api.group.api;

import jakarta.validation.constraints.*;
import java.util.List;

public class GroupDtos {
    public record CreateGroupRequest(@NotBlank @Size(max = 60) String name,
                                     List<@Email String> memberEmails) {}
    public record AddMemberRequest(@NotBlank @Email String email) {}

    public record MemberDto(Long userId, String name, String email, String role) {}
    public record GroupSummaryDto(Long id, String name, int memberCount) {}
    public record GroupDetailDto(Long id, String name, Long createdBy, List<MemberDto> members) {}
}