package com.Podsproject.walletService.entity;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonProperty;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Wallet {
    @Id
    @Column(name = "user_id")
    private Integer userId;
    private Integer balance;
    
    @Version
    private int version;

}
