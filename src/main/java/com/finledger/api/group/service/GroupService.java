package com.finledger.api.group.service;

import com.finledger.api.group.access.GroupAccessPolicy;
import com.finledger.api.group.domain.ExpenseGroup;
import com.finledger.api.group.domain.GroupMember;
import com.finledger.api.group.repository.GroupMemberRepository;
import com.finledger.api.group.repository.GroupRepository;

import com.finledger.api.common.NotFoundException;
import com.finledger.api.group.api.GroupDtos.*;
import com.finledger.api.user.domain.User;
import com.finledger.api.user.repository.UserDirectory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;

/** Group use cases: create, add member, list, detail. Access rules -> GroupAccessPolicy, DTOs -> GroupMapper. */
@Service
@RequiredArgsConstructor
public class GroupService {
    private final GroupRepository groups;
    private final GroupMemberRepository members;
    private final GroupAccessPolicy access;
    private final GroupMapper mapper;
    private final UserDirectory userDirectory;

    @Transactional
    public GroupDetailDto createGroup(Long userId, CreateGroupRequest req) {
        ExpenseGroup g = groups.save(ExpenseGroup.create(req.name(), userId));
        members.save(new GroupMember(g.getId(), userId, GroupMember.Role.ADMIN));
        if (req.memberEmails() != null) req.memberEmails().forEach(e -> addByEmail(g.getId(), e));
        return detail(userId, g.getId());
    }

    @Transactional
    public GroupDetailDto addMember(Long userId, Long groupId, String email) {
        access.requireAdmin(groupId, userId);
        addByEmail(groupId, email);
        return detail(userId, groupId);
    }

    @Transactional(readOnly = true)
    public List<GroupSummaryDto> myGroups(Long userId) {
        List<ExpenseGroup> list = groups.findAllForUser(userId);
        if (list.isEmpty()) return List.of();

        Map<Long, Long> counts = members.countByGroupIds(list.stream().map(ExpenseGroup::getId).toList()).stream()
                .collect(Collectors.toMap(GroupMemberRepository.GroupCount::getGroupId,
                                          GroupMemberRepository.GroupCount::getCount));
        return list.stream().map(g -> mapper.toSummary(g, counts.getOrDefault(g.getId(), 0L))).toList();
    }

    @Transactional(readOnly = true)
    public GroupDetailDto detail(Long userId, Long groupId) {
        access.requireMember(groupId, userId);
        ExpenseGroup g = groups.findById(groupId).orElseThrow(() -> new NotFoundException("Group not found"));
        List<GroupMember> ms = members.findByGroupId(groupId);
        Map<Long, User> users = userDirectory.byIds(ms.stream().map(GroupMember::getUserId).toList());
        return mapper.toDetail(g, ms, users);
    }

    private void addByEmail(Long groupId, String email) {
        User u = userDirectory.getByEmail(email);
        if (!members.existsByGroupIdAndUserId(groupId, u.getId()))
            members.save(new GroupMember(groupId, u.getId(), GroupMember.Role.MEMBER));
    }
}
