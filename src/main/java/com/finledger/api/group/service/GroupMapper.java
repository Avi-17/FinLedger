package com.finledger.api.group.service;

import com.finledger.api.group.domain.ExpenseGroup;
import com.finledger.api.group.domain.GroupMember;

import com.finledger.api.group.api.GroupDtos.*;
import com.finledger.api.user.domain.User;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

/** Entity -> DTO conversion for groups. No business logic, no database access. */
@Component
public class GroupMapper {

    public GroupDetailDto toDetail(ExpenseGroup g, List<GroupMember> members, Map<Long, User> users) {
        List<MemberDto> dtos = members.stream().map(m -> {
            User u = users.get(m.getUserId());
            return new MemberDto(u.getId(), u.getName(), u.getEmail(), m.getRole().name());
        }).toList();
        return new GroupDetailDto(g.getId(), g.getName(), g.getCreatedBy(), dtos);
    }

    public GroupSummaryDto toSummary(ExpenseGroup g, long memberCount) {
        return new GroupSummaryDto(g.getId(), g.getName(), (int) memberCount);
    }
}
