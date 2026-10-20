package br.com.nandesk.commission.infra;

import br.com.nandesk.commission.domain.Payout;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayoutRepository extends JpaRepository<Payout, Long> {

    Page<Payout> findAllByOrderByPaidAtDesc(Pageable pageable);
}
