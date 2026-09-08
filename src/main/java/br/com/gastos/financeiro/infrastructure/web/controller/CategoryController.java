package br.com.gastos.financeiro.infrastructure.web.controller;

import br.com.gastos.financeiro.core.model.Category;
import br.com.gastos.financeiro.core.ports.ingoing.ChangeCategoryStatusUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.ChangeCategoryStatusUseCase.ChangeCategoryStatusCommand;
import br.com.gastos.financeiro.core.ports.ingoing.CreateCategoryUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.CreateCategoryUseCase.CreateCategoryCommand;
import br.com.gastos.financeiro.core.ports.ingoing.DeleteCategoryUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.FindCategoryUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.RenameCategoryUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.RenameCategoryUseCase.RenameCategoryCommand;
import br.com.gastos.financeiro.infrastructure.identity.CurrentUser;
import br.com.gastos.financeiro.infrastructure.web.dto.CategoryNameRequest;
import br.com.gastos.financeiro.infrastructure.web.dto.CategoryResponse;
import br.com.gastos.financeiro.infrastructure.web.dto.ChangeCategoryStatusRequest;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CreateCategoryUseCase createCategory;
    private final RenameCategoryUseCase renameCategory;
    private final ChangeCategoryStatusUseCase changeCategoryStatus;
    private final DeleteCategoryUseCase deleteCategory;
    private final FindCategoryUseCase findCategory;

    public CategoryController(CreateCategoryUseCase createCategory,
                              RenameCategoryUseCase renameCategory,
                              ChangeCategoryStatusUseCase changeCategoryStatus,
                              DeleteCategoryUseCase deleteCategory,
                              FindCategoryUseCase findCategory) {
        this.createCategory = createCategory;
        this.renameCategory = renameCategory;
        this.changeCategoryStatus = changeCategoryStatus;
        this.deleteCategory = deleteCategory;
        this.findCategory = findCategory;
    }

    @Operation(summary = "Cria uma categoria de despesa",
            description = "O nome é único entre as suas categorias, sem diferenciar maiúsculas.")
    @PostMapping
    public ResponseEntity<CategoryResponse> create(@CurrentUser UUID userId,
                                                   @Valid @RequestBody CategoryNameRequest request) {
        Category category = createCategory.execute(new CreateCategoryCommand(userId, request.name()));
        return ResponseEntity
                .created(URI.create("/api/categories/" + category.getId()))
                .body(CategoryResponse.from(category));
    }

    /** Por padrão devolve só as ativas — arquivadas não devem poluir um seletor. */
    @Operation(summary = "Lista as categorias",
            description = "Por padrão só as ativas — arquivadas não devem poluir um seletor.")
    @GetMapping
    public ResponseEntity<List<CategoryResponse>> list(
            @CurrentUser UUID userId,
            @RequestParam(defaultValue = "false") boolean includeInactive) {

        return ResponseEntity.ok(findCategory.listByUser(userId, includeInactive).stream()
                .map(CategoryResponse::from)
                .toList());
    }

    @Operation(summary = "Consulta uma categoria")
    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> findById(@CurrentUser UUID userId, @PathVariable UUID id) {
        return ResponseEntity.ok(CategoryResponse.from(findCategory.findById(id, userId)));
    }

    /** Renomear é seguro: as despesas apontam para o id, não para o texto. */
    @Operation(summary = "Renomeia a categoria",
            description = "Seguro: as despesas apontam para o id, não para o texto, então todas passam a exibir o nome novo sem nenhuma reescrita em cascata.")
    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> rename(@CurrentUser UUID userId,
                                                   @PathVariable UUID id,
                                                   @Valid @RequestBody CategoryNameRequest request) {
        Category category = renameCategory.execute(new RenameCategoryCommand(id, userId, request.name()));
        return ResponseEntity.ok(CategoryResponse.from(category));
    }

    /** Arquivar/reativar. PATCH porque altera um aspecto do recurso, não o substitui. */
    @Operation(summary = "Arquiva ou reativa a categoria",
            description = "Arquivada some dos seletores e não aceita lançamento novo, mas o histórico segue intacto.")
    @PatchMapping("/{id}/active")
    public ResponseEntity<CategoryResponse> changeStatus(@CurrentUser UUID userId,
                                                         @PathVariable UUID id,
                                                         @Valid @RequestBody ChangeCategoryStatusRequest request) {
        Category category = changeCategoryStatus.execute(
                new ChangeCategoryStatusCommand(id, userId, request.active()));
        return ResponseEntity.ok(CategoryResponse.from(category));
    }

    /** Só remove categoria sem uso. Em uso, responde 409 sugerindo arquivar. */
    @Operation(summary = "Exclui a categoria",
            description = "Só remove categoria sem uso. Em uso, devolve 409 dizendo quantas despesas dependem dela — apagar destruiria a classificação do histórico. Nesse caso, arquive.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@CurrentUser UUID userId, @PathVariable UUID id) {
        deleteCategory.execute(id, userId);
        return ResponseEntity.noContent().build();
    }
}
