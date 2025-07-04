package com.Podsproject.walletService.repository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import com.Podsproject.walletService.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Integer> {

    
	Wallet findByUserId(Integer userId);

}

