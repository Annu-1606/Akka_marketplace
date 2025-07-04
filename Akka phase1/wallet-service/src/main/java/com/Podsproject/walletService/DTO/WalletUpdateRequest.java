package com.Podsproject.walletService.DTO;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class WalletUpdateRequest {
    private String action;
    private Integer amount;


}

