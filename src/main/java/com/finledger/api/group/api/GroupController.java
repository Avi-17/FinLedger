package com.finledger.api.group.api;

import com.finledger.api.group.service.GroupService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.finledger.api.group.api.GroupDtos.*;

import java.util.*;


@Tag(name = "Groups")
@RestController
@RequestMapping("/groups")
@RequiredArgsConstructor
public class GroupController {
    private final GroupService groupService;

    @Operation(summary = "Create a group", description = "The creator becomes ADMIN. Members are added by email.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GroupDetailDto createGroup(@AuthenticationPrincipal Long userId,
                                                @Valid @RequestBody CreateGroupRequest req) {
        return groupService.createGroup(userId, req);
    }

    @Operation(summary = "My groups")
    @GetMapping
    public List<GroupSummaryDto> mine(@AuthenticationPrincipal Long userId) {
        return groupService.myGroups(userId);
    }

    @Operation(summary = "Group detail with members", description = "Members only.")
    @GetMapping("/{groupId}")
    public GroupDetailDto detail(@AuthenticationPrincipal Long userId, @PathVariable Long groupId) {
        return groupService.detail(userId, groupId);
    }

    @Operation(summary = "Add a member by email", description = "Group admins only.")
    @PostMapping("/{groupId}/members")
    public GroupDetailDto addMember(@AuthenticationPrincipal Long userId, @PathVariable Long groupId,
                                    @Valid @RequestBody AddMemberRequest req) {
        return groupService.addMember(userId, groupId, req.email());
    }

}
