package com.umc.pediupatrao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.umc.pediupatrao.entity.CampoAlterado;
import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.TipoOperacao;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.repository.ClienteRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ClienteServiceTest {

    private ClienteRepository clienteRepository;
    private AuditoriaService auditoriaService;
    private ClienteService service;

    @BeforeEach
    void configurar() {
        clienteRepository = mock(ClienteRepository.class);
        auditoriaService = mock(AuditoriaService.class);
        service = new ClienteService(clienteRepository, auditoriaService);

        when(clienteRepository.findById("c1")).thenReturn(Optional.of(cliente("Rua das Flores", "(11) 99999-1234")));
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static Cliente cliente(String logradouro, String telefone) {
        Cliente c = new Cliente();
        c.setId("c1");
        c.setNome("Maria Souza");
        c.setTelefone(telefone);
        c.setLogradouro(logradouro);
        c.setNumero("10");
        c.setCidade("Mogi");
        c.setEstado("SP");
        return c;
    }

    @Test
    void alteracaoDeEnderecoETelefoneVaiParaAuditoriaComValorAntigoENovo() {
        service.atualizar("c1", cliente("Av. Brasil", "(11) 98888-7777"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CampoAlterado>> captor = ArgumentCaptor.forClass(List.class);
        verify(auditoriaService).registrar(eq(TipoOperacao.ALTERACAO_CLIENTE), eq("Cliente"), eq("c1"),
                anyString(), captor.capture(), isNull());

        List<CampoAlterado> alteracoes = captor.getValue();
        assertThat(alteracoes).hasSize(2);
        assertThat(alteracoes).anySatisfy(c -> {
            assertThat(c.getCampo()).isEqualTo("logradouro");
            assertThat(c.getValorAnterior()).isEqualTo("Rua das Flores");
            assertThat(c.getValorNovo()).isEqualTo("Av. Brasil");
        });
        assertThat(alteracoes).anySatisfy(c -> {
            assertThat(c.getCampo()).isEqualTo("telefone");
            assertThat(c.getValorAnterior()).isEqualTo("(11) 99999-1234");
            assertThat(c.getValorNovo()).isEqualTo("(11) 98888-7777");
        });
    }

    @Test
    void semMudancaNaoGeraAuditoria() {
        service.atualizar("c1", cliente("Rua das Flores", "(11) 99999-1234"));
        verify(auditoriaService, never()).registrar(any(), anyString(), anyString(), anyString(), anyList(), any());
    }

    @Test
    void telefoneInvalidoEhRecusado() {
        assertThatThrownBy(() -> service.atualizar("c1", cliente("Av. Brasil", "123")))
                .isInstanceOf(RegraNegocioException.class);
        verify(clienteRepository, never()).save(any());
    }
}
