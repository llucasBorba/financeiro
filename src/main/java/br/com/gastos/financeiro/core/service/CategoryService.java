package br.com.gastos.financeiro.core.service;

import br.com.gastos.financeiro.core.exception.BusinessException;
import br.com.gastos.financeiro.core.exception.ResourceNotFoundException;
import br.com.gastos.financeiro.core.model.Category;
import br.com.gastos.financeiro.core.ports.ingoing.ChangeCategoryStatusUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.CreateCategoryUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.DeleteCategoryUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.FindCategoryUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.RenameCategoryUseCase;
import br.com.gastos.financeiro.core.ports.outgoing.CategoryRepositoryPort;
import br.com.gastos.financeiro.core.ports.outgoing.ExpenseRepositoryPort;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class CategoryService implements CreateCategoryUseCase, RenameCategoryUseCase,
        ChangeCategoryStatusUseCase, DeleteCategoryUseCase, FindCategoryUseCase {

    private final CategoryRepositoryPort categoryRepository;
    private final ExpenseRepositoryPort expenseRepository;

    public CategoryService(CategoryRepositoryPort categoryRepository,
                           ExpenseRepositoryPort expenseRepository) {
        this.categoryRepository = Objects.requireNonNull(categoryRepository, "O repositório de categorias é obrigatório.");
        this.expenseRepository = Objects.requireNonNull(expenseRepository, "O repositório de despesas é obrigatório.");
    }

    @Override
    public Category execute(CreateCategoryCommand command) {
        Category category = Category.create(command.userId(), command.name());
        rejectDuplicateName(command.userId(), category.getName(), null);
        return categoryRepository.save(category);
    }

    @Override
    public Category execute(RenameCategoryCommand command) {
        Category category = findById(command.categoryId(), command.userId());

        category.rename(command.name());
        // Ignora a propria categoria na checagem: renomear "Comida" para "Comida" e valido.
        rejectDuplicateName(command.userId(), category.getName(), category.getId());

        return categoryRepository.save(category);
    }

    @Override
    public Category execute(ChangeCategoryStatusCommand command) {
        Category category = findById(command.categoryId(), command.userId());

        if (command.active()) {
            category.activate();
        } else {
            category.deactivate();
        }

        return categoryRepository.save(category);
    }

    @Override
    public void execute(UUID categoryId, UUID userId) {
        Category category = findById(categoryId, userId);

        long emUso = expenseRepository.countByCategoryId(categoryId);
        if (emUso > 0) {
            // 409, não 400: o pedido é válido, mas conflita com o estado atual. A saída é
            // arquivar — apagar destruiria a classificação de despesas já registradas.
            throw new IllegalStateException(
                    "Esta categoria não pode ser excluída porque " + emUso
                            + (emUso == 1 ? " despesa a utiliza." : " despesas a utilizam.")
                            + " Arquive-a se não quiser mais usá-la.");
        }

        categoryRepository.deleteById(category.getId());
    }

    @Override
    public Category findById(UUID categoryId, UUID userId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada com o ID: " + categoryId));

        // "Não encontrada" em vez de "acesso negado", como nas despesas: um 403 confirmaria
        // que aquele ID existe e viraria oráculo para descobrir dados de outros usuários.
        if (!category.isOwnedBy(userId)) {
            throw new ResourceNotFoundException("Categoria não encontrada com o ID: " + categoryId);
        }

        return category;
    }

    @Override
    public List<Category> listByUser(UUID userId, boolean includeInactive) {
        return categoryRepository.findByUserId(userId, includeInactive);
    }

    private void rejectDuplicateName(UUID userId, String name, UUID ignoreId) {
        Optional<Category> existente = categoryRepository.findByUserIdAndNameIgnoreCase(userId, name);
        if (existente.isPresent() && !existente.get().getId().equals(ignoreId)) {
            throw new BusinessException("Você já tem uma categoria chamada \"" + name + "\".");
        }
    }
}
