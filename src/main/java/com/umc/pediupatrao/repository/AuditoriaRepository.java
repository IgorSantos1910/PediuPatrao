package com.umc.pediupatrao.repository;

import com.umc.pediupatrao.entity.RegistroAuditoria;
import java.util.List;
import org.springframework.data.repository.Repository;

// Estende Repository (e não MongoRepository) para expor só insert e consultas:
// assim não existe save (update) nem delete para os registros de auditoria.
public interface AuditoriaRepository extends Repository<RegistroAuditoria, String> {

    RegistroAuditoria insert(RegistroAuditoria registro);

    List<RegistroAuditoria> findAllByOrderByDataHoraDesc();

    List<RegistroAuditoria> findByEntidadeAndEntidadeIdOrderByDataHoraDesc(String entidade, String entidadeId);
}
