package com.finledger.api.group.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.util.List;

public class GroupDtos {
    public record CreateGroupRequest(@Schema(example = "Hail Mary") @NotBlank @Size(max = 60) String name,
                                     @Schema(example = "[\"priya@test.com\", \"aman@test.com\"]") List<@Email String> memberEmails) {}
    public record AddMemberRequest(@Schema(example = "aman@test.com") @NotBlank @Email String email) {}

    public record MemberDto(Long userId, String name, String email, String role) {}
    public record GroupSummaryDto(Long id, String name, int memberCount) {}
    public record GroupDetailDto(Long id, String name, Long createdBy, List<MemberDto> members) {}
}