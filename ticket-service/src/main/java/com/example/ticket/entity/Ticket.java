package com.example.ticket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ticket")
public class Ticket {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String ticketNo;

    private String sessionId;

    private Long userId;

    private String title;

    private String content;

    private Integer status;

    private Integer priority;

    private Long assigneeId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
    
    @TableField(exist = false)
    private String userNickname;
    
    @TableField(exist = false)
    private String assigneeNickname;
}
