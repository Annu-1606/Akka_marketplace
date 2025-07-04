package com.Podsproject.walletService.container;

import com.Podsproject.walletService.DTO.WalletResponse;
import com.Podsproject.walletService.DTO.WalletUpdateRequest;
import com.Podsproject.walletService.entity.Wallet;
import com.Podsproject.walletService.services.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@RestController
public class WalletController {

    @Autowired
    private WalletService walletService;

    @GetMapping("/wallets/{user_id}")
    public ResponseEntity<?> getWalletDetails(@PathVariable("user_id") Integer userId) {
        Wallet wallet = walletService.getWalletByUserId(userId);
        if (wallet != null) {
            WalletResponse walletResponse = new WalletResponse();
            walletResponse.setUser_id(userId);
            walletResponse.setBalance(wallet.getBalance());
            return ResponseEntity.ok(walletResponse);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Wallet not found for user with ID: " + userId);
        }
    }


    @PutMapping("/wallets/{user_id}")
    public ResponseEntity<?> updateWalletBalance(@PathVariable("user_id") Integer userId,  @RequestBody WalletUpdateRequest request) {
        Wallet wallet = walletService.getOrCreateWallet(userId);

        if (wallet == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Failed to create wallet for user with ID: " + userId);
        }

        if ("debit".equals(request.getAction())) {
            int updatedBalance = walletService.debitAmount(wallet, request.getAmount());
            if (updatedBalance == -1) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Insufficient balance for user with ID: " + userId);
            }
            return ResponseEntity.ok(new WalletResponse(userId, updatedBalance));
        } else if ("credit".equals(request.getAction())) {
            int updatedBalance = walletService.creditAmount(wallet, request.getAmount());
            return ResponseEntity.ok(new WalletResponse(userId, updatedBalance));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid action");
        }
    }

    @DeleteMapping("/wallets/{user_id}")
    public ResponseEntity<?> deleteWallet(@PathVariable("user_id") Integer userId) {
        try {
            boolean deleted = walletService.deleteWallet(userId);
            if (deleted) {
                return ResponseEntity.ok().build(); // OK status code if wallet is deleted
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); // Not Found status code if user doesn't have a wallet
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // Internal Server Error status code for other errors
        }
    }


    @DeleteMapping("/wallets")
    public ResponseEntity<?> deleteAllWallets() {
        try {
            walletService.deleteAllWallets();
            return ResponseEntity.ok().build(); // OK status code after deleting all wallets
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // Internal Server Error status code for other errors
        }
    }

}

