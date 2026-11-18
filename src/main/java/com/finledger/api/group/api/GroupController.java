package com.finledger.api.group.api;

import com.finledger.api.group.service.GroupService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.finledger.api.group.api.GroupDtos.*;

import java.util.*;


@RestController
@RequestMapping("/groups")
@RequiredArgsConstructor
public class GroupController {
    private final GroupService groupService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GroupDetailDto createGroup(@AuthenticationPrincipal Long userId,
                                                @Valid @RequestBody CreateGroupRequest req) {
        return groupService.createGroup(userId, req);
    }

    @GetMapping
    public List<GroupSummaryDto> mine(@AuthenticationPrincipal Long userId) {
        return groupService.myGroups(userId);
    }

    @GetMapping("/{groupId}")
    public GroupDetailDto detail(@AuthenticationPrincipal Long userId, @PathVariable Long groupId) {
        return groupService.detail(userId, groupId);
    }

    @PostMapping("/{groupId}/members")
    public GroupDetailDto addMember(@AuthenticationPrincipal Long userId, @PathVariable Long groupId,
                                    @Valid @RequestBody AddMemberRequest req) {
        return groupService.addMember(userId, groupId, req.email());
    }

}
