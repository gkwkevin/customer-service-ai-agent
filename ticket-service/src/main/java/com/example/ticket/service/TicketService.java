package com.example.ticket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.ticket.entity.Ticket;
import com.example.ticket.mapper.TicketMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.UUID;

@Service
public class TicketService {

    @Autowired
    private TicketMapper ticketMapper;
    
    @Autowired
    private UserService userService;

    public Ticket createTicket(Ticket ticket) {
        ticket.setTicketNo(generateTicketNo());
        ticket.setStatus(1);
        if (ticket.getPriority() == null) {
            ticket.setPriority(2);
        }
        ticket.setCreateTime(LocalDateTime.now());
        ticket.setUpdateTime(LocalDateTime.now());
        ticketMapper.insert(ticket);
        
        fillUserNickname(List.of(ticket));
        
        return ticket;
    }

    public Ticket getTicketById(Long id) {
        Ticket ticket = ticketMapper.selectById(id);
        if (ticket != null) {
            fillUserNickname(List.of(ticket));
        }
        return ticket;
    }

    public Page<Ticket> getTicketList(Integer page, Integer size, Integer status, Integer priority, String keyword) {
        Page<Ticket> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Ticket> wrapper = new LambdaQueryWrapper<>();
        
        if (status != null) {
            wrapper.eq(Ticket::getStatus, status);
        }
        if (priority != null) {
            wrapper.eq(Ticket::getPriority, priority);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Ticket::getTitle, keyword).or().like(Ticket::getContent, keyword));
        }
        
        wrapper.orderByDesc(Ticket::getCreateTime);
        
        Page<Ticket> result = ticketMapper.selectPage(pageParam, wrapper);
        
        fillUserNickname(result.getRecords());
        
        return result;
    }
    
    private void fillUserNickname(List<Ticket> tickets) {
        if (tickets == null || tickets.isEmpty()) {
            return;
        }
        
        Set<Long> userIds = new HashSet<>();
        for (Ticket ticket : tickets) {
            if (ticket.getUserId() != null) {
                userIds.add(ticket.getUserId());
            }
            if (ticket.getAssigneeId() != null) {
                userIds.add(ticket.getAssigneeId());
            }
        }
        
        if (userIds.isEmpty()) {
            return;
        }
        
        Map<Long, String> nicknameMap = userService.getUserNicknameMap(userIds);
        
        for (Ticket ticket : tickets) {
            if (ticket.getUserId() != null) {
                ticket.setUserNickname(nicknameMap.get(ticket.getUserId()));
            }
            if (ticket.getAssigneeId() != null) {
                ticket.setAssigneeNickname(nicknameMap.get(ticket.getAssigneeId()));
            }
        }
    }

    public Ticket updateTicket(Ticket ticket) {
        ticket.setUpdateTime(LocalDateTime.now());
        ticketMapper.updateById(ticket);
        Ticket updated = ticketMapper.selectById(ticket.getId());
        fillUserNickname(List.of(updated));
        return updated;
    }

    public boolean deleteTicket(Long id) {
        return ticketMapper.deleteById(id) > 0;
    }

    public boolean updateStatus(Long id, Integer status) {
        Ticket ticket = new Ticket();
        ticket.setId(id);
        ticket.setStatus(status);
        ticket.setUpdateTime(LocalDateTime.now());
        return ticketMapper.updateById(ticket) > 0;
    }

    private String generateTicketNo() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String random = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return "TK" + date + random;
    }

    public long countAll() {
        return ticketMapper.selectCount(null);
    }

    public long countByStatus(Integer status) {
        LambdaQueryWrapper<Ticket> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Ticket::getStatus, status);
        return ticketMapper.selectCount(wrapper);
    }
}
