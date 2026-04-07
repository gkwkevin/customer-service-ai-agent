package com.example.ticket.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.ticket.common.Result;
import com.example.ticket.entity.Ticket;
import com.example.ticket.entity.User;
import com.example.ticket.service.TicketService;
import com.example.ticket.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ticket")
public class TicketController {

    @Autowired
    private TicketService ticketService;
    
    @Autowired
    private UserService userService;

    @PostMapping
    public Result<Ticket> create(@RequestBody Ticket ticket) {
        Ticket created = ticketService.createTicket(ticket);
        return Result.success(created);
    }

    @GetMapping("/{id}")
    public Result<Ticket> getById(@PathVariable("id") Long id) {
        Ticket ticket = ticketService.getTicketById(id);
        if (ticket == null) {
            return Result.error(404, "工单不存在");
        }
        return Result.success(ticket);
    }

    @GetMapping("/list")
    public Result<Map<String, Object>> list(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "priority", required = false) Integer priority,
            @RequestParam(value = "keyword", required = false) String keyword) {
        
        Page<Ticket> pageResult = ticketService.getTicketList(page, size, status, priority, keyword);
        
        Map<String, Object> result = new HashMap<>();
        result.put("list", pageResult.getRecords());
        result.put("total", pageResult.getTotal());
        result.put("page", page);
        result.put("size", size);
        
        return Result.success(result);
    }

    @PutMapping("/{id}")
    public Result<Ticket> update(@PathVariable("id") Long id, @RequestBody Ticket ticket) {
        ticket.setId(id);
        Ticket updated = ticketService.updateTicket(ticket);
        return Result.success(updated);
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable("id") Long id) {
        boolean success = ticketService.deleteTicket(id);
        return Result.success(success);
    }

    @PutMapping("/{id}/status")
    public Result<Boolean> updateStatus(@PathVariable("id") Long id, @RequestParam("status") Integer status) {
        boolean success = ticketService.updateStatus(id, status);
        return Result.success(success);
    }

    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        long total = ticketService.countAll();
        long pending = ticketService.countByStatus(1);
        long resolved = ticketService.countByStatus(3);
        
        double resolveRate = total > 0 ? (resolved * 100.0 / total) : 0;
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("pending", pending);
        stats.put("resolved", resolved);
        stats.put("resolveRate", String.format("%.0f", resolveRate));
        
        return Result.success(stats);
    }
    
    @GetMapping("/agents")
    public Result<List<User>> getAgents() {
        List<User> agents = userService.getAgents();
        return Result.success(agents);
    }
}
