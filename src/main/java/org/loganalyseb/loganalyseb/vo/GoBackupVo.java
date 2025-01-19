package org.loganalyseb.loganalyseb.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Optional;

@Data
public class GoBackupVo {
    private Optional<LocalDateTime> dateDebutMesDoc = Optional.empty();
    private Optional<LocalDateTime> dateDebutCompressionMesdoc = Optional.empty();
    private Optional<LocalDateTime> dateDebutCryptageMesdoc = Optional.empty();
    private Optional<LocalDateTime> dateDebutEnregistrementHash = Optional.empty();
}
