package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.dto.CancelamentoRequest;
import com.umc.pediupatrao.dto.DescontoRequest;
import com.umc.pediupatrao.dto.PedidoRequest;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.service.PedidoService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    // Registra a entrada do pedido. Preços, total, status e responsável são definidos pelo servidor.
    @PostMapping
    public ResponseEntity<Pedido> criarPedido(@RequestBody PedidoRequest pedido) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pedidoService.criarPedido(pedido));
    }

    @GetMapping
    public List<Pedido> listarPedidos() {
        return pedidoService.listarPedidos();
    }

    @GetMapping("/{id}")
    public Pedido buscarPedido(@PathVariable String id) {
        return pedidoService.buscarPorId(id);
    }

    @PutMapping("/{id}")
    public Pedido alterarPedido(@PathVariable String id, @RequestBody PedidoRequest pedido) {
        return pedidoService.alterarPedido(id, pedido);
    }

    @PutMapping("/{id}/status")
    public Pedido atualizarStatus(@PathVariable String id, @RequestParam StatusPedido status) {
        return pedidoService.atualizarStatus(id, status);
    }

    // Exclusivo do GERENTE: percentual de 0 a 20%, antes da saída.
    @PostMapping("/{id}/desconto")
    public Pedido aplicarDesconto(@PathVariable String id, @RequestBody DescontoRequest desconto) {
        return pedidoService.aplicarDesconto(id, desconto.getPercentual(), desconto.getJustificativa());
    }

    // Exclusivo do GERENTE: exige justificativa e só é permitido antes da saída.
    @PostMapping("/{id}/cancelamento")
    public Pedido cancelarPedido(@PathVariable String id, @RequestBody CancelamentoRequest cancelamento) {
        return pedidoService.cancelarPedido(id, cancelamento.getJustificativa());
    }
}
